package org.sagebionetworks.repo.model.grid.encoding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Encoder for ConstantNode in the Indexed format.
 *
 * The con node encoding:
 * <pre>
 * Type (000) and length byte
 * Value (CBOR or timestamp)
 *
 * Type (000)
 * |
 * |   Length (0)
 * |   |
 * |   |     Value
 * |   |     |
 * +---|----+========+
 * |00000000|  CBOR  |
 * +--------+========+
 * When the node holds a logical timestamp:
 *
 *  Type (000)
 *  |
 *  |  Length (1)
 *  |  |
 *  |  |     Timestamp
 *  |  |     |
 * +---|----+========+
 * |00000001|   id   |   (with ID)
 * +-------^+========+
 * </pre>
 *
 * @see <a href="https://jsonjoy.com/specs/json-crdt/encoding/indexed-encoding">Indexed Encoding</a>
 */
public class IndexedConstantNodeCodec implements IndexedNodeTypeCodec<ConstantNode> {

	@Override
	public int encode(ConstantNode node, ClockTable clockTable, OutputStream out) throws IOException {
		ValidateArgument.required(node, "node");
		ValidateArgument.required(node.getId(), "node.id");
		ValidateArgument.required(out, "out");

		int bytesWritten = 0;

		long length = 0L;
		if (ConType.TIMESTAMP.equals(node.getConValue().getType())) {
			length = 1L;
		}
		bytesWritten += IndexedEncodingUtils.writeNodeTypeAndLength(IndexedEncodingUtils.NODE_TYPE_CONSTANT, length, out);

		// Write the value
		byte[] valueBytes = CBORUtils.encodeConValue(node.getConValue(), clockTable);
		out.write(valueBytes);
		bytesWritten += valueBytes.length;

		return bytesWritten;
	}

	@Override
	public ConstantNode decode(LogicalTimestamp nodeId, long length, ClockTable clockTable, InputStream in) throws IOException {
		ValidateArgument.required(nodeId, "nodeId");
		ValidateArgument.requirement(length == 0 || length == 1, "length must be 0 (CBOR) or 1 (timestamp).");
		ValidateArgument.required(clockTable, "clockTable");
		ValidateArgument.required(in, "in");

		boolean isTimestamp = length == 1;
		ConValue value = CBORUtils.decodeConValue(in, clockTable, isTimestamp);

		return new ConstantNode()
			.setId(nodeId)
			.setValue(value);
	}
}
