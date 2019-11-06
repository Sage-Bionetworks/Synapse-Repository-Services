package org.sagebionetworks.repo.model.dbo.statistics;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.dbo.statistics.StatisticsMonthlyProjectFilesDAO;
import org.sagebionetworks.repo.model.statistics.FileEvent;
import org.sagebionetworks.repo.model.statistics.monthly.StatisticsMonthlyUtils;
import org.sagebionetworks.repo.model.statistics.project.StatisticsMonthlyProjectFiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class StatisticsMonthlyProjectFilesDAOTest {

	private static final Integer TEST_FILES_COUNT = 10000;
	private static final Integer TEST_USERS_COUNT = 200;
	@Autowired
	private StatisticsMonthlyProjectFilesDAO dao;

	@BeforeEach
	public void before() {
		dao.clear();
	}

	@AfterEach
	public void after() {
		dao.clear();
	}

	@Test
	public void testGetMonthlyProjectFileStatisticsAbsent() {
		Long projectId = 1L;
		YearMonth month = YearMonth.of(2019, 8);

		// Call under test
		Optional<StatisticsMonthlyProjectFiles> result = dao.getProjectFilesStatistics(projectId, FileEvent.FILE_DOWNLOAD, month);

		assertFalse(result.isPresent());
	}

	@Test
	public void testGetMonthlyProjectFilesStatistics() {
		Long projectId = 1L;
		YearMonth month = YearMonth.of(2019, 8);
		FileEvent eventType = FileEvent.FILE_DOWNLOAD;

		StatisticsMonthlyProjectFiles stats = new StatisticsMonthlyProjectFiles();

		stats.setProjectId(projectId);
		stats.setMonth(month);
		stats.setEventType(eventType);
		stats.setFilesCount(TEST_FILES_COUNT);
		stats.setUsersCount(TEST_USERS_COUNT);

		dao.save(Collections.singletonList(stats));

		// Call under test
		Optional<StatisticsMonthlyProjectFiles> result = dao.getProjectFilesStatistics(projectId, FileEvent.FILE_DOWNLOAD, month);

		assertTrue(result.isPresent());

		StatisticsMonthlyProjectFiles dto = result.get();

		assertEquals(projectId, dto.getProjectId());
		assertEquals(month, dto.getMonth());
		assertEquals(eventType, dto.getEventType());
		assertEquals(TEST_FILES_COUNT, dto.getFilesCount());
		assertEquals(TEST_USERS_COUNT, dto.getUsersCount());
		assertNotNull(dto.getLastUpdatedOn());

	}

	@Test
	public void testGetMonthlyProjectFilesStatisticsInRangeWithInvalidRange() {
		Long projectId = 1L;
		FileEvent eventType = FileEvent.FILE_DOWNLOAD;
		YearMonth from = YearMonth.of(2019, 9);
		YearMonth to = YearMonth.of(2019, 8);

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			dao.getProjectFilesStatisticsInRange(projectId, eventType, from, to);
		});
	}

	@Test
	public void testGetMonthlyProjectFilesStatisticsInRange() {

		Long projectId = 1L;
		FileEvent eventType = FileEvent.FILE_DOWNLOAD;

		List<YearMonth> months = StatisticsMonthlyUtils.generatePastMonths(5);

		List<StatisticsMonthlyProjectFiles> batch = new ArrayList<>();

		for (YearMonth month : months) {
			StatisticsMonthlyProjectFiles dto = new StatisticsMonthlyProjectFiles();
			dto.setProjectId(projectId);
			dto.setEventType(eventType);
			dto.setMonth(month);
			dto.setFilesCount(TEST_FILES_COUNT);
			dto.setUsersCount(TEST_USERS_COUNT);
			batch.add(dto);
		}

		dao.save(batch);

		YearMonth from = months.get(0);
		YearMonth to = months.get(months.size() - 1);

		// Call under test
		List<StatisticsMonthlyProjectFiles> result = dao.getProjectFilesStatisticsInRange(projectId, eventType, from, to);
		
		assertEquals(batch.size(), result.size());
		
		for (int i=0; i<batch.size(); i++) {
			StatisticsMonthlyProjectFiles expected = batch.get(i);
			StatisticsMonthlyProjectFiles actual = result.get(i);
			assertNotNull(actual.getLastUpdatedOn());
			expected.setLastUpdatedOn(actual.getLastUpdatedOn());
			assertEquals(expected, actual);
		}
	}

	@Test
	public void testSaveBatch() {
		int projectsNumber = 200;

		YearMonth month = YearMonth.of(2019, 8);
		FileEvent eventType = FileEvent.FILE_DOWNLOAD;

		List<StatisticsMonthlyProjectFiles> batch = getBatch(eventType, month, projectsNumber);

		// Call under test
		dao.save(batch);

		Long count = dao.countProjectsInRange(eventType, month, month);

		assertEquals(projectsNumber, count);

		for (int projectId = 0; projectId < projectsNumber; projectId++) {
			assertCounts(Long.valueOf(projectId), eventType, month, TEST_FILES_COUNT, TEST_USERS_COUNT);
		}
	}

	@Test
	public void testSaveBatchOverrideAll() {
		int projectsNumber = 10;

		YearMonth month = YearMonth.of(2019, 8);
		FileEvent eventType = FileEvent.FILE_DOWNLOAD;

		List<StatisticsMonthlyProjectFiles> batch = getBatch(eventType, month, projectsNumber);

		// First saves a batch
		dao.save(batch);

		int newFilesCount = TEST_FILES_COUNT - 5;
		int newUsersCount = TEST_USERS_COUNT - 5;

		batch = getBatch(eventType, month, projectsNumber, newFilesCount, newUsersCount);

		// Call under test
		dao.save(batch);

		Long count = dao.countProjectsInRange(eventType, month, month);

		assertEquals(projectsNumber, count);

		for (int projectId = 0; projectId < projectsNumber; projectId++) {
			assertCounts(Long.valueOf(projectId), eventType, month, newFilesCount, newUsersCount);
		}

	}

	@Test
	public void testSaveBatchOverridePartial() {
		int projectsNumber = 10;

		YearMonth month = YearMonth.of(2019, 8);
		FileEvent eventType = FileEvent.FILE_DOWNLOAD;

		List<StatisticsMonthlyProjectFiles> batch = getBatch(eventType, month, projectsNumber);

		// First saves a batch
		dao.save(batch);

		int overrideProjectsNumber = 5;
		int newFilesCount = TEST_FILES_COUNT - 5;
		int newUsersCount = TEST_USERS_COUNT - 5;

		batch = getBatch(eventType, month, overrideProjectsNumber, newFilesCount, newUsersCount);

		// Call under test
		dao.save(batch);

		Long count = dao.countProjectsInRange(eventType, month, month);

		assertEquals(projectsNumber, count);

		for (int projectId = 0; projectId < overrideProjectsNumber; projectId++) {
			assertCounts(Long.valueOf(projectId), eventType, month, newFilesCount, newUsersCount);
		}

		for (int projectId = overrideProjectsNumber; projectId < projectsNumber; projectId++) {
			assertCounts(Long.valueOf(projectId), eventType, month, TEST_FILES_COUNT, TEST_USERS_COUNT);
		}

	}

	private void assertCounts(Long projectId, FileEvent eventType, YearMonth month, Integer filesCount, Integer usersCount) {
		Optional<StatisticsMonthlyProjectFiles> result = dao.getProjectFilesStatistics(Long.valueOf(projectId), eventType, month);

		assertTrue(result.isPresent());

		StatisticsMonthlyProjectFiles stats = result.get();

		assertEquals(filesCount, stats.getFilesCount());
		assertEquals(usersCount, stats.getUsersCount());
	}

	private List<StatisticsMonthlyProjectFiles> getBatch(FileEvent eventType, YearMonth month, int projectsNumber) {
		return getBatch(eventType, month, projectsNumber, TEST_FILES_COUNT, TEST_USERS_COUNT);
	}

	private List<StatisticsMonthlyProjectFiles> getBatch(FileEvent eventType, YearMonth month, int projectsNumber, int filesCount,
			int usersCount) {
		List<StatisticsMonthlyProjectFiles> batch = new ArrayList<>();
		IntStream.range(0, projectsNumber).forEach(index -> {
			StatisticsMonthlyProjectFiles dto = new StatisticsMonthlyProjectFiles();
			dto.setProjectId(Long.valueOf(index));
			dto.setEventType(eventType);
			dto.setMonth(month);
			dto.setFilesCount(filesCount);
			dto.setUsersCount(usersCount);
			batch.add(dto);
		});
		return batch;
	}

}
