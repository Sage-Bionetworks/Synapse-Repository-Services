package org.sagebionetworks.repo.model.grid.node;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class ArrayNodeTest {

	private LogicalTimestamp arrayId;
	private LogicalTimestamp rgaNodeId1;
	private LogicalTimestamp rgaDataId1;
	private LogicalTimestamp rgaNodeId2;
	private LogicalTimestamp rgaDataId2;
	private LogicalTimestamp rgaNodeId3;
	private LogicalTimestamp rgaDataId3;

	@BeforeEach
	public void before() {
		// Array container ID
		arrayId = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(1L);

		// RGA nodes and their data - ensure nodeId and dataId are never the same
		rgaNodeId1 = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L);
		rgaDataId1 = new LogicalTimestamp().setReplicaId(2L).setSequenceNumber(3L);

		rgaNodeId2 = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(4L);
		rgaDataId2 = new LogicalTimestamp().setReplicaId(2L).setSequenceNumber(5L);

		rgaNodeId3 = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(6L);
		rgaDataId3 = new LogicalTimestamp().setReplicaId(2L).setSequenceNumber(7L);
	}

	@Test
	public void testStreamReferencedTimestampsWithElements() {
		ArrayNode array = new ArrayNode().setId(arrayId);
		List<RGANode> elements = new ArrayList<>();

		// First element: containerId = array ID, refId = null (no predecessor)
		elements.add(new RGANode()
				.setContainerId(arrayId)
				.setNodeId(rgaNodeId1)
				.setDataId(rgaDataId1)
				.setReferenceNodeId(null));

		// Second element: containerId = array ID, refId = previous node ID
		elements.add(new RGANode()
				.setContainerId(arrayId)
				.setNodeId(rgaNodeId2)
				.setDataId(rgaDataId2)
				.setReferenceNodeId(rgaNodeId1));

		// Third element: containerId = array ID, refId = previous node ID
		elements.add(new RGANode()
				.setContainerId(arrayId)
				.setNodeId(rgaNodeId3)
				.setDataId(rgaDataId3)
				.setReferenceNodeId(rgaNodeId2));

		array.setElements(elements);

		// call under test
		List<LogicalTimestamp> timestamps = array.streamReferencedTimestamps().collect(Collectors.toList());

		// Should include: arrayId, rgaNodeId1, rgaDataId1, rgaNodeId2, rgaDataId2, rgaNodeId3, rgaDataId3
		// Note: containerId and refId are NOT included as they reference the container and previous nodes
		assertEquals(7, timestamps.size());
		assertEquals(arrayId, timestamps.get(0)); // array node ID first
		assertEquals(rgaNodeId1, timestamps.get(1));
		assertEquals(rgaDataId1, timestamps.get(2));
		assertEquals(rgaNodeId2, timestamps.get(3));
		assertEquals(rgaDataId2, timestamps.get(4));
		assertEquals(rgaNodeId3, timestamps.get(5));
		assertEquals(rgaDataId3, timestamps.get(6));
	}

	@Test
	public void testStreamReferencedTimestampsWithNullElements() {
		ArrayNode array = new ArrayNode().setId(arrayId).setElements(null);

		// call under test
		List<LogicalTimestamp> timestamps = array.streamReferencedTimestamps().collect(Collectors.toList());

		assertEquals(1, timestamps.size());
		assertEquals(arrayId, timestamps.get(0)); // only array node ID
	}

	@Test
	public void testStreamReferencedTimestampsWithEmptyElements() {
		ArrayNode array = new ArrayNode().setId(arrayId).setElements(new ArrayList<>());

		// call under test
		List<LogicalTimestamp> timestamps = array.streamReferencedTimestamps().collect(Collectors.toList());

		assertEquals(1, timestamps.size());
		assertEquals(arrayId, timestamps.get(0)); // only array node ID
	}
}
