package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.CastSpecification;

public class CastSpecificationTest {

	
	@Test
	public void testCastSpecification() throws ParseException {
		CastSpecification element = new TableQueryParser("cast(foo as string)").castSpecification();
		assertEquals("CAST(foo AS STRING)", element.toSql());
	}
	
	@Test
	public void testCastSpecificationWithColumnId() throws ParseException {
		CastSpecification element = new TableQueryParser("cast(foo as 789)").castSpecification();
		assertEquals("CAST(foo AS 789)", element.toSql());
	}
}
