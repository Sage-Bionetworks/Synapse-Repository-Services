package org.sagebionetworks.repo.web.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.ThreadContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.web.AccessInterceptor;

/**
 * Test that we capture what is expected from the header.
 * @author John
 *
 */
public class HttpServletRequestDataTest {
	
	HttpServletRequest mockRequest;
	
	@Before
	public void before() throws IOException{
		mockRequest = Mockito.mock(HttpServletRequest.class);
	}

	@Test
	public void testURI(){
		String uri = "/rep/v1/test";
		when(mockRequest.getRequestURI()).thenReturn(uri);
		HttpServletRequestData data = new HttpServletRequestData(mockRequest);
		assertTrue(data.toString().contains(uri));
		assertEquals(uri, data.getUri());
	}
	
	@Test
	public void testUserId(){
		String userId = "1234";
		when(mockRequest.getParameter(AuthorizationConstants.USER_ID_PARAM)).thenReturn(userId);
		HttpServletRequestData data = new HttpServletRequestData(mockRequest);
		assertTrue(data.toString().contains(userId));
		assertEquals(userId, data.getUserIdString());
	}
	
	@Test
	public void testMethod(){
		String method = "GET";
		when(mockRequest.getMethod()).thenReturn(method);
		HttpServletRequestData data = new HttpServletRequestData(mockRequest);
		assertTrue(data.toString().contains(method));
		assertEquals(method, data.getMethod());
	}
	
	@Test
	public void testThreadId(){
		long threadId = Thread.currentThread().getId();
		HttpServletRequestData data = new HttpServletRequestData(mockRequest);
		assertTrue(data.toString().contains(""+threadId));
		assertEquals(threadId, data.getThreadId());
	}
	
	@Test
	public void testSessionToken(){
		String token = "someToken";
		when(mockRequest.getHeader(AuthorizationConstants.SESSION_TOKEN_PARAM)).thenReturn(token);
		HttpServletRequestData data = new HttpServletRequestData(mockRequest);
		assertTrue(data.toString().contains(token));
		assertEquals(token, data.getSessionToken());
	}
	
	@Test
	public void testSessionId(){
		String sessionId = "someSessionId";
		ThreadContext.put(AccessInterceptor.SESSION_ID, sessionId);
		HttpServletRequestData data = new HttpServletRequestData(mockRequest);
		assertTrue(data.toString().contains(sessionId));
		assertEquals(sessionId, data.getSessionId());
	}
}
