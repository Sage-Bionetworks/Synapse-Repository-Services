package org.sagebionetworks.repo.manager.migration;

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
		if (type!=MigrationType.ACL) return;
		for (D dbo : delta) {
			DBOAccessControlList acl = (DBOAccessControlList)dbo;
			if (ObjectType.valueOf(acl.getOwnerType())!=ObjectType.ENTITY) continue;
			AccessControlList dto = aclDAO.get(""+acl.getId(), ObjectType.ENTITY);
			boolean modified=false;
			Set<ResourceAccess> updatedRAset = new HashSet<ResourceAccess>();
			for (ResourceAccess ra : dto.getResourceAccess()) {
				ResourceAccess updatedRA = new ResourceAccess();
				updatedRA.setPrincipalId(ra.getPrincipalId());
				if (ra.getAccessType().contains(ACCESS_TYPE.READ) && 
						!ra.getAccessType().contains(ACCESS_TYPE.DOWNLOAD)) {
					Set<ACCESS_TYPE> updatedPermissions = new HashSet<ACCESS_TYPE>(ra.getAccessType());
					updatedPermissions.add(ACCESS_TYPE.DOWNLOAD);
					updatedRA.setAccessType(updatedPermissions);
					modified=true;
				} else {
					updatedRA.setAccessType(ra.getAccessType());
				}
				updatedRAset.add(updatedRA);
			}
			if (modified) {
				dto.setResourceAccess(updatedRAset);
				aclDAO.update(dto, ObjectType.ENTITY);
			}
		}
	}

	@Override
	public void beforeDeleteBatch(MigrationType type, List<Long> idsToDelete) {
		// NA
	}

}
