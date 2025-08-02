package org.sagebionetworks.repo.model.grid.patch.compact;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;

import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertVector;

public class InsertVectorSerializableTest {

	private LogicalTimestamp operationId;
	private InsertVectorSerializable serializable;

	@BeforeEach
	public void before() {
		operationId = new LogicalTimestamp().setReplicaId(12L).setSequenceNumber(8L);
		serializable = new InsertVectorSerializable();
	}

	@Test
	public void testRoundTrip() {
		String json = "[11,[1,2],[[2,[3,4]],[0,[5,6]]]]";

		// call under test
		InsertVector obj = serializable.deserialize(operationId, new JSONArray(json));
		LinkedHashMap<Integer, LogicalTimestamp> map = new LinkedHashMap<>();
		map.put(2, new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L));
		map.put(0, new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(6L));
		InsertVector expected = new InsertVector(
				operationId,
				new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L),
				map
		);
		assertEquals(expected, obj);

		// call under test
		String back = serializable.serialize(obj).toString();
		assertEquals(json, back);
	}

	@Test
	public void testRoundTripWithVectorIdMatchsReplicaId() {
		String json = "[11,2,[[2,[3,4]],[0,[5,6]]]]";

		// call under test
		InsertVector obj = serializable.deserialize(operationId, new JSONArray(json));
		LinkedHashMap<Integer, LogicalTimestamp> map = new LinkedHashMap<>();
		map.put(2, new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L));
		map.put(0, new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(6L));
		InsertVector expected = new InsertVector(
				operationId,
				new LogicalTimestamp().setReplicaId(12L).setSequenceNumber(2L),
				map
		);
		assertEquals(expected, obj);

		// call under test
		String back = serializable.serialize(obj).toString();
		assertEquals(json, back);
	}
	
	@Test
	public void testRoundTripWithVectorIdValueIdMatchsReplica() {
		String json = "[11,2,[[2,4],[0,[5,6]]]]";

		// call under test
		InsertVector obj = serializable.deserialize(operationId, new JSONArray(json));
		LinkedHashMap<Integer, LogicalTimestamp> map = new LinkedHashMap<>();
		map.put(2, new LogicalTimestamp().setReplicaId(12L).setSequenceNumber(4L));
		map.put(0, new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(6L));
		InsertVector expected = new InsertVector(
				operationId,
				new LogicalTimestamp().setReplicaId(12L).setSequenceNumber(2L),
				map
		);
		assertEquals(expected, obj);

		// call under test
		String back = serializable.serialize(obj).toString();
		assertEquals(json, back);
	}
	
	@Test
	public void testRoundTripWithAllMatchsReplica() {
		String json = "[11,2,[[2,4],[0,6]]]";

		// call under test
		InsertVector obj = serializable.deserialize(operationId, new JSONArray(json));
		LinkedHashMap<Integer, LogicalTimestamp> map = new LinkedHashMap<>();
		map.put(2, new LogicalTimestamp().setReplicaId(12L).setSequenceNumber(4L));
		map.put(0, new LogicalTimestamp().setReplicaId(12L).setSequenceNumber(6L));
		InsertVector expected = new InsertVector(
				operationId,
				new LogicalTimestamp().setReplicaId(12L).setSequenceNumber(2L),
				map
		);
		assertEquals(expected, obj);

		// call under test
		String back = serializable.serialize(obj).toString();
		assertEquals(json, back);
	}
}
