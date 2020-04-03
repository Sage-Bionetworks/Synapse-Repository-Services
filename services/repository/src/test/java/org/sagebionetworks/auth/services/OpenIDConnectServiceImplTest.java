package org.sagebionetworks.auth.services;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenHelper;
import org.sagebionetworks.repo.manager.oauth.OpenIDConnectManager;
import org.sagebionetworks.repo.model.UnauthenticatedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.oauth.OAuthGrantType;
import org.sagebionetworks.repo.model.oauth.OAuthResponseType;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OIDCSigningAlgorithm;
import org.sagebionetworks.repo.model.oauth.OIDCSubjectIdentifierType;
import org.sagebionetworks.repo.model.oauth.OIDConnectConfiguration;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.impl.DefaultJws;
import io.jsonwebtoken.impl.DefaultJwsHeader;

@ExtendWith(MockitoExtension.class)
public class OpenIDConnectServiceImplTest {
	
	@InjectMocks
	private OpenIDConnectServiceImpl oidcServiceImpl;
	
	@Mock 
	private OIDCTokenHelper oidcTokenHelper;

	@Mock 
	private OpenIDConnectManager oidcManager;

	private static final String OAUTH_ENDPOINT = "https://oauthServerEndpoint";
	
	@Test
	public void testGetOIDCConfiguration() throws Exception {
		
		// method under test
		OIDConnectConfiguration config = oidcServiceImpl.getOIDCConfiguration(OAUTH_ENDPOINT);
		
		assertEquals("http://localhost:8080/authorize", config.getAuthorization_endpoint());
		assertTrue(config.getClaims_parameter_supported());
		assertFalse(config.getClaims_supported().isEmpty());
		assertEquals(Collections.singletonList(OAuthGrantType.authorization_code), config.getGrant_types_supported());
		assertEquals(Collections.singletonList(OIDCSigningAlgorithm.RS256), config.getId_token_signing_alg_values_supported());
		assertEquals(OAUTH_ENDPOINT, config.getIssuer());
		assertEquals(OAUTH_ENDPOINT+"/oauth2/jwks", config.getJwks_uri());
		assertEquals(OAUTH_ENDPOINT+"/oauth2/client", config.getRegistration_endpoint());
		assertEquals(Collections.singletonList(OAuthResponseType.code), config.getResponse_types_supported());
		assertNull(config.getRevocation_endpoint());
		assertEquals(Arrays.asList(OAuthScope.values()), config.getScopes_supported());
		assertEquals("https://docs.synapse.org", config.getService_documentation());
		assertEquals(Collections.singletonList(OIDCSubjectIdentifierType.pairwise), config.getSubject_types_supported());
		assertEquals(OAUTH_ENDPOINT+"/oauth2/token", config.getToken_endpoint());
		assertEquals(OAUTH_ENDPOINT+"/oauth2/userinfo", config.getUserinfo_endpoint());
		assertEquals(Collections.singletonList(OIDCSigningAlgorithm.RS256), config.getUserinfo_signing_alg_values_supported());
		assertEquals(Collections.singletonList("client_secret_basic"), config.getToken_endpoint_auth_methods_supported());
	}
	
	@Test
	public void testGetTokenResponse() throws Exception {
		String verifiedClientId="101";
		String authorizationCode = "xyz";
		String redirectUri = "https://someRedirectUri.com/redir";
		
		// method under test
		oidcServiceImpl.getTokenResponse(verifiedClientId, OAuthGrantType.authorization_code, authorizationCode, redirectUri, 
				null, null, null, OAUTH_ENDPOINT);
		verify(oidcManager).getAccessToken(authorizationCode, verifiedClientId, redirectUri, OAUTH_ENDPOINT);
	}
	
	@Test
	public void testGetUserInfo() throws Exception {
		when(oidcTokenHelper.parseJWT(any(String.class))).thenReturn(new DefaultJws<Claims>(null, null, null));

		Claims claims = Jwts.claims();
		claims.put("foo", "bar");
		String clientId="101";
		claims.setAudience(clientId);
		String accessToken = Jwts.builder().setClaims(claims).
				setHeaderParam(Header.TYPE, Header.JWT_TYPE).compact();
		
		Jwt<JwsHeader, Claims> parsedToken = new DefaultJws<Claims>(new DefaultJwsHeader(), claims, "signature");
		when(oidcTokenHelper.parseJWT(accessToken)).thenReturn(parsedToken);
		
		UserInfo userAuthorization = new UserInfo(false);
		when(oidcManager.getUserAuthorization(accessToken)).thenReturn(userAuthorization);

		// method under test
		oidcServiceImpl.getUserInfo(accessToken, OAUTH_ENDPOINT);
		
		verify(oidcManager).getUserAuthorization(accessToken);
		verify(oidcTokenHelper).parseJWT(accessToken);
		verify(oidcManager).getUserInfo(userAuthorization, clientId, OAUTH_ENDPOINT);
	}
	
	@Test
	public void testGetUserInfo_badToken() throws Exception {
		Claims claims = Jwts.claims();
		claims.put("foo", "bar");
		String clientId="101";
		claims.setAudience(clientId);
		String accessToken = Jwts.builder().setClaims(claims).
				setHeaderParam(Header.TYPE, Header.JWT_TYPE).compact();

		UserInfo userAuthorization = new UserInfo(false);
		when(oidcManager.getUserAuthorization(accessToken)).thenThrow(new UnauthenticatedException("bad token"));

		assertThrows(UnauthenticatedException.class, ()->oidcServiceImpl.getUserInfo(accessToken, OAUTH_ENDPOINT));

		verify(oidcManager).getUserAuthorization(accessToken);
		verify(oidcTokenHelper, never()).parseJWT(accessToken);
		verify(oidcManager, never()).getUserInfo(userAuthorization, clientId, OAUTH_ENDPOINT);
	}
}
