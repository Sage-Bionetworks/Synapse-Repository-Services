package org.sagebionetworks.object.snapshot.worker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

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
			if (newKeys.size() != 0) {
				return findRecords(expectedRecords, newKeys, objectRecordDAO, type);
			}

			// wait for 1 second before calling the service again
			Thread.sleep(1000);
		}
		return false;
	}

	public static Set<String> listAllKeys(ObjectRecordDAO objectRecordDAO, String type) {
		Iterator<String> it = objectRecordDAO.keyIterator(type);
		Set<String> keys = new HashSet<String>();
		while (it.hasNext()) {
			keys.add(it.next());
		}
		return keys;
	}

	private static boolean findRecords(List<ObjectRecord> expectedRecords, Set<String> newKeys, ObjectRecordDAO objectRecordDAO, String type) throws IOException {
		List<ObjectRecord> newRecords = new ArrayList<ObjectRecord>();
		for (String key : newKeys) {
			 newRecords.addAll(objectRecordDAO.getBatch(key, type));
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
