package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class UpdateRowChangeTest {

	@Test
	public void testToAndFromJson() {
		LogicalTimestamp vectorId = new LogicalTimestamp().setReplicaId(123L).setSequenceNumber(45L);
		JSONArray rowData = new JSONArray().put("a").put("b").put("c");
		Integer[] rowVectorIndex = new Integer[] { 0, 1, 2 };
		
		UpdateRowChange change = new UpdateRowChange(vectorId, rowData, rowVectorIndex);
		
		JSONObject json = change.toJson();
		
		assertEquals("{\"r\":[123,45],\"d\":[\"a\",\"b\",\"c\"],\"v\":[0,1,2]}", json.toString());
		
		assertEquals(change, new UpdateRowChange(json));
	}

}
