package org.sagebionetworks.repo.manager;

import java.util.HashSet;
import java.util.Set;

import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

@Service
public class AccessControlListManagerImpl implements AccessControlListManager {
	
	public static final Long TRASH_FOLDER_ID = Long.parseLong(
			StackConfigurationSingleton.singleton().getTrashFolderEntityId());
	
	private final AccessControlListDAO aclDao;

	@Autowired
	public AccessControlListManagerImpl(AccessControlListDAO aclDao) {
		super();
		this.aclDao = aclDao;
	}

	@Override
	public void create(AccessControlList acl, ObjectType objectType) {
		this.aclDao.create(acl, objectType);
	}

	@Override
	public void delete(String objectId, ObjectType objectType) {
		this.aclDao.delete(objectId, objectType);
	}

	@Override
	public Set<Long> getAccessibleBenefactors(UserInfo userInfo, ObjectType objectType, Set<Long> benefactors) {
		Set<Long> results = null;
		if (userInfo.isAdmin()){
			// admin same as input
			results = Sets.newHashSet(benefactors);
		}else{
			// non-adim run a query
			results = this.aclDao.getAccessibleBenefactors(userInfo.getGroups(), benefactors, objectType, ACCESS_TYPE.READ);
		}
		if (ObjectType.ENTITY.equals(objectType)) {
			// The trash folder should not be in the results
			results.remove(TRASH_FOLDER_ID);
		}
		return results;
	}

	@Override
	public boolean canAccess(Set<Long> groups, String objectId, ObjectType objectType, ACCESS_TYPE accessType) {
		return aclDao.canAccess(groups, objectId, objectType, accessType);
	}

	@Override
	public Set<Long> getAccessibleProjectIds(Set<Long> principalIds) {
		ValidateArgument.required(principalIds, "principalIds");
		if(principalIds.isEmpty()){
			return new HashSet<>(0);
		}
		return aclDao.getAccessibleProjectIds(principalIds, ACCESS_TYPE.READ);
	}

}
