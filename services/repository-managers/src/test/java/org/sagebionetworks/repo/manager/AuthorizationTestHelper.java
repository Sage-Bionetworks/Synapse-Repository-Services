package org.sagebionetworks.repo.manager;

import java.util.HashSet;
import java.util.Set;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserGroup;

public class AuthorizationTestHelper {
	
	public static AccessControlList addToACL(AccessControlList acl, Long userId, ACCESS_TYPE at) throws Exception {
		UserGroup ug = new UserGroup();
		ug.setId(userId.toString());
		return addToACL(acl, ug, at);
	}
	/**
	 * Helper to add an access type for a given UserGroup to an existing ACL.
	 * @param acl
	 * @param ug
	 * @param at
	 * @return
	 * @throws Exception
	 */
	public static AccessControlList addToACL(AccessControlList acl, UserGroup ug, ACCESS_TYPE at) throws Exception {
		Set<ResourceAccess> ras = null;
		if (acl.getResourceAccess()==null) {
			ras = new HashSet<ResourceAccess>();
		} else {
			ras = new HashSet<ResourceAccess>(acl.getResourceAccess());
		}
		acl.setResourceAccess(ras);
		ResourceAccess ra = null;
		for (ResourceAccess r : ras) {
			if (r.getPrincipalId().toString().equals(ug.getId())) {
				ra=r;
				break;
			}
		}
		if (ra==null) {
			ra = new ResourceAccess();
			ra.setPrincipalId(Long.parseLong(ug.getId()));
			Set<ACCESS_TYPE> ats = new HashSet<ACCESS_TYPE>();
			ra.setAccessType(ats);
			ras.add(ra);
		}
		ra.getAccessType().add(at);
		return acl;
	}
	

}
