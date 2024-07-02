package org.sagebionetworks.repo.model.dbo.auth;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_ACCESS_TOKEN_EXPIRES_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_ACCESS_TOKEN_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_ACCESS_TOKEN_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_ACCESS_TOKEN_TOKEN_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_OAUTH_ACCESS_TOKEN;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOOAuthAccessToken;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Data access layer to manage the storage of access token records
 */
@Repository
public class OAuthAccessTokenDaoImpl implements OAuthAccessTokenDao {
	
	private static final int DELETE_BATCH_SIZE = 1_000;

	private IdGenerator idGenerator;
	private DBOBasicDao basicDao;
	private JdbcTemplate jdbcTemplate;
	
	public OAuthAccessTokenDaoImpl(DBOBasicDao basicDao, IdGenerator idGenerator, JdbcTemplate jdbcTemplate) {
		this.basicDao = basicDao;
		this.idGenerator = idGenerator;
		this.jdbcTemplate = jdbcTemplate;
	}
	
	@Override
	@WriteTransaction
	public void storeAccessTokenRecord(OIDCAccessTokenData data) {
		DBOOAuthAccessToken token = new DBOOAuthAccessToken();
		
		token.setId(idGenerator.generateNewId(IdType.OAUTH_ACCESS_TOKEN_ID));
		token.setTokenId(data.getTokenId());
		token.setPrincipalId(data.getPrincipalId());
		token.setClientId(data.getClientId());
		token.setCreatedOn(data.getCreatedOn());
		token.setExpiresOn(data.getExpiresOn());
		token.setSessionId(data.getSessionId());
		token.setRefreshTokenId(data.getRefreshTokenId());
		
		basicDao.createNew(token);
	}
	
	@Override
	public boolean doesAccessTokenRecordExist(String tokenId) {
		String sql = "SELECT COUNT(*) FROM " + TABLE_OAUTH_ACCESS_TOKEN + " WHERE " + COL_OAUTH_ACCESS_TOKEN_TOKEN_ID + "=?";
		
		return jdbcTemplate.queryForObject(sql, Long.class, tokenId) > 0;
	}
	
	@Override
	@WriteTransaction
	public void deleteAccessTokenRecords(Long userId) {
		String sql = "DELETE FROM " + TABLE_OAUTH_ACCESS_TOKEN + " WHERE " + COL_OAUTH_ACCESS_TOKEN_PRINCIPAL_ID + "=?";
		
		jdbcTemplate.update(sql, userId);
	}
	
	@Override
	@WriteTransaction
	public void deleteAccessTokenRecord(String tokenId) {
		String sql = "DELETE FROM " + TABLE_OAUTH_ACCESS_TOKEN + " WHERE " + COL_OAUTH_ACCESS_TOKEN_TOKEN_ID + "=?";
		
		jdbcTemplate.update(sql, tokenId);
	}
	
	@Override
	@WriteTransaction
	public int deleteExpiredTokens() {
		String selectSql = "SELECT " + COL_OAUTH_ACCESS_TOKEN_ID + " FROM " +TABLE_OAUTH_ACCESS_TOKEN + " WHERE " 
				+ COL_OAUTH_ACCESS_TOKEN_EXPIRES_ON + " < (NOW() - INTERVAL 1 DAY) LIMIT ?"; 
		
		List<Long> idsToDelete = jdbcTemplate.queryForList(selectSql, Long.class, DELETE_BATCH_SIZE);

		String deleteSql = "DELETE FROM " + TABLE_OAUTH_ACCESS_TOKEN + " WHERE " + COL_OAUTH_ACCESS_TOKEN_ID + "=?";
		
		jdbcTemplate.batchUpdate(deleteSql, new BatchPreparedStatementSetter() {
			
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				ps.setLong(1, idsToDelete.get(i));				
			}
			
			@Override
			public int getBatchSize() {
				return idsToDelete.size();
			}
		});

		return idsToDelete.size();
		
	}

}
