package org.sagebionetworks.repo.manager;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;

public interface AccessControlListManager {

	void create(UserInfo userInfo, AccessControlList acl, ObjectType objectType, Long ownerId);

	void update(UserInfo userInfo, AccessControlList acl, ObjectType objectType, Long ownerId);

	void delete(String objectId, ObjectType objectType);

	int delete(List<Long> ids, ObjectType ownerType);

	/**
	 * Get the intersection of the given benefactor ids and the benefactors the user can read.
	 * @param userInfo
	 * @param originalBenefactors
	 * @return
	 */
	Set<Long> getAccessibleBenefactors(UserInfo userInfo, ObjectType objectType, Set<Long> originalBenefactors, ACCESS_TYPE...accessTypes);

	boolean canAccess(Set<Long> groups, String objectId, ObjectType objectType, ACCESS_TYPE accessType);

	AuthorizationStatus canAccess(UserInfo user, String resourceId, ObjectType resourceType,
								  ACCESS_TYPE permission);

	Optional<AccessControlList> getAcl(String objectId, ObjectType objectType);

	Set<Long> getAccessibleProjectIds(Set<Long> principalIds);
	List<Long> getChildrenEntitiesWithAcls(List<Long> parentIds);

	Set<Long> getNonVisibleChilrenOfEntity(Set<Long> groups,
										   String parentId);

	void truncateAll();

}
