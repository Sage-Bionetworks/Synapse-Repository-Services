package org.sagebionetworks.repo.web.controller;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.repo.web.UrlHelpers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
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
