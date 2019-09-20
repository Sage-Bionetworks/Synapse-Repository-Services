package org.sagebionetworks.repo.manager.statistics.project;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.statistics.StatisticsMonthlyProjectFilesDAO;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.statistics.FileEvent;
import org.sagebionetworks.repo.model.statistics.FilesCountStatistics;
import org.sagebionetworks.repo.model.statistics.MonthlyFilesStatistics;
import org.sagebionetworks.repo.model.statistics.ProjectStatistics;
import org.sagebionetworks.repo.model.statistics.StatisticsObjectType;
import org.sagebionetworks.repo.model.statistics.monthly.StatisticsMonthlyUtils;
import org.sagebionetworks.repo.model.statistics.project.StatisticsMonthlyProjectFiles;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProjectStatisticsManagerImpl implements ProjectStatisticsManager {

	private AuthorizationManager authManager;
	private NodeManager nodeManager;
	private StatisticsMonthlyProjectFilesDAO fileStatsDao;
	private int maxMonths;

	@Autowired
	public ProjectStatisticsManagerImpl(StackConfiguration stackConfig, AuthorizationManager authManager, NodeManager nodeManager, StatisticsMonthlyProjectFilesDAO fileStatsDao) {
		this.authManager = authManager;
		this.nodeManager = nodeManager;
		this.fileStatsDao = fileStatsDao;
		this.maxMonths = stackConfig.getMaximumMonthsForMonthlyStatistics();
	}

	@Override
	public ProjectStatistics getProjectStatistics(UserInfo user, String projectId, boolean fileDownloads, boolean fileUploads) {
		ValidateArgument.required(user, "The user");
		ValidateArgument.required(projectId, "The project id");

		Node project = nodeManager.get(user, projectId);

		if (!EntityType.project.equals(project.getNodeType())) {
			throw new NotFoundException("The id " + projectId + " does not refer to project");
		}

		// Verify access to the project
		authManager.canAccess(user, projectId, ObjectType.ENTITY, ACCESS_TYPE.VIEW_STATISTICS).checkAuthorizationOrElseThrow();

		Long parsedProjectId = KeyFactory.stringToKey(projectId);

		ProjectStatistics statistics = new ProjectStatistics();

		statistics.setObjectType(StatisticsObjectType.PROJECT);
		statistics.setObjectId(projectId);

		List<YearMonth> months = StatisticsMonthlyUtils.generatePastMonths(maxMonths);
		
		if (fileDownloads) {
			statistics.setFileDownloads(getProjectFilesStatistics(parsedProjectId, FileEvent.FILE_DOWNLOAD, months));
		}
		if (fileUploads) {
			statistics.setFileUploads(getProjectFilesStatistics(parsedProjectId, FileEvent.FILE_UPLOAD, months));
		}

		return statistics;
	}

	MonthlyFilesStatistics getProjectFilesStatistics(Long projectId, FileEvent eventType, List<YearMonth> months) {
		MonthlyFilesStatistics statistics = new MonthlyFilesStatistics();

		List<FilesCountStatistics> filesCountStats = new ArrayList<>(months.size());

		YearMonth fromMonth = months.get(0);
		YearMonth toMonth = months.get(months.size() - 1);

		Map<YearMonth, StatisticsMonthlyProjectFiles> statsMap = getMonthlyProjectFilesMap(projectId, eventType, fromMonth, toMonth);

		for (YearMonth month : months) {
			FilesCountStatistics monthStats = getFilesCountStatistics(month, statsMap.get(month));
			filesCountStats.add(monthStats);
		}

		Long lastUpdatedOn = getLastUpdatedOnMax(statsMap.values());

		statistics.setMonths(filesCountStats);
		statistics.setLastUpdatedOn(lastUpdatedOn);

		return statistics;
	}

	Long getLastUpdatedOnMax(Collection<StatisticsMonthlyProjectFiles> statistics) {
		Optional<Long> maxTimestamp = statistics.stream().map(StatisticsMonthlyProjectFiles::getLastUpdatedOn).max(Long::compare);
		return maxTimestamp.isPresent() ? maxTimestamp.get() : -1;

	}

	FilesCountStatistics getFilesCountStatistics(YearMonth month, StatisticsMonthlyProjectFiles statistics) {
		Pair<Long, Long> monthRange = StatisticsMonthlyUtils.getTimestampRange(month);

		FilesCountStatistics monthStats = new FilesCountStatistics();

		monthStats.setRangeStart(monthRange.getFirst());
		monthStats.setRangeEnd(monthRange.getSecond());
		monthStats.setFilesCount(0L);
		monthStats.setUsersCount(0L);

		if (statistics != null) {
			Integer filesCount = statistics.getFilesCount();
			if (filesCount != null) {
				monthStats.setFilesCount(filesCount.longValue());
			}
			Integer usersCount = statistics.getUsersCount();
			if (usersCount != null) {
				monthStats.setUsersCount(usersCount.longValue());
			}
		}

		return monthStats;
	}

	Map<YearMonth, StatisticsMonthlyProjectFiles> getMonthlyProjectFilesMap(Long projectId, FileEvent eventType, YearMonth from, YearMonth to) {
		return fileStatsDao
				.getProjectFilesStatisticsInRange(projectId, eventType, from, to)
				.stream()
				.collect(Collectors.toMap(StatisticsMonthlyProjectFiles::getMonth, Function.identity()));
	}
}
