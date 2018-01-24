package org.sagebionetworks.search;

import com.amazonaws.services.cloudsearchdomain.model.Bucket;
import com.amazonaws.services.cloudsearchdomain.model.BucketInfo;
import com.amazonaws.services.cloudsearchdomain.model.Hits;
import com.amazonaws.services.cloudsearchdomain.model.QueryParser;
import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import com.amazonaws.services.cloudsearchdomain.model.SearchResult;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.DocumentFields;
import org.sagebionetworks.repo.model.search.DocumentTypeNames;
import org.sagebionetworks.repo.model.search.Facet;
import org.sagebionetworks.repo.model.search.FacetConstraint;
import org.sagebionetworks.repo.model.search.FacetTypeNames;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.FacetSort;
import org.sagebionetworks.repo.model.search.query.FacetSortOptions;
import org.sagebionetworks.repo.model.search.query.FacetTopN;
import org.sagebionetworks.repo.model.search.query.KeyList;
import org.sagebionetworks.repo.model.search.query.KeyValue;
import org.sagebionetworks.repo.model.search.query.SearchQuery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.sagebionetworks.search.SearchConstants.FIELD_CREATED_BY_R;
import static org.sagebionetworks.search.SearchConstants.FIELD_CREATED_ON;
import static org.sagebionetworks.search.SearchConstants.FIELD_DESCRIPTION;
import static org.sagebionetworks.search.SearchConstants.FIELD_DISEASE_R;
import static org.sagebionetworks.search.SearchConstants.FIELD_ETAG;
import static org.sagebionetworks.search.SearchConstants.FIELD_ID;
import static org.sagebionetworks.search.SearchConstants.FIELD_MODIFIED_BY_R;
import static org.sagebionetworks.search.SearchConstants.FIELD_MODIFIED_ON;
import static org.sagebionetworks.search.SearchConstants.FIELD_NAME;
import static org.sagebionetworks.search.SearchConstants.FIELD_NODE_TYPE;
import static org.sagebionetworks.search.SearchConstants.FIELD_NODE_TYPE_R;
import static org.sagebionetworks.search.SearchConstants.FIELD_NUM_SAMPLES;
import static org.sagebionetworks.search.SearchConstants.FIELD_TISSUE_R;

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
		q = new ArrayList<>();
		q.add("hello");
		q.add("world");

		// bq
		bq = new ArrayList<>();
		KeyValue kv = new KeyValue();
		kv.setKey("Facet1");
		kv.setValue("Value1");
		bq.add(kv);

		bq2 = new ArrayList<>();
		kv = new KeyValue();
		kv.setKey("Facet1");
		kv.setValue("..2000");
		bq2.add(kv);

		bqNot = new ArrayList<>();
		kv = new KeyValue();
		kv.setKey("Facet1");
		kv.setValue("Value1");
		kv.setNot(true);
		bqNot.add(kv);
		kv = new KeyValue();
		kv.setKey("Facet2");
		kv.setValue("Value2");
		bqNot.add(kv);

		bqSpecialChar = new ArrayList<>();
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
	public void testNullQuery() {
		// null input
		SearchUtil.generateSearchRequest(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoQueryContent() {
		// no actual query content
		SearchUtil.generateSearchRequest( new SearchQuery() );
	}
	@Test (expected = IllegalArgumentException.class)
	public void testEmptyQuery() {
		// empty query
		query.setQueryTerm(Collections.singletonList(""));
		SearchUtil.generateSearchRequest(query);
	}

	@Test
	public void testRegularQueryOnly() {

		// query only
		query.setQueryTerm(q);
		searchRequest = SearchUtil.generateSearchRequest(query);
		assertEquals(expectedSearchRequestBase.withQuery("(and 'hello' 'world')"), searchRequest);
	}

	@Test
	public void testRegularQueryWithPrefix() {
		// q
		q.add("somePrefix*");
		query.setQueryTerm(q);
		searchRequest = SearchUtil.generateSearchRequest(query);
		assertEquals(expectedSearchRequestBase.withQuery("(and 'hello' 'world' (prefix 'somePrefix'))"), searchRequest);
	}

	@Test
	public void testBooleanQuery() {
		// boolean query only
		query.setBooleanQuery(bq);
		searchRequest = SearchUtil.generateSearchRequest(query);
		assertEquals(expectedSearchRequestBase.withQuery("(and Facet1:'Value1')"), searchRequest);
	}

	@Test
	public void testBooleanQueryWithPrefix() {
		KeyValue prefixKV = new KeyValue();
		prefixKV.setKey("someField");
		prefixKV.setValue("somePrefix*");
		bq.add(prefixKV);
		query.setBooleanQuery(bq);
		searchRequest = SearchUtil.generateSearchRequest(query);
		assertEquals(expectedSearchRequestBase.withQuery("(and Facet1:'Value1' (prefix field=someField 'somePrefix'))"), searchRequest);
	}

	@Test
	public void testBooleanQueryWithBlankRegularQuery() {
		// boolean query with blank single q
		query.setQueryTerm(Collections.singletonList(""));
		query.setBooleanQuery(bq);
		searchRequest = SearchUtil.generateSearchRequest(query);
		assertEquals(expectedSearchRequestBase.withQuery("(and Facet1:'Value1')"), searchRequest);
	}

	@Test
	public void testBooleanQueryContinuous() {
		// continuous bq
		query.setBooleanQuery(bq2);
		searchRequest = SearchUtil.generateSearchRequest(query);
		assertEquals(expectedSearchRequestBase.withQuery("(and (range field=Facet1 {,2000]))"), searchRequest);
	}

	@Test
	public void testNegatedBooleanQuery() {
		// negated boolean query
		query.setBooleanQuery(bqNot);
		searchRequest = SearchUtil.generateSearchRequest(query);
		assertEquals(expectedSearchRequestBase.withQuery("(and (not Facet1:'Value1') Facet2:'Value2')"), searchRequest);
	}

	@Test
	public void testSpecialCharactersInBooleanQuery() {
		// special characters in boolean query
		query.setBooleanQuery(bqSpecialChar);
		searchRequest = SearchUtil.generateSearchRequest(query);
		assertEquals(expectedSearchRequestBase.withQuery("(and Facet1:'c:\\\\dave\\'s_folde,r')"), searchRequest);
	}
	@Test
	public void testRegularQueryAndBooleanQuery() {
		// Both q and bq
		query.setBooleanQuery(bq);
		query.setQueryTerm(q);
		searchRequest = SearchUtil.generateSearchRequest(query);
		assertEquals(expectedSearchRequestBase.withQuery("(and (and 'hello' 'world') Facet1:'Value1')"), searchRequest);
	}

	@Test
	public void testFacets() {
		// facets
		query.setQueryTerm(q);
		List<String> facets = new ArrayList<>();
		facets.add("facet1");
		facets.add("facet2");
		query.setQueryTerm(q);
		query.setFacet(facets);
		searchRequest = SearchUtil.generateSearchRequest(query);
		assertEquals( expectedSearchRequestBase.withQuery("(and 'hello' 'world')").withFacet("{\"facet1\":{},\"facet2\":{}}"), searchRequest);
	}


	@Test (expected = IllegalArgumentException.class)
	public void testFacetConstraints() {
		// facet field constraints
		query.setQueryTerm(q);
		List<KeyList> facetFieldConstraints = new ArrayList<>();
		KeyList ffc1 = new KeyList();
		ffc1.setKey("facet1");
		ffc1.setValues(Arrays.asList("one,two\\three", "dave's", "regular"));
		facetFieldConstraints.add(ffc1);
		KeyList ffc2 = new KeyList();
		ffc2.setKey("facet2");
		ffc2.setValues(Arrays.asList("123", "4..5"));
		facetFieldConstraints.add(ffc2);
		query.setFacetFieldConstraints(facetFieldConstraints);
		searchRequest = SearchUtil.generateSearchRequest(query);
	}
	@Test (expected = IllegalArgumentException.class)
	public void testFacetSort() {
		// facet field sort
		query.setQueryTerm(q);
		List<FacetSort> facetFieldSorts = null;
		FacetSort fs = null;

		fs = new FacetSort();
		fs.setFacetName("facet1");
		fs.setSortType(FacetSortOptions.ALPHA);
		facetFieldSorts = new ArrayList<>();
		facetFieldSorts.add(fs);
		query.setQueryTerm(q);
		query.setFacetFieldSort(facetFieldSorts);
		searchRequest = SearchUtil.generateSearchRequest(query);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFacetFieldTopN() {
		// facet field top N
		List<FacetTopN> topNList = new ArrayList<>();
		FacetTopN topn = null;

		topn = new FacetTopN();
		topn.setKey("facet1");
		topn.setValue(10L);
		topNList.add(topn);

		topn = new FacetTopN();
		topn.setKey("facet2");
		topn.setValue(20L);
		topNList.add(topn);

		query.setQueryTerm(q);
		query.setFacetFieldTopN(topNList);
		searchRequest = SearchUtil.generateSearchRequest(query);
	}
	@Test (expected=IllegalArgumentException.class)
	public void testRank() {
		query.setQueryTerm(q);
		query.setRank(Arrays.asList("rankfield1", "-rankfield2"));
		searchRequest = SearchUtil.generateSearchRequest(query);
	}

	@Test
	public void testReturnFields() {
		// return fields
		query.setQueryTerm(q);
		query.setReturnFields(Arrays.asList("retF1", "retF2"));
		searchRequest = SearchUtil.generateSearchRequest(query);
		assertEquals(expectedSearchRequestBase.withQuery("(and 'hello' 'world')").withReturn("retF1,retF2"), searchRequest);

	}
	@Test
	public void testSizeParameter() {
		// size
		query.setQueryTerm(q);
		query.setSize(100L);
		searchRequest = SearchUtil.generateSearchRequest(query);
		assertEquals( expectedSearchRequestBase.withQuery("(and 'hello' 'world')").withSize(100L), searchRequest);
	}
	@Test
	public void testStartParameter() {
		// start
		query.setQueryTerm(q);
		query.setStart(10L);
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

	@Test
	public void testGetFistValueFromMapNullList(){
		assertEquals(null, SearchUtil.getFirstListValueFromMap(Collections.emptyMap(), "someKey"));
	}

	@Test
	public void testGetFistValueFromMapEmptyList(){
		assertEquals(null, SearchUtil.getFirstListValueFromMap(Collections.singletonMap("someKey",Collections.emptyList()), "someKey"));
	}

	@Test
	public void testGetFistValueFromMapSingleValueList(){
		String retrievedValue = SearchUtil.getFirstListValueFromMap(
				Collections.singletonMap("someKey",Collections.singletonList("someValue")),
				"someKey");
		assertEquals("someValue", retrievedValue);
	}

	@Test
	public void testGetFistValueFromMapMultipleValueList(){
		String retrievedValue = SearchUtil.getFirstListValueFromMap(
				Collections.singletonMap("someKey",Arrays.asList("firstValue", "secondValue")),
				"someKey");
		assertEquals("firstValue", retrievedValue);
	}

	@Test
	public void testConvertToSynapseHitAllFieldsWithValue() {
		//Lots of setup
		String createdBy = "1213324";
		String createdOn = "1234567890";
		String description = "Description";
		String disease = "space aids";
		String etag = "some etag";
		String id = "id";
		String modifiedBy = "modifiedBy";
		String modifiedOn = "11958442069423";
		String name = "my name Jeff";
		String nodeType = "dataset";
		String numSamples = "42";
		String tissue = "Kleenex";

		Map<String, List<String>> hitFields = new HashMap<String, List<String>>() {
			{
				put(FIELD_CREATED_BY_R, Collections.singletonList(createdBy));
				put(FIELD_CREATED_ON, Collections.singletonList(createdOn));
				put(FIELD_DESCRIPTION, Collections.singletonList(description));
				put(FIELD_DISEASE_R, Collections.singletonList(disease));
				put(FIELD_ETAG, Collections.singletonList(etag));
				put(FIELD_ID, Collections.singletonList(id));
				put(FIELD_MODIFIED_BY_R, Collections.singletonList(modifiedBy));
				put(FIELD_MODIFIED_ON, Collections.singletonList(modifiedOn));
				put(FIELD_NAME, Collections.singletonList(name));
				put(FIELD_NODE_TYPE_R, Collections.singletonList(nodeType));
				put(FIELD_NUM_SAMPLES, Collections.singletonList(numSamples));
				put(FIELD_TISSUE_R, Collections.singletonList(tissue));
			}
		};
		//end of setup

		org.sagebionetworks.repo.model.search.Hit hit = SearchUtil.convertToSynapseHit(new com.amazonaws.services.cloudsearchdomain.model.Hit().withFields(hitFields).withId(id));

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
				put(FIELD_NODE_TYPE, new BucketInfo().withBuckets(node_type_buckets));
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
				put(FIELD_NODE_TYPE, new BucketInfo());
				put(FIELD_MODIFIED_ON, new BucketInfo());
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


	////////////////////////////////////////
	// formulateAuthorizationFilter() tests
	///////////////////////////////////////
	@Test (expected = IllegalArgumentException.class)
	public void testFormulateAuthorizationFilterNullUserInfo(){
		SearchUtil.formulateAuthorizationFilter(null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testFormulateAuthorizationFilterNullUserGroups(){
		UserInfo userInfo = new UserInfo(false);
		userInfo.setGroups(null);
		SearchUtil.formulateAuthorizationFilter(userInfo);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testFormulateAuthorizationFilterEmptyUserGroups(){
		UserInfo userInfo = new UserInfo(false);
		userInfo.setGroups(new HashSet<>());
		SearchUtil.formulateAuthorizationFilter(userInfo);
	}

	@Test
	public void testFormulateAuthorizationFilterHappyCase(){
		UserInfo userInfo = new UserInfo(false);
		userInfo.setGroups(Sets.newLinkedHashSet(Arrays.asList(123L, 456L, 789L)));
		String authFilter = SearchUtil.formulateAuthorizationFilter(userInfo);
		assertEquals("(or acl:'123' acl:'456' acl:'789')", authFilter);
	}

	///////////////////////////
	// prepareDocument tests
	///////////////////////////
	@Test
	public void testPrepareDocument(){
		Document doc = new Document();
		doc.setId("123");
		// This should prepare the document to be sent
		SearchUtil.prepareDocument(doc);
		assertNotNull(doc.getFields());
		assertEquals("The document ID must be set in the fields when ",doc.getId(), doc.getFields().getId());
	}

	////////////////////////////////////////
	// convertSearchDocumentsToJSONString() test
	////////////////////////////////////////

	@Test(expected = IllegalArgumentException.class)
	public void testConvertSearchDocumentsToJSONStringNullDocuments(){
		SearchUtil.convertSearchDocumentsToJSONString(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConvertSearchDocumentsToJSONStringEmptyDocuments(){
		SearchUtil.convertSearchDocumentsToJSONString(new LinkedList<>());
	}

	@Test
	public void testConvertSearchDocumentsToJSONString(){
		Document deleteDoc = new Document();
		deleteDoc.setId("syn123");
		deleteDoc.setType(DocumentTypeNames.delete);

		Document addDoc = new Document();
		addDoc.setId("syn456");
		addDoc.setType(DocumentTypeNames.add);
		DocumentFields fields = new DocumentFields();
		fields.setName("Fake Entity");
		addDoc.setFields(fields);

		String jsonString = SearchUtil.convertSearchDocumentsToJSONString(Arrays.asList(deleteDoc,addDoc));
		assertEquals("[{\"type\":\"delete\",\"id\":\"syn123\"}, {\"type\":\"add\",\"id\":\"syn456\",\"fields\":{\"name\":\"Fake Entity\",\"id\":\"syn456\"}}]", jsonString);
	}

}
