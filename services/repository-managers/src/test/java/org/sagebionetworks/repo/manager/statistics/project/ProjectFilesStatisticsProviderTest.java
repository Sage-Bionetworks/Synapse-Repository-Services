package org.sagebionetworks.repo.manager.statistics.project;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.dbo.statistics.StatisticsMonthlyProjectFilesDAO;
import org.sagebionetworks.repo.model.statistics.FileEvent;
import org.sagebionetworks.repo.model.statistics.FilesCountStatistics;
import org.sagebionetworks.repo.model.statistics.MonthlyFilesStatistics;
import org.sagebionetworks.repo.model.statistics.ProjectFilesStatisticsRequest;
import org.sagebionetworks.repo.model.statistics.ProjectFilesStatisticsResponse;
import org.sagebionetworks.repo.model.statistics.monthly.StatisticsMonthlyUtils;
import org.sagebionetworks.repo.model.statistics.project.StatisticsMonthlyProjectFiles;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.Pair;

import com.google.common.collect.ImmutableList;

@ExtendWith(MockitoExtension.class)
public class ProjectFilesStatisticsProviderTest {

	private static final int MAX_MONTHS = 12;

	@Mock
	private StackConfiguration mockStackConfig;

	@Mock
	private NodeDAO mockNodeDAO;

	@Mock
	private StatisticsMonthlyProjectFilesDAO mockFileStatsDao;

	@Mock
	private StatisticsMonthlyProjectFiles mockStatistics;

	@Mock
	private Node mockNode;

	private ProjectFilesStatisticsProvider provider;

	@BeforeEach
	public void before() {
		when(mockStackConfig.getMaximumMonthsForMonthlyStatistics()).thenReturn(MAX_MONTHS);
		provider = new ProjectFilesStatisticsProvider(mockStackConfig, mockNodeDAO, mockFileStatsDao);
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
		Map<YearMonth, StatisticsMonthlyProjectFiles> result = provider.getMonthlyProjectFilesMap(projectId, eventType, from, to);

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
		expected.setRangeStart(new Date(range.getFirst()));
		expected.setRangeEnd(new Date(range.getSecond()));

		// Call under test
		FilesCountStatistics result = provider.getFilesCountStatistics(month, mockStatistics);

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
		expected.setRangeStart(new Date(range.getFirst()));
		expected.setRangeEnd(new Date(range.getSecond()));

		// Call under test
		FilesCountStatistics result = provider.getFilesCountStatistics(month, null);

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
		expected.setRangeStart(new Date(range.getFirst()));
		expected.setRangeEnd(new Date(range.getSecond()));

		// Call under test
		FilesCountStatistics result = provider.getFilesCountStatistics(month, mockStatistics);

		assertEquals(expected, result);

	}

	@Test
	public void testGetLastUpdatedOnMax() {
		List<StatisticsMonthlyProjectFiles> stats = ImmutableList.of(mockStatistics, mockStatistics, mockStatistics);

		Date expected = new Date();

		when(mockStatistics.getLastUpdatedOn()).thenReturn(expected.getTime() - 2, expected.getTime() - 1, expected.getTime());

		// Call under test
		Date result = provider.getLastUpdatedOnMax(stats);

		assertEquals(expected, result);
	}

	@Test
	public void testGetLastUpdatedOnMaxWithEmptyList() {
		List<StatisticsMonthlyProjectFiles> stats = Collections.emptyList();

		Long expected = null;

		// Call under test
		Date result = provider.getLastUpdatedOnMax(stats);

		assertEquals(expected, result);
	}

	@Test
	public void testGetProjectFilesStatisticsWhenNoStats() {
		Long projectId = 123L;
		FileEvent eventType = FileEvent.FILE_DOWNLOAD;
		List<YearMonth> months = StatisticsMonthlyUtils.generatePastMonths(MAX_MONTHS);

		when(mockFileStatsDao.getProjectFilesStatisticsInRange(any(), any(), any(), any())).thenReturn(Collections.emptyList());

		MonthlyFilesStatistics expected = new MonthlyFilesStatistics();

		expected.setLastUpdatedOn(null);
		expected.setMonths(new ArrayList<>(MAX_MONTHS));

		months.forEach(month -> {
			FilesCountStatistics stats = new FilesCountStatistics();
			Pair<Long, Long> range = StatisticsMonthlyUtils.getTimestampRange(month);
			stats.setRangeStart(new Date(range.getFirst()));
			stats.setRangeEnd(new Date(range.getSecond()));
			stats.setFilesCount(0L);
			stats.setUsersCount(0L);
			expected.getMonths().add(stats);
		});

		// Call under test
		MonthlyFilesStatistics result = provider.getProjectFilesStatistics(projectId, eventType, months);

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
		expected.setLastUpdatedOn(new Date(statistics.get(statistics.size() - 1).getLastUpdatedOn()));

		when(mockFileStatsDao.getProjectFilesStatisticsInRange(any(), any(), any(), any())).thenReturn(statistics);

		// Call under test
		MonthlyFilesStatistics result = provider.getProjectFilesStatistics(projectId, eventType, months);

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
		expected.setLastUpdatedOn(new Date(statistics.get(statistics.size() - 1).getLastUpdatedOn()));

		for (int i = MAX_MONTHS / 2; i < MAX_MONTHS; i++) {
			YearMonth month = months.get(i);

			FilesCountStatistics mappedStats = new FilesCountStatistics();
			Pair<Long, Long> range = StatisticsMonthlyUtils.getTimestampRange(month);
			mappedStats.setRangeStart(new Date(range.getFirst()));
			mappedStats.setRangeEnd(new Date(range.getSecond()));
			mappedStats.setFilesCount(0L);
			mappedStats.setUsersCount(0L);

			expected.getMonths().add(mappedStats);
		}

		when(mockFileStatsDao.getProjectFilesStatisticsInRange(any(), any(), any(), any())).thenReturn(statistics);

		// Call under test
		MonthlyFilesStatistics result = provider.getProjectFilesStatistics(projectId, eventType, months);

		verify(mockFileStatsDao).getProjectFilesStatisticsInRange(projectId, eventType, months.get(0), months.get(months.size() - 1));

		assertEquals(expected, result);

	}

