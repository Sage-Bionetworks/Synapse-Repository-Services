package org.sagebionetworks.repo.manager.statistics.project;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.statistics.StatisticsMonthlyProjectFilesDAO;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.statistics.FileEvent;
import org.sagebionetworks.repo.model.statistics.MonthlyFilesStatistics;
import org.sagebionetworks.repo.model.statistics.ProjectFilesStatisticsRequest;
import org.sagebionetworks.repo.model.statistics.ProjectFilesStatisticsResponse;
import org.sagebionetworks.repo.model.statistics.monthly.StatisticsMonthlyUtils;
import org.sagebionetworks.repo.model.statistics.project.StatisticsMonthlyProjectFiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class ProjectStatisticsManagerAutowireTest {

	@Autowired
	private StackConfiguration stackConfig;

	@Autowired
	private StatisticsMonthlyProjectFilesDAO dao;

	@Autowired
	private ProjectStatisticsManager manager;

	@Autowired
	private UserManager userManager;

	@Autowired
	private EntityManager entityManager;

	private UserInfo userInfo;
	private String projectId;
	private Integer filesCount = 100;
	private Integer usersCount = 10;

	@BeforeEach
	public void before() {
		dao.clear();
		NewUser user = new NewUser();
		String username = UUID.randomUUID().toString();
		user.setEmail(username + "@test.com");
		user.setUserName(username);
		userInfo = userManager.getUserInfo(userManager.createUser(user));

		Project project = new Project();
		project.setName("Project" + RandomStringUtils.randomAlphanumeric(10));
		projectId = entityManager.createEntity(userInfo, project, null);
	}

	@AfterEach
	public void after() {
		dao.clear();
		UserInfo adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		entityManager.deleteEntity(adminUserInfo, projectId);
		userManager.deletePrincipal(adminUserInfo, userInfo.getId());
	}

	@Test
	public void testGetProjectStatistics() {

		// Creates stats for half the time
		int maxMonths = stackConfig.getMaximumMonthsForMonthlyStatistics() / 2;

		createStats(KeyFactory.stringToKey(projectId), FileEvent.FILE_DOWNLOAD, filesCount, usersCount, maxMonths);
		createStats(KeyFactory.stringToKey(projectId), FileEvent.FILE_UPLOAD, filesCount, usersCount, maxMonths);

		boolean includeDownloads = true;
		boolean includeUploads = true;
		
		ProjectFilesStatisticsRequest request = new ProjectFilesStatisticsRequest();
		request.setObjectId(projectId);
		request.setFileDownloads(includeDownloads);
		request.setFileUploads(includeUploads);
		
		// Call under test
		ProjectFilesStatisticsResponse result = manager.getProjectFilesStatistics(userInfo, request);
		
		assertEquals(projectId, result.getObjectId());
		
		MonthlyFilesStatistics fileDownloads = result.getFileDownloads();
		MonthlyFilesStatistics fileUploads = result.getFileUploads();
		
		verifyMonthlyFilesStats(fileUploads, maxMonths);
		verifyMonthlyFilesStats(fileDownloads, maxMonths);
		
	}
	
	private void verifyMonthlyFilesStats(MonthlyFilesStatistics stats, int maxMonths) {
		assertNotNull(stats);
		
		int monthsDelta = stackConfig.getMaximumMonthsForMonthlyStatistics() - maxMonths;
		// Verify the 12 months are present
		assertEquals(stackConfig.getMaximumMonthsForMonthlyStatistics(), stats.getMonths().size());
		// Verify that the last updated on timestamp is set
		assertNotNull(stats.getLastUpdatedOn());
		// Verify that for some (the delta) months the count will be 0
		assertEquals(monthsDelta, stats.getMonths().stream().filter( count -> count.getFilesCount().equals(0L)).count());
		assertEquals(monthsDelta, stats.getMonths().stream().filter( count -> count.getUsersCount().equals(0L)).count());
		assertEquals(maxMonths, stats.getMonths().stream().filter( count -> count.getFilesCount().equals(Long.valueOf(filesCount))).count());
		assertEquals(maxMonths, stats.getMonths().stream().filter( count -> count.getUsersCount().equals(Long.valueOf(usersCount))).count());
	}

	private void createStats(Long projectId, FileEvent eventType, Integer filesCount, Integer usersCount, int maxMonths) {
		List<YearMonth> months = StatisticsMonthlyUtils.generatePastMonths(maxMonths);
		List<StatisticsMonthlyProjectFiles> statistics = new ArrayList<>(months.size());

		months.forEach(month -> {
			StatisticsMonthlyProjectFiles monthStats = new StatisticsMonthlyProjectFiles();
			monthStats.setMonth(month);
			monthStats.setEventType(eventType);
			monthStats.setProjectId(projectId);
			monthStats.setFilesCount(filesCount);
			monthStats.setUsersCount(usersCount);
			statistics.add(monthStats);
		});

		dao.save(statistics);
	}

}
