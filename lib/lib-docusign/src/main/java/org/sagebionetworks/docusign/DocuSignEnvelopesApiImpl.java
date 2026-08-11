package org.sagebionetworks.docusign;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.docusign.esign.api.EnvelopesApi;
import com.docusign.esign.client.ApiClient;
import com.docusign.esign.model.Envelope;
import com.docusign.esign.model.EnvelopeDefinition;
import com.docusign.esign.model.EnvelopeIdsRequest;
import com.docusign.esign.model.EnvelopeSummary;
import com.docusign.esign.model.EnvelopesInformation;

@Service
class DocuSignEnvelopesApiImpl implements DocuSignEnvelopesApi {

	private final DocuSignClientConfig config;
	private final DocuSignApiRetryHelper retryHelper;

	DocuSignEnvelopesApiImpl(DocuSignClientConfig config, DocuSignAccessTokenProvider accessTokenProvider) {
		this.config = config;
		this.retryHelper = new DocuSignApiRetryHelper(accessTokenProvider);
	}

	@Override
	public EnvelopeSummary createEnvelope(EnvelopeDefinition envelopeDefinition) {
		return retryHelper.executeWithRetry(accessToken -> {
			ApiClient apiClient = new ApiClient(config.getBasePath());
			apiClient.addDefaultHeader("Authorization", "Bearer " + accessToken);
			EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
			return envelopesApi.createEnvelope(config.getAccountId(), envelopeDefinition);
		});
	}

	@Override
	public void voidEnvelope(String envelopeId, String reason) {
		retryHelper.executeWithRetry(accessToken -> {
			ApiClient apiClient = new ApiClient(config.getBasePath());
			apiClient.addDefaultHeader("Authorization", "Bearer " + accessToken);
			EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
			Envelope envelope = new Envelope();
			envelope.setStatus("voided");
			envelope.setVoidedReason(reason);
			return envelopesApi.update(config.getAccountId(), envelopeId, envelope);
		});
	}

	@Override
	public void updateEnvelope(String envelopeId, Envelope envelope) {
		retryHelper.executeWithRetry(accessToken -> {
			ApiClient apiClient = new ApiClient(config.getBasePath());
			apiClient.addDefaultHeader("Authorization", "Bearer " + accessToken);
			EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
			return envelopesApi.update(config.getAccountId(), envelopeId, envelope);
		});
	}

	@Override
	public Envelope getEnvelope(String envelopeId) {
		return retryHelper.executeWithRetry(accessToken -> {
			ApiClient apiClient = new ApiClient(config.getBasePath());
			apiClient.addDefaultHeader("Authorization", "Bearer " + accessToken);
			EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
			EnvelopesApi.GetEnvelopeOptions options = envelopesApi.new GetEnvelopeOptions();
			options.setInclude("recipients");
			return envelopesApi.getEnvelope(config.getAccountId(), envelopeId, options);
		});
	}

	@Override
	public List<Envelope> listStatus(List<String> envelopeIds) {
		return retryHelper.executeWithRetry(accessToken -> {
			ApiClient apiClient = new ApiClient(config.getBasePath());
			apiClient.addDefaultHeader("Authorization", "Bearer " + accessToken);
			EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
			EnvelopeIdsRequest request = new EnvelopeIdsRequest();
			request.setEnvelopeIds(envelopeIds);
			EnvelopesInformation info = envelopesApi.listStatus(config.getAccountId(), request);
			return info.getEnvelopes() != null ? info.getEnvelopes() : Collections.<Envelope>emptyList();
		});
	}

	@Override
	public byte[] getDocument(String envelopeId, String documentId) {
		return retryHelper.executeWithRetry(accessToken -> {
			ApiClient apiClient = new ApiClient(config.getBasePath());
			apiClient.addDefaultHeader("Authorization", "Bearer " + accessToken);
			EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
			return envelopesApi.getDocument(config.getAccountId(), envelopeId, documentId);
		});
	}
}
