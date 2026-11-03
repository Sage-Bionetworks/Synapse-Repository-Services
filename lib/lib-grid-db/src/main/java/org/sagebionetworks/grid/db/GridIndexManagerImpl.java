package org.sagebionetworks.grid.db;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.encoding.IndexedModelEncoder;
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
import org.sagebionetworks.util.FileProvider;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

import com.google.common.collect.Iterables;

@Service
@GridTransaction(readOnly = true)
public class GridIndexManagerImpl implements GridIndexManager {

	public static final Duration MAX_MESSAGE_DURATION = Duration.ofSeconds(60);
	public static final int MAX_MESSAGE_ID = 65535;

	// The maximum number of nodes to process in a single batch during snapshot import.
	public static final int DEFAULT_SNAPSHOT_IMPORT_BATCH_SIZE = 1000;

	private static final Logger log = LogManager.getLogger(GridIndexManagerImpl.class);

	private final GridIndexDao dao;
	private final OperationDispatcher operationDispatcher;
	private final SnapshotFileIndexBuilder snapshotIndexBuilder;
	private final SeekingNodeReaderProvider readerProvider;
	private final FileProvider fileProvider;
	private final int snapshotImportBatchSize;

	@Autowired
	public GridIndexManagerImpl(GridIndexDao dao, OperationDispatcher operationDispatcher, SnapshotFileIndexBuilder snapshotIndexBuilder,
								SeekingNodeReaderProvider readerProvider, FileProvider fileProvider) {
		this(dao, operationDispatcher, snapshotIndexBuilder, readerProvider, fileProvider, DEFAULT_SNAPSHOT_IMPORT_BATCH_SIZE);
	}

