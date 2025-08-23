package org.sagebionetworks.repo.manager.grid.internal.replica.export;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.LocalFileUploadRequest;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.GridReplicaPatchBuilderManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowData;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowMetadata;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowObject;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.SynapseRow;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.GridReplicaViewManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.grid.DownloadFromGridRequest;
import org.sagebionetworks.repo.model.grid.DownloadFromGridResult;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.csv.CSVWriterProvider;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import au.com.bytecode.opencsv.CSVWriter;

@ExtendWith(MockitoExtension.class)
public class GridReplicaCsvExporterImplTest {
    @Mock
    private GridManager mockGridManager;
    @Mock
    private GridReplicaPatchBuilderManager mockReplicaPatchBuilderManager;
    @Mock
    private GridReplicaViewManager mockGridReplicaViewManager;
    @Mock
    private CSVWriterProvider mockCsvWriterProvider;
    @Mock
    private FileHandleManager mockFileHandleManager;

    @InjectMocks
    private GridReplicaCsvExporterImpl exporter;

    @Mock
    private GridSession mockGridSession;
    @Mock
    private GridConnectionInfo mockGridConnectionInfo;
    @Mock
    private GridHeader mockGridHeader;
    @Mock
    private CSVWriter mockCsvWriter;
    @Mock
    private AsyncJobProgressCallback mockJobProgressCallback;
    @Mock
    Iterator<RowView> mockRowViewIterator;
    @Captor
    private ArgumentCaptor<LocalFileUploadRequest> fileUploadCaptor;

    private DownloadFromGridRequest request;
    private List<RowView> rowViews;
    private String jobId = "1234";
    private String sessionId = "fakeGridSessionId";
    private Long replicaId = 999L;
    private String fileHandleId = "8888";
    private Long userId = 987L;
    private UserInfo userInfo;

    @BeforeEach
    public void before() {
        userInfo = new UserInfo(false);
        userInfo.setId(userId);

        request = new DownloadFromGridRequest();
        request.setSessionId(sessionId);

        rowViews = new ArrayList<>();
        rowViews.add(new RowView().setRowObject(new RowObject()
                .setMetadata(new RowMetadata().setSynapseRow(new SynapseRow().setRowId(1L).setVersionNumber(2L).setEtag("etag1")))
                .setData(new RowData().setCells(new JSONArray(List.of("a", "b"))))));
        rowViews.add(new RowView().setRowObject(new RowObject()
                .setMetadata(new RowMetadata().setSynapseRow(new SynapseRow().setRowId(3L).setVersionNumber(4L).setEtag("etag2")))
                .setData(new RowData().setCells(new JSONArray(List.of("c", "d"))))));
    }

    @Test
    public void testExportGridAsCsv() throws IOException {
        when(mockGridManager.getGridSession(userInfo, sessionId)).thenReturn(mockGridSession);
        when(mockGridManager.getDefaultInternalConnection(sessionId)).thenReturn(Optional.of(mockGridConnectionInfo));
        when(mockGridConnectionInfo.getSessionId()).thenReturn(sessionId);
        when(mockGridConnectionInfo.getReplicaId()).thenReturn(replicaId);
        when(mockReplicaPatchBuilderManager.getCurrentClockIfAllPatchesApplied(sessionId, replicaId)).thenReturn(Optional.of(new LogicalTimestamp()));
        when(mockGridReplicaViewManager.readHeader(sessionId, replicaId)).thenReturn(Optional.of(mockGridHeader));
        when(mockGridHeader.getOrderedColumns()).thenReturn(List.of(
                new Column().setName("col1"),
                new Column().setName("col2")
        ));
        when(mockGridReplicaViewManager.getQueryIterator(eq(mockGridHeader), any())).thenReturn(mockRowViewIterator);
        when(mockRowViewIterator.hasNext()).thenReturn(true, true, false);
        when(mockRowViewIterator.next()).thenReturn(rowViews.get(0), rowViews.get(1));
        when(mockCsvWriterProvider.createWriter(any(), any())).thenReturn(mockCsvWriter);
        when(mockFileHandleManager.uploadLocalFile(any())).thenReturn(new S3FileHandle().setId(fileHandleId));

        // Call under test
        DownloadFromGridResult result = exporter.exportGridAsCsv(jobId, userInfo, request, mockJobProgressCallback);

        assertEquals(request.getSessionId(), result.getSessionId());
        assertEquals(fileHandleId, result.getResultsFileHandleId());
        ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
        verify(mockCsvWriter, times(3)).writeNext(captor.capture());
        List<String[]> writtenRows = captor.getAllValues();
        assertArrayEquals(
                new String[]{
                        "ROW_ID", "ROW_VERSION", "etag", "col1", "col2",
                        "1", "2", "etag1", "a", "b",
                        "3", "4", "etag2", "c", "d"
                },
                writtenRows.toArray()
        );
        verify(mockGridManager).getGridSession(userInfo, sessionId);
        verify(mockGridManager).getDefaultInternalConnection(sessionId);
        verify(mockGridReplicaViewManager).readHeader(sessionId, replicaId);
        verifyFileUpload();
    }

