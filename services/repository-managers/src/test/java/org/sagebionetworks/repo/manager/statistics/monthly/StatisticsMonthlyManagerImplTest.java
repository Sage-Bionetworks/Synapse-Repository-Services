package org.sagebionetworks.repo.manager.statistics.monthly;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressListener;
import org.sagebionetworks.repo.manager.statistics.StatisticsProcessingException;
import org.sagebionetworks.repo.model.dbo.statistics.StatisticsMonthlyStatusDAO;
import org.sagebionetworks.repo.model.statistics.StatisticsObjectType;
import org.sagebionetworks.repo.model.statistics.StatisticsStatus;
import org.sagebionetworks.repo.model.statistics.monthly.StatisticsMonthlyStatus;
import org.sagebionetworks.repo.model.statistics.monthly.StatisticsMonthlyUtils;

@ExtendWith(MockitoExtension.class)
public class StatisticsMonthlyManagerImplTest {

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
	private StatisticsMonthlyProcessorNotifier mockNotifier;

	@Mock
	private StackConfiguration mockConfig;

	@Mock
	private ProgressCallback mockCallback;

	@Mock
	private StatisticsMonthlyStatus mockStatus;

	private StatisticsMonthlyManagerImpl manager;

	@BeforeEach
	public void before() {
		when(mockConfig.getMaximumMonthsForMonthlyStatistics()).thenReturn(MAX_MONTHS_TO_PROCESS);

		manager = new StatisticsMonthlyManagerImpl(mockDao, mockProcessorProvider, mockNotifier, mockConfig);
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

		when(mockProcessorProvider.getProcessors(OBJECT_TYPE)).thenReturn(Collections.singletonList(mockProcessor));

		// Call under test
		manager.processMonth(OBJECT_TYPE, month, mockCallback);

		verify(mockCallback).addProgressListener(any(ProgressListener.class));
		verify(mockProcessor).processMonth(month);
		verify(mockDao).setAvailable(OBJECT_TYPE, month);
		verify(mockCallback).removeProgressListener(any(ProgressListener.class));
	}

	@Test
	public void testProcessMonthFailedWithNoErrorMessage() {

		YearMonth month = YearMonth.of(2019, 8);

		Exception ex = new IllegalStateException();

		doThrow(ex).when(mockProcessor).processMonth(any());

		when(mockProcessorProvider.getProcessors(OBJECT_TYPE)).thenReturn(Collections.singletonList(mockProcessor));

		Assertions.assertThrows(StatisticsProcessingException.class, () -> {
			// Call under test
			manager.processMonth(OBJECT_TYPE, month, mockCallback);
		});

		verify(mockCallback).addProgressListener(any(ProgressListener.class));
		verify(mockProcessor).processMonth(month);
		verify(mockDao, never()).setAvailable(OBJECT_TYPE, month);
		verify(mockDao).setProcessingFailed(eq(OBJECT_TYPE), eq(month), nullable(String.class), any(String.class));
		verify(mockCallback).removeProgressListener(any(ProgressListener.class));
	}

	@Test
	public void testProcessMonthFailedWithErrorMessage() {

		YearMonth month = YearMonth.of(2019, 8);

		Exception ex = new IllegalStateException("Some error");

		String errorMessage = ex.getMessage();

		doThrow(ex).when(mockProcessor).processMonth(any());
		when(mockProcessorProvider.getProcessors(OBJECT_TYPE)).thenReturn(Collections.singletonList(mockProcessor));

		Assertions.assertThrows(StatisticsProcessingException.class, () -> {
			// Call under test
			manager.processMonth(OBJECT_TYPE, month, mockCallback);
		});

		verify(mockCallback).addProgressListener(any(ProgressListener.class));
		verify(mockProcessor).processMonth(month);
		verify(mockDao, never()).setAvailable(OBJECT_TYPE, month);
		verify(mockDao).setProcessingFailed(eq(OBJECT_TYPE), eq(month), eq(errorMessage), any(String.class));
		verify(mockCallback).removeProgressListener(any(ProgressListener.class));
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
		verify(mockNotifier).sendStartProcessingNotification(OBJECT_TYPE, month);
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
		verify(mockNotifier, never()).sendStartProcessingNotification(any(), any());
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
		verify(mockNotifier).sendStartProcessingNotification(OBJECT_TYPE, month);
	}

