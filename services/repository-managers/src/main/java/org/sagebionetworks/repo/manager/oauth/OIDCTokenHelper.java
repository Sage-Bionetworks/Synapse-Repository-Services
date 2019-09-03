package org.sagebionetworks.repo.manager.oauth;

import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.oauth.JsonWebKeySet;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwt;

public interface OIDCTokenHelper {

	/**
	 * Create an OIDC ID Token given the specified meta data and user claims
	 * @param issuer the token issuer, Synapse
	 * @param subject the subject of this token, the Synapse user
	 * @param oauthClientId the ID of the registered OAuth cliewnt
	 * @param now the current time stamp
	 * @param nonce a value from the client, to be returned unmodified
	 * @param authTimeSeconds The timestamp for the event in which the user most recently logged in to Synapse
	 * @param tokenId a unique ID for this token
	 * @param userInfo the user claims, i.e. information about the users
	 * @return a serialized JSON Web Token
	 */
	String createOIDCIdToken(String issuer, String subject, String oauthClientId, long now, String nonce,
			Long authTimeSeconds, String tokenId, Map<OIDCClaimName, String> userInfo);

	/**
	 * Create an OIDC access token which is used as an OAuth bearer token to authorize requests.  The
	 * authority is specified by the 'scopes' and 'oidcClaims' param's.
	 * 
	 * @param issuer the token issuer, Synapse
	 * @param subject the subject of this token, the Synapse user
	 * @param oauthClientId the ID of the registered OAuth cliewnt
	 * @param now the current time stamp
	 * @param authTimeSeconds The timestamp for the event in which the user most recently logged in to Synapse
	 * @param tokenId a unique ID for this token
	 * @param scopes the authorized scopes.  To retrieve user info' claims, the 'openid' scope is required
	 * @param oidcClaims the fine-grained details about what user info can be accessed by this access token
	 * @return a serialized JSON Web Token
	 */
	String createOIDCaccessToken(String issuer, String subject, String oauthClientId, long now, Long authTimeSeconds,
			String tokenId, List<OAuthScope> scopes, Map<OIDCClaimName, OIDCClaimsRequestDetails> oidcClaims);

	JsonWebKeySet getJSONWebKeySet();

	/**
	 * Validate the given JWT.  If valid, return the content as a Jwt object otherwise return null
	 * @param a serialized JSON Web Token
	 * @return the parsed and validated JWT or null if not valid
	 */
	Jwt<JwsHeader,Claims> validateJWTSignature(String token);

}
