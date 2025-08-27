package org.sagebionetworks.repo.model.grid.patch.compact;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.Timespan;
import org.sagebionetworks.repo.model.grid.patch.operation.Delete;

public class DeleteSerializableTest {

	private LogicalTimestamp operationId;
	private DeleteSerializable serializable;

	@BeforeEach
	public void before() {
		operationId = new LogicalTimestamp().setReplicaId(123L).setSequenceNumber(13L);
		serializable = new DeleteSerializable();
	}

	@Test
	public void testRoundTrip() {
		String json = "[16,[456,2],[[789,13,1]]]";

		// Call under test
		Delete operation = serializable.deserialize(operationId, new JSONArray(json));
		
		assertEquals(new Delete(
			operationId, 
			new LogicalTimestamp().setReplicaId(456L).setSequenceNumber(2L),
			List.of(
				new Timespan(new LogicalTimestamp().setReplicaId(789L).setSequenceNumber(13L), 1L)
			))
		, operation);
		
		// Call under test
		assertEquals(json, serializable.serialize(operation).toString());
	}
	
	@Test
	public void testRoundTripWithNodeReplicaIdMatching() {
		String json = "[16,2,[[789,13,1]]]";

		// Call under test
		Delete operation = serializable.deserialize(operationId, new JSONArray(json));
		
		assertEquals(new Delete(
			operationId, 
			new LogicalTimestamp().setReplicaId(123L).setSequenceNumber(2L),
			List.of(
				new Timespan(new LogicalTimestamp().setReplicaId(789L).setSequenceNumber(13L), 1L)
			))
		, operation);
		
		// Call under test
		assertEquals(json, serializable.serialize(operation).toString());
	}
	
	@Test
	public void testRoundTripWithTimespanReplicaIdMatching() {
		String json = "[16,[456,2],[[13,1]]]";

		// Call under test
		Delete operation = serializable.deserialize(operationId, new JSONArray(json));
		
		assertEquals(new Delete(
			operationId, 
			new LogicalTimestamp().setReplicaId(456L).setSequenceNumber(2L),
			List.of(
				new Timespan(new LogicalTimestamp().setReplicaId(123L).setSequenceNumber(13L), 1L)
			))
		, operation);
		
		// Call under test
		assertEquals(json, serializable.serialize(operation).toString());
	}
}
