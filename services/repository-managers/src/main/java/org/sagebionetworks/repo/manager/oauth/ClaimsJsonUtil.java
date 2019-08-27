package org.sagebionetworks.repo.manager.oauth;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

import com.nimbusds.jwt.JWTClaimsSet;

import io.jsonwebtoken.Claims;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;

public class ClaimsJsonUtil {
	private static final String ACCESS = "access";
	private static final String SCOPE = "scope";
	private static final String USER_INFO_CLAIMS = "oidc_claims";
	private static final JSONParser MINIDEV_JSON_PARSER = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
	
	/*
	 * Given lists of scopes and claims, build an 'access' claim JSONObject and add it to the 
	 * 'claims' object of a JWT-encoded access token
	 */
	public static void addAccessClaims(List<OAuthScope> scopes, Map<OIDCClaimName, OIDCClaimsRequestDetails> oidcClaims, Claims claims) {
		JSONObject scopeAndClaims = new JSONObject();
		JSONArray scopeArray = new JSONArray();
		for (OAuthScope scope : scopes) {
			scopeArray.add(scope.name());
		}
		scopeAndClaims.put(SCOPE, scopeArray);
		
		
		JSONObject userInfoClaims = new JSONObject();
		for (OIDCClaimName claimName : oidcClaims.keySet()) {
			OIDCClaimsRequestDetails claimDetails = oidcClaims.get(claimName);
			JSONObject claimsDetailsJson = null;
			if (claimDetails!=null) {
				try {
					JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
					claimDetails.writeToJSONObject(adapter);
					// JSONObjectAdapter is a JSON Object but we need an instance of net.minidev.json.JSONObject
					// so we serialize and immediately parse
					claimsDetailsJson = (JSONObject)MINIDEV_JSON_PARSER.parse(adapter.toJSONString());
				} catch (net.minidev.json.parser.ParseException | JSONObjectAdapterException e) {
					throw new RuntimeException(e);
				}				
			}
			userInfoClaims.put(claimName.name(), claimsDetailsJson);	
		}
		scopeAndClaims.put(USER_INFO_CLAIMS, userInfoClaims);
		
		claims.put(ACCESS, scopeAndClaims);
	}

	/* 
	 * Given the claims from a JWT-encoded access token, extract the list of granted scopes.
	 */
	public static List<OAuthScope> getScopeFromClaims(JWTClaimsSet claimSet) {
		JSONObject scopeAndClaims;
		try {
			scopeAndClaims = claimSet.getJSONObjectClaim(ACCESS);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
		JSONArray scopeArray = (JSONArray)scopeAndClaims.get(SCOPE);
		List<OAuthScope> result = new ArrayList<OAuthScope>();
		for (String scope : scopeArray.toArray(new String[] {})) {
			result.add(OAuthScope.valueOf(scope));
		}
		return result;
	}
	
	/* 
	 * Given the claims from a JWT-encoded access token, extract the list of granted claims.
	 */
	public static Map<OIDCClaimName, OIDCClaimsRequestDetails> getOIDCClaimsFromClaimSet(JWTClaimsSet claimSet) {
		JSONObject scopeAndClaims;
		try {
			scopeAndClaims = claimSet.getJSONObjectClaim(ACCESS);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
		JSONObject userInfoClaims = (JSONObject)scopeAndClaims.get(USER_INFO_CLAIMS);
		return ClaimsJsonUtil.getClaimsMapFromJSONObject(userInfoClaims, false);
	}
	
	/**
	 * Extract the claims and their details from a claims JSON, e.g. from a claims parameters in an OIDC authorization request.
	 * 
	 * @param claimsJson the claims, e.g. from an OIDC authorization request
	 * @param ignoreUnknownClaims if true, simply ignore unrecognized claim names, otherwise throw an exception
	 * @return
	 */
	public static Map<OIDCClaimName, OIDCClaimsRequestDetails> getClaimsMapFromJSONObject(JSONObject claimsJson, boolean ignoreUnknownClaims) {
		Map<OIDCClaimName, OIDCClaimsRequestDetails> result = new HashMap<OIDCClaimName, OIDCClaimsRequestDetails>();
		if (claimsJson==null) return result;
		for (String claimName : claimsJson.keySet()) {
			OIDCClaimName claim;
			try {
				claim = OIDCClaimName.valueOf(claimName);
			} catch (IllegalArgumentException e) {	
				if (ignoreUnknownClaims) {
					continue;
				} else {
					throw e;
				}
			}
			String detailsString = claimsJson.getAsString(claimName);
			OIDCClaimsRequestDetails details = null;
			if (detailsString!=null) {
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
