package org.sagebionetworks.table.query;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.BooleanFactor;
import org.sagebionetworks.table.query.model.BooleanTerm;
import org.sagebionetworks.table.query.util.SqlElementUtils;

public class BooleanTermTest {

	@Test
	public void testBooleanTermToSQLSingle() throws ParseException{
		List<BooleanFactor> list = SqlElementUtils.createBooleanFactors("foo=1");
		BooleanTerm element = new BooleanTerm(list);
		assertEquals("foo = 1", element.toString());
	}
	
	@Test
	public void testBooleanTermNegativeToSQLSingle() throws ParseException {
		List<BooleanFactor> list = SqlElementUtils.createBooleanFactors("foo=-1");
		BooleanTerm element = new BooleanTerm(list);
		assertEquals("foo = -1", element.toString());
	}

	@Test
	public void testBooleanTermNegativeDoubleToSQLSingle() throws ParseException {
		List<BooleanFactor> list = SqlElementUtils.createBooleanFactors("foo>-.1");
		BooleanTerm element = new BooleanTerm(list);
		assertEquals("foo > -0.1", element.toString());
	}

	@Test
	public void testBooleanTermToSQLMultiple() throws ParseException{
		List<BooleanFactor> list = SqlElementUtils.createBooleanFactors("foo=1", "bar=2");
		BooleanTerm element = new BooleanTerm(list);
		assertEquals("foo = 1 AND bar = 2", element.toString());
	}
	
	@Test
	public void testGetChidren() throws ParseException {
		BooleanTerm element = new TableQueryParser("foo=1 and bar=2").booleanTerm();
		assertEquals(new LinkedList<>(element.getAndBooleanFactors()), element.getChildren());
	}
}
