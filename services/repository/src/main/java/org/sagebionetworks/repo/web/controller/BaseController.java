package org.sagebionetworks.repo.web.controller;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.trash.EntityInTrashCanException;
import org.sagebionetworks.repo.manager.trash.ParentInTrashCanException;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AsynchJobFailedException;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ErrorResponse;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.LockedException;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.NotReadyException;
import org.sagebionetworks.repo.model.TermsOfUseException;
import org.sagebionetworks.repo.model.TooManyRequestsException;
import org.sagebionetworks.repo.model.UnauthenticatedException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUnavilableException;
import org.sagebionetworks.repo.queryparser.ParseException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.utils.HttpClientHelperException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.support.HandlerMethodInvocationException;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;
import org.springframework.web.util.NestedServletException;

/**
 * This abstract class attempts to encapsulate exception handling for exceptions
 * common to all controllers.
 * <p>
 * 
 * The basic idea is that we want exception stack traces in the service log, but
 * we don't want to return them to the user. We also want to return
 * human-readable error messages to the client in a format that the client can
 * parse such as JSON. The AnnotationMethodHandlerExceptionResolver is
 * configured with the same HttpMessageConverters as the
 * AnnotationMethodHandlerAdapter and therefore should support all the same
 * encodings.
 * <p>
 * 
 * Note that the @ExceptionHandler can take an array of exception classes to all
 * be handled via the same logic and return the same HTTP status code. I chose
 * to implement them individually so that child classes can override them
 * separately and munge exception reasons as they see fit to produce a better
 * human-readable message.
 * <p>
 * 
 * Developer Note: I tried to do this as a separate controller but it did not
 * work. It seems that the exception handling methods must be <a href=
 * "http://blog.flurdy.com/2010/07/spring-mvc-exceptionhandler-needs-to-be.html"
 * >members of the controller class.</a> In this case inheritance seemed
 * appropriate, though in general we want to prefer composition over
 * inheritance. Also note that unfortunately, this only takes care of exceptions
 * thrown by our controllers. If the exception is instead thrown before or after
 * our controller logic, a different mechanism is needed to handle the
 * exception. See default error page configuration in web.xml
 * <p>
 * 
 * Here are some examples of exceptions not handled by these methods. They
 * assume message with id 4 exists:
 * <p>
 * <ul>
 * <li>returns an error page configured via web.xml since we do not have a
 * message converter configured for this encoding: curl -i -H
 * Accept:application/javascript http://localhost:8080/repo/v1/message/4.js
 * <li>returns an error page configured via web.xml since the DispatcherServlet
 * could find no applicable handler: curl -i -H Accept:application/json
 * http://localhost:8080/repo/v1/message/4/foo
 * </ul>
 * <p>
 * 
 * TODO when our integration test framework is in place, add tests for stuff
 * managed by error pages in web.xml since we can't test that with our unit
 * tests
 * 
 * @author deflaux
 */

public abstract class BaseController {

	static final String SERVICE_TEMPORARILY_UNAVAIABLE_PLEASE_TRY_AGAIN_LATER = "Service temporarily unavailable, please try again later.";
	private static Logger log = LogManager.getLogger(BaseController.class);
	
	/**
	 * When a TableUnavilableException occurs we need to communicate the table status to the caller with a 202 ACCEPTED,
	 * indicating we accepted they call but the resource is not ready yet.
	 * @param ex
	 * @param request
	 * @return
	 */
	@ExceptionHandler(TableUnavilableException.class)
	@ResponseStatus(HttpStatus.ACCEPTED)
	public @ResponseBody
	TableStatus handleTableUnavilableException(TableUnavilableException ex, 
			HttpServletRequest request) {
		return ex.getStatus();
	}
	
