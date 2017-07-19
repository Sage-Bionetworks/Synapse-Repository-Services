package org.sagebionetworks.repo.model.table.parser;

import org.sagebionetworks.repo.model.table.AbstractDouble;


public class DoubleParser extends AbstractValueParser {

	@Override
	public Object parseValueForDatabaseWrite(String value) throws IllegalArgumentException {
		if(value == null){
			throw new IllegalArgumentException("For input string: \""+value+"\"");
		}
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException e) {
			// Try to match it to NaN or infinity.
			AbstractDouble meta = AbstractDouble.lookupType(value);
			return meta.getDoubleValue();
		}
	}
	
	@Override
	public String parseValueForDatabaseRead(String value)
			throws IllegalArgumentException {
		return value;
	}

}
