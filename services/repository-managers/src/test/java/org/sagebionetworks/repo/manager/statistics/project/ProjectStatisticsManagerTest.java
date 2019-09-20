package org.sagebionetworks.repo.manager.statistics.project;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationStatus;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.statistics.StatisticsMonthlyProjectFilesDAO;
import org.sagebionetworks.repo.model.statistics.FileEvent;
import org.sagebionetworks.repo.model.statistics.FilesCountStatistics;
import org.sagebionetworks.repo.model.statistics.MonthlyFilesStatistics;
import org.sagebionetworks.repo.model.statistics.ProjectStatistics;
import org.sagebionetworks.repo.model.statistics.StatisticsObjectType;
import org.sagebionetworks.repo.model.statistics.monthly.StatisticsMonthlyUtils;
import org.sagebionetworks.repo.model.statistics.project.StatisticsMonthlyProjectFiles;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.Pair;

import com.google.common.collect.ImmutableList;

@ExtendWith(MockitoExtension.class)
public class ProjectStatisticsManagerTest {

	private static final int MAX_MONTHS = 12;

	@Mock
	private StackConfiguration mockStackConfig;

	@Mock
	private AuthorizationManager mockAuthManager;

	@Mock
	private NodeManager mockNodeManager;

	@Mock
	private StatisticsMonthlyProjectFilesDAO mockFileStatsDao;

	@Mock
	private StatisticsMonthlyProjectFiles mockStatistics;

	@Mock
	private Node mockNode;

	@Mock
	private AuthorizationStatus mockAuthStatus;

	private ProjectStatisticsManagerImpl manager;

	@BeforeEach
	public void before() {
		when(mockStackConfig.getMaximumMonthsForMonthlyStatistics()).thenReturn(MAX_MONTHS);
		manager = new ProjectStatisticsManagerImpl(mockStackConfig, mockAuthManager, mockNodeManager, mockFileStatsDao);
	}

	@Test
	public void testGetMonthlyProjectFilesMap() {
		Long projectId = 123L;
		FileEvent eventType = FileEvent.FILE_DOWNLOAD;
		YearMonth from = YearMonth.of(2019, 8);
		YearMonth to = YearMonth.of(2019, 9);
		YearMonth month = from;

		when(mockStatistics.getMonth()).thenReturn(month);
		when(mockFileStatsDao.getProjectFilesStatisticsInRange(any(), any(), any(), any()))
				.thenReturn(Collections.singletonList(mockStatistics));

		// Call under test
		Map<YearMonth, StatisticsMonthlyProjectFiles> result = manager.getMonthlyProjectFilesMap(projectId, eventType, from, to);

		verify(mockFileStatsDao).getProjectFilesStatisticsInRange(projectId, eventType, from, to);
		verify(mockStatistics).getMonth();

		assertFalse(result.isEmpty());
		assertEquals(mockStatistics, result.get(month));
	}

	@Test
	public void testGetFilesCountStatistics() {

		YearMonth month = YearMonth.of(2019, 8);
		Pair<Long, Long> range = StatisticsMonthlyUtils.getTimestampRange(month);
		Integer filesCount = 10;
		Integer usersCount = 1;

		when(mockStatistics.getFilesCount()).thenReturn(filesCount);
		when(mockStatistics.getUsersCount()).thenReturn(usersCount);

		FilesCountStatistics expected = new FilesCountStatistics();

		expected.setFilesCount(filesCount.longValue());
		expected.setUsersCount(usersCount.longValue());
		expected.setRangeStart(range.getFirst());
		expected.setRangeEnd(range.getSecond());

		// Call under test
		FilesCountStatistics result = manager.getFilesCountStatistics(month, mockStatistics);

		verify(mockStatistics).getFilesCount();
		verify(mockStatistics).getUsersCount();

		assertEquals(expected, result);

	}

	@Test
	public void testGetFilesCountStatisticsWhenNull() {

		YearMonth month = YearMonth.of(2019, 8);
		Pair<Long, Long> range = StatisticsMonthlyUtils.getTimestampRange(month);

		FilesCountStatistics expected = new FilesCountStatistics();

		expected.setFilesCount(0L);
		expected.setUsersCount(0L);
		expected.setRangeStart(range.getFirst());
		expected.setRangeEnd(range.getSecond());

		// Call under test
		FilesCountStatistics result = manager.getFilesCountStatistics(month, null);

		assertEquals(expected, result);

	}

