package org.sagebionetworks.repo.manager.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dbo.webhook.DBOWebhookVerification;
import org.sagebionetworks.repo.model.dbo.webhook.WebhookDao;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.webhook.CreateOrUpdateWebhookRequest;
import org.sagebionetworks.repo.model.webhook.ListUserWebhooksRequest;
import org.sagebionetworks.repo.model.webhook.ListUserWebhooksResponse;
import org.sagebionetworks.repo.model.webhook.SynapseEventType;
import org.sagebionetworks.repo.model.webhook.SynapseObjectType;
import org.sagebionetworks.repo.model.webhook.VerifyWebhookRequest;
import org.sagebionetworks.repo.model.webhook.VerifyWebhookResponse;
import org.sagebionetworks.repo.model.webhook.Webhook;
import org.sagebionetworks.repo.model.webhook.WebhookMessage;
import org.sagebionetworks.repo.model.webhook.WebhookSynapseEventMessage;
import org.sagebionetworks.repo.model.webhook.WebhookVerificationMessage;
import org.sagebionetworks.repo.model.webhook.WebhookVerificationStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.Clock;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;

@ExtendWith(MockitoExtension.class)
public class WebhookManagerUnitTest {
	
	@Mock
	private WebhookDao mockWebhookDao;
	
	@Mock
	private AmazonSQSClient mockSqsClient;
	
	@Mock
	private WebhookAuthorizationManager mockWebhookAuthorizationManager;
	
	@Mock
	private NodeDAO mockNodeDao;
	
	@Mock
	private Clock mockClock;
	
	@Mock
	private StackConfiguration mockStackConfig;

	@InjectMocks
	@Spy
	private WebhookManagerImpl webhookManager;
	
	private UserInfo userInfo;

	private CreateOrUpdateWebhookRequest request;
	
	private Webhook webhook;
	
	private String queueUrl;
	
	@Captor
	private ArgumentCaptor<WebhookMessage> eventCaptor;
	
	@Captor
	private ArgumentCaptor<String> stringCaptor;
	
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
		
		when(mockStackConfig.getQueueName("WEBHOOK_MESSAGE")).thenReturn("queueName");
		
		queueUrl = "queueUrl";
		
		GetQueueUrlResult res = new GetQueueUrlResult();
		res.setQueueUrl(queueUrl);
		
		when(mockSqsClient.getQueueUrl("queueName")).thenReturn(res);
		
