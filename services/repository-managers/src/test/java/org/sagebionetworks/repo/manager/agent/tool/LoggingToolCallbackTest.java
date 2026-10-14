package org.sagebionetworks.repo.manager.agent.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.agent.AgentToolContextKey;
import org.sagebionetworks.repo.manager.agent.CodeSessionSupplier;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.GridAgentSessionContext;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

@ExtendWith(MockitoExtension.class)
public class LoggingToolCallbackTest {

	@Mock
	private ToolCallback mockDelegate;

	@Mock
	private ToolDefinition mockToolDefinition;

	@Mock
	private ToolMetadata mockToolMetadata;

	@Mock
	private AgentTraceCallback mockTraceCallback;

	private LoggingToolCallback callback;

	@BeforeEach
	public void setup() {
		callback = new LoggingToolCallback(mockDelegate);
	}

	@Test
	public void testCallDelegatesAndReturnsResponse() {
		// The logger reads the tool name for every call.
		when(mockDelegate.getToolDefinition()).thenReturn(mockToolDefinition);
		when(mockToolDefinition.name()).thenReturn("queryGrid");
		ToolContext context = new ToolContext(Map.of(AgentToolContextKey.USER_INFO.getKey(), new UserInfo(false, 123L, AuthorizationConstants.DEFAULT_REALM_ID)));
		when(mockDelegate.call("the prompt", context)).thenReturn("the tool response");

		// call under test
		String response = callback.call("the prompt", context);

		assertEquals("the tool response", response);
		verify(mockDelegate).call("the prompt", context);
	}

	@Test
	public void testCallWithTraceCallbackRecordsInputAndResponse() {
		when(mockDelegate.getToolDefinition()).thenReturn(mockToolDefinition);
		when(mockToolDefinition.name()).thenReturn("askGridQuerySpecialist");
		ToolContext context = new ToolContext(Map.of(AgentToolContextKey.TRACE_CALLBACK.getKey(), mockTraceCallback));
		when(mockDelegate.call("the prompt", context)).thenReturn("the tool response");

		// call under test
		String response = callback.call("the prompt", context);

		assertEquals("the tool response", response);
		// The input is recorded before delegation and the response after, so trace reads as a
		// supervisor-to-specialist exchange.
		InOrder order = inOrder(mockTraceCallback);
		order.verify(mockTraceCallback)
				.addTraceToJob("Called tool 'askGridQuerySpecialist' with input: the prompt");
		order.verify(mockTraceCallback)
				.addTraceToJob("Tool 'askGridQuerySpecialist' responded with: the tool response");
	}

	@Test
	public void testCallWithoutTraceCallbackInContextDoesNotRecord() {
		when(mockDelegate.getToolDefinition()).thenReturn(mockToolDefinition);
		when(mockToolDefinition.name()).thenReturn("askGridQuerySpecialist");
		ToolContext context = new ToolContext(Map.of(AgentToolContextKey.USER_INFO.getKey(), new UserInfo(false, 123L, AuthorizationConstants.DEFAULT_REALM_ID)));
		when(mockDelegate.call("the prompt", context)).thenReturn("the tool response");

		// call under test
		callback.call("the prompt", context);

		verifyNoInteractions(mockTraceCallback);
	}

	@Test
	public void testCallWithNoContextDelegatesWithNull() {
		when(mockDelegate.getToolDefinition()).thenReturn(mockToolDefinition);
		when(mockToolDefinition.name()).thenReturn("queryGrid");
		when(mockDelegate.call("the prompt", null)).thenReturn("the tool response");

		// call under test
		String response = callback.call("the prompt");

		assertEquals("the tool response", response);
		verify(mockDelegate).call("the prompt", null);
	}

	@Test
	public void testGetToolDefinitionDelegates() {
		when(mockDelegate.getToolDefinition()).thenReturn(mockToolDefinition);

		// call under test
		assertSame(mockToolDefinition, callback.getToolDefinition());
	}

	@Test
	public void testGetToolMetadataDelegates() {
		when(mockDelegate.getToolMetadata()).thenReturn(mockToolMetadata);

		// call under test
		assertSame(mockToolMetadata, callback.getToolMetadata());
	}

	@Test
	public void testDescribeContextWithNull() {
		// call under test
		assertEquals("{}", LoggingToolCallback.describeContext(null));
	}

	@Test
	public void testDescribeContextWithEmptyContext() {
		// call under test
		assertEquals("{}", LoggingToolCallback.describeContext(new ToolContext(Map.of())));
	}

	@Test
	public void testDescribeContextWithIds() {
		UserInfo userInfo = new UserInfo(false, 123L, AuthorizationConstants.DEFAULT_REALM_ID);
		GridAgentSessionContext gridContext = new GridAgentSessionContext().setGridSessionId("grid-session-9")
				.setUsersReplicaId(42L);
		ToolContext context = new ToolContext(Map.of(AgentToolContextKey.USER_INFO.getKey(), userInfo,
				AgentToolContextKey.CODE_SESSION_SUPPLIER.getKey(), CodeSessionSupplier.of("session-abc"),
				AgentToolContextKey.GRID_SESSION_CONTEXT.getKey(), gridContext));

		// call under test
		String described = LoggingToolCallback.describeContext(context);

		// Correlation IDs are present. A constant supplier reports its id as already resolved.
		assertTrue(described.contains("userId=123"));
		assertTrue(described.contains("session-abc"));
		assertTrue(described.contains("gridSessionId=grid-session-9"));
		assertTrue(described.contains("usersReplicaId=42"));
		// The full UserInfo object must never be rendered (PII guard) -- only its id.
		assertFalse(described.contains(userInfo.toString()));
	}

	@Test
	public void testDescribeContextWithUnknownValueType() {
		// An unrecognized value is reduced to its class name, never its toString().
		ToolContext context = new ToolContext(Map.of("some-object", new StringBuilder("secret")));

		// call under test
		String described = LoggingToolCallback.describeContext(context);

		assertTrue(described.contains("some-object=StringBuilder"));
		assertFalse(described.contains("secret"));
	}
}
