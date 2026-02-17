package org.sagebionetworks.repo.model.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.encoding.Vu57Utils;
import org.sagebionetworks.repo.model.grid.node.ArrayNode;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.node.ObjectNode;
import org.sagebionetworks.repo.model.grid.node.RGANode;
import org.sagebionetworks.repo.model.grid.node.VectorNode;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

import com.google.common.primitives.Bytes;

public class ClockTableTest {

    @Test
    public void testToBinaryRealClockTable() throws IOException {
        // Created a model in json-joy, encoded as compact and binary and printed it to the JavaScript console.
        // The compact model provided human-readable values for the clocks
        // The binary model provided the expected binary output (as unsigned integers).
        ClockTable clockTable = new ClockTable(List.of(
            new LogicalTimestamp().setReplicaId(66537L).setSequenceNumber(217L),
            new LogicalTimestamp().setReplicaId(66534L).setSequenceNumber(214L),
            new LogicalTimestamp().setReplicaId(66533L).setSequenceNumber(217L)
        ));

        // call under test
        byte[] binary = clockTable.toBinary();

        // Raw values copied from JSON Joy JavaScript output - the JavaScript model prints bytes as unsigned ints.
        List<Integer> expectedBytes = List.of(3, 233, 135, 4, 217, 1, 230, 135, 4, 214, 1, 229, 135, 4, 217, 1);

        // Convert signed bytes to unsigned integers for comparison
        List<Integer> actualAsUnsigned = Bytes.asList(binary).stream().map(Byte::toUnsignedInt).collect(Collectors.toList());
        assertEquals(expectedBytes, actualAsUnsigned);

        // call under test - decode and verify round-trip
        ClockTable decodedClockTable = ClockTable.fromBinary(binary);
        assertEquals(clockTable.getClocks(), decodedClockTable.getClocks());
    }

    @Test
    public void testEncodeDecodeTimestampSingleByteEncoding() {
        // Single-byte encoding is used when sessionIndex < 8 and sequenceNumber < 16
        ClockTable clockTable = new ClockTable(List.of(
            new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(50L),
            new LogicalTimestamp().setReplicaId(200L).setSequenceNumber(60L)
        ));

        LogicalTimestamp original = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(5L);

        // call under test
        byte[] encoded = clockTable.encodeTimestamp(original);
        LogicalTimestamp decoded = clockTable.decodeTimestamp(encoded);

        assertEquals(1, encoded.length, "Should use single-byte encoding for small values");
        assertEquals(original, decoded);
    }

    @Test
    public void testEncodeDecodeTimestampSingleByteEncodingMaxValues() {
        // Test the boundary: sessionIndex = 7, sequenceNumber = 15
        List<LogicalTimestamp> clocks = List.of(
            new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L),
            new LogicalTimestamp().setReplicaId(2L).setSequenceNumber(2L),
            new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(3L),
            new LogicalTimestamp().setReplicaId(4L).setSequenceNumber(4L),
            new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(5L),
            new LogicalTimestamp().setReplicaId(6L).setSequenceNumber(6L),
            new LogicalTimestamp().setReplicaId(7L).setSequenceNumber(7L),
            new LogicalTimestamp().setReplicaId(8L).setSequenceNumber(8L)  // index 7
        );
        ClockTable clockTable = new ClockTable(clocks);

        LogicalTimestamp original = new LogicalTimestamp().setReplicaId(8L).setSequenceNumber(15L);

        // call under test
        byte[] encoded = clockTable.encodeTimestamp(original);
        LogicalTimestamp decoded = clockTable.decodeTimestamp(encoded);

