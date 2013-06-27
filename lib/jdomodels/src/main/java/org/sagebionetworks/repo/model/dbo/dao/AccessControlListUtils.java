package org.sagebionetworks.repo.model.dbo.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserInfo;
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
		dbo.setEtag(acl.getEtag());
		dbo.setResource(dbo.getId());
		dbo.setCreationDate(acl.getCreationDate().getTime());
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
		acl.setId(dbo.getId().toString());
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
		String aclId = acl.getId();
		Long.parseLong(aclId); // Throws NumberFormatException if the id is not a number (e.g. with the "syn" prefix)
	}

	/**
	 * Will create an ACL that will grant all permissions to a given user for the given node.
	 * @param nodeId
	 * @param userId
	 * @return
	 */
	public static AccessControlList createACLToGrantAll(String nodeId, UserInfo info){
		if(nodeId == null) throw new IllegalArgumentException("NodeId cannot be null");
		UserInfo.validateUserInfo(info);
		AccessControlList acl = new AccessControlList();
		acl.setCreationDate(new Date(System.currentTimeMillis()));
		acl.setId(KeyFactory.stringToKey(nodeId).toString());
		Set<ResourceAccess> set = new HashSet<ResourceAccess>();
		acl.setResourceAccess(set);
		ResourceAccess access = new ResourceAccess();
		// This user should be able to do everything.
		Set<ACCESS_TYPE> typeSet = new HashSet<ACCESS_TYPE>();
		ACCESS_TYPE array[] = ACCESS_TYPE.values();
		for(ACCESS_TYPE type: array){
			typeSet.add(type);
		}
		access.setAccessType(typeSet);
		//access.setDisplayName(info.getUser().getDisplayName());
		access.setPrincipalId(Long.parseLong(info.getIndividualGroup().getId()));
		set.add(access);
		return acl;
	}
}
