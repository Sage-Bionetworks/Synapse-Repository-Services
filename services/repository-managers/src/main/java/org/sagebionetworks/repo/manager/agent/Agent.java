package org.sagebionetworks.repo.manager.agent;

import java.util.Set;

import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;

/**
 * The delegation contract shared by every specialist and supervisor agent. Callers — {@code
 * SupervisorTools} delegating to a specialist, the curation sub-workers, and the interactive Curie
 * entry point — drive an agent only through this interface, never through a concrete agent class.
 * <p>
 * An implementation declares its {@link AgentRole} and supplies
 * {@link #prepareChatClientRequestSpec(String, ToolContext)} to describe a single turn (its message,
 * tools, system prompt, model, memory, and output-token budget). The default
 * {@link #chat(String, ToolContext)} runs that turn and normalizes the outcome — in particular it
 * converts an output-token truncation into corrective guidance rather than a misleading success, and
 * caps the text a specialist returns to its supervisor (PLFM-9868), so every caller gets that protection
 * for free.
 */
public interface Agent {

	/**
	 * The Bedrock Converse stop reason reported when generation halts because the output-token limit
	 * ({@code maxTokens}) was reached. The tool-calling loop only executes a turn's tool calls when the
	 * finish reason is {@code tool_use} (see {@code BedrockProxyChatModel}); a turn that ends with this
	 * reason has any tool call it was emitting silently dropped, so the intended action never runs.
	 * Compared case-insensitively by {@link ChatResponse#hasFinishReasons(Set)}.
	 */
	static final String MAX_TOKENS_FINISH_REASON = "max_tokens";

	/**
	 * Returned in place of the model's text when a turn was truncated by the output-token limit. It is
	 * fed back to the calling agent (a delegating supervisor, or the end user for a top-level turn) as
	 * corrective guidance to retry the work in smaller pieces — the same self-correction path the tool
	 * layer uses for malformed arguments (PLFM-9868).
	 */
	public static final String TRUNCATED_RESPONSE_MESSAGE = "The response was cut off because it reached the "
			+ "output-token limit before completing, so any action it was in the middle of requesting was not "
			+ "performed. Retry the work as a series of smaller requests so each response stays within the limit.";

	/**
	 * The {@code maxTokens} every agent passes to its {@code BedrockChatOptions}: {@code null} on purpose.
	 * Per the AWS SDK, omitting {@code maxTokens} makes the Bedrock Converse API default to the model's
	 * maximum output budget. Agents are left unbounded so a turn can emit a large tool-call argument (for
	 * example a big {@code updateGrid} batch) without being truncated mid-emit — offloading large work to a
	 * specialist is the point of the delegation. Bounding what an agent returns to its caller is
	 * {@link #chat(String, ToolContext)}'s responsibility, not this generation cap's (PLFM-9868).
	 */
	public static final Integer MODELS_MAX_TOKENS = null;

	/**
	 * The character budget a {@link AgentRole#SPECIALIST specialist's} final response is truncated to
	 * before it is handed back to the supervisor that delegated to it. Because agents generate up to the
	 * model's full output budget ({@link #MODELS_MAX_TOKENS}), a specialist that dumps a large result into
	 * its text would otherwise flood the supervisor's context; {@link #chat(String, ToolContext)} caps it
	 * to this length with {@link #RESPONSE_TRUNCATED_SUFFIX} appended so the supervisor can tell the text
	 * was cut. At roughly four characters per token this bounds the returned text to about 4k tokens. A
	 * {@link AgentRole#SUPERVISOR supervisor} is not capped, because its response goes to the actual caller
	 * (the end user), not into another agent's context (PLFM-9868).
	 */
	public static final int MAX_RESPONSE_CHARACTERS = 16000;

	/**
	 * Appended by {@link #chat(String, ToolContext)} to a response it truncated to
	 * {@link #MAX_RESPONSE_CHARACTERS}, so the caller knows the tail of the text was dropped.
	 */
	public static final String RESPONSE_TRUNCATED_SUFFIX = "\n\n[This response was truncated because it exceeded "
			+ "the size limit returned to the caller. The action itself was not affected; ask for a shorter summary "
			+ "or a narrower request if you need the rest.]";

