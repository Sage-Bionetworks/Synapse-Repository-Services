package org.sagebionetworks.repo.manager.oauth;


import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.sagebionetworks.StackEncrypter;
import org.sagebionetworks.repo.manager.UserInfoHelper;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.manager.VerificationHelper;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.ListWrapper;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.model.auth.OAuthClientDao;
import org.sagebionetworks.repo.model.oauth.OAuthAuthorizationResponse;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.oauth.OAuthResponseType;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequest;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequestDescription;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
import org.sagebionetworks.repo.model.oauth.OIDCTokenResponse;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.securitytools.EncryptionUtils;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwt;

public class OpenIDConnectManagerImpl implements OpenIDConnectManager {
	private static final long AUTHORIZATION_CODE_TIME_LIMIT_MILLIS = 60000L; // one minute

	// from https://openid.net/specs/openid-connect-core-1_0.html#ClaimsParameter
	private static final String ID_TOKEN_CLAIMS_KEY = "id_token";
	private static final String USER_INFO_CLAIMS_KEY = "userinfo";
	
	@Autowired
	private StackEncrypter stackEncrypter;

	@Autowired
	private OAuthClientDao oauthClientDao;

	@Autowired
	private AuthenticationDAO authDao;

	@Autowired
	private UserManager userManager;

	@Autowired
	private UserProfileManager userProfileManager;

	@Autowired
	private TeamDAO teamDAO;
	
	@Autowired
	private OIDCTokenHelper oidcTokenHelper;

