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
	 * Process the statistics for the given object type and month, if any error occurs during the processing sets the status
	 * to {@link StatisticsStatus#PROCESSING_FAILED}, otherwise set it to {@link StatisticsStatus#AVAILABLE} at the end of
	 * the processing
	 * 
	 * @param objectType The statistics object type
	 * @param month      The month to be processed
	 * @return True if the processing finished successfully, false otherwise
	 */
	boolean processMonth(StatisticsObjectType objectType, YearMonth month);

}
