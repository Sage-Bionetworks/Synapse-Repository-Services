package org.sagebionetworks.docusign;

import com.docusign.esign.model.EnvelopeTemplate;
import com.docusign.esign.model.EnvelopeTemplateResults;
import com.docusign.esign.model.Tabs;

/**
 * Wraps the DocuSign Templates REST API so that tests can substitute a mock
 * without needing to mock the final SDK classes. Implementations handle
 * authentication and retry-on-401 internally.
 */
interface DocuSignTemplatesApi {

	EnvelopeTemplateResults listTemplates(String startPosition, String count);

	/**
	 * The template, with its recipients, their tabs, and its list of documents populated. The documents
	 * are named but their tabs are not included; those come from {@link #getDocumentTabs}.
	 */
	EnvelopeTemplate getTemplate(String templateId);

	/**
	 * The tabs belonging to one of a template's documents rather than to a recipient. This is the only
	 * place a sender field appears: having no recipient, it is absent from the template's recipient tabs.
	 */
	Tabs getDocumentTabs(String templateId, String documentId);
}
