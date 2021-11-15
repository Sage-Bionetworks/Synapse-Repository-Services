package org.sagebionetworks.repo.manager.oauth;

import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.oauth.ProvidedUserInfo;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.web.OAuthException;
import org.scribe.model.OAuthConfig;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import static org.sagebionetworks.repo.manager.oauth.GoogleOAuth2Provider.parserResponseBody;

/**
 * NIH Researcher Auth Service OAuth 2.0 implementation of OAuth Provider.
 *
 */
public class NIHOAuth2Provider implements OAuthProviderBinding{

    private static final String AUTHORIZE_URL = "https://www.auth.nih BLAH BLAH PLACE HOLDER /auth/oauth/v2/authorize"; // TODO
    private static final String TOKEN_URL = "https://www.auth.nih BLAH BLAH PLACE HOLDER /auth/oauth/v2/token"; // TODO

    private static final String MESSAGE = " Message: ";
    private static final String FAILED_PREFIX = "Failed to get User's information from NIH. Code: ";
    private static final String NIH_OAUTH_USER_INFO_API_URL = "https://www.auth.nih BLAH BLAH PLACE HOLDER /openid/connect/v1.1/userinfo"; // TODO

    private String clientId;
    private String clientSecret;

    /*
     * Email scope indicates to NIH that we want to request the user's email
     * after authentication.
     */
    private static final String SCOPE_EMAIL = "email";

    public NIHOAuth2Provider(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public String getAuthorizationUrl(String redirectUrl) {
        return new OAuth2Api(AUTHORIZE_URL, TOKEN_URL).
                getAuthorizationUrl(new OAuthConfig(clientId, null, redirectUrl, null, SCOPE_EMAIL, null));
    }

    // TODO: may need to add to ProvidedUserInfo or create NIHProvidedUserInfo
    @Override
    public ProvidedUserInfo validateUserWithProvider(String authorizationCode, String redirectUrl) {
        if (redirectUrl == null) {
            throw new IllegalArgumentException("RedirectUrl cannot be null");
        }
        try {
            OAuthService service = (new OAuth2Api(AUTHORIZE_URL, TOKEN_URL)).
                    createService(new OAuthConfig(clientId, clientSecret, redirectUrl, null, null, null));
            /*
             * Get an access token from NIH using the provided authorization code.
             * This token is used to sign request for user's information.
             */
            Token accessToken = service.getAccessToken(null, new Verifier(authorizationCode));
            // Use the access token to get the UserInfo from NIH.
            OAuthRequest request = new OAuthRequest(Verb.GET, NIH_OAUTH_USER_INFO_API_URL);
            service.signRequest(accessToken, request);
            Response response = request.send();
            if (!response.isSuccessful()) {
                throw new UnauthorizedException(FAILED_PREFIX + response.getCode() + MESSAGE + response.getMessage());
            }
            return parserResponseBody(response.getBody()); // TODO: may need to write one for NIHProvidedUserInfo instead.
        } catch (OAuthException e) {
            throw new UnauthorizedException(e);
        }
    }

    @Override
    public AliasType getAliasType() {
        throw new IllegalArgumentException("Retrieving alias is not supported in Synapse for the NIH OAuth provider.");
    }

    @Override
    public AliasAndType retrieveProvidersId(String authorizationCode, String redirectUrl) {
        throw new IllegalArgumentException("Retrieving alias is not supported in Synapse for the NIH OAuth provider.");
    }
}
