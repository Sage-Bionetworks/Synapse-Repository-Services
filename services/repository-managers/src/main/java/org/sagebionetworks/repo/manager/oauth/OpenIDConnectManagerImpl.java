package org.sagebionetworks.repo.manager.oauth;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.StackEncrypter;
import org.sagebionetworks.repo.manager.PrivateFieldUtils;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.ListWrapper;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.model.auth.OAuthClientDao;
import org.sagebionetworks.repo.model.auth.SectorIdentifier;
import org.sagebionetworks.repo.model.oauth.OAuthAuthorizationResponse;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.oauth.OAuthClientIdAndSecret;
import org.sagebionetworks.repo.model.oauth.OAuthClientList;
import org.sagebionetworks.repo.model.oauth.OAuthResponseType;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequest;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequestDescription;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
import org.sagebionetworks.repo.model.oauth.OIDCTokenResponse;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.securitytools.EncryptionUtils;
import org.sagebionetworks.securitytools.PBKDF2Utils;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwt;
import io.netty.util.internal.StringUtil;

public class OpenIDConnectManagerImpl implements OpenIDConnectManager {
	private static final long AUTHORIZATION_CODE_TIME_LIMIT_MILLIS = 60000L; // one minute

	// from https://openid.net/specs/openid-connect-core-1_0.html#ClaimsParameter
	private static final String ID_TOKEN_CLAIMS_KEY = "id_token";
	private static final String USER_INFO_CLAIMS_KEY = "userinfo";
	
	private static final Random RANDOM = new SecureRandom();

	
	@Autowired
	private StackEncrypter stackEncrypter;

	@Autowired
	private OAuthClientDao oauthClientDao;

	@Autowired
	private AuthenticationDAO authDao;

	@Autowired
	private UserProfileManager userProfileManager;

	@Autowired
	private TeamDAO teamDAO;
	
	@Autowired
	private SimpleHttpClient httpClient;

	
	@Autowired
	OIDCTokenHelper oidcTokenHelper;

	public static void validateOAuthClientForCreateOrUpdate(OAuthClient oauthClient) {
		ValidateArgument.required(oauthClient.getClient_name(), "OAuth client name");
		ValidateArgument.required(oauthClient.getRedirect_uris(), "OAuth client redirect URI list.");
		ValidateArgument.requirement(!oauthClient.getRedirect_uris().isEmpty(), "OAuth client must register at least one redirect URI.");
		for (String uri: oauthClient.getRedirect_uris()) {
			ValidateArgument.validUrl(uri, "Valid redirect URI");
		}
		if (StringUtils.isNotEmpty(oauthClient.getSector_identifier_uri())) {
			ValidateArgument.validUrl(oauthClient.getSector_identifier_uri(), "Sector Identifier URI");
		}
	}

	public List<String> readSectorIdentifierFile(URI uri) throws ServiceUnavailableException {
		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setUri(uri.toString());
		SimpleHttpResponse response = null;
		try {
			response = httpClient.get(request);
		} catch (IOException e) {
			throw new ServiceUnavailableException("Failed to read the content of "+uri+
					".  Please check the URL and the file at the address, then try again.", e);
		}
		if (response.getStatusCode() != HttpStatus.SC_OK) {
			throw new ServiceUnavailableException("Received "+response.getStatusCode()+" status while trying to read the content of "+uri+
					".  Please check the URL and the file at the address, then try again.");
		}
		List<String> result = new ArrayList<String>();
		JSONArray array;
		try {
			array =  new JSONArray(response.getContent());
			for (int i=0; i<array.length(); i++) {
				result.add(array.getString(i));
			}
		} catch (JSONException e) {
			throw new IllegalArgumentException("The content of "+uri+" is not a valid JSON array of strings.", e);
		}
		return result;
	}

