package org.sagebionetworks.repo.manager.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.NodeConstants.BOOTSTRAP_NODES;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dbo.trash.TrashCanDao;
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
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;

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
	private TrashCanDao mockTrashDao;
	
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
			.setInvokeEndpoint("https://abc123.execute-api.us-east-1.amazonaws.com/events")
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
		
		verify(mockWebhookDao).getAllowedDomainsPatterns();		
	}
	
	@Test
	public void testValidateCreateOrUpdateWebhookRequestWithNoAccess() {
		doReturn(AuthorizationStatus.accessDenied("denied")).when(mockWebhookAuthorizationManager).getReadAuthorizationStatus(userInfo, SynapseObjectType.ENTITY, "123");
				
		String result = assertThrows(UnauthorizedException.class, () -> {			
			// Call under test
			webhookManager.validateCreateOrUpdateRequest(userInfo, request);
		}).getMessage();
		
		assertEquals("denied", result);
		
		verify(mockWebhookDao).getAllowedDomainsPatterns();
	}
		
	@ParameterizedTest
	@EnumSource(BOOTSTRAP_NODES.class)
	public void testValidateCreateOrUpdateWebhookRequestWithUnsupportedEntity(BOOTSTRAP_NODES node) {
		request.setObjectId(node.getId().toString());
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.validateCreateOrUpdateRequest(userInfo, request);
		}).getMessage();
		
		assertEquals("The specified object is not valid.", result);
		
		verifyZeroInteractions(mockWebhookAuthorizationManager, mockWebhookDao);
	}
		
	@Test
	public void testValidateCreateOrUpdateWebhookRequestWithUnsupportedDomain() {
		
		when(mockClock.nanoTime()).thenReturn(0L, 0L, WebhookManagerImpl.DOMAIN_CACHE_EXPIRATION.minusSeconds(1).toNanos());
				
		request.setInvokeEndpoint("https://my.endpoint.com/events");
		
		String result = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			webhookManager.validateCreateOrUpdateRequest(userInfo, request);
		}).getMessage();
		
		assertEquals("Unsupported invoke endpoint, please contact support for more information.", result);
				
		result = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			webhookManager.validateCreateOrUpdateRequest(userInfo, request);
		}).getMessage();
		
		assertEquals("Unsupported invoke endpoint, please contact support for more information.", result);
		
		// The two invocations only make one db call
		verify(mockWebhookDao).getAllowedDomainsPatterns();
		verifyZeroInteractions(mockWebhookAuthorizationManager);
	}
	
	@Test
	public void testValidateCreateOrUpdateWebhookRequestWithExpiredDomainCache() {
		doReturn(AuthorizationStatus.authorized()).when(mockWebhookAuthorizationManager).getReadAuthorizationStatus(userInfo, SynapseObjectType.ENTITY, "123");		
		when(mockWebhookDao.getAllowedDomainsPatterns()).thenReturn(Collections.emptyList(), List.of(".+endpoint\\.com"));
		when(mockClock.nanoTime()).thenReturn(0L, 0L, WebhookManagerImpl.DOMAIN_CACHE_EXPIRATION.plusSeconds(1).toNanos());
				
		request.setInvokeEndpoint("https://my.endpoint.com/events");
		
		String result = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			webhookManager.validateCreateOrUpdateRequest(userInfo, request);
		}).getMessage();
		
		assertEquals("Unsupported invoke endpoint, please contact support for more information.", result);
		
		// Call under test, this time the domain is allowed
		webhookManager.validateCreateOrUpdateRequest(userInfo, request);
		
		verify(mockWebhookDao, times(2)).getAllowedDomainsPatterns();
		verifyNoMoreInteractions(mockWebhookAuthorizationManager);
	}
	
	@Test
	public void testValidateCreateOrUpdateWebhookRequestWithAnonymous() {
		userInfo = new UserInfo(false, AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());		
				
		assertThrows(UnauthorizedException.class, () -> {			
			// Call under test
			webhookManager.validateCreateOrUpdateRequest(userInfo, request);
		});
		
		verifyZeroInteractions(mockWebhookAuthorizationManager, mockWebhookDao);
	}
	
	@Test
	public void testValidateCreateOrUpdateWebhookRequestWithNoObjectType() {
		
		request.setObjectType(null);
			
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.validateCreateOrUpdateRequest(userInfo, request);
		}).getMessage();
		
		assertEquals("The objectType is required.", result);
		
		verifyZeroInteractions(mockWebhookAuthorizationManager, mockWebhookDao);
	}
	
	@Test
	public void testValidateCreateOrUpdateWebhookRequestWithNoEvents() {
		
		request.setEventTypes(Collections.emptySet());
			
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.validateCreateOrUpdateRequest(userInfo, request);
		}).getMessage();
		
		assertEquals("The eventTypes is required and must not be empty.", result);
		
		verifyZeroInteractions(mockWebhookAuthorizationManager, mockWebhookDao);
	}
	
	@Test
	public void testValidateCreateOrUpdateWebhookRequestWithNoObjectId() {
		
		request.setObjectId(null);
			
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.validateCreateOrUpdateRequest(userInfo, request);
		}).getMessage();
		
		assertEquals("The objectId is required.", result);
		
		verifyZeroInteractions(mockWebhookAuthorizationManager, mockWebhookDao);
	}
	
	@Test
	public void testValidateCreateOrUpdateWebhookRequestWithNoIsEnabled() {
		
		request.setIsEnabled(null);
			
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.validateCreateOrUpdateRequest(userInfo, request);
		}).getMessage();
		
		assertEquals("isEnabled is required.", result);
		
		verifyZeroInteractions(mockWebhookAuthorizationManager, mockWebhookDao);
	}
	
	static Stream<Arguments> testValidateCreateOrUpdateWebhookRequestWithInvalidEndpoint() {
		return Stream.of(
			arguments(null, "The invokeEndpoint is not a valid url: null"),
			arguments("https://not.valid", "The invokeEndpoint is not a valid url: https://not.valid"),
			arguments("https://localhost/events", "The invokeEndpoint is not a valid url: https://localhost/events"),
			arguments("http://my.webhook.org/events", "The invokedEndpoint only supports https and cannot contain a port, query or fragment"),
			arguments("https://my.webhook.org/events?a=b", "The invokedEndpoint only supports https and cannot contain a port, query or fragment"),
			arguments("https://my.webhook.org:533/events", "The invokedEndpoint only supports https and cannot contain a port, query or fragment"),
			arguments("https://my.webhook.org/events#fragment", "The invokedEndpoint only supports https and cannot contain a port, query or fragment")
		);
	}
	
	@ParameterizedTest
	@MethodSource
	public void testValidateCreateOrUpdateWebhookRequestWithInvalidEndpoint(String invokeEndpoint, String expectedMessage) {
		
		request.setInvokeEndpoint(invokeEndpoint);
			
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.validateCreateOrUpdateRequest(userInfo, request);
		}).getMessage();
		
		assertEquals(expectedMessage, result);
		
		verifyZeroInteractions(mockWebhookAuthorizationManager, mockWebhookDao);
	}
	
	@Test
	public void testCreateWebhook() {
		doNothing().when(webhookManager).validateCreateOrUpdateRequest(userInfo, request);
		doReturn(webhook).when(webhookManager).generateAndSendVerificationCode(userInfo, webhook);
		
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
		
		verify(webhookManager, never()).generateAndSendVerificationCode(any(), any());
	}
	
	@Test
	public void testUpdateWebhookWithUpdatedEndpoint() {
		request.setInvokeEndpoint("https://another.endpoint.org");
		
		Webhook updatedWebhook = new Webhook().setInvokeEndpoint(request.getInvokeEndpoint());
		
		doNothing().when(webhookManager).validateCreateOrUpdateRequest(userInfo, request);
		doReturn(updatedWebhook).when(webhookManager).generateAndSendVerificationCode(userInfo, updatedWebhook);
		
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
		String messageId = "messageId";
		
		when(mockClock.now()).thenReturn(now);
		when(mockWebhookDao.setWebhookVerificationCode(any(), any(), any())).thenReturn(new DBOWebhookVerification().setCodeMessageId(messageId));
		
		doReturn(webhook).when(webhookManager).getWebhook(userInfo, webhook.getId());
		doNothing().when(webhookManager).publishWebhookMessage(any(), any(), any());		
		
		// Call under test
		Webhook updated = webhookManager.generateAndSendVerificationCode(userInfo, webhook);
		
		assertEquals(webhook, updated);
		
		verify(mockWebhookDao).setWebhookVerificationCode(eq(webhook.getId()), stringCaptor.capture(), eq(now.toInstant().plus(60 * 10, ChronoUnit.SECONDS)));
		
		String generatedCode = stringCaptor.getValue();
		
		assertEquals(6, generatedCode.length());
		assertTrue(StringUtils.isAlphanumeric(generatedCode));
				
		verify(webhookManager).publishWebhookMessage(webhook, new WebhookVerificationMessage()
			.setVerificationCode(generatedCode)
			.setEventTimestamp(now), 
			messageId
		);
	}
	
	@Test
	public void testPublishWebhookMessageWithSynapseEvent() throws JSONObjectAdapterException {
		String messageId = "messageId";
		
		WebhookMessage event = new WebhookSynapseEventMessage()
			.setEventTimestamp(new Date())
			.setEventType(SynapseEventType.CREATE)
			.setObjectId("123")
			.setObjectType(SynapseObjectType.ENTITY);
						
		// Call under test
		webhookManager.publishWebhookMessage(webhook, event, messageId);
				
		verify(mockSqsClient).sendMessage(
			new SendMessageRequest()
				.withQueueUrl(queueUrl)
				.withMessageBody(EntityFactory.createJSONStringForEntity(event))
				.withMessageAttributes(Map.of(
					"WebhookMessageId", new MessageAttributeValue().withDataType("String").withStringValue(messageId),
					"WebhookMessageType", new MessageAttributeValue().withDataType("String").withStringValue("SynapseEvent"),
					"WebhookId", new MessageAttributeValue().withDataType("String").withStringValue(webhook.getId()),
					"WebhookOwnerId", new MessageAttributeValue().withDataType("String").withStringValue(webhook.getCreatedBy()),
					"WebhookEndpoint", new MessageAttributeValue().withDataType("String").withStringValue(webhook.getInvokeEndpoint())
				))
		);
	}
	
	@Test
	public void testPublishWebhookMessageWithVerificationEvent() throws JSONObjectAdapterException {
		String messageId = "messageId";
		
		WebhookMessage event = new WebhookVerificationMessage()
			.setEventTimestamp(new Date())
			.setVerificationCode("abcd");
						
		// Call under test
		webhookManager.publishWebhookMessage(webhook, event, messageId);
		
		verify(mockSqsClient).sendMessage(
			new SendMessageRequest()
				.withQueueUrl(queueUrl)
				.withMessageBody(EntityFactory.createJSONStringForEntity(event))
				.withMessageAttributes(Map.of(
					"WebhookMessageId", new MessageAttributeValue().withDataType("String").withStringValue(messageId),
					"WebhookMessageType", new MessageAttributeValue().withDataType("String").withStringValue("Verification"),
					"WebhookId", new MessageAttributeValue().withDataType("String").withStringValue(webhook.getId()),
					"WebhookOwnerId", new MessageAttributeValue().withDataType("String").withStringValue(webhook.getCreatedBy()),
					"WebhookEndpoint", new MessageAttributeValue().withDataType("String").withStringValue(webhook.getInvokeEndpoint())
				))
		);
	}
	
	@Test
	public void testPublishWebhookMessageWithSqsException() throws JSONObjectAdapterException {
		String messageId = "messageId";
		
		RuntimeException ex = new RuntimeException("failed");
		
		when(mockSqsClient.sendMessage(any())).thenThrow(ex);
		
		WebhookMessage event = new WebhookVerificationMessage()
			.setEventTimestamp(new Date())
			.setVerificationCode("abcd");
						
		Throwable cause = assertThrows(RecoverableMessageException.class, () -> {			
			// Call under test
			webhookManager.publishWebhookMessage(webhook, event, messageId);
		}).getCause();
		
		assertEquals(ex, cause);
		
		verify(mockSqsClient).sendMessage(
			new SendMessageRequest()
				.withQueueUrl(queueUrl)
				.withMessageBody(EntityFactory.createJSONStringForEntity(event))
				.withMessageAttributes(Map.of(
					"WebhookMessageId", new MessageAttributeValue().withDataType("String").withStringValue(messageId),
					"WebhookMessageType", new MessageAttributeValue().withDataType("String").withStringValue("Verification"),
					"WebhookId", new MessageAttributeValue().withDataType("String").withStringValue(webhook.getId()),
					"WebhookOwnerId", new MessageAttributeValue().withDataType("String").withStringValue(webhook.getCreatedBy()),
					"WebhookEndpoint", new MessageAttributeValue().withDataType("String").withStringValue(webhook.getInvokeEndpoint())
				))
		);
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
		
		doReturn(List.of(456L, 123L)).when(webhookManager).getEntityActualPathIds(entityId);		
		when(mockWebhookDao.listWebhooksForObjectIds(List.of(456L, 123L), SynapseObjectType.ENTITY, SynapseEventType.CREATE, 1000, 0)).thenReturn(List.of(webhook));		
		when(mockWebhookAuthorizationManager.hasWebhookOwnerReadAccess(webhook)).thenReturn(true);
				
		doNothing().when(webhookManager).publishWebhookMessage(any(), any(), stringCaptor.capture());
				
		// Call under test
		webhookManager.processEntityChange(SynapseEventType.CREATE, eventTimestamp, entityId);
		
		verify(webhookManager).publishWebhookMessage(webhook, new WebhookSynapseEventMessage()
			.setEventTimestamp(eventTimestamp)
			.setEventType(SynapseEventType.CREATE)
			.setObjectId(entityId)
			.setObjectType(SynapseObjectType.ENTITY),
			stringCaptor.getValue()
		);
	}
	
	@Test
	public void testProcessEntityChangeWithEmptyPath() {
		String entityId = "123456";
		Date eventTimestamp = new Date();
		
		doReturn(Collections.emptyList()).when(webhookManager).getEntityActualPathIds(entityId);
						
		// Call under test
		webhookManager.processEntityChange(SynapseEventType.CREATE, eventTimestamp, entityId);
		
		verify(webhookManager, never()).publishWebhookMessage(any(), any(), any());
		verifyZeroInteractions(mockWebhookDao, mockWebhookAuthorizationManager);
	}
	
	@Test
	public void testProcessEntityChangeWithNoMoreReadAccess() {		
		String entityId = "123456";
		Date eventTimestamp = new Date();		
		
		doReturn(List.of(456L, 123L)).when(webhookManager).getEntityActualPathIds(entityId);		
		when(mockWebhookDao.listWebhooksForObjectIds(List.of(456L, 123L), SynapseObjectType.ENTITY, SynapseEventType.CREATE, 1000, 0)).thenReturn(List.of(webhook));	
		when(mockWebhookAuthorizationManager.hasWebhookOwnerReadAccess(webhook)).thenReturn(false);
				
		// Call under test
		webhookManager.processEntityChange(SynapseEventType.CREATE, eventTimestamp, entityId);
		
		verify(webhookManager, never()).publishWebhookMessage(any(), any(), any());
	}
	
	@Test
	public void testGetEntityActualPathIdsWithSingleNode() {
		String entityId = "1";
		
		when(mockNodeDao.getEntityPathIds(entityId)).thenReturn(List.of(BOOTSTRAP_NODES.ROOT.getId(), 1L));
		
		// Call under test
		assertEquals(List.of(1L), webhookManager.getEntityActualPathIds(entityId));
		
		verifyNoMoreInteractions(mockNodeDao, mockTrashDao);
	}
	
	@Test
	public void testGetEntityActualPathIdsWithParentNode() {
		String entityId = "1";
		
		when(mockNodeDao.getEntityPathIds(entityId)).thenReturn(List.of(BOOTSTRAP_NODES.ROOT.getId(), 2L, 1L));
		
		// Call under test
		assertEquals(List.of(2L, 1L), webhookManager.getEntityActualPathIds(entityId));
		
		verifyNoMoreInteractions(mockNodeDao, mockTrashDao);
	}
	
	@Test
	public void testGetEntityActualPathIdsWithEntityInTrashcan() {
		String entityId = "syn1";
		
		when(mockNodeDao.getEntityPathIds(entityId)).thenReturn(List.of(BOOTSTRAP_NODES.ROOT.getId(), BOOTSTRAP_NODES.TRASH.getId(), 1L));
		when(mockTrashDao.getTrashedEntity(entityId)).thenReturn(Optional.of(new TrashedEntity().setOriginalParentId("syn2")));
		when(mockNodeDao.getEntityPathIds("syn2")).thenReturn(List.of(BOOTSTRAP_NODES.ROOT.getId(), 3L, 2L));
		
		// Call under test
		assertEquals(List.of(3L, 2L, 1L), webhookManager.getEntityActualPathIds(entityId));
		
		verifyNoMoreInteractions(mockNodeDao, mockTrashDao);
	}
	
	@Test
	public void testGetEntityActualPathIdsWithProjectInTrashcan() {
		String entityId = "syn1";
		
		when(mockNodeDao.getEntityPathIds(entityId)).thenReturn(List.of(BOOTSTRAP_NODES.ROOT.getId(), BOOTSTRAP_NODES.TRASH.getId(), 1L));
		when(mockTrashDao.getTrashedEntity(entityId)).thenReturn(Optional.of(new TrashedEntity().setOriginalParentId(BOOTSTRAP_NODES.ROOT.getId().toString())));
		
		// Call under test
		assertEquals(List.of(1L), webhookManager.getEntityActualPathIds(entityId));
		
		verifyNoMoreInteractions(mockNodeDao, mockTrashDao);
	}
	
	@Test
	public void testGetEntityActualPathIdsWithEntityRootInTrashcan() {
		String entityId = "syn1";
		
		// The root of the entity is in the trash
		when(mockNodeDao.getEntityPathIds(entityId)).thenReturn(List.of(BOOTSTRAP_NODES.ROOT.getId(), BOOTSTRAP_NODES.TRASH.getId(), 2L, 1L));
		when(mockTrashDao.getTrashedEntity("syn2")).thenReturn(Optional.of(new TrashedEntity().setOriginalParentId("syn3")));
		
		when(mockNodeDao.getEntityPathIds("syn3")).thenReturn(List.of(BOOTSTRAP_NODES.ROOT.getId(), 5L, 4L, 3L));
		
		// Call under test
		assertEquals(List.of(5L, 4L, 3L, 2L, 1L), webhookManager.getEntityActualPathIds(entityId));
		
		verifyNoMoreInteractions(mockNodeDao, mockTrashDao);
	}
	
	@Test
	public void testGetEntityActualPathIdsWithMultipleEntityRootInTrashcan() {
		String entityId = "syn1";
		
		when(mockNodeDao.getEntityPathIds(entityId)).thenReturn(List.of(BOOTSTRAP_NODES.ROOT.getId(), BOOTSTRAP_NODES.TRASH.getId(), 2L, 1L));
		when(mockTrashDao.getTrashedEntity("syn2")).thenReturn(Optional.of(new TrashedEntity().setOriginalParentId("syn3")));
		// The root of this node is also in the trash
		when(mockNodeDao.getEntityPathIds("syn3")).thenReturn(List.of(BOOTSTRAP_NODES.ROOT.getId(), BOOTSTRAP_NODES.TRASH.getId(), 4L, 3L));
		when(mockTrashDao.getTrashedEntity("syn4")).thenReturn(Optional.of(new TrashedEntity().setOriginalParentId("syn5")));
		when(mockNodeDao.getEntityPathIds("syn5")).thenReturn(List.of(BOOTSTRAP_NODES.ROOT.getId(), 6L, 5L));
		
		// Call under test
		assertEquals(List.of(6L, 5L, 4L, 3L, 2L, 1L), webhookManager.getEntityActualPathIds(entityId));
		
		verifyNoMoreInteractions(mockNodeDao, mockTrashDao);
	}
	
	@Test
	public void testGetEntityActualPathIdsWithRootNode() {
		String entityId = BOOTSTRAP_NODES.ROOT.getId().toString();
				
		// Call under test
		assertEquals(Collections.emptyList(), webhookManager.getEntityActualPathIds(entityId));
		
		verifyZeroInteractions(mockNodeDao, mockTrashDao);
	}
	
	@Test
	public void testGetWebhookVerificationStatus() {
		
		when(mockWebhookDao.getWebhookVerificationStatus(webhook.getId(), "messageId")).thenReturn(Optional.of(WebhookVerificationStatus.VERIFIED));
		
		// Call under test
		assertEquals(Optional.of(WebhookVerificationStatus.VERIFIED), webhookManager.getWebhookVerificationStatus(webhook.getId(), "messageId"));
		
	}
	
	@Test
	public void testGetWebhookVerificationStatusWithNoWebhookId() {
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.getWebhookVerificationStatus(null, "messageId");
		}).getMessage();
		
		assertEquals("The webhookId is required.", result);
		
		verifyZeroInteractions(mockWebhookDao);
		
	}
	
	@Test
	public void testGetWebhookVerificationStatusWithNoMessageId() {
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.getWebhookVerificationStatus(webhook.getId(), null);
		}).getMessage();
		
		assertEquals("The messageId is required.", result);
		
		verifyZeroInteractions(mockWebhookDao);
		
	}
	
	@Test
	public void testUpdateWebhookVerificationStatus() {
		
		when(mockWebhookDao.getWebhook(webhook.getId(), true)).thenReturn(Optional.of(webhook));
		
		// Call under test
		webhookManager.updateWebhookVerificationStatus(webhook.getId(), "messageId", WebhookVerificationStatus.VERIFIED, "some message");
		
		verify(mockWebhookDao).setWebhookVerificationStatusIfMessageIdMatch(webhook.getId(), "messageId", WebhookVerificationStatus.VERIFIED, "some message");
	}
	
	@Test
	public void testUpdateWebhookVerificationStatusWithWebhookNotFound() {
		
		when(mockWebhookDao.getWebhook(webhook.getId(), true)).thenReturn(Optional.empty());
		
		// Call under test
		webhookManager.updateWebhookVerificationStatus(webhook.getId(), "messageId", WebhookVerificationStatus.VERIFIED, "some message");
		
		verifyNoMoreInteractions(mockWebhookDao);
	}
	
	@Test
	public void testUpdateWebhookVerificationStatusWithNoMessage() {
		
		when(mockWebhookDao.getWebhook(webhook.getId(), true)).thenReturn(Optional.of(webhook));
		
		// Call under test
		webhookManager.updateWebhookVerificationStatus(webhook.getId(), "messageId", WebhookVerificationStatus.VERIFIED, null);
		
		verify(mockWebhookDao).setWebhookVerificationStatusIfMessageIdMatch(webhook.getId(), "messageId", WebhookVerificationStatus.VERIFIED, null);
	}
	
	@Test
	public void testUpdateWebhookVerificationStatusWithNoWebhookId() {
				
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.updateWebhookVerificationStatus(null, "messageId", WebhookVerificationStatus.VERIFIED, "some message");
		}).getMessage();
		
		assertEquals("The webhookId is required.", result);
		
		verifyZeroInteractions(mockWebhookDao);
	}
	
	@Test
	public void testUpdateWebhookVerificationStatusWithNoMessageId() {
				
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.updateWebhookVerificationStatus(webhook.getId(), null, WebhookVerificationStatus.VERIFIED, "some message");
		}).getMessage();
		
		assertEquals("The messageId is required.", result);
		
		verifyZeroInteractions(mockWebhookDao);
	}
	
	@Test
	public void testUpdateWebhookVerificationStatusWithNoStatus() {
				
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			webhookManager.updateWebhookVerificationStatus(webhook.getId(), "messageId", null, "some message");
		}).getMessage();
		
		assertEquals("The status is required.", result);
		
		verifyZeroInteractions(mockWebhookDao);
	}
	
	@Test
	public void testLoadAllowedDomainPatterns() {
		
		List<String> expected = List.of("^.+\\.execute-api\\..+\\.amazonaws\\.com$");
		List<String> result = webhookManager.loadAllowedDomainPatterns().stream().map(Pattern::pattern).collect(Collectors.toList());
		
		assertEquals(expected, result);
		
		verify(mockWebhookDao).getAllowedDomainsPatterns();
	}
	
	@Test
	public void testLoadAllowedDomainPatternsWithDatabaseList() {
		
		when(mockWebhookDao.getAllowedDomainsPatterns()).thenReturn(List.of("^.+zapier.com$", "\\jinvalid"));
		
		List<String> expected = List.of(
			"^.+\\.execute-api\\..+\\.amazonaws\\.com$",
			"^.+zapier.com$"
		);
		List<String> result = webhookManager.loadAllowedDomainPatterns().stream().map(Pattern::pattern).collect(Collectors.toList());
		
		assertEquals(expected, result);
	}
}
