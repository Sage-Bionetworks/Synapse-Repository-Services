package org.sagebionetworks.repo.web;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.sagebionetworks.repo.model.ResponseData;
import org.sagebionetworks.repo.model.ResponseDataExtractorUtil;
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
	AccessResponseDataListener listener;

	/**
	 * Look at all return objects ma
	 * @param returnValue
	 */
	@AfterReturning(pointcut = "@annotation(org.springframework.web.bind.annotation.ResponseBody)", returning = "responseBody")
	public void inspectResponseBody(Object responseBody) {
		// extract the ID and concreteType from the response body
		ResponseData responseData = ResponseDataExtractorUtil.getResponseData(responseBody);
			listener.setResponseData(responseData);
		}
	}
