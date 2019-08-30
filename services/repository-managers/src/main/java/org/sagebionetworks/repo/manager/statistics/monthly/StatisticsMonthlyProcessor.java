package org.sagebionetworks.repo.manager.statistics.monthly;

import java.time.YearMonth;

import org.sagebionetworks.repo.model.statistics.StatisticsObjectType;

public interface StatisticsMonthlyProcessor {

	/**
	 * @return The type supported by this processor
	 */
	StatisticsObjectType getSupportedType();

	/**
	 * @return The maximum number of past months to process
	 */
	int maxMonthsToProcess();
	
	/**
	 * Process the monthly statistics for the given month
	 * 
	 * @param month The month to process
	 * @return True if the processing was started, false if not needed
	 */
	boolean processMonth(YearMonth month);

}
