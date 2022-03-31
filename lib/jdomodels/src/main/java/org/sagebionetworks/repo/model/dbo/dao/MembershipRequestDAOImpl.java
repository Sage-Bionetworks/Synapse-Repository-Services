/**
 * 
 */
package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GROUP_MEMBERS_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GROUP_MEMBERS_MEMBER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MEMBERSHIP_REQUEST_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MEMBERSHIP_REQUEST_EXPIRES_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MEMBERSHIP_REQUEST_TEAM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MEMBERSHIP_REQUEST_USER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TEAM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.LIMIT_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.OFFSET_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_GROUP_MEMBERS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_MEMBERSHIP_REQUEST;

import java.util.List;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.MembershipRequest;
import org.sagebionetworks.repo.model.MembershipRequestDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMembershipRequest;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * @author brucehoff
 *
 */
public class MembershipRequestDAOImpl implements MembershipRequestDAO {

	@Autowired
	private DBOBasicDao basicDao;
	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	private NamedParameterJdbcTemplate namedJdbcTemplate;
	@Autowired
	GroupMembersDAO groupMembersDAO;
	
	private static final String SELECT_OPEN_REQUESTS_CORE = 
			" FROM "+ TABLE_MEMBERSHIP_REQUEST +" mrs"
			+ " WHERE mrs."+ COL_MEMBERSHIP_REQUEST_TEAM_ID +" NOT IN ("
					+ "SELECT "+COL_GROUP_MEMBERS_GROUP_ID
					+" FROM "+TABLE_GROUP_MEMBERS
					+" WHERE "+COL_GROUP_MEMBERS_MEMBER_ID+"=mrs."+ COL_MEMBERSHIP_REQUEST_USER_ID
					+" ) "
			+" AND ( mrs."+ COL_MEMBERSHIP_REQUEST_EXPIRES_ON +" IS NULL"
					+ " OR mrs."+ COL_MEMBERSHIP_REQUEST_EXPIRES_ON +">:"+ COL_MEMBERSHIP_REQUEST_EXPIRES_ON
					+" ) ";

	private static final String SELECT_OPEN_REQUESTS_BY_TEAMS_COUNT =
			"SELECT COUNT(*)"
			+SELECT_OPEN_REQUESTS_CORE
			+" AND mrs."+ COL_MEMBERSHIP_REQUEST_TEAM_ID +" IN (:"+ COL_MEMBERSHIP_REQUEST_TEAM_ID +")";

	private static final String SELECT_OPEN_REQUESTS_BY_TEAM_CORE = 
			SELECT_OPEN_REQUESTS_CORE
			+" AND mrs."+ COL_MEMBERSHIP_REQUEST_TEAM_ID +"=:"+ COL_MEMBERSHIP_REQUEST_TEAM_ID;
	
	private static final String SELECT_OPEN_REQUESTS_BY_REQUESTER_CORE = 
			SELECT_OPEN_REQUESTS_CORE
			+" AND mrs."+ COL_MEMBERSHIP_REQUEST_USER_ID +"=:"+ COL_MEMBERSHIP_REQUEST_USER_ID;
	
	private static final String SELECT_OPEN_REQUESTS_BY_TEAM_PAGINATED = 
			"SELECT mrs.* "
			+SELECT_OPEN_REQUESTS_BY_TEAM_CORE
			+" ORDER BY "+ COL_MEMBERSHIP_REQUEST_CREATED_ON +" DESC "
			+" LIMIT :"+LIMIT_PARAM_NAME+" OFFSET :"+OFFSET_PARAM_NAME;
	
	private static final String SELECT_OPEN_REQUESTS_BY_TEAM_COUNT = 
			"SELECT COUNT(*) "+SELECT_OPEN_REQUESTS_BY_TEAM_CORE;
	
	private static final String SELECT_OPEN_REQUESTS_BY_REQUESTER_PAGINATED = 
			"SELECT mrs.* "+SELECT_OPEN_REQUESTS_BY_REQUESTER_CORE
			+" ORDER BY "+ COL_MEMBERSHIP_REQUEST_CREATED_ON +" DESC "
			+" LIMIT :"+LIMIT_PARAM_NAME+" OFFSET :"+OFFSET_PARAM_NAME;
	
