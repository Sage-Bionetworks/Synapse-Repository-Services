package org.sagebionetworks.repo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.model.AuthorizationConstants.SYNAPSE_AUTHORIZATION_HEADER_NAME;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.oauth.ClaimsJsonUtil;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenHelper;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.impl.DefaultClaims;

@ExtendWith(MockitoExtension.class)
class OAuthScopeInterceptorTest {

	@Mock
	private OIDCTokenHelper mockOidcTokenHelper;

	@InjectMocks
	private OAuthScopeInterceptor oauthScopeInterceptor;
	
	@Mock
	private HttpServletRequest mockRequest; 
	
	@Mock
	private HttpServletResponse mockResponse; 
	
	@Mock
	private HandlerMethod mockHandler;
	
	@Mock
	private MethodParameter mockUserIdParameter;
	
	@Mock
	private MethodParameter mockAccessTokenHeader;
	
	@Mock
	private Jwt<JwsHeader, Claims> mockJwt;
	
	@Mock
	private RequestParam mockRequestParam;
	
	@Mock
	private RequestHeader mockRequestHeader;
	
	private static final String USER_ID = "100001";
	private static final String ACCESS_TOKEN = "access-token";
	
	private static RequiredScope createRequiredScopeAnnotation(final OAuthScope[] scopes) {
		return new RequiredScope() {
			@Override
			public Class<? extends Annotation> annotationType() {return RequiredScope.class;}
			@Override
			public OAuthScope[] value() {return scopes;}
		};
	}
	
	private static Claims createClaimsForScope(final OAuthScope[] scopes) {
		DefaultClaims result = new DefaultClaims();
		ClaimsJsonUtil.addAccessClaims( Arrays.asList(scopes), 
				Collections.EMPTY_MAP, result);
		return result;
	}
	
	@BeforeEach
	void before() {
	}
	
	// mock an annotated method with a userId parameter
	private void mockRequiredScopeAnnotation() {
		RequiredScope requiredScopeAnnotation = createRequiredScopeAnnotation(OAuthScope.values());
		when(mockHandler.getMethodAnnotation(RequiredScope.class)).thenReturn(requiredScopeAnnotation);
	}
		
	private void mockRequestIdParam() {
		when(mockRequestParam.value()).thenReturn(AuthorizationConstants.USER_ID_PARAM);
		when(mockHandler.getMethodParameters()).thenReturn(new MethodParameter[] {mockUserIdParameter});
		when(mockUserIdParameter.getParameterAnnotation(RequestParam.class)).thenReturn(mockRequestParam);
	}
	
	private void mockRequest(String userId, String accessToken) {
		if (accessToken!=null) {
			when(mockRequest.getHeader(SYNAPSE_AUTHORIZATION_HEADER_NAME)).thenReturn("Bearer "+accessToken);
		}
		when(mockRequest.getHeader(AuthorizationConstants.SYNAPSE_HEADER_SERVICE_NAME)).thenReturn(null);
		when(mockRequest.getParameter(AuthorizationConstants.USER_ID_PARAM)).thenReturn(userId); 	
	}
	
	private void mockAccessToken(OAuthScope[] scopes) {
		when(mockOidcTokenHelper.parseJWT(ACCESS_TOKEN)).thenReturn(mockJwt);
		when(mockJwt.getBody()).thenReturn(createClaimsForScope(scopes));		
	}
	
