package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BOUND_ALIAS_DISPLAY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHALLENGE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHALLENGE_PARTICIPANT_TEAM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHALLENGE_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHALLENGE_TEAM_CHALLENGE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHALLENGE_TEAM_TEAM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GROUP_MEMBERS_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GROUP_MEMBERS_MEMBER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_PROFILE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_PROFILE_PROPS_BLOB;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_CHALLENGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_CHALLENGE_TEAM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_GROUP_MEMBERS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PRINCIPAL_ALIAS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_PROFILE;

import java.io.IOException;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.Challenge;
import org.sagebionetworks.repo.model.ChallengeDAO;
import org.sagebionetworks.repo.model.ChallengeSummary;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.persistence.DBOChallenge;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.principal.AliasEnum;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class DBOChallengeDAOImpl implements ChallengeDAO {

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private DBOBasicDao basicDao;
	
	@Autowired
	private IdGenerator idGenerator;

	private static TableMapping<DBOChallenge> DBO_CHALLENGE_TABLE_MAPPING =
			(new DBOChallenge()).getTableMapping();
	
	private static RowMapper<ChallengeSummary> CHALLENGE_SUMMARY_MAPPING = new RowMapper<ChallengeSummary>() {
		@Override
		public ChallengeSummary mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			DBOChallenge dbo = DBO_CHALLENGE_TABLE_MAPPING.mapRow(rs, rowNum);
			ChallengeSummary result = new ChallengeSummary();
			result.setChallengeId(dbo.getId().toString());
			result.setName(rs.getString(COL_NODE_NAME));
			result.setProjectId(KeyFactory.keyToString(rs.getLong(NODE_ID_LABEL)));
			result.setParticipantTeamId(dbo.getParticipantTeamId().toString());
			return result;
		}};
	
	private static final String SELECT_FOR_PROJECT = "SELECT * FROM "+TABLE_CHALLENGE+
			" WHERE "+COL_CHALLENGE_PROJECT_ID+"=?";
	
	private static final String SELECT_SUMMARIES_FOR_USER_SQL_CORE = 
			" FROM "+TABLE_NODE+" n, "+
					TABLE_CHALLENGE+" c, "+
					TABLE_GROUP_MEMBERS+" gm "+
			" WHERE n."+COL_NODE_PROJECT_ID+"=c."+COL_CHALLENGE_PROJECT_ID+
			" AND c."+COL_CHALLENGE_PARTICIPANT_TEAM_ID+"=gm."+COL_GROUP_MEMBERS_GROUP_ID+
			" AND gm."+COL_GROUP_MEMBERS_MEMBER_ID+"=?";
	
	private static final String LIMIT_OFFSET = " LIMIT ? OFFSET ? ";
	
	private static final String NODE_ID_LABEL = "NODE_ID";
	
	private static final String SELECT_SUMMARIES_FOR_USER_PAGINATED = 
			"SELECT n."+COL_NODE_NAME+", n."+COL_NODE_ID+" AS "+NODE_ID_LABEL+", c.* "+SELECT_SUMMARIES_FOR_USER_SQL_CORE+
			LIMIT_OFFSET;
	
	private static final String SELECT_SUMMARIES_FOR_USER_COUNT = 
			"SELECT count(c."+COL_CHALLENGE_ID+") "+SELECT_SUMMARIES_FOR_USER_SQL_CORE;

	private static final String SELECT_MEMBERS_IN_REGISTERED_TEAM = 
			"SELECT gm1."+COL_GROUP_MEMBERS_MEMBER_ID+
			" FROM "+
			TABLE_CHALLENGE+" c, "+
			TABLE_CHALLENGE_TEAM+" ct, "+
			TABLE_GROUP_MEMBERS+" gm1, "+
			TABLE_GROUP_MEMBERS+" gm2 "+
			" WHERE "+
			" c."+COL_CHALLENGE_ID+"=? "+
			" AND c."+COL_CHALLENGE_PARTICIPANT_TEAM_ID+"=gm1."+COL_GROUP_MEMBERS_GROUP_ID+
			" AND ct."+COL_CHALLENGE_TEAM_CHALLENGE_ID+"=c."+COL_CHALLENGE_ID+
			" AND ct."+COL_CHALLENGE_TEAM_TEAM_ID+"=gm2."+COL_GROUP_MEMBERS_GROUP_ID+
			" AND gm1."+COL_GROUP_MEMBERS_MEMBER_ID+"gm2."+COL_GROUP_MEMBERS_MEMBER_ID;

	private static final String SELECT_PARTICIPANTS_CORE =
			" FROM "+TABLE_CHALLENGE+" c, "+TABLE_GROUP_MEMBERS+" gm LEFT OUTER JOIN "+
			TABLE_USER_PROFILE+" up ON (gm."+COL_GROUP_MEMBERS_MEMBER_ID+"=up."+COL_USER_PROFILE_ID+") "+
			"LEFT OUTER JOIN "+TABLE_PRINCIPAL_ALIAS+" pa ON (gm."+
			COL_GROUP_MEMBERS_MEMBER_ID+"=pa."+COL_PRINCIPAL_ALIAS_PRINCIPAL_ID+" AND pa."+
					COL_PRINCIPAL_ALIAS_TYPE+"='"+AliasEnum.USER_NAME.name()+"'"+") "+
			" WHERE c."+COL_CHALLENGE_PARTICIPANT_TEAM_ID+"=gm."+COL_GROUP_MEMBERS_GROUP_ID+
			" AND c."+COL_CHALLENGE_ID+"=?";
	
	private static final String SELECT_PARTICIPANTS_NOT_IN_REGISTERED_TEAM_CORE =
			SELECT_PARTICIPANTS_CORE+
			" AND gm."+COL_GROUP_MEMBERS_MEMBER_ID+
			" NOT IN ("+SELECT_MEMBERS_IN_REGISTERED_TEAM+")";
	
	private static final String SELECT_PARTICIPANTS_IN_REGISTERED_TEAM_CORE =
			SELECT_PARTICIPANTS_CORE+
			" AND gm."+COL_GROUP_MEMBERS_MEMBER_ID+
			" IN ("+SELECT_MEMBERS_IN_REGISTERED_TEAM+")";
	
	private static final String SELECT_PARTICIPANTS_PREFIX = 
			"SELECT up."+COL_USER_PROFILE_PROPS_BLOB+
			", up."+COL_USER_PROFILE_ID+
			", pa."+COL_BOUND_ALIAS_DISPLAY;
	
	private static final String SELECT_PARTICIPANTS =
			SELECT_PARTICIPANTS_PREFIX+SELECT_PARTICIPANTS_CORE+LIMIT_OFFSET;
	
	private static final String SELECT_PARTICIPANTS_COUNT =
			"SELECT COUNT(*) "+SELECT_PARTICIPANTS_CORE;
	
	private static final String SELECT_PARTICIPANTS_NOT_IN_REGISTERED_TEAM =
			SELECT_PARTICIPANTS_PREFIX+SELECT_PARTICIPANTS_NOT_IN_REGISTERED_TEAM_CORE+LIMIT_OFFSET;
	
	private static final String SELECT_PARTICIPANTS_NOT_IN_REGISTERED_TEAM_COUNT =
			"SELECT COUNT(*) "+SELECT_PARTICIPANTS_NOT_IN_REGISTERED_TEAM_CORE;
	
	private static final String SELECT_PARTICIPANTS_IN_REGISTERED_TEAM =
			SELECT_PARTICIPANTS_PREFIX+SELECT_PARTICIPANTS_IN_REGISTERED_TEAM_CORE+LIMIT_OFFSET;
	
	private static final String SELECT_PARTICIPANTS_IN_REGISTERED_TEAM_COUNT =
			"SELECT COUNT(*) "+SELECT_PARTICIPANTS_IN_REGISTERED_TEAM_CORE;
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public Challenge create(Challenge dto) throws DatastoreException {
		validateChallenge(dto);
		DBOChallenge dbo = copyDTOtoDBO(dto);
		dbo.setId(idGenerator.generateNewId(TYPE.DOMAIN_IDS));
		dbo.setEtag(UUID.randomUUID().toString());
		DBOChallenge created = basicDao.createNew(dbo);
		return copyDBOtoDTO(created);
	}

	public static void validateChallenge(Challenge dto) {
		if (dto.getProjectId()==null) throw new InvalidModelException("Project ID is required.");
		if (dto.getParticipantTeamId()==null) throw new InvalidModelException("Participant Team ID is required.");
	}

	public static DBOChallenge copyDTOtoDBO(Challenge dto) {
		DBOChallenge dbo = new DBOChallenge();
		copyDTOtoDBO(dto, dbo);
		return dbo;
	}
		
	public static void copyDTOtoDBO(Challenge dto, DBOChallenge dbo) {
		dbo.setId(dto.getId()==null?null:Long.parseLong(dto.getId()));
		dbo.setProjectId(KeyFactory.stringToKey(dto.getProjectId()));
		dbo.setParticipantTeamId(Long.parseLong(dto.getParticipantTeamId()));
		dbo.setEtag(dto.getEtag());
		try {
			dbo.setSerializedEntity(JDOSecondaryPropertyUtils.compressObject(dto));
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}
	
	public static Challenge copyDBOtoDTO(DBOChallenge dbo) {
		Challenge dto;
		try {
			dto = (Challenge)JDOSecondaryPropertyUtils.decompressedObject(dbo.getSerializedEntity());
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
		dto.setId(dbo.getId().toString());
		dto.setProjectId(KeyFactory.keyToString(dbo.getProjectId()));
		dto.setEtag(dbo.getEtag());
		dto.setParticipantTeamId(dbo.getParticipantTeamId().toString());
		return dto;
	}

	@Override
	public Challenge getForProject(String projectId) throws NotFoundException, DatastoreException {
		try {
			DBOChallenge dbo = jdbcTemplate.queryForObject(SELECT_FOR_PROJECT, DBO_CHALLENGE_TABLE_MAPPING, 
				KeyFactory.stringToKey(projectId));
			return copyDBOtoDTO(dbo);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("Cannot find the Challenge for project "+projectId);
		}
	}

	@Override
	public List<ChallengeSummary> listForUser(String principalId, long limit,
			long offset) throws NotFoundException, DatastoreException {
		if (limit<=0) throw new IllegalArgumentException("'limit' param must be greater than zero.");
		List<ChallengeSummary> summaries = jdbcTemplate.query(SELECT_SUMMARIES_FOR_USER_PAGINATED, CHALLENGE_SUMMARY_MAPPING, principalId, limit, offset);
		return summaries;
	}

	@Override
	public long listForUserCount(String principalId) throws NotFoundException,
			DatastoreException {
		return jdbcTemplate.queryForObject(SELECT_SUMMARIES_FOR_USER_COUNT, Long.class, principalId);
	}

	@Override
	public List<ChallengeSummary> listForUser(String principalId, String userId, long limit,
			long offset) throws NotFoundException, DatastoreException {
		if (limit<=0) throw new IllegalArgumentException("'limit' param must be greater than zero.");
		throw new RuntimeException("NYI");	}

	@Override
	public long listForUserCount(String principalId, String userId) throws NotFoundException,
			DatastoreException {
		throw new RuntimeException("NYI");
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public Challenge update(Challenge dto) throws NotFoundException,
			DatastoreException {
		if (dto.getId()==null) throw new InvalidModelException("ID is required.");
		validateChallenge(dto);
		DBOChallenge dbo;
		try {
			dbo = basicDao.getObjectByPrimaryKeyWithUpdateLock(DBOChallenge.class, 
					new SinglePrimaryKeySqlParameterSource(dto.getId()));
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("The resource you are attempting to access cannot be found.");
		}
		if (!dbo.getProjectId().equals(KeyFactory.stringToKey(dto.getProjectId()))) {
			throw new IllegalArgumentException(
					"You cannot change the challenge Project ID.");
		}
		// Check dbo's etag against dto's etag
		// if different rollback and throw a meaningful exception
		if (!dbo.getEtag().equals(dto.getEtag())) {
			throw new ConflictingUpdateException(
					"Challenge was updated since you last fetched it.  Retrieve it again and reapply the update.");
		}
		copyDTOtoDBO(dto, dbo);
		// Update with a new e-tag
		dbo.setEtag(UUID.randomUUID().toString());

		boolean success = basicDao.update(dbo);
		if (!success)
			throw new DatastoreException("Unsuccessful updating Challenge in database.");

		dbo = basicDao.getObjectByPrimaryKey(DBOChallenge.class, new SinglePrimaryKeySqlParameterSource(dto.getId()));
		return copyDBOtoDTO(dbo);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void delete(long id) throws NotFoundException, DatastoreException {
		basicDao.deleteObjectByPrimaryKey(DBOChallenge.class, new SinglePrimaryKeySqlParameterSource(id));
	}

	/**
	 * Return challenge participants.  If affiliated=true, return just participants affiliated with 
	 * some registered Team.  If false, return those affiliated with no Team.  If missing return 
	 * all participants.
	 */
	@Override
	public List<UserGroupHeader> listParticipants(String challengeId,
			Boolean affiliated, Long limit, Long offset)
			throws NotFoundException, DatastoreException {
		if (limit<=0) throw new IllegalArgumentException("'limit' param must be greater than zero.");
		if (affiliated==null) {
			return jdbcTemplate.query(SELECT_PARTICIPANTS, CHALLENGE_PARTICIPANT_ROW_MAPPER, challengeId, limit, offset);
		} else if (affiliated) {
			return jdbcTemplate.query(SELECT_PARTICIPANTS_IN_REGISTERED_TEAM, CHALLENGE_PARTICIPANT_ROW_MAPPER, challengeId, limit, offset);
		} else {
			return jdbcTemplate.query(SELECT_PARTICIPANTS_NOT_IN_REGISTERED_TEAM, CHALLENGE_PARTICIPANT_ROW_MAPPER, challengeId, limit, offset);
		}
	}
	
	private static final RowMapper<UserGroupHeader> CHALLENGE_PARTICIPANT_ROW_MAPPER = new RowMapper<UserGroupHeader>(){
		@Override
		public UserGroupHeader mapRow(ResultSet rs, int rowNum) throws SQLException {
			String userName = rs.getString(COL_BOUND_ALIAS_DISPLAY);
			UserGroupHeader ugh = new UserGroupHeader();
			Blob upProperties = rs.getBlob(COL_USER_PROFILE_PROPS_BLOB);
			if (upProperties!=null) {
				ugh.setIsIndividual(true);
				ugh.setOwnerId(rs.getString(COL_USER_PROFILE_ID));
				UserProfileUtils.fillUserGroupHeaderFromUserProfileBlob(upProperties, userName, ugh);
			} else {
				ugh.setIsIndividual(false);
			}
			return ugh;
		}
	};
	
	@Override
	public long listParticipantsCount(String challengeId, Boolean affiliated)
			throws NotFoundException, DatastoreException {
		if (affiliated==null) {
			return jdbcTemplate.queryForObject(SELECT_PARTICIPANTS_COUNT, Long.class, challengeId);
		} else if (affiliated) {
			return jdbcTemplate.queryForObject(SELECT_PARTICIPANTS_IN_REGISTERED_TEAM_COUNT, Long.class, challengeId);
		} else {
			return jdbcTemplate.queryForObject(SELECT_PARTICIPANTS_NOT_IN_REGISTERED_TEAM_COUNT, Long.class, challengeId);
		}
	}

}
