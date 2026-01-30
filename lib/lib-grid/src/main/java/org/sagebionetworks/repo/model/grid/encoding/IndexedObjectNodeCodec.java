package org.sagebionetworks.repo.model.grid.encoding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.node.ObjectNode;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.ValidateArgument;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;

/**
 * Encoder for ObjectNode in the Indexed format.
 *
 * Object node encoding:
 * <pre>
 * - Node type: 010 (2)
 * - Length: number of key-value pairs
 * - Contains: map of string keys (CBOR encoded) to LogicalTimestamp values
 *
 * Key-value pairs are encoded as a key (CBOR), followed by the ID of the nested node.
 * </pre>
 *
 * @see <a href="https://jsonjoy.com/specs/json-crdt/encoding/indexed-encoding">Indexed Encoding</a>
 */
public class IndexedObjectNodeCodec implements IndexedNodeTypeCodec<ObjectNode> {

	@Override
	public int encode(ObjectNode node, ClockTable clockTable, OutputStream out) throws IOException {
		ValidateArgument.required(node, "node");
		ValidateArgument.required(node.getId(), "node.id");
		ValidateArgument.required(node.getValue(), "node.value");
		ValidateArgument.required(out, "out");

		int bytesWritten = 0;

		long length = node.getValue().size();

		bytesWritten += IndexedEncodingUtils.writeNodeTypeAndLength(IndexedEncodingUtils.NODE_TYPE_OBJECT, length, out);

		for (Map.Entry<String, LogicalTimestamp> entry : node.getValue().entrySet()) {
			// Key-value pairs are encoded as a key (CBOR), followed by the ID of the nested node.
			// Use CBOR for other types
			try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				try (CBORGenerator generator = CBORUtils.getCBORFactory().createGenerator(baos)) {
					generator.writeString(entry.getKey());
				}
				byte[] encodedKey = baos.toByteArray();
				out.write(encodedKey);
				bytesWritten += encodedKey.length;
			} catch (IOException e) {
				throw new RuntimeException("Failed to encode ObjectNode key to CBOR", e);
			}
			byte[] encodedValue = clockTable.encodeTimestamp(entry.getValue());
			out.write(encodedValue);
			bytesWritten += encodedValue.length;
		}
		return bytesWritten;
	}

	@Override
	public ObjectNode decode(LogicalTimestamp nodeId, long length, ClockTable clockTable, InputStream in) throws IOException {
		ValidateArgument.required(nodeId, "nodeId");
		ValidateArgument.required(length, "length");
		ValidateArgument.required(clockTable, "clockTable");
		ValidateArgument.required(in, "in");

		Map<String, LogicalTimestamp> map = new LinkedHashMap<>();

		for (long i = 0; i < length; i++) {
			JsonNode node = CBORUtils.parseJsonNode(in);
			if (!node.isTextual()) {
				throw new IOException("Expected CBOR string for ObjectNode key, but found: " + node.getNodeType());
			}
			String key = node.asText();

			LogicalTimestamp value = clockTable.decodeTimestamp(in);
			map.put(key, value);
		}


		return new ObjectNode()
			.setId(nodeId)
			.setValue(map);
	}

}
