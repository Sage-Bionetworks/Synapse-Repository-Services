package org.sagebionetworks.docusign;

import com.docusign.esign.model.Envelope;
import com.docusign.esign.model.EnvelopeDefinition;
import com.docusign.esign.model.EnvelopeSummary;

interface DocuSignEnvelopesApi {

	EnvelopeSummary createEnvelope(EnvelopeDefinition envelopeDefinition);

	void voidEnvelope(String envelopeId, String reason);

	Envelope getEnvelope(String envelopeId);

	byte[] getDocument(String envelopeId, String documentId);
}
