package org.sagebionetworks.repo.manager.oauth;

import org.sagebionetworks.repo.model.oauth.ProvidedUserInfo;

/**
 * An abstraction for a single OAuthProvider. An implementation of this
 * interface will be needed for each third party OAuth provider that we support.
 * 
 * @author John
 * 
 */
public interface OAuthLoginProviderBinding {

	/**
	 * After a user has been authenticated at an OAuthProvider's web page, the
	 * provider will redirect the browser to the provided redirectUrl. The
	 * provider will add a query parameter to the redirectUrl called "code" that
	 * represent the authorization code for the user. This method will use the
	 * authorization code to validate the user and fetch information about the
	 * user from the OAuthProvider.
	 * 
	 * @param authorizationCode
	 *            The value of the "code" query parameter included with the
	 *            redirectUrl.
	 * @return Information about the user provided by the OAuthProvider.
	 */
	public ProvidedUserInfo validateUserWithProvider(String authorizationCode, String redirectUrl);
}