	@Test
	public void testGetFilesCountStatisticsWhenCountsNull() {

		YearMonth month = YearMonth.of(2019, 8);
		Pair<Long, Long> range = StatisticsMonthlyUtils.getTimestampRange(month);
		Integer filesCount = null;
		Integer usersCount = null;

		when(mockStatistics.getFilesCount()).thenReturn(filesCount);
		when(mockStatistics.getUsersCount()).thenReturn(usersCount);

		FilesCountStatistics expected = new FilesCountStatistics();

		expected.setFilesCount(0L);
		expected.setUsersCount(0L);
		expected.setRangeStart(range.getFirst());
		expected.setRangeEnd(range.getSecond());

		// Call under test
		FilesCountStatistics result = manager.getFilesCountStatistics(month, mockStatistics);

		assertEquals(expected, result);

	}

	@Test
	public void testGetLastUpdatedOnMax() {
		List<StatisticsMonthlyProjectFiles> stats = ImmutableList.of(mockStatistics, mockStatistics, mockStatistics);

		Long expected = 3L;

		when(mockStatistics.getLastUpdatedOn()).thenReturn(1L, 2L, expected);

		// Call under test
		Long result = manager.getLastUpdatedOnMax(stats);

		assertEquals(expected, result);
	}

	@Test
	public void testGetLastUpdatedOnMaxWithEmptyList() {
		List<StatisticsMonthlyProjectFiles> stats = Collections.emptyList();

		Long expected = -1L;

		// Call under test
		Long result = manager.getLastUpdatedOnMax(stats);

		assertEquals(expected, result);
	}

	@Test
	public void testGetProjectFilesStatisticsWhenNoStats() {
		Long projectId = 123L;
		FileEvent eventType = FileEvent.FILE_DOWNLOAD;
		List<YearMonth> months = StatisticsMonthlyUtils.generatePastMonths(MAX_MONTHS);

		when(mockFileStatsDao.getProjectFilesStatisticsInRange(any(), any(), any(), any())).thenReturn(Collections.emptyList());

		MonthlyFilesStatistics expected = new MonthlyFilesStatistics();

		expected.setLastUpdatedOn(-1L);
		expected.setMonths(new ArrayList<>(MAX_MONTHS));

		months.forEach(month -> {
			FilesCountStatistics stats = new FilesCountStatistics();
			Pair<Long, Long> range = StatisticsMonthlyUtils.getTimestampRange(month);
			stats.setRangeStart(range.getFirst());
			stats.setRangeEnd(range.getSecond());
			stats.setFilesCount(0L);
			stats.setUsersCount(0L);
			expected.getMonths().add(stats);
		});

		// Call under test
		MonthlyFilesStatistics result = manager.getProjectFilesStatistics(projectId, eventType, months);

		verify(mockFileStatsDao).getProjectFilesStatisticsInRange(projectId, eventType, months.get(0), months.get(months.size() - 1));

		assertEquals(expected, result);

	}

	@Test
	public void testGetProjectFilesStatistics() {
		Long projectId = 123L;
		FileEvent eventType = FileEvent.FILE_DOWNLOAD;
		Integer filesCount = 10;
		Integer usersCount = 1;

		List<YearMonth> months = StatisticsMonthlyUtils.generatePastMonths(MAX_MONTHS);
		List<StatisticsMonthlyProjectFiles> statistics = new ArrayList<>();

		months.forEach(month -> {
			StatisticsMonthlyProjectFiles stats = new StatisticsMonthlyProjectFiles();

			stats.setEventType(eventType);
			stats.setProjectId(projectId);
			stats.setMonth(month);
			stats.setFilesCount(filesCount);
			stats.setUsersCount(usersCount);
			stats.setLastUpdatedOn(month.atEndOfMonth().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli());

			statistics.add(stats);
		});

		MonthlyFilesStatistics expected = new MonthlyFilesStatistics();

		expected.setMonths(statistics.stream().map(this::map).collect(Collectors.toList()));
		expected.setLastUpdatedOn(statistics.get(statistics.size() - 1).getLastUpdatedOn());

		when(mockFileStatsDao.getProjectFilesStatisticsInRange(any(), any(), any(), any())).thenReturn(statistics);

		// Call under test
		MonthlyFilesStatistics result = manager.getProjectFilesStatistics(projectId, eventType, months);

		verify(mockFileStatsDao).getProjectFilesStatisticsInRange(projectId, eventType, months.get(0), months.get(months.size() - 1));

		assertEquals(expected, result);

	}

