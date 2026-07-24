package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_ACCESSOR_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_EXPIRED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_APPROVAL_SUBMITTER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_ACCESS_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_EDUC_ENVELOPE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_USER_REQUEST_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_USER_USER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_DATA_ACCESS_REQUEST_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_STATUS_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_STATUS_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_STATUS_STATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_SUBMISSION_STATUS_SUBMISSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACCESS_APPROVAL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACCESS_REQUIREMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DATA_ACCESS_REQUEST;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DATA_ACCESS_REQUEST_USER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DATA_ACCESS_SUBMISSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DATA_ACCESS_SUBMISSION_STATUS;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dataaccess.AccessRequestSortField;
import org.sagebionetworks.repo.model.dataaccess.AccessType;
import org.sagebionetworks.repo.model.dataaccess.SortDirection;
import org.sagebionetworks.repo.model.dataaccess.AccessorChange;
import org.sagebionetworks.repo.model.dataaccess.PrincipalInvestigator;
import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dataaccess.SubmissionState;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.transactions.MandatoryWriteTransaction;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class DBORequestDAOImpl implements RequestDAO {

	public static final String DATA_ACCESS_REQUEST_DOES_NOT_EXIST = "Data access request: '%s' does not exist";

	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private IdGenerator idGenerator;

	public static final String SQL_DELETE = "DELETE FROM " + TABLE_DATA_ACCESS_REQUEST
			+ " WHERE " + COL_DATA_ACCESS_REQUEST_ID + " = ?";

	public static final String SQL_GET = "SELECT *"
			+ " FROM " + TABLE_DATA_ACCESS_REQUEST
			+ " WHERE " + COL_DATA_ACCESS_REQUEST_ACCESS_REQUIREMENT_ID + " = ?"
			+ " AND " + COL_DATA_ACCESS_REQUEST_CREATED_BY + " = ?";

	public static final String SQL_GET_BY_ID = "SELECT *"
			+ " FROM " + TABLE_DATA_ACCESS_REQUEST
			+ " WHERE " + COL_DATA_ACCESS_REQUEST_ID + " = ?";

	public static final String SQL_GET_AR_ID_BY_ID = "SELECT " + COL_DATA_ACCESS_REQUEST_ACCESS_REQUIREMENT_ID
			+ " FROM " + TABLE_DATA_ACCESS_REQUEST
			+ " WHERE " + COL_DATA_ACCESS_REQUEST_ID + " = ?";

	public static final String SQL_GET_FOR_UPDATE = SQL_GET_BY_ID + " FOR UPDATE";

	private static final String SQL_DELETE_REQUEST_USERS = "DELETE FROM " + TABLE_DATA_ACCESS_REQUEST_USER
			+ " WHERE " + COL_DATA_ACCESS_REQUEST_USER_REQUEST_ID + " = ?";

	private static final String SQL_GET_USER_REQUESTS_BASE =
			"SELECT r." + COL_DATA_ACCESS_REQUEST_ID + " AS REQUEST_ID"
			+ ", r." + COL_DATA_ACCESS_REQUEST_ACCESS_REQUIREMENT_ID + " AS ACCESS_REQUIREMENT_ID"
			+ ", ar." + COL_ACCESS_REQUIREMENT_NAME + " AS ACCESS_REQUIREMENT_NAME"
			+ ", ss." + COL_DATA_ACCESS_SUBMISSION_STATUS_STATE + " AS SUBMISSION_STATUS"
			+ ", r." + COL_DATA_ACCESS_REQUEST_EDUC_ENVELOPE_ID + " AS ENVELOPE_ID"
			+ ", ss." + COL_DATA_ACCESS_SUBMISSION_STATUS_CREATED_ON + " AS SUBMITTED_ON"
			+ ", ss." + COL_DATA_ACCESS_SUBMISSION_STATUS_MODIFIED_ON + " AS MODIFIED_ON"
			+ ", aa." + COL_ACCESS_APPROVAL_EXPIRED_ON + " AS EXPIRES_ON"
			+ " FROM " + TABLE_DATA_ACCESS_REQUEST + " r"
			+ " JOIN " + TABLE_DATA_ACCESS_REQUEST_USER + " ru ON r." + COL_DATA_ACCESS_REQUEST_ID + " = ru." + COL_DATA_ACCESS_REQUEST_USER_REQUEST_ID
			+ " JOIN " + TABLE_ACCESS_REQUIREMENT + " ar ON r." + COL_DATA_ACCESS_REQUEST_ACCESS_REQUIREMENT_ID + " = ar.ID"
			+ " LEFT JOIN " + TABLE_DATA_ACCESS_SUBMISSION + " s ON s." + COL_DATA_ACCESS_SUBMISSION_DATA_ACCESS_REQUEST_ID + " = r." + COL_DATA_ACCESS_REQUEST_ID
			+ " LEFT JOIN " + TABLE_DATA_ACCESS_SUBMISSION_STATUS + " ss ON ss." + COL_DATA_ACCESS_SUBMISSION_STATUS_SUBMISSION_ID + " = s." + COL_DATA_ACCESS_SUBMISSION_ID
			+ " LEFT JOIN " + TABLE_ACCESS_APPROVAL + " aa ON aa." + COL_ACCESS_APPROVAL_REQUIREMENT_ID + " = r." + COL_DATA_ACCESS_REQUEST_ACCESS_REQUIREMENT_ID
			+ " AND aa." + COL_ACCESS_APPROVAL_ACCESSOR_ID + " = ru." + COL_DATA_ACCESS_REQUEST_USER_USER_ID
			+ " AND aa." + COL_ACCESS_APPROVAL_SUBMITTER_ID + " = s." + COL_DATA_ACCESS_SUBMISSION_CREATED_BY
			+ " WHERE ru." + COL_DATA_ACCESS_REQUEST_USER_USER_ID + " = ?";

	private static final RowMapper<DBORequest> MAPPER = new DBORequest().getTableMapping();

	private static final RowMapper<RequestUserInfo> USER_REQUEST_MAPPER = (ResultSet rs, int rowNum) -> {
		RequestUserInfo info = new RequestUserInfo();
		info.setRequestId(String.valueOf(rs.getLong("REQUEST_ID")));
		info.setAccessRequirementId(String.valueOf(rs.getLong("ACCESS_REQUIREMENT_ID")));
		info.setAccessRequirementName(rs.getString("ACCESS_REQUIREMENT_NAME"));
		String state = rs.getString("SUBMISSION_STATUS");
		info.setSubmissionStatus(state != null ? SubmissionState.valueOf(state) : null);
		info.setEnvelopeId(rs.getString("ENVELOPE_ID"));
		long submittedOn = rs.getLong("SUBMITTED_ON");
		info.setSubmittedOn(rs.wasNull() ? null : new Date(submittedOn));
		long modifiedOn = rs.getLong("MODIFIED_ON");
		info.setModifiedOn(rs.wasNull() ? null : new Date(modifiedOn));
		long expiresOn = rs.getLong("EXPIRES_ON");
		info.setExpiresOn(rs.wasNull() || expiresOn == 0 ? null : new Date(expiresOn));
		return info;
	};

	@WriteTransaction
	@Override
	public Request create(Request toCreate) {
		toCreate.setId(idGenerator.generateNewId(IdType.DATA_ACCESS_REQUEST_ID).toString());
		toCreate.setEtag(UUID.randomUUID().toString());
		DBORequest dbo = new DBORequest();
		RequestUtils.copyDtoToDbo(toCreate, dbo);
		basicDao.createNew(dbo);
		populateRequestUsers(toCreate);
		return (Request) getUserOwnCurrentRequest(toCreate.getAccessRequirementId(), toCreate.getCreatedBy());
	}

	@Override
	public RequestInterface getUserOwnCurrentRequest(String accessRequirementId, String userId)
			throws NotFoundException {
		try {
			DBORequest dbo = jdbcTemplate.queryForObject(SQL_GET, MAPPER, accessRequirementId, userId);
			return RequestUtils.copyDboToDto(dbo);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException(String.format("Data access request does not exist for access requirement: '%s' and user id: '%s'", accessRequirementId, userId));
		}
	}

	@WriteTransaction
	@Override
	public RequestInterface update(RequestInterface toUpdate) throws NotFoundException {
		DBORequest dbo = new DBORequest();
		RequestUtils.copyDtoToDbo(toUpdate, dbo);
		dbo.setEtag(UUID.randomUUID().toString());
		basicDao.update(dbo);
		populateRequestUsers(toUpdate);
		return getUserOwnCurrentRequest(toUpdate.getAccessRequirementId(), toUpdate.getCreatedBy());
	}

	@Override
	public void delete(String id) {
		// Note that we don't have to clean up the TABLE_DATA_ACCESS_REQUEST_USER table
		// since that's taken care of by the 'on delete cascade' foreign key constraint
		jdbcTemplate.update(SQL_DELETE, id);
	}

	@MandatoryWriteTransaction
	@Override
	public RequestInterface getForUpdate(String id) {
		try {
			DBORequest dbo = jdbcTemplate.queryForObject(SQL_GET_FOR_UPDATE, MAPPER, id);
			return RequestUtils.copyDboToDto(dbo);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException(String.format(DATA_ACCESS_REQUEST_DOES_NOT_EXIST, id));
		}
	}

	@Override
	public RequestInterface get(String id) {
		try {
			DBORequest dbo = jdbcTemplate.queryForObject(SQL_GET_BY_ID, MAPPER, id);
			return RequestUtils.copyDboToDto(dbo);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException(String.format(DATA_ACCESS_REQUEST_DOES_NOT_EXIST, id));
		}
	}

	@Override
	public String getAccessRequirementId(String requestId) {
		try {
			return jdbcTemplate.queryForObject(SQL_GET_AR_ID_BY_ID, String.class, requestId);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException(String.format(DATA_ACCESS_REQUEST_DOES_NOT_EXIST, requestId));
		}
	}

	@Override
	public List<RequestUserInfo> getUserRequests(Long userId, long limit, long offset,
			AccessRequestSortField sortBy, SortDirection sortDirection) {
		String orderBy = toOrderByClause(sortBy, sortDirection);
		String sql = SQL_GET_USER_REQUESTS_BASE + orderBy + " LIMIT ? OFFSET ?";
		return jdbcTemplate.query(sql, USER_REQUEST_MAPPER, userId, limit, offset);
	}

	static String toOrderByClause(AccessRequestSortField sortBy, SortDirection sortDirection) {
		String field = sortBy != null ? sortBy.name() : AccessRequestSortField.MODIFIED_ON.name();
		String direction = sortDirection != null ? sortDirection.name() : SortDirection.DESC.name();
		return " ORDER BY " + field + " " + direction;
	}

	@Override
	public void truncateAll() {
		jdbcTemplate.update("DELETE FROM " + TABLE_DATA_ACCESS_REQUEST);
	}

	private void populateRequestUsers(RequestInterface request) {
		Long requestId = Long.parseLong(request.getId());
		jdbcTemplate.update(SQL_DELETE_REQUEST_USERS, requestId);

		List<DBORequestUser> users = buildRequestUsers(request);
		if (!users.isEmpty()) {
			basicDao.createBatch(users);
		}
	}

	static List<DBORequestUser> buildRequestUsers(RequestInterface request) {
		Long requestId = Long.parseLong(request.getId());
		LinkedHashSet<Long> userIds = new LinkedHashSet<>();

		userIds.add(Long.parseLong(request.getCreatedBy()));

		PrincipalInvestigator pi = request.getPrincipalInvestigator();
		if (pi != null && pi.getUserId() != null) {
			userIds.add(Long.parseLong(pi.getUserId()));
		}

		List<AccessorChange> accessorChanges = request.getAccessorChanges();
		if (accessorChanges != null) {
			for (AccessorChange change : accessorChanges) {
				if (AccessType.GAIN_ACCESS.equals(change.getType())
						|| AccessType.RENEW_ACCESS.equals(change.getType())) {
					userIds.add(Long.parseLong(change.getUserId()));
				}
			}
		}

		List<DBORequestUser> result = new ArrayList<>(userIds.size());
		for (Long userId : userIds) {
			DBORequestUser dbo = new DBORequestUser();
			dbo.setRequestId(requestId);
			dbo.setUserId(userId);
			result.add(dbo);
		}
		return result;
	}
}
