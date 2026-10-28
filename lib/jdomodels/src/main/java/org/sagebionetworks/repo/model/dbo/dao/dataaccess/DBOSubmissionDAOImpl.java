package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACL_OWNER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACL_OWNER_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_ACCESSOR_CHANGES_ACCESSOR_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_ACCESSOR_CHANGES_SUBMISSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_STATUS_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_STATUS_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_STATUS_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_STATUS_REASON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_STATUS_STATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_STATUS_SUBMISSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_SUBMITTER_ACCESS_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_SUBMITTER_CURRENT_SUBMISSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_SUBMITTER_SUBMITTER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_OWNER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_TYPE_ELEMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_TYPE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACCESS_CONTROL_LIST;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DATA_ACCESS_SUBMISSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DATA_ACCESS_SUBMISSION_ACCESSORS_CHANGES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DATA_ACCESS_SUBMISSION_STATUS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DATA_ACCESS_SUBMISSION_SUBMITTER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_RESOURCE_ACCESS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_RESOURCE_ACCESS_TYPE;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dataaccess.OpenSubmission;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dataaccess.SubmissionInfo;
import org.sagebionetworks.repo.model.dataaccess.SubmissionOrder;
import org.sagebionetworks.repo.model.dataaccess.SubmissionReviewerFilterType;
import org.sagebionetworks.repo.model.dataaccess.SubmissionSearchSort;
import org.sagebionetworks.repo.model.dataaccess.SubmissionState;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStatus;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.transactions.MandatoryWriteTransaction;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DBOSubmissionDAOImpl implements SubmissionDAO {

	public static final String SUBMISSION_DOES_NOT_EXIST = "Submission: '%s' does not exist";

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

	private static final String ACCESSOR_CHANGES_ALIAS = "ACCESSOR_CHANGES";

	/**
	 * Selects the accessor changes of the submission aliased S as a JSON array named {@link #ACCESSOR_CHANGES_ALIAS}, NULL
	 * when it has none, so that a submission is read with its accessor changes in a single statement
	 */
	private static final String ACCESSOR_CHANGES_COLUMN = """
			(SELECT JSON_ARRAYAGG(JSON_OBJECT('userId', CAST(AC.ACCESSOR_ID AS CHAR), 'type', AC.ACCESS_TYPE))
			 FROM DATA_ACCESS_SUBMISSION_ACCESSOR_CHANGES AC WHERE AC.SUBMISSION_ID = S.ID) AS ACCESSOR_CHANGES""";

	private static final String SQL_SELECT_SUBMISSION = "SELECT S.*, SS.STATE, SS.MODIFIED_ON, SS.MODIFIED_BY, SS.REASON, "
			+ ACCESSOR_CHANGES_COLUMN
			+ " FROM DATA_ACCESS_SUBMISSION S JOIN DATA_ACCESS_SUBMISSION_STATUS SS ON S.ID = SS.SUBMISSION_ID";

	private static final String SQL_GET_SUBMISSION_BY_ID = SQL_SELECT_SUBMISSION + " WHERE S.ID = ?";

	// Locks the submission and status rows only: a locking read does not lock the rows read by a nested subquery, so the
	// accessor changes subquery takes no locks
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
	
	private static final String SQL_AR_ID_BY_SUBMISSION_ID = "SELECT " + COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_ID
			+ " FROM " + TABLE_DATA_ACCESS_SUBMISSION
			+ " WHERE " + COL_DATA_ACCESS_SUBMISSION_ID + " = ?";

	
	
	private static final String ORDER_BY = "ORDER BY";
	private static final String DESCENDING = "DESC";

	/**
	 * The latest approved submission of each research project of an access requirement
	 */
	private static final String SQL_FROM_LATEST_APPROVED_SUBMISSION_PER_RESEARCH_PROJECT = """
			FROM DATA_ACCESS_SUBMISSION S, DATA_ACCESS_SUBMISSION_STATUS SS
			WHERE S.ID = SS.SUBMISSION_ID
			AND S.ACCESS_REQUIREMENT_ID = :accessRequirementId
			AND SS.STATE = 'APPROVED'
			AND SS.MODIFIED_ON = (
				SELECT MAX(SS2.MODIFIED_ON)
				FROM DATA_ACCESS_SUBMISSION S2, DATA_ACCESS_SUBMISSION_STATUS SS2
				WHERE S2.ID = SS2.SUBMISSION_ID
				AND S2.ACCESS_REQUIREMENT_ID = S.ACCESS_REQUIREMENT_ID
				AND S2.RESEARCH_PROJECT_ID = S.RESEARCH_PROJECT_ID
				AND SS2.STATE = 'APPROVED'
				GROUP BY S2.ACCESS_REQUIREMENT_ID, S2.RESEARCH_PROJECT_ID
			)
			ORDER BY SS.MODIFIED_ON DESC
			LIMIT :limit OFFSET :offset
			""";

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

	private static final TableMapping<DBOSubmission> SUBMISSION_TABLE_MAPPING = new DBOSubmission().getTableMapping();

	private static final RowMapper<Submission> SUBMISSION_MAPPER = (ResultSet rs, int rowNum) -> {
		Submission submission = SubmissionUtils.toDto(SUBMISSION_TABLE_MAPPING.mapRow(rs, rowNum));
		submission.setState(SubmissionState.valueOf(rs.getString(COL_DATA_ACCESS_SUBMISSION_STATUS_STATE)));
		submission.setRejectedReason(readRejectedReason(rs));
		submission.setModifiedBy(rs.getString(COL_DATA_ACCESS_SUBMISSION_STATUS_MODIFIED_BY));
		submission.setModifiedOn(new Date(rs.getLong(COL_DATA_ACCESS_SUBMISSION_STATUS_MODIFIED_ON)));
		submission.setAccessorChanges(SubmissionUtils.readAccessorChanges(rs.getString(ACCESSOR_CHANGES_ALIAS)));
		return submission;
	};

	private static String readRejectedReason(ResultSet rs) throws SQLException {
		Blob reason = rs.getBlob(COL_DATA_ACCESS_SUBMISSION_STATUS_REASON);
		return reason == null ? null : new String(reason.getBytes(1, (int) reason.length()), StandardCharsets.UTF_8);
	}

	private static SubmissionInfo mapSubmissionInfo(ResultSet rs, boolean includeAccessorChanges) throws SQLException {
		SubmissionInfo result = new SubmissionInfo();
		ResearchProject researchProject = JDOSecondaryPropertyUtils.createObjectFromJSON(ResearchProject.class, rs.getString("RESEARCH_PROJECT_SNAPSHOT"));
		result.setInstitution(researchProject.getInstitution());
		result.setIntendedDataUseStatement(researchProject.getIntendedDataUseStatement());
		result.setProjectLead(researchProject.getProjectLead());
		result.setModifiedOn(new Date(rs.getLong(COL_DATA_ACCESS_SUBMISSION_STATUS_MODIFIED_ON)));
		result.setSubmittedBy(rs.getString(COL_DATA_ACCESS_SUBMISSION_CREATED_BY));
		if (includeAccessorChanges) {
			result.setAccessorChanges(SubmissionUtils.readAccessorChanges(rs.getString(ACCESSOR_CHANGES_ALIAS)));
		}
		return result;
	}

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
			throw new NotFoundException(String.format(SUBMISSION_DOES_NOT_EXIST, submissionId));
		}
	}

	@WriteTransaction
	@Override
	public SubmissionStatus createSubmission(Submission toCreate) {
		toCreate.setId(idGenerator.generateNewId(IdType.DATA_ACCESS_SUBMISSION_ID).toString());
		toCreate.setEtag(UUID.randomUUID().toString());
		DBOSubmission dboSubmission = SubmissionUtils.toDbo(toCreate);
		DBOSubmissionStatus status = SubmissionUtils.getDBOStatus(toCreate);
		DBOSubmissionSubmitter submitter = SubmissionUtils.createDBOSubmissionSubmitter(toCreate, idGenerator);
		List<DBOSubmissionAccessorChange> accessorChanges = SubmissionUtils.createDBOSubmissionAccessorChanges(toCreate);
		basicDao.createNew(dboSubmission);
		basicDao.createNew(status);
		basicDao.createOrUpdate(submitter);
		
		// Needed for testing migration 
		if (!accessorChanges.isEmpty()) {
			basicDao.createBatch(accessorChanges);
		}
		
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
			throw new NotFoundException(String.format(SUBMISSION_DOES_NOT_EXIST, submissionId));
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
			throw new NotFoundException(String.format(SUBMISSION_DOES_NOT_EXIST, submissionId));
		}
	}

	@WriteTransaction
	@Override
	public void delete(String id) {
		jdbcTemplate.update(SQL_DELETE, id);
	}
	
	@Override
	public List<Submission> getSubmissions(String accessRequirementId, SubmissionState stateFilter, String accessorId,
			SubmissionOrder orderBy, Boolean isAscending, long limit, long offset) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue("accessRequirementId", accessRequirementId);
		param.addValue("limit", limit);
		param.addValue("offset", offset);

		String query = SQL_SELECT_SUBMISSION;
		if (accessorId != null) {
			query += " JOIN DATA_ACCESS_SUBMISSION_ACCESSOR_CHANGES SA ON S.ID = SA.SUBMISSION_ID AND SA.ACCESSOR_ID = :accessorId";
			param.addValue("accessorId", accessorId);
		}
		query += " WHERE S.ACCESS_REQUIREMENT_ID = :accessRequirementId";
		if (stateFilter != null) {
			query += " AND SS.STATE = :state";
			param.addValue("state", stateFilter.name());
		}
		query = addOrderByClause(orderBy, isAscending, query);
		query += " LIMIT :limit OFFSET :offset";

		return namedJdbcTemplate.query(query, param, SUBMISSION_MAPPER);
	}

	@Override
	public List<SubmissionInfo> listInfoForApprovedSubmissions(String accessRequirementId, long limit, long offset, boolean includeAccessorChanges) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue("accessRequirementId", accessRequirementId);
		param.addValue("limit", limit);
		param.addValue("offset", offset);

		String query = "SELECT S.ID, S.CREATED_BY, S.SUBMISSION_JSON->'$.researchProjectSnapshot' AS RESEARCH_PROJECT_SNAPSHOT, SS.MODIFIED_ON";
		if (includeAccessorChanges) {
			query += ", " + ACCESSOR_CHANGES_COLUMN;
		}
		query += " " + SQL_FROM_LATEST_APPROVED_SUBMISSION_PER_RESEARCH_PROJECT;

		return namedJdbcTemplate.query(query, param, (rs, rowNum) -> mapSubmissionInfo(rs, includeAccessorChanges));
	}

	// The ACL id requires each search to join the ACL table aliased A
	private static final String SQL_SELECT_SUBMISSION_DATA_AND_ACL_ID = "SELECT S.*, SS.STATE, SS.MODIFIED_ON, SS.MODIFIED_BY, SS.REASON, A.ID AS ACL_ID, "
			+ ACCESSOR_CHANGES_COLUMN
			+ " FROM DATA_ACCESS_SUBMISSION S JOIN DATA_ACCESS_SUBMISSION_STATUS SS ON S.ID = SS.SUBMISSION_ID";

	@Override
	public List<Submission> searchAllSubmissions(SubmissionReviewerFilterType reviewerFilterType, List<SubmissionSearchSort> sort,
			String accessorId, String accessRequirementId, String reviewerId, SubmissionState state, long limit, long offset) {
		ValidateArgument.required(reviewerFilterType, "reviewerType");
		ValidateArgument.requiredNotEmpty(sort, "sort");
	
		List<Object> queryParams = new ArrayList<>();
		List<String> additionalFilters = new ArrayList<>();
		
		String visibleSubmissionsQuery = SQL_SELECT_SUBMISSION_DATA_AND_ACL_ID  
			// Left join on the ACL table on the AR so that we can filter by "assigned acl" or not
			+ " LEFT JOIN " + TABLE_ACCESS_CONTROL_LIST + " A ON S." + COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_ID + " = A." + COL_ACL_OWNER_ID + " AND " +COL_ACL_OWNER_TYPE + "='" + ObjectType.ACCESS_REQUIREMENT.name() + "'";
		
		// Subquery to check if an ACL has REVIEW_SUBMISSIONS permission
		String hasReviewSubmissionsPermission = "EXISTS (SELECT 1 FROM " + TABLE_RESOURCE_ACCESS + " RA"
			+ " JOIN " + TABLE_RESOURCE_ACCESS_TYPE + " AT ON RA." + COL_RESOURCE_ACCESS_ID + " = AT." + COL_RESOURCE_ACCESS_TYPE_ID
			+ " WHERE RA." + COL_RESOURCE_ACCESS_OWNER + " = A." + COL_ACL_ID
			+ " AND AT." + COL_RESOURCE_ACCESS_TYPE_ELEMENT + " = '" + ACCESS_TYPE.REVIEW_SUBMISSIONS.name() + "')";

		// This needs to go in the where clause as we are left joining on the ACL
		switch (reviewerFilterType) {
		case ALL:
			// ACT can access everything, no need to filter on the ACL
			break;
		case ACT_ONLY:
			// Only submissions where there is no ACL with REVIEW_SUBMISSIONS permission
			additionalFilters.add("(A." + COL_ACL_ID + " IS NULL OR NOT " + hasReviewSubmissionsPermission + ")");
			break;
		case DELEGATED_ONLY:
			// Only submissions where there is an ACL with at least one REVIEW_SUBMISSIONS permission
			additionalFilters.add(hasReviewSubmissionsPermission);
			break;
		default:
			break;
		}
		
		visibleSubmissionsQuery = addAdditionalSubmissionSearchFilters(visibleSubmissionsQuery, queryParams, additionalFilters, accessorId, accessRequirementId, reviewerId, state, sort, limit, offset);
		
		return jdbcTemplate.query(visibleSubmissionsQuery, SUBMISSION_MAPPER, queryParams.toArray());
	}
	
	@Override
	public List<Submission> searchSubmissionsReviewableByGroups(Set<Long> groupIds, List<SubmissionSearchSort> sort, String accessorId,
			String accessRequirementId, String reviewerId, SubmissionState state, long limit, long offset) {
		ValidateArgument.requiredNotEmpty(groupIds, "groupIds");
		ValidateArgument.requiredNotEmpty(sort, "sort");
		
		List<Object> queryParams = new ArrayList<>();
		List<String> additionalFilters = new ArrayList<>();
		
		/* 
		 * Join on the resource access and filter by the group, this will ensure that we do not step the boundaries of what the principal can review
		 * 
		 * SELECT S.*, SS.STATE, SS.MODIFIED_ON, SS.MODIFIED_BY, SS.REASON, A.ID AS ACL_ID 
		 * FROM DATA_ACCESS_SUBMISSION S JOIN DATA_ACCESS_SUBMISSION_STATUS SS ON S.ID = SS.SUBMISSION_ID 
		 * JOIN ACL A ON S.ACCESS_REQUIREMENT_ID = A.OWNER_ID AND A.OWNER_TYPE='ACCESS_REQUIREMENT' 
		 * 	AND A.ID = (
		 * 		SELECT RA.OWNER_ID FROM ACL_RESOURCE_ACCESS RA  
		 * 		JOIN ACL_RESOURCE_ACCESS_TYPE AT ON RA.ID = AT.ID_OID AND AT.STRING_ELE = 'REVIEW_SUBMISSIONS' 
		 * 		WHERE RA.OWNER_ID = A.ID AND RA.GROUP_ID IN (?) LIMIT 1
		 * )
		*/
		String visibleSubmissionsQuery = SQL_SELECT_SUBMISSION_DATA_AND_ACL_ID 
			+ " JOIN " + TABLE_ACCESS_CONTROL_LIST + " A ON S." + COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_ID + " = A." + COL_ACL_OWNER_ID + " AND A." + COL_ACL_OWNER_TYPE + "='" + ObjectType.ACCESS_REQUIREMENT.name() + "'"
			// Limit to the ACL id for the given set of groups
			+ " AND A." + COL_ACL_ID + " = (SELECT RA." + COL_RESOURCE_ACCESS_OWNER + " FROM " + TABLE_RESOURCE_ACCESS + " RA "
			+ " JOIN " + TABLE_RESOURCE_ACCESS_TYPE + " AT ON RA." + COL_RESOURCE_ACCESS_ID + " = AT." + COL_RESOURCE_ACCESS_TYPE_ID + " AND AT." + COL_RESOURCE_ACCESS_TYPE_ELEMENT + " = '" + ACCESS_TYPE.REVIEW_SUBMISSIONS + "'"
			+ " WHERE RA." + COL_RESOURCE_ACCESS_OWNER + " = A." + COL_ACL_ID + " AND RA." + COL_RESOURCE_ACCESS_GROUP_ID + " IN (" + String.join(",", Collections.nCopies(groupIds.size(), "?")) + ") LIMIT 1)";
				
		queryParams.addAll(groupIds);
			
		visibleSubmissionsQuery = addAdditionalSubmissionSearchFilters(visibleSubmissionsQuery, queryParams, additionalFilters, accessorId, accessRequirementId, reviewerId, state, sort, limit, offset);
		
		return jdbcTemplate.query(visibleSubmissionsQuery, SUBMISSION_MAPPER, queryParams.toArray());
	}
	
	private String addAdditionalSubmissionSearchFilters(String visibleSubmissionsQuery, List<Object> queryParams, List<String> additionalFilters, String accessorId, String accessRequirementId, String reviewerId, SubmissionState state, List<SubmissionSearchSort> sort, long limit, long offset) {
		
		if (accessorId != null) {
			visibleSubmissionsQuery += " JOIN " + TABLE_DATA_ACCESS_SUBMISSION_ACCESSORS_CHANGES + " SA ON S." + COL_DATA_ACCESS_SUBMISSION_ID + " = SA." + COL_DATA_ACCESS_SUBMISSION_ACCESSOR_CHANGES_SUBMISSION_ID;
			additionalFilters.add("SA." + COL_DATA_ACCESS_SUBMISSION_ACCESSOR_CHANGES_ACCESSOR_ID + " = ?");
			queryParams.add(accessorId);
		}
		
		if (accessRequirementId != null) {
			additionalFilters.add("S." + COL_DATA_ACCESS_SUBMISSION_ACCESS_REQUIREMENT_ID + " = ?");
			queryParams.add(accessRequirementId);
		}
		
		if (state != null) {
			additionalFilters.add("SS." + COL_DATA_ACCESS_SUBMISSION_STATUS_STATE + " = ?");
			queryParams.add(state.name());
		}
		
		if (!additionalFilters.isEmpty()) {
			visibleSubmissionsQuery += " WHERE " + String.join(" AND ", additionalFilters);
		}
		
		// To filter by a specific reviewer we wrap the original query (that might have been filtered already by the principal itself)
		// so that we can filter on another potential reviewer
		if (reviewerId != null) {
			visibleSubmissionsQuery = "WITH VISIBLE_SUBMISSIONS AS (" + visibleSubmissionsQuery + ") SELECT * FROM VISIBLE_SUBMISSIONS S JOIN " + TABLE_RESOURCE_ACCESS + " RA ON S.ACL_ID = RA." + COL_RESOURCE_ACCESS_OWNER + " AND RA." + COL_RESOURCE_ACCESS_GROUP_ID + " = ?"
					+ " JOIN " + TABLE_RESOURCE_ACCESS_TYPE + " AT ON RA." + COL_RESOURCE_ACCESS_ID + " = AT." + COL_RESOURCE_ACCESS_TYPE_ID + " AND AT." + COL_RESOURCE_ACCESS_TYPE_ELEMENT + " = '" + ACCESS_TYPE.REVIEW_SUBMISSIONS + "'";
			queryParams.add(reviewerId);
		}
		
		visibleSubmissionsQuery += " ORDER BY " + String.join(",", sort.stream().map(s-> {
			ValidateArgument.required(s.getField(), "sort.field");
			return s.getField().name() + (s.getDirection() == null ? "" : " " + s.getDirection().name());
		}).collect(Collectors.toList()));
		
		visibleSubmissionsQuery += " LIMIT ? OFFSET ?";
			
		queryParams.add(limit);
		queryParams.add(offset);
		
		return visibleSubmissionsQuery;
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
					query += " "+ORDER_BY+" S."+COL_DATA_ACCESS_SUBMISSION_CREATED_ON;
					break;
				case MODIFIED_ON:
					query += " "+ORDER_BY+" SS."+COL_DATA_ACCESS_SUBMISSION_STATUS_MODIFIED_ON;
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
	public String getAccessRequirementId(String submissionId) {
		try {
			return jdbcTemplate.queryForObject(SQL_AR_ID_BY_SUBMISSION_ID, String.class, submissionId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException(String.format(SUBMISSION_DOES_NOT_EXIST, submissionId));
		}
	}
	
	@Override
	public void truncateAll() {
		jdbcTemplate.update("DELETE FROM " + TABLE_DATA_ACCESS_SUBMISSION);
	}
}
