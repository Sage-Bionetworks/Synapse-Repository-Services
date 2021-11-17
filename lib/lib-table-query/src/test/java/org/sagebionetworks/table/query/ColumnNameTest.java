package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.ColumnName;

public class ColumnNameTest {

	@Test
	public void testColumnNameWithIdentifier() throws ParseException {
		ColumnName columnName = new TableQueryParser("foo").columnName();
		assertEquals("foo", columnName.toSql());
	}
	
	@Test
	public void testColumnNameWithSpaceAndBacktick() throws ParseException {
		ColumnName columnName = new TableQueryParser("`has space`").columnName();
		assertEquals("`has space`", columnName.toSql());
	}
	
	@Test
	public void testColumnNameWithSpaceAndDoubleQuotes() throws ParseException {
		ColumnName columnName = new TableQueryParser("\"has space\"").columnName();
		assertEquals("\"has space\"", columnName.toSql());
	}
	
	/**
	 * A column name cannot be in single quotes.
	 * @throws ParseException
	 */
	@Test
	public void testColumnNameWithSpaceAndSingleQuotes() throws ParseException {
		String message = assertThrows(ParseException.class, ()->{
			new TableQueryParser("'has space'").columnName();
		}).getMessage();
		assertTrue(message.contains("Encountered \" \"\\'\" \"\\' \"\" "));
	}
	
}
