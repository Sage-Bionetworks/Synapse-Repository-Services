package org.sagebionetworks.repo.manager.migration;

import static org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP;
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
			boolean aclUpdateRequired=false;
			boolean authenticatedUsersDownloadRequired=false; // set to true if we need to give authenticated users DOWNLOAD access
			ResourceAccess authenticatedUsersEntry=null; // 'points' to authenticated users entry in ACL
			Set<ResourceAccess> updatedRAset = new HashSet<ResourceAccess>();
			for (ResourceAccess ra : dto.getResourceAccess()) {
				long principalId = ra.getPrincipalId();
				Set<ACCESS_TYPE> updatedPermissions = new HashSet<ACCESS_TYPE>(ra.getAccessType());
				ResourceAccess updatedRA = new ResourceAccess();
				updatedRA.setPrincipalId(principalId);
				updatedRA.setAccessType(updatedPermissions);
				if (principalId==PUBLIC_GROUP.getPrincipalId()) {
					if (updatedPermissions.contains(ACCESS_TYPE.READ)) {
						authenticatedUsersDownloadRequired=true;
					}
					// PUBLIC can't have download permission!
					boolean setChanged = updatedPermissions.remove(ACCESS_TYPE.DOWNLOAD);
					aclUpdateRequired = aclUpdateRequired || setChanged;
				} else if (updatedPermissions.contains(ACCESS_TYPE.READ)) {
					boolean setChanged = updatedPermissions.add(ACCESS_TYPE.DOWNLOAD);
					aclUpdateRequired = aclUpdateRequired || setChanged;
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
				boolean setChanged = authenticatedUsersEntry.getAccessType().addAll(Arrays.asList(new ACCESS_TYPE[]{ACCESS_TYPE.READ,ACCESS_TYPE.DOWNLOAD}));
				aclUpdateRequired = aclUpdateRequired || setChanged;
				updatedRAset.add(authenticatedUsersEntry);
			}
			if (aclUpdateRequired) {
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
