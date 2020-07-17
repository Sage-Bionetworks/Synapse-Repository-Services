package org.sagebionetworks.repo.manager.authentication;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
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
import org.springframework.stereotype.Service;

@Service
public class PersonalAccessTokenManagerImpl implements PersonalAccessTokenManager {

	// The maximum time, in days, that a token remains active if unused.
	private static final long MAX_TOKEN_LEASE_LENGTH_DAYS = 180L;
	private static final long MAX_TOKEN_LEASE_LENGTH_MILLIS = MAX_TOKEN_LEASE_LENGTH_DAYS * 24 * 60 * 60 * 1000;

	@Autowired
	private PersonalAccessTokenDao personalAccessTokenDao;

	@Autowired
	private OIDCTokenHelper oidcTokenHelper;

	@Autowired
	private Clock clock;

	/**
	 * Determine the state of the access token record using the last used or created on fields.
	 * Method exposed for testing.
	 * @param record
	 * @return the state of the token
	 */
	AccessTokenState determineActiveState(AccessTokenRecord record) {
		Date lastUsedDate;
		if (record.getLastUsed() != null) {
			lastUsedDate = record.getLastUsed();
		} else {
			lastUsedDate = record.getCreatedOn();
		}
		Date lastUsedExpirationDate = new Date(clock.currentTimeMillis() - MAX_TOKEN_LEASE_LENGTH_MILLIS);
		boolean active = lastUsedDate.after(lastUsedExpirationDate);
		return active ? AccessTokenState.ACTIVE : AccessTokenState.EXPIRED;
	}

	@WriteTransaction
	@Override
	public AccessTokenGenerationResponse issueToken(UserInfo userInfo, AccessTokenGenerationRequest request, String oauthEndpoint) {
		ValidateArgument.required(request, "AccessTokenGenerationRequest");
		if (AuthorizationUtils.isUserAnonymous(userInfo)) {
			throw new UnauthenticatedException("Anonymous users may not issue personal access tokens.");
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

		if (request.getUserInfoClaims() == null) {
			request.setUserInfoClaims(new HashMap<>());
		}

		// Convert claims to enum and back to remove invalid claims -- see PLFM-6254
		request.setUserInfoClaims(
				EnumKeyedJsonMapUtil.convertToString(
						EnumKeyedJsonMapUtil.convertToEnum(request.getUserInfoClaims(), OIDCClaimName.class)
				)
		);

		AccessTokenRecord record = new AccessTokenRecord();
		record.setUserId(userInfo.getId().toString());
		record.setName(request.getName());
		record.setScopes(request.getScope());
		record.setUserInfoClaims(request.getUserInfoClaims());
		Date now = clock.now();
		record.setCreatedOn(now);
		record.setLastUsed(now);

		record = personalAccessTokenDao.createTokenRecord(record);
		AccessTokenGenerationResponse response = new AccessTokenGenerationResponse();
		response.setToken(oidcTokenHelper.createPersonalAccessToken(oauthEndpoint, record));
		return response;
	}

	@Override
	public boolean isTokenActive(String tokenId) {
		AccessTokenRecord record;
		try {
			record = personalAccessTokenDao.getTokenRecord(tokenId);
		} catch (NotFoundException e) {
			return false;
		}
		return determineActiveState(record).equals(AccessTokenState.ACTIVE);
	}

	@WriteTransaction
	@Override
	public void updateLastUsedTime(String tokenId) {
		personalAccessTokenDao.updateLastUsed(tokenId);
	}

	@Override
	public AccessTokenRecordList getTokenRecords(UserInfo userInfo, String nextPageToken) {
		if (AuthorizationUtils.isUserAnonymous(userInfo)) {
			throw new UnauthenticatedException("Anonymous users cannot have personal access tokens.");
		}

		AccessTokenRecordList records = personalAccessTokenDao.getTokenRecords(userInfo.getId().toString(), nextPageToken);
		for (AccessTokenRecord r : records.getResults()) {
			r.setState(determineActiveState(r));
		}
		return records;
	}

	@Override
	public AccessTokenRecord getTokenRecord(UserInfo userInfo, String tokenId) throws NotFoundException, UnauthorizedException {
		AccessTokenRecord record = personalAccessTokenDao.getTokenRecord(tokenId);
		if (userInfo.getId().toString().equals(record.getUserId()) || userInfo.isAdmin()) {
			record.setState(determineActiveState(record));
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
