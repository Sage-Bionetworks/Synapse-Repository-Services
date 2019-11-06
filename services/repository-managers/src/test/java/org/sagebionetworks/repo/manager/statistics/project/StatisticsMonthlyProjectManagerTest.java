package org.sagebionetworks.repo.manager.statistics.project;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.athena.AthenaQueryResult;
import org.sagebionetworks.repo.model.athena.project.AthenaProjectFileStatisticsDAO;
import org.sagebionetworks.repo.model.dbo.statistics.StatisticsMonthlyProjectFilesDAO;
import org.sagebionetworks.repo.model.statistics.FileEvent;
import org.sagebionetworks.repo.model.statistics.project.StatisticsMonthlyProjectFiles;

@ExtendWith(MockitoExtension.class)
public class StatisticsMonthlyProjectManagerTest {

	@Mock
	private AthenaProjectFileStatisticsDAO mockAthenaDao;

	@Mock
	private AthenaQueryResult<StatisticsMonthlyProjectFiles> mockQueryResults;

	@Mock
	private StatisticsMonthlyProjectFilesDAO mockStatsDao;

	@Mock
	private Iterator<StatisticsMonthlyProjectFiles> mockResultsIterator;

	@Mock
	private List<StatisticsMonthlyProjectFiles> mockBatch;
	

	@InjectMocks
	private StatisticsMonthlyProjectManagerImpl manager;

	private Integer filesCount = 1000;
	private Integer usersCount = 100;

	@Test
	public void testComputeFileStatsForMonthWithInvalidInput() throws Exception {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {

			FileEvent eventType = null;
			YearMonth month = YearMonth.of(2019, 8);

			// Call under test
			manager.computeFileEventsStatistics(eventType, month);
		});

		Assertions.assertThrows(IllegalArgumentException.class, () -> {

			FileEvent eventType = FileEvent.FILE_DOWNLOAD;
			YearMonth month = null;

			// Call under test
			manager.computeFileEventsStatistics(eventType, month);
		});
	}

	@Test
	public void testComputeFileStatsForMonth() throws Exception {

		FileEvent eventType = FileEvent.FILE_DOWNLOAD;
		YearMonth month = YearMonth.of(2019, 8);

		int resultsNumber = 10;

		List<StatisticsMonthlyProjectFiles> results = getBatch(eventType, month, resultsNumber);

		Iterator<StatisticsMonthlyProjectFiles> iterator = results.iterator();

		when(mockResultsIterator.hasNext()).then(_i -> iterator.hasNext());
		when(mockResultsIterator.next()).then(_i -> iterator.next());

		when(mockQueryResults.getQueryResultsIterator()).thenReturn(mockResultsIterator);
		when(mockAthenaDao.aggregateForMonth(eventType, month)).thenReturn(mockQueryResults);

		// Call under test
		manager.computeFileEventsStatistics(eventType, month);

		verify(mockAthenaDao).aggregateForMonth(eventType, month);
		verify(mockQueryResults).getQueryResultsIterator();
		verify(mockStatsDao).save(results);
	}
	
	@Test
	public void testComputeFileStatsForMonthWithNullProjectIds() throws Exception {

		FileEvent eventType = FileEvent.FILE_DOWNLOAD;
		YearMonth month = YearMonth.of(2019, 8);

		int resultsNumber = 10;

		List<StatisticsMonthlyProjectFiles> results = getBatch(eventType, month, resultsNumber);
		
		// Set the project id to null for last result of the batch
		results.get(results.size() - 1).setProjectId(null);

		Iterator<StatisticsMonthlyProjectFiles> iterator = results.iterator();

		when(mockResultsIterator.hasNext()).then(_i -> iterator.hasNext());
		when(mockResultsIterator.next()).then(_i -> iterator.next());

		when(mockQueryResults.getQueryResultsIterator()).thenReturn(mockResultsIterator);
		when(mockAthenaDao.aggregateForMonth(eventType, month)).thenReturn(mockQueryResults);

		// Call under test
		manager.computeFileEventsStatistics(eventType, month);

		verify(mockAthenaDao).aggregateForMonth(eventType, month);
		verify(mockQueryResults).getQueryResultsIterator();
		
		// The last record should have been skipped 
		List<StatisticsMonthlyProjectFiles> expectedResults = results.subList(0, results.size() - 1);
		
		verify(mockStatsDao).save(expectedResults);
	}


	@Test
	public void testComputeFileStatsForMonthWithMultipleBatches() throws Exception {
		FileEvent eventType = FileEvent.FILE_DOWNLOAD;
		YearMonth month = YearMonth.of(2019, 8);

		int firstBatchSize = StatisticsMonthlyProjectManagerImpl.BATCH_SIZE;
		int secondBatchSize = 10;

		List<StatisticsMonthlyProjectFiles> results = getBatch(eventType, month, firstBatchSize + secondBatchSize);

		Iterator<StatisticsMonthlyProjectFiles> iterator = results.iterator();

		when(mockResultsIterator.hasNext()).then(_i -> iterator.hasNext());
		when(mockResultsIterator.next()).then(_i -> iterator.next());

		when(mockQueryResults.getQueryResultsIterator()).thenReturn(mockResultsIterator);
		when(mockAthenaDao.aggregateForMonth(eventType, month)).thenReturn(mockQueryResults);

		// Call under test
		manager.computeFileEventsStatistics(eventType, month);

		verify(mockAthenaDao).aggregateForMonth(eventType, month);
		verify(mockQueryResults).getQueryResultsIterator();
		verify(mockStatsDao).save(results.subList(0, firstBatchSize));
		verify(mockStatsDao).save(results.subList(firstBatchSize, firstBatchSize + secondBatchSize));
	}

	@Test
	public void testSaveBatchWithEmptyBatch() {
		int threshold = 10;
		boolean isEmpty = true;

		when(mockBatch.isEmpty()).thenReturn(isEmpty);

		// Call under test
		manager.saveBatch(mockBatch, threshold);

		verify(mockStatsDao, never()).save(any());
	}

	@Test
	public void testSaveBatchWithUnderThreshold() {
		int threshold = 10;
		int recordsNumber = 5;

		when(mockBatch.size()).thenReturn(recordsNumber);

		// Call under test
		manager.saveBatch(mockBatch, threshold);

		verify(mockStatsDao, never()).save(any());
	}

	@Test
	public void testSaveBatchWithSameAsThreshold() {
		int threshold = 10;
		int recordsNumber = threshold;

		when(mockBatch.size()).thenReturn(recordsNumber);

		// Call under test
		manager.saveBatch(mockBatch, threshold);

		verify(mockStatsDao).save(mockBatch);
	}

	@Test
	public void testSaveBatchWithOverThreshold() {
		int threshold = 10;
		int recordsNumber = 15;

		when(mockBatch.size()).thenReturn(recordsNumber);

		// Call under test
		manager.saveBatch(mockBatch, threshold);

		verify(mockStatsDao).save(mockBatch);
	}

	private List<StatisticsMonthlyProjectFiles> getBatch(FileEvent eventType, YearMonth month, int size) {
		List<StatisticsMonthlyProjectFiles> batch = new ArrayList<>();

		IntStream.range(0, size).forEach(index -> {
			StatisticsMonthlyProjectFiles dto = new StatisticsMonthlyProjectFiles();

			dto.setEventType(eventType);
			dto.setMonth(month);
			dto.setProjectId(Long.valueOf(index));
			dto.setFilesCount(filesCount);
			dto.setUsersCount(usersCount);
			batch.add(dto);
		});

		return batch;
	}

}
