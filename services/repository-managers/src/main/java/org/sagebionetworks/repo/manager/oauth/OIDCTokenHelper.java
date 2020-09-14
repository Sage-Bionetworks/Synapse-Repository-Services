package org.sagebionetworks.repo.manager.oauth;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.auth.AccessTokenRecord;
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
	 * @param authTime The timestamp for the event in which the user most recently logged in to Synapse
	 * @param tokenId a unique ID for this token
	 * @param userInfo the user claims, i.e. information about the users
	 * @return a serialized JSON Web Token
	 */
	String createOIDCIdToken(String issuer, String subject, String oauthClientId, long now, String nonce,
			Date authTime, String tokenId, Map<OIDCClaimName, Object> userInfo);

	/**
	 * Create an OIDC access token which is used as an OAuth bearer token to authorize requests.  The
	 * authority is specified by the 'scopes' and 'oidcClaims' param's.
	 * 
	 * @param issuer the token issuer, Synapse
	 * @param subject the subject of this token, the Synapse user
	 * @param oauthClientId the ID of the registered OAuth cliewnt
	 * @param now the current time stamp
	 * @param expirationTimeSeconds the time, in seconds, until the access token expires
	 * @param authTime The timestamp for the event in which the user most recently logged in to Synapse
	 * @param refreshTokenId the ID of an associated refresh token, if one exists. can be null.
	 * @param accessTokenId a unique ID for this token
	 * @param scopes the authorized scopes.  To retrieve user info' claims, the 'openid' scope is required
	 * @param oidcClaims the fine-grained details about what user info can be accessed by this access token
	 * @return a serialized JSON Web Token
	 */
	String createOIDCaccessToken(String issuer, String subject, String oauthClientId, long now, long expirationTimeSeconds, Date authTime,
			String refreshTokenId, String accessTokenId, List<OAuthScope> scopes, Map<OIDCClaimName, OIDCClaimsRequestDetails> oidcClaims);

	/**
	 * Create a personal access token which is used as a bearer token to authorize requests.  The
	 * authority is specified by the 'scopes' and 'oidcClaims' param's.
	 * @param issuer the token issuer, Synapse
	 * @param record the record of the personal access token
	 * @return a serialized JSON Web Token
	 */
	String createPersonalAccessToken(String issuer, AccessTokenRecord record);

	/**
	 * Return the *public* side of the signature keys in the stack configuration, in the JSON Web Key Set (JWKS) format
	 * @return a JSON object holding the JWKS
	 */
	JsonWebKeySet getJSONWebKeySet();

	/**
	 * Validate the given JWT.
	 * @param a serialized JSON Web Token
	 * @throws IllegalArgumentException if the token is not valid
	 */
	void validateJWT(String token);

	/**
	 * Parse and validate the given JWT
	 * @param a serialized JSON Web Token
	 * @return the parsed and validated JWT
	 * @throws org.sagebionetworks.repo.web.OAuthUnauthenticatedException if the token has expired
	 * @throws IllegalArgumentException if the token is otherwise not valid
	 */
	Jwt<JwsHeader,Claims> parseJWT(String token);

	/**
	 * This used to adapt the existing Synapse auth filter to the 
	 * scoped access token-based system.  Given a Synapse principal
	 * ID it creates a token having 'total access' to the user account,
	 * duplicating the access provided by a Synapse session token.
	 * 
	 * @param principalId
	 * @return
	 */
	String createTotalAccessToken(Long principalId);

}
