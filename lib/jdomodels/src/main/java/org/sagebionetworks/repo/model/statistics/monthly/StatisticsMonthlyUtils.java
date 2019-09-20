package org.sagebionetworks.repo.model.statistics.monthly;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.repo.model.statistics.StatisticsObjectType;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.ValidateArgument;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class StatisticsMonthlyUtils {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	
	static {
		OBJECT_MAPPER.registerModule(new JavaTimeModule());
	}
	
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

	/**
	 * Return a pair representing the start (inclusive) and end (exclusive) timestamps (ms, epoch time) that represent the
	 * range of the given month
	 * 
	 * @param month
	 * @return
	 */
	public static Pair<Long, Long> getTimestampRange(YearMonth month) {
		LocalDate firstOfTheMonth = month.atDay(FIRST_DAY_OF_THE_MONTH);
	
		Long start = firstOfTheMonth.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
		Long end = firstOfTheMonth.plusMonths(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
	
		return new Pair<>(start, end);
	}
	
	public static String buildNotificationBody(StatisticsObjectType objectType, YearMonth month) {
		try {
			return OBJECT_MAPPER.writeValueAsString(new StatisticsMonthlyProcessNotification(objectType, month));
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	public static StatisticsMonthlyProcessNotification fromNotificationBody(String json) {
		try {
			return OBJECT_MAPPER.readValue(json, StatisticsMonthlyProcessNotification.class);
		} catch (IOException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}
	
	public static String createErrorDetails(Throwable ex) {
		StringWriter writer = new StringWriter();
		ex.printStackTrace(new PrintWriter(writer));
		return writer.toString();
	}

	public static String encodeErrorMessage(String errorMessage, int maxChars) {
		return StringUtils.abbreviate(errorMessage, maxChars);
	}
	
	public static byte[] encodeErrorDetails(String errorDetails) {
		if (errorDetails == null) {
			return null;
		}
		try {
			return errorDetails.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

}