	/**
	 * Builds the request spec for a single turn: the user message, the tool context passed straight
	 * through, and the memory advisor bound to this instance's conversation. The implementation wires its
	 * own tools, system prompt, model, and output-token budget here (via its {@code ChatClient}).
	 *
	 * @param message the instruction for this turn
	 * @param context the tool context (acting user, session bindings) handed to every tool the turn invokes
	 * @return the prepared, not-yet-executed request spec
	 */
	ChatClientRequestSpec prepareChatClientRequestSpec(String message, ToolContext context);

	/**
	 * The role this agent plays in a delegation. Every agent must declare its role, so the contract of a
	 * new agent is explicit at a glance. The role drives how much of the agent's response
	 * {@link #chat(String, ToolContext)} returns to its caller (see {@link AgentRole}), and is intended as
	 * the branch point for future role-specific behavior.
	 *
	 * @return this agent's role, never {@code null}
	 */
	AgentRole getAgentRole();

	/**
	 * Runs one turn and returns the agent's textual response, or {@code null} when the model produced no
	 * response. Two protections are applied to the response before it reaches the caller (PLFM-9868):
	 * <ul>
	 * <li>When the turn is truncated at the output-token limit, returns {@link #TRUNCATED_RESPONSE_MESSAGE}
	 * instead of the model's (misleading) partial text, telling the caller to retry the work in smaller
	 * pieces.</li>
	 * <li>When the response text is longer than this agent's {@link AgentRole#getResponseCharacterLimit()},
	 * it is truncated to that length with {@link #RESPONSE_TRUNCATED_SUFFIX} appended, so a specialist that
	 * emits a large body of text cannot flood the supervisor's context.</li>
	 * </ul>
	 *
	 * @param message the instruction for this turn
	 * @param context the tool context handed to every tool the turn invokes
	 * @return the response text (truncated to the agent type's character limit when longer),
	 *         {@link #TRUNCATED_RESPONSE_MESSAGE} on an output-token truncation, or {@code null} when there
	 *         is no response
	 */
	default String chat(String message, ToolContext context) {
		ChatClientRequestSpec spec = prepareChatClientRequestSpec(message, context);
		CallResponseSpec response = spec.call();

		ChatResponse chatResponse = response.chatResponse();
		if (chatResponse == null) {
			return null;
		}
		// A turn that ends on max_tokens had the tool call it was emitting silently dropped, so report the
		// truncation to the caller rather than the model's "success" narration for an action that never ran.
		if (chatResponse.hasFinishReasons(Set.of(MAX_TOKENS_FINISH_REASON))) {
			return TRUNCATED_RESPONSE_MESSAGE;
		}
		Generation result = chatResponse.getResult();
		if (result == null || result.getOutput() == null) {
			return null;
		}
		return truncateResponse(result.getOutput().getText(), getAgentRole().getResponseCharacterLimit());
	}

	/**
	 * Bounds the text returned to the caller: text at or under {@code limit} characters (and {@code null})
	 * is returned unchanged; longer text is cut to {@code limit} with {@link #RESPONSE_TRUNCATED_SUFFIX}
	 * appended.
	 */
	private static String truncateResponse(String text, int limit) {
		if (text == null || text.length() <= limit) {
			return text;
		}
		return text.substring(0, limit) + RESPONSE_TRUNCATED_SUFFIX;
	}

	/**
	 * The role an agent plays in a delegation, declared by every agent via {@link Agent#getAgentRole()}.
	 * The role determines how much of the agent's response is returned to its caller: a specialist's
	 * response is fed back into the delegating supervisor's context and is capped
	 * ({@link #getResponseCharacterLimit()}); a supervisor's response goes to the end user and is left
	 * unbounded. Kept as an explicit contract so a new agent must state its role, and so future features
	 * can branch on it (PLFM-9868).
	 */
	public enum AgentRole {

		/** Orchestrates specialists; its response is returned to the end user, so it is not truncated. */
		SUPERVISOR(Integer.MAX_VALUE),

		/**
		 * Performs one focused task delegated by a supervisor; its response is fed back into that
		 * supervisor's context, so it is capped to {@link Agent#MAX_RESPONSE_CHARACTERS}.
		 */
		SPECIALIST(MAX_RESPONSE_CHARACTERS);

		private final int responseCharacterLimit;

		AgentRole(int responseCharacterLimit) {
			this.responseCharacterLimit = responseCharacterLimit;
		}

		/**
		 * The maximum number of characters of a response from an agent of this type that
		 * {@link Agent#chat(String, ToolContext)} returns to the caller before truncating.
		 */
		public int getResponseCharacterLimit() {
			return responseCharacterLimit;
		}
	}

}
