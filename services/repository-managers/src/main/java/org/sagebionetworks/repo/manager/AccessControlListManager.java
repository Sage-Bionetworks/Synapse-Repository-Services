package org.sagebionetworks.repo.manager;

import java.util.Set;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;

public interface AccessControlListManager {

	void create(AccessControlList acl, ObjectType objectType);

	void delete(String objectId, ObjectType objectType);

	/**
	 * Get the intersection of the given benefactor ids and the benefactors the user can read.
	 * @param userInfo
	 * @param originalBenefactors
	 * @return
	 */
	Set<Long> getAccessibleBenefactors(UserInfo userInfo, ObjectType objectType, Set<Long> originalBenefactors);

	boolean canAccess(Set<Long> groups, String objectId, ObjectType objectType, ACCESS_TYPE accessType);

	Set<Long> getAccessibleProjectIds(Set<Long> principalIds);

}