    @Test
    public void testExportGridAsCsvWithNoHeader() throws IOException {
        request.setWriteHeader(false);

        when(mockGridManager.getGridSession(userInfo, sessionId)).thenReturn(mockGridSession);
        when(mockGridManager.getDefaultInternalConnection(sessionId)).thenReturn(Optional.of(mockGridConnectionInfo));
        when(mockGridConnectionInfo.getSessionId()).thenReturn(sessionId);
        when(mockGridConnectionInfo.getReplicaId()).thenReturn(replicaId);
        when(mockReplicaPatchBuilderManager.getCurrentClockIfAllPatchesApplied(sessionId, replicaId)).thenReturn(Optional.of(new LogicalTimestamp()));
        when(mockGridReplicaViewManager.readHeader(sessionId, replicaId)).thenReturn(Optional.of(mockGridHeader));
        when(mockGridReplicaViewManager.getQueryIterator(eq(mockGridHeader), any())).thenReturn(mockRowViewIterator);
        when(mockRowViewIterator.hasNext()).thenReturn(true, true, false);
        when(mockRowViewIterator.next()).thenReturn(rowViews.get(0), rowViews.get(1));
        when(mockCsvWriterProvider.createWriter(any(), any())).thenReturn(mockCsvWriter);
        when(mockFileHandleManager.uploadLocalFile(any())).thenReturn(new S3FileHandle().setId(fileHandleId));

        // Call under test
        exporter.exportGridAsCsv(jobId, userInfo, request, mockJobProgressCallback);

        ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
        verify(mockCsvWriter, times(2)).writeNext(captor.capture());
        List<String[]> writtenRows = captor.getAllValues();
        assertArrayEquals(
                new String[]{
                        "1", "2", "etag1", "a", "b",
                        "3", "4", "etag2", "c", "d"
                },
                writtenRows.toArray()
        );
        verifyFileUpload();
    }

    @Test
    public void testExportGridAsCsvWithoutRowIdAndVersion() throws IOException {
        request.setIncludeRowIdAndRowVersion(false);

        when(mockGridManager.getGridSession(userInfo, sessionId)).thenReturn(mockGridSession);
        when(mockGridManager.getDefaultInternalConnection(sessionId)).thenReturn(Optional.of(mockGridConnectionInfo));
        when(mockGridConnectionInfo.getSessionId()).thenReturn(sessionId);
        when(mockGridConnectionInfo.getReplicaId()).thenReturn(replicaId);
        when(mockReplicaPatchBuilderManager.getCurrentClockIfAllPatchesApplied(sessionId, replicaId)).thenReturn(Optional.of(new LogicalTimestamp()));
        when(mockGridReplicaViewManager.readHeader(sessionId, replicaId)).thenReturn(Optional.of(mockGridHeader));
        when(mockGridHeader.getOrderedColumns()).thenReturn(List.of(
                new Column().setName("col1"),
                new Column().setName("col2")
        ));
        when(mockGridReplicaViewManager.getQueryIterator(eq(mockGridHeader), any())).thenReturn(mockRowViewIterator);
        when(mockRowViewIterator.hasNext()).thenReturn(true, true, false);
        when(mockRowViewIterator.next()).thenReturn(rowViews.get(0), rowViews.get(1));
        when(mockCsvWriterProvider.createWriter(any(), any())).thenReturn(mockCsvWriter);
        when(mockFileHandleManager.uploadLocalFile(any())).thenReturn(new S3FileHandle().setId(fileHandleId));

        // Call under test
        exporter.exportGridAsCsv(jobId, userInfo, request, mockJobProgressCallback);

        ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
        verify(mockCsvWriter, times(3)).writeNext(captor.capture());
        List<String[]> writtenRows = captor.getAllValues();
        assertArrayEquals(
                new String[]{
                        "etag", "col1", "col2",
                        "etag1", "a", "b",
                        "etag2", "c", "d"
                },
                writtenRows.toArray()
        );
        verifyFileUpload();
    }

