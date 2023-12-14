package org.sagebionetworks.repo.manager.oauth;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class GoogleOAuth2ProviderTest {
	
	String apiKey;
	String apiSecret;
	OIDCConfig mockConfig;
	GoogleOAuth2Provider provider;
	
	private String authEndpoint = "https://auth_url.org";
	private String tokenEndpoint = "https://token_url.org";
	private String userInfoEndpoint = "https://userInfo_url.org";
	
	@BeforeEach
	public void before(){
		apiKey = "fake key";
		apiSecret = "fake secret";
		mockConfig = Mockito.mock(OIDCConfig.class);
		
		when(mockConfig.getAuthorizationEndpoint()).thenReturn(authEndpoint);
		when(mockConfig.getTokenEndpoint()).thenReturn(tokenEndpoint);
		when(mockConfig.getUserInfoEndpoint()).thenReturn(userInfoEndpoint);
		
		provider = new GoogleOAuth2Provider(apiKey, apiSecret, mockConfig);
	}

	
	@Test
	public void testGetAuthorizationUrl(){
		String redirectUrl = "https://domain.com";
		String authUrl = provider.getAuthorizationUrl(redirectUrl);
		assertEquals(authEndpoint + "?response_type=code&client_id=fake key&redirect_uri=https%3A%2F%2Fdomain.com&prompt=select_account&scope=openid%20profile%20email", authUrl);
	}
}
