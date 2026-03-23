package org.sagebionetworks.repo.model.grid.encoding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONObject;
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

public class SeekingNodeReaderTest {

	private Path tempFile;
	private List<Node> originalNodes;
	private SnapshotFileIndexBuilder indexBuilder;

	@BeforeEach
	public void setUp() throws IOException {
		tempFile = Files.createTempFile("seeking-reader-test-", ".cbor");
		indexBuilder = new SnapshotFileIndexBuilder();
	}

	@AfterEach
	public void tearDown() throws IOException {
		if (tempFile != null) {
			Files.deleteIfExists(tempFile);
		}
	}

	@Test
	public void testReadConstantNode() throws IOException {
		LogicalTimestamp rootId = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(1L);
		ConstantNode constant = new ConstantNode()
				.setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(2L))
				.setValue(new ConValue(ConType.LONG, 42L));

		originalNodes = List.of(constant, new ValueNode().setId(rootId).setValue(constant.getId()));
		createSnapshot(tempFile, rootId, originalNodes);

		SnapshotFileIndex index = indexBuilder.build(tempFile);
		Map<LogicalTimestamp, SnapshotFileIndex.NodePointer> constantEntries = index.getEntriesForType(IndexedNodeCodecMapper.CONSTANT);
		assertEquals(1, constantEntries.size());

