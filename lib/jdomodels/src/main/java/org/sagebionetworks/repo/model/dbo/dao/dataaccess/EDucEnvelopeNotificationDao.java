package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.sagebionetworks.repo.model.educ.EDucStatusEnum;

/**
 * Records which eDUC envelopes of data access requests have reached a terminal state and whether each
 * requester has been notified, along with how far the signing service's status changes have been read.
 */
public interface EDucEnvelopeNotificationDao {

	/**
	 * Records that an envelope reached a terminal state, pending notification of the requester.
	 *
	 * @throws IllegalArgumentException if the envelope has already been recorded
	 */
	DBOEDucEnvelopeNotification createPending(Long requestId, String envelopeId, EDucStatusEnum terminalStatus);

	/**
	 * The record for an envelope, locking the row so that it can be acted on once.
	 */
	Optional<DBOEDucEnvelopeNotification> findForUpdate(String envelopeId);

	Optional<DBOEDucEnvelopeNotification> find(String envelopeId);

	/**
	 * Records how the requester was dealt with.
	 *
	 * @param messageId the ID of the message sent, required for {@link EDucNotificationState#SENT} and
	 *                  otherwise null
	 */
	void updateNotificationState(String envelopeId, EDucNotificationState state, Long messageId);

	/**
	 * The data access requests holding any of the given envelopes, keyed by envelope ID. An envelope that no
	 * request holds is absent.
	 */
	Map<String, Long> findRequestIdsByEnvelopeId(Collection<String> envelopeIds);

	/**
	 * Envelopes whose requester has not been dealt with yet, oldest first.
	 */
	List<EDucPendingNotification> listPendingNotifications(long limit);

	/**
	 * The time from which the signing service's status changes are still to be read, if this stack has read
	 * any.
	 */
	Optional<Instant> getStatusCheckpoint();

	void setStatusCheckpoint(Instant lastQueriedOn);

	void truncateAll();
}
