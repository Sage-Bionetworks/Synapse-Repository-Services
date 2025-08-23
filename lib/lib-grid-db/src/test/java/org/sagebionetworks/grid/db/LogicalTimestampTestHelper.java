package org.sagebionetworks.grid.db;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class LogicalTimestampTestHelper {

	/**
	 * Helper to create a set of unique ids.
	 * 
	 * @param count
	 * @return
	 */
	public static List<LogicalTimestamp> createIds(int count) {
		List<LogicalTimestamp> ids = new ArrayList<>();
		for (long i = 0; i < count * 2; i += 2) {
			ids.add(new LogicalTimestamp().setReplicaId(i).setSequenceNumber(i + 1L));
		}
		return ids;
	}
}
