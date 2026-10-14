package org.sagebionetworks.auth.filter;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.service.auth.AuthenticationService;


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
	private UserInfo userInfo;

	@BeforeEach
	public void beforeEach() {	
		userInfo = new UserInfo(false, userId, "0");
		userInfo.setRealmAnonymousUserId(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
	}
	
	@Test
	void testHASAcceptedTermsOfUse() throws Exception {
		when(mockRequest.getParameter("userId")).thenReturn(userId.toString());
		when(mockRequest.getParameter("anonymous")).thenReturn("false");
		when(mockAuthService.hasUserAcceptedTermsOfService(userId)).thenReturn(true);
		
		// method under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		
		verify(mockAuthService).hasUserAcceptedTermsOfService(userId);
		verify(mockFilterChain).doFilter(mockRequest, mockResponse);
	}
	
	@Test
	void testAnonymous() throws Exception {
		Long anonId = BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId();
		when(mockRequest.getParameter("userId")).thenReturn(anonId.toString());
		when(mockRequest.getParameter("anonymous")).thenReturn("true");
		
		// method under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		
		verify(mockAuthService, never()).hasUserAcceptedTermsOfService(userId);
		verify(mockFilterChain).doFilter(mockRequest, mockResponse);
	}
	
	@Test
	public void testHasNOTAcceptedTermsOfUse() throws Exception {
		when(mockRequest.getParameter("userId")).thenReturn(userId.toString());
		when(mockRequest.getParameter("anonymous")).thenReturn("false");
		when(mockResponse.getWriter()).thenReturn(mockPrintWriter);

		// method under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		
		verify(mockAuthService).hasUserAcceptedTermsOfService(userId);
		
		ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
		verify(mockResponse).setStatus((Integer)captor.capture());
		assertEquals(new Integer(HttpStatus.SC_FORBIDDEN), captor.getValue());
		verify(mockPrintWriter).println("{\"concreteType\":\"org.sagebionetworks.repo.model.ErrorResponse\",\"reason\":\"Login to https://synapse.org to accept the latest Terms of Service.\"}");
		
		verify(mockFilterChain, never()).doFilter(any(), any());
	}

	@Test
	public void testMissingUserId() throws Exception {
		when(mockResponse.getWriter()).thenReturn(mockPrintWriter);

		// method under test
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		
		verify(mockAuthService, never()).hasUserAcceptedTermsOfService(anyLong());

		ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
		verify(mockResponse).setStatus((Integer)captor.capture());
		assertEquals(new Integer(HttpStatus.SC_INTERNAL_SERVER_ERROR), captor.getValue());
		verify(mockPrintWriter).println("{\"concreteType\":\"org.sagebionetworks.repo.model.ErrorResponse\",\"reason\":\"Missing user id.\"}");

		verify(mockFilterChain, never()).doFilter(any(), any());
	}

}
