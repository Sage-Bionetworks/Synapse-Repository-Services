package org.sagebionetworks.auth;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.TermsOfUseException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.error.ErrorResponse;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

public class BaseController {
	private static final Logger log = LogManager.getLogger(BaseController.class
			.getName());

	/**
	 * This is an application exception thrown when for example bad parameter
	 * values were passed
	 * 
	 * @param ex
	 *            the exception to be handled
	 * @param request
	 *            the client request
	 * @return an ErrorResponse object containing the exception reason or some
	 *         other human-readable response
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public @ResponseBody
	ErrorResponse handleIllegalArgumentException(IllegalArgumentException ex,
			HttpServletRequest request) {
		return handleException(ex, request, false);
	}

	/**
	 * This exception is thrown when an entity with a given name already exists.
	 */
	@ExceptionHandler(NameConflictException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public @ResponseBody
	ErrorResponse handleNameConflictException(NameConflictException ex,
			HttpServletRequest request) {
		return handleException(ex, request, false);
	}
	
	/**
	 * This is thrown whenever a requested object is not found
	 */
	@ExceptionHandler(NotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public @ResponseBody
	ErrorResponse handleNotFoundException(NotFoundException ex,
			HttpServletRequest request,
			HttpServletResponse response) {
		return handleException(ex, request, false);
	}
	
	/**
	 * This is thrown when there are problems authenticating the user.
	 * The user is usually advised to correct their credentials and try again.  
	 */
	@ExceptionHandler(UnauthorizedException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public @ResponseBody
	ErrorResponse handleForbiddenException(UnauthorizedException ex,
			HttpServletRequest request,
			HttpServletResponse response) {
		return handleException(ex, request, false);
	}
	
	/**
	 * This is thrown when the user has not accepted the terms of use
	 */
	@ExceptionHandler(TermsOfUseException.class)
	@ResponseStatus(HttpStatus.FORBIDDEN)
	public @ResponseBody
	ErrorResponse handleTermsOfUseException(TermsOfUseException ex,
			HttpServletRequest request,
			HttpServletResponse response) {
		return handleException(ex, request, false);
	}

	/**
	 * This exception is thrown when the service is down, or in read-only mode
	 * for non-read calls.
	 * 
	 * @param ex
	 * @param request
	 * @return an ErrorResponse object containing the exception reason or some
	 *         other human-readable response
	 */
	@ExceptionHandler(ServiceUnavailableException.class)
	@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
	public @ResponseBody
	ErrorResponse handleServiceUnavailableException(
			ServiceUnavailableException ex, HttpServletRequest request) {
		return handleException(ex, request, true);
	}
	
	/**
	 * Handle any exceptions not handled by specific handlers. Log an additional
	 * message with higher severity because we really do want to know what sorts
	 * of new exceptions are occurring.
	 */
	@ExceptionHandler(Exception.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public @ResponseBody
	ErrorResponse handleAllOtherExceptions(Exception ex,
			HttpServletRequest request) {
		return handleException(ex, request, true);
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
			HttpServletRequest request, boolean fullStackTrace) {
		if (fullStackTrace) {
			log.error("Handling exception " + ex + " from " 
					+ request.toString(), ex);
		} else {
			log.error("Handling exception " + ex + " from "
					+ request.toString());
		}

		ErrorResponse response = new ErrorResponse();
		response.setReason(ex.getMessage() + "\n");
		return response;
	}


}
