/**
 * 
 */
package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GROUP_MEMBERS_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GROUP_MEMBERS_MEMBER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MEMBERSHIP_INVITATION_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MEMBERSHIP_INVITATION_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MEMBERSHIP_INVITATION_EXPIRES_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MEMBERSHIP_INVITATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MEMBERSHIP_INVITATION_INVITEE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MEMBERSHIP_INVITATION_PROPERTIES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_MEMBERSHIP_INVITATION_TEAM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TEAM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.LIMIT_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.OFFSET_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_GROUP_MEMBERS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_MEMBERSHIP_INVITATION;

import java.sql.Blob;
import java.util.List;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.MembershipInvitationDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMembershipInvitation;
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
public class MembershipInvitationDAOImpl implements MembershipInvitationDAO {

	@Autowired
	private DBOBasicDao basicDao;	
	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	private NamedParameterJdbcTemplate namedJdbcTemplate;
	@Autowired
	GroupMembersDAO groupMembersDAO;

	private static final String SELECT_OPEN_INVITATIONS_CORE =
			" FROM "+ TABLE_MEMBERSHIP_INVITATION +" mis "
			+" WHERE mis."+ COL_MEMBERSHIP_INVITATION_TEAM_ID +" NOT IN ("
					+ "SELECT "+COL_GROUP_MEMBERS_GROUP_ID
					+" FROM "+TABLE_GROUP_MEMBERS
					+" WHERE "+COL_GROUP_MEMBERS_MEMBER_ID+"=mis."+ COL_MEMBERSHIP_INVITATION_INVITEE_ID
					+" ) "
			+" AND ( mis."+ COL_MEMBERSHIP_INVITATION_EXPIRES_ON +" IS NULL"
					+ " OR mis."+ COL_MEMBERSHIP_INVITATION_EXPIRES_ON +">:"+ COL_MEMBERSHIP_INVITATION_EXPIRES_ON +" ) ";
	private static final String SELECT_OPEN_INVITATIONS_BY_USER_CORE =
			SELECT_OPEN_INVITATIONS_CORE
			+" AND mis."+ COL_MEMBERSHIP_INVITATION_INVITEE_ID +"=:"+ COL_MEMBERSHIP_INVITATION_INVITEE_ID;
	
	private static final String SELECT_OPEN_INVITATIONS_BY_TEAM_AND_USER_CORE = 
			SELECT_OPEN_INVITATIONS_BY_USER_CORE
			+" AND mis."+ COL_MEMBERSHIP_INVITATION_TEAM_ID +"=:"+ COL_MEMBERSHIP_INVITATION_TEAM_ID;

	private static final String SELECT_OPEN_INVITATIONS_BY_TEAM_CORE = 
			SELECT_OPEN_INVITATIONS_CORE
			+" AND mis."+ COL_MEMBERSHIP_INVITATION_TEAM_ID +"=:"+ COL_MEMBERSHIP_INVITATION_TEAM_ID;

	private static final String SELECT_OPEN_INVITATIONS_BY_USER_PAGINATED = 
			"SELECT mis.* "+SELECT_OPEN_INVITATIONS_BY_USER_CORE
			+" ORDER BY "+ COL_MEMBERSHIP_INVITATION_CREATED_ON +" DESC "
			+" LIMIT :"+LIMIT_PARAM_NAME+" OFFSET :"+OFFSET_PARAM_NAME;
	
	private static final String SELECT_OPEN_INVITATIONS_BY_TEAM_PAGINATED = 
			"SELECT mis.* "+SELECT_OPEN_INVITATIONS_BY_TEAM_CORE
			+" ORDER BY "+ COL_MEMBERSHIP_INVITATION_CREATED_ON +" DESC "
			+" LIMIT :"+LIMIT_PARAM_NAME+" OFFSET :"+OFFSET_PARAM_NAME;
	
	private static final String SELECT_OPEN_INVITATIONS_BY_USER_COUNT = 
			"SELECT COUNT(*) "+ SELECT_OPEN_INVITATIONS_BY_USER_CORE;
	
	private static final String SELECT_OPEN_INVITATIONS_BY_TEAM_AND_USER_PAGINATED = 
			"SELECT mis.* "
			+SELECT_OPEN_INVITATIONS_BY_TEAM_AND_USER_CORE
			+" ORDER BY "+ COL_MEMBERSHIP_INVITATION_CREATED_ON +" DESC "
			+" LIMIT :"+LIMIT_PARAM_NAME+" OFFSET :"+OFFSET_PARAM_NAME;
	
	private static final String SELECT_INVITER_BY_TEAM_AND_USER = 
			"SELECT "+ COL_MEMBERSHIP_INVITATION_PROPERTIES +
			SELECT_OPEN_INVITATIONS_BY_TEAM_AND_USER_CORE;
	
	private static final String SELECT_OPEN_INVITATIONS_BY_TEAM_AND_USER_COUNT = 
			"SELECT COUNT(*) "+SELECT_OPEN_INVITATIONS_BY_TEAM_AND_USER_CORE;
	
