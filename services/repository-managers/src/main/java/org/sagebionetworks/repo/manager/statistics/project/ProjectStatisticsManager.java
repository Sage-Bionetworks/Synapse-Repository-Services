package org.sagebionetworks.repo.manager.statistics.project;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.statistics.ProjectStatistics;
import org.sagebionetworks.repo.web.NotFoundException;

public interface ProjectStatisticsManager {

	/**
	 * Returns the statistics for the project with the given id
	 * 
	 * @param user          The {@link UserInfo user} that is requesting the statistics
	 * @param projectId     The id of the project
	 * @param fileDownloads Specifies if the file downloads statistics should be included
	 * @param fileUploads   Specifies if the file uploads statistics should be included
	 * @return The statistics for the project with the given id
	 * 
	 * @throws IllegalArgumentException If any of the argument is null, or if the given projectId is malformed
	 * @throws NotFoundException        If the given projectId does not point to an existing project
	 * @throws UnauthorizedException    If the user does not have {@link ACCESS_TYPE#READ} or
	 *                                  {@link ACCESS_TYPE#VIEW_STATISTICS} access for the project with the given id
	 */
	ProjectStatistics getProjectStatistics(UserInfo user, String projectId, boolean fileDownloads, boolean fileUploads);
}
