package org.sagebionetworks.repo.model.dbo.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessControlList;
import org.sagebionetworks.repo.model.dbo.persistence.DBOResourceAccessType;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.util.ValidateArgument;

/**
 * AccessControlList utils.
 * 
 * @author John
 *
 */
public class AccessControlListUtils {

	public static Map<ObjectType, Set<ACCESS_TYPE>> ALLOWED_ACCESS_TYPES = Map.of(
			ObjectType.ENTITY, Set.of(ACCESS_TYPE.CREATE, ACCESS_TYPE.DOWNLOAD, ACCESS_TYPE.READ,
					ACCESS_TYPE.CHANGE_PERMISSIONS, ACCESS_TYPE.CHANGE_SETTINGS, ACCESS_TYPE.DELETE, ACCESS_TYPE.MODERATE,
					ACCESS_TYPE.UPDATE, ACCESS_TYPE.SEND_MESSAGE, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE, ACCESS_TYPE.DELETE_SUBMISSION,
					ACCESS_TYPE.PARTICIPATE, ACCESS_TYPE.READ_PRIVATE_SUBMISSION, ACCESS_TYPE.SUBMIT, ACCESS_TYPE.UPDATE_SUBMISSION,
					ACCESS_TYPE.UPLOAD),
			ObjectType.EVALUATION, Set.of(ACCESS_TYPE.READ, ACCESS_TYPE.CHANGE_PERMISSIONS, ACCESS_TYPE.CREATE,
					ACCESS_TYPE.DELETE, ACCESS_TYPE.DELETE_SUBMISSION, ACCESS_TYPE.READ_PRIVATE_SUBMISSION, ACCESS_TYPE.SUBMIT,
					ACCESS_TYPE.UPDATE, ACCESS_TYPE.UPDATE_SUBMISSION, ACCESS_TYPE.PARTICIPATE, ACCESS_TYPE.CHANGE_SETTINGS,
					ACCESS_TYPE.DOWNLOAD, ACCESS_TYPE.MODERATE),
			ObjectType.FORM_GROUP, Set.of(ACCESS_TYPE.CHANGE_PERMISSIONS, ACCESS_TYPE.READ, ACCESS_TYPE.READ_PRIVATE_SUBMISSION, ACCESS_TYPE.SUBMIT),
			ObjectType.ORGANIZATION, Set.of(ACCESS_TYPE.CHANGE_PERMISSIONS, ACCESS_TYPE.CREATE, ACCESS_TYPE.DELETE, ACCESS_TYPE.READ, ACCESS_TYPE.UPDATE),
			ObjectType.ACCESS_REQUIREMENT, Set.of(ACCESS_TYPE.REVIEW_SUBMISSIONS, ACCESS_TYPE.EXEMPTION_ELIGIBLE),
			ObjectType.TEAM, Set.of(ACCESS_TYPE.DELETE, ACCESS_TYPE.READ, ACCESS_TYPE.SEND_MESSAGE,
					ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE, ACCESS_TYPE.UPDATE, ACCESS_TYPE.CREATE, ACCESS_TYPE.DOWNLOAD,
					ACCESS_TYPE.CHANGE_PERMISSIONS, ACCESS_TYPE.CHANGE_SETTINGS, ACCESS_TYPE.MODERATE, ACCESS_TYPE.DELETE_SUBMISSION,
					ACCESS_TYPE.SUBMIT, ACCESS_TYPE.UPDATE_SUBMISSION, ACCESS_TYPE.PARTICIPATE));

	/**
	 * Create a DBO from the ACL
	 * Note:  The 'id' field in the DTO is called 'ownerId' in the DBO
	 * @param acl
	 * @return
	 * @throws DatastoreException
	 */
	public static DBOAccessControlList createDBO(AccessControlList acl, Long dboId, ObjectType ownerType) throws DatastoreException {
		DBOAccessControlList dbo = new DBOAccessControlList();
		dbo.setId(dboId);
		dbo.setOwnerId(KeyFactory.stringToKey(acl.getId()));
		dbo.setOwnerType(ownerType.name());
		dbo.setEtag(acl.getEtag());
		dbo.setCreationDate(acl.getCreationDate().getTime());
		return dbo;
	}
	
	/**
	 * Create an ACL from a DBO.
	 * Note:  The 'id' field in the DTO is called 'ownerId' in the DBO
	 * @param dbo
	 * @return
	 * @throws DatastoreException
	 */
	public static AccessControlList createAcl(DBOAccessControlList dbo, ObjectType ownerType) throws DatastoreException {
		AccessControlList acl = new AccessControlList();
		if (ObjectType.ENTITY.equals(ownerType)) {
			acl.setId(KeyFactory.keyToString(dbo.getOwnerId()));
		} else {
			acl.setId(dbo.getOwnerId().toString());
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
	 * Validate the ACL has all the expected fields and has allowed access type for specific object type.
	 * @param acl
	 */
	public static void validateACL(AccessControlList acl, ObjectType objectType) {
		ValidateArgument.required(acl, "acl");
		ValidateArgument.required(acl.getId(), "acl.id");
		ValidateArgument.required(acl.getEtag(), "acl.etag");
		ValidateArgument.required(acl.getCreationDate(), "acl.creationDate");
		ValidateArgument.required(acl.getResourceAccess(), "acl.resourceAccess");
		ValidateArgument.required(objectType, "objectType");

		acl.getResourceAccess().forEach(resourceAccess -> {
			Set<ACCESS_TYPE> allowed_types = ALLOWED_ACCESS_TYPES.get(objectType);
			if (allowed_types == null) {
				throw new IllegalArgumentException(String.format("The Acl of owner type %s is not allowed.", objectType));
			} else {
				Set<ACCESS_TYPE> accessSet = resourceAccess.getAccessType();
				for (ACCESS_TYPE type : accessSet) {
					ValidateArgument.requirement(allowed_types.contains(type), String.format("The access type %s is not allowed for %s.", type, objectType));
				}
			}
		});
	}
}
