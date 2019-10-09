package org.sagebionetworks.repo.web.service.statistics;

import org.sagebionetworks.repo.model.statistics.ObjectStatisticsRequest;
import org.sagebionetworks.repo.model.statistics.ObjectStatisticsResponse;

/**
 * Service that groups the statistics operations exposed to the clients
 * 
 * @author Marco
 *
 */
public interface StatisticsService {

	/**
	 * Returns the statistics according to the given request, the user must have {@link ACCESS_TYPE#READ} access
	 * on the object referenced in the request
	 * 
	 * @param <T>     The type of statistics request
	 * @param userId  The id of the user performing the request
	 * @param request The request body
	 * @return The response containing the statistics
	 */
	<T extends ObjectStatisticsRequest> ObjectStatisticsResponse getStatistics(Long userId, T request);

}
