package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.queryparser.ParseException;
import org.sagebionetworks.repo.util.QueryTranslator;
import org.sagebionetworks.repo.web.query.QueryStatement;

public class QueryTranslatorTest {
	
	@Test
	public void testFrom() throws Exception{
		QueryStatement stmt = new QueryStatement("select * from folder");
		assertNotNull(stmt);
		BasicQuery results = QueryTranslator.createBasicQuery(stmt);
		assertEquals(EntityType.folder.name(), results.getFrom());
	}
	
	@Test
	public void testFromEntity() throws Exception{
		QueryStatement stmt = new QueryStatement("select * from entity");
		assertNotNull(stmt);
		BasicQuery results = QueryTranslator.createBasicQuery(stmt);
		assertEquals("entity", results.getFrom());
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
		assertEquals(12, results.getOffset());
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
	
	@Test
	public void testForPLFM_901() throws ParseException{
		QueryStatement stmt = new QueryStatement("select * from dataset where dataset.id == \"4494\" and dataset.parentId == \"4492\"");
		assertNotNull(stmt);
		BasicQuery results = QueryTranslator.createBasicQuery(stmt);
		assertNotNull(results.getFilters());
		System.out.println(results.getFilters());
		assertEquals(2, results.getFilters().size());
	}
	@Test
	public void testForPLFM_901_A() throws ParseException{
		QueryStatement stmt = new QueryStatement("select * from dataset where dataset.id == '4494' and dataset.parentId == '4492'");
		assertNotNull(stmt);
		BasicQuery results = QueryTranslator.createBasicQuery(stmt);
		assertNotNull(results.getFilters());
		System.out.println(results.getFilters());
		assertEquals(2, results.getFilters().size());
		assertEquals("4494", results.getFilters().get(0).getValue());
		assertEquals("4492", results.getFilters().get(1).getValue());
	}
	
	@Test
	public void testForPLFM_901_B() throws ParseException, UnsupportedEncodingException{
		String urlEncoded = "select+*+from+dataset+where+dataset.Species+==+%22Human%22+and+dataset.Number_of_Samples+%3E+100+limit+3+offset+1";
		String decoded = URLDecoder.decode(urlEncoded, "UTF-8");
		System.out.println(decoded);
		QueryStatement stmt = new QueryStatement(decoded);
		assertNotNull(stmt);
		BasicQuery results = QueryTranslator.createBasicQuery(stmt);
		assertNotNull(results.getFilters());
		System.out.println(results.getFilters());
		assertEquals(2, results.getFilters().size());
		assertEquals("Human", results.getFilters().get(0).getValue());
		assertEquals(100L, results.getFilters().get(1).getValue());
	}
	
	
	@Test
	public void testForPLFM_2783() throws ParseException, UnsupportedEncodingException{
		String urlEncoded = "select+*+from+file+order+by+foo+limit+3+offset+1";
		String decoded = URLDecoder.decode(urlEncoded, "UTF-8");
		System.out.println(decoded);
		QueryStatement stmt = new QueryStatement(decoded);
		assertNotNull(stmt);
		assertEquals(new Long(3), stmt.getLimit());
		assertEquals(new Long(1), stmt.getOffset());
		assertEquals("foo", stmt.getSortField());
	}
	

}
