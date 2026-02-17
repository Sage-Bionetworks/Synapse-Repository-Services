package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class AddColumnChangeTest {

	@Test
	public void testToAndFromJson() {
		LogicalTimestamp arrId = new LogicalTimestamp().setReplicaId(123L).setSequenceNumber(1L);
		LogicalTimestamp nodeId = new LogicalTimestamp().setReplicaId(123L).setSequenceNumber(45L);
		Long vectorIndex = 5L;

		AddColumnChange change = new AddColumnChange(arrId, nodeId, vectorIndex);

		JSONObject json = change.toJson();

		assertEquals("{\"c\":[123,1],\"a\":[123,45],\"i\":[5]}", json.toString());

		assertEquals(change, new AddColumnChange(json));
	}

	@Test
	public void testToAndFromJsonWithDifferentReplicaIds() {
		LogicalTimestamp arrId = new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(10L);
		LogicalTimestamp nodeId = new LogicalTimestamp().setReplicaId(200L).setSequenceNumber(20L);
		Long vectorIndex = 0L;

		AddColumnChange change = new AddColumnChange(arrId, nodeId, vectorIndex);

		JSONObject json = change.toJson();

		assertEquals("{\"c\":[100,10],\"a\":[200,20],\"i\":[0]}", json.toString());

		assertEquals(change, new AddColumnChange(json));
	}

}
