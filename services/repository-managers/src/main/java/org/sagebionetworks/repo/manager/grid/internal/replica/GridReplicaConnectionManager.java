package org.sagebionetworks.repo.manager.grid.internal.replica;

import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.GridReplica;

/**
 * Handles creating grid replicas and publishing events related to replica
 * lifecycles.
 */
public interface GridReplicaConnectionManager {

	/**
	 * Create a new replica for the given session, without publishing a connect
	 * event. Use this when the caller establishes the connection separately (e.g.
	 * a websocket client that connects itself). To create a replica and connect it
	 * in one step, use {@link #createReplicaAndConnect(Long, String, boolean, EventSource)}.
	 *
	 * @param userId    the principal that owns the replica
	 * @param sessionId the grid session id
	 * @param isAgent   whether the replica is an AI agent replica
	 * @param source    the event source for the replica
	 * @return the newly created replica
	 */
	GridReplica createReplica(Long userId, String sessionId, boolean isAgent, EventSource source);

	/**
	 * Create a new replica for the given session and publish the CONNECT event
	 * that establishes its connection to the hub.
	 * <p>
	 * The connect event is published <em>after the caller's transaction
	 * commits</em>, and is suppressed entirely if the transaction rolls back, so
	 * a caller may safely continue doing work after this call.
	 * The connection is registered asynchronously once the replica completes its
	 * connect handshake, so it will not be visible via {@code getSingletonConnection}
	 * until then.
	 *
	 * @param userId    the principal that owns the replica
	 * @param sessionId the grid session id
	 * @param isAgent   whether the replica is an AI agent replica
	 * @param source    the event source for the replica
	 * @return the newly created replica
	 */
	GridReplica createReplicaAndConnect(Long userId, String sessionId, boolean isAgent, EventSource source);

	/**
	 * Notify the grid replica validation pipeline that the session's bound JSON
	 * schema changed, so the validation worker should re-validate every row,
	 * regardless of whether the row's data has changed since it was last
	 * validated. The notification is published after the caller's transaction
	 * commits (if one is active).
	 *
	 * @param sessionId
	 */
	void publishSchemaChangedEvent(String sessionId);

	/**
	 * Publish a replica change set to the hub's SNS topic.
	 *
	 * @param changeSet
	 */
	void sendChangesToTopic(ReplicaChangeSet changeSet);

}