	@Test
	public void testGetProjectStatisticsWithInvalidInput() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			provider.getObjectStatistics(null);
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			String projectId = null;
			// Call under test
			provider.getObjectStatistics(getRequest(projectId, true, true));
		});
	}

	@Test
	public void testGetProjectStatisticsWithNonExistingProject() {
		String projectId = "123";

		when(mockNodeDAO.getNode(any())).thenThrow(NotFoundException.class);

		Assertions.assertThrows(NotFoundException.class, () -> {
			// Call under test
			provider.getObjectStatistics(getRequest(projectId, true, true));

		});

		verify(mockNodeDAO).getNode(projectId);
	}

	@Test
	public void testGetProjectStatisticsWithWrongProjectType() {
		String projectId = "123";

		when(mockNodeDAO.getNode(any())).thenReturn(mockNode);
		when(mockNode.getNodeType()).thenReturn(EntityType.file);

		Assertions.assertThrows(NotFoundException.class, () -> {
			// Call under test
			provider.getObjectStatistics(getRequest(projectId, true, true));
		});

		verify(mockNodeDAO).getNode(projectId);
		verify(mockNode).getNodeType();
	}

	@Test
	public void testGetProjectStatistics() {
		List<YearMonth> months = StatisticsMonthlyUtils.generatePastMonths(MAX_MONTHS);

		YearMonth from = months.get(0);
		YearMonth to = months.get(months.size() - 1);
		String projectId = "123";
		boolean fileDownloads = true;
		boolean fileUploads = true;
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

		when(mockNodeDAO.getNode(any())).thenReturn(mockNode);
		when(mockNode.getNodeType()).thenReturn(EntityType.project);
		when(mockFileStatsDao.getProjectFilesStatisticsInRange(any(), eq(FileEvent.FILE_DOWNLOAD), any(), any())).thenReturn(statistics);
		when(mockFileStatsDao.getProjectFilesStatisticsInRange(any(), eq(FileEvent.FILE_UPLOAD), any(), any())).thenReturn(statistics);

		ProjectFilesStatisticsResponse expected = new ProjectFilesStatisticsResponse();

		expected.setObjectId(projectId.toString());
		expected.setFileDownloads(map(statistics, lastUpdatedOn));
		expected.setFileUploads(map(statistics, lastUpdatedOn));

		// Call under test
		ProjectFilesStatisticsResponse result = provider.getObjectStatistics(getRequest(projectId, fileDownloads, fileUploads));

		verify(mockNodeDAO).getNode(projectId);
		verify(mockNode).getNodeType();
		verify(mockFileStatsDao).getProjectFilesStatisticsInRange(Long.valueOf(projectId), FileEvent.FILE_DOWNLOAD, from, to);
		verify(mockFileStatsDao).getProjectFilesStatisticsInRange(Long.valueOf(projectId), FileEvent.FILE_UPLOAD, from, to);

		assertEquals(expected, result);

	}

	@Test
	public void testGetProjectStatisticsWithoutDownloads() {
		List<YearMonth> months = StatisticsMonthlyUtils.generatePastMonths(MAX_MONTHS);

		YearMonth from = months.get(0);
		YearMonth to = months.get(months.size() - 1);

		List<StatisticsMonthlyProjectFiles> statistics = Collections.singletonList(mockStatistics);

		when(mockNodeDAO.getNode(any())).thenReturn(mockNode);
		when(mockNode.getNodeType()).thenReturn(EntityType.project);
		when(mockFileStatsDao.getProjectFilesStatisticsInRange(any(), eq(FileEvent.FILE_UPLOAD), any(), any())).thenReturn(statistics);

		String projectId = "123";
		boolean fileDownloads = false;
		boolean fileUploads = true;

		// Call under test
		ProjectFilesStatisticsResponse result = provider.getObjectStatistics(getRequest(projectId, fileDownloads, fileUploads));

		verify(mockNodeDAO).getNode(projectId);
		verify(mockNode).getNodeType();
		verify(mockFileStatsDao).getProjectFilesStatisticsInRange(Long.valueOf(projectId), FileEvent.FILE_UPLOAD, from, to);
		verifyNoMoreInteractions(mockFileStatsDao);

		assertNull(result.getFileDownloads());
		assertNotNull(result.getFileUploads());

	}

	@Test
	public void testGetProjectStatisticsWithoutUploads() {

		List<YearMonth> months = StatisticsMonthlyUtils.generatePastMonths(MAX_MONTHS);

		YearMonth from = months.get(0);
		YearMonth to = months.get(months.size() - 1);

		List<StatisticsMonthlyProjectFiles> statistics = Collections.singletonList(mockStatistics);

		when(mockNodeDAO.getNode(any())).thenReturn(mockNode);
		when(mockNode.getNodeType()).thenReturn(EntityType.project);
		when(mockFileStatsDao.getProjectFilesStatisticsInRange(any(), eq(FileEvent.FILE_DOWNLOAD), any(), any())).thenReturn(statistics);

		String projectId = "123";
		boolean fileDownloads = true;
		boolean fileUploads = false;

		// Call under test
		ProjectFilesStatisticsResponse result = provider.getObjectStatistics(getRequest(projectId, fileDownloads, fileUploads));

		verify(mockNodeDAO).getNode(projectId);
		verify(mockNode).getNodeType();
		verify(mockFileStatsDao).getProjectFilesStatisticsInRange(Long.valueOf(projectId), FileEvent.FILE_DOWNLOAD, from, to);
		verifyNoMoreInteractions(mockFileStatsDao);

		assertNotNull(result.getFileDownloads());
		assertNull(result.getFileUploads());

	}
	
	@Test
	public void testGetProejctStatisticsWithDefaults() {
		List<YearMonth> months = StatisticsMonthlyUtils.generatePastMonths(MAX_MONTHS);

		YearMonth from = months.get(0);
		YearMonth to = months.get(months.size() - 1);

		List<StatisticsMonthlyProjectFiles> statistics = Collections.singletonList(mockStatistics);

		when(mockNodeDAO.getNode(any())).thenReturn(mockNode);
		when(mockNode.getNodeType()).thenReturn(EntityType.project);
		when(mockFileStatsDao.getProjectFilesStatisticsInRange(any(), any(), any(), any())).thenReturn(statistics);

		String projectId = "123";
		
		// We do not specify what we want, should get back both
		Boolean fileDownloads = null;
		Boolean fileUploads = null;
		
		// Call under test
		ProjectFilesStatisticsResponse result = provider.getObjectStatistics(getRequest(projectId, fileDownloads, fileUploads));

		verify(mockNodeDAO).getNode(projectId);
		verify(mockNode).getNodeType();
		verify(mockFileStatsDao).getProjectFilesStatisticsInRange(Long.valueOf(projectId), FileEvent.FILE_DOWNLOAD, from, to);
		verify(mockFileStatsDao).getProjectFilesStatisticsInRange(Long.valueOf(projectId), FileEvent.FILE_UPLOAD, from, to);

		assertNotNull(result.getFileDownloads());
		assertNotNull(result.getFileUploads());
	}

	private ProjectFilesStatisticsRequest getRequest(String projectId, Boolean fileDownloads, Boolean fileUploads) {
		ProjectFilesStatisticsRequest request = new ProjectFilesStatisticsRequest();
		request.setObjectId(projectId);
		request.setFileDownloads(fileDownloads);
		request.setFileUploads(fileUploads);
		return request;
	}

	private MonthlyFilesStatistics map(List<StatisticsMonthlyProjectFiles> list, Long lastUpdatedOn) {
		MonthlyFilesStatistics stats = new MonthlyFilesStatistics();
		stats.setLastUpdatedOn(new Date(lastUpdatedOn));
		stats.setMonths(list.stream().map(this::map).collect(Collectors.toList()));
		return stats;
	}

	private FilesCountStatistics map(StatisticsMonthlyProjectFiles in) {
		FilesCountStatistics mappedStats = new FilesCountStatistics();
		Pair<Long, Long> range = StatisticsMonthlyUtils.getTimestampRange(in.getMonth());
		mappedStats.setRangeStart(new Date(range.getFirst()));
		mappedStats.setRangeEnd(new Date(range.getSecond()));
		mappedStats.setFilesCount(in.getFilesCount().longValue());
		mappedStats.setUsersCount(in.getUsersCount().longValue());
		return mappedStats;
	}

}
