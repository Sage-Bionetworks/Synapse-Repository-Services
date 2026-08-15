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
	 * An already-resolved AWS Bedrock AgentCore code interpreter session id, placed directly by the
	 * batch sub-workers and by delegated specialists. The interactive Curie path installs a
	 * {@link #CODE_SESSION_SUPPLIER} instead; {@link CodeSessionSupplier#resolveSessionId(ToolContext)}
	 * reads from either source.
	 */
	CODE_SESSION_ID("codeSessionId"),

	/**
	 * A {@link CodeSessionSupplier} that lazily creates and memoizes the code interpreter session
	 * (interactive Curie path).
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
	 * The durable Synapse chat session id of an interactive Curie turn. Distinct from
	 * {@link #CODE_SESSION_ID} (an AWS code interpreter session): the {@code CurieSupervisor} uses it
	 * both to derive its cross-machine conversation id and to build the lazy code-session supplier it
	 * installs under {@link #CODE_SESSION_SUPPLIER}.
	 */
	CHAT_SESSION_ID("chatSessionId"),

	/**
	 * The {@link java.util.List} of {@link org.sagebionetworks.repo.model.agent.AgentChatAttachmentStatus}
	 * successfully staged into the shared code interpreter session for the current Curie turn. The
	 * {@code CurieSupervisor} reads it to prepend a description of those files to the user message.
	 */
	STAGED_ATTACHMENTS("stagedAttachments");

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
