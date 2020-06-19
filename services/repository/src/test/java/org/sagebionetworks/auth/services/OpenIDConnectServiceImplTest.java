package org.sagebionetworks.auth.services;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenHelper;
import org.sagebionetworks.repo.manager.oauth.OpenIDConnectManager;
import org.sagebionetworks.repo.model.oauth.OAuthGrantType;
import org.sagebionetworks.repo.model.oauth.OAuthResponseType;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OIDCSigningAlgorithm;
import org.sagebionetworks.repo.model.oauth.OIDCSubjectIdentifierType;
import org.sagebionetworks.repo.model.oauth.OIDConnectConfiguration;

import com.google.common.collect.ImmutableList;

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
		assertEquals(Arrays.asList(OAuthGrantType.authorization_code, OAuthGrantType.refresh_token), config.getGrant_types_supported());
		assertEquals(Collections.singletonList(OIDCSigningAlgorithm.RS256), config.getId_token_signing_alg_values_supported());
		assertEquals(OAUTH_ENDPOINT, config.getIssuer());
		assertEquals(OAUTH_ENDPOINT+"/oauth2/jwks", config.getJwks_uri());
		assertEquals(OAUTH_ENDPOINT+"/oauth2/client", config.getRegistration_endpoint());
		assertEquals(Collections.singletonList(OAuthResponseType.code), config.getResponse_types_supported());
		assertEquals(OAUTH_ENDPOINT + "/oauth2/revoke", config.getRevocation_endpoint());
		assertEquals(Arrays.asList(OAuthScope.values()), config.getScopes_supported());
		assertEquals("https://docs.synapse.org", config.getService_documentation());
		assertEquals(Collections.singletonList(OIDCSubjectIdentifierType.pairwise), config.getSubject_types_supported());
		assertEquals(OAUTH_ENDPOINT+"/oauth2/token", config.getToken_endpoint());
		assertEquals(OAUTH_ENDPOINT+"/oauth2/userinfo", config.getUserinfo_endpoint());
		assertEquals(Collections.singletonList(OIDCSigningAlgorithm.RS256), config.getUserinfo_signing_alg_values_supported());
		assertEquals(ImmutableList.of("client_secret_basic","client_secret_post"), config.getToken_endpoint_auth_methods_supported());
	}
	
	@Test
	public void testGetTokenResponse() throws Exception {
		String verifiedClientId="101";
		String authorizationCode = "xyz";
		String redirectUri = "https://someRedirectUri.com/redir";
		
		// method under test
		oidcServiceImpl.getTokenResponse(verifiedClientId, OAuthGrantType.authorization_code, authorizationCode, redirectUri, 
				null, null, OAUTH_ENDPOINT);
		verify(oidcManager).generateTokenResponseWithAuthorizationCode(authorizationCode, verifiedClientId, redirectUri, OAUTH_ENDPOINT);
	}
	
	@Test
	public void testGetUserInfo() throws Exception {
		String accessToken = "acess token";
		
		// method under test
		oidcServiceImpl.getUserInfo(accessToken, OAUTH_ENDPOINT);
		
		verify(oidcManager).getUserInfo(accessToken, OAUTH_ENDPOINT);
	}

}
