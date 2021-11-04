package org.sagebionetworks.table.query.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;

class QualifiedJoinTest {
	TableReference lhs;
	TableReference rhs;
	
	@Test
	public void testParse() throws ParseException {
		TableReference lhs = new TableQueryParser("tableA").tableReference();
		QualifiedJoin join = new TableQueryParser("join tableB on tableA.id = tableB.id").qualifiedJoin(lhs);
		assertEquals("tableA JOIN tableB ON tableA.id = tableB.id",join.toSql());
	}
	
	@Test
	public void testParseWithInner() throws ParseException {
		TableReference lhs = new TableQueryParser("tableA").tableReference();
		QualifiedJoin join = new TableQueryParser("inner join tableB on tableA.id = tableB.id").qualifiedJoin(lhs);
		assertEquals("tableA INNER JOIN tableB ON tableA.id = tableB.id",join.toSql());
	}
	
	@Test
	public void testParseWithLeftOuter() throws ParseException {
		TableReference lhs = new TableQueryParser("tableA").tableReference();
		QualifiedJoin join = new TableQueryParser("left outer join tableB on tableA.id = tableB.id").qualifiedJoin(lhs);
		assertEquals("tableA LEFT OUTER JOIN tableB ON tableA.id = tableB.id",join.toSql());
	}
	
	@Test
	public void testParseWithRightOuter() throws ParseException {
		TableReference lhs = new TableQueryParser("tableA").tableReference();
		QualifiedJoin join = new TableQueryParser("right outer join tableB on tableA.id = tableB.id").qualifiedJoin(lhs);
		assertEquals("tableA RIGHT OUTER JOIN tableB ON tableA.id = tableB.id",join.toSql());
	}
	
	/**
	 * ON is required.
	 */
	@Test
	public void testParseWithNoOn() throws ParseException {
		TableReference lhs = new TableQueryParser("tableA").tableReference();
		String message = assertThrows(ParseException.class, ()->{
			new TableQueryParser("join tableB").qualifiedJoin(lhs);
		}).getMessage();
		assertTrue(message.contains("Was expecting:"));
		assertTrue(message.contains("\"ON\" ..."));
	}

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