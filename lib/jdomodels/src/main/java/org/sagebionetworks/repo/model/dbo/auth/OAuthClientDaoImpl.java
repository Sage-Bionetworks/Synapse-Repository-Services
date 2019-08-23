package org.sagebionetworks.repo.model.dbo.auth;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_CLIENT_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_CLIENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_CLIENT_SECRET;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_SECTOR_IDENTIFIER_SECRET;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_SECTOR_IDENTIFIER_URI;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_OAUTH_CLIENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_OAUTH_SECTOR_IDENTIFIER;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
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
import org.sagebionetworks.repo.model.oauth.OAuthClientList;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.IncorrectResultSetColumnCountException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

public class OAuthClientDaoImpl implements OAuthClientDao {

	private static final String SECTOR_IDENTIFIER_SQL_SELECT = "SELECT COUNT(*) FROM "+TABLE_OAUTH_SECTOR_IDENTIFIER			
			+" WHERE "+COL_OAUTH_SECTOR_IDENTIFIER_URI+" = ?";
	;

	private static final String CLIENT_SQL_SELECT = "SELECT * FROM "+TABLE_OAUTH_CLIENT+" WHERE "+
			COL_OAUTH_CLIENT_CREATED_BY+" = ? LIMIT ? OFFSET ?";

	private static final String CLIENT_SECRET_SQL_SELECT = "SELECT "+COL_OAUTH_CLIENT_SECRET+
			" FROM "+TABLE_OAUTH_CLIENT
			+ " WHERE "+COL_OAUTH_CLIENT_ID+" = ?";

	private static final String SECTOR_IDENTIFIER_SELECT_FOR_CLIENT_SQL = "SELECT s."+COL_OAUTH_SECTOR_IDENTIFIER_SECRET+
			" FROM "+TABLE_OAUTH_CLIENT+" c, "+TABLE_OAUTH_SECTOR_IDENTIFIER+" s "
			+" WHERE "+
			"c."+COL_OAUTH_SECTOR_IDENTIFIER_URI+" = s."+COL_OAUTH_SECTOR_IDENTIFIER_URI+
			" AND c."+COL_OAUTH_CLIENT_ID+" = ?";

	private static final String SECTOR_IDENTIFIER_SQL_DELETE = "DELETE FROM "+TABLE_OAUTH_SECTOR_IDENTIFIER
			+" WHERE "+COL_OAUTH_SECTOR_IDENTIFIER_URI+" = ?";

	@Autowired
	private DBOBasicDao basicDao;	

	@Autowired
	private IdGenerator idGenerator;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	public static OAuthClient clientDboToDto(DBOOAuthClient dbo) {
		OAuthClient dto;
		try {
			dto = (OAuthClient)JDOSecondaryPropertyUtils.decompressObject(X_STREAM, dbo.getProperties());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return dto;
	}

	private static final UnmodifiableXStream X_STREAM = UnmodifiableXStream.builder().allowTypes(OAuthClient.class).build();

	public static DBOOAuthClient clientDtoToDbo(OAuthClient dto) {
		DBOOAuthClient dbo = new DBOOAuthClient();
		try {
			dbo.setProperties(JDOSecondaryPropertyUtils.compressObject(X_STREAM, dto));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		if (dto.getClientId()!=null) dbo.setId(Long.parseLong(dto.getClientId()));
		dbo.setCreatedBy(Long.parseLong(dto.getCreatedBy()));
		dbo.setCreatedOn(dto.getCreatedOn().getTime());
		dbo.seteTag(dto.getEtag());
		dbo.setModifiedOn(dto.getModifiedOn().getTime());
		dbo.setSectorIdentifierUri(dto.getSector_identifier());
		dbo.setName(dto.getClient_name());
		return dbo;
	}

	@WriteTransaction
	@Override
	public void deleteSectorIdentifer(String sectorIdentiferUri) {
		jdbcTemplate.update(SECTOR_IDENTIFIER_SQL_DELETE, sectorIdentiferUri);
	}

	@WriteTransaction
	@Override
	public String createOAuthClient(OAuthClient client, String secret) {
		ValidateArgument.required(client, "OAuth client");
		ValidateArgument.required(client.getCreatedBy(), "Created By");
		ValidateArgument.required(client.getSector_identifier(), "Sector Identifier");
		ValidateArgument.required(client.getValidated(), "Is Validated");
		
		Date now = new Date(System.currentTimeMillis());
		client.setCreatedOn(now);
		client.setModifiedOn(now);
		client.setEtag(UUID.randomUUID().toString());
		String id = idGenerator.generateNewId(IdType.OAUTH_CLIENT_ID).toString();
		client.setClientId(id);
		DBOOAuthClient dbo = clientDtoToDbo(client);
		dbo.setSecret(secret);
		try {
			basicDao.createNew(dbo);
		} catch (IllegalArgumentException e) {
			if (e.getCause() instanceof DuplicateKeyException) {
				throw new IllegalArgumentException("OAuth client already exists with name "+dbo.getName(), e);
			}
			throw e;
		}
		return id;
	}

	@Override
	public OAuthClient getOAuthClient(String clientId) {
		ValidateArgument.required(clientId, "Client ID");
		SqlParameterSource param = new SinglePrimaryKeySqlParameterSource(clientId);
		DBOOAuthClient dbo = basicDao.getObjectByPrimaryKey(DBOOAuthClient.class, param);
		return clientDboToDto(dbo);
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
	public String getOAuthClientSecret(String clientId) {
		ValidateArgument.required(clientId, "Client ID");
		try {
			return jdbcTemplate.queryForObject(CLIENT_SECRET_SQL_SELECT, String.class, clientId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException(clientId+" was not found", e);
		}
	}

	@WriteTransaction
	@Override
	public OAuthClient updateOAuthClient(OAuthClient updatedClient) {	
		ValidateArgument.required(updatedClient, "OAuth client");
		ValidateArgument.requiredNotEmpty(updatedClient.getClientId(), "Client ID");

		SqlParameterSource param = new SinglePrimaryKeySqlParameterSource(updatedClient.getClientId());
		DBOOAuthClient origDbo = basicDao.getObjectByPrimaryKeyWithUpdateLock(DBOOAuthClient.class, param);
		OAuthClient toStore = clientDboToDto(origDbo);

		// now update 'toStore' with info from updatedClient
		// we *never* change: clientID, clientSecret, createdBy, createdOn
		toStore.setClient_name(updatedClient.getClient_name());
		toStore.setClient_uri(updatedClient.getClient_uri());
		if (!toStore.getEtag().equals(updatedClient.getEtag())) {
			throw new ConflictingUpdateException(
					"OAuth Client was updated since you last fetched it.  Retrieve it again and reapply the update.");
		}
		toStore.setEtag(UUID.randomUUID().toString());
		toStore.setModifiedOn(new Date());
		toStore.setPolicy_uri(updatedClient.getPolicy_uri());
		toStore.setRedirect_uris(updatedClient.getRedirect_uris()); // the caller must have validated this info
		toStore.setSector_identifier(updatedClient.getSector_identifier());
		toStore.setSector_identifier_uri(updatedClient.getSector_identifier_uri());
		toStore.setTos_uri(updatedClient.getTos_uri());
		toStore.setUserinfo_signed_response_alg(updatedClient.getUserinfo_signed_response_alg());
		// validated can only become true if explicitly set by the caller
		if (updatedClient.getValidated()!=null) toStore.setValidated(updatedClient.getValidated());
		DBOOAuthClient dbo = clientDtoToDbo(toStore);
		dbo.setSecret(origDbo.getSecret());
		basicDao.update(dbo);
		return toStore;
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

}
