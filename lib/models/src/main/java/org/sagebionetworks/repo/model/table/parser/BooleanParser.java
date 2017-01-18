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
		if("0".equals(value)){
			return "false";
		}else if("1".equals(value)){
			return "true";
		}else{
			return value;
		}
	}
}
