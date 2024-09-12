package org.sagebionetworks.repo.manager.webhook;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenManager;
import org.sagebionetworks.repo.manager.webhook.WebhookMessageDispatcher.WebhookMessageAttributes;
import org.sagebionetworks.repo.model.webhook.Webhook;
import org.sagebionetworks.repo.model.webhook.WebhookVerificationStatus;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;

@ExtendWith(MockitoExtension.class)
public class WebhookMessageDispatcherUnitTest {
	
	@Mock
	private WebhookManager mockManager;
	
	@Mock
	private OIDCTokenManager mockTokenManager;
	
	@Mock
	private HttpClient mockClient;
	
	@Mock
	private Clock mockClock;
	
	@Mock
	private WebhookMetricsCollector mockMetricsCollector;
	
	@Mock
	private StackConfiguration mockConfig;
	
	@InjectMocks
	@Spy
	private WebhookMessageDispatcher dispatcher;
	
	@Mock
	private Message mockMessage;
	
	@Mock
	private HttpRequest mockRequest;
	
	@Mock
	private HttpResponse<Void> mockResponse;
	
	@Mock
	private CompletableFuture<HttpResponse<Void>> mockFutureResponse;	
		
	private String userAgent;
	private Webhook webhook;
	private WebhookMessageType messageType;
	private String tokenIssuer;
	private String authToken;

	@BeforeEach
	public void before() {
		when(mockConfig.getStackInstance()).thenReturn("123");
		when(mockConfig.getStack()).thenReturn("dev");
		
		// This is automatically invoked by spring
		dispatcher.configure(mockConfig);
		
		userAgent = "Synapse-Webhook/123";
		
		webhook = new Webhook()
			.setId("123")
			.setCreatedBy("456")
			.setInvokeEndpoint("https://my.endpoint");
		
		messageType = WebhookMessageType.SynapseEvent;
		tokenIssuer = "https://repo-prod.dev.sagebase.org/auth/v1";
		authToken = "authToken";
	}
	
	private Map<String, MessageAttributeValue> expectedMessageAttributes() {
		return WebhookManagerImpl.mapMessageAttributes(messageType.getMessageClass(), webhook, "messageId");
	}
	
	private WebhookMessageAttributes expectedAttributes() {
		return new WebhookMessageAttributes(expectedMessageAttributes());
	}
	
	private HttpRequest expectedRequest() {
		return HttpRequest.newBuilder(URI.create(webhook.getInvokeEndpoint()))
			.timeout(WebhookMessageDispatcher.REQUEST_TIMEOUT)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
			.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.header(HttpHeaders.USER_AGENT, userAgent)
			.headers(new WebhookMessageAttributes(expectedMessageAttributes()).toRequestHeaders())
			.POST(BodyPublishers.ofString("messageBody"))
			.build();
	}	
	
	@Test
	public void testDispatchMessage() {
		
		when(mockMessage.getMessageAttributes()).thenReturn(expectedMessageAttributes());
		when(mockMessage.getBody()).thenReturn("messageBody");
		when(mockMessage.getMD5OfBody()).thenReturn("messageMd5");
		when(mockTokenManager.createWebhookMessageToken(tokenIssuer, "messageMd5", webhook.getId(), webhook.getCreatedBy(), 30)).thenReturn(authToken);
		
		doNothing().when(dispatcher).sendWebhookRequest(expectedAttributes(), expectedRequest());
		
		// Call under test
		dispatcher.dispatchMessage(mockMessage);

		verifyNoMoreInteractions(mockManager);
	}
	
	@ParameterizedTest
	@EnumSource(value = WebhookVerificationStatus.class, mode = Mode.INCLUDE, names = {"PENDING", "FAILED"})
	public void testDispatchMessageWithVerificationAndProcessableStatus(WebhookVerificationStatus status) {
		messageType = WebhookMessageType.Verification;
		
		when(mockMessage.getMessageAttributes()).thenReturn(expectedMessageAttributes());
		when(mockMessage.getBody()).thenReturn("messageBody");
		when(mockMessage.getMD5OfBody()).thenReturn("messageMd5");
		when(mockManager.getWebhookVerificationStatus(webhook.getId(), "messageId")).thenReturn(Optional.of(status));
		when(mockTokenManager.createWebhookMessageToken(tokenIssuer, "messageMd5", webhook.getId(), webhook.getCreatedBy(), 30)).thenReturn(authToken);		
		
		doNothing().when(dispatcher).sendWebhookRequest(expectedAttributes(), expectedRequest());
		
		// Call under test
		dispatcher.dispatchMessage(mockMessage);

		verifyNoMoreInteractions(mockManager);
	}
	
