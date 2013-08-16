package org.sagebionetworks.repo.web;

import java.rmi.dgc.VMID;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.audit.utils.KeyGeneratorUtil;
import org.sagebionetworks.audit.utils.VirtualMachineIdProvider;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.audit.AccessRecord;
import org.sagebionetworks.repo.model.audit.AccessRecorder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * This intercepter is used to audit all web-service access.
 * 
 * @author John
 * 
 */
public class AccessInterceptor implements HandlerInterceptor {

	/**
	 * This map keeps track of the current record for each thread.
	 */
	Map<Long, AccessRecord> threadToRecordMap = Collections
			.synchronizedMap(new HashMap<Long, AccessRecord>());

	@Autowired
	AccessRecorder accessRecorder;
	@Autowired
	UserManager userManager;
	
	private String instancePrefix = KeyGeneratorUtil.getInstancePrefix( new StackConfiguration().getStackInstanceNumber());
	private String stack = StackConfiguration.getStack();

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
			UserInfo user = userManager.getUserInfo(userIdString);
			try {
				data.setUserId(Long
						.parseLong(user.getIndividualGroup().getId()));
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("UserId must be a number");
			}
		}
		data.setTimestamp(System.currentTimeMillis());
		data.setRequestURL(request.getRequestURI());
		data.setMethod(request.getMethod());
		data.setThreadId(Thread.currentThread().getId());
		String sessionId = UUID.randomUUID().toString();
		data.setSessionId(sessionId);
		// capture common headers that tell us more about the user.
		data.setHost(request.getHeader("Host"));
		data.setOrigin(request.getHeader("Origin"));
		data.setUserAgent(request.getHeader("User-Agent"));
		data.setXForwardedFor(request.getHeader("X-Forwarded-For"));
		data.setVia(request.getHeader("Via"));
		data.setDate(KeyGeneratorUtil.getDateString(data.getTimestamp()));
		data.setStack(this.stack);
		data.setInstance(this.instancePrefix);
		data.setVmId(VirtualMachineIdProvider.getVMID());
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
		data.setElapseMS(System.currentTimeMillis() - data.getTimestamp());
		// If there is an exception then it failed.
		data.setSuccess(exception == null);
		// Save this record
		accessRecorder.save(data);
	}

}
