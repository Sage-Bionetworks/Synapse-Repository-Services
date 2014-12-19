package org.sagebionetworks.audit.dao;

import java.io.IOException;
import java.util.List;

import org.sagebionetworks.repo.model.audit.AclRecord;

/**
 * This Data Access Object is used for the permanent persistence and retrieval of acl records.
 *
 */
public interface AclRecordDAO {

	/**
	 * writes acl records to a file and pushes it to S3
	 * 
	 * @param records the acl records to write
	 * @return the path of the file that is stored in S3
	 * @throws IOException
	 */
	String write(List<AclRecord> records) throws IOException;
	
}
