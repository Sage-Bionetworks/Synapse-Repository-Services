package org.sagebionetworks.repo.manager.grid.internal.replica.export;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.LocalFileUploadRequest;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.GridReplicaPatchBuilderManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.SynapseRow;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.GridReplicaViewManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.grid.DownloadFromGridRequest;
import org.sagebionetworks.repo.model.grid.DownloadFromGridResult;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.table.cluster.utils.CSVUtils;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.util.csv.CSVWriterProvider;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.stereotype.Service;

import au.com.bytecode.opencsv.CSVWriter;

@Service
public class GridReplicaCsvExporterImpl implements GridReplicaCsvExporter {
    private final GridManager gridManager;
    private final GridReplicaPatchBuilderManager replicaPatchBuilderManager;
    private final GridReplicaViewManager gridReplicaViewManager;
    private final CSVWriterProvider csvWriterProvider;
    private final FileHandleManager fileHandleManager;

    public GridReplicaCsvExporterImpl(
            GridManager gridManager,
            GridReplicaPatchBuilderManager replicaPatchBuilderManager,
            GridReplicaViewManager gridReplicaViewManager,
            CSVWriterProvider csvWriterProvider,
            FileHandleManager fileHandleManager
    ) {
        this.gridManager = gridManager;
        this.replicaPatchBuilderManager = replicaPatchBuilderManager;
        this.gridReplicaViewManager = gridReplicaViewManager;
        this.csvWriterProvider = csvWriterProvider;
        this.fileHandleManager = fileHandleManager;
    }

    @Override
    public DownloadFromGridResult exportGridAsCsv(String jobId, UserInfo userInfo, DownloadFromGridRequest request, AsyncJobProgressCallback jobProgressCallback) throws IOException {
        ValidateArgument.required(jobId, "jobId");
        ValidateArgument.required(userInfo, "userInfo");
        ValidateArgument.required(request.getSessionId(), "request.sessionId");

        GridHeader header = checkSessionAndGetHeader(userInfo, request.getSessionId());

        String fileName = "Job-" + jobId;
        File temp = null;
        try {
            // For other CSV writers (e.g. tables), we estimate progress by first counting the total number of rows to be written.
            // This is a potentially expensive operation for a grid, so instead we will just estimate that writing the CSV is 50% of the work and uploading to S3 is the other 50%.
            jobProgressCallback.updateProgress("Starting export of grid session to CSV...", 0L, 100L);

            // The CSV data will first be written to this file.
            temp = File.createTempFile(fileName, "." + CSVUtils.guessExtension(
                    request.getCsvTableDescriptor() == null ? null : request.getCsvTableDescriptor().getSeparator()));
            try (CSVWriter writer = csvWriterProvider.createWriter(new FileWriter(temp), request.getCsvTableDescriptor())) {
                this.writeToCsv(header, request, writer);
            }

            // At this point we have the entire CSV written to a local file.
            // Upload the file to S3 can create the filehandle.
            jobProgressCallback.updateProgress("Finished writing CSV file. Uploading to S3...", 50L, 100L);
            String contentType = CSVUtils.guessContentType(request
                    .getCsvTableDescriptor() == null ? null : request.getCsvTableDescriptor().getSeparator());
            String requestFileName = request.getFileName() == null ? null : request.getFileName();
            S3FileHandle fileHandle = fileHandleManager.uploadLocalFile(new LocalFileUploadRequest().withUserId(userInfo.getId().toString()).withFileToUpload(temp).withContentType(contentType)
                    .withFileName(requestFileName));

            DownloadFromGridResult result = new DownloadFromGridResult();
            result.setSessionId(request.getSessionId());
            result.setResultsFileHandleId(fileHandle.getId());
            return result;
        } finally {
            if (temp != null) {
                temp.delete();
            }
        }
    }

    GridHeader checkSessionAndGetHeader(UserInfo userInfo, String gridSessionId) {
        // Get the grid session (and verify that the userInfo has access to it)
        gridManager.getGridSession(userInfo, gridSessionId);

        GridConnectionInfo connectionInfo = gridManager.getDefaultInternalConnection(gridSessionId)
                .orElseThrow(() -> new RecoverableMessageException("No internal connection found for session: " + gridSessionId));

        replicaPatchBuilderManager.getCurrentClockIfAllPatchesApplied(connectionInfo.getSessionId(), connectionInfo.getReplicaId())
                .orElseThrow(() -> new RecoverableMessageException("Current clock could not be retrieved, patches are still being applied to sessionId: " + connectionInfo.getSessionId() + ", replicaId: " + connectionInfo.getReplicaId()));
        return gridReplicaViewManager.readHeader(connectionInfo.getSessionId(), connectionInfo.getReplicaId())
                .orElseThrow(() -> new RecoverableMessageException("Grid header has not yet been instantiated for sessionId: " + gridSessionId));

    }

    void writeToCsv(GridHeader header, DownloadFromGridRequest request, CSVWriter writer) {
        boolean writeHeader = request.getWriteHeader() != null ? request.getWriteHeader() : true;
        boolean includeRowIdAndRowVersion = request.getIncludeRowIdAndRowVersion() != null ? request.getIncludeRowIdAndRowVersion() : true;
        boolean includeEtag = request.getIncludeEtag() != null ? request.getIncludeEtag() : true;

        try {
            if (writeHeader) {
                List<String> csvHeader = new ArrayList<>();
                if (includeRowIdAndRowVersion) {
                    csvHeader.add(TableConstants.ROW_ID);
                    csvHeader.add(TableConstants.ROW_VERSION);
                }
                if (includeEtag) {
                    csvHeader.add("etag");
                }
                header.getOrderedColumns().stream().map(Column::getName).forEach(csvHeader::add);
                writer.writeNext(csvHeader.toArray(new String[0]));
            }

            Iterator<RowView> iterator = gridReplicaViewManager.getQueryIterator(header, Collections.emptyList());

            while (iterator.hasNext()) {
                RowView rowView = iterator.next();
                List<String> csvRow = new ArrayList<>();
                SynapseRow synapseRow = rowView.getSynapseRow();

                if (includeRowIdAndRowVersion) {
                    Long rowId = synapseRow != null ? synapseRow.getRowId() : null;
                    csvRow.add(rowId == null ? "" : rowId.toString());

                    Long rowVersion = synapseRow != null ? synapseRow.getVersionNumber() : null;
                    csvRow.add(rowVersion == null ? "" : rowVersion.toString());
                }

                if (includeEtag) {
                    String etag = synapseRow != null ? synapseRow.getEtag() : null;
                    csvRow.add(etag == null ? "" : etag);
                }

                rowView.getCells().toList().stream()
                        .map(v -> v == null ? "" : v.toString())
                        .forEach(csvRow::add);

                writer.writeNext(csvRow.toArray(new String[0]));
            }
        } catch (IOException e) {
            throw new RuntimeException("Error writing to CSV stream", e);
        }
    }
}
