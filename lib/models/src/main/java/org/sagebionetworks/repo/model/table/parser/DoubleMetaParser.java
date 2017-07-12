package org.sagebionetworks.repo.model.table.parser;

public class DoubleMetaParser extends AbstractValueParser {
	
	enum Types {
		Infintiy,
		-Infinity,
		
	}

	@Override
	public Object parseValueForDatabaseWrite(String value)
			throws IllegalArgumentException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String parseValueForDatabaseRead(String value)
			throws IllegalArgumentException {
		return value;
	}

}
