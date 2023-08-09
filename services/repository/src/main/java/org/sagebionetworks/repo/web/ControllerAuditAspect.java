package org.sagebionetworks.repo.web;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.sagebionetworks.repo.model.AccessRecordExtractorUtil;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

/**
 * This aspect listens to controller and gathers extra data for auditing such as the ID.
 * 
 * @author John
 *
 */
@Aspect
public class ControllerAuditAspect {
	public static final String ID = "id";
	public static final String CONCRETE_TYPE = "concreteType";
	@Autowired
	AccessRecordDataListener listener;

	/**
	 * Look at all return objects ma
	 *
	 * @param responseBody
	 */
	@AfterReturning(pointcut = "@annotation(org.springframework.web.bind.annotation.ResponseBody)", returning = "responseBody")
	public void inspectResponseBody(Object responseBody) {
		// extract the ID from the response body
		Optional<String> id = AccessRecordExtractorUtil.getObjectFieldValue(responseBody, ID);
		if (id.isPresent()) {
			listener.setReturnObjectId(id.get());
		}
	}

	/**
	 * Look at all request objects
	 *
	 * @param
	 */
	@Before("@within(org.springframework.stereotype.Controller) && " +
			"execution(* * (..,@org.springframework.web.bind.annotation.RequestBody (*),..))" )
	public void inspectRequestBody(JoinPoint joinPoint) {
		// extract the concrete type from the request body
		for( Object o : joinPoint.getArgs()){
			if(o instanceof JSONEntity){
				Optional<String> concreteType = AccessRecordExtractorUtil.getObjectFieldValue(o ,CONCRETE_TYPE);
				concreteType.ifPresent(s -> listener.setRequestConcreteType(s));
			}
		}

	}

}
