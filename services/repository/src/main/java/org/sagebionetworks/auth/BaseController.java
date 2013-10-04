package org.sagebionetworks.auth;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.error.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public class BaseController {
	private static final Logger log = LogManager.getLogger(BaseController.class
			.getName());

	@ExceptionHandler(UnauthorizedException.class)
	public @ResponseBody
	ErrorResponse handleForbiddenException(UnauthorizedException ex,
			HttpServletRequest request,
			HttpServletResponse response) {
		response.setStatus(HttpStatus.FORBIDDEN.value());
		return handleException(ex, request, false);
	}


	/**
	 * Handle any exceptions not handled by specific handlers. Log an additional
	 * message with higher severity because we really do want to know what sorts
	 * of new exceptions are occurring.
	 * 
	 * @param ex
	 *            the exception to be handled
	 * @param request
	 *            the client request
	 * @return an ErrorResponse object containing the exception reason or some
	 *         other human-readable response
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
		if(fullStackTrace){
			log.error("Handling " + request.toString(), ex);
		}else{
			log.error("Handling " + request.toString());
		}

		ErrorResponse response = new ErrorResponse();
		response.setReason(ex.getMessage());
		return response;
	}


}
