package org.sagebionetworks.repo.model.grid;

import static org.sagebionetworks.repo.model.grid.encoding.Base36Utils.decodeBase36;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.json.JSONArray;
import org.sagebionetworks.repo.model.grid.encoding.B1Vu56Utils;
import org.sagebionetworks.repo.model.grid.encoding.Base36Utils;
import org.sagebionetworks.repo.model.grid.encoding.Vu57Utils;
import org.sagebionetworks.repo.model.grid.node.Node;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Clock table for serializing to a binary format described by the JSON CRDT specification. The clock table is directly
 * encoded into the serialized document, and is also used referentially to serialize every timestamp in the document.
 * <br/>
 * This implementation currently supports the Indexed Binary format. In the Indexed Binary format, timestamps are encoded
 * in two parts. The replica ID is encoded as an index which references an entry in the clock table, and the sequence number
 * is encoded as-is as a variable-length unsigned integer.
 * <br />
 * Note that the Binary Structural format (which is not currently supported) encodes the sequence number as a delta from the
 * current clock table value.
 */
public class ClockTable {
    private final List<LogicalTimestamp> clocks;

    public ClockTable(List<LogicalTimestamp> clocks) {
        this.clocks = clocks;
    }

    /**
     * Process a node to update the clock table as needed.
     */
    public void processNode(Node node) {
        ValidateArgument.required(node, "node");

        node.streamReferencedTimestamps()
                .forEach(this::updateClockTable);
    }

    /**
     * Update the clock table with the given timestamp.
     */
    public void updateClockTable(LogicalTimestamp timestamp) {
        ValidateArgument.required(timestamp, "timestamp");

        // Check if the replica ID is already in the clock table
        for (LogicalTimestamp clock : clocks) {
            if (clock.getReplicaId().equals(timestamp.getReplicaId())) {
                // Update the sequence number if the new timestamp is greater
                if (timestamp.getSequenceNumber() > clock.getSequenceNumber()) {
                    clock.setSequenceNumber(timestamp.getSequenceNumber());
                }
                return;
            }
        }

        // If not found, add a new entry to the clock table
        clocks.add(new LogicalTimestamp()
                .setReplicaId(timestamp.getReplicaId())
                .setSequenceNumber(timestamp.getSequenceNumber()));
    }

    /**
     * Get the list of clocks in the clock table.
     *
     * @return the clocks
     */
    public List<LogicalTimestamp> getClocks() {
        return clocks;
    }

