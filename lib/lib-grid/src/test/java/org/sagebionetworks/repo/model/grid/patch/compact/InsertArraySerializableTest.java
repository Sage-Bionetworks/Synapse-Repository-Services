package org.sagebionetworks.repo.model.grid.patch.compact;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertArray;

public class InsertArraySerializableTest {

	private LogicalTimestamp operationId;
	private InsertArraySerializable serializable;

	@BeforeEach
	public void before() {
		operationId = new LogicalTimestamp().setReplicaId(12L).setSequenceNumber(8L);
		serializable = new InsertArraySerializable();
	}

	@Test
	public void testRoundTrip() {
		String json = "[14,[1,2],[3,4],[[5,6],[7,8]]]";

		// call under test
		InsertArray in = serializable.deserialize(operationId, new JSONArray(json));
		List<LogicalTimestamp> elements = Arrays.asList(new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(6L),
				new LogicalTimestamp().setReplicaId(7L).setSequenceNumber(8L));
		InsertArray expected = new InsertArray().setOperationId(operationId)
				.setArrayId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L))
				.setReferenceId(new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L)).setElementIds(elements);
		assertEquals(expected, in);

		// call under test
		String back = serializable.serialize(in).toString();
		assertEquals(json, back);
	}
	
	@Test
	public void testRoundTripWithArrayMatchesReplicaId() {
		String json = "[14,2,[3,4],[[5,6],[7,8]]]";

		// call under test
		InsertArray in = serializable.deserialize(operationId, new JSONArray(json));
		List<LogicalTimestamp> elements = Arrays.asList(new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(6L),
				new LogicalTimestamp().setReplicaId(7L).setSequenceNumber(8L));
		InsertArray expected = new InsertArray().setOperationId(operationId)
				.setArrayId(new LogicalTimestamp().setReplicaId(12L).setSequenceNumber(2L))
				.setReferenceId(new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L)).setElementIds(elements);
		assertEquals(expected, in);

		// call under test
		String back = serializable.serialize(in).toString();
		assertEquals(json, back);
	}
	
	@Test
	public void testRoundTripWithReferenceMatchesReplicaId() {
		String json = "[14,2,4,[[5,6],[7,8]]]";

		// call under test
		InsertArray in = serializable.deserialize(operationId, new JSONArray(json));
		List<LogicalTimestamp> elements = Arrays.asList(new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(6L),
				new LogicalTimestamp().setReplicaId(7L).setSequenceNumber(8L));
		InsertArray expected = new InsertArray().setOperationId(operationId)
				.setArrayId(new LogicalTimestamp().setReplicaId(12L).setSequenceNumber(2L))
				.setReferenceId(new LogicalTimestamp().setReplicaId(12L).setSequenceNumber(4L)).setElementIds(elements);
		assertEquals(expected, in);

		// call under test
		String back = serializable.serialize(in).toString();
		assertEquals(json, back);
	}
	
	@Test
	public void testRoundTripWithElementsMatchesReplicaId() {
		String json = "[14,2,4,[6,8]]";

		// call under test
		InsertArray in = serializable.deserialize(operationId, new JSONArray(json));
		List<LogicalTimestamp> elements = Arrays.asList(new LogicalTimestamp().setReplicaId(12L).setSequenceNumber(6L),
				new LogicalTimestamp().setReplicaId(12L).setSequenceNumber(8L));
		InsertArray expected = new InsertArray().setOperationId(operationId)
				.setArrayId(new LogicalTimestamp().setReplicaId(12L).setSequenceNumber(2L))
				.setReferenceId(new LogicalTimestamp().setReplicaId(12L).setSequenceNumber(4L)).setElementIds(elements);
		assertEquals(expected, in);

		// call under test
		String back = serializable.serialize(in).toString();
		assertEquals(json, back);
	}
}
