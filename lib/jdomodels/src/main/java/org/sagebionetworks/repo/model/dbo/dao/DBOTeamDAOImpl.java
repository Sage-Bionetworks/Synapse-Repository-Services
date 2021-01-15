package org.sagebionetworks.repo.model.dbo.dao;

import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ListWrapper;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.TeamSortOrder;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTeam;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.principal.AliasEnum;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.sagebionetworks.repo.model.TeamSortOrder.TEAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GROUP_MEMBERS_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GROUP_MEMBERS_MEMBER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_DISPLAY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TEAM_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TEAM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TEAM_PROPERTIES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_PROFILE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_PROFILE_PROPS_BLOB;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.LIMIT_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.OFFSET_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_GROUP_MEMBERS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PRINCIPAL_ALIAS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TEAM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_PROFILE;

/**
 * @author brucehoff
 *
 */
public class DBOTeamDAOImpl implements TeamDAO {

	@Autowired
	private DBOBasicDao basicDao;
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private NamedParameterJdbcTemplate namedJdbcTemplate;
	
	@Autowired
	private TransactionalMessenger transactionalMessenger;

	private static final RowMapper<DBOTeam> TEAM_ROW_MAPPER = (new DBOTeam()).getTableMapping();

	public static final String INCLUSION_USER_PROFILE_ID_PARAM		= "inclusion";
	public static final String EXCLUSION_USER_PROFILE_ID_PARAM		= "exclusion";
	public static final String GROUP_MEMBERS_GROUP_ID_PARAM_NAME  	= "group_id";

	private static final String SELECT_MULTIPLE_CORE = 
			"SELECT * FROM "+TABLE_TEAM;
	
	private static final String SELECT_MULTIPLE_ORDER_BY = " order by "+COL_TEAM_ID+" asc ";
			
	private static final String SELECT_PAGINATED = 
			SELECT_MULTIPLE_CORE+SELECT_MULTIPLE_ORDER_BY+
			" LIMIT :"+LIMIT_PARAM_NAME+" OFFSET :"+OFFSET_PARAM_NAME;
	
	private static final String SELECT_BY_IDS = SELECT_MULTIPLE_CORE+" WHERE "+
			COL_TEAM_ID+" IN (:"+COL_TEAM_ID+")"+SELECT_MULTIPLE_ORDER_BY;
	
	private static final String SELECT_COUNT = 
			"SELECT COUNT(*) FROM "+TABLE_TEAM;

	private static final String SELECT_FOR_MEMBER_CORE = 
			" FROM "+TABLE_GROUP_MEMBERS+" gm, "+TABLE_TEAM+" t "+
			" WHERE t."+COL_TEAM_ID+"=gm."+COL_GROUP_MEMBERS_GROUP_ID+" AND "+
			" gm."+COL_GROUP_MEMBERS_MEMBER_ID+" IN (:"+COL_GROUP_MEMBERS_MEMBER_ID+")";

	private static final String SELECT_FOR_MEMBER_PAGINATED = 
			"SELECT t.* "+SELECT_FOR_MEMBER_CORE+
			" LIMIT :"+LIMIT_PARAM_NAME+" OFFSET :"+OFFSET_PARAM_NAME;

	private static final String SELECT_IDS_FOR_MEMBER =
			"SELECT t." + COL_TEAM_ID + SELECT_FOR_MEMBER_CORE;

	private static final String SELECT_IDS_FOR_MEMBER_SORTED_BY_NAME_PREFIX =
			"SELECT t." + COL_TEAM_ID +
			" FROM " + TABLE_TEAM + " t " +
			" JOIN (" + TABLE_GROUP_MEMBERS + " gm, " + TABLE_PRINCIPAL_ALIAS + " pa)" +
			" ON (t." + COL_TEAM_ID + " = gm." + COL_GROUP_MEMBERS_GROUP_ID +
			" AND t." + COL_TEAM_ID + " = pa." + COL_PRINCIPAL_ALIAS_PRINCIPAL_ID + ")" +
			" WHERE gm."+COL_GROUP_MEMBERS_MEMBER_ID+" = :"+COL_GROUP_MEMBERS_MEMBER_ID;

