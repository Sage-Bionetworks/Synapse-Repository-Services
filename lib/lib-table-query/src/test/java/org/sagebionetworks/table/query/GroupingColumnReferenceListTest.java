package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.sagebionetworks.table.query.model.GroupingColumnReference;
import org.sagebionetworks.table.query.model.GroupingColumnReferenceList;
import org.sagebionetworks.table.query.util.SqlElementUntils;

public class GroupingColumnReferenceListTest {

	
	@Test
	public void testToSQLOne() throws ParseException{
		List<GroupingColumnReference> list = SqlElementUntils.createGroupingColumnReferences("lhs.rhs");
		GroupingColumnReferenceList element = new GroupingColumnReferenceList(list);
		assertEquals("lhs.rhs", element.toString());
	}
	
	@Test
	public void testToSQLMore() throws ParseException{
		List<GroupingColumnReference> list = SqlElementUntils.createGroupingColumnReferences("lhs.rhs","mid","\"last\"");
		GroupingColumnReferenceList element = new GroupingColumnReferenceList(list);
		assertEquals("lhs.rhs, mid, \"last\"", element.toString());
	}
}
