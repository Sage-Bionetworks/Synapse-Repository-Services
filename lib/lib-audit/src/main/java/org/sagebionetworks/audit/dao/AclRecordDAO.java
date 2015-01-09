package org.sagebionetworks.audit.dao;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.audit.AclRecord;

/**
 * This Data Access Object is used for the permanent persistence and retrieval of acl records.
 *
 */
public interface AclRecordDAO {

	/**
	 * Save a batch of acl records to a file and pushes it to S3
	 * 
	 * @param records the acl records to write
	 * @return the path of the file that is stored in S3
	 * @throws IOException
	 */
	String saveBatch(List<AclRecord> records) throws IOException;
	
	/**
	 * Get a batch of AclRecords from the permanent store using its key
	 * 
	 * @param key - The key of the batch
	 * @return a batch of AclRecords
	 * @throws IOException 
	 */
	List<AclRecord> getBatch(String key) throws IOException;

	/**
	 * Delete all stack instance batches from the bucket.
	 * This should never be called on a production system.
	 * 
	 */
	void deleteAllStackInstanceBatches();

	/**
	 * @return all keys found this this bucket
	 */
	Set<String> listAllKeys();

	/**
	 * Delete a batch.
	 * @param key
	 */
	void deleteBactch(String key);
}
