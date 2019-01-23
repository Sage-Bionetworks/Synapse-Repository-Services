package org.sagebionetworks.repo.web.controller;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * This controller handles all unmapped paths and returns a 404 (Not Found) error.
 */
@Controller
@RequestMapping("/")
public class UnmappedPathsController extends BaseController{
	@ResponseStatus(HttpStatus.NOT_FOUND)
	@RequestMapping(path = "/**", method = {
			RequestMethod.GET,
			RequestMethod.HEAD,
			RequestMethod.POST,
			RequestMethod.PUT,
			RequestMethod.PATCH,
			RequestMethod.DELETE,
			RequestMethod.OPTIONS,
			RequestMethod.TRACE})
	public @ResponseBody ErrorResponse return404(HttpServletRequest request){
		ErrorResponse errorResponse = new ErrorResponse();
		errorResponse.setReason("Resource not found. Please reference our API documentation at https://docs.synapse.org/rest/");
		return errorResponse;
	}
}
