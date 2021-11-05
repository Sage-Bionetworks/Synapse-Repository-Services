package org.sagebionetworks.table.query.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;

class JoinConditionTest {

	@Test
	public void testJoinCondition() throws ParseException {
		JoinCondition joinCondition = new TableQueryParser("on t1.foo = t2.foo").joinCondition();
		assertEquals("ON t1.foo = t2.foo", joinCondition.toSql());
	}
	
	@Test
	public void testJoinConditionWithPerenteses() throws ParseException {
		JoinCondition joinCondition = new TableQueryParser("on (t1.foo = t2.foo)").joinCondition();
		assertEquals("ON ( t1.foo = t2.foo )", joinCondition.toSql());
	}
}