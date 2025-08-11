package org.sagebionetworks.repo.model.grid.patch.compact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertObject;

public class InsertObjectSerializableTest {

	private LogicalTimestamp operationId;
	private InsertObjectSerializable serializable;

	@BeforeEach
	public void before() {
		operationId = new LogicalTimestamp().setReplicaId(12L).setSequenceNumber(8L);
		serializable = new InsertObjectSerializable();
	}

	@Test
	public void testRoundTrip() {
		String json = "[10,[1,2],[[\"a\",[3,4]],[\"b\",[5,6]]]]";

		// call under test
		InsertObject obj = serializable.deserialize(operationId, new JSONArray(json));
		LinkedHashMap<String, LogicalTimestamp> map = new LinkedHashMap<>();
		map.put("a", new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L));
		map.put("b", new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(6L));
		InsertObject expected = new InsertObject(
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
	public void testRoundTripWithObjectMatchsReplica() {
		String json = "[10,2,[[\"a\",[3,4]],[\"b\",[5,6]]]]";

		// call under test
		InsertObject obj = serializable.deserialize(operationId, new JSONArray(json));
		LinkedHashMap<String, LogicalTimestamp> map = new LinkedHashMap<>();
		map.put("a", new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L));
		map.put("b", new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(6L));
		InsertObject expected = new InsertObject(
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
	public void testRoundTripWithObjectOneValueMatchesReplica() {
		String json = "[10,2,[[\"a\",4],[\"b\",[5,6]]]]";

		// call under test
		InsertObject obj = serializable.deserialize(operationId, new JSONArray(json));
		LinkedHashMap<String, LogicalTimestamp> map = new LinkedHashMap<>();
		map.put("a", new LogicalTimestamp().setReplicaId(12L).setSequenceNumber(4L));
		map.put("b", new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(6L));
		InsertObject expected = new InsertObject(
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
	public void testRoundTripWithObjectAllValueMatchesReplica() {
		String json = "[10,2,[[\"a\",4],[\"b\",6]]]";

		// call under test
		InsertObject obj = serializable.deserialize(operationId, new JSONArray(json));
		LinkedHashMap<String, LogicalTimestamp> map = new LinkedHashMap<>();
		map.put("a", new LogicalTimestamp().setReplicaId(12L).setSequenceNumber(4L));
		map.put("b", new LogicalTimestamp().setReplicaId(12L).setSequenceNumber(6L));
		InsertObject expected = new InsertObject(
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
