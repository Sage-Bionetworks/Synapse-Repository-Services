package org.sagebionetworks.repo.manager.statistics.monthly;

import java.time.YearMonth;

import org.sagebionetworks.repo.manager.statistics.StatisticsProcessingException;
import org.sagebionetworks.repo.model.statistics.StatisticsObjectType;

public interface StatisticsMonthlyProcessor {

	/**
	 * @return The type supported by this processor
	 */
	StatisticsObjectType getSupportedType();

	/**
	 * Process the monthly statistics for the given month
	 * 
	 * @param  month                         The month to process
	 * @throws StatisticsProcessingException If something goes wrong during the processing
	 */
	void processMonth(YearMonth month) throws StatisticsProcessingException;

}
