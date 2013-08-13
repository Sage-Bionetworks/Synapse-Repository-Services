package org.sagebionetworks.repo.manager.audit;

import java.io.IOException;
import java.util.List;

import org.sagebionetworks.repo.model.audit.AccessRecord;
import org.sagebionetworks.repo.model.audit.BatchListing;

/**
 * Abstraction for the AccessRecord Manager.
 * 
 * @author jmhill
 * 
 */
public interface AccessRecordManager {

	/**
	 * Save a batch of AccessRecord to the permanent store.
	 * 
	 * @param batch
	 * @return The key
	 */
	public String saveBatch(List<AccessRecord> batch);

	/**
	 * Get a batch that was saved using {@link #saveBatch(List)}
	 * 
	 * @param key
	 *            The key of the batch
	 * @return
	 * @throws IOException 
	 */
	public List<AccessRecord> getSavedBatch(String key) throws IOException;

	/**
	 * List the keys of all batches that makeup the complete access record
	 * database.
	 * 
	 * @param marker
	 *            Set to null for the first call. If a marker is returned for a
	 *            batch then there are more results available. Pass the returned
	 *            marker to get the next page.
	 * @return
	 */
	public BatchListing listBatchKeys(String marker);
}