	private static final String SELECT_OPEN_INVITATIONS_BY_TEAM_COUNT = 
			"SELECT COUNT(*) "+SELECT_OPEN_INVITATIONS_BY_TEAM_CORE;
	
	private static final String DELETE_INVITATIONS_BY_TEAM_AND_INVITEE = 
			"DELETE FROM "+ TABLE_MEMBERSHIP_INVITATION
			+" WHERE "+ COL_MEMBERSHIP_INVITATION_TEAM_ID +"=:"+ COL_MEMBERSHIP_INVITATION_TEAM_ID
			+" AND "+ COL_MEMBERSHIP_INVITATION_INVITEE_ID +"=:"+ COL_MEMBERSHIP_INVITATION_INVITEE_ID;

	private static final String UPDATE_INVITEE_ID =
			"UPDATE " + TABLE_MEMBERSHIP_INVITATION + " " +
			"SET " + COL_MEMBERSHIP_INVITATION_INVITEE_ID + " = :" + COL_MEMBERSHIP_INVITATION_INVITEE_ID + ", " +
					COL_MEMBERSHIP_INVITATION_ETAG + " = :" + COL_MEMBERSHIP_INVITATION_ETAG + " " +
			"WHERE " + COL_MEMBERSHIP_INVITATION_ID + " = :" + COL_MEMBERSHIP_INVITATION_ID;

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.MemberRqstSubmissionDAO#create(org.sagebionetworks.repo.model.MemberRqstSubmission)
	 */
	@WriteTransaction
	@Override
	public MembershipInvitation create(MembershipInvitation dto) throws DatastoreException,
	InvalidModelException {
		if (dto.getId()==null) dto.setId(idGenerator.generateNewId(IdType.MEMBERSHIP_INVITATION_ID).toString());
		DBOMembershipInvitation dbo = new DBOMembershipInvitation();
		MembershipInvitationUtils.copyDtoToDbo(dto, dbo);
		dbo.setEtag(UUID.randomUUID().toString());
		dbo = basicDao.createNew(dbo);
		MembershipInvitation result = MembershipInvitationUtils.copyDboToDto(dbo);
		return result;
	}
	

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.MembershipInvitationDAO#get(java.lang.String)
	 */
	@Override
	public MembershipInvitation get(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_TEAM_ID.toLowerCase(), id);
		DBOMembershipInvitation dbo = basicDao.getObjectByPrimaryKey(DBOMembershipInvitation.class, param);
		MembershipInvitation dto = MembershipInvitationUtils.copyDboToDto(dbo);
		return dto;
	}

	@Override
	public MembershipInvitation getWithUpdateLock(String id) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_TEAM_ID.toLowerCase(), id);
		DBOMembershipInvitation dbo = basicDao.getObjectByPrimaryKeyWithUpdateLock(DBOMembershipInvitation.class, param);
		MembershipInvitation dto = MembershipInvitationUtils.copyDboToDto(dbo);
		return dto;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.MembershipInvitationDAO#delete(java.lang.String)
	 */
	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_TEAM_ID.toLowerCase(), id);
		basicDao.deleteObjectByPrimaryKey(DBOMembershipInvitation.class, param);
	}
	
	private static final RowMapper<String> INVITER_ROW_MAPPER = (rs, rowNum) -> {
		Blob misProperties = rs.getBlob(COL_MEMBERSHIP_INVITATION_PROPERTIES);
		MembershipInvitation mis = MembershipInvitationUtils.deserialize(misProperties.getBytes(1, (int) misProperties.length()));
		return mis.getCreatedBy();
	};
	
	private static final RowMapper<DBOMembershipInvitation> dboMembershipInvtnSubmissionRowMapper =
			(new DBOMembershipInvitation()).getTableMapping();
		
	private static final RowMapper<MembershipInvitation> membershipInvitationRowMapper = (rs, rowNum) -> {
		DBOMembershipInvitation dbo = dboMembershipInvtnSubmissionRowMapper.mapRow(rs, rowNum);
		return MembershipInvitationUtils.copyDboToDto(dbo);
	};
	
	@Override
	public List<MembershipInvitation> getOpenByUserInRange(long userId, long now, long limit,
	                                                       long offset) throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_MEMBERSHIP_INVITATION_INVITEE_ID, userId);
		param.addValue(OFFSET_PARAM_NAME, offset);
		if (limit<=0) throw new IllegalArgumentException("'to' param must be greater than 'from' param.");
		param.addValue(LIMIT_PARAM_NAME, limit);
		param.addValue(COL_MEMBERSHIP_INVITATION_EXPIRES_ON, now);
		return namedJdbcTemplate.query(SELECT_OPEN_INVITATIONS_BY_USER_PAGINATED, param, membershipInvitationRowMapper);
	}

	@Override
	public List<MembershipInvitation> getOpenByTeamInRange(
			long teamId, long now, long limit, long offset) {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(COL_MEMBERSHIP_INVITATION_TEAM_ID, teamId);
		param.addValue(OFFSET_PARAM_NAME, offset);
		if (limit<=0) throw new IllegalArgumentException("'to' param must be greater than 'from' param.");
		param.addValue(LIMIT_PARAM_NAME, limit);	
		param.addValue(COL_MEMBERSHIP_INVITATION_EXPIRES_ON, now);
		return namedJdbcTemplate.query(SELECT_OPEN_INVITATIONS_BY_TEAM_PAGINATED, param, membershipInvitationRowMapper);
	}


	private <T> List<T> getOpenByTeamAndUserInRange(
			long teamId, long userId, long now, long limit,
			long offset, RowMapper<T> rowMapper) {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(COL_MEMBERSHIP_INVITATION_TEAM_ID, teamId);
		param.addValue(COL_MEMBERSHIP_INVITATION_INVITEE_ID, userId);
		param.addValue(OFFSET_PARAM_NAME, offset);
		if (limit<=0) throw new IllegalArgumentException("'to' param must be greater than 'from' param.");
		param.addValue(LIMIT_PARAM_NAME, limit);	
		param.addValue(COL_MEMBERSHIP_INVITATION_EXPIRES_ON, now);
		return namedJdbcTemplate.query(SELECT_OPEN_INVITATIONS_BY_TEAM_AND_USER_PAGINATED, param, rowMapper);
	}
	
	@Override
	public List<String> getInvitersByTeamAndUser(long teamId, long userId, long now) {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(COL_MEMBERSHIP_INVITATION_TEAM_ID, teamId);
		param.addValue(COL_MEMBERSHIP_INVITATION_INVITEE_ID, userId);
		param.addValue(COL_MEMBERSHIP_INVITATION_EXPIRES_ON, now);
		return namedJdbcTemplate.query(SELECT_INVITER_BY_TEAM_AND_USER, param, INVITER_ROW_MAPPER);
	}


	@Override
	public List<MembershipInvitation> getOpenByTeamAndUserInRange(
			long teamId, long userId, long now, long limit, long offset)
			throws DatastoreException, NotFoundException {
		return getOpenByTeamAndUserInRange(teamId, userId, now, limit, offset, membershipInvitationRowMapper);
	}

	@Override
	public long getOpenByUserCount(long userId, long now)
			throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(COL_MEMBERSHIP_INVITATION_INVITEE_ID, userId);
		param.addValue(COL_MEMBERSHIP_INVITATION_EXPIRES_ON, now);
		return namedJdbcTemplate.queryForObject(SELECT_OPEN_INVITATIONS_BY_USER_COUNT, param, Long.class);
	}

	@Deprecated
	@Override
	public long getOpenByTeamAndUserCount(long teamId, long userId,
			long now) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(COL_MEMBERSHIP_INVITATION_TEAM_ID, teamId);
		param.addValue(COL_MEMBERSHIP_INVITATION_INVITEE_ID, userId);
		param.addValue(COL_MEMBERSHIP_INVITATION_EXPIRES_ON, now);
		return namedJdbcTemplate.queryForObject(SELECT_OPEN_INVITATIONS_BY_TEAM_AND_USER_COUNT, param, Long.class);
	}

	@Deprecated
	@Override
	public long getOpenByTeamCount(long teamId,
			long now) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(COL_MEMBERSHIP_INVITATION_TEAM_ID, teamId);
		param.addValue(COL_MEMBERSHIP_INVITATION_EXPIRES_ON, now);
		return namedJdbcTemplate.queryForObject(SELECT_OPEN_INVITATIONS_BY_TEAM_COUNT, param, Long.class);
	}

	@WriteTransaction
	@Override
	public void deleteByTeamAndUser(long teamId, long inviteeId)
			throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(COL_MEMBERSHIP_INVITATION_TEAM_ID, teamId);
		param.addValue(COL_MEMBERSHIP_INVITATION_INVITEE_ID, inviteeId);
		namedJdbcTemplate.update(DELETE_INVITATIONS_BY_TEAM_AND_INVITEE, param);
	}

	@WriteTransaction
	@Override
	public void updateInviteeId(String misId, long inviteeId) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_MEMBERSHIP_INVITATION_INVITEE_ID, inviteeId);
		param.addValue(COL_MEMBERSHIP_INVITATION_ID, misId);
		param.addValue(COL_MEMBERSHIP_INVITATION_ETAG, UUID.randomUUID().toString());
		int rowsUpdated = namedJdbcTemplate.update(UPDATE_INVITEE_ID, param);
		if (rowsUpdated == 0) {
			throw new NotFoundException("Could not update MembershipInvitation with id " + misId + ".");
		} else if (rowsUpdated > 1) {
			throw new DatastoreException("Expected to update 1 row but more than 1 was updated.");
		}
	}
}
