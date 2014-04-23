package org.sagebionetworks.table.worker;

import java.util.List;
import java.util.concurrent.Callable;

import org.sagebionetworks.asynchronous.workers.sqs.MessageWorkerFactory;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.table.TableRowManager;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.sqs.model.Message;
/**
 * Factory to create a worker that downloads CSV data from S3 and appends it to a given TableEntity.
 * 
 * @author jmhill
 *
 */
public class TableCSVAppenderWorkerFactory implements MessageWorkerFactory{

	@Autowired
	private AsynchJobStatusManager asynchJobStatusManager;
	@Autowired
	private TableRowManager tableRowManager;
	@Autowired
	private FileHandleManager fileHandleManger;
	@Autowired
	private UserManager userManager;
	@Autowired
	private AmazonS3Client s3Client;
	
	@Override
	public Callable<List<Message>> createWorker(List<Message> messages) {
		return new TableCSVAppenderWorker(asynchJobStatusManager, tableRowManager,fileHandleManger,userManager,s3Client,messages);
	}

}
