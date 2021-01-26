package org.sagebionetworks.repo.model.dbo.entity;

import java.util.List;
import java.util.Set;

public interface EntityPermissionDao {

	/**
	 * Get the entity permissions for the given user and set of entity IDs.
	 * 
	 * @param user
	 * @param entityIds
	 * @return
	 */
	public List<EntityPermission> getEntityPermissions(Set<Long> userGroups, List<Long> entityIds);

}
