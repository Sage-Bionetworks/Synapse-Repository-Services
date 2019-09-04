package org.sagebionetworks.repo.manager.statistics.monthly;

import java.time.YearMonth;
import java.util.List;

import org.sagebionetworks.repo.model.statistics.StatisticsObjectType;
import org.sagebionetworks.repo.model.statistics.StatisticsStatus;

public interface StatisticsMonthlyManager {

	/**
	 * Returns the list of {@link MonthOfTheYear} that might need to be processed for the given type of object, in
	 * particular all the months that do not have any status or that are not in {@link StatisticsStatus#AVAILABLE} status.
	 * 
	 * Note that this method might return months that are in {@link StatisticsStatus#PROCESSING} status.
	 * 
	 * @param  objectType The statistics object type
	 * @return            The list of {@link MonthOfTheYear} that still needs to be processed
	 */
	List<YearMonth> getUnprocessedMonths(StatisticsObjectType objectType);

	/**
	 * Process the statistics for the given object type and month iff the status is not present, if the status is not
	 * {@link StatisticsStatus#AVAILABLE} or {@link StatisticsStatus#PROCESSING} unless the processing timed out.
	 * 
	 * @param  objectType The statistics object type
	 * @param  month      The month to be processed
	 * @return            True if the month was processed, false if the processing wasn't needed
	 */
	boolean processMonth(StatisticsObjectType objectType, YearMonth month);

}