	/**
	 * When a NotReadyException occurs we need to communicate the async status to the caller with a 202 ACCEPTED,
	 * indicating we accepted they call but the resource is not ready yet.
	 * 
	 * @param ex
	 * @param request
	 * @return
	 */
	@ExceptionHandler(NotReadyException.class)
	@ResponseStatus(HttpStatus.ACCEPTED)
	public @ResponseBody
	AsynchronousJobStatus handleResultNotReadyException(NotReadyException ex, HttpServletRequest request) {
		return ex.getStatus();
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public @ResponseBody
	ErrorResponse handleMissingServletRequestParameterException(MissingServletRequestParameterException ex, 
			HttpServletRequest request) {
		return handleException(ex, request, false);
	}

	/**
	 * This is an application exception thrown when the request references an
	 * entity that does not exist
	 * <p>
	 * 
	 * TODO this exception is getting generic treatment right now but we may
	 * want log this less verbosely if it becomes a normal and expected
	 * exception
	 * 
	 * @param ex
	 *            the exception to be handled
	 * @param request
	 *            the client request
	 * @return an ErrorResponse object containing the exception reason or some
	 *         other human-readable response
	 */
	@ExceptionHandler(NotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public @ResponseBody
	ErrorResponse handleNotFoundException(NotFoundException ex,
			HttpServletRequest request) {
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
	 * This is an application exception thrown when a resource was more recently
	 * updated than the version referenced in the current update request
	 * <p>
	 * 
	 * @param ex
	 *            the exception to be handled
	 * @param request
	 *            the client request
	 * @return an ErrorResponse object containing the exception reason or some
	 *         other human-readable response
	 */
	@ExceptionHandler(ConflictingUpdateException.class)
	@ResponseStatus(HttpStatus.PRECONDITION_FAILED)
	public @ResponseBody
	ErrorResponse handleConflictingUpdateException(
			ConflictingUpdateException ex, HttpServletRequest request) {
		return handleException(ex, request, false);
	}

	/**
	 * This is an application exception thrown when the user does not have
	 * sufficient authorization to perform the activity requested
	 * <p>
	 * Note that per http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html the
	 * 401 Unauthorized status code merely means that the user has not passed
	 * the data required to determine whether or not he is authorized. This is
	 * different than having the correct info and determining that the user is
	 * not authorized. Hence we return forbidden.
	 * 
	 * @param ex
	 *            the exception to be handled
	 * @param request
	 *            the client request
	 * @return an ErrorResponse object containing the exception reason or some
	 *         other human-readable response
	 */
	@ExceptionHandler(UnauthorizedException.class)
	@ResponseStatus(HttpStatus.FORBIDDEN)
	public @ResponseBody
	ErrorResponse handleUnauthorizedException(UnauthorizedException ex,
			HttpServletRequest request) {
		return handleException(ex, request, false);
	}
	
	/**
	 * This is thrown when there are problems authenticating the user.
	 * The user is usually advised to correct their credentials and try again.  
	 */
	@ExceptionHandler(UnauthenticatedException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public @ResponseBody
	ErrorResponse handleUnauthenticatedException(UnauthenticatedException ex,
			HttpServletRequest request,
			HttpServletResponse response) {
		return handleException(ex, request, false);
	}
	
	/**
	 * This exception is thrown when an entity with a given name already exists.
	 * @param ex
	 * @param request
	 * @return
	 */
	@ExceptionHandler(NameConflictException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public @ResponseBody
	ErrorResponse handleNameConflictException(NameConflictException ex,
			HttpServletRequest request) {
		return handleException(ex, request, false);
	}

	/**
	 * This exception is thrown when an async job fails.
	 * @param ex
	 * @param request
	 * @return
	 */
	@ExceptionHandler(AsynchJobFailedException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public @ResponseBody
	ErrorResponse handleAsynchJobFailedException(AsynchJobFailedException ex,
			HttpServletRequest request) {
		return handleException(ex, request, false);
	}

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
	 * This is an application exception thrown when a model object does not pass
	 * validity checks
	 * 
	 * @param ex
	 *            the exception to be handled
	 * @param request
	 *            the client request
	 * @return an ErrorResponse object containing the exception reason or some
	 *         other human-readable response
	 */
	@ExceptionHandler(InvalidModelException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public @ResponseBody
	ErrorResponse handleInvalidModelException(InvalidModelException ex,
			HttpServletRequest request) {
		return handleException(ex, request, false);
	}

	/**
	 * This occurs for example when parsing a URL for an integer but a string is
	 * found in its location
	 * 
	 * @param ex
	 *            the exception to be handled
	 * @param request
	 *            the client request
	 * @return an ErrorResponse object containing the exception reason or some
	 *         other human-readable response
	 */
	@ExceptionHandler(TypeMismatchException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public @ResponseBody
	ErrorResponse handleTypeMismatchException(TypeMismatchException ex,
			HttpServletRequest request) {
		return handleException(ex, request, false);
	}

	/**
	 * This occurs for example when one passes field names that do not exist in
	 * the object type we are trying to deserialize
	 * 
	 * @param ex
	 *            the exception to be handled
	 * @param request
	 *            the client request
	 * @return an ErrorResponse object containing the exception reason or some
	 *         other human-readable response
	 */
	@ExceptionHandler(JSONObjectAdapterException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public @ResponseBody
	ErrorResponse handleJSONObjectAdapterException(JSONObjectAdapterException ex,
			HttpServletRequest request) {
		return handleException(ex, request, false);
	}

	/**
	 * This occurs when a POST or PUT request has no body
	 * 
	 * @param ex
	 *            the exception to be handled
	 * @param request
	 *            the client request
	 * @return an ErrorResponse object containing the exception reason or some
	 *         other human-readable response
	 */
	@ExceptionHandler(EOFException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public @ResponseBody
	ErrorResponse handleEofException(EOFException ex, HttpServletRequest request) {
		return handleException(ex, request, true);
	}

	/**
	 * This occurs for example when the matched handler method expects an HTTP
	 * header not present in the request such as an ETag header.
	 * 
	 * @param ex
	 *            the exception to be handled
	 * @param request
	 *            the client request
	 * @return an ErrorResponse object containing the exception reason or some
	 *         other human-readable response
	 */
	@ExceptionHandler(HandlerMethodInvocationException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public @ResponseBody
	ErrorResponse handleMethodInvocationException(
			HandlerMethodInvocationException ex, HttpServletRequest request) {
		return handleException(ex, request, false);
	}

	/**
	 * This occurs for example when the we send invalid JSON in the request
	 * 
	 * @param ex
	 *            the exception to be handled
	 * @param request
	 *            the client request
	 * @return an ErrorResponse object containing the exception reason or some
	 *         other human-readable response
	 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public @ResponseBody
	ErrorResponse handleNotReadableException(
			HttpMessageNotReadableException ex, HttpServletRequest request) {
		return handleException(ex, request, false);
	}

	/**
	 * This occurs when the user specifies a query that the system cannot parse
	 * or handle
	 * 
	 * @param ex
	 *            the exception to be handled
	 * @param request
	 *            the client request
	 * @return an ErrorResponse object containing the exception reason or some
	 *         other human-readable response
	 */
	@ExceptionHandler(ParseException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public @ResponseBody
	ErrorResponse handleParseException(ParseException ex,
			HttpServletRequest request) {
		return handleException(ex, request, false);
	}

	/**
	 * This occurs for example when the request asks for responses to be in a
	 * content type not supported
	 * 
	 * @param ex
	 *            the exception to be handled
	 * @param request
	 *            the client request
	 * @return an ErrorResponse object containing the exception reason or some
	 *         other human-readable response
	 */
	@ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
	@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
	public @ResponseBody
	ErrorResponse handleNotAcceptableException(
			HttpMediaTypeNotAcceptableException ex, HttpServletRequest request) {
		return handleException(ex, request, false);
	}

	/**
	 * Haven't been able to get this one to happen yet
	 * 
	 * @param ex
	 *            the exception to be handled
	 * @param request
	 *            the client request
	 * @return an ErrorResponse object containing the exception reason or some
	 *         other human-readable response
	 */
	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	@ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
	public @ResponseBody
	ErrorResponse handleNotSupportedException(
			HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
		return handleException(ex, request, false);
	}

	/**
	 * Haven't been able to get this one to happen yet
	 * 
	 * @param ex
	 *            the exception to be handled
	 * @param request
	 *            the client request
	 * @return an ErrorResponse object containing the exception reason or some
	 *         other human-readable response
	 */
	@ExceptionHandler(NoSuchRequestHandlingMethodException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public @ResponseBody
	ErrorResponse handleNoSuchRequestException(
			NoSuchRequestHandlingMethodException ex, HttpServletRequest request) {
		return handleException(ex, request, false);
	}

	/**
	 * This is thrown when there are problems persisting and retrieving objects
	 * from the datastore
	 * 
	 * @param ex
	 *            the exception to be handled
	 * @param request
	 *            the client request
	 * @return an ErrorResponse object containing the exception reason or some
	 *         other human-readable response
	 */
	@ExceptionHandler(DatastoreException.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public @ResponseBody
	ErrorResponse handleDatastoreException(DatastoreException ex,
			HttpServletRequest request) {
		return handleException(ex, request, true);
	}

	/**
	 * Haven't been able to get this one to happen yet. I was assuming this
	 * might catch more exceptions that I am not handling explicitly yet.
	 * 
	 * @param ex
	 *            the exception to be handled
	 * @param request
	 *            the client request
	 * @return an ErrorResponse object containing the exception reason or some
	 *         other human-readable response
	 */
	@ExceptionHandler(ServletException.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public @ResponseBody
	ErrorResponse handleServletException(ServletException ex,
			HttpServletRequest request) {
		return handleException(ex, request, true);
	}

	/**
	 * Haven't been able to get this one to happen yet
	 * 
	 * @param ex
	 *            the exception to be handled
	 * @param request
	 *            the client request
	 * @return an ErrorResponse object containing the exception reason or some
	 *         other human-readable response
	 */
	@ExceptionHandler(NestedServletException.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public @ResponseBody
	ErrorResponse handleNestedServletException(NestedServletException ex,
			HttpServletRequest request) {
		return handleException(ex, request, true);
	}

	/**
	 * Haven't been able to get this one to happen yet
	 * 
	 * @param ex
	 *            the exception to be handled
	 * @param request
	 *            the client request
	 * @return an ErrorResponse object containing the exception reason or some
	 *         other human-readable response
	 */
	@ExceptionHandler(IllegalStateException.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public @ResponseBody
	ErrorResponse handleIllegalStateException(IllegalStateException ex,
			HttpServletRequest request) {
		return handleException(ex, request, true);
	}

	/**
	 * When this exception occurs we want to redirect the caller to the
	 * benefactor's ACL URL.
	 * 
	 * @param ex
	 * @param request
	 * @param response
	 * @return an ErrorResponse object containing the exception reason or some
	 *         other human-readable response
	 * @throws IOException
	 */
	@ExceptionHandler(ACLInheritanceException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public @ResponseBody
	ErrorResponse handleAccessControlListInheritanceException(
			ACLInheritanceException ex, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		// Build and set the redirect URL
		String message = ACLInheritanceException.DEFAULT_MSG_PREFIX
				+ UrlHelpers.createACLRedirectURL(request, ex.getBenefactorId());
		return handleException(ex, request, message, false);
	}

	/**
	 * Return a bit of the stack trace for NullPointerExceptions to help us
	 * debug
	 * 
	 * @param ex
	 * @param request
	 * @param response
	 * @return an ErrorResponse object containing the exception reason or some
	 *         other human-readable response
	 */
	@ExceptionHandler(NullPointerException.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public @ResponseBody
	ErrorResponse handleNullPointerException(NullPointerException ex,
			HttpServletRequest request, HttpServletResponse response) {
		log.error("Handling " + request.toString(), ex);
		final int MAX_STACK_TRACE_LENGTH = 256;
		String trace = stackTraceToString(ex);
		int endIndex = (MAX_STACK_TRACE_LENGTH < trace.length()) ? MAX_STACK_TRACE_LENGTH
				: trace.length();
		String message = "Send a Jira bug report to the platform team with this message: "
				+ trace.substring(0, endIndex);
		ErrorResponse er = new ErrorResponse();
		er.setReason(message);
		return er;
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
		log.error("Consider specifically handling exceptions of type "
						+ ex.getClass().getName());
		return handleException(ex, request, true);
	}
	
	/**
	 * When we encounter deadlock we do not tell the users what the error was,
	 * rather we tell them to try again later with a 503.
	 * @param ex
	 * @param request
	 * @return
	 */
	@ExceptionHandler(TransientDataAccessException.class)
	@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
	public @ResponseBody
	ErrorResponse handleTransientDataAccessExceptions(TransientDataAccessException ex,
			HttpServletRequest request) {
		log.error("Handling " + request.toString(), ex);
		ErrorResponse er = new ErrorResponse();
		er.setReason(SERVICE_TEMPORARILY_UNAVAIABLE_PLEASE_TRY_AGAIN_LATER);
		return er;
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
	 * @param fullTrace Should the full stack trace of the exception be written to the log.
	 * @return an ErrorResponse object containing the exception reason or some
	 *         other human-readable response
	 */
	ErrorResponse handleException(Throwable ex,
			HttpServletRequest request, boolean fullTrace) {

		String message = ex.getMessage();
		if (message == null) {
			message = ex.getClass().getName();
		} else {
			// Some HTTPClient exceptions include the host to which it was trying to
			// connect, just unilaterally find and replace any references to
			// cloudsearch in error messages PLFM-977
			if (0 <= message.toLowerCase().indexOf("cloudsearch")) {
				message = "search failed, try again";
			}
		}
		return handleException(ex, request, message, fullTrace);
	}

	/**
	 * Log the exception at the warning level and return an ErrorResponse object. Child classes should override this
	 * method if they want to change the behavior for all exceptions.
	 * 
	 * @param ex the exception to be handled
	 * @param request the client request
	 * @param fullTrace Should the full stack trace of the exception be written to the log.
	 * @return an ErrorResponse object containing the exception reason or some other human-readable response
	 */
	private ErrorResponse handleException(Throwable ex, HttpServletRequest request, String message, boolean fullTrace) {
		// Always log the stack trace on develop stacks
		if (fullTrace || StackConfiguration.isDevelopStack()) {
			// Print the full stack trace
			log.error("Handling " + request.toString(), ex);
		} else {
			// Only print one line
			log.error("Handling " + request.toString());
		}

		ErrorResponse er = new ErrorResponse();
		er.setReason(message);
		return er;
	}

	/**
	 * @param ex
	 * @return stack trace as a string
	 */
	public static String stackTraceToString(Throwable ex) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ex.printStackTrace(new PrintStream(baos));
		try {
			baos.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return baos.toString();
	}
	
	// TODO:  The status code in 'ex' should dictate the response status, it should not be assumed to be BAD_REQUEST
	@ExceptionHandler(HttpClientHelperException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public @ResponseBody
	ErrorResponse handleExceptionHttpClientHelperExceptio(HttpClientHelperException ex, HttpServletRequest request) {
		// Convert to a IllegalArgumentException
		String message = "Unknown";
		if (ex.getMessage() != null) {
			int index = ex.getMessage().indexOf("\"message\":");
			if (index > 0) {
				message = ex.getMessage().substring(index, ex.getMessage().length());
			}
		}
		IllegalArgumentException ds = new IllegalArgumentException("Invalid request: "+message);
		return handleException(ds, request, true);
	}

	/**
	 * When the entity is in the trash can.
	 *
	 * @param ex
	 *            the exception to be handled
	 * @param request
	 *            the client request
	 * @return an ErrorResponse object containing the exception reason or some
	 *         other human-readable response
	 */
	@ExceptionHandler(EntityInTrashCanException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public @ResponseBody
	ErrorResponse handleEntityInTrashCanException(EntityInTrashCanException ex,
			HttpServletRequest request) {
		return handleException(ex, request, true);
	}
	
	/**
	 * When an entity's parent is in the trash can.
	 *
	 * @param ex
	 *            the exception to be handled
	 * @param request
	 *            the client request
	 * @return an ErrorResponse object containing the exception reason or some
	 *         other human-readable response
	 */
	@ExceptionHandler(ParentInTrashCanException.class)
	@ResponseStatus(HttpStatus.FORBIDDEN)
	public @ResponseBody
	ErrorResponse handleParentInTrashCanException(ParentInTrashCanException ex,
			HttpServletRequest request) {
		return handleException(ex, request, true);
	}

	/**
	 * When the number of requests made to a particular service exceeds a threshold rate
	 */
	@ExceptionHandler(TooManyRequestsException.class)
	@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
	public @ResponseBody
	ErrorResponse handleTooManyRequestsException(TooManyRequestsException ex,
			HttpServletRequest request, HttpServletResponse response) {
		return handleException(ex, request, true);
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
	 * This is thrown when the requested object is being locked
	 */
	@ExceptionHandler(LockedException.class)
	@ResponseStatus(HttpStatus.LOCKED)
	public @ResponseBody
	ErrorResponse handleLockedException(LockedException ex,
			HttpServletRequest request,
			HttpServletResponse response) {
		return handleException(ex, request, false);
	}
}