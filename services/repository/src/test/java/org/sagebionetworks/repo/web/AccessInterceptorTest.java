package org.sagebionetworks.repo.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.audit.utils.KeyGeneratorUtil;
import org.sagebionetworks.audit.utils.VirtualMachineIdProvider;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.audit.AccessRecord;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit test for AccessInterceptor.
 * 
 * @author jmhill
 *
 */
public class AccessInterceptorTest {
	
	HttpServletRequest mockRequest;
	HttpServletResponse mockResponse;
	Object mockHandler;
	UserInfo mockUserInfo;
	StubAccessRecorder stubRecorder;
	AccessInterceptor interceptor;
	Long userId;

	@Before
	public void before() throws Exception {
		userId = 12345L;
		mockRequest = Mockito.mock(HttpServletRequest.class);
		mockResponse = Mockito.mock(HttpServletResponse.class);
		mockHandler = Mockito.mock(Object.class);
		mockUserInfo = new UserInfo(false, 123L);
		stubRecorder = new StubAccessRecorder();
		interceptor = new AccessInterceptor();
		ReflectionTestUtils.setField(interceptor, "accessRecorder", stubRecorder);
		// Setup the happy mock
		when(mockRequest.getParameter(AuthorizationConstants.USER_ID_PARAM)).thenReturn(userId.toString());
		when(mockRequest.getRequestURI()).thenReturn("/entity/syn789");
		when(mockRequest.getMethod()).thenReturn("DELETE");
		when(mockRequest.getHeader("Host")).thenReturn("localhost8080");
		when(mockRequest.getHeader("User-Agent")).thenReturn("HAL 2000");
		when(mockRequest.getHeader("X-Forwarded-For")).thenReturn("moon.org");
		when(mockRequest.getHeader("Origin")).thenReturn("http://www.example-social-network.com");
		when(mockRequest.getHeader("Via")).thenReturn("1.0 fred, 1.1 example.com");
		when(mockRequest.getQueryString()).thenReturn("?param1=foo");
	}
	
	
	
	@Test
	public void testHappyCase() throws Exception{
		long start = System.currentTimeMillis();
		// Start
		interceptor.preHandle(mockRequest, mockResponse, mockHandler);
		interceptor.setReturnObjectId("returnId");
		// Wait to add some elapse time
		Thread.sleep(100);
		// finish the call
		Exception exception = null;
		interceptor.afterCompletion(mockRequest, mockResponse, mockHandler, exception);
		// Now get the results from the stub
		assertNotNull(stubRecorder.getSavedRecords());
		assertEquals(1, stubRecorder.getSavedRecords().size());
		AccessRecord result = stubRecorder.getSavedRecords().get(0);
		assertNotNull(result);
		assertTrue(result.getTimestamp() >= start);
		assertTrue(result.getElapseMS() > 99);
		assertTrue(result.getSuccess());
		assertNotNull(result.getSessionId());
		assertEquals("/entity/syn789", result.getRequestURL());
		assertEquals("DELETE", result.getMethod());
		assertEquals("localhost8080", result.getHost());
		assertEquals("HAL 2000", result.getUserAgent());
		assertEquals("moon.org", result.getXForwardedFor());
		assertEquals("http://www.example-social-network.com", result.getOrigin());
		assertEquals("1.0 fred, 1.1 example.com", result.getVia());
		assertEquals(new Long(Thread.currentThread().getId()), result.getThreadId());
		String stackInstanceNumber = KeyGeneratorUtil.getInstancePrefix( new StackConfiguration().getStackInstanceNumber());
		assertEquals(stackInstanceNumber, result.getInstance());
		assertEquals(StackConfiguration.getStack(), result.getStack());
		assertEquals(VirtualMachineIdProvider.getVMID(), result.getVmId());
		assertEquals("?param1=foo", result.getQueryString());
		assertEquals("returnId", result.getReturnObjectId());
	}
	
	@Test
	public void testHappyCaseWithException() throws Exception{
		long start = System.currentTimeMillis();
		// Start
		interceptor.preHandle(mockRequest, mockResponse, mockHandler);
		// Wait to add some elapse time
		Thread.sleep(100);
		// finish the call
		String error = "Something went horribly wrong!!!";
		Exception exception = new IllegalArgumentException(error);
		interceptor.afterCompletion(mockRequest, mockResponse, mockHandler, exception);
		// Now get the results from the stub
		assertNotNull(stubRecorder.getSavedRecords());
		assertEquals(1, stubRecorder.getSavedRecords().size());
		AccessRecord result = stubRecorder.getSavedRecords().get(0);
		assertNotNull(result);
		assertTrue(result.getTimestamp() >= start);
		assertTrue(result.getElapseMS() > 99);
		assertFalse(result.getSuccess());
	}
}
