package org.sagebionetworks.repo.manager.agent;

import java.util.Map;

import org.springframework.ai.chat.model.ToolContext;

/**
 * The complete set of keys under which values are carried in a Spring AI {@link ToolContext} across
 * the Curie agent tree (supervisors, delegated specialists, and their tools). Centralizing them here
 * gives a single source of truth for the key strings and one typed access path, so a reader and the
 * writer that feeds it cannot drift apart on a bare string literal.
 */
public enum AgentToolContextKey {

	/** The {@link org.sagebionetworks.repo.model.UserInfo} of the caller driving the agent. */
	USER_INFO("userInfo"),

	/**
	 * A {@link CodeSessionSupplier} for the AWS Bedrock AgentCore code interpreter session the agent's
	 * tools execute against. The interactive Curie path installs a lazy, memoizing supplier; the batch
	 * sub-workers and delegated specialists install a constant supplier over an already-started session
	 * (see {@link CodeSessionSupplier#of(String)}). Tools read it via
	 * {@link CodeSessionSupplier#resolveSessionId(ToolContext)}.
	 */
	CODE_SESSION_SUPPLIER("codeSessionSupplier"),

	/**
	 * The {@link org.sagebionetworks.repo.model.agent.GridAgentSessionContext} bound to the current
	 * grid session.
	 */
	GRID_SESSION_CONTEXT("gridAgentSessionContext"),

	/**
	 * An {@link org.sagebionetworks.repo.manager.agent.tool.AgentTraceCallback} used to record each
	 * tool invocation against the originating asynchronous job.
	 */
	TRACE_CALLBACK("agentTraceCallback"),

	/**
	 * The durable Synapse chat session id of an interactive Curie turn. Distinct from the AWS code
	 * interpreter session: the {@code CurieSupervisor} uses it both to derive its cross-machine
	 * conversation id and to build the lazy code-session supplier it installs under
	 * {@link #CODE_SESSION_SUPPLIER}.
	 */
	CHAT_SESSION_ID("chatSessionId"),

	/**
	 * The {@link java.util.List} of {@link org.sagebionetworks.repo.model.agent.AgentChatAttachmentStatus}
	 * successfully staged into the shared code interpreter session for the current Curie turn. The
	 * {@code CurieSupervisor} reads it to prepend a description of those files to the user message.
	 */
	STAGED_ATTACHMENTS("stagedAttachments"),

	/**
	 * A per-{@code chat()} turn counter ({@link java.util.concurrent.atomic.AtomicInteger}) seeded into the
	 * advisor context by {@code Agent.chat()} before the tool-calling loop starts and read by
	 * {@link org.sagebionetworks.repo.manager.agent.tool.TurnLimitAdvisor} to bound the number of model
	 * turns a single chat may take (PLFM-9881). Carried in the same context map as the tool keys, so it is
	 * centralized here to avoid colliding with them.
	 */
	TURN_COUNT("turnCount");

	private final String key;

	AgentToolContextKey(String key) {
		this.key = key;
	}

	/**
	 * The raw string key used within the tool-context map.
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Read this key's value from the given tool context, or {@code null} if absent.
	 */
	public Object get(ToolContext toolContext) {
		return toolContext.getContext().get(key);
	}

	/**
	 * Put a value under this key into a mutable tool-context map.
	 */
	public void put(Map<String, Object> context, Object value) {
		context.put(key, value);
	}
}
