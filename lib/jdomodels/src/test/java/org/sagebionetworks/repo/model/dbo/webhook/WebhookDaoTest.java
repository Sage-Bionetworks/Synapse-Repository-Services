package org.sagebionetworks.repo.model.dbo.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.webhook.Webhook;
import org.sagebionetworks.repo.model.webhook.WebhookObjectType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class WebhookDaoTest {

	@Autowired
	private WebhookDaoImpl webhookDao;

	@Autowired
	private WebhookVerificationDao webhookVerificationDao;

	private Webhook webhook;
	private Webhook anotherWebhook;
	private String userIdAsString = String.valueOf(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
	private String anotherUserId = String.valueOf(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
	private String objectId = "123";
	private WebhookObjectType webhookObjectType = WebhookObjectType.ENTITY;
	private String invokeEndpoint = "https://abcd1234.execute-api.us-east-1.amazonaws.com/prod";
	private String anotherInvokeEndpoint = "https://wxyz6789.execute-api.us-east-1.amazonaws.com/dev";

	@BeforeEach
	public void before() {
		webhookDao.truncateAll();
		webhookVerificationDao.truncateAll();

		webhook = new Webhook().setObjectId(objectId).setObjectType(webhookObjectType).setUserId(userIdAsString)
				.setInvokeEndpoint(invokeEndpoint).setIsVerified(false).setIsWebhookEnabled(true)
				.setIsAuthenticationEnabled(true).setCreatedBy(userIdAsString).setModifiedBy(userIdAsString);

		anotherWebhook = new Webhook().setObjectId(objectId).setObjectType(webhookObjectType).setUserId(anotherUserId)
				.setInvokeEndpoint(anotherInvokeEndpoint).setIsVerified(false).setIsWebhookEnabled(true)
				.setIsAuthenticationEnabled(true).setCreatedBy(anotherUserId).setModifiedBy(anotherUserId);
	}

	@AfterEach
	public void after() {
		webhookDao.truncateAll();
		webhookVerificationDao.truncateAll();
	}

	// TODO Finish the integration testing for WebhookDao.

	@Test
	public void testCreateWebhook() {
		// Call under test
		Webhook result = webhookDao.createWebhook(webhook);

		assertNotNull(result.getWebhookId());
		assertNotNull(result.getEtag());
		assertNotNull(result.getCreatedOn());
		assertNotNull(result.getModifiedOn());

		webhook.setWebhookId(result.getWebhookId());
		webhook.setEtag(result.getEtag());
		webhook.setCreatedOn(result.getCreatedOn());
		webhook.setModifiedOn(result.getModifiedOn());

		assertEquals(webhook, result);
	}

	@Test
	public void testGetWebhook() {
		Webhook one = webhookDao.createWebhook(webhook);
		Webhook two = webhookDao.createWebhook(anotherWebhook);

		assertNotEquals(one, two);
		assertEquals(one, webhookDao.getWebhook(one.getWebhookId()));
		assertEquals(two, webhookDao.getWebhook(two.getWebhookId()));
	}

	@Test
	public void testGetWebhookForUpdate() {
		Webhook one = webhookDao.createWebhook(webhook);
		Webhook two = webhookDao.createWebhook(anotherWebhook);

		assertNotEquals(one, two);
		assertEquals(one, webhookDao.getWebhookForUpdate(one.getWebhookId()));
		assertEquals(two, webhookDao.getWebhookForUpdate(two.getWebhookId()));
	}

	@Test
	public void testDeleteWebhook() {
		Webhook one = webhookDao.createWebhook(webhook);
		String webhookId = one.getWebhookId();

		// Call under test
		webhookDao.deleteWebhook(webhookId);

		assertThrows(NotFoundException.class, () -> {
			webhookDao.deleteWebhook(webhookId);
		});

		webhookDao.createWebhook(webhook);
	}

	@Test
	public void testListUserWebhooks() {
		for (int i = 0; i < 10; i++) {
			webhook.setObjectId(String.valueOf(i));
			webhookDao.createWebhook(webhook);
		}

		List<Webhook> result = webhookDao.listUserWebhooks(Long.valueOf(userIdAsString), NextPageToken.DEFAULT_LIMIT,
				NextPageToken.DEFAULT_OFFSET);

		assertEquals(10, result.size());
	}

}
