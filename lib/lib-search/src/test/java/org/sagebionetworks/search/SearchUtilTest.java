package org.sagebionetworks.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.cloudsearchdomain.model.Bucket;
import com.amazonaws.services.cloudsearchdomain.model.BucketInfo;
import com.amazonaws.services.cloudsearchdomain.model.Hits;
import com.amazonaws.services.cloudsearchdomain.model.QueryParser;
import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import com.amazonaws.services.cloudsearchdomain.model.SearchResult;
import com.amazonaws.services.cloudsearchdomain.model.SearchStatus;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.search.Facet;
import org.sagebionetworks.repo.model.search.FacetConstraint;
import org.sagebionetworks.repo.model.search.FacetTypeNames;
import org.sagebionetworks.repo.model.search.query.FacetSort;
import org.sagebionetworks.repo.model.search.query.FacetSortOptions;
import org.sagebionetworks.repo.model.search.query.FacetTopN;
import org.sagebionetworks.repo.model.search.query.KeyList;
import org.sagebionetworks.repo.model.search.query.KeyValue;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.repo.model.search.SearchResults;

public class SearchUtilTest {
	private SearchQuery query;
	private SearchRequest searchRequest;
	private SearchRequest expectedSearchRequestBase;
	private SearchResult searchResult;

	private List<String> q;
	private List<KeyValue> bq;
	private List<KeyValue> bqNot;
	private List<KeyValue> bq2;
	private List<KeyValue> bqSpecialChar;

	@Before
	public void before() throws Exception {
		query = new SearchQuery();
		expectedSearchRequestBase =  new SearchRequest().withQueryParser(QueryParser.Structured);
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

		searchResult = new SearchResult();
	}

	//////////////////////////////////////
	// generateSearchRequest() tests
	/////////////////////////////////////

