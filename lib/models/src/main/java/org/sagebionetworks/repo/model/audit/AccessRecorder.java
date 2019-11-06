package org.sagebionetworks.repo.model.audit;

import java.io.IOException;

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
	
	/**
	 * Called when the timer is fired
	 * @return
	 * @throws IOException
	 */
	public String timerFired() throws IOException;

}
