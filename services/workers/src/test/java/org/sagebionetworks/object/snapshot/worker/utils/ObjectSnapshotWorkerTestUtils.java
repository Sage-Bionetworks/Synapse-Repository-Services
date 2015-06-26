package org.sagebionetworks.object.snapshot.worker.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.repo.model.audit.ObjectRecord;

public class ObjectSnapshotWorkerTestUtils {
	

	private static final int TIME_OUT = 2 * 60 * 1000;

	/**
	 * Helper method that keep looking for new ObjectRecord files in S3.
	 * 
	 * @return true if found the expectedObjectRecords in TIME_OUT milliseconds,
	 *         false otherwise.
	 */
	public static boolean waitForObjects(Set<String> oldKeys, List<ObjectRecord> expectedRecords, ObjectRecordDAO objectRecordDAO) throws Exception {
		long start = System.currentTimeMillis();
		while (System.currentTimeMillis() < start + TIME_OUT) {
			Set<String> newKeys = objectRecordDAO.listAllKeys();
			newKeys.removeAll(oldKeys);
			if (newKeys.size() != 0) {
				return findRecords(expectedRecords, newKeys, objectRecordDAO);
			}

			// wait for 1 second before calling the service again
			Thread.sleep(1000);
		}
		return false;
	}

	private static boolean findRecords(List<ObjectRecord> expectedRecords, Set<String> newKeys, ObjectRecordDAO objectRecordDAO) throws IOException {
		List<ObjectRecord> newRecords = new ArrayList<ObjectRecord>();
		for (String key : newKeys) {
			 newRecords.addAll(objectRecordDAO.getBatch(key));
		}
		return compareRecords(newRecords, expectedRecords);
	}

	private static boolean compareRecords(List<ObjectRecord> actualRecords, List<ObjectRecord> expectedRecords) {
		for (ObjectRecord record: actualRecords) {
			record.setChangeNumber(null);
			record.setTimestamp(null);
		}
		for (ObjectRecord record : expectedRecords) {
			if (!actualRecords.contains(record)) {
				return false;
			}
		}
		return true;
	}
}
