package org.sagebionetworks.repo.manager.agent;

import org.springframework.ai.chat.model.ToolContext;

/**
 * Supplies the id of the AWS Bedrock AgentCore code interpreter session that a tool's code executes
 * against. It is carried in the agent-immutable tool context under
 * {@link AgentToolContextKey#CODE_SESSION_SUPPLIER}, mirroring
 * {@link org.sagebionetworks.repo.manager.agent.tool.AgentTraceCallback}, so a tool obtains the
 * session id without depending on how it is provisioned.
 * <p>
 * The interactive Curie path installs a lazy, memoizing supplier (see
 * {@link CodeInterpreterSessionProvider#lazySupplier(String)}) so the costly code session is created
 * only on the first code activity of a turn. The batch and delegated-specialist paths already hold a
 * started session id and install a constant supplier via {@link #of(String)}. Either way a tool reads
 * the session id through {@link #resolveSessionId(ToolContext)}, so it need not know how the session
 * was provisioned.
 */
@FunctionalInterface
public interface CodeSessionSupplier {

	/**
	 * The id of the code interpreter session to execute against, created on first call if it does not
	 * already exist.
	 */
	String getSessionId();

	/**
	 * The already-resolved session id, or {@code null} if it has not been resolved yet. Unlike
	 * {@link #getSessionId()}, this never triggers creation — it is safe to call from side-effect-free
	 * contexts such as logging. A lazy supplier returns {@code null} until its first
	 * {@link #getSessionId()} call has memoized an id; a constant supplier ({@link #of(String)}) always
	 * returns its id.
	 */
	default String resolvedSessionIdOrNull() {
		return null;
	}

	/**
	 * A supplier that always returns the given, already-started session id. Used by the batch and
	 * delegated-specialist paths, which start the session eagerly rather than lazily.
	 */
	static CodeSessionSupplier of(String sessionId) {
		return new CodeSessionSupplier() {

			@Override
			public String getSessionId() {
				return sessionId;
			}

			@Override
			public String resolvedSessionIdOrNull() {
				return sessionId;
			}
		};
	}

	/**
	 * Resolve the code interpreter session id from a tool's context by invoking the
	 * {@link AgentToolContextKey#CODE_SESSION_SUPPLIER} supplier installed on it. Returns {@code null}
	 * when no supplier is present, so a caller can preserve its "no session available" guard.
	 */
	static String resolveSessionId(ToolContext toolContext) {
		Object supplier = AgentToolContextKey.CODE_SESSION_SUPPLIER.get(toolContext);
		return supplier == null ? null : ((CodeSessionSupplier) supplier).getSessionId();
	}
}
