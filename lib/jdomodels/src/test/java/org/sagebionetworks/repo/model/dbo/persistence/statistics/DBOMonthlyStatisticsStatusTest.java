package org.sagebionetworks.repo.model.dbo.persistence.statistics;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.statistics.MonthOfTheYear;
import org.sagebionetworks.repo.model.statistics.StatisticsObjectType;
import org.sagebionetworks.repo.model.statistics.StatisticsStatus;
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

	private List<MonthOfTheYear> toDelete;

	private String objectType = StatisticsObjectType.PROJECT.toString();

	@BeforeEach
	private void before() {
		toDelete = new ArrayList<>();
	}

	@AfterEach
	private void after() {
		toDelete.forEach(month -> {
			dboBasicDao.deleteObjectByPrimaryKey(DBOMonthlyStatisticsStatus.class, getPrimaryKeyParams(month));
		});
	}

	@Test
	public void testCreate() {
		MonthOfTheYear month = MonthOfTheYear.of(2019, 8);

		DBOMonthlyStatisticsStatus dbo = newStatus(month);
		DBOMonthlyStatisticsStatus created = dboBasicDao.createNew(dbo);

		assertEquals(dbo, created);
	}

	@Test
	public void testUpdate() {

		MonthOfTheYear month = MonthOfTheYear.of(2019, 8);

		DBOMonthlyStatisticsStatus dbo = newStatus(month);
		DBOMonthlyStatisticsStatus created = dboBasicDao.createNew(dbo);

		assertEquals(created, dbo);

		created.setStatus(StatisticsStatus.PROCESSING.toString());

		boolean updated = dboBasicDao.update(created);

		assertTrue(updated);

		DBOMonthlyStatisticsStatus updatedDbo = getStatus(month);

		assertEquals(created, updatedDbo);

	}

	@Test
	public void testCreateFailOnDuplicate() {

		MonthOfTheYear month = MonthOfTheYear.of(2019, 8);

		DBOMonthlyStatisticsStatus dbo = newStatus(month);
		DBOMonthlyStatisticsStatus created = dboBasicDao.createNew(dbo);

		assertEquals(created, dbo);

		// Makes sure that a duplicate key cannot be inserted
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			DBOMonthlyStatisticsStatus duplicateDbo = newStatus(month);
			dboBasicDao.createNew(duplicateDbo);
		});
	}

	private DBOMonthlyStatisticsStatus getStatus(MonthOfTheYear month) {
		return dboBasicDao.getObjectByPrimaryKey(DBOMonthlyStatisticsStatus.class, getPrimaryKeyParams(month));
	}

	private SqlParameterSource getPrimaryKeyParams(MonthOfTheYear month) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("objectType", objectType);
		params.addValue("month", month.toDate());
		return params;
	}

	private DBOMonthlyStatisticsStatus newStatus(MonthOfTheYear month) {
		DBOMonthlyStatisticsStatus dbo = new DBOMonthlyStatisticsStatus();

		dbo.setObjectType(objectType);
		dbo.setMonth(month.toDate());
		dbo.setStatus(StatisticsStatus.AVAILABLE.toString());

		toDelete.add(month);

		return dbo;
	}

}
