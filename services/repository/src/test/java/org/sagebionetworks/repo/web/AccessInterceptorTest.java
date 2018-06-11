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
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.audit.utils.VirtualMachineIdProvider;
import org.sagebionetworks.aws.utils.s3.KeyGeneratorUtil;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.audit.AccessRecord;
import org.sagebionetworks.util.TestClock;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit test for AccessInterceptor.
 * 
 * @author jmhill
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class AccessInterceptorTest {
	
	@Mock
	HttpServletRequest mockRequest;
	@Mock
	HttpServletResponse mockResponse;
	@Mock
	StackConfiguration mockConfiguration;
	Object mockHandler;
	UserInfo mockUserInfo;
	StubAccessRecorder stubRecorder;
	AccessInterceptor interceptor;
	Long userId;
	TestClock testClock = new TestClock();
	int instanceNumber;
	String stack;

	@Before
	public void before() throws Exception {
		userId = 12345L;
		mockHandler = Mockito.mock(Object.class);
		mockUserInfo = new UserInfo(false, 123L);
		stubRecorder = new StubAccessRecorder();
		interceptor = new AccessInterceptor();
		ReflectionTestUtils.setField(interceptor, "accessRecorder", stubRecorder);
		ReflectionTestUtils.setField(interceptor, "clock", testClock);
		ReflectionTestUtils.setField(interceptor, "stackConfiguration", mockConfiguration);

		// Setup the happy mock
		when(mockRequest.getParameter(AuthorizationConstants.USER_ID_PARAM)).thenReturn(userId.toString());
		when(mockRequest.getRequestURI()).thenReturn("/entity/syn789");
		when(mockRequest.getMethod()).thenReturn("DELETE");
		when(mockRequest.getHeader("Host")).thenReturn("localhost8080");
		when(mockRequest.getHeader("User-Agent")).thenReturn("HAL 2000");
		when(mockRequest.getRemoteAddr()).thenReturn("moon.org");
		when(mockRequest.getHeader("Origin")).thenReturn("http://www.example-social-network.com");
		when(mockRequest.getHeader("Via")).thenReturn("1.0 fred, 1.1 example.com");
		when(mockRequest.getQueryString()).thenReturn("?param1=foo");
		// setup response
		when(mockResponse.getStatus()).thenReturn(200);
		instanceNumber = 101;
		when(mockConfiguration.getStackInstanceNumber()).thenReturn(instanceNumber);
		stack = "dev";
		when(mockConfiguration.getStack()).thenReturn(stack);
	}
	
	
	
	@Test
	public void testHappyCase() throws Exception{
		long start = testClock.currentTimeMillis();
		// Start
		interceptor.preHandle(mockRequest, mockResponse, mockHandler);
		interceptor.setReturnObjectId("returnId");
		// Wait to add some elapse time
		testClock.sleep(234);
		// finish the call
		Exception exception = null;
		interceptor.afterCompletion(mockRequest, mockResponse, mockHandler, exception);
		// Now get the results from the stub
		assertNotNull(stubRecorder.getSavedRecords());
		assertEquals(1, stubRecorder.getSavedRecords().size());
		AccessRecord result = stubRecorder.getSavedRecords().get(0);
		assertNotNull(result);
		assertTrue(result.getTimestamp() >= start);
		assertEquals(234L, result.getElapseMS().longValue());
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
		String stackInstanceNumber = KeyGeneratorUtil.getInstancePrefix(instanceNumber);
		assertEquals(stackInstanceNumber, result.getInstance());
		assertEquals(mockConfiguration.getStack(), result.getStack());
		assertEquals(VirtualMachineIdProvider.getVMID(), result.getVmId());
		assertEquals("?param1=foo", result.getQueryString());
		assertEquals("returnId", result.getReturnObjectId());
	}
	
	@Test
	public void testHappyCaseWithException() throws Exception{
		long start = testClock.currentTimeMillis();
		// Start
		interceptor.preHandle(mockRequest, mockResponse, mockHandler);
		// Wait to add some elapse time
		testClock.sleep(234);
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
		assertEquals(234L, result.getElapseMS().longValue());
		assertFalse(result.getSuccess());
	}
	
	@Test
	public void testStatusCode200() throws Exception{
		long start = testClock.currentTimeMillis();
		// Start
		interceptor.preHandle(mockRequest, mockResponse, mockHandler);
		// Wait to add some elapse time
		testClock.sleep(234);
		// return a 200
		// setup response
		when(mockResponse.getStatus()).thenReturn(200);
		interceptor.afterCompletion(mockRequest, mockResponse, mockHandler, null);
		// Now get the results from the stub
		assertNotNull(stubRecorder.getSavedRecords());
		assertEquals(1, stubRecorder.getSavedRecords().size());
		AccessRecord result = stubRecorder.getSavedRecords().get(0);
		assertNotNull(result);
		assertTrue(result.getTimestamp() >= start);
		assertEquals(234L, result.getElapseMS().longValue());
		assertTrue("200 is a success",result.getSuccess());
		assertEquals(new Long(200), result.getResponseStatus());
	}
	
	@Test
	public void testStatusCode400() throws Exception{
		long start = testClock.currentTimeMillis();
		// Start
		interceptor.preHandle(mockRequest, mockResponse, mockHandler);
		// Wait to add some elapse time
		testClock.sleep(234);
		// return a 200
		// setup response
		when(mockResponse.getStatus()).thenReturn(400);
		interceptor.afterCompletion(mockRequest, mockResponse, mockHandler, null);
		// Now get the results from the stub
		assertNotNull(stubRecorder.getSavedRecords());
		assertEquals(1, stubRecorder.getSavedRecords().size());
		AccessRecord result = stubRecorder.getSavedRecords().get(0);
		assertNotNull(result);
		assertTrue(result.getTimestamp() >= start);
		assertEquals(234L, result.getElapseMS().longValue());
		assertFalse("400 is not a success",result.getSuccess());
		assertEquals(new Long(400), result.getResponseStatus());
	}
}
