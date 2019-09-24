package org.sagebionetworks.repo.manager.statistics;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.statistics.ObjectStatisticsRequest;
import org.sagebionetworks.repo.model.statistics.ObjectStatisticsResponse;

/**
 * Provider interface to retrieve statistics according to {@link ObjectStatisticsRequest}.
 * 
 * @author maras
 *
 */
public interface StatisticsProvider<T extends ObjectStatisticsRequest> {

	/**
	 * @return The type of {@link ObjectStatisticsRequest} supported by the provider
	 */
	Class<T> getSupportedType();

	/**
	 * Returns the statistics relative to the given request
	 * 
	 * @param user    The user requesting the statistics
	 * @param request The request body
	 * @return The statistics according to the given request
	 */
	ObjectStatisticsResponse getObjectStatistics(UserInfo user, T request);

}
