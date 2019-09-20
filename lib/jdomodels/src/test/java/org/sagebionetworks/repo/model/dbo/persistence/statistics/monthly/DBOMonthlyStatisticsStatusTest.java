package org.sagebionetworks.repo.model.dbo.persistence.statistics.monthly;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.statistics.DBOStatisticsMonthlyStatus;
import org.sagebionetworks.repo.model.statistics.StatisticsObjectType;
import org.sagebionetworks.repo.model.statistics.StatisticsStatus;
import org.sagebionetworks.repo.model.statistics.monthly.StatisticsMonthlyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOMonthlyStatisticsStatusTest {

	@Autowired
	private DBOBasicDao dboBasicDao;

	private List<YearMonth> toDelete;

	private String objectType = StatisticsObjectType.PROJECT.toString();

	@BeforeEach
	private void before() {
		toDelete = new ArrayList<>();
	}

	@AfterEach
	private void after() {
		toDelete.forEach(month -> {
			dboBasicDao.deleteObjectByPrimaryKey(DBOStatisticsMonthlyStatus.class, getPrimaryKeyParams(month));
		});
	}

	@Test
	public void testCreate() {
		int year = 2019;
		int month = 8;
		
		YearMonth yearMonth  = YearMonth.of(year, month);

		DBOStatisticsMonthlyStatus dbo = newStatus(yearMonth);
		
		// Call under test
		DBOStatisticsMonthlyStatus created = dboBasicDao.createNew(dbo);

		assertEquals(dbo, created);
	}
	
	@Test
	public void testAlwayFirstOfTheMonth() {
		int year = 2019;
		int month = 8;
		
		YearMonth yearMonth  = YearMonth.of(year, month);

		DBOStatisticsMonthlyStatus dbo = newStatus(yearMonth);
		
		// A day different than the first day of the month
		int dayOfTheMonth = 2;
		
		dbo.setMonth(dbo.getMonth().withDayOfMonth(dayOfTheMonth));
		
		// Makes sure that the DBO sets it back to the first day of the month
		assertEquals(yearMonth.atDay(1), dbo.getMonth());
		
		DBOStatisticsMonthlyStatus created = dboBasicDao.createNew(dbo);

		assertEquals(dbo, created);
		assertEquals(yearMonth.atDay(1), created.getMonth());
	}

	@Test
	public void testUpdate() {
		int year = 2019;
		int month = 8;
		
		YearMonth yearMonth  = YearMonth.of(year, month);

		DBOStatisticsMonthlyStatus dbo = newStatus(yearMonth);
		DBOStatisticsMonthlyStatus created = dboBasicDao.createNew(dbo);

		assertEquals(created, dbo);

		created.setStatus(StatisticsStatus.PROCESSING.toString());

		// Call under test
		boolean updated = dboBasicDao.update(created);

		assertTrue(updated);

		DBOStatisticsMonthlyStatus updatedDbo = getStatus(yearMonth);

		assertEquals(created, updatedDbo);

	}

	@Test
	public void testCreateFailOnDuplicate() {
		int year = 2019;
		int month = 8;
		
		YearMonth yearMonth  = YearMonth.of(year, month);

		DBOStatisticsMonthlyStatus dbo = newStatus(yearMonth);
		DBOStatisticsMonthlyStatus created = dboBasicDao.createNew(dbo);

		assertEquals(created, dbo);

		// Makes sure that a duplicate key cannot be inserted
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			DBOStatisticsMonthlyStatus duplicateDbo = newStatus(yearMonth);
			// Call under test
			dboBasicDao.createNew(duplicateDbo);
		});
	}

	private DBOStatisticsMonthlyStatus getStatus(YearMonth month) {
		return dboBasicDao.getObjectByPrimaryKey(DBOStatisticsMonthlyStatus.class, getPrimaryKeyParams(month));
	}

	private SqlParameterSource getPrimaryKeyParams(YearMonth month) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("objectType", objectType);
		params.addValue("month", StatisticsMonthlyUtils.toDate(month));
		return params;
	}

	private DBOStatisticsMonthlyStatus newStatus(YearMonth month) {
		DBOStatisticsMonthlyStatus dbo = new DBOStatisticsMonthlyStatus();

		dbo.setObjectType(objectType);
		dbo.setMonth(StatisticsMonthlyUtils.toDate(month));
		dbo.setStatus(StatisticsStatus.AVAILABLE.toString());

		toDelete.add(month);

		return dbo;
	}

}
