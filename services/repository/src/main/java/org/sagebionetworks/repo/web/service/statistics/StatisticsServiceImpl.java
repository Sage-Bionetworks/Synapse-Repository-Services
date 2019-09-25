package org.sagebionetworks.repo.web.service.statistics;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.statistics.StatisticsManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.statistics.ObjectStatisticsRequest;
import org.sagebionetworks.repo.model.statistics.ObjectStatisticsResponse;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StatisticsServiceImpl implements StatisticsService {

	private UserManager userManager;
	private StatisticsManager statisticsManager;

	@Autowired
	public StatisticsServiceImpl(UserManager userManager, StatisticsManager statisticsManager) {
		this.userManager = userManager;
		this.statisticsManager = statisticsManager;
	}

	@Override
	public <T extends ObjectStatisticsRequest> ObjectStatisticsResponse getStatistics(Long userId, T request) {
		ValidateArgument.required(userId, "The id of the user");
		ValidateArgument.required(request, "The request body");
		ValidateArgument.required(request.getObjectId(), "The object id");

		UserInfo userInfo = userManager.getUserInfo(userId);

		return statisticsManager.getStatistics(userInfo, request);
	}


}
