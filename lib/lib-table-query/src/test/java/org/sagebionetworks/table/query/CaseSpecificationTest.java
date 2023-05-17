package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.CaseSpecification;

public class CaseSpecificationTest {

	
	@Test
	public void testSimpleCase() throws ParseException {
		CaseSpecification element = new TableQueryParser("CASE FILE_TYPE WHEN 'raw' THEN 1 ELSE 0 END").caseSpecification();
		assertEquals("CASE FILE_TYPE WHEN 'raw' THEN 1 ELSE 0 END", element.toSql());
	}
	
	@Test
	public void testSearchedCase() throws ParseException {
		CaseSpecification element = new TableQueryParser("CASE WHEN FOO > BAR THEN 1 ELSE 0 END").caseSpecification();
		assertEquals("CASE WHEN FOO > BAR THEN 1 ELSE 0 END", element.toSql());
	}
}
