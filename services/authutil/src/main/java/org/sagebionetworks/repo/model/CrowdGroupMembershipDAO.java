package org.sagebionetworks.repo.model;

import java.io.IOException;
import java.util.Collection;

import org.sagebionetworks.authutil.CrowdAuthUtil;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * 
 * Implementation of GroupMembershipDAO which gets information from Crowd
 * 
 */
public class CrowdGroupMembershipDAO implements GroupMembershipDAO {
	
	private CrowdAuthUtil crowdAuthUtil = new CrowdAuthUtil();


	@Override
	public Collection<String> getUserGroupNames(String userName) throws NotFoundException, DatastoreException {
		try {
			return crowdAuthUtil.getUsersGroups(userName);
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}

}
