package org.sagebionetworks.repo.model.grid.encoding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.node.ArrayNode;
import org.sagebionetworks.repo.model.grid.node.RGANode;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class IndexedArrayNodeCodecTest {

    private IndexedArrayNodeCodec codec;
    private ClockTable clockTable;

    @BeforeEach
    public void setUp() {
        codec = new IndexedArrayNodeCodec();
        // Create a clock table with a single session
        clockTable = new ClockTable(List.of(
            new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(50L)
        ));
    }

    public enum TestArrayNodeCase {
        EMPTY(new ArrayNode().setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(50L))
                .setElements(new ArrayList<>()), null),

        SINGLE_ELEMENT(new ArrayNode()
                .setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(10L))
                .setElements(List.of(
                        new RGANode()
                                .setContainerId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(10L))
                                .setReferenceNodeId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(10L))
                                .setNodeId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(11L))
                                .setDataId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(5L))
                                .setIsDeleted(false)
                )), null),

        DELETED_ELEMENT(new ArrayNode()
                .setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(10L))
                .setElements(List.of(
                        new RGANode()
                                .setContainerId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(10L))
                                .setReferenceNodeId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(10L))
                                .setNodeId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(11L))
                                .setIsDeleted(true)
                )), null),

        MIXED_ELEMENTS_NONSEQUENTIAL(new ArrayNode()
                .setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(10L))
                .setElements(List.of(
                        new RGANode()
                                .setContainerId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(10L))
                                .setReferenceNodeId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(10L))
                                .setNodeId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(11L))
                                .setDataId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(5L))
                                .setIsDeleted(false),
                        new RGANode()
                                .setContainerId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(10L))
                                .setNodeId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(13L))
                                .setReferenceNodeId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(11L))
                                .setIsDeleted(true),
                        new RGANode()
                                .setContainerId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(10L))
                                .setNodeId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(14L))
                                .setReferenceNodeId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(13L))
                                .setDataId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(3L))
                                .setIsDeleted(false)
                )), null),

        SEQUENTIAL_ELEMENTS_SINGLE_CHUNK(new ArrayNode()
                .setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(10L))
                .setElements(List.of(
                        new RGANode()
                                .setContainerId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(10L))
                                .setReferenceNodeId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(10L))
                                .setNodeId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(11L))
                                .setDataId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(1L))
                                .setIsDeleted(false),
                        new RGANode()
                                .setContainerId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(10L))
                                .setNodeId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(12L))
                                .setReferenceNodeId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(11L))
                                .setDataId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(2L))
                                .setIsDeleted(false),
                        new RGANode()
                                .setContainerId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(10L))
                                .setNodeId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(13L))
                                .setReferenceNodeId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(12L))
                                .setDataId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(3L))
                                .setIsDeleted(false)
                )), null),

        MULTIPLE_REPLICAS(new ArrayNode()
                .setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(10L))
                .setElements(List.of(
                        new RGANode()
                                .setContainerId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(10L))
                                .setReferenceNodeId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(10L))
                                .setNodeId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(11L))
                                .setDataId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(1L))
                                .setIsDeleted(false),
                        new RGANode()
                                .setContainerId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(10L))
                                .setNodeId(new LogicalTimestamp().setReplicaId(200L).setSequenceNumber(11L))
                                .setReferenceNodeId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(11L))
                                .setDataId(new LogicalTimestamp().setReplicaId(200L).setSequenceNumber(2L))
                                .setIsDeleted(false)
                )), new ClockTable(List.of(
                        new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(50L),
                        new LogicalTimestamp().setReplicaId(200L).setSequenceNumber(50L)
                ))),

        GAP_IN_SEQUENCE_NUMBERS(new ArrayNode()
                .setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(10L))
                .setElements(List.of(
                        new RGANode()
                                .setContainerId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(10L))
                                .setReferenceNodeId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(10L))
                                .setNodeId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(11L))
                                .setDataId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(1L))
                                .setIsDeleted(false),
                        new RGANode()
                                .setContainerId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(10L))
                                .setNodeId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(15L))
                                .setReferenceNodeId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(11L))
                                .setDataId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(2L))
                                .setIsDeleted(false)
                )), null);

        final ArrayNode node;
		final ClockTable clockTable;

        TestArrayNodeCase(ArrayNode node, ClockTable clockTable) {
            this.node = node;
			this.clockTable = clockTable;
        }
    }

    @ParameterizedTest
    @EnumSource(TestArrayNodeCase.class)
    public void testEncodeArrayNode(TestArrayNodeCase testCase) throws IOException {
        ArrayNode node = testCase.node;
        ClockTable table = testCase.clockTable == null ? this.clockTable : testCase.clockTable;

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // call under test - encode
        codec.encode(node, table, out);
        byte[] bytes = out.toByteArray();
        InputStream byteStream = new ByteArrayInputStream(bytes);

        // call under test - decode header
        IndexedNodeHeader nodeHeader = IndexedEncodingUtils.readNodeHeader(byteStream);
        assertEquals(IndexedNodeCodecMapper.ARRAY.code, nodeHeader.getNodeType());
        assertTrue(nodeHeader.getLength() >= 0L);

        // call under test - decode body using the original node id
        ArrayNode decoded = codec.decode(node.getId(), nodeHeader.getLength(), table, byteStream);
        assertEquals(node, decoded);
    }

    @Test
    public void testEncodeArrayNodeNullElements() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ArrayNode node = new ArrayNode()
                .setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(50L))
                .setElements(null);

        int bytesWritten = codec.encode(node, clockTable, out);

        // Should handle null elements gracefully (0 chunks)
        assertEquals(1, bytesWritten);
        byte[] result = out.toByteArray();
        assertEquals(0xC0, result[0] & 0xFF); // type + length 0
    }

    @Test
    public void testEncodeArrayNodeNullNode() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        assertThrows(IllegalArgumentException.class, () -> codec.encode(null, clockTable, out));
    }

    @Test
    public void testEncodeArrayNodeNullId() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ArrayNode node = new ArrayNode()
                .setElements(new ArrayList<>());

        assertThrows(IllegalArgumentException.class, () -> codec.encode(node, clockTable, out));
    }

    @Test
    public void testGroupIntoChunksNullElements() {
        List<IndexedArrayNodeCodec.Chunk> chunks = codec.groupIntoChunks(null);

        assertEquals(0, chunks.size());
    }

    @Test
    public void testGroupIntoChunksEmptyElements() {
        List<IndexedArrayNodeCodec.Chunk> chunks = codec.groupIntoChunks(new ArrayList<>());

        assertEquals(0, chunks.size());
    }

    @Test
    public void testGroupIntoChunksSingleElement() {
        RGANode element = new RGANode()
                .setNodeId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(10L))
                .setIsDeleted(false);

        List<IndexedArrayNodeCodec.Chunk> chunks = codec.groupIntoChunks(List.of(element));

        assertEquals(1, chunks.size());
        assertEquals(1, chunks.get(0).getSpan());
        assertFalse(chunks.get(0).isDeleted);
    }

    @Test
    public void testGroupIntoChunksSequentialElements() {
        RGANode element1 = new RGANode()
                .setNodeId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(10L))
                .setIsDeleted(false);

        RGANode element2 = new RGANode()
                .setNodeId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(11L))
                .setIsDeleted(false);

        RGANode element3 = new RGANode()
                .setNodeId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(12L))
                .setIsDeleted(false);

        List<IndexedArrayNodeCodec.Chunk> chunks = codec.groupIntoChunks(List.of(element1, element2, element3));

        assertEquals(1, chunks.size());
        assertEquals(3, chunks.get(0).getSpan());
        assertEquals(100L, chunks.get(0).chunkId.getReplicaId());
        assertEquals(10L, chunks.get(0).chunkId.getSequenceNumber());
    }

    @Test
    public void testGroupIntoChunksDeletedElementsNotGrouped() {
        // Deleted elements should each form their own chunk, even if sequential
        RGANode deleted1 = new RGANode()
                .setNodeId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(10L))
                .setIsDeleted(true);

        RGANode deleted2 = new RGANode()
                .setNodeId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(11L))
                .setIsDeleted(true);

        List<IndexedArrayNodeCodec.Chunk> chunks = codec.groupIntoChunks(List.of(deleted1, deleted2));

        assertEquals(2, chunks.size());
        assertTrue(chunks.get(0).isDeleted);
        assertTrue(chunks.get(1).isDeleted);
        assertEquals(1, chunks.get(0).getSpan());
        assertEquals(1, chunks.get(1).getSpan());
    }

    @Test
    public void testGroupIntoChunksMixedDeletedAndNonDeleted() {
        RGANode element1 = new RGANode()
                .setNodeId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(10L))
                .setIsDeleted(false);

        RGANode deleted = new RGANode()
                .setNodeId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(11L))
                .setIsDeleted(true);

        RGANode element2 = new RGANode()
                .setNodeId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(12L))
                .setIsDeleted(false);

        List<IndexedArrayNodeCodec.Chunk> chunks = codec.groupIntoChunks(List.of(element1, deleted, element2));

        assertEquals(3, chunks.size());
        assertFalse(chunks.get(0).isDeleted);
        assertTrue(chunks.get(1).isDeleted);
        assertFalse(chunks.get(2).isDeleted);
    }

    @Test
    public void testGroupIntoChunksDifferentReplicas() {
        RGANode element1 = new RGANode()
                .setNodeId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(10L))
                .setIsDeleted(false);

        RGANode element2 = new RGANode()
                .setNodeId(new LogicalTimestamp().setReplicaId(200L).setSequenceNumber(11L)) // Different replica
                .setIsDeleted(false);

        List<IndexedArrayNodeCodec.Chunk> chunks = codec.groupIntoChunks(List.of(element1, element2));

        assertEquals(2, chunks.size()); // Should not be grouped due to different replicas
    }

    @Test
    public void testGroupIntoChunksSequenceGap() {
        RGANode element1 = new RGANode()
                .setNodeId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(10L))
                .setIsDeleted(false);

        RGANode element2 = new RGANode()
                .setNodeId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(15L)) // Gap (not 11)
                .setIsDeleted(false);

        List<IndexedArrayNodeCodec.Chunk> chunks = codec.groupIntoChunks(List.of(element1, element2));

        assertEquals(2, chunks.size()); // Should not be grouped due to sequence gap
    }

    @Test
    public void testIsSequential() {
        IndexedArrayNodeCodec.Chunk chunk = new IndexedArrayNodeCodec.Chunk(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(10L), false);
        chunk.elements.add(new RGANode().setNodeId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(10L)));

        // call under test - true case
        assertTrue(IndexedArrayNodeCodec.isSequential(chunk, new RGANode().setNodeId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(11L))));

        // call under test - different replica
        assertFalse(IndexedArrayNodeCodec.isSequential(chunk, new RGANode().setNodeId(new LogicalTimestamp().setReplicaId(200L).setSequenceNumber(11L))));

        // call under test - non-sequential sequence number
        assertFalse(IndexedArrayNodeCodec.isSequential(chunk, new RGANode().setNodeId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(12L))));
    }
}
