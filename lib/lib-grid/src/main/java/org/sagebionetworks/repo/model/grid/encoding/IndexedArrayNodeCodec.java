package org.sagebionetworks.repo.model.grid.encoding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.node.ArrayNode;
import org.sagebionetworks.repo.model.grid.node.RGANode;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Encoder for ArrayNode in the Indexed format.
 *
 * Array node encoding:
 * <pre>
 * - Node type: 110 (6)
 * - Length: number of chunks
 * - Contains: chunks with IDs, deletion flags, and content IDs
 *
 * Chunks are consecutively encoded one after another. A chunk begins with its ID, followed by b1vu56 integer,
 * where the flag is truthy if the chunk is a tombstone. The value of the b1vu56 integer is the span of the
 * chunk. If the chunk is not deleted (is not a tombstone) it is followed by contents of the chunk, a list of
 * CRDT nodes of length equal to the span of the chunk.
 *
 * For the indexed encoding format, only the chunk IDs are encoded, without the contents.
 * Chunks contain node IDs, instead of inlined nodes.
 *
 *  Chunk 1
 *  |                 Chunk 2 (tombstone)
 *  |                 |
 *  |                 |        Chunk 3
 *  |                 |        |
 * +========+========+========+========+========+........+
 * | b1vu56 |   id   | b1vu56 | b1vu56 |   id   |        |
 * +========+========+========+========+========+........+
 * </pre>
 *
 * Consecutive non-deleted elements with sequential node IDs (same replica ID, sequence number n+1)
 * are grouped into a single chunk for more compact encoding.
 *
 * @see <a href="https://jsonjoy.com/specs/json-crdt/encoding/indexed-encoding">Indexed Encoding</a>
 */
public class IndexedArrayNodeCodec implements IndexedNodeTypeCodec<ArrayNode> {

	/**
	 * Represents a chunk of consecutive RGA elements.
	 */
	static class Chunk {
		final LogicalTimestamp chunkId;
		final boolean isDeleted;
		final List<RGANode> elements;

		Chunk(LogicalTimestamp chunkId, boolean isDeleted) {
			this.chunkId = chunkId;
			this.isDeleted = isDeleted;
			this.elements = new ArrayList<>();
		}

		int getSpan() {
			return elements.size();
		}
	}

	@Override
	public int encode(ArrayNode node, ClockTable clockTable, OutputStream out) throws IOException {
		ValidateArgument.required(node, "node");
		ValidateArgument.required(node.getId(), "node.id");
		ValidateArgument.required(out, "out");

		int bytesWritten = 0;

		List<Chunk> chunks = groupIntoChunks(node.getElements());

		// Write node type (110) and length (number of chunks)
		bytesWritten += IndexedEncodingUtils.writeNodeTypeAndLength(IndexedEncodingUtils.NODE_TYPE_ARRAY, chunks.size(), out);

		for (Chunk chunk : chunks) {
			// 1. Write chunk ID (the node ID of the first element in the chunk)
			byte[] chunkId = clockTable.encodeTimestamp(chunk.chunkId);
			out.write(chunkId);
			bytesWritten += chunkId.length;

			// 2. Write b1vu56 header (deleted flag, span)
			byte[] chunkHeader = B1Vu56Utils.encodeB1Vu56(chunk.isDeleted, chunk.getSpan());
			out.write(chunkHeader);
			bytesWritten += chunkHeader.length;

			// 3. Write content IDs for each element in the chunk - only if not deleted
			if (!chunk.isDeleted) {
				for (RGANode rgaNode : chunk.elements) {
					if (rgaNode.getDataId() != null) {
						byte[] contentId = clockTable.encodeTimestamp(rgaNode.getDataId());
						out.write(contentId);
						bytesWritten += contentId.length;
					}
				}
			}
		}

		return bytesWritten;
	}

