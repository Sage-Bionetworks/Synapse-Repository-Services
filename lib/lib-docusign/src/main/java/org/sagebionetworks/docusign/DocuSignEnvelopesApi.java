package org.sagebionetworks.docusign;

import java.util.List;

import com.docusign.esign.model.Envelope;
import com.docusign.esign.model.EnvelopeDefinition;
import com.docusign.esign.model.EnvelopeSummary;
import com.docusign.esign.model.Recipients;
import com.docusign.esign.model.Tabs;
import com.docusign.esign.model.TemplateInformation;

interface DocuSignEnvelopesApi {

	EnvelopeSummary createEnvelope(EnvelopeDefinition envelopeDefinition);

	void voidEnvelope(String envelopeId, String reason);

	/**
	 * Throw an envelope away.
	 * <p>
	 * Not the same as voiding one. DocuSign has no endpoint that deletes an envelope; it models deletion
	 * as a move into the account's "Deleted" folder, which only <em>voids</em> the envelope when it is
	 * already in process. A draft moved there is simply gone, which is what this is for — voiding a draft
	 * is refused, since an envelope has to have been sent before it can be voided.
	 */
	void discardEnvelope(String envelopeId);

	void updateEnvelope(String envelopeId, Envelope envelope);

	Envelope getEnvelope(String envelopeId);

	/**
	 * The given envelopes, each with its recipients populated.
	 * <p>
	 * This deliberately uses DocuSign's envelope listing rather than its status endpoint. The status
	 * endpoint returns a status-only projection: it has no way to ask for recipients, so every
	 * envelope it returns has a null recipient list and a signature count cannot be derived from it.
	 */
	List<Envelope> listStatusChanges(List<String> envelopeIds);

	/**
	 * The templates that were applied to an envelope. An envelope created from a template does not
	 * carry the template's ID as a field, so this is how the envelope's origin is established.
	 */
	TemplateInformation listTemplates(String envelopeId);

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

	/**
	 * Add tabs to a recipient already on an envelope. Adding a recipient does not create the tabs
	 * nested in it, so they have to be created in their own request.
	 */
	void createTabs(String envelopeId, String recipientId, Tabs tabs);
}