        assertEquals(1, encoded.length, "Should use single-byte encoding at boundary");
        assertEquals(original, decoded);
    }

    @Test
    public void testEncodeDecodeTimestampMultiByteEncodingLargeSessionIndex() {
        // Multi-byte encoding when sessionIndex >= 8
        List<LogicalTimestamp> clocks = List.of(
            new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L),
            new LogicalTimestamp().setReplicaId(2L).setSequenceNumber(2L),
            new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(3L),
            new LogicalTimestamp().setReplicaId(4L).setSequenceNumber(4L),
            new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(5L),
            new LogicalTimestamp().setReplicaId(6L).setSequenceNumber(6L),
            new LogicalTimestamp().setReplicaId(7L).setSequenceNumber(7L),
            new LogicalTimestamp().setReplicaId(8L).setSequenceNumber(8L),
            new LogicalTimestamp().setReplicaId(9L).setSequenceNumber(9L)  // index 8
        );
        ClockTable clockTable = new ClockTable(clocks);

        LogicalTimestamp original = new LogicalTimestamp().setReplicaId(9L).setSequenceNumber(5L);

        // call under test
        byte[] encoded = clockTable.encodeTimestamp(original);
        LogicalTimestamp decoded = clockTable.decodeTimestamp(encoded);

        assertEquals(2, encoded.length, "Should use multi-byte encoding for index >= 8");
        assertEquals(original, decoded);
    }

    @Test
    public void testEncodeDecodeTimestampMultiByteEncodingLargeSequenceNumber() {
        // Multi-byte encoding when sequenceNumber >= 16
        ClockTable clockTable = new ClockTable(List.of(
            new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(50L)
        ));

        LogicalTimestamp original = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(16L);

        // call under test
        byte[] encoded = clockTable.encodeTimestamp(original);
        LogicalTimestamp decoded = clockTable.decodeTimestamp(encoded);

        assertEquals(2, encoded.length, "Should use multi-byte encoding for sequenceNumber >= 16");
        assertEquals(original, decoded);
    }

    @Test
    public void testEncodeDecodeTimestampMultiByteEncodingVeryLargeValues() {
        ClockTable clockTable = new ClockTable(List.of(
            new LogicalTimestamp().setReplicaId(66537L).setSequenceNumber(1000L)
        ));

        LogicalTimestamp original = new LogicalTimestamp().setReplicaId(66537L).setSequenceNumber(999L);

        // call under test
        byte[] encoded = clockTable.encodeTimestamp(original);
        LogicalTimestamp decoded = clockTable.decodeTimestamp(encoded);

        assertEquals(original, decoded);
    }

    @Test
    public void testEncodeDecodeTimestampZeroSequenceNumber() {
        ClockTable clockTable = new ClockTable(List.of(
            new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(50L)
        ));

        LogicalTimestamp original = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(0L);

        // call under test
        byte[] encoded = clockTable.encodeTimestamp(original);
        LogicalTimestamp decoded = clockTable.decodeTimestamp(encoded);

        assertEquals(1, encoded.length, "Should use single-byte encoding for zero sequence");
        assertEquals(original, decoded);
    }

    @Test
    public void testEncodeTimestampReplicaIdNotInClockTable() {
        ClockTable clockTable = new ClockTable(List.of(
            new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(50L)
        ));

        LogicalTimestamp timestamp = new LogicalTimestamp().setReplicaId(999L).setSequenceNumber(5L);

        // call under test
        assertThrows(IllegalArgumentException.class, () -> clockTable.encodeTimestamp(timestamp),
            "Should throw when replicaId not found in clock table");
    }

    @Test
    public void testGetClocks() {
        List<LogicalTimestamp> clocks = List.of(
            new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(50L),
            new LogicalTimestamp().setReplicaId(200L).setSequenceNumber(60L)
        );
        ClockTable clockTable = new ClockTable(clocks);

        // call under test
        assertEquals(clocks, clockTable.getClocks());
    }


    @Test
    public void testToBinaryClockValue() throws Exception {
        Long replica = 123L;
        Long seq = 456L;
        LogicalTimestamp ts = new LogicalTimestamp().setReplicaId(replica).setSequenceNumber(seq);

        ByteArrayOutputStream expectedOut = new ByteArrayOutputStream();
        expectedOut.write(Vu57Utils.encodeVu57(replica));
        expectedOut.write(Vu57Utils.encodeVu57(seq));
        byte[] expected = expectedOut.toByteArray();

        byte[] actual = ClockTable.toBinaryClockValue(ts);
        // Verify lengths and content
        assertEquals(expected.length, actual.length, "Encoded byte array length should match expected");
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i], "Byte at index " + i + " should match");
        }
    }

    @Test
    public void testEncodeDecodeNodeKey() {
        ClockTable clockTable = new ClockTable(List.of(
            new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(50L),
            new LogicalTimestamp().setReplicaId(200L).setSequenceNumber(100L)
        ));

        LogicalTimestamp original = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(0L);
        String encoded = clockTable.encodeNodeKey(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(0L));
        assertEquals("0_0", encoded);
        assertEquals(original, clockTable.decodeNodeKey(encoded));

        original = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(1L);
        encoded = clockTable.encodeNodeKey(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(1L));
        assertEquals("0_1", encoded);
        assertEquals(original, clockTable.decodeNodeKey(encoded));

        original = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(36L);
        encoded = clockTable.encodeNodeKey(original);
        assertEquals("0_10", encoded);
        assertEquals(original, clockTable.decodeNodeKey(encoded));

        original = new LogicalTimestamp().setReplicaId(200L).setSequenceNumber(0L);
        encoded = clockTable.encodeNodeKey(original);
        assertEquals("1_0", encoded);
        assertEquals(original, clockTable.decodeNodeKey(encoded));

        original = new LogicalTimestamp().setReplicaId(200L).setSequenceNumber(35L);
        encoded = clockTable.encodeNodeKey(original);
        assertEquals("1_z", encoded);
        assertEquals(original, clockTable.decodeNodeKey(encoded));
    }

    @Test
    public void testEncodeNodeKeyReplicaNotInClockTable() {
        ClockTable clockTable = new ClockTable(List.of(
                new LogicalTimestamp().setReplicaId(123L).setSequenceNumber(456L)
        ));

        assertThrows(IllegalArgumentException.class, () -> {
            clockTable.encodeTimestamp(new LogicalTimestamp().setReplicaId(789L).setSequenceNumber(1L));
        });
    }


    @Test
    public void testDecodeNodeKeyInvalidFormat() {
        ClockTable clockTable = new ClockTable(List.of(
                new LogicalTimestamp().setReplicaId(123L).setSequenceNumber(456L)
        ));

        assertThrows(IllegalArgumentException.class, () -> {
            clockTable.decodeNodeKey("abc");
        });
    }


    @Test
    public void testDecodeNodeKeyOutOfBounds() {
        ClockTable clockTable = new ClockTable(List.of(
                new LogicalTimestamp().setReplicaId(123L).setSequenceNumber(456L)
        ));

        assertThrows(IllegalArgumentException.class, () -> {
            clockTable.decodeNodeKey("1_0");
        });
    }

    @Test
    public void testDecodeNodeKeyNull() {
        ClockTable clockTable = new ClockTable(List.of(
                new LogicalTimestamp().setReplicaId(123L).setSequenceNumber(456L)
        ));

        assertThrows(IllegalArgumentException.class, () -> {
            clockTable.decodeNodeKey(null);
        });
    }

    @Test
    public void testProcessNodeAddsReplica() {
        ClockTable clockTable = new ClockTable(new ArrayList<>());

        ConstantNode node = new ConstantNode()
            .setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(10L))
            .setValue("test");

        // call under test
        clockTable.processNode(node);

        assertEquals(1, clockTable.getClocks().size());
        assertEquals(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(10L), clockTable.getClocks().get(0));
    }

    @Test
    public void testProcessNodeUpdatesExistingEntries() {
        ClockTable clockTable = new ClockTable(new ArrayList<>(List.of(
            new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(10L),
            new LogicalTimestamp().setReplicaId(101L).setSequenceNumber(11L)
        )));

        RGANode element1 = new RGANode()
            .setNodeId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(50L))
            .setDataId(new LogicalTimestamp().setReplicaId(101L).setSequenceNumber(51L));

        ArrayNode node = new ArrayNode()
            .setId(new LogicalTimestamp().setReplicaId(200L).setSequenceNumber(20L))
            .setElements(List.of(element1));

        // call under test
        clockTable.processNode(node);

        assertEquals(3, clockTable.getClocks().size());
        assertEquals(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(50L), clockTable.getClocks().get(0));
        assertEquals(new LogicalTimestamp().setReplicaId(101L).setSequenceNumber(51L), clockTable.getClocks().get(1));
        assertEquals(new LogicalTimestamp().setReplicaId(200L).setSequenceNumber(20L), clockTable.getClocks().get(2));
    }

    @Test
    public void testProcessNodeNullNode() {
        ClockTable clockTable = new ClockTable(new ArrayList<>());

        // call under test
        assertThrows(IllegalArgumentException.class, () -> clockTable.processNode(null));
    }

}

