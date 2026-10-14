package org.sagebionetworks.docusign;

/**
 * The identity of a DocuSign envelope recipient (signer). Both an email address and a name are
 * required before an envelope can be sent.
 */
public record RecipientInfo(String email, String name) {
}
