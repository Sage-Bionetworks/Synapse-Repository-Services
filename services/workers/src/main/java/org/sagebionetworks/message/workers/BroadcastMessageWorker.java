package org.sagebionetworks.message.workers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.message.BroadcastMessageManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A worker that send email notifications to all subscribers.
 * 
 * @author kimyentruong
 *
 */
public class BroadcastMessageWorker implements ChangeMessageDrivenRunner{
	private static Logger log = LogManager.getLogger(BroadcastMessageWorker.class);

	@Autowired
	private BroadcastMessageManager broadcastManager;
	@Autowired
	private UserManager userManager;

	@Override
	public void run(ProgressCallback<ChangeMessage> progressCallback, ChangeMessage message)
			throws RecoverableMessageException, Exception {
		UserInfo admin = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		log.info("broadcasting "+message.getChangeType()+" "+message.getObjectType());
		broadcastManager.broadcastMessage(admin, progressCallback, message);
	}

}
