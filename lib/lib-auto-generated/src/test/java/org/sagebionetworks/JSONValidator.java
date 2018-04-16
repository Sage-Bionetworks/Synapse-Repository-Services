package org.sagebionetworks;

import java.io.IOException;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;

public class JSONValidator {
	
	/**
	 * Fully parse a string to validate JSON compliance
	 * 
	 * @param json String to validate
	 * @return true if valid; otherwise false
	 * @throws JsonParseException
	 */
	public static void validateJSON(final String json) throws IOException {
		final JsonParser parser = new ObjectMapper().getJsonFactory().createJsonParser(json);			
		while (parser.nextToken() != null) {}
	}
}
