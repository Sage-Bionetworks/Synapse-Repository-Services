package org.sagebionetworks.repo.manager.authentication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.repo.manager.oauth.ClaimsJsonUtil;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenHelper;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.UnauthenticatedException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AccessTokenGenerationRequest;
import org.sagebionetworks.repo.model.auth.AccessTokenGenerationResponse;
import org.sagebionetworks.repo.model.auth.AccessTokenRecord;
import org.sagebionetworks.repo.model.auth.AccessTokenRecordList;
import org.sagebionetworks.repo.model.auth.AccessTokenState;
import org.sagebionetworks.repo.model.auth.PersonalAccessTokenDao;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.EnumKeyedJsonMapUtil;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwt;

@Service
public class PersonalAccessTokenManagerImpl implements PersonalAccessTokenManager {

	static final String DUPLICATE_TOKEN_NAME_MSG = "You cannot have two tokens with the same name. You must delete the existing token to create a new one with this name.";

	// The maximum time, in days, that a token remains active if unused.
	private static final long MAX_TOKEN_LEASE_LENGTH_DAYS = 180L;
	private static final long MAX_TOKEN_LEASE_LENGTH_MILLIS = MAX_TOKEN_LEASE_LENGTH_DAYS * 24 * 60 * 60 * 1000;

	private static final long MAX_NUMBER_OF_TOKENS_PER_USER = 100L;

	// the minimum period in which we update the 'last updated' time stamp for a token
	private static final Long UPDATE_THRESHOLD_MILLIS = 60*1000L; // one minute

	@Autowired
	private PersonalAccessTokenDao personalAccessTokenDao;

	@Autowired
	private OIDCTokenHelper oidcTokenHelper;

	@Autowired
	private Clock clock;

	/**
	 * Determine the state of the access token record using the last used date.
	 * Method exposed for testing.
	 * @param lastUsedDate The last used date of the token.
	 * @return the state of the token
	 */
	AccessTokenState determineActiveState(Date lastUsedDate) {
		Date lastUsedExpirationDate = new Date(clock.currentTimeMillis() - MAX_TOKEN_LEASE_LENGTH_MILLIS);
		boolean active = lastUsedDate.after(lastUsedExpirationDate);
		return active ? AccessTokenState.ACTIVE : AccessTokenState.EXPIRED;
	}

	@WriteTransaction
	@Override
	public AccessTokenGenerationResponse issueToken(UserInfo userInfo, String accessToken, AccessTokenGenerationRequest request, String oauthEndpoint) {
		ValidateArgument.required(request, "AccessTokenGenerationRequest");
		if (AuthorizationUtils.isUserAnonymous(userInfo)) {
			throw new UnauthenticatedException("Anonymous users may not issue personal access tokens.");
		}
		
		// now get the scopes permitted in the Synapse access token
		List<OAuthScope> oauthScopes = Collections.EMPTY_LIST;
		if (accessToken!=null) {
			Jwt<JwsHeader, Claims> jwt = oidcTokenHelper.parseJWT(accessToken);
			oauthScopes = ClaimsJsonUtil.getScopeFromClaims(jwt.getBody());
		}
		
		// Replace an empty name with a UUID
		if (StringUtils.isBlank(request.getName())) {
			request.setName(UUID.randomUUID().toString());
		}

		// Replace null/empty scope with all scopes
		if (request.getScope() == null || request.getScope().isEmpty()) {
			request.setScope(new ArrayList<>());
			for (OAuthScope scope : OAuthScope.values()) {
				request.getScope().add(scope);
			}
		}
		
		// cannot have more scopes than those granted by the authorizing access token
		List<OAuthScope> grantedScopes = new ArrayList(request.getScope());
		grantedScopes.retainAll(oauthScopes);
		
		// we do not grant 'authorize' scope
		// Note, we use 'removeAll' rather than 'remove' to remove *all* occurrences of the disallowed scope.
		grantedScopes.removeAll(Collections.singletonList(OAuthScope.authorize));

		if (request.getUserInfoClaims() == null) {
			request.setUserInfoClaims(new HashMap<>());
		}

		// Convert claims to enum and back to remove invalid claims -- see PLFM-6254
		request.setUserInfoClaims(
				EnumKeyedJsonMapUtil.convertKeysToStrings(
						EnumKeyedJsonMapUtil.convertKeysToEnums(request.getUserInfoClaims(), OIDCClaimName.class)
				)
		);

		AccessTokenRecord record = new AccessTokenRecord();
		record.setUserId(userInfo.getId().toString());
		record.setName(request.getName());
		record.setScopes(grantedScopes);
		record.setUserInfoClaims(request.getUserInfoClaims());
		Date now = clock.now();
		record.setCreatedOn(now);
		record.setLastUsed(now);

		try {
			record = personalAccessTokenDao.createTokenRecord(record);
		} catch (IllegalArgumentException e) {
			if (e.getCause() instanceof DuplicateKeyException) {
				throw new IllegalArgumentException(DUPLICATE_TOKEN_NAME_MSG, e);
			} else {
				throw e;
			}
		}
		AccessTokenGenerationResponse response = new AccessTokenGenerationResponse();
		response.setToken(oidcTokenHelper.createPersonalAccessToken(oauthEndpoint, record));

		// If the user has over 100 tokens, delete the least recently used to get under the limit.
		personalAccessTokenDao.deleteLeastRecentlyUsedTokensOverLimit(userInfo.getId().toString(), MAX_NUMBER_OF_TOKENS_PER_USER);

		return response;
	}

