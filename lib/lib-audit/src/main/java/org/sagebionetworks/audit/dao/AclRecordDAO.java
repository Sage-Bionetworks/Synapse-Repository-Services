package org.sagebionetworks.audit.dao;

import java.io.IOException;

import org.sagebionetworks.repo.model.audit.AclRecord;

/**
 * This Data Access Object is used for the permanent persistence and retrieval of acl records.
 *
 */
public interface AclRecordDAO {

	void write(AclRecord record) throws IOException;
}
