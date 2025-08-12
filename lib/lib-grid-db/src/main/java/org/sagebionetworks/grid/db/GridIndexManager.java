package org.sagebionetworks.grid.db;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.sagebionetworks.repo.model.grid.node.IndexType;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.Patch;

public interface GridIndexManager {

	/**
	 * Apply the patch in a transaction.
	 * 
	 * @param sessionId
	 * @param replicaId
	 * @param patch
	 * @return The unique node ids group by type that were changed by the provided
	 *         patch.
	 */
	Map<IndexType, Set<LogicalTimestamp>> applyPatch(String sessionId, Long replicaId, Patch patch);

	/**
	 * Get the a replica's full clock.
	 * 
	 * @param sessionId
	 * @param replicaId
	 * @return
	 */
	List<LogicalTimestamp> getClock(String sessionId, Long replicaId);

	/**
	 * Start a new message chain.
	 * 
	 * @param sessionId
	 * @param replicaId
	 * @param method
	 * @return
	 */
	MessageChain startMessageChain(String sessionId, Long replicaId, String method);

	/**
	 * Get an existing message chain given its id.
	 * 
	 * @param sessionId
	 * @param replicaId
	 * @param chainId
	 * @return Optional.empty() if the chain no longer exists.
	 */
	Optional<MessageChain> getMessageChain(String sessionId, Long replicaId, Integer chainId);

	/**
	 * Complete a message chain. This will delete the chain an free the ID to be
	 * recycled.
	 * 
	 * @param sessionId
	 * @param replicaId
	 * @param chainId
	 */
	void completeMessageChain(String sessionId, Long replicaId, Integer chainId);

	void truncateAll();

}
