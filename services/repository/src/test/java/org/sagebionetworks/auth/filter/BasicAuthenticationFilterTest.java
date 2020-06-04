package org.sagebionetworks.auth.filter;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.cloudwatch.ProfileData;

@ExtendWith(MockitoExtension.class)
public class BasicAuthenticationFilterTest {
	
	@Mock
	private HttpServletRequest mockRequest;
	
	@Mock
	private HttpServletResponse mockResponse;
	
	@Mock
	private FilterChain mockFilterChain;
	
	@Mock
	private FilterHelper mockFilterHelper;
	
	private BasicAuthenticationFilter filter;
	
	@Captor 
	private ArgumentCaptor<List<ProfileData>> captorProfileData;
	
	private static final String USER = "user";
	private static final String PASS = "pass";
	private static final String ENCODED = Base64.getEncoder().encodeToString((USER + ":" + PASS).getBytes());
	
	@BeforeEach
	public void before() {
		filter = Mockito.mock(BasicAuthenticationFilter.class, withSettings()
					.useConstructor(mockFilterHelper)
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
		
		// Call under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		
		//verifyRejectRequestPassingException();
		verifyRejectRequest("Missing required credentials in the authorization header.");
	}
	
	@Test
	public void testDoFilterWithInvalidHeader() throws Exception {
		when(mockRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Some invalid header");
		
		// Call under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		
		verifyRejectRequestPassingException("Invalid Authorization header for basic authentication (Missing \"Basic \" prefix)");
	}
	
	@Test
	public void testDoFilterWithInvalideEncodingHeader() throws Exception {
		when(mockRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic " + ENCODED + "___");
		
		// Call under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		
		verifyRejectRequestPassingException("Invalid Authorization header for basic authentication (Malformed Base64 encoding: Illegal base64 character 5f)");
	}
	
	@Test
	public void testDoFilterWithValidCredentials() throws Exception {
		
		when(mockRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic " + ENCODED);
		when(filter.credentialsRequired()).thenReturn(true);
		
		// Call under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);

		verify(filter).validateCredentialsAndDoFilterInternal(eq(mockRequest), eq(mockResponse), eq(mockFilterChain), any());
	}
	
	private void verifyRejectRequest(String message) throws IOException, ServletException {
		verify(mockFilterHelper).rejectRequest(false, mockResponse, message);
		verify(filter, never()).validateCredentialsAndDoFilterInternal(eq(mockRequest), eq(mockResponse), eq(mockFilterChain), any());
	}
	
	private void verifyRejectRequestPassingException(String message) throws IOException, ServletException {
		ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
		verify(mockFilterHelper).rejectRequest(eq(false), eq(mockResponse), captor.capture());
		assertEquals(message, captor.getValue().getMessage());
		verify(filter, never()).validateCredentialsAndDoFilterInternal(eq(mockRequest), eq(mockResponse), eq(mockFilterChain), any());
	}
	
}
