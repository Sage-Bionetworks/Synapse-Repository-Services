package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VERIFICATION_FILE_FILEHANDLEID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VERIFICATION_FILE_VERIFICATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VERIFICATION_STATE_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VERIFICATION_STATE_STATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VERIFICATION_STATE_VERIFICATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VERIFICATION_SUBMISSION_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VERIFICATION_SUBMISSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_VERIFICATION_FILE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_VERIFICATION_STATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_VERIFICATION_SUBMISSION;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.VerificationDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.persistence.DBOVerificationState;
import org.sagebionetworks.repo.model.dbo.persistence.DBOVerificationSubmission;
import org.sagebionetworks.repo.model.dbo.persistence.DBOVerificationSubmissionFile;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.verification.VerificationState;
import org.sagebionetworks.repo.model.verification.VerificationStateEnum;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;



public class DBOVerificationDAOImpl implements VerificationDAO {
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private DBOBasicDao basicDao;
	
	@Autowired
	private IdGenerator idGenerator;

	private static final String VERIFICATION_SUBMISSION_CORE = "FROM "+TABLE_VERIFICATION_SUBMISSION;
	
	// select verifications whose latest/newest state has the given state value
	private static final String VERIFICATION_SUBMISSION_WITH_STATE_CORE = 
			"FROM "+TABLE_VERIFICATION_SUBMISSION+" v, "+TABLE_VERIFICATION_STATE+" s "+
			"WHERE s."+COL_VERIFICATION_STATE_VERIFICATION_ID+"=v."+COL_VERIFICATION_SUBMISSION_ID+
			" AND s."+COL_VERIFICATION_STATE_CREATED_ON+
			" = (SELECT MAX("+COL_VERIFICATION_STATE_CREATED_ON+") FROM "+TABLE_VERIFICATION_STATE+
			" s2 WHERE s2."+COL_VERIFICATION_STATE_VERIFICATION_ID+"=v."+COL_VERIFICATION_SUBMISSION_ID+") "+
			"AND s."+COL_VERIFICATION_STATE_STATE+" in (:"+COL_VERIFICATION_STATE_STATE+")";
		
	private static final String USER_ID_FILTER = COL_VERIFICATION_SUBMISSION_CREATED_BY+"=:"+COL_VERIFICATION_SUBMISSION_CREATED_BY;
	
	private static final String LIMIT = "LIMIT";
	private static final String OFFSET = "OFFSET";
	private static final String LIMIT_OFFSET =" "+LIMIT+" :"+LIMIT+" "+OFFSET+" :"+OFFSET;
	
	private static final String VERIFICATION_STATE_SQL = 
			"SELECT * FROM "+TABLE_VERIFICATION_STATE+
			" WHERE "+COL_VERIFICATION_STATE_VERIFICATION_ID+" in (:"+COL_VERIFICATION_STATE_VERIFICATION_ID+") ORDER BY "+
			COL_VERIFICATION_STATE_CREATED_ON+" ASC";
	
	private static final String FILE_ID_IN_VERIFICATION_SQL = 
			"SELECT COUNT(*) FROM "+TABLE_VERIFICATION_FILE+" WHERE "+
			COL_VERIFICATION_FILE_VERIFICATION_ID+"=? AND "+COL_VERIFICATION_FILE_FILEHANDLEID+"=?";

	private static TableMapping<DBOVerificationSubmission> DBO_VERIFICATION_SUB_MAPPING =
			(new DBOVerificationSubmission()).getTableMapping();
	
	private static TableMapping<DBOVerificationState> DBO_VERIFICATION_STATE_MAPPING =
			(new DBOVerificationState()).getTableMapping();
	
