package org.sagebionetworks.evaluation.dao;

import static org.sagebionetworks.repo.model.jdo.AuthorizationSqlUtil.ACCESS_TYPE_BIND_VAR;
import static org.sagebionetworks.repo.model.jdo.AuthorizationSqlUtil.AUTHORIZATION_SQL_JOIN;
import static org.sagebionetworks.repo.model.jdo.AuthorizationSqlUtil.AUTHORIZATION_SQL_TABLES;
import static org.sagebionetworks.repo.model.jdo.AuthorizationSqlUtil.BIND_VAR_PREFIX;
import static org.sagebionetworks.repo.model.jdo.AuthorizationSqlUtil.RESOURCE_TYPE_BIND_VAR;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_END_TIMESTAMP;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_ETAG;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_NAME;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_ROUND_ETAG;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_ROUND_EVALUATION_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_ROUND_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_ROUND_ROUND_END;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_ROUND_ROUND_START;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_EVALUATION_START_TIMESTAMP;
import static org.sagebionetworks.repo.model.query.SQLConstants.LIMIT_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.SQLConstants.OFFSET_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.SQLConstants.TABLE_EVALUATION;
import static org.sagebionetworks.repo.model.query.SQLConstants.TABLE_EVALUATION_ROUND;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACL_OWNER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACL_OWNER_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_OWNER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_TYPE_ELEMENT;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.sagebionetworks.evaluation.dbo.DBOConstants;
import org.sagebionetworks.evaluation.dbo.EvaluationDBO;
import org.sagebionetworks.evaluation.dbo.EvaluationDBOUtil;
import org.sagebionetworks.evaluation.dbo.EvaluationRoundDBO;
import org.sagebionetworks.evaluation.dbo.EvaluationRoundDBOUtil;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationRound;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.SinglePrimaryKeySqlParameterSource;
import org.sagebionetworks.repo.model.query.SQLConstants;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

public class EvaluationDAOImpl implements EvaluationDAO {
	
	@Autowired
	private DBOBasicDao basicDao;
	
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private NamedParameterJdbcTemplate namedJdbcTemplate;
	
	private static final String ID = DBOConstants.PARAM_EVALUATION_ID;
	private static final String NAME = DBOConstants.PARAM_EVALUATION_NAME;
	private static final String CONTENT_SOURCE = DBOConstants.PARAM_EVALUATION_CONTENT_SOURCE;
	
	private static final String SELECT_AVAILABLE_EVALUATIONS_PAGINATED_PREFIX =
			"SELECT DISTINCT e.* FROM " + AUTHORIZATION_SQL_TABLES+", "+TABLE_EVALUATION+" e ";
		
	private static final String AUTHORIZATION_SQL_WHERE_1 = 
			"where (ra."+COL_RESOURCE_ACCESS_GROUP_ID+
			" in (";

	private static final String AUTHORIZATION_SQL_WHERE_2 = 
			")) AND "+AUTHORIZATION_SQL_JOIN+
		    " and acl."+COL_ACL_OWNER_TYPE+"=:"+RESOURCE_TYPE_BIND_VAR+
			" and at."+COL_RESOURCE_ACCESS_TYPE_ELEMENT+"=:"+ACCESS_TYPE_BIND_VAR;

	private static final String SELECT_AVAILABLE_EVALUATIONS_PAGINATED_SUFFIX =
			" and e."+COL_EVALUATION_ID+"=acl."+COL_ACL_OWNER_ID+
			" and acl."+COL_ACL_OWNER_TYPE+"='"+ObjectType.EVALUATION.name()+
			"' and acl."+COL_ACL_ID+"=ra."+COL_RESOURCE_ACCESS_OWNER+
			" ORDER BY e."+COL_EVALUATION_NAME+" LIMIT :"+LIMIT_PARAM_NAME+" OFFSET :"+OFFSET_PARAM_NAME;
	
	private static final String SELECT_AVAILABLE_EVALUATIONS_FILTER =
			" and e."+COL_EVALUATION_ID+" in (:"+COL_EVALUATION_ID+")";
	
