package org.sagebionetworks.grid.workers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.grid.workers.message.AbstractJsonRxMessage;
import org.sagebionetworks.grid.workers.message.JsonRxMessageBase;
import org.sagebionetworks.grid.workers.message.factory.JsonRxMessageFactory;
import org.sagebionetworks.repo.manager.grid.response.GridEventResponsePublisher;
import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.EventType;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessageType;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.context.ApplicationEventPublisher;

import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;

@ExtendWith(MockitoExtension.class)
public class GridEventBrokerWorkerUnitTest {

	@Mock
	private ProgressCallback mockProgressCallback;

	@Mock
	private GridEventResponsePublisher mockPublisher;

	@Mock
	private ApplicationEventPublisher mockApplicationEventPublisher;

	@Mock
	private JsonRxMessageFactory<JsonRxMessageBase> mockFactory;

	@Mock
	private EventContext mockContext;

	@Mock
	private Message mockMessage;

	@Captor
	private ArgumentCaptor<JSONArray> arrayCaptor;

	private List<JsonRxMessageFactory<?>> factories;
	private GridEventBrokerWorker broker;
	private GridEventBrokerWorker brokerSpy;
	private TestMessage testMessage;
	private TestMessage testMessageTwo;

	@BeforeEach
	public void before() {
		factories = List.of(mockFactory);
		broker = new GridEventBrokerWorker(mockPublisher, mockApplicationEventPublisher, factories);
		brokerSpy = Mockito.spy(broker);
		testMessage = new TestMessage(mockContext, 1, arrayCaptor);
		testMessageTwo = new TestMessage(mockContext, 2, arrayCaptor);
	}

	@ParameterizedTest
	@MethodSource("provideAllTestCases")
	void testCreateEventWithEachType(ExampleEvent example) {

		when(mockFactory.type()).thenReturn(example.type);
		broker = new GridEventBrokerWorker(mockPublisher, mockApplicationEventPublisher, factories);

		TestMessage expected = new TestMessage(mockContext, example.id, example.getBody());
		when(mockFactory.createMessage(mockContext, example.id, example.method, example.body)).thenReturn(expected);

		// call under test
		Object result = broker.createEvent(mockContext, example.createMessage());
		assertEquals(expected, result);
	}

	/**
	 * Provides an example of each unique combination of elements in a JSONRx
	 * message.
	 * 
	 * @return
	 */
	private static Stream<Arguments> provideAllTestCases() {
		return Stream.of(
				// no id and no body
				Arguments.of(new ExampleEvent().setType(JsonRxMessageType.Notification).setMethod("ping")),
				// no id and JSONObject body
				Arguments.of(new ExampleEvent().setType(JsonRxMessageType.Notification).setMethod("hasBody")
						.setBody(new JSONObject("{\"key\":4}"))),
				// no id and JSONArray body
				Arguments.of(new ExampleEvent().setType(JsonRxMessageType.Notification).setMethod("hasBodyArray")
						.setBody(new JSONArray("[3,4]"))),
				// All parts with JSONArray body
				Arguments.of(new ExampleEvent().setType(JsonRxMessageType.RequestData).setId(44)
						.setMethod("hasBodyArray").setBody(new JSONArray("[3,4]"))),
				// All parts with JSONObject body
				Arguments.of(new ExampleEvent().setType(JsonRxMessageType.RequestData).setId(44).setMethod("hasBody")
						.setBody(new JSONObject("{\"key\":4}"))),
				// No method and no body.
				Arguments.of(new ExampleEvent().setType(JsonRxMessageType.RequestUnsubscribe).setId(44)),
				// No method and JSONObject body
				Arguments.of(
						new ExampleEvent().setType(JsonRxMessageType.RequestUnsubscribe).setId(44)
								.setBody(new JSONObject("{\"key\":4}")),
						// No method and JSONArray body
						Arguments.of(new ExampleEvent().setType(JsonRxMessageType.RequestUnsubscribe).setId(44)
								.setBody(new JSONArray("[3,4]"))))

		);
	}

