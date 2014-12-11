package org.sagebionetworks.table.query;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.table.query.model.OrderingSpecification;
import org.sagebionetworks.table.query.model.SortKey;
import org.sagebionetworks.table.query.model.SortSpecification;
import org.sagebionetworks.table.query.util.SqlElementUntils;

public class SortSpecificationTest {
	
	@Test
	public void testToSQLNoOrding() throws ParseException{
		SortKey sortKey = SqlElementUntils.createSortKey("foo.bar");
		OrderingSpecification orderingSpecification = null;
		SortSpecification element= new SortSpecification(sortKey, orderingSpecification);
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder, null);
		assertEquals("foo.bar", builder.toString());
	}
	
	@Test
	public void testToSQLASC() throws ParseException{
		SortKey sortKey = SqlElementUntils.createSortKey("foo asc");
		OrderingSpecification orderingSpecification = OrderingSpecification.ASC;
		SortSpecification element= new SortSpecification(sortKey, orderingSpecification);
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder, null);
		assertEquals("foo ASC", builder.toString());
	}
	
	@Test
	public void testToSQLDESC() throws ParseException{
		SortKey sortKey = SqlElementUntils.createSortKey("foo desc");
		OrderingSpecification orderingSpecification = OrderingSpecification.DESC;;
		SortSpecification element= new SortSpecification(sortKey, orderingSpecification);
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder, null);
		assertEquals("foo DESC", builder.toString());
	}

}
