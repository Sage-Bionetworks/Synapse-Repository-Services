package org.sagebionetworks.docusign;

import java.util.List;

import org.sagebionetworks.repo.model.educ.EDucSignatureStatus;

/*
 * Allows returning the signers' emails, which are not part of the EDucSignatureStatus DTO
 */
public record EnvelopeStatusResult(EDucSignatureStatus status, List<String> signerEmails) {
}
