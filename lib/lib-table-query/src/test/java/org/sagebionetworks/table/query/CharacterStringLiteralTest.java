package org.sagebionetworks.table.query;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.table.ColumnConstants;
import org.sagebionetworks.table.query.model.CharacterStringLiteral;

import com.google.common.base.Strings;

public class CharacterStringLiteralTest {

	/**
	 * Character String Literals are surrounded by single quotes.
	 * 
	 * @throws ParseException
	 */
	@Test
	public void testCharacterStringLiteral() throws ParseException{
		CharacterStringLiteral element = new TableQueryParser("'This is a long string in quotes'").characterStringLiteral();
		assertEquals("This is a long string in quotes", element.toSqlWithoutQuotes());
		assertEquals("'This is a long string in quotes'", element.toSql());
		assertTrue(element.hasQuotes());
	}

	@Test
	public void testCharacterStringLiteral_exeedSize() throws ParseException{
		String maxString = Strings.repeat("a", ColumnConstants.MAX_ALLOWED_STRING_SIZE.intValue() + 1);
		assertThrows(IllegalArgumentException.class, () -> new CharacterStringLiteral(maxString));
	}

	/**
	 * Single quotes within a Character String Literal are escaped with single quotes
	 * @throws ParseException
	 */
	@Test
	public void testCharacterStringLiteralEscape() throws ParseException{
		CharacterStringLiteral element = new TableQueryParser("'A string ''within a'' string.'").characterStringLiteral();
		assertEquals("A string 'within a' string.", element.toSqlWithoutQuotes());
		assertEquals("'A string ''within a'' string.'", element.toSql());
	}

	@Test
	public void testCharacterStringLiteralEmptyString() throws ParseException{
		CharacterStringLiteral element = new TableQueryParser("''").characterStringLiteral();
		assertEquals("", element.toSqlWithoutQuotes());
	}
	
	@Test
	public void testGetChidren() throws ParseException {
		CharacterStringLiteral element = new TableQueryParser("'A string ''within a'' string.'").characterStringLiteral();
		assertEquals(Collections.emptyList(), element.getChildren());
	}

}
