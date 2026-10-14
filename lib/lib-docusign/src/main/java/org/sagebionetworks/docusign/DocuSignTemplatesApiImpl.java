package org.sagebionetworks.docusign;

import org.springframework.stereotype.Service;

import com.docusign.esign.api.TemplatesApi;
import com.docusign.esign.client.ApiClient;
import com.docusign.esign.model.EnvelopeTemplate;
import com.docusign.esign.model.EnvelopeTemplateResults;
import com.docusign.esign.model.Tabs;

@Service
class DocuSignTemplatesApiImpl implements DocuSignTemplatesApi {

	private final DocuSignClientConfig config;
	private final DocuSignApiRetryHelper retryHelper;

	DocuSignTemplatesApiImpl(DocuSignClientConfig config, DocuSignAccessTokenProvider accessTokenProvider) {
		this.config = config;
		this.retryHelper = new DocuSignApiRetryHelper(accessTokenProvider);
	}

	@Override
	public EnvelopeTemplateResults listTemplates(String startPosition, String count) {
		return retryHelper.executeWithRetry(accessToken -> {
			ApiClient apiClient = new ApiClient(config.getBasePath());
			apiClient.addDefaultHeader("Authorization", "Bearer " + accessToken);
			TemplatesApi templatesApi = new TemplatesApi(apiClient);
			TemplatesApi.ListTemplatesOptions options = templatesApi.new ListTemplatesOptions();
			options.setStartPosition(startPosition);
			options.setCount(count);
			return templatesApi.listTemplates(config.getAccountId(), options);
		});
	}

	@Override
	public EnvelopeTemplate getTemplate(String templateId) {
		return retryHelper.executeWithRetry(accessToken -> {
			ApiClient apiClient = new ApiClient(config.getBasePath());
			apiClient.addDefaultHeader("Authorization", "Bearer " + accessToken);
			TemplatesApi templatesApi = new TemplatesApi(apiClient);
			TemplatesApi.GetOptions options = templatesApi.new GetOptions();
			// Documents are asked for so that their IDs are known; this does not return their tabs.
			options.setInclude("documents,tabs,recipients");
			return templatesApi.get(config.getAccountId(), templateId, options);
		});
	}

	@Override
	public Tabs getDocumentTabs(String templateId, String documentId) {
		return retryHelper.executeWithRetry(accessToken -> {
			ApiClient apiClient = new ApiClient(config.getBasePath());
			apiClient.addDefaultHeader("Authorization", "Bearer " + accessToken);
			TemplatesApi templatesApi = new TemplatesApi(apiClient);
			return templatesApi.getDocumentTabs(config.getAccountId(), templateId, documentId);
		});
	}
}
