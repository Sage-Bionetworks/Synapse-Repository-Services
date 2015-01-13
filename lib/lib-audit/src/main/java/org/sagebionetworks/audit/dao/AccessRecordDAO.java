package org.sagebionetworks.audit.dao;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.audit.AccessRecord;

import com.amazonaws.services.s3.model.ObjectListing;

/**
 * This Data Access Object is used for the permanent persistence and retrieval of access records.
 * 
 * @author John
 *
 */
public interface AccessRecordDAO {

	/**
	 * Save a batch of AccessRecord to the permanent store using the current time as the timestamp.
	 *
	 * @param rolling  Whether the batch is saved as "rolling". A rolling file will be collated and merged.
	 */
	String saveBatch(List<AccessRecord> batch, boolean rolling) throws IOException;

	/**
	 * @param rolling  Whether the batch is saved as "rolling". A rolling file will be collated and merged.
	 */
	String saveBatch(List<AccessRecord> batch, long timestamp, boolean rolling) throws IOException;

	/**
	 * Get a batch of AccessRecords from the permanent store using its key (see:  {@link #saveBatch(List)})
	 * 
	 * @param key
	 *            The key of the batch
	 * @return
	 * @throws IOException 
	 */
	List<AccessRecord> getBatch(String key) throws IOException;

	/**
	 * @return all keys found this this bucket
	 */
	Set<String> listAllKeys();

	/**
	 * Delete a batch.
	 * @param key
	 */
	void deleteBactch(String key);

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
	ObjectListing listBatchKeys(String marker);

	/**
	 * Delete all stack instance batches from the bucket.  This should never be called on a production system.
	 * 
	 */
	void deleteAllStackInstanceBatches();
}
