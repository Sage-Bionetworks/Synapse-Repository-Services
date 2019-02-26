package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sagebionetworks.table.query.model.CharacterStringLiteral;

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

}