	/**
	 * Groups RGA elements into chunks. Consecutive non-deleted elements with sequential node IDs
	 * (same replica ID, sequence number n+1) are grouped into a single chunk.
	 * Deleted elements always form their own chunk.
	 *
	 * @param elements the list of RGA elements to group
	 * @return a list of chunks
	 */
	List<Chunk> groupIntoChunks(List<RGANode> elements) {
		List<Chunk> chunks = new ArrayList<>();

		if (elements == null || elements.isEmpty()) {
			return chunks;
		}

		Chunk currentChunk = null;

		for (RGANode element : elements) {
			boolean isDeleted = element.getIsDeleted() == true;

			if (currentChunk == null) {
				// Start a new chunk
				currentChunk = new Chunk(element.getNodeId(), isDeleted);
				currentChunk.elements.add(element);
			} else if (isDeleted || currentChunk.isDeleted) {
				// Deleted elements always start a new chunk
				// Also, if the current chunk is deleted, we can't add non-deleted elements to it
				chunks.add(currentChunk);
				currentChunk = new Chunk(element.getNodeId(), isDeleted);
				currentChunk.elements.add(element);
			} else if (isSequential(currentChunk, element)) {
				// Non-deleted element with sequential node ID - add to current chunk
				currentChunk.elements.add(element);
			} else {
				// Non-sequential node ID - start a new chunk
				chunks.add(currentChunk);
				currentChunk = new Chunk(element.getNodeId(), isDeleted);
				currentChunk.elements.add(element);
			}
		}

		// Add the last chunk
        chunks.add(currentChunk);

        return chunks;
	}

	/**
	 * Checks if the element's node ID is sequential to the last element in the chunk.
	 * Sequential means same replica ID and sequence number is exactly one more than the last element.
	 */
	static boolean isSequential(Chunk chunk, RGANode element) {
		if (chunk.elements.isEmpty()) {
			return false;
		}

		RGANode lastElement = chunk.elements.get(chunk.elements.size() - 1);
		LogicalTimestamp lastNodeId = lastElement.getNodeId();
		LogicalTimestamp currentNodeId = element.getNodeId();

		return lastNodeId.getReplicaId().equals(currentNodeId.getReplicaId())
			&& lastNodeId.getSequenceNumber() + 1 == currentNodeId.getSequenceNumber();
	}

	@Override
	public ArrayNode decode(LogicalTimestamp arrayNodeId, long length, ClockTable clockTable, InputStream in) throws IOException {
		ValidateArgument.required(arrayNodeId, "arrayNodeId");
		ValidateArgument.required(length, "length");
		ValidateArgument.required(clockTable, "clockTable");
		ValidateArgument.required(in, "in");

		// For arrays, the length indicates the number of chunks
		long numChunks = length;

		List<RGANode> elements = new ArrayList<>();
		LogicalTimestamp lastNodeId = null;

		// Read chunks - the header length indicates the number of chunks, not elements
		for (long chunksRead = 0; chunksRead < numChunks; chunksRead++) {
			// 1. Read chunk ID (the position in the array) - use raw timestamp decoding for indexed format
			LogicalTimestamp chunkId = clockTable.decodeTimestamp(in);

			// 2. Read chunk header: b1u56 (flag=isDeleted, value=chunkLength)
			B1Vu56Utils.B1Vu56Result chunkHeader = B1Vu56Utils.decodeB1Vu56(in);
			boolean isDeleted = chunkHeader.getFlag();
			long chunkLength = chunkHeader.getValue();

			// 3. Read content IDs for each element in the chunk
			for (int i = 0; i < chunkLength; i++) {
				LogicalTimestamp nodeId = new LogicalTimestamp()
						.setReplicaId(chunkId.getReplicaId())
						.setSequenceNumber(chunkId.getSequenceNumber() + i);
				RGANode rgaNode = new RGANode()
					.setContainerId(arrayNodeId)
					.setNodeId(nodeId)
					.setReferenceNodeId(lastNodeId)
					.setIsDeleted(isDeleted);

				if (!isDeleted) {
					// Read the content/data ID for non-deleted elements
					LogicalTimestamp dataId = clockTable.decodeTimestamp(in);
					rgaNode.setDataId(dataId);
				}

				elements.add(rgaNode);
				lastNodeId = nodeId;
			}
		}

		return new ArrayNode()
			.setId(arrayNodeId)
			.setElements(elements);
	}
}
