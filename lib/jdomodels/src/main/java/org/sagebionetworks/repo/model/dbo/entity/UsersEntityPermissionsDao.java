package org.sagebionetworks.repo.model.dbo.entity;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface UsersEntityPermissionsDao {

	/**
	 * Get a batch of UserEntityPermissions for the given the user's principal ID
	 * and a batch of entity IDs.
	 * 
	 * @param usersPrincipalIds The user's principal ids.
	 * @param entityIds         The batch of entity ID to fetch information
	 * @return The order of the results will match the order of the provided
	 *         entityIDs.
	 */
	List<UserEntityPermissionsState> getEntityPermissions(Set<Long> usersPrincipalIds, List<Long> entityIds);
	
	/**
	 * See: {link{@link #getEntityPermissions(Set, List)}
	 * @param usersPrincipalIds
	 * @param entityIds
	 * @return Map of entity ID to UserEntityPermissionsState
	 */
	Map<Long, UserEntityPermissionsState> getEntityPermissionsAsMap(Set<Long> usersPrincipalIds, List<Long> entityIds);

}
