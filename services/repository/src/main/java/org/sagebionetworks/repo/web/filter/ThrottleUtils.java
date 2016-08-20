package org.sagebionetworks.repo.web.filter;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.springframework.http.HttpStatus;
import javax.servlet.Filter;
public class ThrottleUtils {
	
	/**
	 * reports to cloudwatch that a lock could not be acquired and sets the response message to return to user
	 * @param userId id of user
	 * @param response 
	 * @param eventName name of event to be reported to cloudwatch
	 * @param reason reason for user to see
	 * @param comsumer Consumer object of the filter for cloudwatch
	 * @param filterClass class of the filter reporting the error. use: this.getClass()
	 * @throws IOException 
	 */
	public static void reportLockAcquireError(String userId, ServletResponse response, String eventName, String reason, Consumer consumer, Class<? extends Filter> filterClass) throws IOException{
		
		ProfileData lockUnavailableEvent = new ProfileData();
		lockUnavailableEvent.setNamespace(filterClass.getName());
		lockUnavailableEvent.setName(eventName);
		lockUnavailableEvent.setValue(1.0);
		lockUnavailableEvent.setUnit("Count");
		lockUnavailableEvent.setTimestamp(new Date());
		lockUnavailableEvent.setDimension(Collections.singletonMap("UserId", userId));
		consumer.addProfileData(lockUnavailableEvent);
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		//TODO: Switch to 429 http code once clients have been implemented to expect that code
		httpResponse.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
		httpResponse.getWriter().println(reason);
	}
	
	//returns whether the userId is that of the admin used for migration
	public static boolean isMigrationAdmin(long userId){
		return userId == AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
	}
}
