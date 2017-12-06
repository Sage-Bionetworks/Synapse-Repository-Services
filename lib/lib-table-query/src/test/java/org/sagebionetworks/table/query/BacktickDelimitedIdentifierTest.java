package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sagebionetworks.table.query.model.BacktickDelimitedIdentifier;

public class BacktickDelimitedIdentifierTest {

	@Test
	public void testDelimitedIdentifier() throws ParseException{
		BacktickDelimitedIdentifier element = new TableQueryParser("`has backtick quotes`").backtickDelimitedIdentifier();
		assertEquals("`has backtick quotes`", element.toSql());
		assertTrue(element.hasQuotes());
		assertEquals("has backtick quotes", element.toSqlWithoutQuotes());
	}
	
	@Test
	public void testDelimitedIdentifierContainsDoubleQuotes() throws ParseException{
		BacktickDelimitedIdentifier element = new TableQueryParser("`contains ``backtick`` quotes`").backtickDelimitedIdentifier();
		assertEquals("`contains ``backtick`` quotes`", element.toSql());
		assertTrue(element.hasQuotes());
		assertEquals("contains `backtick` quotes", element.toSqlWithoutQuotes());
	}
	
	@Test
	public void testDelimitedIdentifierEmptyString() throws ParseException{
		BacktickDelimitedIdentifier element = new TableQueryParser("``").backtickDelimitedIdentifier();
		assertEquals("``", element.toSql());
		assertTrue(element.hasQuotes());
		assertEquals("", element.toSqlWithoutQuotes());
	}
}
