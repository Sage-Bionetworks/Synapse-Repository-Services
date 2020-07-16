package org.sagebionetworks.dataaccess.workers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.dataaccess.AccessApprovalNotificationManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Worker that process {@link ObjectType#ACCESS_APPROVAL} changes to send
 * revocation notifications
 * 
 * @author Marco Marasca
 *
 */
public class AccessApprovalRevokedNotificationWorker implements ChangeMessageDrivenRunner {

	private static final Logger LOG = LogManager.getLogger(AccessApprovalRevokedNotificationWorker.class);

	private UserManager userManager;
	private AccessApprovalNotificationManager notificationManager;

	@Autowired
	public AccessApprovalRevokedNotificationWorker(final UserManager userManager,
			final AccessApprovalNotificationManager notificationManager) {
		this.userManager = userManager;
		this.notificationManager = notificationManager;
	}

	@Override
	public void run(ProgressCallback progressCallback, ChangeMessage message)
			throws RecoverableMessageException, Exception {
		try {
			UserInfo admin = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
			notificationManager.processAccessApprovalChangeMessage(admin, message);
		} catch (RecoverableMessageException e) {
			throw e;
		} catch (Throwable e) {
			LOG.error(e.getMessage(), e);
		}

	}

}
