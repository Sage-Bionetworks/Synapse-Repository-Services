package org.sagebionetworks.repo.manager.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springaicommunity.agentcore.codeinterpreter.AgentCoreCodeInterpreterClient;
import org.springframework.ai.chat.model.ToolContext;

import software.amazon.awssdk.services.bedrockagentcore.BedrockAgentCoreClient;
import software.amazon.awssdk.services.bedrockagentcore.model.CodeInterpreterSessionStatus;
import software.amazon.awssdk.services.bedrockagentcore.model.CodeInterpreterSessionSummary;
import software.amazon.awssdk.services.bedrockagentcore.model.ListCodeInterpreterSessionsRequest;
import software.amazon.awssdk.services.bedrockagentcore.model.ListCodeInterpreterSessionsResponse;

@ExtendWith(MockitoExtension.class)
public class CodeInterpreterSessionProviderTest {

	@Mock
	private BedrockAgentCoreClient bedrockAgentCoreClient;
	@Mock
	private AgentCoreCodeInterpreterClient codeInterpreterClient;

	@InjectMocks
	private CodeInterpreterSessionProvider provider;

	private static final String AGENT_SESSION_ID = "abc-123-def";
	private static final String EXPECTED_NAME = "curie_abc-123-def";

	private CodeInterpreterSessionSummary summary(String name, String sessionId) {
		return CodeInterpreterSessionSummary.builder()
				.name(name)
				.sessionId(sessionId)
				.status(CodeInterpreterSessionStatus.READY)
				.build();
	}

	private ListCodeInterpreterSessionsResponse page(String nextToken, CodeInterpreterSessionSummary... items) {
		return ListCodeInterpreterSessionsResponse.builder()
				.items(List.of(items))
				.nextToken(nextToken)
				.build();
	}

	@Test
	public void testGetOrCreateSessionWithExistingReadySession() {
		when(bedrockAgentCoreClient.listCodeInterpreterSessions(any(ListCodeInterpreterSessionsRequest.class)))
				.thenReturn(page(null, summary("someone_else", "aws-other"), summary(EXPECTED_NAME, "aws-match")));

		// call under test
		String result = provider.getOrCreateSession(AGENT_SESSION_ID);

		assertEquals("aws-match", result);
		verify(codeInterpreterClient, never()).startSession(any());
	}

	@Test
	public void testGetOrCreateSessionWithNoMatchCreates() {
		when(bedrockAgentCoreClient.listCodeInterpreterSessions(any(ListCodeInterpreterSessionsRequest.class)))
				.thenReturn(page(null, summary("someone_else", "aws-other")));
		when(codeInterpreterClient.startSession(EXPECTED_NAME)).thenReturn("aws-new");

		// call under test
		String result = provider.getOrCreateSession(AGENT_SESSION_ID);

		assertEquals("aws-new", result);
		verify(codeInterpreterClient).startSession(EXPECTED_NAME);
	}

	@Test
	public void testGetOrCreateSessionWithOnlyTerminatedCreates() {
		// A timed-out session has left READY, so the status=READY filter returns no items and a fresh
		// session is created — expiry handling is automatic.
		when(bedrockAgentCoreClient.listCodeInterpreterSessions(any(ListCodeInterpreterSessionsRequest.class)))
				.thenReturn(page(null));
		when(codeInterpreterClient.startSession(EXPECTED_NAME)).thenReturn("aws-new");

		// call under test
		String result = provider.getOrCreateSession(AGENT_SESSION_ID);

		assertEquals("aws-new", result);
		verify(codeInterpreterClient).startSession(EXPECTED_NAME);
	}

	@Test
	public void testGetOrCreateSessionWithMatchOnSecondPage() {
		when(bedrockAgentCoreClient.listCodeInterpreterSessions(any(ListCodeInterpreterSessionsRequest.class)))
				.thenReturn(page("token", summary("someone_else", "aws-other")))
				.thenReturn(page(null, summary(EXPECTED_NAME, "aws-match")));

		// call under test
		String result = provider.getOrCreateSession(AGENT_SESSION_ID);

		assertEquals("aws-match", result);
		verify(codeInterpreterClient, never()).startSession(any());
	}

	@Test
	public void testCodeSessionNameWithDisallowedCharacters() {
		// call under test
		assertEquals("curie_abc_123_xyz", provider.codeSessionName("abc.123 xyz"));
	}

	@Test
	public void testCodeSessionNameKeepsHyphensAndUnderscores() {
		// call under test
		assertEquals(EXPECTED_NAME, provider.codeSessionName(AGENT_SESSION_ID));
	}

	@Test
	public void testLazySupplierIsLazyAndMemoizes() {
		when(bedrockAgentCoreClient.listCodeInterpreterSessions(any(ListCodeInterpreterSessionsRequest.class)))
				.thenReturn(page(null, summary(EXPECTED_NAME, "aws-match")));

		CodeSessionSupplier supplier = provider.lazySupplier(AGENT_SESSION_ID);
		// Not created until first invoked; a peek at the memoized id does not resolve it.
		assertEquals(null, supplier.resolvedSessionIdOrNull());
		verifyNoInteractions(bedrockAgentCoreClient);

		// call under test
		assertEquals("aws-match", supplier.getSessionId());
		assertEquals("aws-match", supplier.getSessionId());

		// Once resolved, the id is visible without re-resolving.
		assertEquals("aws-match", supplier.resolvedSessionIdOrNull());

		// Memoized: the second call did not re-list.
		verify(bedrockAgentCoreClient).listCodeInterpreterSessions(any(ListCodeInterpreterSessionsRequest.class));
	}

	@Test
	public void testLazySupplierWithBlankSessionId() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			provider.lazySupplier(" ");
		}).getMessage();
		assertEquals("agentSessionId is required and must not be a blank string.", message);
	}

	@Test
	public void testResolveSessionIdWithLazySupplier() {
		CodeSessionSupplier supplier = () -> "aws-resolved";
		ToolContext toolContext = new ToolContext(
				java.util.Map.of(AgentToolContextKey.CODE_SESSION_SUPPLIER.getKey(), supplier));

		// call under test -- interactive Curie path: the lazy supplier is invoked to resolve the session.
		assertEquals("aws-resolved", CodeSessionSupplier.resolveSessionId(toolContext));
	}

	@Test
	public void testResolveSessionIdWithConstantSupplier() {
		ToolContext toolContext = new ToolContext(java.util.Map.of(AgentToolContextKey.CODE_SESSION_SUPPLIER.getKey(),
				CodeSessionSupplier.of("aws-started")));

		// call under test -- batch/specialist path: a constant supplier over an already-started session.
		assertEquals("aws-started", CodeSessionSupplier.resolveSessionId(toolContext));
	}

	@Test
	public void testResolveSessionIdWithNeitherPresent() {
		ToolContext toolContext = new ToolContext(java.util.Map.of(AgentToolContextKey.USER_INFO.getKey(), "someUser"));

		// call under test
		assertEquals(null, CodeSessionSupplier.resolveSessionId(toolContext));
	}
}
