package org.sagebionetworks.repo.web.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
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

import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.google.common.collect.ImmutableMap;

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
	@InjectMocks
	private UnexpectedExceptionFilter filter;
	
	@Captor
	private ArgumentCaptor<List<ProfileData>> dataCaptor;

	private String stackInstance = "stackInstance";
	
	@Test
	public void testNoException() throws IOException, ServletException{
		filter.doFilter(mockRequest, mockResponse, mockChain);
		// The response should not be changed
		verify(mockResponse, never()).setStatus(anyInt());
		verify(mockResponse, never()).getWriter();
	}

	@Test
	public void testWithException() throws IOException, ServletException{
		ServletException exception = new ServletException("Exception");
		
		doThrow(exception).when(mockChain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
		when(mockResponse.getWriter()).thenReturn(mockWriter);
		when(mockStackConfig.getStackInstance()).thenReturn(stackInstance);
		
		// Call under test
		filter.doFilter(mockRequest, mockResponse, mockChain);
		
		// The response should not be changed
		verify(mockResponse).setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
		verify(mockResponse).setContentType(MediaType.APPLICATION_JSON_VALUE);
		verify(mockWriter).println(AuthorizationConstants.REASON_SERVER_ERROR);
		verify(mockConsumer).addProfileData(dataCaptor.capture());
		
		List<ProfileData> profileData = dataCaptor.getValue();
		
		assertEquals(2, profileData.size());
		assertEquals(expectedProfileData(profileData.get(0).getTimestamp(), null), profileData.get(0));
		assertEquals(expectedProfileData(profileData.get(1).getTimestamp(), exception), profileData.get(1));
	}
	
	private ProfileData expectedProfileData(Date timestamp, Throwable ex) {
		ProfileData logEvent = new ProfileData();

		logEvent.setNamespace("UnexpectedExceptionFilter - " + stackInstance);
		logEvent.setName("UnhandledException");
		logEvent.setValue(1.0);
		logEvent.setUnit(StandardUnit.Count.toString());
		logEvent.setTimestamp(timestamp);
		
		if (ex != null) {
			Map<String, String> dimensions = ImmutableMap.of(
					"exceptionClass", ex.getClass().getSimpleName(),
					"message", MetricUtils.stackTracetoString(ex)
			);
			logEvent.setDimension(dimensions);
		}
		
		return logEvent;
	}
}
