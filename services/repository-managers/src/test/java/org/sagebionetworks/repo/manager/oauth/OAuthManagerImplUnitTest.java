package org.sagebionetworks.repo.manager.oauth;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.net.URLEncoder;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.oauth.OAuthProvider;
import org.sagebionetworks.repo.model.oauth.ProvidedUserInfo;
import org.sagebionetworks.repo.model.principal.AliasType;

@ExtendWith(MockitoExtension.class)
public class OAuthManagerImplUnitTest {
	
	@Mock
	private Map<OAuthProvider, OAuthProviderBinding> mockProviderMap;
	@InjectMocks
	private OAuthManagerImpl oauthManager;
	
	@Mock
	private OAuthProviderBinding mockProvider;
	
	private OAuthProvider PROVIDER_ENUM = OAuthProvider.ORCID;
	

	@BeforeEach
	public void before() throws Exception {
		when(mockProviderMap.get(any())).thenReturn(mockProvider);
	}

	@Test
	public void testGetAuthorizationUrl() {
		String redirUrl = "redirectUrl";
		String expected = "http://foo.bar.com?response_type=code&redirect_uri="+redirUrl;
		when(mockProvider.getAuthorizationUrl(redirUrl)).thenReturn(expected);
		assertEquals(expected, oauthManager.getAuthorizationUrl(PROVIDER_ENUM, redirUrl, null));
	}

	
	@Test
	public void testGetAuthorizationUrlWithState() {
		String redirUrl = "redirectUrl";
		String state = "some state#@%";
		String authUrl = "http://foo.bar.com?response_type=code&redirect_uri="+redirUrl;
		String expected = authUrl+"&state="+URLEncoder.encode(state);
		when(mockProvider.getAuthorizationUrl(redirUrl)).thenReturn(authUrl);
		assertEquals(expected, oauthManager.getAuthorizationUrl(PROVIDER_ENUM, redirUrl, state));
	}

	
	@Test
	public void testValidateUserWithProvider() {
		String authCode = "xxx";
		String redirUrl = "redirectUrl";
		ProvidedUserInfo expected = new ProvidedUserInfo();
		expected.setUsersVerifiedEmail("foo@bar.com");
		when(mockProvider.validateUserWithProvider(authCode, redirUrl)).thenReturn(expected);
		assertEquals(expected, oauthManager.validateUserWithProvider(PROVIDER_ENUM, authCode, redirUrl));
	}

	
	@Test
	public void testRetrieveProvidersId() {
		String authCode = "xxx";
		String redirUrl = "redirectUrl";
		AliasAndType expected = new AliasAndType("ID", AliasType.USER_ORCID);
		when(mockProvider.retrieveProvidersId(authCode, redirUrl)).thenReturn(expected);
		assertEquals(expected, oauthManager.retrieveProvidersId(PROVIDER_ENUM, authCode, redirUrl));
	}

}
