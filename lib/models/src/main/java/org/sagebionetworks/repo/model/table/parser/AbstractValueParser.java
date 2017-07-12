package org.sagebionetworks.repo.model.table.parser;

import org.sagebionetworks.repo.model.table.ValueParser;

public abstract class AbstractValueParser implements ValueParser {

	@Override
	public boolean isOfType(String value) {
		try{
			Object result = parseValueForDatabaseWrite(value);
			return result != null;
		}catch (IllegalArgumentException e){
			return false;
		}
	}
}
