package org.sagebionetworks.repo.manager.statistics;

import org.sagebionetworks.repo.model.statistics.ObjectStatisticsRequest;
import org.sagebionetworks.repo.model.statistics.ObjectStatisticsResponse;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Provider interface to retrieve statistics according to {@link ObjectStatisticsRequest}.
 * 
 * @author maras
 *
 */
public interface StatisticsProvider<S extends ObjectStatisticsRequest, T extends ObjectStatisticsResponse> {

	/**
	 * @return The type of {@link ObjectStatisticsRequest} supported by the provider
	 */
	Class<S> getSupportedType();

	/**
	 * Returns the statistics relative to the given request.
	 * 
	 * @param request The request body
	 * @return The statistics according to the given request
	 * 
	 * @throws NotFoundException If the object with the given id does not exist
	 */
	T getObjectStatistics(S request);

}
