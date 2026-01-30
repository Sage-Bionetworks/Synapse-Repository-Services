package org.sagebionetworks.repo.model.grid.encoding;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.node.ArrayNode;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.node.Node;
import org.sagebionetworks.repo.model.grid.node.ObjectNode;
import org.sagebionetworks.repo.model.grid.node.VectorNode;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class IndexedNodeTypeCodecProviderTest {

	private IndexedNodeEncoderProvider provider;

	@BeforeEach
	public void setUp() {
		provider = new IndexedNodeEncoderProvider();
	}

	@Test
	public void testGetConstantNodeEncoder() {
		IndexedNodeTypeCodec<ConstantNode> encoder = provider.getConstantNodeEncoder();

		assertNotNull(encoder);
		assertTrue(encoder instanceof IndexedConstantNodeCodec);
	}

	@Test
	public void testGetArrayNodeEncoder() {
		IndexedNodeTypeCodec<ArrayNode> encoder = provider.getArrayNodeEncoder();

		assertNotNull(encoder);
		assertTrue(encoder instanceof IndexedArrayNodeCodec);
	}

	@Test
	public void testGetObjectNodeEncoder() {
		IndexedNodeTypeCodec<ObjectNode> encoder = provider.getObjectNodeEncoder();

		assertNotNull(encoder);
		assertTrue(encoder instanceof IndexedObjectNodeCodec);
	}

	@Test
	public void testGetVectorNodeEncoder() {
		IndexedNodeTypeCodec<VectorNode> encoder = provider.getVectorNodeEncoder();

		assertNotNull(encoder);
		assertTrue(encoder instanceof IndexedVectorNodeCodec);
	}

	@Test
	public void testGetEncoderConstantNode() {
		IndexedNodeTypeCodec<ConstantNode> encoder = provider.getEncoder(ConstantNode.class);

		assertNotNull(encoder);
		assertSame(provider.getConstantNodeEncoder(), encoder);
	}

	@Test
	public void testGetEncoderArrayNode() {
		IndexedNodeTypeCodec<ArrayNode> encoder = provider.getEncoder(ArrayNode.class);

		assertNotNull(encoder);
		assertSame(provider.getArrayNodeEncoder(), encoder);
	}

	@Test
	public void testGetEncoderObjectNode() {
		IndexedNodeTypeCodec<ObjectNode> encoder = provider.getEncoder(ObjectNode.class);

		assertNotNull(encoder);
		assertSame(provider.getObjectNodeEncoder(), encoder);
	}

	@Test
	public void testGetEncoderVectorNode() {
		IndexedNodeTypeCodec<VectorNode> encoder = provider.getEncoder(VectorNode.class);

		assertNotNull(encoder);
		assertSame(provider.getVectorNodeEncoder(), encoder);
	}

	@Test
	public void testGetEncoderUnsupportedNodeType() {
		Class<? extends Node> unsupportedNodeClass = new Node() {
			@Override
			public LogicalTimestamp getId() {
				return null;
			}
		}.getClass();

		assertThrows(IllegalArgumentException.class, () -> {
			provider.getEncoder(unsupportedNodeClass);
		});
	}

	@Test
	public void testEncodersAreSingletons() {
		// Verify that the same encoder instances are returned on subsequent calls
		assertSame(provider.getConstantNodeEncoder(), provider.getConstantNodeEncoder());
		assertSame(provider.getArrayNodeEncoder(), provider.getArrayNodeEncoder());
		assertSame(provider.getObjectNodeEncoder(), provider.getObjectNodeEncoder());
		assertSame(provider.getVectorNodeEncoder(), provider.getVectorNodeEncoder());
	}
}
