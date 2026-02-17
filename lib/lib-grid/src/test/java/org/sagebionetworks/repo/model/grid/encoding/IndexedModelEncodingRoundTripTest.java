package org.sagebionetworks.repo.model.grid.encoding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.node.ArrayNode;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.node.Node;
import org.sagebionetworks.repo.model.grid.node.ObjectNode;
import org.sagebionetworks.repo.model.grid.node.RGANode;
import org.sagebionetworks.repo.model.grid.node.VectorNode;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

/**
 * Round-trip tests for the indexed model encoder/decoder.
 * These tests verify that encoding followed by decoding produces the original data.
 */
public class IndexedModelEncodingRoundTripTest {

    private Path tempFile;
    private SnapshotFileIndexBuilder indexBuilder;

    @BeforeEach
    public void setUp() throws IOException {
        tempFile = Files.createTempFile("round-trip-test-", ".cbor");
        indexBuilder = new SnapshotFileIndexBuilder();
    }

    @AfterEach
    public void tearDown() throws IOException {
        if (tempFile != null) {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    public void testWriteModelToFile() throws IOException {
        // Encode and decode a JSON CRDT model. If making changes to this test, also consider attempting to read the
        // file using the json-joy JavaScript library.

        LogicalTimestamp testRootNodeId = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(2L);

        List<Node> nodes = new ArrayList<>();
        nodes.add(new ObjectNode()
                .setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(2L))
                .setValue(Map.of(
                        "const", new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(3L),
                        "vector", new LogicalTimestamp().setReplicaId(200L).setSequenceNumber(4L),
                        "array", new LogicalTimestamp().setReplicaId(200L).setSequenceNumber(10L)
                )));
        nodes.add(new ConstantNode()
                .setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(3L))
                .setValue(new ConValue(ConType.LONG, 42L)));
        nodes.add(new VectorNode()
                .setId(new LogicalTimestamp().setReplicaId(200L).setSequenceNumber(4L))
                .setValues(Map.of(0, new ConstantNode()
                                .setId(new LogicalTimestamp().setReplicaId(200L).setSequenceNumber(6L)),
                        1, new ConstantNode()
                                .setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(7L)),
                        3, new ConstantNode()
                                .setId(new LogicalTimestamp().setReplicaId(200L).setSequenceNumber(8L)))));
        nodes.add(new ConstantNode()
                .setId(new LogicalTimestamp().setReplicaId(200L).setSequenceNumber(6L))
                .setValue(new ConValue(ConType.STRING, "hello")));
        nodes.add(new ConstantNode()
                .setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(7L))
                .setValue(new ConValue(ConType.DOUBLE, 3.14)));
        nodes.add(new ConstantNode()
                .setId(new LogicalTimestamp().setReplicaId(200L).setSequenceNumber(8L))
                .setValue(new ConValue(ConType.UNDEFINED, null)));

        // Note: The decoder sets referenceNodeId to the array head (containerId) for the first element
        LogicalTimestamp arrayId = new LogicalTimestamp().setReplicaId(200L).setSequenceNumber(10L);
        nodes.add(new ArrayNode()
                .setId(arrayId)
                .setElements(List.of(
                        new RGANode()
                                .setContainerId(arrayId)
                                .setNodeId(new LogicalTimestamp().setReplicaId(200L).setSequenceNumber(11L))
                                .setReferenceNodeId(arrayId)  // First element references the array head
                                .setDataId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(12L))
                                .setIsDeleted(false),
                        new RGANode()
                                .setContainerId(arrayId)
                                .setNodeId(new LogicalTimestamp().setReplicaId(200L).setSequenceNumber(13L))
                                .setDataId(new LogicalTimestamp().setReplicaId(200L).setSequenceNumber(14L))
                                .setReferenceNodeId(new LogicalTimestamp().setReplicaId(200L).setSequenceNumber(11L))
                                .setIsDeleted(false)
                )));
        nodes.add(new ConstantNode()
                .setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(15L))
                .setValue(new ConValue(ConType.BOOLEAN, true)));
        nodes.add(new ConstantNode()
                .setId(new LogicalTimestamp().setReplicaId(200L).setSequenceNumber(16L))
                .setValue(new ConValue(ConType.NULL, null)));


        byte[] encodedBytes;
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            try (IndexedModelEncoder encoder = new IndexedModelEncoder(byteArrayOutputStream, testRootNodeId)) {
                nodes.forEach(node -> {
                    try {
                        encoder.writeNode(node);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
            encodedBytes = byteArrayOutputStream.toByteArray();
        }
        assertTrue(encodedBytes.length > 0);

        // Write to temp file for the decoder
        Files.write(tempFile, encodedBytes);

        ClockTable expectedClockTable = new ClockTable(List.of(
                new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(15L),
                new LogicalTimestamp().setReplicaId(200L).setSequenceNumber(16L)
        ));

        // Build the decoder index and verify metadata
        SnapshotFileIndex index = indexBuilder.build(tempFile);
        assertEquals(testRootNodeId, index.getRootNodeId(), "Root node ID should match");
        assertEquals(expectedClockTable, index.getClockTable(), "Clock table should match");
        assertEquals(nodes.size(), index.getTotalNodeCount(), "Total node count should match");

        // Read all nodes back using SeekingNodeReader and verify they match
        List<Node> decodedNodes = new ArrayList<>();
        try (SeekingNodeReader reader = new SeekingNodeReader(tempFile, index)) {
            // Read nodes in type order (same order they were indexed)
            for (IndexedNodeCodecMapper type : IndexedNodeCodecMapper.values()) {
                Map<LogicalTimestamp, SnapshotFileIndex.NodePointer> entries = index.getEntriesForType(type);
                for (LogicalTimestamp nodeId : entries.keySet()) {
                    decodedNodes.add(reader.readNode(type, nodeId));
                }
            }
        }

        // Sort the node lists by timestamp and compare
        Comparator<Node> byId = Comparator.comparing(Node::getId);
        assertEquals(
                nodes.stream().sorted(byId).collect(Collectors.toList()),
                decodedNodes.stream().sorted(byId).collect(Collectors.toList()),
                "Original and decoded are equal"
        );
    }
}
