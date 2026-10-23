package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

/**
 * A routed eDUC envelope whose terminal state has not been recorded yet, and the request it belongs to.
 *
 * @param requestId  the data access request holding the envelope
 * @param envelopeId the envelope to ask the signing service about
 * @param createdBy  the principal who created the request, and so the one to notify
 */
public record EDucEnvelopeToExamine(Long requestId, String envelopeId, Long createdBy) {
}
