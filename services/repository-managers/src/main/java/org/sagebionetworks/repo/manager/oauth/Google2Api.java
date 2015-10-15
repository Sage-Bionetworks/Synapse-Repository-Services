package org.sagebionetworks.repo.manager.oauth;

public class Google2Api extends OAuth2Api {
    private static final String AUTHORIZE_URL = "https://accounts.google.com/o/oauth2/auth?response_type=code&client_id=%s&redirect_uri=%s";
    private static final String TOKEN_URL = "https://accounts.google.com/o/oauth2/token";

	public Google2Api() {
		setAuthorizationEndpoint(AUTHORIZE_URL);
		setAccessTokenEndpoint(TOKEN_URL);
	}

}
