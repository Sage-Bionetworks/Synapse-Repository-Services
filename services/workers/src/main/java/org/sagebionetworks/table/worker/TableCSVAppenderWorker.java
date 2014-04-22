package org.sagebionetworks.table.worker;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.io.input.CountingInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.bridge.model.dbo.dao.CsvNullReader;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.table.TableRowManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.AsynchUploadJobBody;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

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
public class TableCSVAppenderWorker implements Callable<List<Message>> {

	static private Logger log = LogManager.getLogger(TableCSVAppenderWorker.class);

	private AsynchJobStatusManager asynchJobStatusManager;
	private TableRowManager tableRowManager;
	private FileHandleManager fileHandleManger;
	private UserManager userManger;
	private AmazonS3Client s3Client;
	private List<Message> messages;

	public TableCSVAppenderWorker(
			AsynchJobStatusManager asynchJobStatusManager,
			TableRowManager tableRowManager,
			FileHandleManager fileHandleManger, UserManager userManger,
			AmazonS3Client s3Client, List<Message> messages) {
		super();
		this.asynchJobStatusManager = asynchJobStatusManager;
		this.tableRowManager = tableRowManager;
		this.fileHandleManger = fileHandleManger;
		this.userManger = userManger;
		this.s3Client = s3Client;
		this.messages = messages;
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
		processStatus(status);
		return message;
	}

	/**
	 * @param status
	 * @throws Throwable 
	 */
	public void processStatus(AsynchronousJobStatus status) throws Throwable {
		CsvNullReader reader = null;
		try{
			UserInfo user = userManger.getUserInfo(status.getStartedByUserId());
			AsynchUploadJobBody body = (AsynchUploadJobBody) status.getJobBody();
			// Get the filehandle
			S3FileHandle fileHandle = (S3FileHandle) fileHandleManger.getRawFileHandle(user, body.getUploadFileHandleId());
			// Get the schema for the table
			List<ColumnModel> tableSchema = tableRowManager.getColumnModelsForTable(body.getTableId());
			// Get the metadat for this file
			ObjectMetadata fileMetadata = s3Client.getObjectMetadata(fileHandle.getBucketName(), fileHandle.getKey());
			long progressCurrent = 0L;
			long progressTotal = fileMetadata.getContentLength();
			// Start the progress
			asynchJobStatusManager.updateJobProgress(status.getJobId(), progressCurrent, progressTotal, "Starting...");
			// Start reading
			S3Object s3Object = s3Client.getObject(fileHandle.getBucketName(), fileHandle.getKey());
			// This stream is used to keep track of the bytes read.
			CountingInputStream countingInputStream = new CountingInputStream(s3Object.getObjectContent());
			reader = new CsvNullReader(new InputStreamReader(countingInputStream, "UTF-8"));
			// Reports progress back the caller.
			ProgressReporter progressReporter = new ProgressReporterImpl(status.getJobId(),fileMetadata.getContentLength(),countingInputStream, asynchJobStatusManager);
			// Create the iterator
			CSVRowIterator iterator = new CSVRowIterator(tableSchema, reader, 2000, progressReporter);
			// Append the data to the table
			tableRowManager.appendRowsAsStream(user, body.getTableId(), tableSchema, iterator, null, null);
			// Done
			asynchJobStatusManager.setComplete(status.getJobId(), body);
		}catch(Throwable e){
			// Record the error
			asynchJobStatusManager.setJobFailed(status.getJobId(), e);
			throw e;
		}finally{
			if(reader != null){
				try {
					// Unconditionally close the stream
					reader.close();
				} catch (IOException e) {}
			}
		}
	}
	

	/**
	 * Extract the AsynchUploadJobBody from the message.
	 * @param message
	 * @return
	 * @throws JSONObjectAdapterException
	 */
	AsynchronousJobStatus extractStatus(Message message) throws JSONObjectAdapterException{
		if(message == null){
			throw new IllegalArgumentException("Message cannot be null");
		}
		AsynchronousJobStatus status = MessageUtils.readMessageBody(message, AsynchronousJobStatus.class);
		if(status.getJobBody() == null){
			throw new IllegalArgumentException("Job body cannot be null");
		}
		if(!(status.getJobBody() instanceof AsynchUploadJobBody)){
			throw new IllegalArgumentException("Expected a job body of type: "+AsynchUploadJobBody.class.getName()+" but received: "+status.getJobBody().getClass().getName());
		}
		return status;
	}
	
	
}
