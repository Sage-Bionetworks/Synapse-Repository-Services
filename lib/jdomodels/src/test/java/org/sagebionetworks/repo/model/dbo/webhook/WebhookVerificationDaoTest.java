package org.sagebionetworks.repo.model.dbo.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.webhook.Webhook;
import org.sagebionetworks.repo.model.webhook.WebhookObjectType;
import org.sagebionetworks.repo.model.webhook.WebhookVerification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class WebhookVerificationDaoTest {

	@Autowired
	private WebhookVerificationDao webhookVerificationDao;

	@Autowired
	private WebhookDao webhookDao;

	private Date currentDate;
	private Date expirationDate;
	private WebhookVerification webhookVerification;
	private Webhook webhook;
	private String userIdAsString = String.valueOf(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
	private String verificationCode = "AAAAAA";
	private String objectId = "123";
	private WebhookObjectType webhookObjectType = WebhookObjectType.ENTITY;
	private String invokeEndpoint = "https://abcd1234.execute-api.us-east-1.amazonaws.com/prod";

	@BeforeEach
	public void before() {
		webhookDao.truncateAll();
		webhookVerificationDao.truncateAll();
		webhook = new Webhook().setObjectId(objectId).setObjectType(webhookObjectType).setUserId(userIdAsString)
				.setInvokeEndpoint(invokeEndpoint).setIsVerified(false).setIsWebhookEnabled(true)
				.setIsAuthenticationEnabled(true).setCreatedBy(userIdAsString).setModifiedBy(userIdAsString);

		currentDate = new Date(System.currentTimeMillis());
		expirationDate = new Date(currentDate.getTime() + 10 ^ 9);
		webhookVerification = new WebhookVerification().setVerificationCode(verificationCode)
				.setExpiresOn(expirationDate).setAttempts(0L).setCreatedBy(userIdAsString).setCreatedOn(currentDate)
				.setModifiedOn(currentDate).setModifiedBy(userIdAsString);
	}

	@AfterEach
	public void after() {
		webhookDao.truncateAll();
		webhookVerificationDao.truncateAll();
	}

	// TODO Finish the integration testing for WebhookVerificationDao.

	@Test
	public void testCreateWebhookVerification() {
		Webhook created = webhookDao.createWebhook(webhook);
		webhookVerification.setWebhookId(created.getWebhookId());
		WebhookVerification result = webhookVerificationDao.createWebhookVerification(webhookVerification);
		assertEquals(webhookVerification, result);
	}

}
