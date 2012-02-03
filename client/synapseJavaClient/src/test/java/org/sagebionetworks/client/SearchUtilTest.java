package org.sagebionetworks.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
	public void generateQueryStringTest() {
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
			assertNotNull(null);
		} catch (InvalidArgumentException ex) {
			assertNull(null);
		}
		
		// query only
		query = new SearchQuery();
		query.setQueryTerm(q);
		queryStr = SearchUtil.generateQueryString(query);
		assertEquals("q=hello,world", queryStr);

		// boolean query only
		query = new SearchQuery();
		query.setBooleanQuery(bq);
		queryStr = SearchUtil.generateQueryString(query);
		assertEquals("bq=Facet1:'Value1'", queryStr);
		
		// continuous bq
		query = new SearchQuery();
		query.setBooleanQuery(bq2);
		queryStr = SearchUtil.generateQueryString(query);
		assertEquals("bq=Facet1:..2000", queryStr);
		
		// Both q and bq
		query = new SearchQuery();
		query.setBooleanQuery(bq);
		query.setQueryTerm(q);
		queryStr = SearchUtil.generateQueryString(query);
		assertEquals("q=hello,world&bq=Facet1:'Value1'", queryStr);
		
		// facets
		query = new SearchQuery();
		query.setQueryTerm(q);
		List<String> facets = new ArrayList<String>();
		facets.add("facet1");
		facets.add("facet2");
		query.setQueryTerm(q);
		query.setFacet(facets);
		queryStr = SearchUtil.generateQueryString(query);
		assertEquals("q=hello,world&facet=facet1,facet2", queryStr);

		// facet field constraints
		query = new SearchQuery();
		query.setQueryTerm(q);
		List<KeyList> facetFieldConstraints = new ArrayList<KeyList>();
		KeyList ffc1 = new KeyList();
		ffc1.setKey("facet1");		
		ffc1.setValues(Arrays.asList(new String[] { "ffc1v1", "ffc1v2" }));
		facetFieldConstraints.add(ffc1);
		KeyList ffc2 = new KeyList();
		ffc2.setKey("facet2");		
		ffc2.setValues(Arrays.asList(new String[] { "ffc2v1" }));
		facetFieldConstraints.add(ffc2);
		query.setFacetFieldConstraints(facetFieldConstraints);
		queryStr = SearchUtil.generateQueryString(query);
		assertEquals("q=hello,world&facet-facet1-constraints=ffc1v1,ffc1v2&facet-facet2-constraints=ffc2v1", queryStr);

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
		assertEquals("q=hello,world&facet-facet1-sort=alpha", queryStr);
		
		fs = new FacetSort();
		fs.setFacetName("facet2");
		fs.setSortType(FacetSortOptions.COUNT);
		facetFieldSorts = new ArrayList<FacetSort>();
		facetFieldSorts.add(fs);
		query = new SearchQuery();
		query.setQueryTerm(q);
		query.setFacetFieldSort(facetFieldSorts);
		queryStr = SearchUtil.generateQueryString(query);
		assertEquals("q=hello,world&facet-facet2-sort=count", queryStr);
		
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
		assertEquals("q=hello,world&facet-facet3-sort=max(maxfield)", queryStr);

				
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
		assertEquals("q=hello,world&facet-facet4-sort=sum(sum1,sum2)", queryStr);
		

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
		assertEquals("q=hello,world&facet-facet1-top-n=10&facet-facet2-top-n=20", queryStr);
		
		// return fields
		query = new SearchQuery();
		query.setQueryTerm(q);
		query.setReturnFields(Arrays.asList(new String[] { "retF1", "retF2" }));
		queryStr = SearchUtil.generateQueryString(query);
		assertEquals("q=hello,world&return-fields=retF1,retF2", queryStr);
		
		// size
		query = new SearchQuery();
		query.setQueryTerm(q);
		query.setSize(new Long(100));
		queryStr = SearchUtil.generateQueryString(query);
		assertEquals("q=hello,world&size=100", queryStr);
		
		// start
		query = new SearchQuery();
		query.setQueryTerm(q);
		query.setStart(new Long(10));
		queryStr = SearchUtil.generateQueryString(query);
		assertEquals("q=hello,world&start=10", queryStr);		
		
	}
}










