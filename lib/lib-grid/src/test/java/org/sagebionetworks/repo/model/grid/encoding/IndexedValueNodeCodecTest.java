package org.sagebionetworks.repo.model.grid.encoding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.node.ValueNode;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class IndexedValueNodeCodecTest {

	private IndexedValueNodeCodec codec;
	private ClockTable clockTable;

	@BeforeEach
	public void setUp() {
		codec = new IndexedValueNodeCodec();
		// Create a clock table with a single session
		clockTable = new ClockTable(List.of(
			new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(50L)
		));
	}

	@Test
	public void testEncodeValueNode() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		LogicalTimestamp value = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(25L);
		ValueNode node = new ValueNode()
			.setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(50L))
			.setValue(value);

		codec.encode(node, clockTable, out);

		byte[] result = out.toByteArray();

		// Verify that the same value can be decoded back
		InputStream bytes = new ByteArrayInputStream(result);
		// Read the type and header
		IndexedNodeHeader nodeHeader = IndexedEncodingUtils.readNodeHeader(bytes);

		assertEquals(IndexedNodeCodecMapper.VAL.code, nodeHeader.getNodeType());
		assertEquals(0L, nodeHeader.getLength());

		ValueNode decoded = codec.decode(node.getId(), nodeHeader.getLength(), clockTable, bytes);
		assertEquals(node, decoded);
		assertEquals(value, decoded.getValue());
	}

	@Test
	public void testEncodeValueNodeNullNode() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		assertThrows(IllegalArgumentException.class, () -> {
			codec.encode(null, clockTable, out);
		});
	}

	@Test
	public void testEncodeValueNodeNullId() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ValueNode node = new ValueNode()
			.setValue(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(25L));

		assertThrows(IllegalArgumentException.class, () -> {
			codec.encode(node, clockTable, out);
		});
	}

	@Test
	public void testEncodeValueNodeNullValue() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ValueNode node = new ValueNode()
			.setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(50L));

		assertThrows(IllegalArgumentException.class, () -> {
			codec.encode(node, clockTable, out);
		});
	}

	@Test
	public void testEncodeValueNodeNullOutputStream() {
		ValueNode node = new ValueNode()
			.setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(50L))
			.setValue(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(25L));

		assertThrows(IllegalArgumentException.class, () -> {
			codec.encode(node, clockTable, null);
		});
	}

	@Test
	public void testEncodeValueNodeNullClockTable() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ValueNode node = new ValueNode()
			.setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(50L))
			.setValue(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(25L));

		assertThrows(IllegalArgumentException.class, () -> {
			codec.encode(node, null, out);
		});
	}

	@Test
	public void testDecodeValueNodeInvalidLength() {
		assertThrows(IllegalArgumentException.class, () -> {
			codec.decode(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(50L),
				1L, clockTable, new ByteArrayInputStream(new byte[0]));
		});
	}

	@Test
	public void testDecodeValueNodeNullNodeId() {
		ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);

		assertThrows(IllegalArgumentException.class, () -> {
			codec.decode(null, 0L, clockTable, in);
		});
	}

	@Test
	public void testDecodeValueNodeNullClockTable() {
		ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);

		assertThrows(IllegalArgumentException.class, () -> {
			codec.decode(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(50L),
				0L, null, in);
		});
	}

	@Test
	public void testDecodeValueNodeNullInputStream() {
		assertThrows(IllegalArgumentException.class, () -> {
			codec.decode(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(50L),
				0L, clockTable, null);
		});
	}
}
