package org.sagebionetworks.repo.manager.oauth;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.scribe.model.Token;

public class AccessTokenResponse extends Token {
	
	private static final Logger LOGGER = LogManager.getLogger(AccessTokenResponse.class);
	
	private String idToken;
	
	public AccessTokenResponse(String accessToken, String idToken, String rawResponse) {
		super(accessToken, "", rawResponse);
		this.idToken = idToken;
	}
	
	public String getIdToken() {
		return idToken;
	}
	
	public ProvidedUserInfo parseIdToken() {
		if (idToken == null) {
			throw new UnauthorizedException("The id_token was not included by the provider");
		}
		
		String[] idTokenParts = idToken.split("\\.");
		
		if (idTokenParts.length < 2) {
			LOGGER.warn("Malformed id_token, not enough parts: {}", idToken);
			throw new UnauthorizedException("The id_token included by the provider was malformed");
		}
				
		try {
			
			String idTokenClaims = new String(Base64.getUrlDecoder().decode(idTokenParts[1]), StandardCharsets.UTF_8);
			
			JSONObject json = new JSONObject(idTokenClaims);

			ProvidedUserInfo info = new ProvidedUserInfo();
			if (json.has(OIDCClaimName.family_name.name())) {
				info.setLastName(json.getString(OIDCClaimName.family_name.name()));
			}
			if (json.has(OIDCClaimName.given_name.name())) {
				info.setFirstName(json.getString(OIDCClaimName.given_name.name()));
			}
			if (json.has(OIDCClaimName.sub.name())) {
				info.setSubject(json.getString(OIDCClaimName.sub.name()));
			}
			if (json.has(OIDCClaimName.email_verified.name()) && json.getBoolean(OIDCClaimName.email_verified.name()) && json.has(OIDCClaimName.email.name())) {
				info.setUsersVerifiedEmail(json.getString(OIDCClaimName.email.name()));
			}
			return info;
		} catch (JSONException | IllegalArgumentException e) {
			LOGGER.warn("Malformed id_token: {}", idToken, e);
			throw new UnauthorizedException("The id_token included by the provider was malformed", e);
		}
	}

}
