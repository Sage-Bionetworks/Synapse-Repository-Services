package org.sagebionetworks.repo.model.dbo.statistics;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STATISTICS_MONTHLY_PROJECT_FILES_EVENT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STATISTICS_MONTHLY_PROJECT_FILES_FILES_COUNT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STATISTICS_MONTHLY_PROJECT_FILES_LAST_UPDATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STATISTICS_MONTHLY_PROJECT_FILES_MONTH;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STATISTICS_MONTHLY_PROJECT_FILES_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STATISTICS_MONTHLY_PROJECT_FILES_USERS_COUNT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_STATISTICS_MONTHLY_PROJECT_FILES;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.statistics.FileEvent;
import org.sagebionetworks.repo.model.statistics.monthly.StatisticsMonthlyUtils;
import org.sagebionetworks.repo.model.statistics.project.StatisticsMonthlyProjectFiles;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
public class StatisticsMonthlyProjectFilesDAOImpl implements StatisticsMonthlyProjectFilesDAO {

	private static final String PARAM_PROJECT_ID = "projectId";
	private static final String PARAM_MONTH = "month";
	private static final String PARAM_EVENT_TYPE = "eventType";

	// @formatter:off

	private static final String SQL_DELETE_ALL = "DELETE FROM " + TABLE_STATISTICS_MONTHLY_PROJECT_FILES;

	private static final String SQL_SELECT_IN_RANGE = "SELECT * FROM " 
			+ TABLE_STATISTICS_MONTHLY_PROJECT_FILES + " WHERE "
			+ COL_STATISTICS_MONTHLY_PROJECT_FILES_PROJECT_ID + " = ? AND "
			+ COL_STATISTICS_MONTHLY_PROJECT_FILES_EVENT_TYPE + " = ? AND " 
			+ COL_STATISTICS_MONTHLY_PROJECT_FILES_MONTH + " BETWEEN ? AND ?"
			+ " ORDER BY " + COL_STATISTICS_MONTHLY_PROJECT_FILES_MONTH;
	
	private static final String SQL_COUNT_PROJECTS_IN_RANGE = "SELECT COUNT(DISTINCT " + COL_STATISTICS_MONTHLY_PROJECT_FILES_PROJECT_ID + ") FROM " 
			+ TABLE_STATISTICS_MONTHLY_PROJECT_FILES + " WHERE "
			+ COL_STATISTICS_MONTHLY_PROJECT_FILES_EVENT_TYPE + " = ? AND " 
			+ COL_STATISTICS_MONTHLY_PROJECT_FILES_MONTH + " BETWEEN ? AND ? " 
			+ "ORDER BY " + COL_STATISTICS_MONTHLY_PROJECT_FILES_MONTH;

	private static final String SQL_SAVE_BATCH = "INSERT INTO " + TABLE_STATISTICS_MONTHLY_PROJECT_FILES 
			+ "(" + COL_STATISTICS_MONTHLY_PROJECT_FILES_PROJECT_ID + ", " 
			+ COL_STATISTICS_MONTHLY_PROJECT_FILES_MONTH + ", "
			+ COL_STATISTICS_MONTHLY_PROJECT_FILES_EVENT_TYPE + ", " 
			+ COL_STATISTICS_MONTHLY_PROJECT_FILES_FILES_COUNT + ", "
			+ COL_STATISTICS_MONTHLY_PROJECT_FILES_USERS_COUNT + ", " 
			+ COL_STATISTICS_MONTHLY_PROJECT_FILES_LAST_UPDATED_ON
			+ ") VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE " 
			+ COL_STATISTICS_MONTHLY_PROJECT_FILES_FILES_COUNT + " = ?, "
			+ COL_STATISTICS_MONTHLY_PROJECT_FILES_USERS_COUNT + " = ?, " 
			+ COL_STATISTICS_MONTHLY_PROJECT_FILES_LAST_UPDATED_ON + " = ?";

	// @formatter:on

	private static final RowMapper<DBOStatisticsMonthlyProjectFiles> DBO_MAPPER = new DBOStatisticsMonthlyProjectFiles().getTableMapping();

	private static final RowMapper<StatisticsMonthlyProjectFiles> ROW_MAPPER = new RowMapper<StatisticsMonthlyProjectFiles>() {
		@Override
		public StatisticsMonthlyProjectFiles mapRow(ResultSet rs, int rowNum) throws SQLException {
			return map(DBO_MAPPER.mapRow(rs, rowNum));
		}
	};

	private DBOBasicDao basicDao;
	private JdbcTemplate jdbcTemplate;

