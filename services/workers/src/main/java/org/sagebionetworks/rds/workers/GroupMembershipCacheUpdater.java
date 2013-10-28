package org.sagebionetworks.rds.workers;

import java.util.Collection;
import java.util.Date;

import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Checks and updates the group memberships of all users
 */
public class GroupMembershipCacheUpdater implements Runnable {

	@Autowired
	private Consumer consumer;
	
	@Autowired
	private GroupMembersDAO groupMembersDAO;
	
	@Autowired
	private UserGroupDAO userGroupDAO;

	@Override
	public void run() {
		long start = System.currentTimeMillis();
		
		// Iterate through all users in the system
		// And refresh the respective cache entries
		Collection<UserGroup> ugs = userGroupDAO.getAll();
		for (UserGroup ug : ugs) {
			groupMembersDAO.validateCache(ug.getId());
		}
		
		addMetric("TotalLatency", System.currentTimeMillis() - start, "Milliseconds");
		addMetric("TotalNumberOfUsersProcessed", ugs.size(), "Count");
	}

	private void addMetric(String name, long metric, String unit) {
		ProfileData profileData = new ProfileData();
		profileData.setNamespace("GroupMembershipCacheUpdater");
		profileData.setName(name);
		profileData.setLatency(metric);
		profileData.setUnit(unit);
		profileData.setTimestamp(new Date());
		consumer.addProfileData(profileData);
	}
}
