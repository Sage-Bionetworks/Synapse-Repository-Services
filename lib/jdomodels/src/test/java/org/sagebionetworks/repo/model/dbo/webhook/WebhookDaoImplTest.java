package org.sagebionetworks.repo.model.dbo.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.webhook.CreateOrUpdateWebhookRequest;
import org.sagebionetworks.repo.model.webhook.SynapseEventType;
import org.sagebionetworks.repo.model.webhook.SynapseObjectType;
import org.sagebionetworks.repo.model.webhook.Webhook;
import org.sagebionetworks.repo.model.webhook.WebhookVerificationStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class WebhookDaoImplTest {

	@Autowired
	private WebhookDaoImpl webhookDao;
	
	private Long userId = AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
	private Long otherUserId = AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId();

	@BeforeEach
	public void before() {
		webhookDao.truncateAll();

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
		CreateOrUpdateWebhookRequest request = new CreateOrUpdateWebhookRequest()
			.setObjectId("123")
			.setObjectType(SynapseObjectType.ENTITY)
			.setEventTypes(Set.of(SynapseEventType.CREATE, SynapseEventType.UPDATE))
			.setInvokeEndpoint("https://my.webhook.endpoint/callme")
			.setIsEnabled(true);
			
		Webhook expected = new Webhook()
			.setCreatedBy(userId.toString())
			.setObjectId("123")
			.setObjectType(SynapseObjectType.ENTITY)
			.setEventTypes(new TreeSet<>(Set.of(SynapseEventType.CREATE, SynapseEventType.UPDATE)))
			.setInvokeEndpoint("https://my.webhook.endpoint/callme")
			.setIsEnabled(true);
		
		// Call under test
		Webhook webhook = webhookDao.createWebhook(userId, request);
				
		expected
			.setId(webhook.getId())
			.setCreatedOn(webhook.getCreatedOn())
			.setModifiedOn(webhook.getModifiedOn())
			.setVerificationStatus(WebhookVerificationStatus.PENDING);
		
		assertEquals(expected, webhook);
		
		// Another user cannot reuse the same endpoint on the same object
		String errMsg = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			webhookDao.createWebhook(otherUserId, request);
		}).getMessage();
		
		assertEquals("The same invokeEndpoint cannot be used for the same object.", errMsg);
	}
	
	@Test
	public void testUpdateWebhook() throws InterruptedException {
		CreateOrUpdateWebhookRequest request = new CreateOrUpdateWebhookRequest()
			.setObjectId("123")
			.setObjectType(SynapseObjectType.ENTITY)
			.setEventTypes(Set.of(SynapseEventType.CREATE, SynapseEventType.UPDATE))
			.setInvokeEndpoint("https://my.webhook.endpoint/callme")
			.setIsEnabled(true);
		
		String errMsg = assertThrows(IllegalStateException.class, () -> {
			// Call under test
			webhookDao.updateWebhook("123", request);
		}).getMessage();
		
		assertEquals("A webhook with id 123 does not exist.", errMsg);
		
		Webhook webhook = webhookDao.createWebhook(userId, request);
		
		Thread.sleep(50L);
		
		// Call under test
		Webhook updated = webhookDao.updateWebhook(webhook.getId(), request.setEventTypes(Set.of(SynapseEventType.DELETE)));
		
		assertNotEquals(webhook, updated);
		
		webhook.setEventTypes(Set.of(SynapseEventType.DELETE)).setModifiedOn(updated.getModifiedOn());
		
		assertEquals(webhook, updated);
		
		Webhook anotherWebhook = webhookDao.createWebhook(otherUserId, request.setObjectId("456"));
		
		// Another user cannot reuse the same endpoint on the same object
		errMsg = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			webhookDao.updateWebhook(anotherWebhook.getId(), request.setObjectId("123"));
		}).getMessage();
		
		assertEquals("The same invokeEndpoint cannot be used for the same object.", errMsg);		
	}
	
	@Test
	public void testGetWebhook() {
		// Call under test
		assertTrue(webhookDao.getWebhook("-123").isEmpty());
		
		CreateOrUpdateWebhookRequest request = new CreateOrUpdateWebhookRequest()
			.setObjectId("123")
			.setObjectType(SynapseObjectType.ENTITY)
			.setEventTypes(Set.of(SynapseEventType.CREATE, SynapseEventType.UPDATE))
			.setInvokeEndpoint("https://my.webhook.endpoint/callme")
			.setIsEnabled(true);
			
		Webhook webhook = webhookDao.createWebhook(userId, request);
		
		assertEquals(webhook, webhookDao.getWebhook(webhook.getId()).get());
	}
	
	@Test
	public void testDeleteWebhook() {
		// Call under test
		webhookDao.deleteWebhook("-123");
		
		CreateOrUpdateWebhookRequest request = new CreateOrUpdateWebhookRequest()
			.setObjectId("123")
			.setObjectType(SynapseObjectType.ENTITY)
			.setEventTypes(Set.of(SynapseEventType.CREATE, SynapseEventType.UPDATE))
			.setInvokeEndpoint("https://my.webhook.endpoint/callme")
			.setIsEnabled(true);
			
		Webhook webhook = webhookDao.createWebhook(userId, request);
		
		// Call under test
		webhookDao.deleteWebhook(webhook.getId());
		
		assertTrue(webhookDao.getWebhook(webhook.getId()).isEmpty());
	}
	
	@Test
	public void testListUserWebhooks() {
		assertTrue(webhookDao.listUserWebhooks(userId, 10, 0).isEmpty());
		
		List<Webhook> expected = new ArrayList<>();
		
		for (int i=0; i<10; i++) {
			CreateOrUpdateWebhookRequest request = new CreateOrUpdateWebhookRequest()
				.setObjectId(String.valueOf(i))
				.setObjectType(SynapseObjectType.ENTITY)
				.setEventTypes(Set.of(SynapseEventType.CREATE, SynapseEventType.UPDATE))
				.setInvokeEndpoint("https://my.webhook.endpoint/callme")
				.setIsEnabled(true);
				
			expected.add(webhookDao.createWebhook(userId, request));
		}
		
		assertEquals(expected, webhookDao.listUserWebhooks(userId, 10, 0));
		
		assertEquals(expected.subList(0, 5), webhookDao.listUserWebhooks(userId, 5, 0));
		assertEquals(expected.subList(5, 10), webhookDao.listUserWebhooks(userId, 5, 5));
	}

}
