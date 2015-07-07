package org.sagebionetworks.object.snapshot.worker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.repo.model.audit.ObjectRecord;

public class ObjectSnapshotWorkerIntegrationTestUtils {
	
	public static final int TIME_OUT = 2 * 60 * 1000;

	/**
	 * Helper method that keep looking for new ObjectRecord files in S3.
	 * 
	 * @return true if found the expectedObjectRecords in TIME_OUT milliseconds,
	 *         false otherwise.
	 */
	public static boolean waitForObjects(Set<String> oldKeys, List<ObjectRecord> expectedRecords, ObjectRecordDAO objectRecordDAO, String type) throws Exception {
		long start = System.currentTimeMillis();
		while (System.currentTimeMillis() < start + TIME_OUT) {
			Set<String> newKeys = listAllKeys(objectRecordDAO, type);
			newKeys.removeAll(oldKeys);
			if (newKeys.size() != 0 && findRecords(expectedRecords, newKeys, objectRecordDAO, type)) {
				return true;
			}

			// wait for 1 second before calling the service again
			Thread.sleep(1000);
		}
		return false;
	}

	/**
	 * 
	 * @param objectRecordDAO
	 * @param type
	 * @return all keys available for this type in S3
	 */
	public static Set<String> listAllKeys(ObjectRecordDAO objectRecordDAO, String type) {
		Iterator<String> it = objectRecordDAO.keyIterator(type);
		Set<String> keys = new HashSet<String>();
		while (it.hasNext()) {
			keys.add(it.next());
		}
		return keys;
	}

	/**
	 * @return true if newKeys contains information for all expectedRecords,
	 * 		false otherwise.
	 */
	private static boolean findRecords(List<ObjectRecord> expectedRecords, Set<String> newKeys, ObjectRecordDAO objectRecordDAO, String type) throws IOException {
		List<ObjectRecord> newRecords = new ArrayList<ObjectRecord>();
		for (String key : newKeys) {
			 newRecords.addAll(objectRecordDAO.getBatch(key, type));
		}
		return compareRecords(newRecords, expectedRecords);
	}

	/**
	 * Compares two list of ObjectRecords
	 * 
	 * @param actualRecords
	 * @param expectedRecords
	 * @return true if actualRecords contains all expectedRecords regardless of timestamp,
	 * 		 false otherwise.
	 */
	private static boolean compareRecords(List<ObjectRecord> actualRecords, List<ObjectRecord> expectedRecords) {
		for (ObjectRecord record: actualRecords) {
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
