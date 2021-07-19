package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.RegularIdentifier;

public class RegularIdentifierTest {
	
	@Test
	public void testRegularIdentifier() throws ParseException{
		RegularIdentifier element = new TableQueryParser("C_123").regularIdentifier();
		assertEquals("C_123", element.toSql());
	}
	
	@Test
	public void testRegularIdentifierNotExponent() throws ParseException{
		// This must match a regular identifier and not part of an exponent such as 1.0e123
		RegularIdentifier element = new TableQueryParser("e123").regularIdentifier();
		assertEquals("e123", element.toSql());
	}
	
	@Test
	public void testRegularIdentifierNumericStart() throws ParseException{
		assertThrows(ParseException.class, ()->{
			new TableQueryParser("1bay").regularIdentifier();
		});
		
	}
	
	@Test
	public void testRegularIdentifierUnderScoreStart() throws ParseException{
		RegularIdentifier element = new TableQueryParser("_abcd").regularIdentifier();
		assertEquals("_abcd", element.toSql());
	}

	@Test
	public void testGetChildren() throws ParseException {
		RegularIdentifier element = new TableQueryParser("_abcd").regularIdentifier();
		assertEquals(Collections.emptyList(), element.getChildren());
	}
}
