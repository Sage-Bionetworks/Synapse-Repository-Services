package org.sagebionetworks.repo.manager.grid;

import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.grid.CreateGridPresignedUrlRequest;
import org.sagebionetworks.repo.model.grid.CreateGridPresignedUrlResponse;
import org.sagebionetworks.repo.model.grid.CreateGridRequest;
import org.sagebionetworks.repo.model.grid.CreateGridResponse;
import org.sagebionetworks.repo.model.grid.CreateReplicaRequest;
import org.sagebionetworks.repo.model.grid.CreateReplicaResponse;
import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.EventType;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridReplica;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.ListGridSessionsRequest;
import org.sagebionetworks.repo.model.grid.ListGridSessionsResponse;
import org.sagebionetworks.repo.model.grid.internal.Connection;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public interface GridManager extends PatchStore {

	/**
	 * Create a new grid session.
	 * 
	 * @param user
	 * @param request
	 * @return
	 */
	CreateGridResponse createGrid(AsyncJobProgressCallback progressCallback, UserInfo user, CreateGridRequest request);

	/**
	 * Get information about a grid session.
	 * 
	 * @param user
	 * @param gridSessionId
	 * @return
	 */
	GridSession getGridSession(UserInfo user, String gridSessionId);

	/**
	 * Create a new replica from from a request.
	 * 
	 * @param user
	 * @param request
	 * @return
	 */
	CreateReplicaResponse createReplica(UserInfo user, CreateReplicaRequest request);

	/**
	 * Create replica from an internal source.
	 * 
	 * @param user
	 * @param gridSessionId
	 * @param isAgent
	 * @param source
	 * @return
	 */
	CreateReplicaResponse createReplica(UserInfo user, String gridSessionId, boolean isAgent, EventSource source);

	/**
	 * 
	 * Get the identified replica.
	 * 
	 * @param user
	 * @param sessionId
	 * @param repicaId
	 * @return
	 */
	GridReplica getReplica(UserInfo user, String sessionId, Long repicaId);

	/**
	 * Create new presigned URL to establish a websocket connection to the grid.
	 * 
	 * @param user
	 * @param request
	 * @return
	 */
	CreateGridPresignedUrlResponse createWebsocketPresignedUrl(UserInfo user, CreateGridPresignedUrlRequest request);

	/**
	 * Called when a connection is established with a replica.
	 * 
	 * @param user
	 * @param context
	 * @param connection
	 */
	void createReplicaConnection(UserInfo user, EventContext context, Connection connection);

	/**
	 * Remove a connection if the type matches the expected type.
	 * 
	 * @param type
	 * @param connectionId
	 */
	void removeReplicatConnection(EventType type, String connectionId);

	/**
	 * Unconditionally remove a connection.
	 * 
	 * @param connectionId
	 */
	void removeReplicaConnection(String connectionId);

	/**
	 * Save a patch.
	 * 
	 * @param context
	 * @param patchId
	 * @param body
	 * @return True if this is a new patch. False if this patch has been saved
	 *         before.
	 */
	boolean savePatch(EventContext context, LogicalTimestamp patchId, String body);
	
	/**
	 * List the active connections for a grid session.
	 * 
	 * @param connectionId
	 * @return
	 */
	List<GridConnectionInfo> listActiveConnections(String connectionId);

	/**
	 * Given a replica's clock, find the next patch that the replica is missing.
	 * 
	 * @param context
	 * @param clock
	 * @return {@link Optional#empty()} If the replica is up-to-date.
	 */
	Optional<String> getNextMissingPatch(EventContext context, List<LogicalTimestamp> clock);

	ListGridSessionsResponse listActiveGridSessions(UserInfo user, ListGridSessionsRequest request);

	void deleteGridSession(UserInfo user, String gridSessionId);



}
