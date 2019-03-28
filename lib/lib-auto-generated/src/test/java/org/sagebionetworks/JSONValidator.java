package org.sagebionetworks;

import java.io.IOException;


import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JSONValidator {
	
	/**
	 * Fully parse a string to validate JSON compliance
	 * 
	 * @param json String to validate
	 * @return true if valid; otherwise false
	 * @throws JsonParseException
	 */
	public static void validateJSON(final String json) throws IOException {
		final JsonParser parser = new ObjectMapper().getFactory().createParser(json);
		while (parser.nextToken() != null) {}
	}
}
