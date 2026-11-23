package org.sagebionetworks.repo.manager.docusign;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.dbo.dao.dataaccess.EDucPendingNotification;
import org.sagebionetworks.repo.model.educ.EDucStatusEnum;

/**
 * Tells the creator of a data access request when its eDUC signature envelope reaches a terminal state.
 * <p>
 * Terminal states are first recorded, from the signing service's status changes since a checkpoint, and the
 * requesters are then dealt with from the recorded envelopes, so that an envelope still in flight never holds
 * up one that has finished.
 */
public interface EDucSignatureNotificationManager {

	/**
	 * The time from which the signing service's status changes are still to be read. A stack that has not
	 * read any starts a lookback window before now.
	 */
	Instant getStatusCheckpoint();

	/**
	 * Records the envelopes of this stack's data access requests that reached a terminal state, pending
	 * notification of their requesters, and moves the checkpoint on to the given time.
	 * <p>
	 * Envelopes that are not terminal, that no request on this stack holds, or that are already recorded are
	 * ignored, so a window can safely be read more than once.
	 *
	 * @param statusByEnvelopeId the status each envelope changed to
	 * @param queriedOn          the signing service's time for the read, where the next window starts
	 */
	void recordTerminalStatuses(Map<String, EDucStatusEnum> statusByEnvelopeId, Instant queriedOn);

	/**
	 * Recorded envelopes whose requester has not been dealt with yet, oldest first.
	 */
	List<EDucPendingNotification> listPendingNotifications(long limit);

	/**
	 * Deals with the requester of a recorded envelope: they are notified for those terminal states they need to
	 * know about, and either way the envelope is no longer pending. An envelope already dealt with is left
	 * alone.
	 */
	void processPendingNotification(EDucPendingNotification pending);
}
