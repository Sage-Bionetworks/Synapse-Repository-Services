package org.sagebionetworks.repo.model.dao.statistics;

import java.util.Optional;

import org.sagebionetworks.repo.model.statistics.MonthOfTheYear;
import org.sagebionetworks.repo.model.statistics.StatisticsMonthlyStatus;
import org.sagebionetworks.repo.model.statistics.StatisticsObjectType;

public interface StatisticsMonthlyDAO {

	/**
	 * Retrieve the monthly statistics status for the given month
	 * 
	 * @param objectType The type of object
	 * @param month      The considered month
	 * @return An optional containing the statistics status for the given type and month if present,
	 *         empty otherwise
	 */
	Optional<StatisticsMonthlyStatus> getStatus(StatisticsObjectType objectType, MonthOfTheYear month);

}