	private OutputStream mockResponse() throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		when(mockResponse.getWriter()).thenReturn(new PrintWriter(os));
		return os;
	}
	
	@Test
	void testHasUserIdParameterOrAccessTokenHeader_withUserId() {
		when(mockHandler.getMethodParameters()).thenReturn(new MethodParameter[] {mockUserIdParameter});
		when(mockUserIdParameter.getParameterAnnotation(RequestParam.class)).thenReturn(mockRequestParam);
		when(mockRequestParam.value()).thenReturn( AuthorizationConstants.USER_ID_PARAM);

		// method under test
		assertTrue(OAuthScopeInterceptor.hasUserIdParameterOrAccessTokenHeader(mockHandler));
	}
	
	@Test
	void testHasUserIdParameterOrAccessTokenHeader_withAccessToken() {
		when(mockHandler.getMethodParameters()).thenReturn(new MethodParameter[] {mockAccessTokenHeader});
		when(mockAccessTokenHeader.getParameterAnnotation(RequestParam.class)).thenReturn(null);
		when(mockAccessTokenHeader.getParameterAnnotation(RequestHeader.class)).thenReturn(mockRequestHeader);
		when(mockRequestHeader.value()).thenReturn( AuthorizationConstants.SYNAPSE_AUTHORIZATION_HEADER_NAME);

		// method under test
		assertTrue(OAuthScopeInterceptor.hasUserIdParameterOrAccessTokenHeader(mockHandler));
	}
	
	@Test
	void testHasUserIdParameterOrAccessTokenHeader_Nothing() {
		when(mockHandler.getMethodParameters()).thenReturn(new MethodParameter[] {});
		// method under test
		assertFalse(OAuthScopeInterceptor.hasUserIdParameterOrAccessTokenHeader(mockHandler));
	}
	
	@Test
	void testIsAnonymous_anonymousId() {
		when(mockRequest.getParameter(AuthorizationConstants.USER_ID_PARAM)).
			thenReturn(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId().toString());
		assertTrue(OAuthScopeInterceptor.isAnonymous(mockRequest));
	}
	
	@Test
	void testIsAnonymous_missingId() {
		assertTrue(OAuthScopeInterceptor.isAnonymous(mockRequest));
	}
	
	@Test
	void testIsAnonymous_NOT_anonymous() {
		when(mockRequest.getParameter(AuthorizationConstants.USER_ID_PARAM)).thenReturn("123");
		assertFalse(OAuthScopeInterceptor.isAnonymous(mockRequest));
	}
	
	@Test
	void testPrehandleAnonymous() throws Exception {
		mockRequest(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId().toString(), null);// anonymous, no access token
		
		// method under test
		boolean result = oauthScopeInterceptor.preHandle(mockRequest, mockResponse, mockHandler);
		
		assertTrue(result);
		
		verify(mockRequest).getHeader(AuthorizationConstants.SYNAPSE_HEADER_SERVICE_NAME);
		verify(mockRequest).getParameter(AuthorizationConstants.USER_ID_PARAM);
		
		verifyNoMoreInteractions(mockRequest);
		verifyZeroInteractions(mockResponse);
		verifyZeroInteractions(mockHandler);
	}

	@Test
	void testPrehandleAnonymous_handlerWrongType() throws Exception {
		mockRequest("123", null);// NOT anonymous
		
		// method under test
		assertThrows(IllegalStateException.class, 
				() -> {oauthScopeInterceptor.preHandle(mockRequest, mockResponse, String.class);
		});

	}

	@Test
	void testPrehandleNoUserIdORAccessTokenParameter() throws Exception {
		mockRequest(null, null);// anonymous, no access token

		// method under test
		boolean result = oauthScopeInterceptor.preHandle(mockRequest, mockResponse, mockHandler);
		
		assertTrue(result);
	}

	@Test
	void testPrehandleHappyCase() throws Exception {
		mockRequiredScopeAnnotation();
		mockRequestIdParam();
		mockRequest(USER_ID, ACCESS_TOKEN);// NOT anonymous	
		mockAccessToken(OAuthScope.values());
		
		// method under test
		boolean result = oauthScopeInterceptor.preHandle(mockRequest, mockResponse, mockHandler);
		
		assertTrue(result);
		
		verify(mockHandler).getMethodAnnotation(RequiredScope.class);
		verify(mockRequest).getHeader(SYNAPSE_AUTHORIZATION_HEADER_NAME);
		verify(mockOidcTokenHelper).parseJWT(ACCESS_TOKEN);
	}

	@Test
	void testPrehandleNoScopeAnnotation() throws Exception {
		mockRequest("123", null);// NOT anonymous
		mockRequestIdParam();
		when(mockHandler.getMethodAnnotation(RequiredScope.class)).thenReturn(null);
		
		// method under test
		assertThrows(IllegalStateException.class, ()->{
			oauthScopeInterceptor.preHandle(mockRequest, mockResponse, mockHandler);
		});
		
		verify(mockHandler).getMethodAnnotation(RequiredScope.class);
		verify(mockRequest, never()).getHeader(SYNAPSE_AUTHORIZATION_HEADER_NAME);
		verify(mockOidcTokenHelper, never()).parseJWT(ACCESS_TOKEN);
	}

	@Test
	void testPrehandleInsufficentScope() throws Exception {
		mockRequiredScopeAnnotation();
		mockRequestIdParam();
		
		mockRequest(USER_ID, ACCESS_TOKEN);// NOT anonymous	
		mockAccessToken(new OAuthScope[] {OAuthScope.view});
		OutputStream os = mockResponse();
		
		// method under test
		boolean result = oauthScopeInterceptor.preHandle(mockRequest, mockResponse, mockHandler);
		
		assertFalse(result);
		
		verify(mockRequest).getHeader(SYNAPSE_AUTHORIZATION_HEADER_NAME);

		String expectedError = OAuthErrorCode.insufficient_scope.name();
		String expectedErrorDescription = "Request lacks scope(s) required by this service: authorize, download, modify, offline_access, openid";
		String expectedReason = expectedError + ". " + expectedErrorDescription;

		assertEquals("{\"reason\":\"" + expectedReason + "\",\"error\":\"" + expectedError + "\",\"error_description\":\"" + expectedErrorDescription + "\"}"+System.lineSeparator() , os.toString());
		
	}

	@Test
	void testPrehandleNoAccessToken() throws Exception {
		mockRequiredScopeAnnotation();
		mockRequestIdParam();
		
		mockRequest(USER_ID, null);// NOT anonymous, no access token
		OutputStream os = mockResponse();
		
		// method under test
		boolean result = oauthScopeInterceptor.preHandle(mockRequest, mockResponse, mockHandler);
		
		assertFalse(result);

		String expectedError = OAuthErrorCode.insufficient_scope.name();
		String expectedErrorDescription = "Request lacks scope(s) required by this service: authorize, download, modify, offline_access, openid, view";
		String expectedReason = expectedError + ". " + expectedErrorDescription;
		assertEquals("{\"reason\":\"" + expectedReason + "\",\"error\":\"" + expectedError + "\",\"error_description\":\"" + expectedErrorDescription + "\"}" + System.lineSeparator(),  os.toString());
		
		verify(mockRequest).getHeader(SYNAPSE_AUTHORIZATION_HEADER_NAME);
		verify(mockOidcTokenHelper, never()).parseJWT(anyString());
		verify(mockHandler).getMethodAnnotation(RequiredScope.class);
	}

	@Test
	void testPrehandleInvalidAccessToken() throws Exception {
		mockRequiredScopeAnnotation();
		mockRequestIdParam();
		
		mockRequest(USER_ID, ACCESS_TOKEN);// NOT anonymous, no access token
		
		String message = "the error message";

		when(mockOidcTokenHelper.parseJWT(ACCESS_TOKEN)).thenThrow(new IllegalArgumentException(message));
		
		OutputStream os = mockResponse();
		
		// method under test
		boolean result = oauthScopeInterceptor.preHandle(mockRequest, mockResponse, mockHandler);
		
		assertFalse(result);

		assertEquals("{\"reason\":\""+message+"\"}"+System.lineSeparator(),  os.toString());
		
		verify(mockRequest).getHeader(SYNAPSE_AUTHORIZATION_HEADER_NAME);
		verify(mockOidcTokenHelper).parseJWT(anyString());
		verify(mockHandler).getMethodAnnotation(RequiredScope.class);
	}
	
	@Test
	void testPrehandleWithServiceCall() throws Exception {
		when(mockRequest.getHeader(AuthorizationConstants.SYNAPSE_HEADER_SERVICE_NAME)).thenReturn("serviceName"); 	

		// method under test
		boolean result = oauthScopeInterceptor.preHandle(mockRequest, mockResponse, mockHandler);
		
		assertTrue(result);
		
		verify(mockRequest).getHeader(AuthorizationConstants.SYNAPSE_HEADER_SERVICE_NAME);
		verifyZeroInteractions(mockResponse);
		verifyZeroInteractions(mockHandler);
	}
}
