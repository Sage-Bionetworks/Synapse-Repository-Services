package org.sagebionetworks.repo.model.table.parser;

import java.util.function.Function;

import org.json.JSONArray;
import org.json.JSONException;
import org.sagebionetworks.repo.model.table.ColumnConstants;
import org.sagebionetworks.repo.model.table.ValueParser;

/**
 * Parser for JSON List (e.g. ["a","b", "c"] or [1,2,3] )
 */
public class ListStringParser extends AbstractValueParser{
	private ValueParser individualElementParser;
	private boolean parseForRead;

	/**
	 * @param individualElementParser parser that will be applied to each element in the list
	 * @param parseForRead determines whether requires re-parsing when read from database via {@link #parseValueForDatabaseRead(String)}
	 */
	public ListStringParser(ValueParser individualElementParser, boolean parseForRead){
		this.individualElementParser = individualElementParser;
		this.parseForRead = parseForRead;
	}

	@Override
	public Object parseValueForDatabaseWrite(String value) throws IllegalArgumentException {
		return applyFunctionOnParsedJsonElements(value, individualElementParser::parseValueForDatabaseWrite);
	}

	@Override
	public String parseValueForDatabaseRead(String value) throws IllegalArgumentException {
		if (parseForRead) {
			return applyFunctionOnParsedJsonElements(value, individualElementParser::parseValueForDatabaseRead);
		}
		return value;
	}

	String applyFunctionOnParsedJsonElements(String value, Function<String,Object> parserFunction){
		try {
			JSONArray parsed = new JSONArray(value);
			if(parsed.length() == 0){
				return null;
			}
			if(parsed.length() > ColumnConstants.MAX_ALLOWED_LIST_LENGTH){
				throw new IllegalArgumentException("value can not exceed " + ColumnConstants.MAX_ALLOWED_LIST_LENGTH + " elements in list: " + value);
			}
			for(int i = 0; i < parsed.length(); i++){
				if(parsed.isNull(i)){
					throw new IllegalArgumentException("null value is not allowed");
				}

				String element = parsed.getString(i);
				Object parsedObject = parserFunction.apply(element);

				parsed.put(i, parsedObject);
			}
			return parsed.toString();
		} catch (JSONException e){
			throw new IllegalArgumentException("Not a JSON Array: " + value);
		}
	}
}
