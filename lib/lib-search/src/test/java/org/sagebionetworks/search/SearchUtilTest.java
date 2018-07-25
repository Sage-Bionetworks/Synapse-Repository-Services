package org.sagebionetworks.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.sagebionetworks.search.SearchConstants.FIELD_CONSORTIUM;
import static org.sagebionetworks.search.SearchConstants.FIELD_CREATED_BY;
import static org.sagebionetworks.search.SearchConstants.FIELD_CREATED_ON;
import static org.sagebionetworks.search.SearchConstants.FIELD_DESCRIPTION;
import static org.sagebionetworks.search.SearchConstants.FIELD_DISEASE;
import static org.sagebionetworks.search.SearchConstants.FIELD_ETAG;
import static org.sagebionetworks.search.SearchConstants.FIELD_MODIFIED_BY;
import static org.sagebionetworks.search.SearchConstants.FIELD_MODIFIED_ON;
import static org.sagebionetworks.search.SearchConstants.FIELD_NAME;
import static org.sagebionetworks.search.SearchConstants.FIELD_NODE_TYPE;
import static org.sagebionetworks.search.SearchConstants.FIELD_NUM_SAMPLES;
import static org.sagebionetworks.search.SearchConstants.FIELD_TISSUE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.cloudsearchdomain.model.Bucket;
import com.amazonaws.services.cloudsearchdomain.model.BucketInfo;
import com.amazonaws.services.cloudsearchdomain.model.Hits;
import com.amazonaws.services.cloudsearchdomain.model.QueryParser;
import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import com.amazonaws.services.cloudsearchdomain.model.SearchResult;
import com.google.common.collect.Sets;
import org.json.JSONObject;
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
import org.sagebionetworks.repo.model.search.query.KeyRange;
import org.sagebionetworks.repo.model.search.query.KeyValue;
import org.sagebionetworks.repo.model.search.query.SearchFacetOption;
import org.sagebionetworks.repo.model.search.query.SearchFacetSort;
import org.sagebionetworks.repo.model.search.query.SearchFieldName;
import org.sagebionetworks.repo.model.search.query.SearchQuery;

public class SearchUtilTest {
	private SearchQuery query;
	private SearchRequest searchRequest;
	private SearchRequest expectedSearchRequestBaseWithQueryTerm;
	private SearchRequest expectedSearchRequestBaseNoQueryTermSet;
	private SearchResult searchResult;
	private SearchFacetOption searchFacetOption;

	private List<String> q;
	private List<KeyValue> bq;
	private List<KeyValue> bqNot;
	private List<KeyValue> bq2;
	private List<KeyValue> bqSpecialChar;
	private List<KeyRange> keyRangeList;
	private KeyRange keyRange;

	UserInfo userInfo;
	@Before
	public void before() throws Exception {
		query = new SearchQuery();
		expectedSearchRequestBaseWithQueryTerm =  new SearchRequest().withQueryParser(QueryParser.Simple);
		expectedSearchRequestBaseNoQueryTermSet = new SearchRequest().withQueryParser(QueryParser.Structured).withQuery("matchall");
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

		userInfo = new UserInfo(false);
		userInfo.setGroups(Sets.newLinkedHashSet(Arrays.asList(123L, 456L, 789L)));

		keyRangeList = new ArrayList<>();
		keyRange = new KeyRange();
		keyRange.setKey("SomeRangeFacet");
		keyRangeList.add(keyRange);

		searchFacetOption = new SearchFacetOption();
		searchFacetOption.setMaxResultCount(42L);
		searchFacetOption.setSortType(SearchFacetSort.COUNT);
		searchFacetOption.setName(SearchFieldName.EntityType);
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
		assertEquals(expectedSearchRequestBaseWithQueryTerm.withQuery("hello world"), searchRequest);
	}

	@Test
	public void testBooleanQuery() {
		// boolean query only
		query.setBooleanQuery(bq);
		searchRequest = SearchUtil.generateSearchRequest(query);
		assertEquals(expectedSearchRequestBaseNoQueryTermSet.withFilterQuery("(and Facet1:'Value1')"), searchRequest);
	}

