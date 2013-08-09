package org.sagebionetworks.evaluation.dao;

import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_ETAG;
import java.io.IOException;
import java.util.Date;

import org.sagebionetworks.evaluation.dbo.DBOConstants;
import org.sagebionetworks.evaluation.dbo.SubmissionStatusDBO;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.util.EvaluationUtils;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.TagMessenger;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.model.query.SQLConstants;
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
	
	private static final String SQL_ETAG_WITHOUT_LOCK = "SELECT " + COL_SUBSTATUS_ETAG + " FROM " +
			SQLConstants.TABLE_SUBSTATUS +" WHERE ID = ?";

	private static final String SQL_ETAG_FOR_UPDATE = SQL_ETAG_WITHOUT_LOCK + " FOR UPDATE";

	private static final String SUBMISSION_NOT_FOUND = "Submission could not be found with id :";
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public String create(SubmissionStatus dto) throws DatastoreException {
		return create(dto, false);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public String createFromBackup(SubmissionStatus dto) throws DatastoreException {
		return create(dto, true);
	}	
	
	private String create(SubmissionStatus dto, boolean fromBackup) throws DatastoreException {
		// Convert to DBO
		SubmissionStatusDBO dbo = convertDtoToDbo(dto);
		
		// generate a new eTag, unless restoring from backup
		if (!fromBackup) {			
			tagMessenger.generateEtagAndSendMessage(dbo, ChangeType.CREATE);
		}
		
		// Ensure DBO has required information
		verifySubmissionStatusDBO(dbo);
		
		// Create DBO
		try {
			dbo = basicDao.createNew(dbo);
			return dbo.getId().toString();
		} catch (Exception e) {
			throw new DatastoreException(e.getMessage() + " id=" + dbo.getId(), e);
		}
	}

	@Override
	public SubmissionStatus get(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID, id);
		SubmissionStatusDBO dbo = basicDao.getObjectByPrimaryKey(SubmissionStatusDBO.class, param);		
		return convertDboToDto(dbo);
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
		SubmissionStatusDBO dbo = convertDtoToDbo(dto);
		dbo.setModifiedOn(System.currentTimeMillis());
		verifySubmissionStatusDBO(dbo);
				
		if (fromBackup) {
			// keep same eTag but send message of update
			lockAndSendTagMessage(dbo, ChangeType.UPDATE); 
		} else {
			// update eTag and send message of update
			String newEtag = lockAndGenerateEtag(dbo.getIdString(), dbo.geteTag(), ChangeType.UPDATE);
			dbo.seteTag(newEtag);
		}
		
		basicDao.update(dbo);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void delete(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID, id);
		basicDao.deleteObjectByPrimaryKey(SubmissionStatusDBO.class, param);
		// Send a delete message
		tagMessenger.sendDeleteMessage(id, ObjectType.SUBMISSION);
	}

	/**
	 * Ensure that a SubmissionStatusDBO object has all required components
	 * 
	 * @param dbo
	 */
	private void verifySubmissionStatusDBO(SubmissionStatusDBO dbo) {
		EvaluationUtils.ensureNotNull(dbo.getId(), "Submission ID");
		EvaluationUtils.ensureNotNull(dbo.geteTag(), "eTag");
		EvaluationUtils.ensureNotNull(dbo.getModifiedOn(), "Modified date");
		EvaluationUtils.ensureNotNull(dbo.getStatusEnum(), "Submission status");
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
		EvaluationUtils.ensureNotNull(id, "Submission id");
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(DBOConstants.PARAM_EVALUATION_ID, id);
		try {
			SubmissionStatusDBO dbo = basicDao.getObjectByPrimaryKey(SubmissionStatusDBO.class, param);
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

	@Override
	public long getCount() throws DatastoreException {
		return basicDao.getCount(SubmissionStatusDBO.class);
	}

	/**
	 * Convert a SubmissionStatusDBO database object to a Participant data transfer object
	 * 
	 * @param dto
	 * @param dbo
	 */
	protected static SubmissionStatusDBO convertDtoToDbo(SubmissionStatus dto) {
		SubmissionStatusDBO dbo = new SubmissionStatusDBO();
		try {
			dbo.setId(dto.getId() == null ? null : Long.parseLong(dto.getId()));
		} catch (NumberFormatException e) {
			throw new NumberFormatException("Invalid Submission ID: " + dto.getId());
		}
		dbo.seteTag(dto.getEtag());
		dbo.setModifiedOn(dto.getModifiedOn() == null ? null : dto.getModifiedOn().getTime());
		dbo.setScore(dto.getScore());
		dbo.setStatusEnum(dto.getStatus());
		copyToSerializedField(dto, dbo);
		
		return dbo;
	}

	/**
	 * Convert a SubmissionStatus data transfer object to a SubmissionDBO database object
	 * 
	 * @param dbo
	 * @param dto
	 * @throws DatastoreException
	 */
	protected static SubmissionStatus convertDboToDto(SubmissionStatusDBO dbo) throws DatastoreException {		
		// serialized entity is regarded as the "true" representation of the object
		SubmissionStatus dto = copyFromSerializedField(dbo);
		
		// use non-serialized eTag and modified date as the "true" values
		dto.setEtag(dbo.geteTag());
		dto.setModifiedOn(dbo.getModifiedOn() == null ? null : new Date(dbo.getModifiedOn()));
		
		// populate from secondary columns if necessary (to support legacy non-serialized objects)
		if (dto.getId() == null) 
			dto.setId(dbo.getId().toString());			
		if (dto.getScore() == null)
			dto.setScore(dbo.getScore());
		if (dto.getStatus() == null)
			dto.setStatus(dbo.getStatusEnum());
		
		return dto;
	}	
	
	protected static void copyToSerializedField(SubmissionStatus dto, SubmissionStatusDBO dbo) throws DatastoreException {
		try {
			dbo.setSerializedEntity(JDOSecondaryPropertyUtils.compressObject(dto));
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}
	
	protected static SubmissionStatus copyFromSerializedField(SubmissionStatusDBO dbo) throws DatastoreException {
		try {
			return (SubmissionStatus) JDOSecondaryPropertyUtils.decompressedObject(dbo.getSerializedEntity());
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}
}
