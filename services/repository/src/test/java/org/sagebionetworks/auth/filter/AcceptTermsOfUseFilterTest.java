package org.sagebionetworks.auth.filter;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.auth.services.AuthenticationService;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;


@ExtendWith(MockitoExtension.class)
class AcceptTermsOfUseFilterTest {
	@Mock
	private HttpServletRequest mockRequest;
	@Mock
	private HttpServletResponse mockResponse;
	@Mock
	private FilterChain mockFilterChain;
	@Mock
	private PrintWriter mockPrintWriter;
	
	@Mock
	private AuthenticationService mockAuthService;
	
	@InjectMocks
	private AcceptTermsOfUseFilter filter;
	
	private static final Long userId = 101L;

	@BeforeEach
	public void beforeEach() {	
	}
	
	@Test
	void testHASAcceptedTermsOfUse() throws Exception {
		when(mockRequest.getParameter("userId")).thenReturn(userId.toString());
		when(mockAuthService.hasUserAcceptedTermsOfUse(userId)).thenReturn(true);
		
		// method under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		
		verify(mockAuthService).hasUserAcceptedTermsOfUse(userId);
		verify(mockFilterChain).doFilter(mockRequest, mockResponse);
	}
	
	@Test
	void testAnonymous() throws Exception {
		when(mockRequest.getParameter("userId")).thenReturn(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId().toString());
		
		// method under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		
		verify(mockAuthService, never()).hasUserAcceptedTermsOfUse(userId);
		verify(mockFilterChain).doFilter(mockRequest, mockResponse);
	}
	
	@Test
	public void testHasNOTAcceptedTermsOfUse() throws Exception {
		when(mockRequest.getParameter("userId")).thenReturn(userId.toString());
		when(mockResponse.getWriter()).thenReturn(mockPrintWriter);

		// method under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		
		verify(mockAuthService).hasUserAcceptedTermsOfUse(userId);
		
		ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
		verify(mockResponse).setStatus((Integer)captor.capture());
		assertEquals(new Integer(HttpStatus.SC_FORBIDDEN), captor.getValue());
		verify(mockPrintWriter).println("{\"reason\":\"Terms of use have not been signed.\"}");
		
		verify(mockFilterChain, never()).doFilter(any(), any());
	}

	@Test
	public void testMissingUserId() throws Exception {
		when(mockResponse.getWriter()).thenReturn(mockPrintWriter);

		// method under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		
		verify(mockAuthService, never()).hasUserAcceptedTermsOfUse(anyLong());

		ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
		verify(mockResponse).setStatus((Integer)captor.capture());
		assertEquals(new Integer(HttpStatus.SC_INTERNAL_SERVER_ERROR), captor.getValue());
		verify(mockPrintWriter).println("{\"reason\":\"Missing user id.\"}");

		verify(mockFilterChain, never()).doFilter(any(), any());
	}

}
