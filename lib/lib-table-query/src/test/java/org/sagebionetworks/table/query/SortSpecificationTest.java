package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.table.query.model.OrderingSpecification;
import org.sagebionetworks.table.query.model.SortKey;
import org.sagebionetworks.table.query.model.SortSpecification;

public class SortSpecificationTest {
	
	@Test
	public void testToSQLNoOrding() throws ParseException{
		SortKey sortKey = new TableQueryParser("foo.bar").sortKey();
		OrderingSpecification orderingSpecification = null;
		SortSpecification element= new SortSpecification(sortKey, orderingSpecification);
		assertEquals("foo.bar", element.toString());
	}
	
	@Test
	public void testToSQLASC() throws ParseException{
		SortKey sortKey = new TableQueryParser("foo asc").sortKey();
		OrderingSpecification orderingSpecification = OrderingSpecification.ASC;
		SortSpecification element= new SortSpecification(sortKey, orderingSpecification);
		assertEquals("foo ASC", element.toString());
	}
	
	@Test
	public void testToSQLDESC() throws ParseException{
		SortKey sortKey = new TableQueryParser("foo desc").sortKey();
		OrderingSpecification orderingSpecification = OrderingSpecification.DESC;;
		SortSpecification element= new SortSpecification(sortKey, orderingSpecification);
		assertEquals("foo DESC", element.toString());
	}

	@Test
	public void testToSQLAggregate() throws ParseException {
		SortKey sortKey = new TableQueryParser("avg(foo)").sortKey();
		OrderingSpecification orderingSpecification = OrderingSpecification.ASC;
		SortSpecification element = new SortSpecification(sortKey, orderingSpecification);
		assertEquals("AVG(foo) ASC", element.toString());
	}
}