	// implements https://openid.net/specs/openid-connect-core-1_0.html#PairwiseAlg
	public String resolveSectorIdentifier(String sectorIdentifierUriString, List<String> redirectUris) throws ServiceUnavailableException {
		if (StringUtils.isEmpty(sectorIdentifierUriString)) {
			ValidateArgument.required(redirectUris, "Redirect URI list");
			// the sector ID is the host common to all uris in the list
			String result=null;
			for (String uriString: redirectUris) {
				URI uri=null;
				try {
					uri = new URI(uriString);
				} catch (URISyntaxException e) {
					ValidateArgument.failRequirement(uriString+" is not a valid URI.");
				}
				if (result==null) {
					result=uri.getHost();
				} else {
					ValidateArgument.requirement(result.equals(uri.getHost()), 
							"if redirect URIs do not share a common host then you must register a sector identifier URI.");
				}
			}
			ValidateArgument.requirement(result!=null, "Missing redirect URI.");
			return result;
		} else {
			// scheme must be https
			URI uri=null;
			try {
				uri = new URI(sectorIdentifierUriString);
			} catch (URISyntaxException e) {
				ValidateArgument.failRequirement(sectorIdentifierUriString+" is not a valid URI.");
			}
			ValidateArgument.requirement(uri.getScheme().equalsIgnoreCase("https"), 
					sectorIdentifierUriString+" must use the https scheme.");
			// read file, parse json, and make sure it contains all of redirectUris values
			List<String> siList = readSectorIdentifierFile(uri);
			ValidateArgument.requirement(siList.containsAll(redirectUris), 
					"Not all of the submitted redirect URIs are found in the list hosted at "+uri);
			// As per https://openid.net/specs/openid-connect-registration-1_0.html#SectorIdentifierValidation,
			// the sector ID is the host of the sectorIdentifierUri
			return uri.getHost();
		}
	}

	private void ensureSectorIdentifierExists(String sectorIdentiferURI, Long createdBy) {
		if (oauthClientDao.doesSectorIdentifierExistForURI(sectorIdentiferURI)) {
			return;
		}
		SectorIdentifier sectorIdentifier = new SectorIdentifier();
		sectorIdentifier.setCreatedBy(createdBy);
		sectorIdentifier.setCreatedOn(System.currentTimeMillis());
		String sectorIdentifierSecret = EncryptionUtils.newSecretKey();
		sectorIdentifier.setSecret(sectorIdentifierSecret);
		sectorIdentifier.setSectorIdentifierUri(sectorIdentiferURI);
		oauthClientDao.createSectorIdentifier(sectorIdentifier);
	}

	public static boolean canCreate(UserInfo userInfo) {
		return !AuthorizationUtils.isUserAnonymous(userInfo);
	}

	public static boolean canAdministrate(UserInfo userInfo, String createdBy) {
		return createdBy.equals(userInfo.getId().toString()) || userInfo.isAdmin();
	}

	@WriteTransaction
	@Override
	public OAuthClient createOpenIDConnectClient(UserInfo userInfo, OAuthClient oauthClient) throws ServiceUnavailableException {
		if (!canCreate(userInfo)) {
			throw new UnauthorizedException("Anonymous user may not create an OAuth Client");
		}
		validateOAuthClientForCreateOrUpdate(oauthClient);

		oauthClient.setCreatedBy(userInfo.getId().toString());
		oauthClient.setEtag(UUID.randomUUID().toString());
		oauthClient.setVerified(false);

		String resolvedSectorIdentifier = resolveSectorIdentifier(oauthClient.getSector_identifier_uri(), oauthClient.getRedirect_uris());
		oauthClient.setSector_identifier(resolvedSectorIdentifier);
		// find or create SectorIdentifier
		ensureSectorIdentifierExists(resolvedSectorIdentifier, userInfo.getId());

		return oauthClientDao.createOAuthClient(oauthClient);
	}
	
	@Override
	public OAuthClient getOpenIDConnectClient(UserInfo userInfo, String id) {
		OAuthClient result = oauthClientDao.getOAuthClient(id);
		if (!canAdministrate(userInfo, result.getCreatedBy())) {
			PrivateFieldUtils.clearPrivateFields(result);
		}
		return result;
	}

	@Override
	public OAuthClientList listOpenIDConnectClients(UserInfo userInfo, String nextPageToken) {
		return oauthClientDao.listOAuthClients(nextPageToken, userInfo.getId());
	}

