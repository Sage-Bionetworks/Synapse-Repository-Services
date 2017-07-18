package org.sagebionetworks.repo.model.table.parser;

import org.sagebionetworks.repo.model.table.AbstractDouble;

public class DoubleTypeParser extends AbstractValueParser {
	
	@Override
	public Object parseValueForDatabaseWrite(String value)
			throws IllegalArgumentException {
		AbstractDouble type = AbstractDouble.lookupType(value);
		return type.getEnumerationValue();
	}

	@Override
	public String parseValueForDatabaseRead(String value)
			throws IllegalArgumentException {
		return value;
	}

}
