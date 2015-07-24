package org.sagebionetworks.audit.dao;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

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
	 * @param type - the type of the synapse object
	 * 
	 */
	void deleteAllStackInstanceBatches(String type);

	/**
	 * @param type - the type of the synapse object
	 * @return the key iterator for a bucket of this type
	 */
	Iterator<String> keyIterator(String type);

	/**
	 * Delete the bucket for the requested type.
	 * Only used for integration tests.
	 * @param type
	 */
	void deleteBucket(String type);
}
