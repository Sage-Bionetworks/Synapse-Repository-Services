package org.sagebionetworks.table.worker;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

import org.apache.commons.lang.BooleanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobUtils;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.table.TableQueryManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.DownloadFromTableRequest;
import org.sagebionetworks.repo.model.table.DownloadFromTableResult;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.table.cluster.utils.CSVUtils;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.beans.factory.annotation.Autowired;

import au.com.bytecode.opencsv.CSVWriter;
import au.com.bytecode.opencsv.Constants;

import com.amazonaws.services.sqs.model.Message;

/**
 * This worker will stream the results of a table SQL query to a local CSV file and upload the file
 * to S3 as a FileHandle.
 * 
 * @author jmhill
 *
 */
public class TableCSVDownloadWorker implements MessageDrivenRunner {

	static private Logger log = LogManager.getLogger(TableCSVDownloadWorker.class);

	@Autowired
	private AsynchJobStatusManager asynchJobStatusManager;
	@Autowired
	private TableQueryManager tableQueryManger;
	@Autowired
	private UserManager userManger;
	@Autowired
	private FileHandleManager fileHandleManager;
	@Autowired
	Clock clock;	

	@Override
	public void run(ProgressCallback<Void> progressCallback, Message message) throws Exception {
		AsynchronousJobStatus status = asynchJobStatusManager.lookupJobStatus(message.getBody());
		String fileName = "Job-"+status.getJobId();
		File temp = null;
		CSVWriter writer = null;
		try{
			UserInfo user = userManger.getUserInfo(status.getStartedByUserId());
			DownloadFromTableRequest request = AsynchJobUtils.extractRequestBody(status, DownloadFromTableRequest.class);
			// Before we start determine how many rows there are.
			QueryResultBundle queryResult = tableQueryManger.querySinglePage(progressCallback, user, request.getSql(), request.getSort(), null, null, null, false,
					true, false, true);
			long rowCount = queryResult.getQueryCount();
			// Since each row must first be read from the database then uploaded to S3
			// The total amount of progress is two times the number of rows.
			long totalProgress = rowCount*2;
			long currentProgress = 0;
			// The CSV data will first be written to this file.
			temp = File.createTempFile(
					fileName,
					"."
							+ CSVUtils.guessExtension(request.getCsvTableDescriptor() == null ? null : request.getCsvTableDescriptor()
									.getSeparator()));
			writer = createCSVWriter(new FileWriter(temp), request);
			// this object will update the progress of both the job and refresh the timeout on the message as rows are read from the DB.
			ProgressingCSVWriterStream stream = new ProgressingCSVWriterStream(writer, progressCallback, message, asynchJobStatusManager, currentProgress, totalProgress, status.getJobId(), clock);
			boolean includeRowIdAndVersion = BooleanUtils.isNotFalse(request.getIncludeRowIdAndRowVersion());
			boolean writeHeaders = BooleanUtils.isNotFalse(request.getWriteHeader());
			// Execute the actual query and stream the results to the file.
			DownloadFromTableResult result = null;
			try{
				result = tableQueryManger.runConsistentQueryAsStream(progressCallback, user, request.getSql(), request.getSort(), request.getSelectedFacets(), stream,
						includeRowIdAndVersion, writeHeaders);
			}finally{
				writer.close();
			}

			// At this point we have the entire CSV written to a local file.
			// Upload the file to S3 can create the filehandle.
			long startProgress = totalProgress/2; // we are half done at this point
			double bytesPerRow = rowCount == 0 ? 1 : temp.length() / rowCount;
			// This will keep the progress updated as the file is uploaded.
			UploadProgressListener uploadListener = new UploadProgressListener(progressCallback, message, startProgress, bytesPerRow, totalProgress, asynchJobStatusManager, status.getJobId());
			S3FileHandle fileHandle = fileHandleManager.multipartUploadLocalFile(user, temp, CSVUtils.guessContentType(request
					.getCsvTableDescriptor() == null ? null : request.getCsvTableDescriptor().getSeparator()), uploadListener);
			result.setResultsFileHandleId(fileHandle.getId());
			// Create the file
			// Now upload the file as a filehandle
			asynchJobStatusManager.setComplete(status.getJobId(), result);
		}catch (TableUnavailableException e){
			// This just means we cannot do this right now.  We can try again later.
			asynchJobStatusManager.updateJobProgress(status.getJobId(), 0L, 100L, "Waiting for the table index to become available...");
			// Throwing this will put the message back on the queue in 5 seconds.
			throw new RecoverableMessageException();
		} catch (LockUnavilableException e){
			// This just means we cannot do this right now.  We can try again later.
			asynchJobStatusManager.updateJobProgress(status.getJobId(), 0L, 100L, "Waiting for the table index to become available...");
			// Throwing this will put the message back on the queue in 5 seconds.
			throw new RecoverableMessageException();
		} catch (TableFailedException e) {
			// This means we cannot use this table
			asynchJobStatusManager.setJobFailed(status.getJobId(), e);
		}catch(Throwable e){
			// The job failed
			asynchJobStatusManager.setJobFailed(status.getJobId(), e);
			log.error("Worker Failed", e);
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
	 * Prepare a writer with the parameters from the request.
	 * @param writer
	 * @param request
	 * @return
	 */
	public static CSVWriter createCSVWriter(Writer writer, DownloadFromTableRequest request) {
		if (request == null)
			throw new IllegalArgumentException("DownloadFromTableRequest cannot be null");
		char separator = Constants.DEFAULT_SEPARATOR;
		char quotechar = Constants.DEFAULT_QUOTE_CHARACTER;
		char escape = Constants.DEFAULT_ESCAPE_CHARACTER;
		String lineEnd = Constants.DEFAULT_LINE_END;
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