		try (SeekingNodeReader reader = new SeekingNodeReader(tempFile, index)) {
			// call under test
			Node readNode = reader.readNode(IndexedNodeCodecMapper.CONSTANT, constant.getId());

			assertNotNull(readNode);
			assertTrue(readNode instanceof ConstantNode);
			assertEquals(constant, readNode);
		}
	}

	@Test
	public void testReadObjectNode() throws IOException {
		LogicalTimestamp rootId = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(1L);
		LogicalTimestamp constId = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(2L);
		ObjectNode object = new ObjectNode()
				.setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(3L))
				.setValue(Map.of("key", constId));

		originalNodes = List.of(
				new ConstantNode().setId(constId).setValue(new ConValue(ConType.STRING, "value")),
				object,
				new ValueNode().setId(rootId).setValue(object.getId())
		);
		createSnapshot(tempFile, rootId, originalNodes);

		SnapshotFileIndex index = indexBuilder.build(tempFile);
		Map<LogicalTimestamp, SnapshotFileIndex.NodePointer> objectEntries = index.getEntriesForType(IndexedNodeCodecMapper.OBJECT);
		assertEquals(1, objectEntries.size());

		try (SeekingNodeReader reader = new SeekingNodeReader(tempFile, index)) {
			// call under test
			Node readNode = reader.readNode(IndexedNodeCodecMapper.OBJECT, object.getId());

			assertNotNull(readNode);
			assertTrue(readNode instanceof ObjectNode);
			assertEquals(object, readNode);
		}
	}

	@Test
	public void testReadValueNode() throws IOException {
		LogicalTimestamp rootId = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(1L);
		LogicalTimestamp constId = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(2L);
		ConstantNode constant = new ConstantNode().setId(constId).setValue(new ConValue(ConType.LONG, 42L));
		ValueNode value = new ValueNode().setId(rootId).setValue(constId);

		originalNodes = List.of(constant, value);
		createSnapshot(tempFile, rootId, originalNodes);

		SnapshotFileIndex index = indexBuilder.build(tempFile);
		Map<LogicalTimestamp, SnapshotFileIndex.NodePointer> valueEntries = index.getEntriesForType(IndexedNodeCodecMapper.VAL);
		assertEquals(1, valueEntries.size());

		try (SeekingNodeReader reader = new SeekingNodeReader(tempFile, index)) {
			// call under test
			Node readNode = reader.readNode(IndexedNodeCodecMapper.VAL, rootId);

			assertNotNull(readNode);
			assertTrue(readNode instanceof ValueNode);
			assertEquals(value, readNode);
		}
	}

	@Test
	public void testReadVectorNode() throws IOException {
		LogicalTimestamp rootId = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(1L);
		LogicalTimestamp constId = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(2L);
		LogicalTimestamp vecId = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(3L);
		VectorNode vector = new VectorNode()
				.setId(vecId)
				.setValues(Map.of(0, new ConstantNode().setId(constId)));

		originalNodes = List.of(
				new ConstantNode().setId(constId).setValue(new ConValue(ConType.LONG, 99L)),
				vector,
				new ValueNode().setId(rootId).setValue(vector.getId())
		);
		createSnapshot(tempFile, rootId, originalNodes);

		SnapshotFileIndex index = indexBuilder.build(tempFile);
		Map<LogicalTimestamp, SnapshotFileIndex.NodePointer> vectorEntries = index.getEntriesForType(IndexedNodeCodecMapper.VECTOR);
		assertEquals(1, vectorEntries.size());

		try (SeekingNodeReader reader = new SeekingNodeReader(tempFile, index)) {
			// call under test
			Node readNode = reader.readNode(IndexedNodeCodecMapper.VECTOR, vecId);

			assertNotNull(readNode);
			assertTrue(readNode instanceof VectorNode);
			assertEquals(vector, readNode);
		}
	}

	@Test
	public void testReadArrayNode() throws IOException {
		LogicalTimestamp rootId = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(1L);
		LogicalTimestamp arrayId = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(2L);
		LogicalTimestamp dataId = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(3L);

		// Note: The decoder sets referenceNodeId to the array head (containerId) for the first element
		ArrayNode array = new ArrayNode()
				.setId(arrayId)
				.setElements(List.of(
						new RGANode()
								.setContainerId(arrayId)
								.setNodeId(arrayId)
								.setReferenceNodeId(arrayId)  // First element references the array head
								.setDataId(dataId)
								.setIsDeleted(false)
				));

		originalNodes = List.of(
				new ConstantNode().setId(dataId).setValue(new ConValue(ConType.BOOLEAN, true)),
				array,
				new ValueNode().setId(rootId).setValue(arrayId)
		);
		createSnapshot(tempFile, rootId, originalNodes);

		SnapshotFileIndex index = indexBuilder.build(tempFile);
		Map<LogicalTimestamp, SnapshotFileIndex.NodePointer> arrayEntries = index.getEntriesForType(IndexedNodeCodecMapper.ARRAY);
		assertEquals(1, arrayEntries.size());

		try (SeekingNodeReader reader = new SeekingNodeReader(tempFile, index)) {
			// call under test
			Node readNode = reader.readNode(IndexedNodeCodecMapper.ARRAY, arrayId);

			assertNotNull(readNode);
			assertTrue(readNode instanceof ArrayNode);
			assertEquals(array, readNode);
		}
	}

	@Test
	public void testStreamNodes() throws IOException {
		LogicalTimestamp rootId = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(1L);
		ConstantNode const1 = new ConstantNode()
				.setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(2L))
				.setValue(new ConValue(ConType.LONG, 1L));
		ConstantNode const2 = new ConstantNode()
				.setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(3L))
				.setValue(new ConValue(ConType.LONG, 2L));
		ConstantNode const3 = new ConstantNode()
				.setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(4L))
				.setValue(new ConValue(ConType.LONG, 3L));

		originalNodes = List.of(const1, const2, const3, new ValueNode().setId(rootId).setValue(const1.getId()));
		createSnapshot(tempFile, rootId, originalNodes);

		SnapshotFileIndex index = indexBuilder.build(tempFile);
		Map<LogicalTimestamp, SnapshotFileIndex.NodePointer> constantEntries = index.getEntriesForType(IndexedNodeCodecMapper.CONSTANT);
		assertEquals(3, constantEntries.size());

		try (SeekingNodeReader reader = new SeekingNodeReader(tempFile, index)) {
			// call under test
			List<Node> readNodes = reader.streamConstantNodes().collect(Collectors.toList());

			assertEquals(3, readNodes.size());
			// The order matches the entry order, which is the file order
			assertTrue(readNodes.stream().allMatch(n -> n instanceof ConstantNode));
		}
	}

	@Test
	public void testReadNodesInReverseOrder() throws IOException {
		// Tests that seeking works correctly regardless of access order
		LogicalTimestamp rootId = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(1L);
		ConstantNode const1 = new ConstantNode()
				.setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(2L))
				.setValue(new ConValue(ConType.LONG, 1L));
		ConstantNode const2 = new ConstantNode()
				.setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(3L))
				.setValue(new ConValue(ConType.LONG, 2L));

		originalNodes = List.of(const1, const2, new ValueNode().setId(rootId).setValue(const1.getId()));
		createSnapshot(tempFile, rootId, originalNodes);

		SnapshotFileIndex index = indexBuilder.build(tempFile);
		try (SeekingNodeReader reader = new SeekingNodeReader(tempFile, index)) {
			// Read in reverse order
			Node second = reader.readNode(IndexedNodeCodecMapper.CONSTANT, const2.getId());
			Node first = reader.readNode(IndexedNodeCodecMapper.CONSTANT, const1.getId());

			assertEquals(const2, second);
			assertEquals(const1, first);
		}
	}

	@Test
	public void testReadNodeWithNullEntry() throws IOException {
		LogicalTimestamp rootId = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(1L);
		LogicalTimestamp constId = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(2L);
		originalNodes = List.of(
				new ConstantNode().setId(constId).setValue(new ConValue(ConType.LONG, 42L)),
				new ValueNode().setId(rootId).setValue(constId)
		);
		createSnapshot(tempFile, rootId, originalNodes);

		SnapshotFileIndex index = indexBuilder.build(tempFile);
		try (SeekingNodeReader reader = new SeekingNodeReader(tempFile, index)) {
			// call under test
			assertThrows(IllegalArgumentException.class, () ->
					reader.readNode(null, null)
			);
		}
	}

	@Test
	public void testConstructorWithNullPath() {
		SnapshotFileIndex ct = new SnapshotFileIndex(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L),
				new ClockTable(List.of(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(1L))),
				Collections.emptyMap()
		);

		// call under test
		assertThrows(IllegalArgumentException.class, () ->
				new SeekingNodeReader(null, ct)
		);
	}

	@Test
	public void testConstructorWithNullClockTable() {
		// call under test
		assertThrows(IllegalArgumentException.class, () ->
				new SeekingNodeReader(tempFile, null)
		);
	}

	@Test
	public void testDecodeJsonJoySnapshotEmptyObject() throws IOException {
		Path resourceFile = copyResourceToTempFile("/indexed-model/empty-object.cbor");
		try {
			SnapshotFileIndex index = indexBuilder.build(resourceFile);

			// Verify clock table
			ClockTable clockTable = index.getClockTable();
			assertNotNull(clockTable);
			assertEquals(1, clockTable.getClocks().size());
			assertEquals(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L), clockTable.getClocks().get(0));

			// Verify root node ID
			LogicalTimestamp expectedRootId = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L);
			assertEquals(expectedRootId, index.getRootNodeId());

			// Verify node counts
			assertEquals(1, index.getTotalNodeCount());
			assertEquals(1, index.getEntriesForType(IndexedNodeCodecMapper.OBJECT).size());

			// Verify we can read the node using SeekingNodeReader
			try (SeekingNodeReader reader = new SeekingNodeReader(resourceFile, index)) {
				List<Node> nodes = reader.streamObjectNodes().collect(Collectors.toList());
				assertEquals(1, nodes.size());

				// Empty object
				ObjectNode expected = new ObjectNode()
						.setId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L))
						.setValue(java.util.Map.of());
				assertEquals(expected, nodes.get(0));
			}
		} finally {
			Files.deleteIfExists(resourceFile);
		}
	}

	@Test
	public void testDecodeJsonJoySnapshotMultipleReplicas() throws IOException {
		Path resourceFile = copyResourceToTempFile("/indexed-model/multiple-replicas.cbor");
		try {
			SnapshotFileIndex index = indexBuilder.build(resourceFile);

			// Verify clock table
			ClockTable clockTable = index.getClockTable();
			assertNotNull(clockTable);
			assertEquals(3, clockTable.getClocks().size());
			assertEquals(new LogicalTimestamp().setReplicaId(100002L).setSequenceNumber(6L), clockTable.getClocks().get(0));
			assertEquals(new LogicalTimestamp().setReplicaId(100001L).setSequenceNumber(4L), clockTable.getClocks().get(1));
			assertEquals(new LogicalTimestamp().setReplicaId(2L).setSequenceNumber(2L), clockTable.getClocks().get(2));

			// Verify root node ID
			assertEquals(new LogicalTimestamp().setReplicaId(2L).setSequenceNumber(1L), index.getRootNodeId());

			// Verify node counts
			assertEquals(3, index.getTotalNodeCount());
			assertEquals(1, index.getEntriesForType(IndexedNodeCodecMapper.OBJECT).size());
			assertEquals(2, index.getEntriesForType(IndexedNodeCodecMapper.CONSTANT).size());

			// Verify we can read the nodes using SeekingNodeReader
			try (SeekingNodeReader reader = new SeekingNodeReader(resourceFile, index)) {
				// Read the object node
				List<Node> objectNodes = reader.streamObjectNodes().collect(Collectors.toList());
				assertEquals(1, objectNodes.size());

				ObjectNode expectedObj = new ObjectNode()
						.setId(new LogicalTimestamp().setReplicaId(2L).setSequenceNumber(1L))
						.setValue(java.util.Map.of("rep1", new LogicalTimestamp().setReplicaId(100001L).setSequenceNumber(3L),
								"rep2", new LogicalTimestamp().setReplicaId(100002L).setSequenceNumber(5L)));
				assertEquals(expectedObj, objectNodes.get(0));

				// Read constant nodes
				Map<LogicalTimestamp, SnapshotFileIndex.NodePointer> constants = index.getEntriesForType(IndexedNodeCodecMapper.CONSTANT);
				assertEquals(2, constants.size());

				// Read specific constant by ID
				LogicalTimestamp const1Id = new LogicalTimestamp().setReplicaId(100001L).setSequenceNumber(3L);
				Node const1 = reader.readNode(IndexedNodeCodecMapper.CONSTANT, const1Id);
				ConstantNode expectedConst1 = new ConstantNode()
						.setId(const1Id)
						.setValue(new ConValue(ConType.STRING, "From replica 1"));
				assertEquals(expectedConst1, const1);

				LogicalTimestamp const2Id = new LogicalTimestamp().setReplicaId(100002L).setSequenceNumber(5L);
				Node const2 = reader.readNode(IndexedNodeCodecMapper.CONSTANT, const2Id);
				ConstantNode expectedConst2 = new ConstantNode()
						.setId(const2Id)
						.setValue(new ConValue(ConType.STRING, "From replica 2"));
				assertEquals(expectedConst2, const2);
			}
		} finally {
			Files.deleteIfExists(resourceFile);
		}
	}

	@Test
	public void testDecodeJsonJoySnapshotAllTypes() throws IOException {
		Path resourceFile = copyResourceToTempFile("/indexed-model/all-types.cbor");
		try {
			SnapshotFileIndex index = indexBuilder.build(resourceFile);

			// Verify clock table
			ClockTable clockTable = index.getClockTable();
			assertNotNull(clockTable);
			assertEquals(1, clockTable.getClocks().size());
			assertEquals(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(24L), clockTable.getClocks().get(0));

			// Verify root node ID
			assertEquals(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L), index.getRootNodeId());

			// Verify node counts by type
			assertEquals(17, index.getTotalNodeCount());
			assertEquals(2, index.getEntriesForType(IndexedNodeCodecMapper.OBJECT).size()); // Root + nested
			assertEquals(13, index.getEntriesForType(IndexedNodeCodecMapper.CONSTANT).size()); // Various constants
			assertEquals(1, index.getEntriesForType(IndexedNodeCodecMapper.ARRAY).size());
			assertEquals(1, index.getEntriesForType(IndexedNodeCodecMapper.VECTOR).size());

			// Verify we can read all nodes using SeekingNodeReader
			try (SeekingNodeReader reader = new SeekingNodeReader(resourceFile, index)) {
				// Read root object
				Map<LogicalTimestamp, SnapshotFileIndex.NodePointer> objects = index.getEntriesForType(IndexedNodeCodecMapper.OBJECT);
				LogicalTimestamp rootId = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L);
				Node rootNode = reader.readNode(IndexedNodeCodecMapper.OBJECT, rootId);
				assertTrue(rootNode instanceof ObjectNode);
				ObjectNode rootObj = (ObjectNode) rootNode;
				assertEquals(1L, rootObj.getId().getSequenceNumber());
				assertNotNull(rootObj.getValue());
				assertEquals(8, rootObj.getValue().size());
				assertTrue(rootObj.getValue().containsKey("str"));
				assertTrue(rootObj.getValue().containsKey("num"));
				assertTrue(rootObj.getValue().containsKey("bool"));
				assertTrue(rootObj.getValue().containsKey("nil"));
				assertTrue(rootObj.getValue().containsKey("undf"));
				assertTrue(rootObj.getValue().containsKey("arr"));
				assertTrue(rootObj.getValue().containsKey("vec"));
				assertTrue(rootObj.getValue().containsKey("obj"));

				// Read constant nodes and verify various types
				Map<LogicalTimestamp, SnapshotFileIndex.NodePointer> constants = index.getEntriesForType(IndexedNodeCodecMapper.CONSTANT);

				// String constant
				LogicalTimestamp strId = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L);
				assertEquals(new ConstantNode()
						.setId(strId)
						.setValue(new ConValue(ConType.STRING, "Hello, json-joy!")),
						reader.readNode(IndexedNodeCodecMapper.CONSTANT, strId));

				// Long constant
				LogicalTimestamp numId = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(3L);
				assertEquals(new ConstantNode()
						.setId(numId)
						.setValue(new ConValue(ConType.LONG, 42L)),
						reader.readNode(IndexedNodeCodecMapper.CONSTANT, numId));

				// Boolean constant
				LogicalTimestamp boolId = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(4L);
				assertEquals(new ConstantNode()
						.setId(boolId)
						.setValue(new ConValue(ConType.BOOLEAN, true)),
						reader.readNode(IndexedNodeCodecMapper.CONSTANT, boolId));

				// Null constant
				LogicalTimestamp nilId = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(5L);
				assertEquals(new ConstantNode()
						.setId(nilId)
						.setValue(new ConValue(ConType.NULL, JSONObject.NULL)),
						reader.readNode(IndexedNodeCodecMapper.CONSTANT, nilId));

				// Undefined constant
				LogicalTimestamp undfId = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(6L);
				assertEquals(new ConstantNode()
						.setId(undfId)
						.setValue(new ConValue(ConType.UNDEFINED, null)),
						reader.readNode(IndexedNodeCodecMapper.CONSTANT, undfId));

				// Array node
				LogicalTimestamp arrId = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(7L);
				Node arrNode = reader.readNode(IndexedNodeCodecMapper.ARRAY, arrId);
				assertTrue(arrNode instanceof ArrayNode);
				ArrayNode arrayNode = (ArrayNode) arrNode;
				assertEquals(3, arrayNode.getElements().size());

				// Note: The decoder sets referenceNodeId to the array head (containerId) for the first element
				ArrayNode expectedArray = new ArrayNode()
						.setId(arrId)
						.setElements(List.of(
								new RGANode()
										.setNodeId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(11L))
										.setContainerId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(7L))
										.setDataId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(8L))
										.setReferenceNodeId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(7L)),
								new RGANode()
										.setNodeId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(12L))
										.setContainerId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(7L))
										.setDataId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(9L))
										.setReferenceNodeId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(11L)),
								new RGANode()
										.setNodeId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(13L))
										.setContainerId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(7L))
										.setDataId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(10L))
										.setReferenceNodeId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(12L))
						));
				assertEquals(expectedArray, arrayNode);

				// Vector node
				LogicalTimestamp vecId = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(14L);
				Node vecNode = reader.readNode(IndexedNodeCodecMapper.VECTOR, vecId);
				assertTrue(vecNode instanceof VectorNode);
				VectorNode vectorNode = (VectorNode) vecNode;
				assertEquals(14L, vectorNode.getId().getSequenceNumber());
				assertNotNull(vectorNode.getValues());
				assertEquals(3, vectorNode.getValues().size());

				// Nested object
				LogicalTimestamp nestedObjId = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(19L);
				ObjectNode expectedNestedObj = new ObjectNode()
						.setId(nestedObjId)
						.setValue(java.util.Map.of(
								"a", new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(20L),
								"b", new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(21L)
						));
				assertEquals(expectedNestedObj, reader.readNode(IndexedNodeCodecMapper.OBJECT, nestedObjId));
			}
		} finally {
			Files.deleteIfExists(resourceFile);
		}
	}

	/**
	 * Helper to create a snapshot file.
	 */
	private void createSnapshot(Path file, LogicalTimestamp rootId, List<Node> nodes) throws IOException {
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
	}

	/**
	 * Helper to copy a resource file to a temporary file.
	 */
	private Path copyResourceToTempFile(String resourcePath) throws IOException {
		Path temp = Files.createTempFile("resource-test-", ".cbor");
		try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
			if (in == null) {
				throw new IOException("Resource not found: " + resourcePath);
			}
			Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
		}
		return temp;
	}
}
