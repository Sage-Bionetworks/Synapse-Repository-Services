package org.sagebionetworks.repo.manager.oauth;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
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
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
import org.sagebionetworks.repo.model.oauth.OIDCTokenResponse;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.securitytools.EncryptionUtils;
import org.sagebionetworks.util.ValidateArgument;

import com.nimbusds.jwt.JWT;

public class OpenIDConnectManagerImpl implements OpenIDConnectManager {
	private static final long AUTHORIZATION_CODE_TIME_LIMIT_MILLIS = 60000L; // one minutes
	
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
	
	private static Map<OIDCClaimName,String> CLAIM_DESCRIPTION;
	static {
		CLAIM_DESCRIPTION = new HashMap<OIDCClaimName,String>();
		CLAIM_DESCRIPTION.put(OIDCClaimName.team, "Your team membership");
		CLAIM_DESCRIPTION.put(OIDCClaimName.family_name, "Your last name, if you share it with Synapse"); // https://openid.net/specs/openid-connect-core-1_0.html#StandardClaims
		CLAIM_DESCRIPTION.put(OIDCClaimName.given_name, "Your first name, if you share it with Synapse"); // https://openid.net/specs/openid-connect-core-1_0.html#StandardClaims
		CLAIM_DESCRIPTION.put(OIDCClaimName.email, "Your email address (<username>@synapse.org)");
		CLAIM_DESCRIPTION.put(OIDCClaimName.email_verified, "Your email address (<username>@synapse.org)");
		CLAIM_DESCRIPTION.put(OIDCClaimName.company, "Your company, if you share it with Synapse");
		CLAIM_DESCRIPTION.put(OIDCClaimName.auth_time, "The time when you last logged in to Synapse.");
	}
	
	private static void validateAuthenticationRequest(OIDCAuthorizationRequest authorizationRequest, OAuthClient client) {
		ValidateArgument.validUrl(authorizationRequest.getRedirectUri(), "Redirect URI");
		if (!client.getClient_uri().contains(authorizationRequest.getRedirectUri())) { // TODO is this the right way to match URLs?
			throw new IllegalArgumentException("Redirect URI "+authorizationRequest.getRedirectUri()+
					" is not registered for "+client.getClient_name());
		}		
		if (authorizationRequest.getResponseType()!=OAuthResponseType.code) 
			throw new IllegalArgumentException("Unsupported response type "+authorizationRequest.getResponseType());
	}

	@Override
	public OIDCAuthorizationRequestDescription getAuthenticationRequestDescription(
			OIDCAuthorizationRequest authorizationRequest) {
		
		ValidateArgument.required(authorizationRequest, "Authorization Request");
		ValidateArgument.requiredNotEmpty(authorizationRequest.getClientId(), "Client ID");
		OAuthClient client = null; // TODO get client from database
		validateAuthenticationRequest(authorizationRequest, client);
		OIDCAuthorizationRequestDescription result = new OIDCAuthorizationRequestDescription();
		result.setClient_name(client.getClient_name());
		result.setClient_uri(client.getClient_uri());
		result.setPolicy_uri(client.getPolicy_uri());
		result.setTos_uri(client.getTos_uri());

		List<OAuthScope> scopes = parseScopeString(authorizationRequest.getScope());
		Set<String> scopeDescriptions = new TreeSet<String>();
		for (OAuthScope scope : scopes) {
			String scopeDescription = null;
			switch (scope) {
			case openid:
				// required for OIDC requests.  Doesn't add anything to what access is requested
				continue;
			case userid:
				scopeDescription = "Your Synapse user id";
				break;
			default:
				// unrecognized scope values should be ignored https://openid.net/specs/openid-connect-core-1_0.html#AuthRequest
				continue;
			}
			scopeDescriptions.add(scopeDescription);
		}
		
		if (authorizationRequest.getClaims()!=null) {
			Map<OIDCClaimName,OIDCClaimsRequestDetails> idTokenClaims = 
					authorizationRequest.getClaims().getId_token();
			if (idTokenClaims!=null) {
				for (OIDCClaimName claim : idTokenClaims.keySet()) {
					scopeDescriptions.add(CLAIM_DESCRIPTION.get(claim));
				}
			}
			Map<OIDCClaimName,OIDCClaimsRequestDetails> userInfoClaims = 
					authorizationRequest.getClaims().getUserinfo();
			if (userInfoClaims!=null) {
				for (OIDCClaimName claim : userInfoClaims.keySet()) {
					scopeDescriptions.add(CLAIM_DESCRIPTION.get(claim));
				}
			}
		}
		result.setScope(new ArrayList<String>(scopeDescriptions));
		return result;
	}

	@Override
	public OAuthAuthorizationResponse authorizeClient(UserInfo userInfo,
			OIDCAuthorizationRequest authorizationRequest) {

		// 	when evaluating the claims object, how do we differentiate between a null value and a missing key?
		// 	They mean different things https://openid.net/specs/openid-connect-core-1_0.html#ClaimsParameter
		OAuthClient client = null; // TODO get client from database
		validateAuthenticationRequest(authorizationRequest, client);
		 
		authorizationRequest.setUserId((new Long(userInfo.getId()).toString()));
		authorizationRequest.setAuthorizedAt((new Date(System.currentTimeMillis())));
		// TODO determine when the user was logged in and record it: authorizationRequest.setAuthenticatedAt(authenticatedAt);
		
//		if (!scopes.contains(OAuthScope.openid)) {
//			// TODO don't return id token
//		}
		
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		try {
			authorizationRequest.writeToJSONObject(adapter);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
		String serializedAuthorizationRequest = adapter.toJSONString();
		String oauthClientEncryptionKey=null;  // TODO
		String encryptedAuthorizationRequest = EncryptionUtils.encrypt(serializedAuthorizationRequest, oauthClientEncryptionKey);
				
		OAuthAuthorizationResponse result = new OAuthAuthorizationResponse();
		result.setAccess_code(encryptedAuthorizationRequest);
		return result;
	}

	@Override
	public OIDCTokenResponse getAccessToken(String code, String clientId, String redirectUri) {
		String serializedAuthorizationRequest;
		String oauthClientEncryptionKey=null;  // TODO
		try {
			serializedAuthorizationRequest = EncryptionUtils.decrypt(code, oauthClientEncryptionKey);
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
		// The following implements 'pairwise' subject_type, https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderConfigurationResponse
		// Pairwise Pseudonymous Identifier (PPID)
		// TODO https://openid.net/specs/openid-connect-registration-1_0.html#SectorIdentifierValidation
		String ppid = EncryptionUtils.encrypt(authorizationRequest.getUserId(), oauthClientEncryptionKey);
		String oauthClientId = authorizationRequest.getClientId();
		
		OIDCTokenResponse result = new OIDCTokenResponse();
		if (scopes.contains(OAuthScope.openid)) {
			String idToken = OIDCTokenUtil.createOIDCidToken(ppid, oauthClientId, now, 
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