	@Test
	public void testStartProcessingMonthWhenProcessingOngoing() {
		YearMonth month = YearMonth.of(2019, 8);

		when(mockStatus.getStatus()).thenReturn(StatisticsStatus.PROCESSING);
		when(mockStatus.getLastUpdatedOn()).thenReturn(System.currentTimeMillis() + PROCESSING_TIMEOUT);

		Optional<StatisticsMonthlyStatus> status = Optional.of(mockStatus);

		when(mockDao.getStatusForUpdate(OBJECT_TYPE, month)).thenReturn(status);

		// Call under test

		boolean result = manager.startProcessingMonth(OBJECT_TYPE, month, PROCESSING_TIMEOUT);

		assertFalse(result);

		verify(mockDao).getStatusForUpdate(OBJECT_TYPE, month);
		verify(mockStatus).getStatus();
		verify(mockStatus).getLastUpdatedOn();
		verify(mockDao, never()).setProcessing(any(), any());
		verify(mockNotifier, never()).sendStartProcessingNotification(any(), any());
	}

	@Test
	public void testStartProcessingMonthWhenProcessingTimeout() {
		YearMonth month = YearMonth.of(2019, 8);

		when(mockStatus.getStatus()).thenReturn(StatisticsStatus.PROCESSING);
		when(mockStatus.getLastUpdatedOn()).thenReturn(System.currentTimeMillis() - PROCESSING_TIMEOUT);

		Optional<StatisticsMonthlyStatus> status = Optional.of(mockStatus);

		when(mockDao.getStatusForUpdate(OBJECT_TYPE, month)).thenReturn(status);

		// Call under test

		boolean result = manager.startProcessingMonth(OBJECT_TYPE, month, PROCESSING_TIMEOUT);

		assertTrue(result);

		verify(mockDao).getStatusForUpdate(OBJECT_TYPE, month);
		verify(mockStatus).getStatus();
		verify(mockStatus).getLastUpdatedOn();
		verify(mockDao).setProcessing(OBJECT_TYPE, month);
		verify(mockNotifier).sendStartProcessingNotification(OBJECT_TYPE, month);
	}

	@Test
	public void testShouldStartProcessingWhenProcessingFailed() {
		long now = System.currentTimeMillis();

		when(mockStatus.getStatus()).thenReturn(StatisticsStatus.PROCESSING_FAILED);

		// Call under test
		boolean result = manager.shouldStartProcessing(mockStatus, now, PROCESSING_TIMEOUT);

		assertTrue(result);

		verify(mockStatus).getStatus();
		verify(mockStatus, never()).getLastUpdatedOn();
	}

	@Test
	public void testShouldStartProcessingWhenAvailable() {
		long now = System.currentTimeMillis();

		when(mockStatus.getStatus()).thenReturn(StatisticsStatus.AVAILABLE);

		// Call under test
		boolean result = manager.shouldStartProcessing(mockStatus, now, PROCESSING_TIMEOUT);

		assertFalse(result);

		verify(mockStatus).getStatus();
		verify(mockStatus, never()).getLastUpdatedOn();
	}

	@Test
	public void testShouldStartProcessingWhenProcessingOngoing() {
		long now = System.currentTimeMillis();

		when(mockStatus.getStatus()).thenReturn(StatisticsStatus.PROCESSING);
		when(mockStatus.getLastUpdatedOn()).thenReturn(now + PROCESSING_TIMEOUT);

		// Call under test
		boolean result = manager.shouldStartProcessing(mockStatus, now, PROCESSING_TIMEOUT);

		assertFalse(result);

		verify(mockStatus).getStatus();
		verify(mockStatus).getLastUpdatedOn();
	}

	@Test
	public void testShouldStartProcessingWhenProcessingTimedout() {
		long now = System.currentTimeMillis();

		when(mockStatus.getStatus()).thenReturn(StatisticsStatus.PROCESSING);
		when(mockStatus.getLastUpdatedOn()).thenReturn(now - PROCESSING_TIMEOUT);

		// Call under test
		boolean result = manager.shouldStartProcessing(mockStatus, now, PROCESSING_TIMEOUT);

		assertTrue(result);

		verify(mockStatus).getStatus();
		verify(mockStatus).getLastUpdatedOn();
	}

}