    @Test
    public void testExportGridAsCsvWithoutEtag() throws IOException {
        request.setIncludeEtag(false);

        when(mockGridManager.getGridSession(userInfo, sessionId)).thenReturn(mockGridSession);
        when(mockGridManager.getDefaultInternalConnection(sessionId)).thenReturn(Optional.of(mockGridConnectionInfo));
        when(mockGridConnectionInfo.getSessionId()).thenReturn(sessionId);
        when(mockGridConnectionInfo.getReplicaId()).thenReturn(replicaId);
        when(mockReplicaPatchBuilderManager.getCurrentClockIfAllPatchesApplied(sessionId, replicaId)).thenReturn(Optional.of(new LogicalTimestamp()));
        when(mockGridReplicaViewManager.readHeader(sessionId, replicaId)).thenReturn(Optional.of(mockGridHeader));
        when(mockGridHeader.getOrderedColumns()).thenReturn(List.of(
                new Column().setName("col1"),
                new Column().setName("col2")
        ));
        when(mockGridReplicaViewManager.getQueryIterator(eq(mockGridHeader), any())).thenReturn(mockRowViewIterator);
        when(mockRowViewIterator.hasNext()).thenReturn(true, true, false);
        when(mockRowViewIterator.next()).thenReturn(rowViews.get(0), rowViews.get(1));
        when(mockCsvWriterProvider.createWriter(any(), any())).thenReturn(mockCsvWriter);
        when(mockFileHandleManager.uploadLocalFile(any())).thenReturn(new S3FileHandle().setId(fileHandleId));

        // Call under test
        exporter.exportGridAsCsv(jobId, userInfo, request, mockJobProgressCallback);

        ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
        verify(mockCsvWriter, times(3)).writeNext(captor.capture());
        List<String[]> writtenRows = captor.getAllValues();
        assertArrayEquals(
                new String[]{
                        "ROW_ID", "ROW_VERSION", "col1", "col2",
                        "1", "2", "a", "b",
                        "3", "4", "c", "d"
                },
                writtenRows.toArray()
        );
        verifyFileUpload();
    }

    @Test
    public void testExportGridAsCsvWithNullValues() throws IOException {
        rowViews.get(0).getRowObject().getMetadata().getSynapseRow().setRowId(null).setVersionNumber(null).setEtag(null);
        rowViews.get(0).getRowObject().getData().setCells(new JSONArray("[\"a\",null]"));

        when(mockGridManager.getGridSession(userInfo, sessionId)).thenReturn(mockGridSession);
        when(mockGridManager.getDefaultInternalConnection(sessionId)).thenReturn(Optional.of(mockGridConnectionInfo));
        when(mockGridConnectionInfo.getSessionId()).thenReturn(sessionId);
        when(mockGridConnectionInfo.getReplicaId()).thenReturn(replicaId);
        when(mockReplicaPatchBuilderManager.getCurrentClockIfAllPatchesApplied(sessionId, replicaId)).thenReturn(Optional.of(new LogicalTimestamp()));
        when(mockGridReplicaViewManager.readHeader(sessionId, replicaId)).thenReturn(Optional.of(mockGridHeader));
        when(mockGridHeader.getOrderedColumns()).thenReturn(List.of(
                new Column().setName("col1"),
                new Column().setName("col2")
        ));
        when(mockGridReplicaViewManager.getQueryIterator(eq(mockGridHeader), any())).thenReturn(mockRowViewIterator);
        when(mockRowViewIterator.hasNext()).thenReturn(true, true, false);
        when(mockRowViewIterator.next()).thenReturn(rowViews.get(0), rowViews.get(1));
        when(mockCsvWriterProvider.createWriter(any(), any())).thenReturn(mockCsvWriter);
        when(mockFileHandleManager.uploadLocalFile(any())).thenReturn(new S3FileHandle().setId(fileHandleId));

        // Call under test
        exporter.exportGridAsCsv(jobId, userInfo, request, mockJobProgressCallback);

        ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
        verify(mockCsvWriter, times(3)).writeNext(captor.capture());
        List<String[]> writtenRows = captor.getAllValues();
        assertArrayEquals(
                new String[]{
                        "ROW_ID", "ROW_VERSION", "etag", "col1", "col2",
                        "", "", "", "a", "",
                        "3", "4", "etag2", "c", "d"
                },
                writtenRows.toArray()
        );
        verifyFileUpload();
    }

