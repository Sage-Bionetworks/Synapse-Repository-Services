package org.sagebionetworks.repo.model.table.parser;

import org.sagebionetworks.repo.model.table.ValueParser;

public class StringParser implements ValueParser {

	@Override
	public Object parseValueForDatabaseWrite(String value) throws IllegalArgumentException {
		return value;
	}
	
	@Override
	public String parseValueForDatabaseRead(String value)
			throws IllegalArgumentException {
		return value;
	}

}
