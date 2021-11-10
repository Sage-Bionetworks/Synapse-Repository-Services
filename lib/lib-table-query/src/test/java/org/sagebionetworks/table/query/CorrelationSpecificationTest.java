package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.CorrelationSpecification;

public class CorrelationSpecificationTest {

	@Test
	public void testCorrelationSpecificationWithoutAs() throws ParseException {
		CorrelationSpecification spec = new TableQueryParser("foo").correlationSpecification();
		assertEquals("foo", spec.toSql());
	}
	
	@Test
	public void testCorrelationSpecificationWithAs() throws ParseException {
		CorrelationSpecification spec = new TableQueryParser("as foo").correlationSpecification();
		assertEquals("AS foo", spec.toSql());
	}
}
