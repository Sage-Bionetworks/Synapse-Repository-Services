package org.sagebionetworks.repo.web.controller;

import java.io.EOFException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.map.JsonMappingException;
import org.sagebionetworks.repo.model.ErrorResponse;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

/**
 * This abstract class attempts to encapsulate exception handling for
 * exceptions common to all controllers.  (Note that I tried to do
 * this as a separate controller but it did not work.  In this case
 * inheritance seemed appropriate, though in general we want to prefer
 * composition over inheritance.)
 * <p>
 * The basic idea is that we want exception stack traces in the
 * service log, but we don't want to return them to the user.  We also
 * want to return human-readable error messages to the client in a
 * format that the client can parse such as JSON.  The
 * AnnotationMethodHandlerExceptionResolver is configured with the
 * same HttpMessageConverters as the AnnotationMethodHandlerAdapter
 * and therefore should support all the same encodings.
 * <p>
 * Note that the @ExceptionHandler can take an array of exception
 * classes to all be handled via the same logic and return the same
 * HTTP status code.  I chose to implement them individually so that
 * child classes can override them separately and munge exception
 * reasons as they see fit to produce a better human-readable
 * message.
 * <p>
 * TODO It seems there needs to be a default exception handling strategy for 
 * any Exception that the server generates, e.g. something that catches 
 * Throwable and logs the full stack trace at ERROR level. This would cover 
 * run-time Exceptions that we (hopefully) don't see in production with well-tested 
 * code. Then, for specific exceptions that we expect could get generated we 
 * could have less sever handling, e.g. request for a resource that doesn't 
 * exist might just log a single-line statement at the WARNING level.
 * <p>
 * TODO this still does not catch and handle all exceptions.  Consider
 * configuring SimpleMappingExceptionResolver.  More info <a href="
 * http://pietrowski.info/2010/06/spring-mvc-exception-handler/">http://pietrowski.info/2010/06/spring-mvc-exception-handler/</a>
 * <ul>
 * <li>returns an html error page that I do not control: curl -H Accept:application/javascript http://localhost:8080/message/4.js
 * <li>no handler for this, currently returns an html instead of json encoded error: curl -H Accept:application/json http://localhost:8080/message/4/foo
 * <li>notice missing double quotes around key, currently returns an html instead of json encoded error: curl -H Accept:application/json  -H Content-Type:application/json -d '{text:"this is a test"}' http://localhost:8080/message
 * </ul>
 * @author deflaux
 */

public abstract class BaseController {

    private static final Logger log = Logger.getLogger(BaseController.class.getName());

    /**
     * This is an application exception thrown when the request
     * references an entity that does not exist
     *
     * @param ex the exception to be handled
     * @param request the client request
     * @return an ErrorResponse object containing the exception reason or some other human-readable response
     */
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public @ResponseBody ErrorResponse handleNotFoundException(NotFoundException ex, HttpServletRequest request) {
        return handleException(ex, request);
    }

    /**
     * This occurs for example when parsing a URL for an integer but a
     * string is found in its location
     *
     * @param ex the exception to be handled
     * @param request the client request
     * @return an ErrorResponse object containing the exception reason or some other human-readable response
     */
    @ExceptionHandler(TypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorResponse handleTypeMismatchException(TypeMismatchException ex, HttpServletRequest request) {
        return handleException(ex, request);
    }

    /**
     * This occurs for example when one passes field names that do not
     * exist in the object type we are trying to deserialize
     *
     * @param ex the exception to be handled
     * @param request the client request
     * @return an ErrorResponse object containing the exception reason or some other human-readable response
     */
    @ExceptionHandler(JsonMappingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorResponse handleJsonMappingException(JsonMappingException ex, HttpServletRequest request) {
        return handleException(ex, request);
    }

    /**
     * This occurs when a POST or PUT request has no body
     *
     * @param ex the exception to be handled
     * @param request the client request
     * @return an ErrorResponse object containing the exception reason or some other human-readable response
     */
    @ExceptionHandler(EOFException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public @ResponseBody ErrorResponse handleEofException(EOFException ex, HttpServletRequest request) {
        return handleException(ex, request);
    }

    /**
     * This occurs for example when the request asks for responses to
     * be in a content type not supported
     *
     * @param ex the exception to be handled
     * @param request the client request
     * @return an ErrorResponse object containing the exception reason or some other human-readable response
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    @ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
    public @ResponseBody ErrorResponse handleNotAcceptableException(HttpMediaTypeNotAcceptableException ex, HttpServletRequest request) {
        return handleException(ex, request);
    }

    /**
     * Haven't been able to get this one to happen yet
     *
     * @param ex the exception to be handled
     * @param request the client request
     * @return an ErrorResponse object containing the exception reason or some other human-readable response
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public @ResponseBody ErrorResponse handleNotSupportedException(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return handleException(ex, request);
    }

    /**
     * Haven't been able to get this one to happen yet
     *
     * @param ex the exception to be handled
     * @param request the client request
     * @return an ErrorResponse object containing the exception reason or some other human-readable response
     */
    @ExceptionHandler(NoSuchRequestHandlingMethodException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public @ResponseBody ErrorResponse handleNoSuchRequestException(NoSuchRequestHandlingMethodException ex, HttpServletRequest request) {
        return handleException(ex, request);
    }

    /**
     * Haven't been able to get this one to happen yet.  I was
     * assuming this might catch more exceptions that I am not
     * handling explicitly yet.
     *
     * @param ex the exception to be handled
     * @param request the client request
     * @return an ErrorResponse object containing the exception reason or some other human-readable response
     */
    @ExceptionHandler(ServletException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public @ResponseBody ErrorResponse handleServletException(ServletException ex, HttpServletRequest request) {
        return handleException(ex, request);
    }

    /**
     * Log the exception at the warning level and return an
     * ErrorResponse object.  Child classes should override this
     * method if they want to change the behavior for all exceptions.
     *
     * @param ex the exception to be handled
     * @param request the client request
     * @return an ErrorResponse object containing the exception reason or some other human-readable response
     */
    protected ErrorResponse handleException(Throwable ex, HttpServletRequest request) {
        log.log(Level.WARNING, "Handling " + request.toString(), ex);
        return new ErrorResponse(ex.getMessage());
    }

}