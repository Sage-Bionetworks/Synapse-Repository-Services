package org.sagebionetworks.repo.manager.oauth;

import org.sagebionetworks.repo.model.oauth.OAuthProvider;
import org.sagebionetworks.repo.model.oauth.ProvidedUserInfo;

/**
 * Abstraction for authenticating users using third party OauthProviders.
 * 
 * @author John
 *
 */
public interface OAuthLoginManager {
	/**
	 * After a user has been authenticated at an OAuthProvider's web page, the
	 * provider will redirect the browser to the provided redirectUrl. The
	 * provider will add a query parameter to the redirectUrl called "code" that
	 * represent the authorization code for the user. This method will use the
	 * authorization code to validate the user and fetch information about the
	 * user from the OAuthProvider.
	 * 
	 * @param provider
	 *            The OAuthProvder that authenticated the user.
	 * @param authorizationCode
	 *            The value of the "code" query parameter included with the
	 *            redirectUrl.
	 * @return Information about the user provided by the OAuthProvider.
	 */
	public ProvidedUserInfo validateUserWithProvider(OAuthProvider provider,
			String authorizationCode, String redirectUrl);
	
	/**
	 * Get the binding for a provider.
	 * @param provider
	 * @return
	 */
	public OAuthLoginProviderBinding getLoginProviderBinding(OAuthProvider provider);

}
