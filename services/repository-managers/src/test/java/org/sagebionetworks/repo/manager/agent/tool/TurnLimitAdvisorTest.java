package org.sagebionetworks.repo.manager.agent.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.agent.AgentToolContextKey;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

@ExtendWith(MockitoExtension.class)
public class TurnLimitAdvisorTest {

	private static final String A_TOOL = "askSpecialist";

	@Mock
	private CallAdvisorChain chain;

	@Mock
	private ChatClientResponse chainResponse;

	/**
	 * Builds a request carrying the tool the model would loop on, and (optionally) a value seeded under the
	 * key the advisor reads. Accepts {@link Object} so tests can seed a wrong-typed value (or none) to
	 * exercise the fail-fast path.
	 */
	private ChatClientRequest requestWithCounter(Object counter) {
		ToolCallingChatOptions options = ToolCallingChatOptions.builder().toolNames(A_TOOL).build();
		Prompt prompt = new Prompt(List.of(new UserMessage("do work")), options);
		Map<String, Object> context = new HashMap<>();
		if (counter != null) {
			context.put(AgentToolContextKey.TURN_COUNT.getKey(), counter);
		}
		return ChatClientRequest.builder().prompt(prompt).context(context).build();
	}

	@Test
	public void testAdviseCallWithinBudget() {
		AtomicInteger counter = new AtomicInteger(0);
		ChatClientRequest request = requestWithCounter(counter);
		when(chain.nextCall(any())).thenReturn(chainResponse);
		TurnLimitAdvisor advisor = new TurnLimitAdvisor(3);

		// call under test
		ChatClientResponse result = advisor.adviseCall(request, chain);

		assertSame(chainResponse, result);
		assertEquals(1, counter.get());
		// A turn within the budget is passed downstream to the model untouched.
		ArgumentCaptor<ChatClientRequest> captor = ArgumentCaptor.forClass(ChatClientRequest.class);
		verify(chain).nextCall(captor.capture());
		assertSame(request, captor.getValue());
	}

	@Test
	public void testAdviseCallOnLastBudgetedTurn() {
		// Seeded so this call is the maxTurns-th turn: the model still gets called (the limit is not yet
		// exceeded), so a model that finishes on its last budgeted turn can still succeed.
		AtomicInteger counter = new AtomicInteger(2);
		ChatClientRequest request = requestWithCounter(counter);
		when(chain.nextCall(any())).thenReturn(chainResponse);
		TurnLimitAdvisor advisor = new TurnLimitAdvisor(3);

		// call under test
		ChatClientResponse result = advisor.adviseCall(request, chain);

		assertSame(chainResponse, result);
		assertEquals(3, counter.get());
		ArgumentCaptor<ChatClientRequest> captor = ArgumentCaptor.forClass(ChatClientRequest.class);
		verify(chain).nextCall(captor.capture());
		assertSame(request, captor.getValue());
	}

	@Test
	public void testAdviseCallOverBudget() {
		// Seeded past the budget: the model must not be called (the history holds toolUse/toolResult blocks,
		// which Bedrock rejects without a toolConfig). The advisor returns a terminal, tool-free result.
		AtomicInteger counter = new AtomicInteger(3);
		ChatClientRequest request = requestWithCounter(counter);
		TurnLimitAdvisor advisor = new TurnLimitAdvisor(3);

		// call under test
		ChatClientResponse result = advisor.adviseCall(request, chain);

		assertEquals(4, counter.get());
		// The model is never called on the over-budget turn.
		verify(chain, never()).nextCall(any());
		// The terminal response ends the ToolCallAdvisor loop (no tool calls) and carries the failure marker.
		assertFalse(result.chatResponse().hasToolCalls());
		assertEquals(TurnLimitAdvisor.TURN_LIMIT_REACHED_RESULT, result.chatResponse().getResult().getOutput().getText());
		// The original context is carried through unchanged.
		assertSame(counter, result.context().get(AgentToolContextKey.TURN_COUNT.getKey()));
	}

	@Test
	public void testAdviseCallCountsAcrossInvocations() {
		// A single counter threaded through the loop: the budgeted turns call the model, the turn past the
		// budget returns the terminal result without calling it.
		AtomicInteger counter = new AtomicInteger(0);
		ChatClientRequest request = requestWithCounter(counter);
		when(chain.nextCall(any())).thenReturn(chainResponse);
		TurnLimitAdvisor advisor = new TurnLimitAdvisor(3);

		// call under test
		assertSame(chainResponse, advisor.adviseCall(request, chain));
		assertSame(chainResponse, advisor.adviseCall(request, chain));
		assertSame(chainResponse, advisor.adviseCall(request, chain));
		ChatClientResponse overBudget = advisor.adviseCall(request, chain);

		assertEquals(4, counter.get());
		// The model was called for the three budgeted turns only.
		verify(chain, times(3)).nextCall(request);
		assertEquals(TurnLimitAdvisor.TURN_LIMIT_REACHED_RESULT,
				overBudget.chatResponse().getResult().getOutput().getText());
	}

	@Test
	public void testAdviseCallWithNoCounter() {
		// Without a seeded counter the advisor cannot enforce a bound, so it fails fast rather than silently
		// running unbounded — this is what stops a chain wired without the counter from looping forever.
		ChatClientRequest request = requestWithCounter(null);
		TurnLimitAdvisor advisor = new TurnLimitAdvisor(1);

		// call under test
		assertThrows(IllegalStateException.class, () -> advisor.adviseCall(request, chain));

		verify(chain, never()).nextCall(any());
	}

	@Test
	public void testAdviseCallWithWrongTypeCounter() {
		// A value of the wrong type under the counter key is treated like a missing counter: fail fast.
		ChatClientRequest request = requestWithCounter("not-a-counter");
		TurnLimitAdvisor advisor = new TurnLimitAdvisor(1);

		// call under test
		assertThrows(IllegalStateException.class, () -> advisor.adviseCall(request, chain));

		verify(chain, never()).nextCall(any());
	}

	@Test
	public void testConstructorWithNonPositiveMaxTurns() {
		assertThrows(IllegalArgumentException.class, () -> new TurnLimitAdvisor(0));
		assertThrows(IllegalArgumentException.class, () -> new TurnLimitAdvisor(-1));
	}

	@Test
	public void testDefaultsAndMetadata() {
		TurnLimitAdvisor advisor = new TurnLimitAdvisor();
		assertEquals(TurnLimitAdvisor.DEFAULT_MAX_TURNS, advisor.getMaxTurns());
		assertEquals(50, advisor.getMaxTurns());
		assertEquals(Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER + 100, advisor.getOrder());
		assertEquals("Turn Limit Advisor", advisor.getName());
	}
}
