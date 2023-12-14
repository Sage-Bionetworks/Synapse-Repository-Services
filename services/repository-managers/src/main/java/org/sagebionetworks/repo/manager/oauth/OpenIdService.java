package org.sagebionetworks.repo.manager.oauth;

import org.scribe.model.Token;
import org.scribe.oauth.OAuthService;

public interface OpenIdService extends OAuthService {

	/**
	 * @param accessToken
	 * @return The user info endpoint content
	 */
	ProvidedUserInfo getUserInfo(Token accessToken);
	
}
