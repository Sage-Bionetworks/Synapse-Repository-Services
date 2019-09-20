package org.sagebionetworks.repo.model.athena.project;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.YearMonth;
import java.util.Iterator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.athena.AthenaQueryResult;
import org.sagebionetworks.repo.model.athena.AthenaQueryStatistics;
import org.sagebionetworks.repo.model.athena.AthenaSupport;
import org.sagebionetworks.repo.model.athena.RowMapper;
import org.sagebionetworks.repo.model.statistics.FileEvent;
import org.sagebionetworks.repo.model.statistics.project.StatisticsMonthlyProjectFiles;
import org.sagebionetworks.repo.web.ServiceUnavailableException;

import com.amazonaws.services.athena.model.Datum;
import com.amazonaws.services.athena.model.Row;
import com.amazonaws.services.glue.model.Database;

@ExtendWith(MockitoExtension.class)
public class AthenaProjectFileStatisticsDAOImplTest {

	@Mock
	private AthenaSupport mockAthenaSupport;
	
	@Mock
	private FileEventTableNameProvider mockTableNameProvider;

	@Mock
	private Database mockDatabase;

	@Mock
	private AthenaQueryResult<StatisticsMonthlyProjectFiles> mockQueryResult;

	@Mock
	private AthenaQueryStatistics mockQueryStats;

	@Mock
	private Iterator<StatisticsMonthlyProjectFiles> mockResultsIterator;

	@InjectMocks
	private AthenaProjectFileStatisticsDAOImpl dao;

	private YearMonth month = YearMonth.of(2019, 8);

