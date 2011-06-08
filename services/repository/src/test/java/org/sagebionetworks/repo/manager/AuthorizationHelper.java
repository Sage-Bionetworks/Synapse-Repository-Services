package org.sagebionetworks.repo.manager;

import java.util.HashSet;
import java.util.Set;

import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserGroup;

public class AuthorizationHelper {
	public static void addToACL(AccessControlList acl, UserGroup ug, ACCESS_TYPE at, AccessControlListDAO aclDAO) throws Exception {
		Set<ResourceAccess> ras = null;
		if (acl.getResourceAccess()==null) {
			ras = new HashSet<ResourceAccess>();
		} else {
			ras = new HashSet<ResourceAccess>(acl.getResourceAccess());
		}
		acl.setResourceAccess(ras);
		ResourceAccess ra = null;
		for (ResourceAccess r : ras) {
			if (r.getUserGroupId()==ug.getId()) {
				ra=r;
				break;
			}
		}
		if (ra==null) {
			ra = new ResourceAccess();
			ra.setUserGroupId(ug.getId());
			Set<ACCESS_TYPE> ats = new HashSet<ACCESS_TYPE>();
			ra.setAccessType(ats);
			ras.add(ra);
		}
		ra.getAccessType().add(at);
		aclDAO.update(acl);
	}
	

}
