package org.sagebionetworks.repo.web.controller;

import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.sagebionetworks.repo.model.ErrorResponse;
import org.sagebionetworks.repo.model.UnauthenticatedException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.drs.DrsErrorResponse;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.controller.ExceptionHandlers.ExceptionType;
import org.sagebionetworks.repo.web.controller.ExceptionHandlers.TestEntry;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.BatchUpdateException;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author deflaux
 *
 */
public class BaseControllerExceptionHandlerAdviceTest {

	BaseControllerExceptionHandlerAdvice controller;

	HttpServletRequest request;

	@Before
	public void setUp(){
		controller = new BaseControllerExceptionHandlerAdvice();
		request = new MockHttpServletRequest();
	}

	@Test
	public void testDeadlockError(){
		ErrorResponse response = (ErrorResponse) controller.handleTransientDataAccessExceptions(new DeadlockLoserDataAccessException("Message",
				new BatchUpdateException()), request);
		assertEquals(BaseControllerExceptionHandlerAdvice.SERVICE_TEMPORARILY_UNAVAIABLE_PLEASE_TRY_AGAIN_LATER, response.getReason());
	}

	@Test
	public void testNotFoundDrsErrorResponse(){
		final MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
		final String errorMessage = "some message";
		mockHttpServletRequest.setPathInfo(UrlHelpers.DRS_PATH);
		final DrsErrorResponse response = (DrsErrorResponse) controller.handleException(
				new NotFoundException(errorMessage), mockHttpServletRequest);
		assertEquals(errorMessage, response.getMsg());
		assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus_code().intValue());
	}
	@Test
	public void testBadRequestDrsErrorResponse(){
		final MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
		final String errorMessage = "some message";
		mockHttpServletRequest.setPathInfo(UrlHelpers.DRS_PATH);
		final DrsErrorResponse response = (DrsErrorResponse) controller.handleException(
				new IllegalArgumentException(errorMessage), mockHttpServletRequest);
		assertEquals(errorMessage, response.getMsg());
		assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus_code().intValue());
	}
	@Test
	public void testUnauthorizedDrsErrorResponse(){
		final MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
		final String errorMessage = "some message.";
		mockHttpServletRequest.setPathInfo(UrlHelpers.DRS_PATH);
		final DrsErrorResponse response = (DrsErrorResponse) controller.handleException(
				new UnauthorizedException(errorMessage), mockHttpServletRequest);
		assertEquals(errorMessage, response.getMsg());
		assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus_code().intValue());
	}

	@Test
	public void testUnauthenticatedDrsErrorResponse(){
		final MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
		final String errorMessage = "some message.";
		mockHttpServletRequest.setPathInfo(UrlHelpers.DRS_PATH);
		final DrsErrorResponse response = (DrsErrorResponse) controller.handleException(
				new UnauthenticatedException(errorMessage), mockHttpServletRequest);
		assertEquals(errorMessage, response.getMsg());
		assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus_code().intValue());
	}

	@Test
	public void testInternalServerErrorDrsErrorResponse(){
		final MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
		final String errorMessage = "some message.";
		mockHttpServletRequest.setPathInfo(UrlHelpers.DRS_PATH);
		final DrsErrorResponse response = (DrsErrorResponse) controller.handleException(
				new UnsupportedOperationException(errorMessage), mockHttpServletRequest);
		assertEquals(errorMessage, response.getMsg());
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatus_code().intValue());
	}
	
	@Test
	public void testTransientError() {
		ErrorResponse response = (ErrorResponse) controller.handleTransientDataAccessExceptions(new TransientDataAccessException("Message",
				new BatchUpdateException()) {
			private static final long serialVersionUID = 1L;
		}, request);
		assertEquals(BaseControllerExceptionHandlerAdvice.SERVICE_TEMPORARILY_UNAVAIABLE_PLEASE_TRY_AGAIN_LATER, response.getReason());
	}

	@Test
	public void testAllExceptionHandlersTested() throws Exception {
		// this test makes sure all exception handlers are represented in the exception handler test which lives in the
		// integration test package
		Reflections reflections = new Reflections(BaseControllerExceptionHandlerAdvice.class, Scanners.MethodsAnnotated);
		Set<Method> handlers = reflections.getMethodsAnnotatedWith(ExceptionHandler.class);
		Map<String, Integer> exceptions = Maps.newHashMap();
		for (Method handler : handlers) {
			Class<? extends Throwable>[] exceptionsThrown = handler.getAnnotation(ExceptionHandler.class).value();
				int statusCode = handler.getAnnotation(ResponseStatus.class).value().value();
				for (Class<? extends Throwable> exceptionThrown : exceptionsThrown) {
					assertNull("duplicate exception handler? " + exceptionThrown.getName(), exceptions.put(exceptionThrown.getName(), statusCode));
				}
		}

		for (TestEntry testEntry : ExceptionHandlers.testEntries) {
			for (ExceptionType exception : testEntry.exceptions) {
				Integer statusCode = exceptions.remove(exception.name);
				assertNotNull("Exception " + exception.name + " not handled?", statusCode);
				assertEquals(testEntry.statusCode, statusCode.intValue());
				Class<?> exceptionClass = Class.forName(exception.name);
				Class<?> expectedClass = exception.isRuntimeException ? RuntimeException.class : Exception.class;
				assertTrue(exception + " is not a " + expectedClass, expectedClass.isAssignableFrom(exceptionClass));
				if (Modifier.isAbstract(exceptionClass.getModifiers())) {
					assertNotNull("Abstract class " + exception.name + " needs concrete class", exception.concreteClassName);
				} else {
					assertNull(exception.concreteClassName);
				}
			}
		}

		assertEquals(exceptions.toString(), 0, exceptions.size());
	}
}
