package org.sagebionetworks.auth.services;

import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenHelper;
import org.sagebionetworks.repo.manager.oauth.OpenIDConnectManager;
import org.sagebionetworks.repo.model.oauth.OAuthGrantType;
import org.sagebionetworks.repo.model.oauth.OIDConnectConfiguration;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.SignedJWT;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@RunWith(MockitoJUnitRunner.class)
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
		
	}
	
	@Test
	public void testGetTokenResponse() throws Exception {
		String verifiedClientId="101";
		String authorizationCode = "xyz";
		String redirectUri = "https://someRedirectUri.com/redir";
		oidcServiceImpl.getTokenResponse(verifiedClientId, OAuthGrantType.authorization_code, authorizationCode, redirectUri, 
				null, null, null, OAUTH_ENDPOINT);
		verify(oidcManager).getAccessToken(authorizationCode, verifiedClientId, redirectUri, OAUTH_ENDPOINT);
	}
	
	@Test
	public void testGetUserInfo() throws Exception {
		Claims claims = Jwts.claims();
		claims.put("foo", "bar");
		String accessToken = Jwts.builder().setClaims(claims).
				setHeaderParam(Header.TYPE, Header.JWT_TYPE).compact();
		oidcServiceImpl.getUserInfo(accessToken, OAUTH_ENDPOINT);
	}
}
