package org.sagebionetworks.repo.model.audit;

import org.sagebionetworks.repo.model.audit.AccessRecord;

/**
 * Abstraction for recording access events.
 * @author jmhill
 *
 */
public interface AccessRecorder {
	
	/**
	 * Save the given access record.
	 * @param record
	 */
	public void save(AccessRecord record);

}
