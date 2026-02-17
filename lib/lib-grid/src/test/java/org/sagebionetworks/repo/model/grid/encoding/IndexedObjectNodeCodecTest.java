package org.sagebionetworks.repo.model.grid.encoding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.node.ObjectNode;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class IndexedObjectNodeCodecTest {

	private IndexedObjectNodeCodec codec;
	private ClockTable clockTable;

	@BeforeEach
	public void setUp() {
		codec = new IndexedObjectNodeCodec();
		// Create a clock table with a single session
		clockTable = new ClockTable(List.of(
			new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(50L)
		));
	}

	@Test
	public void testEncodeObjectNode_Empty() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectNode node = new ObjectNode()
			.setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(1L))
			.setValue(new LinkedHashMap<>());

		// Call under test - encode
		codec.encode(node, clockTable, out);

		byte[] result = out.toByteArray();

		InputStream in = new ByteArrayInputStream(result);

		// call under test - decode header
		IndexedNodeHeader nodeHeader = IndexedEncodingUtils.readNodeHeader(in);
		assertEquals(IndexedNodeCodecMapper.OBJECT.code, nodeHeader.getNodeType());
		assertEquals(0L, nodeHeader.getLength());

		// call under test - decode node contents
		ObjectNode decoded = codec.decode(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(1L), nodeHeader.getLength(), clockTable, in);
		assertEquals(node, decoded);
	}

	@Test
	public void testEncodeObjectNodeOneItem() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		Map<String, LogicalTimestamp> map = new LinkedHashMap<>();
		map.put("name", new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(11L));

		ObjectNode node = new ObjectNode()
			.setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(1L))
			.setValue(map);

		codec.encode(node, clockTable, out);

		byte[] result = out.toByteArray();

		InputStream in = new ByteArrayInputStream(result);

		// call under test - decode header
		IndexedNodeHeader nodeHeader = IndexedEncodingUtils.readNodeHeader(in);
		assertEquals(IndexedNodeCodecMapper.OBJECT.code, nodeHeader.getNodeType());
		assertEquals(1L, nodeHeader.getLength());

		// call under test - decode node contents
		ObjectNode decoded = codec.decode(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(1L), nodeHeader.getLength(), clockTable, in);
		assertEquals(node, decoded);
	}

	@Test
	public void testEncodeObjectNodeMultipleItems() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		Map<String, LogicalTimestamp> map = new LinkedHashMap<>();
		map.put("a", new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(45L));
		map.put("b", new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(46L));
		map.put("c", new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(47L));

		ObjectNode node = new ObjectNode()
			.setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(1L))
			.setValue(map);

		codec.encode(node, clockTable, out);

		byte[] result = out.toByteArray();

		InputStream in = new ByteArrayInputStream(result);

		// call under test - decode header
		IndexedNodeHeader nodeHeader = IndexedEncodingUtils.readNodeHeader(in);
		assertEquals(IndexedNodeCodecMapper.OBJECT.code, nodeHeader.getNodeType());
		assertEquals(3L, nodeHeader.getLength());

		// call under test - decode node contents
		ObjectNode decoded = codec.decode(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(1L), nodeHeader.getLength(), clockTable, in);
		assertEquals(node, decoded);
	}

	@Test
	public void testEncodeObjectNodeNullValue() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectNode node = new ObjectNode()
			.setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(1L))
			.setValue(null);

		assertThrows(IllegalArgumentException.class, () -> {
			codec.encode(node, clockTable, out);
		});
	}

	@Test
	public void testEncodeObjectNodeNullNode() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		assertThrows(IllegalArgumentException.class, () -> {
			codec.encode(null, clockTable, out);
		});
	}

	@Test
	public void testEncodeObjectNodeNullId() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectNode node = new ObjectNode()
			.setValue(new LinkedHashMap<>());

		assertThrows(IllegalArgumentException.class, () -> {
			codec.encode(node, clockTable, out);
		});
	}
}
