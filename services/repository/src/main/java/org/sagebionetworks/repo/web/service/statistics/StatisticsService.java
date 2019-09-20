package org.sagebionetworks.repo.web.service.statistics;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.statistics.ProjectStatistics;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Service that groups the statistics operations exposed to the clients
 * 
 * @author Marco
 *
 */
public interface StatisticsService {

	/**
	 * Returns the statistics for the project with the given id
	 * 
	 * @param userId        The id of the user requesting the statistics
	 * @param projectId     The id of the project
	 * @param fileDownloads True if the file downloads statistics should be included
	 * @param fileUploads   True if the file uploads statistcis should be included
	 * @return The statistics for the given project
	 * 
	 * @throws IllegalArgumentException If any of the argument is null, or if the given projectId is malformed
	 * @throws NotFoundException        If the given projectId does not point to an existing project
	 * @throws UnauthorizedException    If the user does not have {@link ACCESS_TYPE#READ} or
	 *                                  {@link ACCESS_TYPE#VIEW_STATISTICS} access for the project with the given id
	 */
	ProjectStatistics getProjectStatistics(Long userId, String projectId, boolean fileDownloads, boolean fileUploads);

}
