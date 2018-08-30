package org.sagebionetworks.repo.web.filter.throttle;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.sagebionetworks.repo.web.filter.throttle.ThrottleUtils.generateCloudwatchProfiledata;
import static org.sagebionetworks.repo.web.filter.throttle.ThrottleUtils.setResponseError;
import static org.sagebionetworks.repo.web.filter.throttle.ThrottleUtils.isMigrationAdmin;
import static org.sagebionetworks.repo.web.filter.throttle.ThrottleUtils.JSON_HTTP_CONTENT_TYPE;
import static org.sagebionetworks.repo.web.filter.throttle.ThrottleUtils.UTF8_ENCODING;


import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.repo.model.AuthorizationConstants;

import javax.servlet.http.HttpServletResponse;

@RunWith(MockitoJUnitRunner.class)
public class ThrottleUtilTest {
	
	private static final String eventName = "Event Name";
	private static final String reason = "Why? Because I can.";
	private static final Long userId = 123456L;
	private static final String namespace = "ecapseman";
	private static final int httpError = 420;
	private static final Map<String, String> dimensions = Collections.singletonMap("UserId", userId.toString());
	@Mock
	HttpServletResponse mockResponse;
	
	@Mock
	PrintWriter mockWriter;
	
	////////////////////////////////
	// reportLockAcquireError() Tests
	////////////////////////////////
	
	@Test (expected = IllegalArgumentException.class)
	public void testReportLockAcquireErrorNullEventName(){
		generateCloudwatchProfiledata(null, namespace, dimensions);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testReportLockAcquireErrorNullFilterClass(){
		generateCloudwatchProfiledata(eventName, null, dimensions);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testReportLockAcquireErrorNullDimensions(){
		generateCloudwatchProfiledata(userId.toString(), eventName, null);
	}
	
	@Test
	public void testReportLockAcquireError(){
		ProfileData report = generateCloudwatchProfiledata(eventName, namespace, dimensions);
			
		assertEquals(eventName, report.getName());
		assertEquals(namespace, report.getNamespace());
		assertEquals(userId.toString(), report.getDimension().get("UserId"));
		assertEquals(1.0, report.getValue(), 1e-15);
		assertEquals("Count", report.getUnit());
		assertEquals(dimensions, report.getDimension());
	}
	
	////////////////////////////////////////////
	// httpTooManyRequestsErrorResponse() Tests
	////////////////////////////////////////////
	
	@Test (expected = IllegalArgumentException.class)
	public void testHttpTooManyRequestsErrorResponseNullResponse() throws IOException{
		setResponseError(null, httpError, reason);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testHttpTooManyRequestsErrorResponseNullReason() throws IOException{
		setResponseError(mockResponse, httpError, null);
	}
	
	@Test
	public void testHttpTooManyRequestsErrorResponse() throws IOException{
		Mockito.when(mockResponse.getWriter()).thenReturn(mockWriter);
		
		setResponseError(mockResponse, httpError, reason);
		//TODO: Switch to 429 http code once clients have been implemented to expect that code
		verify(mockResponse).setStatus(httpError);
		verify(mockResponse).setContentType(JSON_HTTP_CONTENT_TYPE);
		verify(mockResponse).setCharacterEncoding(UTF8_ENCODING);
		verify(mockWriter).println(reason);
	}
	
	////////////////////////////
	// isMigrationAdmin() Tests
	////////////////////////////
	
	@Test
	public void testIsMigrationAdminWhenIsAdmin(){
		assertTrue(isMigrationAdmin(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId()));
	}
	
	@Test
	public void testIsMigrationAdminWhenIsNotAdmin(){
		assertFalse(isMigrationAdmin(userId));
	}
}