package org.sagebionetworks.rds.workers;

import java.util.Date;

import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Checks and updates the group memberships of all users
 */
public class GroupMembershipCacheUpdater implements Runnable {

	@Autowired
	private Consumer consumer;

	@Override
	public void run() {
		
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
