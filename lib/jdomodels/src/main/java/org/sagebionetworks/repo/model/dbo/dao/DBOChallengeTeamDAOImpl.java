package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CHALLENGE_TEAM_CHALLENGE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_CHALLENGE_TEAM;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.ChallengeTeam;
import org.sagebionetworks.repo.model.ChallengeTeamDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.persistence.DBOChallengeTeam;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
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
	
	private static final String SELECT_FOR_CHALLENGE_SQL_CORE = 
			" FROM "+TABLE_CHALLENGE_TEAM+" where "+COL_CHALLENGE_TEAM_CHALLENGE_ID+"=?";

	private static final String SELECT_FOR_CHALLENGE_PAGINATED = 
			"SELECT * "+SELECT_FOR_CHALLENGE_SQL_CORE+
			" LIMIT ? OFFSET ?";
	
	private static final String SELECT_FOR_CHALLENGE_COUNT = 
			"SELECT count(*) "+SELECT_FOR_CHALLENGE_SQL_CORE;


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
		try {
			if (dto.getId()==null) throw new InvalidModelException("ID is required.");
			validateChallengeTeam(dto);
			DBOChallengeTeam dbo;
			
			dbo = basicDao.getObjectByPrimaryKeyWithUpdateLock(DBOChallengeTeam.class, 
					new SinglePrimaryKeySqlParameterSource(dto.getId()));
			
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
						"Project setting was updated since you last fetched it, retrieve it again and reapply the update.");
			}
			copyDTOtoDBO(dto, dbo);
			// Update with a new e-tag
			dbo.setEtag(UUID.randomUUID().toString());

			boolean success = basicDao.update(dbo);
			if (!success)
				throw new DatastoreException("Unsuccessful updating ChallengeTeam in database.");

			dbo = basicDao.getObjectByPrimaryKey(DBOChallengeTeam.class, new SinglePrimaryKeySqlParameterSource(dto.getId()));
			return copyDBOtoDTO(dbo);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("The resource you are attempting to access cannot be found.");
		}
	}
	
	@Override
	public List<ChallengeTeam> listForChallenge(String challengeId, long limit,
			long offset) throws NotFoundException, DatastoreException {
		if (limit<=0) throw new IllegalArgumentException("'limit' param must be greater than zero.");
		List<DBOChallengeTeam> dbos = jdbcTemplate.query(SELECT_FOR_CHALLENGE_PAGINATED, DBO_CHALLENGE_TEAM_TABLE_MAPPING, challengeId, offset, limit);
		List<ChallengeTeam> dtos = new ArrayList<ChallengeTeam>();
		for (DBOChallengeTeam dbo : dbos) dtos.add(copyDBOtoDTO(dbo));
		return dtos;
	}

	@Override
	public long listForChallengeCount(String challengeId)
			throws NotFoundException, DatastoreException {
		return jdbcTemplate.queryForObject(SELECT_FOR_CHALLENGE_COUNT, Long.class, challengeId);
	}

	@Override
	public void delete(long id) throws NotFoundException, DatastoreException {
		basicDao.deleteObjectByPrimaryKey(DBOChallengeTeam.class, new SinglePrimaryKeySqlParameterSource(id));
	}
}
