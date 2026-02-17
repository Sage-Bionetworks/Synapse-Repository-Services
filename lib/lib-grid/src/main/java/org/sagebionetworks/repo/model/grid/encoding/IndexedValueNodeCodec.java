package org.sagebionetworks.repo.model.grid.encoding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.node.ValueNode;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Encoder for ValueNode in the Indexed format.
 *
 * The con node encoding:
 * <pre>
 * Type (001) and length (always 0)
 * Value (id of the nested node)
 *
 *   Type (001)
 *  |
 *  |  Length (0)
 *  |  |
 *  |  |     Value
 *  |  |     |
 * +---|----+========+
 * |00100000|   id   |
 * +--------+========+
 * </pre>
 *
 * @see <a href="https://jsonjoy.com/specs/json-crdt/encoding/indexed-encoding">Indexed Encoding</a>
 */
public class IndexedValueNodeCodec implements IndexedNodeTypeCodec<ValueNode> {
	static final long LENGTH = 0L;


	@Override
	public int encode(ValueNode node, ClockTable clockTable, OutputStream out) throws IOException {
		ValidateArgument.required(node, "node");
		ValidateArgument.required(node.getId(), "node.id");
		ValidateArgument.required(node.getValue(), "node.value");
		ValidateArgument.required(clockTable, "clockTable");
		ValidateArgument.required(out, "out");

		int bytesWritten = 0;

		bytesWritten += IndexedEncodingUtils.writeNodeHeader(IndexedNodeCodecMapper.VAL.code, LENGTH, out);

		// Write the value
		byte[] valueBytes = clockTable.encodeTimestamp(node.getValue());
		out.write(valueBytes);
		bytesWritten += valueBytes.length;

		return bytesWritten;
	}

	@Override
	public ValueNode decode(LogicalTimestamp nodeId, long length, ClockTable clockTable, InputStream in) throws IOException {
		ValidateArgument.required(nodeId, "nodeId");
		ValidateArgument.requirement(length == 0, "length must be 0");
		ValidateArgument.required(clockTable, "clockTable");
		ValidateArgument.required(in, "in");

		LogicalTimestamp value = clockTable.decodeTimestamp(in);

		return new ValueNode()
			.setId(nodeId)
			.setValue(value);
	}
}
