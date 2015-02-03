package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.jdo.AuthorizationSqlUtil.AUTHORIZATION_SQL_JOIN;
import static org.sagebionetworks.repo.model.jdo.AuthorizationSqlUtil.AUTHORIZATION_SQL_TABLES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACL_OWNER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACL_OWNER_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BOUND_ALIAS_DISPLAY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHALLENGE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHALLENGE_PARTICIPANT_TEAM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHALLENGE_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHALLENGE_TEAM_CHALLENGE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHALLENGE_TEAM_TEAM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GROUP_MEMBERS_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GROUP_MEMBERS_MEMBER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_TYPE_ELEMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_PROFILE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_PROFILE_PROPS_BLOB;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_CHALLENGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_CHALLENGE_TEAM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_GROUP_MEMBERS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NODE;

import java.io.IOException;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.Challenge;
import org.sagebionetworks.repo.model.ChallengeDAO;
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
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
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
	
	private static final String SELECT_FOR_PROJECT = "SELECT * FROM "+TABLE_CHALLENGE+
			" WHERE "+COL_CHALLENGE_PROJECT_ID+"=?";
	
	private static final String SELECT_FOR_PARTICIPANT_SQL_FROM_CORE = 
			" FROM "+TABLE_NODE+" n, "+
					TABLE_CHALLENGE+" c, "+
					TABLE_GROUP_MEMBERS+" gm ";
	
	private static final String SELECT_FOR_PARTICIPANT_SQL_WHERE_CORE = 
			" WHERE n."+COL_NODE_PROJECT_ID+"=c."+COL_CHALLENGE_PROJECT_ID+
			" AND c."+COL_CHALLENGE_PARTICIPANT_TEAM_ID+"=gm."+COL_GROUP_MEMBERS_GROUP_ID+
			" AND gm."+COL_GROUP_MEMBERS_MEMBER_ID+"=?";
	
	private static final String SELECT_FOR_PARTICIPANT_SQL_CORE = 
			SELECT_FOR_PARTICIPANT_SQL_FROM_CORE+
			SELECT_FOR_PARTICIPANT_SQL_WHERE_CORE;

	private static final String LIMIT_OFFSET = " LIMIT ? OFFSET ? ";
	
	private static final String NODE_ID_LABEL = "NODE_ID";
	
	private static final String SELECT_FOR_PARTICIPANT_PAGINATED = 
			"SELECT c.* "+
			SELECT_FOR_PARTICIPANT_SQL_CORE+LIMIT_OFFSET;
	
	private static final String SELECT_FOR_PARTICIPANT_COUNT = 
			"SELECT count(c."+COL_CHALLENGE_ID+") "+SELECT_FOR_PARTICIPANT_SQL_CORE;

	private static final String SELECT_FOR_PARTICIPANT_AND_REQUESTER_SQL_CORE = 
			SELECT_FOR_PARTICIPANT_SQL_FROM_CORE+","+
			AUTHORIZATION_SQL_TABLES+
			SELECT_FOR_PARTICIPANT_SQL_WHERE_CORE+
			" and acl."+COL_ACL_OWNER_ID+"=n."+COL_NODE_ID+
			" and acl."+COL_ACL_OWNER_TYPE+"='ENTITY'"+
			" and "+AUTHORIZATION_SQL_JOIN+
			" and at."+COL_RESOURCE_ACCESS_TYPE_ELEMENT+"='READ'"+
			" and ra."+COL_RESOURCE_ACCESS_GROUP_ID+" IN (";

	/*
	 * Adds 'requesterGroupCount' number of bind variables, for a total of requesterGroupCount+3
	 */
	private static String selectForParticipantAndRequesterPaginated(int requesterGroupCount) {
		StringBuilder sb = new StringBuilder("SELECT c.* "+
				SELECT_FOR_PARTICIPANT_AND_REQUESTER_SQL_CORE);
		boolean firstTime = true;
		for (int i=0; i<requesterGroupCount; i++) {
			if (firstTime) firstTime=false; else sb.append(",");
			sb.append("?");
		}
		sb.append(")"+LIMIT_OFFSET);
		return sb.toString();
	}
	
	private static String selectSummariesForParticipantAndRequesterCount(int requesterGroupCount) {
		StringBuilder sb = new StringBuilder(
				"SELECT count(c."+COL_CHALLENGE_ID+") "+
				SELECT_FOR_PARTICIPANT_AND_REQUESTER_SQL_CORE);
		boolean firstTime = true;
		for (int i=0; i<requesterGroupCount; i++) {
			if (firstTime) firstTime=false; else sb.append(",");
			sb.append("?");
		}
		sb.append(")");
		return sb.toString();
	}
	
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
			" AND gm1."+COL_GROUP_MEMBERS_MEMBER_ID+"=gm2."+COL_GROUP_MEMBERS_MEMBER_ID;

	private static final String SELECT_PARTICIPANTS_CORE =
			" FROM "+TABLE_CHALLENGE+" c, "+TABLE_GROUP_MEMBERS+" gm "+
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
			"SELECT gm."+COL_GROUP_MEMBERS_MEMBER_ID;
	
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
		dbo.setId(idGenerator.generateNewId(TYPE.CHALLENGE_ID));
		dbo.setEtag(UUID.randomUUID().toString());
		try {
			DBOChallenge created = basicDao.createNew(dbo);
			return copyDBOtoDTO(created);
		} catch (IllegalArgumentException e) {
			if (e.getCause() instanceof DuplicateKeyException) {
				throw new IllegalArgumentException("The specified project may already have a challenge.", e);
			} else if (e.getCause() instanceof DataIntegrityViolationException) {
				throw new InvalidModelException("The submitted data is invalid.  Please ensure the given project and team IDs are correct.");
			} else {
				throw e;
			}
		}
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
	public Challenge get(long challengeId) throws NotFoundException, DatastoreException {
		SqlParameterSource param = new SinglePrimaryKeySqlParameterSource(challengeId);
		DBOChallenge dbo = basicDao.getObjectByPrimaryKey(DBOChallenge.class, param);
		return copyDBOtoDTO(dbo);
	}

	@Override
	public List<Challenge> listForUser(long principalId, long limit,
			long offset) throws NotFoundException, DatastoreException {
		if (limit<=0) throw new IllegalArgumentException("'limit' param must be greater than zero.");
		List<DBOChallenge> dbos = jdbcTemplate.query(SELECT_FOR_PARTICIPANT_PAGINATED, DBO_CHALLENGE_TABLE_MAPPING, principalId, limit, offset);
		List<Challenge> dtos = new ArrayList<Challenge>();
		for (DBOChallenge dbo : dbos) dtos.add(copyDBOtoDTO(dbo));
		return dtos;
	}

	@Override
	public long listForUserCount(long principalId) throws NotFoundException,
			DatastoreException {
		return jdbcTemplate.queryForObject(SELECT_FOR_PARTICIPANT_COUNT, Long.class, principalId);
	}

	@Override
	public List<Challenge> listForUser(final long principalId, final Set<Long> requesterPrincipals,
			final long limit,final long offset) throws NotFoundException, DatastoreException {
		if (limit<=0) throw new IllegalArgumentException("'limit' param must be greater than zero.");
		int n = requesterPrincipals.size();
		if (n==0) return Collections.emptyList();
		Object[] args = new Object[n+3];
		args[0] = principalId;
		System.arraycopy(requesterPrincipals.toArray(), 0, args, 1, n);
		args[n+1]=limit;
		args[n+2]=offset;
		List<DBOChallenge> dbos = jdbcTemplate.query(
				selectForParticipantAndRequesterPaginated(n), 
				args, DBO_CHALLENGE_TABLE_MAPPING);
		List<Challenge> dtos = new ArrayList<Challenge>();
		for (DBOChallenge dbo : dbos) dtos.add(copyDBOtoDTO(dbo));
		return dtos;
	}

	@Override
	public long listForUserCount(long principalId, Set<Long> requesterPrincipals) throws NotFoundException,
			DatastoreException {
		int n = requesterPrincipals.size();
		if (n==0) return 0L;
		Object[] args = new Object[n+1];
		args[0] = principalId;
		System.arraycopy(requesterPrincipals.toArray(), 0, args, 1, n);
		return jdbcTemplate.queryForObject(selectSummariesForParticipantAndRequesterCount(n), args, Long.class);
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
		
		try {
			boolean success = basicDao.update(dbo);
			if (!success)
				throw new DatastoreException("Unsuccessful updating Challenge in database.");
		} catch (IllegalArgumentException e) {
			if (e.getCause() instanceof DataIntegrityViolationException) {
				throw new InvalidModelException("The submitted data is invalid.  Please ensure the given project and team IDs are correct.");
			} else {
				throw e;
			}
		}
			

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
	public List<Long> listParticipants(long challengeId,
			Boolean affiliated, long limit, long offset)
			throws NotFoundException, DatastoreException {
		if (limit<=0) throw new IllegalArgumentException("'limit' param must be greater than zero.");
		if (affiliated==null) {
			return jdbcTemplate.queryForList(SELECT_PARTICIPANTS, Long.class, challengeId, limit, offset);
		} else if (affiliated) {
			return jdbcTemplate.queryForList(SELECT_PARTICIPANTS_IN_REGISTERED_TEAM, Long.class, challengeId, challengeId, limit, offset);
		} else {
			return jdbcTemplate.queryForList(SELECT_PARTICIPANTS_NOT_IN_REGISTERED_TEAM, Long.class, challengeId, challengeId, limit, offset);
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
	public long listParticipantsCount(long challengeId, Boolean affiliated)
			throws NotFoundException, DatastoreException {
		if (affiliated==null) {
			return jdbcTemplate.queryForObject(SELECT_PARTICIPANTS_COUNT, Long.class, challengeId);
		} else if (affiliated) {
			return jdbcTemplate.queryForObject(SELECT_PARTICIPANTS_IN_REGISTERED_TEAM_COUNT, Long.class, challengeId, challengeId);
		} else {
			return jdbcTemplate.queryForObject(SELECT_PARTICIPANTS_NOT_IN_REGISTERED_TEAM_COUNT, Long.class, challengeId, challengeId);
		}
	}

}
