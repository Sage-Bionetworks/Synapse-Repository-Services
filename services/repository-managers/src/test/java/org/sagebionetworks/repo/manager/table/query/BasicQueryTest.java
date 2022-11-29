package org.sagebionetworks.repo.manager.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BasicQueryTest {
	
	private String sql;
	private Map<String, Object> parameters;
	
	@BeforeEach
	public void before() {
		sql = "select * from syn123";
		parameters = new HashMap<String, Object>();
		parameters.put("one", 101L);
	}
	
	@Test
	public void testBasicQuery() {
		// call under test
		BasicQuery query = new BasicQuery(sql, parameters);
		assertEquals(sql, query.getSql());
		assertEquals(parameters, query.getParameters());
	}

	@Test
	public void testBasicQueryWithNullSql() {
		sql = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			new BasicQuery(sql, parameters);
		}).getMessage();
		assertEquals("sql is required.", message);
	}
	
	@Test
	public void testBasicQueryWithNullParameters() {
		parameters = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			new BasicQuery(sql, parameters);
		}).getMessage();
		assertEquals("parameters is required.", message);
	}
}
