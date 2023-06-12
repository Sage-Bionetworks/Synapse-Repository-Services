package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.QueryExpression;
import org.sagebionetworks.table.query.model.QuerySpecification;

public class QueryExpressionTest {

	@Test
	public void testQueryExpression() throws ParseException {
		QueryExpression element = new TableQueryParser("select * from syn1 union select * from syn2").queryExpression();
		assertEquals("SELECT * FROM syn1 UNION SELECT * FROM syn2", element.toSql());
	}

	@Test
	public void testQueryExpressionWithWith() throws ParseException {
		QueryExpression element = new TableQueryParser("with cte as (select * from syn1) select * from cte")
				.queryExpression();
		assertEquals("WITH cte AS (SELECT * FROM syn1) SELECT * FROM cte", element.toSql());
	}

	@Test
	public void testQueryExpressionWithWithMultiple() throws ParseException {
		QueryExpression element = new TableQueryParser(
				"with cte as (select * from syn1), cte2(a,b,c) as (select * from syn2) select * from cte union select * from cte2")
				.queryExpression();
		assertEquals(
				"WITH cte AS (SELECT * FROM syn1), cte2 (a, b, c) AS (SELECT * FROM syn2) SELECT * FROM cte UNION SELECT * FROM cte2",
				element.toSql());
	}

	@Test
	public void testGetChildernInOrder() throws ParseException {
		QueryExpression element = new TableQueryParser(
				"with cte as (select * from syn1), cte2(a,b,c) as (select * from syn2) select * from cte union select * from cte2")
				.queryExpression();
		// call under test
		List<String> ordered = element.stream(QuerySpecification.class).map(s -> s.toSql())
				.collect(Collectors.toList());
		// the first query defines the final select statement.
		assertEquals(List.of("SELECT * FROM cte", "SELECT * FROM cte2", "SELECT * FROM syn1", "SELECT * FROM syn2"),
				ordered);
	}

}
