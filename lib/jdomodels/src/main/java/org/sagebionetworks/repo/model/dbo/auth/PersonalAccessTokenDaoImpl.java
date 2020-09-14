package org.sagebionetworks.repo.model.dbo.auth;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PERSONAL_ACCESS_TOKEN_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PERSONAL_ACCESS_TOKEN_LAST_USED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PERSONAL_ACCESS_TOKEN_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PERSONAL_ACCESS_TOKEN;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.auth.AccessTokenRecord;
import org.sagebionetworks.repo.model.auth.AccessTokenRecordList;
import org.sagebionetworks.repo.model.auth.PersonalAccessTokenDao;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.persistence.DBOPersonalAccessToken;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class PersonalAccessTokenDaoImpl implements PersonalAccessTokenDao {

	private static final String PARAM_TOKEN_ID = "id";
	private static final String PARAM_LAST_USED = "lastUsed";
	private static final String PARAM_PRINCIPAL_ID = "principalId";
	private static final String PARAM_LIMIT = "limitParam";
	private static final String PARAM_OFFSET = "offsetParam";
	private static final String PARAM_MAX_NUM_TOKENS = "maxNumberOfTokens";

	private static final String SELECT_LAST_USED_DATE_BY_ID =
			"SELECT " + COL_PERSONAL_ACCESS_TOKEN_LAST_USED + " FROM " + TABLE_PERSONAL_ACCESS_TOKEN
			+ " WHERE " + COL_PERSONAL_ACCESS_TOKEN_ID + " = :" + PARAM_TOKEN_ID;

	private static final String SELECT_TOKENS_FOR_PRINCIPAL = "SELECT * FROM " + TABLE_PERSONAL_ACCESS_TOKEN
			+ " WHERE " + COL_PERSONAL_ACCESS_TOKEN_PRINCIPAL_ID + " = :" + PARAM_PRINCIPAL_ID
			+ " ORDER BY " + COL_PERSONAL_ACCESS_TOKEN_LAST_USED + " DESC"
			+ " LIMIT :" + PARAM_LIMIT + " OFFSET :" + PARAM_OFFSET;

	private static final String UPDATE_LAST_USED = "UPDATE " + TABLE_PERSONAL_ACCESS_TOKEN+
			" SET "+
			COL_PERSONAL_ACCESS_TOKEN_LAST_USED+" = :" + PARAM_LAST_USED +
			" WHERE "+ COL_PERSONAL_ACCESS_TOKEN_ID+" = :" + PARAM_TOKEN_ID;


	/*
	 * We use a JOIN because
	 *   - MySQL doesn't support OFFSET when using DELETE
	 *   - MySQL doesn't support LIMIT & IN/ALL/ANY/SOME subquery (so we can't do DELETE WHERE id IN (SELECT ... LIMIT x OFFSET y))
	 *
	 * https://stackoverflow.com/a/42030157
	 *
	 * Additionally, there is no need to check expiration date because tokens that are "revoked" have been deleted, so they will not count against the limit.
	 */
	private static final String DELETE_LEAST_RECENTLY_USED_TOKENS = "DELETE t FROM " + TABLE_PERSONAL_ACCESS_TOKEN + " t "
			+ " JOIN ("
				+ "SELECT tt." + COL_PERSONAL_ACCESS_TOKEN_ID
				+ " FROM " + TABLE_PERSONAL_ACCESS_TOKEN + " tt "
				+ " WHERE " + COL_PERSONAL_ACCESS_TOKEN_PRINCIPAL_ID + " = :" + PARAM_PRINCIPAL_ID
				+ " ORDER BY " + COL_PERSONAL_ACCESS_TOKEN_LAST_USED + " DESC "
				+ " LIMIT 18446744073709551615 OFFSET :" + PARAM_MAX_NUM_TOKENS //Limit is still required even if you just want offset: https://stackoverflow.com/questions/255517/mysql-offset-infinite-rows
			+ ") tt ON t." + COL_PERSONAL_ACCESS_TOKEN_ID + " = tt." + COL_PERSONAL_ACCESS_TOKEN_ID;

	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private IdGenerator idGenerator;

	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	private static final TableMapping<DBOPersonalAccessToken> PERSONAL_ACCESS_TOKEN_TABLE_MAPPING = (new DBOPersonalAccessToken()).getTableMapping();
	// We serialize explicitly chosen fields, not the entire DTO, so no need to omit fields in the builder
	private static final UnmodifiableXStream X_STREAM = UnmodifiableXStream.builder().build();

	public static DBOPersonalAccessToken personalAccessTokenDtoToDbo(AccessTokenRecord dto) {
		DBOPersonalAccessToken dbo = new DBOPersonalAccessToken();
		try {
			dbo.setScopes(JDOSecondaryPropertyUtils.compressObject(X_STREAM, dto.getScopes()));
			dbo.setClaims(JDOSecondaryPropertyUtils.compressObject(X_STREAM, dto.getUserInfoClaims()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		dbo.setId(Long.parseLong(dto.getId()));
		dbo.setPrincipalId(Long.parseLong(dto.getUserId()));
		dbo.setName(dto.getName());
		dbo.setCreatedOn(new Timestamp(dto.getCreatedOn().getTime()));
		dbo.setLastUsed(new Timestamp(dto.getLastUsed().getTime()));
		return dbo;
	}

	public static AccessTokenRecord personalAccessTokenDboToDto(DBOPersonalAccessToken dbo) {
		AccessTokenRecord dto = new AccessTokenRecord();
		try {
			dto.setScopes((List<OAuthScope>) JDOSecondaryPropertyUtils.decompressObject(X_STREAM, dbo.getScopes()));
			dto.setUserInfoClaims((Map<String, OIDCClaimsRequestDetails>) JDOSecondaryPropertyUtils.decompressObject(X_STREAM, dbo.getClaims()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		dto.setId(dbo.getId().toString());
		dto.setUserId(dbo.getPrincipalId().toString());
		dto.setName(dbo.getName());
		// Timestamp must be converted to Date for .equals to work on the DTO
		dto.setCreatedOn(new Date(dbo.getCreatedOn().getTime()));
		dto.setLastUsed(new Date(dbo.getLastUsed().getTime()));
		return dto;
	}

	@Override
	public AccessTokenRecord getTokenRecord(String tokenId) throws NotFoundException {
		SinglePrimaryKeySqlParameterSource params = new SinglePrimaryKeySqlParameterSource(tokenId);
		DBOPersonalAccessToken dbo =  basicDao.getObjectByPrimaryKey(DBOPersonalAccessToken.class, params);
		return personalAccessTokenDboToDto(dbo);
	}

	@Override
	public Date getLastUsedDate(String tokenId) throws NotFoundException {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(PARAM_TOKEN_ID, tokenId);
		Date result;
		try {
			result = namedParameterJdbcTemplate.queryForObject(SELECT_LAST_USED_DATE_BY_ID, params, Date.class);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("The token record does not exist.");
		}
		return result;
	}

	@WriteTransaction
	@Override
	public AccessTokenRecord createTokenRecord(AccessTokenRecord metadata) {
		ValidateArgument.required(metadata.getUserId(), "userId");
		ValidateArgument.required(metadata.getName(), "Token Name");
		ValidateArgument.required(metadata.getScopes(), "Scope");
		ValidateArgument.required(metadata.getUserInfoClaims(), "Claims");
		ValidateArgument.required(metadata.getCreatedOn(), "Created On");
		ValidateArgument.required(metadata.getLastUsed(), "Last Used");
		metadata.setId(idGenerator.generateNewId(IdType.PERSONAL_ACCESS_TOKEN_ID).toString());
		DBOPersonalAccessToken dbo = personalAccessTokenDtoToDbo(metadata);
		basicDao.createNew(dbo);
		return this.getTokenRecord(dbo.getId().toString());
	}

	@Override
	public AccessTokenRecordList getTokenRecords(String userId, String nextPageToken) {
		NextPageToken nextPage = new NextPageToken(nextPageToken);
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(PARAM_PRINCIPAL_ID, userId);
		params.addValue(PARAM_LIMIT, nextPage.getLimitForQuery());
		params.addValue(PARAM_OFFSET, nextPage.getOffset());

		List<DBOPersonalAccessToken> tokenDbos = namedParameterJdbcTemplate.query(
				SELECT_TOKENS_FOR_PRINCIPAL, params, PERSONAL_ACCESS_TOKEN_TABLE_MAPPING);
		AccessTokenRecordList result = new AccessTokenRecordList();
		result.setNextPageToken(nextPage.getNextPageTokenForCurrentResults(tokenDbos));
		result.setResults(
				tokenDbos.stream().map(PersonalAccessTokenDaoImpl::personalAccessTokenDboToDto).collect(Collectors.toList())
		);
		return result;
	}

	@WriteTransaction
	@Override
	public void updateLastUsed(String tokenId) {
		Date now = new Date();
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(PARAM_TOKEN_ID, tokenId);
		params.addValue(PARAM_LAST_USED, now);

		namedParameterJdbcTemplate.update(UPDATE_LAST_USED, params);
	}

	@WriteTransaction
	@Override
	public void deleteToken(String tokenId) {
		ValidateArgument.required(tokenId, "tokenId");
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(PARAM_TOKEN_ID, tokenId);
		basicDao.deleteObjectByPrimaryKey(DBOPersonalAccessToken.class, param);
	}

	@Override
	public void deleteLeastRecentlyUsedTokensOverLimit(String userId, Long maxNumberOfTokens) {
		ValidateArgument.required(userId, "Principal ID");
		ValidateArgument.required(maxNumberOfTokens, "maxNumberOfTokens");
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(PARAM_PRINCIPAL_ID, userId);
		params.addValue(PARAM_MAX_NUM_TOKENS, maxNumberOfTokens);
		namedParameterJdbcTemplate.update(DELETE_LEAST_RECENTLY_USED_TOKENS, params);
	}
}
