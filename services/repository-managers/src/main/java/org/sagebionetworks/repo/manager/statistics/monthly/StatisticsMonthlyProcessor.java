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
	int getMaxMonthsToProcess();

	/**
	 * @return The amount in ms for the processing to timeout that allows to restart another processing
	 */
	long getProcessingTimeout();

	/**
	 * Process the monthly statistics for the given month
	 * 
	 * @param  month The month to process
	 */
	void processMonth(YearMonth month);

}
