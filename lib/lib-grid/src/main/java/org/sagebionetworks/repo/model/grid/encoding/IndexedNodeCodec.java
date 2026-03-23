package org.sagebionetworks.repo.model.grid.encoding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.node.Node;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Encodes and decodes JSON CRDT nodes in the Indexed binary format.
 *
 * @see <a href="https://jsonjoy.com/specs/json-crdt/encoding/indexed-encoding">Indexed Encoding</a>
 */
public class IndexedNodeCodec implements NodeCodec {

	public IndexedNodeCodec() {
	}

	@Override
	public <T extends Node> int encode(T node, ClockTable clockTable, OutputStream out) throws IOException {
		ValidateArgument.required(node, "node");
		ValidateArgument.required(clockTable, "clockTable");
		ValidateArgument.required(out, "out");

		IndexedNodeCodecMapper mapper = IndexedNodeCodecMapper.getByNodeClass(node.getClass());
		IndexedNodeTypeCodec<T> codec = (IndexedNodeTypeCodec<T>) mapper.codec;

		return codec.encode(node, clockTable, out);
	}

	@Override
	public Node decode(LogicalTimestamp nodeId, ClockTable clockTable, InputStream in) throws IOException {
		ValidateArgument.required(nodeId, "nodeId");
		ValidateArgument.required(clockTable, "clockTable");
		ValidateArgument.required(in, "in");

		IndexedNodeHeader nodeHeader = IndexedEncodingUtils.readNodeHeader(in);
		IndexedNodeCodecMapper mapper = IndexedNodeCodecMapper.getByCode(nodeHeader.getNodeType());

		return mapper.codec.decode(nodeId, nodeHeader.getLength(), clockTable, in);
	}
}
