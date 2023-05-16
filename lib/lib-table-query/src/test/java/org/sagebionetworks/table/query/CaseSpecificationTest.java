package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.CaseSpecification;

public class CaseSpecificationTest {

	
	@Test
	public void testSimpleCase() throws ParseException {
		CaseSpecification element = new TableQueryParser("CASE FILE_TYPE WHEN 'raw' THEN 1 ELSE 0 END").caseSpecification();
		assertEquals("NULLIF('one','two')", element.toSql());
	}
}
