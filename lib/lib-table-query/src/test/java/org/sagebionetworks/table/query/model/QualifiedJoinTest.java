package org.sagebionetworks.table.query.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;

class QualifiedJoinTest {
	TableReference lhs;
	TableReference rhs;

	@Test
	public void testWithJoinCondition() throws ParseException {
		lhs = new TableQueryParser("tableA").tableReference();
		rhs = new TableQueryParser("tableB").tableReference();
		JoinCondition joinCondition = new JoinCondition(new TableQueryParser("tableA.foo = tableB.foo").searchCondition());
		//not exposed to parser so no parsing of qualified join string
		QualifiedJoin join = new QualifiedJoin(lhs, rhs, joinCondition);
		assertEquals("tableA JOIN tableB ON tableA.foo = tableB.foo", join.toSql());
	}

	@Test
	public void testWithOuterJoinAndJoinCondition() throws ParseException {
		lhs = new TableQueryParser("tableA").tableReference();
		rhs = new TableQueryParser("tableB").tableReference();
		JoinCondition joinCondition = new JoinCondition(new TableQueryParser("tableA.foo = tableB.foo").searchCondition());
		//not exposed to parser so no parsing of qualified join string
		QualifiedJoin join = new QualifiedJoin(lhs, new JoinType(OuterJoinType.LEFT), rhs, joinCondition);
		assertEquals("tableA LEFT JOIN tableB ON tableA.foo = tableB.foo", join.toSql());
	}
}