	public GridIndexManagerImpl(GridIndexDao dao, OperationDispatcher operationDispatcher, SnapshotFileIndexBuilder snapshotIndexBuilder,
								SeekingNodeReaderProvider readerProvider, FileProvider fileProvider, int snapshotImportBatchSize) {
		super();
		this.dao = dao;
		this.operationDispatcher = operationDispatcher;
		this.snapshotIndexBuilder = snapshotIndexBuilder;
		this.readerProvider = readerProvider;
		this.fileProvider = fileProvider;
		this.snapshotImportBatchSize = snapshotImportBatchSize;
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

		// Clear CRDT data without deleting the replica row or its message chains.
		// Using clearReplicaData() instead of deleteReplica() preserves GRID_REPLICA_MESSAGE,
		// so any active sync message chain (e.g. id=0) survives the snapshot import.
		// deleteReplica() would cascade-delete GRID_REPLICA_MESSAGE, causing the hub's
		// subsequent patch messages to fail with "No message chain found" and be permanently
		// discarded. See PLFM-9571.
		dao.clearReplicaData(sessionId, replicaId);

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

		// Update the replica clock to match the snapshot clock
		dao.setClocks(sessionId, replicaId, snapshotClockTable.getClocks());
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method requires at least repeatable read isolation — no concurrent writes to the same
	 * grid session should occur while the export is in progress. This is guaranteed by
	 * routing the export through the {@code GRID_INTERNAL_EVENT.fifo} queue, which
	 * serializes all messages for a given connection. The {@code READ_COMMITTED}
	 * transaction isolation (the default) is sufficient under this guarantee.
	 */
	@Override
	@GridTransaction(readOnly = true)
	public ClockTable exportSnapshot(String sessionId, Long replicaId, Path snapshotFile) {
		ValidateArgument.required(sessionId, "sessionId");
		ValidateArgument.required(replicaId, "replicaId");
		ValidateArgument.required(snapshotFile, "snapshotFile");

		// Get the root object to determine the root node ID
		ObjectNode rootObject = dao.getRootObject(sessionId, replicaId)
				.orElseThrow(() -> new IllegalStateException(
						"No root object found for session: " + sessionId + " replica: " + replicaId));

		LogicalTimestamp rootNodeId = rootObject.getId();

		try (OutputStream out = fileProvider.createFileOutputStream(snapshotFile.toFile());
				IndexedModelEncoder encoder = new IndexedModelEncoder(out, rootNodeId)) {

			// Stream and write all constants (paginated)
			exportConstants(sessionId, replicaId, encoder);

			// Stream and write all objects (paginated)
			exportObjects(sessionId, replicaId, encoder);

			// Stream and write all values, excluding (0,0) (paginated)
			exportValues(sessionId, replicaId, encoder);

			// Stream and write all vectors (paginated)
			exportVectors(sessionId, replicaId, encoder);

			// Get all array IDs, then for each array, read and write with tombstones
			exportArrays(sessionId, replicaId, encoder);

			/*
			 * Patches may have incremented the clock without creating corresponding nodes. This is expected; not all
			 * patch operations create nodes).
			 *
			 * Ensure these operations are accounted for by updating the snapshot's clock values to use the replica's
			 * database clock rather than the encoder's node-derived clock (which is intended to only be used for
			 *  newly-instantiated grids).
			 */
			ClockTable dbClock = new ClockTable(dao.getClock(sessionId, replicaId));
			ClockTable clockTable = encoder.getClockTable();
			for (LogicalTimestamp c : dbClock.getClocks()) {
				clockTable.updateClockTable(c);
			}
			// Return the database clock to be stored with snapshot metadata.
			return dbClock;
		} catch (IOException e) {
			throw new RuntimeException("Failed to export snapshot to file: " + snapshotFile, e);
		}
	}

	private void exportConstants(String sessionId, Long replicaId, IndexedModelEncoder encoder) throws IOException {
		LogicalTimestamp lastSeen = null;
		List<ConstantNode> batch;
		while (!(batch = dao.streamConstants(sessionId, replicaId, snapshotImportBatchSize, lastSeen)).isEmpty()) {
			for (ConstantNode node : batch) {
				encoder.writeNode(node);
			}
			lastSeen = batch.get(batch.size() - 1).getId();
		}
	}

	private void exportObjects(String sessionId, Long replicaId, IndexedModelEncoder encoder) throws IOException {
		LogicalTimestamp lastSeen = null;
		List<ObjectNode> batch;
		while (!(batch = dao.streamObjects(sessionId, replicaId, snapshotImportBatchSize, lastSeen)).isEmpty()) {
			for (ObjectNode node : batch) {
				encoder.writeNode(node);
			}
			lastSeen = batch.get(batch.size() - 1).getId();
		}
	}

	private void exportValues(String sessionId, Long replicaId, IndexedModelEncoder encoder) throws IOException {
		LogicalTimestamp lastSeen = null;
		List<ValueNode> batch;
		while (!(batch = dao.streamValues(sessionId, replicaId, snapshotImportBatchSize, lastSeen)).isEmpty()) {
			for (ValueNode node : batch) {
				encoder.writeNode(node);
			}
			lastSeen = batch.get(batch.size() - 1).getId();
		}
	}

	private void exportVectors(String sessionId, Long replicaId, IndexedModelEncoder encoder) throws IOException {
		LogicalTimestamp lastSeen = null;
		List<VectorNode> batch;
		while (!(batch = dao.streamVectors(sessionId, replicaId, snapshotImportBatchSize, lastSeen)).isEmpty()) {
			for (VectorNode node : batch) {
				encoder.writeNode(node);
			}
			lastSeen = batch.get(batch.size() - 1).getId();
		}
	}

	private void exportArrays(String sessionId, Long replicaId, IndexedModelEncoder encoder) throws IOException {
		List<LogicalTimestamp> arrayIds = dao.getAllArrayIds(sessionId, replicaId);
		for (LogicalTimestamp arrayId : arrayIds) {
			// Read the full array with tombstones
			ArrayNode arrayNode = dao.getArrayNode(sessionId, replicaId, arrayId, true, Long.MAX_VALUE, 0L);
			encoder.writeNode(arrayNode);
		}
	}

	/**
	 * Import constant nodes in a snapshot into the database.
	 */
	private void importConstantsFromSnapshot(String sessionId, Long replicaId, SeekingNodeReader reader) throws IOException {
		Stream<ConstantNode> stream = reader.streamConstantNodes();

		for (List<ConstantNode> batch : Iterables.partition(stream::iterator, snapshotImportBatchSize)) {
			dao.saveIndex(sessionId, replicaId, IndexType.con, batch.stream().map(Node::getId).collect(Collectors.toList()));
			dao.saveNewConstants(sessionId, replicaId, batch);
		}
	}

	/**
	 * Import object nodes in a snapshot into the database.
	 */
	private void importObjectsFromSnapshot(String sessionId, Long replicaId, SeekingNodeReader reader) throws IOException {
		Stream<ObjectNode> stream = reader.streamObjectNodes();

		for (List<ObjectNode> batch : Iterables.partition(stream::iterator, snapshotImportBatchSize)) {
			dao.saveIndex(sessionId, replicaId, IndexType.obj, batch.stream().map(Node::getId).collect(Collectors.toList()));
			dao.saveObjects(sessionId, replicaId, batch);
		}
	}

	/**
	 * Import value nodes in a snapshot into the database.
	 */
	private void importValuesFromSnapshot(String sessionId, Long replicaId, SeekingNodeReader reader) throws IOException {
		Stream<ValueNode> stream = reader.streamValueNodes();

		for (List<ValueNode> batch : Iterables.partition(stream::iterator, snapshotImportBatchSize)) {
			dao.saveIndex(sessionId, replicaId, IndexType.val, batch.stream().map(Node::getId).collect(Collectors.toList()));
			dao.saveValues(sessionId, replicaId, batch);
		}
	}

	/**
	 * Import array nodes in a snapshot into the database.
	 */
	private void importArraysFromSnapshot(String sessionId, Long replicaId, SeekingNodeReader reader) throws IOException {
		Stream<ArrayNode> stream = reader.streamArrayNodes();

		for (List<ArrayNode> batch : Iterables.partition(stream::iterator, snapshotImportBatchSize)) {
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

		for (List<VectorNode> batch : Iterables.partition(stream::iterator, snapshotImportBatchSize)) {
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
