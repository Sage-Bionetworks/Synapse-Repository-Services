package org.sagebionetworks.repo.model.dbo.auth;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_CLIENT_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_CLIENT_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_CLIENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_CLIENT_IS_VERIFIED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_CLIENT_SECRET_HASH;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_CLIENT_SECTOR_IDENTIFIER_URI;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_REFRESH_TOKEN_CLIENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_REFRESH_TOKEN_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_REFRESH_TOKEN_LAST_USED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_REFRESH_TOKEN_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_SECTOR_IDENTIFIER_SECRET;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_SECTOR_IDENTIFIER_URI;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_OAUTH_CLIENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_OAUTH_REFRESH_TOKEN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_OAUTH_SECTOR_IDENTIFIER;

import java.io.IOException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.BooleanUtils;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.auth.OAuthClientDao;
import org.sagebionetworks.repo.model.auth.SectorIdentifier;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.dbo.persistence.DBOOAuthClient;
import org.sagebionetworks.repo.model.dbo.persistence.DBOSectorIdentifier;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.oauth.OAuthClientAuthorizationHistory;
import org.sagebionetworks.repo.model.oauth.OAuthClientAuthorizationHistoryList;
import org.sagebionetworks.repo.model.oauth.OAuthClientList;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.securitytools.PBKDF2Utils;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.IncorrectResultSetColumnCountException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

public class OAuthClientDaoImpl implements OAuthClientDao {

	private static final String INTERMEDIATE_COL_OVERALL_LAST_USED = "OVERALL_LAST_USED";
	private static final String INTERMEDIATE_COL_FIRST_AUTHORIZED_ON = "FIRST_AUTHORIZED_ON";

	private static final String SECTOR_IDENTIFIER_SQL_SELECT = "SELECT COUNT(*) FROM "+TABLE_OAUTH_SECTOR_IDENTIFIER			
			+" WHERE "+COL_OAUTH_SECTOR_IDENTIFIER_URI+" = ?";

	private static final String CLIENT_SQL_SELECT = "SELECT * FROM "+TABLE_OAUTH_CLIENT+" WHERE "+
			COL_OAUTH_CLIENT_CREATED_BY+" = ? LIMIT ? OFFSET ?";
	
	private static final String CLIENT_CREATOR_SQL_SELECT = "SELECT "+COL_OAUTH_CLIENT_CREATED_BY+" FROM "+TABLE_OAUTH_CLIENT+" WHERE "+
			COL_OAUTH_CLIENT_ID+" = ?";

	private static final String CLIENT_SECRET_HASH_SQL_SELECT = "SELECT "+COL_OAUTH_CLIENT_SECRET_HASH+
			" FROM "+TABLE_OAUTH_CLIENT
			+ " WHERE "+COL_OAUTH_CLIENT_ID+" = ?";

	private static final String CLIENT_AND_SECRET_HASH_COUNT_SQL_SELECT = "SELECT COUNT(*) "+
			" FROM "+TABLE_OAUTH_CLIENT
			+ " WHERE "+COL_OAUTH_CLIENT_ID+" = ? AND "+COL_OAUTH_CLIENT_SECRET_HASH+" = ?";

	private static final String SECTOR_IDENTIFIER_SELECT_FOR_CLIENT_SQL = "SELECT s."+COL_OAUTH_SECTOR_IDENTIFIER_SECRET+
			" FROM "+TABLE_OAUTH_SECTOR_IDENTIFIER+" s INNER JOIN "+TABLE_OAUTH_CLIENT+" c ON "+
			"c."+COL_OAUTH_CLIENT_SECTOR_IDENTIFIER_URI+" = s."+COL_OAUTH_SECTOR_IDENTIFIER_URI+
			" WHERE c."+COL_OAUTH_CLIENT_ID+" = ?";

	private static final String SECTOR_IDENTIFIER_SQL_DELETE = "DELETE FROM "+TABLE_OAUTH_SECTOR_IDENTIFIER
			+" WHERE "+COL_OAUTH_SECTOR_IDENTIFIER_URI+" = ?";

	private static final String SET_CLIENT_SECRET_HASH = "UPDATE "+TABLE_OAUTH_CLIENT+
			" SET "+COL_OAUTH_CLIENT_SECRET_HASH+"= ?, "+
			COL_OAUTH_CLIENT_ETAG+"= ? WHERE "+ COL_OAUTH_CLIENT_ID+"= ?";
	
