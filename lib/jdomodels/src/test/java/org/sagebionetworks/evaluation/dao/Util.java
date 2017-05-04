package org.sagebionetworks.evaluation.dao;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.ResourceAccess;

public class Util {

	public static AccessControlList createACL(String objectId, long userId, Set<ACCESS_TYPE> permissions, Date now) {
		if(objectId == null) throw new IllegalArgumentException("NodeId cannot be null");
		AccessControlList acl = new AccessControlList();
		acl.setCreatedBy(""+userId);
		acl.setCreationDate(now);
		acl.setId(objectId);
		Set<ResourceAccess> set = new HashSet<ResourceAccess>();
		acl.setResourceAccess(set);
		ResourceAccess access = new ResourceAccess();
		access.setAccessType(new HashSet<ACCESS_TYPE>(permissions));
		access.setPrincipalId(userId);
		set.add(access);
		return acl;
	}

}
