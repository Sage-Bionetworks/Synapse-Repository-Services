package org.sagebionetworks.repo.model.dbo.dao.statistics;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STATISTICS_MONTHLY_PROJECT_FILES_ACTION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STATISTICS_MONTHLY_PROJECT_FILES_MONTH;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STATISTICS_MONTHLY_PROJECT_FILES_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_STATISTICS_MONTHLY_PROJECT_FILES;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.dao.statistics.StatisticsMonthlyProjectDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.statistics.monthly.DBOMonthlyStatisticsProjectFiles;
import org.sagebionetworks.repo.model.statistics.FileAction;
import org.sagebionetworks.repo.model.statistics.monthly.StatisticsMonthlyProjectFiles;
import org.sagebionetworks.repo.model.statistics.monthly.StatisticsMonthlyUtils;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
public class StatisticsMonthlyProjectFilesDAOImpl implements StatisticsMonthlyProjectDAO {

	private static final String PARAM_PROJECT_ID = "projectId";
	private static final String PARAM_MONTH = "month";
	private static final String PARAM_FILE_ACTION = "fileAction";
	private static final String PARAM_FROM = "from";
	private static final String PARAM_TO = "to";

	private static final String SQL_SELECT_IN_RANGE = "SELECT * FROM " + TABLE_STATISTICS_MONTHLY_PROJECT_FILES + " WHERE "
			+ COL_STATISTICS_MONTHLY_PROJECT_FILES_PROJECT_ID + " = :" + PARAM_PROJECT_ID + " AND "
			+ COL_STATISTICS_MONTHLY_PROJECT_FILES_ACTION + " =:" + PARAM_FILE_ACTION + " AND " + COL_STATISTICS_MONTHLY_PROJECT_FILES_MONTH
			+ " BETWEEN :" + PARAM_FROM + " AND :" + PARAM_TO + " ORDER BY " + COL_STATISTICS_MONTHLY_PROJECT_FILES_MONTH;

	private static final RowMapper<DBOMonthlyStatisticsProjectFiles> DBO_MAPPER = new DBOMonthlyStatisticsProjectFiles().getTableMapping();

	private static final RowMapper<StatisticsMonthlyProjectFiles> ROW_MAPPER = new RowMapper<StatisticsMonthlyProjectFiles>() {
		@Override
		public StatisticsMonthlyProjectFiles mapRow(ResultSet rs, int rowNum) throws SQLException {
			return map(DBO_MAPPER.mapRow(rs, rowNum));
		}
	};

	private DBOBasicDao basicDao;
	private NamedParameterJdbcTemplate jdbcTemplate;

	@Autowired
	public StatisticsMonthlyProjectFilesDAOImpl(DBOBasicDao basicDao, NamedParameterJdbcTemplate jdbcTemplate) {
		this.basicDao = basicDao;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public List<StatisticsMonthlyProjectFiles> getProjectFilesStatisticsInRange(Long projectId, FileAction fileAction, YearMonth from,
			YearMonth to) {
		ValidateArgument.required(projectId, "projectId");
		ValidateArgument.required(fileAction, "fileAction");
		ValidateArgument.required(from, "from");
		ValidateArgument.required(to, "to");
		ValidateArgument.requirement(from.isBefore(to), "The start of the range should be before the end");

		MapSqlParameterSource params = new MapSqlParameterSource();

		params.addValue(PARAM_PROJECT_ID, projectId);
		params.addValue(PARAM_FILE_ACTION, fileAction.toString());
		params.addValue(PARAM_FROM, StatisticsMonthlyUtils.toDate(from));
		params.addValue(PARAM_TO, StatisticsMonthlyUtils.toDate(to));

		return jdbcTemplate.query(SQL_SELECT_IN_RANGE, params, ROW_MAPPER);
	}

	@Override
	public Optional<StatisticsMonthlyProjectFiles> getProjectFilesStatistics(Long projectId, FileAction fileAction, YearMonth month) {
		ValidateArgument.required(projectId, "projectId");
		ValidateArgument.required(fileAction, "fileAction");
		ValidateArgument.required(month, "month");

		SqlParameterSource params = getPrimaryKeyParams(projectId, month, fileAction);

		DBOMonthlyStatisticsProjectFiles dbo = basicDao.getObjectByPrimaryKeyIfExists(DBOMonthlyStatisticsProjectFiles.class, params);

		if (dbo == null) {
			return Optional.empty();
		}

		return Optional.of(map(dbo));
	}

	@Override
	@WriteTransaction
	public void saveBatch(List<StatisticsMonthlyProjectFiles> batch) {
		JdbcTemplate template = jdbcTemplate.getJdbcTemplate();
		
		template.batchUpdate("UPDATE blablabla", new BatchPreparedStatementSetter() {
			
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public int getBatchSize() {
				// TODO Auto-generated method stub
				return 0;
			}
		});

	}

	private MapSqlParameterSource getPrimaryKeyParams(Long projectId, YearMonth month, FileAction fileAction) {
		MapSqlParameterSource params = new MapSqlParameterSource();

		params.addValue(PARAM_PROJECT_ID, projectId);
		params.addValue(PARAM_MONTH, StatisticsMonthlyUtils.toDate(month));
		params.addValue(PARAM_FILE_ACTION, fileAction.toString());

		return params;
	}

	private static StatisticsMonthlyProjectFiles map(DBOMonthlyStatisticsProjectFiles dbo) {
		StatisticsMonthlyProjectFiles dto = new StatisticsMonthlyProjectFiles();

		dto.setProjectId(dbo.getProjectId());
		dto.setMonth(YearMonth.from(dbo.getMonth()));
		dto.setFilesCount(dbo.getFilesCount());
		dto.setUsersCount(dbo.getUsersCount());
		dto.setLastUpdatedOn(dbo.getLastUpdatedOn());

		return dto;
	}

}
