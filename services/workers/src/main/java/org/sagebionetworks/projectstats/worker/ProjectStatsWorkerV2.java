package org.sagebionetworks.projectstats.worker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.ProjectStatsManager;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.util.TimeoutUtils;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

public class ProjectStatsWorkerV2 implements ChangeMessageDrivenRunner {
	
	static private Logger log = LogManager.getLogger(ProjectStatsWorkerV2.class);
	
	/*
	 * Messages older than this will be ignored by this worker.
	 */
	public static final long MAX_MESSAGE_TIMEOUT_MS = 1000*60*60; // one hour
	
	@Autowired
	TimeoutUtils timeoutUtils;
	@Autowired
	ProjectStatsManager projectStatsManager;

	@Override
	public void run(ProgressCallback<ChangeMessage> progressCallback,
			ChangeMessage message) throws RecoverableMessageException,
			Exception {
		
		// ignore all old messages.
		if(timeoutUtils.hasExpired(MAX_MESSAGE_TIMEOUT_MS, message.getTimestamp().getTime())){
			if(log.isTraceEnabled()){
				log.trace("Ignoring change message: "+message.getChangeNumber()+" since it is older than: "+MAX_MESSAGE_TIMEOUT_MS+" MS");
			}
			return;
		}
		
		// manager does the worker
		projectStatsManager.updateProjectStats(message.getUserId(), message.getObjectId(), message.getObjectType(), message.getTimestamp());
	}

}