	@ParameterizedTest
	@EnumSource(value = WebhookVerificationStatus.class, mode = Mode.EXCLUDE, names = {"PENDING", "FAILED"})
	public void testDispatchMessageWithVerificationAndUnprocessableStatus(WebhookVerificationStatus status) {
		messageType = WebhookMessageType.Verification;
		
		when(mockMessage.getMessageAttributes()).thenReturn(expectedMessageAttributes());
		when(mockManager.getWebhookVerificationStatus(webhook.getId(), "messageId")).thenReturn(Optional.of(status));
				
		assertThrows(RecoverableMessageException.class, () -> {
			// Call under test
			dispatcher.dispatchMessage(mockMessage);			
		});

		verify(dispatcher, never()).sendWebhookRequest(any(), any());
		verifyNoMoreInteractions(mockManager);
	}
	
	@Test
	public void testDispatchMessageWithVerificationAndEmptyStatus() {
		messageType = WebhookMessageType.Verification;
		
		when(mockMessage.getMessageAttributes()).thenReturn(expectedMessageAttributes());
		when(mockManager.getWebhookVerificationStatus(webhook.getId(), "messageId")).thenReturn(Optional.empty());
				
		assertThrows(RecoverableMessageException.class, () -> {
			// Call under test
			dispatcher.dispatchMessage(mockMessage);			
		});

		verify(dispatcher, never()).sendWebhookRequest(any(), any());
		verifyNoMoreInteractions(mockManager);
	}
	
	@Test
	public void testDispatchMessageWithMissingMessageAttributes() {
		assertEquals("Could not find attribute: WebhookMessageId", assertThrows(IllegalStateException.class, () -> {			
			// Call under test
			dispatcher.dispatchMessage(mockMessage);
		}).getMessage());
		
		verifyZeroInteractions(mockManager, mockMetricsCollector);
	}
	
	@ParameterizedTest
	@EnumSource(value = HttpStatus.class, mode = Mode.INCLUDE, names = {"OK", "ACCEPTED", "CREATED", "NO_CONTENT"})
	public void testSendWebhookRequest() throws Exception {
		
		when(mockClock.currentTimeMillis()).thenReturn(0l, 150l);
		when(mockClient.sendAsync(mockRequest, WebhookMessageDispatcher.DISCARDING_BODY_HANDLER)).thenReturn(mockFutureResponse);
		when(mockFutureResponse.get(WebhookMessageDispatcher.REQUEST_TIMEOUT.getSeconds(), TimeUnit.SECONDS)).thenReturn(mockResponse);
		when(mockResponse.statusCode()).thenReturn(200);
		
		doNothing().when(dispatcher).updateVerificationStatus(any(), anyBoolean(), any(), any());
		
		WebhookMessageAttributes attributes = expectedAttributes();
		
		// Call under test
		dispatcher.sendWebhookRequest(attributes, mockRequest);
		
		verify(mockMetricsCollector).requestCompleted(webhook.getId(), 150, false);
		verify(dispatcher).updateVerificationStatus(attributes, true, mockResponse, null);
		
		verifyNoMoreInteractions(mockMetricsCollector);
	}
		
	@Test
	public void testSendWebhookRequestWithException() throws Exception {
		
		Throwable ex = new ExecutionException(new RuntimeException("Failed"));
		
		when(mockClock.currentTimeMillis()).thenReturn(0l, 150l);
		when(mockClient.sendAsync(mockRequest, WebhookMessageDispatcher.DISCARDING_BODY_HANDLER)).thenReturn(mockFutureResponse);
		when(mockFutureResponse.get(WebhookMessageDispatcher.REQUEST_TIMEOUT.getSeconds(), TimeUnit.SECONDS)).thenThrow(ex);
		
		doNothing().when(dispatcher).updateVerificationStatus(any(), anyBoolean(), any(), any());
		
		WebhookMessageAttributes attributes = expectedAttributes();
		
		Throwable result = assertThrows(RecoverableMessageException.class, () -> {			
			// Call under test
			dispatcher.sendWebhookRequest(attributes, mockRequest);
		});
		
		assertEquals(ex, result.getCause());
		
		verify(mockMetricsCollector).requestCompleted(webhook.getId(), 150, true);
		
		verify(dispatcher).updateVerificationStatus(attributes, false, null, ex);
		
		verifyNoMoreInteractions(mockMetricsCollector);
	}
	
