package org.sagebionetworks.repo.manager.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.agent.Agent.AgentRole;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;

@ExtendWith(MockitoExtension.class)
public class AgentTest {

	@Mock
	private ChatClientRequestSpec requestSpec;
	@Mock
	private CallResponseSpec responseSpec;
	@Mock
	private ChatResponse chatResponse;

	private ToolContext context = new ToolContext(java.util.Map.of());

	@BeforeEach
	public void before() {
		// chat() attaches the turn-limit advisors and seeds the counter on the spec before calling it; the
		// spec is a builder, so return it from advisors(...) so the chain reaches call(). The advisor wiring
		// itself is exercised against a real ChatClient in AgentTurnLimitWiringTest.
		when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
	}

	/**
	 * Minimal {@link Agent} of the given type whose only job is to hand back the mocked request spec, so
	 * the default {@code chat()} finish-reason and response-truncation handling can be exercised in
	 * isolation.
	 */
	private Agent agentReturning(ChatClientRequestSpec spec, AgentRole role) {
		return new Agent() {
			@Override
			public AgentRole getAgentRole() {
				return role;
			}

			@Override
			public ChatClientRequestSpec prepareChatClientRequestSpec(String message, ToolContext ctx) {
				return spec;
			}
		};
	}

	@Test
	public void testChatWithNormalCompletion() {
		when(requestSpec.call()).thenReturn(responseSpec);
		when(responseSpec.chatResponse()).thenReturn(chatResponse);
		when(chatResponse.hasFinishReasons(Set.of(Agent.MAX_TOKENS_FINISH_REASON))).thenReturn(false);
		when(chatResponse.getResult()).thenReturn(new Generation(new AssistantMessage("all done")));

		// call under test
		String result = agentReturning(requestSpec, AgentRole.SPECIALIST).chat("do the work", context);

		assertEquals("all done", result);
	}

	@Test
	public void testChatWithMaxTokensTruncation() {
		when(requestSpec.call()).thenReturn(responseSpec);
		when(responseSpec.chatResponse()).thenReturn(chatResponse);
		when(chatResponse.hasFinishReasons(Set.of(Agent.MAX_TOKENS_FINISH_REASON))).thenReturn(true);

		// call under test — a truncated turn returns the corrective message instead of the narration
		String result = agentReturning(requestSpec, AgentRole.SPECIALIST).chat("apply a huge batch", context);

		assertEquals(Agent.TRUNCATED_RESPONSE_MESSAGE, result);
	}

	@Test
	public void testChatWithNullChatResponse() {
		when(requestSpec.call()).thenReturn(responseSpec);
		when(responseSpec.chatResponse()).thenReturn(null);

		// call under test
		String result = agentReturning(requestSpec, AgentRole.SPECIALIST).chat("do the work", context);

		assertNull(result);
	}

	@Test
	public void testChatWithNullGeneration() {
		when(requestSpec.call()).thenReturn(responseSpec);
		when(responseSpec.chatResponse()).thenReturn(chatResponse);
		when(chatResponse.hasFinishReasons(Set.of(Agent.MAX_TOKENS_FINISH_REASON))).thenReturn(false);
		when(chatResponse.getResult()).thenReturn(null);

		// call under test
		String result = agentReturning(requestSpec, AgentRole.SPECIALIST).chat("do the work", context);

		assertNull(result);
	}

	@Test
	public void testChatWithSpecialistResponseAtLimit() {
		String atLimit = "a".repeat(Agent.MAX_RESPONSE_CHARACTERS);
		stubNormalCompletion(atLimit);

		// call under test — text exactly at the limit is returned unchanged
		String result = agentReturning(requestSpec, AgentRole.SPECIALIST).chat("do the work", context);

		assertEquals(atLimit, result);
	}

	@Test
	public void testChatWithSpecialistResponseExceedingLimit() {
		String overLimit = "a".repeat(Agent.MAX_RESPONSE_CHARACTERS + 1);
		stubNormalCompletion(overLimit);

		// call under test — a specialist's oversized text is cut to the limit with the truncation suffix
		String result = agentReturning(requestSpec, AgentRole.SPECIALIST).chat("do the work", context);

		assertEquals("a".repeat(Agent.MAX_RESPONSE_CHARACTERS) + Agent.RESPONSE_TRUNCATED_SUFFIX, result);
	}

	@Test
	public void testChatWithSupervisorResponseExceedingSpecialistLimit() {
		String overSpecialistLimit = "a".repeat(Agent.MAX_RESPONSE_CHARACTERS + 1);
		stubNormalCompletion(overSpecialistLimit);

		// call under test — a supervisor's response goes to the end user, so it is never truncated
		String result = agentReturning(requestSpec, AgentRole.SUPERVISOR).chat("do the work", context);

		assertEquals(overSpecialistLimit, result);
	}

	private void stubNormalCompletion(String responseText) {
		when(requestSpec.call()).thenReturn(responseSpec);
		when(responseSpec.chatResponse()).thenReturn(chatResponse);
		when(chatResponse.hasFinishReasons(Set.of(Agent.MAX_TOKENS_FINISH_REASON))).thenReturn(false);
		when(chatResponse.getResult()).thenReturn(new Generation(new AssistantMessage(responseText)));
	}
}
