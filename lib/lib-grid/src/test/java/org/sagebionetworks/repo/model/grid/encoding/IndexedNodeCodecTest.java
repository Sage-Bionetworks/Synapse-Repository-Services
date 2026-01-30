package org.sagebionetworks.repo.model.grid.encoding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.node.ArrayNode;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.node.Node;
import org.sagebionetworks.repo.model.grid.node.ObjectNode;
import org.sagebionetworks.repo.model.grid.node.VectorNode;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class IndexedNodeCodecTest {

	private IndexedNodeCodec codec;
	private ClockTable clockTable;
	private LogicalTimestamp nodeId;

	@BeforeEach
	public void setUp() {
		codec = new IndexedNodeCodec();
		clockTable = new ClockTable(List.of(
			new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(50L)
		));
		nodeId = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(10L);
	}

	@Test
	public void testEncodeNullNode() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		assertThrows(IllegalArgumentException.class, () -> {
			codec.encode(null, clockTable, out);
		});
	}

	@Test
	public void testEncodeNullClockTable() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ConstantNode node = new ConstantNode()
			.setId(nodeId)
			.setValue(new ConValue(ConType.LONG, 42L));

		assertThrows(IllegalArgumentException.class, () -> {
			codec.encode(node, null, out);
		});
	}

	@Test
	public void testEncodeNullOutputStream() {
		ConstantNode node = new ConstantNode()
			.setId(nodeId)
			.setValue(new ConValue(ConType.LONG, 42L));

		assertThrows(IllegalArgumentException.class, () -> {
			codec.encode(node, clockTable, null);
		});
	}

	@Test
	public void testEncodeConstantNode() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ConstantNode node = new ConstantNode()
			.setId(nodeId)
			.setValue(new ConValue(ConType.LONG, 42L));

		// call under test - encode
		int bytesWritten = codec.encode(node, clockTable, out);

		assertTrue(bytesWritten > 0, "Should write at least one byte");
		assertEquals(bytesWritten, out.size(), "Bytes written should match output size");

		// call under test - decode
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		Node decodedNode = codec.decode(nodeId, clockTable, in);
		assertEquals(node, decodedNode);
	}

	@Test
	public void testEncodeArrayNode() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ArrayNode node = new ArrayNode()
			.setId(nodeId)
			.setElements(new ArrayList<>());

		// call under test - encode
		int bytesWritten = codec.encode(node, clockTable, out);

		assertTrue(bytesWritten > 0, "Should write at least one byte");
		assertEquals(bytesWritten, out.size(), "Bytes written should match output size");

		// call under test - decode
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		Node decodedNode = codec.decode(nodeId, clockTable, in);
		assertEquals(node, decodedNode);
	}

	@Test
	public void testEncodeObjectNode() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectNode node = new ObjectNode()
			.setId(nodeId)
			.setValue(new LinkedHashMap<>());

		// call under test - encode
		int bytesWritten = codec.encode(node, clockTable, out);

		assertTrue(bytesWritten > 0, "Should write at least one byte");
		assertEquals(bytesWritten, out.size(), "Bytes written should match output size");

		// call under test - decode
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		Node decodedNode = codec.decode(nodeId, clockTable, in);
		assertEquals(node, decodedNode);
	}

	@Test
	public void testEncodeVectorNode() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		VectorNode node = new VectorNode()
			.setId(nodeId)
			.setValues(new LinkedHashMap<>());

		// call under test - encode
		int bytesWritten = codec.encode(node, clockTable, out);

		assertTrue(bytesWritten > 0, "Should write at least one byte");
		assertEquals(bytesWritten, out.size(), "Bytes written should match output size");

		// call under test - decode
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		Node decodedNode = codec.decode(nodeId, clockTable, in);
		assertEquals(node, decodedNode);
	}

	@Test
	public void testDecodeNullNodeId() {
		ByteArrayInputStream in = new ByteArrayInputStream(new byte[]{0x00});
		assertThrows(IllegalArgumentException.class, () -> {
			codec.decode(null, clockTable, in);
		});
	}

	@Test
	public void testDecodeNullClockTable() {
		ByteArrayInputStream in = new ByteArrayInputStream(new byte[]{0x00});
		assertThrows(IllegalArgumentException.class, () -> {
			codec.decode(nodeId, null, in);
		});
	}

	@Test
	public void testDecodeNullInputStream() {
		assertThrows(IllegalArgumentException.class, () -> {
			codec.decode(nodeId, clockTable, null);
		});
	}
}
