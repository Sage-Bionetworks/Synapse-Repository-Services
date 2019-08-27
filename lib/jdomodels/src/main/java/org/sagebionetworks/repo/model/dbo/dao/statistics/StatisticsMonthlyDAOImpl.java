package org.sagebionetworks.repo.model.dbo.dao.statistics;

import java.util.Optional;

import org.sagebionetworks.repo.model.dao.statistics.StatisticsMonthlyDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.statistics.DBOMonthlyStatisticsStatus;
import org.sagebionetworks.repo.model.statistics.MonthOfTheYear;
import org.sagebionetworks.repo.model.statistics.StatisticsMonthlyStatus;
import org.sagebionetworks.repo.model.statistics.StatisticsObjectType;
import org.sagebionetworks.repo.model.statistics.StatisticsStatus;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
public class StatisticsMonthlyDAOImpl implements StatisticsMonthlyDAO {

	private DBOBasicDao basicDao;

	@Autowired
	public StatisticsMonthlyDAOImpl(DBOBasicDao basicDao) {
		this.basicDao = basicDao;
	}

	@Override
	public Optional<StatisticsMonthlyStatus> getStatus(StatisticsObjectType objectType, MonthOfTheYear month) {
		ValidateArgument.required(objectType, "objectType");
		ValidateArgument.required(month, "month");

		SqlParameterSource params = getPrimaryKeyParams(objectType, month);
		
		DBOMonthlyStatisticsStatus dbo = basicDao.getObjectByPrimaryKeyIfExists(DBOMonthlyStatisticsStatus.class, params);

		if (dbo == null) {
			return Optional.empty();
		}

		return Optional.of(map(dbo));
	}
	
	private SqlParameterSource getPrimaryKeyParams(StatisticsObjectType objectType, MonthOfTheYear month) {
		MapSqlParameterSource params = new MapSqlParameterSource();

		params.addValue("objectType", objectType);
		params.addValue("month", month.toDate());

		return params;
	}

	private StatisticsMonthlyStatus map(DBOMonthlyStatisticsStatus dbo) {
		StatisticsMonthlyStatus dto = new StatisticsMonthlyStatus();

		dto.setObjectType(StatisticsObjectType.valueOf(dbo.getObjectType()));
		dto.setStatus(StatisticsStatus.valueOf(dbo.getStatus()));
		dto.setMonth(MonthOfTheYear.valueOf(dbo.getMonth()));

		return dto;

	}

}
