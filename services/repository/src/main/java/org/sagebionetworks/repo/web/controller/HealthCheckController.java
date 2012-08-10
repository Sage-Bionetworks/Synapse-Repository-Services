package org.sagebionetworks.repo.web.controller;

import org.sagebionetworks.repo.web.UrlHelpers;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;

//import java.lang.management.ManagementFactory;
//import java.lang.management.MemoryMXBean;
//import java.lang.management.MemoryUsage;
//import java.lang.management.ThreadMXBean;
//import java.lang.management.ThreadInfo;

/**
 *
 * @author xschildw
 */
@Controller
public class HealthCheckController extends BaseController {
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(
			value=UrlHelpers.HEALTHCHECK,
			method=RequestMethod.HEAD
			)
	public void checkAmznHealthHead() {
		// Per discussion with John, just implement basic for now.
	}
	
}