	private static final String CLIENT_VERIFIED_SQL_SELECT = "SELECT " + COL_OAUTH_CLIENT_IS_VERIFIED +
			" FROM " + TABLE_OAUTH_CLIENT
			+ " WHERE " + COL_OAUTH_CLIENT_ID + " = ?";

	private static final String SELECT_CLIENTS_WITH_ACTIVE_TOKENS_FOR_PRINCIPAL =
			"SELECT MAX(rt." + COL_OAUTH_REFRESH_TOKEN_LAST_USED + ") AS " + INTERMEDIATE_COL_OVERALL_LAST_USED
					+ ", MIN(rt." + COL_OAUTH_REFRESH_TOKEN_CREATED_ON + ") AS " + INTERMEDIATE_COL_FIRST_AUTHORIZED_ON
					+ ", c.*"
					+ " FROM " + TABLE_OAUTH_REFRESH_TOKEN + " rt, "
					+ TABLE_OAUTH_CLIENT + " c "
					+ "WHERE rt." + COL_OAUTH_REFRESH_TOKEN_CLIENT_ID + " = c." + COL_OAUTH_CLIENT_ID
					+ " AND " + COL_OAUTH_REFRESH_TOKEN_PRINCIPAL_ID + " = ?"
					+ " AND " + COL_OAUTH_REFRESH_TOKEN_LAST_USED + " > (NOW() - INTERVAL ? DAY) "
					+ " GROUP BY c." + COL_OAUTH_CLIENT_ID
					+ " LIMIT ? OFFSET ?";


	@Autowired
	private DBOBasicDao basicDao;	

	@Autowired
	private IdGenerator idGenerator;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	// Note, we do not serialize fields which are 'broken out' ino their own column in DBOAuthClient
	private static final UnmodifiableXStream X_STREAM = UnmodifiableXStream.builder()
			.allowTypes(OAuthClient.class)
			.omitField(OAuthClient.class, "client_id")
			.omitField(OAuthClient.class, "createdBy")
			.omitField(OAuthClient.class, "createdOn")
			.omitField(OAuthClient.class, "etag")
			.omitField(OAuthClient.class, "modifiedOn")
			.omitField(OAuthClient.class, "sector_identifier")
			.omitField(OAuthClient.class, "client_name")
			.omitField(OAuthClient.class, "verified")
			.build();

