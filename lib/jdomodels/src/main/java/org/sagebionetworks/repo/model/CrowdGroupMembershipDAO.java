package org.sagebionetworks.repo.model;

import java.util.Collection;
import java.util.HashSet;

public class CrowdGroupMembershipDAO implements GroupMembershipDAO {

	@Override
	public Collection<String> getUserGroupNames(String userName) {
		return new HashSet<String>(); // not yet implemented
	}

}
