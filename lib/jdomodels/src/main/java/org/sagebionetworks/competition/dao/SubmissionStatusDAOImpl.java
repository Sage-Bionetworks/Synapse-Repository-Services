package org.sagebionetworks.competition.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACTIVITY_ETAG;

import java.util.Date;

import org.sagebionetworks.competition.dbo.DBOConstants;
import org.sagebionetworks.competition.dbo.SubmissionStatusDBO;
import org.sagebionetworks.competition.model.SubmissionStatus;
import org.sagebionetworks.competition.query.jdo.SQLConstants;
import org.sagebionetworks.competition.util.CompetitionUtils;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.TagMessenger;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class SubmissionStatusDAOImpl implements SubmissionStatusDAO {
	
	@Autowired
	private DBOBasicDao basicDao;
	
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	
	@Autowired
	private TagMessenger tagMessenger;
	
	private static final String ID = DBOConstants.PARAM_SUBMISSION_ID;
	
	private static final String SQL_ETAG_WITHOUT_LOCK = "SELECT "+COL_ACTIVITY_ETAG+" FROM " +
			SQLConstants.TABLE_SUBSTATUS +" WHERE ID = ?";

	private static final String SQL_ETAG_FOR_UPDATE = SQL_ETAG_WITHOUT_LOCK + " FOR UPDATE";

	private static final String SUBMISSION_NOT_FOUND = "Submission could not be found with id :";
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public SubmissionStatus create(SubmissionStatus dto) throws DatastoreException {		
		// Convert to DBO
		SubmissionStatusDBO dbo = new SubmissionStatusDBO();
		copyDtoToDbo(dto, dbo);

		// Set modified date
		dbo.setModifiedOn(System.currentTimeMillis());
		
		// Generate eTag
		tagMessenger.generateEtagAndSendMessage(dbo, ChangeType.CREATE);
		
		// Ensure DBO has required information
		verifySubmissionStatusDBO(dbo);
		
		// Create DBO
		try {
			dbo = basicDao.createNew(dbo);
			dto = new SubmissionStatus();
			copyDboToDto(dbo, dto);
			return dto;
		} catch (Exception e) {
			throw new DatastoreException("id=" + dbo.getId(), e);
		}
	}

	@Override
	public SubmissionStatus get(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID, id);
		SubmissionStatusDBO dbo = basicDao.getObjectById(SubmissionStatusDBO.class, param);
		SubmissionStatus dto = new SubmissionStatus();
		copyDboToDto(dbo, dto);
		return dto;
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void update(SubmissionStatus dto) throws DatastoreException,
			InvalidModelException, NotFoundException, ConflictingUpdateException {
		update(dto, false);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void updateFromBackup(SubmissionStatus dto) throws DatastoreException,
			InvalidModelException, NotFoundException, ConflictingUpdateException {
		update(dto, true);
	}
	
	private void update(SubmissionStatus dto, boolean fromBackup) throws ConflictingUpdateException, DatastoreException, NotFoundException {
		SubmissionStatusDBO dbo = new SubmissionStatusDBO();
		copyDtoToDbo(dto, dbo);
		dbo.setModifiedOn(System.currentTimeMillis());
		verifySubmissionStatusDBO(dbo);
				
		if (fromBackup) {
			// keep same eTag but send message of update
			lockAndSendTagMessage(dbo, ChangeType.UPDATE); 
		} else {
			// update eTag and send message of update
			lockAndGenerateEtag(dbo.getIdString(), dbo.geteTag(), ChangeType.UPDATE);			
		}
		
		basicDao.update(dbo);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void delete(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID, id);
		basicDao.deleteObjectById(SubmissionStatusDBO.class, param);		
	}

	/**
	 * Copy a SubmissionStatusDBO database object to a Participant data transfer object
	 * 
	 * @param dto
	 * @param dbo
	 */
	private static void copyDtoToDbo(SubmissionStatus dto, SubmissionStatusDBO dbo) {	
		dbo.setId(dto.getId() == null ? null : Long.parseLong(dto.getId()));
		dbo.seteTag(dto.getEtag());
		dbo.setModifiedOn(dto.getModifiedOn() == null ? null : dto.getModifiedOn().getTime());
		dbo.setScore(dto.getScore());
		dbo.setStatusEnum(dto.getStatus());
	}
	
	/**
	 * Copy a SubmissionStatus data transfer object to a SubmissionDBO database object
	 * 
	 * @param dbo
	 * @param dto
	 * @throws DatastoreException
	 */
	private static void copyDboToDto(SubmissionStatusDBO dbo, SubmissionStatus dto) throws DatastoreException {
		dto.setId(dbo.getId() == null ? null : dbo.getId().toString());
		dto.setEtag(dbo.geteTag());
		dto.setModifiedOn(dbo.getModifiedOn() == null ? null : new Date(dbo.getModifiedOn()));
		dto.setScore(dbo.getScore());
		dto.setStatus(dbo.getStatusEnum());
	}

	/**
	 * Ensure that a SubmissionStatusDBO object has all required components
	 * 
	 * @param dbo
	 */
	private void verifySubmissionStatusDBO(SubmissionStatusDBO dbo) {
		CompetitionUtils.ensureNotNull(dbo.getId(), "Submission ID");
		CompetitionUtils.ensureNotNull(dbo.geteTag(), "eTag");
		CompetitionUtils.ensureNotNull(dbo.getModifiedOn(), "Modified date");
		CompetitionUtils.ensureNotNull(dbo.getStatusEnum(), "Submission status");
	}
	
	private String lockAndGenerateEtag(String id, String eTag, ChangeType changeType)
			throws NotFoundException, ConflictingUpdateException, DatastoreException {
		String currentTag = lockForUpdate(id);
		// Check the eTags
		if(!currentTag.equals(eTag)){
			throw new ConflictingUpdateException("Node: "+id+" was updated since you last fetched it, retrieve it again and reapply the update");
		}
		// Get a new e-tag
		SubmissionStatusDBO dbo = getDBO(id);
		tagMessenger.generateEtagAndSendMessage(dbo, changeType);
		return dbo.geteTag();
	}
	
	private SubmissionStatusDBO getDBO(String id) throws NotFoundException {
		CompetitionUtils.ensureNotNull(id, "Submission id");
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(DBOConstants.PARAM_COMPETITION_ID, id);
		try {
			SubmissionStatusDBO dbo = basicDao.getObjectById(SubmissionStatusDBO.class, param);
			return dbo;
		} catch (NotFoundException e) {
			throw new NotFoundException(SUBMISSION_NOT_FOUND + id);
		}
	}
	
	private String lockForUpdate(String id) {
		// Create a Select for update query
		return simpleJdbcTemplate.queryForObject(SQL_ETAG_FOR_UPDATE, String.class, id);
	}
	
	private void lockAndSendTagMessage(SubmissionStatusDBO dbo, ChangeType changeType) {
		lockForUpdate(dbo.getIdString());
		tagMessenger.sendMessage(dbo, changeType);		
	}	
}
