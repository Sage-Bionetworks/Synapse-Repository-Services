package org.sagebionetworks.audit.dao;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.audit.ObjectRecord;

/**
 * This Data Access Object is used for the permanent persistence and retrieval of object records.
 *
 */
public interface ObjectRecordDAO {

	/**
	 * Save a batch of object records to a file and pushes it to S3
	 * 
	 * @param records - the object records to write
	 * @param type - the type of the synapse object that is going to be written 
	 * @return the path of the file that is stored in S3
	 * @throws IOException
	 */
	String saveBatch(List<ObjectRecord> records, String type) throws IOException;
	
	/**
	 * Get a batch of ObjectRecords from the permanent store using its key
	 * 
	 * @param key - The key of the batch
	 * @param type - the type of the synapse object
	 * @return a batch of ObjectRecords
	 * @throws IOException 
	 */
	List<ObjectRecord> getBatch(String key, String type) throws IOException;

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
