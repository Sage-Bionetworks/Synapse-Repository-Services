package org.sagebionetworks.repo.model.dbo.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_TYPE;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessControlList;
import org.sagebionetworks.repo.model.dbo.persistence.DBOResourceAccessType;
import org.sagebionetworks.repo.model.jdo.KeyFactory;

/**
 * AccessControlList utils.
 * 
 * @author John
 *
 */
public class AccessControlListUtils {

	/**
	 * Create a DBO from the ACL
	 * @param acl
	 * @return
	 * @throws DatastoreException
	 */
	public static DBOAccessControlList createDBO(AccessControlList acl) throws DatastoreException {
		DBOAccessControlList dbo = new DBOAccessControlList();
		dbo.setId(KeyFactory.stringToKey(acl.getId()));
		dbo.setResource(dbo.getId());
		dbo.setCreatedBy(acl.getCreatedBy());
		dbo.setCreationDate(acl.getCreationDate().getTime());
		dbo.setModifiedBy(acl.getModifiedBy());
		dbo.setModifiedOn(acl.getModifiedOn().getTime());
		return dbo;
	}
	
	/**
	 * Create an ACL from a DBO.
	 * @param dbo
	 * @return
	 * @throws DatastoreException
	 */
	public static AccessControlList createAcl(DBOAccessControlList dbo) throws DatastoreException {
		AccessControlList acl = new AccessControlList();
		acl.setId(KeyFactory.keyToString(dbo.getId()));
		acl.setCreatedBy(dbo.getCreatedBy());
		acl.setCreationDate(new Date(dbo.getCreationDate()));
		acl.setModifiedBy(dbo.getModifiedBy());
		acl.setModifiedOn(new Date(dbo.getModifiedOn()));
		return acl;
	}
	
	/**
	 * Create a batch of resource access.
	 * @param id
	 * @param access
	 * @return
	 */
	public static List<DBOResourceAccessType> createResourceAccessTypeBatch(Long id, Set<ACCESS_TYPE> access) {
		List<DBOResourceAccessType> batch = new ArrayList<DBOResourceAccessType>();
		for(ACCESS_TYPE type: access){
			DBOResourceAccessType dboType = new DBOResourceAccessType();
			dboType.setId(id);
			dboType.setElement(type.name());
			batch.add(dboType);
		}
		return batch;
	}
	
	/**
	 * Validate the ACL has all of the expected fields.
	 * @param acl
	 */
	public static void validateACL(AccessControlList acl) {
		if(acl == null) throw new IllegalArgumentException("ACL cannot be null");
		if(acl.getId() == null) throw new IllegalArgumentException("ACL.getID cannot be null");
		if(acl.getCreatedBy() == null) throw new IllegalArgumentException("ACL.getCreatedBy() cannot be null");
		if(acl.getCreationDate() == null) throw new IllegalArgumentException("ACL.getCreationDate() cannot be null");
		if(acl.getModifiedBy() == null) throw new IllegalArgumentException("ACL.getModifiedBy() cannot be null");
		if(acl.getModifiedOn() == null) throw new IllegalArgumentException("ACL.getModifiedOn() cannot be null");
		if(acl.getResourceAccess() == null) throw new IllegalArgumentException("ACL.getResourceAccess() cannot be null");
	}
}
