package org.sagebionetworks.repo.model.query;

import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public interface QueryDAO {

	/**
	 * Execute a user query.
	 * @throws JSONObjectAdapterException 
	 */
	public QueryTableResults executeQuery(BasicQuery query,
			UserInfo userInfo) throws DatastoreException, NotFoundException, JSONObjectAdapterException;

	/**
	 * Set the ACL DAO to use (for testing).
	 */
	public void setAclDAO(AccessControlListDAO aclDAO);
	
}