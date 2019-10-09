package org.sagebionetworks.repo.manager.statistics;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.statistics.ObjectStatisticsRequest;
import org.sagebionetworks.repo.model.statistics.ObjectStatisticsResponse;

/**
 * Manager layer to retrieve the backend statistics
 * 
 * @author Marco
 */
public interface StatisticsManager {

	/**
	 * Returns the statistics according to the given request, the user should have
	 * {@link ACCESS_TYPE#READ} access on the {@link ObjectStatisticsRequest#getObjectId()
	 * objectId} referenced by the request.
	 * 
	 * @param <T>     The request type
	 * @param user    The user asking for the statistics, should not be anonymous and should have
	 *                {@link ACCESS_TYPE#READ} access on the object referred in the request
	 * @param request The request body
	 * @return THe {@link ObjectStatisticsResponse} containing the statistics
	 */
	<T extends ObjectStatisticsRequest> ObjectStatisticsResponse getStatistics(UserInfo user, T request);

}