	private static final String SELECT_OPEN_REQUESTS_BY_REQUESTER_COUNT = 
			"SELECT COUNT(*) "+SELECT_OPEN_REQUESTS_BY_REQUESTER_CORE;
	
	private static final String SELECT_OPEN_REQUESTS_BY_TEAM_AND_REQUESTER_CORE = 
			SELECT_OPEN_REQUESTS_BY_TEAM_CORE
			+" AND mrs."+ COL_MEMBERSHIP_REQUEST_USER_ID +"=:"+ COL_MEMBERSHIP_REQUEST_USER_ID;
	
	private static final String SELECT_OPEN_REQUESTS_BY_TEAM_AND_REQUESTER_PAGINATED = 
			"SELECT mrs.* "
			+SELECT_OPEN_REQUESTS_BY_TEAM_AND_REQUESTER_CORE
			+" ORDER BY "+ COL_MEMBERSHIP_REQUEST_CREATED_ON +" DESC "
			+" LIMIT :"+LIMIT_PARAM_NAME+" OFFSET :"+OFFSET_PARAM_NAME;
	
	private static final String SELECT_OPEN_REQUESTS_BY_TEAM_AND_REQUESTER_COUNT = 
			"SELECT COUNT(*) "+SELECT_OPEN_REQUESTS_BY_TEAM_AND_REQUESTER_CORE;
	
