package org.sagebionetworks.repo.model.table.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class JsonParserTest {
	
	private JsonParser jsonParser = new JsonParser();

	@Test
	public void testParseValueForDatabaseWriteWithJsonObject() {
		String result = (String) jsonParser.parseValueForDatabaseWrite("{\"foo\": 123, \"bar\": \"abc\"}");
		
		assertEquals("{\"foo\":123,\"bar\":\"abc\"}", result);
	}
	
	@Test
	public void testParseValueForDatabaseWriteWithJsonArray() {
		String result = (String) jsonParser.parseValueForDatabaseWrite("[{\"foo\": 123, \"bar\": \"abc\"}, {\"foo\": \"bar\"}]");
		
		assertEquals("[{\"foo\":123,\"bar\":\"abc\"},{\"foo\":\"bar\"}]", result);
	}
	
	@Test
	public void testParseValueForDatabaseWriteWithInvaliJsonObject() {
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			jsonParser.parseValueForDatabaseWrite("Not a json object or array");
		}).getMessage();
		
		assertEquals("Invalid JSON object or array: A JSONObject text must begin with '{' at 1 [character 2 line 1], A JSONArray text must start with '[' at 1 [character 2 line 1]", result);
	}
	
	@Test
	public void testParseValueForDatabaseRead() {
		String result = jsonParser.parseValueForDatabaseRead("{\"foo\": 123, \"bar\": \"abc\"}");
		
		assertEquals("{\"foo\": 123, \"bar\": \"abc\"}", result);
	}

}