	// Note, this drop the 'secretHash' fields, which is not part of the DTO
	public static OAuthClient clientDboToDto(DBOOAuthClient dbo) {
		OAuthClient dto;
		try {
			dto = (OAuthClient)JDOSecondaryPropertyUtils.decompressObject(X_STREAM, dbo.getProperties());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		dto.setClient_id(dbo.getId().toString());
		dto.setClient_name(dbo.getName());
		dto.setCreatedBy(dbo.getCreatedBy().toString());
		dto.setCreatedOn(new Date(dbo.getCreatedOn()));
		dto.setModifiedOn(new Date(dbo.getModifiedOn()));
		dto.setEtag(dbo.geteTag());
		dto.setSector_identifier(dbo.getSectorIdentifierUri());
		dto.setVerified(dbo.getVerified());
		return dto;
	}

	// Note, the returned value has no 'secretHash' field, which is managed separately
	public static DBOOAuthClient clientDtoToDbo(OAuthClient dto) {
		DBOOAuthClient dbo = new DBOOAuthClient();
		try {
			dbo.setProperties(JDOSecondaryPropertyUtils.compressObject(X_STREAM, dto));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		dbo.setId(Long.parseLong(dto.getClient_id()));
		dbo.setName(dto.getClient_name());
		dbo.setCreatedBy(Long.parseLong(dto.getCreatedBy()));
		dbo.setCreatedOn(dto.getCreatedOn().getTime());
		dbo.setModifiedOn(dto.getModifiedOn().getTime());
		dbo.seteTag(dto.getEtag());
		dbo.setSectorIdentifierUri(dto.getSector_identifier());
		dbo.setVerified(BooleanUtils.isTrue(dto.getVerified()));
		return dbo;
	}

	@WriteTransaction
	@Override
	public void deleteSectorIdentifer(String sectorIdentiferUri) {
		jdbcTemplate.update(SECTOR_IDENTIFIER_SQL_DELETE, sectorIdentiferUri);
	}

	@WriteTransaction
	@Override
	public OAuthClient createOAuthClient(OAuthClient client) {
		ValidateArgument.required(client, "OAuth client");
		ValidateArgument.required(client.getCreatedBy(), "Created By");
		ValidateArgument.required(client.getSector_identifier(), "Sector Identifier");
		ValidateArgument.required(client.getVerified(), "Is Verified");
		ValidateArgument.required(client.getEtag(), "etag");
		Date now = new Date(System.currentTimeMillis());
		client.setCreatedOn(now);
		client.setModifiedOn(now);
		String id = idGenerator.generateNewId(IdType.OAUTH_CLIENT_ID).toString();
		client.setClient_id(id);
		DBOOAuthClient dbo = clientDtoToDbo(client);
		try {
			basicDao.createNew(dbo);
		} catch (IllegalArgumentException e) {
			if (e.getCause() instanceof DuplicateKeyException) {
				throw new IllegalArgumentException("OAuth client already exists with name "+dbo.getName(), e);
			}
			throw e;
		}
		return client;
	}

	@Override
	public OAuthClient getOAuthClient(String clientId) {
		ValidateArgument.required(clientId, "Client ID");
		SqlParameterSource param = new SinglePrimaryKeySqlParameterSource(clientId);
		DBOOAuthClient dbo = basicDao.getObjectByPrimaryKey(DBOOAuthClient.class, param);
		return clientDboToDto(dbo);
	}
	
	@Override
	public OAuthClient selectOAuthClientForUpdate(String clientId) {
		ValidateArgument.required(clientId, "Client ID");
		SqlParameterSource param = new SinglePrimaryKeySqlParameterSource(clientId);
		DBOOAuthClient dbo = basicDao.getObjectByPrimaryKeyWithUpdateLock(DBOOAuthClient.class, param);
		return clientDboToDto(dbo);
	}
	
	/**
	 * 
	 * @param clientId
	 * @return
	 */
	@Override
	public String getOAuthClientCreator(String clientId) {
		try {
			return jdbcTemplate.queryForObject(CLIENT_CREATOR_SQL_SELECT, String.class, clientId);
		} catch (EmptyResultDataAccessException e) {
			throw clientNotFoundException(clientId);
		}
	}

	@Override
	public OAuthClientList listOAuthClients(String nextPageToken, Long userId) {
		NextPageToken nextPage = new NextPageToken(nextPageToken);
		List<DBOOAuthClient> dboList = jdbcTemplate.query(CLIENT_SQL_SELECT, 
				new Object[] {userId, nextPage.getLimitForQuery(), nextPage.getOffset()}, 
				(new DBOOAuthClient()).getTableMapping());
		OAuthClientList result = new OAuthClientList();
		result.setNextPageToken(nextPage.getNextPageTokenForCurrentResults(dboList));
		List<OAuthClient> dtoList = new ArrayList<OAuthClient>();
		for (DBOOAuthClient dbo : dboList) {
			dtoList.add(clientDboToDto(dbo));
		}
		result.setResults(dtoList);
		return result;
	}

	@Override
	public boolean checkOAuthClientSecretHash(String clientId, String secretHash) {
		ValidateArgument.required(clientId, "Client ID");
		ValidateArgument.required(secretHash, "Secret Hash");
		return jdbcTemplate.queryForObject(CLIENT_AND_SECRET_HASH_COUNT_SQL_SELECT, Integer.class, clientId, secretHash)>0;
	}

	@Override
	public byte[] getSecretSalt(String clientId) {
		
		String secretHash;
		
		try {
			secretHash = jdbcTemplate.queryForObject(CLIENT_SECRET_HASH_SQL_SELECT, String.class, clientId);
		} catch (EmptyResultDataAccessException e) {
			throw clientNotFoundException(clientId);
		}
		
		if (secretHash == null) {
			throw new NotFoundException("OAuth client (" + clientId + ") does not have a secret defined");
		}
		
		return PBKDF2Utils.extractSalt(secretHash);

	}

	@WriteTransaction
	@Override
	public OAuthClient updateOAuthClient(OAuthClient updatedClient) {	
		ValidateArgument.required(updatedClient, "OAuth client");
		ValidateArgument.requiredNotEmpty(updatedClient.getClient_id(), "Client ID");
		ValidateArgument.required(updatedClient.getEtag(), "etag");

		DBOOAuthClient dbo = clientDtoToDbo(updatedClient);
		String secretHash = jdbcTemplate.queryForObject(CLIENT_SECRET_HASH_SQL_SELECT, String.class, updatedClient.getClient_id());
		dbo.setSecretHash(secretHash);
		basicDao.update(dbo);
		return updatedClient;
	}
	
	@WriteTransaction
	@Override
	public void deleteOAuthClient(String clientId) {
		SqlParameterSource param = new SinglePrimaryKeySqlParameterSource(clientId);
		basicDao.deleteObjectByPrimaryKey(DBOOAuthClient.class, param);
	}

	@Override
	public String createSectorIdentifier(SectorIdentifier dto) {
		DBOSectorIdentifier dbo = new DBOSectorIdentifier();
		dbo.setSecret(dto.getSecret());
		dbo.setUri(dto.getSectorIdentifierUri());
		dbo.setCreatedBy(dto.getCreatedBy());
		dbo.setCreatedOn(dto.getCreatedOn());
		Long id = idGenerator.generateNewId(IdType.OAUTH_SECTOR_IDENTIFIER_ID);
		dbo.setId(id);
		basicDao.createNew(dbo);
		return id.toString();
	}

	@Override
	public String getSectorIdentifierSecretForClient(String clientId) {
		try {
			return jdbcTemplate.queryForObject(SECTOR_IDENTIFIER_SELECT_FOR_CLIENT_SQL, String.class, clientId);
		} catch (EmptyResultDataAccessException | IncorrectResultSetColumnCountException e) {
			throw new NotFoundException("Could not find Sector Identifier for "+clientId, e);
		}
	}

	@Override
	public boolean doesSectorIdentifierExistForURI(String sectorIdentifierUri) {
		long count = jdbcTemplate.queryForObject(SECTOR_IDENTIFIER_SQL_SELECT, Long.class, sectorIdentifierUri);
		return count>0;
	}
	
	@Override
	public void setOAuthClientSecretHash(String clientId, String secretHash, String newEtag) {
		jdbcTemplate.update(SET_CLIENT_SECRET_HASH, secretHash, newEtag, clientId);	
	}
	
	@Override
	public boolean isOauthClientVerified(String clientId) {
		ValidateArgument.required(clientId, "Client ID");
		try {
			return jdbcTemplate.queryForObject(CLIENT_VERIFIED_SQL_SELECT, Boolean.class, clientId);
		} catch (EmptyResultDataAccessException e) {
			throw clientNotFoundException(clientId);
		}
	}

	@Override
	public OAuthClientAuthorizationHistoryList getAuthorizedClientHistory(String userId, String nextPageToken, Long maxLeaseLengthInDays) {
		NextPageToken nextPage = new NextPageToken(nextPageToken);

		List<OAuthClientAuthorizationHistory> authorizations = jdbcTemplate.query(
				SELECT_CLIENTS_WITH_ACTIVE_TOKENS_FOR_PRINCIPAL,
				(ResultSet rs, int rowNum) -> {
					OAuthClientAuthorizationHistory authorization = new OAuthClientAuthorizationHistory();
					DBOOAuthClient dbo = new DBOOAuthClient();
					dbo = dbo.getTableMapping().mapRow(rs, rowNum);
					authorization.setClient(clientDboToDto(dbo));
					authorization.setLastUsed(rs.getTimestamp(INTERMEDIATE_COL_OVERALL_LAST_USED));
					authorization.setAuthorizedOn(rs.getTimestamp(INTERMEDIATE_COL_FIRST_AUTHORIZED_ON));
					return authorization;
				},
				userId, maxLeaseLengthInDays, nextPage.getLimitForQuery(), nextPage.getOffset());
		OAuthClientAuthorizationHistoryList result = new OAuthClientAuthorizationHistoryList();
		result.setNextPageToken(nextPage.getNextPageTokenForCurrentResults(authorizations));
		result.setResults(authorizations);
		return result;
	}

	private NotFoundException clientNotFoundException(String clientId) {
		return new NotFoundException("The OAuth client (" + clientId + ") does not exist");
	}
	
}
