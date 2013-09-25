/**
 * 
 */
package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GROUP_MEMBERS_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GROUP_MEMBERS_MEMBER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MEMBERSHIP_REQUEST_SUBMISSION_EXPIRES_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MEMBERSHIP_REQUEST_SUBMISSION_TEAM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MEMBERSHIP_REQUEST_SUBMISSION_USER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TEAM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.LIMIT_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.OFFSET_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_GROUP_MEMBERS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_MEMBERSHIP_REQUEST_SUBMISSION;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.ids.ETagGenerator;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
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
	private ETagGenerator eTagGenerator;
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	@Autowired
	GroupMembersDAO groupMembersDAO;

	private static final String SELECT_OPEN_REQUESTS_BY_TEAM_PAGINATED = 
			"SELECT mrs.* FROM "+TABLE_MEMBERSHIP_REQUEST_SUBMISSION+" mrs "+
			" WHERE mrs."+COL_MEMBERSHIP_REQUEST_SUBMISSION_TEAM_ID+"=:"+COL_MEMBERSHIP_REQUEST_SUBMISSION_TEAM_ID+
			" AND mrs."+COL_MEMBERSHIP_REQUEST_SUBMISSION_TEAM_ID+" NOT IN (SELECT "+COL_GROUP_MEMBERS_GROUP_ID+" FROM "+
				TABLE_GROUP_MEMBERS+" WHERE "+COL_GROUP_MEMBERS_MEMBER_ID+"=mrs."+COL_MEMBERSHIP_REQUEST_SUBMISSION_USER_ID+" ) "+
			" AND mrs."+COL_MEMBERSHIP_REQUEST_SUBMISSION_EXPIRES_ON+">:"+COL_MEMBERSHIP_REQUEST_SUBMISSION_EXPIRES_ON+
			" LIMIT :"+LIMIT_PARAM_NAME+" OFFSET :"+OFFSET_PARAM_NAME;
	
	private static final String SELECT_OPEN_REQUESTS_BY_TEAM_AND_REQUESTOR_PAGINATED = 
			"SELECT mrs.* FROM "+TABLE_MEMBERSHIP_REQUEST_SUBMISSION+" mrs "+
					" WHERE mrs."+COL_MEMBERSHIP_REQUEST_SUBMISSION_TEAM_ID+"=:"+COL_MEMBERSHIP_REQUEST_SUBMISSION_TEAM_ID+
			" AND mrs."+COL_MEMBERSHIP_REQUEST_SUBMISSION_USER_ID+"=:"+COL_MEMBERSHIP_REQUEST_SUBMISSION_USER_ID+
			" AND mrs."+COL_MEMBERSHIP_REQUEST_SUBMISSION_TEAM_ID+" NOT IN (SELECT "+COL_GROUP_MEMBERS_GROUP_ID+" FROM "+
				TABLE_GROUP_MEMBERS+" WHERE "+COL_GROUP_MEMBERS_MEMBER_ID+"=mrs."+COL_MEMBERSHIP_REQUEST_SUBMISSION_USER_ID+" ) "+
			" AND mrs."+COL_MEMBERSHIP_REQUEST_SUBMISSION_EXPIRES_ON+">:"+COL_MEMBERSHIP_REQUEST_SUBMISSION_EXPIRES_ON+
			" LIMIT :"+LIMIT_PARAM_NAME+" OFFSET :"+OFFSET_PARAM_NAME;
	
	private static final RowMapper<DBOMembershipRqstSubmission> teamRowMapper = (new DBOMembershipRqstSubmission()).getTableMapping();
	
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
		dbo.setEtag(eTagGenerator.generateETag());
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

	@Override
	public List<MembershipRqstSubmission> getOpenByTeamInRange(long teamId, long now,
			long offset, long limit) throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(COL_MEMBERSHIP_REQUEST_SUBMISSION_TEAM_ID, teamId);
		param.addValue(OFFSET_PARAM_NAME, offset);
		if (limit<=0) throw new IllegalArgumentException("'to' param must be greater than 'from' param.");
		param.addValue(LIMIT_PARAM_NAME, limit);	
		param.addValue(COL_MEMBERSHIP_REQUEST_SUBMISSION_EXPIRES_ON, now);	
		List<DBOMembershipRqstSubmission> dbos = simpleJdbcTemplate.query(SELECT_OPEN_REQUESTS_BY_TEAM_PAGINATED, teamRowMapper, param);
		List<MembershipRqstSubmission> dtos = new ArrayList<MembershipRqstSubmission>();
		for (DBOMembershipRqstSubmission dbo : dbos) dtos.add(MembershipRqstSubmissionUtils.copyDboToDto(dbo));
		return dtos;
	}

	@Override
	public List<MembershipRqstSubmission> getOpenByTeamAndRequestorInRange(
			long teamId, long requestorId, long now, long offset, long limit)
			throws DatastoreException, NotFoundException {
		List<MembershipRqstSubmission> dtos = new ArrayList<MembershipRqstSubmission>();
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(COL_MEMBERSHIP_REQUEST_SUBMISSION_TEAM_ID, teamId);
		param.addValue(COL_MEMBERSHIP_REQUEST_SUBMISSION_USER_ID, requestorId);
		param.addValue(OFFSET_PARAM_NAME, offset);
		if (limit<=0) throw new IllegalArgumentException("'to' param must be greater than 'from' param.");
		param.addValue(LIMIT_PARAM_NAME, limit);	
		param.addValue(COL_MEMBERSHIP_REQUEST_SUBMISSION_EXPIRES_ON, now);	
		List<DBOMembershipRqstSubmission> dbos = simpleJdbcTemplate.query(SELECT_OPEN_REQUESTS_BY_TEAM_AND_REQUESTOR_PAGINATED, teamRowMapper, param);
		for (DBOMembershipRqstSubmission dbo : dbos) dtos.add(MembershipRqstSubmissionUtils.copyDboToDto(dbo));
		return dtos;
	}

}
