package org.sagebionetworks.audit.dao;

import java.io.IOException;
import java.util.List;

import org.sagebionetworks.repo.model.audit.ResourceAccessRecord;

/**
 * This Data Access Object is used for the permanent persistence and retrieval of resource access records.
 *
 */
public interface ResourceAccessRecordDAO {
	
	/**
	 * writes an resource access record to a file and pushes it to S3
	 * 
	 * @param records - the resource access records to write
	 * @return the path of the file that is stored in S3
	 * @throws IOException
	 */
	String write(List<ResourceAccessRecord> records) throws IOException;
}
