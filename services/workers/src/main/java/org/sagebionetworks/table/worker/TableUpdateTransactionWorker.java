package org.sagebionetworks.table.worker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.table.TableEntityManagerImpl;
import org.sagebionetworks.repo.manager.table.TableViewManager;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

public class TableUpdateTransactionWorker implements MessageDrivenRunner {
	
	static private Logger log = LogManager.getLogger(TableUpdateTransactionWorker.class);
	
	@Autowired
	AsynchJobStatusManager asynchJobStatusManager;
	
	@Autowired
	TableEntityManagerImpl tableEntityManager;
	
	@Autowired
	TableViewManager tableViewManager;

	@Override
	public void run(ProgressCallback<Void> progressCallback, Message message)
			throws RecoverableMessageException, Exception {
		AsynchronousJobStatus status = asynchJobStatusManager.lookupJobStatus(message.getBody());
		TableUpdateTransactionRequest request = asynchJobStatusManager.extractRequestBody(status, TableUpdateTransactionRequest.class);
	}

}
