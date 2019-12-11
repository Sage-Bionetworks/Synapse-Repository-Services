package org.sagebionetworks.table.query.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;

class JoinConditionTest {

	@Test
	public void testJoinCondition() throws ParseException {
		SearchCondition searchCondition = new TableQueryParser("t1.foo = t2.foo").searchCondition();
		//join condition is not exposed to parser so we need to parse its child instead

		JoinCondition joinCondition = new JoinCondition(searchCondition);

		assertEquals("ON t1.foo = t2.foo", joinCondition.toSql());
	}
}