package org.sagebionetworks.repo.manager.oauth;


import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

import com.google.common.collect.ImmutableMap;

import io.jsonwebtoken.Claims;

public class ClaimsJsonUtil {
	private static final String ACCESS = "access";
	private static final String SCOPE = "scope";
	private static final String USER_INFO_CLAIMS = "oidc_claims";
	private static final String NULL = "null";
	
	/*
	 * Given lists of scopes and claims, build an 'access' claim JSONObject and add it to the 
	 * 'claims' object of a JWT-encoded access token
	 */
	public static void addAccessClaims(List<OAuthScope> scopes, Map<OIDCClaimName, OIDCClaimsRequestDetails> oidcClaims, Claims claims) {		
		claims.put(ACCESS, ImmutableMap.of(SCOPE, scopes, USER_INFO_CLAIMS, oidcClaims));
	}

	/* 
	 * Given the claims from a JWT-encoded access token, extract the list of granted scopes.
	 */
	public static List<OAuthScope> getScopeFromClaims(Claims claims) {
		if (claims.containsKey(ACCESS)) {
			Map<String,Object> scopeAndClaims = (Map<String,Object>)claims.get(ACCESS, Map.class);
			if (scopeAndClaims.containsKey(SCOPE)) {
				return (List<OAuthScope>)scopeAndClaims.get(SCOPE);
			}
		}
		return Collections.EMPTY_LIST;
	}

	/* 
	 * Given the claims from a JWT-encoded access token, extract the list of granted claims.
	 */
	public static Map<OIDCClaimName, OIDCClaimsRequestDetails> getOIDCClaimsFromClaimSet(Claims claims) {
		if (claims.containsKey(ACCESS)) {
			Map<String,Object> scopeAndClaims = (Map<String,Object>)claims.get(ACCESS, Map.class);
			if (scopeAndClaims.containsKey(USER_INFO_CLAIMS)) {
				return (Map<OIDCClaimName, OIDCClaimsRequestDetails>)scopeAndClaims.get(USER_INFO_CLAIMS);
			}
		}
		return Collections.EMPTY_MAP;
	}

	/**
	 * Extract the claims and their details from a claims JSON, e.g. from a claims parameters in an OIDC authorization request.
	 * 
	 * @param claimsJson the claims, e.g. from an OIDC authorization request
	 * @return
	 */
	public static Map<OIDCClaimName, OIDCClaimsRequestDetails> getClaimsMapFromJSONObject(JSONObject claimsJson) {
		Map<OIDCClaimName, OIDCClaimsRequestDetails> result = new HashMap<OIDCClaimName, OIDCClaimsRequestDetails>();
		if (claimsJson==null) return result;
		for (String claimName : claimsJson.keySet()) {
			OIDCClaimName claim;
			try {
				claim = OIDCClaimName.valueOf(claimName);
			} catch (IllegalArgumentException e) {
				continue; // ignore unknown claims as per https://openid.net/specs/openid-connect-core-1_0.html#ClaimsParameter
			}
			String detailsString = claimsJson.getString(claimName);
			OIDCClaimsRequestDetails details = null;
			if (detailsString!=null && !NULL.equalsIgnoreCase(detailsString)) {
				details = new OIDCClaimsRequestDetails();
				try {
					JSONObjectAdapter adapter = new JSONObjectAdapterImpl(detailsString);
					details.initializeFromJSONObject(adapter);
				} catch (JSONObjectAdapterException e) {
					throw new IllegalArgumentException(e);
				}
			}
			result.put(claim, details);
		}
		return result;
	}
}
