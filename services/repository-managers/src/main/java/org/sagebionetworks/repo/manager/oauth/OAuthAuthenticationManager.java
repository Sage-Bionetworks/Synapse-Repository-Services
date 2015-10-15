package org.sagebionetworks.repo.manager.oauth;

import org.sagebionetworks.repo.model.oauth.OAuthProvider;

/**
 * Abstraction for authenticating users using third party OauthProviders.
 * 
 * @author John
 *
 */
public interface OAuthAuthenticationManager {

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
	 * @param provider
	 *            The OAuthProvder to be used for authentication.
	 * @param redirectUrl
	 *            The URL that the OAuthProvider will redirect to after
	 *            successful authentication.
	 * @return The url to the OAuthProvider's authentication web page.
	 */
	public String getAuthorizationUrl(OAuthProvider provider, String redirectUrl);

	/**
	 * Get the binding for a provider.
	 * @param provider
	 * @return
	 */
	public OAuthAuthenticationProviderBinding getAuthenticationProviderBinding(OAuthProvider provider);
}
