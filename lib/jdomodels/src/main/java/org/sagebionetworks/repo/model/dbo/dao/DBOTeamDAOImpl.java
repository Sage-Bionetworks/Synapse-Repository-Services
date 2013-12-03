/**
 * 
 */
package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GROUP_MEMBERS_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GROUP_MEMBERS_MEMBER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_OWNER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_TYPE_ELEMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_RESOURCE_ACCESS_TYPE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TEAM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TEAM_PROPERTIES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_PROFILE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_PROFILE_PROPS_BLOB;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.LIMIT_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.OFFSET_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_GROUP_MEMBERS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_RESOURCE_ACCESS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_RESOURCE_ACCESS_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TEAM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_GROUP;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_PROFILE;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTeam;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author brucehoff
 *
 */
public class DBOTeamDAOImpl implements TeamDAO {

	@Autowired
	private DBOBasicDao basicDao;	

	@Autowired
	private IdGenerator idGenerator;

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;

	private static final RowMapper<DBOTeam> teamRowMapper = (new DBOTeam()).getTableMapping();
	
	private static final String SELECT_PAGINATED = 
			"SELECT t.*, g."+COL_USER_GROUP_NAME+" FROM "+TABLE_USER_GROUP+" g, "+TABLE_TEAM+" t "+
			" WHERE t."+COL_TEAM_ID+"=g."+COL_USER_GROUP_ID+" order by "+COL_USER_GROUP_NAME+" asc "+
			" LIMIT :"+LIMIT_PARAM_NAME+" OFFSET :"+OFFSET_PARAM_NAME;
	
	private static final String SELECT_COUNT = 
			"SELECT COUNT(*) FROM "+TABLE_TEAM;

	private static final String SELECT_FOR_MEMBER_SQL_CORE = 
			" FROM "+TABLE_GROUP_MEMBERS+" gm, "+TABLE_TEAM+" t "+
			" WHERE t."+COL_TEAM_ID+"=gm."+COL_GROUP_MEMBERS_GROUP_ID+" AND "+
			" gm."+COL_GROUP_MEMBERS_MEMBER_ID+" IN (:"+COL_GROUP_MEMBERS_MEMBER_ID+")";

	private static final String SELECT_FOR_MEMBER_PAGINATED = 
			"SELECT t.* "+SELECT_FOR_MEMBER_SQL_CORE+
			" LIMIT :"+LIMIT_PARAM_NAME+" OFFSET :"+OFFSET_PARAM_NAME;
	
	private static final String SELECT_FOR_MEMBER_COUNT = 
			"SELECT count(*) "+SELECT_FOR_MEMBER_SQL_CORE;

	private static final String USER_PROFILE_PROPERTIES_COLUMN_LABEL = "USER_PROFILE_PROPERTIES";

	private static final String SELECT_ALL_TEAMS_AND_MEMBERS =
			"SELECT t.*, up."+COL_USER_PROFILE_PROPS_BLOB+" as "+USER_PROFILE_PROPERTIES_COLUMN_LABEL+", up."+COL_USER_PROFILE_ID+
			" FROM "+TABLE_TEAM+" t, "+TABLE_GROUP_MEMBERS+" gm LEFT OUTER JOIN "+
			TABLE_USER_PROFILE+" up ON (gm."+COL_GROUP_MEMBERS_MEMBER_ID+"=up."+COL_USER_PROFILE_ID+") "+
			" WHERE t."+COL_TEAM_ID+"=gm."+COL_GROUP_MEMBERS_GROUP_ID;
	
	private static final String SELECT_ALL_TEAMS_AND_ADMIN_MEMBERS_CORE =
			" FROM "+
				TABLE_TEAM+" t, "+
				TABLE_RESOURCE_ACCESS+" ra, "+TABLE_RESOURCE_ACCESS_TYPE+" at, "+
				TABLE_GROUP_MEMBERS+" gm "+
			" WHERE t."+COL_TEAM_ID+"=gm."+COL_GROUP_MEMBERS_GROUP_ID+
			" and ra."+COL_RESOURCE_ACCESS_GROUP_ID+"=gm."+COL_GROUP_MEMBERS_MEMBER_ID+
			" and ra."+COL_RESOURCE_ACCESS_OWNER+"=gm."+COL_GROUP_MEMBERS_GROUP_ID+
			" and at."+COL_RESOURCE_ACCESS_TYPE_ID+"=ra."+COL_RESOURCE_ACCESS_ID+
			" and at."+COL_RESOURCE_ACCESS_TYPE_ELEMENT+"='"+ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE+"'";
			
