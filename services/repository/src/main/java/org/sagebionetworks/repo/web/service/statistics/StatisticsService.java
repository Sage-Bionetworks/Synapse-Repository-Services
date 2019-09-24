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

	<T extends ObjectStatisticsRequest> ObjectStatisticsResponse getStatistics(Long userId, T request);

}
