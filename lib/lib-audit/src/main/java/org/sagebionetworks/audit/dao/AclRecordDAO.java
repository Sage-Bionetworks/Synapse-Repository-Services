package org.sagebionetworks.audit.dao;

import java.io.IOException;

import org.sagebionetworks.repo.model.audit.AclRecord;

/**
 * This Data Access Object is used for the permanent persistence and retrieval of acl records.
 *
 */
public interface AclRecordDAO {

	/**
	 * writes an acl record to a file and pushes it to S3
	 * 
	 * @param record the acl record to write
	 * @return the path of the file that is stored in S3
	 * @throws IOException
	 */
	String write(AclRecord record) throws IOException;
	
}
