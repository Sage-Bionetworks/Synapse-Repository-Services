package org.sagebionetworks;

import java.io.IOException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;

public class JSONValidator {
	
	private static JSONValidator instance;
	
	protected JSONValidator() {}
	
	/**
	 * Get the singleton instance of JSONValidator
	 * 
	 * @return the JSONValidator
	 */
	public static JSONValidator getValidator() {
		if (instance == null)
			instance = new JSONValidator();
		return instance;
	}
	
	/**
	 * Fully parse a string to validate JSON compliance
	 * 
	 * @param json String to validate
	 * @return true if valid; otherwise false
	 * @throws JsonParseException
	 */
	public static boolean isValidJSON(final String json) throws IOException {
		boolean valid = false;
		final JsonParser parser = new ObjectMapper().getJsonFactory().createJsonParser(json);			
		while (parser.nextToken() != null) {}
		valid = true;
		return valid;
	}
}
