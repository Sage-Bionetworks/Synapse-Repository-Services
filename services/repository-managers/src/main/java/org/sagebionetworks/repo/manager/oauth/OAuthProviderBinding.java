package org.sagebionetworks.repo.manager.oauth;

import org.sagebionetworks.repo.model.oauth.ProvidedUserInfo;

/**
 * An abstraction for a single OAuthProvider. An implementation of this
 * interface will be needed for each third party OAuth provider that we support.
 * 
 * @author John
 * 
 */
public interface OAuthProviderBinding {

	/**
	 * The first step in OAuth authentication involves sending the user to
	 * authenticate on an OAuthProvider's web page. Use this method to get a
	 * properly formed URL to redirect the browser to an OAuthProvider's
	 * authentication page.
	 * 
	 * Upon successful authentication at the OAuthProvider's page, the provider
	 * will redirect the browser to the redirectURL. The provider will add a query
	 * parameter to the redirect URL named "code". The code parameter's value is
	 * an authorization code that must be provided to Synapse to validate a
	 * user.
	 * 
	 * @param redirectUrl
	 *            The URL that the OAuthProvider will redirect to after
	 *            successful authentication.
	 * @return The url to the OAuthProvider's authentication web page.
	 */
	public String getAuthorizationUrl(String redirectUrl);

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
