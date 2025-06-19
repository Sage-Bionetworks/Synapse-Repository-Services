package org.sagebionetworks.repo.model.dbo.portals;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PORTAL_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PORTAL_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PORTAL_ENDPOINT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PORTAL_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PORTAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PORTAL_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PORTAL_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PORTAL_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PORTAL;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.portals.Portal;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class PortalDaoImpl implements PortalDao {
	
	static Long mapPortalId(String portalId) {
		try {
			return Long.valueOf(portalId);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid portal id.");
		}
	}
	
	private static final String MSG_DUPLICATE_PORTAL = "A portal with the given name and/or URL already exists.";

	private static final RowMapper<Portal> PORTAL_ROW_MAPPER = (rs, rowNum) -> new Portal()
		.setId(String.valueOf(rs.getLong(COL_PORTAL_ID)))
		.setEtag(rs.getString(COL_PORTAL_ETAG))
		.setCreatedBy(rs.getString(COL_PORTAL_CREATED_BY))
		.setCreatedOn(new Date(rs.getTimestamp(COL_PORTAL_CREATED_ON).getTime()))
		.setModifiedBy(rs.getString(COL_PORTAL_MODIFIED_BY))
		.setModifiedOn(new Date(rs.getTimestamp(COL_PORTAL_MODIFIED_ON).getTime()))
		.setName(rs.getString(COL_PORTAL_NAME))
		.setUrl(rs.getString(COL_PORTAL_ENDPOINT));

	private JdbcTemplate jdbcTemplate;

	private IdGenerator idGenerator;

	private DBOBasicDao basicDao;

	public PortalDaoImpl(JdbcTemplate jdbcTemplate, IdGenerator idGenerator, DBOBasicDao basicDao, TransactionTemplate readCommittedRequiresNew) {
		this.jdbcTemplate = jdbcTemplate;
		this.idGenerator = idGenerator;
		this.basicDao = basicDao;
	}
	
	@Override
	@WriteTransaction
	public void bootstrap() {
		if (getPortal(DBOPortal.SYNAPSE_PORTAL_ID.toString()).isEmpty()) {
			Long adminId = AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
 			Timestamp now = Timestamp.from(Instant.now());
 			
 			DBOPortal portalDbo = new DBOPortal()
				.setId(DBOPortal.SYNAPSE_PORTAL_ID)
				.setCreatedBy(adminId)
				.setCreatedOn(now)
				.setModifiedBy(adminId)
				.setModifiedOn(now)
				.setEtag(UUID.randomUUID().toString())
				.setName("Synapse")
				.setEndpoint("https://synapse.org");
			
			basicDao.createNew(portalDbo);
		}
	}

	@Override
	@WriteTransaction
	public Portal createPortal(Long userId, String name, String endpoint) {
		Instant now = Instant.now();
		
		DBOPortal portalDbo = new DBOPortal()
			.setId(idGenerator.generateNewId(IdType.PORTAL_ID))
			.setCreatedBy(userId)
			.setCreatedOn(Timestamp.from(now))
			.setModifiedBy(userId)
			.setModifiedOn(Timestamp.from(now))
			.setEtag(UUID.randomUUID().toString())
			.setName(name)
			.setEndpoint(endpoint);
		
		try {
			basicDao.createNew(portalDbo);
		} catch (IllegalArgumentException e) {
			// DBOBasicDao.createNew wraps any DataIntegrityViolationException into an IllegalArgumentException
			if (e.getCause() instanceof DuplicateKeyException) {
				throw new IllegalArgumentException(MSG_DUPLICATE_PORTAL);
			}
			throw e;
		}
		
		return getPortal(portalDbo.getId().toString()).orElseThrow(() -> new IllegalStateException("The portal was not created."));
	}

	@Override
	@WriteTransaction
	public Portal updatePortal(Long userId, String portalId, String name, String endpoint) {
		String sql = "UPDATE " + TABLE_PORTAL + " SET " 
			+ COL_PORTAL_ETAG + "=UUID(),"
			+ COL_PORTAL_MODIFIED_BY + "=?,"
			+ COL_PORTAL_MODIFIED_ON + "=NOW(),"
			+ COL_PORTAL_NAME + "=?,"
			+ COL_PORTAL_ENDPOINT + "=?"
			+ " WHERE " + COL_PORTAL_ID + "=?";

		try {
			jdbcTemplate.update(sql, userId, name, endpoint, mapPortalId(portalId));
		} catch (DuplicateKeyException e) {
			throw new IllegalArgumentException(MSG_DUPLICATE_PORTAL);
		}
		
		return getPortal(portalId).orElseThrow(() -> new IllegalStateException("The portal was not updated."));
	}
	
	@Override
	@WriteTransaction
	public void deletePortal(String portalId) {
		basicDao.deleteObjectByPrimaryKey(DBOPortal.class, new SinglePrimaryKeySqlParameterSource(mapPortalId(portalId)));
	}

	@Override
	public Optional<Portal> getPortal(String portalId) {
		String sql = "SELECT * FROM " + TABLE_PORTAL + " WHERE " + COL_PORTAL_ID+ " =?";
		
		try {
			return Optional.of(jdbcTemplate.queryForObject(sql, PORTAL_ROW_MAPPER, mapPortalId(portalId)));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@Override
	public List<Portal> getPortalPage(long limit, long offset) {
		String sql = " SELECT * FROM " + TABLE_PORTAL + " ORDER BY " + COL_PORTAL_ID + " LIMIT ? OFFSET ?";
		
		return jdbcTemplate.query(sql, PORTAL_ROW_MAPPER, limit, offset);
	}
	
	@Override
	@WriteTransaction
	public void truncateAll() {
		jdbcTemplate.update("DELETE FROM " + TABLE_PORTAL);
	}

}
