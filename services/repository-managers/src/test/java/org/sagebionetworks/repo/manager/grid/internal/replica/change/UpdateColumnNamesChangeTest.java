package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class UpdateColumnNamesChangeTest {

	@Test
	public void testToAndFromJson() {
		LogicalTimestamp vectorId = new LogicalTimestamp().setReplicaId(123L).setSequenceNumber(1L);
		Map<Integer, String> indexToNameMap = Map.of(0, "col1", 1, "col2", 2, "col3");

		UpdateColumnNamesChange change = new UpdateColumnNamesChange(vectorId, indexToNameMap);

		JSONObject json = change.toJson();

		assertEquals("{\"v\":[123,1],\"m\":{\"0\":[\"col1\"],\"1\":[\"col2\"],\"2\":[\"col3\"]}}", json.toString());

		assertEquals(change, new UpdateColumnNamesChange(json));
	}

	@Test
	public void testToAndFromJsonWithEmptyMap() {
		LogicalTimestamp vectorId = new LogicalTimestamp().setReplicaId(123L).setSequenceNumber(1L);
		Map<Integer, String> indexToNameMap = Map.of();

		UpdateColumnNamesChange change = new UpdateColumnNamesChange(vectorId, indexToNameMap);

		JSONObject json = change.toJson();

		assertEquals("{\"v\":[123,1],\"m\":{}}", json.toString());

		assertEquals(change, new UpdateColumnNamesChange(json));
	}

	@Test
	public void testToAndFromJsonWithSingleEntry() {
		LogicalTimestamp vectorId = new LogicalTimestamp().setReplicaId(456L).setSequenceNumber(10L);
		Map<Integer, String> indexToNameMap = Map.of(5, "singleColumn");

		UpdateColumnNamesChange change = new UpdateColumnNamesChange(vectorId, indexToNameMap);

		JSONObject json = change.toJson();

		assertEquals("{\"v\":[456,10],\"m\":{\"5\":[\"singleColumn\"]}}", json.toString());

		assertEquals(change, new UpdateColumnNamesChange(json));
	}

}
