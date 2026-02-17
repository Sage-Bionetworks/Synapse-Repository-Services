package org.sagebionetworks.grid.db;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.encoding.SnapshotFileIndex;
import org.sagebionetworks.repo.model.grid.encoding.SnapshotFileIndexBuilder;
import org.sagebionetworks.repo.model.grid.encoding.IndexedNodeCodecMapper;
import org.sagebionetworks.repo.model.grid.encoding.SeekingNodeReader;
import org.sagebionetworks.repo.model.grid.node.ArrayNode;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.node.IndexType;
import org.sagebionetworks.repo.model.grid.node.Node;
import org.sagebionetworks.repo.model.grid.node.ObjectNode;
import org.sagebionetworks.repo.model.grid.node.RGANode;
import org.sagebionetworks.repo.model.grid.node.ValueNode;
import org.sagebionetworks.repo.model.grid.node.VectorNode;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.Patch;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

import com.google.common.collect.Iterables;

@Service
@GridTransaction(readOnly = true)
public class GridIndexManagerImpl implements GridIndexManager {

	public static final Duration MAX_MESSAGE_DURATION = Duration.ofSeconds(60);
	public static final int MAX_MESSAGE_ID = 65535;

	// The maximum number of nodes to process in a single batch during snapshot import.
	private static final int SNAPSHOT_IMPORT_BATCH_SIZE = 1000;

	private static final Logger log = LogManager.getLogger(GridIndexManagerImpl.class);

	private final GridIndexDao dao;
	private final OperationDispatcher operationDispatcher;
	private final SnapshotFileIndexBuilder snapshotIndexBuilder;
	private final SeekingNodeReaderProvider readerProvider;

	public GridIndexManagerImpl(GridIndexDao dao, OperationDispatcher operationDispatcher, SnapshotFileIndexBuilder snapshotIndexBuilder,
								SeekingNodeReaderProvider readerProvider) {
		super();
		this.dao = dao;
		this.operationDispatcher = operationDispatcher;
		this.snapshotIndexBuilder = snapshotIndexBuilder;
		this.readerProvider = readerProvider;
	}

	@Override
	@GridTransaction(readOnly = false)
	public Map<IndexType, Set<LogicalTimestamp>> applyPatch(String sessionId, Long replicaId, Patch patch) {
		ValidateArgument.required(sessionId, "sessionId");
		ValidateArgument.required(replicaId, "replicaId");
		ValidateArgument.required(patch, "patch");
		ValidateArgument.required(patch.getOperations(), "patch.operations");

		createReplicaIfNotExist(sessionId, replicaId);

		if (isPatchAlreadyApplied(sessionId, replicaId, patch.getPatchId())) {
			log.info("Patch: {}.{} has already been applied to session: {} replica: {}",
					patch.getPatchId().getReplicaId(), patch.getPatchId().getSequenceNumber(), sessionId, replicaId);
			return Collections.emptyMap();
		}

		// Operations are batched and processed by type.
		Map<IndexType, Set<LogicalTimestamp>> changes = operationDispatcher.processAll(sessionId, replicaId,
				patch.getOperations());

		LogicalTimestamp patchClock = LogicalTimestamp.newIncrement(patch.getPatchId(), patch.getSpan());

		/*
		 * Set the replica's clock to reflect the applied patch. For bootstrap patches
		 * (created during grid initialization), we must be careful not to increment
		 * this replica's sequence beyond other replicas' sequences, as this could cause
		 * outstanding bootstrap patches to be ignored during synchronization.
		 */
		dao.setClock(sessionId, replicaId, patchClock);
		return changes;
	}

