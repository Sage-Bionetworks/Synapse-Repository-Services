package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.BatchUpdateException;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.sagebionetworks.repo.model.ErrorResponse;
import org.sagebionetworks.repo.web.controller.ExceptionHandlers.ExceptionType;
import org.sagebionetworks.repo.web.controller.ExceptionHandlers.TestEntry;
import org.sagebionetworks.utils.HttpClientHelperException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.google.common.collect.Maps;

/**
 * @author deflaux
 *
 */
public class BaseControllerTest {


	@Test
	public void testDeadlockError(){
		EntityController controller = new EntityController();
		HttpServletRequest request = new MockHttpServletRequest();
		ErrorResponse response = controller.handleTransientDataAccessExceptions(new DeadlockLoserDataAccessException("Message",
				new BatchUpdateException()), request);
		assertEquals(BaseController.SERVICE_TEMPORARILY_UNAVAIABLE_PLEASE_TRY_AGAIN_LATER, response.getReason());
	}
	
	@Test
	public void testTransientError() {
		EntityController controller = new EntityController();
		HttpServletRequest request = new MockHttpServletRequest();
		ErrorResponse response = controller.handleTransientDataAccessExceptions(new TransientDataAccessException("Message",
				new BatchUpdateException()) {
			private static final long serialVersionUID = 1L;
		}, request);
		assertEquals(BaseController.SERVICE_TEMPORARILY_UNAVAIABLE_PLEASE_TRY_AGAIN_LATER, response.getReason());
	}

	/**
	 * 
	 */
	@Test
	public void testCSExceptionScrubbing() {

		SearchController controller = new SearchController();
		HttpServletRequest request = new MockHttpServletRequest();
		HttpClientHelperException ex = new HttpClientHelperException(
				"Connect to search-prod-20120206-vigj35bjslyimyxftqh4mludxm.us-east-1.cloudsearch.amazonaws.com:80 timed out");
		// org.apache.http.conn.ConnectTimeoutException: Connect to
		// search-prod-20120206-vigj35bjslyimyxftqh4mludxm.us-east-1.cloudsearch.amazonaws.com:80
		// timed out
		ErrorResponse response = controller.handleException(ex, request, true);
		assertEquals("search failed, try again", response.getReason());
	}

	@Test
	public void testAllExceptionHandlersTested() throws Exception {
		// this test makes sure all exception handlers are represented in the exception handler test which lives in the
		// integration test package
		Reflections reflections = new Reflections(BaseController.class, new MethodAnnotationsScanner());
		Set<Method> handlers = reflections.getMethodsAnnotatedWith(ExceptionHandler.class);
		Map<String, Integer> exceptions = Maps.newHashMap();
		for (Method handler : handlers) {
			Class<? extends Throwable>[] exceptionsThrown = handler.getAnnotation(ExceptionHandler.class).value();
			int statusCode = handler.getAnnotation(ResponseStatus.class).value().value();
			for (Class<? extends Throwable> exceptionThrown : exceptionsThrown) {
				assertNull("duplicate exception handler?", exceptions.put(exceptionThrown.getName(), statusCode));
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