	private static final String SELECT_ALL_TEAMS_AND_ADMIN_MEMBERS =
				"SELECT t."+COL_TEAM_ID+", gm."+COL_GROUP_MEMBERS_MEMBER_ID+
				SELECT_ALL_TEAMS_AND_ADMIN_MEMBERS_CORE;
	
	private static final String SELECT_MEMBERS_OF_TEAM_CORE =
			"SELECT up."+COL_USER_PROFILE_PROPS_BLOB+" as "+USER_PROFILE_PROPERTIES_COLUMN_LABEL+
			", up."+COL_USER_PROFILE_ID+
			", gm."+COL_GROUP_MEMBERS_GROUP_ID+
			" FROM "+TABLE_GROUP_MEMBERS+" gm, "+TABLE_USER_PROFILE+" up "+
			" WHERE gm."+COL_GROUP_MEMBERS_MEMBER_ID+"=up."+COL_USER_PROFILE_ID+" "+
			" and gm."+COL_GROUP_MEMBERS_GROUP_ID+"=:"+COL_GROUP_MEMBERS_GROUP_ID;
	
	private static final String SELECT_MEMBERS_OF_TEAM_PAGINATED =
			SELECT_MEMBERS_OF_TEAM_CORE+
			" LIMIT :"+LIMIT_PARAM_NAME+" OFFSET :"+OFFSET_PARAM_NAME;
	
	private static final String SELECT_SINGLE_MEMBER_OF_TEAM =
			SELECT_MEMBERS_OF_TEAM_CORE+" AND gm."+COL_GROUP_MEMBERS_MEMBER_ID+"=:"+COL_GROUP_MEMBERS_MEMBER_ID;
			
	
	private static final String SELECT_MEMBERS_OF_TEAM_COUNT =
			"SELECT COUNT(*) FROM "+TABLE_GROUP_MEMBERS+" gm "+
			" WHERE gm."+COL_GROUP_MEMBERS_GROUP_ID+"=:"+COL_GROUP_MEMBERS_GROUP_ID;
	
	private static final String SELECT_ADMIN_MEMBERS_OF_TEAM =
			SELECT_ALL_TEAMS_AND_ADMIN_MEMBERS+" and gm."+COL_GROUP_MEMBERS_GROUP_ID+"=:"+COL_GROUP_MEMBERS_GROUP_ID;
	
	private static final String SELECT_ADMIN_MEMBERS_OF_TEAM_COUNT = 
			"SELECT COUNT(gm."+COL_GROUP_MEMBERS_MEMBER_ID+") "+SELECT_ALL_TEAMS_AND_ADMIN_MEMBERS_CORE+
			" and gm."+COL_GROUP_MEMBERS_GROUP_ID+"=:"+COL_GROUP_MEMBERS_GROUP_ID;
	
	private static final String IS_MEMBER_AN_ADMIN = 
			SELECT_ADMIN_MEMBERS_OF_TEAM_COUNT +
			" and gm."+COL_GROUP_MEMBERS_MEMBER_ID+"=:"+COL_GROUP_MEMBERS_MEMBER_ID;
	
	private static final String SELECT_FOR_UPDATE_SQL = "select * from "+TABLE_TEAM+" where "+COL_TEAM_ID+
			"=:"+COL_TEAM_ID+" for update";

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.TeamDAO#create(org.sagebionetworks.repo.model.Team)
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public Team create(Team dto) throws DatastoreException,
	InvalidModelException {
		if (dto.getId()==null) throw new InvalidModelException("ID is required");
		DBOTeam dbo = new DBOTeam();
		TeamUtils.copyDtoToDbo(dto, dbo);
		dbo.setEtag(UUID.randomUUID().toString());
		dbo = basicDao.createNew(dbo);
		Team result = TeamUtils.copyDboToDto(dbo);
		return result;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.TeamDAO#get(java.lang.String)
	 */
	@Override
	public Team get(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_TEAM_ID.toLowerCase(), id);
		DBOTeam dbo = basicDao.getObjectByPrimaryKey(DBOTeam.class, param);
		Team dto = TeamUtils.copyDboToDto(dbo);
		return dto;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.TeamDAO#getInRange(long, long)
	 */
	@Override
	public List<Team> getInRange(long limit, long offset)
			throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(OFFSET_PARAM_NAME, offset);
		if (limit<=0) throw new IllegalArgumentException("'limit' param must be greater than zero.");
		param.addValue(LIMIT_PARAM_NAME, limit);	
		List<DBOTeam> dbos = simpleJdbcTemplate.query(SELECT_PAGINATED, teamRowMapper, param);
		List<Team> dtos = new ArrayList<Team>();
		for (DBOTeam dbo : dbos) dtos.add(TeamUtils.copyDboToDto(dbo));
		return dtos;
	}

