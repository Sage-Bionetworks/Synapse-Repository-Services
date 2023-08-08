package org.sagebionetworks.repo.web;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.sagebionetworks.repo.model.AccessRecordExtractorUtil;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This aspect listens to controller and gathers extra data for auditing such as the ID.
 * 
 * @author John
 *
 */
@Aspect
public class ControllerAuditAspect {

	@Autowired
	AccessRecordListener listener;

	/**
	 * Look at all return objects ma
	 *
	 * @param responseBody
	 */
	@AfterReturning(pointcut = "@annotation(org.springframework.web.bind.annotation.ResponseBody)", returning = "responseBody")
	public void inspectResponseBody(Object responseBody) {
		// extract the ID from the response body
		String id = AccessRecordExtractorUtil.getObjectId(responseBody);
		if (id != null) {
			listener.setReturnObjectId(id);
		}
	}

	/**
	 * Look at all request objects
	 *
	 * @param
	 */

	@Before("within(@org.springframework.stereotype.Controller *) && (args(.., @RequestBody requestBody))")
	public void inspectRequestBody(JoinPoint joinPoint, Object requestBody) {
		// extract the concrete type from the request body
		String concreteType = AccessRecordExtractorUtil.getConcreteType(requestBody);
		if (concreteType != null) {
			listener.setRequestConcreteType(concreteType);
		}
	}

}
