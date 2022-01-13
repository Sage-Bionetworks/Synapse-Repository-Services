package org.sagebionetworks.report.worker;

import java.io.File;
import java.io.FileWriter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.LocalFileUploadRequest;
import org.sagebionetworks.repo.manager.report.StorageReportManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.table.TableExceptionTranslator;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.report.DownloadStorageReportRequest;
import org.sagebionetworks.repo.model.report.DownloadStorageReportResponse;
import org.sagebionetworks.table.worker.ProgressingCSVWriterStream;
import org.sagebionetworks.table.worker.UploadProgressListener;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.worker.AsyncJobProgressCallback;
import org.sagebionetworks.worker.AsyncJobRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * This worker will generate a Synapse storage report saved in a local CSV file, and will upload the
 * file to S3 as a FileHandle.
 *
 */
public class StorageReportCSVDownloadWorker implements AsyncJobRunner<DownloadStorageReportRequest, DownloadStorageReportResponse> {

	static private Logger log = LogManager.getLogger(StorageReportCSVDownloadWorker.class);

	@Autowired
	private StorageReportManager storageReportManager;
	@Autowired
	private FileHandleManager fileHandleManager;
	@Autowired
	private Clock clock;
	@Autowired
	private TableExceptionTranslator tableExceptionTranslator;

	@Override
	public Class<DownloadStorageReportRequest> getRequestType() {
		return DownloadStorageReportRequest.class;
	}

	@Override
	public Class<DownloadStorageReportResponse> getResponseType() {
		return DownloadStorageReportResponse.class;
	}

	@Override
	public DownloadStorageReportResponse run(ProgressCallback progressCallback, String jobId, UserInfo user,
			DownloadStorageReportRequest request, AsyncJobProgressCallback jobProgressCallback)
			throws RecoverableMessageException, Exception {
		String fileName = "Job-" + jobId;
		File temp = null;
		try {
			// The CSV data will first be written to this file.
			temp = File.createTempFile(fileName, ".csv");
			// Execute the actual query and stream the results to the file.
			try (CSVWriter writer = new CSVWriter(new FileWriter(temp))) {
				// this object will update the progress of both the job and refresh the timeout on the message as
				// rows are read from the DB.
				ProgressingCSVWriterStream stream = new ProgressingCSVWriterStream(writer, jobProgressCallback, 0L, 0L, clock);
				storageReportManager.writeStorageReport(user, request, stream);
			}

			// At this point we have the entire CSV written to a local file.
			// Upload the file to S3 can create the filehandle.
			UploadProgressListener uploadListener = new UploadProgressListener(jobProgressCallback, 0L, 0L, 0L);
			String contentType = "text/csv";

			S3FileHandle fileHandle = fileHandleManager.uploadLocalFile(new LocalFileUploadRequest()
				.withUserId(user.getId().toString())
				.withFileToUpload(temp)
				.withContentType(contentType)
				.withListener(uploadListener)
			);
			
			DownloadStorageReportResponse response = new DownloadStorageReportResponse()
				.setResultsFileHandleId(fileHandle.getId());
			
			return response;
		} catch (Throwable e) {
			log.error("Worker Failed", e);
			// Attempt to translate the exception into a 'user-friendly' message.
			RuntimeException translatedException = tableExceptionTranslator.translateException(e);
			// The job failed
			throw translatedException;
		} finally {
			if (temp != null) {
				temp.delete();
			}
		}
	}
}
