package org.sagebionetworks.repo.manager.statistics.monthly;

import java.time.YearMonth;
import java.util.List;

import org.sagebionetworks.repo.model.statistics.StatisticsObjectType;

public interface StatisticsMonthlyManager {

	/**
	 * Returns the list of {@link MonthOfTheYear} that needs to be processed for the given type of object
	 * 
	 * @param objectType The statistics object type
	 * @return The list of {@link MonthOfTheYear} that still needs to be processed
	 */
	List<YearMonth> getUnprocessedMonths(StatisticsObjectType objectType);

	/**
	 * Process the statistics for the given object type and month
	 * 
	 * @param objectType The statistics object type
	 * @param month      The month to be processed
	 * @return True if the month was processed, false if the processing wasn't needed
	 */
	boolean processMonth(StatisticsObjectType objectType, YearMonth month);

}
