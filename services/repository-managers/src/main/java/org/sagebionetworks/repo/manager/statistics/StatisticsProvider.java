package org.sagebionetworks.repo.manager.statistics;

import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
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
	 * Verifies that the user has access for the object with the given id. The provider
	 * should implement this method according to the supported object type. If an object with the given id of the expected
	 * type does not exists a {@link NotFoundException} should be thrown
	 * 
	 * @param user     The user asking for the statistics, never the anonymous user
	 * @param objectId The id of the object
	 * @throws UnauthorizedException If the user does not have access to the object with
	 *                               the given id
	 * @throws NotFoundException     If the object with the given id does not exist
	 */
	void verifyViewStatisticsAccess(UserInfo user, String objectId) throws UnauthorizedException, NotFoundException;

	/**
	 * Returns the statistics relative to the given request, the {@link #verifyViewStatisticsAccess(UserInfo, String)} is
	 * invoked before this method
	 * 
	 * @param request The request body
	 * @return The statistics according to the given request
	 */
	T getObjectStatistics(S request);

}
