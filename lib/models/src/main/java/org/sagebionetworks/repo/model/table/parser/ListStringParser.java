package org.sagebionetworks.repo.model.table.parser;

import org.json.JSONArray;
import org.json.JSONException;
import org.sagebionetworks.repo.model.table.ColumnConstants;
import org.sagebionetworks.repo.model.table.ValueParser;

/**
 * Parser for JSON List (e.g. ["a","b", "c"] or [1,2,3] )
 */
public class ListStringParser extends AbstractValueParser{
	private ValueParser individualElementParser;

	/**
	 *
	 * @param individualElementParser parser that will be applied to each element in the list
	 */
	public ListStringParser(ValueParser individualElementParser){
		this.individualElementParser = individualElementParser;
	}

	@Override
	public Object parseValueForDatabaseWrite(String value) throws IllegalArgumentException {
		try {
			JSONArray parsed = new JSONArray(value);
			JSONArray toDatabase = new JSONArray();
			if(parsed.length() > ColumnConstants.MAX_ALLOWED_LIST_LENGTH){
				throw new IllegalArgumentException("value can not exceed " + ColumnConstants.MAX_ALLOWED_LIST_LENGTH + " elements in list: " + value);
			}
			for(int i = 0; i < parsed.length(); i++){
				if(parsed.isNull(i)){
					throw new IllegalArgumentException("null value is not allowed");
				}

				String element = parsed.getString(i);
				Object parsedObject = individualElementParser.parseValueForDatabaseWrite(element);

				toDatabase.put(parsedObject);
			}
			return toDatabase.toString();
		} catch (JSONException e){
			throw new IllegalArgumentException("Not a JSON Array: " + value);
		}
	}

	@Override
	public String parseValueForDatabaseRead(String value) throws IllegalArgumentException {
		// the value should already have be a JSON string when retrieved from the database so nothing left to do
		return value;
	}
}
