package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
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

}
