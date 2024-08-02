package org.sagebionetworks.repo.manager.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dbo.webhook.WebhookDao;
import org.sagebionetworks.repo.model.webhook.CreateOrUpdateWebhookRequest;
import org.sagebionetworks.repo.model.webhook.SynapseEventType;
import org.sagebionetworks.repo.model.webhook.SynapseObjectType;
import org.sagebionetworks.repo.model.webhook.Webhook;
import org.sagebionetworks.repo.model.webhook.WebhookVerificationStatus;
import org.sagebionetworks.repo.web.NotFoundException;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;

@ExtendWith(MockitoExtension.class)
public class WebhookManagerUnitTest {

	@Mock
	private WebhookDao mockWebhookDao;

	@Mock
	private AccessControlListDAO mockAclDao;
	
	@Mock
	private AmazonSQSClient mockSqsClient;
	
	@Mock
	private StackConfiguration mockStackConfig;

	@InjectMocks
	@Spy
	private WebhookManagerImpl webhookManager;
	
	private UserInfo userInfo;

	private CreateOrUpdateWebhookRequest request;
	
	private Webhook webhook;
	
	@BeforeEach
	public void before() {
		userInfo = new UserInfo(false, 321L);
		
		request = new CreateOrUpdateWebhookRequest()
			.setObjectType(SynapseObjectType.ENTITY)
			.setObjectId("123")
			.setEventTypes(Set.of(SynapseEventType.CREATE, SynapseEventType.UPDATE))
			.setInvokeEndpoint("https://my.endpoint.org/events")
			.setIsEnabled(true);
		
		webhook = new Webhook()
			.setId("456")
			.setCreatedBy(userInfo.getId().toString())
			.setEventTypes(request.getEventTypes())
			.setObjectType(request.getObjectType())
			.setObjectId(request.getObjectId())
			.setInvokeEndpoint(request.getInvokeEndpoint())
			.setIsEnabled(request.getIsEnabled())
			.setVerificationStatus(WebhookVerificationStatus.PENDING);
		
		when(mockStackConfig.getQueueName("WEBHOOK_MESSAGE")).thenReturn("queuName");
		
		GetQueueUrlResult res = new GetQueueUrlResult();
		res.setQueueUrl("queueUrl");
		
		when(mockSqsClient.getQueueUrl("queuName")).thenReturn(res);
		
		// This is automatically invoked by spring
		webhookManager.configureMessageQueueUrl(mockStackConfig);
	}
	
	@Test
	public void testValidateCreateOrUpdateWebhookRequest() {
		when(mockAclDao.canAccess(userInfo, "123", ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.authorized());
				
		// Call under test
		webhookManager.validateCreateOrUpdateRequest(userInfo, request);
		
	}
	
	@Test
	public void testValidateCreateOrUpdateWebhookRequestWithNoAccess() {
		when(mockAclDao.canAccess(userInfo, "123", ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.accessDenied("denied"));
				
		assertThrows(UnauthorizedException.class, () -> {			
			// Call under test
			webhookManager.validateCreateOrUpdateRequest(userInfo, request);
		});
	}
	
	@Test
	public void testValidateCreateOrUpdateWebhookRequestWithAnonymous() {
		userInfo = new UserInfo(false, AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());		
				
		assertThrows(UnauthorizedException.class, () -> {			
			// Call under test
			webhookManager.validateCreateOrUpdateRequest(userInfo, request);
		});
		
		verifyZeroInteractions(mockAclDao);
	}
	
	@Test
	public void testValidateCreateOrUpdateWebhookRequestWithAdmin() {
		userInfo = new UserInfo(true, 123L);
					
		// Call under test
		webhookManager.validateCreateOrUpdateRequest(userInfo, request);
		
		verifyZeroInteractions(mockAclDao);
	}
	
	@Test
	public void testValidateCreateOrUpdateWebhookRequestWithNoObjectType() {
		
		request.setObjectType(null);
			
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.validateCreateOrUpdateRequest(userInfo, request);
		}).getMessage();
		
		assertEquals("The objectType is required.", result);
		
		verifyZeroInteractions(mockAclDao);
	}
	