	@Test
	public void testBooleanQueryWithBlankRegularQuery() {
		// boolean query with blank single q
		query.setQueryTerm(Collections.singletonList(""));
		query.setBooleanQuery(bq);
		searchRequest = SearchUtil.generateSearchRequest(query);
		assertEquals(expectedSearchRequestBaseNoQueryTermSet.withFilterQuery("(and Facet1:'Value1')"), searchRequest);
	}

	@Test
	public void testNegatedBooleanQuery() {
		// negated boolean query
		query.setBooleanQuery(bqNot);
		searchRequest = SearchUtil.generateSearchRequest(query);
		assertEquals(expectedSearchRequestBaseNoQueryTermSet.withFilterQuery("(and (not Facet1:'Value1') Facet2:'Value2')"), searchRequest);
	}

	@Test
	public void testSpecialCharactersInBooleanQuery() {
		// special characters in boolean query
		query.setBooleanQuery(bqSpecialChar);
		searchRequest = SearchUtil.generateSearchRequest(query);
		assertEquals(expectedSearchRequestBaseNoQueryTermSet.withFilterQuery("(and Facet1:'c:\\\\dave\\'s_folde,r')"), searchRequest);
	}
	@Test
	public void testRegularQueryAndBooleanQuery() {
		// Both q and bq
		query.setBooleanQuery(bq);
		query.setQueryTerm(q);
		searchRequest = SearchUtil.generateSearchRequest(query);
		assertEquals(expectedSearchRequestBaseWithQueryTerm.withQuery("hello world").withFilterQuery("(and Facet1:'Value1')"), searchRequest);
	}

	@Test
	public void testGenerateSearchRequest_rangeQueriesOnly(){
		query.setQueryTerm(q);
		query.setRangeQuery(keyRangeList);
		keyRange.setMax("42");

		searchRequest = SearchUtil.generateSearchRequest(query);

		assertEquals(expectedSearchRequestBaseWithQueryTerm.withQuery("hello world")
				.withFilterQuery("(and (range field=SomeRangeFacet {,42]))"), searchRequest);
	}

	@Test
	public void testGenerateSearchRequest_booleanQueriesAndRangeQueries(){
		query.setBooleanQuery(bq);
		query.setQueryTerm(q);
		query.setRangeQuery(keyRangeList);
		keyRange.setMax("42");

		searchRequest = SearchUtil.generateSearchRequest(query);

		assertEquals(expectedSearchRequestBaseWithQueryTerm.withQuery("hello world")
				.withFilterQuery("(and Facet1:'Value1' (range field=SomeRangeFacet {,42]))"), searchRequest);
	}

	@Test
	public void testBooleanQueryWithBrackets(){
		KeyValue kv = new KeyValue();
		kv.setKey("disease");
		kv.setValue("[\"normal\",\"carcinoma\"]");
		query.setQueryTerm(q);
		query.setBooleanQuery(Collections.singletonList(kv));

		searchRequest = SearchUtil.generateSearchRequest(query);

		assertEquals(expectedSearchRequestBaseWithQueryTerm.withQuery("hello world")
				.withFilterQuery("(and disease:'[\"normal\",\"carcinoma\"]')"), searchRequest);	}

	@Test
	public void testReturnFields() {
		// return fields
		query.setQueryTerm(q);
		query.setReturnFields(Arrays.asList("retF1", "retF2"));
		searchRequest = SearchUtil.generateSearchRequest(query);
		assertEquals(expectedSearchRequestBaseWithQueryTerm.withQuery("hello world").withReturn("retF1,retF2"), searchRequest);

	}
	@Test
	public void testSizeParameter() {
		// size
		query.setQueryTerm(q);
		query.setSize(100L);
		searchRequest = SearchUtil.generateSearchRequest(query);
		assertEquals( expectedSearchRequestBaseWithQueryTerm.withQuery("hello world").withSize(100L), searchRequest);
	}
	@Test
	public void testStartParameter() {
		// start
		query.setQueryTerm(q);
		query.setStart(10L);
		searchRequest = SearchUtil.generateSearchRequest(query);
		assertEquals( expectedSearchRequestBaseWithQueryTerm.withQuery("hello world").withStart(10L), searchRequest);
	}

