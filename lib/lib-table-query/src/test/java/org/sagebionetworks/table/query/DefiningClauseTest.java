package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.DefiningClause;
import org.sagebionetworks.table.query.model.SearchCondition;

public class DefiningClauseTest {

	@Test
	public void testDefiningClause() throws ParseException {
		DefiningClause element = new TableQueryParser(
				"defining_where a < b and b > c and (c between 1 and 3 or c between 5 and 6)").definingClause();
		assertEquals("DEFINING_WHERE ( c BETWEEN 1 AND 3 OR c BETWEEN 5 AND 6 ) AND a < b AND b > c", element.toSql());
		
		SearchCondition child = element.getFirstElementOfType(SearchCondition.class);
		assertEquals("( c BETWEEN 1 AND 3 OR c BETWEEN 5 AND 6 ) AND a < b AND b > c", child.toSql());
	}
}
