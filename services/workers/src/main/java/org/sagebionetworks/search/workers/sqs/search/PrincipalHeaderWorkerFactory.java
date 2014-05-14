package org.sagebionetworks.search.workers.sqs.search;

import java.util.List;
import java.util.concurrent.Callable;

import org.sagebionetworks.asynchronous.workers.sqs.MessageWorkerFactory;
import org.sagebionetworks.asynchronous.workers.sqs.WorkerProgress;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.repo.model.PrincipalHeaderDAO;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

/**
 * Spawns message sending workers
 */
public class PrincipalHeaderWorkerFactory implements MessageWorkerFactory {
	
	@Autowired
	private PrincipalHeaderDAO prinHeadDAO;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private UserProfileDAO userProfileDAO;
	
	@Autowired
	private TeamDAO teamDAO;
	
	@Autowired
	private WorkerLogger workerLogger;

	@Override
	public Callable<List<Message>> createWorker(List<Message> messages, WorkerProgress workerProgress) {
		return new PrincipalHeaderWorker(messages, prinHeadDAO, userGroupDAO, userProfileDAO, teamDAO, workerLogger);
	}

}
