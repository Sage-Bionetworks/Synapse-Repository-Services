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
 * only on the first code activity of a turn. The batch and specialist paths instead place an
 * already-started session id directly under {@link AgentToolContextKey#CODE_SESSION_ID};
 * {@link #resolveSessionId} reads from either source.
 */
@FunctionalInterface
public interface CodeSessionSupplier {

	/**
	 * The id of the code interpreter session to execute against, created on first call if it does not
	 * already exist.
	 */
	String getSessionId();

	/**
	 * Resolve the code interpreter session id from a tool's context. If a
	 * {@link AgentToolContextKey#CODE_SESSION_SUPPLIER} supplier was installed (the interactive Curie
	 * path, where the session is created lazily), invoke it; otherwise fall back to the id placed
	 * directly under {@link AgentToolContextKey#CODE_SESSION_ID} (the batch and specialist paths, where
	 * the session was already started). Returns {@code null} when neither is present, so a caller can
	 * preserve its "no session available" guard.
	 */
	static String resolveSessionId(ToolContext toolContext) {
		Object supplier = AgentToolContextKey.CODE_SESSION_SUPPLIER.get(toolContext);
		if (supplier != null) {
			return ((CodeSessionSupplier) supplier).getSessionId();
		}
		return (String) AgentToolContextKey.CODE_SESSION_ID.get(toolContext);
	}
}
