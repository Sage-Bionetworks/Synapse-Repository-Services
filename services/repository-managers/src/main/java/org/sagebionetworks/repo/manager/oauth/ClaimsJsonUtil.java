package org.sagebionetworks.repo.manager.oauth;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

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
		Map<String,Object> scopeAndClaims = new HashMap<String,Object>();
		scopeAndClaims.put(SCOPE, scopes);
		scopeAndClaims.put(USER_INFO_CLAIMS, oidcClaims);		
		claims.put(ACCESS, scopeAndClaims);
	}

	/* 
	 * Given the claims from a JWT-encoded access token, extract the list of granted scopes.
	 */
	public static List<OAuthScope> getScopeFromClaims(Claims claims) {
		Map<String,Object> scopeAndClaims = (Map<String,Object>)claims.get(ACCESS, Map.class);
		return (List<OAuthScope>)scopeAndClaims.get(SCOPE);
	}
	
	/* 
	 * Given the claims from a JWT-encoded access token, extract the list of granted claims.
	 */
	public static Map<OIDCClaimName, OIDCClaimsRequestDetails> getOIDCClaimsFromClaimSet(Claims claims) {
		Map<String,Object> scopeAndClaims = (Map<String,Object>)claims.get(ACCESS, Map.class);
		return (Map<OIDCClaimName, OIDCClaimsRequestDetails>)scopeAndClaims.get(USER_INFO_CLAIMS);
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
				continue; // ignore unknown claims
			}
			String detailsString = claimsJson.getString(claimName);
			OIDCClaimsRequestDetails details = null;
			if (detailsString!=null && !NULL.equals(detailsString)) {
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