	private static final String DELETE_REQUESTS_BY_TEAM_AND_REQUESTER = 
			"DELETE FROM "+ TABLE_MEMBERSHIP_REQUEST
			+" WHERE "+ COL_MEMBERSHIP_REQUEST_TEAM_ID +"=:"+ COL_MEMBERSHIP_REQUEST_TEAM_ID
			+" AND "+ COL_MEMBERSHIP_REQUEST_USER_ID +"=:"+ COL_MEMBERSHIP_REQUEST_USER_ID;

	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.MemberRqstSubmissionDAO#create(org.sagebionetworks.repo.model.MemberRqstSubmission)
	 */
	@WriteTransaction
	@Override
	public MembershipRequest create(MembershipRequest dto) throws DatastoreException,
	InvalidModelException {
		DBOMembershipRequest dbo = new DBOMembershipRequest();
		MembershipRequestUtils.copyDtoToDbo(dto, dbo);
		if (dbo.getId()==null) dbo.setId(idGenerator.generateNewId(IdType.MEMBERSHIP_REQUEST_SUBMISSION_ID));
		dbo = basicDao.createNew(dbo);
		MembershipRequest result = MembershipRequestUtils.copyDboToDto(dbo);
		return result;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.MembershipRequestDAO#get(java.lang.String)
	 */
	@Override
	public MembershipRequest get(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_TEAM_ID.toLowerCase(), id);
		DBOMembershipRequest dbo = basicDao.getObjectByPrimaryKey(DBOMembershipRequest.class, param)
				.orElseThrow(() -> new NotFoundException(String.format("Membership request '%s' does not exist", id)));
		MembershipRequest dto = MembershipRequestUtils.copyDboToDto(dbo);
		return dto;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.MembershipRequestDAO#delete(java.lang.String)
	 */
	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_TEAM_ID.toLowerCase(), id);
		basicDao.deleteObjectByPrimaryKey(DBOMembershipRequest.class, param);
	}

	private static final RowMapper<DBOMembershipRequest> dboMembershipRequestRowMapper =
			(new DBOMembershipRequest()).getTableMapping();
	
	private static final RowMapper<MembershipRequest> membershipRequestRowMapper = (rs, rowNum) -> {
		DBOMembershipRequest dbo = dboMembershipRequestRowMapper.mapRow(rs,  rowNum);
		return MembershipRequestUtils.copyDboToDto(dbo);
	};
	
	private <T> List<T> getOpenByTeamAndRequesterInRange(
			long teamId, long requesterId, long now, long limit, long offset, RowMapper<T> rowMapper)
			throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(COL_MEMBERSHIP_REQUEST_TEAM_ID, teamId);
		param.addValue(COL_MEMBERSHIP_REQUEST_USER_ID, requesterId);
		param.addValue(OFFSET_PARAM_NAME, offset);
		if (limit<=0) throw new IllegalArgumentException("'to' param must be greater than 'from' param.");
		param.addValue(LIMIT_PARAM_NAME, limit);	
		param.addValue(COL_MEMBERSHIP_REQUEST_EXPIRES_ON, now);
		return namedJdbcTemplate.query(SELECT_OPEN_REQUESTS_BY_TEAM_AND_REQUESTER_PAGINATED, param, rowMapper);
	}
	
	@Override
	public List<MembershipRequest> getOpenByTeamInRange(long teamId, long now, long limit,
			long offset) throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(COL_MEMBERSHIP_REQUEST_TEAM_ID, teamId);
		param.addValue(OFFSET_PARAM_NAME, offset);
		if (limit<=0) throw new IllegalArgumentException("'to' param must be greater than 'from' param.");
		param.addValue(LIMIT_PARAM_NAME, limit);	
		param.addValue(COL_MEMBERSHIP_REQUEST_EXPIRES_ON, now);
		return namedJdbcTemplate.query(SELECT_OPEN_REQUESTS_BY_TEAM_PAGINATED, param, membershipRequestRowMapper);
	}

	@Deprecated
	@Override
	public long getOpenByTeamCount(long teamId, long now)
			throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(COL_MEMBERSHIP_REQUEST_TEAM_ID, teamId);
		param.addValue(COL_MEMBERSHIP_REQUEST_EXPIRES_ON, now);
		return namedJdbcTemplate.queryForObject(SELECT_OPEN_REQUESTS_BY_TEAM_COUNT, param, Long.class);
	}

	@Override
	public long getOpenByTeamsCount(List<String> teamIds, long expireAfter) {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(COL_MEMBERSHIP_REQUEST_TEAM_ID, teamIds);
		param.addValue(COL_MEMBERSHIP_REQUEST_EXPIRES_ON, expireAfter);
		return namedJdbcTemplate.queryForObject(SELECT_OPEN_REQUESTS_BY_TEAMS_COUNT, param, Long.class);
	}

	@Override
	public List<MembershipRequest> getOpenByTeamAndRequesterInRange(
			long teamId, long requesterId, long now, long limit, long offset)
			throws DatastoreException, NotFoundException {
		return getOpenByTeamAndRequesterInRange(teamId, requesterId, now, limit, offset, membershipRequestRowMapper);
	}

	@Deprecated
	@Override
	public long getOpenByTeamAndRequesterCount(long teamId,
			long requestorId, long now) throws DatastoreException,
			NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(COL_MEMBERSHIP_REQUEST_TEAM_ID, teamId);
		param.addValue(COL_MEMBERSHIP_REQUEST_USER_ID, requestorId);
		param.addValue(COL_MEMBERSHIP_REQUEST_EXPIRES_ON, now);
		return namedJdbcTemplate.queryForObject(SELECT_OPEN_REQUESTS_BY_TEAM_AND_REQUESTER_COUNT, param, Long.class);
	}

	@Override
	public List<MembershipRequest> getOpenByRequesterInRange(
			long requesterId, long now, long limit, long offset) {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(COL_MEMBERSHIP_REQUEST_USER_ID, requesterId);
		param.addValue(OFFSET_PARAM_NAME, offset);
		if (limit<=0) throw new IllegalArgumentException("'to' param must be greater than 'from' param.");
		param.addValue(LIMIT_PARAM_NAME, limit);	
		param.addValue(COL_MEMBERSHIP_REQUEST_EXPIRES_ON, now);
		return namedJdbcTemplate.query(SELECT_OPEN_REQUESTS_BY_REQUESTER_PAGINATED, param, membershipRequestRowMapper);
	}

	@Deprecated
	@Override
	public long getOpenByRequesterCount(long requesterId, long now) throws DatastoreException,
			NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(COL_MEMBERSHIP_REQUEST_USER_ID, requesterId);
		param.addValue(COL_MEMBERSHIP_REQUEST_EXPIRES_ON, now);
		return namedJdbcTemplate.queryForObject(SELECT_OPEN_REQUESTS_BY_REQUESTER_COUNT, param, Long.class);
	}


	@WriteTransaction
	@Override
	public void deleteByTeamAndRequester(long teamId, long requesterId)
			throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(COL_MEMBERSHIP_REQUEST_TEAM_ID, teamId);
		param.addValue(COL_MEMBERSHIP_REQUEST_USER_ID, requesterId);
		namedJdbcTemplate.update(DELETE_REQUESTS_BY_TEAM_AND_REQUESTER, param);
	}

}