	@ParameterizedTest
	@EnumSource(value = HttpStatus.class, mode = Mode.INCLUDE, names = {"TOO_MANY_REQUESTS", "INTERNAL_SERVER_ERROR", "BAD_GATEWAY", "SERVICE_UNAVAILABLE", "GATEWAY_TIMEOUT"})
	public void testSendWebhookRequestWithRecoverableResponseStatus(HttpStatus status) throws Exception {
		
		when(mockClock.currentTimeMillis()).thenReturn(0l, 150l);
		when(mockClient.sendAsync(mockRequest, WebhookMessageDispatcher.DISCARDING_BODY_HANDLER)).thenReturn(mockFutureResponse);
		when(mockFutureResponse.get(WebhookMessageDispatcher.REQUEST_TIMEOUT.getSeconds(), TimeUnit.SECONDS)).thenReturn(mockResponse);
		when(mockResponse.statusCode()).thenReturn(status.value());
		
		doNothing().when(dispatcher).updateVerificationStatus(any(), anyBoolean(), any(), any());
		
		WebhookMessageAttributes attributes = expectedAttributes();
		
		assertThrows(RecoverableMessageException.class, () -> {			
			// Call under test
			dispatcher.sendWebhookRequest(attributes, mockRequest);
		});

		verify(mockMetricsCollector).requestCompleted(webhook.getId(), 150, true);
		
		verify(dispatcher).updateVerificationStatus(attributes, false, mockResponse, null);
		
		verifyNoMoreInteractions(mockMetricsCollector);
	}
	
	@ParameterizedTest
	@EnumSource(value = HttpStatus.class, mode = Mode.EXCLUDE, names = {"OK", "ACCEPTED", "CREATED", "NO_CONTENT", "TOO_MANY_REQUESTS", "INTERNAL_SERVER_ERROR", "BAD_GATEWAY", "SERVICE_UNAVAILABLE", "GATEWAY_TIMEOUT"})
	public void testSendWebhookRequestWithUnrecoverableResponseStatus(HttpStatus status) throws Exception {
		
		when(mockClock.currentTimeMillis()).thenReturn(0l, 150l);
		when(mockClient.sendAsync(mockRequest, WebhookMessageDispatcher.DISCARDING_BODY_HANDLER)).thenReturn(mockFutureResponse);
		when(mockFutureResponse.get(WebhookMessageDispatcher.REQUEST_TIMEOUT.getSeconds(), TimeUnit.SECONDS)).thenReturn(mockResponse);
		when(mockResponse.statusCode()).thenReturn(status.value());
		
		doNothing().when(dispatcher).updateVerificationStatus(any(), anyBoolean(), any(), any());
		
		WebhookMessageAttributes attributes = expectedAttributes();	
		
		// Call under test
		dispatcher.sendWebhookRequest(attributes, mockRequest);

		verify(mockMetricsCollector).requestCompleted(webhook.getId(), 150, true);
		
		verify(dispatcher).updateVerificationStatus(attributes, false, mockResponse, null);
		
		verifyNoMoreInteractions(mockMetricsCollector);
	}
	
	@ParameterizedTest
	@EnumSource(WebhookMessageType.class)
	public void testWebhookMessageAttributes(WebhookMessageType messageType) {
		this.messageType = messageType; 
		
		// Call under test
		WebhookMessageAttributes attributes = expectedAttributes();
		
		assertEquals("messageId", attributes.getMessageId());
		assertEquals(webhook.getId(), attributes.getWebhookId());
		assertEquals(webhook.getInvokeEndpoint(), attributes.getWebhookEndpoint());
		assertEquals(webhook.getCreatedBy(), attributes.getWebhookOwnerId());
		assertEquals(messageType, attributes.getMessageType());
		assertEquals(WebhookMessageType.Verification.equals(messageType), attributes.isVerification());
		
		assertArrayEquals(new String[] {
				WebhookMessageDispatcher.HEADER_WEBHOOK_ID, webhook.getId(),
				WebhookMessageDispatcher.HEADER_WEBHOOK_MSG_ID, "messageId",
				WebhookMessageDispatcher.HEADER_WEBHOOK_OWNER_ID, webhook.getCreatedBy(),
				WebhookMessageDispatcher.HEADER_WEBHOOK_MESSAGE_TYPE, messageType.name()
			}, 
			// Call under test
			attributes.toRequestHeaders()
		);
		
	}
	
