package org.sagebionetworks.evaluation.dao;

import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_ETAG;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_NAME;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_STATUS;
import static org.sagebionetworks.repo.model.query.SQLConstants.LIMIT_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.SQLConstants.OFFSET_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.SQLConstants.TABLE_EVALUATION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACL_OWNER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACL_OWNER_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_OWNER;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.sagebionetworks.evaluation.dbo.DBOConstants;
import org.sagebionetworks.evaluation.dbo.EvaluationDBO;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.util.EvaluationUtils;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.jdo.AuthorizationSqlUtil;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.query.SQLConstants;
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
	private TransactionalMessenger transactionalMessenger;
	
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	
	private static final String ID = DBOConstants.PARAM_EVALUATION_ID;
	private static final String NAME = DBOConstants.PARAM_EVALUATION_NAME;
	private static final String STATUS = DBOConstants.PARAM_EVALUATION_STATUS;
	private static final String CONTENT_SOURCE = DBOConstants.PARAM_EVALUATION_CONTENT_SOURCE;
	
	private static final String OFFSET_AND_LIMIT = 
			" LIMIT :"+ LIMIT_PARAM_NAME +
			" OFFSET :" + OFFSET_PARAM_NAME;
	
	private static final String SELECT_BY_CONTENT_SOURCE = 
			"SELECT * FROM "+SQLConstants.TABLE_EVALUATION+
			" WHERE "+SQLConstants.COL_EVALUATION_CONTENT_SOURCE+"=:"+CONTENT_SOURCE+
			OFFSET_AND_LIMIT;
	
	private static final String COUNT_BY_CONTENT_SOURCE = 
			"SELECT COUNT(*) FROM "+SQLConstants.TABLE_EVALUATION+
			" WHERE "+SQLConstants.COL_EVALUATION_CONTENT_SOURCE+"=:"+CONTENT_SOURCE;
	
	private static final String SELECT_BY_NAME_SQL = 
			"SELECT ID FROM "+ TABLE_EVALUATION +
			" WHERE "+ COL_EVALUATION_NAME + "=:" + NAME;
	
	private static final String SELECT_ALL_SQL_PAGINATED = 
			"SELECT * FROM "+ TABLE_EVALUATION +
			OFFSET_AND_LIMIT;
	
	private static final String SELECT_BY_STATUS_SQL_PAGINATED = 
			"SELECT * FROM "+ TABLE_EVALUATION +
			" WHERE " + COL_EVALUATION_STATUS + "=:" + STATUS +
			OFFSET_AND_LIMIT;
	
	private static final String SQL_ETAG_WITHOUT_LOCK = "SELECT " + COL_EVALUATION_ETAG + " FROM " +
														TABLE_EVALUATION +" WHERE ID = ?";
	
	private static final String SQL_ETAG_FOR_UPDATE = SQL_ETAG_WITHOUT_LOCK + " FOR UPDATE";
	
	private static final RowMapper<EvaluationDBO> rowMapper = ((new EvaluationDBO()).getTableMapping());

	private static final String EVALUATION_NOT_FOUND = "Evaluation could not be found with id :";

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public String create(Evaluation dto, Long ownerId) throws DatastoreException {
		EvaluationUtils.ensureNotNull(dto, "Evaluation object");
		EvaluationUtils.ensureNotNull(ownerId, "Owner ID");
		EvaluationUtils.ensureNotNull(dto.getId(), "Evaluation ID");
		
		// convert to DBO
		EvaluationDBO dbo = new EvaluationDBO();		
		
		// set Owner ID
		dto.setOwnerId(ownerId.toString());
		
		// serialize
		copyDtoToDbo(dto, dbo);
		
		// Generate a new eTag and CREATE message
		dbo.seteTag(UUID.randomUUID().toString());
		transactionalMessenger.sendMessageAfterCommit(dbo, ChangeType.CREATE);
				
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
	public List<Evaluation> getByContentSource(String id, long limit, long offset) 
			throws DatastoreException, NotFoundException {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(CONTENT_SOURCE, KeyFactory.stringToKey(id));
		params.addValue(OFFSET_PARAM_NAME, offset);
		params.addValue(LIMIT_PARAM_NAME, limit);	
		List<EvaluationDBO> dbos = simpleJdbcTemplate.query(SELECT_BY_CONTENT_SOURCE, rowMapper, params);
		List<Evaluation> dtos = new ArrayList<Evaluation>();
		copyDbosToDtos(dbos, dtos);
		return dtos;
	}

	@Override
	public List<Evaluation> getInRange(long limit, long offset) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(OFFSET_PARAM_NAME, offset);
		param.addValue(LIMIT_PARAM_NAME, limit);	
		List<EvaluationDBO> dbos = simpleJdbcTemplate.query(SELECT_ALL_SQL_PAGINATED, rowMapper, param);
		List<Evaluation> dtos = new ArrayList<Evaluation>();
		copyDbosToDtos(dbos, dtos);
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
		copyDbosToDtos(dbos, dtos);
		return dtos;
	}

	@Override
	public long getCount() throws DatastoreException {
		return basicDao.getCount(EvaluationDBO.class);
	}
	
	@Override
	public long getCountByContentSource(String id) throws DatastoreException {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(CONTENT_SOURCE, KeyFactory.stringToKey(id));
		return simpleJdbcTemplate.queryForLong(COUNT_BY_CONTENT_SOURCE, params);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void update(Evaluation dto)
			throws DatastoreException, InvalidModelException,
			NotFoundException, ConflictingUpdateException {
		EvaluationDBO dbo = new EvaluationDBO();
		copyDtoToDbo(dto, dbo);
		verifyEvaluationDBO(dbo);
		
		String newEtag = lockAndGenerateEtag(dbo.getIdString(), dbo.getEtag(), ChangeType.UPDATE);	
		dbo.seteTag(newEtag);
		
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
		dto.setEtag(dbo.getEtag());
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
	
	protected static void copyDbosToDtos(List<EvaluationDBO> dbos, List<Evaluation> dtos) throws DatastoreException {
		for (EvaluationDBO dbo : dbos) {
			Evaluation dto = new Evaluation();
			copyDboToDto(dbo, dto);
			dtos.add(dto);
		}
	}

	/**
	 * Ensure that a EvaluationDBO object has all required components
	 * 
	 * @param dbo
	 */
	private void verifyEvaluationDBO(EvaluationDBO dbo) {
		EvaluationUtils.ensureNotNull(dbo.getId(), "ID");
		EvaluationUtils.ensureNotNull(dbo.getEtag(), "etag");
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
		dbo.seteTag(UUID.randomUUID().toString());
		transactionalMessenger.sendMessageAfterCommit(dbo, changeType);
		return dbo.getEtag();
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
	
	private static final String SELECT_AVAILABLE_EVALUATIONS_PAGINATED_PREFIX =
		"SELECT DISTINCT e.* " + AuthorizationSqlUtil.AUTHORIZATION_SQL_FROM+", "+TABLE_EVALUATION+" e ";
	
	// parameters here are LIMIT and OFFSET
	private static final String SELECT_AVAILABLE_EVALUATIONS_PAGINATED_SUFFIX =
			" and e."+COL_EVALUATION_ID+"=acl."+COL_ACL_OWNER_ID+
			" and acl."+COL_ACL_OWNER_TYPE+"='"+ObjectType.EVALUATION.name()+
			"' and acl."+COL_ACL_ID+"=ra."+COL_RESOURCE_ACCESS_OWNER+
			" ORDER BY e."+COL_EVALUATION_NAME+" LIMIT :"+LIMIT_PARAM_NAME+" OFFSET :"+OFFSET_PARAM_NAME;
	
	private static final String SELECT_AVAILABLE_EVALUATIONS_COUNT_PREFIX =
		"SELECT count(distinct e."+COL_EVALUATION_ID+") " + AuthorizationSqlUtil.AUTHORIZATION_SQL_FROM+", "+TABLE_EVALUATION+" e ";

	private static final String SELECT_AVAILABLE_EVALUATIONS_COUNT_SUFFIX =
			" and e."+COL_EVALUATION_ID+"=acl."+COL_ACL_OWNER_ID+
			" and acl."+COL_ACL_OWNER_TYPE+"='"+ObjectType.EVALUATION.name()+
			"' and acl."+COL_ACL_ID+"=ra."+COL_RESOURCE_ACCESS_OWNER;

	/**
	 * return the evaluations in which the user (given as a list of principal Ids)
	 * is either the owner or is a participant
	 */
	@Override
	public List<Evaluation> getAvailableInRange(List<Long> principalIds, long limit, long offset) throws DatastoreException {
		if (principalIds.isEmpty()) return new ArrayList<Evaluation>(); // SQL breaks down if list is empty
		MapSqlParameterSource param = new MapSqlParameterSource();
		// parameters here are 
		//	BIND_VAR_PREFIX, appended with 0,1,2,... (to bind group id)
		//	AuthorizationSqlUtil.ACCESS_TYPE_BIND_VAR (to bind ACCESS_TYPE, e.g. 'SUBMIT')
		for (int i=0; i<principalIds.size(); i++) {
			param.addValue(AuthorizationSqlUtil.BIND_VAR_PREFIX+i, principalIds.get(i));	
		}
		param.addValue(AuthorizationSqlUtil.ACCESS_TYPE_BIND_VAR, ACCESS_TYPE.SUBMIT.name());
		param.addValue(OFFSET_PARAM_NAME, offset);
		param.addValue(LIMIT_PARAM_NAME, limit);	
		param.addValue(AuthorizationSqlUtil.RESOURCE_TYPE_BIND_VAR, ObjectType.EVALUATION.name());
		StringBuilder sql = new StringBuilder(SELECT_AVAILABLE_EVALUATIONS_PAGINATED_PREFIX);
		sql.append(AuthorizationSqlUtil.authorizationSQLWhere(principalIds.size()));
		sql.append(SELECT_AVAILABLE_EVALUATIONS_PAGINATED_SUFFIX);

		List<EvaluationDBO> dbos = simpleJdbcTemplate.query(sql.toString(), rowMapper, param);
		List<Evaluation> dtos = new ArrayList<Evaluation>();
		copyDbosToDtos(dbos, dtos);
		return dtos;
	}

	@Override
	public long getAvailableCount(List<Long> principalIds) throws DatastoreException {
		if (principalIds.isEmpty()) return 0L; // SQL breaks down if list is empty
		MapSqlParameterSource param = new MapSqlParameterSource();
		// parameters here are 
		//	BIND_VAR_PREFIX, appended with 0,1,2,... (to bind group id)
		//	AuthorizationSqlUtil.ACCESS_TYPE_BIND_VAR (to bind ACCESS_TYPE, e.g. 'SUBMIT')
		for (int i=0; i<principalIds.size(); i++) {
			param.addValue(AuthorizationSqlUtil.BIND_VAR_PREFIX+i, principalIds.get(i));	
		}
		param.addValue(AuthorizationSqlUtil.ACCESS_TYPE_BIND_VAR, ACCESS_TYPE.SUBMIT.name());
		param.addValue(AuthorizationSqlUtil.RESOURCE_TYPE_BIND_VAR, ObjectType.EVALUATION.name());
		StringBuilder sql = new StringBuilder(SELECT_AVAILABLE_EVALUATIONS_COUNT_PREFIX);
		sql.append(AuthorizationSqlUtil.authorizationSQLWhere(principalIds.size())); 
		sql.append(SELECT_AVAILABLE_EVALUATIONS_COUNT_SUFFIX);
		return simpleJdbcTemplate.queryForLong(sql.toString(), param);
	}

	
}
