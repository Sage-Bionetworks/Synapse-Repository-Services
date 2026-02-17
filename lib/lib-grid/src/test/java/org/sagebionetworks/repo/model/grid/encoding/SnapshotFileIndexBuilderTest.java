package org.sagebionetworks.repo.model.grid.encoding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.node.ArrayNode;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.node.Node;
import org.sagebionetworks.repo.model.grid.node.ObjectNode;
import org.sagebionetworks.repo.model.grid.node.RGANode;
import org.sagebionetworks.repo.model.grid.node.ValueNode;
import org.sagebionetworks.repo.model.grid.node.VectorNode;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class SnapshotFileIndexBuilderTest {

	private Path tempFile;

	private SnapshotFileIndexBuilder indexBuilder;

	@BeforeEach
	public void setUp() throws IOException {
		tempFile = Files.createTempFile("snapshot-index-test-", ".cbor");
		indexBuilder = new SnapshotFileIndexBuilder();
	}

	@AfterEach
	public void tearDown() throws IOException {
		if (tempFile != null) {
			Files.deleteIfExists(tempFile);
		}
	}

	@Test
	public void testBuildIndexWithAllNodeTypes() throws IOException {
		// Create a snapshot with all node types
		LogicalTimestamp rootId = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(1L);
		LogicalTimestamp objectId = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(4L);

		List<Node> nodes = new ArrayList<>();
		nodes.add(new ConstantNode()
				.setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(2L))
				.setValue(new ConValue(ConType.LONG, 42L)));
		nodes.add(new ConstantNode()
				.setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(3L))
				.setValue(new ConValue(ConType.STRING, "hello")));
		nodes.add(new ObjectNode()
				.setId(objectId)
				.setValue(Map.of("key", new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(2L))));
		nodes.add(new ValueNode()
				.setId(rootId)
				.setValue(objectId));  // ValueNode points to the object
		nodes.add(new VectorNode()
				.setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(5L))
				.setValues(Map.of(0, new ConstantNode().setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(2L)))));
		nodes.add(new ArrayNode()
				.setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(6L))
				.setElements(List.of(
						new RGANode()
								.setContainerId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(6L))
								.setNodeId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(6L))
								.setDataId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(2L))
								.setIsDeleted(false)
				)));

		// Write the snapshot to a file
		createSnapshot(tempFile, rootId, nodes);

		// call under test
		SnapshotFileIndex index = indexBuilder.build(tempFile);

		// Verify the index
		assertNotNull(index);
		assertEquals(rootId, index.getRootNodeId());
		assertEquals(6, index.getTotalNodeCount());

		// Verify constants (2 nodes)
		Map<LogicalTimestamp, SnapshotFileIndex.NodePointer> constants = index.getEntriesForType(IndexedNodeCodecMapper.CONSTANT);
		assertEquals(2, constants.size());

		// Verify objects (1 node)
		Map<LogicalTimestamp, SnapshotFileIndex.NodePointer> objects = index.getEntriesForType(IndexedNodeCodecMapper.OBJECT);
		assertEquals(1, objects.size());

		// Verify values (1 node)
		Map<LogicalTimestamp, SnapshotFileIndex.NodePointer> values = index.getEntriesForType(IndexedNodeCodecMapper.VAL);
		assertEquals(1, values.size());

		// Verify vectors (1 node)
		Map<LogicalTimestamp, SnapshotFileIndex.NodePointer> vectors = index.getEntriesForType(IndexedNodeCodecMapper.VECTOR);
		assertEquals(1, vectors.size());

		// Verify arrays (1 node)
		Map<LogicalTimestamp, SnapshotFileIndex.NodePointer> arrays = index.getEntriesForType(IndexedNodeCodecMapper.ARRAY);
		assertEquals(1, arrays.size());

		// Verify each entry has valid offset and length
		for (Map.Entry<LogicalTimestamp, SnapshotFileIndex.NodePointer> entry : constants.entrySet()) {
			assertNotNull(entry.getKey());
			assertTrue(entry.getValue().byteOffset() > 0, "Byte offset should be positive");
			assertTrue(entry.getValue().binaryLength() > 0, "Binary length should be positive");
		}
	}

	@Test
	public void testBuildIndexWithMinimalFile() throws IOException {
		// Create a minimal snapshot (just root value + constant)
		LogicalTimestamp rootId = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(1L);
		LogicalTimestamp constId = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(2L);
		List<Node> nodes = List.of(
				new ConstantNode().setId(constId).setValue(new ConValue(ConType.LONG, 42L)),
				new ValueNode().setId(rootId).setValue(constId)
		);

		createSnapshot(tempFile, rootId, nodes);

		// call under test
		SnapshotFileIndex index = indexBuilder.build(tempFile);

		assertEquals(rootId, index.getRootNodeId());
		assertEquals(2, index.getTotalNodeCount());
		assertEquals(1, index.getEntriesForType(IndexedNodeCodecMapper.VAL).size());
		assertEquals(1, index.getEntriesForType(IndexedNodeCodecMapper.CONSTANT).size());
		assertEquals(0, index.getEntriesForType(IndexedNodeCodecMapper.OBJECT).size());
		assertEquals(0, index.getEntriesForType(IndexedNodeCodecMapper.VECTOR).size());
		assertEquals(0, index.getEntriesForType(IndexedNodeCodecMapper.ARRAY).size());
	}

	@Test
	public void testBuildIndexWithNullPath() {
		// call under test
		assertThrows(IllegalArgumentException.class, () ->
				indexBuilder.build(null)
		);
	}

	@Test
	public void testGetClockTable() throws IOException {
		LogicalTimestamp rootId = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(1L);
		LogicalTimestamp constId = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(2L);
		List<Node> nodes = List.of(
				new ConstantNode().setId(constId).setValue(new ConValue(ConType.LONG, 42L)),
				new ValueNode().setId(rootId).setValue(constId)
		);

		ClockTable expectedClockTable = createSnapshot(tempFile, rootId, nodes);

		SnapshotFileIndex index = indexBuilder.build(tempFile);

		// call under test
		assertEquals(expectedClockTable, index.getClockTable());
		assertEquals(rootId, index.getRootNodeId());
	}

	@Test
	public void testGetIndex() throws IOException {
		LogicalTimestamp rootId = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(1L);
		LogicalTimestamp constId = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(2L);
		List<Node> nodes = List.of(
				new ConstantNode().setId(constId).setValue(new ConValue(ConType.LONG, 42L)),
				new ValueNode().setId(rootId).setValue(constId)
		);
		createSnapshot(tempFile, rootId, nodes);

		SnapshotFileIndex index = indexBuilder.build(tempFile);

		// call under test
		assertThrows(IllegalArgumentException.class, () ->
				index.getPointer(null, null)
		);
	}

	/**
	 * Helper to create a snapshot file and return its clock table.
	 */
	private ClockTable createSnapshot(Path file, LogicalTimestamp rootId, List<Node> nodes) throws IOException {
		byte[] encodedBytes;
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
			 IndexedModelEncoder encoder = new IndexedModelEncoder(baos, rootId)) {
			for (Node node : nodes) {
				encoder.writeNode(node);
			}
			encoder.close();
			encodedBytes = baos.toByteArray();
		}

		Files.write(file, encodedBytes);

		// Extract clock table from the encoded data
		ClockTable clockTable = new ClockTable(new ArrayList<>());
		for (Node node : nodes) {
			clockTable.processNode(node);
		}
		return clockTable;
	}
}
