package org.sagebionetworks.repo.web.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

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

	@Before
	public void before() throws Exception{
	}
	
	@Test
	public void generateQueryStringTest() throws Exception {
		SearchQuery query = null;
		String queryStr = null;
		
		// q
		List<String> q = new ArrayList<String>();
		q.add("hello");
		q.add("world");
		
		// bq
		List<KeyValue> bq = new ArrayList<KeyValue>();
		KeyValue kv = new KeyValue();
		kv.setKey("Facet1");
		kv.setValue("Value1");
		bq.add(kv);

		List<KeyValue> bq2 = new ArrayList<KeyValue>();
		kv = new KeyValue();
		kv.setKey("Facet1");
		kv.setValue("..2000");
		bq2.add(kv);

		List<KeyValue> bqNot = new ArrayList<KeyValue>();
		kv = new KeyValue();
		kv.setKey("Facet1");
		kv.setValue("Value1");
		kv.setNot(true);
		bqNot.add(kv);
		kv = new KeyValue();
		kv.setKey("Facet2");
		kv.setValue("Value2");
		bqNot.add(kv);

		List<KeyValue> bqSpecialChar = new ArrayList<KeyValue>();
		kv = new KeyValue();
		kv.setKey("Facet1");
		kv.setValue("c:\\dave's_folde,r");
		bqSpecialChar.add(kv);
		
		// null input
		try {
			SearchUtil.generateQueryString(query);
			assertNotNull(null);
		} catch (InvalidArgumentException ex) {
			assertNull(null);
		}

		// no actual query content
		query = new SearchQuery();
		try {
			SearchUtil.generateQueryString(query);
			fail("no query content should fail");
		} catch (InvalidArgumentException ex) {
			assertNull(null);
		}

		// empty query
		query = new SearchQuery();
		query.setQueryTerm(Arrays.asList(new String[] {""}));
		try {
			SearchUtil.generateQueryString(query);
			fail("no real query should fail");
		} catch (InvalidArgumentException ex) {
			assertNull(null);
		}

		

		// query only
		query = new SearchQuery();
		query.setQueryTerm(q);
		queryStr = SearchUtil.generateQueryString(query);
		assertEquals("q=hello%2Cworld", queryStr);
		
		// boolean query only
		query = new SearchQuery();
		query.setBooleanQuery(bq);
		queryStr = SearchUtil.generateQueryString(query);
		assertEquals("bq=Facet1%3A%27Value1%27", queryStr);

		// boolean query with blank single q
		query = new SearchQuery();
		query.setQueryTerm(Arrays.asList(new String[] {""}));
		query.setBooleanQuery(bq);
		queryStr = SearchUtil.generateQueryString(query);
		assertEquals("bq=Facet1%3A%27Value1%27", queryStr);
		
		// boolean query with blank single q
		query = new SearchQuery();
		query.setQueryTerm(Arrays.asList(new String[] {""}));
		query.setBooleanQuery(bq);
		queryStr = SearchUtil.generateQueryString(query);
		assertEquals("bq=Facet1%3A%27Value1%27", queryStr);

		// continuous bq
		query = new SearchQuery();
		query.setBooleanQuery(bq2);
		queryStr = SearchUtil.generateQueryString(query);
		assertEquals("bq=Facet1%3A..2000", queryStr);
		
		// negated boolean query
		query = new SearchQuery();
		query.setBooleanQuery(bqNot);
		queryStr = SearchUtil.generateQueryString(query);
		assertEquals("bq=%28not+Facet1%3A%27Value1%27%29&bq=Facet2%3A%27Value2%27", queryStr);
		
		// special characters in boolean query
		query = new SearchQuery();
		query.setBooleanQuery(bqSpecialChar);
		queryStr = SearchUtil.generateQueryString(query);
		assertEquals("bq=Facet1%3A%27c%3A%5C%5Cdave%5C%27s_folde%2Cr%27", queryStr);		
		
		// Both q and bq
		query = new SearchQuery();
		query.setBooleanQuery(bq);
		query.setQueryTerm(q);
		queryStr = SearchUtil.generateQueryString(query);
		assertEquals("q=hello%2Cworld&bq=Facet1%3A%27Value1%27", queryStr);
		
		// facets
		query = new SearchQuery();
		query.setQueryTerm(q);
		List<String> facets = new ArrayList<String>();
		facets.add("facet1");
		facets.add("facet2");
		query.setQueryTerm(q);
		query.setFacet(facets);
		queryStr = SearchUtil.generateQueryString(query);
		assertEquals("q=hello%2Cworld&facet=facet1%2Cfacet2", queryStr);

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
		queryStr = SearchUtil.generateQueryString(query);
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
		queryStr = SearchUtil.generateQueryString(query);
		assertEquals("q=hello%2Cworld&facet-facet1-sort=alpha", queryStr);
		
		fs = new FacetSort();
		fs.setFacetName("facet2");
		fs.setSortType(FacetSortOptions.COUNT);
		facetFieldSorts = new ArrayList<FacetSort>();
		facetFieldSorts.add(fs);
		query = new SearchQuery();
		query.setQueryTerm(q);
		query.setFacetFieldSort(facetFieldSorts);
		queryStr = SearchUtil.generateQueryString(query);
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
		queryStr = SearchUtil.generateQueryString(query);
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
		queryStr = SearchUtil.generateQueryString(query);
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
		queryStr = SearchUtil.generateQueryString(query);
		assertEquals("q=hello%2Cworld&facet-facet1-top-n=10&facet-facet2-top-n=20", queryStr);
		
		// return fields
		query = new SearchQuery();
		query.setQueryTerm(q);
		query.setReturnFields(Arrays.asList(new String[] { "retF1", "retF2" }));
		queryStr = SearchUtil.generateQueryString(query);
		assertEquals("q=hello%2Cworld&return-fields=retF1%2CretF2", queryStr);
		
		// size
		query = new SearchQuery();
		query.setQueryTerm(q);
		query.setSize(new Long(100));
		queryStr = SearchUtil.generateQueryString(query);
		assertEquals("q=hello%2Cworld&size=100", queryStr);
		
		// start
		query = new SearchQuery();
		query.setQueryTerm(q);
		query.setStart(new Long(10));
		queryStr = SearchUtil.generateQueryString(query);
		assertEquals("q=hello%2Cworld&start=10", queryStr);		
		
	}
}










