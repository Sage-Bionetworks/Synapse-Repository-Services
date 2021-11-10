package org.sagebionetworks.repo.manager.oauth;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class NIHOAuth2ProviderTest {

    String clientId;
    String clientSecret;
    NIHOAuth2Provider provider;

    @Before
    public void before() throws Exception {
        clientId = "fake_clientId";
        clientSecret = "fake_clientSecret";
        provider = new NIHOAuth2Provider(clientId, clientSecret);
    }

    @Test
    public void testGetAuthorizationUrl() {
        String redirectUrl = "https://domain.com";
        String authUrl = provider.getAuthorizationUrl(redirectUrl);
        assertEquals("this is a placeholder&scope=fake_clientId", authUrl); // TODO: update when authorize URL is provided.
    }

    @Test
    public void testValidateUserWithProvider_NullRedirectUrl() {
        try {
            provider.getAuthorizationUrl(null);
            fail("IllegalArgumentException expected.");
        } catch (IllegalArgumentException ex) {
            // expected exception
        }
    }

    @Test
    public void testGetAliasType() {
        try {
            provider.getAliasType();
            fail("IllegalArgumentException expected.");
        } catch (IllegalArgumentException ex) {
            // expected exception
        }
    }

    @Test
    public void testRetrieveProvidersId() {
        try {
            String authorizationCode = "testAuthCode";
            String redirectUrl = "testRedirectUrl";
            provider.retrieveProvidersId(authorizationCode, redirectUrl);
            fail("IllegalArgumentException expected.");
        } catch (IllegalArgumentException ex) {
            // expected exception
        }
    }
}