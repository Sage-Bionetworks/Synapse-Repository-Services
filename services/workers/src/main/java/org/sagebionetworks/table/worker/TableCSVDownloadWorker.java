package org.sagebionetworks.table.worker;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;

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
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.DownloadFromTableRequest;
import org.sagebionetworks.repo.model.table.DownloadFromTableResult;
import org.sagebionetworks.repo.model.table.QueryResult;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableUnavilableException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;

import au.com.bytecode.opencsv.CSVWriter;

import com.amazonaws.services.sqs.model.Message;

/**
 * This worker will stream the results of a table SQL query to a local CSV file and upload the file
 * to S3 as a FileHandle.
 * 
 * @author jmhill
 *
 */
public class TableCSVDownloadWorker implements Worker {

	private static final String TEXT_CSV = "text/csv";
	static private Logger log = LogManager.getLogger(TableCSVDownloadWorker.class);
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

	private int retryTimeoutOnTableUnavailableInSeconds = 5;

	@Override
	public void setMessages(List<Message> messages) {
		this.messages = messages;
	}

	@Override
	public void setWorkerProgress(WorkerProgress workerProgress) {
		this.workerProgress = workerProgress;
	}

	public void setRetryTimeoutOnTableUnavailableInSeconds(int retryTimeoutOnTableUnavailableInSeconds) {
		this.retryTimeoutOnTableUnavailableInSeconds = retryTimeoutOnTableUnavailableInSeconds;
	}

	@Override
	public List<Message> call() throws Exception {
		List<Message> toDelete = new LinkedList<Message>();
		for(Message message: messages){
			try{
				Message returned = processMessage(message);
				if(returned != null){
					toDelete.add(returned);
				}
			}catch(Throwable e){
				// Treat unknown errors as unrecoverable and return them
				toDelete.add(message);
				log.error("Worker Failed", e);
			}
		}
		return toDelete;
	}

	private Message processMessage(Message message) throws Throwable {
		AsynchronousJobStatus status = extractStatus(message);
		String fileName = "Job-"+status.getJobId();
		File temp = null;
		CSVWriter writer = null;
		try{
			UserInfo user = userManger.getUserInfo(status.getStartedByUserId());
			DownloadFromTableRequest request = (DownloadFromTableRequest) status.getRequestBody();
			// Before we start determine how many rows there are.
			Pair<QueryResult, Long> queryResult = tableRowManager.query(user, request.getSql(), request.getSort(), null, null, false, true,
					true);
			long rowCount = queryResult.getSecond();
			// Since each row must first be read from the database then uploaded to S3
			// The total amount of progress is two times the number of rows.
			long totalProgress = rowCount*2;
			long currentProgress = 0;
			// The CSV data will first be written to this file.
			temp = File.createTempFile(fileName, ".csv");
			writer = createCSVWriter(new FileWriter(temp), request);
			// this object will update the progress of both the job and refresh the timeout on the message as rows are read from the DB.
			ProgressingCSVWriterStream stream = new ProgressingCSVWriterStream(writer, workerProgress, message, asynchJobStatusManager, currentProgress, totalProgress, status.getJobId());
			boolean includeRowIdAndVersion = true;
			if(request.getIncludeRowIdAndRowVersion() != null){
				includeRowIdAndVersion = request.getIncludeRowIdAndRowVersion();
			}
			// Execute the actual query and stream the results to the file.
			DownloadFromTableResult result = null;
			try{
				result = tableRowManager
						.runConsistentQueryAsStream(user, request.getSql(), request.getSort(), stream, includeRowIdAndVersion);
			}finally{
				writer.close();
			}

			// At this point we have the entire CSV written to a local file.
			// Upload the file to S3 can create the filehandle.
			long startProgress = totalProgress/2; // we are half done at this point
			double bytesPerRow = rowCount == 0 ? 1 : temp.length() / rowCount;
			// This will keep the progress updated as the file is uploaded.
			UploadProgressListener uploadListener = new UploadProgressListener(workerProgress, message, startProgress, bytesPerRow, totalProgress, asynchJobStatusManager, status.getJobId());
			S3FileHandle fileHandle = fileHandleManager.multipartUploadLocalFile(user, temp, TEXT_CSV, uploadListener);
			result.setResultsFileHandleId(fileHandle.getId());
			// Create the file
			// Now upload the file as a filehandle
			asynchJobStatusManager.setComplete(status.getJobId(), result);
			return message;
		}catch (TableUnavilableException e){
			// This just means we cannot do this right now.  We can try again later.
			asynchJobStatusManager.updateJobProgress(status.getJobId(), 0L, 100L, "Waiting for the table index to become available...");
			// do not return the message because we do not want it to be deleted.
			// but we don't want to wait too long, so set the visibility timeout to something smaller
			workerProgress.retryMessage(message, retryTimeoutOnTableUnavailableInSeconds);
			return null;
		} catch (TableFailedException e) {
			// This means we cannot use this table
			asynchJobStatusManager.setJobFailed(status.getJobId(), e);
			return message;
		}catch(Throwable e){
			// The job failed
			asynchJobStatusManager.setJobFailed(status.getJobId(), e);
			throw e;
		}finally{
			if(writer != null){
				try {
					writer.close();
				} catch (Exception e2) {}
			}
			if(temp != null){
				temp.delete();
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
		if (!(status.getRequestBody() instanceof DownloadFromTableRequest)) {
			throw new IllegalArgumentException("Expected a job body of type: " + DownloadFromTableRequest.class.getName() + " but received: "
					+ status.getRequestBody().getClass().getName());
		}
		return status;
	}

	/**
	 * Prepare a writer with the parameters from the request.
	 * @param writer
	 * @param request
	 * @return
	 */
	public static CSVWriter createCSVWriter(Writer writer, DownloadFromTableRequest request) {
		if (request == null)
			throw new IllegalArgumentException("DownloadFromTableRequest cannot be null");
		char separator = CSVWriter.DEFAULT_SEPARATOR;
		char quotechar = CSVWriter.DEFAULT_QUOTE_CHARACTER;
		char escape = CSVWriter.DEFAULT_ESCAPE_CHARACTER;
		String lineEnd = CSVWriter.DEFAULT_LINE_END;
		if (request.getCsvTableDescriptor() != null) {
			if (request.getCsvTableDescriptor().getSeparator() != null) {
				if (request.getCsvTableDescriptor().getSeparator().length() != 1) {
					throw new IllegalArgumentException("CsvTableDescriptor.separator must be exactly one character.");
				}
				separator = request.getCsvTableDescriptor().getSeparator().charAt(0);
			}
			if (request.getCsvTableDescriptor().getQuoteCharacter() != null) {
				if (request.getCsvTableDescriptor().getQuoteCharacter().length() != 1) {
					throw new IllegalArgumentException("CsvTableDescriptor.quoteCharacter must be exactly one character.");
				}
				quotechar = request.getCsvTableDescriptor().getQuoteCharacter().charAt(0);
			}
			if (request.getCsvTableDescriptor().getEscapeCharacter() != null) {
				if (request.getCsvTableDescriptor().getEscapeCharacter().length() != 1) {
					throw new IllegalArgumentException("CsvTableDescriptor.escapeCharacter must be exactly one character.");
				}
				escape = request.getCsvTableDescriptor().getEscapeCharacter().charAt(0);
			}
			if (request.getCsvTableDescriptor().getLineEnd() != null) {
				lineEnd = request.getCsvTableDescriptor().getLineEnd();
			}
		}
		// Create the reader.
		return new CSVWriter(writer, separator, quotechar, escape, lineEnd);
	}
}
