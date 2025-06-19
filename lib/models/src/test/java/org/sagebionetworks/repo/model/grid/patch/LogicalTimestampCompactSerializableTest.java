package org.sagebionetworks.repo.model.grid.patch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.patch.compact.LogicalTimestampCompactSerializable;

public class LogicalTimestampCompactSerializableTest {

	@Test
	public void testDeserializeAndSeserialize() {
		LogicalTimestamp ts = new LogicalTimestamp().setReplicaId(1l).setSequenceNumber(2L);
		// call under test
		JSONArray array = LogicalTimestampCompactSerializable.serialize(ts);
		// call under test
		LogicalTimestamp back = LogicalTimestampCompactSerializable.deserialize(array);
		assertEquals(ts, back);
	}

	@Test
	public void testSerializeClock() {
		List<LogicalTimestamp> clock = Arrays.asList(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(22L),
				new LogicalTimestamp().setReplicaId(99L).setSequenceNumber(34L));
		// call under test
		JSONArray serialized = LogicalTimestampCompactSerializable.serializeClock(clock);
		// call under test
		List<LogicalTimestamp> back = LogicalTimestampCompactSerializable.deserializeClock(serialized);
		assertEquals(clock, back);
	}
	
	@Test
	public void testDeserializeAndSerializeWithReplicaIdMatch() {
		Long replicaId = 1L;
		LogicalTimestamp ts = new LogicalTimestamp().setReplicaId(1l).setSequenceNumber(2L);
		// call under test
		Object value = LogicalTimestampCompactSerializable.serialize(replicaId, ts);
		assertEquals("2", value.toString());
		JSONArray array = new JSONArray().put(value);
		// call under test
		LogicalTimestamp back = LogicalTimestampCompactSerializable.deserialize(replicaId, array, 0);
		assertEquals(ts, back);
	}
	
	@Test
	public void testDeserializeAndSerializeWithReplicaIdNoMatch() {
		Long replicaId = 9L;
		LogicalTimestamp ts = new LogicalTimestamp().setReplicaId(1l).setSequenceNumber(2L);
		// call under test
		Object value = LogicalTimestampCompactSerializable.serialize(replicaId, ts);
		assertEquals("[1,2]", value.toString());
		JSONArray array = new JSONArray().put(value);
		// call under test
		LogicalTimestamp back = LogicalTimestampCompactSerializable.deserialize(replicaId, array, 0);
		assertEquals(ts, back);
	}

}
