package org.sagebionetworks.repo.manager.statistics.monthly;

import java.util.List;

import org.sagebionetworks.repo.model.statistics.StatisticsObjectType;

/**
 * Provider to retrieve the list of available {@link StatisticsMonthlyProcessor}s for a given
 * {@link StatisticsObjectType}.
 * 
 * @author Marco
 *
 */
public interface StatisticsMonthlyProcessorProvider {

	/**
	 * @param  objectType The statistics object type
	 * @return            The list of available processors for the given type
	 */
	List<StatisticsMonthlyProcessor> getProcessors(StatisticsObjectType objectType);

}
