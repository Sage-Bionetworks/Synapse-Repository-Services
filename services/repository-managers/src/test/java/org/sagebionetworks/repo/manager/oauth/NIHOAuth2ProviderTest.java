package org.sagebionetworks.repo.manager.oauth;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class NIHOAuth2ProviderTest {

    private static String clientId;
    private static String clientSecret;
    private static NIHOAuth2Provider provider;

    @BeforeAll
    public static void beforeAll() throws Exception {
        clientId = "fake_clientId";
        clientSecret = "fake_clientSecret";
        provider = new NIHOAuth2Provider(clientId, clientSecret);
    }

    @Test
    public void testGetAuthorizationUrl() {
        String redirectUrl = "https://domain.com";
        String authUrl = provider.getAuthorizationUrl(redirectUrl);
        assertEquals("https://www.auth.nih BLAH BLAH PLACE HOLDER /auth/oauth/v2/authorize&scope=fake_clientId", authUrl); // TODO: update when authorize URL is provided.
    }

    @Test
    public void testValidateUserWithProvider_NullRedirectUrl() {
        assertThrows(IllegalArgumentException.class, () -> {
            provider.getAuthorizationUrl(null);
        });
    }

    @Test
    public void testGetAliasType() {
        assertThrows(IllegalArgumentException.class, () -> {
            provider.getAliasType();
        });
    }

    @Test
    public void testRetrieveProvidersId() {
        assertThrows(IllegalArgumentException.class, () -> {
            String authorizationCode = "testAuthCode";
            String redirectUrl = "testRedirectUrl";
            provider.retrieveProvidersId(authorizationCode, redirectUrl);
        });
    }
}