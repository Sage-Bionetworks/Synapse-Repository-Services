package org.sagebionetworks.repo.manager.agent.tool;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.agent.AgentToolContextKey;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

/**
 * Caps the number of model turns (Bedrock round-trips) a single {@code chat()} may take.
 * <p>
 * A {@link ToolCallAdvisor} runs the tool-calling loop as part of the advisor chain and, on every
 * iteration, re-invokes the advisors that sit after it in the chain. This advisor is one of those
 * downstream advisors: registered just after the chat-memory advisor, it is therefore called once per
 * model turn and counts them. Without a bound the loop only ends when the model stops emitting tool
 * calls, so a model that never converges (e.g. it retries a tool that keeps failing) loops forever — a
 * Compute task once ran for 9+ hours retrying {@code runPython} against an expired code-interpreter
 * session (PLFM-9879). This bounds that loop (PLFM-9881).
 * <p>
 * Once the model has taken its full turn budget, this advisor ends the loop by short-circuiting: on the
 * turn past the budget it returns a terminal, tool-free response carrying a {@code RESULT: ERROR} marker
 * <em>without calling the model</em>. The {@link ToolCallAdvisor} continues its loop only while the
 * response {@link ChatResponse#hasToolCalls() has tool calls}, so a response with none ends it
 * immediately. Not calling the model on that turn is both necessary and sufficient: the Compute
 * sub-workers require a {@code RESULT: SUCCESS} marker, so this fails an over-budget task fast; and a
 * final model call is not a safe alternative here, because by this point the conversation history
 * contains {@code toolUse}/{@code toolResult} content blocks and Bedrock Converse rejects any request
 * carrying those blocks without a {@code toolConfig} — so a tool-stripped "final answer" request returns
 * a 400.
 * <p>
 * The per-invocation turn counter is an {@link AtomicInteger} read from the advisor context under
 * {@link AgentToolContextKey#TURN_COUNT}; the counter must be seeded there before the loop starts (see
 * {@code Agent.chat}) because the {@link ToolCallAdvisor} rebuilds each iteration's request from the
 * original context, so a counter this advisor tried to add itself would be discarded on the next
 * iteration. If the counter is absent or of the wrong type this advisor throws an
 * {@link IllegalStateException} rather than silently running unbounded, so a chain wired without the
 * seeded counter fails fast instead of reintroducing the runaway loop. A single advisor instance holds
 * no per-conversation state and is safe to share across concurrent chats.
 */
public class TurnLimitAdvisor implements CallAdvisor {

	private static final Logger LOG = LogManager.getLogger(TurnLimitAdvisor.class);

	/**
	 * The default per-{@code chat()} turn budget: the maximum number of model turns the tool-calling loop
	 * may take. Sized above the turns a legitimate multi-step generation needs (schema lookups, per-file
	 * reads, delegations, and Python transforms) but far below a runaway loop, so it is a coarse safety
	 * ceiling rather than a tuning target.
	 */
	public static final int DEFAULT_MAX_TURNS = 50;

	/**
	 * Placed after the chat-memory advisor ({@link Advisor#DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER}) and, by
	 * design, <em>inside</em> the loop-driving {@link ToolCallAdvisor} (see {@code Agent}). Being the
	 * innermost advisor before the model, it is re-invoked on every tool-loop iteration — which is how it
	 * counts turns — and short-circuits the over-budget turn immediately before the model would be called.
	 */
	public static final int ADVISOR_ORDER = Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER + 100;

	/**
	 * The text of the terminal response returned once the turn budget is exhausted. It carries the
	 * Compute sub-workers' {@code RESULT: ERROR} marker so an over-budget run fails the task with a
	 * meaningful message instead of running forever.
	 */
	public static final String TURN_LIMIT_REACHED_RESULT = "RESULT: ERROR - reached the maximum number of tool-use turns "
			+ "before completing the task.";

	private final int maxTurns;

	public TurnLimitAdvisor() {
		this(DEFAULT_MAX_TURNS);
	}

	public TurnLimitAdvisor(int maxTurns) {
		ValidateArgument.requirement(maxTurns > 0, "maxTurns must be greater than 0");
		this.maxTurns = maxTurns;
	}

	public int getMaxTurns() {
		return maxTurns;
	}

	@Override
	public String getName() {
		return "Turn Limit Advisor";
	}

	@Override
	public int getOrder() {
		return ADVISOR_ORDER;
	}

	@Override
	public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
		// The counter must have been seeded into the context before the loop started (see Agent.chat). An
		// absent or wrong-typed counter means this advisor was wired into a chain without that seeding, which
		// would leave the tool-calling loop unbounded — the exact PLFM-9879 failure mode. Fail loudly rather
		// than silently degrade to no limit, so a misconfiguration cannot reintroduce the runaway loop.
		Object counter = chatClientRequest.context().get(AgentToolContextKey.TURN_COUNT.getKey());
		if (!(counter instanceof AtomicInteger turnCount)) {
			throw new IllegalStateException("A turn counter (AtomicInteger) must be seeded into the advisor context "
					+ "under '" + AgentToolContextKey.TURN_COUNT.getKey() + "' before the tool-calling loop starts.");
		}
		// The model is allowed maxTurns turns. The loop only re-enters this advisor when the previous turn
		// asked for another tool call, so the first turn past the budget means the model is still not done.
		int turn = turnCount.incrementAndGet();
		if (turn <= maxTurns) {
			return callAdvisorChain.nextCall(chatClientRequest);
		}
		LOG.warn("Reached the turn limit of {} for this chat; ending the tool-calling loop with a failure result.",
				maxTurns);
		return terminalFailureResponse(chatClientRequest);
	}

	/**
	 * Builds the terminal response that ends the {@link ToolCallAdvisor} loop: a single assistant
	 * generation with the {@link #TURN_LIMIT_REACHED_RESULT} text and no tool calls, so
	 * {@link ChatResponse#hasToolCalls()} is {@code false}. The original context is carried through so any
	 * upstream advisor (e.g. chat memory) sees the same context it started with.
	 */
	private ChatClientResponse terminalFailureResponse(ChatClientRequest chatClientRequest) {
		ChatResponse chatResponse = new ChatResponse(
				List.of(new Generation(new AssistantMessage(TURN_LIMIT_REACHED_RESULT))));
		return ChatClientResponse.builder()
				.chatResponse(chatResponse)
				.context(chatClientRequest.context())
				.build();
	}
}