	@Autowired
	public StatisticsMonthlyProjectFilesDAOImpl(DBOBasicDao basicDao, JdbcTemplate jdbcTemplate) {
		this.basicDao = basicDao;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public List<StatisticsMonthlyProjectFiles> getProjectFilesStatisticsInRange(Long projectId, FileEvent eventType, YearMonth from,
			YearMonth to) {
		ValidateArgument.required(projectId, "projectId");
		ValidateArgument.required(eventType, "eventType");
		ValidateArgument.required(from, "from");
		ValidateArgument.required(to, "to");
		ValidateArgument.requirement(from.equals(to) || from.isBefore(to), "The start of the range should be before the end");
		
		LocalDate fromDate = StatisticsMonthlyUtils.toDate(from);
		LocalDate toDate = StatisticsMonthlyUtils.toDate(to);
		
		return jdbcTemplate.query(SQL_SELECT_IN_RANGE, ROW_MAPPER, projectId, eventType.toString(), fromDate, toDate);
	}

	@Override
	public Long countProjectsInRange(FileEvent eventType, YearMonth from, YearMonth to) {
		ValidateArgument.required(eventType, "eventType");
		ValidateArgument.required(from, "from");
		ValidateArgument.required(to, "to");
		ValidateArgument.requirement(from.equals(to) || from.isBefore(to), "The start of the range should be before the end");

		LocalDate fromDate = StatisticsMonthlyUtils.toDate(from);
		LocalDate toDate = StatisticsMonthlyUtils.toDate(to);
		
		return jdbcTemplate.queryForObject(SQL_COUNT_PROJECTS_IN_RANGE, Long.class, eventType.toString(), fromDate, toDate);
	}

	@Override
	public Optional<StatisticsMonthlyProjectFiles> getProjectFilesStatistics(Long projectId, FileEvent eventType, YearMonth month) {
		ValidateArgument.required(projectId, "projectId");
		ValidateArgument.required(eventType, "eventType");
		ValidateArgument.required(month, "month");

		SqlParameterSource params = getPrimaryKeyParams(projectId, month, eventType);

		Optional<DBOStatisticsMonthlyProjectFiles> dbo = basicDao.getObjectByPrimaryKeyIfExists(DBOStatisticsMonthlyProjectFiles.class, params);
		return dbo.map(StatisticsMonthlyProjectFilesDAOImpl::map);
	}

	@Override
	@WriteTransaction
	public void save(List<StatisticsMonthlyProjectFiles> batch) {
		ValidateArgument.required(batch, "batch");
		
		if (batch.isEmpty()) {
			return;
		}

		jdbcTemplate.batchUpdate(SQL_SAVE_BATCH, new BatchPreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				StatisticsMonthlyProjectFiles dto = batch.get(i);

				int index = 1;

				long now = System.currentTimeMillis();

				// On create fields
				ps.setLong(index++, dto.getProjectId());
				ps.setObject(index++, StatisticsMonthlyUtils.toDate(dto.getMonth()));
				ps.setString(index++, dto.getEventType().toString());
				ps.setInt(index++, dto.getFilesCount());
				ps.setInt(index++, dto.getUsersCount());
				ps.setLong(index++, now);

				// On duplicate update fields
				ps.setInt(index++, dto.getFilesCount());
				ps.setInt(index++, dto.getUsersCount());
				ps.setLong(index++, now);

			}

			@Override
			public int getBatchSize() {
				return batch.size();
			}
		});

	}

	@Override
	@WriteTransaction
	public void clear() {
		jdbcTemplate.update(SQL_DELETE_ALL);
	}

	private MapSqlParameterSource getPrimaryKeyParams(Long projectId, YearMonth month, FileEvent eventType) {
		MapSqlParameterSource params = new MapSqlParameterSource();

		params.addValue(PARAM_PROJECT_ID, projectId);
		params.addValue(PARAM_MONTH, StatisticsMonthlyUtils.toDate(month));
		params.addValue(PARAM_EVENT_TYPE, eventType.toString());

		return params;
	}

	private static StatisticsMonthlyProjectFiles map(DBOStatisticsMonthlyProjectFiles dbo) {
		StatisticsMonthlyProjectFiles dto = new StatisticsMonthlyProjectFiles();

		dto.setProjectId(dbo.getProjectId());
		dto.setMonth(YearMonth.from(dbo.getMonth()));
		dto.setEventType(FileEvent.valueOf(dbo.getEventType()));
		dto.setFilesCount(dbo.getFilesCount());
		dto.setUsersCount(dbo.getUsersCount());
		dto.setLastUpdatedOn(dbo.getLastUpdatedOn());

		return dto;
	}

}
