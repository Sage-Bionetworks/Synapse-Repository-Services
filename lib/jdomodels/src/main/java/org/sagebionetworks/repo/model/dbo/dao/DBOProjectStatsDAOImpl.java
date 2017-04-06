package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STAT_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STAT_USER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PROJECT_STAT;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.ProjectStat;
import org.sagebionetworks.repo.model.ProjectStatsDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.persistence.DBOProjectStat;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class DBOProjectStatsDAOImpl implements ProjectStatsDAO {

	private static final String SQL_GET_STATS_FOR_USER = "SELECT * FROM " + TABLE_PROJECT_STAT + " WHERE " + COL_PROJECT_STAT_USER_ID
			+ " = ?";
	private static final String SQL_GET_STATS = "SELECT * FROM " + TABLE_PROJECT_STAT + " WHERE " + COL_PROJECT_STAT_PROJECT_ID + " = ? and "
			+ COL_PROJECT_STAT_USER_ID + " = ?";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DBOBasicDao basicDAO;

	@Autowired
	private IdGenerator idGenerator;

	private static TableMapping<DBOProjectStat> rowMapper = new DBOProjectStat().getTableMapping();

	@WriteTransaction
	@Override
	public void update(ProjectStat projectStat) {
		ValidateArgument.required(projectStat.getLastAccessed(), "ProjectStat.lastAccessed");

		DBOProjectStat dbo;
		try {
			dbo = jdbcTemplate.queryForObject(SQL_GET_STATS, rowMapper, projectStat.getProjectId(), projectStat.getUserId());
			if (projectStat.getLastAccessed().after(dbo.getLastAccessed())) {
				dbo.setLastAccessed(projectStat.getLastAccessed());
				dbo.setEtag(UUID.randomUUID().toString());
				basicDAO.update(dbo);
			}
		} catch (EmptyResultDataAccessException e) {
			dbo = new DBOProjectStat();
			dbo.setId(idGenerator.generateNewId(IdType.PROJECT_STATS_ID));
			dbo.setProjectId(projectStat.getProjectId());
			dbo.setUserId(projectStat.getUserId());
			dbo.setLastAccessed(projectStat.getLastAccessed());
			dbo.setEtag(UUID.randomUUID().toString());
			basicDAO.createNew(dbo);
		}
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