    /**
     * Serializes the clock table to a binary format.
     *
     * @return the binary-encoded clock table
     */
    public byte[] toBinary() {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            outputStream.write(Vu57Utils.encodeVu57(clocks.size()));
            for (LogicalTimestamp clock : clocks) {
                outputStream.write(toBinaryClockValue(clock));
            }
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to encode ClockTable to binary", e);
        }
    }

    public static ClockTable fromBinary(byte[] bytes) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        // First, read the number of clocks
        long numClocks = Vu57Utils.decodeVu57(in);

        List<LogicalTimestamp> clocks = new ArrayList<>((int) numClocks);
        for (int i = 0; i < numClocks; i++) {
            // Each clock is two vu57 integers: session ID and sequence number
            long sessionId = Vu57Utils.decodeVu57(in);
            long sequenceNumber = Vu57Utils.decodeVu57(in);
            clocks.add(new LogicalTimestamp()
                    .setReplicaId(sessionId)
                    .setSequenceNumber(sequenceNumber));
        }

        return new ClockTable(clocks);
    }

    /**
     * The binary encoding of this LogicalTimestamp as it appears in the clock table. Exposed for testing only, there
     * is no reason to use this outside of encoding the clock table.
     * @see <a href="https://jsonjoy.com/specs/json-crdt/encoding/structural-encoding/binary-structural-format#Timestamp-Encoding">Timestamp Encoding</a>
     */
    public static byte[] toBinaryClockValue(LogicalTimestamp timestamp) {
        // Each clock is encoded as two vu57 integers, where the first integer encodes the session ID, and the second
        // integer encodes the logical time sequence number.
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            outputStream.write(Vu57Utils.encodeVu57(timestamp.getReplicaId()));
            outputStream.write(Vu57Utils.encodeVu57(timestamp.getSequenceNumber()));
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode LogicalTimestamp to binary", e);
        }
    }


    /**
     * Decodes a binary-encoded timestamp from a byte array which references the clock table.
     *
     * @param bytes the byte array containing the encoded root reference
     * @return the decoded LogicalTimestamp
     */
    public LogicalTimestamp decodeTimestamp(byte[] bytes) {
        try {
            return decodeTimestamp(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new RuntimeException("Failed to decode root reference", e);
        }
    }

    /**
     * Encodes a timestamp where the replica ID is mapped to a session index in the clock table.
     *
     * @param timestamp the timestamp to encode
     * @return the encoded bytes
     */
    public byte[] encodeTimestamp(LogicalTimestamp timestamp) {
        long sessionIndex = findSessionIndex(timestamp.getReplicaId());

        // Calculate the value to encode based on mode
        long valueToEncode = timestamp.getSequenceNumber();

        // Use compact single-byte encoding if possible
        if (sessionIndex < 8 && valueToEncode < 16) {
            // b1u3u4 encoding: flag=0, 3 bits for session index, 4 bits for value
            int encodedByte = (int) ((sessionIndex << 4) | valueToEncode);
            return new byte[] { (byte) encodedByte };
        }

        // Use multi-byte encoding
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            B1Vu56Utils.encodeB1Vu56(true, sessionIndex, outputStream);
            outputStream.write(Vu57Utils.encodeVu57(valueToEncode));
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to encode timestamp", e);
        }
    }

    /**
     * Internal method to decode a timestamp with specified encoding mode.
     *
     * @param in the input stream to read from
     * @return the decoded LogicalTimestamp
     * @throws IOException if an I/O error occurs
     */
    public LogicalTimestamp decodeTimestamp(InputStream in) throws IOException {
        int firstByte = in.read();
        if (firstByte == -1) {
            throw new IOException("Unexpected end of stream while decoding timestamp");
        }

        long sessionIndex;
        long encodedValue;

        if ((firstByte & 0x80) == 0) {
            // Single-byte b1u3u4 encoding
            sessionIndex = (firstByte >> 4) & 0x07;
            encodedValue = firstByte & 0x0F;
        } else {
            // Multi-byte encoding
            java.io.ByteArrayInputStream tempIn = new java.io.ByteArrayInputStream(new byte[] { (byte) firstByte });
            java.io.SequenceInputStream combinedIn = new java.io.SequenceInputStream(tempIn, in);
            B1Vu56Utils.B1Vu56Result b1Vu56Result = B1Vu56Utils.decodeB1Vu56(combinedIn);
            sessionIndex = b1Vu56Result.getValue();
            encodedValue = Vu57Utils.decodeVu57(in);
        }

        if (sessionIndex >= clocks.size()) {
            throw new IllegalArgumentException("Session index out of bounds: " + sessionIndex + " >= " + clocks.size());
        }

        LogicalTimestamp clockEntry = clocks.get((int) sessionIndex);

        // Calculate final sequence number based on mode
        long sequenceNumber = encodedValue;

        return new LogicalTimestamp()
            .setReplicaId(clockEntry.getReplicaId())
            .setSequenceNumber(sequenceNumber);
    }

    /**
     * Encode a node key from session index and sequence number.
     * Format: "&lt;sid&gt;_&lt;seq&gt;" where both are Base36 encoded.
     *
     * @param timestamp the timestamp to encode
     * @return the encoded node key
     */
    public String encodeNodeKey(LogicalTimestamp timestamp) {
        ValidateArgument.required(timestamp, "timestamp");

        long sessionIndex = -1;
        for (int i = 0; i < clocks.size(); i++) {
            if (clocks.get(i).getReplicaId().equals(timestamp.getReplicaId())) {
                sessionIndex = i;
                break;
            }
        }
        if (sessionIndex == -1) {
            throw new IllegalArgumentException("Replica ID not found in clock table: " + timestamp.getReplicaId());
        }

        return Base36Utils.encodeBase36(sessionIndex) + "_" + Base36Utils.encodeBase36(timestamp.getSequenceNumber());
    }

    public LogicalTimestamp decodeNodeKey(String nodeKey) {
        ValidateArgument.required(nodeKey, "nodeKey");

        int underscoreIndex = nodeKey.indexOf('_');
        if (underscoreIndex < 0) {
            throw new IllegalArgumentException("Invalid node key format, missing underscore: " + nodeKey);
        }

        long sessionIndex = decodeBase36(nodeKey.substring(0, underscoreIndex));
        long sequenceNumber = decodeBase36(nodeKey.substring(underscoreIndex + 1));

        if (sessionIndex >= clocks.size()) {
            throw new IllegalArgumentException("Session index out of bounds: " + sessionIndex + " >= " + clocks.size());
        }

        return new LogicalTimestamp()
                .setReplicaId(clocks.get((int) sessionIndex).getReplicaId())
                .setSequenceNumber(sequenceNumber);
    }

    public JSONArray toJsonArray() {
        JSONArray jsonArray = new JSONArray();
        for (LogicalTimestamp clock : clocks) {
            JSONArray clockJson = new JSONArray();
            clockJson.put(clock.getReplicaId());
            clockJson.put(clock.getSequenceNumber());
            jsonArray.put(clockJson);
        }
        return jsonArray;
    }

    public static ClockTable fromJsonArray(JSONArray jsonArray) {
        ValidateArgument.required(jsonArray, "jsonArray");
        List<LogicalTimestamp> clocks = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONArray clockJson = jsonArray.getJSONArray(i);
            Long replicaId = clockJson.getLong(0);
            Long sequenceNumber = clockJson.getLong(1);
            clocks.add(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(sequenceNumber));
        }
        return new ClockTable(clocks);
    }

    /**
     * Finds the session index for a given replica ID.
     *
     * @param replicaId the replica ID to find
     * @return the session index
     * @throws IllegalArgumentException if replica ID not found
     */
    private long findSessionIndex(Long replicaId) {
        for (int i = 0; i < clocks.size(); i++) {
            if (clocks.get(i).getReplicaId().equals(replicaId)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Timestamp replicaId not found in clock table: " + replicaId);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ClockTable that = (ClockTable) o;
        return Objects.equals(clocks, that.clocks);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(clocks);
    }

    @Override
    public String toString() {
        return "ClockTable{" +
                "clocks=" + clocks +
                '}';
    }
}
