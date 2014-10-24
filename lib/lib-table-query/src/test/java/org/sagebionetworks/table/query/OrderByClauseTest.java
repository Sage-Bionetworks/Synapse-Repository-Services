package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.table.query.model.OrderByClause;
import org.sagebionetworks.table.query.model.SortSpecificationList;
import org.sagebionetworks.table.query.util.SqlElementUntils;

public class OrderByClauseTest {
	
	@Test
	public void testToSQl() throws ParseException{
		SortSpecificationList list = SqlElementUntils.createSortSpecificationList("foo, bar");
		OrderByClause element= new OrderByClause(list);
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder, null);
		assertEquals("ORDER BY foo, bar", builder.toString());
	}

}
