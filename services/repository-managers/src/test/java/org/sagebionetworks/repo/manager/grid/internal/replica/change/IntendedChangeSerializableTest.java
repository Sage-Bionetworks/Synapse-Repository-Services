package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class IntendedChangeSerializableTest {

	private List<IntendedChange> changes;
	private String sessionId;
	private Long replicaId;
	private String connectionId;
	private Long clockMax;
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
		clockMax = 921L;
		set = new IntendedChangeSet().setChanges(changes).setSessionId(sessionId).setReplicaId(replicaId)
				.setConnectionId(connectionId).setClockSequenceMaximum(clockMax);

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
				"{\"con\":\"con44\",\"ses\":\"session123\",\"rep\":111,\"max\":921,\"set\":[[0,{\"o\":[1,2]}],[0,{\"m\":[3,4]}]]}",
				json.toString());
		// call under test
		IntendedChangeSet clone = IntendedChangeSerializable.deserialize(json);
		assertEquals(set, clone);
	}
	
	@ParameterizedTest
	@MethodSource("provideIntendedChanges")
	public void testRoundTripForEachChangeType(IntendedChange change) {
	    List<IntendedChange> changes = List.of(change);
	    
	    // Serialize
	    JSONArray json = IntendedChangeSerializable.serialize(changes);
	    
	    // Deserialize
	    List<IntendedChange> deserialized = IntendedChangeSerializable.deserialize(json);
	    
	    // Verify
	    assertEquals(changes, deserialized);
	}

	private static Stream<IntendedChange> provideIntendedChanges() {
	    return Stream.of(
	        new UpdateMetadataChange()
	            .setRowObjectId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L)),
	        
	        new UpdateMetadataChange()
	            .setRowMetadataId(new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(4L)),
	        
	        new InsertRowChange(
	            new LogicalTimestamp().setReplicaId(123L).setSequenceNumber(1L),
	            new LogicalTimestamp().setReplicaId(123L).setSequenceNumber(45L),
	            List.of(new ConValue(ConType.STRING, "a"), new ConValue(ConType.STRING, "b")),
	            new Integer[]{0, 1}
	        ),
	        
	        new DeleteArrayNodeChange(
	            new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(10L),
	            new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(20L)
	        ),
	        
	        new AddColumnChange(
	            new LogicalTimestamp().setReplicaId(50L).setSequenceNumber(5L),
	            new LogicalTimestamp().setReplicaId(50L).setSequenceNumber(15L),
	            100L
	        ),
	        
	        new UpdateColumnNamesChange(
	            new LogicalTimestamp().setReplicaId(60L).setSequenceNumber(6L),
	            Map.of(0, "col1", 1, "col2")
	        )
	    );
	}

}
