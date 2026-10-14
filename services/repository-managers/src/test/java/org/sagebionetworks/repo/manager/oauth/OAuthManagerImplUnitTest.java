package org.sagebionetworks.repo.manager.oauth;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.oauth.OAuthProvider;
import org.sagebionetworks.repo.model.principal.AliasType;

@ExtendWith(MockitoExtension.class)
public class OAuthManagerImplUnitTest {

	@Mock
	private OAuthProviderBinding mockProvider;

	private OAuthManagerImpl oauthManager;

	private OAuthProvider PROVIDER_ENUM = OAuthProvider.ORCID;

	@BeforeEach
	public void before() throws Exception {
		Map<OAuthProvider, OAuthProviderBinding> providerMap = Arrays.stream(OAuthProvider.values())
				.collect(Collectors.toMap(p -> p, p -> mockProvider));
		oauthManager = new OAuthManagerImpl(providerMap);
	}

	@Test
	public void testConstructorWithMissingProvider() {
		Map<OAuthProvider, OAuthProviderBinding> incompleteMap = new HashMap<>();
		incompleteMap.put(OAuthProvider.GOOGLE_OAUTH_2_0, mockProvider);

		// call under test
		assertThrows(IllegalStateException.class, () -> new OAuthManagerImpl(incompleteMap));
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
