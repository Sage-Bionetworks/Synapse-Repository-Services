package org.sagebionetworks.repo.web.filter.throttle;

import org.sagebionetworks.cloudwatch.ProfileData;

public class RequestThrottledException extends Exception{
	private ProfileData profileData;

	public RequestThrottledException(String message, ProfileData profileData){
		super(message);
		this.profileData = profileData;
	}

	public ProfileData getProfileData() {
		return profileData;
	}

}
