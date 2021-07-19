package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.OrderByClause;
import org.sagebionetworks.table.query.model.SortSpecificationList;
import org.sagebionetworks.table.query.util.SqlElementUntils;

public class OrderByClauseTest {

	@Test
	public void testToSQl() throws ParseException {
		SortSpecificationList list = SqlElementUntils.createSortSpecificationList("foo, bar");
		OrderByClause element = new OrderByClause(list);
		assertEquals("ORDER BY foo, bar", element.toString());
	}

	@Test
	public void testGetChildren() throws ParseException {
		OrderByClause element = new TableQueryParser("ORDER BY foo, bar").orderByClause();
		assertEquals(Collections.singleton(element.getSortSpecificationList()), element.getChildren());
	}

}
