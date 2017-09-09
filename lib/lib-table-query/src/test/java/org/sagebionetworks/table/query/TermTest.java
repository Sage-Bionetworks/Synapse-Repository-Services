package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.table.query.model.Factor;
import org.sagebionetworks.table.query.model.Term;

public class TermTest {
	
	@Test
	public void testFactorOnly() throws ParseException{
		Factor factor = new TableQueryParser("foo").factor();
		Term term = new Term(factor);
		assertEquals("foo", term.toSql());
	}
	
	@Test
	public void testTermArithmeticParse() throws ParseException{
		Term term = new TableQueryParser("foo/100*5/12").term();
		assertEquals("foo/100*5/12", term.toSql());
	}
}
