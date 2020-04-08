package org.sagebionetworks.auth.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.PrintWriter;
import java.util.Base64;

import javax.servlet.FilterChain;
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
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;

import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.google.common.collect.ImmutableMap;

@ExtendWith(MockitoExtension.class)
public class BasicAuthenticationFilterTest {
	
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
	private ArgumentCaptor<ProfileData> captorProfileData;
	
	private static final String USER = "user";
	private static final String PASS = "pass";
	private static final String ENCODED = Base64.getEncoder().encodeToString((USER + ":" + PASS).getBytes());
	
	@BeforeEach
	public void before() {
		filter = Mockito.mock(BasicAuthenticationFilter.class, withSettings()
					.useConstructor(mockConsumer)
					.defaultAnswer(Answers.CALLS_REAL_METHODS)
		);
	}
	
	@Test
	public void testDoFilterInternalWithCredentialsNotRequired() throws Exception {
		when(filter.credentialsRequired()).thenReturn(false);
		
		// Call under test
		filter.doFilterInternal(mockRequest, mockResponse, mockFilterChain);
		
		verify(mockFilterChain).doFilter(mockRequest, mockResponse);
	}
	
	@Test
	public void testDoFilterInternalWithoutCredentials() throws Exception {
		when(mockResponse.getWriter()).thenReturn(mockPrintWriter);
		
		// Call under test
		filter.doFilterInternal(mockRequest, mockResponse, mockFilterChain);
		
		verifyRejectRequest();
	}
	
	@Test
	public void testDoFilterInternalWithInvalidHeader() throws Exception {
		when(mockRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Some invalid header");
		when(mockResponse.getWriter()).thenReturn(mockPrintWriter);
		
		// Call under test
		filter.doFilterInternal(mockRequest, mockResponse, mockFilterChain);
		
		verifyRejectRequest();
	}
	
	@Test
	public void testDoFilterInternalWithInvalideEncodingHeader() throws Exception {
		
		when(mockRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic " + ENCODED + "___");
		when(mockResponse.getWriter()).thenReturn(mockPrintWriter);
		
		// Call under test
		filter.doFilterInternal(mockRequest, mockResponse, mockFilterChain);
		
		verifyRejectRequest();
	}
	
	@Test
	public void testDoFilterInternalWithInvalidCredentials() throws Exception {
		
		when(mockRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic " + ENCODED);
		when(filter.credentialsRequired()).thenReturn(true);
		when(filter.validCredentials(any())).thenReturn(false);
		when(mockResponse.getWriter()).thenReturn(mockPrintWriter);
		
		// Call under test
		filter.doFilterInternal(mockRequest, mockResponse, mockFilterChain);
		
		verifyRejectRequest();
	}
	
	@Test
	public void testDoFilterInternalWithValidCredentials() throws Exception {
		
		when(mockRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic " + ENCODED);
		when(filter.credentialsRequired()).thenReturn(true);
		when(filter.validCredentials(any())).thenReturn(true);
		
		// Call under test
		filter.doFilterInternal(mockRequest, mockResponse, mockFilterChain);

		verify(mockFilterChain).doFilter(mockRequest, mockResponse);
	}
	
	@Test
	public void testRejectRequestWithNoReporting() throws Exception {
		when(filter.reportBadCredentialsMetric()).thenReturn(false);
		when(mockResponse.getWriter()).thenReturn(mockPrintWriter);
		
		filter.rejectRequest(mockResponse);
		
		verifyRejectRequest();
		
		verifyZeroInteractions(mockConsumer);
	}
	
	@Test
	public void testRejectRequestWithReporting() throws Exception {
		when(filter.reportBadCredentialsMetric()).thenReturn(true);
		when(mockResponse.getWriter()).thenReturn(mockPrintWriter);
		
		filter.rejectRequest(mockResponse);
		
		verifyRejectRequest();
		
		verify(mockConsumer).addProfileData(captorProfileData.capture());
		
		ProfileData data = captorProfileData.getValue();
		
		ProfileData expected = new ProfileData();
		
		expected.setNamespace("Authentication");
		expected.setName("BadCredentials");
		expected.setUnit(StandardUnit.Count.toString());
		expected.setValue(1.0);
		expected.setDimension(ImmutableMap.of("filterClass", filter.getClass().getName()));
		expected.setTimestamp(data.getTimestamp());
		
		assertEquals(expected, data);
	}
	
	private void verifyRejectRequest() {
		verify(filter).reportBadCredentialsMetric();
		verify(mockResponse).setStatus(HttpStatus.SC_UNAUTHORIZED);
		verify(mockPrintWriter).println("{\"reason\":\"Credentials are missing or invalid.\"}");
		verifyZeroInteractions(mockFilterChain);
	}
	
}
