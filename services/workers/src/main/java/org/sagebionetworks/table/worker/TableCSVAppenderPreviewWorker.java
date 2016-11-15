package org.sagebionetworks.table.worker;

import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.io.input.CountingInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ThrottlingProgressCallback;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobUtils;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewRequest;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewResult;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.table.cluster.utils.CSVUtils;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sqs.model.Message;

import au.com.bytecode.opencsv.CSVReader;
/**
 * This worker reads CSV files from S3 and appends the data to a given TableEntity.
 * 
 * @author jmhill
 *
 */
public class TableCSVAppenderPreviewWorker implements MessageDrivenRunner {

	static private Logger log = LogManager.getLogger(TableCSVAppenderPreviewWorker.class);

	@Autowired
	private AsynchJobStatusManager asynchJobStatusManager;
	@Autowired
	private TableEntityManager tableEntityManager;
	@Autowired
	private UserManager userManger;
	@Autowired
	private FileHandleManager fileHandleManager;
	@Autowired
	private AmazonS3Client s3Client;


	@Override
	public void run(ProgressCallback<Void> progressCallback, Message message)
			throws RecoverableMessageException, Exception {
		// We should only get one message
		try{
			processStatus(progressCallback, message);
		}catch(Throwable e){
			// Treat unknown errors as unrecoverable and return them
			log.error("Worker Failed", e);
		}
	}
	

	/**
	 * @param status
	 * @throws Throwable 
	 */
	public void processStatus(final ProgressCallback<Void> progressCallback, final Message message) throws Throwable {
		final AsynchronousJobStatus status = asynchJobStatusManager.lookupJobStatus(message.getBody());
		CSVReader reader = null;
		try{
			UserInfo user = userManger.getUserInfo(status.getStartedByUserId());
			UploadToTablePreviewRequest body = AsynchJobUtils.extractRequestBody(status, UploadToTablePreviewRequest.class);
			// Get the filehandle
			S3FileHandle fileHandle = (S3FileHandle) fileHandleManager.getRawFileHandle(user, body.getUploadFileHandleId());
			// Get the metadat for this file
			ObjectMetadata fileMetadata = s3Client.getObjectMetadata(fileHandle.getBucketName(), fileHandle.getKey());
			long progressCurrent = 0L;
			final long progressTotal = fileMetadata.getContentLength();
			// Start the progress
			asynchJobStatusManager.updateJobProgress(status.getJobId(), progressCurrent, progressTotal, "Starting...");
			// Open a stream to the file in S3.
			S3Object s3Object = s3Client.getObject(fileHandle.getBucketName(), fileHandle.getKey());
			// This stream is used to keep track of the bytes read.
			final CountingInputStream countingInputStream = new CountingInputStream(s3Object.getObjectContent());
			// Create a reader from the passed parameters
			reader = CSVUtils.createCSVReader(new InputStreamReader(countingInputStream, "UTF-8"), body.getCsvTableDescriptor(), body.getLinesToSkip());
			
			// Report progress every 2 seconds.
			long progressIntervalMs = 2000;
			ThrottlingProgressCallback<Integer> throttledProgressCallback = new ThrottlingProgressCallback<Integer>(new ProgressCallback<Integer>() {
				@Override
				public void progressMade(Integer rowNumber) {
					// update the job progress.
					asynchJobStatusManager.updateJobProgress(status.getJobId(),
							countingInputStream.getByteCount(), progressTotal,
							"Processed: " + rowNumber + " rows");
					// update the message.
					progressCallback.progressMade(null);
				}
			}, progressIntervalMs);
			// This builder does the work of building an actual preview.
			UploadPreviewBuilder builder = new UploadPreviewBuilder(reader, throttledProgressCallback, body);
			UploadToTablePreviewResult result = builder.buildResult();
			asynchJobStatusManager.setComplete(status.getJobId(), result);
		}catch(Throwable e){
			// Record the error
			asynchJobStatusManager.setJobFailed(status.getJobId(), e);
			throw e;
		}finally{
			if(reader != null){
				try {
					// Unconditionally close the stream to the S3 file.
					reader.close();
				} catch (IOException e) {}
			}
		}
	}	
}
