package org.sagebionetworks.repo.web.filter.throttle;

import org.sagebionetworks.cloudwatch.ProfileData;

/**
 * Thrown by a {@link RequestThrottler} when an HTTP request needs to be throttled.
 */
public class RequestThrottledException extends Exception{
	private ProfileData profileData;

	public RequestThrottledException(String message, ProfileData profileData){
		super(message);
		this.profileData = profileData;
	}

	/**
	 *
	 * @return data to be passed to a CloudWatch {@link org.sagebionetworks.cloudwatch.Consumer}
	 * in order to log the throttled request.
	 */
	public ProfileData getProfileData() {
		return profileData;
	}

}