	private static final String SELECT_AVAILABLE_CONTENT_SOURCE_FILTER =
			" and "+SQLConstants.COL_EVALUATION_CONTENT_SOURCE+"=:"+CONTENT_SOURCE;
	
	private static final String CURRENT_TIME_PARAM_NAME = "currentTime";
	
	private static final String SELECT_TIME_RANGE_FILTER = 
			" and ("+COL_EVALUATION_START_TIMESTAMP+" IS NULL OR :"+CURRENT_TIME_PARAM_NAME+">="+COL_EVALUATION_START_TIMESTAMP+
			") and ("+COL_EVALUATION_END_TIMESTAMP+" IS NULL OR :"+CURRENT_TIME_PARAM_NAME+"<"+COL_EVALUATION_END_TIMESTAMP+")";
	
	private static final String SELECT_BY_NAME_SQL = 
			"SELECT ID FROM "+ TABLE_EVALUATION +
			" WHERE "+ COL_EVALUATION_NAME + "=:" + NAME;
	

	private static final String SQL_ETAG_FOR_UPDATE_FORMAT = "SELECT %s FROM %s WHERE ID = ? FOR UPDATE";
	
	private static final String SQL_SELECT_AVAILABLE = "SELECT " + COL_EVALUATION_ID 
			+ " FROM " + TABLE_EVALUATION + " WHERE " + COL_EVALUATION_ID + " IN (:" + ID + ")";

	private static final String SELECT_ALL_ROUNDS_FOR_EVALUATION = "SELECT * FROM " + TABLE_EVALUATION_ROUND +
			" WHERE " + COL_EVALUATION_ROUND_EVALUATION_ID + "= :" + DBOConstants.PARAM_EVALUATION_ROUND_EVALUATION_ID +
			" ORDER BY " + COL_EVALUATION_ROUND_ROUND_START + " ASC " +
			" LIMIT :" + DBOConstants.PARAM_LIMIT + " OFFSET :" + DBOConstants.PARAM_OFFSET;

	private static final String PARAM_BETWEEN_DATE = "betweenDate";
	private static final String SELECT_ROUND_BETWEEN_RANGE = "SELECT * FROM " + TABLE_EVALUATION_ROUND +
			" WHERE " + COL_EVALUATION_ROUND_EVALUATION_ID + "= :" + DBOConstants.PARAM_EVALUATION_ROUND_EVALUATION_ID +
			" AND " + COL_EVALUATION_ROUND_ROUND_START + "<= :"+PARAM_BETWEEN_DATE +" AND " + COL_EVALUATION_ROUND_ROUND_END + " > :"+PARAM_BETWEEN_DATE;

	private static final RowMapper<EvaluationRoundDBO> EVALUATION_ROUND_ROW_MAPPER = new EvaluationRoundDBO().getTableMapping();

	private static final RowMapper<EvaluationDBO> evaluationRowMapper = ((new EvaluationDBO()).getTableMapping());

	private static final String EVALUATION_NOT_FOUND = "Evaluation could not be found with id :";

	private static final String EVALUATION_ROUND_NOT_FOUND_FORMAT = "Evaluation Round with id=%s, belonging to Evaluation id=%s, could not be found.";

	@Override
	@WriteTransaction
	public String create(Evaluation dto, Long ownerId) throws DatastoreException {
		ValidateArgument.required(dto, "Evaluation object");
		ValidateArgument.required(ownerId, "Owner ID");
		ValidateArgument.required(dto.getId(), "Evaluation ID");
		
		// convert to DBO
		EvaluationDBO dbo = new EvaluationDBO();		
		
		// set Owner ID
		dto.setOwnerId(ownerId.toString());
		
		// Generate a new eTag and CREATE message
		dto.setEtag(UUID.randomUUID().toString());
				
		// serialize
		EvaluationDBOUtil.copyDtoToDbo(dto, dbo);
		
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
		try {
			EvaluationDBO dbo = basicDao.getObjectByPrimaryKey(EvaluationDBO.class, param);
			Evaluation dto = new Evaluation();
			EvaluationDBOUtil.copyDboToDto(dbo, dto);
			return dto;
		} catch (NotFoundException e) {
			throw new NotFoundException(EVALUATION_NOT_FOUND + id);
		}
	}

