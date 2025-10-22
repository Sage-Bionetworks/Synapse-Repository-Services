package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class InsertRowChangeTest {

	@Test
	public void testToAndFromJson() {
		LogicalTimestamp arrId = new LogicalTimestamp().setReplicaId(123L).setSequenceNumber(1L);
		LogicalTimestamp nodeId = new LogicalTimestamp().setReplicaId(123L).setSequenceNumber(45L);
		JSONArray rowData = new JSONArray().put("a").put("b").put("c");
		Integer[] rowVectorIndex = new Integer[] { 0, 1, 2 };
		
		InsertRowChange change = new InsertRowChange(arrId, nodeId, rowData, rowVectorIndex);
		
		JSONObject json = change.toJson();
		
		assertEquals("{\"a\":[123,1],\"n\":[123,45],\"d\":[\"a\",\"b\",\"c\"],\"v\":[0,1,2]}", json.toString());
		
		assertEquals(change, new InsertRowChange(json));
	}
	
	@Test
	public void testToAndFromJsonWithNoNodeId() {
		LogicalTimestamp arrId = new LogicalTimestamp().setReplicaId(123L).setSequenceNumber(1L);
		LogicalTimestamp nodeId = null;
		JSONArray rowData = new JSONArray().put("a").put("b").put("c");
		Integer[] rowVectorIndex = new Integer[] { 0, 1, 2 };
		
		InsertRowChange change = new InsertRowChange(arrId, nodeId, rowData, rowVectorIndex);
		
		JSONObject json = change.toJson();
		
		assertEquals("{\"a\":[123,1],\"d\":[\"a\",\"b\",\"c\"],\"v\":[0,1,2]}", json.toString());
		
		assertEquals(change, new InsertRowChange(json));
	}

}
