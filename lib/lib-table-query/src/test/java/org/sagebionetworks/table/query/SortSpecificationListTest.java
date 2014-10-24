package org.sagebionetworks.table.query;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.sagebionetworks.table.query.model.SortSpecification;
import org.sagebionetworks.table.query.model.SortSpecificationList;
import org.sagebionetworks.table.query.util.SqlElementUntils;

public class SortSpecificationListTest {
	
	@Test
	public void testToSQLOne() throws ParseException{
		List<SortSpecification> list = SqlElementUntils.createSortSpecifications("foo asc");
		SortSpecificationList element= new SortSpecificationList(list);
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder, null);
		assertEquals("foo ASC", builder.toString());
	}
	
	@Test
	public void testToSQLMore() throws ParseException{
		List<SortSpecification> list = SqlElementUntils.createSortSpecifications("foo asc","bar desc");
		SortSpecificationList element= new SortSpecificationList(list);
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder, null);
		assertEquals("foo ASC, bar DESC", builder.toString());
	}

	
	@Test
	public void testToSQLMoreNoOrderSpec() throws ParseException{
		List<SortSpecification> list = SqlElementUntils.createSortSpecifications("foo","bar");
		SortSpecificationList element= new SortSpecificationList(list);
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder, null);
		assertEquals("foo, bar", builder.toString());
	}
}
