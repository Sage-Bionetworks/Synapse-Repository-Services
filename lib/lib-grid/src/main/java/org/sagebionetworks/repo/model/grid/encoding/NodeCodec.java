package org.sagebionetworks.repo.model.grid.encoding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.node.Node;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

/**
 * Interface for encoding and decoding JSON CRDT nodes to a serialized format.
 */
public interface NodeCodec {
	/**
	 * Encodes a node to a serialized format and writes it to the output stream.
	 *
	 * @param node the node to encode
	 * @param out the output stream
	 * @return the number of bytes written
	 * @throws IOException if an I/O error occurs
	 * @throws IllegalArgumentException if the node type is not supported
	 */
	<T extends Node> int encode(T node, ClockTable clockTable, OutputStream out) throws IOException;



	/**
	 * Decode a node from the input stream.
	 *
	 * @param nodeId the ID of the node being decoded (provided externally for indexed format)
	 * @param clockTable the clock table for timestamp decoding
	 * @param in the input stream
	 * @return the decoded Node
	 * @throws IOException if an I/O error occurs
	 */
	Node decode(LogicalTimestamp nodeId, ClockTable clockTable, InputStream in) throws IOException;


}


