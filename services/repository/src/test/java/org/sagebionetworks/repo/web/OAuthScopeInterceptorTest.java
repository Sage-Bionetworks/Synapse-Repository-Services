package org.sagebionetworks.repo.web;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.model.AuthorizationConstants.SYNAPSE_AUTHORIZATION_HEADER_NAME;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.oauth.ClaimsJsonUtil;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenHelper;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.web.controller.RequiredScope;
import org.springframework.web.method.HandlerMethod;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.impl.DefaultClaims;

@ExtendWith(MockitoExtension.class)
class OAuthScopeInterceptorTest {

	@Mock
	private OIDCTokenHelper oidcTokenHelper;

	@InjectMocks
	private OAuthScopeInterceptor oauthScopeInterceptor;
	
	@Mock
	HttpServletRequest mockRequest; 
	
	@Mock
	HttpServletResponse mockResponse; 
	
	@Mock
	HandlerMethod mockHandler;
	
	@Mock
	Jwt<JwsHeader, Claims> mockJwt;
	
	io.jsonwebtoken.Claims CLAIMS;
	
	private static final String ACCESS_TOKEN = "access-token";
	
	private static RequiredScope createRequiredScopeAnnotation(final OAuthScope[] scopes) {
		return new RequiredScope() {
			@Override
			public Class<? extends Annotation> annotationType() {return RequiredScope.class;}
			@Override
			public OAuthScope[] scope() {return scopes;}
		};
	}
	
	private static Claims createClaimsForScope(final OAuthScope[] scopes) {
		DefaultClaims result = new DefaultClaims();
		ClaimsJsonUtil.addAccessClaims( Arrays.asList(scopes), 
				Collections.EMPTY_MAP, result);
		return result;
	}
	
	@Test
	void testHappyCase() throws Exception {
		RequiredScope requiredScopeAnnotation = createRequiredScopeAnnotation(OAuthScope.values());
			
		when(mockHandler.getMethodAnnotation(RequiredScope.class)).thenReturn(requiredScopeAnnotation);
		when(mockRequest.getHeader(SYNAPSE_AUTHORIZATION_HEADER_NAME)).thenReturn("Bearer "+ACCESS_TOKEN);
		when(oidcTokenHelper.parseJWT(ACCESS_TOKEN)).thenReturn(mockJwt);
		when(mockJwt.getBody()).thenReturn(createClaimsForScope(OAuthScope.values()));
		
		// method under test
		boolean result = oauthScopeInterceptor.preHandle(mockRequest, mockResponse, mockHandler);
		
		assertTrue(result);
	}

}
