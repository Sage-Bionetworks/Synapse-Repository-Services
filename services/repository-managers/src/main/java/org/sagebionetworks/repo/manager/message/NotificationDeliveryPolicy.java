package org.sagebionetworks.repo.manager.message;

import org.sagebionetworks.repo.manager.feature.FeatureManager;
import org.sagebionetworks.repo.manager.stack.ProdDetector;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.stereotype.Service;

/**
 * Decides whether a notification produced by a background process is actually delivered from this stack.
 * <p>
 * Staging runs the same workers against the same data as production, so delivering from both would notify
 * every recipient twice.
 */
@Service
public class NotificationDeliveryPolicy {

	private final FeatureManager featureManager;
	private final ProdDetector prodDetector;

	public NotificationDeliveryPolicy(FeatureManager featureManager, ProdDetector prodDetector) {
		this.featureManager = featureManager;
		this.prodDetector = prodDetector;
	}

	/**
	 * @return True if the notification should be delivered to the given recipient: always for a member of the
	 *         Synapse testing group, so that notifications can be tried out on staging, and otherwise only on
	 *         the production stack.
	 * @throws RecoverableMessageException If the current stack cannot be identified, so that the caller retries
	 *                                     rather than wrongly delivering or dropping the notification.
	 */
	public boolean shouldDeliver(UserInfo recipient) throws RecoverableMessageException {
		if (featureManager.isUserInTestingGroup(recipient)) {
			return true;
		}
		return prodDetector.isProductionStack()
				.orElseThrow(() -> new RecoverableMessageException("Could not detect current stack version."));
	}
}
