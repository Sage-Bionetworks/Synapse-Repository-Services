package org.sagebionetworks.repo.model.grid.encoding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.node.Node;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

/**
 * Interface for encoding and decoding a specific node type in the Indexed format.
 *
 * @param <T> the type of node this encoder handles
 * @see <a href="https://jsonjoy.com/specs/json-crdt/encoding/indexed-encoding">Indexed Encoding</a>
 */
public interface IndexedNodeTypeCodec<T extends Node> {

	/**
	 * Encode the given node to the output stream.
	 *
	 * @param node the node to encode
	 * @param clockTable the clock table, used for timestamp encoding
	 * @param out the output stream to write to
	 * @return the number of bytes written
	 * @throws IOException if an I/O error occurs
	 */
	int encode(T node, ClockTable clockTable, OutputStream out) throws IOException;

	/**
	 * Decode a node from the input stream, reading and validating the type header.
	 *
	 * @param nodeId the ID to assign to the decoded node
	 * @param length the length (or other numeric value encoded in the node header) of the node
	 * @param clockTable the clock table, used for timestamp decoding
	 * @param in the input stream to read from
	 * @return the decoded node
	 * @throws IOException if an I/O error occurs
	 * @throws IllegalArgumentException if the node type in the stream doesn't match the expected type
	 */
	T decode(LogicalTimestamp nodeId, long length, ClockTable clockTable, InputStream in) throws IOException;
}
