package org.sagebionetworks.docusign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.docusign.esign.client.auth.OAuth;

@ExtendWith(MockitoExtension.class)
public class DocuSignAccessTokenProviderTest {

	@Mock
	private DocuSignClientConfig mockConfig;

	private DocuSignAccessTokenProvider provider;

	@BeforeEach
	public void before() {
		provider = spy(new DocuSignAccessTokenProvider(mockConfig));
	}

	private static OAuth.OAuthToken oAuthToken(String accessToken, long expiresIn) {
		OAuth.OAuthToken token = new OAuth.OAuthToken();
		token.setAccessToken(accessToken);
		token.setExpiresIn(expiresIn);
		return token;
	}

	@Test
	public void testGetAccessTokenFetchesOnFirstCall() {
		doReturn(oAuthToken("token-1", 3600L)).when(provider).requestJwtUserToken();

		// call under test
		String token = provider.getAccessToken();

		assertEquals("token-1", token);
		verify(provider, times(1)).requestJwtUserToken();
	}

	@Test
	public void testGetAccessTokenReturnsCachedWhenNotExpired() {
		doReturn(oAuthToken("token-1", 3600L)).when(provider).requestJwtUserToken();

		// call under test
		String first = provider.getAccessToken();
		String second = provider.getAccessToken();
		String third = provider.getAccessToken();

		assertEquals("token-1", first);
		assertEquals("token-1", second);
		assertEquals("token-1", third);
		verify(provider, times(1)).requestJwtUserToken();
	}

	@Test
	public void testGetAccessTokenRefreshesWhenExpired() {
		doReturn(oAuthToken("old-token", 0L), oAuthToken("new-token", 3600L))
				.when(provider).requestJwtUserToken();

		// call under test
		String first = provider.getAccessToken();
		String second = provider.getAccessToken();

		assertEquals("old-token", first);
		assertEquals("new-token", second);
		verify(provider, times(2)).requestJwtUserToken();
	}

	@Test
	public void testInvalidateAccessTokenForcesFreshFetch() {
		doReturn(oAuthToken("token-1", 3600L), oAuthToken("token-2", 3600L))
				.when(provider).requestJwtUserToken();

		String first = provider.getAccessToken();
		provider.invalidateAccessToken();

		// call under test
		String second = provider.getAccessToken();

		assertEquals("token-1", first);
		assertEquals("token-2", second);
		verify(provider, times(2)).requestJwtUserToken();
	}
}
