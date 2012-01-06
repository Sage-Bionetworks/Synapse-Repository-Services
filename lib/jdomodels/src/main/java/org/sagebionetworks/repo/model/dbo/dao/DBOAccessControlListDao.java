package org.sagebionetworks.repo.model.dbo.dao;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * CRUD for ACLs.
 * 
 * @author jmhill
 *
 */
public interface DBOAccessControlListDao {
	
	public AccessControlList createACL(AccessControlList acl) throws DatastoreException, NotFoundException;
	
	public AccessControlList getACL(Long owner) throws DatastoreException, NotFoundException;
	
	public AccessControlList update(AccessControlList acl) throws DatastoreException, NotFoundException;
	
	public boolean delete(Long owner) throws DatastoreException;

}
