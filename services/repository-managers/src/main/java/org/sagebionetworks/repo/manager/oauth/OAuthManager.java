package org.sagebionetworks.repo.manager.oauth;

import org.sagebionetworks.repo.model.oauth.OAuthProvider;
import org.sagebionetworks.repo.model.oauth.ProvidedUserInfo;
import org.sagebionetworks.repo.model.principal.AliasType;

/**
 * Abstraction for authenticating users using third party OauthProviders.
 * 
 * @author John
 *
 */
public interface OAuthManager {

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
	 * @param state An optional string to be added by the provider to 
	 * 				the redirect URL as a request parameter.
	 * @return The url to the OAuthProvider's authentication web page.
	 */
	public String getAuthorizationUrl(OAuthProvider provider, String redirectUrl, String state);

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
	public OAuthProviderBinding getBinding(OAuthProvider provider);
	
	/**
	 * Retrieve the unique ID for the user defined by the provider
	 * 
	 * @param provider
	 * @param authorizationCode
	 * @param redirectUrl
	 * @return
	 */
	AliasAndType retrieveProvidersId(OAuthProvider provider,
			String authorizationCode, String redirectUrl);

	public AliasType getAliasTypeForProvider(OAuthProvider provider);


}
