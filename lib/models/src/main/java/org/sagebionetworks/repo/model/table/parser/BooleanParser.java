package org.sagebionetworks.repo.model.table.parser;


public class BooleanParser extends AbstractValueParser {

	@Override
	public Object parseValueForDatabaseWrite(String value) throws IllegalArgumentException {
		if (Boolean.TRUE.toString().equalsIgnoreCase(value)) {
			return Boolean.TRUE;
		} else if (Boolean.FALSE.toString().equalsIgnoreCase(value)) {
			return Boolean.FALSE;
		} 
		throw new IllegalArgumentException("Not a boolean: "+value);
	}

	@Override
	public String parseValueForDatabaseRead(String value)
			throws IllegalArgumentException {
		if("0".equals(value)){
			return Boolean.FALSE.toString();
		}else if("1".equals(value)){
			return Boolean.TRUE.toString();
		}else {
			return parseValueForDatabaseWrite(value).toString();
		}
	}

}
