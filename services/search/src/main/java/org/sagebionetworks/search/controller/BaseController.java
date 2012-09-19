package org.sagebionetworks.search.controller;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.conn.ConnectTimeoutException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.error.ErrorResponse;
import org.sagebionetworks.utils.HttpClientHelperException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 *  This base controller handles exceptions.
 * @author jmhill
 *
 */
public  abstract class BaseController {
	
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
	ErrorResponse handleServiceUnavailableException(Exception ex, HttpServletRequest request) {
		return handleException(ex, request);
	}
	
	@ExceptionHandler(ConnectTimeoutException.class)
	@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
	public @ResponseBody
	ErrorResponse handleException(ConnectTimeoutException ex, HttpServletRequest request) {
		// Convert to a DatastoreException
		DatastoreException ds = new DatastoreException("ConnectTimeoutException while connecting to the search index.");
		return handleException(ds, request);
	}
	
	@ExceptionHandler(HttpClientHelperException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public @ResponseBody
	ErrorResponse handleException(HttpClientHelperException ex, HttpServletRequest request) {
		// Convert to a DatastoreException
		int index = ex.getMessage().indexOf("\"message\":");
		String message = "Unknown";
		if(index > 0){
			message = ex.getMessage().substring(index, ex.getMessage().length());
		}
		IllegalArgumentException ds = new IllegalArgumentException("Invalid request: "+message);
		return handleException(ds, request);
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
	protected ErrorResponse handleException(Throwable ex,
			HttpServletRequest request) {
		log.log(Level.WARNING, "Handling " + request.toString(), ex);
		String message = ex.getMessage();
		ErrorResponse response = new ErrorResponse();
		response.setReason(message);
		return response;
	}
}
