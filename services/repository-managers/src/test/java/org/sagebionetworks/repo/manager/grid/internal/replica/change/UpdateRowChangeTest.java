package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.SynapseRow;
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
		assertEquals(Optional.empty(), change.getMetadataObjectId());
		assertEquals(Optional.empty(), change.getSynapseRow());
		
		JSONObject json = change.toJson();
		
		assertEquals("{\"r\":[123,45],\"d\":[[\"a\"],[\"b\"],[\"c\"]],\"v\":[0,1,2]}", json.toString());
		
		assertEquals(change, new UpdateRowChange(json));
	}
	
	
	@Test
	public void testToAndFromJsonWithSynRow() {
		LogicalTimestamp vectorId = new LogicalTimestamp().setReplicaId(123L).setSequenceNumber(45L);
		List<ConValue> rowData = List.of(new ConValue(ConType.STRING, "a"), new ConValue(ConType.STRING, "b"), new ConValue(ConType.STRING,"c"));
		Integer[] rowVectorIndex = new Integer[] { 0, 1, 2 };
		SynapseRow sr = new SynapseRow().setRowId(1L).setVersionNumber(0L).setEtag("e1");
		LogicalTimestamp metadataObjectId = new LogicalTimestamp().setReplicaId(44L).setSequenceNumber(55L);
		
		UpdateRowChange change = new UpdateRowChange(vectorId, rowData, rowVectorIndex, metadataObjectId, sr.toConValue());
		assertEquals(Optional.of(metadataObjectId), change.getMetadataObjectId());
		assertEquals(Optional.of(sr.toConValue()), change.getSynapseRow());
		
		JSONObject json = change.toJson();
		
		assertEquals("{\"r\":[123,45],\"d\":[[\"a\"],[\"b\"],[\"c\"]],\"v\":[0,1,2],\"m\":[44,55],\"s\":[[1,0,\"e1\"]]}", json.toString());
		
		assertEquals(change, new UpdateRowChange(json));
	}

}
