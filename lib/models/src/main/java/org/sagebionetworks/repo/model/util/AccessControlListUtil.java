package org.sagebionetworks.repo.model.util;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserInfo;

public class AccessControlListUtil {
	/**
	 * Will create an ACL that will grant all permissions to a given user for the given node.
	 * @param nodeId
	 * @param userId
	 * @return
	 */
	public static AccessControlList createACLToGrantAll(String nodeId, UserInfo info){
		if(nodeId == null) throw new IllegalArgumentException("NodeId cannot be null");
		UserInfo.validateUserInfo(info);
		AccessControlList acl = new AccessControlList();
		acl.setCreationDate(new Date(System.currentTimeMillis()));
		acl.setId(nodeId);
		acl.setOwnerType(ObjectType.ENTITY);
		Set<ResourceAccess> set = new HashSet<ResourceAccess>();
		acl.setResourceAccess(set);
		ResourceAccess access = new ResourceAccess();
		// This user should be able to do everything.
		Set<ACCESS_TYPE> typeSet = new HashSet<ACCESS_TYPE>();
		ACCESS_TYPE array[] = ACCESS_TYPE.values();
		for(ACCESS_TYPE type: array){
			typeSet.add(type);
		}
		access.setAccessType(typeSet);
		//access.setDisplayName(info.getUser().getDisplayName());
		access.setPrincipalId(Long.parseLong(info.getIndividualGroup().getId()));
		set.add(access);
		return acl;
	}

}
