package org.sagebionetworks.evaluation.dao;

import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_EVAL_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_ETAG;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_SUBMISSION_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_VERSION;
import static org.sagebionetworks.repo.model.query.SQLConstants.TABLE_SUBMISSION;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.sagebionetworks.evaluation.dbo.DBOConstants;
import org.sagebionetworks.evaluation.dbo.SubmissionStatusDBO;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.util.EvaluationUtils;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.evaluation.SubmissionStatusDAO;
import org.sagebionetworks.repo.model.query.SQLConstants;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class SubmissionStatusDAOImpl implements SubmissionStatusDAO {
	
	@Autowired
	private DBOBasicDao basicDao;
	
	@Autowired
	private NamedParameterJdbcTemplate namedJdbcTemplate;
	
	private static final String ID = DBOConstants.PARAM_SUBMISSION_ID;

	private static final String SQL_ETAG_FOR_UPDATE_BATCH = "SELECT " + 
			COL_SUBSTATUS_SUBMISSION_ID + " , "+ COL_SUBSTATUS_ETAG +", "+
			COL_SUBSTATUS_VERSION + " FROM " +
			SQLConstants.TABLE_SUBSTATUS +" WHERE "+
			COL_SUBSTATUS_SUBMISSION_ID+" IN (:"+COL_SUBSTATUS_SUBMISSION_ID+")" + " FOR UPDATE";

	// SELECT s.EVALUATION_ID FROM JDOSUBMISSION s WHERE s.ID IN (:ID)
	private static final String SELECT_EVALUATION_FOR_IDS = 
			"SELECT DISTINCT s."+COL_SUBMISSION_EVAL_ID+" FROM "+TABLE_SUBMISSION+
			" s WHERE s."+COL_SUBMISSION_ID+" IN (:"+COL_SUBMISSION_ID+")";

	private static final String SELECT_BY_IDS = "SELECT * FROM "+SQLConstants.TABLE_SUBSTATUS+" WHERE "+
			COL_SUBSTATUS_SUBMISSION_ID+" IN (:"+COL_SUBSTATUS_SUBMISSION_ID+")";
	
	private RowMapper<SubmissionStatusDBO> SUBSTATUS_ROW_MAPPER = (new SubmissionStatusDBO()).getTableMapping();

	@Override
	@WriteTransaction
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
	public List<SubmissionStatus> list(List<String> ids) throws DatastoreException, NotFoundException {
		if (ids==null || ids.size()<1) {
			return Collections.emptyList();
		}
		Set<String> idSet = new LinkedHashSet<String>(ids);
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_SUBSTATUS_SUBMISSION_ID, idSet);
		List<SubmissionStatusDBO> dbos = namedJdbcTemplate.query(SELECT_BY_IDS, param, SUBSTATUS_ROW_MAPPER);
		if (dbos.size()<idSet.size()) throw new NotFoundException("Expected submission statuses for "+idSet+
				" but only found results for "+dbos.size());
		List<SubmissionStatus> result = new ArrayList<SubmissionStatus>();
		for (SubmissionStatusDBO dbo : dbos) result.add(SubmissionUtils.convertDboToDto(dbo));
		return result;
	}
	
	@Override
	@WriteTransaction
	public void update(List<SubmissionStatus> batch)
			throws DatastoreException, InvalidModelException,
			NotFoundException, ConflictingUpdateException {
		Map<String,String> idToEtagMap = new HashMap<String,String>();
		List<SubmissionStatusDBO> dbos = new ArrayList<SubmissionStatusDBO>();
		for (SubmissionStatus dto : batch) {
			SubmissionStatusDBO dbo = SubmissionUtils.convertDtoToDbo(dto);
			dbo.setModifiedOn(System.currentTimeMillis());
			verifySubmissionStatusDBO(dbo);
			dbos.add(dbo);
			if (null!=idToEtagMap.put(dbo.getId().toString(), dbo.geteTag())) {
				throw new InvalidModelException(""+dbo.getId()+" occurs more than once in the SubmissonStatus batch.");
			}
		}

		// update eTag and increment the version
		Map<Long, Long> versionMap = lockForUpdateAndGetVersion(idToEtagMap);
		for (SubmissionStatusDBO dbo : dbos) {
			Long currentVersion = versionMap.get(dbo.getId());
			dbo.seteTag(UUID.randomUUID().toString());
			dbo.setVersion(currentVersion+1);
			// we also need to update the serialized field
			SubmissionStatus dto = SubmissionUtils.convertDboToDto(dbo);
			SubmissionUtils.copyToSerializedField(dto, dbo);
		}
		
		basicDao.createOrUpdateBatch(dbos);
	}
	
	@Override
	@WriteTransaction
	public void delete(String id) throws DatastoreException {
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
		EvaluationUtils.ensureNotNull(dbo.geteTag(), "eTag");
		EvaluationUtils.ensureNotNull(dbo.getModifiedOn(), "Modified date");
		EvaluationUtils.ensureNotNull(dbo.getStatusEnum(), "Submission status");
		EvaluationUtils.ensureNotNull(dbo.getVersion(), "Status version");
	}
	
	// returns a map whose key is submission Id and whose value is new Etag and new version
	private Map<Long,Long> lockForUpdateAndGetVersion(Map<String,String> idToEtagMap)
			throws NotFoundException, ConflictingUpdateException, DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_SUBSTATUS_SUBMISSION_ID, idToEtagMap.keySet());
		List<IdETagVersion> current = namedJdbcTemplate.query(SQL_ETAG_FOR_UPDATE_BATCH, param, new RowMapper<IdETagVersion>() {
			@Override
			public IdETagVersion mapRow(ResultSet rs, int rowNum) throws SQLException {
				return new IdETagVersion(
						rs.getLong(COL_SUBSTATUS_SUBMISSION_ID), 
						rs.getString(COL_SUBSTATUS_ETAG), 
						rs.getLong(COL_SUBSTATUS_VERSION));
			}
		});
		// Check the eTags
		Map<Long,Long> result = new HashMap<Long,Long>();
		for (IdETagVersion ev : current) {
			String id = ev.getId().toString();
			String etagFromClient = idToEtagMap.get(id);
			String currentEtag = ev.getEtag();
			if(!currentEtag.equals(etagFromClient)) {
				throw new ConflictingUpdateException("Submission Status: "+id+" was updated since you last fetched it, retrieve it again and reapply the update");
			}
			result.put(ev.getId(), ev.getVersion());
		}
		
		return result;
	}
	
	@Override
	public long getCount() throws DatastoreException {
		return basicDao.getCount(SubmissionStatusDBO.class);
	}
	
	@Override
	public Long getEvaluationIdForBatch(List<SubmissionStatus> batch)
			throws DatastoreException, InvalidModelException,
			NotFoundException, ConflictingUpdateException {
		Set<String> ids = new HashSet<String>();
		for (SubmissionStatus s : batch) {
			if (s.getId()!=null) ids.add(s.getId());
		}
		if (ids.isEmpty()) throw new IllegalArgumentException("SubmissionStatus batch has no Submission Ids.");
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_SUBMISSION_ID, ids);
		try {
			return namedJdbcTemplate.queryForObject(SELECT_EVALUATION_FOR_IDS, param, Long.class);
		} catch (EmptyResultDataAccessException erda) {
			throw new NotFoundException("Submissions are not found in the system.");
		} catch (IncorrectResultSizeDataAccessException irsdae) {
			throw new IllegalArgumentException("Submission batch must be for a single Evaluation queue.", irsdae);
			
		}
	}
}
