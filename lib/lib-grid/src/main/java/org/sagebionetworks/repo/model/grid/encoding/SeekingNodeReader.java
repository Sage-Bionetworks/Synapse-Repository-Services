package org.sagebionetworks.repo.model.grid.encoding;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.sagebionetworks.repo.model.grid.node.ArrayNode;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.node.Node;
import org.sagebionetworks.repo.model.grid.node.ObjectNode;
import org.sagebionetworks.repo.model.grid.node.ValueNode;
import org.sagebionetworks.repo.model.grid.node.VectorNode;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.ValidateArgument;

/**
 * A reader that uses random access to read nodes from a snapshot file at specific byte offsets.
 * This enables efficient type-based batch processing by allowing nodes to be read in any order
 * without sequential scanning.
 *
 * <p>The byte offset recorded by {@link SnapshotFileIndexBuilder} points directly to the binary content,
 * so this reader simply seeks to the offset and reads the specified number of bytes.</p>
 */
public class SeekingNodeReader implements Closeable {

	private final RandomAccessFile raf;
	private final SnapshotFileIndex index;
	private final NodeCodec decoder;

	/**
	 * Create a new SeekingNodeReader.
	 *
	 * @param snapshotFile the path to the snapshot CBOR file
	 * @param clockTable the clock table for decoding nodes
	 * @throws IOException if an I/O error occurs opening the file
	 */
	public SeekingNodeReader(Path snapshotFile, SnapshotFileIndex index) throws IOException {
		ValidateArgument.required(snapshotFile, "snapshotFile");
		ValidateArgument.required(index, "index");

		this.raf = new RandomAccessFile(snapshotFile.toFile(), "r");
		this.index = index;
		this.decoder = new IndexedNodeCodec();
	}

	private Node seekAndRead(LogicalTimestamp nodeId, SnapshotFileIndex.NodePointer nodePointer) throws IOException {
		// Seek directly to the binary content (offset points to content, not CBOR header)
		raf.seek(nodePointer.byteOffset());

		// Read the binary content
		byte[] nodeBytes = new byte[nodePointer.binaryLength()];
		raf.readFully(nodeBytes);

		// Decode the node
		try (ByteArrayInputStream nodeIn = new ByteArrayInputStream(nodeBytes)) {
			return decoder.decode(nodeId, index.getClockTable(), nodeIn);
		}
	}

	/**
	 * Read a single node from the file at the offset specified by the entry.
	 *
	 * @param nodeType type of the node to look up
	 * @param nodeId the id of the node look up
	 * @return the decoded node
	 * @throws IOException if an I/O error occurs
	 */
	public Node readNode(IndexedNodeCodecMapper nodeType, LogicalTimestamp nodeId) throws IOException {
		ValidateArgument.required(nodeType, "nodeType");
		ValidateArgument.required(nodeId, "nodeId");

		// Get the pointer from the map
		SnapshotFileIndex.NodePointer nodePointer = index.getPointer(nodeType, nodeId);
		ValidateArgument.required(nodePointer, "A reference must exist in the index for nodeType: " + nodeType + " and nodeId: " + nodeId);

		return seekAndRead(nodeId, nodePointer);
	}

	private Stream<? extends Node> streamNodesOfType(IndexedNodeCodecMapper type) {
		return index.getEntriesForType(type).entrySet().stream()
				.map(entry -> {
                    try {
                        return seekAndRead(entry.getKey(), entry.getValue());
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read node of type " + type + " with id " + entry.getKey(), e);
                    }
                });
	}

	public Stream<ConstantNode> streamConstantNodes() {
		return (Stream<ConstantNode>) streamNodesOfType(IndexedNodeCodecMapper.CONSTANT);
	}

	public Stream<ValueNode> streamValueNodes() {
		return (Stream<ValueNode>) streamNodesOfType(IndexedNodeCodecMapper.VAL);
	}

	public Stream<ObjectNode> streamObjectNodes() {
		return (Stream<ObjectNode>) streamNodesOfType(IndexedNodeCodecMapper.OBJECT);
	}

	public Stream<VectorNode> streamVectorNodes() {
		return (Stream<VectorNode>) streamNodesOfType(IndexedNodeCodecMapper.VECTOR);
	}

	public Stream<ArrayNode> streamArrayNodes() {
		return (Stream<ArrayNode>) streamNodesOfType(IndexedNodeCodecMapper.ARRAY);
	}

	@Override
	public void close() throws IOException {
		raf.close();
	}
}
