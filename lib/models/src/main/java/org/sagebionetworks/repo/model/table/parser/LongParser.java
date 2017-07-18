package org.sagebionetworks.repo.model.table.parser;


public class LongParser extends AbstractValueParser {

	@Override
	public Object parseValueForDatabaseWrite(String value) throws IllegalArgumentException {
		return Long.parseLong(value);
	}

	@Override
	public String parseValueForDatabaseRead(String value)
			throws IllegalArgumentException {
		return value;
	}
}
