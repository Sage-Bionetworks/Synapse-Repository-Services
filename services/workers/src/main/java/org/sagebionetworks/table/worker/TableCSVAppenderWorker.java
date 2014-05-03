package org.sagebionetworks.table.worker;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.io.input.CountingInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.table.TableRowManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.dbo.dao.table.CSVToRowIterator;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.AsynchUploadRequestBody;
import org.sagebionetworks.repo.model.table.AsynchUploadResponseBody;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.util.csv.CsvNullReader;

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
			AsynchUploadRequestBody body = (AsynchUploadRequestBody) status.getRequestBody();
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
			// Open a stream to the file in S3.
			S3Object s3Object = s3Client.getObject(fileHandle.getBucketName(), fileHandle.getKey());
			// This stream is used to keep track of the bytes read.
			CountingInputStream countingInputStream = new CountingInputStream(s3Object.getObjectContent());
			// Create a reader from the passed parameters
			reader = createCSVReader(new InputStreamReader(countingInputStream, "UTF-8"), body);
			// Reports progress back the caller.
			// Report progress every 2 seconds.
			long progressIntervalMs = 2000;
			ProgressReporter progressReporter = new IntervalProgressReporter(status.getJobId(),fileMetadata.getContentLength(), countingInputStream, asynchJobStatusManager, progressIntervalMs);
			// Create the iterator
			CSVToRowIterator iterator = new CSVToRowIterator(tableSchema, reader);
			ProgressingIteratorProxy iteratorProxy = new  ProgressingIteratorProxy(iterator, progressReporter);
			// Append the data to the table
			String etag = tableRowManager.appendRowsAsStream(user, body.getTableId(), tableSchema, iteratorProxy, null, null);
			// Done
			AsynchUploadResponseBody response = new AsynchUploadResponseBody();
			response.setRowsProcessed(new Long(progressReporter.getRowNumber()+1));
			response.setEtag(etag);
			asynchJobStatusManager.setComplete(status.getJobId(), response);
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
		if(!(status.getRequestBody() instanceof AsynchUploadRequestBody)){
			throw new IllegalArgumentException("Expected a job body of type: "+AsynchUploadRequestBody.class.getName()+" but received: "+status.getRequestBody().getClass().getName());
		}
		return status;
	}
	
	/**
	 * Create CsvNullReader with the correct parameters using the provided parameters or default values.
	 * @param reader
	 * @param body
	 * @param contentType
	 * @return
	 */
	public static CsvNullReader createCSVReader(Reader reader, AsynchUploadRequestBody body){
		if(body == null) throw new IllegalArgumentException("AsynchUploadRequestBody cannot be null");
		char separator = CsvNullReader.DEFAULT_SEPARATOR;
		char quotechar = CsvNullReader.DEFAULT_QUOTE_CHARACTER;
		char escape = CsvNullReader.DEFAULT_ESCAPE_CHARACTER;
		int skipLines = CsvNullReader.DEFAULT_SKIP_LINES;
		if(body.getSeparator() != null){
			if(body.getSeparator().length() != 1){
				throw new IllegalArgumentException("AsynchUploadRequestBody.separator must be exactly one character.");
			}
			separator = body.getSeparator().charAt(0);
		}
		if(body.getQuoteCharacter() != null){
			if(body.getQuoteCharacter().length() != 1){
				throw new IllegalArgumentException("AsynchUploadRequestBody.quoteCharacter must be exactly one character.");
			}
			quotechar = body.getQuoteCharacter().charAt(0);
		}
		if(body.getEscapeCharacter() != null){
			if(body.getEscapeCharacter().length() != 1){
				throw new IllegalArgumentException("AsynchUploadRequestBody.escapeCharacter must be exactly one character.");
			}
			escape = body.getEscapeCharacter().charAt(0);
		}
		if(body.getLinesToSkip() != null){
			skipLines = body.getLinesToSkip().intValue();
		}
		// Create the reader.
		return new CsvNullReader(reader, separator, quotechar, escape, skipLines);
	}
	
}
