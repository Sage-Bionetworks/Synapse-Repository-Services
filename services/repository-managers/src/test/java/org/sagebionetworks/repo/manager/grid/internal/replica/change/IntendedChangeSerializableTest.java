package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class IntendedChangeSerializableTest {

	private List<IntendedChange> changes;
	private String sessionId;
	private Long replicaId;
	private String connectionId;
	private IntendedChangeSet set;

	@BeforeEach
	public void before() {
		changes = List.of(
				new UpdateMetadataChange()
						.setRowObjectId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L)),
				new UpdateMetadataChange()
						.setRowMetadataId(new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L)));

		sessionId = "session123";
		replicaId = 111L;
		connectionId = "con44";
		set = new IntendedChangeSet().setChanges(changes).setSessionId(sessionId).setReplicaId(replicaId)
				.setConnectionId(connectionId);

	}

	@Test
	public void testSerializeDeserialize() {
		// call under test
		JSONArray json = IntendedChangeSerializable.serialize(changes);
		assertEquals("[[0,{\"o\":[1,2]}],[0,{\"m\":[3,4]}]]", json.toString());
		// call under test
		List<IntendedChange> clone = IntendedChangeSerializable.deserialize(json);
		assertEquals(changes, clone);

	}

	@Test
	public void testSerializeDeserializeSet() {
		// call under test
		JSONObject json = IntendedChangeSerializable.serialize(set);
		assertEquals(
				"{\"con\":\"con44\",\"ses\":\"session123\",\"rep\":111,\"set\":[[0,{\"o\":[1,2]}],[0,{\"m\":[3,4]}]]}",
				json.toString());
		// call under test
		IntendedChangeSet clone = IntendedChangeSerializable.deserialize(json);
		assertEquals(set, clone);
	}

}
