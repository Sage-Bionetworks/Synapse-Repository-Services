package org.sagebionetworks.repo.model.statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.statistics.monthly.StatisticsMonthlyProcessNotification;
import org.sagebionetworks.repo.model.statistics.monthly.StatisticsMonthlyUtils;
import org.sagebionetworks.util.Pair;

public class StatisticsMonthlyUtilsTest {

	@Test
	public void testGeneratePastMonthsInvalid() {
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			StatisticsMonthlyUtils.generatePastMonths(-1);
		});
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			StatisticsMonthlyUtils.generatePastMonths(0);
		});
	}
	
	@Test
	public void testGeneratePastMonthsSingle() {
		int numberOfMonths = 1;
		testForXMonths(numberOfMonths);
		
	}
	
	@Test
	public void testGeneratePastMonthsYear() {
		int numberOfMonths = 12;
		testForXMonths(numberOfMonths);
	}
	
	@Test
	public void testGeneratePastMonthsLessThanYear() {
		int numberOfMonths = 5;
		testForXMonths(numberOfMonths);
	}
	
	@Test
	public void testGeneratePastMonthsMoreThanYear() {
		int numberOfMonths = 15;
		testForXMonths(numberOfMonths);
	}
	
	@Test
	public void testBuildNotificationBody() {
		YearMonth month = YearMonth.of(2019, 8);
		
		StatisticsMonthlyProcessNotification notification = new StatisticsMonthlyProcessNotification(StatisticsObjectType.PROJECT, month);
		
		String json = StatisticsMonthlyUtils.buildNotificationBody(notification.getObjectType(), notification.getMonth());
		
		assertEquals("{\"objectType\":\"PROJECT\",\"month\":[2019,8]}", json);
	}
	
	@Test
	public void testFromNotificationBody() {
		YearMonth month = YearMonth.of(2019, 8);
		
		StatisticsMonthlyProcessNotification notification = new StatisticsMonthlyProcessNotification(StatisticsObjectType.PROJECT, month);
		
		String notificationBody = "{\"objectType\":\"PROJECT\",\"month\":[2019,8]}";
		
		StatisticsMonthlyProcessNotification result = StatisticsMonthlyUtils.fromNotificationBody(notificationBody);
		
		assertEquals(notification, result);
		
	}
	
	@Test
	public void testGetTimestampRange() {
		YearMonth month = YearMonth.of(2019, 8);
		
		// Call under test
		Pair<Long, Long> result = StatisticsMonthlyUtils.getTimestampRange(month);
		
		assertEquals(1564617600000L, result.getFirst());
		assertEquals(1567296000000L, result.getSecond());
	}
	
	@Test
	public void testGetTimestampRangeLeapYear() {
		YearMonth month = YearMonth.of(2020, 2);
		
		// Call under test
		Pair<Long, Long> result = StatisticsMonthlyUtils.getTimestampRange(month);
		
		assertEquals(1580515200000L, result.getFirst());
		assertEquals(1583020800000L, result.getSecond());
	}
	
	private void testForXMonths(int numberOfMonths) {
		YearMonth currentMonth = YearMonth.now(ZoneOffset.UTC);
		
		// Call under test
		List<YearMonth> months = StatisticsMonthlyUtils.generatePastMonths(numberOfMonths);
		
		assertEquals(numberOfMonths, months.size());
		// The last month should be the past month
		assertEquals(currentMonth.minusMonths(1), months.get(months.size() - 1));
		assertEquals(currentMonth.minusMonths(numberOfMonths), months.get(0));
		
		for (int i=0; i<months.size(); i++) {
			assertEquals(currentMonth.minusMonths(numberOfMonths - i), months.get(i));
		}
	}
	
}
