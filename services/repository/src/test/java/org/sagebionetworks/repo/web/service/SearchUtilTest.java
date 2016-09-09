package org.sagebionetworks.repo.web.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

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

import com.amazonaws.services.cloudfront.model.InvalidArgumentException;

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
		kv.setValue("{,2000]");
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
	@Test (expected = InvalidArgumentException.class)
	public void testNullQuery() throws Exception{
		// null input
		SearchUtil.generateStructuredQueryString(null);
	}
	
	@Test(expected = InvalidArgumentException.class)
	public void testNoQueryContent() throws Exception{
		// no actual query content
		SearchUtil.generateStructuredQueryString( new SearchQuery() );
	}
	@Test (expected = InvalidArgumentException.class)
	public void testEmptyQuery() throws Exception{
		// empty query
		query = new SearchQuery();
		query.setQueryTerm(Arrays.asList(new String[] {""}));
		SearchUtil.generateStructuredQueryString(query);
	}

	@Test
	public void testRegularQueryOnly() throws Exception{

		// query only
		query = new SearchQuery();
		query.setQueryTerm(q);
		queryStr = SearchUtil.generateStructuredQueryString(query);
		assertEquals(EXPECTED_QUERY_PREFIX+encodeUTF8("(and 'hello' 'world')"), queryStr);
	}
	
	@Test
	public void testBooleanQuery() throws Exception{
		// boolean query only
		query = new SearchQuery();
		query.setBooleanQuery(bq);
		queryStr = SearchUtil.generateStructuredQueryString(query);
		assertEquals(EXPECTED_QUERY_PREFIX+encodeUTF8("(and Facet1:'Value1')"), queryStr);
	}
	
	@Test
	public void testBooleanQueryWithBlankRegularQuery() throws Exception{
		// boolean query with blank single q
		query = new SearchQuery();
		query.setQueryTerm(Arrays.asList(new String[] {""}));
		query.setBooleanQuery(bq);
		queryStr = SearchUtil.generateStructuredQueryString(query);
		assertEquals(EXPECTED_QUERY_PREFIX+encodeUTF8("(and Facet1:'Value1')"), queryStr);
	}

	@Test
	public void testBooleanQueryContinuous() throws Exception{
		// continuous bq
		query = new SearchQuery();
		query.setBooleanQuery(bq2);
		queryStr = SearchUtil.generateStructuredQueryString(query);
		assertEquals(EXPECTED_QUERY_PREFIX+encodeUTF8("(and Facet1:{,2000])"), queryStr);
	}
	
	@Test
	public void testNegatedBooleanQuery() throws Exception{
		// negated boolean query
		query = new SearchQuery();
		query.setBooleanQuery(bqNot);
		queryStr = SearchUtil.generateStructuredQueryString(query);
		assertEquals(EXPECTED_QUERY_PREFIX + encodeUTF8("(and (not Facet1:'Value1') Facet2:'Value2')"), queryStr);
	}
	
	@Test
	public void testSpecialCharactersInBooleanQuery() throws Exception{
		// special characters in boolean query
		query = new SearchQuery();
		query.setBooleanQuery(bqSpecialChar);
		queryStr = SearchUtil.generateStructuredQueryString(query);
		assertEquals(EXPECTED_QUERY_PREFIX + "%28and+Facet1%3A%27c%3A%5C%5Cdave%5C%27s_folde%2Cr%27%29", queryStr);	
	}	
	@Test
	public void testRegularQueryAndBooleanQuery() throws Exception{
		// Both q and bq
		query = new SearchQuery();
		query.setBooleanQuery(bq);
		query.setQueryTerm(q);
		queryStr = SearchUtil.generateStructuredQueryString(query);
		assertEquals(EXPECTED_QUERY_PREFIX + encodeUTF8("(and (and 'hello' 'world') Facet1:'Value1')"), queryStr);
	}
	
	@Test
	public void testFacets() throws Exception{
		// facets
		query = new SearchQuery();
		query.setQueryTerm(q);
		List<String> facets = new ArrayList<String>();
		facets.add("facet1");
		facets.add("facet2");
		query.setQueryTerm(q);
		query.setFacet(facets);
		queryStr = SearchUtil.generateStructuredQueryString(query);
		assertEquals("q=hello%2Cworld&facet=facet1%2Cfacet2", queryStr);
	}
		
	@Test
	public void asdf() throws Exception{	
			
		// facet field constraints
		query = new SearchQuery();
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
		assertEquals("q=hello%2Cworld&facet-facet1-constraints=%27one%5C%2Ctwo%5C%5Cthree%27%2C%27dave%5C%27s%27%2C%27regular%27&facet-facet2-constraints=123%2C4..5", queryStr);

		// facet field sort
		query = new SearchQuery();
		query.setQueryTerm(q);
		List<FacetSort> facetFieldSorts = null;
		FacetSort fs = null;
		
		fs = new FacetSort();
		fs.setFacetName("facet1");
		fs.setSortType(FacetSortOptions.ALPHA);
		facetFieldSorts = new ArrayList<FacetSort>();
		facetFieldSorts.add(fs);
		query = new SearchQuery();
		query.setQueryTerm(q);
		query.setFacetFieldSort(facetFieldSorts);
		queryStr = SearchUtil.generateStructuredQueryString(query);
		assertEquals("q=hello%2Cworld&facet-facet1-sort=alpha", queryStr);
		
		fs = new FacetSort();
		fs.setFacetName("facet2");
		fs.setSortType(FacetSortOptions.COUNT);
		facetFieldSorts = new ArrayList<FacetSort>();
		facetFieldSorts.add(fs);
		query = new SearchQuery();
		query.setQueryTerm(q);
		query.setFacetFieldSort(facetFieldSorts);
		queryStr = SearchUtil.generateStructuredQueryString(query);
		assertEquals("q=hello%2Cworld&facet-facet2-sort=count", queryStr);
		
		fs = new FacetSort();
		fs.setFacetName("facet3");
		fs.setSortType(FacetSortOptions.MAX);
		fs.setMaxfield("maxfield");
		facetFieldSorts = new ArrayList<FacetSort>();
		facetFieldSorts.add(fs);
		query = new SearchQuery();
		query.setQueryTerm(q);
		query.setFacetFieldSort(facetFieldSorts);
		queryStr = SearchUtil.generateStructuredQueryString(query);
		assertEquals("q=hello%2Cworld&facet-facet3-sort=max%28maxfield%29", queryStr);

				
		fs = new FacetSort();
		fs.setFacetName("facet4");
		fs.setSortType(FacetSortOptions.SUM);
		fs.setSumFields(Arrays.asList(new String[] { "sum1", "sum2" }));
		facetFieldSorts = new ArrayList<FacetSort>();
		facetFieldSorts.add(fs);
		query = new SearchQuery();
		query.setQueryTerm(q);
		query.setFacetFieldSort(facetFieldSorts);
		queryStr = SearchUtil.generateStructuredQueryString(query);
		assertEquals("q=hello%2Cworld&facet-facet4-sort=sum%28sum1%2Csum2%29", queryStr);
		

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
		
		query = new SearchQuery();
		query.setQueryTerm(q);
		query.setFacetFieldTopN(topNList);
		queryStr = SearchUtil.generateStructuredQueryString(query);
		assertEquals("q=hello%2Cworld&facet-facet1-top-n=10&facet-facet2-top-n=20", queryStr);
		
		// return fields
		query = new SearchQuery();
		query.setQueryTerm(q);
		query.setReturnFields(Arrays.asList(new String[] { "retF1", "retF2" }));
		queryStr = SearchUtil.generateStructuredQueryString(query);
		assertEquals("q=hello%2Cworld&return-fields=retF1%2CretF2", queryStr);
		
		// size
		query = new SearchQuery();
		query.setQueryTerm(q);
		query.setSize(new Long(100));
		queryStr = SearchUtil.generateStructuredQueryString(query);
		assertEquals("q=hello%2Cworld&size=100", queryStr);
		
		// start
		query = new SearchQuery();
		query.setQueryTerm(q);
		query.setStart(new Long(10));
		queryStr = SearchUtil.generateStructuredQueryString(query);
		assertEquals("q=hello%2Cworld&start=10", queryStr);		
		
	}
}










