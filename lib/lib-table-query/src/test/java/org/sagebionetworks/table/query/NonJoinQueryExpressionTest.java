package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.NonJoinQueryExpression;

public class NonJoinQueryExpressionTest {

	@Test
	public void testSingleQuery() throws ParseException {
		NonJoinQueryExpression element = new TableQueryParser("select * from syn123").nonJoinQueryExpression();
		assertNotNull(element);
		assertEquals("SELECT * FROM syn123", element.toSql());
	}

	@Test
	public void testUnion() throws ParseException {
		NonJoinQueryExpression element = new TableQueryParser("select * from syn111 union select * from syn222")
				.nonJoinQueryExpression();
		assertNotNull(element);
		assertEquals("SELECT * FROM syn111 UNION SELECT * FROM syn222", element.toSql());
	}

	@Test
	public void testUnionAll() throws ParseException {
		NonJoinQueryExpression element = new TableQueryParser("select * from syn111 union all select * from syn222")
				.nonJoinQueryExpression();
		assertNotNull(element);
		assertEquals("SELECT * FROM syn111 UNION ALL SELECT * FROM syn222", element.toSql());
	}

	@Test
	public void testUnionDistinct() throws ParseException {
		NonJoinQueryExpression element = new TableQueryParser(
				"select * from syn111 union distinct select * from syn222").nonJoinQueryExpression();
		assertNotNull(element);
		assertEquals("SELECT * FROM syn111 UNION DISTINCT SELECT * FROM syn222", element.toSql());
	}

	@Test
	public void testUnionNested() throws ParseException {
		NonJoinQueryExpression element = new TableQueryParser(
				"select * from syn111 union select * from syn222 union select * from syn333 union select * from syn444")
						.nonJoinQueryExpression();
		assertNotNull(element);
		assertEquals("SELECT * FROM syn111 UNION SELECT * FROM syn222 UNION SELECT * FROM syn333 UNION SELECT * FROM syn444", element.toSql());
	}
}
