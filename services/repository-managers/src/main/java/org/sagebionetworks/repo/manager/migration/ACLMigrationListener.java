package org.sagebionetworks.repo.manager.migration;

import static org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER;
import static org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAccessControlList;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.springframework.beans.factory.annotation.Autowired;


/*
 * This class adds DOWNLOAD permission in addition to READ to Entity ACLs
 */
public class ACLMigrationListener implements MigrationTypeListener {
	
	@Autowired
	AccessControlListDAO aclDAO;

	@Override
	public <D extends DatabaseObject<?>> void afterCreateOrUpdate(
			MigrationType type, List<D> delta) {
		if (type!=MigrationType.ACL) {
			return;
		}
		for (D dbo : delta) {
			Long ownerId = ((DBOAccessControlList)dbo).getOwnerId();
			ObjectType ownerType = ObjectType.valueOf(((DBOAccessControlList)dbo).getOwnerType());
			if (ownerType!=ObjectType.ENTITY) {
				continue;
			}
			AccessControlList dto = aclDAO.get(ownerId.toString(), ObjectType.ENTITY);
			boolean modified=false;
			boolean authenticatedUsersDownloadRequired=false; // set to true if we need to give authenticated users DOWNLOAD access
			ResourceAccess authenticatedUsersEntry=null; // 'points' to authenticated users entry in ACL
			Set<ResourceAccess> updatedRAset = new HashSet<ResourceAccess>();
			for (ResourceAccess ra : dto.getResourceAccess()) {
				long principalId = ra.getPrincipalId();
				Set<ACCESS_TYPE> updatedPermissions = new HashSet<ACCESS_TYPE>(ra.getAccessType());
				ResourceAccess updatedRA = new ResourceAccess();
				updatedRA.setPrincipalId(principalId);
				updatedRA.setAccessType(updatedPermissions);
				if (principalId==ANONYMOUS_USER.getPrincipalId()) {
					if (updatedPermissions.contains(ACCESS_TYPE.READ)) {
						authenticatedUsersDownloadRequired=true;
					}
					// anonymous can't have download permission!
					modified = modified || updatedPermissions.remove(ACCESS_TYPE.DOWNLOAD);
				} else if (updatedPermissions.contains(ACCESS_TYPE.READ)) {
					modified = modified || updatedPermissions.add(ACCESS_TYPE.DOWNLOAD);
				}
				if (principalId==AUTHENTICATED_USERS_GROUP.getPrincipalId()) {
					authenticatedUsersEntry=updatedRA;
				}
				updatedRAset.add(updatedRA);
			}
			if (authenticatedUsersDownloadRequired) {
				if (authenticatedUsersEntry==null) {
					authenticatedUsersEntry = new ResourceAccess();
					authenticatedUsersEntry.setPrincipalId(AUTHENTICATED_USERS_GROUP.getPrincipalId());
					authenticatedUsersEntry.setAccessType(new HashSet<ACCESS_TYPE>());
				} else {
					// can't modify an object in a hash set. Remove it, modify it, and re-add it.
					updatedRAset.remove(authenticatedUsersEntry);
				}
				authenticatedUsersEntry.getAccessType().addAll(Arrays.asList(new ACCESS_TYPE[]{ACCESS_TYPE.READ,ACCESS_TYPE.DOWNLOAD}));
				updatedRAset.add(authenticatedUsersEntry);
				modified=true;
			}
			if (modified) {
				dto.setResourceAccess(updatedRAset);
				aclDAO.update(dto, ObjectType.ENTITY);
			}
		}
	}

	@Override
	public void beforeDeleteBatch(MigrationType type, List<Long> idsToDelete) {
		// N/A
	}

}
