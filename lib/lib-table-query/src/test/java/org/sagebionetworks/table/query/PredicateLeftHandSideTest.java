package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.CastSpecification;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.MySqlFunction;
import org.sagebionetworks.table.query.model.PredicateLeftHandSide;
import org.sagebionetworks.table.query.util.SqlElementUtils;

public class PredicateLeftHandSideTest {

	@Test
	public void testPredicateLeftHandSideWithColumnReference() throws ParseException {
		ColumnReference columnReference = SqlElementUtils.createColumnReference("foo");
		PredicateLeftHandSide element = new PredicateLeftHandSide(columnReference);
		assertEquals("foo", element.toString());
	}

	@Test
	public void testPredicateLeftHandSideWithMySqlFunction() throws ParseException {
		MySqlFunction mySqlFunction = new TableQueryParser("JSON_OVERLAPS(foo, '[1,2]')").mysqlFunction();
		PredicateLeftHandSide element = new PredicateLeftHandSide(mySqlFunction);
		assertEquals("JSON_OVERLAPS(foo,'[1,2]')", element.toString());
	}
	
	@Test
	public void testPredicateLeftHandSideWithCastSpecification() throws ParseException {
		CastSpecification castSpecification = new TableQueryParser("CAST(foo AS STRING)").castSpecification();
		PredicateLeftHandSide element = new PredicateLeftHandSide(castSpecification);
		assertEquals("CAST(foo AS STRING)", element.toString());
	}

}