	@Override
	public long getCount() throws DatastoreException {
		return simpleJdbcTemplate.queryForLong(SELECT_COUNT);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.TeamDAO#getForMemberInRange(java.lang.String, long, long)
	 */
	@Override
	public List<Team> getForMemberInRange(String principalId,
			long limit, long offset) throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(COL_GROUP_MEMBERS_MEMBER_ID, principalId);
		param.addValue(OFFSET_PARAM_NAME, offset);
		if (limit<=0) throw new IllegalArgumentException("'limit' param must be greater than zero.");
		param.addValue(LIMIT_PARAM_NAME, limit);	
		List<DBOTeam> dbos = simpleJdbcTemplate.query(SELECT_FOR_MEMBER_PAGINATED, teamRowMapper, param);
		List<Team> dtos = new ArrayList<Team>();
		for (DBOTeam dbo : dbos) dtos.add(TeamUtils.copyDboToDto(dbo));
		return dtos;
	}

	@Override
	public long getCountForMember(String principalId) throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(COL_GROUP_MEMBERS_MEMBER_ID, principalId);
		return simpleJdbcTemplate.queryForLong(SELECT_FOR_MEMBER_COUNT, param);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.TeamDAO#update(org.sagebionetworks.repo.model.Team)
	 */
	@Override
	public Team update(Team dto) throws InvalidModelException,
			NotFoundException, ConflictingUpdateException, DatastoreException {

		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_TEAM_ID, dto.getId());
		DBOTeam dbo = null;
		try{
			dbo = simpleJdbcTemplate.queryForObject(SELECT_FOR_UPDATE_SQL, teamRowMapper, param);
		}catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("The resource you are attempting to access cannot be found");
		}
		
		String oldEtag = dbo.getEtag();
		// check dbo's etag against dto's etag
		// if different rollback and throw a meaningful exception
		if (!oldEtag.equals(dto.getEtag())) {
			throw new ConflictingUpdateException("Use profile was updated since you last fetched it, retrieve it again and reapply the update.");
		}

		{
			Team deserializedProperties = TeamUtils.copyFromSerializedField(dbo);
			if (dto.getCreatedBy()==null) dto.setCreatedBy(deserializedProperties.getCreatedBy());
			if (dto.getCreatedOn()==null) dto.setCreatedOn(deserializedProperties.getCreatedOn());
			if (!dto.getName().equals(deserializedProperties.getName())) throw new InvalidModelException("Cannot modify team name.");
		}
		
		// Update with a new e-tag
		dto.setEtag(UUID.randomUUID().toString());
		TeamUtils.copyDtoToDbo(dto, dbo);

		boolean success = basicDao.update(dbo);
		if (!success) throw new DatastoreException("Unsuccessful updating Team in database.");

		Team resultantDto = TeamUtils.copyDboToDto(dbo);
		return resultantDto;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.TeamDAO#delete(java.lang.String)
	 */
	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_TEAM_ID.toLowerCase(), id);
		basicDao.deleteObjectByPrimaryKey(DBOTeam.class, param);
	}

	public static class TeamMemberPair {
		private Team team;
		private TeamMember teamMember;
		public Team getTeam() {
			return team;
		}
		public void setTeam(Team team) {
			this.team = team;
		}
		public TeamMember getTeamMember() {
			return teamMember;
		}
		public void setTeamMember(TeamMember teamMember) {
			this.teamMember = teamMember;
		}

	}
	
	private static final RowMapper<TeamMemberPair> teamMemberPairRowMapper = new RowMapper<TeamMemberPair>(){
		@Override
		public TeamMemberPair mapRow(ResultSet rs, int rowNum) throws SQLException {
			TeamMemberPair tmp = new TeamMemberPair();
			Team team = null;
			{
				Blob teamProperties = rs.getBlob(COL_TEAM_PROPERTIES);
				team = TeamUtils.deserialize(teamProperties.getBytes(1, (int) teamProperties.length()));
			}
			team.setId(rs.getString(COL_TEAM_ID));
			tmp.setTeam(team);
			{
				UserGroupHeader ugh = new UserGroupHeader();
				TeamMember tm = new TeamMember();
				tm.setMember(ugh);
				tm.setTeamId(team.getId());
				tm.setIsAdmin(false);
				tmp.setTeamMember(tm);
				Blob upProperties = rs.getBlob(USER_PROFILE_PROPERTIES_COLUMN_LABEL);
				if (upProperties!=null) {
					ugh.setIsIndividual(true);
					ugh.setOwnerId(rs.getString(COL_USER_PROFILE_ID));
					fillUserGroupHeaderFromUserProfileBlob(upProperties, ugh);
				} else {
					ugh.setIsIndividual(false);
				}
			}
			return tmp;
		}
	};
	
	private static void fillUserGroupHeaderFromUserProfileBlob(Blob upProperties, UserGroupHeader ugh) throws SQLException {
		UserProfile up = UserProfileUtils.deserialize(upProperties.getBytes(1, (int) upProperties.length()));
		ugh.setDisplayName(up.getDisplayName());
		ugh.setFirstName(up.getFirstName());
		ugh.setLastName(up.getLastName());
		ugh.setPic(up.getPic());
		ugh.setEmail(up.getEmail());		
	}
	
	public static class TeamMemberId {
		private Long teamId;
		public Long getTeamId() {
			return teamId;
		}
		public void setTeamId(Long teamId) {
			this.teamId = teamId;
		}
		public Long getMemberId() {
			return memberId;
		}
		public void setMemberId(Long memberId) {
			this.memberId = memberId;
		}
		private Long memberId;
	}
	
	private static final RowMapper<TeamMemberId> teamMemberIdRowMapper = new RowMapper<TeamMemberId>(){
		@Override
		public TeamMemberId mapRow(ResultSet rs, int rowNum) throws SQLException {
			TeamMemberId tmi = new TeamMemberId();
			tmi.setTeamId(rs.getLong(COL_TEAM_ID));
			tmi.setMemberId(rs.getLong(COL_GROUP_MEMBERS_MEMBER_ID));
			return tmi;
		}
	};

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public Map<Team, Collection<TeamMember>> getAllTeamsAndMembers() throws DatastoreException {
		// first get all the Teams and Members, regardless of whether the members are administrators
		List<TeamMemberPair> queryResults = simpleJdbcTemplate.query(SELECT_ALL_TEAMS_AND_MEMBERS, teamMemberPairRowMapper);
		Map<Long, Team> teamMap = new HashMap<Long, Team>();
		Map<Long, Map<Long,TeamMember>> teamMemberMap = new HashMap<Long, Map<Long,TeamMember>>();
		for (TeamMemberPair tmp : queryResults) {
			long teamId = Long.parseLong(tmp.getTeam().getId());
			teamMap.put(teamId, tmp.getTeam());
			Map<Long,TeamMember> tms = teamMemberMap.get(teamId);
			if (tms==null) {
				tms = new HashMap<Long,TeamMember>();
				teamMemberMap.put(teamId, tms);
			}
			tms.put(Long.parseLong(tmp.getTeamMember().getMember().getOwnerId()), tmp.getTeamMember());
		}
		// second, get the team, member pairs for which the member is an administrator of the team
		List<TeamMemberId> adminTeamMembers = simpleJdbcTemplate.query(SELECT_ALL_TEAMS_AND_ADMIN_MEMBERS, teamMemberIdRowMapper);
		for (TeamMemberId tmi : adminTeamMembers) {
			// since the admin's are a subset of the entire <team,member> universe, we *must* find them in the map
			Map<Long,TeamMember> members = teamMemberMap.get(tmi.getTeamId());
			if (members==null) throw new IllegalStateException("No members found for team ID: "+tmi.getTeamId());
			TeamMember tm = members.get(tmi.getMemberId());
			if (tm==null) throw new IllegalStateException("No member found for team ID: "+tmi.getTeamId()+", member ID: "+tmi.getMemberId());
			tm.setIsAdmin(true);
		}
		Map<Team, Collection<TeamMember>> results = new HashMap<Team, Collection<TeamMember>>();
		// finally, create the results to return
		for (Long teamId : teamMap.keySet()) {
			Team team = teamMap.get(teamId);
			if (team==null) throw new IllegalStateException("Missing Team for team ID: "+teamId);
			Collection<TeamMember> teamMembers = teamMemberMap.get(teamId).values();
			if (teamMembers==null || teamMembers.isEmpty()) throw new IllegalStateException("Missing team members for team ID :"+teamId);
			results.put(team, teamMembers);
		}
		return results;
	}

	private static final RowMapper<TeamMember> teamMemberRowMapper = new RowMapper<TeamMember>(){
		@Override
		public TeamMember mapRow(ResultSet rs, int rowNum) throws SQLException {
			UserGroupHeader ugh = new UserGroupHeader();
			TeamMember tm = new TeamMember();
			tm.setMember(ugh);
			tm.setTeamId(rs.getString(COL_GROUP_MEMBERS_GROUP_ID));
			tm.setIsAdmin(false);
			Blob upProperties = rs.getBlob(USER_PROFILE_PROPERTIES_COLUMN_LABEL);
			ugh.setOwnerId(rs.getString(COL_USER_PROFILE_ID));
			if (upProperties!=null) {
				ugh.setIsIndividual(true);
				fillUserGroupHeaderFromUserProfileBlob(upProperties, ugh);
			} else {
				ugh.setIsIndividual(false);
			}
			return tm;
		}
	};
	
	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public List<TeamMember> getMembersInRange(String teamId, long limit, long offset)
			throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(COL_GROUP_MEMBERS_GROUP_ID, teamId);
		param.addValue(OFFSET_PARAM_NAME, offset);
		if (limit<=0) throw new IllegalArgumentException("'limit' param must be greater than zero.");
		param.addValue(LIMIT_PARAM_NAME, limit);	
		List<TeamMember> teamMembers = simpleJdbcTemplate.query(SELECT_MEMBERS_OF_TEAM_PAGINATED, teamMemberRowMapper, param);
		Map<Long, TeamMember> teamMemberMap = new HashMap<Long, TeamMember>();
		for (TeamMember tm : teamMembers) teamMemberMap.put(Long.parseLong(tm.getMember().getOwnerId()), tm);

		// now update the 'isAdmin' field for those members that are admins on the team
		param = new MapSqlParameterSource();
		param.addValue(COL_GROUP_MEMBERS_GROUP_ID, teamId);
		List<TeamMemberId> adminTeamMembers = simpleJdbcTemplate.query(SELECT_ADMIN_MEMBERS_OF_TEAM, teamMemberIdRowMapper, param);
		for (TeamMemberId id : adminTeamMembers) {
			TeamMember tm = teamMemberMap.get(id.getMemberId());
			if (tm!=null) tm.setIsAdmin(true);
		}
		
		return teamMembers;
	}
	
	
	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public TeamMember getMember(String teamId, String principalId) throws NotFoundException, DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(COL_GROUP_MEMBERS_GROUP_ID, teamId);
		param.addValue(COL_GROUP_MEMBERS_MEMBER_ID, principalId);
		List<TeamMember> teamMembers = simpleJdbcTemplate.query(SELECT_SINGLE_MEMBER_OF_TEAM, teamMemberRowMapper, param);
		if (teamMembers.size()==0) throw new NotFoundException("Could not find member "+principalId+" in team "+teamId);
		if (teamMembers.size()>1) throw new DatastoreException("Expected one result but found "+teamMembers.size());
		TeamMember theMember = teamMembers.get(0);
		// now find if it's an admin
		long adminCount = simpleJdbcTemplate.queryForLong(IS_MEMBER_AN_ADMIN, param);
		if (adminCount==1) theMember.setIsAdmin(true);
		if (adminCount>1) throw new DatastoreException("Expected 0-1 but found "+adminCount);
		return theMember;
	}
	
	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public long getAdminMemberCount(String teamId) throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(COL_GROUP_MEMBERS_GROUP_ID, teamId);
		return simpleJdbcTemplate.queryForLong(SELECT_ADMIN_MEMBERS_OF_TEAM_COUNT, param);

	}

	@Override
	public long getMembersCount(String teamId) throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(COL_GROUP_MEMBERS_GROUP_ID, teamId);
		return simpleJdbcTemplate.queryForLong(SELECT_MEMBERS_OF_TEAM_COUNT, param);
	}

}