	@Test
	public void testCreateEventWithEmptyArray() {

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			broker.createEvent(mockContext, new JSONArray("[]"));
		}).getMessage();
		assertEquals("Expected the fist element of the array to be a message code.", message);
	}

	@Test
	public void testCreateEventWithUnknowCode() {

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			broker.createEvent(mockContext, new JSONArray("[100,1]"));
		}).getMessage();
		assertEquals("Unknown JSON-Rx code: 100", message);
	}

	@Test
	public void testCreateEventWithNoBuidler() {

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			broker.createEvent(mockContext, new JSONArray("[8,\"foo\"]"));
		}).getMessage();
		assertEquals("Unknown message type -- code: 8 and method: 'foo'", message);
	}

	@Test
	public void testCreateEventWithNoBuidlerAndNullMethod() {

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			broker.createEvent(mockContext, new JSONArray("[8,1]"));
		}).getMessage();
		assertEquals("Unknown message type -- code: 8 and method: ''", message);
	}

	@Test
	public void testCreateEventsWithBatch() {

		JSONArray batch = new JSONArray("[[1,2],[3,4]]");
		doReturn(testMessage, testMessageTwo).when(brokerSpy).createEvent(eq(mockContext), arrayCaptor.capture());
		// call under test
		List<JsonRxMessageBase> results = brokerSpy.createEvents(mockContext, batch);
		List<Object> expected = List.of(testMessage, testMessageTwo);
		assertEquals(results, expected);
		List<String> arrayString = arrayCaptor.getAllValues().stream().map(a -> a.toString())
				.collect(Collectors.toList());
		assertEquals(List.of("[1,2]", "[3,4]"), arrayString);

	}

	@Test
	public void testCreateEventsWithBatchNotAllArrays() {

		JSONArray batch = new JSONArray("[[1,2],{\"a\":0}]");
		String message = assertThrows(IllegalArgumentException.class, () -> {

			// call under test
			brokerSpy.createEvents(mockContext, batch);
		}).getMessage();
		assertEquals("The message at index: 1 is not a JSON array", message);
	}

	@Test
	public void testCreateEventsWithNonbatch() {

		JSONArray batch = new JSONArray("[1,2]");
		doReturn(testMessage).when(brokerSpy).createEvent(eq(mockContext), arrayCaptor.capture());
		// call under test
		List<JsonRxMessageBase> results = brokerSpy.createEvents(mockContext, batch);
		List<Object> expected = List.of(testMessage);
		assertEquals(results, expected);
		List<String> arrayString = arrayCaptor.getAllValues().stream().map(a -> a.toString())
				.collect(Collectors.toList());
		assertEquals(List.of("[1,2]"), arrayString);
	}

	@Test
	public void testGetMessageAttributeNames() {
		// call under test
		assertEquals(List.of(".*"), broker.getMessageAttributeNames());
	}

	@Test
	public void testBuildEventContext() {
		Map<String, MessageAttributeValue> attributes = new HashMap<>();
		attributes.put("EventType", new MessageAttributeValue().withStringValue(EventType.MESSAGE.name()));
		attributes.put("EventSource", new MessageAttributeValue().withStringValue(EventSource.WEBSOCKET.name()));
		attributes.put("ConnectionId", new MessageAttributeValue().withStringValue("c123"));
		when(mockMessage.getMessageAttributes()).thenReturn(attributes);

		// call under test
		EventContext context = GridEventBrokerWorker.buildEventContext(mockMessage);
		EventContext expected = new EventContext(EventType.MESSAGE, EventSource.WEBSOCKET, "c123");
		assertEquals(expected, context);
	}

	@Test
	public void testBuildEventContextWithNullEventType() {
		Map<String, MessageAttributeValue> attributes = new HashMap<>();
		attributes.put("EventSource", new MessageAttributeValue().withStringValue(EventSource.WEBSOCKET.name()));
		attributes.put("ConnectionId", new MessageAttributeValue().withStringValue("c123"));
		when(mockMessage.getMessageAttributes()).thenReturn(attributes);

		String message = assertThrows(IllegalStateException.class, () -> {
			// call under test
			GridEventBrokerWorker.buildEventContext(mockMessage);
		}).getMessage();
		assertEquals("attribute.EventType is required.", message);
	}

	@Test
	public void testBuildEventContextWithNullEventSource() {
		Map<String, MessageAttributeValue> attributes = new HashMap<>();
		attributes.put("EventType", new MessageAttributeValue().withStringValue(EventType.MESSAGE.name()));
		attributes.put("ConnectionId", new MessageAttributeValue().withStringValue("c123"));
		when(mockMessage.getMessageAttributes()).thenReturn(attributes);

		String message = assertThrows(IllegalStateException.class, () -> {
			// call under test
			GridEventBrokerWorker.buildEventContext(mockMessage);
		}).getMessage();
		assertEquals("attribute.EventSource is required.", message);
	}

	@Test
	public void testBuildEventContextWithNullConnectionId() {
		Map<String, MessageAttributeValue> attributes = new HashMap<>();
		attributes.put("EventType", new MessageAttributeValue().withStringValue(EventType.MESSAGE.name()));
		attributes.put("EventSource", new MessageAttributeValue().withStringValue(EventSource.WEBSOCKET.name()));
		when(mockMessage.getMessageAttributes()).thenReturn(attributes);

		String message = assertThrows(IllegalStateException.class, () -> {
			// call under test
			GridEventBrokerWorker.buildEventContext(mockMessage);
		}).getMessage();
		assertEquals("attribute.ConnectionId is required.", message);
	}

	@Test
	public void testRun() throws RecoverableMessageException, Exception {
		Map<String, MessageAttributeValue> attributes = new HashMap<>();
		attributes.put("EventType", new MessageAttributeValue().withStringValue(EventType.MESSAGE.name()));
		attributes.put("EventSource", new MessageAttributeValue().withStringValue(EventSource.WEBSOCKET.name()));
		attributes.put("ConnectionId", new MessageAttributeValue().withStringValue("c123"));
		when(mockMessage.getMessageAttributes()).thenReturn(attributes);
		when(mockMessage.getBody()).thenReturn("[8,\"ping\"]");

		EventContext context = GridEventBrokerWorker.buildEventContext(mockMessage);

		doReturn(List.of(testMessage, testMessageTwo)).when(brokerSpy).createEvents(eq(context), arrayCaptor.capture());

		// call under test
		brokerSpy.run(mockProgressCallback, mockMessage);

		verify(mockApplicationEventPublisher).publishEvent(testMessage);
		verify(mockApplicationEventPublisher).publishEvent(testMessageTwo);
		List<String> arrayString = arrayCaptor.getAllValues().stream().map(a -> a.toString())
				.collect(Collectors.toList());
		assertEquals(List.of("[8,\"ping\"]"), arrayString);
		verifyZeroInteractions(mockPublisher);

	}

	@Test
	public void testRunWithNonJSON() throws RecoverableMessageException, Exception {
		Map<String, MessageAttributeValue> attributes = new HashMap<>();
		attributes.put("EventType", new MessageAttributeValue().withStringValue(EventType.MESSAGE.name()));
		attributes.put("EventSource", new MessageAttributeValue().withStringValue(EventSource.WEBSOCKET.name()));
		attributes.put("ConnectionId", new MessageAttributeValue().withStringValue("c123"));
		when(mockMessage.getMessageAttributes()).thenReturn(attributes);
		when(mockMessage.getBody()).thenReturn("not JSON");

		EventContext context = GridEventBrokerWorker.buildEventContext(mockMessage);

		// call under test
		brokerSpy.run(mockProgressCallback, mockMessage);

		verify(mockPublisher).publishEventResponse(context,
				"[8,\"error\",{\"message\":\"A JSONArray text must start with '[' at 1 [character 2 line 1]\",\"code\":\"Bad Request\",\"errno\":400}]");

	}

	@Test
	public void testRunWithIllegalArgument() throws RecoverableMessageException, Exception {
		Map<String, MessageAttributeValue> attributes = new HashMap<>();
		attributes.put("EventType", new MessageAttributeValue().withStringValue(EventType.MESSAGE.name()));
		attributes.put("EventSource", new MessageAttributeValue().withStringValue(EventSource.WEBSOCKET.name()));
		attributes.put("ConnectionId", new MessageAttributeValue().withStringValue("c123"));
		when(mockMessage.getMessageAttributes()).thenReturn(attributes);
		when(mockMessage.getBody()).thenReturn("[1,2]");

		EventContext context = GridEventBrokerWorker.buildEventContext(mockMessage);

		doReturn(List.of(testMessage)).when(brokerSpy).createEvents(eq(context), arrayCaptor.capture());
		doThrow(new IllegalArgumentException("wrong")).when(mockApplicationEventPublisher).publishEvent(testMessage);

		// call under test
		brokerSpy.run(mockProgressCallback, mockMessage);

		verify(mockPublisher).publishEventResponse(context,
				"[8,\"error\",{\"message\":\"wrong\",\"code\":\"Bad Request\",\"errno\":400}]");

	}

	@Test
	public void testRunWithIllegalState() throws RecoverableMessageException, Exception {
		Map<String, MessageAttributeValue> attributes = new HashMap<>();
		attributes.put("EventType", new MessageAttributeValue().withStringValue(EventType.MESSAGE.name()));
		attributes.put("EventSource", new MessageAttributeValue().withStringValue(EventSource.WEBSOCKET.name()));
		attributes.put("ConnectionId", new MessageAttributeValue().withStringValue("c123"));
		when(mockMessage.getMessageAttributes()).thenReturn(attributes);
		when(mockMessage.getBody()).thenReturn("[1,2]");

		EventContext context = GridEventBrokerWorker.buildEventContext(mockMessage);

		doReturn(List.of(testMessage)).when(brokerSpy).createEvents(eq(context), arrayCaptor.capture());
		doThrow(new IllegalStateException("wrong")).when(mockApplicationEventPublisher).publishEvent(testMessage);

		// call under test
		brokerSpy.run(mockProgressCallback, mockMessage);

		verify(mockPublisher).publishEventResponse(context,
				"[8,\"error\",{\"message\":\"wrong\",\"code\":\"Internal Server Error\",\"errno\":500}]");
	}

	@Test
	public void testRunWithNullContext() throws RecoverableMessageException, Exception {
		Map<String, MessageAttributeValue> attributes = new HashMap<>();
		attributes.put("EventType", new MessageAttributeValue().withStringValue(EventType.MESSAGE.name()));
		attributes.put("EventSource", new MessageAttributeValue().withStringValue(EventSource.WEBSOCKET.name()));

		// The connection ID is missing so this will fail to create a context.
		when(mockMessage.getMessageAttributes()).thenReturn(attributes);

		// call under test
		brokerSpy.run(mockProgressCallback, mockMessage);

		verifyZeroInteractions(mockPublisher);
	}

	/**
	 * Helper to build an example event.
	 */
	private static class ExampleEvent {

		private JsonRxMessageType type;
		private String method;
		private Integer id;
		private Object body;

		public JsonRxMessageType getType() {
			return type;
		}

		public ExampleEvent setType(JsonRxMessageType type) {
			this.type = type;
			return this;
		}

		public String getMethod() {
			return method;
		}

		public ExampleEvent setMethod(String method) {
			this.method = method;
			return this;
		}

		public Integer getId() {
			return id;
		}

		public ExampleEvent setId(Integer id) {
			this.id = id;
			return this;
		}

		public Object getBody() {
			return body;
		}

		public ExampleEvent setBody(Object body) {
			this.body = body;
			return this;
		}

		/**
		 * Build the message for this case.
		 * 
		 * @return
		 */
		public JSONArray createMessage() {
			JSONArray array = new JSONArray();
			int index = 0;
			array.put(index++, type.getCode());
			if (id != null) {
				array.put(index++, id);
			}
			if (method != null) {
				array.put(index++, method);
			}
			if (body != null) {
				array.put(index++, body);
			}
			return array;
		}

		@Override
		public int hashCode() {
			return Objects.hash(body, id, method, type);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ExampleEvent other = (ExampleEvent) obj;
			return Objects.equals(body, other.body) && Objects.equals(id, other.id)
					&& Objects.equals(method, other.method) && type == other.type;
		}

		@Override
		public String toString() {
			return "ExampleEvent [type=" + type + ", method=" + method + ", id=" + id + ", body=" + body + "]";
		}

	}

	private static class TestMessage extends AbstractJsonRxMessage {

		public TestMessage(EventContext context, Integer id, Object body) {
			super(context, id, body);
		}

	}
}
