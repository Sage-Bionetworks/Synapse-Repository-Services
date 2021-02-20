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
	 * Get a UserEntityPermissionsState for each entityId. The order of the
	 * resulting list will match the order of the provided entity Ids.
	 * 
	 * @return
	 */
	List<UserEntityPermissionsState> getUserEntityPermissionsState();

	/**
	 * For a given entityID load all of the access restrictions on the entity
	 * including the the user's approval state for each.
	 * 
	 * @param entityId
	 * @return
	 */
	UsersRestrictionStatus getUserRestrictionStatus(Long entityId);
}
