package org.sagebionetworks.repo.model.grid.encoding;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.TreeMap;

import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.ValidateArgument;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.dataformat.cbor.CBORParser;

/**
 * Parses a file formatted in the JSON CRDT Indexed Binary format and builds an index that can be used to scan the file.
 *
 * <p>
 * The indexed binary format is a CBOR map containing:
 * <ul>
 *   <li>"c" — the clock table (binary encoded)</li>
 *   <li>"r" — the root node ID (binary encoded timestamp)</li>
 *   <li>"&lt;sid&gt;_&lt;seq&gt;" — node entries (binary encoded nodes)</li>
 * </ul>
 * </p>
 * <p>
 * The {@link SnapshotFileIndexBuilder#build } method scans the file to retrieve the clock table and root node, and builds an
 * index of nodes grouped by type. The index optimizes reading batches of nodes to insert them into the database,
 * minimizing the number of database round-trips. This index is built by scanning the CBOR file and recording the byte
 * offset and length of each node's binary data, allowing for efficient random access.
 * </p>
 *
 * @see <a href="https://jsonjoy.com/specs/json-crdt/encoding/indexed-encoding">Indexed Encoding</a>
 */
public class SnapshotFileIndexBuilder {

	private static class ClockTableAndRootNodeId {
		final ClockTable clockTable;
		final LogicalTimestamp rootNodeId;

		ClockTableAndRootNodeId(ClockTable clockTable, LogicalTimestamp rootNodeId) {
			this.clockTable = clockTable;
			this.rootNodeId = rootNodeId;
		}
	}

	/**
	 * Build a decoder by scanning the CBOR file.
	 * This method extracts the ClockTable and rootNodeId from the file, then builds
	 * an index of all nodes grouped by type.
	 *
	 * @param snapshotFile the path to the snapshot CBOR file
	 * @return the built decoder
	 * @throws IOException if an I/O error occurs
	 */
	public SnapshotFileIndex build(Path snapshotFile) throws IOException {
		ValidateArgument.required(snapshotFile, "snapshotFile");

		// Pass 1: Get the clock table and root node ID.
		ClockTableAndRootNodeId clockTableAndRoot = scanFileForClockTableAndRoot(snapshotFile);

		// Pass 2: Build the index of nodes by type, now that we have the clock table to decode node keys
		Map<IndexedNodeCodecMapper, Map<LogicalTimestamp, SnapshotFileIndex.NodePointer>> entriesByType = buildNodeIndex(snapshotFile, clockTableAndRoot.clockTable);

		return new SnapshotFileIndex(clockTableAndRoot.rootNodeId, clockTableAndRoot.clockTable, entriesByType);
	}

	static ClockTableAndRootNodeId scanFileForClockTableAndRoot(Path snapshotFile) throws IOException {
		ClockTable clockTable = null;
		LogicalTimestamp rootNodeId = null;
		byte[] rootNodeBytes = null;

		try (InputStream in = new BufferedInputStream(new FileInputStream(snapshotFile.toFile()));
			 CBORParser parser = CBORUtils.getCBORFactory().createParser(in)) {

			// Expect start of object
			JsonToken token = parser.nextToken();
			if (token != JsonToken.START_OBJECT) {
				throw new IOException("Expected CBOR map, got: " + token);
			}

			// Scan all fields
			while ((token = parser.nextToken()) != null && token != JsonToken.END_OBJECT) {
				if (token != JsonToken.FIELD_NAME) {
					throw new IOException("Expected field name, got: " + token);
				}

				String fieldName = parser.currentName();
				parser.nextToken();

				if ("c".equals(fieldName)) {
					// Clock table field
					byte[] clockTableBytes = parser.getBinaryValue();
					clockTable = ClockTable.fromBinary(clockTableBytes);
					// Decode rootNodeId if we already have the bytes
					if (rootNodeBytes != null) {
						rootNodeId = clockTable.decodeTimestamp(rootNodeBytes);
						rootNodeBytes = null;
					}
				} else if ("r".equals(fieldName)) {
					// Root node ID field
					byte[] bytes = parser.getBinaryValue();
					if (clockTable != null) {
						rootNodeId = clockTable.decodeTimestamp(bytes);
					} else {
						rootNodeBytes = bytes; // Store for later
					}
				} else {
					parser.skipChildren();
				}
			}
		}


		if (clockTable == null) {
			throw new IOException("Clock table ('c') not found in snapshot");
		}
		if (rootNodeId == null) {
			throw new IOException("Root node ID ('r') not found in snapshot");
		}

		return new ClockTableAndRootNodeId(clockTable, rootNodeId);
	}

	static Map<IndexedNodeCodecMapper, Map<LogicalTimestamp, SnapshotFileIndex.NodePointer>> buildNodeIndex(Path snapshotFile, ClockTable clockTable) throws IOException {
		Map<IndexedNodeCodecMapper, Map<LogicalTimestamp, SnapshotFileIndex.NodePointer>> entriesByType = new EnumMap<>(IndexedNodeCodecMapper.class);
		for (IndexedNodeCodecMapper type : IndexedNodeCodecMapper.values()) {
			entriesByType.put(type, new TreeMap<>());
		}
		try (InputStream in = new BufferedInputStream(new FileInputStream(snapshotFile.toFile()));
			 CBORParser parser = CBORUtils.getCBORFactory().createParser(in)) {

			// Expect start of object
			JsonToken token = parser.nextToken();
			if (token != JsonToken.START_OBJECT) {
				throw new IOException("Expected CBOR map, got: " + token);
			}

			// Scan all fields
			while ((token = parser.nextToken()) != null && token != JsonToken.END_OBJECT) {
				if (token != JsonToken.FIELD_NAME) {
					throw new IOException("Expected field name, got: " + token);
				}

				String fieldName = parser.currentName();
				parser.nextToken();

				if ("c".equals(fieldName)) {
					parser.skipChildren();
				} else if ("r".equals(fieldName)) {
					parser.skipChildren();
				} else {
					// Node entry
					byte[] nodeBytes = parser.getBinaryValue();
					int binaryLength = nodeBytes.length;
					long byteOffset = parser.currentLocation().getByteOffset() - binaryLength;

					// Clock table already available - process immediately
					LogicalTimestamp nodeId = clockTable.decodeNodeKey(fieldName);
					IndexedNodeCodecMapper nodeType = IndexedNodeCodecMapper.getByFirstByte(nodeBytes[0]);
					SnapshotFileIndex.NodePointer nodePointer = new SnapshotFileIndex.NodePointer(byteOffset, binaryLength);
					entriesByType.get(nodeType).put(nodeId, nodePointer);
				}
			}
		}

		return entriesByType;
	}
}
