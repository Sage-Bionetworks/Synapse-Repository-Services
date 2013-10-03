/**
 * 
 */
package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.ids.ETagGenerator;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.TeamHeader;
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
	private ETagGenerator eTagGenerator;
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;

	private static final RowMapper<DBOTeam> teamRowMapper = (new DBOTeam()).getTableMapping();
	
	private static final String SELECT_SQL_PAGINATED = 
			"SELECT * FROM "+TABLE_TEAM+
			" LIMIT :"+LIMIT_PARAM_NAME+" OFFSET :"+OFFSET_PARAM_NAME;
	
	private static final String SELECT_COUNT_FOR_MEMBER_SQL = 
			"SELECT count(*) FROM "+TABLE_GROUP_MEMBERS+" gm, "+TABLE_TEAM+" t "+
			" WHERE t."+COL_TEAM_ID+"=gm."+COL_GROUP_MEMBERS_GROUP_ID+" AND "+
			" gm."+COL_GROUP_MEMBERS_MEMBER_ID+" IN (:"+COL_GROUP_MEMBERS_MEMBER_ID+")";

	private static final String SELECT_COUNT_SQL = 
			"SELECT COUNT(*) FROM "+TABLE_TEAM;

	private static final String SELECT_GROUPS_OF_MEMBER = 
			"SELECT t.* FROM "+TABLE_GROUP_MEMBERS+" gm, "+TABLE_TEAM+" t "+
			" WHERE t."+COL_TEAM_ID+"=gm."+COL_GROUP_MEMBERS_GROUP_ID+" AND "+
			" gm."+COL_GROUP_MEMBERS_MEMBER_ID+" IN (:"+COL_GROUP_MEMBERS_MEMBER_ID+")"+
			" LIMIT :"+LIMIT_PARAM_NAME+" OFFSET :"+OFFSET_PARAM_NAME;

	
	private static final String USER_PROFILE_PROPERTIES_COLUMN_LABEL = "USER_PROFILE_PROPERTIES";

	private static final String SELECT_ALL_TEAMS_AND_MEMBERS =
		"SELECT t.*, up."+COL_USER_PROFILE_PROPS_BLOB+" as "+USER_PROFILE_PROPERTIES_COLUMN_LABEL+
		" FROM "+TABLE_TEAM+" t, "+TABLE_GROUP_MEMBERS+" gm LEFT OUTER JOIN "+
		TABLE_USER_PROFILE+" up ON (gm."+COL_GROUP_MEMBERS_MEMBER_ID+"=up."+COL_USER_PROFILE_ID+") "+
		" WHERE t."+COL_TEAM_ID+"=gm."+COL_GROUP_MEMBERS_GROUP_ID;

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
		dbo.setEtag(eTagGenerator.generateETag());
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
	public List<Team> getInRange(long offset, long limit)
			throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(OFFSET_PARAM_NAME, offset);
		if (limit<=0) throw new IllegalArgumentException("'to' param must be greater than 'from' param.");
		param.addValue(LIMIT_PARAM_NAME, limit);	
		List<DBOTeam> dbos = simpleJdbcTemplate.query(SELECT_SQL_PAGINATED, teamRowMapper, param);
		List<Team> dtos = new ArrayList<Team>();
		for (DBOTeam dbo : dbos) dtos.add(TeamUtils.copyDboToDto(dbo));
		return dtos;
	}

	@Override
	public long getCount() throws DatastoreException {
		return simpleJdbcTemplate.queryForLong(SELECT_COUNT_SQL);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.TeamDAO#getForMemberInRange(java.lang.String, long, long)
	 */
	@Override
	public List<Team> getForMemberInRange(String principalId, long offset,
			long limit) throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(COL_GROUP_MEMBERS_MEMBER_ID, principalId);
		param.addValue(OFFSET_PARAM_NAME, offset);
		if (limit<=0) throw new IllegalArgumentException("'to' param must be greater than 'from' param.");
		param.addValue(LIMIT_PARAM_NAME, limit);	
		List<DBOTeam> dbos = simpleJdbcTemplate.query(SELECT_GROUPS_OF_MEMBER, teamRowMapper, param);
		List<Team> dtos = new ArrayList<Team>();
		for (DBOTeam dbo : dbos) dtos.add(TeamUtils.copyDboToDto(dbo));
		return dtos;
	}

	@Override
	public long getCountForMember(String principalId) throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		param.addValue(COL_GROUP_MEMBERS_MEMBER_ID, principalId);
		return simpleJdbcTemplate.queryForLong(SELECT_COUNT_FOR_MEMBER_SQL, param);
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
		
		TeamUtils.copyDtoToDbo(dto, dbo);
		// Update with a new e-tag
		dbo.setEtag(eTagGenerator.generateETag());

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
		private TeamHeader teamHeader;
		private UserGroupHeader userGroupHeader;
		public TeamHeader getTeamHeader() {
			return teamHeader;
		}
		public void setTeamHeader(TeamHeader teamHeader) {
			this.teamHeader = teamHeader;
		}
		public UserGroupHeader getUserGroupHeader() {
			return userGroupHeader;
		}
		public void setUserGroupHeader(UserGroupHeader userGroupHeader) {
			this.userGroupHeader = userGroupHeader;
		}
	}
	
	private static final RowMapper<TeamMemberPair> teamMemberPairRowMapper = new RowMapper<TeamMemberPair>(){
		@Override
		public TeamMemberPair mapRow(ResultSet rs, int rowNum) throws SQLException {
			TeamMemberPair tmp = new TeamMemberPair();
			TeamHeader teamHeader = new TeamHeader();
			tmp.setTeamHeader(teamHeader);
			teamHeader.setId(rs.getString(COL_TEAM_ID));
			{
				Blob teamProperties = rs.getBlob(COL_TEAM_PROPERTIES);
				Team team = TeamUtils.deserialize(teamProperties.getBytes(1, (int) teamProperties.length()));
				teamHeader.setName(team.getName());
			}
			{
				UserGroupHeader ugh = new UserGroupHeader();
				tmp.setUserGroupHeader(ugh);
				Blob upProperties = rs.getBlob(USER_PROFILE_PROPERTIES_COLUMN_LABEL);
				if (upProperties!=null) {
					UserProfile up = UserProfileUtils.deserialize(upProperties.getBytes(1, (int) upProperties.length()));
					ugh.setDisplayName(up.getDisplayName());
					ugh.setFirstName(up.getFirstName());
					ugh.setIsIndividual(true);
					ugh.setLastName(up.getLastName());
					ugh.setOwnerId(up.getOwnerId());
					ugh.setPic(up.getPic());
				} else {
					ugh.setIsIndividual(false);
				}
			}
			return tmp;
		}
	};

	@Override
	public Map<TeamHeader, List<UserGroupHeader>> getAllTeamsAndMembers()
			throws DatastoreException {
		List<TeamMemberPair> results = simpleJdbcTemplate.query(SELECT_ALL_TEAMS_AND_MEMBERS, teamMemberPairRowMapper);
		Map<TeamHeader, List<UserGroupHeader>> map = new HashMap<TeamHeader, List<UserGroupHeader>>();
		for (TeamMemberPair tmp : results) {
			List<UserGroupHeader> ughs = map.get(tmp.getTeamHeader());
			if (ughs==null) {
				ughs = new ArrayList<UserGroupHeader>();
				map.put(tmp.getTeamHeader(), ughs);
			}
			ughs.add(tmp.getUserGroupHeader());
		}
		return map;
	}

}
