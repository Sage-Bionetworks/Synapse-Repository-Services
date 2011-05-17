package org.sagebionetworks.repo.manager;

import java.util.Collection;
import java.util.HashSet;

import org.junit.Test;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.GroupMembershipDAO;

public class TestGroupMembershipDAO implements GroupMembershipDAO {

	@Override
	public Collection<String> getUserGroupNames(String userName) {
		Collection<String> ans = new HashSet<String>();
		ans.add("test-group");
		if ("admin".equals(userName)) ans.add(AuthorizationConstants.ADMIN_GROUP_NAME);
		return ans;
	}

	@Test
	public void fake() {
	}
	

}