	private static final String AUTHORIZATION_SQL_WHERE = 
			AUTHORIZATION_SQL_WHERE_1+
			":"+
			BIND_VAR_PREFIX+
			AUTHORIZATION_SQL_WHERE_2;
	
	@Override
	public List<Evaluation> getAccessibleEvaluations(EvaluationFilter filter, long limit, long offset) throws DatastoreException, NotFoundException {
		ValidateArgument.required(filter, "filter");
		
		if (filter.getPrincipalIds() == null || filter.getPrincipalIds().isEmpty()) {
			return Collections.emptyList();
		}
		
		MapSqlParameterSource param = new MapSqlParameterSource();
		
		param.addValue(BIND_VAR_PREFIX, filter.getPrincipalIds());
		param.addValue(ACCESS_TYPE_BIND_VAR, filter.getAccessType().name());
		param.addValue(OFFSET_PARAM_NAME, offset);
		param.addValue(LIMIT_PARAM_NAME, limit);	
		param.addValue(RESOURCE_TYPE_BIND_VAR, ObjectType.EVALUATION.name());
		
		StringBuilder sql = new StringBuilder(SELECT_AVAILABLE_EVALUATIONS_PAGINATED_PREFIX);
		sql.append(AUTHORIZATION_SQL_WHERE);		

		if (filter.getIdsFilter() !=null && !filter.getIdsFilter().isEmpty()) {
			param.addValue(COL_EVALUATION_ID, filter.getIdsFilter());
			sql.append(SELECT_AVAILABLE_EVALUATIONS_FILTER);
		}
		
		if (filter.getContentSourceFilter() != null) {
			param.addValue(CONTENT_SOURCE, filter.getContentSourceFilter());
			sql.append(SELECT_AVAILABLE_CONTENT_SOURCE_FILTER);
		}
		
		if (filter.getTimeFilter() != null) {
			sql.append(SELECT_TIME_RANGE_FILTER);
			param.addValue(CURRENT_TIME_PARAM_NAME, filter.getTimeFilter());
		}
		
		sql.append(SELECT_AVAILABLE_EVALUATIONS_PAGINATED_SUFFIX);

		return namedJdbcTemplate.query(sql.toString(), param, (ResultSet rs, int rowNum) -> {
			EvaluationDBO dbo = evaluationRowMapper.mapRow(rs, rowNum);
			Evaluation dto = new Evaluation();
			EvaluationDBOUtil.copyDboToDto(dbo, dto);
			return dto;
		});
	}

	@Override
	@WriteTransaction
	public void update(Evaluation dto)
			throws DatastoreException, InvalidModelException,
			NotFoundException, ConflictingUpdateException {
		// we do this to preserve the EvaluationSubmissions etag
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID, dto.getId());
		String newEtag = lockAndGenerateEtag(dto.getId(), dto.getEtag(),
				TABLE_EVALUATION, COL_EVALUATION_ETAG, Evaluation.class);
		dto.setEtag(newEtag);
		
		EvaluationDBO dbo = new EvaluationDBO();
		EvaluationDBOUtil.copyDtoToDbo(dto, dbo);
		
