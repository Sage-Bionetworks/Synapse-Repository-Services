package org.sagebionetworks.repo.web;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.model.audit.AccessRecord;
import org.sagebionetworks.repo.model.audit.AccessRecorder;

/**
 * A simple stub implementation of the AccessRecorder.
 * 
 * @author jmhill
 *
 */
public class StubAccessRecorder  implements AccessRecorder{

	List<AccessRecord> savedRecords = new LinkedList<AccessRecord>();
	
	@Override
	public void save(AccessRecord record) {
		savedRecords.add(record);
	}

	/**
	 * Get the saved records
	 * @return
	 */
	public List<AccessRecord> getSavedRecords() {
		return savedRecords;
	}

	@Override
	public String timerFired() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}


}