	@WriteTransaction
	@Override
	public OAuthClient updateOpenIDConnectClient(UserInfo userInfo, OAuthClient toUpdate) throws ServiceUnavailableException {
		ValidateArgument.requiredNotEmpty(toUpdate.getClientId(), "Client ID");
		OAuthClient currentClient = oauthClientDao.selectOAuthClientForUpdate(toUpdate.getClientId());
		if (!canAdministrate(userInfo, currentClient.getCreatedBy())) {
			throw new UnauthorizedException("You can only update your own OAuth client(s).");
		}
		validateOAuthClientForCreateOrUpdate(toUpdate);
		
		ValidateArgument.requiredNotEmpty(toUpdate.getEtag(), "etag");
		if (!currentClient.getEtag().equals(toUpdate.getEtag())) {
			throw new ConflictingUpdateException(
					"OAuth Client was updated since you last fetched it.  Retrieve it again and reapply the update.");
		}
		
		String resolvedSectorIdentifier = resolveSectorIdentifier(toUpdate.getSector_identifier_uri(), toUpdate.getRedirect_uris());
		if (!resolvedSectorIdentifier.equals(currentClient.getSector_identifier())) {
			ensureSectorIdentifierExists(resolvedSectorIdentifier, userInfo.getId());
		}
		
		OAuthClient toStore = new OAuthClient();

		// now fill in 'toStore' with info from updatedClient
		// we *never* change: clientID, createdBy, createdOn
		// (1) immutable:
		toStore.setClientId(currentClient.getClientId());
		toStore.setCreatedBy(currentClient.getCreatedBy());
		toStore.setCreatedOn(currentClient.getCreatedOn());
		// (2) settable by client
		toStore.setClient_name(toUpdate.getClient_name());
		toStore.setClient_uri(toUpdate.getClient_uri());
		toStore.setPolicy_uri(toUpdate.getPolicy_uri());
		toStore.setTos_uri(toUpdate.getTos_uri());
		toStore.setUserinfo_signed_response_alg(toUpdate.getUserinfo_signed_response_alg());
		toStore.setRedirect_uris(toUpdate.getRedirect_uris());
		toStore.setSector_identifier_uri(toUpdate.getSector_identifier_uri());
		// set by system
		toStore.setModifiedOn(new Date());
		toStore.setEtag(UUID.randomUUID().toString());
		toStore.setVerified(currentClient.getVerified());
		toStore.setSector_identifier(resolvedSectorIdentifier);
		if (!resolvedSectorIdentifier.equals(currentClient.getSector_identifier())) {
			toStore.setVerified(false);
		}
		return oauthClientDao.updateOAuthClient(toStore);
	}

	@WriteTransaction
	@Override
	public void deleteOpenIDConnectClient(UserInfo userInfo, String id) {
		String creator = oauthClientDao.getOAuthClientCreator(id);
		if (!canAdministrate(userInfo, creator)) {
			throw new UnauthorizedException("You can only delete your own OAuth client(s).");
		}
		oauthClientDao.deleteOAuthClient(id);
	}

	public static String generateOAuthClientSecret() {
		byte[] randomBytes = new byte[32];
		RANDOM.nextBytes(randomBytes);
		return Base64.getUrlEncoder().encodeToString(randomBytes);
	}