	@Test
	public void testGetProjectFilesStatisticsPartial() {
		Long projectId = 123L;
		FileEvent eventType = FileEvent.FILE_DOWNLOAD;
		Integer filesCount = 10;
		Integer usersCount = 1;

		List<YearMonth> months = StatisticsMonthlyUtils.generatePastMonths(MAX_MONTHS);
		List<StatisticsMonthlyProjectFiles> statistics = new ArrayList<>();

		for (int i = 0; i < MAX_MONTHS / 2; i++) {
			YearMonth month = months.get(i);

			StatisticsMonthlyProjectFiles stats = new StatisticsMonthlyProjectFiles();

			stats.setEventType(eventType);
			stats.setProjectId(projectId);
			stats.setMonth(month);
			stats.setFilesCount(filesCount);
			stats.setUsersCount(usersCount);
			stats.setLastUpdatedOn(month.atEndOfMonth().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli());

			statistics.add(stats);
		}

		MonthlyFilesStatistics expected = new MonthlyFilesStatistics();

		expected.setMonths(new ArrayList<>(MAX_MONTHS));
		expected.getMonths().addAll(statistics.stream().map(this::map).collect(Collectors.toList()));
		expected.setLastUpdatedOn(statistics.get(statistics.size() - 1).getLastUpdatedOn());

		for (int i = MAX_MONTHS / 2; i < MAX_MONTHS; i++) {
			YearMonth month = months.get(i);

			FilesCountStatistics mappedStats = new FilesCountStatistics();
			Pair<Long, Long> range = StatisticsMonthlyUtils.getTimestampRange(month);
			mappedStats.setRangeStart(range.getFirst());
			mappedStats.setRangeEnd(range.getSecond());
			mappedStats.setFilesCount(0L);
			mappedStats.setUsersCount(0L);

			expected.getMonths().add(mappedStats);
		}

		when(mockFileStatsDao.getProjectFilesStatisticsInRange(any(), any(), any(), any())).thenReturn(statistics);

		// Call under test
		MonthlyFilesStatistics result = manager.getProjectFilesStatistics(projectId, eventType, months);

		verify(mockFileStatsDao).getProjectFilesStatisticsInRange(projectId, eventType, months.get(0), months.get(months.size() - 1));

		assertEquals(expected, result);

	}

