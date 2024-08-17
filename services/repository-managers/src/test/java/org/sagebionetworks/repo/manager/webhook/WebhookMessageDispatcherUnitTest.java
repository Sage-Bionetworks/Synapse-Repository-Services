package org.sagebionetworks.repo.manager.webhook;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

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
import org.sagebionetworks.repo.model.webhook.Webhook;
import org.sagebionetworks.repo.model.webhook.WebhookVerificationStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.amazonaws.services.sqs.model.Message;

@ExtendWith(MockitoExtension.class)
public class WebhookMessageDispatcherUnitTest {
	
	@Mock
	private WebhookManager mockManager;
	
	@Mock
	private HttpClient mockClient;
	
	@Mock
	private StackConfiguration mockConfig;
	
	@InjectMocks
	@Spy
	private WebhookMessageDispatcher dispatcher;
	
	@Mock
	private Message mockMessage;
	
	@Mock
	private CompletableFuture<HttpResponse<Void>> mockFutureResponse;
	
	@Mock
	private HttpResponse<Void> mockResponse;
	
	@Captor
	private ArgumentCaptor<BiConsumer <HttpResponse<Void>, Throwable>> actionCaptor;
	
	private String userAgent;
	private Webhook webhook;

	@BeforeEach
	public void before() {
		when(mockConfig.getStackInstance()).thenReturn("123");
		// This is automatically invoked by spring
		dispatcher.configure(mockConfig);
		userAgent = "Synapse-Webhook/123";
		webhook = new Webhook()
			.setId("123")
			.setCreatedBy("456")
			.setInvokeEndpoint("https://my.endpoint");
	}
	
	@ParameterizedTest
	@EnumSource(WebhookMessageType.class)
	public void testDispatchMessage(WebhookMessageType messageType) throws Exception {
		when(mockMessage.getMessageAttributes()).thenReturn(WebhookManagerImpl.mapMessageAttributes(messageType.getMessageClass(), webhook));
		when(mockMessage.getBody()).thenReturn("messageBody");
		when(mockClient.sendAsync(any(), eq(WebhookMessageDispatcher.DISCARDING_BODY_HANDLER))).thenReturn(mockFutureResponse);
		
		// Call under test
		dispatcher.dispatchMessage(mockMessage);
		
		HttpRequest expectedRequest = HttpRequest.newBuilder(new URI(webhook.getInvokeEndpoint()))
				.timeout(WebhookMessageDispatcher.REQUEST_TIMEOUT)
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.header(HttpHeaders.USER_AGENT, userAgent)
				.header(WebhookMessageDispatcher.HEADER_WEBHOOK_ID, webhook.getId())
				.header(WebhookMessageDispatcher.HEADER_WEBHOOK_OWNER_ID, webhook.getCreatedBy())
				.header(WebhookMessageDispatcher.HEADER_WEBHOOK_MESSAGE_TYPE, messageType.name())
				.POST(BodyPublishers.ofString("messageBody"))
				.build();
		
		verify(mockClient).sendAsync(expectedRequest, WebhookMessageDispatcher.DISCARDING_BODY_HANDLER);
		verify(mockFutureResponse).whenComplete(actionCaptor.capture());
		
		BiConsumer <HttpResponse<Void>, Throwable> completionHandler = actionCaptor.getValue();
		
		doNothing().when(dispatcher).handleResponse(any(), any(), any(), any());
		
		// Mimic the future completition
		completionHandler.accept(mockResponse, null);
		
		verify(dispatcher).handleResponse(messageType, "123", mockResponse, null);
		
	}
	
	@Test
	public void testHandleResponseWithVerificationEventType() {
		when(mockResponse.statusCode()).thenReturn(200);
		
		doNothing().when(dispatcher).handleWebhookVerificationResponse("123", HttpStatus.OK, null);
		
		// Call under test
		dispatcher.handleResponse(WebhookMessageType.Verification, "123", mockResponse, null);
	}
	
	@Test
	public void testHandleResponseWithSynapseEventType() {
		when(mockResponse.statusCode()).thenReturn(200);
		
		doNothing().when(dispatcher).handleWebhookSynapseEventResponse("123", HttpStatus.OK, null);
		
		// Call under test
		dispatcher.handleResponse(WebhookMessageType.SynapseEvent, "123", mockResponse, null);
	}
	
	@Test
	public void testHandleResponseWithNoResponse() {
				
		doNothing().when(dispatcher).handleWebhookSynapseEventResponse("123", null, null);
		
		// Call under test
		dispatcher.handleResponse(WebhookMessageType.SynapseEvent, "123", null, null);
	}
	
	@Test
	public void testHandleResponseWithNoException() {
		
		Throwable ex = new RuntimeException();
				
		doNothing().when(dispatcher).handleWebhookSynapseEventResponse("123", null, ex);
		
		// Call under test
		dispatcher.handleResponse(WebhookMessageType.SynapseEvent, "123", null, ex);
	}
	
	@ParameterizedTest
	@EnumSource(value = HttpStatus.class, mode = Mode.INCLUDE, names = {"OK", "CREATED", "ACCEPTED", "NO_CONTENT"})
	public void testHandleWebhookVerificationEventResponseValidResponseCodes() {
		// Call under test
		dispatcher.handleWebhookVerificationResponse("123", HttpStatus.OK, null);
		
		verify(mockManager).updateWebhookVerificationStatus("123", WebhookVerificationStatus.CODE_SENT, "A verification code was sent to the webhook endpoint.");
	}
	
	@ParameterizedTest
	@EnumSource(value = HttpStatus.class, mode = Mode.EXCLUDE, names = {"OK", "CREATED", "ACCEPTED", "NO_CONTENT"})
	public void testHandleWebhookVerificationEventResponseWithNot2xx(HttpStatus status) {
		// Call under test
		dispatcher.handleWebhookVerificationResponse("123", status, null);
		
		verify(mockManager).updateWebhookVerificationStatus("123", WebhookVerificationStatus.FAILED, "The request to the webhook endpoint failed with status " + status.value() + ".");
	}

	@Test
	public void testHandleWebhookVerificationEventResponseWithNoStatus() {
		// Call under test
		dispatcher.handleWebhookVerificationResponse("123", null, null);
		
		verify(mockManager).updateWebhookVerificationStatus("123", WebhookVerificationStatus.FAILED, "The request to the webhook endpoint failed with no response.");
	}
	
	@Test
	public void testHandleWebhookVerificationEventResponseWithException() {
		Throwable ex = new RuntimeException("Nope");
		// Call under test
		dispatcher.handleWebhookVerificationResponse("123", null, ex);
		
		verify(mockManager).updateWebhookVerificationStatus("123", WebhookVerificationStatus.FAILED, "The request to the webhook endpoint failed.");
	}
}
