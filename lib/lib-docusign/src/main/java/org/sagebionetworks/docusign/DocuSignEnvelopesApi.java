package org.sagebionetworks.docusign;

import java.util.List;

import com.docusign.esign.model.Envelope;
import com.docusign.esign.model.EnvelopeDefinition;
import com.docusign.esign.model.EnvelopeSummary;
import com.docusign.esign.model.Recipients;

interface DocuSignEnvelopesApi {

	EnvelopeSummary createEnvelope(EnvelopeDefinition envelopeDefinition);

	void voidEnvelope(String envelopeId, String reason);

	void updateEnvelope(String envelopeId, Envelope envelope);

	Envelope getEnvelope(String envelopeId);

	List<Envelope> listStatus(List<String> envelopeIds);

	byte[] getDocument(String envelopeId, String documentId);

	/**
	 * Update existing recipients (and their tabs) on an envelope.
	 *
	 * @param resend when true, DocuSign re-notifies the affected recipients
	 */
	void updateRecipients(String envelopeId, Recipients recipients, boolean resend);

	/**
	 * Add new recipients (and their tabs) to an envelope.
	 *
	 * @param resend when true, DocuSign notifies the newly added recipients
	 */
	void createRecipients(String envelopeId, Recipients recipients, boolean resend);

	/**
	 * Remove the given recipients from an envelope.
	 */
	void deleteRecipients(String envelopeId, Recipients recipients);
}
