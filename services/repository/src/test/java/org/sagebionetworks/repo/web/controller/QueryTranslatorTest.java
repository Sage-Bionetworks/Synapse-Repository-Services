package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.web.query.QueryStatement;

public class QueryTranslatorTest {
	
	@Test
	public void testFrom() throws Exception{
		QueryStatement stmt = new QueryStatement("select * from dataset");
		assertNotNull(stmt);
		BasicQuery results = QueryTranslator.createBasicQuery(stmt);
		assertEquals(EntityType.dataset, results.getFrom());
	}
	
	@Test
	public void testFromEntity() throws Exception{
		QueryStatement stmt = new QueryStatement("select * from entity");
		assertNotNull(stmt);
		BasicQuery results = QueryTranslator.createBasicQuery(stmt);
		assertEquals(null, results.getFrom());
	}
	
	@Test
	public void testSort() throws Exception{
		QueryStatement stmt = new QueryStatement("select * from dataset order by id");
		assertNotNull(stmt);
		BasicQuery results = QueryTranslator.createBasicQuery(stmt);
		assertNotNull(results.getFrom());
	}
	
	@Test
	public void testSortAscending() throws Exception{
		QueryStatement stmt = new QueryStatement("select * from dataset order by id asc");
		assertNotNull(stmt);
		BasicQuery results = QueryTranslator.createBasicQuery(stmt);
		assertTrue(results.isAscending());
	}
	
	@Test
	public void testSortDecending() throws Exception{
		QueryStatement stmt = new QueryStatement("select * from dataset order by id desc");
		assertNotNull(stmt);
		BasicQuery results = QueryTranslator.createBasicQuery(stmt);
		assertFalse(results.isAscending());
	}
	
	@Test
	public void testOffset() throws Exception{
		QueryStatement stmt = new QueryStatement("select * from dataset offset 12");
		assertNotNull(stmt);
		BasicQuery results = QueryTranslator.createBasicQuery(stmt);
		assertEquals(11, results.getOffset());
	}
	
	@Test
	public void testLimit() throws Exception{
		QueryStatement stmt = new QueryStatement("select * from dataset limit 10");
		assertNotNull(stmt);
		BasicQuery results = QueryTranslator.createBasicQuery(stmt);
		assertEquals(10, results.getLimit());
	}
	
	@Test
	public void testFilter() throws Exception{
		QueryStatement stmt = new QueryStatement("select * from dataset where parentId == 123");
		assertNotNull(stmt);
		BasicQuery results = QueryTranslator.createBasicQuery(stmt);
		assertNotNull(results.getFilters());
		assertEquals(1, results.getFilters().size());
	}
	
	@Test
	public void testSelectStar() throws Exception{
		QueryStatement stmt = new QueryStatement("select * from dataset");
		assertNotNull(stmt);
		BasicQuery results = QueryTranslator.createBasicQuery(stmt);
		assertEquals(null, results.getSelect());
	}
	
	@Test
	public void testSelectList() throws Exception{
		QueryStatement stmt = new QueryStatement("select a,b,c from dataset");
		assertNotNull(stmt);
		BasicQuery results = QueryTranslator.createBasicQuery(stmt);
		List<String> expectedSelect = new ArrayList<String>();
		expectedSelect.add("a");
		expectedSelect.add("b");
		expectedSelect.add("c");
		assertEquals(expectedSelect, results.getSelect());
	}

}
