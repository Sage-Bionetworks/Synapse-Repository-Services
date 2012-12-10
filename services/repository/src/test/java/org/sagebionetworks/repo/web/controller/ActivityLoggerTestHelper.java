package org.sagebionetworks.repo.web.controller;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * A stub-class that is here solely to help in testing the functionality of the ActivityLogger
 * in package profiler.org.sagebionetworks.usagemetrics
 * 
 * DO NOT remove the annotation see testAnnotationsMethod for why.
 * @author geoff
 *
 */
@Controller
public class ActivityLoggerTestHelper {

	/**
	 * Fake test to keep the test suite from failing because 
	 * this class has no test methods.
	 */
	@Test
	public void fakeTest() {

	}

	public void testMethod(String arg1, Integer arg2) {
	}

	/**
	 * DO NOT CHANGE!  This method's signature and annotations are depended on by
	 * ActivityLoggerTest and ActivityLoggerAutowiredTest.
	 * @param id
	 * @param userId
	 * @return
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = {UrlHelpers.ENTITY_ID + "/special_id"},
	method = RequestMethod.GET)
	public @ResponseBody String testAnnotationsMethod(
			@PathVariable String id,
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false, defaultValue = "") String userId,
			HttpServletRequest request) {
		return "";
	}
}
