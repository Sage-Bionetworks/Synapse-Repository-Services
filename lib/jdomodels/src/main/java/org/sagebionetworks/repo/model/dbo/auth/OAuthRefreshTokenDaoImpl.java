package org.sagebionetworks.repo.model.dbo.auth;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_REFRESH_TOKEN_CLIENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_REFRESH_TOKEN_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_REFRESH_TOKEN_HASH;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_REFRESH_TOKEN_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_REFRESH_TOKEN_LAST_USED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_REFRESH_TOKEN_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_REFRESH_TOKEN_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_REFRESH_TOKEN_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_OAUTH_REFRESH_TOKEN;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.auth.OAuthRefreshTokenDao;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.persistence.DBOOAuthRefreshToken;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.oauth.OAuthRefreshTokenInformation;
import org.sagebionetworks.repo.model.oauth.OAuthRefreshTokenInformationList;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequest;
import org.sagebionetworks.repo.transactions.MandatoryWriteTransaction;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class OAuthRefreshTokenDaoImpl implements OAuthRefreshTokenDao {

	private static final String PARAM_TOKEN_HASH = "tokenHash";
	private static final String PARAM_NAME = "tokenName";
	private static final String PARAM_ETAG = "etag";
	private static final String PARAM_MODIFIED_ON = "modifiedOn";
	private static final String PARAM_LAST_USED = "lastUsed";
	private static final String PARAM_TOKEN_ID = "id";
	private static final String PARAM_PRINCIPAL_ID = "principalId";
	private static final String PARAM_CLIENT_ID = "clientId";
	private static final String PARAM_LIMIT = "limitParam";
	private static final String PARAM_OFFSET = "offsetParam";
	private static final String PARAM_DAYS = "days";
	private static final String PARAM_MAX_NUM_TOKENS = "maxNumberOfTokens";

	private static final String SELECT_IS_TOKEN_ACTIVE_COUNT = "SELECT COUNT(*) FROM " + TABLE_OAUTH_REFRESH_TOKEN +
			" WHERE "+ COL_OAUTH_REFRESH_TOKEN_ID+" = :" + PARAM_TOKEN_ID +
			" AND " + COL_OAUTH_REFRESH_TOKEN_LAST_USED + " > (NOW() - INTERVAL  :" + PARAM_DAYS + " DAY)";

	private static final String UPDATE_REFRESH_TOKEN_METADATA = "UPDATE "+TABLE_OAUTH_REFRESH_TOKEN+
			" SET "+
			COL_OAUTH_REFRESH_TOKEN_NAME+" = :" + PARAM_NAME + ", "+
			COL_OAUTH_REFRESH_TOKEN_ETAG+" = :" + PARAM_ETAG + ", " +
			COL_OAUTH_REFRESH_TOKEN_MODIFIED_ON + " = :"+ PARAM_MODIFIED_ON +
			" WHERE "+ COL_OAUTH_REFRESH_TOKEN_ID+" = :" + PARAM_TOKEN_ID;

	private static final String UPDATE_REFRESH_TOKEN_HASH = "UPDATE "+TABLE_OAUTH_REFRESH_TOKEN+
			" SET "+
			COL_OAUTH_REFRESH_TOKEN_HASH+" = :" + PARAM_TOKEN_HASH + ", "+
			COL_OAUTH_REFRESH_TOKEN_LAST_USED + " = :"+ PARAM_LAST_USED + ", " +
			COL_OAUTH_REFRESH_TOKEN_ETAG+" = :" + PARAM_ETAG +
			" WHERE "+ COL_OAUTH_REFRESH_TOKEN_ID+" = :" + PARAM_TOKEN_ID;

	private static final String SELECT_TOKENS_FOR_CLIENT_AND_PRINCIPAL = "SELECT * FROM " + TABLE_OAUTH_REFRESH_TOKEN
			+ " WHERE " + COL_OAUTH_REFRESH_TOKEN_PRINCIPAL_ID + " = :" + PARAM_PRINCIPAL_ID
			+ " AND " + COL_OAUTH_REFRESH_TOKEN_CLIENT_ID + " = :" + PARAM_CLIENT_ID
			+ " AND " + COL_OAUTH_REFRESH_TOKEN_LAST_USED +
				" > (NOW() - INTERVAL  :" + PARAM_DAYS + " DAY)"
			+ " ORDER BY " + COL_OAUTH_REFRESH_TOKEN_LAST_USED + " DESC"
			+ " LIMIT :" + PARAM_LIMIT + " OFFSET :" + PARAM_OFFSET;

	private static final String SELECT_TOKEN_BY_HASH = "SELECT * FROM " + TABLE_OAUTH_REFRESH_TOKEN
			+ " WHERE " + COL_OAUTH_REFRESH_TOKEN_HASH + " = :" + PARAM_TOKEN_HASH;

	private static final String DELETE_TOKEN_BY_ID = "DELETE FROM " + TABLE_OAUTH_REFRESH_TOKEN
			+ " WHERE " + COL_OAUTH_REFRESH_TOKEN_ID + " = :" + PARAM_TOKEN_ID;

	private static final String DELETE_TOKEN_BY_CLIENT_USER_PAIR = "DELETE FROM " + TABLE_OAUTH_REFRESH_TOKEN
			+ " WHERE " + COL_OAUTH_REFRESH_TOKEN_PRINCIPAL_ID + " = :" + PARAM_PRINCIPAL_ID
			+ " AND " + COL_OAUTH_REFRESH_TOKEN_CLIENT_ID + " = :" + PARAM_CLIENT_ID;

	/*
	 * We use a JOIN because
	 *   - MySQL doesn't support OFFSET when using DELETE
	 *   - MySQL doesn't support LIMIT & IN/ALL/ANY/SOME subquery (so we can't do DELETE WHERE id IN (SELECT ... LIMIT x OFFSET y))
	 *
	 * https://stackoverflow.com/a/42030157
	 *
	 * Additionally, there is no need to check expiration date because tokens that are "revoked" have been deleted, so they will not count against the limit.
	 */
	private static final String DELETE_LEAST_RECENTLY_USED_ACTIVE_TOKENS = "DELETE t FROM " + TABLE_OAUTH_REFRESH_TOKEN + " t "
			+ " JOIN ("
				+ "SELECT tt." + COL_OAUTH_REFRESH_TOKEN_ID
				+ " FROM " + TABLE_OAUTH_REFRESH_TOKEN + " tt "
				+ " WHERE " + COL_OAUTH_REFRESH_TOKEN_PRINCIPAL_ID + " = :" + PARAM_PRINCIPAL_ID
				+ " AND " + COL_OAUTH_REFRESH_TOKEN_CLIENT_ID + " = :" + PARAM_CLIENT_ID
				+ " ORDER BY " + COL_OAUTH_REFRESH_TOKEN_LAST_USED + " DESC "
				+ " LIMIT 18446744073709551615 OFFSET :" + PARAM_MAX_NUM_TOKENS //Limit is still required even if you just want offset: https://stackoverflow.com/questions/255517/mysql-offset-infinite-rows
			+ ") tt ON t." + COL_OAUTH_REFRESH_TOKEN_ID + " = tt." + COL_OAUTH_REFRESH_TOKEN_ID;

	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private IdGenerator idGenerator;

	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	private static final TableMapping<DBOOAuthRefreshToken> REFRESH_TOKEN_TABLE_MAPPING = (new DBOOAuthRefreshToken()).getTableMapping();
	// We serialize explicitly chosen fields, not the entire DTO, so no need to omit fields in the builder
	private static final UnmodifiableXStream X_STREAM = UnmodifiableXStream.builder().build();

	public static DBOOAuthRefreshToken refreshTokenDtoToDbo(OAuthRefreshTokenInformation dto) {
		DBOOAuthRefreshToken dbo = new DBOOAuthRefreshToken();
		try {
			dbo.setScopes(JDOSecondaryPropertyUtils.compressObject(X_STREAM, dto.getScopes()));
			dbo.setClaims(JDOSecondaryPropertyUtils.compressObject(X_STREAM, dto.getClaims()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		dbo.setId(Long.parseLong(dto.getTokenId()));
		dbo.setName(dto.getName());
		dbo.setPrincipalId(Long.parseLong(dto.getPrincipalId()));
		dbo.setClientId(Long.parseLong(dto.getClientId()));
		dbo.setCreatedOn(new Timestamp(dto.getAuthorizedOn().getTime()));
		dbo.setModifiedOn(new Timestamp(dto.getModifiedOn().getTime()));
		dbo.setLastUsed(new Timestamp(dto.getLastUsed().getTime()));
		dbo.setEtag(dto.getEtag());
		return dbo;
	}

	public static OAuthRefreshTokenInformation refreshTokenDboToDto(DBOOAuthRefreshToken dbo) {
		OAuthRefreshTokenInformation dto = new OAuthRefreshTokenInformation();
		try {
			dto.setScopes((List<OAuthScope>) JDOSecondaryPropertyUtils.decompressObject(X_STREAM, dbo.getScopes()));
			dto.setClaims((OIDCClaimsRequest) JDOSecondaryPropertyUtils.decompressObject(X_STREAM, dbo.getClaims()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		dto.setTokenId(dbo.getId().toString());
		dto.setName(dbo.getName());
		dto.setPrincipalId(dbo.getPrincipalId().toString());
		dto.setClientId(dbo.getClientId().toString());
		// Timestamp must be converted to Date for .equals to work on the DTO
		dto.setAuthorizedOn(new Date(dbo.getCreatedOn().getTime()));
		dto.setModifiedOn(new Date(dbo.getModifiedOn().getTime()));
		dto.setLastUsed(new Date(dbo.getLastUsed().getTime()));
		dto.setEtag(dbo.getEtag());
		return dto;
	}

	@Override
	public boolean isTokenActive(String tokenId, Long maxLeaseLengthInDays) {
		ValidateArgument.required(tokenId, "tokenId");
		ValidateArgument.required(maxLeaseLengthInDays, "maxLeaseLengthInDays");

		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(PARAM_TOKEN_ID, tokenId);
		params.addValue(PARAM_DAYS, maxLeaseLengthInDays);

		Integer count = namedParameterJdbcTemplate.queryForObject(SELECT_IS_TOKEN_ACTIVE_COUNT, params, Integer.class);

		return count != null && count > 0;
	}

	@MandatoryWriteTransaction
	@Override
	public void updateTokenHash(OAuthRefreshTokenInformation tokenInformation, String newHash) {
		ValidateArgument.required(tokenInformation, "token information");
		ValidateArgument.required(tokenInformation.getTokenId(), "Token ID");
		ValidateArgument.required(tokenInformation.getEtag(), "etag");
		ValidateArgument.required(tokenInformation.getLastUsed(), "Last used");
		ValidateArgument.required(newHash, "Token hash");

		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(PARAM_TOKEN_HASH, newHash);
		params.addValue(PARAM_ETAG, tokenInformation.getEtag());
		params.addValue(PARAM_LAST_USED, tokenInformation.getLastUsed());
		params.addValue(PARAM_TOKEN_ID, tokenInformation.getTokenId());
		namedParameterJdbcTemplate.update(UPDATE_REFRESH_TOKEN_HASH, params);
	}

	private Optional<OAuthRefreshTokenInformation> getMatchingTokenByHash(String hash, boolean forUpdate) {
		ValidateArgument.required(hash, "token hash");
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(PARAM_TOKEN_HASH, hash);

		String sql = SELECT_TOKEN_BY_HASH;
		if (forUpdate) {
			sql += " FOR UPDATE";
		}

		Optional<DBOOAuthRefreshToken> dbo;
		try {
			dbo = Optional.ofNullable(namedParameterJdbcTemplate.queryForObject(sql, params, REFRESH_TOKEN_TABLE_MAPPING));
		} catch (EmptyResultDataAccessException e) {
			dbo = Optional.empty();
		}
		return dbo.map(OAuthRefreshTokenDaoImpl::refreshTokenDboToDto);
	}

	@Override
	public Optional<OAuthRefreshTokenInformation> getMatchingTokenByHash(String hash) {
		return this.getMatchingTokenByHash(hash, false);
	}

	@MandatoryWriteTransaction
	@Override
	public Optional<OAuthRefreshTokenInformation> getMatchingTokenByHashForUpdate(String hash) {
		return this.getMatchingTokenByHash(hash, true);
	}

	@Override
	public Optional<OAuthRefreshTokenInformation> getRefreshTokenMetadata(String tokenId) {
		SinglePrimaryKeySqlParameterSource params = new SinglePrimaryKeySqlParameterSource(tokenId);
		return basicDao.getObjectByPrimaryKeyIfExists(DBOOAuthRefreshToken.class, params)
				.map(OAuthRefreshTokenDaoImpl::refreshTokenDboToDto);
	}

	@WriteTransaction
	@Override
	public OAuthRefreshTokenInformation createRefreshToken(String hashedToken, OAuthRefreshTokenInformation metadata) {
		ValidateArgument.required(metadata.getName(), "Token Name");
		ValidateArgument.required(metadata.getClientId(), "Client ID");
		ValidateArgument.required(metadata.getPrincipalId(), "Principal ID");
		ValidateArgument.required(metadata.getScopes(), "Scope");
		ValidateArgument.required(metadata.getClaims(), "Claims");
		ValidateArgument.required(metadata.getAuthorizedOn(), "Authorized On");
		ValidateArgument.required(metadata.getLastUsed(), "Last Used");
		ValidateArgument.required(metadata.getModifiedOn(), "Modified On");
		ValidateArgument.required(metadata.getEtag(), "eTag");
		metadata.setTokenId(idGenerator.generateNewId(IdType.OAUTH_REFRESH_TOKEN_ID).toString());
		DBOOAuthRefreshToken dbo = refreshTokenDtoToDbo(metadata);
		dbo.setTokenHash(hashedToken);
		basicDao.createNew(dbo);
		return this.getRefreshTokenMetadata(dbo.getId().toString()).get();
	}

	@WriteTransaction
	@Override
	public void updateRefreshTokenMetadata(OAuthRefreshTokenInformation metadata) {
		ValidateArgument.required(metadata, "Refresh token metadata");
		ValidateArgument.required(metadata.getTokenId(), "Token ID");
		ValidateArgument.required(metadata.getName(), "Token name");
		ValidateArgument.required(metadata.getEtag(), "eTag");

		// Currently, the only mutable fields are NAME, ETAG, MODIFIED_ON
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(PARAM_NAME, metadata.getName());
		params.addValue(PARAM_ETAG, metadata.getEtag());
		params.addValue(PARAM_MODIFIED_ON, metadata.getModifiedOn());
		params.addValue(PARAM_TOKEN_ID, metadata.getTokenId());
		namedParameterJdbcTemplate.update(UPDATE_REFRESH_TOKEN_METADATA, params);
	}

	@Override
	public OAuthRefreshTokenInformationList getActiveTokenInformation(String userId, String clientId, String nextPageToken, Long maxLeaseLengthInDays) {
		NextPageToken nextPage = new NextPageToken(nextPageToken);
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(PARAM_PRINCIPAL_ID, userId);
		params.addValue(PARAM_CLIENT_ID, clientId);
		params.addValue(PARAM_LIMIT, nextPage.getLimitForQuery());
		params.addValue(PARAM_OFFSET, nextPage.getOffset());
		params.addValue(PARAM_DAYS, maxLeaseLengthInDays);

		List<DBOOAuthRefreshToken> tokenDbos = namedParameterJdbcTemplate.query(
				SELECT_TOKENS_FOR_CLIENT_AND_PRINCIPAL, params, REFRESH_TOKEN_TABLE_MAPPING);
		OAuthRefreshTokenInformationList result = new OAuthRefreshTokenInformationList();
		result.setNextPageToken(nextPage.getNextPageTokenForCurrentResults(tokenDbos));
		result.setResults(
				tokenDbos.stream().map(OAuthRefreshTokenDaoImpl::refreshTokenDboToDto).collect(Collectors.toList())
		);
		return result;
	}

	@WriteTransaction
	@Override
	public void deleteToken(String tokenId) {
		ValidateArgument.required(tokenId, "tokenId");
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(PARAM_TOKEN_ID, tokenId);
		namedParameterJdbcTemplate.update(DELETE_TOKEN_BY_ID, param);
	}

	@WriteTransaction
	@Override
	public void deleteAllTokensForUserClientPair(String userId, String clientId) {
		ValidateArgument.required(userId, "Principal ID");
		ValidateArgument.required(clientId, "Client ID");
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(PARAM_PRINCIPAL_ID, userId);
		params.addValue(PARAM_CLIENT_ID, clientId);
		namedParameterJdbcTemplate.update(DELETE_TOKEN_BY_CLIENT_USER_PAIR, params);
	}

	@Override
	public void deleteLeastRecentlyUsedTokensOverLimit(String userId, String clientId, Long maxNumberOfTokens) {
		ValidateArgument.required(userId, "Principal ID");
		ValidateArgument.required(clientId, "Client ID");
		ValidateArgument.required(maxNumberOfTokens, "maxNumberOfTokens");
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(PARAM_PRINCIPAL_ID, userId);
		params.addValue(PARAM_CLIENT_ID, clientId);
		params.addValue(PARAM_MAX_NUM_TOKENS, maxNumberOfTokens);
		namedParameterJdbcTemplate.update(DELETE_LEAST_RECENTLY_USED_ACTIVE_TOKENS, params);
	}
}