	@Override
	@GridTransaction(readOnly = false)
	public void applySnapshot(String sessionId, Long replicaId, Path snapshotFile) {
		ValidateArgument.required(sessionId, "sessionId");
		ValidateArgument.required(replicaId, "replicaId");
		ValidateArgument.required(snapshotFile, "snapshotFile");

		// Delete the replica to clear the index; the snapshot will repopulate the index.
		dao.deleteReplica(sessionId, replicaId);

		// Recreate the replica. Exclude the root node, which is included in the snapshot.
		createReplicaIfNotExist(sessionId, replicaId);

		// Build the decoder (extracts ClockTable and rootNodeId, and builds a node index in a single pass)
		SnapshotFileIndex index;
		try {
			index = snapshotIndexBuilder.build(snapshotFile);
		} catch (IOException e) {
			throw new RuntimeException("Failed to build snapshot index: " + snapshotFile, e);
		}

		ClockTable snapshotClockTable = index.getClockTable();

		// Process each type in order using seeking reads
		try (SeekingNodeReader reader = readerProvider.create(snapshotFile, index)) {
			// Nodes are batched by type to minimize database round-trips.
			importConstantsFromSnapshot(sessionId, replicaId, reader);
			importObjectsFromSnapshot(sessionId, replicaId, reader);
			importValuesFromSnapshot(sessionId, replicaId, reader);
			importArraysFromSnapshot(sessionId, replicaId, reader);
			importVectorsFromSnapshot(sessionId, replicaId, reader);
		} catch (IOException e) {
			throw new RuntimeException("Failed to import snapshot from file: " + snapshotFile, e);
		}

		// Update the root val node (which always has ID 0.0) to point to the root object from the snapshot.
		dao.saveValues(sessionId, replicaId, List.of(new ValueNode()
				.setId(new LogicalTimestamp().setReplicaId(0L).setSequenceNumber(0L))
				.setValue(index.getRootNodeId()))
		);

		// Update the replica clock
		dao.setClocks(sessionId, replicaId, snapshotClockTable.getClocks());
	}

	/**
	 * Import constant nodes in a snapshot into the database.
	 */
	private void importConstantsFromSnapshot(String sessionId, Long replicaId, SeekingNodeReader reader) throws IOException {
		Stream<ConstantNode> stream = reader.streamConstantNodes();

		for (List<ConstantNode> batch : Iterables.partition(stream::iterator, SNAPSHOT_IMPORT_BATCH_SIZE)) {
			dao.saveIndex(sessionId, replicaId, IndexType.con, batch.stream().map(Node::getId).collect(Collectors.toList()));
			dao.saveNewConstants(sessionId, replicaId, batch);
		}
	}

	/**
	 * Import object nodes in a snapshot into the database.
	 */
	private void importObjectsFromSnapshot(String sessionId, Long replicaId, SeekingNodeReader reader) throws IOException {
		Stream<ObjectNode> stream = reader.streamObjectNodes();

		for (List<ObjectNode> batch : Iterables.partition(stream::iterator, SNAPSHOT_IMPORT_BATCH_SIZE)) {
			dao.saveIndex(sessionId, replicaId, IndexType.obj, batch.stream().map(Node::getId).collect(Collectors.toList()));
			dao.saveObjects(sessionId, replicaId, batch);
		}
	}

	/**
	 * Import value nodes in a snapshot into the database.
	 */
	private void importValuesFromSnapshot(String sessionId, Long replicaId, SeekingNodeReader reader) throws IOException {
		Stream<ValueNode> stream = reader.streamValueNodes();

		for (List<ValueNode> batch : Iterables.partition(stream::iterator, SNAPSHOT_IMPORT_BATCH_SIZE)) {
			dao.saveIndex(sessionId, replicaId, IndexType.val, batch.stream().map(Node::getId).collect(Collectors.toList()));
			dao.saveValues(sessionId, replicaId, batch);
		}
	}

	/**
	 * Import array nodes in a snapshot into the database.
	 */
	private void importArraysFromSnapshot(String sessionId, Long replicaId, SeekingNodeReader reader) throws IOException {
		Stream<ArrayNode> stream = reader.streamArrayNodes();

		for (List<ArrayNode> batch : Iterables.partition(stream::iterator, SNAPSHOT_IMPORT_BATCH_SIZE)) {
			List<LogicalTimestamp> ids = batch.stream().map(Node::getId).collect(Collectors.toList());
			dao.saveIndex(sessionId, replicaId, IndexType.arr, ids);
			dao.createArrayBatch(sessionId, replicaId, ids);
			// Read nodes and insert RGA elements for this batch
			List<RGANode> allElements = batch.stream()
				.flatMap(arr -> arr.getElements().stream())
				.collect(Collectors.toList());

			if (!allElements.isEmpty()) {
				// Since this Arrays are guaranteed empty (fresh import) - use fast path directly
				dao.batchInsertRgaNodes(sessionId, replicaId, allElements);
			}
		}
	}

