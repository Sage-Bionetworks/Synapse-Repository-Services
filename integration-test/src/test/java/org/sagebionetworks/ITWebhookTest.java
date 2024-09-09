package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseBadRequestException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.ErrorResponseCode;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.webhook.CreateOrUpdateWebhookRequest;
import org.sagebionetworks.repo.model.webhook.ListUserWebhooksRequest;
import org.sagebionetworks.repo.model.webhook.SynapseEventType;
import org.sagebionetworks.repo.model.webhook.SynapseObjectType;
import org.sagebionetworks.repo.model.webhook.VerifyWebhookRequest;
import org.sagebionetworks.repo.model.webhook.Webhook;
import org.sagebionetworks.repo.model.webhook.WebhookVerificationStatus;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;

@ExtendWith(ITTestExtension.class)
public class ITWebhookTest {
	
	private static final int TIMEOUT = 60_000;
	
	private SynapseClient client;

	private Project project;
	
	public ITWebhookTest(SynapseClient client) {
		this.client = client;
	}
	
	@BeforeEach
	public void before() throws SynapseException {
		project = client.createEntity(new Project());
	}

	@Test
	public void testRoundTrip() throws Exception {
		
		CreateOrUpdateWebhookRequest createRequest = new CreateOrUpdateWebhookRequest()
			.setObjectId(project.getId())
			.setObjectType(SynapseObjectType.ENTITY)
			.setEventTypes(Set.of(SynapseEventType.UPDATE))
			.setIsEnabled(true)
			.setInvokeEndpoint("https://invalid.sagebase.org/events");
		
		assertEquals(ErrorResponseCode.UNSUPPORTED_WEBHOOK_DOMAIN, assertThrows(SynapseBadRequestException.class, () -> {
			client.createWebhook(createRequest).getId();	
		}).getErrorResponseCode());		
		
		Webhook webhook = client.createWebhook(createRequest.setInvokeEndpoint("https://abc123.execute-api.us-east-1.amazonaws.com/events"));
		
		assertEquals(WebhookVerificationStatus.PENDING, webhook.getVerificationStatus());
		
		String webhookId = webhook.getId();
		
		webhook = TimeUtils.waitFor(TIMEOUT, 1000, () -> {
			Webhook updatedWebhook = client.getWebhook(webhookId);
			return Pair.create(WebhookVerificationStatus.FAILED.equals(updatedWebhook.getVerificationStatus()), updatedWebhook);
		});
		
		assertEquals(List.of(webhook), client.listWebhooks(new ListUserWebhooksRequest()).getPage());
		
		assertEquals("Cannot verify the webhook at this time.", assertThrows(SynapseBadRequestException.class, () -> {
			client.verifyWebhook(webhookId, new VerifyWebhookRequest().setVerificationCode("abcd"));
		}).getMessage());
		
		assertEquals(WebhookVerificationStatus.PENDING, client.generateWebhookVerificationCode(webhookId).getVerificationStatus());		
		
		client.deleteWebhook(webhookId);
	}
}
