package org.sagebionetworks.repo.model.grid.encoding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.node.Node;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.dataformat.cbor.CBORParser;

@ExtendWith(MockitoExtension.class)
public class IndexedModelEncoderTest {

	private LogicalTimestamp rootNodeId;

	@Mock
	private OutputStream mockOutputStream;

	@BeforeEach
	public void setUp() throws IOException {
		rootNodeId = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(1L);
	}

	@Test
	public void testEncodeEmptyModel() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		// Call under test - encode model with no nodes
		try (IndexedModelEncoder encoder = new IndexedModelEncoder(out, rootNodeId)) {
			// No nodes added
		}

		byte[] result = out.toByteArray();
		assertNotNull(result);
		assertTrue(result.length > 0, "Encoded model should not be empty");

		// Decode the CBOR and verify that 'c' and 'r' keys are present
		try (CBORParser parser = CBORUtils.getCBORFactory().createParser(out.toByteArray())) {
			JsonToken token = parser.nextToken();
			assertEquals(JsonToken.START_OBJECT, token);
			token = parser.nextToken();
			assertEquals(JsonToken.FIELD_NAME, token);
			assertEquals("c", parser.getCurrentName());
			token = parser.nextToken();
			assertEquals(JsonToken.VALUE_EMBEDDED_OBJECT, token); // Clock table
			assertNotNull(parser.getBinaryValue());
			token = parser.nextToken();
			assertEquals(JsonToken.FIELD_NAME, token);
			assertEquals("r", parser.getCurrentName());
			token = parser.nextToken();
			assertEquals(JsonToken.VALUE_EMBEDDED_OBJECT, token); // Root node ID
			assertNotNull(parser.getBinaryValue());
			token = parser.nextToken();
			assertEquals(JsonToken.END_OBJECT, token);
		}
	}

	@Test
	public void testEncodeModel() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		List<Node> nodes = new ArrayList<>();
		nodes.add(new ConstantNode()
				.setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(2L))
				.setValue(new ConValue(ConType.LONG, 42L)));
		nodes.add(new ConstantNode()
				.setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(3L))
				.setValue(new ConValue(ConType.STRING, "hello")));
		nodes.add(new ConstantNode()
				.setId(new LogicalTimestamp().setReplicaId(200L).setSequenceNumber(5L))
				.setValue(new ConValue(ConType.BOOLEAN, true)));

		// Call under test - encode model with nodes
		try (IndexedModelEncoder encoder = new IndexedModelEncoder(out, rootNodeId)) {
			for (Node node : nodes) {
				encoder.writeNode(node);
			}
		}

		byte[] result = out.toByteArray();
		assertNotNull(result);
		assertTrue(result.length > 0);

		// Decode the CBOR
		try (CBORParser parser = CBORUtils.getCBORFactory().createParser(out.toByteArray())) {
			JsonToken token = parser.nextToken();
			assertEquals(JsonToken.START_OBJECT, token);

			// Verify each node is in the document
			for (Node node : nodes) {
				token = parser.nextToken();
				assertEquals(JsonToken.FIELD_NAME, token);
				// Node key
				assertNotNull(parser.getCurrentName());
				// Node value
				token = parser.nextToken();
				assertEquals(JsonToken.VALUE_EMBEDDED_OBJECT, token);
				assertNotNull(parser.getBinaryValue());
			}
			// Verify 'c' and 'r' keys are present
			token = parser.nextToken();
			assertEquals(JsonToken.FIELD_NAME, token);
			assertEquals("c", parser.getCurrentName());
			token = parser.nextToken();
			assertEquals(JsonToken.VALUE_EMBEDDED_OBJECT, token); // Clock table
			assertNotNull(parser.getBinaryValue());
			token = parser.nextToken();
			assertEquals(JsonToken.FIELD_NAME, token);
			assertEquals("r", parser.getCurrentName());
			token = parser.nextToken();
			assertEquals(JsonToken.VALUE_EMBEDDED_OBJECT, token); // Root node ID
			assertNotNull(parser.getBinaryValue());
			token = parser.nextToken();
			assertEquals(JsonToken.END_OBJECT, token);
		}

		// IndexedModelEncodingRoundTripTest verifies that the model can be decoded.
	}

	@Test
	public void testEncodeNullNode() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		try (IndexedModelEncoder encoder = new IndexedModelEncoder(out, rootNodeId)) {
			assertThrows(IllegalArgumentException.class, () -> {
				encoder.writeNode(null);
			});
		}
	}

	@Test
	public void testEncodeNodeWithNullId() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		ConstantNode nodeWithoutId = new ConstantNode()
			.setValue(new ConValue(ConType.LONG, 42L));

		try (IndexedModelEncoder encoder = new IndexedModelEncoder(out, rootNodeId)) {
			assertThrows(IllegalArgumentException.class, () -> {
				encoder.writeNode(nodeWithoutId);
			});
		}
	}

	@Test
	public void testWriteAfterClose() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		IndexedModelEncoder encoder = new IndexedModelEncoder(out, rootNodeId);
		encoder.close();

		ConstantNode node = new ConstantNode()
			.setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(2L))
			.setValue(new ConValue(ConType.LONG, 42L));

		assertThrows(IllegalStateException.class, () -> {
			encoder.writeNode(node);
		});
	}


	@Test
	public void testOutputStreamIsClosedOnClose() throws IOException {
		IndexedModelEncoder encoder = new IndexedModelEncoder(mockOutputStream, rootNodeId);

		// Call under test - close should close the underlying output stream
		encoder.close();
		verify(mockOutputStream).close();
	}
}