	@Test
	public void testGenerateSearchRequest_FacetOptions_usingFacetOptions(){
		query.setQueryTerm(q);
		query.setFacetOptions(Collections.singletonList(searchFacetOption));
		searchRequest = SearchUtil.generateSearchRequest(query);
		assertEquals(expectedSearchRequestBaseWithQueryTerm.withQuery("hello world").withFacet("{\"node_type\":{\"sort\":\"count\",\"size\":42}}"), searchRequest);
	}

	///////////////////////////////////
	// createRangeFilterQueries() test
	///////////////////////////////////
	@Test (expected = IllegalArgumentException.class)
	public void testCreateRangeFilterQueries_keyRangeWithNullMinAndNullMax(){
		keyRange.setMin(null);
		keyRange.setMax(null);

		SearchUtil.createRangeFilterQueries(keyRangeList);
	}

	@Test
	public void testCreateRangeFilterQueries_keyRangeWithNullMin(){
		keyRange.setMin("56");
		keyRange.setMax(null);

		List<String> rangeFilterQueries = SearchUtil.createRangeFilterQueries(keyRangeList);
		assertEquals(Arrays.asList("(range field=SomeRangeFacet [56,})"), rangeFilterQueries);
	}

	@Test
	public void testCreateRangeFilterQueries_keyRangeWithNullMax(){
		keyRange.setMin(null);
		keyRange.setMax("89");

		List<String> rangeFilterQueries = SearchUtil.createRangeFilterQueries(keyRangeList);
		assertEquals(Arrays.asList("(range field=SomeRangeFacet {,89])"), rangeFilterQueries);

	}