	private static final String SELECT_FOR_MEMBER_COUNT =
			"SELECT count(*) "+SELECT_FOR_MEMBER_CORE;

	private static final String USER_PROFILE_PROPERTIES_COLUMN_LABEL = "USER_PROFILE_PROPERTIES";

	private static final String SELECT_ADMIN_MEMBERS =
			"SELECT gm."+COL_GROUP_MEMBERS_MEMBER_ID+" FROM "+
			TeamUtils.ALL_TEAMS_AND_ADMIN_MEMBERS_CORE;

	private static final String SELECT_ALL_TEAMS_AND_ADMIN_MEMBERS =
			"SELECT t."+COL_TEAM_ID+", gm."+COL_GROUP_MEMBERS_MEMBER_ID+" FROM "+
			TeamUtils.ALL_TEAMS_AND_ADMIN_MEMBERS_CORE;

	private static final String SELECT_ALL_TEAMS_AND_MEMBERS =
			"SELECT t.*, up."+COL_USER_PROFILE_PROPS_BLOB+" as "+USER_PROFILE_PROPERTIES_COLUMN_LABEL+
			", up."+COL_USER_PROFILE_ID+
			", pa."+ COL_PRINCIPAL_ALIAS_DISPLAY +
			" FROM "+TABLE_TEAM+" t, "+TABLE_GROUP_MEMBERS+" gm LEFT OUTER JOIN "+
			TABLE_USER_PROFILE+" up ON (gm."+COL_GROUP_MEMBERS_MEMBER_ID+"=up."+COL_USER_PROFILE_ID+") "+
			"LEFT OUTER JOIN "+TABLE_PRINCIPAL_ALIAS+" pa ON (gm."+
			COL_GROUP_MEMBERS_MEMBER_ID+"=pa."+COL_PRINCIPAL_ALIAS_PRINCIPAL_ID+" AND pa."+
					COL_PRINCIPAL_ALIAS_TYPE+"='"+AliasEnum.USER_NAME.name()+"'"+") "+
			" WHERE t."+COL_TEAM_ID+"=gm."+COL_GROUP_MEMBERS_GROUP_ID;
	
	private static final String SELECT_MEMBERS_OF_TEAM_CORE =
			"SELECT up."+COL_USER_PROFILE_PROPS_BLOB+" as "+USER_PROFILE_PROPERTIES_COLUMN_LABEL+
			", up."+COL_USER_PROFILE_ID+
			", gm."+COL_GROUP_MEMBERS_GROUP_ID+
			", pa."+ COL_PRINCIPAL_ALIAS_DISPLAY +
			" FROM "+TABLE_GROUP_MEMBERS+" gm "+
			"LEFT OUTER JOIN "+TABLE_PRINCIPAL_ALIAS+" pa ON (gm."+
			COL_GROUP_MEMBERS_MEMBER_ID+"=pa."+COL_PRINCIPAL_ALIAS_PRINCIPAL_ID+" AND pa."+
					COL_PRINCIPAL_ALIAS_TYPE+"='"+
					AliasEnum.USER_NAME.name()+"'"+") "+
			", "+TABLE_USER_PROFILE+" up "+
			" WHERE gm."+COL_GROUP_MEMBERS_MEMBER_ID+"=up."+COL_USER_PROFILE_ID;

	private static final String AND_MEMBERS_FOR_ONE_GROUP = "and gm."+COL_GROUP_MEMBERS_GROUP_ID+ "=:" + GROUP_MEMBERS_GROUP_ID_PARAM_NAME;

	private static final String AND_USERS_NOT_IN_ID_LIST = "and up."+COL_USER_PROFILE_ID+ " NOT IN (:" + EXCLUSION_USER_PROFILE_ID_PARAM+")";

	private static final String AND_USERS_IN_ID_LIST = "and up."+COL_USER_PROFILE_ID+ " IN (:" + INCLUSION_USER_PROFILE_ID_PARAM+")";

