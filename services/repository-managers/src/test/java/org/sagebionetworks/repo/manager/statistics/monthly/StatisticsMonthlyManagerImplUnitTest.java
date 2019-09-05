package org.sagebionetworks.repo.manager.statistics.monthly;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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

	private static final int PROCESSING_TIMEOUT = 100;
	private static final int MAX_MONTHS_TO_PROCESS = 12;
	private static final StatisticsObjectType OBJECT_TYPE = StatisticsObjectType.PROJECT;

	@Mock
	private StatisticsMonthlyStatusDAO mockDao;

	@Mock
	private StatisticsMonthlyProcessorProvider mockProcessorProvider;

	@Mock
	private StatisticsMonthlyProcessor mockProcessor;

	@Mock
	private StackConfiguration mockConfig;

	@Mock
	private StatisticsMonthlyStatus mockStatus;

	private StatisticsMonthlyManagerImpl manager;

	@BeforeEach
	public void before() {
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
	public void testProcessMonthSuccess() {

		YearMonth month = YearMonth.of(2019, 8);

		// Call under test
		boolean result = manager.processMonth(OBJECT_TYPE, month);

		assertTrue(result);
		verify(mockProcessor, times(1)).processMonth(month);
		verify(mockDao, times(1)).setAvailable(OBJECT_TYPE, month);
	}

	@Test
	public void testProcessMonthFailedWithNoErrorMessage() {

		YearMonth month = YearMonth.of(2019, 8);

		doThrow(IllegalStateException.class).when(mockProcessor).processMonth(any());

		// Call under test
		boolean result = manager.processMonth(OBJECT_TYPE, month);

		assertFalse(result);
		verify(mockProcessor, times(1)).processMonth(any());
		verify(mockDao, never()).setAvailable(OBJECT_TYPE, month);
		verify(mockDao, times(1)).setProcessingFailed(eq(OBJECT_TYPE), eq(month), nullable(String.class), any(String.class));
	}

	@Test
	public void testProcessMonthFailedWithErrorMessage() {

		YearMonth month = YearMonth.of(2019, 8);

		Exception ex = new IllegalStateException("Some error");

		String errorMessage = ex.getMessage();

		doThrow(ex).when(mockProcessor).processMonth(any());

		// Call under test
		boolean result = manager.processMonth(OBJECT_TYPE, month);

		assertFalse(result);
		verify(mockProcessor, times(1)).processMonth(any());
		verify(mockDao, never()).setAvailable(OBJECT_TYPE, month);
		verify(mockDao, times(1)).setProcessingFailed(eq(OBJECT_TYPE), eq(month), eq(errorMessage), any(String.class));
	}

	@Test
	public void testStartProcessingMonthWhenAbsent() {
		YearMonth month = YearMonth.of(2019, 8);

		Optional<StatisticsMonthlyStatus> status = Optional.empty();

		when(mockDao.getStatusForUpdate(OBJECT_TYPE, month)).thenReturn(status);

		// Call under test

		boolean result = manager.startProcessingMonth(OBJECT_TYPE, month, PROCESSING_TIMEOUT);

		assertTrue(result);

		verify(mockDao).getStatusForUpdate(OBJECT_TYPE, month);
		verify(mockDao).setProcessing(OBJECT_TYPE, month);
	}

	@Test
	public void testStartProcessingMonthWhenAvailable() {
		YearMonth month = YearMonth.of(2019, 8);

		when(mockStatus.getStatus()).thenReturn(StatisticsStatus.AVAILABLE);

		Optional<StatisticsMonthlyStatus> status = Optional.of(mockStatus);

		when(mockDao.getStatusForUpdate(OBJECT_TYPE, month)).thenReturn(status);

		// Call under test

		boolean result = manager.startProcessingMonth(OBJECT_TYPE, month, PROCESSING_TIMEOUT);

		assertFalse(result);

		verify(mockDao).getStatusForUpdate(OBJECT_TYPE, month);
		verify(mockStatus).getStatus();
		verify(mockDao, never()).setProcessing(any(), any());
	}

	@Test
	public void testStartProcessingMonthWhenProcessingFailed() {
		YearMonth month = YearMonth.of(2019, 8);

		when(mockStatus.getStatus()).thenReturn(StatisticsStatus.PROCESSING_FAILED);

		Optional<StatisticsMonthlyStatus> status = Optional.of(mockStatus);

		when(mockDao.getStatusForUpdate(OBJECT_TYPE, month)).thenReturn(status);

		// Call under test

		boolean result = manager.startProcessingMonth(OBJECT_TYPE, month, PROCESSING_TIMEOUT);

		assertTrue(result);

		verify(mockDao).getStatusForUpdate(OBJECT_TYPE, month);
		verify(mockStatus).getStatus();
		verify(mockDao).setProcessing(OBJECT_TYPE, month);
	}

	@Test
	public void testStartProcessingMonthWhenProcessingOngoing() {
		YearMonth month = YearMonth.of(2019, 8);

		when(mockStatus.getStatus()).thenReturn(StatisticsStatus.PROCESSING);
		when(mockStatus.getLastStartedOn()).thenReturn(System.currentTimeMillis() + PROCESSING_TIMEOUT);

		Optional<StatisticsMonthlyStatus> status = Optional.of(mockStatus);

		when(mockDao.getStatusForUpdate(OBJECT_TYPE, month)).thenReturn(status);

		// Call under test

		boolean result = manager.startProcessingMonth(OBJECT_TYPE, month, PROCESSING_TIMEOUT);

		assertFalse(result);

		verify(mockDao).getStatusForUpdate(OBJECT_TYPE, month);
		verify(mockStatus).getStatus();
		verify(mockStatus).getLastStartedOn();
		verify(mockDao, never()).setProcessing(any(), any());
	}
	
	@Test
	public void testStartProcessingMonthWhenProcessingTimeout() {
		YearMonth month = YearMonth.of(2019, 8);

		when(mockStatus.getStatus()).thenReturn(StatisticsStatus.PROCESSING);
		when(mockStatus.getLastStartedOn()).thenReturn(System.currentTimeMillis() - PROCESSING_TIMEOUT);

		Optional<StatisticsMonthlyStatus> status = Optional.of(mockStatus);

		when(mockDao.getStatusForUpdate(OBJECT_TYPE, month)).thenReturn(status);

		// Call under test

		boolean result = manager.startProcessingMonth(OBJECT_TYPE, month, PROCESSING_TIMEOUT);

		assertTrue(result);

		verify(mockDao).getStatusForUpdate(OBJECT_TYPE, month);
		verify(mockStatus).getStatus();
		verify(mockStatus).getLastStartedOn();
		verify(mockDao).setProcessing(OBJECT_TYPE, month);
	}

}
