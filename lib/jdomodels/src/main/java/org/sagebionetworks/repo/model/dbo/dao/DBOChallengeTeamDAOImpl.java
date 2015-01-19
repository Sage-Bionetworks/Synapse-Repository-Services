package org.sagebionetworks.repo.model.dbo.dao;

import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.ChallengeTeam;
import org.sagebionetworks.repo.model.ChallengeTeamDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOChallengeTeam;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
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

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public ChallengeTeam create(ChallengeTeam dto) throws DatastoreException {
		validateChallengeTeam(dto);
		DBOChallengeTeam dbo = copyDTOtoDBO(dto);
		dbo.setId(idGenerator.generateNewId(TYPE.DOMAIN_IDS));
		dbo.setEtag(UUID.randomUUID().toString());
		basicDao.createNew(dbo);
		return copyDTOtoDBO(dbo);
	}

	public static void validateChallengeTeam(ChallengeTeam dto) {
		if (dto.getChallengeProjectId()==null) throw new InvalidModelException("Challenge Project ID is required.");
		if (dto.getTeamId()==null) throw new InvalidModelException("Team ID is required.");
	}

	public static DBOChallengeTeam copyDTOtoDBO(ChallengeTeam dto) {
		DBOChallengeTeam dbo = new DBOChallengeTeam();
		dbo.setGroupId(dto.getTeamId()==null?null:Long.parseLong(dto.getTeamId()));
		dbo.setId(dto.getId()==null?null:Long.parseLong(dto.getId()));
		dbo.setNodeId(dto.getChallengeProjectId()==null?null:Long.parseLong(dto.getChallengeProjectId()));
		return dbo;
	}
	
	public static ChallengeTeam copyDBOtoDTO(DBOChallengeTeam dbo) {
		ChallengeTeam dto = new ChallengeTeam();
		dto.setTeamId(dbo.getNodeId().toString());
		dto.setId(dbo.getId().toString());
		dto.setChallengeProjectId(dbo.getNodeId().toString());
		return dto;
	}

	@Override
	public ChallengeTeam get(String challengeProjectId, String teamId)
			throws NotFoundException, DatastoreException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public ChallengeTeam listForChallenge(String challengeProjectId,
			long limit, long offset) throws NotFoundException,
			DatastoreException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public long ListForChallengeCount(String challengeProjectId)
			throws NotFoundException, DatastoreException {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public void update(ChallengeTeam dto) throws NotFoundException,
			DatastoreException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void delete(long id) throws NotFoundException, DatastoreException {
		// TODO Auto-generated method stub
		
	}
	
}
