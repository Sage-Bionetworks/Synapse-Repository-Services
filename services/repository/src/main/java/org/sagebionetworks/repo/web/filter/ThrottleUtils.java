package org.sagebionetworks.repo.web.filter;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.http.HttpStatus;
import javax.servlet.Filter;
public class ThrottleUtils {
	
	/**
	 * reports to Cloudwatch that a lock could not be acquired
	 * @param userId id of user
	 * @param eventName name of event to be reported to Cloudwatch
	 * @param comsumer Consumer object of the filter for Cloudwatch
	 * @param filterClass class of the filter reporting the error. use: this.getClass()
	 * @throws IOException 
	 * @return ProfileData event that was generated from provided parameters
	 */
	public static ProfileData generateLockAcquireErrorEvent(String userId, String eventName, Class<? extends Filter> filterClass){
		ValidateArgument.required(userId, "userId");
		ValidateArgument.required(eventName, "eventName");
		ValidateArgument.required(filterClass, "filterClass");
		
		ProfileData lockUnavailableEvent = new ProfileData();
		lockUnavailableEvent.setNamespace(filterClass.getName());
		lockUnavailableEvent.setName(eventName);
		lockUnavailableEvent.setValue(1.0);
		lockUnavailableEvent.setUnit("Count");
		lockUnavailableEvent.setTimestamp(new Date());
		lockUnavailableEvent.setDimension(Collections.singletonMap("UserId", userId));
		return lockUnavailableEvent;
	}
	/**
	 * Sets the response message to return to user as HTTP 503.
	 * @param response the response object the user will receive.
	 * @param reason message to include along with the http code indicating why error occurred
	 * @throws IOException
	 */
	public static void httpTooManyRequestsErrorResponse(ServletResponse response, String reason) throws IOException {
		ValidateArgument.required(response,"respose");
		ValidateArgument.required(reason, "reason");
		
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		//TODO: Switch to 429 http code once clients have been implemented to expect that code
		httpResponse.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
		httpResponse.getWriter().println(reason);
	}
	
	//returns true if the user is the migration admin, false otherwise.
	public static boolean isMigrationAdmin(long userId){
		return userId == AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
	}
}

