package org.sagebionetworks.repo.model.table.parser;

import org.joda.time.format.ISODateTimeFormat;
import org.sagebionetworks.util.TimeUtils;

public class DateToLongParser extends AbstractValueParser {

	@Override
	public Object parseValueForDatabaseWrite(String value) throws IllegalArgumentException {
		if(value == null){
			throw new IllegalArgumentException("Value cannot be null");
		}
		/*
		 * Dates can be a long or string. Long is tried first, then the
		 * string.
		 */
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException e) {
			try {
				return TimeUtils.parseSqlDate(value);
			} catch (IllegalArgumentException e2){
				// To keep the error type and messages consistent,
				// we use Joda instead of Java 8's Instant to parse
				// since TimeUtils.parseSqlDate already uses Joda
				return ISODateTimeFormat.dateTimeParser().parseDateTime(value).getMillis();
			}
		}
	}
	
	@Override
	public String parseValueForDatabaseRead(String value)
			throws IllegalArgumentException {
		return value;
	}

}