	@WriteTransaction
	@Override
	public VerificationSubmission createVerificationSubmission(
			VerificationSubmission dto) throws DatastoreException {
		dto.setId(idGenerator.generateNewId(TYPE.VERIFICATION_SUBMISSION_ID).toString());
		DBOVerificationSubmission dbo = copyVerificationDTOtoDBO(dto);
		DBOVerificationSubmission created = basicDao.createNew(dbo);
		VerificationState initialState = new VerificationState();
		initialState.setCreatedBy(dto.getCreatedBy());
		initialState.setCreatedOn(dto.getCreatedOn());
		initialState.setState(VerificationStateEnum.SUBMITTED);
		appendVerificationSubmissionState(dbo.getId(), initialState);
		storeFileHandleIds(dbo.getId(), dto.getFiles());
		return copyVerificationDBOtoDTO(created, Collections.singletonList(initialState));
	}
	
	private static DBOVerificationSubmission copyVerificationDTOtoDBO(VerificationSubmission dto) {
		DBOVerificationSubmission dbo = new DBOVerificationSubmission();
		dbo.setCreatedBy(Long.parseLong(dto.getCreatedBy()));
		dbo.setCreatedOn(dto.getCreatedOn().getTime());
		dbo.setId(Long.parseLong(dto.getId()));
		try {
			dbo.setSerialized(JDOSecondaryPropertyUtils.compressObject(dto));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return dbo;
	}
	
	private static VerificationSubmission copyVerificationDBOtoDTO(DBOVerificationSubmission dbo, List<VerificationState> stateHistory) {
		VerificationSubmission dto = new VerificationSubmission();
		try {
			dto = (VerificationSubmission)JDOSecondaryPropertyUtils.decompressedObject(dbo.getSerialized());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		dto.setStateHistory(stateHistory);
		return dto;
	}
	
	private void storeFileHandleIds(Long verificationSubmissionId, List<String> fileHandleIds) {
		if (fileHandleIds==null || fileHandleIds.isEmpty()) return;
		List<DBOVerificationSubmissionFile> batch = new ArrayList<DBOVerificationSubmissionFile>();
		for (String fileHandleId : fileHandleIds) {
			DBOVerificationSubmissionFile sf = new DBOVerificationSubmissionFile();
			sf.setVerificationId(verificationSubmissionId);
			sf.setFileHandleId(Long.parseLong(fileHandleId));
			batch.add(sf);
		}
		basicDao.createBatch(batch);
	}
	
	@WriteTransaction
	@Override
	public void deleteVerificationSubmission(String id) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_VERIFICATION_SUBMISSION_ID.toLowerCase(), id);
		basicDao.deleteObjectByPrimaryKey(DBOVerificationSubmission.class, param);
	}

	@WriteTransaction
	@Override
	public void appendVerificationSubmissionState(long verificationSubmissionId, VerificationState newState) {
		DBOVerificationState dbo = copyVerificationStateDTOtoDBO(verificationSubmissionId, newState);
		basicDao.createNew(dbo);
	}
	
	private static String listVerificationSubmissionsSQLcore(List<VerificationStateEnum> states, Long userId) {
		String sql;
		if (states==null || states.isEmpty()) {
			sql = VERIFICATION_SUBMISSION_CORE;
			if (userId!=null) {
				sql += " WHERE "+USER_ID_FILTER;
			}
		} else {
			sql = VERIFICATION_SUBMISSION_WITH_STATE_CORE;
			if (userId!=null) {
				sql += " AND v."+USER_ID_FILTER;
			}
		}
		return sql;
	}
	
