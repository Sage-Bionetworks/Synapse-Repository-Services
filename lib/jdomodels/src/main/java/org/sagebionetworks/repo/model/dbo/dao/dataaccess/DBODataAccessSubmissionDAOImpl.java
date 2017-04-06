package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmission;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionOrder;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionState;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dataaccess.ACTAccessRequirementStatus;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.transactions.MandatoryWriteTransaction;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class DBODataAccessSubmissionDAOImpl implements DataAccessSubmissionDAO{

	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private NamedParameterJdbcTemplate namedJdbcTemplate;

	@Autowired
	private IdGenerator idGenerator;

	private static final String SQL_GET_STATUS_FOR_USER = "SELECT "
				+TABLE_DATA_ACCESS_SUBMISSION_ACCESSOR+"."+COL_DATA_ACCESS_SUBMISSION_ACCESSOR_ACCESS_REQUIREMENT_ID+", "
				+TABLE_DATA_ACCESS_SUBMISSION_STATUS+"."+COL_DATA_ACCESS_SUBMISSION_STATUS_SUBMISSION_ID+", "
				+TABLE_DATA_ACCESS_SUBMISSION_STATUS+"."+COL_DATA_ACCESS_SUBMISSION_STATUS_CREATED_BY+", "
				+TABLE_DATA_ACCESS_SUBMISSION_STATUS+"."+COL_DATA_ACCESS_SUBMISSION_STATUS_STATE+", "
				+TABLE_DATA_ACCESS_SUBMISSION_STATUS+"."+COL_DATA_ACCESS_SUBMISSION_STATUS_MODIFIED_ON+", "
				+TABLE_DATA_ACCESS_SUBMISSION_STATUS+"."+COL_DATA_ACCESS_SUBMISSION_STATUS_REASON
			+ " FROM "+TABLE_DATA_ACCESS_SUBMISSION_ACCESSOR+", "
				+TABLE_DATA_ACCESS_SUBMISSION_STATUS
			+ " WHERE "+TABLE_DATA_ACCESS_SUBMISSION_ACCESSOR+"."+COL_DATA_ACCESS_SUBMISSION_ACCESSOR_CURRENT_SUBMISSION_ID
				+" = "+TABLE_DATA_ACCESS_SUBMISSION_STATUS+"."+COL_DATA_ACCESS_SUBMISSION_STATUS_SUBMISSION_ID
			+ " AND "+COL_DATA_ACCESS_SUBMISSION_ACCESSOR_ACCESS_REQUIREMENT_ID+" = ?"
			+ " AND "+COL_DATA_ACCESS_SUBMISSION_ACCESSOR_ACCESSOR_ID+" = ?";

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
				+TABLE_DATA_ACCESS_SUBMISSION_ACCESSOR
			+ " WHERE "+TABLE_DATA_ACCESS_SUBMISSION_ACCESSOR+"."+COL_DATA_ACCESS_SUBMISSION_ACCESSOR_CURRENT_SUBMISSION_ID
				+" = "+TABLE_DATA_ACCESS_SUBMISSION_STATUS+"."+COL_DATA_ACCESS_SUBMISSION_STATUS_SUBMISSION_ID
			+ " AND "+COL_DATA_ACCESS_SUBMISSION_STATUS_STATE+" = ?"
			+ " AND "+COL_DATA_ACCESS_SUBMISSION_ACCESSOR_ACCESS_REQUIREMENT_ID+" = ?"
			+ " AND "+COL_DATA_ACCESS_SUBMISSION_ACCESSOR_ACCESSOR_ID+" = ?";

	private static final String SQL_DELETE = "DELETE FROM "+TABLE_DATA_ACCESS_SUBMISSION
			+" WHERE "+COL_DATA_ACCESS_SUBMISSION_ID+" = ?";

	private static final String SQL_LIST_SUBMISSIONS = "SELECT *"
			+ " FROM "+TABLE_DATA_ACCESS_SUBMISSION+", "+ TABLE_DATA_ACCESS_SUBMISSION_STATUS
			+ " WHERE "+TABLE_DATA_ACCESS_SUBMISSION+"."+COL_DATA_ACCESS_SUBMISSION_ID
			+ " = "+TABLE_DATA_ACCESS_SUBMISSION_STATUS+"."+COL_DATA_ACCESS_SUBMISSION_STATUS_SUBMISSION_ID
			+ " AND "+COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_ID+" = ?";

	private static final String SQL_LIST_SUBMISSIONS_WITH_FILTER = SQL_LIST_SUBMISSIONS
			+ " AND "+COL_DATA_ACCESS_SUBMISSION_STATUS_STATE+" =?";

	private static final String ORDER_BY = "ORDER BY";
	private static final String DESCENDING = "DESC";
	private static final String LIMIT = "LIMIT";
	private static final String OFFSET = "OFFSET";

	private static final String SQL_IS_ACCESSOR = "SELECT COUNT(*)"
			+ " FROM "+TABLE_DATA_ACCESS_SUBMISSION_ACCESSOR
			+ " WHERE "+COL_DATA_ACCESS_SUBMISSION_ACCESSOR_CURRENT_SUBMISSION_ID+" = ?"
			+ " AND "+COL_DATA_ACCESS_SUBMISSION_ACCESSOR_ACCESSOR_ID+" = ?";

	private static final RowMapper<ACTAccessRequirementStatus> STATUS_MAPPER = new RowMapper<ACTAccessRequirementStatus>(){
		@Override
		public ACTAccessRequirementStatus mapRow(ResultSet rs, int rowNum) throws SQLException {
			ACTAccessRequirementStatus status = new ACTAccessRequirementStatus();
			status.setAccessRequirementId(rs.getString(COL_DATA_ACCESS_SUBMISSION_ACCESSOR_ACCESS_REQUIREMENT_ID));
			status.setSubmissionId(rs.getString(COL_DATA_ACCESS_SUBMISSION_STATUS_SUBMISSION_ID));
			status.setSubmittedBy(rs.getString(COL_DATA_ACCESS_SUBMISSION_STATUS_CREATED_BY));
			status.setModifiedOn(new Date(rs.getLong(COL_DATA_ACCESS_SUBMISSION_STATUS_MODIFIED_ON)));
			status.setState(DataAccessSubmissionState.valueOf(rs.getString(COL_DATA_ACCESS_SUBMISSION_STATUS_STATE)));
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

	private static final RowMapper<DataAccessSubmission> SUBMISSION_MAPPER = new RowMapper<DataAccessSubmission>(){

		@Override
		public DataAccessSubmission mapRow(ResultSet rs, int rowNum) throws SQLException {
			try {
				Blob blob = rs.getBlob(COL_DATA_ACCESS_SUBMISSION_SUBMISSION_SERIALIZED);
				DataAccessSubmission submission = (DataAccessSubmission)JDOSecondaryPropertyUtils.decompressedObject(blob.getBytes(1, (int) blob.length()));
				submission.setId(rs.getString(COL_DATA_ACCESS_SUBMISSION_ID));
				submission.setAccessRequirementId(rs.getString(COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_ID));
				submission.setDataAccessRequestId(rs.getString(COL_DATA_ACCESS_SUBMISSION_DATA_ACCESS_REQUEST_ID));
				submission.setSubmittedBy(rs.getString(COL_DATA_ACCESS_SUBMISSION_CREATED_BY));
				submission.setSubmittedOn(new Date(rs.getLong(COL_DATA_ACCESS_SUBMISSION_CREATED_ON)));
				submission.setEtag(rs.getString(COL_DATA_ACCESS_SUBMISSION_ETAG));
				submission.setState(DataAccessSubmissionState.valueOf(rs.getString(COL_DATA_ACCESS_SUBMISSION_STATUS_STATE)));
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

	@Override
	public ACTAccessRequirementStatus getStatusByRequirementIdAndPrincipalId(String accessRequirementId, String userId) {
		try {
			return jdbcTemplate.queryForObject(SQL_GET_STATUS_FOR_USER, STATUS_MAPPER, accessRequirementId, userId);
		} catch (EmptyResultDataAccessException e) {
			ACTAccessRequirementStatus status = new ACTAccessRequirementStatus();
			status.setAccessRequirementId(accessRequirementId);
			status.setState(DataAccessSubmissionState.NOT_SUBMITTED);
			return status;
		}
	}

	@WriteTransactionReadCommitted
	@Override
	public DataAccessSubmission updateSubmissionStatus(String submissionId,
			DataAccessSubmissionState newState, String reason, String userId, Long timestamp) {
		jdbcTemplate.update(SQL_UPDATE_STATUS, newState.toString(), userId, timestamp, reason, submissionId);
		jdbcTemplate.update(SQL_UPDATE_SUBMISSION_ETAG, UUID.randomUUID().toString(), submissionId);
		return getSubmission(submissionId);
	}

	@Override
	public DataAccessSubmission getSubmission(String submissionId) {
		try {
			return jdbcTemplate.queryForObject(SQL_GET_SUBMISSION_BY_ID, SUBMISSION_MAPPER, submissionId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException();
		}
	}

	@WriteTransactionReadCommitted
	@Override
	public ACTAccessRequirementStatus createSubmission(DataAccessSubmission toCreate) {
		toCreate.setId(idGenerator.generateNewId(IdType.DATA_ACCESS_SUBMISSION_ID).toString());
		toCreate.setEtag(UUID.randomUUID().toString());
		DBODataAccessSubmission dboSubmission = new DBODataAccessSubmission();
		DataAccessSubmissionUtils.copyDtoToDbo(toCreate, dboSubmission);
		DBODataAccessSubmissionStatus status = DataAccessSubmissionUtils.getDBOStatus(toCreate);
		List<DBODataAccessSubmissionAccessor> accessors = DataAccessSubmissionUtils.createDBODataAccessSubmissionAccessor(toCreate, idGenerator);
		basicDao.createNew(dboSubmission);
		basicDao.createNew(status);
		basicDao.createOrUpdateBatch(accessors);
		return getSubmissionStatus(toCreate.getId());
	}

	@WriteTransactionReadCommitted
	@Override
	public ACTAccessRequirementStatus cancel(String submissionId, String userId, Long timestamp, String etag) {
		jdbcTemplate.update(SQL_UPDATE_STATUS, DataAccessSubmissionState.CANCELLED.toString(), userId, timestamp, null, submissionId);
		jdbcTemplate.update(SQL_UPDATE_SUBMISSION_ETAG, etag, submissionId);
		return getSubmissionStatus(submissionId);
	}

	private ACTAccessRequirementStatus getSubmissionStatus(String submissionId) {
		try {
			return jdbcTemplate.queryForObject(SQL_GET_STATUS_BY_ID, STATUS_MAPPER, submissionId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException();
		}
	}

	@Override
	public boolean hasSubmissionWithState(String userId, String accessRequirementId, DataAccessSubmissionState state) {
		Integer count = jdbcTemplate.queryForObject(SQL_HAS_STATE, Integer.class, state.toString(), accessRequirementId, userId);
		return !count.equals(0);
	}

	@MandatoryWriteTransaction
	@Override
	public DataAccessSubmission getForUpdate(String submissionId) {
		try {
			return jdbcTemplate.queryForObject(SQL_GET_SUBMISSION_FOR_UPDATE, SUBMISSION_MAPPER, submissionId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException();
		}
	}

	@WriteTransactionReadCommitted
	@Override
	public void delete(String id) {
		jdbcTemplate.update(SQL_DELETE, id);
	}

	@Override
	public List<DataAccessSubmission> getSubmissions(String accessRequirementId, DataAccessSubmissionState filterBy,
			DataAccessSubmissionOrder orderBy, Boolean isAscending, long limit, long offset) {
			String query = null;
		if (filterBy != null) {
			query = SQL_LIST_SUBMISSIONS_WITH_FILTER;
		} else {
			query = SQL_LIST_SUBMISSIONS;
		}
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
		query += " "+LIMIT+" "+limit+" "+OFFSET+" "+offset;
		if (filterBy != null) {
			return jdbcTemplate.query(query, SUBMISSION_MAPPER, accessRequirementId, filterBy.name());
		} else {
			return jdbcTemplate.query(query, SUBMISSION_MAPPER, accessRequirementId);
		}
	}

	@Override
	public boolean isAccessor(String submissionId, String userId) {
		return jdbcTemplate.queryForObject(SQL_IS_ACCESSOR, Integer.class, submissionId, userId) > 0;
	}
}
