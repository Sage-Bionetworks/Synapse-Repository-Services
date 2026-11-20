package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.util.List;
import java.util.Optional;

/**
 * Records which eDUC envelopes have been seen to reach a terminal state, so that each is acted on once.
 */
public interface EDucEnvelopeNotificationDao {

	/**
	 * Records that an envelope reached a terminal state.
	 * <p>
	 * The unique key on the envelope is the last-resort guard against notifying a requester twice;
	 * {@link #findForUpdate} is what callers should rely on, since breaching the key is reported as an
	 * {@link IllegalArgumentException} indistinguishable from a programming error.
	 *
	 * @param messageId the ID of the message sent to the requester, or
	 *                  {@link DBOEDucEnvelopeNotification#NO_MESSAGE_SENT} when none was sent
	 * @throws IllegalArgumentException if the envelope has already been recorded
	 */
	DBOEDucEnvelopeNotification create(Long requestId, String envelopeId, String terminalStatus, Long messageId);

	/**
	 * The record for an envelope, locking the row so that two instances examining the same envelope cannot
	 * both decide to notify.
	 */
	Optional<DBOEDucEnvelopeNotification> findForUpdate(String envelopeId);

	Optional<DBOEDucEnvelopeNotification> find(String envelopeId);

	/**
	 * Routed envelopes with no terminal state recorded yet — those still in flight, plus any that reached a
	 * terminal state since the last examination.
	 * <p>
	 * Ordered by request ID so that a batch is stable, and bounded so that a backlog is worked through over
	 * successive examinations rather than in one pass.
	 */
	List<EDucEnvelopeToExamine> listEnvelopesToExamine(long limit);

	void truncateAll();
}
