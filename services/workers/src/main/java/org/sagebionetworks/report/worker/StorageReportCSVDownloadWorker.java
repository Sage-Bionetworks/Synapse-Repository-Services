package org.sagebionetworks.report.worker;

import java.io.File;
import java.io.FileWriter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobUtils;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.LocalFileUploadRequest;
import org.sagebionetworks.repo.manager.report.StorageReportManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.dbo.dao.table.TableExceptionTranslator;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.report.DownloadStorageReportRequest;
import org.sagebionetworks.repo.model.report.DownloadStorageReportResponse;
import org.sagebionetworks.table.worker.ProgressingCSVWriterStream;
import org.sagebionetworks.table.worker.UploadProgressListener;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * This worker will generate a Synapse storage report saved in a local CSV file, and will upload the file
 * to S3 as a FileHandle.
 *
 */
public class StorageReportCSVDownloadWorker implements MessageDrivenRunner {

	static private Logger log = LogManager.getLogger(StorageReportCSVDownloadWorker.class);

	@Autowired
	private AsynchJobStatusManager asynchJobStatusManager;
	@Autowired
	private StorageReportManager storageReportManager;
	@Autowired
	private UserManager userManager;
	@Autowired
	private FileHandleManager fileHandleManager;
	@Autowired
	private Clock clock;
	@Autowired
	private TableExceptionTranslator tableExceptionTranslator;

	@Override
	public void run(ProgressCallback progressCallback, Message message) throws Exception {
		AsynchronousJobStatus status = asynchJobStatusManager.lookupJobStatus(message.getBody());
		String fileName = "Job-"+status.getJobId();
		File temp = null;
		try {
			UserInfo user = userManager.getUserInfo(status.getStartedByUserId());
			DownloadStorageReportRequest request = AsynchJobUtils.extractRequestBody(status, DownloadStorageReportRequest.class);
			// The CSV data will first be written to this file.
			temp = File.createTempFile(fileName, ".csv");
			// Execute the actual query and stream the results to the file.
			try (CSVWriter writer = new CSVWriter(new FileWriter(temp))){
				// this object will update the progress of both the job and refresh the timeout on the message as rows are read from the DB.
				ProgressingCSVWriterStream stream = new ProgressingCSVWriterStream(writer, progressCallback, message, asynchJobStatusManager, 0L, 0L, status.getJobId(), clock);
				storageReportManager.writeStorageReport(user, request, stream);
			}

			// At this point we have the entire CSV written to a local file.
			// Upload the file to S3 can create the filehandle.
			UploadProgressListener uploadListener = new UploadProgressListener(progressCallback, message, 0L, 0L, 0L, asynchJobStatusManager, status.getJobId());
			String contentType = "text/csv";
			S3FileHandle fileHandle = fileHandleManager.uploadLocalFile(new LocalFileUploadRequest().withUserId(user.getId().toString()).withFileToUpload(temp).withContentType(contentType).withListener(uploadListener));

			DownloadStorageReportResponse result = new DownloadStorageReportResponse();
			result.setResultsFileHandleId(fileHandle.getId());
			asynchJobStatusManager.setComplete(status.getJobId(), result);
		} catch(Throwable e){
			// Attempt to translate the exception into a 'user-friendly' message.
			RuntimeException translatedException = tableExceptionTranslator.translateException(e);
			// The job failed
			asynchJobStatusManager.setJobFailed(status.getJobId(), translatedException);
			log.error("Worker Failed", e);
		} finally {
			if (temp != null) {
				temp.delete();
			}
		}
	}
}
