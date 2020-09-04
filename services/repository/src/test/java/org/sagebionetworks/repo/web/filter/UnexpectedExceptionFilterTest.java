package org.sagebionetworks.repo.web.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.MetricUtils;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.google.common.collect.ImmutableList;

@ExtendWith(MockitoExtension.class)
public class UnexpectedExceptionFilterTest {

	@Mock
	private StackConfiguration mockStackConfig;
	@Mock
	private Consumer mockConsumer;
	@Mock
	private HttpServletRequest mockRequest;
	@Mock
	private HttpServletResponse mockResponse;
	@Mock
	private FilterChain mockChain;
	@Mock
	private PrintWriter mockWriter;
	@Mock
	private HandlerExceptionResolver mockExceptionResolver;
	
	@InjectMocks
	private UnexpectedExceptionFilter filter;
	
	@Captor
	private ArgumentCaptor<List<ProfileData>> dataCaptor;

	private String stackInstance = "stackInstance";
	private String requestUri = "/some/path";
	
	@BeforeEach
	public void before() {
		filter.configureExceptionResolvers(Collections.singletonList(mockExceptionResolver));
	}
	
	@Test
	public void testWithNoException() throws IOException, ServletException{
		filter.doFilter(mockRequest, mockResponse, mockChain);
		// The response should not be changed
		verify(mockResponse, never()).setStatus(anyInt());
		verify(mockResponse, never()).getWriter();
	}

	@Test
	public void testWithUnresolvedException() throws IOException, ServletException {
		ServletException exception = new ServletException("Exception");
		
		doThrow(exception).when(mockChain).doFilter(any(), any());
		
		when(mockRequest.getRequestURI()).thenReturn(requestUri);
		when(mockExceptionResolver.resolveException(any(), any(), any(), any())).thenReturn(null);
		when(mockResponse.getWriter()).thenReturn(mockWriter);
		when(mockStackConfig.getStackInstance()).thenReturn(stackInstance);
		
		// Call under test
		filter.doFilter(mockRequest, mockResponse, mockChain);
		
		verify(mockConsumer).addProfileData(dataCaptor.capture());
		
		List<ProfileData> profileData = dataCaptor.getValue();
		
		List<ProfileData> expectedData = ImmutableList.of(
				expectedProfileData(profileData.get(0).getTimestamp(), exception, false, requestUri),
				expectedProfileData(profileData.get(1).getTimestamp(), exception, true, requestUri)
		);		
		
		assertEquals(expectedData, profileData);
	
		verify(mockExceptionResolver).resolveException(mockRequest, mockResponse, null, exception);
		verify(mockResponse).setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
		verify(mockResponse).setContentType(MediaType.APPLICATION_JSON_VALUE);
		verify(mockResponse).getWriter();
		verify(mockWriter).println(AuthorizationConstants.REASON_SERVER_ERROR);
	}
	
	@Test
	public void testWithResolvedException() throws IOException, ServletException {
		ServletException exception = new ServletException("Exception");
		
		doThrow(exception).when(mockChain).doFilter(any(), any());
		
		when(mockRequest.getRequestURI()).thenReturn(requestUri);
		when(mockExceptionResolver.resolveException(any(), any(), any(), any())).thenReturn(new ModelAndView());
		when(mockStackConfig.getStackInstance()).thenReturn(stackInstance);
		
		// Call under test
		filter.doFilter(mockRequest, mockResponse, mockChain);
		
		verify(mockConsumer).addProfileData(dataCaptor.capture());
		
		List<ProfileData> profileData = dataCaptor.getValue();
		
		List<ProfileData> expectedData = ImmutableList.of(
				expectedProfileData(profileData.get(0).getTimestamp(), exception, false, requestUri),
				expectedProfileData(profileData.get(1).getTimestamp(), exception, true, requestUri)
		);		
		
		assertEquals(expectedData, profileData);
	
		verify(mockExceptionResolver).resolveException(mockRequest, mockResponse, null, exception);
		verifyNoMoreInteractions(mockResponse);
	}
	
	private ProfileData expectedProfileData(Date timestamp, Throwable ex, boolean withMessage, String uri) {
		ProfileData logEvent = new ProfileData();

		logEvent.setNamespace("UnexpectedExceptionFilter - " + stackInstance);
		logEvent.setName("UnhandledException");
		logEvent.setValue(1.0);
		logEvent.setUnit(StandardUnit.Count.toString());
		logEvent.setTimestamp(timestamp);
		
		Map<String, String> dimensions = new HashMap<>();

		logEvent.setDimension(dimensions);
		
		dimensions.put("exceptionClass", ex.getClass().getName());
		dimensions.put("requestUri", uri);
		
		if (withMessage) {
			dimensions.put("message", MetricUtils.stackTracetoString(ex));
		}
		
		return logEvent;
	}
}
