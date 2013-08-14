package org.sagebionetworks.audit.dao;

import java.io.IOException;
import java.util.List;

import org.sagebionetworks.repo.model.audit.AccessRecord;
import org.sagebionetworks.repo.model.audit.BatchListing;

/**
 * This Data Access Object is used for the permanent persistence and retrieval of access records.
 * 
 * @author John
 *
 */
public interface AccessRecordDAO {

	/**
	 * Save a batch of AccessRecord to the permanent store.
	 * 
	 * @param batch
	 * @return The key
	 * @throws IOException 
	 */
	public String saveBatch(List<AccessRecord> batch) throws IOException;

	/**
	 * Get a batch of AccessRecords from the permanent store using its key (see:  {@link #saveBatch(List)})
	 * 
	 * @param key
	 *            The key of the batch
	 * @return
	 * @throws IOException 
	 */
	public List<AccessRecord> getBatch(String key) throws IOException;
	
	/**
	 * Delete a batch.
	 * @param key
	 */
	public void deleteBactch(String key);

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
	
	/**
	 * Delete all stack instance batches from the bucket.  This should never be called on a production system.
	 * 
	 */
	public void deleteAllStackInstanceBatches();
}
