package org.sagebionetworks.repo.model.dbo.grid;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.GridReplica;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.PatchInfo;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public interface GridDao {

	/**
	 * Create a new grid session.
	 * 
	 * @param userId
	 * @param schemaId
	 * @param tableId
	 * @return
	 */
	GridSession createGridSession(CreateGridSession create);

	/**
	 * Get the user that started the grid session.
	 * 
	 * @param gridSessionId
	 * @return
	 */
	Optional<Long> getGridSessionStartedBy(String gridSessionId);

	/**
	 * Get session by ID.
	 * 
	 * @param gridSessionId
	 * @return
	 */
	Optional<GridSession> geGridSession(String gridSessionId);

	/**
	 * Create a new replica.
	 * 
	 * @param userId
	 * @param gridSessionId
	 * @param isAgent
	 * @param source
	 * @return
	 */
	GridReplica createReplica(Long userId, String gridSessionId, boolean isAgent, EventSource source);

	/**
	 * Get information about a grid replica.
	 * 
	 * @param sessionId
	 * @param replicaId
	 * @return
	 */
	Optional<GridReplica> getGridReplica(String sessionId, Long replicaId);

	/**
	 * Get the replica createdBy of the replica matching the parameters.
	 * 
	 * @param sessionId
	 * @param replicaId
	 * @param isAgent
	 * @return
	 */
	Optional<Long> getReplicaCreatedBy(String sessionId, Long replicaId, boolean isAgentReplica);

	/**
	 * Crete a new connection.
	 * 
	 * @param con
	 */
	void createConnection(GridConnectionInfo con);

	/**
	 * Get a connection by its id
	 * 
	 * @param connectionId
	 * @return
	 */
	Optional<GridConnectionInfo> getConnection(String connectionId);

	/**
	 * List all active connections for a session.
	 * 
	 * @param sessionId
	 * @return
	 */
	List<GridConnectionInfo> listConnections(String sessionId);

	/**
	 * Remove an actvie connection.
	 * 
	 * @param connectionId
	 */
	void removeConnection(String connectionId);

	/**
	 * Save grid patch data.
	 * 
	 * @param sessionId
	 * @param patchId
	 * @param s3Key
	 * @param expires
	 * @return True of this was a new patch, else false.
	 */
	boolean savePatch(String sessionId, LogicalTimestamp patchId, String s3Key, Duration expires);

	/**
	 * Get information about a patch.
	 * 
	 * @param sessionId
	 * @param patchId
	 * @return
	 */
	Optional<PatchInfo> getPatchInfo(String sessionId, LogicalTimestamp patchId);

	/**
	 * List all of the missing patches give a clock
	 * 
	 * @param sessionId
	 * @param clock
	 * @param limit
	 * @return
	 */
	List<LogicalTimestamp> listMissingPatchIdsForClock(String sessionId, List<LogicalTimestamp> clock, long limit);

	/**
	 * List the active grid session for a user filtered by the provided sourceId.
	 * @param userId
	 * @param sourceId The synID of the grid data source.
	 * @return
	 */
	List<GridSession> listActiveGridSession(Long userId, String sourceId, Long limit, Long offset);
	
	/**
	 * List all active grid sessions started by the provided user.
	 * @param userId
	 * @return
	 */
	List<GridSession> listActiveGridSession(Long userId, Long limit, Long offset);
	
	void deleteGridSession(String sessionId);
	
	void truncateAll();

}
