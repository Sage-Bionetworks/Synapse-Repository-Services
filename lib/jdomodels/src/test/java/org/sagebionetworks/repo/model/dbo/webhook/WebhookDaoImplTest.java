package org.sagebionetworks.repo.model.dbo.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.webhook.CreateOrUpdateWebhookRequest;
import org.sagebionetworks.repo.model.webhook.SynapseEventType;
import org.sagebionetworks.repo.model.webhook.SynapseObjectType;
import org.sagebionetworks.repo.model.webhook.Webhook;
import org.sagebionetworks.repo.model.webhook.WebhookVerificationStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class WebhookDaoImplTest {

	@Autowired
	private WebhookDaoImpl webhookDao;
	
	@Autowired
	private PlatformTransactionManager txManager;
	
	private CreateOrUpdateWebhookRequest cuRequest;
	private TransactionTemplate txTemplate;
	private Long userId = AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
	private Long otherUserId = AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId();
	
	@BeforeEach
	public void before() {
		webhookDao.truncateAll();
		
		cuRequest = new CreateOrUpdateWebhookRequest()
			.setObjectId("123")
			.setObjectType(SynapseObjectType.ENTITY)
			.setEventTypes(Set.of(SynapseEventType.CREATE, SynapseEventType.UPDATE))
			.setInvokeEndpoint("https://my.webhook.endpoint/callme")
			.setIsEnabled(true);
		
		DefaultTransactionDefinition txDefinition = new DefaultTransactionDefinition();

		txDefinition.setIsolationLevel(Connection.TRANSACTION_READ_COMMITTED);
		txDefinition.setReadOnly(false);
		txDefinition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		txDefinition.setTimeout(2);
		
		txTemplate = new TransactionTemplate(txManager, txDefinition);

	}

	@AfterEach
	public void after() {
		webhookDao.truncateAll();
	}
	
	@ParameterizedTest
	@EnumSource(SynapseObjectType.class)
	public void testSynapseObjectTypeEnum(SynapseObjectType type) {
		// Verifies that the SynapseObjectType enum is a subset of the ObjectType
		ObjectType.valueOf(type.name());
	}
	
	@ParameterizedTest
	@EnumSource(WebhookVerificationStatus.class)
	public void testStatusFromString(WebhookVerificationStatus status) {
		assertEquals(status, WebhookDaoImpl.statusFromString(status.name()));
	}
	
	@Test
	public void testStatusFromStringWithNullInput() {
		assertEquals(WebhookVerificationStatus.PENDING, WebhookDaoImpl.statusFromString(null));
	}

	@Test
	public void testEventsToJsonRoundTrip() {
		Set<SynapseEventType> events = new TreeSet<>(List.of(SynapseEventType.UPDATE, SynapseEventType.DELETE, SynapseEventType.CREATE));
		
		String result = WebhookDaoImpl.eventsToJson(events);
		
		assertEquals(events, WebhookDaoImpl.eventsFromJson(result));
	}
	
	@Test
	public void testCreateWebhook() {
			
		Webhook expected = new Webhook()
			.setCreatedBy(userId.toString())
			.setObjectId("123")
			.setObjectType(SynapseObjectType.ENTITY)
			.setEventTypes(new TreeSet<>(Set.of(SynapseEventType.CREATE, SynapseEventType.UPDATE)))
			.setInvokeEndpoint("https://my.webhook.endpoint/callme")
			.setIsEnabled(true);
		
		// Call under test
		Webhook webhook = webhookDao.createWebhook(userId, cuRequest);
				
		expected
			.setId(webhook.getId())
			.setCreatedOn(webhook.getCreatedOn())
			.setModifiedOn(webhook.getModifiedOn())
			.setVerificationStatus(WebhookVerificationStatus.PENDING);
		
		assertEquals(expected, webhook);
		
		// A default verification should have been created
		assertNull(webhookDao.getWebhookVerification(webhook.getId()).getCode());
		
		// Another user cannot reuse the same endpoint on the same object
		String errMsg = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			webhookDao.createWebhook(otherUserId, cuRequest);
		}).getMessage();
		
		assertEquals("The same invokeEndpoint cannot be used for the same object.", errMsg);
	}
	
	@Test
	public void testUpdateWebhook() throws InterruptedException {
		
		String errMsg = assertThrows(IllegalStateException.class, () -> {
			// Call under test
			webhookDao.updateWebhook("123", cuRequest);
		}).getMessage();
		
		assertEquals("The webhook was not updated.", errMsg);
		
		Webhook webhook = webhookDao.createWebhook(userId, cuRequest);
		
		Thread.sleep(50L);
		
		// Call under test
		Webhook updated = webhookDao.updateWebhook(webhook.getId(), cuRequest.setEventTypes(Set.of(SynapseEventType.DELETE)));
		
		assertNotEquals(webhook, updated);
		
		webhook.setEventTypes(Set.of(SynapseEventType.DELETE)).setModifiedOn(updated.getModifiedOn());
		
		assertEquals(webhook, updated);
		
		Webhook anotherWebhook = webhookDao.createWebhook(otherUserId, cuRequest.setObjectId("456"));
		
		// Another user cannot reuse the same endpoint on the same object
		errMsg = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			webhookDao.updateWebhook(anotherWebhook.getId(), cuRequest.setObjectId("123"));
		}).getMessage();
		
		assertEquals("The same invokeEndpoint cannot be used for the same object.", errMsg);		
	}
	
	@Test
	public void testGetWebhook() {
		boolean forUpdate = false;
		
		// Call under test
		assertTrue(webhookDao.getWebhook("-123", forUpdate).isEmpty());
			
		Webhook webhook = webhookDao.createWebhook(userId, cuRequest);
		
		txTemplate.executeWithoutResult( txStatus -> {
			// Call under test
			assertEquals(webhook, webhookDao.getWebhook(webhook.getId(), forUpdate).get());
			
			txTemplate.executeWithoutResult( tx2Status -> {
				// this is not blocked
				webhookDao.updateWebhook(webhook.getId(), cuRequest.setIsEnabled(false));				
			});
		});
	}
	
	@Test
	public void testGetWebhookForUpdate() {
		boolean forUpdate = true;
		
		// Call under test
		assertTrue(webhookDao.getWebhook("-123", forUpdate).isEmpty());
			
		Webhook webhook = webhookDao.createWebhook(userId, cuRequest);
		
		txTemplate.executeWithoutResult( txStatus -> {
			// Call under test
			assertEquals(webhook, webhookDao.getWebhook(webhook.getId(), forUpdate).get());
			
			assertThrows(QueryTimeoutException.class, () -> {				
				txTemplate.executeWithoutResult( tx2Status -> {
					// this is blocked
					webhookDao.updateWebhook(webhook.getId(), cuRequest.setIsEnabled(false));				
				});
			});
		});
	}
	
	@Test
	public void testDeleteWebhook() {
		// Call under test
		webhookDao.deleteWebhook("-123");
					
		Webhook webhook = webhookDao.createWebhook(userId, cuRequest);
		
		// Call under test
		webhookDao.deleteWebhook(webhook.getId());
		
		assertTrue(webhookDao.getWebhook(webhook.getId(), false).isEmpty());
	}
	
	@Test
	public void testListUserWebhooks() {
		assertTrue(webhookDao.listUserWebhooks(userId, 10, 0).isEmpty());
		
		List<Webhook> expected = new ArrayList<>();
		
		for (int i=0; i<10; i++) {
			expected.add(webhookDao.createWebhook(userId, cuRequest.setObjectId(String.valueOf(i))));
		}
		
		assertEquals(expected, webhookDao.listUserWebhooks(userId, 10, 0));
		
		assertEquals(expected.subList(0, 5), webhookDao.listUserWebhooks(userId, 5, 0));
		assertEquals(expected.subList(5, 10), webhookDao.listUserWebhooks(userId, 5, 5));
	}
	
	@Test
	public void testListWebhooksForObjectIds() {
		assertTrue(webhookDao.listWebhooksForObjectIds(List.of(1L, 2L), SynapseObjectType.ENTITY, SynapseEventType.CREATE, 10, 0).isEmpty());
		
		List<Long> objectIds = new ArrayList<>();
		List<Webhook> webhooks = new ArrayList<>();
		
		for (int i=0; i<10; i++) {
			Long objectId = (long) i;
			objectIds.add(objectId);
			webhooks.add(webhookDao.createWebhook(userId, cuRequest.setObjectId(objectId.toString())));
		}
		
		// Initially no results since the webhook is not verified
		assertEquals(Collections.emptyList(), webhookDao.listWebhooksForObjectIds(objectIds, SynapseObjectType.ENTITY, SynapseEventType.CREATE, 10, 0));
		
		// Verifies all the webhooks
		webhooks.forEach(webhook -> {
			webhookDao.setWebhookVerificationStatus(webhook.getId(), WebhookVerificationStatus.VERIFIED, null);
			webhook.setVerificationStatus(WebhookVerificationStatus.VERIFIED);
		});
		
		assertEquals(webhooks, webhookDao.listWebhooksForObjectIds(objectIds, SynapseObjectType.ENTITY, SynapseEventType.CREATE, 10, 0));
		assertEquals(webhooks.subList(0, 5), webhookDao.listWebhooksForObjectIds(objectIds, SynapseObjectType.ENTITY, SynapseEventType.CREATE, 5, 0));
		assertEquals(webhooks.subList(5, 10), webhookDao.listWebhooksForObjectIds(objectIds, SynapseObjectType.ENTITY, SynapseEventType.CREATE, 5, 5));
		
		// No webhook with delete
		assertEquals(Collections.emptyList(), webhookDao.listWebhooksForObjectIds(objectIds, SynapseObjectType.ENTITY, SynapseEventType.DELETE, 10, 0));
		
	}

	@Test
	public void testGetWebhookVerification() {
		assertThrows(IllegalStateException.class, () -> {
			webhookDao.getWebhookVerification("123");
		});
		
		Webhook webhook = webhookDao.createWebhook(userId, cuRequest);
		
		DBOWebhookVerification expected = new DBOWebhookVerification()
				.setWebhookId(Long.valueOf(webhook.getId()))
				.setCode(null)
				.setCodeExpiresOn(null)
				.setCodeMessageId(null)
				.setStatus(WebhookVerificationStatus.PENDING.name())
				.setMessage(null);
		
		DBOWebhookVerification verification = webhookDao.getWebhookVerification(webhook.getId());
		
		assertNotNull(verification.getModifiedOn());
		assertNotNull(verification.getEtag());
		
		expected.setEtag(verification.getEtag());
		expected.setModifiedOn(verification.getModifiedOn());
		
		assertEquals(expected, verification);
	}
	
	@Test
	public void testSetWebhookVerificationCode() {
		
		assertThrows(IllegalStateException.class, () -> {
			// Call under test
			webhookDao.setWebhookVerificationCode("123", "123456", Instant.now());			
		});
					
		Webhook webhook = webhookDao.createWebhook(userId, cuRequest);
		
		// Set some other status and message
		webhookDao.setWebhookVerificationStatus(webhook.getId(), WebhookVerificationStatus.FAILED, "Failed");
				
		DBOWebhookVerification verification = webhookDao.getWebhookVerification(webhook.getId());
		
		String currentEtag = verification.getEtag();
		
		Instant expiration = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		
		DBOWebhookVerification expected = new DBOWebhookVerification()
			.setWebhookId(Long.valueOf(webhook.getId()))
			.setCode("123456")
			.setCodeExpiresOn(Timestamp.from(expiration))
			.setStatus(WebhookVerificationStatus.PENDING.name())
			.setMessage(null);
		
		// Call under test
		verification = webhookDao.setWebhookVerificationCode(webhook.getId(), "123456", expiration);
		
		assertNotNull(verification.getModifiedOn());
		assertNotNull(verification.getCodeMessageId());
		assertNotEquals(currentEtag, verification.getEtag());
		
		expected.setEtag(verification.getEtag());
		expected.setModifiedOn(verification.getModifiedOn());
		expected.setCodeMessageId(verification.getCodeMessageId());
		
		assertEquals(expected, verification);
	}
		
	@ParameterizedTest
	@EnumSource(WebhookVerificationStatus.class)
	public void testSetWebhookVerificationStatus(WebhookVerificationStatus status) {
		
		assertThrows(IllegalStateException.class, () -> {
			// Call under test
			webhookDao.setWebhookVerificationStatus("123", status, "message");
		});
					
		Webhook webhook = webhookDao.createWebhook(userId, cuRequest);
		DBOWebhookVerification verification = webhookDao.getWebhookVerification(webhook.getId());
		
		String currentEtag = verification.getEtag();
				
		DBOWebhookVerification expected = new DBOWebhookVerification()
			.setWebhookId(Long.valueOf(webhook.getId()))
			.setCode(null)
			.setCodeExpiresOn(null)
			.setCodeMessageId(null)
			.setStatus(status.name())
			.setMessage("message");
		
		// Call under test
		webhookDao.setWebhookVerificationStatus(webhook.getId(), status, "message");
		
		verification = webhookDao.getWebhookVerification(webhook.getId());
		
		assertNotNull(verification.getModifiedOn());
		assertNotEquals(currentEtag, verification.getEtag());
		
		expected.setEtag(verification.getEtag());
		expected.setModifiedOn(verification.getModifiedOn());
		
		assertEquals(expected, verification);
	}
	
	@Test
	public void testGetWebhookVerificationStatus() {
		String messageId = "messageId";
		// Call under test
		assertEquals(Optional.empty(), webhookDao.getWebhookVerificationStatus("123", messageId));
		
		Webhook webhook = webhookDao.createWebhook(userId, cuRequest);
		
		// Call under test
		assertEquals(Optional.empty(), webhookDao.getWebhookVerificationStatus(webhook.getId(), messageId));
		
		messageId = webhookDao.setWebhookVerificationCode(webhook.getId(), "1234", Instant.now()).getCodeMessageId();
		
		// Call under test
		assertEquals(Optional.of(WebhookVerificationStatus.PENDING), webhookDao.getWebhookVerificationStatus(webhook.getId(), messageId));
	}

	@ParameterizedTest
	@EnumSource(WebhookVerificationStatus.class)
	public void testSetWebhookVerificationStatusIfMessageIdMatch(WebhookVerificationStatus status) {
		String messageId = "messageId";
		
		// Call under test
		webhookDao.setWebhookVerificationStatusIfMessageIdMatch("123", messageId, status, "message");
					
		Webhook webhook = webhookDao.createWebhook(userId, cuRequest);
		
		Instant expiresOn = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		
		messageId = webhookDao.setWebhookVerificationCode(webhook.getId(), "12345", expiresOn).getCodeMessageId();
		
		DBOWebhookVerification verification = webhookDao.getWebhookVerification(webhook.getId());
		
		String currentEtag = verification.getEtag();
				
		DBOWebhookVerification expected = new DBOWebhookVerification()
			.setWebhookId(Long.valueOf(webhook.getId()))
			.setCode("12345")
			.setCodeExpiresOn(Timestamp.from(expiresOn))
			.setCodeMessageId(messageId)
			.setStatus(status.name())
			.setMessage("message");
		
		// Call under test
		webhookDao.setWebhookVerificationStatusIfMessageIdMatch(webhook.getId(), messageId, status, "message");
		
		verification = webhookDao.getWebhookVerification(webhook.getId());
		
		assertNotNull(verification.getModifiedOn());
		assertNotEquals(currentEtag, verification.getEtag());
		
		expected.setEtag(verification.getEtag());
		expected.setModifiedOn(verification.getModifiedOn());
		
		assertEquals(expected, verification);

		// Call under test
		webhookDao.setWebhookVerificationStatusIfMessageIdMatch(webhook.getId(), "wrongMessageId", status, "message");
		
		// Makes sure nothing changed
		assertEquals(verification, webhookDao.getWebhookVerification(webhook.getId()));
	}

	
	@Test
	public void testMapWebhookId() {
		assertEquals(123L, WebhookDaoImpl.mapWebhookId("123"));
	}
	
	@ParameterizedTest
	@ValueSource(strings = {"123.0", "syn123", "abc"})
	public void testMapWebhookIdWithInvalidId(String invalidId) {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			WebhookDaoImpl.mapWebhookId(invalidId);
		}).getMessage();
		
		assertEquals("Invalid webhook id.", message);
	}
	
}
