package org.sagebionetworks.repo.model.grid.encoding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.node.VectorNode;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class IndexedVectorNodeCodecTest {

	private IndexedVectorNodeCodec codec;
	private ClockTable clockTable;

	@BeforeEach
	public void setUp() {
		codec = new IndexedVectorNodeCodec();
		// Create a clock table with a single session
		clockTable = new ClockTable(List.of(
				new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(50L)
		));
	}

	public enum TestVectorNodeCase {
		EMPTY(new VectorNode()
				.setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(50L))
				.setValues(new LinkedHashMap<>()), null),
		SINGLE_ELEMENT(new VectorNode()
				.setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(10L))
				.setValues(new LinkedHashMap<>() {{
					put(0, new ConstantNode()
						.setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(5L)));
				}}), null),
		SPARSE_INDICES(new VectorNode()
				.setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(10L))
				.setValues(new LinkedHashMap<>() {{
					put(0, new ConstantNode()
						.setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(5L)));
					put(2, new ConstantNode()
						.setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(4L)));
				}}), null);

		final VectorNode node;
		final ClockTable clockTable;

		TestVectorNodeCase(VectorNode node, ClockTable clockTable) {
			this.node = node;
			this.clockTable = clockTable;
		}
	}


	@ParameterizedTest
	@EnumSource(TestVectorNodeCase.class)
	public void testEncodeVectorNode(TestVectorNodeCase testCase) throws IOException {
		VectorNode node = testCase.node;
		ClockTable clockTable = testCase.clockTable != null ? testCase.clockTable : this.clockTable;

		ByteArrayOutputStream out = new ByteArrayOutputStream();

		// encode
		codec.encode(node, clockTable, out);

		byte[] bytes = out.toByteArray();
		InputStream byteStream = new ByteArrayInputStream(bytes);

		// decode header
		IndexedNodeHeader nodeHeader = IndexedEncodingUtils.readNodeTypeAndLength(byteStream);
		assertEquals(IndexedEncodingUtils.NODE_TYPE_VECTOR, nodeHeader.getNodeType());
		assertTrue(nodeHeader.getLength() >= 0L);

		// decode body
		VectorNode decoded = codec.decode(node.getId(), nodeHeader.getLength(), clockTable, byteStream);

		assertEquals(node, decoded);
	}

	@Test
	public void testEncodeVectorNodeNullNode() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		assertThrows(IllegalArgumentException.class, () -> codec.encode(null, clockTable, out));
	}

	@Test
	public void testEncodeVectorNodeNullId() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		VectorNode node = new VectorNode()
			.setValues(new LinkedHashMap<>());

		assertThrows(IllegalArgumentException.class, () -> codec.encode(node, clockTable, out));
	}
}
