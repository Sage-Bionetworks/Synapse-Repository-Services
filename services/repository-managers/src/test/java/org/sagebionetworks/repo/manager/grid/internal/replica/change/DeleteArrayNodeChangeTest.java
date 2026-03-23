package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class DeleteArrayNodeChangeTest {

	@Test
	public void testToAndFromJson() {
		LogicalTimestamp arrId = new LogicalTimestamp().setReplicaId(123L).setSequenceNumber(1L);
		LogicalTimestamp nodeId = new LogicalTimestamp().setReplicaId(123L).setSequenceNumber(45L);

		DeleteArrayNodeChange change = new DeleteArrayNodeChange(arrId, nodeId);

		JSONObject json = change.toJson();

		assertEquals("{\"a\":[123,1],\"r\":[123,45]}", json.toString());

		assertEquals(change, new DeleteArrayNodeChange(json));
	}
}
