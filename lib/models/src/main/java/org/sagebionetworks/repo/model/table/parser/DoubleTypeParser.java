package org.sagebionetworks.repo.model.table.parser;

import org.sagebionetworks.repo.model.table.AbstractDoubles;

public class DoubleTypeParser extends AbstractValueParser {
	
	@Override
	public Object parseValueForDatabaseWrite(String value)
			throws IllegalArgumentException {
		AbstractDoubles type = AbstractDoubles.lookupValue(value);
		return type.getEnumerationValue();
	}

	@Override
	public String parseValueForDatabaseRead(String value)
			throws IllegalArgumentException {
		return value;
	}

}
