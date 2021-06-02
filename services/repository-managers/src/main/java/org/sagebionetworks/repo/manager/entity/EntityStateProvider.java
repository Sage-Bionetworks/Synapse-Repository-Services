package org.sagebionetworks.repo.manager.entity;

import java.util.List;

import org.sagebionetworks.repo.model.ar.UsersRestrictionStatus;
import org.sagebionetworks.repo.model.dbo.entity.UserEntityPermissionsState;

/**
 * Provides database state information about a user's entity permission and
 * access restriction status.
 *
 */
public interface EntityStateProvider {

	/**
	 * The full list of entity IDs that this provider can provide.
	 * 
	 * @return
	 */
	List<Long> getEntityIds();

	/**
	 * Get a UserEntityPermissionsState for the given entity Id.
	 * 
	 * @return
	 */
	UserEntityPermissionsState getPermissionsState(Long entityId);

	/**
	 * For a given entityID load all of the access restrictions on the entity
	 * including the the user's approval state for each.
	 * 
	 * @param entityId
	 * @return
	 */
	UsersRestrictionStatus getRestrictionStatus(Long entityId);
}
