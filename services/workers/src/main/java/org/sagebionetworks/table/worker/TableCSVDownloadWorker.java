package org.sagebionetworks.table.worker;

import java.io.File;
import java.io.FileWriter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.LocalFileUploadRequest;
import org.sagebionetworks.repo.manager.table.TableQueryManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.dbo.dao.table.TableExceptionTranslator;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.DownloadFromTableRequest;
import org.sagebionetworks.repo.model.table.DownloadFromTableResult;
import org.sagebionetworks.repo.model.table.QueryOptions;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.table.cluster.utils.CSVUtils;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.worker.AsyncJobRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * This worker will stream the results of a table SQL query to a local CSV file and upload the file
 * to S3 as a FileHandle.
 * 
 * @author jmhill
 *
 */
@Service
public class TableCSVDownloadWorker implements AsyncJobRunner<DownloadFromTableRequest, DownloadFromTableResult> {

	static private Logger log = LogManager.getLogger(TableCSVDownloadWorker.class);

	@Autowired
	private TableQueryManager tableQueryManager;
	@Autowired
	private FileHandleManager fileHandleManager;
	@Autowired
	private Clock clock;
	@Autowired
	private TableExceptionTranslator tableExceptionTranslator;
	
	@Override
	public Class<DownloadFromTableRequest> getRequestType() {
		return DownloadFromTableRequest.class;
	}
	
	@Override
	public Class<DownloadFromTableResult> getResponseType() {
		return DownloadFromTableResult.class;
	}
	
	@Override
	public DownloadFromTableResult run(String jobId, UserInfo user, DownloadFromTableRequest request, AsyncJobProgressCallback jobProgressCallback) throws RecoverableMessageException, Exception {
		String fileName = "Job-"+jobId;
		File temp = null;
		CSVWriter writer = null;
		
		try {
			// only run the count
			QueryOptions queryOptions = new QueryOptions().withRunQuery(false).withRunCount(true).withReturnFacets(false);
			// Before we start determine how many rows there are.
			QueryResultBundle queryResult = tableQueryManager.querySinglePage(jobProgressCallback, user, request, queryOptions);
			long rowCount = queryResult.getQueryCount();
			// Since each row must first be read from the database then uploaded to S3
			// The total amount of progress is two times the number of rows.
			long totalProgress = rowCount*2;
			long currentProgress = 0;
			// The CSV data will first be written to this file.
			temp = File.createTempFile(
					fileName,
					"." + CSVUtils.guessExtension(request.getCsvTableDescriptor() == null ? null : request.getCsvTableDescriptor().getSeparator()));
			writer = CSVUtils.createCSVWriter(new FileWriter(temp), request.getCsvTableDescriptor());
			// this object will update the progress of both the job and refresh the timeout on the message as rows are read from the DB.
			ProgressingCSVWriterStream stream = new ProgressingCSVWriterStream(writer, jobProgressCallback, currentProgress, totalProgress, clock);
			// Execute the actual query and stream the results to the file.
			DownloadFromTableResult result = null;
			try{
				result = tableQueryManager.runQueryDownloadAsStream(jobProgressCallback, user, request, stream);
			}finally{
				writer.close();
			}
	
			// At this point we have the entire CSV written to a local file.
			// Upload the file to S3 can create the filehandle.
			long startProgress = totalProgress/2; // we are half done at this point
			double bytesPerRow = rowCount == 0 ? 1 : temp.length() / rowCount;
			// This will keep the progress updated as the file is uploaded.
			UploadProgressListener uploadListener = new UploadProgressListener(jobProgressCallback, startProgress, bytesPerRow, totalProgress);
			String contentType = CSVUtils.guessContentType(request
					.getCsvTableDescriptor() == null ? null : request.getCsvTableDescriptor().getSeparator());
			String requestFileName = request.getFileName() == null ? null : request.getFileName();
			S3FileHandle fileHandle = fileHandleManager.uploadLocalFile(new LocalFileUploadRequest().withUserId(user.getId().toString()).withFileToUpload(temp).withContentType(contentType).withListener(uploadListener)
					.withFileName(requestFileName));
			result.setResultsFileHandleId(fileHandle.getId());
			return result;
		} catch (TableUnavailableException | LockUnavilableException e){
			// This just means we cannot do this right now.  We can try again later.
			jobProgressCallback.updateProgress("Waiting for the table index to become available...", 0L, 100L);
			// Throwing this will put the message back on the queue in 5 seconds.
			throw new RecoverableMessageException();
		} catch (TableFailedException e) {
			throw e;
		} catch(Throwable e){
			log.error("Worker Failed", e);
			// Attempt to translate the exception into a 'user-friendly' message.
			RuntimeException translatedException = tableExceptionTranslator.translateException(e);

			throw translatedException;
		} finally {
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

}
