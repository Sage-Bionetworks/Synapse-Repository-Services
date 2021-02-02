package org.sagebionetworks.repo.model.table.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.StringJoiner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.table.ColumnConstants;
import org.sagebionetworks.repo.model.table.ValueParser;

class ListStringParserTest {
	ListStringParser listStringParser = new ListStringParser(new TestStringParser(), true);


	/////////////////////////////////////////////////////////
	// test main helper function that the class relies upon
	/////////////////////////////////////////////////////////

	@Test
	public void testApplyFunctionOnParsedJsonElements_listTooLong(){
		StringJoiner joiner = new StringJoiner(",", "[", "]");
		for(int i = 0; i < ColumnConstants.MAX_ALLOWED_LIST_LENGTH + 1; i++){
			joiner.add("str" + i);
		}

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->{
			listStringParser.applyFunctionOnParsedJsonElements(joiner.toString(), this::testFunction);
		});

		assertTrue(exception.getMessage().contains("value can not exceed 100 elements in list: "));
	}

	@Test
	public void testApplyFunctionOnParsedJsonElements_EmptyJSONArrayList(){
		String result = listStringParser.applyFunctionOnParsedJsonElements("[]", this::testFunction);
		assertNull( result);

		result = listStringParser.applyFunctionOnParsedJsonElements("[        ]", this::testFunction);
		assertNull( result);
	}

	@Test
	public void testApplyFunctionOnParsedJsonElements_containsNullValues(){
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->{
			listStringParser.applyFunctionOnParsedJsonElements("[\"str\", null, \"str2\"]", this::testFunction);
		});

		assertEquals("null value is not allowed", exception.getMessage());
	}

	@Test
	public void testApplyFunctionOnParsedJsonElements_NotJsonArray(){
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->{
			listStringParser.applyFunctionOnParsedJsonElements("what is json?", this::testFunction);
		});

		assertEquals("Not a JSON Array: what is json?", exception.getMessage());
	}

	@Test
	public void testApplyFunctionOnParsedJsonElements_happyCase(){
		assertEquals("[\"testFunc=str\",\"testFunc=str2\"]",
				listStringParser.applyFunctionOnParsedJsonElements("[\"str\", \"str2\"]", this::testFunction));
	}

	//////////////////////////////////
	// test exposed interface methods
	//////////////////////////////////

	@Test
	public void testParseValueForDatabaseWrite(){
		assertEquals("[\"write=str\",\"write=str2\"]", listStringParser.parseValueForDatabaseWrite("[\"str\",\"str2\"]"));
	}

	@Test
	public void testParseValueForDatabaseRead_parseForReadTrue(){
		listStringParser = new ListStringParser(new TestStringParser(), true);
		assertEquals("[\"read=str\",\"read=str2\"]", listStringParser.parseValueForDatabaseRead("[\"str\",\"str2\"]"));
	}

	@Test
	public void testParseValueForDatabaseRead_parseForReadFalse(){
		listStringParser = new ListStringParser(new TestStringParser(), false);
		assertEquals("[\"str\",\"str2\"]", listStringParser.parseValueForDatabaseRead("[\"str\",\"str2\"]"));
	}

	/////////////////
	// helpers
	////////////////

	/**
	 * Parser that append different prefixes for the parseValueForDatabaseWrite and parseValueForDatabaseRead functions
	 */
	private class TestStringParser implements ValueParser{

		@Override
		public Object parseValueForDatabaseWrite(String value) throws IllegalArgumentException {
			return "write=" + value;
		}

		@Override
		public String parseValueForDatabaseRead(String value) throws IllegalArgumentException {
			return "read=" + value;
		}

		@Override
		public boolean isOfType(String value) {
			return true;
		}
	}

	//helper function used to test ApplyFunctionOnParsedJsonElements()
	private Object testFunction(String value){
		return "testFunc=" + value;
	}
}