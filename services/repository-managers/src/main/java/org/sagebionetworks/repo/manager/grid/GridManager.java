package org.sagebionetworks.repo.manager.grid;

import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.dbo.grid.GridSource;
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
import org.sagebionetworks.repo.model.grid.GridQueryJobRequest;
import org.sagebionetworks.repo.model.grid.GridQueryJobResponse;
import org.sagebionetworks.repo.model.grid.GridReplica;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.GridUpdateJobRequest;
import org.sagebionetworks.repo.model.grid.GridUpdateJobResponse;
import org.sagebionetworks.repo.model.grid.ListGridReplicasRequest;
import org.sagebionetworks.repo.model.grid.ListGridReplicasResponse;
import org.sagebionetworks.repo.model.grid.ListGridSessionsRequest;
import org.sagebionetworks.repo.model.grid.ListGridSessionsResponse;
import org.sagebionetworks.repo.model.grid.internal.Connection;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public interface GridManager extends PatchStore, SnapshotStore {

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
	 * List all replicas for a grid session with their connection status and type.
	 *
	 * @param user
	 * @param request
	 * @return
	 */
	ListGridReplicasResponse listReplicas(UserInfo user, ListGridReplicasRequest request);

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
	 * Retrieves the default internal connection for a given grid session ID.
	 *
	 * @param sessionId
	 * @param source
	 * @return
	 */
	Optional<GridConnectionInfo> getSingletonConnection(String sessionId, EventSource source);

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
	 * Updates the session's stored benefactor IDs to reflect the current source
	 * state as seen by the action user, then evicts any active WebSocket connections
	 * belonging to users who no longer pass the session's authorization check.
	 * Non-user connections (INTERNAL, VALIDATION, USER_SUPPORT) are always skipped
	 * during eviction.
	 *
	 * @param sessionId
	 * @param benefactorIds
	 */
	void updateSessionBenefactorIds(String sessionId, Set<Long> benefactorIds);

	/**
	 * Update the baseline source version (sourceEntityVersionNumber) recorded on a
	 * grid session. This is the source revision the grid was last synchronized to,
	 * used for deletion detection on subsequent synchronizations.
	 *
	 * @param sessionId     the grid session id
	 * @param sourceVersion the source revision the grid is now synchronized to
	 */
	void updateSourceEntityVersion(String sessionId, Long sourceVersion);

	/**
	 * Update the bound JSON schema $id recorded on a grid session. Used during
	 * synchronization when the source's bound schema changes, so that subsequent
	 * row validation runs against the new schema.
	 *
	 * @param sessionId the grid session id
	 * @param schemaId  the new bound JSON schema $id
	 */
	void updateSessionSchemaId(String sessionId, String schemaId);

	/**
	 * Checks all active WebSocket connections for the given session and removes any
	 * belonging to users who no longer pass the session's authorization check.
	 * Non-user connections (INTERNAL, VALIDATION, USER_SUPPORT) are always skipped.
	 *
	 * @param sessionId
	 */
	void evictUnauthorizedConnections(String sessionId);

	/**
	 * Given a replica's clock, find the next snapshot or patch that the replica is missing, and format a message that
	 * can be sent to the replica to apply the snapshot/patch.
	 *
	 * @param context
	 * @param clock
	 * @return {@link Optional#empty()} If the replica is up-to-date.
	 */
	Optional<String> getNextSynchronizeResponse(EventContext context, List<LogicalTimestamp> clock);

	/**
	 * Given a replica's clock, find the next patch that the replica is missing.
	 * 
	 * @param context
	 * @param clock
	 * @return {@link Optional#empty()} If the replica is up-to-date.
	 */
	Optional<JSONArray> getNextMissingPatch(EventContext context, List<LogicalTimestamp> clock);

	/**
	 * Retrieve a pre-signed URL that can be used to download the latest snapshot data for a grid session.
	 * @param context
	 * @return
	 */
	Optional<URL> getLatestSnapshotPresignedUrl(EventContext context);

	ListGridSessionsResponse listActiveGridSessions(UserInfo user, ListGridSessionsRequest request);

	void deleteGridSession(UserInfo user, String gridSessionId);

	Optional<GridConnectionInfo> getConnectionInfoOptional(String connectionId);

	/**
	 * Create a new Agent replica that can be used to update the grid.
	 * 
	 * @param user
	 * @param session
	 * @return
	 */
	GridReplica createAgentReplica(UserInfo user, GridSession session);

	Optional<GridConnectionInfo> getConnection(String gridSessionId, Long agentsReplicaId);
	
	/**
	 * Get the grid session source information.
	 * @param sessionId
	 * @return Optional.empty() if the session does not have a source.
	 */
	Optional<GridSource> getSessionSource(String sessionId);

	/**
	 * Backfill CHANGES entries for all existing grid sessions. This must be run on
	 * the source stack before migration so the entries migrate with the CHANGES
	 * table.
	 *
	 * @return The number of sessions backfilled.
	 */
	long backfillGridSessionChanges();

	/**
	 * Execute a single update operation against a grid session. The patches are
	 * attributed to the caller's replica. Used by both the agent handler and the
	 * async job worker.
	 *
	 * @param header               Grid header for column metadata
	 * @param publishingConnection Connection whose replicaId is embedded in patches
	 * @param rawUpdate            Raw JSON of a single Update object
	 * @return Number of rows updated
	 */
	long executeGridUpdate(GridHeader header, GridConnectionInfo publishingConnection,
			JSONObject rawUpdate) throws Exception;

	/**
	 * Execute a grid query async job. Called by the GridQueryWorker.
	 *
	 * @param user
	 * @param request The job request containing sessionId, replicaId, and query.
	 * @return
	 */
	GridQueryJobResponse queryGrid(UserInfo user, GridQueryJobRequest request);

	/**
	 * Execute a grid update async job. Called by the GridUpdateWorker.
	 *
	 * @param user
	 * @param request The job request containing sessionId, replicaId, and updates.
	 * @return
	 */
	GridUpdateJobResponse updateGrid(UserInfo user, GridUpdateJobRequest request) throws Exception;

}
