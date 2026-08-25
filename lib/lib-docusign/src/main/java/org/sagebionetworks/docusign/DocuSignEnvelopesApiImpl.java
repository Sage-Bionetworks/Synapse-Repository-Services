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
import com.docusign.esign.model.Recipients;

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
			// The status endpoint only reads the envelope ids from the request body when the
			// envelope_ids query parameter is set to "request_body"; otherwise DocuSign returns 400.
			EnvelopesApi.ListStatusOptions options = envelopesApi.new ListStatusOptions();
			options.setEnvelopeIds("request_body");
			EnvelopesInformation info = envelopesApi.listStatus(config.getAccountId(), request, options);
			return info.getEnvelopes() != null ? info.getEnvelopes() : Collections.<Envelope>emptyList();
		});
	}

	@Override
	public void updateRecipients(String envelopeId, Recipients recipients, boolean resend) {
		retryHelper.executeWithRetry(accessToken -> {
			ApiClient apiClient = new ApiClient(config.getBasePath());
			apiClient.addDefaultHeader("Authorization", "Bearer " + accessToken);
			EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
			EnvelopesApi.UpdateRecipientsOptions options = envelopesApi.new UpdateRecipientsOptions();
			options.setResendEnvelope(Boolean.toString(resend));
			return envelopesApi.updateRecipients(config.getAccountId(), envelopeId, recipients, options);
		});
	}

	@Override
	public void createRecipients(String envelopeId, Recipients recipients, boolean resend) {
		retryHelper.executeWithRetry(accessToken -> {
			ApiClient apiClient = new ApiClient(config.getBasePath());
			apiClient.addDefaultHeader("Authorization", "Bearer " + accessToken);
			EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
			EnvelopesApi.CreateRecipientOptions options = envelopesApi.new CreateRecipientOptions();
			options.setResendEnvelope(Boolean.toString(resend));
			return envelopesApi.createRecipient(config.getAccountId(), envelopeId, recipients, options);
		});
	}

	@Override
	public void deleteRecipients(String envelopeId, Recipients recipients) {
		retryHelper.executeWithRetry(accessToken -> {
			ApiClient apiClient = new ApiClient(config.getBasePath());
			apiClient.addDefaultHeader("Authorization", "Bearer " + accessToken);
			EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
			return envelopesApi.deleteRecipients(config.getAccountId(), envelopeId, recipients);
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
