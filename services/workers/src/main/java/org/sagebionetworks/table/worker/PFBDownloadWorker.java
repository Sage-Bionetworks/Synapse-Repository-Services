package org.sagebionetworks.table.worker;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.avro.pfb.model.Metadata;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.LocalFileUploadRequest;
import org.sagebionetworks.repo.manager.table.TableQueryManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.dbo.dao.table.TableExceptionTranslator;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.DownloadPFBRequest;
import org.sagebionetworks.repo.model.table.DownloadPFBResult;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.table.cluster.avro.RowPFBWriterProvider;
import org.sagebionetworks.util.FileProvider;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.worker.AsyncJobRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PFBDownloadWorker implements AsyncJobRunner<DownloadPFBRequest, DownloadPFBResult> {

	static private Logger log = LogManager.getLogger(PFBDownloadWorker.class);

	private final FileProvider fileProvider;
	private final TableQueryManager tableQueryManager;
	private final FileHandleManager fileHandleManager;
	private final RowPFBWriterProvider writerProvider;
	private final TableExceptionTranslator tableExceptionTranslator;

	@Autowired
	public PFBDownloadWorker(FileProvider fileProvider, TableQueryManager tableQueryManager,
			FileHandleManager fileHandleManager, RowPFBWriterProvider writerProvider,
			TableExceptionTranslator tableExceptionTranslator) {
		super();
		this.fileProvider = fileProvider;
		this.tableQueryManager = tableQueryManager;
		this.fileHandleManager = fileHandleManager;
		this.writerProvider = writerProvider;
		this.tableExceptionTranslator = tableExceptionTranslator;
	}

	@Override
	public Class<DownloadPFBRequest> getRequestType() {
		return DownloadPFBRequest.class;
	}

	@Override
	public Class<DownloadPFBResult> getResponseType() {
		return DownloadPFBResult.class;
	}

	@Override
	public DownloadPFBResult run(String jobId, UserInfo user, DownloadPFBRequest request,
			AsyncJobProgressCallback jobProgressCallback) throws RecoverableMessageException, Exception {
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getPfbEntityName(), "request.pfbEntityName");
		String jobName = "Job-" + jobId;
		String fileName = request.getFileName() != null ? request.getFileName() : jobName + ".avro";
		File temp = fileProvider.createTempFile(jobName, ".avro");
		
		try {
			jobProgressCallback.updateProgress("running query...", 0L, 100L);
			// Add a blank metadata row for now.
			Metadata metadata = new Metadata().setNodes(Collections.emptyList());
			QueryResultBundle qrb = tableQueryManager.runQueryAsStream(jobProgressCallback, user, request, t -> {
				// Note that the schema of the select does not include the column model id
				List<ColumnModel> schema = t.getMainQuery().getTranslator().getSchemaOfSelect();
				return writerProvider.createWriter(request.getPfbEntityName(), schema, request.getPfbEntityIdColumnNames(), metadata, temp);
			});
			jobProgressCallback.updateProgress("saving results...", 0L, 100L);
			S3FileHandle fileHandle = fileHandleManager
					.uploadLocalFile(new LocalFileUploadRequest().withUserId(user.getId().toString())
							.withFileToUpload(temp).withContentType("application/octet-stream").withFileName(fileName));

			return new DownloadPFBResult().setTableId(qrb.getQueryResult().getQueryResults().getTableId())
					.setResultsFileHandleId(fileHandle.getId());
		} catch (TableUnavailableException | LockUnavilableException e) {
			jobProgressCallback.updateProgress("Waiting for the table/view to become available...", 0L, 100L);
			throw new RecoverableMessageException();
		} catch (TableFailedException | RecoverableMessageException e) {
			throw e;
		} catch (Throwable e) {
			log.error("Worker Failed", e);
			throw tableExceptionTranslator.translateException(e);
		} finally {
			temp.delete();
		}
	}
}
