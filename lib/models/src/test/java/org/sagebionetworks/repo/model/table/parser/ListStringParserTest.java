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
	ListStringParser listStringParser = new ListStringParser(new StringParser());

	@Test
	public void testParseValueForDatabaseWrite_listTooLong(){
		StringJoiner joiner = new StringJoiner(",", "[", "]");
		for(int i = 0; i < ColumnConstants.MAX_ALLOWED_LIST_LENGTH + 1; i++){
			joiner.add("str" + i);
		}

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->{
			listStringParser.parseValueForDatabaseWrite(joiner.toString());
		});

		assertTrue(exception.getMessage().contains("value can not exceed 100 elements in list: "));
	}

	@Test
	public void testParseValueForDatabaseWrite_containsNullValues(){
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->{
			listStringParser.parseValueForDatabaseWrite("[\"str\", null, \"str2\"]");
		});

		assertEquals("null value is not allowed", exception.getMessage());
	}

	@Test
	public void testParseValueForDatabaseWrite_NotJsonArray(){
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->{
			listStringParser.parseValueForDatabaseWrite("what is json?");
		});

		assertEquals("Not a JSON Array: what is json?", exception.getMessage());
	}

	@Test
	public void testParseValueForDatabaseWrite_happyCase(){
		assertEquals("[\"str\",\"str2\"]", listStringParser.parseValueForDatabaseWrite("[\"str\", \"str2\"]"));
	}
}