	private static final String PAGINATED = "LIMIT :"+LIMIT_PARAM_NAME+" OFFSET :"+OFFSET_PARAM_NAME;

	private static final String SELECT_SINGLE_MEMBER_OF_TEAM =
			SELECT_MEMBERS_OF_TEAM_CORE+
			" and gm."+COL_GROUP_MEMBERS_GROUP_ID+"=:"+COL_GROUP_MEMBERS_GROUP_ID+" AND gm."+COL_GROUP_MEMBERS_MEMBER_ID+"=:"+COL_GROUP_MEMBERS_MEMBER_ID;
			
	private static final String SELECT_MEMBERS_OF_TEAMS_GIVEN_MEMBER_IDS = 
			SELECT_MEMBERS_OF_TEAM_CORE+
			" and gm."+COL_GROUP_MEMBERS_GROUP_ID+" IN (:"+COL_GROUP_MEMBERS_GROUP_ID+")"+
			" and gm."+COL_GROUP_MEMBERS_MEMBER_ID+" IN (:"+COL_GROUP_MEMBERS_MEMBER_ID+")";
	
	private static final String SELECT_MEMBERS_OF_TEAM_COUNT =
			"SELECT COUNT(*) FROM "+TABLE_GROUP_MEMBERS+" gm "+
			" WHERE gm."+COL_GROUP_MEMBERS_GROUP_ID+"=:"+COL_GROUP_MEMBERS_GROUP_ID;
	
	private static final String SELECT_ADMIN_MEMBER_IDS =
			SELECT_ADMIN_MEMBERS+" and gm."+COL_GROUP_MEMBERS_GROUP_ID+" IN (:"+COL_GROUP_MEMBERS_GROUP_ID+")";

	private static final String SELECT_ADMIN_MEMBERS_OF_TEAMS =
			SELECT_ALL_TEAMS_AND_ADMIN_MEMBERS+" and gm."+COL_GROUP_MEMBERS_GROUP_ID+" IN (:"+COL_GROUP_MEMBERS_GROUP_ID+")";

	private static final String SELECT_ADMIN_MEMBERS_OF_TEAM_COUNT = 
			"SELECT COUNT(gm."+COL_GROUP_MEMBERS_MEMBER_ID+") FROM "+TeamUtils.ALL_TEAMS_AND_ADMIN_MEMBERS_CORE+
			" and gm."+COL_GROUP_MEMBERS_GROUP_ID+"=:"+COL_GROUP_MEMBERS_GROUP_ID;
	
	private static final String IS_MEMBER_AN_ADMIN = 
			SELECT_ADMIN_MEMBERS_OF_TEAM_COUNT +
			" and gm."+COL_GROUP_MEMBERS_MEMBER_ID+"=:"+COL_GROUP_MEMBERS_MEMBER_ID;
	
	private static final String SELECT_FOR_UPDATE_SQL = "select * from "+TABLE_TEAM+" where "+COL_TEAM_ID+
			"=:"+COL_TEAM_ID+" for update";

	private static final String SELECT_ALL_TEAMS_USER_IS_ADMIN = "SELECT gm."+COL_GROUP_MEMBERS_GROUP_ID
				+" FROM "+TeamUtils.ALL_TEAMS_AND_ADMIN_MEMBERS_CORE
				+" AND gm."+COL_GROUP_MEMBERS_MEMBER_ID+"=:"+COL_GROUP_MEMBERS_MEMBER_ID;

	private static final String SELECT_CHECK_TEAM_EXISTS = "SELECT COUNT(*) FROM " + TABLE_TEAM + " WHERE " + COL_TEAM_ID + " = ?";

	private static final String ORDER_BY_TEAM_NAME = " ORDER BY LOWER(pa." + COL_PRINCIPAL_ALIAS_DISPLAY + ")";
	private static final String ASC = " ASC ";
	private static final String DESC = " DESC ";

	private static final String LIMIT_AND_OFFSET = " LIMIT :" + LIMIT_PARAM_NAME + " OFFSET :" + OFFSET_PARAM_NAME;

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
	
