package org.sagebionetworks.repo.manager.oauth;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.UUID;

import org.json.JSONObject;
import org.sagebionetworks.repo.manager.OIDCTokenUtil;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.oauth.OAuthAuthorizationResponse;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.oauth.OAuthClientList;
import org.sagebionetworks.repo.model.oauth.OAuthResponseType;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequest;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequestDescription;
import org.sagebionetworks.repo.model.oauth.OIDCTokenResponse;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.securitytools.EncryptionUtils;

import com.google.inject.Inject;
import com.nimbusds.jwt.JWT;

public class OpenIDConnectManagerImpl implements OpenIDConnectManager {
	private static final long AUTHORIZATION_CODE_TIME_LIMIT_MILLIS = 60000L; // one minutes
	
	private EncryptionUtils encryptionUtils;
	
	@Inject
	public OpenIDConnectManagerImpl(EncryptionUtils encryptionUtils) {
		this.encryptionUtils=encryptionUtils;
	}

	@Override
	public OAuthClient createOpenIDConnectClient(UserInfo userInfo, OAuthClient oauthClient) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OAuthClient getOpenIDConnectClient(UserInfo userInfo, String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OAuthClientList listOpenIDConnectClients(UserInfo userInfo, String nextPageToken) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OAuthClient updateOpenIDConnectClient(UserInfo userInfo, OAuthClient oauthClient) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object deleteOpenIDConnectClient(UserInfo userInfo, String id) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/*
	 * The scope parameter in an OAuth authorization request is a space-delimted list of scope values.
	 */
	private static List<OAuthScope> parseScopeString(String s) {
		StringTokenizer st = new StringTokenizer(s);
		List<OAuthScope> result = new ArrayList<OAuthScope>();
		while (st.hasMoreTokens()) {
			result.add(OAuthScope.valueOf(st.nextToken())); // TODO verify this throws Illegal argument exception if scope is not from enum
		}
		return result;
	}

	@Override
	public OIDCAuthorizationRequestDescription getAuthenticationRequestDescription(
			OIDCAuthorizationRequest authorizationRequest) {
		List<OAuthScope> scopes = parseScopeString(authorizationRequest.getScope());
		return null;
	}

	@Override
	public OAuthAuthorizationResponse authorizeClient(UserInfo userInfo,
			OIDCAuthorizationRequest authorizationRequest) {
		// TODO validate:
		// clientId is valid
		// scope is valid
		// claims are valid
		// 	when evaluating the claims object, how do we differentiate between a null value and a missing key?
		// 	They mean different things https://openid.net/specs/openid-connect-core-1_0.html#ClaimsParameter
		// response type is 'code'
		if (authorizationRequest.getResponseType()!=OAuthResponseType.code) 
			throw new IllegalArgumentException("Unsupported response type "+authorizationRequest.getResponseType());
		// redirectUri was preregistered
		 
		authorizationRequest.setUserId((new Long(userInfo.getId()).toString()));
		authorizationRequest.setAuthorizedAt((new Date(System.currentTimeMillis())));
		// TODO determine when the user was logged in and record it: authorizationRequest.setAuthenticatedAt(authenticatedAt);
		
		
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		try {
			authorizationRequest.writeToJSONObject(adapter);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
		String serializedAuthorizationRequest = adapter.toJSONString();
		String encryptedAuthorizationRequest = encryptionUtils.encryptStringWithStackKey(serializedAuthorizationRequest);
		OAuthAuthorizationResponse result = new OAuthAuthorizationResponse();
		result.setAccess_code(encryptedAuthorizationRequest);
		return result;
	}

	@Override
	public OIDCTokenResponse getAccessToken(String code, String redirectUri) {
		String serializedAuthorizationRequest;
		try {
			serializedAuthorizationRequest = encryptionUtils.decryptStackEncryptedString(code);
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid authorization code", e);
		}
		OIDCAuthorizationRequest authorizationRequest = new OIDCAuthorizationRequest();
		try {
			JSONObjectAdapter adapter = new JSONObjectAdapterImpl(serializedAuthorizationRequest);
			authorizationRequest.initializeFromJSONObject(adapter);
		} catch (JSONObjectAdapterException e) {
			throw new IllegalArgumentException("Invalid authorization code", e);
		}
		
		// enforce expiration of authorization code
		long now = System.currentTimeMillis();
		if (authorizationRequest.getAuthorizedAt().getTime()+AUTHORIZATION_CODE_TIME_LIMIT_MILLIS>System.currentTimeMillis()) {
			throw new IllegalArgumentException("Authorization code is too old.");
		}
		
		// ensure redirect URI matches
		if (!authorizationRequest.getRedirectUri().equals(redirectUri)) {
			throw new IllegalArgumentException("URI mismatch: "+authorizationRequest.getRedirectUri()+" vs. "+redirectUri);
		}
		
		List<OAuthScope> scopes = parseScopeString(authorizationRequest.getScope());
		
		
		Date authTime = authorizationRequest.getAuthenticatedAt();
		String tokenId = UUID.randomUUID().toString();
		JSONObject userClaims = new JSONObject(); // TODO authorizationRequest.getClaims();
		if (scopes.contains(OAuthScope.userid)) {
			userClaims.put(OAuthScope.userid.name(), authorizationRequest.getUserId());
		}
		String user = authorizationRequest.getUserId(); // TODO, obfuscate the user id 
		String oauthClientId = authorizationRequest.getClientId();
		
		
		OIDCTokenResponse result = new OIDCTokenResponse();
		if (scopes.contains(OAuthScope.openid)) {
			String idToken = OIDCTokenUtil.createOIDCidToken(user, oauthClientId, now, 
				authorizationRequest.getNonce(), authTime, tokenId, userClaims);
			result.setId_token(idToken);
		}
		
		String accessToken = OIDCTokenUtil.createOIDCaccessToken(); // TODO
		result.setAccess_token(accessToken);
		result.setRefresh_token(UUID.randomUUID().toString()); // TODO implement refresh
		return result;
	}

	@Override
	public Object getUserInfo(JWT accessToken) {
		// TODO Auto-generated method stub
		return null;
	}

}
