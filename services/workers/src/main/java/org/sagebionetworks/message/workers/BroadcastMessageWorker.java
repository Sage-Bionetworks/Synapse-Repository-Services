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
import org.sagebionetworks.utils.HttpClientHelperException;
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
			throws RecoverableMessageException {
		UserInfo admin = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		log.info("broadcasting "+message.getChangeType()+" "+message.getObjectType());
		try {
			broadcastManager.broadcastMessage(admin, progressCallback, message);
		} catch (HttpClientHelperException e) {
			log.error("Fail to broadcast message. Reason: "+e.getMessage()+". Status: "+e.getHttpStatus()+". Response: "+e.getResponse());
			throw new RecoverableMessageException();
		} catch (Exception e) {
			log.error("Fail to broadcast message. "+e.getMessage());
		}
	}

}
