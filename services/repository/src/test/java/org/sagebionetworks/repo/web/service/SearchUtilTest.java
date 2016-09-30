package org.sagebionetworks.repo.web.service;

import static org.junit.Assert.assertEquals;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.search.query.FacetSort;
import org.sagebionetworks.repo.model.search.query.FacetSortOptions;
import org.sagebionetworks.repo.model.search.query.FacetTopN;
import org.sagebionetworks.repo.model.search.query.KeyList;
import org.sagebionetworks.repo.model.search.query.KeyValue;
import org.sagebionetworks.repo.model.search.query.SearchQuery;

public class SearchUtilTest {
	private SearchQuery query;
	private String queryStr;
	
	private List<String> q;
	private List<KeyValue> bq;
	private List<KeyValue> bqNot;
	private List<KeyValue> bq2;
	private List<KeyValue> bqSpecialChar;
	
	private static final String EXPECTED_QUERY_PREFIX = "q.parser=structured&q=";
	
	private String encodeUTF8(String s) throws UnsupportedEncodingException{
		return URLEncoder.encode(s, "UTF-8");
	}
	
	@Before
	public void before() throws Exception {
		query = new SearchQuery();
		// q
		q = new ArrayList<String>();
		q.add("hello");
		q.add("world");
		
		// bq
		bq = new ArrayList<KeyValue>();
		KeyValue kv = new KeyValue();
		kv.setKey("Facet1");
		kv.setValue("Value1");
		bq.add(kv);

		bq2 = new ArrayList<KeyValue>();
		kv = new KeyValue();
		kv.setKey("Facet1");
		kv.setValue("..2000");
		bq2.add(kv);

		bqNot = new ArrayList<KeyValue>();
		kv = new KeyValue();
		kv.setKey("Facet1");
		kv.setValue("Value1");
		kv.setNot(true);
		bqNot.add(kv);
		kv = new KeyValue();
		kv.setKey("Facet2");
		kv.setValue("Value2");
		bqNot.add(kv);

		bqSpecialChar = new ArrayList<KeyValue>();
		kv = new KeyValue();
		kv.setKey("Facet1");
		kv.setValue("c:\\dave's_folde,r");
		bqSpecialChar.add(kv);
	}
	@Test (expected = IllegalArgumentException.class)
	public void testNullQuery() throws Exception{
		// null input
		SearchUtil.generateStructuredQueryString(null);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testNoQueryContent() throws Exception{
		// no actual query content
		SearchUtil.generateStructuredQueryString( new SearchQuery() );
	}
	@Test (expected = IllegalArgumentException.class)
	public void testEmptyQuery() throws Exception{
		// empty query
		query.setQueryTerm(Arrays.asList(new String[] {""}));
		SearchUtil.generateStructuredQueryString(query);
	}

	@Test
	public void testRegularQueryOnly() throws Exception{

		// query only
		query.setQueryTerm(q);
		queryStr = SearchUtil.generateStructuredQueryString(query);
		assertEquals(EXPECTED_QUERY_PREFIX+encodeUTF8("(and 'hello' 'world')"), queryStr);
	}
	
	@Test
	public void testRegularQueryWithPrefix() throws Exception{
		// q
		q.add("somePrefix*");
		query.setQueryTerm(q);
		queryStr = SearchUtil.generateStructuredQueryString(query);
		assertEquals(EXPECTED_QUERY_PREFIX+encodeUTF8("(and 'hello' 'world' (prefix 'somePrefix'))"), queryStr);
	}
	
	@Test
	public void testBooleanQuery() throws Exception{
		// boolean query only
		query.setBooleanQuery(bq);
		queryStr = SearchUtil.generateStructuredQueryString(query);
		assertEquals(EXPECTED_QUERY_PREFIX+encodeUTF8("(and Facet1:'Value1')"), queryStr);
	}
	
	@Test
	public void testBooleanQueryWithPrefix() throws Exception{
		KeyValue prefixKV = new KeyValue();
		prefixKV.setKey("someField");
		prefixKV.setValue("somePrefix*");
		bq.add(prefixKV);
		query.setBooleanQuery(bq);
		queryStr = SearchUtil.generateStructuredQueryString(query);
		assertEquals(EXPECTED_QUERY_PREFIX+encodeUTF8("(and Facet1:'Value1' (prefix field=someField 'somePrefix'))"), queryStr);
	}
	
	@Test
	public void testBooleanQueryWithBlankRegularQuery() throws Exception{
		// boolean query with blank single q
		query.setQueryTerm(Arrays.asList(new String[] {""}));
		query.setBooleanQuery(bq);
		queryStr = SearchUtil.generateStructuredQueryString(query);
		assertEquals(EXPECTED_QUERY_PREFIX+encodeUTF8("(and Facet1:'Value1')"), queryStr);
	}

	@Test
	public void testBooleanQueryContinuous() throws Exception{
		// continuous bq
		query.setBooleanQuery(bq2);
		queryStr = SearchUtil.generateStructuredQueryString(query);
		assertEquals(EXPECTED_QUERY_PREFIX+encodeUTF8("(and (range field=Facet1 {,2000]))"), queryStr);
	}
	
	@Test
	public void testNegatedBooleanQuery() throws Exception{
		// negated boolean query
		query.setBooleanQuery(bqNot);
		queryStr = SearchUtil.generateStructuredQueryString(query);
		assertEquals(EXPECTED_QUERY_PREFIX + encodeUTF8("(and (not Facet1:'Value1') Facet2:'Value2')"), queryStr);
	}
	
	@Test
	public void testSpecialCharactersInBooleanQuery() throws Exception{
		// special characters in boolean query
		query.setBooleanQuery(bqSpecialChar);
		queryStr = SearchUtil.generateStructuredQueryString(query);
		assertEquals(EXPECTED_QUERY_PREFIX + "%28and+Facet1%3A%27c%3A%5C%5Cdave%5C%27s_folde%2Cr%27%29", queryStr);	
	}	
	@Test
	public void testRegularQueryAndBooleanQuery() throws Exception{
		// Both q and bq
		query.setBooleanQuery(bq);
		query.setQueryTerm(q);
		queryStr = SearchUtil.generateStructuredQueryString(query);
		assertEquals(EXPECTED_QUERY_PREFIX + encodeUTF8("(and (and 'hello' 'world') Facet1:'Value1')"), queryStr);
	}
	
	@Test
	public void testFacets() throws Exception{
		// facets
		query.setQueryTerm(q);
		List<String> facets = new ArrayList<String>();
		facets.add("facet1");
		facets.add("facet2");
		query.setQueryTerm(q);
		query.setFacet(facets);
		queryStr = SearchUtil.generateStructuredQueryString(query);
		assertEquals( EXPECTED_QUERY_PREFIX + encodeUTF8("(and 'hello' 'world')")+"&facet.facet1=" + encodeUTF8("{}") + "&facet.facet2=" + encodeUTF8("{}"), queryStr);
	}
		
	
	@Test (expected = IllegalArgumentException.class)
	public void testFacetConstraints() throws Exception{
		// facet field constraints
		query.setQueryTerm(q);
		List<KeyList> facetFieldConstraints = new ArrayList<KeyList>();
		KeyList ffc1 = new KeyList();
		ffc1.setKey("facet1");		
		ffc1.setValues(Arrays.asList(new String[] { "one,two\\three", "dave's", "regular" }));
		facetFieldConstraints.add(ffc1);
		KeyList ffc2 = new KeyList();
		ffc2.setKey("facet2");		
		ffc2.setValues(Arrays.asList(new String[] { "123", "4..5" }));
		facetFieldConstraints.add(ffc2);
		query.setFacetFieldConstraints(facetFieldConstraints);
		queryStr = SearchUtil.generateStructuredQueryString(query);
	}
	@Test (expected = IllegalArgumentException.class)
	public void testFacetSort() throws Exception{
		// facet field sort
		query.setQueryTerm(q);
		List<FacetSort> facetFieldSorts = null;
		FacetSort fs = null;
		
		fs = new FacetSort();
		fs.setFacetName("facet1");
		fs.setSortType(FacetSortOptions.ALPHA);
		facetFieldSorts = new ArrayList<FacetSort>();
		facetFieldSorts.add(fs);
		query.setQueryTerm(q);
		query.setFacetFieldSort(facetFieldSorts);
		queryStr = SearchUtil.generateStructuredQueryString(query);
	}
		
	@Test(expected = IllegalArgumentException.class)
	public void testFacetFieldTopN() throws Exception{
		// facet field top N
		List<FacetTopN> topNList = new ArrayList<FacetTopN>();
		FacetTopN topn = null; 
		
		topn = new FacetTopN();
		topn.setKey("facet1");
		topn.setValue(new Long(10));
		topNList.add(topn);
		
		topn = new FacetTopN();
		topn.setKey("facet2");
		topn.setValue(new Long(20));
		topNList.add(topn);
		
		query.setQueryTerm(q);
		query.setFacetFieldTopN(topNList);
		queryStr = SearchUtil.generateStructuredQueryString(query);
	}	
	@Test (expected=IllegalArgumentException.class)
	public void testRank() throws Exception{
		query.setQueryTerm(q);
		query.setRank(Arrays.asList(new String[]{"rankfield1", "-rankfield2"}));
		queryStr = SearchUtil.generateStructuredQueryString(query);
	}
	
	@Test
	public void testReturnFields() throws Exception{
		// return fields
		query.setQueryTerm(q);
		query.setReturnFields(Arrays.asList(new String[] { "retF1", "retF2" }));
		queryStr = SearchUtil.generateStructuredQueryString(query);
		assertEquals(EXPECTED_QUERY_PREFIX+encodeUTF8("(and 'hello' 'world')")+ "&return=" + encodeUTF8("retF1,retF2"), queryStr);
		
	}
	@Test
	public void testSizeParameter() throws Exception{
		// size
		query.setQueryTerm(q);
		query.setSize(new Long(100));
		queryStr = SearchUtil.generateStructuredQueryString(query);
		assertEquals( EXPECTED_QUERY_PREFIX+encodeUTF8("(and 'hello' 'world')") +"&size=100", queryStr);
	}
	@Test
	public void testStartParameter() throws Exception{
		// start
		query.setQueryTerm(q);
		query.setStart(new Long(10));
		queryStr = SearchUtil.generateStructuredQueryString(query);
		assertEquals( EXPECTED_QUERY_PREFIX+encodeUTF8("(and 'hello' 'world')") +"&start=10", queryStr);	
	}
}