	/*
	 * The scope parameter in an OAuth authorization request is a space-delimited list of scope values.
	 */
	public static List<OAuthScope> parseScopeString(String s) {
		if (StringUtils.isEmpty(s)) {
			return Collections.EMPTY_LIST;
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
				throw new IllegalArgumentException("Unrecognized scope: "+token, e);
			}
			result.add(scope);
		}
		return result;
	}

	public static Map<OIDCClaimName,String> CLAIM_DESCRIPTION;
	static {
		CLAIM_DESCRIPTION = new HashMap<OIDCClaimName,String>();
		CLAIM_DESCRIPTION.put(OIDCClaimName.team, "Your team membership");
		CLAIM_DESCRIPTION.put(OIDCClaimName.family_name, "Your last name, if you share it with Synapse"); // https://openid.net/specs/openid-connect-core-1_0.html#StandardClaims
		CLAIM_DESCRIPTION.put(OIDCClaimName.given_name, "Your first name, if you share it with Synapse"); // https://openid.net/specs/openid-connect-core-1_0.html#StandardClaims
		CLAIM_DESCRIPTION.put(OIDCClaimName.email, "Your email address (<username>@synapse.org)");
		CLAIM_DESCRIPTION.put(OIDCClaimName.email_verified, "Your email address (<username>@synapse.org)");
		CLAIM_DESCRIPTION.put(OIDCClaimName.company, "Your company, if you share it with Synapse");
		CLAIM_DESCRIPTION.put(OIDCClaimName.auth_time, "The time when you last logged in to Synapse");
		CLAIM_DESCRIPTION.put(OIDCClaimName.is_certified, "Whether you are a certified Synapse user");
		CLAIM_DESCRIPTION.put(OIDCClaimName.is_validated, "Whether you are a validated Synapse user");
		CLAIM_DESCRIPTION.put(OIDCClaimName.orcid, "The ORCID you have linked to your Synapse account, if any");
		CLAIM_DESCRIPTION.put(OIDCClaimName.userid, "Your Synapse user ID, which can be used to access your public profile");
		CLAIM_DESCRIPTION.put(OIDCClaimName.validated_at, "If you are a validated user, the date when your profile was validated");
		CLAIM_DESCRIPTION.put(OIDCClaimName.validated_company, "If you are a validated user, your validated company or organization");
		CLAIM_DESCRIPTION.put(OIDCClaimName.validated_email, "If you are a validated user, your validated email");
		CLAIM_DESCRIPTION.put(OIDCClaimName.validated_family_name, "If you are a validated user, your validated last name");
		CLAIM_DESCRIPTION.put(OIDCClaimName.validated_given_name, "If you are a validated user, your validated first name");
		CLAIM_DESCRIPTION.put(OIDCClaimName.validated_location, "If you are a validated user, your validated location");
		CLAIM_DESCRIPTION.put(OIDCClaimName.validated_orcid, "If you are a validated user, your validated ORCID");
	}

	public static void validateAuthenticationRequest(OIDCAuthorizationRequest authorizationRequest, OAuthClient client) {
		ValidateArgument.validUrl(authorizationRequest.getRedirectUri(), "Redirect URI");
		if (!client.getRedirect_uris().contains(authorizationRequest.getRedirectUri())) {
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
		OAuthClient client;
		try {
			client = oauthClientDao.getOAuthClient(authorizationRequest.getClientId());
		} catch (NotFoundException e) {
			throw new IllegalArgumentException("Invalid OAuth Client ID: "+authorizationRequest.getClientId());
		}
		validateAuthenticationRequest(authorizationRequest, client);
		OIDCAuthorizationRequestDescription result = new OIDCAuthorizationRequestDescription();
		result.setClientId(client.getClientId());
		result.setRedirect_uri(authorizationRequest.getRedirectUri());

		List<OAuthScope> scopes = parseScopeString(authorizationRequest.getScope());
		Set<String> scopeDescriptions = new TreeSet<String>();
		for (OAuthScope scope : scopes) {
			String scopeDescription = null;
			switch (scope) {
			case openid:
				// required for OIDC requests.  Doesn't add anything to what access is requested
				break;
			default:
				// unrecognized scope values should be ignored https://openid.net/specs/openid-connect-core-1_0.html#AuthRequest
				break;
			}
			if (StringUtils.isNotEmpty(scopeDescription)) {
				scopeDescriptions.add(scopeDescription);
			}
		}

		// Use of [the OpenID Connect] extension [to OAuth 2.0] is requested by Clients by including the openid scope value in the Authorization Request.
		// https://openid.net/specs/openid-connect-core-1_0.html#Introduction
		if (scopes.contains(OAuthScope.openid) && !StringUtils.isEmpty(authorizationRequest.getClaims())) {
			{
				Map<OIDCClaimName,OIDCClaimsRequestDetails> idTokenClaimsMap = 
						ClaimsJsonUtil.getClaimsMapFromClaimsRequestParam(authorizationRequest.getClaims(), ID_TOKEN_CLAIMS_KEY);
				for (OIDCClaimName claim : idTokenClaimsMap.keySet()) {
					scopeDescriptions.add(CLAIM_DESCRIPTION.get(claim));
				}
			}
			{
				Map<OIDCClaimName,OIDCClaimsRequestDetails> userInfoClaimsMap = 
						ClaimsJsonUtil.getClaimsMapFromClaimsRequestParam(authorizationRequest.getClaims(), USER_INFO_CLAIMS_KEY);
				for (OIDCClaimName claim : userInfoClaimsMap.keySet()) {
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
		if (AuthorizationUtils.isUserAnonymous(userInfo)) {
			throw new UnauthorizedException("Anonymous users may not provide access to OAuth clients.");
		}

		OAuthClient client;
		try {
			client = oauthClientDao.getOAuthClient(authorizationRequest.getClientId());
		} catch (NotFoundException e) {
			throw new IllegalArgumentException("Invalid OAuth Client ID: "+authorizationRequest.getClientId());
		}
		validateAuthenticationRequest(authorizationRequest, client);

		authorizationRequest.setUserId((new Long(userInfo.getId()).toString()));
		authorizationRequest.setAuthorizedAt((new Date(System.currentTimeMillis())));
		authorizationRequest.setAuthenticatedAt(authDao.getSessionValidatedOn(userInfo.getId()));

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
		return result;
	}

	// As per, https://openid.net/specs/openid-connect-core-1_0.html#PairwiseAlg
	public String ppid(String userId, String clientId) {
		String sectorIdentifierSecret = oauthClientDao.getSectorIdentifierSecretForClient(clientId);
		return EncryptionUtils.encrypt(userId, sectorIdentifierSecret);
	}

	public String getUserIdFromPPID(String ppid, String clientId) {
		String sectorIdentifierSecret = oauthClientDao.getSectorIdentifierSecretForClient(clientId);
		return EncryptionUtils.decrypt(ppid, sectorIdentifierSecret);
	}
	
	/*
	 * Given the scopes and additional OIDC claims requested by the user, return the 
	 * user info claims to add to the returned User Info object or JSON Web Token
	 */
	public Map<OIDCClaimName,String> getUserInfo(final String userId, List<OAuthScope> scopes, Map<OIDCClaimName, OIDCClaimsRequestDetails>  oidcClaims) {
		Map<OIDCClaimName,String> result = new HashMap<OIDCClaimName,String>();
		// Use of [the OpenID Connect] extension [to OAuth 2.0] is requested by Clients by including the openid scope value in the Authorization Request.
		// https://openid.net/specs/openid-connect-core-1_0.html#Introduction
		if (!scopes.contains(OAuthScope.openid)) return result;

		CachingGetter<UserInfo> userInfoGetter = new CachingGetter<UserInfo>() {
			protected UserInfo getIntern() {return userManager.getUserInfo(Long.parseLong(userId));}
		};

		CachingGetter<UserProfile> userProfileGetter = new CachingGetter<UserProfile>() {
			protected UserProfile getIntern() {return userProfileManager.getUserProfile(userId);}
		};

		CachingGetter<VerificationSubmission> verificationSubmissionGetter = new CachingGetter<VerificationSubmission>() {
			protected VerificationSubmission getIntern() {
				return userProfileManager.getCurrentVerificationSubmission(Long.parseLong(userId));
			}
		};

		for (OIDCClaimName claimName : oidcClaims.keySet()) {
			OIDCClaimsRequestDetails claimsDetails = oidcClaims.get(claimName);
			String claimValue = null;
			switch (claimName) {
			case email:
			case email_verified:
				List<String> emails = userProfileGetter.get().getEmails();
				if (emails!=null && !emails.isEmpty()) {
					claimValue = emails.get(0);
				}
				break;
			case given_name:
				claimValue = userProfileGetter.get().getFirstName();
				break;
			case family_name:
				claimValue = userProfileGetter.get().getLastName();
				break;
			case company:
				claimValue = userProfileGetter.get().getCompany();
				break;
			case team:
				if (claimsDetails==null) {
					continue;
				}
				Set<String> requestedTeamIds = new HashSet<String>();
				if (StringUtils.isNotEmpty(claimsDetails.getValue())) {
					requestedTeamIds.add(claimsDetails.getValue());
				}
				if (claimsDetails.getValues()!=null && !claimsDetails.getValues().isEmpty()) {
					requestedTeamIds.addAll(requestedTeamIds);
				}
				Set<String> memberTeamIds = getMemberTeamIds(userId, requestedTeamIds);
				claimValue = asSerializedJSON(memberTeamIds);
				break;
			case userid:
				claimValue = userId;
				break;
			case is_certified:
				claimValue = ""+UserInfoHelper.isCertified(userInfoGetter.get());
				break;
			case is_validated:
				claimValue = ""+VerificationHelper.isVerified(verificationSubmissionGetter.get());
				break;
			case orcid:
				claimValue = userProfileManager.getOrcid(Long.parseLong(userId));
				break;
			case validated_at:
				Date approvalDate = VerificationHelper.getApprovalDate(verificationSubmissionGetter.get());
				if (approvalDate!=null) {
					Long validatedEpochSeconds = approvalDate.getTime()/1000L;
					claimValue = validatedEpochSeconds.toString();
				}
			case validated_company:
				if (verificationSubmissionGetter.get()!=null) {
					claimValue = verificationSubmissionGetter.get().getCompany();
				}
				break;
			case validated_email:
				if (verificationSubmissionGetter.get()!=null) {
					claimValue = verificationSubmissionGetter.get().getEmails().toString();
				}
				break;
			case validated_family_name:
				if (verificationSubmissionGetter.get()!=null) {
					claimValue = verificationSubmissionGetter.get().getLastName();
				}
				break;
			case validated_given_name:
				if (verificationSubmissionGetter.get()!=null) {
					claimValue = verificationSubmissionGetter.get().getFirstName();
				}
				break;
			case validated_location:
				if (verificationSubmissionGetter.get()!=null) {
					claimValue = verificationSubmissionGetter.get().getLocation();
				}
				break;
			case validated_orcid:
				if (verificationSubmissionGetter.get()!=null) {
					claimValue = verificationSubmissionGetter.get().getOrcid();
				}
				break;
			default:
				continue;

			}
			// from https://openid.net/specs/openid-connect-core-1_0.html#UserInfoResponse
			// "If a Claim is not returned, that Claim Name SHOULD be omitted from the JSON object 
			// representing the Claims; it SHOULD NOT be present with a null or empty string value."
			if (StringUtils.isNotEmpty(claimValue)) {
				result.put(claimName, claimValue);
			}
		}
		return result;
	}

	/*
	 * return the subset of team Ids in which the given user is a member
	 */
	private Set<String> getMemberTeamIds(String userId, Set<String> requestedTeamIds) {
		List<Long> numericTeamIds = new ArrayList<Long>();
		for (String stringTeamId : requestedTeamIds) {
			try {
				numericTeamIds.add(Long.parseLong(stringTeamId));
			} catch (NumberFormatException e) {
				// this will be translated into a 400 level status, sent back to the client
				throw new IllegalArgumentException(stringTeamId+" is not a valid Team ID");
			}
		}
		ListWrapper<TeamMember> teamMembers = teamDAO.listMembers(numericTeamIds, Collections.singletonList(Long.parseLong(userId)));
		Set<String> result = new HashSet<String>();
		for (TeamMember teamMember : teamMembers.getList()) {
			result.add(teamMember.getTeamId());
		}
		return result;
	}

	public static String asSerializedJSON(Collection<?> c) {
		JSONArray array = new JSONArray();
		for (Object s : c) {
			array.put(s.toString());
		}
		return array.toString();
	}

	@Override
	public OIDCTokenResponse getAccessToken(String code, String verifiedClientId, String redirectUri, String oauthEndpoint) {
		String serializedAuthorizationRequest;
		try {
			serializedAuthorizationRequest = stackEncrypter.decryptStackEncryptedAndBase64EncodedString(code);
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid authorization code: "+code, e);
		}
		OIDCAuthorizationRequest authorizationRequest = new OIDCAuthorizationRequest();
		try {
			JSONObjectAdapter adapter = new JSONObjectAdapterImpl(serializedAuthorizationRequest);
			authorizationRequest.initializeFromJSONObject(adapter);
		} catch (JSONObjectAdapterException e) {
			throw new IllegalArgumentException("Incorrectly formatted authorization code: "+code, e);
		}

		// enforce expiration of authorization code
		long now = System.currentTimeMillis();
		if (System.currentTimeMillis() > authorizationRequest.getAuthorizedAt().getTime()+AUTHORIZATION_CODE_TIME_LIMIT_MILLIS) {
			throw new IllegalArgumentException("Authorization code has expired.");
		}

		// ensure redirect URI matches
		if (!authorizationRequest.getRedirectUri().equals(redirectUri)) {
			throw new IllegalArgumentException("URI mismatch: "+authorizationRequest.getRedirectUri()+" vs. "+redirectUri);
		}

		List<OAuthScope> scopes = parseScopeString(authorizationRequest.getScope());

		Long authTimeSeconds = null;
		if (authorizationRequest.getAuthenticatedAt()!=null) {
			authTimeSeconds = authorizationRequest.getAuthenticatedAt().getTime()/1000L;
		}

		// The following implements 'pairwise' subject_type, https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderConfigurationResponse
		// Pairwise Pseudonymous Identifier (PPID)
		String ppid = ppid(authorizationRequest.getUserId(), verifiedClientId);
		String oauthClientId = authorizationRequest.getClientId();

		OIDCTokenResponse result = new OIDCTokenResponse();

		// Use of [the OpenID Connect] extension [to OAuth 2.0] is requested by Clients by including the openid scope value in the Authorization Request.
		// https://openid.net/specs/openid-connect-core-1_0.html#Introduction
		if (scopes.contains(OAuthScope.openid)) {
			String idTokenId = UUID.randomUUID().toString();
			Map<OIDCClaimName,String> userInfo = getUserInfo(authorizationRequest.getUserId(), 
					scopes, ClaimsJsonUtil.getClaimsMapFromClaimsRequestParam(authorizationRequest.getClaims(), ID_TOKEN_CLAIMS_KEY));
			String idToken = oidcTokenHelper.createOIDCIdToken(oauthEndpoint, ppid, oauthClientId, now, 
					authorizationRequest.getNonce(), authTimeSeconds, idTokenId, userInfo);
			result.setId_token(idToken);
		}

		String accessTokenId = UUID.randomUUID().toString();
		String accessToken = oidcTokenHelper.createOIDCaccessToken(oauthEndpoint, ppid, 
				oauthClientId, now, authTimeSeconds, accessTokenId, scopes, 
				ClaimsJsonUtil.getClaimsMapFromClaimsRequestParam(authorizationRequest.getClaims(), USER_INFO_CLAIMS_KEY));
		result.setAccess_token(accessToken);
		return result;
	}
	
	@Override
	public Object getUserInfo(Jwt<JwsHeader,Claims> accessToken, String oauthEndpoint) {

		Claims accessTokenClaims = accessToken.getBody();
		// We set exactly one Audience when creating the token
		String oauthClientId = accessTokenClaims.getAudience();
		if (oauthClientId==null) {
			throw new IllegalArgumentException("Missing 'audience' value in the OAuth Access Token.");
		}
		OAuthClient oauthClient = oauthClientDao.getOAuthClient(oauthClientId);

		String ppid = accessTokenClaims.getSubject();
		Long authTimeSeconds = accessTokenClaims.get(OIDCClaimName.auth_time.name(), Long.class);

		// userId is used to retrieve the user info
		String userId = getUserIdFromPPID(ppid, oauthClientId);

		List<OAuthScope> scopes = ClaimsJsonUtil.getScopeFromClaims(accessTokenClaims);
		Map<OIDCClaimName, OIDCClaimsRequestDetails>  oidcClaims = ClaimsJsonUtil.getOIDCClaimsFromClaimSet(accessTokenClaims);

		Map<OIDCClaimName,String> userInfo = getUserInfo(userId, scopes, oidcClaims);

		// From https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata
		// "If this is specified, the response will be JWT serialized, and signed using JWS. 
		// The default, if omitted, is for the UserInfo Response to return the Claims as a UTF-8 
		// encoded JSON object using the application/json content-type."
		// 
		// Note: This leaves ambiguous what to do if the client is registered with a signing algorithm
		// and then sends a request with Accept: application/json or vice versa (registers with no 
		// algorithm and then sends a request with Accept: application/jwt).
		boolean returnJson = oauthClient.getUserinfo_signed_response_alg()==null;

		if (returnJson) {
			// https://openid.net/specs/openid-connect-core-1_0.html#UserInfoResponse
			userInfo.put(OIDCClaimName.sub, ppid);
			return userInfo;
		} else {
			long now = System.currentTimeMillis();
			String jwtIdToken = oidcTokenHelper.createOIDCIdToken(oauthEndpoint, ppid, oauthClientId, now, null,
					authTimeSeconds, UUID.randomUUID().toString(), userInfo);

			return jwtIdToken;
		}
	}
}
