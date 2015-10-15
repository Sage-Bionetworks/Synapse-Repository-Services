package org.sagebionetworks.repo.manager.oauth;

public class ORCID2Api extends OAuth2Api {
    private static final String AUTHORIZE_URL = "https://orcid.org/oauth/authorize?response_type=code&client_id=%s&redirect_uri=%s";
    private static final String TOKEN_URL = "https://pub.orcid.org/oauth/token";

    public ORCID2Api() {
		setAuthorizationEndpoint(AUTHORIZE_URL);
		setAccessTokenEndpoint(TOKEN_URL);
	}

}
