package org.sagebionetworks.table.query;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.Element;
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
	
	@Test
	public void testTermArithmeticDiv() throws ParseException{
		Term term = new TableQueryParser("5 div 2").term();
		assertEquals("5 DIV 2", term.toSql());
	}
	
	@Test
	public void testGetChildren() throws ParseException {
		Term element = new TableQueryParser("5 div 2 div 5").term();
		List<Element> list = new LinkedList<Element>();
		list.add(element.getFactor());
		list.addAll(element.getPrimeList());
		assertEquals(list, element.getChildren());
	}
}
