package org.sagebionetworks.repo.model.table.parser;

import org.sagebionetworks.repo.model.table.ValueParser;
import org.sagebionetworks.util.TimeUtils;

public class DateToLongParser implements ValueParser {

	@Override
	public Object parseValueForDatabaseWrite(String value) throws IllegalArgumentException {
		/*
		 * Dates can be a long or string. Long is tried first, then the
		 * string.
		 */
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException e) {
			return TimeUtils.parseSqlDate(value);
		}
	}
	
	@Override
	public String parseValueForDatabaseRead(String value)
			throws IllegalArgumentException {
		return value;
	}

}