		// This is automatically invoked by spring
		webhookManager.configureMessageQueueUrl(mockStackConfig);
	}
	
	@Test
	public void testValidateCreateOrUpdateWebhookRequest() {
		doReturn(AuthorizationStatus.authorized()).when(mockWebhookAuthorizationManager).getReadAuthorizationStatus(userInfo, SynapseObjectType.ENTITY, "123");
				
		// Call under test
		webhookManager.validateCreateOrUpdateRequest(userInfo, request);
		
	}
	
	@Test
	public void testValidateCreateOrUpdateWebhookRequestWithNoAccess() {
		doReturn(AuthorizationStatus.accessDenied("denied")).when(mockWebhookAuthorizationManager).getReadAuthorizationStatus(userInfo, SynapseObjectType.ENTITY, "123");
				
		String result = assertThrows(UnauthorizedException.class, () -> {			
			// Call under test
			webhookManager.validateCreateOrUpdateRequest(userInfo, request);
		}).getMessage();
		
		assertEquals("denied", result);
	}
	
	@Test
	public void testValidateCreateOrUpdateWebhookRequestWithAnonymous() {
		userInfo = new UserInfo(false, AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());		
				
		assertThrows(UnauthorizedException.class, () -> {			
			// Call under test
			webhookManager.validateCreateOrUpdateRequest(userInfo, request);
		});
	}
	
	@Test
	public void testValidateCreateOrUpdateWebhookRequestWithNoObjectType() {
		
		request.setObjectType(null);
			
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.validateCreateOrUpdateRequest(userInfo, request);
		}).getMessage();
		
		assertEquals("The objectType is required.", result);
	}
	
	@Test
	public void testValidateCreateOrUpdateWebhookRequestWithNoEvents() {
		
		request.setEventTypes(Collections.emptySet());
			
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.validateCreateOrUpdateRequest(userInfo, request);
		}).getMessage();
		
		assertEquals("The eventTypes is required and must not be empty.", result);
	}
	
	@Test
	public void testValidateCreateOrUpdateWebhookRequestWithNoObjectId() {
		
		request.setObjectId(null);
			
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.validateCreateOrUpdateRequest(userInfo, request);
		}).getMessage();
		
		assertEquals("The objectId is required.", result);
	}
	
	@Test
	public void testValidateCreateOrUpdateWebhookRequestWithNoIsEnabled() {
		
		request.setIsEnabled(null);
			
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.validateCreateOrUpdateRequest(userInfo, request);
		}).getMessage();
		
		assertEquals("isEnabled is required.", result);
	}
	
	@Test
	public void testValidateCreateOrUpdateWebhookRequestWithNoEndpoint() {
		
		request.setInvokeEndpoint(null);
			
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.validateCreateOrUpdateRequest(userInfo, request);
		}).getMessage();
		
		assertEquals("The invokeEndpoint is not a valid url: null", result);
	}
	
	@Test
	public void testValidateCreateOrUpdateWebhookRequestWithInvalidEndpoint() {
		
		request.setInvokeEndpoint("https://not.valid");
			
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.validateCreateOrUpdateRequest(userInfo, request);
		}).getMessage();
		
		assertEquals("The invokeEndpoint is not a valid url: https://not.valid", result);
	}
	
	@Test
	public void testValidateCreateOrUpdateWebhookRequestWithLocalEndpoint() {
		
		request.setInvokeEndpoint("https://localhost/events");
			
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.validateCreateOrUpdateRequest(userInfo, request);
		}).getMessage();
		
		assertEquals("The invokeEndpoint is not a valid url: https://localhost/events", result);
	}
	
	@Test
	public void testValidateCreateOrUpdateWebhookRequestWithQuery() {
		
		request.setInvokeEndpoint("https://my.webhook.org/events?a=b");
			
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.validateCreateOrUpdateRequest(userInfo, request);
		}).getMessage();
		
		assertEquals("The invokedEndpoint only supports https and cannot contain a port, query or fragment", result);
	}
	
	@Test
	public void testValidateCreateOrUpdateWebhookRequestWithPort() {
		
		request.setInvokeEndpoint("https://my.webhook.org:533/events");
			
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.validateCreateOrUpdateRequest(userInfo, request);
		}).getMessage();
		
		assertEquals("The invokedEndpoint only supports https and cannot contain a port, query or fragment", result);
	}
	
	@Test
	public void testValidateCreateOrUpdateWebhookRequestWithFragment() {
		
		request.setInvokeEndpoint("https://my.webhook.org/events#fragment");
			
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.validateCreateOrUpdateRequest(userInfo, request);
		}).getMessage();
		
		assertEquals("The invokedEndpoint only supports https and cannot contain a port, query or fragment", result);
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
		
		verifyZeroInteractions(mockWebhookDao);
	}
	
	@Test
	public void testDeleteWebhookWithNoId() {
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.deleteWebhook(userInfo, null);
		}).getMessage();
		
		assertEquals("The webhookId is required and must not be the empty string.", result);
		
		verifyZeroInteractions(mockWebhookDao);
	}
	
	@Test
	public void testListUserWebhooks() {

		when(mockWebhookDao.listUserWebhooks(userInfo.getId(), NextPageToken.DEFAULT_LIMIT + 1, 0L)).thenReturn(List.of(webhook));
		
		ListUserWebhooksRequest listRequest = new ListUserWebhooksRequest();
		
		ListUserWebhooksResponse expected = new ListUserWebhooksResponse()
			.setPage(List.of(webhook));
		
		// Call under test
		ListUserWebhooksResponse result = webhookManager.listUserWebhooks(userInfo, listRequest);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testListUserWebhooksWithoutUser() {
		
		ListUserWebhooksRequest listRequest = new ListUserWebhooksRequest();
		
		userInfo = null;
	
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.listUserWebhooks(userInfo, listRequest);
		}).getMessage();
		
		assertEquals("The userInfo is required.", result);
		
		verifyZeroInteractions(mockWebhookDao);
	}
	
	@Test
	public void testListUserWebhooksWithoutRequest() {
		
		ListUserWebhooksRequest listRequest = null;
	
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.listUserWebhooks(userInfo, listRequest);
		}).getMessage();
		
		assertEquals("The request is required.", result);
		
		verifyZeroInteractions(mockWebhookDao);
	}
	
	@Test
	public void testGenerateAndSendVerificationCode() {
		Date now = new Date();
		
		when(mockClock.now()).thenReturn(now);
		doNothing().when(webhookManager).publishWebhookMessage(any());		
		
		// Call under test
		webhookManager.generateAndSendVerificationCode(webhook);
		
		verify(mockWebhookDao).setWebhookVerificationCode(eq(webhook.getId()), stringCaptor.capture(), eq(now.toInstant().plus(60 * 10, ChronoUnit.SECONDS)));
		
		String generatedCode = stringCaptor.getValue();
		
		assertEquals(6, generatedCode.length());
		assertTrue(StringUtils.isAlphanumeric(generatedCode));
		
		verify(webhookManager).publishWebhookMessage(eventCaptor.capture());
		
		WebhookMessage sentEvent = eventCaptor.getValue();
				
		assertEquals(new WebhookVerificationMessage()
			.setVerificationCode(generatedCode)
			.setWebhookId(webhook.getId())
			.setEventTimestamp(now)
			.setWebhookOwnerId(userInfo.getId().toString())
			.setWebhookInvokeEndpoint(webhook.getInvokeEndpoint()), sentEvent
		);
	}
	
	@Test
	public void testPublishWebhookMessageWithSynapseEvent() throws JSONObjectAdapterException {
		
		WebhookMessage event = new WebhookSynapseEventMessage()
			.setEventTimestamp(new Date())
			.setEventType(SynapseEventType.CREATE)
			.setObjectId("123")
			.setObjectType(SynapseObjectType.ENTITY)
			.setWebhookId(webhook.getId())
			.setWebhookOwnerId(webhook.getCreatedBy())
			.setWebhookInvokeEndpoint(webhook.getInvokeEndpoint());
						
		// Call under test
		webhookManager.publishWebhookMessage(event);
		
		verify(mockSqsClient).sendMessage(queueUrl, EntityFactory.createJSONStringForEntity(event));
	}
	
	@Test
	public void testPublishWebhookMessageWithVerificationEvent() throws JSONObjectAdapterException {
		
		WebhookMessage event = new WebhookVerificationMessage()
			.setEventTimestamp(new Date())
			.setWebhookId(webhook.getId())
			.setWebhookOwnerId(webhook.getCreatedBy())
			.setWebhookInvokeEndpoint(webhook.getInvokeEndpoint())
			.setVerificationCode("abcd");
						
		// Call under test
		webhookManager.publishWebhookMessage(event);
		
		verify(mockSqsClient).sendMessage(queueUrl, EntityFactory.createJSONStringForEntity(event));
	}
	
	@Test
	public void testVerifyWebhook() {
		
		webhook.setVerificationStatus(WebhookVerificationStatus.CODE_SENT);
		
		Date now = new Date();
		
		when(mockClock.now()).thenReturn(now);
		doReturn(webhook).when(webhookManager).getWebhook(userInfo, webhook.getId(), true);
		
		when(mockWebhookDao.getWebhookVerification(webhook.getId())).thenReturn(new DBOWebhookVerification()
			.setCode("abcdef")
			.setCodeExpiresOn(new Timestamp(now.getTime() + 10_000))
		);
		
		VerifyWebhookResponse expectedResult = new VerifyWebhookResponse()
			.setIsValid(true)
			.setInvalidReason(null);
			
		// Call under test
		VerifyWebhookResponse result = webhookManager.verifyWebhook(userInfo, webhook.getId(), new VerifyWebhookRequest().setVerificationCode("abcdef"));
		
		assertEquals(expectedResult, result);
		
		verify(mockWebhookDao).setWebhookVerificationStatus(webhook.getId(), WebhookVerificationStatus.VERIFIED, null);
	}
	
	@ParameterizedTest
	@EnumSource(value = WebhookVerificationStatus.class, mode = Mode.EXCLUDE, names = "CODE_SENT")
	public void testVerifyWebhookWithWrongState(WebhookVerificationStatus status) {
		
		webhook.setVerificationStatus(status);
		
		doReturn(webhook).when(webhookManager).getWebhook(userInfo, webhook.getId(), true);
		
		String result = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			webhookManager.verifyWebhook(userInfo, webhook.getId(), new VerifyWebhookRequest().setVerificationCode("abcdef"));			
		}).getMessage();
		
		assertEquals("Cannot verify the webhook at this time.", result);
		
		verifyZeroInteractions(mockWebhookDao);
	}
	
	@Test
	public void testVerifyWebhookWithExpiredCode() {
		
		webhook.setVerificationStatus(WebhookVerificationStatus.CODE_SENT);
		
		Date now = new Date();
		
		when(mockClock.now()).thenReturn(now);
		doReturn(webhook).when(webhookManager).getWebhook(userInfo, webhook.getId(), true);
		
		when(mockWebhookDao.getWebhookVerification(webhook.getId())).thenReturn(new DBOWebhookVerification()
			.setCode("abcdef")
			.setCodeExpiresOn(new Timestamp(now.getTime() - 10_000))
		);
		
		VerifyWebhookResponse expectedResult = new VerifyWebhookResponse()
			.setIsValid(false)
			.setInvalidReason("The verification code has expired.");
			
		// Call under test
		VerifyWebhookResponse result = webhookManager.verifyWebhook(userInfo, webhook.getId(), new VerifyWebhookRequest().setVerificationCode("abcdef"));
		
		assertEquals(expectedResult, result);
		
		verify(mockWebhookDao).setWebhookVerificationStatus(webhook.getId(), WebhookVerificationStatus.FAILED, "The verification code has expired.");
	}
	
	@Test
	public void testVerifyWebhookWithInvalidCode() {
		
		webhook.setVerificationStatus(WebhookVerificationStatus.CODE_SENT);
		
		Date now = new Date();
		
		when(mockClock.now()).thenReturn(now);
		doReturn(webhook).when(webhookManager).getWebhook(userInfo, webhook.getId(), true);
		
		when(mockWebhookDao.getWebhookVerification(webhook.getId())).thenReturn(new DBOWebhookVerification()
			.setCode("abcdef")
			.setCodeExpiresOn(new Timestamp(now.getTime() + 10_000))
		);
		
		VerifyWebhookResponse expectedResult = new VerifyWebhookResponse()
			.setIsValid(false)
			.setInvalidReason("The provided verification code is invalid.");
			
		// Call under test
		VerifyWebhookResponse result = webhookManager.verifyWebhook(userInfo, webhook.getId(), new VerifyWebhookRequest().setVerificationCode("abcdef-wrong"));
		
		assertEquals(expectedResult, result);
		
		verify(mockWebhookDao).setWebhookVerificationStatus(webhook.getId(), WebhookVerificationStatus.CODE_SENT, "The provided verification code is invalid.");
	}
	
	@Test
	public void testVerifyWebhookWithNoUserInfo() {
		userInfo = null;
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.verifyWebhook(userInfo, webhook.getId(), new VerifyWebhookRequest().setVerificationCode("abcdef"));
		}).getMessage();
		
		assertEquals("The userInfo is required.", result);
		
		verifyZeroInteractions(mockWebhookDao);
	}
	
	@Test
	public void testVerifyWebhookWithNoWebhookId() {
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.verifyWebhook(userInfo, null, new VerifyWebhookRequest().setVerificationCode("abcdef"));
		}).getMessage();
		
		assertEquals("The webhookId is required.", result);
		
		verifyZeroInteractions(mockWebhookDao);
	}
	
	@Test
	public void testVerifyWebhookWithNoRequest() {
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.verifyWebhook(userInfo, webhook.getId(), null);
		}).getMessage();
		
		assertEquals("The request is required.", result);
		
		verifyZeroInteractions(mockWebhookDao);
	}
	
	@Test
	public void testVerifyWebhookWithNoCode() {
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.verifyWebhook(userInfo, webhook.getId(), new VerifyWebhookRequest());
		}).getMessage();
		
		assertEquals("The verificationCode is required and must not be the empty string.", result);
		
		verifyZeroInteractions(mockWebhookDao);
	}
	
	
	@Test
	public void testProcessChangeMessage() {
		Date now = new Date();
		
		when(mockClock.now()).thenReturn(now);

		ChangeMessage change = new ChangeMessage()
				.setObjectType(ObjectType.ENTITY)
				.setObjectId("123")
				.setTimestamp(now)
				.setChangeType(ChangeType.UPDATE);
		
		doNothing().when(webhookManager).processEntityChange(SynapseEventType.UPDATE, change.getTimestamp(), change.getObjectId());
						
		// Call under test
		webhookManager.processChangeMessage(change);
		
	}
	
	@Test
	public void testProcessChangeMessageWithOldMessage() {
		Date now = new Date();
		
		when(mockClock.now()).thenReturn(now);

		Date changeTimestamp = Date.from(now.toInstant().minus(Duration.ofHours(1).plusSeconds(1))); 
		ChangeMessage change = new ChangeMessage()
				.setObjectType(ObjectType.ENTITY)
				.setObjectId("123")
				.setTimestamp(changeTimestamp)
				.setChangeType(ChangeType.UPDATE);
								
		// Call under test
		webhookManager.processChangeMessage(change);
		
		verify(webhookManager, never()).processEntityChange(any(), any(), any());
		
	}
	
	@Test
	public void testProcessChangeMessageWithUnsupportedtype() {
		Date now = new Date();
		
		when(mockClock.now()).thenReturn(now);

		ChangeMessage change = new ChangeMessage()
				.setObjectType(ObjectType.ACCESS_APPROVAL)
				.setObjectId("123")
				.setTimestamp(now)
				.setChangeType(ChangeType.UPDATE);
						
		// Call under test
		webhookManager.processChangeMessage(change);
		
		verify(webhookManager, never()).processEntityChange(any(), any(), any());
		
	}
	
	@Test
	public void testProcessEntityChange() {
		String entityId = "123456";
		Date eventTimestamp = new Date();		
		
		when(mockNodeDao.getEntityPathIds(entityId)).thenReturn(List.of(456L, 123L));		
		when(mockWebhookDao.listWebhooksForObjectIds(List.of(456L, 123L), SynapseObjectType.ENTITY, SynapseEventType.CREATE, 1000, 0)).thenReturn(List.of(webhook));		
		when(mockWebhookAuthorizationManager.hasWebhookOwnerReadAccess(webhook)).thenReturn(true);
				
		doNothing().when(webhookManager).publishWebhookMessage(any());
				
		// Call under test
		webhookManager.processEntityChange(SynapseEventType.CREATE, eventTimestamp, entityId);
		
		verify(webhookManager).publishWebhookMessage(new WebhookSynapseEventMessage()
			.setEventTimestamp(eventTimestamp)
			.setEventType(SynapseEventType.CREATE)
			.setObjectId(entityId)
			.setObjectType(SynapseObjectType.ENTITY)
			.setWebhookId(webhook.getId())
			.setWebhookOwnerId(webhook.getCreatedBy())
			.setWebhookInvokeEndpoint(webhook.getInvokeEndpoint())
		);
	}
	
	@Test
	public void testProcessEntityChangeWithNoMoreReadAccess() {		
		String entityId = "123456";
		Date eventTimestamp = new Date();		
		
		when(mockNodeDao.getEntityPathIds(entityId)).thenReturn(List.of(456L, 123L));		
		when(mockWebhookDao.listWebhooksForObjectIds(List.of(456L, 123L), SynapseObjectType.ENTITY, SynapseEventType.CREATE, 1000, 0)).thenReturn(List.of(webhook));	
		when(mockWebhookAuthorizationManager.hasWebhookOwnerReadAccess(webhook)).thenReturn(false);
				
		// Call under test
		webhookManager.processEntityChange(SynapseEventType.CREATE, eventTimestamp, entityId);
		
		verify(webhookManager, never()).publishWebhookMessage(any());
	}
		
}
