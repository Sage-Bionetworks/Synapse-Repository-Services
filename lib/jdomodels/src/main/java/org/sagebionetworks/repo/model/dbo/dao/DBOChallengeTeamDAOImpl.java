package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHALLENGE_TEAM_CHALLENGE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHALLENGE_TEAM_TEAM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GROUP_MEMBERS_MEMBER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TEAM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_CHALLENGE_TEAM;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.ChallengeTeam;
import org.sagebionetworks.repo.model.ChallengeTeamDAO;
import org.sagebionetworks.repo.model.ChallengeTeamSummary;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.persistence.DBOChallengeTeam;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTeam;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class DBOChallengeTeamDAOImpl implements ChallengeTeamDAO {
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private DBOBasicDao basicDao;
	
	@Autowired
	private IdGenerator idGenerator;

	private static TableMapping<DBOChallengeTeam> DBO_CHALLENGE_TEAM_TABLE_MAPPING =
			(new DBOChallengeTeam()).getTableMapping();
	
	private static RowMapper<ChallengeTeamSummary> CHALLENGE_TEAM_SUMMARY_MAPPING = new RowMapper<ChallengeTeamSummary>() {
		@Override
		public ChallengeTeamSummary mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			ChallengeTeam dto = copyDBOtoDTO(DBO_CHALLENGE_TEAM_TABLE_MAPPING.mapRow(rs, rowNum));
			ChallengeTeamSummary result = new ChallengeTeamSummary();
			result.setId(dto.getId());
			result.setChallengeId(dto.getChallengeId());			
			result.setMessage(dto.getMessage());
			result.setTeamId(dto.getTeamId());
			result.setUserId(rs.getString(SPECIFIED_USER));
			result.setUserIsAdmin(null!=rs.getString(COL_GROUP_MEMBERS_MEMBER_ID));
			return result;
		}};
	
	private static final String SELECT_FOR_CHALLENGE_SQL_CORE = 
			" FROM "+TABLE_CHALLENGE_TEAM+" ct, LEFT JOIN ("+TeamUtils.SELECT_ALL_TEAMS_AND_ADMIN_MEMBERS_CORE+
			" WHERE gm."+COL_GROUP_MEMBERS_MEMBER_ID+"=?) ON ct."+COL_CHALLENGE_TEAM_TEAM_ID+"=t."+COL_TEAM_ID+
			" WHERE "+COL_CHALLENGE_TEAM_CHALLENGE_ID+"=?";

	private static final String SPECIFIED_USER = "SPECIFIED_USER";
	
	private static final String LIMIT_OFFSET = " LIMIT ? OFFSET ?";
	
	private static final String SELECT_FOR_CHALLENGE_PAGINATED = 
			"SELECT gm."+COL_GROUP_MEMBERS_MEMBER_ID+" as "+SPECIFIED_USER+", ct.*, gm. "+COL_GROUP_MEMBERS_MEMBER_ID+
			SELECT_FOR_CHALLENGE_SQL_CORE+LIMIT_OFFSET;
	
	private static final String SELECT_FOR_CHALLENGE_COUNT = 
			"SELECT count(*) "+" FROM "+TABLE_CHALLENGE_TEAM+
			" WHERE "+COL_CHALLENGE_TEAM_CHALLENGE_ID+"=?";
	
	private static final String SELECT_REGISTRATABLE_TEAMS_CORE = 
			TeamUtils.SELECT_ALL_TEAMS_AND_ADMIN_MEMBERS_CORE+
			" AND "+ "gm."+COL_GROUP_MEMBERS_MEMBER_ID+"=?"+
			" AND t."+COL_TEAM_ID+" NOT IN (SELECT "+COL_CHALLENGE_TEAM_TEAM_ID+" FROM "+TABLE_CHALLENGE_TEAM+
			" WHERE "+COL_CHALLENGE_TEAM_CHALLENGE_ID+"=?)";
	
	private static final String SELECT_REGISTRATABLE_TEAMS_PAGINATED = 
			"SELECT t.* FROM "+SELECT_REGISTRATABLE_TEAMS_CORE+LIMIT_OFFSET;
	
	private static final String SELECT_REGISTRATABLE_TEAMS_COUNT = 
			"SELECT count(*) FROM "+SELECT_REGISTRATABLE_TEAMS_CORE;
	

	private static final RowMapper<DBOTeam> TEAM_ROW_MAPPER = (new DBOTeam()).getTableMapping();
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public ChallengeTeam create(ChallengeTeam dto) throws DatastoreException {
		validateChallengeTeam(dto);
		DBOChallengeTeam dbo = copyDTOtoDBO(dto);
		dbo.setId(idGenerator.generateNewId(TYPE.DOMAIN_IDS));
		dbo.setEtag(UUID.randomUUID().toString());
		DBOChallengeTeam created = basicDao.createNew(dbo);
		return copyDBOtoDTO(created);
	}

	public static void validateChallengeTeam(ChallengeTeam dto) {
		if (dto.getChallengeId()==null) throw new InvalidModelException("Challenge ID is required.");
		if (dto.getTeamId()==null) throw new InvalidModelException("Team ID is required.");
	}

	public static DBOChallengeTeam copyDTOtoDBO(ChallengeTeam dto) {
		DBOChallengeTeam dbo = new DBOChallengeTeam();
		copyDTOtoDBO(dto, dbo);
		return dbo;
	}
		
	public static void copyDTOtoDBO(ChallengeTeam dto, DBOChallengeTeam dbo) {
		dbo.setId(dto.getId()==null?null:Long.parseLong(dto.getId()));
		dbo.setChallengeId(Long.parseLong(dto.getChallengeId()));
		dbo.setEtag(dto.getEtag());
		dbo.setTeamId(Long.parseLong(dto.getTeamId()));
		try {
			dbo.setSerializedEntity(JDOSecondaryPropertyUtils.compressObject(dto));
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}
	
	public static ChallengeTeam copyDBOtoDTO(DBOChallengeTeam dbo) {
		ChallengeTeam dto;
		try {
			dto = (ChallengeTeam)JDOSecondaryPropertyUtils.decompressedObject(dbo.getSerializedEntity());
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
		dto.setId(dbo.getId().toString());
		dto.setChallengeId(dbo.getChallengeId().toString());
		dto.setEtag(dbo.getEtag());
		dto.setTeamId(dbo.getTeamId().toString());
		return dto;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public ChallengeTeam update(ChallengeTeam dto) throws DatastoreException, InvalidModelException, NotFoundException,
			ConflictingUpdateException {
		if (dto.getId()==null) throw new InvalidModelException("ID is required.");
		validateChallengeTeam(dto);
		DBOChallengeTeam dbo;
		try {
			dbo = basicDao.getObjectByPrimaryKeyWithUpdateLock(DBOChallengeTeam.class, 
					new SinglePrimaryKeySqlParameterSource(dto.getId()));
			
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("The resource you are attempting to access cannot be found.");
		}
		if (!dbo.getChallengeId().equals(Long.parseLong(dto.getChallengeId()))) {
			throw new IllegalArgumentException(
					"You cannot change the challenge ID.");
		}
		if (!dbo.getTeamId().equals(Long.parseLong(dto.getTeamId()))) {
			throw new IllegalArgumentException(
					"You cannot change the Team ID.");
		}
		// Check dbo's etag against dto's etag
		// if different rollback and throw a meaningful exception
		if (!dbo.getEtag().equals(dto.getEtag())) {
			throw new ConflictingUpdateException(
					"ChallengeTeam was updated since you last fetched it. Retrieve it again and reapply the update.");
		}
		copyDTOtoDBO(dto, dbo);
		// Update with a new e-tag
		dbo.setEtag(UUID.randomUUID().toString());

		boolean success = basicDao.update(dbo);
		if (!success)
			throw new DatastoreException("Unsuccessful updating ChallengeTeam in database.");

		dbo = basicDao.getObjectByPrimaryKey(DBOChallengeTeam.class, new SinglePrimaryKeySqlParameterSource(dto.getId()));
		return copyDBOtoDTO(dbo);
	}
	
	@Override
	public List<ChallengeTeamSummary> listForChallenge(String userId, String challengeId, long limit,
			long offset) throws NotFoundException, DatastoreException {
		if (limit<=0) throw new IllegalArgumentException("'limit' param must be greater than zero.");
		return jdbcTemplate.query(SELECT_FOR_CHALLENGE_PAGINATED, CHALLENGE_TEAM_SUMMARY_MAPPING, userId, challengeId, limit, offset);
	}

	@Override
	public long listForChallengeCount(String challengeId)
			throws NotFoundException, DatastoreException {
		return jdbcTemplate.queryForObject(SELECT_FOR_CHALLENGE_COUNT, Long.class, challengeId);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void delete(long id) throws DatastoreException {
		basicDao.deleteObjectByPrimaryKey(DBOChallengeTeam.class, new SinglePrimaryKeySqlParameterSource(id));
	}
	
	/*
	 * Returns the Teams which are NOT registered for the challenge and on which is current user is an ADMIN.
	 */
	@Override
	public List<Team> listRegistratable(String challengeId, String userId,
			long limit, long offset) throws NotFoundException,
			DatastoreException {
		List<DBOTeam> dbos = jdbcTemplate.query(SELECT_REGISTRATABLE_TEAMS_PAGINATED, 
				TEAM_ROW_MAPPER, userId, challengeId, limit, offset);
		List<Team> result = new ArrayList<Team>();
		for (DBOTeam dbo : dbos) result.add(TeamUtils.copyDboToDto(dbo));
		return result;
	}

	@Override
	public long listRegistratableCount(String challengeId, String userId)
			throws NotFoundException, DatastoreException {
		return jdbcTemplate.queryForObject(SELECT_REGISTRATABLE_TEAMS_COUNT, Long.class, userId, challengeId);
	}
}
