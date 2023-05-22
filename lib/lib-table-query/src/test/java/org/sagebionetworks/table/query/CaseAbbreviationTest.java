package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.CaseAbbreviation;

public class CaseAbbreviationTest {

	@Test
	public void testCaseAbbreviationWithNullIf() throws ParseException {
		CaseAbbreviation element = new TableQueryParser("NULLIF ('one','two')").caseAbbreviation();
		assertEquals("NULLIF('one','two')", element.toSql());
	}
	
	@Test
	public void testCaseAbbreviationWithCoalesce() throws ParseException {
		CaseAbbreviation element = new TableQueryParser("COALESCE(NULL,'one','two','three',NULL)").caseAbbreviation();
		assertEquals("COALESCE(NULL,'one','two','three',NULL)", element.toSql());
	}
}
