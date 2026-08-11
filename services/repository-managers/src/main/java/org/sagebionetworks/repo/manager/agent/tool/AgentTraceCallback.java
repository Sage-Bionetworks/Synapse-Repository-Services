package org.sagebionetworks.repo.manager.agent.tool;

/**
 * A sink for trace events produced while an agent chat turn runs. It is carried in the
 * agent-immutable tool context under
 * {@link org.sagebionetworks.repo.manager.agent.AgentToolContextKey#TRACE_CALLBACK} so that
 * {@link LoggingToolCallback} can record each tool invocation against the originating asynchronous
 * job.
 * <p>
 * The Curie multi-agent path uses this callback because its tools run several layers below the
 * manager and have no direct access to either the {@code AgentDao} bean or the {@code jobId}. The
 * manager binds both when it constructs the callback and threads the callback down through the
 * supervisor and specialist chat methods.
 */
@FunctionalInterface
public interface AgentTraceCallback {

	/**
	 * Record a single trace event for the current job. The implementation supplies the event
	 * timestamp (from the manager's clock), so callers separated by model round-trips get distinct
	 * events.
	 *
	 * @param message The human-readable trace message.
	 */
	void addTraceToJob(String message);
}
