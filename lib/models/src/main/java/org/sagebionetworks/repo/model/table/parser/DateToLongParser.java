package org.sagebionetworks.repo.model.table.parser;

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
			return TimeUtils.parseSqlDate(value);
		}
	}
	
	@Override
	public String parseValueForDatabaseRead(String value)
			throws IllegalArgumentException {
		return value;
	}

}