	@Test
	public void testWebhookMessageAttributesWithMissingAttributes() {
		Map<String, MessageAttributeValue> messageAttributes = Collections.emptyMap();
		
		assertThrows(IllegalStateException.class, () -> {
			// Call under test
			new WebhookMessageAttributes(messageAttributes);
		});
		
	}
	
	@Test
	public void testWebhookMessageAttributesWithMissingWebhookId() {
		Map<String, MessageAttributeValue> messageAttributes = Map.of(
			WebhookManager.MSG_ATTR_WEBHOOK_MESSAGE_ID, new MessageAttributeValue().withStringValue("messageId"),
			WebhookManager.MSG_ATTR_WEBHOOK_ENDPOINT, new MessageAttributeValue().withStringValue(webhook.getInvokeEndpoint()),
			WebhookManager.MSG_ATTR_WEBHOOK_OWNER_ID, new MessageAttributeValue().withStringValue(webhook.getCreatedBy()),
			WebhookManager.MSG_ATTR_WEBHOOK_MESSAGE_TYPE, new MessageAttributeValue().withStringValue(WebhookMessageType.Verification.name())
		);
		
		assertEquals("Could not find attribute: WebhookId", assertThrows(IllegalStateException.class, () -> {
			// Call under test
			new WebhookMessageAttributes(messageAttributes);
		}).getMessage());
		
	}
	
	@Test
	public void testWebhookMessageAttributesWithMissingWebhookOwnerId() {
		Map<String, MessageAttributeValue> messageAttributes = Map.of(
			WebhookManager.MSG_ATTR_WEBHOOK_MESSAGE_ID, new MessageAttributeValue().withStringValue("messageId"),
			WebhookManager.MSG_ATTR_WEBHOOK_ENDPOINT, new MessageAttributeValue().withStringValue(webhook.getInvokeEndpoint()),
			WebhookManager.MSG_ATTR_WEBHOOK_ID, new MessageAttributeValue().withStringValue(webhook.getId()),
			WebhookManager.MSG_ATTR_WEBHOOK_MESSAGE_TYPE, new MessageAttributeValue().withStringValue(WebhookMessageType.Verification.name())
		);
		
		assertEquals("Could not find attribute: WebhookOwnerId", assertThrows(IllegalStateException.class, () -> {
			// Call under test
			new WebhookMessageAttributes(messageAttributes);
		}).getMessage());
		
	}
	
	@Test
	public void testWebhookMessageAttributesWithMissingWebhookEndpoint() {
		Map<String, MessageAttributeValue> messageAttributes = Map.of(
			WebhookManager.MSG_ATTR_WEBHOOK_MESSAGE_ID, new MessageAttributeValue().withStringValue("messageId"),
			WebhookManager.MSG_ATTR_WEBHOOK_OWNER_ID, new MessageAttributeValue().withStringValue(webhook.getCreatedBy()),
			WebhookManager.MSG_ATTR_WEBHOOK_ID, new MessageAttributeValue().withStringValue(webhook.getId()),
			WebhookManager.MSG_ATTR_WEBHOOK_MESSAGE_TYPE, new MessageAttributeValue().withStringValue(WebhookMessageType.Verification.name())
		);
		
		assertEquals("Could not find attribute: WebhookEndpoint", assertThrows(IllegalStateException.class, () -> {
			// Call under test
			new WebhookMessageAttributes(messageAttributes);
		}).getMessage());
		
	}
	
	@Test
	public void testWebhookMessageAttributesWithMissingMessageType() {
		Map<String, MessageAttributeValue> messageAttributes = Map.of(
			WebhookManager.MSG_ATTR_WEBHOOK_MESSAGE_ID, new MessageAttributeValue().withStringValue("messageId"),
			WebhookManager.MSG_ATTR_WEBHOOK_OWNER_ID, new MessageAttributeValue().withStringValue(webhook.getCreatedBy()),
			WebhookManager.MSG_ATTR_WEBHOOK_ID, new MessageAttributeValue().withStringValue(webhook.getId()),
			WebhookManager.MSG_ATTR_WEBHOOK_ENDPOINT, new MessageAttributeValue().withStringValue(webhook.getInvokeEndpoint())
		);
		
		assertEquals("Could not find attribute: WebhookMessageType", assertThrows(IllegalStateException.class, () -> {
			// Call under test
			new WebhookMessageAttributes(messageAttributes);
		}).getMessage());
		
	}
	
