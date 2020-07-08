package org.sagebionetworks.repo.web;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.ThreadContext;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.audit.utils.VirtualMachineIdProvider;
import org.sagebionetworks.auth.HttpAuthUtil;
import org.sagebionetworks.aws.utils.s3.KeyGeneratorUtil;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenHelper;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.audit.AccessRecord;
import org.sagebionetworks.repo.model.audit.AccessRecorder;
import org.sagebionetworks.util.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * This intercepter is used to audit all web-service access.
 * 
 * @author John
 * 
 */
public class AccessInterceptor implements HandlerInterceptor, AccessIdListener{

	public static final String SESSION_ID = "sessionId";

	/**
	 * This map keeps track of the current record for each thread.
	 */
	Map<Long, AccessRecord> threadToRecordMap = Collections
			.synchronizedMap(new HashMap<Long, AccessRecord>());

	@Autowired
	AccessRecorder accessRecorder;
	
	@Autowired
	Clock clock;
	
	@Autowired
	StackConfiguration stackConfiguration;

	@Autowired
	private OIDCTokenHelper oidcTokenHelper;

	String getOAuthClientId(HttpServletRequest request) {
		/*
		 * There are two different places the client ID might be:
		 *  - in the access token, if the OAuth client is acting on behalf of a user
		 *  - injected into the verified client ID header, if the client used basic auth
		 */
		String accessToken = HttpAuthUtil.getBearerTokenFromStandardAuthorizationHeader(request);
		if (accessToken != null) {
			return oidcTokenHelper.parseJWT(accessToken).getBody().getAudience();
		} else {
			return request.getHeader(AuthorizationConstants.OAUTH_VERIFIED_CLIENT_ID_HEADER);
		}
	}

	/**
	 * This is called before a controller runs.
	 */
	@Override
	public boolean preHandle(HttpServletRequest request,
			HttpServletResponse response, Object handler) throws Exception {
		// Build up the record
		AccessRecord data = new AccessRecord();
		// Extract the UserID when provided
		String userIdString = request.getParameter(AuthorizationConstants.USER_ID_PARAM);
		if (userIdString != null) {
			Long userId = Long.parseLong(userIdString); 
			data.setUserId(userId);
		}
		data.setTimestamp(clock.currentTimeMillis());
		data.setRequestURL(request.getRequestURI());
		data.setMethod(request.getMethod());
		data.setThreadId(Thread.currentThread().getId());
		String sessionId = UUID.randomUUID().toString();
		data.setSessionId(sessionId);
		// capture common headers that tell us more about the user.
		data.setHost(request.getHeader("Host"));
		data.setOrigin(request.getHeader("Origin"));
		data.setUserAgent(request.getHeader("User-Agent"));
		data.setXForwardedFor(IpAddressUtil.getIpAddress(request));
		data.setVia(request.getHeader("Via"));
		data.setDate(KeyGeneratorUtil.getDateString(data.getTimestamp()));
		data.setStack(stackConfiguration.getStack());
		data.setInstance(KeyGeneratorUtil.getInstancePrefix(stackConfiguration.getStackInstanceNumber()));
		data.setVmId(VirtualMachineIdProvider.getVMID());
		data.setQueryString(request.getQueryString());
		data.setOauthClientId(getOAuthClientId(request));
		if (HttpAuthUtil.usesBasicAuthentication(request)) {
			data.setBasicAuthUsername(HttpAuthUtil.getBasicAuthenticationCredentials(request).get().getUserName());
		}
		// push the session id to the logging thread context
		ThreadContext.put(SESSION_ID, data.getSessionId());
		// Bind this record to this thread.
		threadToRecordMap.put(Thread.currentThread().getId(), data);
		return true;
	}

	@Override
	public void postHandle(HttpServletRequest request,
			HttpServletResponse response, Object handler, ModelAndView arg3)
			throws Exception {
		// Nothing to do here
	}

	/**
	 * This is called after a controller returns.
	 */
	@Override
	public void afterCompletion(HttpServletRequest request,
			HttpServletResponse response, Object handler, Exception exception)
			throws Exception {
		// Get the record for this thread
		AccessRecord data = threadToRecordMap.remove(Thread.currentThread()
				.getId());
		if (data == null)
			throw new IllegalStateException(
					"Failed to get the access record for this thread: "
							+ Thread.currentThread().getId());
		// Calculate the elapse time
		data.setElapseMS(clock.currentTimeMillis() - data.getTimestamp());
		// If there is an exception then it failed.
		int status = response.getStatus();
		data.setSuccess(exception == null && status >= 200 && status <= 299);
		data.setResponseStatus(new Long(status));
		// Save this record
		accessRecorder.save(data);
		// Clear the logging thread context
		ThreadContext.clearAll();
	}

	@Override
	public void setReturnObjectId(String returneObjectId) {
		// Set this value on the current thread's access
		getCurrentThreadAccessRecord().setReturnObjectId(returneObjectId);
	}
	
	/**
	 * Get the current AccessRecord for this thread.
	 * @return
	 */
	private AccessRecord getCurrentThreadAccessRecord(){
		AccessRecord ar = threadToRecordMap.get(Thread.currentThread().getId());
		if(ar == null) 	throw new IllegalStateException(
				"Failed to get the access record for this thread: "
						+ Thread.currentThread().getId());
		return ar;
	}

}
