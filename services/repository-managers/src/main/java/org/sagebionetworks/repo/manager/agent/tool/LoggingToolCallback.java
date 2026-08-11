package org.sagebionetworks.repo.manager.agent.tool;

import java.util.Map;
import java.util.StringJoiner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.agent.AgentToolContextKey;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.GridAgentSessionContext;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.lang.Nullable;

/**
 * A {@link ToolCallback} decorator that logs each tool invocation: the target tool name, the
 * caller's prompt (the tool-input JSON), a PII-safe summary of the tool context, and the tool's
 * response. All other behavior is delegated unchanged.
 */
public class LoggingToolCallback implements ToolCallback {

	private static final Logger log = LogManager.getLogger(LoggingToolCallback.class);

	private final ToolCallback delegate;

	public LoggingToolCallback(ToolCallback delegate) {
		this.delegate = delegate;
	}

	@Override
	public ToolDefinition getToolDefinition() {
		return delegate.getToolDefinition();
	}

	@Override
	public ToolMetadata getToolMetadata() {
		return delegate.getToolMetadata();
	}

	@Override
	public String call(String toolInput) {
		return call(toolInput, null);
	}

	@Override
	public String call(String toolInput, @Nullable ToolContext toolContext) {
		String name = delegate.getToolDefinition().name();
		log.info("Calling tool '{}' [context: {}] with prompt: {}", name, describeContext(toolContext), toolInput);
		AgentTraceCallback traceCallback = extractTraceCallback(toolContext);
		if (traceCallback != null) {
			traceCallback.addTraceToJob("Called tool '" + name + "' with input: " + toolInput);
		}
		String response = delegate.call(toolInput, toolContext);
		log.info("Tool '{}' response: {}", name, response);
		if (traceCallback != null) {
			traceCallback.addTraceToJob("Tool '" + name + "' responded with: " + response);
		}
		return response;
	}

	/**
	 * The trace callback is carried only in the supervisor's tool context, so recording both the
	 * input and the response here captures the supervisor's conversation with its specialists — the
	 * delegation instruction and the specialist's answer — without the noise of the specialists' own
	 * nested tool calls. When no callback is present (trace disabled, or the legacy path), tracing is
	 * skipped.
	 */
	@Nullable
	private static AgentTraceCallback extractTraceCallback(@Nullable ToolContext toolContext) {
		if (toolContext == null) {
			return null;
		}
		Object callback = AgentToolContextKey.TRACE_CALLBACK.get(toolContext);
		return callback instanceof AgentTraceCallback traceCallback ? traceCallback : null;
	}

	/**
	 * Renders the tool context as a PII-safe, IDs-only summary. Known value types are reduced to
	 * their identifiers; any other type is reduced to its class name so that no unrecognized value
	 * (such as a full {@link UserInfo}) can leak its {@code toString()} into the logs.
	 */
	static String describeContext(@Nullable ToolContext toolContext) {
		if (toolContext == null || toolContext.getContext().isEmpty()) {
			return "{}";
		}
		StringJoiner joiner = new StringJoiner(", ", "{", "}");
		for (Map.Entry<String, Object> entry : toolContext.getContext().entrySet()) {
			joiner.add(entry.getKey() + "=" + describeValue(entry.getValue()));
		}
		return joiner.toString();
	}

	private static String describeValue(Object value) {
		if (value == null) {
			return "null";
		}
		if (value instanceof UserInfo userInfo) {
			return "userId=" + userInfo.getId();
		}
		if (value instanceof GridAgentSessionContext gridContext) {
			return "gridSessionId=" + gridContext.getGridSessionId() + ",usersReplicaId="
					+ gridContext.getUsersReplicaId();
		}
		if (value instanceof String stringValue) {
			return stringValue;
		}
		return value.getClass().getSimpleName();
	}
}
