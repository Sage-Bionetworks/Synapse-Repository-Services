package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import org.sagebionetworks.repo.model.educ.EDucStatusEnum;

/**
 * A terminal eDUC envelope whose requester has not been dealt with yet.
 *
 * @param requestId      the data access request holding the envelope
 * @param envelopeId     the envelope that reached a terminal state
 * @param terminalStatus the terminal state it reached
 * @param createdBy      the principal who created the request, and so the one to notify
 */
public record EDucPendingNotification(Long requestId, String envelopeId, EDucStatusEnum terminalStatus,
		Long createdBy) {
}
