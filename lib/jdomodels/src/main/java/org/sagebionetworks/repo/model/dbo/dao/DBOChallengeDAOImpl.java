package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.jdo.AuthorizationSqlUtil.AUTHORIZATION_SQL_JOIN;
import static org.sagebionetworks.repo.model.jdo.AuthorizationSqlUtil.AUTHORIZATION_SQL_TABLES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACL_OWNER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACL_OWNER_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHALLENGE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHALLENGE_PARTICIPANT_TEAM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHALLENGE_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHALLENGE_TEAM_CHALLENGE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHALLENGE_TEAM_TEAM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GROUP_MEMBERS_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GROUP_MEMBERS_MEMBER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_TYPE_ELEMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_CHALLENGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_CHALLENGE_TEAM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_GROUP_MEMBERS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.Challenge;
import org.sagebionetworks.repo.model.ChallengeDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
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
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.sagebionetworks.repo.transactions.WriteTransaction;

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
			" WHERE "+COL_CHALLENGE_PROJECT_ID+"=:"+COL_CHALLENGE_PROJECT_ID;
	
	private static final String SELECT_FOR_PARTICIPANT_SQL_FROM_CORE = 
			" FROM "+TABLE_CHALLENGE+" c, "+
					TABLE_GROUP_MEMBERS+" gm ";
	
	private static final String SELECT_FOR_PARTICIPANT_SQL_WHERE_CORE = 
			" WHERE c."+COL_CHALLENGE_PARTICIPANT_TEAM_ID+"=gm."+COL_GROUP_MEMBERS_GROUP_ID+
			" AND gm."+COL_GROUP_MEMBERS_MEMBER_ID+"=:"+COL_GROUP_MEMBERS_MEMBER_ID;
	
	private static final String SELECT_FOR_PARTICIPANT_SQL_CORE = 
			SELECT_FOR_PARTICIPANT_SQL_FROM_CORE+
			SELECT_FOR_PARTICIPANT_SQL_WHERE_CORE;

	private static final String LIMIT = "limitp";
	private static final String OFFSET = "offsetp";
	private static final String LIMIT_OFFSET = " LIMIT :"+LIMIT+" OFFSET :"+OFFSET;
	
	private static final String SELECT_FOR_PARTICIPANT_PAGINATED = 
			"SELECT DISTINCT c.* "+
			SELECT_FOR_PARTICIPANT_SQL_CORE+LIMIT_OFFSET;
	
	private static final String SELECT_FOR_PARTICIPANT_COUNT = 
			"SELECT count(DISTINCT c."+COL_CHALLENGE_ID+") "+SELECT_FOR_PARTICIPANT_SQL_CORE;

	private static final String SELECT_FOR_PARTICIPANT_AND_REQUESTER_SQL_CORE = 
			SELECT_FOR_PARTICIPANT_SQL_FROM_CORE+","+
			AUTHORIZATION_SQL_TABLES+
			SELECT_FOR_PARTICIPANT_SQL_WHERE_CORE+
			" and acl."+COL_ACL_OWNER_ID+"=c."+COL_CHALLENGE_PROJECT_ID+
			" and acl."+COL_ACL_OWNER_TYPE+"='ENTITY'"+
			" and "+AUTHORIZATION_SQL_JOIN+
			" and at."+COL_RESOURCE_ACCESS_TYPE_ELEMENT+"='READ'"+
			" and ra."+COL_RESOURCE_ACCESS_GROUP_ID+" IN (:"+COL_RESOURCE_ACCESS_GROUP_ID+") ";
	
	private static final String SELECT_FOR_PARTICIPANT_AND_REQUESTER_PAGINATED = 
			"SELECT DISTINCT c.* "+SELECT_FOR_PARTICIPANT_AND_REQUESTER_SQL_CORE+LIMIT_OFFSET;

	private static final String SELECT_FOR_PARTICIPANT_AND_REQUESTER_COUNT = 
			"SELECT COUNT(DISTINCT c."+COL_CHALLENGE_ID+") "+SELECT_FOR_PARTICIPANT_AND_REQUESTER_SQL_CORE;

	private static final String SELECT_MEMBERS_IN_REGISTERED_TEAM = 
			"SELECT gm1."+COL_GROUP_MEMBERS_MEMBER_ID+
			" FROM "+
			TABLE_CHALLENGE+" c, "+
			TABLE_CHALLENGE_TEAM+" ct, "+
			TABLE_GROUP_MEMBERS+" gm1, "+
			TABLE_GROUP_MEMBERS+" gm2 "+
			" WHERE "+
			" c."+COL_CHALLENGE_ID+"=:"+COL_CHALLENGE_ID+
			" AND c."+COL_CHALLENGE_PARTICIPANT_TEAM_ID+"=gm1."+COL_GROUP_MEMBERS_GROUP_ID+
			" AND ct."+COL_CHALLENGE_TEAM_CHALLENGE_ID+"=c."+COL_CHALLENGE_ID+
			" AND ct."+COL_CHALLENGE_TEAM_TEAM_ID+"=gm2."+COL_GROUP_MEMBERS_GROUP_ID+
			" AND gm1."+COL_GROUP_MEMBERS_MEMBER_ID+"=gm2."+COL_GROUP_MEMBERS_MEMBER_ID;

	private static final String SELECT_PARTICIPANTS_CORE =
			" FROM "+TABLE_CHALLENGE+" c, "+TABLE_GROUP_MEMBERS+" gm "+
			" WHERE c."+COL_CHALLENGE_PARTICIPANT_TEAM_ID+"=gm."+COL_GROUP_MEMBERS_GROUP_ID+
			" AND c."+COL_CHALLENGE_ID+"=:"+COL_CHALLENGE_ID;
	
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
	
	@WriteTransaction
	@Override
	public Challenge create(Challenge dto) throws DatastoreException {
		validateChallenge(dto);
		DBOChallenge dbo = copyDTOtoDBO(dto);
		dbo.setId(idGenerator.generateNewId(IdType.CHALLENGE_ID));
		dbo.setEtag(UUID.randomUUID().toString());
		try {
			DBOChallenge created = basicDao.createNew(dbo);
			return copyDBOtoDTO(created);
		} catch (IllegalArgumentException e) {
			if (e.getCause() instanceof DuplicateKeyException) {
				throw new IllegalArgumentException("The specified project may already have a challenge.", e);
			} else if (e.getCause() instanceof DataIntegrityViolationException) {
				throw new InvalidModelException("The submitted data is invalid.  Please ensure the given project and team IDs are correct.", e);
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
			NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
			MapSqlParameterSource param = new MapSqlParameterSource();
			param.addValue(COL_CHALLENGE_PROJECT_ID, KeyFactory.stringToKey(projectId));
			DBOChallenge dbo = namedTemplate.queryForObject(SELECT_FOR_PROJECT, param, DBO_CHALLENGE_TABLE_MAPPING);
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
		NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_GROUP_MEMBERS_MEMBER_ID, principalId);
		param.addValue(LIMIT, limit);
		param.addValue(OFFSET, offset);
		List<DBOChallenge> dbos = namedTemplate.query(SELECT_FOR_PARTICIPANT_PAGINATED, param, DBO_CHALLENGE_TABLE_MAPPING);
		List<Challenge> dtos = new ArrayList<Challenge>();
		for (DBOChallenge dbo : dbos) dtos.add(copyDBOtoDTO(dbo));
		return dtos;
	}

	@Override
	public long listForUserCount(long principalId) throws NotFoundException,
			DatastoreException {
		NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_GROUP_MEMBERS_MEMBER_ID, principalId);
		return namedTemplate.queryForObject(SELECT_FOR_PARTICIPANT_COUNT, param, Long.class);
	}

	@Override
	public List<Challenge> listForUser(final long principalId, final Set<Long> requesterPrincipals,
			final long limit,final long offset) throws NotFoundException, DatastoreException {
		if (limit<=0) throw new IllegalArgumentException("'limit' param must be greater than zero.");
		if (requesterPrincipals.size()==0) return Collections.emptyList();
		NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_RESOURCE_ACCESS_GROUP_ID, requesterPrincipals);
		param.addValue(COL_GROUP_MEMBERS_MEMBER_ID, principalId);
		param.addValue(LIMIT, limit);
		param.addValue(OFFSET, offset);
		List<DBOChallenge> dbos = namedTemplate.query(SELECT_FOR_PARTICIPANT_AND_REQUESTER_PAGINATED, param, DBO_CHALLENGE_TABLE_MAPPING);
		List<Challenge> dtos = new ArrayList<Challenge>();
		for (DBOChallenge dbo : dbos) dtos.add(copyDBOtoDTO(dbo));
		return dtos;
	}

	@Override
	public long listForUserCount(long principalId, Set<Long> requesterPrincipals) throws NotFoundException,
			DatastoreException {
		if (requesterPrincipals.size()==0) return 0L;
		NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_GROUP_MEMBERS_MEMBER_ID, principalId);
		param.addValue(COL_RESOURCE_ACCESS_GROUP_ID, requesterPrincipals);
		return namedTemplate.queryForObject(SELECT_FOR_PARTICIPANT_AND_REQUESTER_COUNT, param, Long.class);
		
	}

	@WriteTransaction
	@Override
	public Challenge update(Challenge dto) throws NotFoundException,
			DatastoreException {
		if (dto.getId()==null) throw new InvalidModelException("ID is required.");
		validateChallenge(dto);
		DBOChallenge dbo = basicDao.getObjectByPrimaryKeyWithUpdateLock(DBOChallenge.class,
				new SinglePrimaryKeySqlParameterSource(dto.getId()));
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
				throw new InvalidModelException("The submitted data is invalid.  Please ensure the given project and team IDs are correct.", e);
			} else {
				throw e;
			}
		}
			

		dbo = basicDao.getObjectByPrimaryKey(DBOChallenge.class, new SinglePrimaryKeySqlParameterSource(dto.getId()));
		return copyDBOtoDTO(dbo);
	}

	@WriteTransaction
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
		NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_CHALLENGE_ID, challengeId);
		param.addValue(LIMIT, limit);
		param.addValue(OFFSET, offset);
		if (affiliated==null) {
			return namedTemplate.queryForList(SELECT_PARTICIPANTS, param, Long.class);
		} else if (affiliated) {
			return namedTemplate.queryForList(SELECT_PARTICIPANTS_IN_REGISTERED_TEAM, param, Long.class);
		} else {
			return namedTemplate.queryForList(SELECT_PARTICIPANTS_NOT_IN_REGISTERED_TEAM, param, Long.class);
		}
	}
	
	@Override
	public long listParticipantsCount(long challengeId, Boolean affiliated)
			throws NotFoundException, DatastoreException {
		NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_CHALLENGE_ID, challengeId);
		if (affiliated==null) {
			return namedTemplate.queryForObject(SELECT_PARTICIPANTS_COUNT, param, Long.class);
		} else if (affiliated) {
			return namedTemplate.queryForObject(SELECT_PARTICIPANTS_IN_REGISTERED_TEAM_COUNT, param, Long.class);
		} else {
			return namedTemplate.queryForObject(SELECT_PARTICIPANTS_NOT_IN_REGISTERED_TEAM_COUNT, param, Long.class);
		}
	}

}