	private static final RowMapper<TeamMemberPair> TEAM_MEMBER_PAIR_ROW_MAPPER = new RowMapper<TeamMemberPair>(){

		@Override
		public TeamMemberPair mapRow(ResultSet rs, int rowNum) throws SQLException {
			TeamMemberPair tmp = new TeamMemberPair();
			Team team = null;
			{
				Blob teamProperties = rs.getBlob(COL_TEAM_PROPERTIES);
				team = TeamUtils.deserialize(teamProperties.getBytes(1, (int) teamProperties.length()));
			}
			team.setId(rs.getString(COL_TEAM_ID));
			team.setEtag(rs.getString(COL_TEAM_ETAG));
			tmp.setTeam(team);
			String userName = rs.getString(COL_PRINCIPAL_ALIAS_DISPLAY);
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
					UserProfileUtils.fillUserGroupHeaderFromUserProfileBlob(upProperties, userName, ugh);
				} else {
					ugh.setIsIndividual(false);
				}
			}
			return tmp;
		}
	};

	private static final RowMapper<TeamMemberId> TEAM_MEMBER_ID_ROW_MAPPER = new RowMapper<TeamMemberId>(){

		@Override
		public TeamMemberId mapRow(ResultSet rs, int rowNum) throws SQLException {
			return new TeamMemberId(rs.getLong(COL_TEAM_ID), rs.getLong(COL_GROUP_MEMBERS_MEMBER_ID));
		}
	};

	private static final RowMapper<String> MEMBER_ID_ROW_MAPPER = new RowMapper<String>(){

		@Override
		public String mapRow(ResultSet rs, int rowNum) throws SQLException {
			return rs.getString(COL_GROUP_MEMBERS_MEMBER_ID);
		}
	};

	private static final RowMapper<TeamMember> TEAM_MEMBER_ROW_MAPPER = new RowMapper<TeamMember>(){
		@Override
		public TeamMember mapRow(ResultSet rs, int rowNum) throws SQLException {
			UserGroupHeader ugh = new UserGroupHeader();
			TeamMember tm = new TeamMember();
			tm.setMember(ugh);
			tm.setTeamId(rs.getString(COL_GROUP_MEMBERS_GROUP_ID));
			tm.setIsAdmin(false);
			Blob upProperties = rs.getBlob(USER_PROFILE_PROPERTIES_COLUMN_LABEL);
			ugh.setOwnerId(rs.getString(COL_USER_PROFILE_ID));
			String userName = rs.getString(COL_PRINCIPAL_ALIAS_DISPLAY);
			if (upProperties!=null) {
				ugh.setIsIndividual(true);
				UserProfileUtils.fillUserGroupHeaderFromUserProfileBlob(upProperties, userName, ugh);
			} else {
				ugh.setIsIndividual(false);
			}
			return tm;
		}
	};
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.TeamDAO#create(org.sagebionetworks.repo.model.Team)
	 */
	@WriteTransaction
	@Override
	public Team create(Team dto) throws DatastoreException, InvalidModelException {
		if (dto.getId() == null) {
			throw new InvalidModelException("ID is required");
		}
		DBOTeam dbo = new DBOTeam();
		TeamUtils.copyDtoToDbo(dto, dbo);
		dbo.setEtag(UUID.randomUUID().toString());
		dbo = basicDao.createNew(dbo);
		Team result = TeamUtils.copyDboToDto(dbo);
		transactionalMessenger.sendMessageAfterCommit(dbo.getId().toString(), ObjectType.PRINCIPAL, ChangeType.CREATE);
		return result;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.TeamDAO#get(java.lang.String)
	 */
	@Override
	public Team get(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_TEAM_ID.toLowerCase(), id);
		DBOTeam dbo = null;
		try {
			dbo = basicDao.getObjectByPrimaryKey(DBOTeam.class, param);
		} catch(NotFoundException e) {
			throw new NotFoundException("Team does not exist for teamId: " + id, e);
		}
		Team dto = TeamUtils.copyDboToDto(dbo);
		return dto;
	}

	@Override
	public void validateTeamExists(String teamId) {
		boolean exists = jdbcTemplate.queryForObject(SELECT_CHECK_TEAM_EXISTS, boolean.class, teamId);
		if (!exists) {
			throw new NotFoundException("Team does not exist for teamId: " + teamId);
		}
	}
	
	@Override
	public ListWrapper<Team> list(List<Long> ids) throws DatastoreException, NotFoundException {
		if (ids==null || ids.size()<1) {
			return ListWrapper.wrap(Collections.emptyList(), Team.class);
		}
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_TEAM_ID, ids);
		List<DBOTeam> dbos = namedJdbcTemplate.query(SELECT_BY_IDS, param, TEAM_ROW_MAPPER);
		
		Map<String,Team> map = new HashMap<String,Team>();
		for (DBOTeam dbo : dbos) {
			Team team = TeamUtils.copyDboToDto(dbo);
			map.put(team.getId(), team);
		}
		
		List<Team> dtos = new ArrayList<Team>();
		for (Long id : ids) {
			Team team = map.get(id.toString());
			if (team == null) {
				throw new NotFoundException("Team with id " + id + " not found");
			}
			dtos.add(team);
		}
		
		return ListWrapper.wrap(dtos, Team.class);
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
		List<DBOTeam> dbos = namedJdbcTemplate.query(SELECT_PAGINATED, param, TEAM_ROW_MAPPER);
		List<Team> dtos = new ArrayList<Team>();
		for (DBOTeam dbo : dbos) dtos.add(TeamUtils.copyDboToDto(dbo));
		return dtos;
	}

	@Override
	public long getCount() throws DatastoreException {
		return jdbcTemplate.queryForObject(SELECT_COUNT, Long.class);
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
		List<DBOTeam> dbos = namedJdbcTemplate.query(SELECT_FOR_MEMBER_PAGINATED, param, TEAM_ROW_MAPPER);
		List<Team> dtos = new ArrayList<Team>();
		for (DBOTeam dbo : dbos) dtos.add(TeamUtils.copyDboToDto(dbo));
		return dtos;
	}

	@Override
	public List<String> getIdsForMember(String teamMemberId, long limit, long offset, TeamSortOrder sort, Boolean ascending) {
		ValidateArgument.required(teamMemberId, "principalId");
		ValidateArgument.requirement((sort == null && ascending == null)
				|| (sort != null && ascending != null),"sort and ascending must both be null or both be not null");
		String query = SELECT_IDS_FOR_MEMBER;
		if (sort != null && ascending != null) {
			query = SELECT_IDS_FOR_MEMBER_SORTED_BY_NAME_PREFIX;
			if (sort == TEAM_NAME) {
				query += ORDER_BY_TEAM_NAME;
			} else {
				throw new IllegalArgumentException("Unsupported order " + sort);
			}
			query += ascending ? ASC : DESC;
		}
		query += LIMIT_AND_OFFSET;
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(COL_GROUP_MEMBERS_MEMBER_ID, teamMemberId);
		params.addValue(LIMIT_PARAM_NAME, limit);
		params.addValue(OFFSET_PARAM_NAME, offset);
		return namedJdbcTemplate.queryForList(query, params, String.class);
	}

	@Override
	public long getCountForMember(String principalId) throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(COL_GROUP_MEMBERS_MEMBER_ID, principalId);
		return namedJdbcTemplate.queryForObject(SELECT_FOR_MEMBER_COUNT, param, Long.class);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.TeamDAO#update(org.sagebionetworks.repo.model.Team)
	 */
	@WriteTransaction
	@Override
	public Team update(Team dto) throws InvalidModelException,
			NotFoundException, ConflictingUpdateException, DatastoreException {

		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_TEAM_ID, dto.getId());
		DBOTeam dbo = null;
		try{
			dbo = namedJdbcTemplate.queryForObject(SELECT_FOR_UPDATE_SQL, param, TEAM_ROW_MAPPER);
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
		}
		
		// Update with a new e-tag
		dto.setEtag(UUID.randomUUID().toString());
		TeamUtils.copyDtoToDbo(dto, dbo);

		boolean success = basicDao.update(dbo);
		if (!success) throw new DatastoreException("Unsuccessful updating Team in database.");
		// update message.
		transactionalMessenger.sendMessageAfterCommit(dbo.getId().toString(), ObjectType.PRINCIPAL, ChangeType.UPDATE);
		Team resultantDto = TeamUtils.copyDboToDto(dbo);
		
		return resultantDto;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.TeamDAO#delete(java.lang.String)
	 */
	@WriteTransaction
	@Override
	public void delete(String id) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_TEAM_ID.toLowerCase(), id);
		try {
			basicDao.deleteObjectByPrimaryKey(DBOTeam.class, param);
		} catch (DataIntegrityViolationException e) {
			throw new IllegalArgumentException("Cannot delete team "+id+".  It is referenced by another object.", e);
		}
		// update message.
		transactionalMessenger.sendDeleteMessageAfterCommit(id, ObjectType.PRINCIPAL);
	}

	@Override
	public Map<Team, Collection<TeamMember>> getAllTeamsAndMembers() throws DatastoreException {
		// first get all the Teams and Members, regardless of whether the members are administrators
		List<TeamMemberPair> queryResults = namedJdbcTemplate.query(SELECT_ALL_TEAMS_AND_MEMBERS, TEAM_MEMBER_PAIR_ROW_MAPPER);
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
		List<TeamMemberId> adminTeamMembers = namedJdbcTemplate.query(SELECT_ALL_TEAMS_AND_ADMIN_MEMBERS, TEAM_MEMBER_ID_ROW_MAPPER);
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

	@Override
	public List<TeamMember> getMembersInRange(String teamId, Set<Long> include, Set<Long> exclude, long limit, long offset)
			throws DatastoreException {
		// Note: isAdmin is not set for each team member, do this in the manager!

		if (limit <= 0) throw new IllegalArgumentException("'limit' param must be greater than zero.");

		MapSqlParameterSource param = new MapSqlParameterSource();
		StringBuilder sql = new StringBuilder();
		sql.append(SELECT_MEMBERS_OF_TEAM_CORE).append(" ").append(AND_MEMBERS_FOR_ONE_GROUP);
		param.addValue(GROUP_MEMBERS_GROUP_ID_PARAM_NAME, teamId);

		if (include != null && !include.isEmpty()) {
			sql.append(" ").append(AND_USERS_IN_ID_LIST);
			param.addValue(INCLUSION_USER_PROFILE_ID_PARAM, include);
		}

		if (exclude != null && !exclude.isEmpty()) {
			sql.append(" ").append(AND_USERS_NOT_IN_ID_LIST);
			param.addValue(EXCLUSION_USER_PROFILE_ID_PARAM, exclude);
		}

		sql.append(" ").append(PAGINATED);
		param.addValue(OFFSET_PARAM_NAME, offset);
		param.addValue(LIMIT_PARAM_NAME, limit);

		List<TeamMember> tms = namedJdbcTemplate.query(sql.toString(), param, TEAM_MEMBER_ROW_MAPPER);
		return tms;
	}

	// update the 'isAdmin' field for those members that are admins on their teams
	private Map<TeamMemberId, TeamMember> setAdminStatus(List<TeamMember> teamMembers) {
		Set<String> teamIds = new HashSet<String>();
		Map<TeamMemberId, TeamMember> teamMemberMap = new HashMap<TeamMemberId, TeamMember>();
		if (teamMembers.isEmpty()) return teamMemberMap;
		for (TeamMember tm : teamMembers) {
			teamIds.add(tm.getTeamId());
			TeamMemberId tmi = new TeamMemberId(Long.parseLong(tm.getTeamId()), 
					Long.parseLong(tm.getMember().getOwnerId()));
			teamMemberMap.put(tmi, tm);
		}
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_GROUP_MEMBERS_GROUP_ID, teamIds);
		List<TeamMemberId> adminTeamMembers = namedJdbcTemplate.query(SELECT_ADMIN_MEMBERS_OF_TEAMS, param,
				TEAM_MEMBER_ID_ROW_MAPPER);
		for (TeamMemberId tmi : adminTeamMembers) {
			TeamMember tm = teamMemberMap.get(tmi);
			if (tm!=null) tm.setIsAdmin(true);
		}	
		
		return teamMemberMap;
	}
	
	@Override
	public ListWrapper<TeamMember> listMembers(List<Long> teamIds, List<Long> principalIds) throws NotFoundException, DatastoreException {
		if (teamIds==null || teamIds.size()<1 || principalIds==null || principalIds.size()<1) {
			return ListWrapper.wrap(Collections.emptyList(), TeamMember.class);
		}
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_GROUP_MEMBERS_GROUP_ID, teamIds);
		param.addValue(COL_GROUP_MEMBERS_MEMBER_ID, principalIds);
		List<TeamMember> teamMembers = namedJdbcTemplate.query(SELECT_MEMBERS_OF_TEAMS_GIVEN_MEMBER_IDS, param,
				TEAM_MEMBER_ROW_MAPPER);
		
		Map<TeamMemberId,TeamMember> map = setAdminStatus(teamMembers);

		List<TeamMember> result = new ArrayList<TeamMember>();
		for (Long teamId : teamIds) {
			for (Long principalId : principalIds) {
				TeamMemberId key = new TeamMemberId(teamId, principalId);
				TeamMember tm = map.get(key);
				if (tm==null) throw new NotFoundException("teamId: "+teamId+" memberId: "+principalId);
				result.add(tm);
			}
		}
		return ListWrapper.wrap(result, TeamMember.class);
	}
	
	@Override
	public List<String> getAdminTeamMemberIds(String teamId)
			throws NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(COL_GROUP_MEMBERS_GROUP_ID, teamId);
		return namedJdbcTemplate.query(SELECT_ADMIN_MEMBER_IDS, param, MEMBER_ID_ROW_MAPPER);
	}
	
	
	@Override
	public TeamMember getMember(String teamId, String principalId) throws NotFoundException, DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(COL_GROUP_MEMBERS_GROUP_ID, teamId);
		param.addValue(COL_GROUP_MEMBERS_MEMBER_ID, principalId);
		List<TeamMember> teamMembers = namedJdbcTemplate.query(SELECT_SINGLE_MEMBER_OF_TEAM, param, TEAM_MEMBER_ROW_MAPPER);
		if (teamMembers.size()==0) throw new NotFoundException("Could not find member "+principalId+" in team "+teamId);
		if (teamMembers.size()>1) throw new DatastoreException("Expected one result but found "+teamMembers.size());
		TeamMember theMember = teamMembers.get(0);
		// now find if it's an admin
		long adminCount = namedJdbcTemplate.queryForObject(IS_MEMBER_AN_ADMIN, param, Long.class);
		if (adminCount==1) theMember.setIsAdmin(true);
		if (adminCount>1) throw new DatastoreException("Expected 0-1 but found "+adminCount);
		return theMember;
	}
	
	@Override
	public long getAdminMemberCount(String teamId) throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(COL_GROUP_MEMBERS_GROUP_ID, teamId);
		return namedJdbcTemplate.queryForObject(SELECT_ADMIN_MEMBERS_OF_TEAM_COUNT, param, Long.class);

	}

	@Override
	public long getMembersCount(String teamId) throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(COL_GROUP_MEMBERS_GROUP_ID, teamId);
		return namedJdbcTemplate.queryForObject(SELECT_MEMBERS_OF_TEAM_COUNT, param, Long.class);
	}

	@Override
	public List<String> getAllTeamsUserIsAdmin(String userId) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_GROUP_MEMBERS_MEMBER_ID, userId);
		return namedJdbcTemplate.queryForList(SELECT_ALL_TEAMS_USER_IS_ADMIN, param, String.class);
	}
}
