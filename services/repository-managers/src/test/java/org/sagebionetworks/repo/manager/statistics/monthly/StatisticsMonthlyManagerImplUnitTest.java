package org.sagebionetworks.repo.manager.statistics.monthly;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.dao.statistics.StatisticsMonthlyStatusDAO;
import org.sagebionetworks.repo.model.statistics.StatisticsObjectType;
import org.sagebionetworks.repo.model.statistics.StatisticsStatus;
import org.sagebionetworks.repo.model.statistics.monthly.StatisticsMonthlyStatus;
import org.sagebionetworks.repo.model.statistics.monthly.StatisticsMonthlyUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class StatisticsMonthlyManagerImplUnitTest {

	private static final int MAX_MONTHS_TO_PROCESS = 12;
	private static final StatisticsObjectType OBJECT_TYPE = StatisticsObjectType.PROJECT;
	private static final long PROCESSING_TIMEOUT = 10000L;

	@Mock
	private StatisticsMonthlyStatusDAO mockDao;

	@Mock
	private StatisticsMonthlyProcessorProvider mockProcessorProvider;

	@Mock
	private StatisticsMonthlyProcessor mockProcessor;

	@Mock
	private StackConfiguration mockConfig;
	
	private StatisticsMonthlyManagerImpl manager;

	@BeforeEach
	public void before() {
		when(mockProcessor.getProcessingTimeout()).thenReturn(PROCESSING_TIMEOUT);
		when(mockProcessorProvider.getMonthlyProcessor(any(StatisticsObjectType.class))).thenReturn(mockProcessor);
		when(mockConfig.getMaximumMonthsForMonthlyStatistics()).thenReturn(MAX_MONTHS_TO_PROCESS);
		
		manager = new StatisticsMonthlyManagerImpl(mockDao, mockProcessorProvider, mockConfig);
	}

	@Test
	public void testGetUnprocessedMonthsWithInvalidInput() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			manager.getUnprocessedMonths(null);
		});
	}

	@Test
	public void testGetUnprocessedMonthsWithAllAvailable() {
		List<YearMonth> pastMonths = StatisticsMonthlyUtils.generatePastMonths(MAX_MONTHS_TO_PROCESS);
		List<StatisticsMonthlyStatus> availableMonths = new ArrayList<>();

		for (YearMonth month : pastMonths) {
			StatisticsMonthlyStatus status = new StatisticsMonthlyStatus();
			status.setMonth(month);
			status.setStatus(StatisticsStatus.AVAILABLE);
			availableMonths.add(status);
		}

		YearMonth from = pastMonths.get(0);
		YearMonth to = pastMonths.get(pastMonths.size() - 1);

		when(mockDao.getAvailableStatusInRange(OBJECT_TYPE, from, to)).thenReturn(availableMonths);

		// Call under test
		List<YearMonth> result = manager.getUnprocessedMonths(OBJECT_TYPE);

		assertTrue(result.isEmpty());
		verify(mockDao).getAvailableStatusInRange(OBJECT_TYPE, from, to);
	}

	@Test
	public void testGetUnprocessedMonthsWithAvailable() {
		List<YearMonth> pastMonths = StatisticsMonthlyUtils.generatePastMonths(MAX_MONTHS_TO_PROCESS);
		List<StatisticsMonthlyStatus> availableMonths = Collections.emptyList();

		YearMonth from = pastMonths.get(0);
		YearMonth to = pastMonths.get(pastMonths.size() - 1);

		when(mockDao.getAvailableStatusInRange(OBJECT_TYPE, from, to)).thenReturn(availableMonths);

		// Call under test
		List<YearMonth> result = manager.getUnprocessedMonths(OBJECT_TYPE);

		assertEquals(MAX_MONTHS_TO_PROCESS, result.size());
		verify(mockDao).getAvailableStatusInRange(OBJECT_TYPE, from, to);
	}

	@Test
	public void testGetUnprocessedMonthsWithPartiallyAvailable() {

		List<YearMonth> pastMonths = StatisticsMonthlyUtils.generatePastMonths(MAX_MONTHS_TO_PROCESS);

		StatisticsMonthlyStatus status = new StatisticsMonthlyStatus();
		status.setMonth(pastMonths.get(0));
		status.setStatus(StatisticsStatus.AVAILABLE);

		List<StatisticsMonthlyStatus> availableMonths = Collections.singletonList(status);

		YearMonth from = pastMonths.get(0);
		YearMonth to = pastMonths.get(pastMonths.size() - 1);

		when(mockDao.getAvailableStatusInRange(OBJECT_TYPE, from, to)).thenReturn(availableMonths);

		// Call under test
		List<YearMonth> result = manager.getUnprocessedMonths(OBJECT_TYPE);

		assertEquals(pastMonths.size() - availableMonths.size(), result.size());
		verify(mockDao).getAvailableStatusInRange(OBJECT_TYPE, from, to);
	}

	@Test
	public void testProcessMonthNoGo() {

		boolean processingStarted = false;

		YearMonth month = YearMonth.of(2019, 8);

		when(mockDao.startProcessing(OBJECT_TYPE, month, PROCESSING_TIMEOUT)).thenReturn(processingStarted);

		// Call under test
		boolean result = manager.processMonth(OBJECT_TYPE, month);

		assertFalse(result);
		verify(mockDao, times(1)).startProcessing(OBJECT_TYPE, month, PROCESSING_TIMEOUT);
		verify(mockProcessor, never()).processMonth(any());
		verify(mockDao, never()).setAvailable(any(), any());
		verify(mockDao, never()).setProcessingFailed(any(), any());

	}

	@Test
	public void testProcessMonthSuccess() {

		boolean processingStarted = true;

		YearMonth month = YearMonth.of(2019, 8);

		when(mockDao.startProcessing(OBJECT_TYPE, month, PROCESSING_TIMEOUT)).thenReturn(processingStarted);

		// Call under test
		boolean result = manager.processMonth(OBJECT_TYPE, month);

		assertTrue(result);
		verify(mockDao, times(1)).startProcessing(OBJECT_TYPE, month, PROCESSING_TIMEOUT);
		verify(mockProcessor, times(1)).processMonth(month);
		verify(mockDao, times(1)).setAvailable(OBJECT_TYPE, month);
	}

	@Test
	public void testProcessMonthFailed() {

		boolean processingStarted = true;

		YearMonth month = YearMonth.of(2019, 8);

		when(mockDao.startProcessing(OBJECT_TYPE, month, PROCESSING_TIMEOUT)).thenReturn(processingStarted);
		doThrow(IllegalStateException.class).when(mockProcessor).processMonth(any());

		// Call under test
		boolean result = manager.processMonth(OBJECT_TYPE, month);

		assertTrue(result);
		verify(mockDao, times(1)).startProcessing(OBJECT_TYPE, month, PROCESSING_TIMEOUT);
		verify(mockProcessor, times(1)).processMonth(any());
		verify(mockDao, times(1)).setProcessingFailed(OBJECT_TYPE, month);
		verify(mockDao, never()).setAvailable(OBJECT_TYPE, month);
	}

}
