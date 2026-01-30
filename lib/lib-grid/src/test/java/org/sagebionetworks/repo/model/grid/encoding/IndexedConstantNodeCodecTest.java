package org.sagebionetworks.repo.model.grid.encoding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class IndexedConstantNodeCodecTest {

	private IndexedConstantNodeCodec codec;
	private ClockTable clockTable;

	@BeforeEach
	public void setUp() {
		codec = new IndexedConstantNodeCodec();
		// Create a clock table with a single session
		clockTable = new ClockTable(List.of(
			new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(50L)
		));
	}

	@Test
	public void testEncodeConstantNode() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ConstantNode node = new ConstantNode()
			.setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(50L))
			.setValue(new ConValue(ConType.LONG, 42L));

		codec.encode(node, clockTable, out);

		byte[] result = out.toByteArray();

		// Verify that the same value can be decoded back
		InputStream bytes = new PushbackInputStream(new ByteArrayInputStream(result));
		// Read the type and header
		IndexedNodeHeader nodeHeader = IndexedEncodingUtils.readNodeTypeAndLength(bytes);

		assertEquals(IndexedEncodingUtils.NODE_TYPE_CONSTANT, nodeHeader.getNodeType());
		assertEquals(0L, nodeHeader.getLength());

		ConstantNode decoded = codec.decode(node.getId(), nodeHeader.getLength(), clockTable, bytes);
		assertEquals(node, decoded);
	}

	@Test
	public void testEncodeConstantNodeTimestamp() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ConstantNode node = new ConstantNode()
				.setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(50L))
				.setValue(new ConValue(ConType.TIMESTAMP, new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(15L)));

		codec.encode(node, clockTable, out);

		byte[] result = out.toByteArray();

		// Verify that the same value can be decoded back
		InputStream bytes = new ByteArrayInputStream(result);
		// Read the type and header
		IndexedNodeHeader nodeHeader = IndexedEncodingUtils.readNodeTypeAndLength(bytes);

		assertEquals(IndexedEncodingUtils.NODE_TYPE_CONSTANT, nodeHeader.getNodeType());
		assertEquals(1L, nodeHeader.getLength()); // For timestamps, the length should be 1

		ConstantNode decoded = codec.decode(node.getId(), nodeHeader.getLength(), clockTable, bytes);
	}

	@Test
	public void testEncodeConstantNodeNullNode() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		assertThrows(IllegalArgumentException.class, () -> {
			codec.encode(null, clockTable, out);
		});
	}

	@Test
	public void testEncodeConstantNodeNullId() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ConstantNode node = new ConstantNode()
			.setValue(new ConValue(ConType.LONG, 42L));

		assertThrows(IllegalArgumentException.class, () -> {
			codec.encode(node, clockTable, out);
		});
	}


	@Test
	public void testDecodeConstantNodeInvalidLength() {
		assertThrows(IllegalArgumentException.class, () -> {
			codec.decode(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(50L), 2L, clockTable, new ByteArrayInputStream(new byte[0]));
		});
	}
}
