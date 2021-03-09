package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_DATA_ACCESS_REQUEST_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_RESEARCH_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_STATUS_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_STATUS_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_STATUS_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_STATUS_REASON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_STATUS_STATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_STATUS_SUBMISSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_SUBMISSION_SERIALIZED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_SUBMITTER_ACCESS_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_SUBMITTER_CURRENT_SUBMISSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_SUBMITTER_SUBMITTER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DATA_ACCESS_SUBMISSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DATA_ACCESS_SUBMISSION_STATUS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DATA_ACCESS_SUBMISSION_SUBMITTER;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.dataaccess.OpenSubmission;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dataaccess.SubmissionInfo;
import org.sagebionetworks.repo.model.dataaccess.SubmissionOrder;
import org.sagebionetworks.repo.model.dataaccess.SubmissionState;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStatus;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.transactions.MandatoryWriteTransaction;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class DBOSubmissionDAOImpl implements SubmissionDAO{

	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private NamedParameterJdbcTemplate namedJdbcTemplate;

	@Autowired
	private IdGenerator idGenerator;

	private static final String SQL_GET_STATUS_FOR_USER = "SELECT "
				+TABLE_DATA_ACCESS_SUBMISSION_SUBMITTER+"."+COL_DATA_ACCESS_SUBMISSION_SUBMITTER_ACCESS_REQUIREMENT_ID+", "
				+TABLE_DATA_ACCESS_SUBMISSION_STATUS+"."+COL_DATA_ACCESS_SUBMISSION_STATUS_SUBMISSION_ID+", "
				+TABLE_DATA_ACCESS_SUBMISSION_STATUS+"."+COL_DATA_ACCESS_SUBMISSION_STATUS_CREATED_BY+", "
				+TABLE_DATA_ACCESS_SUBMISSION_STATUS+"."+COL_DATA_ACCESS_SUBMISSION_STATUS_STATE+", "
				+TABLE_DATA_ACCESS_SUBMISSION_STATUS+"."+COL_DATA_ACCESS_SUBMISSION_STATUS_MODIFIED_ON+", "
				+TABLE_DATA_ACCESS_SUBMISSION_STATUS+"."+COL_DATA_ACCESS_SUBMISSION_STATUS_REASON
			+ " FROM "+TABLE_DATA_ACCESS_SUBMISSION_SUBMITTER+", "
				+TABLE_DATA_ACCESS_SUBMISSION_STATUS
			+ " WHERE "+TABLE_DATA_ACCESS_SUBMISSION_SUBMITTER+"."+COL_DATA_ACCESS_SUBMISSION_SUBMITTER_CURRENT_SUBMISSION_ID
				+" = "+TABLE_DATA_ACCESS_SUBMISSION_STATUS+"."+COL_DATA_ACCESS_SUBMISSION_STATUS_SUBMISSION_ID
			+ " AND "+COL_DATA_ACCESS_SUBMISSION_SUBMITTER_ACCESS_REQUIREMENT_ID+" = ?"
			+ " AND "+COL_DATA_ACCESS_SUBMISSION_SUBMITTER_SUBMITTER_ID+" = ?";

	private static final String SQL_GET_STATUS_BY_ID = " SELECT "
				+TABLE_DATA_ACCESS_SUBMISSION+"."+COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_ID+", "
				+TABLE_DATA_ACCESS_SUBMISSION_STATUS+"."+COL_DATA_ACCESS_SUBMISSION_STATUS_SUBMISSION_ID+", "
				+TABLE_DATA_ACCESS_SUBMISSION_STATUS+"."+COL_DATA_ACCESS_SUBMISSION_STATUS_CREATED_BY+", "
				+TABLE_DATA_ACCESS_SUBMISSION_STATUS+"."+COL_DATA_ACCESS_SUBMISSION_STATUS_STATE+", "
				+TABLE_DATA_ACCESS_SUBMISSION_STATUS+"."+COL_DATA_ACCESS_SUBMISSION_STATUS_MODIFIED_ON+", "
				+TABLE_DATA_ACCESS_SUBMISSION_STATUS+"."+COL_DATA_ACCESS_SUBMISSION_STATUS_REASON
			+ " FROM "+TABLE_DATA_ACCESS_SUBMISSION+", "
				+TABLE_DATA_ACCESS_SUBMISSION_STATUS
			+ " WHERE "+TABLE_DATA_ACCESS_SUBMISSION+"."+COL_DATA_ACCESS_SUBMISSION_ID
				+" = "+TABLE_DATA_ACCESS_SUBMISSION_STATUS+"."+COL_DATA_ACCESS_SUBMISSION_STATUS_SUBMISSION_ID
			+" AND "+COL_DATA_ACCESS_SUBMISSION_STATUS_SUBMISSION_ID+" = ?";
	
	private static final String SQL_UPDATE_SUBMISSION_ETAG = "UPDATE "+TABLE_DATA_ACCESS_SUBMISSION
			+ " SET "+COL_DATA_ACCESS_SUBMISSION_ETAG+" = ?"
			+ " WHERE "+COL_DATA_ACCESS_SUBMISSION_ID+" = ?";

	private static final String SQL_UPDATE_STATUS = "UPDATE "+TABLE_DATA_ACCESS_SUBMISSION_STATUS
			+" SET "+COL_DATA_ACCESS_SUBMISSION_STATUS_STATE+" = ?, "
			+COL_DATA_ACCESS_SUBMISSION_STATUS_MODIFIED_BY+" = ?, "
			+COL_DATA_ACCESS_SUBMISSION_STATUS_MODIFIED_ON+" = ?, "
			+COL_DATA_ACCESS_SUBMISSION_STATUS_REASON+" = ?"
			+ " WHERE "+COL_DATA_ACCESS_SUBMISSION_STATUS_SUBMISSION_ID+" = ?";

	private static final String SQL_GET_SUBMISSION_BY_ID = "SELECT *"
			+ " FROM "+TABLE_DATA_ACCESS_SUBMISSION+", "
				+TABLE_DATA_ACCESS_SUBMISSION_STATUS
			+ " WHERE "+TABLE_DATA_ACCESS_SUBMISSION+"."+COL_DATA_ACCESS_SUBMISSION_ID
				+" = "+TABLE_DATA_ACCESS_SUBMISSION_STATUS+"."+COL_DATA_ACCESS_SUBMISSION_STATUS_SUBMISSION_ID
			+ " AND "+TABLE_DATA_ACCESS_SUBMISSION+"."+COL_DATA_ACCESS_SUBMISSION_ID+" = ?";

	private static final String SQL_GET_SUBMISSION_FOR_UPDATE = SQL_GET_SUBMISSION_BY_ID + " FOR UPDATE";

	private static final String SQL_HAS_STATE = "SELECT COUNT(*)"
			+ " FROM "+TABLE_DATA_ACCESS_SUBMISSION_STATUS+", "
				+TABLE_DATA_ACCESS_SUBMISSION
			+ " WHERE "+TABLE_DATA_ACCESS_SUBMISSION+"."+COL_DATA_ACCESS_SUBMISSION_ID
				+" = "+TABLE_DATA_ACCESS_SUBMISSION_STATUS+"."+COL_DATA_ACCESS_SUBMISSION_STATUS_SUBMISSION_ID
			+ " AND "+COL_DATA_ACCESS_SUBMISSION_STATUS_STATE+" = ?"
			+ " AND "+COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_ID+" = ?"
			+ " AND "+TABLE_DATA_ACCESS_SUBMISSION+"."+COL_DATA_ACCESS_SUBMISSION_CREATED_BY+" = ?";

	private static final String SQL_DELETE = "DELETE FROM "+TABLE_DATA_ACCESS_SUBMISSION
			+" WHERE "+COL_DATA_ACCESS_SUBMISSION_ID+" = ?";

	private static final String SQL_LIST_SUBMISSIONS = "SELECT *"
			+ " FROM "+TABLE_DATA_ACCESS_SUBMISSION+", "+ TABLE_DATA_ACCESS_SUBMISSION_STATUS
			+ " WHERE "+TABLE_DATA_ACCESS_SUBMISSION+"."+COL_DATA_ACCESS_SUBMISSION_ID
			+ " = "+TABLE_DATA_ACCESS_SUBMISSION_STATUS+"."+COL_DATA_ACCESS_SUBMISSION_STATUS_SUBMISSION_ID
			+ " AND "+COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_ID+" = :"+COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_ID;

	private static final String SQL_LIST_SUBMISSIONS_WITH_FILTER = SQL_LIST_SUBMISSIONS
			+ " AND "+COL_DATA_ACCESS_SUBMISSION_STATUS_STATE+" = :"+COL_DATA_ACCESS_SUBMISSION_STATUS_STATE;

	private static final String ORDER_BY = "ORDER BY";
	private static final String DESCENDING = "DESC";
	private static final String LIMIT = "LIMIT";
	private static final String OFFSET = "OFFSET";

	/*
	 * SELECT s.SUBMISSION_SERIALIZED, ss.MODIFIED_ON 
	 * FROM DATA_ACCESS_SUBMISSION s, DATA_ACCESS_SUBMISSION_STATUS ss  
	 * WHERE s.ID = ss.SUBMISSION_ID AND s.ACCESS_REQUIREMENT_ID = ? 
	 * AND ss.STATE = 'APPROVED' AND ss.MODIFIED_ON = (
	 * 		SELECT MAX(ss2.MODIFIED_ON) 
	 * 		FROM DATA_ACCESS_SUBMISSION s2, DATA_ACCESS_SUBMISSION_STATUS ss2  
	 * 		WHERE s2.ID = ss2.SUBMISSION_ID AND s2.ACCESS_REQUIREMENT_ID = s.ACCESS_REQUIREMENT_ID AND 
	 * 		s2.RESEARCH_PROJECT_ID = s.RESEARCH_PROJECT_ID AND ss2.STATE = 'APPROVED' 
	 * 		GROUP BY s2.ACCESS_REQUIREMENT_ID, s2.RESEARCH_PROJECT_ID ) 
	 * ORDER BY ss.MODIFIED_ON;
	 */
	private static final String SQL_LIST_SUBMISSION_INFO = 
			"SELECT s."+COL_DATA_ACCESS_SUBMISSION_SUBMISSION_SERIALIZED+", ss."+COL_DATA_ACCESS_SUBMISSION_STATUS_MODIFIED_ON
			+ " FROM "+TABLE_DATA_ACCESS_SUBMISSION+" s, "+ TABLE_DATA_ACCESS_SUBMISSION_STATUS+" ss "
			+ " WHERE s."+COL_DATA_ACCESS_SUBMISSION_ID
			+ " = ss."+COL_DATA_ACCESS_SUBMISSION_STATUS_SUBMISSION_ID
			+ " AND s."+COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_ID+" = :"+COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_ID
			+ " AND ss."+COL_DATA_ACCESS_SUBMISSION_STATUS_STATE+" = '"+SubmissionState.APPROVED+"'"
			+ " AND ss."+COL_DATA_ACCESS_SUBMISSION_STATUS_MODIFIED_ON+" = (SELECT MAX(ss2."+COL_DATA_ACCESS_SUBMISSION_STATUS_MODIFIED_ON+")"
			+ " FROM "+TABLE_DATA_ACCESS_SUBMISSION+" s2, "+ TABLE_DATA_ACCESS_SUBMISSION_STATUS+" ss2 "
			+ " WHERE s2."+COL_DATA_ACCESS_SUBMISSION_ID+" = ss2."+COL_DATA_ACCESS_SUBMISSION_STATUS_SUBMISSION_ID
			+ " AND s2."+COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_ID+" = s."+COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_ID
			+ " AND s2."+COL_DATA_ACCESS_SUBMISSION_RESEARCH_PROJECT_ID+" = s."+COL_DATA_ACCESS_SUBMISSION_RESEARCH_PROJECT_ID
			+ " AND ss2."+COL_DATA_ACCESS_SUBMISSION_STATUS_STATE+" = '"+SubmissionState.APPROVED+"'"
			+ " GROUP BY s2."+COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_ID+", s2."+COL_DATA_ACCESS_SUBMISSION_RESEARCH_PROJECT_ID
			+ " )"
			+ " ORDER BY ss."+COL_DATA_ACCESS_SUBMISSION_STATUS_MODIFIED_ON;

	private static final String SQL_IS_ACCESSOR = "SELECT COUNT(*)"
			+ " FROM "+TABLE_DATA_ACCESS_SUBMISSION_SUBMITTER
			+ " WHERE "+COL_DATA_ACCESS_SUBMISSION_SUBMITTER_CURRENT_SUBMISSION_ID+" = ?"
			+ " AND "+COL_DATA_ACCESS_SUBMISSION_SUBMITTER_SUBMITTER_ID+" = ?";

	private static final String OPEN_SUBMISSIONS_ALIAS = "NUMBER_OF_OPEN_SUBMISSIONS";
	private static final String SQL_OPEN_SUBMISSION = "SELECT "
				+COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_ID+", "
				+ "COUNT("+COL_DATA_ACCESS_SUBMISSION_STATUS_SUBMISSION_ID+") AS "+OPEN_SUBMISSIONS_ALIAS
			+ " FROM "+TABLE_DATA_ACCESS_SUBMISSION+", "+ TABLE_DATA_ACCESS_SUBMISSION_STATUS
			+ " WHERE "+TABLE_DATA_ACCESS_SUBMISSION+"."+COL_DATA_ACCESS_SUBMISSION_ID
				+ " = "+TABLE_DATA_ACCESS_SUBMISSION_STATUS+"."+COL_DATA_ACCESS_SUBMISSION_STATUS_SUBMISSION_ID
			+ " AND "+COL_DATA_ACCESS_SUBMISSION_STATUS_STATE+" = '"+SubmissionState.SUBMITTED.name()+"'"
			+ " GROUP BY "+COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_ID
			+ " ORDER BY "+COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_ID
			+ " LIMIT ? OFFSET ?";

	private static final RowMapper<SubmissionStatus> STATUS_MAPPER = new RowMapper<SubmissionStatus>(){
		@Override
		public SubmissionStatus mapRow(ResultSet rs, int rowNum) throws SQLException {
			SubmissionStatus status = new SubmissionStatus();
			status.setSubmissionId(rs.getString(COL_DATA_ACCESS_SUBMISSION_STATUS_SUBMISSION_ID));
			status.setSubmittedBy(rs.getString(COL_DATA_ACCESS_SUBMISSION_STATUS_CREATED_BY));
			status.setModifiedOn(new Date(rs.getLong(COL_DATA_ACCESS_SUBMISSION_STATUS_MODIFIED_ON)));
			status.setState(SubmissionState.valueOf(rs.getString(COL_DATA_ACCESS_SUBMISSION_STATUS_STATE)));
			Blob blob = rs.getBlob(COL_DATA_ACCESS_SUBMISSION_STATUS_REASON);
			if (!rs.wasNull()) {
				try {
					status.setRejectedReason(new String(blob.getBytes(1, (int) blob.length()), "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					throw new RuntimeException();
				}
			}
			return status;
		}
	};

	private static final UnmodifiableXStream X_STREAM = UnmodifiableXStream.builder().allowTypes(Submission.class).build();

	private static final RowMapper<Submission> SUBMISSION_MAPPER = new RowMapper<Submission>(){

		@Override
		public Submission mapRow(ResultSet rs, int rowNum) throws SQLException {
			try {
				Blob blob = rs.getBlob(COL_DATA_ACCESS_SUBMISSION_SUBMISSION_SERIALIZED);
				Submission submission = (Submission)JDOSecondaryPropertyUtils.decompressObject(X_STREAM, blob.getBytes(1, (int) blob.length()));
				submission.setId(rs.getString(COL_DATA_ACCESS_SUBMISSION_ID));
				submission.setAccessRequirementId(rs.getString(COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_ID));
				submission.setRequestId(rs.getString(COL_DATA_ACCESS_SUBMISSION_DATA_ACCESS_REQUEST_ID));
				submission.setSubmittedBy(rs.getString(COL_DATA_ACCESS_SUBMISSION_CREATED_BY));
				submission.setSubmittedOn(new Date(rs.getLong(COL_DATA_ACCESS_SUBMISSION_CREATED_ON)));
				submission.setEtag(rs.getString(COL_DATA_ACCESS_SUBMISSION_ETAG));
				submission.setState(SubmissionState.valueOf(rs.getString(COL_DATA_ACCESS_SUBMISSION_STATUS_STATE)));
				Blob reason = rs.getBlob(COL_DATA_ACCESS_SUBMISSION_STATUS_REASON);
				if (!rs.wasNull()) {
					submission.setRejectedReason(new String(reason.getBytes(1, (int) reason.length()), "UTF-8"));
				}
				submission.setModifiedBy(rs.getString(COL_DATA_ACCESS_SUBMISSION_STATUS_MODIFIED_BY));
				submission.setModifiedOn(new Date(rs.getLong(COL_DATA_ACCESS_SUBMISSION_STATUS_MODIFIED_ON)));
				return submission;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	};

	private static final RowMapper<SubmissionInfo> SUBMISSION_INFO_MAPPER = new RowMapper<SubmissionInfo>(){

		@Override
		public SubmissionInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
			try {
				SubmissionInfo result = new SubmissionInfo();
				Blob blob = rs.getBlob(COL_DATA_ACCESS_SUBMISSION_SUBMISSION_SERIALIZED);
				Submission submission = (Submission)JDOSecondaryPropertyUtils.decompressObject(X_STREAM, blob.getBytes(1, (int) blob.length()));
				ResearchProject researchProject = submission.getResearchProjectSnapshot();
				result.setInstitution(researchProject.getInstitution());
				result.setIntendedDataUseStatement(researchProject.getIntendedDataUseStatement());
				result.setProjectLead(researchProject.getProjectLead());
				result.setModifiedOn(new Date(rs.getLong(COL_DATA_ACCESS_SUBMISSION_STATUS_MODIFIED_ON)));
				return result;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	};

	@Override
	public SubmissionStatus getStatusByRequirementIdAndPrincipalId(String accessRequirementId, String userId) {
		try {
			return jdbcTemplate.queryForObject(SQL_GET_STATUS_FOR_USER, STATUS_MAPPER, accessRequirementId, userId);
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	@WriteTransaction
	@Override
	public Submission updateSubmissionStatus(String submissionId,
			SubmissionState newState, String reason, String userId, Long timestamp) {
		jdbcTemplate.update(SQL_UPDATE_STATUS, newState.toString(), userId, timestamp, reason, submissionId);
		jdbcTemplate.update(SQL_UPDATE_SUBMISSION_ETAG, UUID.randomUUID().toString(), submissionId);
		return getSubmission(submissionId);
	}

	@Override
	public Submission getSubmission(String submissionId) {
		try {
			return jdbcTemplate.queryForObject(SQL_GET_SUBMISSION_BY_ID, SUBMISSION_MAPPER, submissionId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException();
		}
	}

	@WriteTransaction
	@Override
	public SubmissionStatus createSubmission(Submission toCreate) {
		toCreate.setId(idGenerator.generateNewId(IdType.DATA_ACCESS_SUBMISSION_ID).toString());
		toCreate.setEtag(UUID.randomUUID().toString());
		DBOSubmission dboSubmission = new DBOSubmission();
		SubmissionUtils.copyDtoToDbo(toCreate, dboSubmission);
		DBOSubmissionStatus status = SubmissionUtils.getDBOStatus(toCreate);
		DBOSubmissionSubmitter submitter = SubmissionUtils.createDBOSubmissionSubmitter(toCreate, idGenerator);
		basicDao.createNew(dboSubmission);
		basicDao.createNew(status);
		basicDao.createOrUpdate(submitter);
		return getSubmissionStatus(toCreate.getId());
	}

	@WriteTransaction
	@Override
	public SubmissionStatus cancel(String submissionId, String userId, Long timestamp, String etag) {
		jdbcTemplate.update(SQL_UPDATE_STATUS, SubmissionState.CANCELLED.toString(), userId, timestamp, null, submissionId);
		jdbcTemplate.update(SQL_UPDATE_SUBMISSION_ETAG, etag, submissionId);
		return getSubmissionStatus(submissionId);
	}

	private SubmissionStatus getSubmissionStatus(String submissionId) {
		try {
			return jdbcTemplate.queryForObject(SQL_GET_STATUS_BY_ID, STATUS_MAPPER, submissionId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException();
		}
	}

	@Override
	public boolean hasSubmissionWithState(String userId, String accessRequirementId, SubmissionState state) {
		Integer count = jdbcTemplate.queryForObject(SQL_HAS_STATE, Integer.class, state.toString(), accessRequirementId, userId);
		return !count.equals(0);
	}

	@MandatoryWriteTransaction
	@Override
	public Submission getForUpdate(String submissionId) {
		try {
			return jdbcTemplate.queryForObject(SQL_GET_SUBMISSION_FOR_UPDATE, SUBMISSION_MAPPER, submissionId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException();
		}
	}

	@WriteTransaction
	@Override
	public void delete(String id) {
		jdbcTemplate.update(SQL_DELETE, id);
	}

	@Override
	public List<Submission> getSubmissions(String accessRequirementId, SubmissionState filterBy,
			SubmissionOrder orderBy, Boolean isAscending, long limit, long offset) {

		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_ID, accessRequirementId);

		String query = null;
		if (filterBy != null) {
			query = SQL_LIST_SUBMISSIONS_WITH_FILTER;
			param.addValue(COL_DATA_ACCESS_SUBMISSION_STATUS_STATE, filterBy.name());
		} else {
			query = SQL_LIST_SUBMISSIONS;
		}
		query = addOrderByClause(orderBy, isAscending, query);
		query += " "+LIMIT+" "+limit+" "+OFFSET+" "+offset;
		return namedJdbcTemplate.query(query, param, SUBMISSION_MAPPER);
	}

	@Override
	public List<SubmissionInfo> listInfoForApprovedSubmissions(String accessRequirementId, long limit, long offset) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_ID, accessRequirementId);

		String query =  SQL_LIST_SUBMISSION_INFO;
		query += " "+LIMIT+" "+limit+" "+OFFSET+" "+offset;
		return namedJdbcTemplate.query(query, param, SUBMISSION_INFO_MAPPER);
	}

	/**
	 * @param orderBy
	 * @param isAscending
	 * @param query
	 * @return
	 */
	public static String addOrderByClause(SubmissionOrder orderBy, Boolean isAscending, String query) {
		if (orderBy != null) {
			switch(orderBy) {
				case CREATED_ON:
					query += " "+ORDER_BY+" "+TABLE_DATA_ACCESS_SUBMISSION+"."+COL_DATA_ACCESS_SUBMISSION_CREATED_ON;
					break;
				case MODIFIED_ON:
					query += " "+ORDER_BY+" "+TABLE_DATA_ACCESS_SUBMISSION_STATUS+"."+COL_DATA_ACCESS_SUBMISSION_STATUS_MODIFIED_ON;
					break;
				default:
					throw new IllegalArgumentException("Do not support order by "+orderBy.name());
			}
			if (isAscending != null && !isAscending) {
				query += " "+DESCENDING;
			}
		}
		return query;
	}

	@Override
	public boolean isAccessor(String submissionId, String userId) {
		return jdbcTemplate.queryForObject(SQL_IS_ACCESSOR, Integer.class, submissionId, userId) > 0;
	}

	@Override
	public List<OpenSubmission> getOpenSubmissions(long limit, long offset) {
		return jdbcTemplate.query(SQL_OPEN_SUBMISSION, new RowMapper<OpenSubmission>(){

			@Override
			public OpenSubmission mapRow(ResultSet rs, int rowNum) throws SQLException {
				OpenSubmission os = new OpenSubmission();
				os.setAccessRequirementId(rs.getString(COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_ID));
				os.setNumberOfSubmittedSubmission(rs.getLong(OPEN_SUBMISSIONS_ALIAS));
				return os;
			}
		}, limit, offset);
	}
	
	@Override
	public void truncateAll() {
		jdbcTemplate.update("DELETE FROM " + TABLE_DATA_ACCESS_SUBMISSION);
	}
}
