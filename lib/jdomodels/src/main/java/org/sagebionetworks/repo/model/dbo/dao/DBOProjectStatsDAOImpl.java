package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STAT_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STAT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STAT_LAST_ACCESSED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STAT_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STAT_USER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PROJECT_STAT;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.sagebionetworks.ids.BatchOfIds;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.ProjectStat;
import org.sagebionetworks.repo.model.ProjectStatsDAO;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.persistence.DBOProjectStat;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class DBOProjectStatsDAOImpl implements ProjectStatsDAO {

	private static final String SQL_GET_STATS_FOR_USER = "SELECT * FROM " + TABLE_PROJECT_STAT + " WHERE " + COL_PROJECT_STAT_USER_ID
			+ " = ?";
	
	/*
	 * Note: For an update, LAST_ACCESSED is only updated if the current value
	 * is less than the passed value.
	 */
	private static final String SQL_INSERT_BATCH =
			"INSERT INTO "+TABLE_PROJECT_STAT+ " ("
					+ COL_PROJECT_STAT_ID
					+", "+COL_PROJECT_STAT_PROJECT_ID
					+", "+COL_PROJECT_STAT_USER_ID
					+", "+COL_PROJECT_STAT_LAST_ACCESSED
					+", "+COL_PROJECT_STAT_ETAG
					+") VALUES(?,?,?,?,?)"
					+ " ON DUPLICATE KEY UPDATE"
					+ " "+COL_PROJECT_STAT_LAST_ACCESSED+" = CASE"
							+ " WHEN "+COL_PROJECT_STAT_LAST_ACCESSED+" <"
									+ " ? THEN ? ELSE "+COL_PROJECT_STAT_LAST_ACCESSED+" END"
					+ ", "+COL_PROJECT_STAT_ETAG+" = ?";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private IdGenerator idGenerator;

	private static TableMapping<DBOProjectStat> rowMapper = new DBOProjectStat().getTableMapping();

	@WriteTransaction
	@Override
	public void updateProjectStat(final ProjectStat...projectStats) {
		ValidateArgument.required(projectStats, "projectStats");
		for(ProjectStat stat: projectStats){
			ValidateArgument.required(stat.getProjectId(), "stat.projectId");
			ValidateArgument.required(stat.getUserId(), "stat.userId");
			ValidateArgument.required(stat.getLastAccessed(), "stat.lastAccessed");
		}
		// Reserve the IDs
		final BatchOfIds ids = idGenerator.generateBatchNewIds(IdType.PROJECT_STATS_ID, projectStats.length);
		
		jdbcTemplate.batchUpdate(SQL_INSERT_BATCH, new BatchPreparedStatementSetter() {
			
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				ProjectStat stat = projectStats[i];
				long statId = ids.getFirstId()+(i-1);
				String etag = UUID.randomUUID().toString();
				long lastAccessed = stat.getLastAccessed().getTime();
				int parameterIndex = 1;
				// insert parameters
				ps.setLong(parameterIndex, statId);
				ps.setLong(++parameterIndex, stat.getProjectId());
				ps.setLong(++parameterIndex, stat.getUserId());
				ps.setLong(++parameterIndex, lastAccessed);
				ps.setString(++parameterIndex, etag);
				// update parameters
				ps.setLong(++parameterIndex, lastAccessed);
				ps.setLong(++parameterIndex, lastAccessed);
				ps.setString(++parameterIndex, etag);
			}
			
			@Override
			public int getBatchSize() {
				return projectStats.length;
			}
		});
	}

	@Override
	public List<ProjectStat> getProjectStatsForUser(Long userId) {
		List<DBOProjectStat> queryResult = jdbcTemplate.query(SQL_GET_STATS_FOR_USER, new Object[] { userId }, rowMapper);
		return Lists.transform(queryResult, new Function<DBOProjectStat, ProjectStat>() {
			@Override
			public ProjectStat apply(DBOProjectStat input) {
				return new ProjectStat(input.getProjectId(), input.getUserId(), input.getLastAccessed(), input.getEtag());
			}
		});
	}

}
