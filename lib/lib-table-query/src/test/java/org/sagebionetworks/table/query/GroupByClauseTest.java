package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.GroupByClause;
import org.sagebionetworks.table.query.model.GroupingColumnReferenceList;
import org.sagebionetworks.table.query.util.SqlElementUntils;

public class GroupByClauseTest {
	
	@Test
	public void testToSQL() throws ParseException{
		GroupingColumnReferenceList list = SqlElementUntils.createGroupingColumnReferenceList("one, two");
		GroupByClause element = new GroupByClause(list);
		assertEquals("GROUP BY one, two", element.toString());
	}

	@Test
	public void testGetChildren() throws ParseException{
		GroupByClause element = new TableQueryParser("GROUP BY one, two").groupByClause();
		assertEquals(Collections.singleton(element.getGroupingColumnReferenceList()), element.getChildren());
	}
}
