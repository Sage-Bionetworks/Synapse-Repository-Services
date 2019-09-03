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

	String createOIDCIdToken(String issuer, String subject, String oauthClientId, long now, String nonce,
			Long authTimeSeconds, String tokenId, Map<OIDCClaimName, String> userInfo);

	String createOIDCaccessToken(String issuer, String subject, String oauthClientId, long now, Long authTimeSeconds,
			String tokenId, List<OAuthScope> scopes, Map<OIDCClaimName, OIDCClaimsRequestDetails> oidcClaims);

	JsonWebKeySet getJSONWebKeySet();

	/**
	 * Validate the given JWT.  If valid, return the content as a Jwt object otherwise return null
	 * @param token
	 * @return
	 */
	Jwt<JwsHeader,Claims> validateJWTSignature(String token);

}
