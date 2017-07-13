package org.sagebionetworks.repo.model.table.parser;

import org.sagebionetworks.repo.model.table.ValueParser;

/**
 * A parser that can handle any type of long value.
 *
 */
public class AllLongTypeParser extends AbstractValueParser {
	
	ValueParser[] parsers = new ValueParser[]{
			new LongParser(),
			new EntityIdParser(),
			new DateToLongParser(),
	};
	
	@Override
	public Object parseValueForDatabaseWrite(String value)
			throws IllegalArgumentException {
		// Try each type.
		for(ValueParser parser: parsers){
			if(parser.isOfType(value)){
				return parser.parseValueForDatabaseWrite(value);
			}
		}
		throw new NumberFormatException("For input string: \""+value+"\"");
	}

	@Override
	public String parseValueForDatabaseRead(String value)
			throws IllegalArgumentException {
		throw new UnsupportedOperationException("Not supported");
	}

}
