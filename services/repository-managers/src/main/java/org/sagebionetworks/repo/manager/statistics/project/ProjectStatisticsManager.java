package org.sagebionetworks.repo.manager.statistics.project;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.statistics.ProjectFilesStatisticsRequest;
import org.sagebionetworks.repo.model.statistics.ProjectFilesStatisticsResponse;
import org.sagebionetworks.repo.web.NotFoundException;

public interface ProjectStatisticsManager {

	/**
	 * Returns the project files statistics for the project with the given id
	 * 
	 * @param user    The {@link UserInfo user} that is requesting the statistics
	 * @param request The body of the statistics request
	 * @return The file statistics for the project with the given id
	 * 
	 * @throws IllegalArgumentException If any of the argument is null
	 * @throws NotFoundException        If the given projectId does not point to an existing project
	 * @throws UnauthorizedException    If the user does not have {@link ACCESS_TYPE#VIEW_STATISTICS}
	 *                                  access on the project with the given id
	 */
	ProjectFilesStatisticsResponse getProjectFilesStatistics(UserInfo user, ProjectFilesStatisticsRequest request);
}
