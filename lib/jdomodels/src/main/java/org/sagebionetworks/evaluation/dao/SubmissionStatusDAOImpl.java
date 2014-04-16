package org.sagebionetworks.evaluation.dao;

import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_ETAG;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_VERSION;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.sagebionetworks.evaluation.dbo.DBOConstants;
import org.sagebionetworks.evaluation.dbo.SubmissionStatusDBO;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.util.EvaluationUtils;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ObservableEntity;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.evaluation.SubmissionStatusDAO;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.query.SQLConstants;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class SubmissionStatusDAOImpl implements SubmissionStatusDAO {
	
	@Autowired
	private DBOBasicDao basicDao;
	
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	
	private static final String ID = DBOConstants.PARAM_SUBMISSION_ID;
	
	private static final String SQL_ETAG_WITHOUT_LOCK = "SELECT " + COL_SUBSTATUS_ETAG +", "+
			COL_SUBSTATUS_VERSION + " FROM " +
			SQLConstants.TABLE_SUBSTATUS +" WHERE ID = ?";

	private static final String SQL_ETAG_FOR_UPDATE = SQL_ETAG_WITHOUT_LOCK + " FOR UPDATE";

	private static final String SUBMISSION_NOT_FOUND = "Submission could not be found with id :";
	
	
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public String create(SubmissionStatus dto) throws DatastoreException {
		// Convert to DBO
		dto.setStatusVersion(DBOConstants.SUBSTATUS_INITIAL_VERSION_NUMBER);
		SubmissionStatusDBO dbo = SubmissionUtils.convertDtoToDbo(dto);
		
		// Generate a new eTag
		dbo.seteTag(UUID.randomUUID().toString());

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
		return SubmissionUtils.convertDboToDto(dbo);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void update(SubmissionStatus dto) throws DatastoreException,
			InvalidModelException, NotFoundException, ConflictingUpdateException {
		SubmissionStatusDBO dbo = SubmissionUtils.convertDtoToDbo(dto);
		dbo.setModifiedOn(System.currentTimeMillis());
		verifySubmissionStatusDBO(dbo);

		// update eTag and send message of update
		EtagAndVersion newEtagAndVersion = lockAndGenerateEtag(dbo.getIdString(), dbo.getEtag());
		dbo.seteTag(newEtagAndVersion.getEtag());
		dbo.setVersion(newEtagAndVersion.getVersion());
		
		// we also need to update the serialized field
		dto = SubmissionUtils.convertDboToDto(dbo);
		SubmissionUtils.copyToSerializedField(dto, dbo);
		
		basicDao.update(dbo);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void delete(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID, id);
		basicDao.deleteObjectByPrimaryKey(SubmissionStatusDBO.class, param);
	}

	/**
	 * Ensure that a SubmissionStatusDBO object has all required components
	 * 
	 * @param dbo
	 */
	private void verifySubmissionStatusDBO(SubmissionStatusDBO dbo) {
		EvaluationUtils.ensureNotNull(dbo.getId(), "Submission ID");
		EvaluationUtils.ensureNotNull(dbo.getEtag(), "eTag");
		EvaluationUtils.ensureNotNull(dbo.getModifiedOn(), "Modified date");
		EvaluationUtils.ensureNotNull(dbo.getStatusEnum(), "Submission status");
		EvaluationUtils.ensureNotNull(dbo.getVersion(), "Status version");
	}
	
	private EtagAndVersion lockAndGenerateEtag(String id, String eTag)
			throws NotFoundException, ConflictingUpdateException, DatastoreException {
		EtagAndVersion current = lockForUpdate(id);
		// Check the eTags
		if(!current.getEtag().equals(eTag)){
			throw new ConflictingUpdateException("Node: "+id+" was updated since you last fetched it, retrieve it again and reapply the update");
		}
		// Get a new e-tag
		return new EtagAndVersion(UUID.randomUUID().toString(), current.getVersion()+1L);
	}
	
	private EtagAndVersion lockForUpdate(String id) {
		// Create a Select for update query
		return simpleJdbcTemplate.queryForObject(SQL_ETAG_FOR_UPDATE, new RowMapper<EtagAndVersion>() {
			@Override
			public EtagAndVersion mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				return new EtagAndVersion(rs.getString(COL_SUBSTATUS_ETAG), rs.getLong(COL_SUBSTATUS_VERSION));
			}}, id);
	}

	@Override
	public long getCount() throws DatastoreException {
		return basicDao.getCount(SubmissionStatusDBO.class);
	}
}
