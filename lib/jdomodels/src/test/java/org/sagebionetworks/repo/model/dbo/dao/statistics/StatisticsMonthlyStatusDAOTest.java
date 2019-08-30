package org.sagebionetworks.repo.model.dbo.dao.statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.dao.statistics.StatisticsMonthlyStatusDAO;
import org.sagebionetworks.repo.model.statistics.StatisticsMonthlyStatus;
import org.sagebionetworks.repo.model.statistics.StatisticsObjectType;
import org.sagebionetworks.repo.model.statistics.StatisticsStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class StatisticsMonthlyStatusDAOTest {

	private static final StatisticsObjectType OBJECT_TYPE = StatisticsObjectType.PROJECT;

	@Autowired
	private StatisticsMonthlyStatusDAO dao;

	@BeforeEach
	public void before() {
		dao.clear();
	}

	@AfterEach
	public void after() {
		dao.clear();
	}

	@Test
	public void testSetOnCreate() {
		int year = 2019;
		int month = 8;

		YearMonth yearMonth = YearMonth.of(year, month);

		// Makes sure that it does not exist
		assertFalse(dao.getStatus(OBJECT_TYPE, yearMonth).isPresent());

		// Call under test
		StatisticsMonthlyStatus status = dao.setAvailable(OBJECT_TYPE, yearMonth);

		assertEquals(yearMonth, status.getMonth());
		assertEquals(StatisticsStatus.AVAILABLE, status.getStatus());
	}

	@Test
	public void testSetOnUpdate() {
		int year = 2019;
		int month = 8;

		YearMonth yearMonth = YearMonth.of(year, month);

		// First set a status
		dao.setProcessing(OBJECT_TYPE, yearMonth);

		// Makes sure that it does exist
		Optional<StatisticsMonthlyStatus> getStatusResult = dao.getStatus(OBJECT_TYPE, yearMonth);

		assertTrue(getStatusResult.isPresent());

		StatisticsMonthlyStatus status = getStatusResult.get();

		assertEquals(StatisticsStatus.PROCESSING, status.getStatus());

		// Call under test
		StatisticsMonthlyStatus updated = dao.setAvailable(OBJECT_TYPE, yearMonth);

		assertEquals(yearMonth, updated.getMonth());
		assertEquals(StatisticsStatus.AVAILABLE, updated.getStatus());
	}

	@Test
	public void testSetFailedWithEmptyType() {
		assertThrows(IllegalArgumentException.class, () -> {
			YearMonth yearMonth = YearMonth.of(2019, 8);
			dao.setProcessingFailed(null, yearMonth);
		});
	}

	@Test
	public void testSetFailedWithEmptyMonth() {
		assertThrows(IllegalArgumentException.class, () -> {
			YearMonth yearMonth = null;
			dao.setProcessingFailed(OBJECT_TYPE, yearMonth);
		});
	}

	@Test
	public void testSetAvailableWithEmptyType() {
		assertThrows(IllegalArgumentException.class, () -> {
			YearMonth yearMonth = YearMonth.of(2019, 8);
			dao.setAvailable(null, yearMonth);
		});
	}

	@Test
	public void testSetAvailableWithEmptyMonth() {
		assertThrows(IllegalArgumentException.class, () -> {
			YearMonth yearMonth = null;
			dao.setAvailable(OBJECT_TYPE, yearMonth);
		});
	}

	@Test
	public void testSetProcessingWithEmptyType() {
		assertThrows(IllegalArgumentException.class, () -> {
			YearMonth yearMonth = YearMonth.of(2019, 8);
			dao.setProcessing(null, yearMonth);
		});
	}

	@Test
	public void testSetProcessingWithEmptyMonth() {
		assertThrows(IllegalArgumentException.class, () -> {
			YearMonth yearMonth = null;
			dao.setProcessing(OBJECT_TYPE, yearMonth);
		});
	}

	@Test
	public void testSetAvailableTimestamp() {
		int year = 2019;
		int month = 8;

		YearMonth yearMonth = YearMonth.of(year, month);

		// Call under test
		StatisticsMonthlyStatus status = dao.setAvailable(OBJECT_TYPE, yearMonth);

		assertEquals(yearMonth, status.getMonth());
		assertEquals(StatisticsStatus.AVAILABLE, status.getStatus());
		assertNotNull(status.getLastSucceededAt());
		assertNull(status.getLastStartedAt());
		assertNull(status.getLastFailedAt());
	}

	@Test
	public void testSetFailedTimestamp() {
		int year = 2019;
		int month = 8;

		YearMonth yearMonth = YearMonth.of(year, month);

		// Call under test
		StatisticsMonthlyStatus status = dao.setProcessingFailed(OBJECT_TYPE, yearMonth);

		assertEquals(yearMonth, status.getMonth());
		assertEquals(StatisticsStatus.PROCESSING_FAILED, status.getStatus());
		assertNull(status.getLastStartedAt());
		assertNull(status.getLastSucceededAt());
		assertNotNull(status.getLastFailedAt());
	}

	@Test
	public void testSetProcessing() {
		int year = 2019;
		int month = 8;

		YearMonth yearMonth = YearMonth.of(year, month);

		// Call under test
		StatisticsMonthlyStatus status = dao.setProcessing(OBJECT_TYPE, yearMonth);

		assertEquals(yearMonth, status.getMonth());
		assertEquals(StatisticsStatus.PROCESSING, status.getStatus());
		assertNotNull(status.getLastStartedAt());
		assertNull(status.getLastSucceededAt());
		assertNull(status.getLastFailedAt());
	}
	
	@Test
	public void testSetAvailableAfterProcessing() {
		int year = 2019;
		int month = 8;

		YearMonth yearMonth = YearMonth.of(year, month);

		// Set if first as available, this sets the started at timestamp
		StatisticsMonthlyStatus status = dao.setProcessing(OBJECT_TYPE, yearMonth);
		
		assertNotNull(status.getLastStartedAt());
		assertNull(status.getLastSucceededAt());
		assertNull(status.getLastFailedAt());
		
		// Call under test
		StatisticsMonthlyStatus statusUpdated = dao.setAvailable(OBJECT_TYPE, yearMonth);
		
		assertEquals(StatisticsStatus.AVAILABLE, statusUpdated.getStatus());
		assertNotNull(statusUpdated.getLastStartedAt());
		assertEquals(status.getLastStartedAt(), statusUpdated.getLastStartedAt());
		assertNotNull(statusUpdated.getLastSucceededAt());
		assertNull(statusUpdated.getLastFailedAt());
		
	}

	@Test
	public void testGetStatusWithEmptyType() {
		assertThrows(IllegalArgumentException.class, () -> {
			YearMonth yearMonth = YearMonth.of(2019, 8);
			// Call under test
			dao.getStatus(null, yearMonth);
		});
	}

	@Test
	public void testGetStatusWithEmptyMonth() {
		assertThrows(IllegalArgumentException.class, () -> {
			YearMonth yearMonth = null;
			// Call under test
			dao.getStatus(OBJECT_TYPE, yearMonth);
		});
	}

	@Test
	public void testGetStatusEmpty() {
		int year = 2019;
		int month = 8;

		YearMonth yearMonth = YearMonth.of(year, month);

		Optional<StatisticsMonthlyStatus> getStatusResult = dao.getStatus(OBJECT_TYPE, yearMonth);

		assertFalse(getStatusResult.isPresent());
	}

	@Test
	public void testGetAvailableStatusInRangeWithIllegalRange() {
		assertThrows(IllegalArgumentException.class, () -> {
			YearMonth from = YearMonth.of(2019, 8);
			YearMonth to = YearMonth.of(2019, 8);
			// Call under test
			dao.getAvailableStatusInRange(OBJECT_TYPE, from, to);
		});
		assertThrows(IllegalArgumentException.class, () -> {
			YearMonth from = YearMonth.of(2019, 9);
			YearMonth to = YearMonth.of(2019, 8);
			// Call under test
			dao.getAvailableStatusInRange(OBJECT_TYPE, from, to);
		});
		assertThrows(IllegalArgumentException.class, () -> {
			YearMonth from = null;
			YearMonth to = YearMonth.of(2019, 8);
			// Call under test
			dao.getAvailableStatusInRange(OBJECT_TYPE, from, to);
		});
		assertThrows(IllegalArgumentException.class, () -> {
			YearMonth from = YearMonth.of(2019, 7);
			YearMonth to = null;
			// Call under test
			dao.getAvailableStatusInRange(OBJECT_TYPE, from, to);
		});
	}

	@Test
	public void testGetAvailableStatusInRangeWithEmptyType() {
		assertThrows(IllegalArgumentException.class, () -> {
			YearMonth from = YearMonth.of(2019, 7);
			YearMonth to = YearMonth.of(2019, 8);
			// Call under test
			dao.getAvailableStatusInRange(null, from, to);
		});
	}

	@Test
	public void testGetAvailableStatusInRangeEmpty() {
		YearMonth from = YearMonth.of(2019, 7);
		YearMonth to = YearMonth.of(2019, 8);

		// Call under test
		List<StatisticsMonthlyStatus> result = dao.getAvailableStatusInRange(OBJECT_TYPE, from, to);

		assertTrue(result.isEmpty());
	}

	@Test
	public void testGetAvailableStatusSameYear() {
		int year = 2019;
		int minMonth = 1;
		int maxMonth = 12;

		// Setup some data

		for (int month = minMonth; month <= maxMonth; month++) {
			dao.setAvailable(OBJECT_TYPE, YearMonth.of(2019, month));
		}

		YearMonth from = YearMonth.of(year, minMonth);
		YearMonth to = YearMonth.of(year, maxMonth);

		// Call under test
		List<StatisticsMonthlyStatus> result = dao.getAvailableStatusInRange(OBJECT_TYPE, from, to);

		assertEquals(from.until(to, ChronoUnit.MONTHS) + 1, result.size());

		for (int i = 0; i < result.size(); i++) {
			StatisticsMonthlyStatus status = result.get(i);
			assertEquals(StatisticsStatus.AVAILABLE, status.getStatus());
			assertEquals(YearMonth.of(year, minMonth + i), status.getMonth());
		}

	}

	@Test
	public void testGetAvailableStatusSpanYears() {
		int startYear = 2018;
		int startMonth = 7;
		int months = 12;

		YearMonth from = YearMonth.of(startYear, startMonth);

		YearMonth month = from;
		
		for (int i = 0; i < months; i++) {
			dao.setAvailable(OBJECT_TYPE, month);
			month = month.plusMonths(1);
		}
		
		YearMonth to = month.minusMonths(1);
		
		// Call under test
		List<StatisticsMonthlyStatus> result = dao.getAvailableStatusInRange(OBJECT_TYPE, from, to);
		
		assertEquals(from.until(to, ChronoUnit.MONTHS) + 1, result.size());
	}

	@Test
	public void testGetAvailableStatusPartial() {
		int year = 2019;
		int startMonth = 1;
		int months = 12;

		YearMonth from = YearMonth.of(year, startMonth);
		YearMonth to = from.plusMonths(months);

		YearMonth month = from;

		// Set half of the months to be available
		for (int i = 0; i < months; i++) {
			if (i % 2 == 0) {
				dao.setAvailable(OBJECT_TYPE, month);
			} else {
				dao.setProcessing(OBJECT_TYPE, month);
			}
			month = month.plusMonths(1);
		}

		// Call under test
		List<StatisticsMonthlyStatus> result = dao.getAvailableStatusInRange(OBJECT_TYPE, from, to);

		assertEquals(months / 2, result.size());

		for (int i = 0; i < result.size(); i++) {
			StatisticsMonthlyStatus status = result.get(i);
			assertEquals(StatisticsStatus.AVAILABLE, status.getStatus());
		}

	}

}