	@WriteTransaction
	@Override
	public OAuthClientIdAndSecret createClientSecret(UserInfo userInfo, String clientId) {
		String creator = oauthClientDao.getOAuthClientCreator(clientId);
		if (!canAdministrate(userInfo, creator)) {
			throw new UnauthorizedException("You can only generate credentials for your own OAuth client(s).");
		}		
		String secret = generateOAuthClientSecret();
		String secretHash = PBKDF2Utils.hashPassword(secret, null);
		oauthClientDao.setOAuthClientSecretHash(clientId, secretHash, UUID.randomUUID().toString());
		OAuthClientIdAndSecret result = new OAuthClientIdAndSecret();
		result.setClientId(clientId);
		result.setClientSecret(secret);
		return result;
	}
	/*
	 * The scope parameter in an OAuth authorization request is a space-delimited list of scope values.
	 */
	public static List<OAuthScope> parseScopeString(String s) {
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

	public static void validateAuthenticationRequest(OIDCAuthorizationRequest authorizationRequest, OAuthClient client) {
		ValidateArgument.validUrl(authorizationRequest.getRedirectUri(), "Redirect URI");
		if (!client.getRedirect_uris().contains(authorizationRequest.getRedirectUri())) {
			throw new IllegalArgumentException("Redirect URI "+authorizationRequest.getRedirectUri()+
					" is not registered for "+client.getClient_name());
		}		
		if (authorizationRequest.getResponseType()!=OAuthResponseType.code) 
			throw new IllegalArgumentException("Unsupported response type "+authorizationRequest.getResponseType());
	}

	public static Map<OIDCClaimName,OIDCClaimsRequestDetails> getClaimsMapFromClaimsRequestParam(String claims, String claimsField) {
		JSONObject claimsObject;
		try {
			claimsObject = new JSONObject(claims);
		} catch (JSONException e) {
			throw new IllegalArgumentException(e);
		}
		if (!claimsObject.has(claimsField)) {
			return Collections.EMPTY_MAP;
		}
		JSONObject idTokenClaims = (JSONObject)claimsObject.get(claimsField);
		return ClaimsJsonUtil.getClaimsMapFromJSONObject(idTokenClaims);
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
		if (scopes.contains(OAuthScope.openid) && !StringUtil.isNullOrEmpty(authorizationRequest.getClaims())) {
			{
				Map<OIDCClaimName,OIDCClaimsRequestDetails> idTokenClaimsMap = 
						getClaimsMapFromClaimsRequestParam(authorizationRequest.getClaims(), ID_TOKEN_CLAIMS_KEY);
				for (OIDCClaimName claim : idTokenClaimsMap.keySet()) {
					scopeDescriptions.add(CLAIM_DESCRIPTION.get(claim));
				}
			}
			{
				Map<OIDCClaimName,OIDCClaimsRequestDetails> userInfoClaimsMap = 
						getClaimsMapFromClaimsRequestParam(authorizationRequest.getClaims(), USER_INFO_CLAIMS_KEY);
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

		// 	when evaluating the claims object, how do we differentiate between a null value and a missing key?
		// 	They mean different things https://openid.net/specs/openid-connect-core-1_0.html#ClaimsParameter
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
	public Map<OIDCClaimName,String> getUserInfo(String userId, List<OAuthScope> scopes, Map<OIDCClaimName, OIDCClaimsRequestDetails>  oidcClaims) {
		Map<OIDCClaimName,String> result = new HashMap<OIDCClaimName,String>();
		// Use of [the OpenID Connect] extension [to OAuth 2.0] is requested by Clients by including the openid scope value in the Authorization Request.
		// https://openid.net/specs/openid-connect-core-1_0.html#Introduction
		if (!scopes.contains(OAuthScope.openid)) return result;

		UserProfile privateUserProfile = userProfileManager.getUserProfile(userId);

		for (OIDCClaimName claimName : oidcClaims.keySet()) {
			OIDCClaimsRequestDetails claimsDetails = oidcClaims.get(claimName);
			String claimValue = null;
			switch (claimName) {
			case email:
			case email_verified:
				claimValue = privateUserProfile.getUserName()+"@synapse.org";
				break;
			case given_name:
				claimValue = privateUserProfile.getFirstName();
				break;
			case family_name:
				claimValue = privateUserProfile.getLastName();
				break;
			case company:
				claimValue = privateUserProfile.getCompany();
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
					scopes, getClaimsMapFromClaimsRequestParam(authorizationRequest.getClaims(), ID_TOKEN_CLAIMS_KEY));
			String idToken = oidcTokenHelper.createOIDCIdToken(oauthEndpoint, ppid, oauthClientId, now, 
					authorizationRequest.getNonce(), authTimeSeconds, idTokenId, userInfo);
			result.setId_token(idToken);
		}

		String accessTokenId = UUID.randomUUID().toString();
		String accessToken = oidcTokenHelper.createOIDCaccessToken(oauthEndpoint, ppid, 
				oauthClientId, now, authTimeSeconds, accessTokenId, scopes, 
				getClaimsMapFromClaimsRequestParam(authorizationRequest.getClaims(), USER_INFO_CLAIMS_KEY));
		result.setAccess_token(accessToken);
		return result;
	}
	
	@Override
	public Object getUserInfo(Jwt<JwsHeader,Claims> accessToken, String oauthEndpoint) {

		Claims accessTokenClaimsSet = accessToken.getBody();
		// We set exactly one Audience when creating the token
		String oauthClientId = accessTokenClaimsSet.getAudience();
		if (oauthClientId==null) {
			throw new IllegalArgumentException("Missing 'audience' value in the OAuth Access Token.");
		}
		OAuthClient oauthClient = oauthClientDao.getOAuthClient(oauthClientId);

		String ppid = accessTokenClaimsSet.getSubject();
		Long authTimeSeconds = accessTokenClaimsSet.get(OIDCClaimName.auth_time.name(), Long.class);

		// userId is used to retrieve the user info
		String userId = getUserIdFromPPID(ppid, oauthClientId);

		List<OAuthScope> scopes = ClaimsJsonUtil.getScopeFromClaims(accessTokenClaimsSet);
		Map<OIDCClaimName, OIDCClaimsRequestDetails>  oidcClaims = ClaimsJsonUtil.getOIDCClaimsFromClaimSet(accessTokenClaimsSet);

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
