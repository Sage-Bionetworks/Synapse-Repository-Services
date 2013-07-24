package org.sagebionetworks.evaluation.dao;

import static org.sagebionetworks.evaluation.query.jdo.SQLConstants.COL_EVALUATION_ETAG;
import static org.sagebionetworks.evaluation.query.jdo.SQLConstants.COL_EVALUATION_ID;
import static org.sagebionetworks.evaluation.query.jdo.SQLConstants.COL_EVALUATION_NAME;
import static org.sagebionetworks.evaluation.query.jdo.SQLConstants.COL_EVALUATION_STATUS;
import static org.sagebionetworks.evaluation.query.jdo.SQLConstants.COL_PARTICIPANT_EVAL_ID;
import static org.sagebionetworks.evaluation.query.jdo.SQLConstants.COL_PARTICIPANT_USER_ID;
import static org.sagebionetworks.evaluation.query.jdo.SQLConstants.LIMIT_PARAM_NAME;
import static org.sagebionetworks.evaluation.query.jdo.SQLConstants.OFFSET_PARAM_NAME;
import static org.sagebionetworks.evaluation.query.jdo.SQLConstants.TABLE_EVALUATION;
import static org.sagebionetworks.evaluation.query.jdo.SQLConstants.TABLE_PARTICIPANT;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.sagebionetworks.evaluation.dbo.DBOConstants;
import org.sagebionetworks.evaluation.dbo.EvaluationDBO;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.util.EvaluationUtils;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.TagMessenger;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class EvaluationDAOImpl implements EvaluationDAO {
	
	@Autowired
	private DBOBasicDao basicDao;
	
	@Autowired
	private TagMessenger tagMessenger;
	
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	
	private static final String ID = DBOConstants.PARAM_EVALUATION_ID;
	private static final String NAME = DBOConstants.PARAM_EVALUATION_NAME;
	private static final String STATUS = DBOConstants.PARAM_EVALUATION_STATUS;
	
	private static final String SELECT_BY_NAME_SQL = 
			"SELECT ID FROM "+ TABLE_EVALUATION +
			" WHERE "+ COL_EVALUATION_NAME + "=:" + NAME;
	
	private static final String SELECT_ALL_SQL_PAGINATED = 
			"SELECT * FROM "+ TABLE_EVALUATION +
			" LIMIT :"+ LIMIT_PARAM_NAME +
			" OFFSET :" + OFFSET_PARAM_NAME;
	
	private static final String SELECT_BY_STATUS_SQL_PAGINATED = 
			"SELECT * FROM "+ TABLE_EVALUATION +
			" WHERE " + COL_EVALUATION_STATUS + "=:" + STATUS +
			" LIMIT :"+ LIMIT_PARAM_NAME +
			" OFFSET :" + OFFSET_PARAM_NAME;
	
	private static final String SQL_ETAG_WITHOUT_LOCK = "SELECT " + COL_EVALUATION_ETAG + " FROM " +
														TABLE_EVALUATION +" WHERE ID = ?";
	
	private static final String SQL_ETAG_FOR_UPDATE = SQL_ETAG_WITHOUT_LOCK + " FOR UPDATE";
	
	private static final RowMapper<EvaluationDBO> rowMapper = ((new EvaluationDBO()).getTableMapping());

	private static final String EVALUATION_NOT_FOUND = "Evaluation could not be found with id :";

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public String create(Evaluation dto, Long ownerId) throws DatastoreException {
		return create(dto, ownerId, false);
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public String createFromBackup(Evaluation dto, Long ownerId) throws DatastoreException {
		return create(dto, ownerId, true);
	}
		
	private String create(Evaluation dto, Long ownerId, boolean fromBackup) {
		EvaluationUtils.ensureNotNull(dto, "Evaluation object");
		EvaluationUtils.ensureNotNull(ownerId, "Owner ID");
		EvaluationUtils.ensureNotNull(dto.getId(), "Evaluation ID");
		
		// convert to DBO
		EvaluationDBO dbo = new EvaluationDBO();		
		
		// set Owner ID
		dto.setOwnerId(ownerId.toString());
		
		// serialize
		copyDtoToDbo(dto, dbo);
		
		// generate a new eTag, unless restoring from backup
		if (!fromBackup) {			
			tagMessenger.generateEtagAndSendMessage(dbo, ChangeType.CREATE);
		}
				
		// ensure DBO has required information
		verifyEvaluationDBO(dbo);
		
		// create DBO
		try {
			dbo = basicDao.createNew(dbo);
			return dbo.getId().toString();
		} catch (Exception e) {
			String message = e.getMessage() + " id=" + dbo.getId() + " name=" + dto.getName();
			
			// check if a name conflict occurred
			if (e.getClass() == IllegalArgumentException.class) {
				IllegalArgumentException e2 = (IllegalArgumentException) e;
				if (e2.getCause().getClass() == DuplicateKeyException.class)
					message = "An Evaluation already exists with the name '" +
							dto.getName() + "'";
				throw new NameConflictException(message, e);
			}
			
			throw new DatastoreException(message, e);
		}
	}

	@Override
	public Evaluation get(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID, id);
		EvaluationDBO dbo = basicDao.getObjectByPrimaryKey(EvaluationDBO.class, param);
		Evaluation dto = new Evaluation();
		copyDboToDto(dbo, dto);
		return dto;
	}

	@Override
	public List<Evaluation> getInRange(long limit, long offset) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(OFFSET_PARAM_NAME, offset);
		param.addValue(LIMIT_PARAM_NAME, limit);	
		List<EvaluationDBO> dbos = simpleJdbcTemplate.query(SELECT_ALL_SQL_PAGINATED, rowMapper, param);
		List<Evaluation> dtos = new ArrayList<Evaluation>();
		for (EvaluationDBO dbo : dbos) {
			Evaluation dto = new Evaluation();
			copyDboToDto(dbo, dto);
			dtos.add(dto);
		}
		return dtos;
	}

	@Override
	public List<Evaluation> getInRange(long limit, long offset,
			EvaluationStatus status) throws DatastoreException,
			NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(OFFSET_PARAM_NAME, offset);
		param.addValue(LIMIT_PARAM_NAME, limit);	
		param.addValue(STATUS, status.ordinal());
		List<EvaluationDBO> dbos = simpleJdbcTemplate.query(SELECT_BY_STATUS_SQL_PAGINATED, rowMapper, param);
		List<Evaluation> dtos = new ArrayList<Evaluation>();
		for (EvaluationDBO dbo : dbos) {
			Evaluation dto = new Evaluation();
			copyDboToDto(dbo, dto);
			dtos.add(dto);
		}
		return dtos;
	}

	@Override
	public long getCount() throws DatastoreException {
		return basicDao.getCount(EvaluationDBO.class);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void update(Evaluation dto)
			throws DatastoreException, InvalidModelException,
			NotFoundException, ConflictingUpdateException {
		update(dto, false);		
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void updateFromBackup(Evaluation dto)
			throws DatastoreException, InvalidModelException,
			NotFoundException, ConflictingUpdateException {
		update(dto, true);
	}

	private void update(Evaluation dto, boolean fromBackup) throws ConflictingUpdateException, DatastoreException, NotFoundException {
		EvaluationDBO dbo = new EvaluationDBO();
		copyDtoToDbo(dto, dbo);
		verifyEvaluationDBO(dbo);
		
		if (fromBackup) {
			// keep same eTag
			lockAndSendTagMessage(dbo, ChangeType.UPDATE); 
		} else {
			// update eTag
			String newEtag = lockAndGenerateEtag(dbo.getIdString(), dbo.geteTag(), ChangeType.UPDATE);	
			dbo.seteTag(newEtag);
		}
		
		// TODO: detect and log NO-OP update
		basicDao.update(dbo);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void delete(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID, id);
		basicDao.deleteObjectByPrimaryKey(EvaluationDBO.class, param);		
	}

	@Override
	public String lookupByName(String name) throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(NAME, name);
		try {
			Long id = simpleJdbcTemplate.queryForLong(SELECT_BY_NAME_SQL, param);		
			return id.toString();
		} catch (EmptyResultDataAccessException e) {
			// name is not in use
			return null;
		}
	}

	/**
	 * Copy a EvaluationDBO database object to a Evaluation data transfer object
	 * 
	 * @param dto
	 * @param dbo
	 */
	protected static void copyDtoToDbo(Evaluation dto, EvaluationDBO dbo) {	
		try {
			dbo.setId(dto.getId() == null ? null : Long.parseLong(dto.getId()));
		} catch (NumberFormatException e) {
			throw new NumberFormatException("Invalid Evaluation ID: " + dto.getId());
		}
		dbo.seteTag(dto.getEtag());
		dbo.setName(dto.getName());
		dbo.setDescription(dto.getDescription() == null ? null : dto.getDescription().getBytes());
		try {
			dbo.setOwnerId(dto.getOwnerId() == null ? null : Long.parseLong(dto.getOwnerId()));
		} catch (NumberFormatException e) {
			throw new NumberFormatException("Invalid Owner ID: " + dto.getOwnerId());
		}
		dbo.setCreatedOn(dto.getCreatedOn() == null ? null : dto.getCreatedOn().getTime());
		dbo.setContentSource(KeyFactory.stringToKey(dto.getContentSource()));
		dbo.setStatusEnum(dto.getStatus());
		if (dto.getSubmissionInstructionsMessage() != null) {
			dbo.setSubmissionInstructionsMessage(dto.getSubmissionInstructionsMessage().getBytes());
		}
		if (dto.getSubmissionReceiptMessage() != null) {
			dbo.setSubmissionReceiptMessage(dto.getSubmissionReceiptMessage().getBytes());
		}
	}
	
	/**
	 * Copy a Evaluation data transfer object to a EvaluationDBO database object
	 *
	 * @param dbo
	 * @param dto
	 * @throws DatastoreException
	 */
	protected static void copyDboToDto(EvaluationDBO dbo, Evaluation dto) throws DatastoreException {	
		dto.setId(dbo.getId() == null ? null : dbo.getId().toString());
		dto.setEtag(dbo.geteTag());
		dto.setName(dbo.getName());
		if (dbo.getDescription() != null) {
			try {
				dto.setDescription(new String(dbo.getDescription(), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new DatastoreException(e);
			}
		} else {
			dto.setDescription(null);
		}
		dto.setOwnerId(dbo.getOwnerId().toString());
		dto.setCreatedOn(new Date(dbo.getCreatedOn()));
		dto.setContentSource(KeyFactory.keyToString(dbo.getContentSource()));
		dto.setStatus(dbo.getStatusEnum());
		if (dbo.getSubmissionInstructionsMessage() != null) {
			try {
				dto.setSubmissionInstructionsMessage(new String(dbo.getSubmissionInstructionsMessage(), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new DatastoreException(e);
			}
		}
		if (dbo.getSubmissionReceiptMessage() != null) {
			try {
				dto.setSubmissionReceiptMessage(new String(dbo.getSubmissionReceiptMessage(), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new DatastoreException(e);
			}
		}
	}

	/**
	 * Ensure that a EvaluationDBO object has all required components
	 * 
	 * @param dbo
	 */
	private void verifyEvaluationDBO(EvaluationDBO dbo) {
		EvaluationUtils.ensureNotNull(dbo.getId(), "ID");
		EvaluationUtils.ensureNotNull(dbo.geteTag(), "eTag");
		EvaluationUtils.ensureNotNull(dbo.getName(), "name");
		EvaluationUtils.ensureNotNull(dbo.getOwnerId(), "ownerID");
		EvaluationUtils.ensureNotNull(dbo.getCreatedOn(), "creation date");
		EvaluationUtils.ensureNotNull(dbo.getContentSource(), "content source");
		EvaluationUtils.ensureNotNull(dbo.getStatusEnum(), "status");
	}
	
	private String lockAndGenerateEtag(String id, String eTag, ChangeType changeType)
			throws NotFoundException, ConflictingUpdateException, DatastoreException {
		String currentTag = lockForUpdate(id);
		// Check the eTags
		if(!currentTag.equals(eTag)){
			throw new ConflictingUpdateException("Evaluation: " + id + " has been updated since " +
					"you last fetched it; please retrieve it again and re-apply the update");
		}
		// Get a new e-tag
		EvaluationDBO dbo = getDBO(id);
		tagMessenger.generateEtagAndSendMessage(dbo, changeType);
		return dbo.geteTag();
	}
	
	private EvaluationDBO getDBO(String id) throws NotFoundException {
		EvaluationUtils.ensureNotNull(id, "Evaluation id");
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(DBOConstants.PARAM_EVALUATION_ID, id);
		try {
			EvaluationDBO dbo = basicDao.getObjectByPrimaryKey(EvaluationDBO.class, param);
			return dbo;
		} catch (NotFoundException e) {
			throw new NotFoundException(EVALUATION_NOT_FOUND + id);
		}
	}
	
	private String lockForUpdate(String id) {
		// Create a Select for update query
		return simpleJdbcTemplate.queryForObject(SQL_ETAG_FOR_UPDATE, String.class, id);
	}
	
	private void lockAndSendTagMessage(EvaluationDBO dbo, ChangeType changeType) {
		lockForUpdate(dbo.getIdString());
		tagMessenger.sendMessage(dbo, changeType);		
	}
	
	private static final String SELECT_BY_USER_CORE = 
		TABLE_EVALUATION +" e, "+TABLE_PARTICIPANT + " p WHERE "+
		" e."+ COL_EVALUATION_ID + "=p."+ COL_PARTICIPANT_EVAL_ID + " AND " +
		"p."+COL_PARTICIPANT_USER_ID + " IN (:"+ COL_PARTICIPANT_USER_ID+" ) ";
	
	private static final String SELECT_BY_USER_SQL =
		"SELECT e.* FROM "+ SELECT_BY_USER_CORE + 
		" LIMIT :"+ LIMIT_PARAM_NAME +
		" OFFSET :" + OFFSET_PARAM_NAME;

	private static final String SELECT_BY_USER_SQL_COUNT = 
		"SELECT count(*) FROM "+ SELECT_BY_USER_CORE;
		
	private static final String SELECT_BY_USER_AND_STATUS_CORE = 
		TABLE_EVALUATION +" e, "+TABLE_PARTICIPANT + " p WHERE "+
		" e." + COL_EVALUATION_STATUS + "=:"+STATUS + " AND " +
		" e."+ COL_EVALUATION_ID + "=p."+ COL_PARTICIPANT_EVAL_ID + " AND " +
		"p."+COL_PARTICIPANT_USER_ID + " IN (:"+ COL_PARTICIPANT_USER_ID+" ) ";

	private static final String SELECT_BY_USER_AND_STATUS_SQL =
		"SELECT e.* FROM "+ SELECT_BY_USER_AND_STATUS_CORE + 
		 " LIMIT :"+ LIMIT_PARAM_NAME +
		 " OFFSET :" + OFFSET_PARAM_NAME;
		
	private static final String SELECT_BY_USER_AND_STATUS_SQL_COUNT =
		"SELECT count(*) FROM "+ SELECT_BY_USER_AND_STATUS_CORE;

	/**
	 * return the evaluations in which the user (given as a list of principal Ids)
	 * is either the owner or is a participant
	 */
	@Override
	public List<Evaluation> getAvailableInRange(List<Long> principalIds, EvaluationStatus status, long limit, long offset) throws DatastoreException {
		if (principalIds.isEmpty()) return new ArrayList<Evaluation>(); // SQL breaks down if list is empty
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_PARTICIPANT_USER_ID, principalIds);	
		param.addValue(OFFSET_PARAM_NAME, offset);
		param.addValue(LIMIT_PARAM_NAME, limit);	
		String sql = null;
		if (null==status) {
			sql = SELECT_BY_USER_SQL;
		} else {
			param.addValue(STATUS, status.ordinal());
			sql = SELECT_BY_USER_AND_STATUS_SQL;
		}
		List<EvaluationDBO> dbos = simpleJdbcTemplate.query(sql, rowMapper, param);
		List<Evaluation> dtos = new ArrayList<Evaluation>();
		for (EvaluationDBO dbo : dbos) {
			Evaluation dto = new Evaluation();
			copyDboToDto(dbo, dto);
			dtos.add(dto);
		}
		return dtos;
	}

	@Override
	public long getAvailableCount(List<Long> principalIds, EvaluationStatus status) throws DatastoreException {
		if (principalIds.isEmpty()) return 0L; // SQL breaks down if list is empty
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_PARTICIPANT_USER_ID, principalIds);		
		String sql = null;
		if (null==status) {
			sql = SELECT_BY_USER_SQL_COUNT;
		} else {
			param.addValue(STATUS, status.ordinal());
			sql = SELECT_BY_USER_AND_STATUS_SQL_COUNT;
		}
		return simpleJdbcTemplate.queryForLong(sql, param);
	}

	
}
