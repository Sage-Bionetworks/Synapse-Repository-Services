package org.sagebionetworks.repo.model.dbo.verification;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VERIFICATION_FILE_FILEHANDLEID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VERIFICATION_FILE_VERIFICATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VERIFICATION_STATE_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VERIFICATION_STATE_STATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VERIFICATION_STATE_VERIFICATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VERIFICATION_SUBMISSION_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VERIFICATION_SUBMISSION_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_VERIFICATION_SUBMISSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_VERIFICATION_FILE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_VERIFICATION_STATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_VERIFICATION_SUBMISSION;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.persistence.DBOVerificationState;
import org.sagebionetworks.repo.model.dbo.persistence.DBOVerificationSubmission;
import org.sagebionetworks.repo.model.dbo.persistence.DBOVerificationSubmissionFile;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.verification.AttachmentMetadata;
import org.sagebionetworks.repo.model.verification.VerificationState;
import org.sagebionetworks.repo.model.verification.VerificationStateEnum;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class VerificationDAOImpl implements VerificationDAO {
	
	@Autowired
	private NamedParameterJdbcTemplate namedJdbcTemplate;

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private DBOBasicDao basicDao;
	
	@Autowired
	private IdGenerator idGenerator;

	// get the latest submission for a user
	// select * from verification_submission v where 
	// v.created_by=? and 
	// v.created_on=
	// (select max(v2.created_on) from verification_submission v2 where v2.created_by=v.created_by)
	private static final String LATEST_VERIFICATION_SUBMISSION_SQL = 
		"SELECT * FROM "+TABLE_VERIFICATION_SUBMISSION+" v "+
		" WHERE v."+COL_VERIFICATION_SUBMISSION_CREATED_BY+"=? AND v."+
		COL_VERIFICATION_SUBMISSION_CREATED_ON+"=(SELECT MAX(v2."+COL_VERIFICATION_SUBMISSION_CREATED_ON+
		") FROM "+TABLE_VERIFICATION_SUBMISSION+" v2 WHERE v2."+COL_VERIFICATION_SUBMISSION_CREATED_BY+
		"=v."+COL_VERIFICATION_SUBMISSION_CREATED_BY+")";
	
	// get the latest/current state for a verification submission
	// select s.state from VERIFICATION_STATE s where
	// s.verification_id=? and 
	// s.created_on =
	// select(max(s2.created_on) from verification_state s2 where s2.verification_id=s.verification_id)
	private static final String CURRENT_VERIFICATION_STATE_SQL =
			"SELECT s."+COL_VERIFICATION_STATE_STATE+" FROM "+TABLE_VERIFICATION_STATE+
			" s WHERE s."+COL_VERIFICATION_STATE_VERIFICATION_ID+"=? AND s."+
			COL_VERIFICATION_STATE_CREATED_ON+"=(SELECT MAX(s2."+COL_VERIFICATION_STATE_CREATED_ON+
			") FROM "+TABLE_VERIFICATION_STATE+" s2 WHERE s2."+COL_VERIFICATION_STATE_VERIFICATION_ID+
			"=s."+COL_VERIFICATION_STATE_VERIFICATION_ID+")";
	
	private static final String VERIFICATION_SUBMISSION_CORE = "FROM "+TABLE_VERIFICATION_SUBMISSION;
	
	private static final String VERIFICATION_SUBMITTER_SQL = "SELECT "+COL_VERIFICATION_SUBMISSION_CREATED_BY+
			" FROM "+TABLE_VERIFICATION_SUBMISSION+" WHERE "+COL_VERIFICATION_SUBMISSION_ID+"=?";
	
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
	
	private static final String FILE_IDS_IN_VERIFICATION_SQL = 
			"SELECT "+COL_VERIFICATION_FILE_FILEHANDLEID+" FROM "+TABLE_VERIFICATION_FILE+" WHERE "+
			COL_VERIFICATION_FILE_VERIFICATION_ID+"=?";

	private static final String IDS_PARAM = "IDS";
	private static final String SQL_VALIDATED_COUNT = 
			"SELECT COUNT(DISTINCT S2."+COL_VERIFICATION_SUBMISSION_CREATED_BY+")"
			+ " FROM "+TABLE_VERIFICATION_STATE+" S JOIN "+TABLE_VERIFICATION_SUBMISSION+" S2"
			+ " ON S."+COL_VERIFICATION_STATE_VERIFICATION_ID+" = S2."+COL_VERIFICATION_SUBMISSION_ID
			+ " WHERE S2."+COL_VERIFICATION_SUBMISSION_CREATED_BY+" IN (:"+IDS_PARAM+")"
			+ " AND S."+COL_VERIFICATION_STATE_STATE+" = '"+VerificationStateEnum.APPROVED.toString()+"'"
			+ " AND S."+COL_VERIFICATION_STATE_CREATED_ON+" = ("
			+ " SELECT MAX(S3."+COL_VERIFICATION_STATE_CREATED_ON+")"
			+ " FROM "+TABLE_VERIFICATION_STATE+" S3"
			+ " WHERE S."+COL_VERIFICATION_STATE_VERIFICATION_ID+" = S3."+COL_VERIFICATION_STATE_VERIFICATION_ID+")";
	
	private static final String SQL_GET_SUBMISSION = "SELECT * FROM " + TABLE_VERIFICATION_SUBMISSION + " WHERE " + COL_VERIFICATION_SUBMISSION_ID + " = ?";

	private static final String SQL_DELETE_FILE_IDS = "DELETE FROM " + TABLE_VERIFICATION_FILE + " WHERE " + COL_VERIFICATION_FILE_VERIFICATION_ID + " = ?";
	
	private static TableMapping<DBOVerificationSubmission> DBO_VERIFICATION_SUB_MAPPING =
			new DBOVerificationSubmission().getTableMapping();
	
	private static TableMapping<DBOVerificationState> DBO_VERIFICATION_STATE_MAPPING =
			new DBOVerificationState().getTableMapping();

	private static final UnmodifiableXStream X_STREAM = UnmodifiableXStream.builder().allowTypes(VerificationSubmission.class).build();

	@WriteTransaction
	@Override
	public VerificationSubmission createVerificationSubmission(
			VerificationSubmission dto) throws DatastoreException {
		dto.setId(idGenerator.generateNewId(IdType.VERIFICATION_SUBMISSION_ID).toString());
		DBOVerificationSubmission dbo = copyVerificationDTOtoDBO(dto);
		DBOVerificationSubmission created = basicDao.createNew(dbo);
		VerificationState initialState = new VerificationState();
		initialState.setCreatedBy(dto.getCreatedBy());
		initialState.setCreatedOn(dto.getCreatedOn());
		initialState.setState(VerificationStateEnum.SUBMITTED);
		appendVerificationSubmissionState(dbo.getId(), initialState);
		storeFileHandleIds(dbo.getId(), dto.getAttachments());
		return copyVerificationDBOtoDTO(created, Collections.singletonList(initialState));
	}
	
	private static DBOVerificationSubmission copyVerificationDTOtoDBO(VerificationSubmission dto) {
		DBOVerificationSubmission dbo = new DBOVerificationSubmission();
		dbo.setCreatedBy(Long.parseLong(dto.getCreatedBy()));
		dbo.setCreatedOn(dto.getCreatedOn().getTime());
		dbo.setId(Long.parseLong(dto.getId()));
		dbo.setSerialized(serializeDTO(dto));
		dbo.setEtag(UUID.randomUUID().toString());
		return dbo;
	}
	
	private static VerificationSubmission copyVerificationDBOtoDTO(DBOVerificationSubmission dbo, List<VerificationState> stateHistory) {
		VerificationSubmission dto = deserializeDTO(dbo);
		// Avoid breaking the client
		if (dto.getAttachments() == null) {
			dto.setAttachments(Collections.emptyList());
		}
		dto.setStateHistory(stateHistory);
		return dto;
	}
	
	private static byte[] serializeDTO(VerificationSubmission dto) {
		try {
			return JDOSecondaryPropertyUtils.compressObject(X_STREAM, dto);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static VerificationSubmission deserializeDTO(DBOVerificationSubmission dbo) {
		try {
			return (VerificationSubmission)JDOSecondaryPropertyUtils.decompressObject(X_STREAM, dbo.getSerialized());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void storeFileHandleIds(Long verificationSubmissionId, List<AttachmentMetadata> attachments) {
		if (attachments==null || attachments.isEmpty()) {
			return;
		}
		List<DBOVerificationSubmissionFile> batch = new ArrayList<DBOVerificationSubmissionFile>();
		for (AttachmentMetadata attachmentMetadata : attachments) {
			DBOVerificationSubmissionFile sf = new DBOVerificationSubmissionFile();
			sf.setVerificationId(verificationSubmissionId);
			sf.setFileHandleId(Long.parseLong(attachmentMetadata.getId()));
			batch.add(sf);
		}
		basicDao.createBatch(batch);
	}
	
	@WriteTransaction
	@Override
	public void deleteVerificationSubmission(long verificationId) {
		wrapSubmissionNotFound(verificationId, () -> {
			MapSqlParameterSource param = new MapSqlParameterSource();
			param.addValue(COL_VERIFICATION_SUBMISSION_ID.toLowerCase(), verificationId);
			basicDao.deleteObjectByPrimaryKey(DBOVerificationSubmission.class, param);
			return false;
		});
	}

	@WriteTransaction
	@Override
	public void appendVerificationSubmissionState(long verificationId, VerificationState newState) {
		long stateId = idGenerator.generateNewId(IdType.VERIFICATION_SUBMISSION_ID);
		DBOVerificationState dbo = copyVerificationStateDTOtoDBO(verificationId, stateId, newState);
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
	public List<VerificationSubmission> listVerificationSubmissions(List<VerificationStateEnum> currentVerificationState, Long userId, long limit, long offset) {
		String sql = "SELECT * "+listVerificationSubmissionsSQLcore(currentVerificationState, userId)+LIMIT_OFFSET;
		MapSqlParameterSource param = listVerificationSubmissionsParams(currentVerificationState, userId);
		param.addValue(LIMIT, limit);
		param.addValue(OFFSET, offset);
		
		NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
		List<DBOVerificationSubmission> dbos = namedTemplate.query(sql, param, DBO_VERIFICATION_SUB_MAPPING);
		
		if (dbos.isEmpty())  {
			return Collections.emptyList();
		}
		
		List<VerificationSubmission> result = new ArrayList<VerificationSubmission>();

		Set<Long> verificationIds = dbos.stream().map(DBOVerificationSubmission::getId).collect(Collectors.toSet());

		Map<Long,List<VerificationState>> stateMap = getVerificationStates(verificationIds, true);
		
		for (DBOVerificationSubmission dbo : dbos) {
			List<VerificationState> stateHistory = stateMap.get(dbo.getId());
			if (stateHistory == null) {
				throw new IllegalStateException("Failed to retrieve state history for submission verification " + dbo.getId());
			}
			result.add(copyVerificationDBOtoDTO(dbo, stateHistory));
		}
		
		return result;
	}
	
	private Map<Long,List<VerificationState>> getVerificationStates(Set<Long> verificationIds, boolean includeNotes) {
		NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);

		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_VERIFICATION_STATE_VERIFICATION_ID, verificationIds);

		final Map<Long, List<VerificationState>> result = new HashMap<Long, List<VerificationState>>();

		namedTemplate.query(VERIFICATION_STATE_SQL, param, (ResultSet rs, int rowNum) -> {
			DBOVerificationState state = DBO_VERIFICATION_STATE_MAPPING.mapRow(rs, rowNum);
			List<VerificationState> states = result.get(state.getVerificationId());
			if (states == null) {
				states = new ArrayList<VerificationState>();
				result.put(state.getVerificationId(), states);
			}
			states.add(copyVerificationStateDBOtoDTO(state, includeNotes));

			// We do not need the result from the mapper
			return null;
		});

		return result;
	}

	@Override
	public long countVerificationSubmissions(List<VerificationStateEnum> states,
			Long userId) {
		String sql = "SELECT COUNT(*) " + listVerificationSubmissionsSQLcore(states, userId);
		MapSqlParameterSource param = listVerificationSubmissionsParams(states, userId);
		NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
		return namedTemplate.queryForObject(sql, param, Long.class);
	}

	public static DBOVerificationState copyVerificationStateDTOtoDBO(long verificationSubmissionId, long stateId, VerificationState dto) {
		DBOVerificationState dbo = new DBOVerificationState();
		dbo.setId(stateId);
		dbo.setCreatedBy(Long.parseLong(dto.getCreatedBy()));
		dbo.setCreatedOn(dto.getCreatedOn().getTime());
		dbo.setEtag(UUID.randomUUID().toString());
		
		if (!StringUtils.isBlank(dto.getReason())) {
			byte[] reason = dto.getReason().getBytes(StandardCharsets.UTF_8);
			dbo.setReason(reason);
		}
				
		if (!StringUtils.isBlank(dto.getNotes())) {
			byte[] notes = dto.getNotes().getBytes(StandardCharsets.UTF_8);
			dbo.setNotes(notes);
		}
		
		dbo.setState(dto.getState());
		dbo.setVerificationId(verificationSubmissionId);
		
		return dbo;
	}
	
	public static VerificationState copyVerificationStateDBOtoDTO(DBOVerificationState dbo, boolean includeNotes) {
		VerificationState dto = new VerificationState();
		
		dto.setCreatedBy(dbo.getCreatedBy().toString());
		dto.setCreatedOn(new Date(dbo.getCreatedOn()));
		
		if (dbo.getReason() != null) {
			String reason = new String(dbo.getReason(), StandardCharsets.UTF_8);
			dto.setReason(reason);
		}
		
		if (includeNotes && dbo.getNotes() != null) {
			String notes = new String(dbo.getNotes(), StandardCharsets.UTF_8);
			dto.setNotes(notes);
		}
		
		dto.setState(dbo.getState());
		
		return dto;
	}
	
	@Override
	public List<Long> listFileHandleIds(long verificationId) {
		return listFileHandleIds(verificationId, false);
	}
	
	private List<Long> listFileHandleIds(long verificationId, boolean forUpdate) {
		StringBuilder sql = new StringBuilder(FILE_IDS_IN_VERIFICATION_SQL);
		if (forUpdate) {
			sql.append(" FOR UPDATE");
		}
		return jdbcTemplate.queryForList(sql.toString(), Long.class, verificationId);
	}

	@Override
	public VerificationSubmission getCurrentVerificationSubmissionForUser(long userId) {
		DBOVerificationSubmission dbo = null;
		try {
			dbo = jdbcTemplate.queryForObject(LATEST_VERIFICATION_SUBMISSION_SQL, DBO_VERIFICATION_SUB_MAPPING, userId);
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
		Map<Long, List<VerificationState>> stateMap = getVerificationStates(Collections.singleton(dbo.getId()), false);
		List<VerificationState> stateHistory = stateMap.get(dbo.getId());
		if (stateHistory == null) {
			throw new IllegalStateException("Failed to retrieve state history for submission verification " + dbo.getId());
		}
		return copyVerificationDBOtoDTO(dbo, stateHistory);
	}

	@Override
	public VerificationStateEnum getVerificationState(long verificationId) {
		return wrapSubmissionNotFound(verificationId, () -> {
			String stateName = jdbcTemplate.queryForObject(CURRENT_VERIFICATION_STATE_SQL, String.class, verificationId);
			return VerificationStateEnum.valueOf(stateName);
		});
	}

	@Override
	public long getVerificationSubmitter(long verificationId) {
		return wrapSubmissionNotFound(verificationId, () -> 
			jdbcTemplate.queryForObject(VERIFICATION_SUBMITTER_SQL, Long.class, verificationId)
		);
	}

	@Override
	public boolean haveValidatedProfiles(Set<String> userIds) {
		if (userIds == null || userIds.isEmpty()) {
			return false;
		}
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(IDS_PARAM, userIds);
		Integer count = namedJdbcTemplate.queryForObject(SQL_VALIDATED_COUNT, params, Integer.class);
		return count.equals(userIds.size());
	}
	
	@Override
	public List<Long> removeFileHandleIds(long verificationId) {
		DBOVerificationSubmission dbo = wrapSubmissionNotFound(verificationId, () -> 
				jdbcTemplate.queryForObject(SQL_GET_SUBMISSION, DBO_VERIFICATION_SUB_MAPPING, verificationId)
		);
		
		VerificationSubmission dto = deserializeDTO(dbo);
		
		// Clear the attachments from the DTO
		dto.setAttachments(null);
		dbo.setSerialized(serializeDTO(dto));
		dbo.setEtag(UUID.randomUUID().toString());
		
		basicDao.update(dbo);
		
		// Get the list of ids
		List<Long> fileHandleIds = listFileHandleIds(verificationId, true);
		
		// Drop the ids
		jdbcTemplate.update(SQL_DELETE_FILE_IDS, verificationId);
		
		return fileHandleIds;
	}
	
	private <T> T wrapSubmissionNotFound(long verificationId, Supplier<T> supplier) {
		try {
			return supplier.get();
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("A verification submission with id " + verificationId + " could not be found");
		}
	}

}
