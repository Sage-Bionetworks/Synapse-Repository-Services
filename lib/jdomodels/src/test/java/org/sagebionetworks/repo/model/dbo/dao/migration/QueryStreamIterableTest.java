package org.sagebionetworks.repo.model.dbo.dao.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.dbo.migration.QueryStreamIterable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.google.common.collect.Lists;

@RunWith(MockitoJUnitRunner.class)
public class QueryStreamIterableTest {

	@Mock
	NamedParameterJdbcTemplate mockTemplate;
	@Mock
	RowMapper<String> rowMapper;
	
	String sql;
	Map<String, Object> parameters;
	List<String> queryResults;
	
	@Before
	public void before() {
		sql = "select * from foo where bar = :p1";
		parameters = new HashMap<>(2);
		parameters.put("p1", "something");
		queryResults = Lists.newArrayList("one", "two", "three", "four", "five");
		// return the results as three pages
		when(mockTemplate.query(any(String.class), anyMapOf(String.class, Object.class), any(RowMapper.class)))
		.thenReturn(queryResults.subList(0, 2), queryResults.subList(2, 4), queryResults.subList(4, 5), new LinkedList());
	}
	
	@Test
	public void testIterator() {
		long limit = 2;
		QueryStreamIterable<String> iterable = new QueryStreamIterable<String>(mockTemplate, rowMapper, sql, parameters, limit);
		// capture all of the results
		List<String> results = new LinkedList();
		for(String value: iterable) {
			results.add(value);
		}
		assertEquals(queryResults, results);
	}
	
	
	@Test
	public void testQueryStreamIterable() {
		String expectedSQL = "select * from foo where bar = :p1 LIMIT :KEY_LIMIT OFFSET :KEY_OFFSET";
		Map<String, Object> expectedParams = new HashMap<>();
		expectedParams.put("p1", "something");
		expectedParams.put(QueryStreamIterable.KEY_LIMIT, 2L);
		
		long limit = 2;
		QueryStreamIterable<String> iterable = new QueryStreamIterable<String>(mockTemplate, rowMapper, sql, parameters, limit);
		
		// Setup first page
		when(mockTemplate.query(any(String.class), anyMapOf(String.class, Object.class), any(RowMapper.class)))
		.thenReturn(Lists.newArrayList("one", "two"));
		expectedParams.put(QueryStreamIterable.KEY_OFFSET, 0L);
		// call under test
		assertTrue(iterable.hasNext());
		// call under test
		assertEquals("one", iterable.next());
		// query should be called
		verify(mockTemplate).query(expectedSQL, expectedParams, rowMapper);
		reset(mockTemplate);
		
		// call under test
		assertTrue(iterable.hasNext());
		// call under test
		assertEquals("two", iterable.next());
		// no query call for this next()
		verify(mockTemplate, never()).query(anyString(), anyMap(), any(RowMapper.class));
		reset(mockTemplate);
		
		// Setup page two
		when(mockTemplate.query(any(String.class), anyMapOf(String.class, Object.class), any(RowMapper.class)))
		.thenReturn(Lists.newArrayList("three"));
		expectedParams.put(QueryStreamIterable.KEY_OFFSET, 2L);
		// call under test
		assertTrue(iterable.hasNext());
		// call under test
		assertEquals("three", iterable.next());
		// query should be called
		verify(mockTemplate).query(expectedSQL, expectedParams, rowMapper);
		reset(mockTemplate);
		
		// Last page should be empty
		when(mockTemplate.query(any(String.class), anyMapOf(String.class, Object.class), any(RowMapper.class)))
		.thenReturn(new LinkedList<>());
		
		// call under test
		assertFalse(iterable.hasNext());		
	}
}
