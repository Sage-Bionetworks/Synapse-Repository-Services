package org.sagebionetworks.docusign;

import com.docusign.esign.model.EnvelopeTemplate;
import com.docusign.esign.model.EnvelopeTemplateResults;

/**
 * Wraps the DocuSign Templates REST API so that tests can substitute a mock
 * without needing to mock the final SDK classes. Implementations handle
 * authentication and retry-on-401 internally.
 */
interface DocuSignTemplatesApi {

	EnvelopeTemplateResults listTemplates(String startPosition, String count);

	EnvelopeTemplate getTemplate(String templateId);
}
