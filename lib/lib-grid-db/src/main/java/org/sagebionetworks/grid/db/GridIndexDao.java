package org.sagebionetworks.grid.db;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.grid.node.ArrayNode;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.node.IndexNode;
import org.sagebionetworks.repo.model.grid.node.IndexType;
import org.sagebionetworks.repo.model.grid.node.ObjectNode;
import org.sagebionetworks.repo.model.grid.node.ValueNode;
import org.sagebionetworks.repo.model.grid.node.VectorNode;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.Timespan;
import org.sagebionetworks.repo.model.grid.patch.operation.NewVector;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

public interface GridIndexDao extends ConstantProvider {

	/**
	 * Create a new Replica, if it does not already exist. This the first step to
	 * start a new replica.
	 * 
	 * @param sessionId
	 * @param replicaId
	 */
	boolean createReplicaIfNotExists(String sessionId, Long replicaId);

	/**
	 * Get the created on for a grid replica.
	 * 
	 * @param sessionId
	 * @param replicaId
	 * @return
	 */
	Optional<Timestamp> getReplicaCreatedOn(String sessionId, Long replicaId);

	/**
	 * Delete a replica and all of its data.
	 * 
	 * @param sessionId
	 * @param replicaId
	 */
	void deleteReplica(String sessionId, Long replicaId);

	/**
	 * Get the a replica's full clock.
	 * 
	 * @param sessionId
	 * @param replicaId
	 * @return
	 */
	List<LogicalTimestamp> getClock(String sessionId, Long replicaId);

	/**
	 * Get the current sequence number for a clock replica ID if it exists.
	 * 
	 * @param sessionId
	 * @param replicaId
	 * @param clockIdRep
	 * @return {@link Optional#empty()} if there is no clock for the provided
	 *         patchId.
	 */
	Optional<Long> getClockSequenceNumber(String sessionId, Long replicaId, Long clockIdRep);

	/**
	 * Save a batch of Index objects to a replica.
	 * 
	 * @param sessionId
	 * @param replicaId
	 * @param type      The type of objects in the batch.
	 * @param batch     The batch of timestamps to save.
	 */
	void saveIndex(String sessionId, Long replicaId, IndexType type, List<LogicalTimestamp> batch);

	/**
	 * Get a list of indices given their IDs.
	 * 
	 * @param sessionId
	 * @param replicaId
	 * @param ids
	 * @return
	 */
	List<IndexNode> getIndices(String sessionId, Long replicaId, List<LogicalTimestamp> ids);

	/**
	 * Save a batch of {@link ObjectNode} to a replica.
	 * 
	 * @param sessionId
	 * @param replicaId
	 * @param batch
	 */
	void saveObjects(String sessionId, Long replicaId, List<ObjectNode> batch);

	/**
	 * Get a batch of {@link ObjectNode} given their Ids.
	 * 
	 * @param sessionId
	 * @param replicaId
	 * @param ids
	 * @return
	 */
	List<ObjectNode> getObjects(String sessionId, Long replicaId, List<LogicalTimestamp> ids);

	/**
	 * Save a batch of {@link ConstantNode} to a replica.
	 * 
	 * @param sessionId
	 * @param replicaId
	 * @param batch
	 */
	void saveNewConstants(String sessionId, Long replicaId, List<ConstantNode> batch);

	/**
	 * Get a batch of {@link ConstantNode} given their Ids.
	 * 
	 * @param sessionId
	 * @param replicaId
	 * @param ids
	 * @return
	 */
	List<ConstantNode> getConstants(String sessionId, Long replicaId, List<LogicalTimestamp> ids);

	/**
	 * Set a single clock value for a replica.
	 * 
	 * @param sessionId
	 * @param replicaId
	 * @param clock
	 */
	void setClock(String sessionId, Long replicaId, LogicalTimestamp clock);

	/**
	 * Save a batch of {@link ValueNode} to a replica.
	 * 
	 * @param sessionIdString
	 * @param replicaId
	 * @param batch
	 */
	void saveValues(String sessionIdString, Long replicaId, List<ValueNode> batch);

	/**
	 * Get a batch of {@link ValueNode} from given their IDs.
	 * 
	 * @param sessionIdString
	 * @param replicaId
	 * @param ids
	 * @return
	 */
	List<ValueNode> getValues(String sessionIdString, Long replicaId, List<LogicalTimestamp> ids);

	/**
	 * Save a batch of {@link NewVector} to a replica.
	 * 
	 * @param sessionId
	 * @param replicaId
	 * @param list
	 */
	void saveVectors(String sessionId, Long replicaId, List<VectorNode> list);

	/**
	 * Get a batch of {@link VectorNode} given their IDs
	 * 
	 * @param sessionIdString
	 * @param replicaId
	 * @param ids
	 * @return
	 */
	List<VectorNode> getVectors(String sessionIdString, Long replicaId, List<LogicalTimestamp> ids);

