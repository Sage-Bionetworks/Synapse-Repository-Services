package org.sagebionetworks.repo.model.table.parser;

import org.sagebionetworks.util.doubles.DoubleUtils;


public class DoubleParser extends AbstractValueParser {

	@Override
	public Object parseValueForDatabaseWrite(String value) throws IllegalArgumentException {
		return DoubleUtils.fromString(value);
	}
	
	@Override
	public String parseValueForDatabaseRead(String value)
			throws IllegalArgumentException {
		return value;
	}

}