	@Test
	public void testValidateCreateOrUpdateWebhookRequestWithNoEvents() {
		
		request.setEventTypes(Collections.emptySet());
			
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.validateCreateOrUpdateRequest(userInfo, request);
		}).getMessage();
		
		assertEquals("The eventTypes is required and must not be empty.", result);
		
		verifyZeroInteractions(mockAclDao);
	}
	
	@Test
	public void testValidateCreateOrUpdateWebhookRequestWithNoObjectId() {
		
		request.setObjectId(null);
			
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.validateCreateOrUpdateRequest(userInfo, request);
		}).getMessage();
		
		assertEquals("The objectId is required.", result);
		
		verifyZeroInteractions(mockAclDao);
	}
	
	@Test
	public void testValidateCreateOrUpdateWebhookRequestWithNoIsEnabled() {
		
		request.setIsEnabled(null);
			
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.validateCreateOrUpdateRequest(userInfo, request);
		}).getMessage();
		
		assertEquals("isEnabled is required.", result);
		
		verifyZeroInteractions(mockAclDao);
	}
	
	@Test
	public void testValidateCreateOrUpdateWebhookRequestWithNoEndpoint() {
		
		request.setInvokeEndpoint(null);
			
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.validateCreateOrUpdateRequest(userInfo, request);
		}).getMessage();
		
		assertEquals("The invokeEndpoint is not a valid url: null", result);
		
		verifyZeroInteractions(mockAclDao);
	}
	
	@Test
	public void testValidateCreateOrUpdateWebhookRequestWithInvalidEndpoint() {
		
		request.setInvokeEndpoint("https://not.valid");
			
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.validateCreateOrUpdateRequest(userInfo, request);
		}).getMessage();
		
		assertEquals("The invokeEndpoint is not a valid url: https://not.valid", result);
		
		verifyZeroInteractions(mockAclDao);
	}
	
	@Test
	public void testValidateCreateOrUpdateWebhookRequestWithLocalEndpoint() {
		
		request.setInvokeEndpoint("https://localhost/events");
			
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.validateCreateOrUpdateRequest(userInfo, request);
		}).getMessage();
		
		assertEquals("The invokeEndpoint is not a valid url: https://localhost/events", result);
		
		verifyZeroInteractions(mockAclDao);
	}
	
	@Test
	public void testValidateCreateOrUpdateWebhookRequestWithQuery() {
		
		request.setInvokeEndpoint("https://my.webhook.org/events?a=b");
			
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.validateCreateOrUpdateRequest(userInfo, request);
		}).getMessage();
		
		assertEquals("The invokedEndpoint only supports https and cannot contain a port, query or fragment", result);
		
		verifyZeroInteractions(mockAclDao);
	}
	
	@Test
	public void testValidateCreateOrUpdateWebhookRequestWithPort() {
		
		request.setInvokeEndpoint("https://my.webhook.org:533/events");
			
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.validateCreateOrUpdateRequest(userInfo, request);
		}).getMessage();
		
		assertEquals("The invokedEndpoint only supports https and cannot contain a port, query or fragment", result);
		
		verifyZeroInteractions(mockAclDao);
	}
	
	@Test
	public void testValidateCreateOrUpdateWebhookRequestWithFragment() {
		
		request.setInvokeEndpoint("https://my.webhook.org/events#fragment");
			
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.validateCreateOrUpdateRequest(userInfo, request);
		}).getMessage();
		
		assertEquals("The invokedEndpoint only supports https and cannot contain a port, query or fragment", result);
		
		verifyZeroInteractions(mockAclDao);
	}
	
	@Test
	public void testCreateWebhook() {
		doNothing().when(webhookManager).validateCreateOrUpdateRequest(userInfo, request);
		doNothing().when(webhookManager).generateAndSendVerificationCode(webhook);
		
		when(mockWebhookDao.createWebhook(userInfo.getId(), request)).thenReturn(webhook);
		
		// Call under test
		assertEquals(webhook, webhookManager.createWebhook(userInfo, request));
	}
	
	@Test	
	public void testGetWebhookWithForUpdateFalse() {
		when(mockWebhookDao.getWebhook(webhook.getId(), false)).thenReturn(Optional.of(webhook));
		
		// Call under test
		assertEquals(webhook, webhookManager.getWebhook(userInfo, webhook.getId(), false));
	}
	
	@Test	
	public void testGetWebhookWithForUpdateTrue() {
		when(mockWebhookDao.getWebhook(webhook.getId(), true)).thenReturn(Optional.of(webhook));
		
		// Call under test
		assertEquals(webhook, webhookManager.getWebhook(userInfo, webhook.getId(), true));
	}
	
	@Test	
	public void testGetWebhookWithForUpdateAndNotFound() {
		when(mockWebhookDao.getWebhook(webhook.getId(), true)).thenReturn(Optional.empty());
		
		String result = assertThrows(NotFoundException.class, () -> {			
			// Call under test
			webhookManager.getWebhook(userInfo, webhook.getId(), true);
		}).getMessage();
		
		assertEquals("A webhook with the given id does not exist.", result);
	}
	
	@Test	
	public void testGetWebhookWithForUpdateAndNotCreator() {
		webhook.setCreatedBy("1");
		
		when(mockWebhookDao.getWebhook(webhook.getId(), true)).thenReturn(Optional.of(webhook));
		
		String result = assertThrows(UnauthorizedException.class, () -> {			
			// Call under test
			webhookManager.getWebhook(userInfo, webhook.getId(), true);
		}).getMessage();
		
		assertEquals("You are not authorized to access this resource.", result);
	}
	
	@Test	
	public void testGetWebhookWithForUpdateAndNotCreatorAndAdmin() {
		userInfo = new UserInfo(true, 123L);
		
		webhook.setCreatedBy("1");
		
		when(mockWebhookDao.getWebhook(webhook.getId(), true)).thenReturn(Optional.of(webhook));
					
		// Call under test
		assertEquals(webhook, webhookManager.getWebhook(userInfo, webhook.getId(), true));
	}
	
	@Test	
	public void testGetWebhookWithForUpdateWithNoUser() {
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.getWebhook(null, webhook.getId(), false);
		}).getMessage();
		
		assertEquals("The userInfo is required.", result);
	}
	
	@Test	
	public void testGetWebhookWithForUpdateWithNoId() {
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.getWebhook(userInfo, null, false);
		}).getMessage();
		
		assertEquals("The webhookId is required and must not be the empty string.", result);
	}
	
	@Test
	public void testGetWebhook() {
		doReturn(webhook).when(webhookManager).getWebhook(userInfo, webhook.getId(), false);
		
		// Call under test
		assertEquals(webhook, webhookManager.getWebhook(userInfo, webhook.getId()));
	}
	
	@Test
	public void testUpdateWebhook() {
		doNothing().when(webhookManager).validateCreateOrUpdateRequest(userInfo, request);
		doReturn(webhook).when(webhookManager).getWebhook(userInfo, webhook.getId(), true);
		when(mockWebhookDao.updateWebhook(webhook.getId(), request)).thenReturn(webhook);
		
		// Call under test
		assertEquals(webhook, webhookManager.updateWebhook(userInfo, webhook.getId(), request));
		
		verify(webhookManager, never()).generateAndSendVerificationCode(any());
	}
	
	@Test
	public void testUpdateWebhookWithUpdatedEndpoint() {
		request.setInvokeEndpoint("https://another.endpoint.org");
		
		Webhook updatedWebhook = new Webhook().setInvokeEndpoint(request.getInvokeEndpoint());
		
		doNothing().when(webhookManager).validateCreateOrUpdateRequest(userInfo, request);
		doNothing().when(webhookManager).generateAndSendVerificationCode(updatedWebhook);
		
		doReturn(webhook).when(webhookManager).getWebhook(userInfo, webhook.getId(), true);
		
		// Call under test
		when(mockWebhookDao.updateWebhook(webhook.getId(), request)).thenReturn(updatedWebhook);
		
		assertEquals(updatedWebhook, webhookManager.updateWebhook(userInfo, webhook.getId(), request));		
	}
	
	@Test
	public void testDeleteWebhook() {
		doReturn(webhook).when(webhookManager).getWebhook(userInfo, webhook.getId(), true);
		
		// Call under test
		webhookManager.deleteWebhook(userInfo, webhook.getId());
		
		verify(mockWebhookDao).deleteWebhook(webhook.getId());
	}
	
	@Test
	public void testDeleteWebhookWithNoUser() {
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.deleteWebhook(null, webhook.getId());
		}).getMessage();
		
		assertEquals("The userInfo is required.", result);
	}
	
	@Test
	public void testDeleteWebhookWithNoId() {
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.deleteWebhook(userInfo, null);
		}).getMessage();
		
		assertEquals("The webhookId is required and must not be the empty string.", result);
	}
	
}
