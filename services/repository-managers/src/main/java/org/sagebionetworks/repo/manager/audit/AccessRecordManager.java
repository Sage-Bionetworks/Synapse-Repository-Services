package org.sagebionetworks.repo.manager.audit;

import java.io.IOException;
import java.util.List;

import org.sagebionetworks.repo.model.audit.AccessRecord;


/**
 * Abstraction for the AccessRecord Manager.
 * 
 * @author jmhill
 * 
 */
public interface AccessRecordManager {

	/**
	 * Save a batch to the permanent store
	 * @param batch
	 * @return
	 * @throws IOException
	 */
	public String saveBatch(List<AccessRecord> batch) throws IOException;

	/**
	 * Get a batch using its key.
	 * @param key
	 * @return
	 * @throws IOException 
	 */
	public List<AccessRecord> getBatch(String key) throws IOException;
}
