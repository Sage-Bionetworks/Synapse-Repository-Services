package org.sagebionetworks.repo.model.table.parser;

import java.sql.Date;

import org.sagebionetworks.repo.model.table.ValueParser;

public class DateParser implements ValueParser {
	
	DateToLongParser dateToLongParser = new DateToLongParser();

	@Override
	public Object parseValueForDatabaseWrite(String value) throws IllegalArgumentException {
		Long dateValue = (Long) dateToLongParser.parseValueForDatabaseWrite(value);
		return new Date(dateValue);
	}
	
	@Override
	public String parseValueForDatabaseRead(String value)
			throws IllegalArgumentException {
		return value;
	}

}