	@Test
	public void testWebhookMessageAttributesWithMissingMessageId() {
		Map<String, MessageAttributeValue> messageAttributes = Map.of(
			WebhookManager.MSG_ATTR_WEBHOOK_OWNER_ID, new MessageAttributeValue().withStringValue(webhook.getCreatedBy()),
			WebhookManager.MSG_ATTR_WEBHOOK_ID, new MessageAttributeValue().withStringValue(webhook.getId()),
			WebhookManager.MSG_ATTR_WEBHOOK_ENDPOINT, new MessageAttributeValue().withStringValue(webhook.getInvokeEndpoint()),
			WebhookManager.MSG_ATTR_WEBHOOK_MESSAGE_TYPE, new MessageAttributeValue().withStringValue(WebhookMessageType.Verification.name())
		);
		
		assertEquals("Could not find attribute: WebhookMessageId", assertThrows(IllegalStateException.class, () -> {
			// Call under test
			new WebhookMessageAttributes(messageAttributes);
		}).getMessage());
		
	}
	
	@Test
	public void testUpdateVerificationStatus() {
		messageType = WebhookMessageType.Verification;
		WebhookMessageAttributes attributes = expectedAttributes();
		boolean success = true;
		Exception ex = null;
		
		// Call under test
		dispatcher.updateVerificationStatus(attributes, success, mockResponse, ex);
		
		verify(mockManager).updateWebhookVerificationStatus(webhook.getId(), attributes.getMessageId(), WebhookVerificationStatus.CODE_SENT, "A code was sent to the webhook endpoint.");
	}
	
	@Test
	public void testUpdateVerificationStatusWithSynapseEvent() {
		messageType = WebhookMessageType.SynapseEvent;
		WebhookMessageAttributes attributes = expectedAttributes();
		boolean success = true;
		Exception ex = null;
		
		// Call under test
		dispatcher.updateVerificationStatus(attributes, success, mockResponse, ex);
		
		verifyZeroInteractions(mockManager);
	}
	
	@Test
	public void testUpdateVerificationStatusWithFailedAndResponse() {
		messageType = WebhookMessageType.Verification;
		WebhookMessageAttributes attributes = expectedAttributes();
		boolean success = false;
		Exception ex = null;
	
		when(mockResponse.statusCode()).thenReturn(404);
		
		// Call under test
		dispatcher.updateVerificationStatus(attributes, success, mockResponse, ex);
		
		verify(mockManager).updateWebhookVerificationStatus(webhook.getId(), attributes.getMessageId(), WebhookVerificationStatus.FAILED, "The request to the webhook endpoint failed with status 404.");
	}
	
	static Stream<Arguments> updateVerificationStatusExceptions() {
		return Stream.of(
			Arguments.of(null, "unknown"),
			Arguments.of(new RuntimeException("Failed"), "unknown"),
			Arguments.of(new InterruptedException("Interrupted"), "request timeout"),
			Arguments.of(new TimeoutException("Timeout"), "request timeout"),
			Arguments.of(new HttpTimeoutException("Timeout"), "request timeout"),
			Arguments.of(new HttpConnectTimeoutException("Timeout"), "connection timeout"),
			Arguments.of(new ConnectException("Timeout"), "connection timeout")
		);
	}
	
	@ParameterizedTest
	@MethodSource("updateVerificationStatusExceptions")
	public void testUpdateVerificationStatusWithFailedAndException(Throwable ex, String expectedReason) {
		messageType = WebhookMessageType.Verification;
		WebhookMessageAttributes attributes = expectedAttributes();
		boolean success = false;
		mockResponse = null;
		
		// Call under test
		dispatcher.updateVerificationStatus(attributes, success, mockResponse, ex);
		
		verify(mockManager).updateWebhookVerificationStatus(webhook.getId(), attributes.getMessageId(), WebhookVerificationStatus.FAILED, "The request to the webhook endpoint failed (Reason: " + expectedReason + ").");
	}
	
	@ParameterizedTest
	@MethodSource("updateVerificationStatusExceptions")
	public void testUpdateVerificationStatusWithFailedAndExecutionException(Throwable ex, String expectedReason) {
		messageType = WebhookMessageType.Verification;
		WebhookMessageAttributes attributes = expectedAttributes();
		boolean success = false;
		mockResponse = null;
		ex = new ExecutionException(ex);
		
		// Call under test
		dispatcher.updateVerificationStatus(attributes, success, mockResponse, ex);
		
		verify(mockManager).updateWebhookVerificationStatus(webhook.getId(), attributes.getMessageId(), WebhookVerificationStatus.FAILED, "The request to the webhook endpoint failed (Reason: " + expectedReason + ").");
	}
	
}
