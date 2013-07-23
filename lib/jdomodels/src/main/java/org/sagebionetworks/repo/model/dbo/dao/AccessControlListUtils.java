package org.sagebionetworks.repo.model.dbo.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessControlList;
import org.sagebionetworks.repo.model.dbo.persistence.DBOResourceAccessType;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ObjectType;

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
		dbo.setEtag(acl.getEtag());
		dbo.setCreationDate(acl.getCreationDate().getTime());
		return dbo;
	}
	
	/**
	 * Create an ACL from a DBO.
	 * @param dbo
	 * @return
	 * @throws DatastoreException
	 */
	public static AccessControlList createAcl(DBOAccessControlList dbo, ObjectType objectType) throws DatastoreException {
		AccessControlList acl = new AccessControlList();
		if (ObjectType.ENTITY.equals(objectType)) {
			acl.setId(KeyFactory.keyToString(dbo.getId()));
		} else {
			acl.setId(dbo.getId().toString());
		}
		acl.setEtag(dbo.getEtag());
		acl.setCreationDate(new Date(dbo.getCreationDate()));
		return acl;
	}
	
	/**
	 * Create a batch of resource access.
	 * @param id
	 * @param access
	 * @return
	 */
	public static List<DBOResourceAccessType> createResourceAccessTypeBatch(Long id, Long owner, Set<ACCESS_TYPE> access) {
		List<DBOResourceAccessType> batch = new ArrayList<DBOResourceAccessType>();
		for(ACCESS_TYPE type: access){
			DBOResourceAccessType dboType = new DBOResourceAccessType();
			dboType.setId(id);
			dboType.setOwner(owner);
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
		if(acl == null) throw new IllegalArgumentException("ACL cannot be null.");
		if(acl.getId() == null) throw new IllegalArgumentException("ACL.getID() cannot return null.");
		if(acl.getEtag() == null) throw new IllegalArgumentException("ACL.getEtag() cannot return null.");
		if(acl.getCreationDate() == null) throw new IllegalArgumentException("ACL.getCreationDate() cannot return null.");
		if(acl.getResourceAccess() == null) throw new IllegalArgumentException("ACL.getResourceAccess() cannot return null.");
	}
}
