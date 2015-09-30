package org.sagebionetworks.repo.model.util;

import static org.sagebionetworks.repo.model.ACCESS_TYPE.CHANGE_PERMISSIONS;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.CHANGE_SETTINGS;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.CREATE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.DELETE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.DELETE_SUBMISSION;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.READ;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.READ_PRIVATE_SUBMISSION;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.SUBMIT;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.UPDATE;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.UPDATE_SUBMISSION;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserInfo;

public class AccessControlListUtil {
	/**
	 * Will create an ACL that will grant all permissions to a given user for the given node.
	 * @param objectId
	 * @param userId
	 * @return
	 */
	public static AccessControlList createACL(String objectId, UserInfo info, Set<ACCESS_TYPE> permissions) {
		if(objectId == null) throw new IllegalArgumentException("NodeId cannot be null");
		UserInfo.validateUserInfo(info);
		AccessControlList acl = new AccessControlList();
		acl.setCreationDate(new Date(System.currentTimeMillis()));
		acl.setId(objectId);
		Set<ResourceAccess> set = new HashSet<ResourceAccess>();
		acl.setResourceAccess(set);
		ResourceAccess access = new ResourceAccess();
		access.setAccessType(new HashSet<ACCESS_TYPE>(permissions));
		access.setPrincipalId(info.getId());
		set.add(access);
		return acl;
	}

	public static AccessControlList createACLToGrantEntityAdminAccess(String nodeId, UserInfo info) {
		return createACL(nodeId, info, ModelConstants.ENITY_ADMIN_ACCESS_PERMISSIONS);
	}

	public static AccessControlList createACLToGrantEvaluationAdminAccess(String evaluationId, UserInfo info) {
		return createACL(evaluationId, info, ModelConstants.EVALUATION_ADMIN_ACCESS_PERMISSIONS);
	}

}
