package org.sagebionetworks.repo.web.service.statistics;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.statistics.project.ProjectStatisticsManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.statistics.ProjectStatistics;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StatisticsServiceImpl implements StatisticsService {

	private ProjectStatisticsManager projectStatisticsManager;
	private UserManager userManager;

	@Autowired
	public StatisticsServiceImpl(ProjectStatisticsManager projectStatisticsManager, UserManager userManager) {
		this.projectStatisticsManager = projectStatisticsManager;
		this.userManager = userManager;
	}

	@Override
	public ProjectStatistics getProjectStatistics(Long userId, String projectId, boolean fileDownloads, boolean fileUploads) {
		ValidateArgument.required(userId, "The current user id");
		ValidateArgument.required(projectId, "The project id");

		UserInfo user = userManager.getUserInfo(userId);

		return projectStatisticsManager.getProjectStatistics(user, projectId, fileDownloads, fileUploads);
	}

}