	private static MapSqlParameterSource listVerificationSubmissionsParams(List<VerificationStateEnum> states, Long userId) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		if (states!=null) {
			List<String> stateNames = new ArrayList<String>();
			for (VerificationStateEnum state : states) stateNames.add(state.name());
			param.addValue(COL_VERIFICATION_STATE_STATE, stateNames);
		}
		if (userId!=null) {
			param.addValue(COL_VERIFICATION_SUBMISSION_CREATED_BY, userId);
		}
		return param;
	}
	
	@Override
	public List<VerificationSubmission> listVerificationSubmissions(
			List<VerificationStateEnum> states, Long userId, long limit, long offset) {
		String sql = "SELECT * "+listVerificationSubmissionsSQLcore(states, userId)+LIMIT_OFFSET;
		MapSqlParameterSource param = listVerificationSubmissionsParams(states, userId);
		param.addValue(LIMIT, limit);
		param.addValue(OFFSET, offset);
		
		NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
		List<DBOVerificationSubmission> dbos =
				namedTemplate.query(sql, param, DBO_VERIFICATION_SUB_MAPPING);
		
		List<VerificationSubmission> result = new ArrayList<VerificationSubmission>();
		if (dbos.isEmpty()) return result;
		Set<Long> verificationIds = new HashSet<Long>();
		for (DBOVerificationSubmission dbo : dbos) verificationIds.add(dbo.getId());
		Map<Long,List<VerificationState>> stateMap = getVerificationStates(verificationIds);
		for (DBOVerificationSubmission dbo : dbos) {
			List<VerificationState> stateHistory = stateMap.get(dbo.getId());
			if (stateHistory==null) throw new IllegalStateException("Failed to retrieve state history for submission verification "+dbo.getId());
			result.add(copyVerificationDBOtoDTO(dbo, stateHistory));
		}
		return result;
	}
	
	private Map<Long,List<VerificationState>> getVerificationStates(Set<Long> verificationIds) {
		NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_VERIFICATION_STATE_VERIFICATION_ID, verificationIds);
		final Map<Long,List<VerificationState>> result = new HashMap<Long,List<VerificationState>>();
		namedTemplate.query(VERIFICATION_STATE_SQL, param, new RowMapper<VerificationState>() {

			@Override
			public VerificationState mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				DBOVerificationState state = DBO_VERIFICATION_STATE_MAPPING.mapRow(rs, rowNum);
				List<VerificationState> states = result.get(state.getVerificationId());
				if (states==null) {
					states = new ArrayList<VerificationState>();
					result.put(state.getVerificationId(), states);
				}
				states.add(copyVerificationStateDBOtoDTO(state));
				return null;
			}
			
		});
		return result;
	}

	@Override
	public long countVerificationSubmissions(List<VerificationStateEnum> states,
			Long userId) {
		String sql = "SELECT COUNT(*) "+listVerificationSubmissionsSQLcore(states, userId);
		MapSqlParameterSource param = listVerificationSubmissionsParams(states, userId);
		NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
		return namedTemplate.queryForObject(sql, param, Long.class);
	}
	
	private static final String REASON_CHARACTER_SET = "UTF-8";

	public static DBOVerificationState copyVerificationStateDTOtoDBO(long verificationSubmissionId, VerificationState dto) {
		DBOVerificationState dbo = new DBOVerificationState();
		dbo.setCreatedBy(Long.parseLong(dto.getCreatedBy()));
		dbo.setCreatedOn(dto.getCreatedOn().getTime());
		if (dto.getReason()==null) {
			dbo.setReason(null);
		} else {
			try {
				dbo.setReason((dto.getReason().getBytes(REASON_CHARACTER_SET)));
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}
		dbo.setState(dto.getState());
		dbo.setVerificationId(verificationSubmissionId);
		return dbo;
	}
	
	public static VerificationState copyVerificationStateDBOtoDTO(DBOVerificationState dbo) {
		VerificationState dto = new VerificationState();
		dto.setCreatedBy(dbo.getCreatedBy().toString());
		dto.setCreatedOn(new Date(dbo.getCreatedOn()));
		if (dbo.getReason()==null) {
			dto.setReason(null);
		} else {
			try {
				dto.setReason(new String(dbo.getReason(), REASON_CHARACTER_SET));
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}
		dto.setState(dbo.getState());
		return dto;
	}
	
	@Override
	public boolean isFileHandleIdInVerificationSubmission(long id,
			long fileHandleId) {
		Long count = jdbcTemplate.queryForObject(FILE_ID_IN_VERIFICATION_SQL, Long.class, id, fileHandleId);
		return count>0;
	}

}