	@Test (expected = IllegalArgumentException.class)
	public void testNullQuery() throws Exception{
		// null input
		SearchUtil.generateSearchRequest(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoQueryContent() throws Exception{
		// no actual query content
		SearchUtil.generateSearchRequest( new SearchQuery() );
	}
	@Test (expected = IllegalArgumentException.class)
	public void testEmptyQuery() throws Exception{
		// empty query
		query.setQueryTerm(Arrays.asList(new String[] {""}));
		SearchUtil.generateSearchRequest(query);
	}

	@Test
	public void testRegularQueryOnly() throws Exception{

		// query only
		query.setQueryTerm(q);
		searchRequest = SearchUtil.generateSearchRequest(query);
		assertEquals(expectedSearchRequestBase.withQuery("(and 'hello' 'world')"), searchRequest);
	}

	@Test
	public void testRegularQueryWithPrefix() throws Exception{
		// q
		q.add("somePrefix*");
		query.setQueryTerm(q);
		searchRequest = SearchUtil.generateSearchRequest(query);
		assertEquals(expectedSearchRequestBase.withQuery("(and 'hello' 'world' (prefix 'somePrefix'))"), searchRequest);
	}

	@Test
	public void testBooleanQuery() throws Exception{
		// boolean query only
		query.setBooleanQuery(bq);
		searchRequest = SearchUtil.generateSearchRequest(query);
		assertEquals(expectedSearchRequestBase.withQuery("(and Facet1:'Value1')"), searchRequest);
	}

	@Test
	public void testBooleanQueryWithPrefix() throws Exception{
		KeyValue prefixKV = new KeyValue();
		prefixKV.setKey("someField");
		prefixKV.setValue("somePrefix*");
		bq.add(prefixKV);
		query.setBooleanQuery(bq);
		searchRequest = SearchUtil.generateSearchRequest(query);
		assertEquals(expectedSearchRequestBase.withQuery("(and Facet1:'Value1' (prefix field=someField 'somePrefix'))"), searchRequest);
	}

	@Test
	public void testBooleanQueryWithBlankRegularQuery() throws Exception{
		// boolean query with blank single q
		query.setQueryTerm(Arrays.asList(new String[] {""}));
		query.setBooleanQuery(bq);
		searchRequest = SearchUtil.generateSearchRequest(query);
		assertEquals(expectedSearchRequestBase.withQuery("(and Facet1:'Value1')"), searchRequest);
	}

	@Test
	public void testBooleanQueryContinuous() throws Exception{
		// continuous bq
		query.setBooleanQuery(bq2);
		searchRequest = SearchUtil.generateSearchRequest(query);
		assertEquals(expectedSearchRequestBase.withQuery("(and (range field=Facet1 {,2000]))"), searchRequest);
	}

	@Test
	public void testNegatedBooleanQuery() throws Exception{
		// negated boolean query
		query.setBooleanQuery(bqNot);
		searchRequest = SearchUtil.generateSearchRequest(query);
		assertEquals(expectedSearchRequestBase.withQuery("(and (not Facet1:'Value1') Facet2:'Value2')"), searchRequest);
	}

	@Test
	public void testSpecialCharactersInBooleanQuery() throws Exception{
		// special characters in boolean query
		query.setBooleanQuery(bqSpecialChar);
		searchRequest = SearchUtil.generateSearchRequest(query);
		assertEquals(expectedSearchRequestBase.withQuery("(and Facet1:'c:\\\\dave\\'s_folde,r')"), searchRequest);
	}
	@Test
	public void testRegularQueryAndBooleanQuery() throws Exception{
		// Both q and bq
		query.setBooleanQuery(bq);
		query.setQueryTerm(q);
		searchRequest = SearchUtil.generateSearchRequest(query);
		assertEquals(expectedSearchRequestBase.withQuery("(and (and 'hello' 'world') Facet1:'Value1')"), searchRequest);
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
		searchRequest = SearchUtil.generateSearchRequest(query);
		assertEquals( expectedSearchRequestBase.withQuery("(and 'hello' 'world')").withFacet("\"facet1\":{},\"facet2\":{}}"), searchRequest);
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
		searchRequest = SearchUtil.generateSearchRequest(query);
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
		searchRequest = SearchUtil.generateSearchRequest(query);
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
		searchRequest = SearchUtil.generateSearchRequest(query);
	}
	@Test (expected=IllegalArgumentException.class)
	public void testRank() throws Exception{
		query.setQueryTerm(q);
		query.setRank(Arrays.asList(new String[]{"rankfield1", "-rankfield2"}));
		searchRequest = SearchUtil.generateSearchRequest(query);
	}

	@Test
	public void testReturnFields() throws Exception{
		// return fields
		query.setQueryTerm(q);
		query.setReturnFields(Arrays.asList(new String[] { "retF1", "retF2" }));
		searchRequest = SearchUtil.generateSearchRequest(query);
		assertEquals(expectedSearchRequestBase.withQuery("(and 'hello' 'world')").withReturn("retF1,retF2"), searchRequest);

	}
	@Test
	public void testSizeParameter() throws Exception{
		// size
		query.setQueryTerm(q);
		query.setSize(new Long(100));
		searchRequest = SearchUtil.generateSearchRequest(query);
		assertEquals( expectedSearchRequestBase.withQuery("(and 'hello' 'world')").withSize(100L), searchRequest);
	}
	@Test
	public void testStartParameter() throws Exception{
		// start
		query.setQueryTerm(q);
		query.setStart(new Long(10));
		searchRequest = SearchUtil.generateSearchRequest(query);
		assertEquals( expectedSearchRequestBase.withQuery("(and 'hello' 'world')").withStart(10L), searchRequest);
	}

	/////////////////////////////////
	// convertToSynapseSearchResult
	/////////////////////////////////

	@Test
	public void nullHitsNullFacets(){
		searchResult.setFacets(null);
		searchResult.setHits(null);
		SearchResults convertedResults = SearchUtil.convertToSynapseSearchResult(searchResult);
		assertNotNull(convertedResults.getHits());
		assertNotNull(convertedResults.getFacets());
		assertEquals(0, convertedResults.getFacets().size());
		assertEquals(0, convertedResults.getHits().size());
		assertEquals(null, convertedResults.getFound());
		assertEquals(null, convertedResults.getStart());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testHitTransaltionAllFields() {
		long found = 1;
		long start = 0;
		//Lots of setup
		String createdBy = "1213324";
		String createdOn = "1234567890";
		String description = "Description";
		String disease = "space aids";
		String etag = "etag";
		String id = "id";
		String modifiedBy = "modifiedBy";
		String modifiedOn = "11958442069423";
		String name = "my name Jeff";
		String nodeType = "dataset";
		String numSamples = "42";
		String tissue = "Kleenex";

		Map<String, List<String>> hitFields = new HashMap<String, List<String>>() {
			{
				put(SearchUtil.CREATED_BY_RETURN_FIELD, Collections.singletonList(createdBy));
				put(SearchUtil.CREATED_ON_FIELD, Collections.singletonList(createdOn));
				put(SearchUtil.DESCRIPTION_FIELD, Collections.singletonList(description));
				put(SearchUtil.DISEASE_RETURN_FIELD, Collections.singletonList(disease));
				put(SearchUtil.ETAG_FIELD, Collections.singletonList(etag));
				put(SearchUtil.ID_FIELD, Collections.singletonList(id));
				put(SearchUtil.MODIFIED_BY_RETURN_FIELD, Collections.singletonList(modifiedBy));
				put(SearchUtil.MODIFIED_ON_FIELD, Collections.singletonList(modifiedOn));
				put(SearchUtil.NAME_FIELD, Collections.singletonList(name));
				put(SearchUtil.NODE_TYPE_RETURN_FIELD, Collections.singletonList(nodeType));
				put(SearchUtil.NUM_SAMPLES_FIELD, Collections.singletonList(numSamples));
				put(SearchUtil.TISSUE_RETURN_FIELD, Collections.singletonList(tissue));
			}
		};

		searchResult.withHits(new Hits().withFound(found).withStart(start).withHit(new com.amazonaws.services.cloudsearchdomain.model.Hit().withFields(hitFields).withId(id)));
		//end of setup

		SearchResults convertedResults = SearchUtil.convertToSynapseSearchResult(searchResult);

		assertEquals(found, convertedResults.getHits().size());
		assertEquals((Long) start, convertedResults.getStart());
		assertEquals((Long) found, convertedResults.getFound());

		org.sagebionetworks.repo.model.search.Hit hit = convertedResults.getHits().get(0);
		assertEquals(id, hit.getId());
		assertEquals(name, hit.getName());
		assertEquals(description, hit.getDescription());
		assertEquals(etag, hit.getEtag());
		assertEquals(new Long(modifiedOn), hit.getModified_on());
		assertEquals(new Long(createdOn), hit.getCreated_on());
		assertEquals(new Long(numSamples), hit.getNum_samples());
		assertEquals(createdBy, hit.getCreated_by());
		assertEquals(modifiedBy, hit.getModified_by());
		assertEquals(nodeType, hit.getNode_type());
		assertEquals(disease, hit.getDisease());
		assertEquals(tissue, hit.getTissue());
	}


	@Test
	public void testMultipleHits(){
		long found = 2;
		long start = 0;
		com.amazonaws.services.cloudsearchdomain.model.Hit[] hits = new com.amazonaws.services.cloudsearchdomain.model.Hit[]{new com.amazonaws.services.cloudsearchdomain.model.Hit(),
				new com.amazonaws.services.cloudsearchdomain.model.Hit()};
		SearchResult searchResult = new SearchResult().withHits(new Hits().withFound(found)
																		.withStart(start)
																		.withHit(hits));

		SearchResults convertedResults = SearchUtil.convertToSynapseSearchResult(searchResult);
		assertEquals(found, convertedResults.getHits().size());
		assertEquals((Long) found, convertedResults.getFound());
		assertEquals((Long) start, convertedResults.getStart());

	}


	@Test
	public void testFacetResultsConstraintValueCountTranslation(){
		Bucket[] node_type_buckets = {new Bucket().withValue("file").withCount(3L),
				new Bucket().withValue("project").withCount(4L)};

		Map<String, BucketInfo> bucketMap = new HashMap<String, BucketInfo>(){
			{
				put(SearchUtil.NODE_TYPE_FACET_FIELD, new BucketInfo().withBuckets(node_type_buckets));
			}
		};

		searchResult.setFacets(bucketMap);

		SearchResults convertedResults = SearchUtil.convertToSynapseSearchResult(searchResult);

		assertEquals(1, convertedResults.getFacets().size());

		Facet facet = convertedResults.getFacets().get(0);
		assertEquals(FacetTypeNames.LITERAL, facet.getType());

		List<FacetConstraint> constraints = facet.getConstraints();
		assertEquals(2, constraints.size());
		for (FacetConstraint constraint : constraints) {
			String constraintVal = constraint.getValue();
			Long constraintCount = constraint.getCount();
			if ("file".equals(constraintVal)) {
				assertEquals((Long) 3L, constraintCount);
			} else if ("project".equals(constraintVal)){
				assertEquals((Long) 4L, constraintCount);
			} else{
				fail("Somehow got a unexpected constraint value");
			}
		}

	}


	@Test
	public void testFacetResultsMultipleResultConstraints(){
		Map<String, BucketInfo> bucketMap = new HashMap<String, BucketInfo>(){
			{
				put(SearchUtil.NODE_TYPE_FACET_FIELD, new BucketInfo());
				put(SearchUtil.MODIFIED_ON_FIELD, new BucketInfo());
			}
		};
		searchResult.setFacets(bucketMap);
		SearchResults convertedResults = SearchUtil.convertToSynapseSearchResult(searchResult);
		assertEquals(2, convertedResults.getFacets().size());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFacetResultsUnknownField(){
		Map<String, BucketInfo> bucketMap = new HashMap<String, BucketInfo>(){
			{
				put("FAKE FIELD", new BucketInfo());
			}
		};
		searchResult.setFacets(bucketMap);
		SearchResults convertedResults = SearchUtil.convertToSynapseSearchResult(searchResult);
	}

}
