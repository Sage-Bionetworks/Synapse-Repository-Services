package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.GeneralLiteral;

public class GeneralLiteralTest {

	@Test
	public void testGeneralLiteral() throws ParseException{
		GeneralLiteral element = new TableQueryParser("'in single quotes'").generalLiteral();
		assertEquals("'in single quotes'", element.toSql());
		assertEquals("in single quotes", element.toSqlWithoutQuotes());
		assertTrue(element.hasQuotesRecursive());
	}
	
	@Test
	public void testGeneralLiteralEscapeQuotes() throws ParseException{
		GeneralLiteral element = new TableQueryParser("'Batman''s car'").generalLiteral();
		assertEquals("'Batman''s car'", element.toSql());
		// single quotes within the string must not be escaped.
		assertEquals("Batman's car", element.toSqlWithoutQuotes());
	}
	
	@Test
	public void testInterval() throws ParseException{
		GeneralLiteral element = new TableQueryParser("INTERVAL 1 second").generalLiteral();
		assertEquals("INTERVAL 1 SECOND", element.toSql());
	}
	
	@Test
	public void testGetChildren() throws ParseException{
		GeneralLiteral element = new TableQueryParser("INTERVAL 1 second").generalLiteral();
		assertEquals(Collections.singleton(element.getChild()), element.getChildren());
	}
	
}
