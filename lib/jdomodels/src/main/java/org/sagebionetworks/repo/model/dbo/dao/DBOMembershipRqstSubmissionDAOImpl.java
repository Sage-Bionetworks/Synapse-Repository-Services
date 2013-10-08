/**
 * 
 */
package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GROUP_MEMBERS_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GROUP_MEMBERS_MEMBER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MEMBERSHIP_REQUEST_SUBMISSION_EXPIRES_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MEMBERSHIP_REQUEST_SUBMISSION_PROPERTIES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MEMBERSHIP_REQUEST_SUBMISSION_TEAM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MEMBERSHIP_REQUEST_SUBMISSION_USER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TEAM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.LIMIT_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.OFFSET_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_GROUP_MEMBERS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_MEMBERSHIP_REQUEST_SUBMISSION;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.MembershipRequest;
import org.sagebionetworks.repo.model.MembershipRqstSubmission;
import org.sagebionetworks.repo.model.MembershipRqstSubmissionDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMembershipRqstSubmission;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author brucehoff
 *
 */
public class DBOMembershipRqstSubmissionDAOImpl implements MembershipRqstSubmissionDAO {

	@Autowired
	private DBOBasicDao basicDao;	
	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	@Autowired
	GroupMembersDAO groupMembersDAO;
	
	private static final String SELECT_OPEN_REQUESTS_BY_TEAM_CORE = 
			" FROM "+TABLE_MEMBERSHIP_REQUEST_SUBMISSION+
			" mrs WHERE mrs."+COL_MEMBERSHIP_REQUEST_SUBMISSION_TEAM_ID+"=:"+COL_MEMBERSHIP_REQUEST_SUBMISSION_TEAM_ID+
			" AND mrs."+COL_MEMBERSHIP_REQUEST_SUBMISSION_TEAM_ID+" NOT IN (SELECT "+COL_GROUP_MEMBERS_GROUP_ID+" FROM "+
				TABLE_GROUP_MEMBERS+" WHERE "+COL_GROUP_MEMBERS_MEMBER_ID+"=mrs."+COL_MEMBERSHIP_REQUEST_SUBMISSION_USER_ID+" ) "+
			" AND ( mrs."+COL_MEMBERSHIP_REQUEST_SUBMISSION_EXPIRES_ON+" IS NULL OR mrs."+COL_MEMBERSHIP_REQUEST_SUBMISSION_EXPIRES_ON+">:"+COL_MEMBERSHIP_REQUEST_SUBMISSION_EXPIRES_ON+" ) ";
	
	private static final String SELECT_OPEN_REQUESTS_BY_TEAM_PAGINATED = 
			"SELECT mrs.* "+SELECT_OPEN_REQUESTS_BY_TEAM_CORE+
			" LIMIT :"+LIMIT_PARAM_NAME+" OFFSET :"+OFFSET_PARAM_NAME;
	
	private static final String SELECT_OPEN_REQUESTS_BY_TEAM_COUNT = 
			"SELECT COUNT(*) "+SELECT_OPEN_REQUESTS_BY_TEAM_CORE;
	
	private static final String SELECT_OPEN_REQUESTS_BY_TEAM_AND_REQUESTOR_CORE = 
			SELECT_OPEN_REQUESTS_BY_TEAM_CORE+
			" AND mrs."+COL_MEMBERSHIP_REQUEST_SUBMISSION_USER_ID+"=:"+COL_MEMBERSHIP_REQUEST_SUBMISSION_USER_ID;
	
	private static final String SELECT_OPEN_REQUESTS_BY_TEAM_AND_REQUESTOR_PAGINATED = 
			"SELECT mrs.* "+SELECT_OPEN_REQUESTS_BY_TEAM_AND_REQUESTOR_CORE+
			" LIMIT :"+LIMIT_PARAM_NAME+" OFFSET :"+OFFSET_PARAM_NAME;
	
