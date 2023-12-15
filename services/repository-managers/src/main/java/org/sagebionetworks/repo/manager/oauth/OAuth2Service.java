package org.sagebionetworks.repo.manager.oauth;

import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

public interface OAuth2Service extends OAuthService {
	
	@Override
	AccessTokenResponse getAccessToken(Token requestToken, Verifier verifier);

}