	/**
	 * Will create an a new empty array for each provided array ID.s
	 * 
	 * @param sessionIdString
	 * @param replicaId
	 * @param arrayIds
	 */
	void createArrayBatch(String sessionIdString, Long replicaId, List<LogicalTimestamp> arrayIds);

	/**
	 * Insert an {@link ArrayNode} into an array.
	 * 
	 * @param sessionIdString
	 * @param replicaId
	 * @param toInsert
	 */
	void insertIntoArray(String sessionIdString, Long replicaId, ArrayNode toInsert);

	/**
	 * Get a single page of ordered {@link ArrayNode}.
	 * 
	 * @param sessionIdString
	 * @param replicaId
	 * @param arrayId
	 * @param limit
	 * @param offset
	 * @return
	 */
	List<ArrayNode> getArrayNodesInOrder(String sessionIdString, Long replicaId, LogicalTimestamp arrayId, Long limit,
			Long offset);


	/**
	 * 
	 * @param sessionIdString
	 * @param replicaId
	 * @param arrayId
	 * 
	 * @return The last node in the given array, empty if the array has no nodes.
	 */
	Optional<ArrayNode> getArrayLastNode(String sessionIdString, Long replicaId, LogicalTimestamp arrayId);
	
	/**
	 * Given a new {@link ArrayNode} to insert, find the location where the node
	 * should actually be inserted following the RGA insert algorithm (specifically
	 * step three).
	 */
	Optional<LogicalTimestamp> findArrayInsertLocation(String sessionIdString, Long replicaId, ArrayNode toInsert);

	/**
	 * Marks as deleted all the nodes that belongs to the given arrayId and falls into the given batch of id ranges.
	 * 
	 * @param sessionId
	 * @param replicaId
	 * @param arrayId
	 * @param idRangeBatch A batch of intervals of logical timestamps
	 */
	void deleteArrayNodes(String sessionId, Long replicaId, LogicalTimestamp arrayId, List<Timespan> idRangeBatch);
	
	/**
	 * Create the next message ID to start a new message chain.The id resets to zero
	 * when it reaches 65535.
	 * 
	 * @see <a href=
	 *      "https://jsonjoy.com/specs/json-rx/messages#Sequence-number-(message-ID)-component">Sequence-number-(message-ID)-component</a>
	 * 
	 * @param sessionIdString
	 * @param replicaId
	 * @param maxValue
	 * @return
	 */
	Integer createNextMessageId(String sessionIdString, Long replicaId, int maxValue);

	/**
	 * Create a new {@link MessageChain} to track this chain as it is executed.
	 * 
	 * @param setMethod
	 * @return
	 */
	MessageChain createMessageChain(MessageChain setMethod,  Duration expires);
	
	/**
	 * Refresh the expiration of the provided message chain.
	 * @param sessionId
	 * @param replicaId
	 * @param chainId
	 * @param expires
	 * @return
	 */
	boolean refreshMessageChain(String sessionId, Long replicaId, Integer chainId, Duration expires);

	/**
	 * Get a {@link MessageChain} if it exists.
	 * 
	 * @param sessionId
	 * @param replicaId
	 * @param chainId
	 * @return Optional.empty() if the chain no longer exists
	 */
	Optional<MessageChain> getMessageChain(String sessionId, Long replicaId, Integer chainId);
	
	/**
	 * Determine if a non-expired message chain already exists for the given method name.
	 * 
	 * @param sessionId
	 * @param replicaId
	 * @param method
	 * @return
	 */
	Optional<MessageChain> getNonExpiredMessageChain(String sessionId, Long replicaId, String method);
	

	/**
	 * Delete a message chain upon completion. This will free up the ID to be
	 * recycled if needed.
	 * 
	 * @param sessionId
	 * @param replicaId
	 * @param chainId
	 */
	void deleteMessageChain(String sessionId, Long replicaId, Integer chainId);

	/**
	 * Get the root object of the document.
	 * 
	 * @param sessionId
	 * @param replicaId
	 * @return
	 */
	Optional<ObjectNode> getRootObject(String sessionId, Long replicaId);

	/**
	 * Run a custom query against the nodes.
	 * 
	 * @param <T>
	 * @param sql
	 * @param paramSource
	 * @param rowMapper
	 * @return
	 */
	<T> List<T> query(String sql, SqlParameterSource paramSource, RowMapper<T> rowMapper);
	
	/**
	 * Delete all grid data from the database.
	 */
	void truncateAll();

	/**
	 * The maximum sequence number from the replica's clock.
	 * @param gridSessionId
	 * @param replicaId
	 * @return
	 */
	Long getClockSequenceMaximum(String gridSessionId, Long replicaId);

}
