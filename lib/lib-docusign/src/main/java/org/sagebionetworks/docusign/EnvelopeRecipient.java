package org.sagebionetworks.docusign;

/**
 * A signer currently on an envelope: the template role it fills, the address the envelope was sent
 * to, and whether it has finished signing. A recipient that has finished can no longer be modified
 * or removed.
 */
public record EnvelopeRecipient(String roleName, String email, boolean completed) {
}
