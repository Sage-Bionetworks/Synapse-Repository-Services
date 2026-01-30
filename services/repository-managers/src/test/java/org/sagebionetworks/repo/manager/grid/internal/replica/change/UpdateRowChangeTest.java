package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class UpdateRowChangeTest {

	@Test
	public void testToAndFromJson() {
		LogicalTimestamp vectorId = new LogicalTimestamp().setReplicaId(123L).setSequenceNumber(45L);
		List<ConValue> rowData = List.of(new ConValue(ConType.STRING, "a"), new ConValue(ConType.STRING, "b"), new ConValue(ConType.STRING,"c"));
		Integer[] rowVectorIndex = new Integer[] { 0, 1, 2 };
		
		UpdateRowChange change = new UpdateRowChange(vectorId, rowData, rowVectorIndex);
		
		JSONObject json = change.toJson();
		
		assertEquals("{\"r\":[123,45],\"d\":[[\"a\"],[\"b\"],[\"c\"]],\"v\":[0,1,2]}", json.toString());
		
		assertEquals(change, new UpdateRowChange(json));
	}

}
