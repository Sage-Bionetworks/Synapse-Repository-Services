package org.sagebionetworks.repo.model.dbo.dao.statistics;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STATISTICS_MONTHLY_MONTH;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STATISTICS_MONTHLY_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STATISTICS_MONTHLY_STATUS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_STATISTICS_MONTHLY;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.dao.statistics.StatisticsMonthlyStatusDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.statistics.monthly.DBOMonthlyStatisticsStatus;
import org.sagebionetworks.repo.model.statistics.StatisticsMonthlyStatus;
import org.sagebionetworks.repo.model.statistics.StatisticsMonthlyUtils;
import org.sagebionetworks.repo.model.statistics.StatisticsObjectType;
import org.sagebionetworks.repo.model.statistics.StatisticsStatus;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.EmptySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
public class StatisticsMonthlyStatusDAOImpl implements StatisticsMonthlyStatusDAO {

	private static final String PARAM_OBJECT_TYPE = "objectType";
	private static final String PARAM_MONTH = "month";
	private static final String PARAM_STATUS = "status";
	private static final String PARAM_FROM = "from";
	private static final String PARAM_TO = "to";

	private static final String SQL_DELETE_ALL = "DELETE FROM " + TABLE_STATISTICS_MONTHLY;
	private static final String SQL_SELECT_IN_RANGE = "SELECT * FROM " + TABLE_STATISTICS_MONTHLY + " WHERE "
			+ COL_STATISTICS_MONTHLY_OBJECT_TYPE + " = :" + PARAM_OBJECT_TYPE + " AND " + COL_STATISTICS_MONTHLY_STATUS + " =:"
			+ PARAM_STATUS + " AND " + COL_STATISTICS_MONTHLY_MONTH + " BETWEEN :" + PARAM_FROM + " AND :" + PARAM_TO + " ORDER BY "
			+ COL_STATISTICS_MONTHLY_MONTH;

	private static final RowMapper<DBOMonthlyStatisticsStatus> DBO_MAPPER = new DBOMonthlyStatisticsStatus().getTableMapping();

	private static final RowMapper<StatisticsMonthlyStatus> ROW_MAPPER = new RowMapper<StatisticsMonthlyStatus>() {

		@Override
		public StatisticsMonthlyStatus mapRow(ResultSet rs, int rowNum) throws SQLException {
			return map(DBO_MAPPER.mapRow(rs, rowNum));
		}
	};

	private DBOBasicDao basicDao;
	private NamedParameterJdbcTemplate jdbcTemplate;

	@Autowired
	public StatisticsMonthlyStatusDAOImpl(DBOBasicDao basicDao, NamedParameterJdbcTemplate jdbcTemplate) {
		this.basicDao = basicDao;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	@WriteTransaction
	public StatisticsMonthlyStatus setAvailable(StatisticsObjectType objectType, YearMonth month) {
		return createOrUpdate(objectType, month, StatisticsStatus.AVAILABLE, null, System.currentTimeMillis(), null);
	}

	@Override
	@WriteTransaction
	public StatisticsMonthlyStatus setProcessingFailed(StatisticsObjectType objectType, YearMonth month) {
		return createOrUpdate(objectType, month, StatisticsStatus.PROCESSING_FAILED, null, null, System.currentTimeMillis());
	}

	@Override
	@WriteTransaction
	public StatisticsMonthlyStatus setProcessing(StatisticsObjectType objectType, YearMonth month) {
		return createOrUpdate(objectType, month, StatisticsStatus.PROCESSING, System.currentTimeMillis(), null, null);
	}

	@Override
	public Optional<StatisticsMonthlyStatus> getStatus(StatisticsObjectType objectType, YearMonth month) {
		ValidateArgument.required(objectType, "objectType");
		ValidateArgument.required(month, "month");

		SqlParameterSource params = getPrimaryKeyParams(objectType, month);

		DBOMonthlyStatisticsStatus dbo = basicDao.getObjectByPrimaryKeyIfExists(DBOMonthlyStatisticsStatus.class, params);

		if (dbo == null) {
			return Optional.empty();
		}

		return Optional.of(map(dbo));
	}

	@Override
	public List<StatisticsMonthlyStatus> getAvailableStatusInRange(StatisticsObjectType objectType, YearMonth from, YearMonth to) {
		ValidateArgument.required(objectType, "objectType");
		ValidateArgument.required(from, "from");
		ValidateArgument.required(to, "to");
		ValidateArgument.requirement(from.isBefore(to), "The start of the range should be before the end");

		MapSqlParameterSource params = new MapSqlParameterSource();

		params.addValue(PARAM_OBJECT_TYPE, objectType.toString());
		params.addValue(PARAM_STATUS, StatisticsStatus.AVAILABLE.toString());
		params.addValue(PARAM_FROM, StatisticsMonthlyUtils.toDate(from));
		params.addValue(PARAM_TO, StatisticsMonthlyUtils.toDate(to));

		return jdbcTemplate.query(SQL_SELECT_IN_RANGE, params, ROW_MAPPER);
	}

	@Override
	@WriteTransaction
	public void clear() {
		jdbcTemplate.update(SQL_DELETE_ALL, EmptySqlParameterSource.INSTANCE);
	}

	private StatisticsMonthlyStatus createOrUpdate(StatisticsObjectType objectType, YearMonth month, StatisticsStatus status,
			Long lastStartedAt, Long lastSucceededAt, Long lastFailedAt) {
		ValidateArgument.required(objectType, "objectType");
		ValidateArgument.required(month, "month");
		ValidateArgument.required(status, "status");
		
		SqlParameterSource params = getPrimaryKeyParams(objectType, month);
		
		DBOMonthlyStatisticsStatus dbo;

		try {
			 dbo = basicDao.getObjectByPrimaryKeyWithUpdateLock(DBOMonthlyStatisticsStatus.class, params);
		} catch (NotFoundException e) {
			dbo = new DBOMonthlyStatisticsStatus();
		}

		dbo.setObjectType(objectType.toString());
		dbo.setMonth(StatisticsMonthlyUtils.toDate(month));
		dbo.setStatus(status.toString());
		
		// Updates the timestamps only if updated
		if (lastStartedAt != null) {
			dbo.setLastStartedAt(lastStartedAt);
		}
		if (lastSucceededAt != null) {
			dbo.setLastSucceededAt(lastSucceededAt);
		}
		if (lastFailedAt != null) {
			dbo.setLastFailedAt(lastFailedAt);
		}

		return map(basicDao.createOrUpdate(dbo));
	}

	private SqlParameterSource getPrimaryKeyParams(StatisticsObjectType objectType, YearMonth month) {
		MapSqlParameterSource params = new MapSqlParameterSource();

		params.addValue(PARAM_OBJECT_TYPE, objectType.toString());
		params.addValue(PARAM_MONTH, StatisticsMonthlyUtils.toDate(month));

		return params;
	}

	private static StatisticsMonthlyStatus map(DBOMonthlyStatisticsStatus dbo) {
		StatisticsMonthlyStatus dto = new StatisticsMonthlyStatus();

		dto.setObjectType(StatisticsObjectType.valueOf(dbo.getObjectType()));
		dto.setStatus(StatisticsStatus.valueOf(dbo.getStatus()));
		dto.setMonth(YearMonth.from(dbo.getMonth()));
		dto.setLastStartedAt(dbo.getLastStartedAt());
		dto.setLastSucceededAt(dbo.getLastSucceededAt());
		dto.setLastFailedAt(dbo.getLastFailedAt());

		return dto;

	}

}
