package org.sagebionetworks.repo.web.filter.throttle;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.http.HttpStatus;
public class ThrottleUtils {
	public static int THROTTLED_HTTP_STATUS = HttpStatus.TOO_MANY_REQUESTS.value();
	public static String JSON_HTTP_CONTENT_TYPE = "application/json";
	public static String UTF8_ENCODING = "UTF-8";
	
	/**
	 * reports to Cloudwatch that a lock could not be acquired
	 * @param userId id of user
	 * @param eventName name of event to be reported to Cloudwatch
	 * @param comsumer Consumer object of the filter for Cloudwatch
	 * @param namespace namespace of the Cloudwatch ProfileData
	 * @throws IOException 
	 * @return ProfileData event that was generated from provided parameters
	 */
	public static ProfileData generateCloudwatchProfiledata(String eventName, String namespace, Map<String, String> dimensions){
		ValidateArgument.required(eventName, "eventName");
		ValidateArgument.required(namespace, "namespace");
		ValidateArgument.required(dimensions, "dimensions");
		
		ProfileData lockUnavailableEvent = new ProfileData();
		lockUnavailableEvent.setNamespace(namespace);
		lockUnavailableEvent.setName(eventName);
		lockUnavailableEvent.setValue(1.0);
		lockUnavailableEvent.setUnit("Count");
		lockUnavailableEvent.setTimestamp(new Date());
		lockUnavailableEvent.setDimension(dimensions);
		return lockUnavailableEvent;
	}
	/**
	 * Sets the response message to return to user as HTTP 503.
	 * @param response the response object the user will receive.
	 * @param errorCode the HTTP error code to return to the user
	 * @param errorMessage message to include along with the HTTP code indicating why error occurred
	 * @throws IOException
	 */
	public static void setResponseError(ServletResponse response, int errorCode, String errorMessage) throws IOException {
		ValidateArgument.required(response,"respose");
		ValidateArgument.required(errorMessage, "errorMessage");
		
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		//TODO: Switch to 429 http code once clients have been implemented to expect that code
		httpResponse.setStatus(errorCode);
		httpResponse.setContentType(JSON_HTTP_CONTENT_TYPE);
		httpResponse.setCharacterEncoding(UTF8_ENCODING);
		httpResponse.getWriter().println(errorMessage);
	}
	
	//returns true if the user is the migration admin, false otherwise.
	public static boolean isMigrationAdmin(long userId){
		return userId == AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
	}
}