	@Test
	public void testCreateRangeFilterQueries_keyRangeWithMultipleValues(){
		keyRange.setMin("56");
		keyRange.setMax("89");

		KeyRange keyRange2 = new KeyRange();
		keyRange2.setMin("234");
		keyRange2.setMax("567");

		List<String> rangeFilterQueries = SearchUtil.createRangeFilterQueries(keyRangeList);
		Arrays.asList("(range field=SomeRangeFacet [56,89])", "(range field=SomeRangeFacet [234,567])");
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
		String consortium = "consortium";

		Map<String, List<String>> hitFields = new HashMap<String, List<String>>() {
			{
				put(FIELD_CREATED_BY, Collections.singletonList(createdBy));
				put(FIELD_CREATED_ON, Collections.singletonList(createdOn));
				put(FIELD_DESCRIPTION, Collections.singletonList(description));
				put(FIELD_DISEASE, Collections.singletonList(disease));
				put(FIELD_ETAG, Collections.singletonList(etag));
				put(FIELD_MODIFIED_BY, Collections.singletonList(modifiedBy));
				put(FIELD_MODIFIED_ON, Collections.singletonList(modifiedOn));
				put(FIELD_NAME, Collections.singletonList(name));
				put(FIELD_NODE_TYPE, Collections.singletonList(nodeType));
				put(FIELD_NUM_SAMPLES, Collections.singletonList(numSamples));
				put(FIELD_TISSUE, Collections.singletonList(tissue));
				put(FIELD_CONSORTIUM, Collections.singletonList(consortium));
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
		assertEquals(consortium, hit.getConsortium());
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
		userInfo.setGroups(null);
		SearchUtil.formulateAuthorizationFilter(userInfo);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testFormulateAuthorizationFilterEmptyUserGroups(){
		userInfo.setGroups(Collections.emptySet());
		SearchUtil.formulateAuthorizationFilter(userInfo);
	}

	@Test
	public void testFormulateAuthorizationFilterHappyCase(){
		String authFilter = SearchUtil.formulateAuthorizationFilter(userInfo);
		assertEquals("(or acl:'123' acl:'456' acl:'789')", authFilter);
	}

	///////////////////////////////////
	// addAuthorizationFilter() tests
	///////////////////////////////////

	@Test
	public void testAddAuthourizationFilter__filterQueryAlreadyExists(){
		String fitlerQuery = "(and indexName:'asdf')";
		searchRequest = new SearchRequest().withFilterQuery(fitlerQuery);

		//method under test
		SearchUtil.addAuthorizationFilter(searchRequest, userInfo);

		assertEquals("(and (or acl:'123' acl:'456' acl:'789') (and indexName:'asdf'))", searchRequest.getFilterQuery());
	}

	@Test
	public void testAddAuthourizationFilter__filterQueryNotExist(){
		searchRequest = new SearchRequest();

		//method under test
		SearchUtil.addAuthorizationFilter(searchRequest, userInfo);

		assertEquals("(or acl:'123' acl:'456' acl:'789')", searchRequest.getFilterQuery());
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
		assertEquals("[{\"type\":\"delete\",\"id\":\"syn123\"}, {\"type\":\"add\",\"id\":\"syn456\",\"fields\":{\"name\":\"Fake Entity\"}}]", jsonString);
	}

	@Test
	public void testConvertSearchDocumentsToJSONStringWithUnsupportedUnicode(){
		Document addDoc = new Document();
		addDoc.setId("syn5158082362");
		addDoc.setType(DocumentTypeNames.add);
		DocumentFields fields = new DocumentFields();
		fields.setName("John Cena");
		fields.setDescription("You Can't See Me: \uD83D\uDC68\uD83D\uDC4B");
		addDoc.setFields(fields);

		String jsonString = SearchUtil.convertSearchDocumentsToJSONString(Arrays.asList(addDoc));
		assertEquals("[{\"type\":\"add\",\"id\":\"syn5158082362\",\"fields\":{\"name\":\"John Cena\",\"description\":\"You Can't See Me: \"}}]", jsonString);
	}


	////////////////////////////////////////////
	// stripUnsupportedUnicodeCharacters() test
	////////////////////////////////////////////

	@Test
	public void testStripUnsupportedUnicodeCharacters(){
		//test unicode characters from https://docs.aws.amazon.com/cloudsearch/latest/developerguide/preparing-data.html
		String testString = "⌐( ͡° ͜ʖ ͡°) ╯╲___\uD800\uDBFF\uDFFF\uDC00\uFFFE\uFFFF\uD83C\uDF0ADon't mind me just taking my unsupported unicode characters for a walk";

		String result = SearchUtil.stripUnsupportedUnicodeCharacters(testString);
		assertEquals("⌐( ͡° ͜ʖ ͡°) ╯╲___Don't mind me just taking my unsupported unicode characters for a walk", result);
	}

	////////////////////////////////////////
	//  createOptionsJSONForFacet() tests
	////////////////////////////////////////

	@Test (expected = IllegalArgumentException.class)
	public void testCreateCloudSearchFacetJSON_SingleFieldIsNotFacetable(){
		searchFacetOption.setName(SearchFieldName.Description);
		SearchUtil.addOptionsJSONForFacet(new JSONObject(), searchFacetOption);
	}

	@Test
	public void testCreateOptionsJSONForFacet_SortTypeNull(){
		searchFacetOption.setMaxResultCount(56L);
		searchFacetOption.setSortType(null);
		JSONObject jsonObject = new JSONObject();
		SearchUtil.addOptionsJSONForFacet(jsonObject, searchFacetOption);
		assertEquals("{\"node_type\":{\"size\":56}}", jsonObject.toString());
	}

	@Test
	public void testCreateOptionsJSONForFacet_MaxCountNull(){
		searchFacetOption.setMaxResultCount(null);
		searchFacetOption.setSortType(SearchFacetSort.ALPHA);
		JSONObject jsonObject = new JSONObject();

		SearchUtil.addOptionsJSONForFacet(jsonObject, searchFacetOption);
		assertEquals("{\"node_type\":{\"sort\":\"bucket\"}}", jsonObject.toString());
	}


	///////////////////////////////////////
	// createCloudSearchFacetJSON() tests
	///////////////////////////////////////
	@Test
	public void testCreateCloudSearchFacetJSON_SingleField(){
		JSONObject result = SearchUtil.createCloudSearchFacetJSON(Collections.singletonList(searchFacetOption));
		assertEquals("{\"node_type\":{\"sort\":\"count\",\"size\":42}}", result.toString());
	}

	@Test
	public void testCreateCloudSearchFacetJSON_MultipleField(){
		SearchFacetOption otherFacetOption = new SearchFacetOption();
		otherFacetOption.setName(SearchFieldName.CreatedOn);

		JSONObject result = SearchUtil.createCloudSearchFacetJSON(Arrays.asList( searchFacetOption, otherFacetOption));
		assertEquals("{\"node_type\":{\"sort\":\"count\",\"size\":42},\"created_on\":{}}", result.toString());
	}
}