    @Test
    public void testExportGridAsCsvWithNoConnection() {
        when(mockGridManager.getGridSession(userInfo, sessionId)).thenReturn(mockGridSession);
        when(mockGridManager.getDefaultInternalConnection(sessionId)).thenReturn(Optional.empty());

        assertThrows(RecoverableMessageException.class, () -> {
            exporter.exportGridAsCsv(jobId, userInfo, request, mockJobProgressCallback);
        }, "No internal connection found for session: " + sessionId);
        verifyNoFileUpload();
    }

    @Test
    public void testExportGridAsCsvClockNotReady() {
        when(mockGridManager.getGridSession(userInfo, sessionId)).thenReturn(mockGridSession);
        when(mockGridManager.getDefaultInternalConnection(sessionId)).thenReturn(Optional.of(mockGridConnectionInfo));
        when(mockGridConnectionInfo.getSessionId()).thenReturn(sessionId);
        when(mockGridConnectionInfo.getReplicaId()).thenReturn(replicaId);
        when(mockReplicaPatchBuilderManager.getCurrentClockIfAllPatchesApplied(sessionId, replicaId)).thenReturn(Optional.empty());

        assertThrows(RecoverableMessageException.class, () -> {
            exporter.exportGridAsCsv(jobId, userInfo, request, mockJobProgressCallback);
        }, "Current clock could not be retrieved, patches are still being applied to sessionId: " + sessionId + ", replicaId: " + replicaId);
        verifyNoFileUpload();
    }

    @Test
    public void testExportGridAsCsvWithNoHeaderFound() {
        when(mockGridManager.getGridSession(userInfo, sessionId)).thenReturn(mockGridSession);
        when(mockGridManager.getDefaultInternalConnection(sessionId)).thenReturn(Optional.of(mockGridConnectionInfo));
        when(mockGridConnectionInfo.getSessionId()).thenReturn(sessionId);
        when(mockGridConnectionInfo.getReplicaId()).thenReturn(replicaId);
        when(mockReplicaPatchBuilderManager.getCurrentClockIfAllPatchesApplied(sessionId, replicaId)).thenReturn(Optional.of(new LogicalTimestamp()));
        when(mockGridReplicaViewManager.readHeader(sessionId, replicaId)).thenReturn(Optional.empty());

        assertThrows(RecoverableMessageException.class, () -> {
            exporter.exportGridAsCsv(jobId, userInfo, request, mockJobProgressCallback);
        }, "Grid header has not yet been instantiated for sessionId: " + sessionId);
        verifyNoFileUpload();
    }

    private void verifyFileUpload() {
        verify(mockFileHandleManager).uploadLocalFile(fileUploadCaptor.capture());
        LocalFileUploadRequest fileUploadRequest = fileUploadCaptor.getValue();
        assertNotNull(fileUploadRequest);
        assertEquals(userInfo.getId().toString(), fileUploadRequest.getUserId());
        assertEquals("text/csv", fileUploadRequest.getContentType());
        assertNull(fileUploadRequest.getFileName());
    }

    private void verifyNoFileUpload() {
        verify(mockFileHandleManager, never()).uploadLocalFile(any());
    }


}
