/**
 * 
 */
package org.sagebionetworks.bridge.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_COMMUNITY_TEAM_COMMUNITY_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_COMMUNITY_TEAM_TEAM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GROUP_MEMBERS_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GROUP_MEMBERS_MEMBER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TEAM_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_COMMUNITY_TEAM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_GROUP_MEMBERS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TEAM;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.bridge.model.CommunityTeamDAO;
import org.sagebionetworks.bridge.model.dbo.persistence.DBOCommunityTeam;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class DBOCommunityTeamDAOImpl implements CommunityTeamDAO {

	@Autowired
	private DBOBasicDao basicDao;
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;

	private static final String SELECT_COMMUNITY_IDS = "SELECT DISTINCT " + COL_COMMUNITY_TEAM_COMMUNITY_ID + " FROM " + TABLE_COMMUNITY_TEAM;
	private static final String SELECT_FOR_MEMBER = 
			" SELECT DISTINCT ct." + COL_COMMUNITY_TEAM_COMMUNITY_ID + 
			" FROM " + TABLE_GROUP_MEMBERS + " gm, " + TABLE_TEAM + " t, " + TABLE_COMMUNITY_TEAM + " ct" +
			" WHERE t." + COL_TEAM_ID + "=gm." + COL_GROUP_MEMBERS_GROUP_ID +
			"   AND ct." + COL_COMMUNITY_TEAM_TEAM_ID + "=t." + COL_TEAM_ID +
			"   AND gm." + COL_GROUP_MEMBERS_MEMBER_ID + "= :" + COL_GROUP_MEMBERS_MEMBER_ID;

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void create(long communityId, long teamId) throws DatastoreException {
		DBOCommunityTeam dbo = new DBOCommunityTeam();
		dbo.setCommunityId(communityId);
		dbo.setTeamId(teamId);
		dbo = basicDao.createNew(dbo);
	}

	@Override
	public long getCommunityId(long teamId) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource(DBOCommunityTeam.TEAM_ID_FIELDNAME, teamId);
		DBOCommunityTeam dbo = basicDao.getObjectByPrimaryKey(DBOCommunityTeam.class, param);
		return dbo.getCommunityId();
	}

	@Override
	public List<Long> getCommunityIdsByMember(String memberId) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource(COL_GROUP_MEMBERS_MEMBER_ID, memberId);
		return simpleJdbcTemplate.query(SELECT_FOR_MEMBER, new RowMapper<Long>() {
			@Override
			public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
				Long id = rs.getLong(COL_COMMUNITY_TEAM_COMMUNITY_ID);
				return id;
			}
		}, param);
	}

	@Override
	public List<Long> getCommunityIds() throws DatastoreException, NotFoundException {
		return simpleJdbcTemplate.query(SELECT_COMMUNITY_IDS, new RowMapper<Long>() {
			@Override
			public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
				Long id = rs.getLong(COL_COMMUNITY_TEAM_COMMUNITY_ID);
				return id;
			}
		});
	}
}

