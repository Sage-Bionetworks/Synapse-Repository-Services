package org.sagebionetworks.repo.model.statistics;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.util.ValidateArgument;

public class StatisticsMonthlyUtils {
	
	public final static int FIRST_DAY_OF_THE_MONTH = 1;

	/**
	 * @param monthsNumber The number of past months
	 * @return A list of {@link YearMonth}s in the past for the given monthsNumber (excludes the current month)
	 */
	public static List<YearMonth> generatePastMonths(int monthsNumber) {
		ValidateArgument.requirement(monthsNumber > 0, "The number of months should be greater than 0");

		YearMonth currentMonth = YearMonth.now(ZoneOffset.UTC);

		List<YearMonth> months = new ArrayList<>();

		for (int delta = monthsNumber; delta > 0; delta--) {
			months.add(currentMonth.minusMonths(delta));
		}

		return months;
	}

	/**
	 * @return A standardized representation of the given {@link YearMonth} to a {@link LocalDate} to be used at the DAO
	 *         layer, always the first of the month
	 */
	public static LocalDate toDate(YearMonth month) {
		return LocalDate.of(month.getYear(), month.getMonth(), FIRST_DAY_OF_THE_MONTH);
	}

}
