package org.sagebionetworks.table.worker;

import java.io.File;
import java.io.FileWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.swing.text.TabExpander;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.table.TableRowManager;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.dao.table.RowAndHeaderHandler;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelUtils;
import org.sagebionetworks.repo.model.table.AsynchDownloadRequestBody;
import org.sagebionetworks.repo.model.table.AsynchDownloadResponseBody;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.SqlQuery;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.util.SqlElementUntils;

import au.com.bytecode.opencsv.CSVWriter;

import com.amazonaws.services.sqs.model.Message;

public class TableCSVDownloadWorker implements Callable<List<Message>>{

	static private Logger log = LogManager.getLogger(TableCSVDownloadWorker.class);
	private List<Message> messages;
	private ConnectionFactory tableConnectionFactory;
	private AsynchJobStatusManager asynchJobStatusManager;
	private TableRowManager tableRowManager;
	
	@Override
	public List<Message> call() throws Exception {
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

	private Message processMessage(Message message) throws Throwable {
		AsynchronousJobStatus status = extractStatus(message);
		String fileName = "Job-"+status.getJobId();
		File temp = null;
		CSVWriter writer = null;
		try{
			temp = File.createTempFile(fileName, ".csv");
			writer = new CSVWriter(new FileWriter(temp));
			AsynchDownloadRequestBody request = (AsynchDownloadRequestBody) status.getRequestBody();
			// Pares the query
			AsynchDownloadResponseBody response = tableRowManager.runConsistentQueryAsStream(request.getSql(), writer, true);
			
			asynchJobStatusManager.setComplete(status.getJobId(), response);
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
		if(!(status.getRequestBody() instanceof AsynchDownloadRequestBody)){
			throw new IllegalArgumentException("Expected a job body of type: "+AsynchDownloadRequestBody.class.getName()+" but received: "+status.getRequestBody().getClass().getName());
		}
		return status;
	}

}
