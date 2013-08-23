package org.sagebionetworks.repo.web;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.sagebionetworks.repo.model.IdExtractorUtil;
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
	AccessIdListener listener;

	/**
	 * Look at all return objects ma
	 * @param returnValue
	 */
	@AfterReturning(pointcut = "@annotation(org.springframework.web.bind.annotation.ResponseBody)", returning = "responseBody")
	public void inspectResponseBody(Object responseBody) {
		// extract the ID from the response body
		String id = IdExtractorUtil.getObjectId(responseBody);
		if(id != null){
			listener.setReturnObjectId(id);
		}
	}
	
}
