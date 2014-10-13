package org.sagebionetworks.table.worker;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.input.CountingInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.asynchronous.workers.sqs.Worker;
import org.sagebionetworks.asynchronous.workers.sqs.WorkerProgress;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.table.TableRowManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.dbo.dao.table.CSVToRowIterator;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;
import org.sagebionetworks.repo.model.table.UploadToTableResult;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.util.ProgressCallback;
import org.sagebionetworks.util.csv.CsvNullReader;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sqs.model.Message;
/**
 * This worker reads CSV files from S3 and appends the data to a given TableEntity.
 * 
 * @author jmhill
 *
 */
public class TableCSVAppenderWorker implements Worker {

	static private Logger log = LogManager.getLogger(TableCSVAppenderWorker.class);
	private List<Message> messages;
	private WorkerProgress workerProgress;

	@Autowired
	private AsynchJobStatusManager asynchJobStatusManager;
	@Autowired
	private TableRowManager tableRowManager;
	@Autowired
	private UserManager userManger;
	@Autowired
	private FileHandleManager fileHandleManager;
	@Autowired
	private AmazonS3Client s3Client;

	@Override
	public void setMessages(List<Message> messages) {
		this.messages = messages;
	}

	@Override
	public void setWorkerProgress(WorkerProgress workerProgress) {
		this.workerProgress = workerProgress;
	}

	@Override
	public List<Message> call() throws Exception {
		// We should only get one message
		List<Message> toDelete = new LinkedList<Message>();
		for(Message message: messages){
			try{
				toDelete.add(processMessage(message));
			}catch(Throwable e){
				// Treat unknown errors as unrecoverable and return them
				toDelete.add(message);
				log.error("Worker Failed", e);
			}
		}
		return toDelete;
	}
	
	/**
	 * Process a single message
	 * @param message
	 * @return
	 * @throws Throwable 
	 */
	public Message processMessage(Message message) throws Throwable{
		// First read the body
		AsynchronousJobStatus status = extractStatus(message);
		processStatus(message, status);
		return message;
	}

	/**
	 * @param status
	 * @throws Throwable 
	 */
	public void processStatus(final Message message, AsynchronousJobStatus status) throws Throwable {
		CsvNullReader reader = null;
		try{
			UserInfo user = userManger.getUserInfo(status.getStartedByUserId());
			UploadToTableRequest body = (UploadToTableRequest) status.getRequestBody();
			// Get the filehandle
			S3FileHandle fileHandle = (S3FileHandle) fileHandleManager.getRawFileHandle(user, body.getUploadFileHandleId());
			// Get the schema for the table
			List<ColumnModel> tableSchema = tableRowManager.getColumnModelsForTable(body.getTableId());
			// Get the metadat for this file
			ObjectMetadata fileMetadata = s3Client.getObjectMetadata(fileHandle.getBucketName(), fileHandle.getKey());
			long progressCurrent = 0L;
			long progressTotal = fileMetadata.getContentLength();
			// Start the progress
			asynchJobStatusManager.updateJobProgress(status.getJobId(), progressCurrent, progressTotal, "Starting...");
			// Open a stream to the file in S3.
			S3Object s3Object = s3Client.getObject(fileHandle.getBucketName(), fileHandle.getKey());
			// This stream is used to keep track of the bytes read.
			CountingInputStream countingInputStream = new CountingInputStream(s3Object.getObjectContent());
			// Create a reader from the passed parameters
			reader = CSVUtils.createCSVReader(new InputStreamReader(countingInputStream, "UTF-8"), body.getCsvTableDescriptor(), body.getLinesToSkip());
			// Reports progress back the caller.
			// Report progress every 2 seconds.
			long progressIntervalMs = 2000;
			ProgressReporter progressReporter = new IntervalProgressReporter(status.getJobId(),fileMetadata.getContentLength(), countingInputStream, asynchJobStatusManager, progressIntervalMs);
			// Create the iterator
			boolean isFirstLineHeader = CSVUtils.isFirstRowHeader(body.getCsvTableDescriptor());
			CSVToRowIterator iterator = new CSVToRowIterator(tableSchema, reader, isFirstLineHeader);
			ProgressingIteratorProxy iteratorProxy = new  ProgressingIteratorProxy(iterator, progressReporter);
			// Append the data to the table
			String etag = tableRowManager.appendRowsAsStream(user, body.getTableId(), tableSchema, iteratorProxy, body.getUpdateEtag(), null, new ProgressCallback<Long>(){

				@Override
				public void progressMade(Long count) {
					if(workerProgress != null){
						workerProgress.progressMadeForMessage(message);
					}
				}});
			// Done
			UploadToTableResult result = new UploadToTableResult();
			result.setRowsProcessed(Long.valueOf(progressReporter.getRowNumber() + 1));
			result.setEtag(etag);
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
	

	/**
	 * Extract the AsynchUploadRequestBody from the message.
	 * @param message
	 * @return
	 * @throws JSONObjectAdapterException
	 */
	AsynchronousJobStatus extractStatus(Message message) throws JSONObjectAdapterException{
		if(message == null){
			throw new IllegalArgumentException("Message cannot be null");
		}
		AsynchronousJobStatus status = MessageUtils.readMessageBody(message, AsynchronousJobStatus.class);
		if(status.getRequestBody() == null){
			throw new IllegalArgumentException("Job body cannot be null");
		}
		if (!(status.getRequestBody() instanceof UploadToTableRequest)) {
			throw new IllegalArgumentException("Expected a job body of type: " + UploadToTableRequest.class.getName() + " but received: "
					+ status.getRequestBody().getClass().getName());
		}
		return status;
	}
	
}
