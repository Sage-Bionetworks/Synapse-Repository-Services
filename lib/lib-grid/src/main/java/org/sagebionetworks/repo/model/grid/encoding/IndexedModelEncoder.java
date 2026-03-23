package org.sagebionetworks.repo.model.grid.encoding;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.node.Node;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.ValidateArgument;

import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;

/**
 * Encoder for the JSON CRDT indexed model format.
 * <p>
 * The indexed model format is a CBOR map containing:
 * <ul>
 *   <li>"c" — the clock table (binary encoded)</li>
 *   <li>"r" — the root node ID (binary encoded timestamp)</li>
 *   <li>"&lt;sid&gt;_&lt;seq&gt;" — node entries (binary encoded nodes)</li>
 * </ul>
 * </p>
 * This encoder streams nodes without buffering, completing an indefinite-length CBOR map on close.
 *
 * Usage:
 * <pre>
 * try (IndexedModelEncoder encoder = new IndexedModelEncoder(outputStream, clockTable, rootNodeId)) {
 *     for (Node node : nodes) {
 *         encoder.writeNode(node);
 *     }
 *    // Closing the encoder writes the rest of the file.
 * }
 * </pre>
 *
 * @see <a href="https://jsonjoy.com/specs/json-crdt/encoding/indexed-encoding">Indexed Encoding</a>
 */
public class IndexedModelEncoder implements Closeable {
	private final LogicalTimestamp rootNodeId;
	private final NodeCodec nodeCodec;
	private final CBORGenerator generator;
    private final ClockTable clockTable;

	private boolean closed = false;

	/**
	 * Create a new IndexedModelEncoder.
	 *
	 * @param out the output stream to write to
	 * @param rootNodeId the ID of the root node
	 * @throws IOException if an I/O error occurs
	 */
	public IndexedModelEncoder(OutputStream out, LogicalTimestamp rootNodeId) {
		ValidateArgument.required(out, "out");
		ValidateArgument.required(rootNodeId, "rootNodeId");

		this.rootNodeId = rootNodeId;
		this.clockTable = new ClockTable(new ArrayList<>());
		clockTable.updateClockTable(rootNodeId);
		this.nodeCodec = new IndexedNodeCodec();

		try {
			this.generator = CBORUtils.getCBORFactory().createGenerator(out);
			this.generator.writeStartObject();
		} catch (IOException e) {
			throw new RuntimeException("Failed to create CBOR generator", e);
		}
	}

	/**
	 * Write a node to the model.
	 *
	 * @param node the node to write
	 * @throws IOException if an I/O error occurs
	 * @throws IllegalStateException if the encoder has been closed
	 */
	public void writeNode(Node node) throws IOException {
		if (closed) {
			throw new IllegalStateException("Encoder has been closed");
		}
		ValidateArgument.required(node, "node");
		ValidateArgument.required(node.getId(), "node.id");

		// Update the clock table with the node.
		clockTable.processNode(node);

		// Generate the node key
		String nodeKey = clockTable.encodeNodeKey(node.getId());

		// Encode the node to binary
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			nodeCodec.encode(node, clockTable, baos);

			// Write the entry to the output file
			generator.writeFieldName(nodeKey);
			generator.writeBinary(baos.toByteArray());
		}
	}

	public ClockTable getClockTable() {
		return clockTable;
	}

	/**
	 * Close the encoder, writing the complete CBOR map.
	 * This must be called to produce valid output.
	 *
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	public void close() throws IOException {
		if (!closed) {
			// Write clock table ("c")
			generator.writeFieldName("c");
			generator.writeBinary(clockTable.toBinary());

			// Write root node ID ("r") - uses raw sequence number, not difference encoding
			generator.writeFieldName("r");
			generator.writeBinary(clockTable.encodeTimestamp(rootNodeId));

			generator.writeEndObject();


			closed = true;
			generator.close();
		}
	}
}