	private static final String SELECT_OPEN_REQUESTS_BY_TEAM_AND_REQUESTOR_COUNT = 
			"SELECT COUNT(*) "+SELECT_OPEN_REQUESTS_BY_TEAM_AND_REQUESTOR_CORE;
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.MemberRqstSubmissionDAO#create(org.sagebionetworks.repo.model.MemberRqstSubmission)
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public MembershipRqstSubmission create(MembershipRqstSubmission dto) throws DatastoreException,
	InvalidModelException {
		DBOMembershipRqstSubmission dbo = new DBOMembershipRqstSubmission();
		MembershipRqstSubmissionUtils.copyDtoToDbo(dto, dbo);
		if (dbo.getId()==null) dbo.setId(idGenerator.generateNewId());
		dbo = basicDao.createNew(dbo);
		MembershipRqstSubmission result = MembershipRqstSubmissionUtils.copyDboToDto(dbo);
		return result;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.MembershipRqstSubmissionDAO#get(java.lang.String)
	 */
	@Override
	public MembershipRqstSubmission get(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_TEAM_ID.toLowerCase(), id);
		DBOMembershipRqstSubmission dbo = basicDao.getObjectByPrimaryKey(DBOMembershipRqstSubmission.class, param);
		MembershipRqstSubmission dto = MembershipRqstSubmissionUtils.copyDboToDto(dbo);
		return dto;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.MembershipRqstSubmissionDAO#delete(java.lang.String)
	 */
	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_TEAM_ID.toLowerCase(), id);
		basicDao.deleteObjectByPrimaryKey(DBOMembershipRqstSubmission.class, param);
	}

	private static final RowMapper<MembershipRequest> membershipRequestRowMapper = new RowMapper<MembershipRequest>(){
		@Override
		public MembershipRequest mapRow(ResultSet rs, int rowNum) throws SQLException {
			MembershipRequest mr = new MembershipRequest();
			mr.setUserId(""+rs.getLong(COL_MEMBERSHIP_REQUEST_SUBMISSION_USER_ID));
			mr.setTeamId(""+rs.getLong(COL_MEMBERSHIP_REQUEST_SUBMISSION_TEAM_ID));
			mr.setExpiresOn(new Date(rs.getLong(COL_MEMBERSHIP_REQUEST_SUBMISSION_EXPIRES_ON)));

			Blob mrsProperties = rs.getBlob(COL_MEMBERSHIP_REQUEST_SUBMISSION_PROPERTIES);
			MembershipRqstSubmission mrs = MembershipRqstSubmissionUtils.deserialize(mrsProperties.getBytes(1, (int) mrsProperties.length()));
			mr.setMessage(mrs.getMessage());
			return mr;
		}
	};
	
	@Override
	public List<MembershipRequest> getOpenByTeamInRange(long teamId, long now,
			long offset, long limit) throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(COL_MEMBERSHIP_REQUEST_SUBMISSION_TEAM_ID, teamId);
		param.addValue(OFFSET_PARAM_NAME, offset);
		if (limit<=0) throw new IllegalArgumentException("'to' param must be greater than 'from' param.");
		param.addValue(LIMIT_PARAM_NAME, limit);	
		param.addValue(COL_MEMBERSHIP_REQUEST_SUBMISSION_EXPIRES_ON, now);	
		return simpleJdbcTemplate.query(SELECT_OPEN_REQUESTS_BY_TEAM_PAGINATED, membershipRequestRowMapper, param);
	}

	@Override
	public long getOpenByTeamCount(long teamId, long now)
			throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(COL_MEMBERSHIP_REQUEST_SUBMISSION_TEAM_ID, teamId);
		param.addValue(COL_MEMBERSHIP_REQUEST_SUBMISSION_EXPIRES_ON, now);	
		return simpleJdbcTemplate.queryForLong(SELECT_OPEN_REQUESTS_BY_TEAM_COUNT, param);
	}

	@Override
	public List<MembershipRequest> getOpenByTeamAndRequestorInRange(
			long teamId, long requestorId, long now, long offset, long limit)
			throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(COL_MEMBERSHIP_REQUEST_SUBMISSION_TEAM_ID, teamId);
		param.addValue(COL_MEMBERSHIP_REQUEST_SUBMISSION_USER_ID, requestorId);
		param.addValue(OFFSET_PARAM_NAME, offset);
		if (limit<=0) throw new IllegalArgumentException("'to' param must be greater than 'from' param.");
		param.addValue(LIMIT_PARAM_NAME, limit);	
		param.addValue(COL_MEMBERSHIP_REQUEST_SUBMISSION_EXPIRES_ON, now);	
		return simpleJdbcTemplate.query(SELECT_OPEN_REQUESTS_BY_TEAM_AND_REQUESTOR_PAGINATED, membershipRequestRowMapper, param);
	}

	@Override
	public long getOpenByTeamAndRequestorCount(long teamId,
			long requestorId, long now) throws DatastoreException,
			NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(COL_MEMBERSHIP_REQUEST_SUBMISSION_TEAM_ID, teamId);
		param.addValue(COL_MEMBERSHIP_REQUEST_SUBMISSION_USER_ID, requestorId);
		param.addValue(COL_MEMBERSHIP_REQUEST_SUBMISSION_EXPIRES_ON, now);	
		return simpleJdbcTemplate.queryForLong(SELECT_OPEN_REQUESTS_BY_TEAM_AND_REQUESTOR_COUNT, param);
	}

}
