package org.sagebionetworks.repo.manager.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.oauth.OAuthProvider;
import org.sagebionetworks.repo.model.oauth.ProvidedUserInfo;
import org.sagebionetworks.repo.model.principal.AliasType;

public class OAuthManagerImplUnitTest {
	
	private static OAuthManagerImpl oauthManager;
	private static OAuthProviderBinding providerBinding;
	private static OAuthProviderBinding nihProviderBinding;
	private static OAuthProvider ORCID_PROVIDER_ENUM = OAuthProvider.ORCID;
	private static OAuthProvider NIH_PROVIDER_ENUM = OAuthProvider.NIH_OAUTH_2_0;

	@BeforeAll
	public static void beforeAll() throws Exception {
		oauthManager = new OAuthManagerImpl();
		Map<OAuthProvider, OAuthProviderBinding> providerMap = new HashMap<OAuthProvider, OAuthProviderBinding>();
		providerBinding = Mockito.mock(OAuthProviderBinding.class);
		nihProviderBinding = Mockito.mock(OAuthProviderBinding.class);
		providerMap.put(ORCID_PROVIDER_ENUM, providerBinding);
		providerMap.put(NIH_PROVIDER_ENUM, nihProviderBinding);
		oauthManager.setProviderMap(providerMap);
	}

	@Test
	public void testGetAuthorizationUrl() {
		String redirUrl = "redirectUrl";
		String expected = "http://foo.bar.com?response_type=code&redirect_uri="+redirUrl;
		when(providerBinding.getAuthorizationUrl(redirUrl)).thenReturn(expected);
		assertEquals(expected, oauthManager.getAuthorizationUrl(ORCID_PROVIDER_ENUM, redirUrl, null));
	}

	
	@Test
	public void testGetAuthorizationUrlWithState() {
		String redirUrl = "redirectUrl";
		String state = "some state#@%";
		String authUrl = "http://foo.bar.com?response_type=code&redirect_uri="+redirUrl;
		String expected = authUrl+"&state="+URLEncoder.encode(state);
		when(providerBinding.getAuthorizationUrl(redirUrl)).thenReturn(authUrl);
		assertEquals(expected, oauthManager.getAuthorizationUrl(ORCID_PROVIDER_ENUM, redirUrl, state));
	}

	@Test
	public void testGetNIHAuthorizationUrlWithState() {
		String redirUrl = "redirectUrl";
		String state = "some state#@%";
		String authUrl = "http://foo.bar.com?response_type=code&redirect_uri="+redirUrl;
		String expected = authUrl+"&state="+URLEncoder.encode(state);
		when(nihProviderBinding.getAuthorizationUrl(redirUrl)).thenReturn(authUrl);
		assertEquals(expected, oauthManager.getAuthorizationUrl(NIH_PROVIDER_ENUM, redirUrl, state));
	}

	
	@Test
	public void testValidateUserWithProvider() {
		String authCode = "xxx";
		String redirUrl = "redirectUrl";
		ProvidedUserInfo expected = new ProvidedUserInfo();
		expected.setUsersVerifiedEmail("foo@bar.com");
		when(providerBinding.validateUserWithProvider(authCode, redirUrl)).thenReturn(expected);
		assertEquals(expected, oauthManager.validateUserWithProvider(ORCID_PROVIDER_ENUM, authCode, redirUrl));
	}

	
	@Test
	public void testRetrieveProvidersId() {
		String authCode = "xxx";
		String redirUrl = "redirectUrl";
		AliasAndType expected = new AliasAndType("ID", AliasType.USER_ORCID);
		when(providerBinding.retrieveProvidersId(authCode, redirUrl)).thenReturn(expected);
		assertEquals(expected, oauthManager.retrieveProvidersId(ORCID_PROVIDER_ENUM, authCode, redirUrl));
	}

}
