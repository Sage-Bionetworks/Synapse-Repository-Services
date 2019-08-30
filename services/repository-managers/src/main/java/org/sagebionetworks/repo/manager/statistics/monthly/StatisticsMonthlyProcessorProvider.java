package org.sagebionetworks.repo.manager.statistics.monthly;

import org.sagebionetworks.repo.model.statistics.StatisticsObjectType;

public interface StatisticsMonthlyProcessorProvider {

	/**
	 * @param objectType The statistics object type
	 * @return The processor for the given type
	 */
	StatisticsMonthlyProcessor getMonthlyProcessor(StatisticsObjectType objectType);

}
