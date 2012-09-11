package org.sagebionetworks.asynchronous.workers.controller;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 *  This base controller handles exceptions.
 * @author jmhill
 *
 */
public class BaseController {
	
	private static final Logger log = Logger.getLogger(BaseController.class.getName());

	/**
	 * This exception is thrown when the service is down, or in read-only mode
	 * for non-read calls.
	 * 
	 * @param ex
	 * @param request
	 * @return an ErrorResponse object containing the exception reason or some
	 *         other human-readable response
	 */
	@ExceptionHandler(Exception.class)
	@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
	public @ResponseBody
	String handleServiceUnavailableException(Exception ex, HttpServletRequest request) {
		return handleException(ex, request);
	}
	
	/**
	 * Log the exception at the warning level and return an ErrorResponse
	 * object. Child classes should override this method if they want to change
	 * the behavior for all exceptions.
	 * 
	 * @param ex
	 *            the exception to be handled
	 * @param request
	 *            the client request
	 * @return an ErrorResponse object containing the exception reason or some
	 *         other human-readable response
	 */
	protected String handleException(Throwable ex,
			HttpServletRequest request) {
		log.log(Level.WARNING, "Handling " + request.toString(), ex);
		String message = ex.getMessage();
		return message;
	}
}