	@Test
	public void testGetProjectStatisticsWithInvalidInput() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			UserInfo user = null;
			String projectId = "123";
			boolean fileDownloads = true;
			boolean fileUploads = true;
			// Call under test
			manager.getProjectStatistics(user, projectId, fileDownloads, fileUploads);
		});

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			UserInfo user = new UserInfo(true);
			String projectId = null;
			boolean fileDownloads = true;
			boolean fileUploads = true;
			// Call under test
			manager.getProjectStatistics(user, projectId, fileDownloads, fileUploads);
		});

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			when(mockNodeManager.get(any(), any())).thenThrow(IllegalArgumentException.class);
			UserInfo user = new UserInfo(true);
			String projectId = "wrong_id";
			boolean fileDownloads = true;
			boolean fileUploads = true;
			// Call under test
			manager.getProjectStatistics(user, projectId, fileDownloads, fileUploads);
		});

	}

	@Test
	public void testGetProjectStatisticsWithNonExistingProject() {
		UserInfo user = new UserInfo(true);
		String projectId = "123";
		boolean fileDownloads = true;
		boolean fileUploads = true;
		
		when(mockNodeManager.get(any(), any())).thenThrow(NotFoundException.class);
		
		Assertions.assertThrows(NotFoundException.class, () -> {
			// Call under test
			manager.getProjectStatistics(user, projectId, fileDownloads, fileUploads);

		});

		verify(mockNodeManager).get(user, projectId);
	}

	@Test
	public void testGetProjectStatisticsWithWrongProjectType() {
		UserInfo user = new UserInfo(true);
		String projectId = "123";
		boolean fileDownloads = true;
		boolean fileUploads = true;
		
		when(mockNodeManager.get(any(), any())).thenReturn(mockNode);
		when(mockNode.getNodeType()).thenReturn(EntityType.file);
		
		Assertions.assertThrows(NotFoundException.class, () -> {	
			// Call under test
			manager.getProjectStatistics(user, projectId, fileDownloads, fileUploads);
		});

		verify(mockNodeManager).get(user, projectId);
		verify(mockNode).getNodeType();
	}

	@Test
	public void testGetProjectStatisticsWithNoAccess() {
		Long userId = 123L;
		Long creator = 456L;
		UserInfo user = new UserInfo(false, userId);
		String projectId = "123";
		boolean fileDownloads = true;
		boolean fileUploads = true;
		
		when(mockNodeManager.get(any(), any())).thenReturn(mockNode);
		when(mockNode.getNodeType()).thenReturn(EntityType.project);
		when(mockNode.getCreatedByPrincipalId()).thenReturn(creator);
		when(mockAuthManager.isUserCreatorOrAdmin(any(), any())).thenReturn(false);
		when(mockAuthManager.canAccess(any(), any(), any(), any())).thenReturn(mockAuthStatus);
		doThrow(UnauthorizedException.class).when(mockAuthStatus).checkAuthorizationOrElseThrow();

		Assertions.assertThrows(UnauthorizedException.class, () -> {
			// Call under test
			manager.getProjectStatistics(user, projectId, fileDownloads, fileUploads);

		});

		verify(mockAuthManager).isUserCreatorOrAdmin(user, creator.toString());
		verify(mockAuthManager).canAccess(user, projectId, ObjectType.ENTITY, ACCESS_TYPE.VIEW_STATISTICS);
	}

	@Test
	public void testGetProjectStatisticsWithAsAdmin() {
		Long userId = 123L;
		Long creator = 456L;
		boolean isAdmin = true;
		
		UserInfo user = new UserInfo(isAdmin, userId);
		String projectId = "123";
		boolean fileDownloads = true;
		boolean fileUploads = true;

		when(mockNodeManager.get(any(), any())).thenReturn(mockNode);
		when(mockNode.getNodeType()).thenReturn(EntityType.project);
		when(mockNode.getCreatedByPrincipalId()).thenReturn(creator);
		when(mockAuthManager.isUserCreatorOrAdmin(any(), any())).thenReturn(isAdmin);

		// Call under test
		manager.getProjectStatistics(user, projectId, fileDownloads, fileUploads);

		verify(mockAuthManager).isUserCreatorOrAdmin(user, creator.toString());
		verifyNoMoreInteractions(mockAuthManager);
	}
	
	@Test
	public void testGetProjectStatisticsWithAsCreator() {
		Long userId = 123L;
		Long creator = userId;
		boolean isAdmin = false;
		UserInfo user = new UserInfo(isAdmin, userId);
		String projectId = "123";
		boolean fileDownloads = true;
		boolean fileUploads = true;

		when(mockNodeManager.get(any(), any())).thenReturn(mockNode);
		when(mockNode.getNodeType()).thenReturn(EntityType.project);
		when(mockNode.getCreatedByPrincipalId()).thenReturn(creator);
		when(mockAuthManager.isUserCreatorOrAdmin(any(), any())).thenReturn(true);

		// Call under test
		manager.getProjectStatistics(user, projectId, fileDownloads, fileUploads);

		verify(mockAuthManager).isUserCreatorOrAdmin(user, creator.toString());
		verifyNoMoreInteractions(mockAuthManager);
	}

	@Test
	public void testGetProjectStatistics() {
		when(mockNodeManager.get(any(), any())).thenReturn(mockNode);
		when(mockNode.getNodeType()).thenReturn(EntityType.project);
		when(mockAuthManager.isUserCreatorOrAdmin(any(), any())).thenReturn(true);

		List<YearMonth> months = StatisticsMonthlyUtils.generatePastMonths(MAX_MONTHS);

		YearMonth from = months.get(0);
		YearMonth to = months.get(months.size() - 1);
		String projectId = "123";
		Integer filesCount = 100;
		Integer usersCount = 2;

		List<StatisticsMonthlyProjectFiles> statistics = new ArrayList<>();

		months.forEach(month -> {
			StatisticsMonthlyProjectFiles stats = new StatisticsMonthlyProjectFiles();
			stats.setMonth(month);
			stats.setFilesCount(filesCount);
			stats.setUsersCount(usersCount);
			stats.setLastUpdatedOn(month.atEndOfMonth().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli());

			statistics.add(stats);
		});

		Long lastUpdatedOn = statistics.get(statistics.size() - 1).getLastUpdatedOn();

		when(mockFileStatsDao.getProjectFilesStatisticsInRange(any(), eq(FileEvent.FILE_DOWNLOAD), any(), any())).thenReturn(statistics);
		when(mockFileStatsDao.getProjectFilesStatisticsInRange(any(), eq(FileEvent.FILE_UPLOAD), any(), any())).thenReturn(statistics);

		UserInfo user = new UserInfo(true);
		boolean fileDownloads = true;
		boolean fileUploads = true;

		ProjectStatistics expected = new ProjectStatistics();

		expected.setObjectType(StatisticsObjectType.PROJECT);
		expected.setObjectId(projectId.toString());
		expected.setFileDownloads(map(statistics, lastUpdatedOn));
		expected.setFileUploads(map(statistics, lastUpdatedOn));

		// Call under test
		ProjectStatistics result = manager.getProjectStatistics(user, projectId, fileDownloads, fileUploads);

		verify(mockFileStatsDao).getProjectFilesStatisticsInRange(Long.valueOf(projectId), FileEvent.FILE_DOWNLOAD, from, to);
		verify(mockFileStatsDao).getProjectFilesStatisticsInRange(Long.valueOf(projectId), FileEvent.FILE_UPLOAD, from, to);

		assertEquals(expected, result);

	}

	@Test
	public void testGetProjectStatisticsWithoutDownloads() {
		when(mockNodeManager.get(any(), any())).thenReturn(mockNode);
		when(mockNode.getNodeType()).thenReturn(EntityType.project);
		when(mockAuthManager.canAccess(any(), any(), any(), any())).thenReturn(mockAuthStatus);
		doNothing().when(mockAuthStatus).checkAuthorizationOrElseThrow();

		List<YearMonth> months = StatisticsMonthlyUtils.generatePastMonths(MAX_MONTHS);

		YearMonth from = months.get(0);
		YearMonth to = months.get(months.size() - 1);

		List<StatisticsMonthlyProjectFiles> statistics = Collections.singletonList(mockStatistics);

		when(mockFileStatsDao.getProjectFilesStatisticsInRange(any(), eq(FileEvent.FILE_UPLOAD), any(), any())).thenReturn(statistics);

		String projectId = "123";
		UserInfo user = new UserInfo(true);
		boolean fileDownloads = false;
		boolean fileUploads = true;

		// Call under test
		ProjectStatistics result = manager.getProjectStatistics(user, projectId, fileDownloads, fileUploads);

		verify(mockFileStatsDao).getProjectFilesStatisticsInRange(Long.valueOf(projectId), FileEvent.FILE_UPLOAD, from, to);
		verifyNoMoreInteractions(mockFileStatsDao);

		assertNull(result.getFileDownloads());
		assertNotNull(result.getFileUploads());

	}

	@Test
	public void testGetProjectStatisticsWithoutUploads() {
		when(mockNodeManager.get(any(), any())).thenReturn(mockNode);
		when(mockNode.getNodeType()).thenReturn(EntityType.project);
		when(mockAuthManager.canAccess(any(), any(), any(), any())).thenReturn(mockAuthStatus);
		doNothing().when(mockAuthStatus).checkAuthorizationOrElseThrow();

		List<YearMonth> months = StatisticsMonthlyUtils.generatePastMonths(MAX_MONTHS);

		YearMonth from = months.get(0);
		YearMonth to = months.get(months.size() - 1);

		List<StatisticsMonthlyProjectFiles> statistics = Collections.singletonList(mockStatistics);

		when(mockFileStatsDao.getProjectFilesStatisticsInRange(any(), eq(FileEvent.FILE_DOWNLOAD), any(), any())).thenReturn(statistics);

		String projectId = "123";
		UserInfo user = new UserInfo(true);
		boolean fileDownloads = true;
		boolean fileUploads = false;

		// Call under test
		ProjectStatistics result = manager.getProjectStatistics(user, projectId, fileDownloads, fileUploads);

		verify(mockFileStatsDao).getProjectFilesStatisticsInRange(Long.valueOf(projectId), FileEvent.FILE_DOWNLOAD, from, to);
		verifyNoMoreInteractions(mockFileStatsDao);

		assertNotNull(result.getFileDownloads());
		assertNull(result.getFileUploads());

	}

	private MonthlyFilesStatistics map(List<StatisticsMonthlyProjectFiles> list, Long lastUpdatedOn) {
		MonthlyFilesStatistics stats = new MonthlyFilesStatistics();
		stats.setLastUpdatedOn(lastUpdatedOn);
		stats.setMonths(list.stream().map(this::map).collect(Collectors.toList()));
		return stats;
	}

	private FilesCountStatistics map(StatisticsMonthlyProjectFiles in) {
		FilesCountStatistics mappedStats = new FilesCountStatistics();
		Pair<Long, Long> range = StatisticsMonthlyUtils.getTimestampRange(in.getMonth());
		mappedStats.setRangeStart(range.getFirst());
		mappedStats.setRangeEnd(range.getSecond());
		mappedStats.setFilesCount(in.getFilesCount().longValue());
		mappedStats.setUsersCount(in.getUsersCount().longValue());
		return mappedStats;
	}

}