		basicDao.update(dbo);
	}

	@Override
	@WriteTransaction
	public void delete(String id) throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID, id);
		basicDao.deleteObjectByPrimaryKey(EvaluationDBO.class, param);
	}

	@Override
	public String lookupByName(String name) throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(NAME, name);
		try {
			Long id = namedJdbcTemplate.queryForObject(SELECT_BY_NAME_SQL, param, Long.class);
			return id.toString();
		} catch (EmptyResultDataAccessException e) {
			// name is not in use
			return null;
		}
	}

	@Override
	public Set<Long> getAvailableEvaluations(List<Long> ids) {
		ValidateArgument.required(ids, "ids");
		
		if (ids.isEmpty()) {
			return Collections.emptySet();
		}
		
		MapSqlParameterSource param = new MapSqlParameterSource(ID, ids);
		
		Set<Long> result = new HashSet<>(ids.size());
		
		namedJdbcTemplate.query(SQL_SELECT_AVAILABLE, param, (ResultSet rs) -> {
			result.add(rs.getLong(COL_EVALUATION_ID));
		});
		
		return result;
	}


	private String lockAndGenerateEtag(String id, String eTag, String tableName, String etagColName, Class<?> clazz)
			throws NotFoundException, ConflictingUpdateException, DatastoreException {
		// Create a Select for update query
		String sql = String.format(SQL_ETAG_FOR_UPDATE_FORMAT, etagColName, tableName);
		String currentTag = jdbcTemplate.queryForObject(sql, String.class, id);
		// Check the eTags
		if(!currentTag.equals(eTag)){
			throw new ConflictingUpdateException(clazz.getSimpleName() + ": " + id + " has been updated since " +
					"you last fetched it; please retrieve it again and re-apply the update");
		}
		// Generate a new e-tag
		return UUID.randomUUID().toString();
	}

	@Override
	@WriteTransaction
	public EvaluationRound createEvaluationRound(EvaluationRound evaluationRound){
		//generate initial etag
		evaluationRound.setEtag(UUID.randomUUID().toString());
		EvaluationRoundDBO dbo = EvaluationRoundDBOUtil.toDBO(evaluationRound);

		// create DBO
		dbo = basicDao.createNew(dbo);
		return EvaluationRoundDBOUtil.toDTO(dbo);
	}

	@Override
	@WriteTransaction
	public void updateEvaluationRound(EvaluationRound evaluationRound) {
		EvaluationRoundDBO dbo = EvaluationRoundDBOUtil.toDBO(evaluationRound);

		try{
			//update etag
			String newEtag = lockAndGenerateEtag(dbo.getId().toString(), dbo.getEtag(),
					TABLE_EVALUATION_ROUND, COL_EVALUATION_ROUND_ETAG, EvaluationRound.class);
			dbo.setEtag(newEtag);
		} catch (EmptyResultDataAccessException e){
			throw new NotFoundException(String.format(EVALUATION_ROUND_NOT_FOUND_FORMAT, evaluationRound.getId(), evaluationRound.getEvaluationId()), e);
		}


		// create DBO
		if( ! basicDao.update(dbo) ){
			throw new NotFoundException(String.format(EVALUATION_ROUND_NOT_FOUND_FORMAT, evaluationRound.getId(), evaluationRound.getEvaluationId()));
		}

	}

	@Override
	@WriteTransaction
	public void deleteEvaluationRound(String evaluationId, String evaluationRoundId) {
		SqlParameterSource param = new SinglePrimaryKeySqlParameterSource(evaluationRoundId);
		if( ! basicDao.deleteObjectByPrimaryKey(EvaluationRoundDBO.class, param) ){
			throw new NotFoundException(String.format(EVALUATION_ROUND_NOT_FOUND_FORMAT, evaluationRoundId, evaluationId));
		}
	}

	@Override
	public EvaluationRound getEvaluationRound(String evaluationId, String evaluationRoundId) {
		SqlParameterSource param = new SinglePrimaryKeySqlParameterSource(evaluationRoundId);

		EvaluationRoundDBO dbo = basicDao.getObjectByPrimaryKeyIfExists(EvaluationRoundDBO.class, param).orElseThrow(
				() -> new NotFoundException(String.format(EVALUATION_ROUND_NOT_FOUND_FORMAT, evaluationRoundId, evaluationId))
		);
		EvaluationRound dto = EvaluationRoundDBOUtil.toDTO(dbo);
		return dto;
	}

	@Override
	public List<EvaluationRound> getAssociatedEvaluationRounds(String evaluationId, long limit, long offset) {
		MapSqlParameterSource sqlParameterSource = new MapSqlParameterSource();
		sqlParameterSource.addValue(DBOConstants.PARAM_EVALUATION_ROUND_EVALUATION_ID, evaluationId);
		sqlParameterSource.addValue(DBOConstants.PARAM_LIMIT, limit);
		sqlParameterSource.addValue(DBOConstants.PARAM_OFFSET, offset);


		return namedJdbcTemplate.query(SELECT_ALL_ROUNDS_FOR_EVALUATION, sqlParameterSource, (ResultSet resultSet, int rowNumber) ->{
			return EvaluationRoundDBOUtil.toDTO(EVALUATION_ROUND_ROW_MAPPER.mapRow(resultSet, rowNumber));
		});
	}

	@Override
	public Optional<EvaluationRound> getEvaluationRoundForTimestamp(String evaluationId, Instant timestamp) {
		MapSqlParameterSource sqlParameterSource = new MapSqlParameterSource();
		sqlParameterSource.addValue(DBOConstants.PARAM_EVALUATION_ROUND_EVALUATION_ID, evaluationId);
		sqlParameterSource.addValue(PARAM_BETWEEN_DATE, Timestamp.from(timestamp));

		try {
			return namedJdbcTemplate.queryForObject(SELECT_ROUND_BETWEEN_RANGE, sqlParameterSource,
					(ResultSet resultSet, int rowNumber) -> {
						return Optional.of(EvaluationRoundDBOUtil.toDTO(EVALUATION_ROUND_ROW_MAPPER.mapRow(resultSet, rowNumber)));
					}
			);
		} catch (EmptyResultDataAccessException e){
			return Optional.empty();
		}
	}

	@Override
	public boolean hasEvaluationRounds(String evaluationId){
		return jdbcTemplate.queryForObject("SELECT COUNT(*) > 0 FROM " + TABLE_EVALUATION_ROUND +
						" WHERE " + COL_EVALUATION_ROUND_EVALUATION_ID +" = ? ",
				Boolean.class, evaluationId);
	}

	@Override
	public List<EvaluationRound> overlappingEvaluationRounds(String evaluationId, String currentRoundId, Instant startTimestamp, Instant endTimestamp) {
		MapSqlParameterSource sqlParameterSource = new MapSqlParameterSource();
		sqlParameterSource.addValue(DBOConstants.PARAM_EVALUATION_ROUND_EVALUATION_ID, evaluationId);
		sqlParameterSource.addValue(DBOConstants.PARAM_EVALUATION_ROUND_ID, currentRoundId);
		sqlParameterSource.addValue(DBOConstants.PARAM_EVALUATION_ROUND_ROUND_START, Timestamp.from(startTimestamp));
		sqlParameterSource.addValue(DBOConstants.PARAM_EVALUATION_ROUND_ROUND_END, Timestamp.from(endTimestamp));

		return namedJdbcTemplate.query("SELECT * FROM " + TABLE_EVALUATION_ROUND +
				" WHERE " + COL_EVALUATION_ROUND_EVALUATION_ID + "= :" + DBOConstants.PARAM_EVALUATION_ROUND_EVALUATION_ID +
				" AND "+ COL_EVALUATION_ROUND_ROUND_END + " > :" + DBOConstants.PARAM_EVALUATION_ROUND_ROUND_START +
				" AND " + COL_EVALUATION_ROUND_ROUND_START + " < :" + DBOConstants.PARAM_EVALUATION_ROUND_ROUND_END +
				" AND " + COL_EVALUATION_ROUND_ID + "<> :" + DBOConstants.PARAM_EVALUATION_ROUND_ID, sqlParameterSource,
				(ResultSet resultSet, int rowNumber) -> {
					return EvaluationRoundDBOUtil.toDTO(EVALUATION_ROUND_ROW_MAPPER.mapRow(resultSet, rowNumber));
				}
		);
	}
}
