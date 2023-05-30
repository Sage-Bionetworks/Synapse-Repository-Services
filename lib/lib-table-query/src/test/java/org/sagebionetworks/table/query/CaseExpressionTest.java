package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.CaseExpression;

public class CaseExpressionTest {

	
	@Test
	public void testCaseExpressionWithCaseAbbreviation() throws ParseException {
		CaseExpression element = new TableQueryParser("COALESCE(null, 'foo',null)").caseExpression();
		assertEquals("COALESCE(NULL,'foo',NULL)", element.toSql());
	}
	
	@Test
	public void testCaseExpressionWithCaseSpecification() throws ParseException {
		CaseExpression element = new TableQueryParser("case foo when 'one' then 1 else 0 end").caseExpression();
		assertEquals("CASE foo WHEN 'one' THEN 1 ELSE 0 END", element.toSql());
	}
}