	/**
	 * Import vector nodes in a snapshot into the database.
	 */
	private void importVectorsFromSnapshot(String sessionId, Long replicaId, SeekingNodeReader reader) throws IOException {
		Stream<VectorNode> stream = reader.streamVectorNodes();

		for (List<VectorNode> batch : Iterables.partition(stream::iterator, SNAPSHOT_IMPORT_BATCH_SIZE)) {
			dao.saveIndex(sessionId, replicaId, IndexType.vec, batch.stream().map(Node::getId).collect(Collectors.toList()));

			// Populate each vector with its constant values
			for (VectorNode vector : batch) {
				if (vector.getValues() != null) {
					vector.getValues().forEach((idx, constantNodeStub) -> {
						if (constantNodeStub != null) {
                            ConstantNode constantNodeWithData;
                            try {
                                constantNodeWithData = (ConstantNode) reader.readNode(IndexedNodeCodecMapper.CONSTANT, constantNodeStub.getId());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            if (constantNodeWithData != null) {
								constantNodeStub.setValue(constantNodeWithData.getConValue());
							}
						}
					});
				}
			}

			dao.saveVectors(sessionId, replicaId, batch);
		}
	}

	void createReplicaIfNotExist(String sessionId, Long replicaId) {
		if (dao.createReplicaIfNotExists(sessionId, replicaId)) {
			// this is the first patch of a replica.
			LogicalTimestamp rootId = new LogicalTimestamp().setReplicaId(0L).setSequenceNumber(0L);
			// create the root value of the document.
			dao.saveIndex(sessionId, replicaId, IndexType.val, List.of(rootId));
			dao.saveValues(sessionId, replicaId, List.of(new ValueNode().setId(rootId)));
		}
	}

	/**
	 * Has the given patch already been applied to this replica?
	 * 
	 * @param sessionId
	 * @param replicaId
	 * @param patchId
	 * @return
	 */
	boolean isPatchAlreadyApplied(String sessionId, Long replicaId, LogicalTimestamp patchId) {
		return dao.getClockSequenceNumber(sessionId, replicaId, patchId.getReplicaId())
				.map(seq -> patchId.getSequenceNumber() < seq).orElse(false);
	}

	@Override
	public List<LogicalTimestamp> getClock(String sessionId, Long replicaId) {
		return dao.getClock(sessionId, replicaId);
	}

	@Override
	@GridTransaction(readOnly = false)
	public MessageChain startMessageChain(String sessionId, Long replicaId, String method) {
		createReplicaIfNotExist(sessionId, replicaId);
		Integer id = dao.createNextMessageId(sessionId, replicaId, MAX_MESSAGE_ID);
		return dao.createMessageChain(
				new MessageChain().setSessionId(sessionId).setReplicaId(replicaId).setMethod(method).setId(id),
				MAX_MESSAGE_DURATION);
	}

	@Override
	public Optional<MessageChain> getMessageChain(String sessionId, Long replicaId, Integer chainId) {
		return dao.getMessageChain(sessionId, replicaId, chainId);
	}

	@Override
	@GridTransaction(readOnly = false)
	public void completeMessageChain(String sessionId, Long replicaId, Integer chainId) {
		dao.deleteMessageChain(sessionId, replicaId, chainId);
	}

	@Override
	@GridTransaction(readOnly = false)
	public void truncateAll() {
		dao.truncateAll();
	}

	@Override
	@GridTransaction(readOnly = false)
	public boolean refreshMessageChain(String sessionId, Long replicaId, Integer chainId) {
		return dao.refreshMessageChain(sessionId, replicaId, chainId, MAX_MESSAGE_DURATION);
	}

	@Override
	public Optional<MessageChain> getNonExpiredMessageChain(String sessionId, Long replicaId, String method) {
		return dao.getNonExpiredMessageChain(sessionId, replicaId, method);
	}
}
