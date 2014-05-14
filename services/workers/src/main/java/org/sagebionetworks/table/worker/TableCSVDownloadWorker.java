package org.sagebionetworks.table.worker;

import java.io.File;
import java.io.FileWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.asynchronous.workers.sqs.WorkerProgress;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.table.TableRowManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.table.AsynchDownloadRequestBody;
import org.sagebionetworks.repo.model.table.AsynchDownloadResponseBody;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableUnavilableException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

import au.com.bytecode.opencsv.CSVWriter;

import com.amazonaws.services.sqs.model.Message;

public class TableCSVDownloadWorker implements Callable<List<Message>>{

	static private Logger log = LogManager.getLogger(TableCSVDownloadWorker.class);
	private List<Message> messages;
	private AsynchJobStatusManager asynchJobStatusManager;
	private TableRowManager tableRowManager;
	private UserManager userManger;
	private WorkerProgress workerProgress;
	
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
			AsynchDownloadRequestBody request = (AsynchDownloadRequestBody) status.getRequestBody();
			// Before we start determine how many rows there are.
			RowSet countSet = tableRowManager.query(user, request.getSql(), true, true);
			long rowCount = Long.parseLong(countSet.getRows().get(0).getValues().get(0));
			// For now we are assuming the query takes 3/4ths of the time and the file upload takes 1/4;
			long totalProgress = rowCount+(rowCount/4L);
			long currentProgress = 0;
			
			temp = File.createTempFile(fileName, ".csv");
			writer = new CSVWriter(new FileWriter(temp));
			// this object will update the progress of both the job and refresh the timeout on the message as rows are read from the DB.
			ProgressingCSVWriterStream stream = new ProgressingCSVWriterStream(writer, workerProgress, message, asynchJobStatusManager, currentProgress, totalProgress, status.getJobId());
			// we can use this to update the progress
			// Pares the query
			AsynchDownloadResponseBody response = tableRowManager.runConsistentQueryAsStream(request.getSql(), stream, true);
			// Create the file
			// Now upload the file as a filehandle
			asynchJobStatusManager.setComplete(status.getJobId(), response);
			return message;
		}catch (TableUnavilableException e){
			// This just means we cannot do this right now.  We can try again later.
			asynchJobStatusManager.updateJobProgress(status.getJobId(), 0L, 100L, "Waiting for the table index to become available...");
			// do not return the message because we do not want it to be deleted.
			return null;
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
		if(!(status.getRequestBody() instanceof AsynchDownloadRequestBody)){
			throw new IllegalArgumentException("Expected a job body of type: "+AsynchDownloadRequestBody.class.getName()+" but received: "+status.getRequestBody().getClass().getName());
		}
		return status;
	}

}
