package org.sagebionetworks.repo.model.table.parser;

import org.sagebionetworks.repo.model.table.ValueParser;

public class BooleanParser implements ValueParser {

	@Override
	public Object parseValueForDatabaseWrite(String value) throws IllegalArgumentException {
		return Boolean.parseBoolean(value);
	}

	@Override
	public String parseValueForDatabaseRead(String value)
			throws IllegalArgumentException {
		return value;
	}
}
