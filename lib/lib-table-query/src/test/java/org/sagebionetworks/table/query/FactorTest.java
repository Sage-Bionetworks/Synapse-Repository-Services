package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.Factor;

public class FactorTest {

	@Test
	public void testNoSign() throws ParseException{
		Factor element = new TableQueryParser("123").factor();
		assertEquals("123", element.toSql());
	}
	
	@Test
	public void testWithSignNegative() throws ParseException{
		Factor element = new TableQueryParser("-123").factor();
		assertEquals("-123", element.toSql());
	}
	
	@Test
	public void testWithSignPositive() throws ParseException{
		Factor element = new TableQueryParser("+123").factor();
		assertEquals("+123", element.toSql());
	}

	@Test
	public void testGetChildren() throws ParseException{
		Factor element = new TableQueryParser("+123").factor();
		assertEquals(Collections.singleton(element.getNumericPrimary()), element.getChildren());
	}
	
}