	@Override
	public boolean isTokenActive(String tokenId) {
		Date lastUsedDate;
		try {
			lastUsedDate = personalAccessTokenDao.getLastUsedDate(tokenId);
		} catch (NotFoundException e) {
			return false;
		}
		return determineActiveState(lastUsedDate).equals(AccessTokenState.ACTIVE);
	}
	
	@WriteTransaction
	@Override
	public void updateLastUsedTime(String tokenId) {
		Date lastUsedDate;
		try {
			lastUsedDate = personalAccessTokenDao.getLastUsedDate(tokenId);
		} catch (NotFoundException e) {
			lastUsedDate = null;
		}
		if (lastUsedDate==null || clock.currentTimeMillis()>=lastUsedDate.getTime()+UPDATE_THRESHOLD_MILLIS) {
			personalAccessTokenDao.updateLastUsed(tokenId);
		}
	}

	@Override
	public AccessTokenRecordList getTokenRecords(UserInfo userInfo, String nextPageToken) {
		if (AuthorizationUtils.isUserAnonymous(userInfo)) {
			throw new UnauthenticatedException("Anonymous users cannot have personal access tokens.");
		}

		AccessTokenRecordList records = personalAccessTokenDao.getTokenRecords(userInfo.getId().toString(), nextPageToken);
		for (AccessTokenRecord r : records.getResults()) {
			r.setState(determineActiveState(r.getLastUsed()));
		}
		return records;
	}

	@Override
	public AccessTokenRecord getTokenRecord(UserInfo userInfo, String tokenId) throws NotFoundException, UnauthorizedException {
		AccessTokenRecord record = personalAccessTokenDao.getTokenRecord(tokenId);
		if (userInfo.getId().toString().equals(record.getUserId()) || userInfo.isAdmin()) {
			record.setState(determineActiveState(record.getLastUsed()));
			return record;
		} else {
			throw new UnauthorizedException("You do not have permission to view this token record.");
		}
	}

	@WriteTransaction
	@Override
	public void revokeToken(UserInfo userInfo, String tokenId) {
		AccessTokenRecord record = personalAccessTokenDao.getTokenRecord(tokenId);
		if (userInfo.getId().toString().equals(record.getUserId()) || userInfo.isAdmin()) {
			personalAccessTokenDao.deleteToken(tokenId);
		} else {
			throw new UnauthorizedException("You do not have permission to revoke this token.");
		}
	}
}
