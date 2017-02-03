package org.sagebionetworks.repo.model.table.parser;

import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ValueParser;

public class EntityIdParser implements ValueParser {

	@Override
	public Object parseValueForDatabaseWrite(String value) throws IllegalArgumentException {
		return KeyFactory.stringToKey(value);
	}
	
	@Override
	public String parseValueForDatabaseRead(String value)
			throws IllegalArgumentException {
		return KeyFactory.keyToString(Long.parseLong(value));
	}

}
