package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.SynapseRow;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class InsertRowChangeTest {

	@Test
	public void testToAndFromJson() {
		LogicalTimestamp arrId = new LogicalTimestamp().setReplicaId(123L).setSequenceNumber(1L);
		LogicalTimestamp nodeId = new LogicalTimestamp().setReplicaId(123L).setSequenceNumber(45L);
		List<ConValue> rowData = List.of(new ConValue(ConType.STRING, "a"), new ConValue(ConType.STRING, "b"), new ConValue(ConType.STRING,"c"));
		Integer[] rowVectorIndex = new Integer[] { 0, 1, 2 };
		
		InsertRowChange change = new InsertRowChange(arrId, nodeId, rowData, rowVectorIndex);
		
		JSONObject json = change.toJson();
		
		assertEquals("{\"a\":[123,1],\"n\":[123,45],\"d\":[[\"a\"],[\"b\"],[\"c\"]],\"v\":[0,1,2]}", json.toString());
		
		assertEquals(change, new InsertRowChange(json));
	}
	
	@Test
	public void testToAndFromJsonWithNoNodeId() {
		LogicalTimestamp arrId = new LogicalTimestamp().setReplicaId(123L).setSequenceNumber(1L);
		LogicalTimestamp nodeId = null;
		List<ConValue> rowData = List.of(new ConValue(ConType.STRING, "a"), new ConValue(ConType.STRING, "b"), new ConValue(ConType.STRING,"c"));
		Integer[] rowVectorIndex = new Integer[] { 0, 1, 2 };
		
		InsertRowChange change = new InsertRowChange(arrId, nodeId, rowData, rowVectorIndex);
		
		JSONObject json = change.toJson();
		
		assertEquals("{\"a\":[123,1],\"d\":[[\"a\"],[\"b\"],[\"c\"]],\"v\":[0,1,2]}", json.toString());
		
		assertEquals(change, new InsertRowChange(json));
	}
	
	@Test
	public void testToAndFromJsonWithSynapseRow() {
		LogicalTimestamp arrId = new LogicalTimestamp().setReplicaId(123L).setSequenceNumber(1L);
		LogicalTimestamp nodeId = new LogicalTimestamp().setReplicaId(123L).setSequenceNumber(45L);
		List<ConValue> rowData = List.of(new ConValue(ConType.STRING, "a"), new ConValue(ConType.STRING, "b"), new ConValue(ConType.STRING,"c"));
		Integer[] rowVectorIndex = new Integer[] { 0, 1, 2 };
		SynapseRow sr = new SynapseRow().setRowId(1L).setVersionNumber(0L).setEtag("e1");
		
		InsertRowChange change = new InsertRowChange(arrId, nodeId, rowData, rowVectorIndex, sr);
		
		JSONObject json = change.toJson();
		
		assertEquals("{\"a\":[123,1],\"n\":[123,45],\"d\":[[\"a\"],[\"b\"],[\"c\"]],\"v\":[0,1,2],\"s\":[1,0,\"e1\"]}", json.toString());
		
		assertEquals(change, new InsertRowChange(json));
	}

}
