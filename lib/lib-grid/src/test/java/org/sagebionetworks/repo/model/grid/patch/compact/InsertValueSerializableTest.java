package org.sagebionetworks.repo.model.grid.patch.compact;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertValue;

public class InsertValueSerializableTest {

	private LogicalTimestamp id;
	private InsertValueSerializable serializable;

	@BeforeEach
	public void before() {
		id = new LogicalTimestamp().setReplicaId(12L).setSequenceNumber(8L);
		serializable = new InsertValueSerializable();
	}

	@Test
	public void testRoundTrip() {
		String json = "[9,[1,2],[3,4]]";

		// call under test
		InsertValue val = serializable.deserialize(id, new JSONArray(json));
		InsertValue expected = new InsertValue(
				id,
				new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L),
				new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L)
		);
		assertEquals(expected, val);

		// call under test
		String back = serializable.serialize(val).toString();
		assertEquals(json, back);
	}

	@Test
	public void testRoundTripWithRefMatchesRepica() {
		String json = "[9,2,[3,4]]";

		// call under test
		InsertValue val = serializable.deserialize(id, new JSONArray(json));
		InsertValue expected = new InsertValue(
				id,
				new LogicalTimestamp().setReplicaId(12L).setSequenceNumber(2L),
				new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L)
		);
		assertEquals(expected, val);

		// call under test
		String back = serializable.serialize(val).toString();
		assertEquals(json, back);
	}

	@Test
	public void testRoundTripWithValueMatchesReplica() {
		String json = "[9,[1,2],4]";

		// call under test
		InsertValue val = serializable.deserialize(id, new JSONArray(json));
		InsertValue expected = new InsertValue(
				id,
				new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L),
				new LogicalTimestamp().setReplicaId(12L).setSequenceNumber(4L)
		);
		assertEquals(expected, val);

		// call under test
		String back = serializable.serialize(val).toString();
		assertEquals(json, back);
	}

	@Test
	public void testRoundTripWithValueBothMatchesReplica() {
		String json = "[9,2,4]";

		// call under test
		InsertValue val = serializable.deserialize(id, new JSONArray(json));
		InsertValue expected = new InsertValue(
				id,
				new LogicalTimestamp().setReplicaId(12L).setSequenceNumber(2L),
				new LogicalTimestamp().setReplicaId(12L).setSequenceNumber(4L)
		);
		assertEquals(expected, val);

		// call under test
		String back = serializable.serialize(val).toString();
		assertEquals(json, back);
	}

}
