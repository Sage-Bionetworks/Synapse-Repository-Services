package org.sagebionetworks.repo.model.grid.encoding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.node.VectorNode;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Encoder for VectorNode in the Indexed format.
 *
 * Vector node encoding:
 * <pre>
 * - Node type: 011 (3)
 * - Length: the total number of slots (maxIndex + 1), not the number of entries
 * - Contains: slot entries from index 0 to maxIndex, each either:
 *   - 0x00 for absent slots
 *   - 0x01 followed by the node ID for present slots
 * </pre>
 *
 * @see <a href="https://jsonjoy.com/specs/json-crdt/encoding/indexed-encoding">Indexed Encoding</a>
 */
public class IndexedVectorNodeCodec implements IndexedNodeTypeCodec<VectorNode> {

	@Override
	public int encode(VectorNode node, ClockTable clockTable, OutputStream out) throws IOException {
		ValidateArgument.required(node, "node");
		ValidateArgument.required(node.getId(), "node.id");
		ValidateArgument.required(out, "out");

		int bytesWritten = 0;

		// Get the maximum index to determine the length of the vector
		// The length in the header must match the number of slots we write (maxIndex + 1)
		int maxIndex = node.getValues().keySet().stream().max(Integer::compareTo).orElse(-1);
		long length = maxIndex + 1;
		bytesWritten += IndexedEncodingUtils.writeNodeTypeAndLength(IndexedEncodingUtils.NODE_TYPE_VECTOR, length, out);

		for (int i = 0; i <= maxIndex; i++) {
			if (!node.getValues().containsKey(i)) {
				// If the index is missing, add a 0-byte
				byte[] zeroByte = new byte[] { 0x00 };
				out.write(zeroByte);
				bytesWritten += zeroByte.length;
			} else {
				out.write(0x01); // Presence byte
				bytesWritten += 1;
				// Write the ID of the node - use raw timestamp encoding for indexed format
				byte[] nodeId = clockTable.encodeTimestamp(node.getValues().get(i).getId());
				out.write(nodeId);
				bytesWritten += nodeId.length;
			}
		}
		return bytesWritten;
	}

	@Override
	public VectorNode decode(LogicalTimestamp nodeId, long length, ClockTable clockTable, InputStream in) throws IOException {
		ValidateArgument.required(nodeId, "nodeId");
		ValidateArgument.required(in, "in");

		Map<Integer, ConstantNode> values = new LinkedHashMap<>();

		// The encoder writes either a 0x00 byte for missing entries or 0x01 followed by the full timestamp for present entries.
		for (int index = 0; index < length; index++) {
			int presenceByte = in.read();
			if (presenceByte == -1) {
				throw new IOException("Unexpected end of stream while reading VectorNode entries");
			}

			if (presenceByte != 0x00) {
				// Non-zero byte means present - read the timestamp
				LogicalTimestamp entryId = clockTable.decodeTimestamp(in);

				// Create a ConstantNode for this entry
				// NOTE: Our vector node implementation only supports ConstantNode entries, but the spec allows any node type.
				// The actual value of the ConstantNode is stored elsewhere in the indexed format; here we only have the ID reference.
				ConstantNode constantNode = new ConstantNode().setId(entryId);
				values.put(index, constantNode);
			}
		}

		return new VectorNode()
			.setId(nodeId)
			.setValues(values);
	}
}
