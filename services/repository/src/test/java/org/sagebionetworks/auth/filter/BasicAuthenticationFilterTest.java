package org.sagebionetworks.auth.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.MetricUtils;
import org.sagebionetworks.cloudwatch.ProfileData;

import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.google.common.collect.ImmutableList;

@ExtendWith(MockitoExtension.class)
public class BasicAuthenticationFilterTest {
	
	@Mock
	private StackConfiguration mockConfig;
	
	@Mock
	private PrintWriter mockPrintWriter;

	@Mock
	private HttpServletRequest mockRequest;
	
	@Mock
	private HttpServletResponse mockResponse;
	
	@Mock
	private FilterChain mockFilterChain;
	
	@Mock
	private Consumer mockConsumer;
	
	private BasicAuthenticationFilter filter;
	
	@Captor 
	private ArgumentCaptor<List<ProfileData>> captorProfileData;
	
	private static final String USER = "user";
	private static final String PASS = "pass";
	private static final String ENCODED = Base64.getEncoder().encodeToString((USER + ":" + PASS).getBytes());
	private static final String STACK_INSTANCE = "instance";
	
	@BeforeEach
	public void before() {
		filter = Mockito.mock(BasicAuthenticationFilter.class, withSettings()
					.useConstructor(mockConfig, mockConsumer)
					.defaultAnswer(Answers.CALLS_REAL_METHODS)
		);
	}
	
	@Test
	public void testDoFilterWithCredentialsNotRequired() throws Exception {
		when(filter.credentialsRequired()).thenReturn(false);
		
		// Call under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		
		verify(filter).validateCredentialsAndDoFilterInternal(
				eq(mockRequest), eq(mockResponse), eq(mockFilterChain), eq(Optional.empty()));
	}
	
	@Test
	public void testDoFilterWithoutCredentials() throws Exception {
		when(mockResponse.getWriter()).thenReturn(mockPrintWriter);
		
		// Call under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		
		verifyRejectRequest("{\"reason\":\"Missing required credentials in the authorization header.\"}");
	}
	
	@Test
	public void testDoFilterWithInvalidHeader() throws Exception {
		when(mockRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Some invalid header");
		when(mockResponse.getWriter()).thenReturn(mockPrintWriter);
		
		// Call under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		
		verifyRejectRequest("{\"reason\":\"Invalid Authorization header for basic authentication (Missing \\\"Basic \\\" prefix)\"}");
	}
	
	@Test
	public void testDoFilterWithInvalideEncodingHeader() throws Exception {
		when(mockRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic " + ENCODED + "___");
		when(mockResponse.getWriter()).thenReturn(mockPrintWriter);
		
		// Call under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		
		verifyRejectRequest("{\"reason\":\"Invalid Authorization header for basic authentication (Malformed Base64 encoding: Illegal base64 character 5f)\"}");
	}
	
	@Test
	public void testDoFilterWithValidCredentials() throws Exception {
		
		when(mockRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic " + ENCODED);
		when(filter.credentialsRequired()).thenReturn(true);
		
		// Call under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);

		verify(filter).validateCredentialsAndDoFilterInternal(eq(mockRequest), eq(mockResponse), eq(mockFilterChain), any());
	}
	
	@Test
	public void testRejectRequestWithNoReporting() throws Exception {
		when(filter.reportBadCredentialsMetric()).thenReturn(false);
		when(mockResponse.getWriter()).thenReturn(mockPrintWriter);
		
		filter.rejectRequest(mockResponse, "Some message");
		
		verifyRejectRequest("{\"reason\":\"Some message\"}");
		
		verifyZeroInteractions(mockConsumer);
	}
	
	@Test
	public void testRejectRequestWithReporting() throws Exception {
		when(mockConfig.getStackInstance()).thenReturn(STACK_INSTANCE);
		when(filter.reportBadCredentialsMetric()).thenReturn(true);
		when(mockResponse.getWriter()).thenReturn(mockPrintWriter);
		
		String message = "Some message";
		
		filter.rejectRequest(mockResponse, message);
		
		verifyRejectRequest("{\"reason\":\"Some message\"}");
		
		verify(mockConsumer).addProfileData(captorProfileData.capture());
		
		List<ProfileData> data = captorProfileData.getValue();
		
		assertEquals(2, data.size());
		
		List<ProfileData> expected = ImmutableList.of(
				generateProfileData(data.get(0).getTimestamp(), null),
				generateProfileData(data.get(1).getTimestamp(), message)
		);
		
		assertEquals(expected, data);
	}
	
	@Test
	public void testRejectRequestWithExceptionReporting() throws Exception {
		when(mockConfig.getStackInstance()).thenReturn(STACK_INSTANCE);
		when(filter.reportBadCredentialsMetric()).thenReturn(true);
		when(mockResponse.getWriter()).thenReturn(mockPrintWriter);
		
		IllegalArgumentException ex = new IllegalArgumentException("Some message");
		
		String message = MetricUtils.stackTracetoString(ex);
		
		filter.rejectRequest(mockResponse, ex);
		
		verifyRejectRequest("{\"reason\":\"Some message\"}");
		
		verify(mockConsumer).addProfileData(captorProfileData.capture());
		
		List<ProfileData> data = captorProfileData.getValue();
		
		assertEquals(2, data.size());
		
		List<ProfileData> expected = ImmutableList.of(
				generateProfileData(data.get(0).getTimestamp(), null),
				generateProfileData(data.get(1).getTimestamp(), message)
		);
		
		assertEquals(expected, data);
	}
	
	private ProfileData generateProfileData(Date timestamp, String message) {
		ProfileData data = new ProfileData();
		
		data.setNamespace("Authentication - " + STACK_INSTANCE);
		data.setName("BadCredentials");
		data.setUnit(StandardUnit.Count.toString());
		data.setValue(1.0);
		data.setTimestamp(timestamp);
		
		Map<String, String> dimensions = new HashMap<>();
		
		dimensions.put("filterClass", filter.getClass().getName());

		if (message != null) {
			dimensions.put("message", message);
		}
		
		data.setDimension(dimensions);
		
		return data;
		
	}
	
	private void verifyRejectRequest(String message) throws IOException, ServletException {
		verify(filter).reportBadCredentialsMetric();
		verify(mockResponse).setStatus(HttpStatus.SC_UNAUTHORIZED);
		verify(mockPrintWriter).println(message);
		verify(filter, never()).validateCredentialsAndDoFilterInternal(eq(mockRequest), eq(mockResponse), eq(mockFilterChain), any());
	}
	
}
