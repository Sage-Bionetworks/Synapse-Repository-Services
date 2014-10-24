package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.sagebionetworks.table.query.model.BooleanFactor;
import org.sagebionetworks.table.query.model.BooleanTerm;
import org.sagebionetworks.table.query.util.SqlElementUntils;

public class BooleanTermTest {

	@Test
	public void testBooleanTermToSQLSingle() throws ParseException{
		List<BooleanFactor> list = SqlElementUntils.createBooleanFactors("foo=1");
		BooleanTerm element = new BooleanTerm(list);
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder, null);
		assertEquals("foo = 1", builder.toString());
	}
	
	@Test
	public void testBooleanTermNegativeToSQLSingle() throws ParseException {
		List<BooleanFactor> list = SqlElementUntils.createBooleanFactors("foo=-1");
		BooleanTerm element = new BooleanTerm(list);
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder, null);
		assertEquals("foo = -1", builder.toString());
	}

	@Test
	public void testBooleanTermNegativeDoubleToSQLSingle() throws ParseException {
		List<BooleanFactor> list = SqlElementUntils.createBooleanFactors("foo>-.1");
		BooleanTerm element = new BooleanTerm(list);
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder, null);
		assertEquals("foo > -.1", builder.toString());
	}

	@Test
	public void testBooleanTermToSQLMultiple() throws ParseException{
		List<BooleanFactor> list = SqlElementUntils.createBooleanFactors("foo=1", "bar=2");
		BooleanTerm element = new BooleanTerm(list);
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder, null);
		assertEquals("foo = 1 AND bar = 2", builder.toString());
	}
}
