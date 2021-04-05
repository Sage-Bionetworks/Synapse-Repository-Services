package org.sagebionetworks.repo.manager.oauth;

import static org.sagebionetworks.repo.manager.oauth.OpenIDConnectManager.getScopeHash;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.StackEncrypter;
import org.sagebionetworks.manager.util.OAuthPermissionUtils;
import org.sagebionetworks.repo.manager.authentication.PersonalAccessTokenManager;
import org.sagebionetworks.repo.manager.oauth.claimprovider.OIDCClaimProvider;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.model.auth.OAuthClientDao;
import org.sagebionetworks.repo.model.auth.OAuthDao;
import org.sagebionetworks.repo.model.auth.TokenType;
import org.sagebionetworks.repo.model.oauth.OAuthAuthorizationResponse;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.oauth.OAuthRefreshTokenInformation;
import org.sagebionetworks.repo.model.oauth.OAuthResponseType;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OAuthTokenRevocationRequest;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequest;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequestDescription;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequest;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
import org.sagebionetworks.repo.model.oauth.OIDCTokenResponse;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.ForbiddenException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.OAuthBadRequestException;
import org.sagebionetworks.repo.web.OAuthErrorCode;
import org.sagebionetworks.repo.web.OAuthUnauthenticatedException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.securitytools.EncryptionUtils;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.EnumKeyedJsonMapUtil;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwt;

public class OpenIDConnectManagerImpl implements OpenIDConnectManager {
	private static final long AUTHORIZATION_CODE_TIME_LIMIT_MILLIS = 60000L; // one minute

	// user authorization times out after one year
	private static final long AUTHORIZATION_TIME_OUT_MILLIS = 1000L*3600L*24L*365L;
	
	private static final long ACCESS_TOKEN_EXPIRATION_TIME_SECONDS = 3600*24L; // a day
	
	// token_type=Bearer, as per https://openid.net/specs/openid-connect-core-1_0.html#TokenResponse
	private static final String TOKEN_TYPE_BEARER = "Bearer";

	@Autowired
	private StackEncrypter stackEncrypter;

	@Autowired
	private OAuthClientDao oauthClientDao;

	@Autowired
	private OAuthRefreshTokenManager oauthRefreshTokenManager;

	@Autowired
	private PersonalAccessTokenManager personalAccessTokenManager;

	@Autowired
	private AuthenticationDAO authDao;
	
	@Autowired
	private OAuthDao oauthDao;

	@Autowired
	private OIDCTokenHelper oidcTokenHelper;

	@Autowired
	private Clock clock;
	
	/**
	 * Injected.
	 */
	private Map<OIDCClaimName, OIDCClaimProvider> claimProviders;
	
	public void setClaimProviders(Map<OIDCClaimName, OIDCClaimProvider> claimProviders) {
		this.claimProviders = claimProviders;
	}