	@Test
	public void testAggregateForMonthInvalidInput() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			FileEvent eventType = null;
			YearMonth month = YearMonth.of(2019, 8);
			dao.aggregateForMonth(eventType, month);
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			FileEvent eventType = FileEvent.FILE_DOWNLOAD;
			YearMonth month = null;
			dao.aggregateForMonth(eventType, month);
		});
	}

	@Test
	public void testAggregateFileDownloadsForMonth() throws Exception {
		testAggregateFileEventForMonth(FileEvent.FILE_DOWNLOAD);
	}

	@Test
	public void testAggregateFileUploadsForMonth() throws Exception {
		testAggregateFileEventForMonth(FileEvent.FILE_UPLOAD);
	}

	@Test
	public void testRowMapper() {
		FileEvent eventType = FileEvent.FILE_DOWNLOAD;

		String projectId = "123";
		String filesCount = "10000";
		String usersCount = "100";

		Row row = new Row().withData(datum(projectId), datum(filesCount), datum(usersCount));

		RowMapper<StatisticsMonthlyProjectFiles> mapper = dao.getMapper(eventType, month);

		StatisticsMonthlyProjectFiles expected = new StatisticsMonthlyProjectFiles();

		expected.setEventType(eventType);
		expected.setMonth(month);
		expected.setProjectId(Long.valueOf(projectId));
		expected.setFilesCount(Integer.valueOf(filesCount));
		expected.setUsersCount(Integer.valueOf(usersCount));

		// Call under test
		StatisticsMonthlyProjectFiles result = mapper.mapRow(row);

		assertEquals(expected, result);
	}

	@Test
	public void testRowMapperWithNullProject() {
		FileEvent eventType = FileEvent.FILE_DOWNLOAD;

		String projectId = null;
		String filesCount = "10000";
		String usersCount = "100";

		Row row = new Row().withData(datum(projectId), datum(filesCount), datum(usersCount));

		RowMapper<StatisticsMonthlyProjectFiles> mapper = dao.getMapper(eventType, month);

		// Call under test
		StatisticsMonthlyProjectFiles result = mapper.mapRow(row);

		assertNotNull(result);
		assertNull(result.getProjectId());
		assertEquals(Integer.valueOf(filesCount), result.getFilesCount());
		assertEquals(Integer.valueOf(usersCount), result.getUsersCount());

	}

	@Test
	public void testRowMapperWithNullCounts() {
		FileEvent eventType = FileEvent.FILE_DOWNLOAD;

		String projectId = "123";
		String filesCount = null;
		String usersCount = null;

		Row row = new Row().withData(datum(projectId), datum(filesCount), datum(usersCount));

		RowMapper<StatisticsMonthlyProjectFiles> mapper = dao.getMapper(eventType, month);

		// Call under test
		StatisticsMonthlyProjectFiles result = mapper.mapRow(row);

		assertNotNull(result);
		assertEquals(Long.valueOf(projectId), result.getProjectId());
		assertEquals(0, result.getFilesCount());
		assertEquals(0, result.getUsersCount());

	}

	@Test
	public void testRowMapperWithInvalidRow() {
		FileEvent eventType = FileEvent.FILE_DOWNLOAD;

		String projectId = "123_invalid";
		String filesCount = "10000";
		String usersCount = "100";

		Row row = new Row().withData(datum(projectId), datum(filesCount), datum(usersCount));

		RowMapper<StatisticsMonthlyProjectFiles> mapper = dao.getMapper(eventType, month);

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			mapper.mapRow(row);
		});
	}

	@Test
	public void testGetAggregateQueryOneDigitMonth() {
		FileEvent eventType = FileEvent.FILE_DOWNLOAD;
		YearMonth month = YearMonth.of(2019, 1);
		String tableName = getExpectedTableName(eventType);

		when(mockTableNameProvider.getTableName(eventType)).thenReturn(tableName);

		// Call under test
		String result = dao.getAggregateQuery(eventType, month);
		String expectedQuery = getExpectedQuery(tableName, month);

		assertEquals(expectedQuery, result);
	}

	@Test
	public void testGetAggregateQueryTwoDigitsMonth() {
		FileEvent eventType = FileEvent.FILE_DOWNLOAD;
		YearMonth month = YearMonth.of(2019, 10);
		String tableName = getExpectedTableName(eventType);

		when(mockTableNameProvider.getTableName(eventType)).thenReturn(tableName);

		// Call under test
		String result = dao.getAggregateQuery(eventType, month);
		String expectedQuery = getExpectedQuery(tableName, month);

		assertEquals(expectedQuery, result);
	}

	private Datum datum(String value) {
		return new Datum().withVarCharValue(value);
	}

	@SuppressWarnings("unchecked")
	private void testAggregateFileEventForMonth(FileEvent eventType) throws ServiceUnavailableException {

		String tableName = getExpectedTableName(eventType);

		when(mockAthenaSupport.getDatabase(AthenaProjectFileStatisticsDAOImpl.DATABASE_NAME)).thenReturn(mockDatabase);
		when(mockTableNameProvider.getTableName(eventType)).thenReturn(tableName);
		
		when(mockQueryResult.getQueryExecutionStatistics()).thenReturn(mockQueryStats);
		when(mockQueryResult.getQueryResultsIterator()).thenReturn(mockResultsIterator);
		when(mockAthenaSupport.executeQuery(eq(mockDatabase), any(), (RowMapper<StatisticsMonthlyProjectFiles>) any()))
				.thenReturn(mockQueryResult);

		// Call under test
		AthenaQueryResult<StatisticsMonthlyProjectFiles> result = dao.aggregateForMonth(eventType, month);

		assertEquals(mockQueryResult, result);
		assertEquals(mockQueryStats, result.getQueryExecutionStatistics());
		assertEquals(mockResultsIterator, result.getQueryResultsIterator());

		verify(mockAthenaSupport).getDatabase(AthenaProjectFileStatisticsDAOImpl.DATABASE_NAME);
		verify(mockTableNameProvider).getTableName(eventType);

		ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);

		verify(mockAthenaSupport).executeQuery(eq(mockDatabase), queryCaptor.capture(), any());

		assertEquals(getExpectedQuery(tableName, month), queryCaptor.getValue());
	}

	private String getExpectedTableName(FileEvent eventType) {
		return eventType.getGlueTableName();
	}

	private String getExpectedQuery(String tableName, YearMonth month) {
		return String.format(
				"SELECT projectId AS PROJECT_ID, COUNT(projectId) AS FILES_COUNT, COUNT(DISTINCT userId) AS USERS_COUNT FROM %1$s WHERE year='%2$d' AND month='%3$02d' GROUP BY projectId",
				tableName, month.getYear(), month.getMonthValue());
	}

}