	/*
	 * The scope parameter in an OAuth authorization request is a space-delimited list of scope values.
	 */
	public static List<OAuthScope> parseScopeString(String s) {
		if (StringUtils.isEmpty(s)) {
			return Collections.emptyList();
		}
		String decoded;
		try {
			decoded = URLDecoder.decode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		List<OAuthScope> result = new ArrayList<OAuthScope>();
		for (String token : decoded.split("\\s")) {
			OAuthScope scope;
			try {
				scope = OAuthScope.valueOf(token);
			} catch (IllegalArgumentException e) {
				throw new OAuthBadRequestException(OAuthErrorCode.invalid_scope, "Unrecognized scope: "+token, e);
			}
			result.add(scope);
		}
		return result;
	}

	public static void validateAuthenticationRequest(OIDCAuthorizationRequest authorizationRequest, OAuthClient client) {
		try {
			ValidateArgument.validUrl(authorizationRequest.getRedirectUri(), "Redirect URI");
		} catch (IllegalArgumentException e) {
			throw new OAuthBadRequestException(OAuthErrorCode.invalid_request, e);
		}
		if (!client.getRedirect_uris().contains(authorizationRequest.getRedirectUri())) {
			throw new OAuthBadRequestException(OAuthErrorCode.invalid_grant, "Redirect URI "+authorizationRequest.getRedirectUri()+
					" is not registered for "+client.getClient_name());
		}		
		if (authorizationRequest.getResponseType()==null) {
			throw new OAuthBadRequestException(OAuthErrorCode.invalid_request, "Missing response_type.");
		}
		if (OAuthResponseType.code!=authorizationRequest.getResponseType()) {
			throw new OAuthBadRequestException(OAuthErrorCode.unsupported_response_type, "Unsupported response type "+authorizationRequest.getResponseType());
		}
	}

	@Override
	public OIDCAuthorizationRequestDescription getAuthenticationRequestDescription(
			OIDCAuthorizationRequest authorizationRequest) {

		ValidateArgument.required(authorizationRequest, "Authorization Request");
		ValidateArgument.requiredNotEmpty(authorizationRequest.getClientId(), "Client ID");
		OAuthClient client;
		try {
			client = oauthClientDao.getOAuthClient(authorizationRequest.getClientId());
		} catch (NotFoundException e) {
			throw new OAuthBadRequestException(OAuthErrorCode.invalid_client, "Invalid OAuth Client ID: "+authorizationRequest.getClientId());
		}
		
		validateClientVerificationStatus(client.getClient_id());
		
		validateAuthenticationRequest(authorizationRequest, client);
		
		OIDCAuthorizationRequestDescription result = new OIDCAuthorizationRequestDescription();
		result.setClientId(client.getClient_id());
		result.setRedirect_uri(authorizationRequest.getRedirectUri());

		List<OAuthScope> scopes = parseScopeString(authorizationRequest.getScope());
		Set<String> scopeDescriptions = new TreeSet<String>();
		for (OAuthScope scope : scopes) {
			String scopeDescription = null;
			if (scope==OAuthScope.openid) {
				// required for OIDC requests.  Doesn't add anything to what access is requested, so leave description empty
			} else {
				scopeDescription = OAuthPermissionUtils.scopeDescription(scope);
			}
			if (StringUtils.isNotEmpty(scopeDescription)) {
				scopeDescriptions.add(scopeDescription);
			}
		}

		// Use of [the OpenID Connect] extension [to OAuth 2.0] is requested by Clients by including the openid scope value in the Authorization Request.
		// https://openid.net/specs/openid-connect-core-1_0.html#Introduction
		if (scopes.contains(OAuthScope.openid) && authorizationRequest.getClaims() != null) {
			if (authorizationRequest.getClaims().getUserinfo() != null && !authorizationRequest.getClaims().getUserinfo().isEmpty()) {
				Map<OIDCClaimName,OIDCClaimsRequestDetails> userinfoClaims = EnumKeyedJsonMapUtil.convertKeysToEnums(authorizationRequest.getClaims().getUserinfo(), OIDCClaimName.class);
				scopeDescriptions.addAll(getDescriptionsForClaims(userinfoClaims));
			}
			if (authorizationRequest.getClaims().getId_token() != null && !authorizationRequest.getClaims().getId_token().isEmpty()) {
				Map<OIDCClaimName,OIDCClaimsRequestDetails> idTokenClaims = EnumKeyedJsonMapUtil.convertKeysToEnums(authorizationRequest.getClaims().getId_token(), OIDCClaimName.class);
				scopeDescriptions.addAll(getDescriptionsForClaims(idTokenClaims));
			}
		}
		result.setScope(new ArrayList<String>(scopeDescriptions));
		return result;
	}
	
	private Set<String> getDescriptionsForClaims(Map<OIDCClaimName,OIDCClaimsRequestDetails> idTokenClaimsMap) {
		Set<String> scopeDescriptions = new TreeSet<String>();
		{
			for (OIDCClaimName claim : idTokenClaimsMap.keySet()) {
				OIDCClaimProvider provider = claimProviders.get(claim);
				if (provider!=null) {
					scopeDescriptions.add(provider.getDescription());
				}
			}
		}
		return scopeDescriptions;
	}
	
	@Override
	public boolean hasUserGrantedConsent(UserInfo userInfo, OIDCAuthorizationRequest authorizationRequest) {
		Date notBefore = new Date(clock.currentTimeMillis()-AUTHORIZATION_TIME_OUT_MILLIS);
		return oauthDao.lookupAuthorizationConsent(userInfo.getId(), 
				Long.valueOf(authorizationRequest.getClientId()), 
				getScopeHash(authorizationRequest),
				notBefore);
	}

	@Override
	@WriteTransaction
	public OAuthAuthorizationResponse authorizeClient(UserInfo userInfo,
			OIDCAuthorizationRequest authorizationRequest) {
		if (AuthorizationUtils.isUserAnonymous(userInfo)) {
			throw new OAuthUnauthenticatedException(OAuthErrorCode.login_required, "Anonymous users may not provide access to OAuth clients.");
		}

		OAuthClient client;
		try {
			client = oauthClientDao.getOAuthClient(authorizationRequest.getClientId());
		} catch (NotFoundException e) {
			throw new OAuthBadRequestException(OAuthErrorCode.invalid_client, "Invalid OAuth Client ID: "+authorizationRequest.getClientId());
		}
		
		validateClientVerificationStatus(client.getClient_id());

		validateAuthenticationRequest(authorizationRequest, client);

		authorizationRequest.setUserId((new Long(userInfo.getId()).toString()));
		authorizationRequest.setAuthorizedAt(clock.now());
		authorizationRequest.setAuthenticatedAt(authDao.getAuthenticatedOn(userInfo.getId()));

		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		try {
			authorizationRequest.writeToJSONObject(adapter);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
		String serializedAuthorizationRequest = adapter.toJSONString();
		String encryptedAuthorizationRequest = stackEncrypter.encryptAndBase64EncodeStringWithStackKey(serializedAuthorizationRequest);

		OAuthAuthorizationResponse result = new OAuthAuthorizationResponse();
		result.setAccess_code(encryptedAuthorizationRequest);
		oauthDao.saveAuthorizationConsent(userInfo.getId(), 
				Long.valueOf(authorizationRequest.getClientId()), 
				getScopeHash(authorizationRequest), new Date());
		return result;
	}

	// As per, https://openid.net/specs/openid-connect-core-1_0.html#PairwiseAlg
	public String ppid(String userId, String clientId) {
		// we introduce the 'Synapse OAuth client ID' for internal use only,
		// when we construct a token corresponding to a (total access) session token.
		// when treating a subject in the context of this client, we skip encryption/decryption
		if (AuthorizationConstants.SYNAPSE_OAUTH_CLIENT_ID.equals(clientId)) {
			return userId;
		}
		String sectorIdentifierSecret = oauthClientDao.getSectorIdentifierSecretForClient(clientId);
		return EncryptionUtils.encrypt(userId, sectorIdentifierSecret);
	}

	public String getUserIdFromPPID(String ppid, String clientId) {
		// we introduce the 'Synapse OAuth client ID' for internal use only,
		// when we construct a token corresponding to a (total access) session token.
		// when treating a subject in the context of this client, we skip encryption/decryption
		validateClientVerificationStatus(clientId);
		if (AuthorizationConstants.SYNAPSE_OAUTH_CLIENT_ID.equals(clientId)) {
			return ppid;
		}
		String sectorIdentifierSecret = oauthClientDao.getSectorIdentifierSecretForClient(clientId);
		return EncryptionUtils.decrypt(ppid, sectorIdentifierSecret);
	}
	
	@Override
	public String validateAccessToken(String jwtToken) {
		// Parsing the JWT handles tokens that have expired
		Claims claims = oidcTokenHelper.parseJWT(jwtToken).getBody();

		String userId = getUserIdFromPPID(claims.getSubject(), claims.getAudience());
		TokenType tokenType = TokenType.valueOf(claims.get(OIDCClaimName.token_type.name(), String.class));
		switch (tokenType) {
			case OIDC_ACCESS_TOKEN:
				// If the access token has an associated refresh token, we check to see if the refresh token has been revoked.
				String refreshTokenId = claims.get(OIDCClaimName.refresh_token_id.name(), String.class);
				if (refreshTokenId != null && !oauthRefreshTokenManager.isRefreshTokenActive(refreshTokenId)) {
					throw new OAuthUnauthenticatedException(OAuthErrorCode.invalid_token, "The access token has been revoked.");
				}
				break;
			case PERSONAL_ACCESS_TOKEN:
				String personalAccessTokenId = claims.getId();
				if (personalAccessTokenManager.isTokenActive(personalAccessTokenId)) {
					personalAccessTokenManager.updateLastUsedTime(personalAccessTokenId);
				} else {
					throw new ForbiddenException("The provided personal access token has expired or has been revoked.");
				}
				break;
			case OIDC_ID_TOKEN:
				throw new OAuthUnauthenticatedException(OAuthErrorCode.invalid_token, "The provided token is an OIDC ID token and cannot be used to authenticate requests.");

		}
		return userId;
	}

	/*
	 * Given the scopes and additional OIDC claims requested by the user, return the 
	 * user info claims to add to the returned User Info object or JSON Web Token
	 */
	public Map<OIDCClaimName,Object> getUserInfo(final String userId, List<OAuthScope> scopes, Map<OIDCClaimName, OIDCClaimsRequestDetails>  oidcClaims) {
		Map<OIDCClaimName,Object> result = new HashMap<OIDCClaimName,Object>();
		// Use of [the OpenID Connect] extension [to OAuth 2.0] is requested by Clients by including the openid scope value in the Authorization Request.
		// https://openid.net/specs/openid-connect-core-1_0.html#Introduction
		if (!scopes.contains(OAuthScope.openid)) return result;

		for (OIDCClaimName claimName : oidcClaims.keySet()) {
			Object claimValue = null;
			OIDCClaimProvider claimProvider = claimProviders.get(claimName);
			if (claimProvider!=null) {
				claimValue = claimProvider.getClaim(userId, oidcClaims.get(claimName));
			}
			// from https://openid.net/specs/openid-connect-core-1_0.html#UserInfoResponse
			// "If a Claim is not returned, that Claim Name SHOULD be omitted from the JSON object 
			// representing the Claims; it SHOULD NOT be present with a null or empty string value."
			if (claimValue!=null) {
				result.put(claimName, claimValue);
			}
		}
		return result;
	}

	@Override
	public OIDCTokenResponse generateTokenResponseWithAuthorizationCode(String code, String verifiedClientId, String redirectUri, String oauthEndpoint) {
		ValidateArgument.required(code, "Authorization Code");
		ValidateArgument.required(verifiedClientId, "OAuth Client ID");
		ValidateArgument.required(redirectUri, "Redirect URI");
		ValidateArgument.required(oauthEndpoint, "Authorization Endpoint");
		
		validateClientVerificationStatus(verifiedClientId);
		
		String serializedAuthorizationRequest;
		try {
			serializedAuthorizationRequest = stackEncrypter.decryptStackEncryptedAndBase64EncodedString(code);
		} catch (Exception e) {
			throw new OAuthBadRequestException(OAuthErrorCode.invalid_grant, "Invalid authorization code: "+code, e);
		}
		OIDCAuthorizationRequest authorizationRequest = new OIDCAuthorizationRequest();
		try {
			JSONObjectAdapter adapter = new JSONObjectAdapterImpl(serializedAuthorizationRequest);
			authorizationRequest.initializeFromJSONObject(adapter);
		} catch (JSONObjectAdapterException e) {
			// This should never happen. If it does, the authz code was likely improperly encoded/decoded, which isn't the user's fault.
			throw new IllegalStateException("Incorrectly formatted authorization code: "+code, e);
		}

		// enforce expiration of authorization code
		long now = clock.currentTimeMillis();
		if (now > authorizationRequest.getAuthorizedAt().getTime()+AUTHORIZATION_CODE_TIME_LIMIT_MILLIS) {
			throw new OAuthBadRequestException(OAuthErrorCode.invalid_grant, "Authorization code has expired.");
		}

		// ensure redirect URI matches
		if (!authorizationRequest.getRedirectUri().equals(redirectUri)) {
			throw new OAuthBadRequestException(OAuthErrorCode.invalid_grant, "URI mismatch: "+authorizationRequest.getRedirectUri()+" vs. "+redirectUri);
		}

		List<OAuthScope> scopes = parseScopeString(authorizationRequest.getScope());

		OIDCClaimsRequest normalizedClaims = normalizeClaims(authorizationRequest.getClaims());

		Date authTime = authorizationRequest.getAuthenticatedAt();

		// The following implements 'pairwise' subject_type, https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderConfigurationResponse
		// Pairwise Pseudonymous Identifier (PPID)
		String ppid = ppid(authorizationRequest.getUserId(), verifiedClientId);
		String oauthClientId = authorizationRequest.getClientId();

		OIDCTokenResponse result = new OIDCTokenResponse();

		// Use of [the OpenID Connect] extension [to OAuth 2.0] is requested by Clients by including the openid scope value in the Authorization Request.
		// https://openid.net/specs/openid-connect-core-1_0.html#Introduction
		if (scopes.contains(OAuthScope.openid)) {
			String idTokenId = UUID.randomUUID().toString();
			Map<OIDCClaimName,Object> userInfo = getUserInfo(authorizationRequest.getUserId(), 
					scopes, EnumKeyedJsonMapUtil.convertKeysToEnums(normalizedClaims.getId_token(), OIDCClaimName.class));
			String idToken = oidcTokenHelper.createOIDCIdToken(oauthEndpoint, ppid, oauthClientId, now, 
					authorizationRequest.getNonce(), authTime, idTokenId, userInfo);
			result.setId_token(idToken);
		}

		// A refresh token should only be issued when `offline_access` is requested.
		boolean issueRefreshToken = scopes.contains(OAuthScope.offline_access);
		String refreshTokenId = null;
		if (issueRefreshToken) {

			OAuthRefreshTokenAndMetadata refreshToken = oauthRefreshTokenManager
					.createRefreshToken(authorizationRequest.getUserId(),
							oauthClientId,
							scopes,
							normalizedClaims
					);
			refreshTokenId = refreshToken.getMetadata().getTokenId();
			result.setRefresh_token(refreshToken.getRefreshToken());
		}

		String accessTokenId = UUID.randomUUID().toString();
		String accessToken = oidcTokenHelper.createOIDCaccessToken(oauthEndpoint, ppid,
				oauthClientId, now, ACCESS_TOKEN_EXPIRATION_TIME_SECONDS, authTime, refreshTokenId, accessTokenId, scopes,
				EnumKeyedJsonMapUtil.convertKeysToEnums(normalizedClaims.getUserinfo(), OIDCClaimName.class));
		result.setAccess_token(accessToken);
		result.setToken_type(TOKEN_TYPE_BEARER);
		result.setExpires_in(ACCESS_TOKEN_EXPIRATION_TIME_SECONDS);
		return result;
	}

	@WriteTransaction
	@Override
	public OIDCTokenResponse generateTokenResponseWithRefreshToken(String refreshToken, String verifiedClientId, String scope, String oauthEndpoint) {
		ValidateArgument.required(refreshToken, "Refresh Token");
		ValidateArgument.required(verifiedClientId, "OAuth Client ID");
		ValidateArgument.required(oauthEndpoint, "Authorization Endpoint");
		// scopes is not required

		validateClientVerificationStatus(verifiedClientId);
		List<OAuthScope> scopes = parseScopeString(scope);

		// Retrieve the refresh token and rotate it.
		OAuthRefreshTokenAndMetadata rotatedRefreshToken = oauthRefreshTokenManager.rotateRefreshToken(refreshToken);
		OAuthRefreshTokenInformation refreshTokenMetadata = rotatedRefreshToken.getMetadata();

		// Ensure the client is permitted to use this refresh token
		if (!refreshTokenMetadata.getClientId().equals(verifiedClientId)) {
			// Defined by https://tools.ietf.org/html/rfc6749#section-5.2
			throw new IllegalArgumentException("invalid_grant");
		}

		if (scopes.isEmpty()) {
			// Per RFC-6479 Section 6, if [the requested scope is] omitted[, it] is treated as equal to the scope originally granted by the resource owner. https://tools.ietf.org/html/rfc6749#section-6
			scopes = refreshTokenMetadata.getScopes();
		} else if (!refreshTokenMetadata.getScopes().containsAll(scopes)) { // Ensure the requested scopes are a subset of previously authorized scopes and claims
			// Defined by https://tools.ietf.org/html/rfc6749#section-5.2
			throw new IllegalArgumentException("invalid_scope");
		}

		// In the JWT, we will need to supply both the current time and the date/time of the initial authorization
		long now = clock.currentTimeMillis();
		Date authTime = authDao.getAuthenticatedOn(Long.parseLong(refreshTokenMetadata.getPrincipalId()));

		// The following implements 'pairwise' subject_type, https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderConfigurationResponse
		// Pairwise Pseudonymous Identifier (PPID)
		String ppid = ppid(refreshTokenMetadata.getPrincipalId(), verifiedClientId);
		String oauthClientId = refreshTokenMetadata.getClientId();

		OIDCTokenResponse result = new OIDCTokenResponse();
		result.setRefresh_token(rotatedRefreshToken.getRefreshToken());

		// Use of [the OpenID Connect] extension [to OAuth 2.0] is requested by Clients by including the openid scope value in the Authorization Request.
		// https://openid.net/specs/openid-connect-core-1_0.html#Introduction
		Map<OIDCClaimName, OIDCClaimsRequestDetails> idTokenClaims = EnumKeyedJsonMapUtil.convertKeysToEnums(refreshTokenMetadata.getClaims().getId_token(), OIDCClaimName.class);
		Map<OIDCClaimName, OIDCClaimsRequestDetails> userInfoClaims = EnumKeyedJsonMapUtil.convertKeysToEnums(refreshTokenMetadata.getClaims().getUserinfo(), OIDCClaimName.class);
		if (scopes.contains(OAuthScope.openid)) {
			String idTokenId = UUID.randomUUID().toString();
			Map<OIDCClaimName,Object> userInfo = getUserInfo(refreshTokenMetadata.getPrincipalId(), scopes, idTokenClaims);
			String idToken = oidcTokenHelper.createOIDCIdToken(oauthEndpoint, ppid, oauthClientId, now, null, authTime, idTokenId, userInfo);
			result.setId_token(idToken);
		} else {
			idTokenClaims = Collections.emptyMap();
			userInfoClaims = Collections.emptyMap();
		}

		String accessTokenId = UUID.randomUUID().toString();
		String accessToken = oidcTokenHelper.createOIDCaccessToken(oauthEndpoint, ppid,
				oauthClientId, now,ACCESS_TOKEN_EXPIRATION_TIME_SECONDS,  authTime, refreshTokenMetadata.getTokenId(), accessTokenId, scopes, userInfoClaims);
		result.setAccess_token(accessToken);
		result.setToken_type(TOKEN_TYPE_BEARER);
		result.setExpires_in(ACCESS_TOKEN_EXPIRATION_TIME_SECONDS);
		return result;
	}

	/**
	 * Removes null fields and unrecognized claims from the OIDCClaimsRequest. Also replaces
	 * null objects with empty ones (a requirement for the {@link OAuthRefreshTokenManager}, if the
	 * claims are saved)
	 *
	 * Protected access for testing
	 * @param claims
	 * @return
	 */
	protected static OIDCClaimsRequest normalizeClaims(OIDCClaimsRequest claims) {
		if (claims == null) {
			claims = new OIDCClaimsRequest();
		}
		if (claims.getId_token() == null) {
			claims.setId_token(Collections.emptyMap());
		} else {
			// Converting the key to enum and back to string will drop unrecognized claims
			claims.setId_token(
					EnumKeyedJsonMapUtil.convertKeysToStrings(
							EnumKeyedJsonMapUtil.convertKeysToEnums(claims.getId_token(), OIDCClaimName.class)
					)
			);
		}
		if (claims.getUserinfo() == null) {
			claims.setUserinfo(Collections.emptyMap());
		} else {
			claims.setUserinfo(
					EnumKeyedJsonMapUtil.convertKeysToStrings(
							EnumKeyedJsonMapUtil.convertKeysToEnums(claims.getUserinfo(), OIDCClaimName.class)
					)
			);
		}
		return claims;
	}

	@Override
	public Object getUserInfo(String accessTokenParam, String oauthEndpoint) {
		ValidateArgument.required(accessTokenParam, "Access token");
		Jwt<JwsHeader,Claims> accessToken = oidcTokenHelper.parseJWT(accessTokenParam);
		Claims accessTokenClaims = accessToken.getBody();
		String oauthClientId = accessTokenClaims.getAudience();

		List<OAuthScope> scopes = ClaimsJsonUtil.getScopeFromClaims(accessTokenClaims);
		Map<OIDCClaimName, OIDCClaimsRequestDetails> oidcClaims = ClaimsJsonUtil.getOIDCClaimsFromClaimSet(accessTokenClaims);

		String ppid = accessTokenClaims.getSubject();

		// userId is used to retrieve the user info
		String userId = getUserIdFromPPID(ppid, oauthClientId);

		Map<OIDCClaimName,Object> userInfo = getUserInfo(userId, scopes, oidcClaims);

		// From https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata
		// "If [a signing algorithm] is specified, the response will be JWT serialized, and signed using JWS. 
		// The default, if omitted, is for the UserInfo Response to return the Claims as a UTF-8 
		// encoded JSON object using the application/json content-type."
		// 
		// Note: This leaves ambiguous what to do if the client is registered with a signing algorithm
		// and then sends a request with Accept: application/json or vice versa (registers with no 
		// algorithm and then sends a request with Accept: application/jwt).
		boolean returnJson;
		if (oauthClientId.equals(AuthorizationConstants.SYNAPSE_OAUTH_CLIENT_ID)) {
			returnJson = true;
		} else {
			OAuthClient oauthClient = oauthClientDao.getOAuthClient(oauthClientId);
			returnJson = oauthClient.getUserinfo_signed_response_alg()==null;
		}		

		if (returnJson) {
			// https://openid.net/specs/openid-connect-core-1_0.html#UserInfoResponse
			userInfo.put(OIDCClaimName.sub, ppid);
			return userInfo;
		} else {
			Date authTime = authDao.getAuthenticatedOn(Long.parseLong(userId));

			String jwtIdToken = oidcTokenHelper.createOIDCIdToken(oauthEndpoint, ppid, oauthClientId, clock.currentTimeMillis(), null,
					authTime, UUID.randomUUID().toString(), userInfo);

			return new JWTWrapper(jwtIdToken);
		}
	}
	
	/**
	 * Validates that the verified flag is true for the client with the given id
	 * 
	 * @throws OAuthClientNotVerifiedException If the client is not verified
	 * @throws NotFoundException               If a client with the given id does not exist
	 */
	protected void validateClientVerificationStatus(String clientId) throws NotFoundException, OAuthClientNotVerifiedException {
		ValidateArgument.required(clientId, "OAuth Client ID");
		if (clientId.equals(AuthorizationConstants.SYNAPSE_OAUTH_CLIENT_ID)) {
			// Since the reserved Synapse Oauth Client is not present in the database, we simply return
			return;
		}
		if (!oauthClientDao.isOauthClientVerified(clientId)) {			
			throw new OAuthClientNotVerifiedException("The OAuth client (" + clientId + ") is not verified.");
		}
	}

	@WriteTransaction
	@Override
	public void revokeToken(String verifiedClientId, OAuthTokenRevocationRequest revocationRequest) {
		switch (revocationRequest.getToken_type_hint()) {
			case access_token: // retrieve the refresh token ID from the JWT
				String refreshTokenId = this.getRefreshTokenId(revocationRequest.getToken());
				if (refreshTokenId == null) {
					// Access tokens that were not issued alongside refresh tokens cannot be revoked.
					throw new IllegalArgumentException("The access token has no associated refresh token so it cannot be revoked.");
				}
				oauthRefreshTokenManager.revokeRefreshToken(verifiedClientId, refreshTokenId);
				return;
			case refresh_token: // retrieve the token ID using the token
				OAuthRefreshTokenInformation metadata = oauthRefreshTokenManager.getRefreshTokenMetadataWithToken(verifiedClientId, revocationRequest.getToken());
				oauthRefreshTokenManager.revokeRefreshToken(verifiedClientId, metadata.getTokenId());
				return;
			default:
				throw new OAuthBadRequestException(OAuthErrorCode.unsupported_token_type, "Unable to revoke a token with token_type_hint=" + revocationRequest.getToken_type_hint().name());
		}
	}


	/**
	 * Given an OAuth access token, return the associated refresh token ID.
	 *
	 * When an OAuth authorization request with the `{@link org.sagebionetworks.repo.model.oauth.OAuthScope#offline_access}`
	 * scope is granted, an access token is issued alongside a refresh token that has a unique, persistent ID. This access token,
	 * along with any future access tokens issued using that refresh token will contain that refresh token ID in the {@link org.sagebionetworks.repo.model.oauth.OIDCClaimName#refresh_token_id} scope
	 * @param accessToken a JWT access token
	 * @return the refresh token ID, or null if this token was not issued with a refresh token.
	 */
	private String getRefreshTokenId(String accessToken) {
		Claims claims = oidcTokenHelper.parseJWT(accessToken).getBody();
		return claims.get(OIDCClaimName.refresh_token_id.name(), String.class);
	}
}
