package org.sagebionetworks.repo.manager.search.oss;

import com.google.common.collect.Sets;
import jakarta.json.Json;
import jakarta.json.stream.JsonGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.LongTermsBucket;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.RangeQuery;
import org.opensearch.client.opensearch._types.query_dsl.TermQuery;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.TotalHitsRelation;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.search.DocumentFields;
import org.sagebionetworks.repo.model.search.Facet;
import org.sagebionetworks.repo.model.search.FacetConstraint;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.KeyRange;
import org.sagebionetworks.repo.model.search.query.KeyValue;
import org.sagebionetworks.repo.model.search.query.SearchFacetOption;
import org.sagebionetworks.repo.model.search.query.SearchFacetSort;
import org.sagebionetworks.repo.model.search.query.SearchFieldName;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.search.SearchConstants;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.sagebionetworks.search.SearchConstants.FIELD_ACL;

public class OssUtilTest {
    List<Long> userGroups;
    UserInfo userInfo;
    private SearchQuery query;
    private SearchRequest.Builder expectedSearchRequestBaseNoQueryTermSet;
    private SearchFacetOption searchFacetOption;
    private List<String> q;
    private List<KeyValue> bq;
    private List<KeyValue> bqNot;
    private List<KeyValue> bq2;
    private List<KeyValue> bqSpecialChar;
    private List<KeyRange> keyRangeList;
    private KeyRange keyRange;

    @BeforeEach
    public void before() throws Exception {
        query = new SearchQuery();
        // q
        q = new ArrayList<>();
        q.add("hello");
        q.add("world");

        expectedSearchRequestBaseNoQueryTermSet = new SearchRequest.Builder()
                .index(SearchConstants.OPEN_SEARCH_INDEX_NAME);


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
        kv.setKey("Facet2");
        kv.setValue("Value2");
        bqNot.add(kv);
        kv = new KeyValue();
        kv.setKey("Facet1");
        kv.setValue("Value1");
        kv.setNot(true);
        bqNot.add(kv);

        bqSpecialChar = new ArrayList<>();
        kv = new KeyValue();
        kv.setKey("Facet1");
        kv.setValue("c:\\dave's_folde,r");
        bqSpecialChar.add(kv);

        //searchResponse = new SearchResponse<DocumentFields>();

        userInfo = new UserInfo(false);
        userGroups = List.of(123L, 456L, 789L);
        userInfo.setGroups(Sets.newLinkedHashSet(userGroups));

        keyRangeList = new ArrayList<>();
        keyRange = new KeyRange();
        keyRange.setKey("SomeRangeFacet");
        keyRange.setMax("45");
        keyRange.setMin("35");
        keyRangeList.add(keyRange);

        searchFacetOption = new SearchFacetOption();
        searchFacetOption.setMaxResultCount(42L);
        searchFacetOption.setSortType(SearchFacetSort.COUNT);
        searchFacetOption.setName(SearchFieldName.EntityType);
    }

    @Test
    public void testGenerateSearchRequestWithNullQuery() {
        String message = assertThrows(IllegalArgumentException.class, () -> {
            OssUtil.generateSearchRequest(userInfo, null);
        }).getMessage();

        assertEquals("searchQuery is required.", message);
    }

    @Test
    public void testGenerateSearchRequestWithNullUser() {
        String message = assertThrows(IllegalArgumentException.class, () -> {
            //call under test
            OssUtil.generateSearchRequest(null, new SearchQuery());
        }).getMessage();

        assertEquals("userInfo is required.", message);
    }

    @Test
    public void testGenerateSearchRequestWithNoTermAndNoBooleanQuery() {
        String message = assertThrows(IllegalArgumentException.class, () -> {
            //call under test
            OssUtil.generateSearchRequest(userInfo, new SearchQuery());
        }).getMessage();

        assertEquals("Either one queryTerm or one booleanQuery must be defined.", message);
    }

    @Test
    public void testGenerateSearchRequestWithEmptyTerm() {
        String message = assertThrows(IllegalArgumentException.class, () -> {
            //call under test
            OssUtil.generateSearchRequest(userInfo, new SearchQuery().setQueryTerm(List.of("")));
        }).getMessage();

        assertEquals("Either one queryTerm or one booleanQuery must be defined.", message);
    }


    @Test
    public void testGenerateSearchRequestWithTerm() {
        String expectedTerms = q.stream()
                .collect(Collectors.joining(" "));

        expectedSearchRequestBaseNoQueryTermSet.query(query -> query.bool(b1 -> b1.must(List.of(
                        Query.of(q1 -> q1.multiMatch(mm -> mm.query(expectedTerms)))))
                .filter(Query.of(tq -> tq.terms(t -> t
                        .field(FIELD_ACL)
                        .terms(queryTerm -> queryTerm.value(
                                userGroups.stream()
                                        .map(FieldValue::of)
                                        .collect(Collectors.toList())
                        )))))));


        SearchRequest expectedRequest = expectedSearchRequestBaseNoQueryTermSet.build();

        //call under test
        SearchRequest request = OssUtil.generateSearchRequest(userInfo, query.setQueryTerm(q));
        assertEquals(1, request.query().bool().must().size());
        assertEquals(1, request.query().bool().filter().size());

        String actualJson = toJson(request);
        String expectedJson = toJson(expectedRequest);

        assertEquals(expectedJson, actualJson);
    }

    @Test
    public void testGenerateSearchRequestAdminUserCanAccessAllDocuments() {
        String expectedTerms = q.stream()
                .collect(Collectors.joining(" "));

        expectedSearchRequestBaseNoQueryTermSet.query(query -> query.bool(b1 -> b1.must(
                Query.of(q1 -> q1.multiMatch(mm -> mm.query(expectedTerms))))));


        SearchRequest expectedRequest = expectedSearchRequestBaseNoQueryTermSet.build();

        //call under test
        SearchRequest request = OssUtil.generateSearchRequest(new UserInfo(true), query.setQueryTerm(q));
        assertEquals(1, request.query().bool().must().size());
        assertEquals(0, request.query().bool().filter().size());

        String actualJson = toJson(request);
        String expectedJson = toJson(expectedRequest);

        assertEquals(expectedJson, actualJson);
    }

    @Test
    public void testGenerateSearchRequestWithBoolean() {
        expectedSearchRequestBaseNoQueryTermSet.query(query -> query.bool(b1 -> b1.must(Query.of(q1 -> q1.matchAll(m -> m)))
                .filter(List.of(
                        Query.of(q2 -> q2.term(TermQuery.of(t -> t.field(bq.get(0).getKey()).value(FieldValue.of(bq.get(0).getValue()))))),
                        Query.of(tq -> tq.terms(t -> t
                                .field(FIELD_ACL)
                                .terms(queryTerm -> queryTerm.value(
                                        userGroups.stream()
                                                .map(FieldValue::of)
                                                .collect(Collectors.toList())))))
                ))));


        SearchRequest expectedRequest = expectedSearchRequestBaseNoQueryTermSet.build();

        //call under test
        SearchRequest request = OssUtil.generateSearchRequest(userInfo, query.setBooleanQuery(bq));
        assertEquals(1, request.query().bool().must().size());
        assertEquals(2, request.query().bool().filter().size());

        String actualJson = toJson(request);
        String expectedJson = toJson(expectedRequest);

        assertEquals(expectedJson, actualJson);
    }

    @Test
    public void testGenerateSearchRequestWithBooleanSpecialChar() {
        expectedSearchRequestBaseNoQueryTermSet.query(query -> query.bool(b1 -> b1.must(List.of(
                        Query.of(q1 -> q1.matchAll(m -> m))))
                .filter(List.of(
                        Query.of(q2 -> q2.term(TermQuery.of(t -> t.field(bqSpecialChar.get(0).getKey()).value(FieldValue.of(bqSpecialChar.get(0).getValue()))))),
                        Query.of(tq -> tq.terms(t -> t
                                .field(FIELD_ACL)
                                .terms(queryTerm -> queryTerm.value(
                                        userGroups.stream()
                                                .map(FieldValue::of)
                                                .collect(Collectors.toList())
                                ))))))));


        SearchRequest expectedRequest = expectedSearchRequestBaseNoQueryTermSet.build();

        //call under test
        SearchRequest request = OssUtil.generateSearchRequest(userInfo, query.setBooleanQuery(bqSpecialChar));
        assertEquals(1, request.query().bool().must().size());
        assertEquals(2, request.query().bool().filter().size());

        String actualJson = toJson(request);
        String expectedJson = toJson(expectedRequest);

        assertEquals(expectedJson, actualJson);
    }

    @Test
    public void testGenerateSearchRequestWithNotBoolean() {
        expectedSearchRequestBaseNoQueryTermSet.query(query -> query.bool(b1 -> b1
                .must(Query.of(q1 -> q1.matchAll(m -> m)))
                .mustNot(Query.of(q3 -> q3.term(TermQuery.of(t -> t.field(bqNot.get(1).getKey()).value(FieldValue.of(bqNot.get(1).getValue()))))))
                .filter(List.of(
                        Query.of(q2 -> q2.term(TermQuery.of(t -> t.field(bqNot.get(0).getKey()).value(FieldValue.of(bqNot.get(0).getValue()))))),
                        Query.of(tq -> tq.terms(t -> t
                                .field(FIELD_ACL)
                                .terms(queryTerm -> queryTerm.value(
                                        userGroups.stream()
                                                .map(FieldValue::of)
                                                .collect(Collectors.toList())
                                ))))))));

        SearchRequest expectedRequest = expectedSearchRequestBaseNoQueryTermSet.build();


        //call under test
        SearchRequest request = OssUtil.generateSearchRequest(userInfo, query.setBooleanQuery(bqNot));
        assertEquals(1, request.query().bool().must().size());
        assertEquals(1, request.query().bool().mustNot().size());
        assertEquals(2, request.query().bool().filter().size());

        String actualJson = toJson(request);
        String expectedJson = toJson(expectedRequest);

        assertEquals(expectedJson, actualJson);
    }

    @ParameterizedTest
    @EnumSource(SearchFieldName.class)
    public void testGenerateSearchRequestWithFacetOrderDesc(SearchFieldName searchFieldName) {
        //Test fields are not supported by OpenSearch Aggregation
        if (searchFieldName == SearchFieldName.Name || searchFieldName == SearchFieldName.Description) {
            return;
        }

        expectedSearchRequestBaseNoQueryTermSet.query(query -> query.bool(b1 -> b1
                        .must(Query.of(q1 -> q1.matchAll(m -> m)))
                        .filter(List.of(
                                Query.of(q2 -> q2.term(TermQuery.of(t -> t.field(bq.get(0).getKey()).value(FieldValue.of(bq.get(0).getValue()))))),
                                Query.of(tq -> tq.terms(t -> t
                                        .field(FIELD_ACL)
                                        .terms(queryTerm -> queryTerm.value(
                                                userGroups.stream()
                                                        .map(FieldValue::of)
                                                        .collect(Collectors.toList())
                                        ))))))))

                .aggregations(SynapseToOpenSearchAggregationField.openSearchFieldFor(searchFieldName), Aggregation.of(a -> a
                        .terms(t -> t
                                .field(SynapseToOpenSearchAggregationField.openSearchFieldFor(searchFieldName))
                                .order((List.of(Map.of("_count", SortOrder.Desc))))
                                .size(300))));

        SearchRequest expectedRequest = expectedSearchRequestBaseNoQueryTermSet.build();

        //call under test
        SearchRequest request = OssUtil.generateSearchRequest(userInfo, query.setBooleanQuery(bq)
                .setFacetOptions(List.of(
                        new SearchFacetOption().setName(searchFieldName).setSortType(SearchFacetSort.COUNT).setMaxResultCount(300l)
                )));
        assertEquals(1, request.query().bool().must().size());
        assertEquals(2, request.query().bool().filter().size());
        assertEquals(1, request.aggregations().size());

        String actualJson = toJson(request);
        String expectedJson = toJson(expectedRequest);
        assertEquals(expectedJson, actualJson);
    }

    @Test
    public void testGenerateSearchRequestWithFacetOrderAsc() {
        expectedSearchRequestBaseNoQueryTermSet.query(query -> query.bool(b1 -> b1
                        .must(Query.of(q1 -> q1.matchAll(m -> m)))
                        .filter(List.of(
                                Query.of(q2 -> q2.term(TermQuery.of(t -> t.field(bq.get(0).getKey()).value(FieldValue.of(bq.get(0).getValue()))))),
                                Query.of(tq -> tq.terms(t -> t
                                        .field(FIELD_ACL)
                                        .terms(queryTerm -> queryTerm.value(
                                                userGroups.stream()
                                                        .map(FieldValue::of)
                                                        .collect(Collectors.toList())
                                        ))))))))
                .aggregations(SynapseToOpenSearchAggregationField.openSearchFieldFor(SearchFieldName.EntityType), Aggregation.of(a -> a
                        .terms(t -> t
                                .field(SynapseToOpenSearchAggregationField.openSearchFieldFor(SearchFieldName.EntityType))
                                .order((List.of(Map.of("_count", SortOrder.Asc))))
                                .size(Math.toIntExact(searchFacetOption.getMaxResultCount())))));

        SearchRequest expectedRequest = expectedSearchRequestBaseNoQueryTermSet.build();

        //call under test
        SearchRequest request = OssUtil.generateSearchRequest(userInfo, query.setBooleanQuery(bq)
                .setFacetOptions(List.of(searchFacetOption.setSortType(SearchFacetSort.ALPHA))));
        assertEquals(1, request.query().bool().must().size());
        assertEquals(2, request.query().bool().filter().size());
        assertEquals(1, request.aggregations().size());

        String actualJson = toJson(request);
        String expectedJson = toJson(expectedRequest);
        assertEquals(expectedJson, actualJson);
    }

    @Test
    public void testGenerateSearchRequestWithRangeQuery() {
        expectedSearchRequestBaseNoQueryTermSet.query(query -> query.bool(b1 -> b1
                .must(Query.of(q1 -> q1.matchAll(m -> m)))
                .filter(List.of(
                        Query.of(q2 -> q2.term(TermQuery.of(t -> t.field(bq.get(0).getKey()).value(FieldValue.of(bq.get(0).getValue()))))),
                        Query.of((q3 -> q3.range(RangeQuery.of(r -> r.field(keyRange.getKey()).gte(JsonData.of(keyRange.getMin())))))),
                        Query.of(tq -> tq.terms(t -> t
                                .field(FIELD_ACL)
                                .terms(queryTerm -> queryTerm.value(
                                        userGroups.stream()
                                                .map(FieldValue::of)
                                                .collect(Collectors.toList())
                                ))))))));

        SearchRequest expectedRequest = expectedSearchRequestBaseNoQueryTermSet.build();

        //call under test
        SearchRequest request = OssUtil.generateSearchRequest(userInfo, query.setBooleanQuery(bq).setRangeQuery(keyRangeList));
        assertEquals(1, request.query().bool().must().size());
        assertEquals(3, request.query().bool().filter().size());

    }

    @Test
    public void testGenerateSearchRequestWithRangeQueryWithoutMinAndMax() {


        String message = assertThrows(IllegalArgumentException.class, () -> {

            //call under test
            OssUtil.generateSearchRequest(userInfo, query.setBooleanQuery(bq).setRangeQuery(List.of(new KeyRange().setKey("anyKey"))));
        }).getMessage();

        assertEquals("At least one of min or max for key=anyKey must be not null.", message);

    }

    @Test
    public void testGenerateSearchRequestWithSizeAndStart() {
        expectedSearchRequestBaseNoQueryTermSet.query(query -> query.bool(b1 -> b1
                .must(Query.of(q1 -> q1.matchAll(m -> m)))
                .filter(List.of(
                        Query.of(q2 -> q2.term(TermQuery.of(t -> t.field(bq.get(0).getKey()).value(FieldValue.of(bq.get(0).getValue()))))),
                        Query.of(tq -> tq.terms(t -> t
                                .field(FIELD_ACL)
                                .terms(queryTerm -> queryTerm.value(
                                        userGroups.stream()
                                                .map(FieldValue::of)
                                                .collect(Collectors.toList())
                                )))))))).size(10).from(5);

        SearchRequest expectedRequest = expectedSearchRequestBaseNoQueryTermSet.build();

        //call under test
        SearchRequest request = OssUtil.generateSearchRequest(userInfo, query.setBooleanQuery(bq).setSize(10l).setStart(5L));
        assertEquals(1, request.query().bool().must().size());
        assertEquals(2, request.query().bool().filter().size());

        String actualJson = toJson(request);
        String expectedJson = toJson(expectedRequest);

        assertEquals(expectedJson, actualJson);
    }

    @Test
    public void testGenerateSearchRequestWithReturnFields() {
        List<String> returnFields = List.of(SearchConstants.FIELD_NAME, SearchConstants.FIELD_DESCRIPTION);
        expectedSearchRequestBaseNoQueryTermSet.query(query -> query.bool(b1 -> b1
                .must(Query.of(q1 -> q1.matchAll(m -> m)))
                .filter(List.of(
                        Query.of(q2 -> q2.term(TermQuery.of(t -> t.field(bq.get(0).getKey()).value(FieldValue.of(bq.get(0).getValue()))))),
                        Query.of(tq -> tq.terms(t -> t
                                .field(FIELD_ACL)
                                .terms(queryTerm -> queryTerm.value(
                                        userGroups.stream()
                                                .map(FieldValue::of)
                                                .collect(Collectors.toList())
                                )))))))).source(s -> s.filter(f -> f.includes(returnFields)));

        SearchRequest expectedRequest = expectedSearchRequestBaseNoQueryTermSet.build();

        //call under test
        SearchRequest request = OssUtil.generateSearchRequest(userInfo, query.setBooleanQuery(bq).setReturnFields(returnFields));
        assertEquals(1, request.query().bool().must().size());
        assertEquals(2, request.query().bool().filter().size());

        String actualJson = toJson(request);
        String expectedJson = toJson(expectedRequest);

        assertEquals(expectedJson, actualJson);
    }

    @Test
    public void testConvertToSynapseSearchResult() {
        DocumentFields document = getDocument();
        String nodeType = SynapseToOpenSearchAggregationField.openSearchFieldFor(SearchFieldName.EntityType);
        String createdON = SynapseToOpenSearchAggregationField.openSearchFieldFor(SearchFieldName.CreatedOn);
        String nodeBucketKey = "project";
        String createdOnBucketKey = "1234567";
        SearchResponse<DocumentFields> response = SearchResponse.searchResponseOf(res -> res.documents(document)
                .shards(shard -> shard.total(1).failed(0l).successful(1l))
                .aggregations(Map.of(nodeType, Aggregate.of(a -> a
                                        .sterms(terms -> terms
                                                .buckets(buckets -> buckets.array(Arrays.asList(
                                                        StringTermsBucket.of(b -> b.key(nodeBucketKey).docCount(100L))
                                                ))).sumOtherDocCount(0))),
                                createdON, Aggregate.of(a -> a
                                        .lterms(terms -> terms
                                                .buckets(buckets -> buckets.array(Arrays.asList(
                                                        LongTermsBucket.of(b -> b.key(createdOnBucketKey).docCount(100L))
                                                ))).sumOtherDocCount(0)))
                        )
                )
                .timedOut(false).took(10).hits(hh -> hh.hits(ht -> ht.source(document).id("id")
                        .index(SearchConstants.OPEN_SEARCH_INDEX_NAME)).total(t -> t.value(1l).relation(TotalHitsRelation.valueOf("Eq")))));


        SearchResults expectedSynapseSearchResults = new SearchResults().setFound(1l).setStart(5l).setFacets(List.of(
                        new Facet().setName(createdON).setType(OpenSearchFieldType.fieldType(SynapseToOpenSearchAggregationField.synapseFieldFor(createdON)))
                                .setConstraints(List.of(new FacetConstraint().setCount(100l).setValue(createdOnBucketKey))),
                        new Facet().setName(nodeType).setType(OpenSearchFieldType.fieldType(SynapseToOpenSearchAggregationField.synapseFieldFor(nodeType)))
                                .setConstraints(List.of(new FacetConstraint().setCount(100l).setValue(nodeBucketKey)))))
                .setHits(List.of(getHitFromDocument(document)));

        //call under test
        SearchResults results = OssUtil.convertToSynapseSearchResult(response, 5);
        results.getFacets().sort((o1, o2) -> o1.getName().compareTo(o2.getName()));
        assertEquals(2, results.getFacets().size());
        assertEquals(expectedSynapseSearchResults.getFacets(), results.getFacets());

    }

    public DocumentFields getDocument() {
        return new DocumentFields()
                .setAcl(userGroups.stream().map(String::valueOf).collect(Collectors.toList()))
                .setName("AnyName").setConsortium("cons").setCreated_by("me").setCreated_on(123l)
                .setModified_by("you").setModified_on(345l).setDescription("description").setDiagnosis("2")
                .setEtag("1").setNode_type("folder").setOrgan("ear").setTissue("tissue")
                .setParent_id("p_id").setUpdate_acl(List.of("234", "678"));
    }

    public org.sagebionetworks.repo.model.search.Hit getHitFromDocument(DocumentFields document) {
        org.sagebionetworks.repo.model.search.Hit synapseHit = new org.sagebionetworks.repo.model.search.Hit();
        synapseHit.setId("id");
        synapseHit.setCreated_by(document.getCreated_by());
        synapseHit.setCreated_on(document.getCreated_on());
        synapseHit.setDescription(document.getDescription());
        synapseHit.setDiagnosis(document.getDiagnosis());
        synapseHit.setEtag(document.getEtag());
        synapseHit.setModified_by(document.getModified_by());
        synapseHit.setModified_on(document.getModified_on());
        synapseHit.setName(document.getName());
        synapseHit.setNode_type(document.getNode_type());
        synapseHit.setTissue(document.getTissue());
        synapseHit.setConsortium(document.getConsortium());
        synapseHit.setOrgan(document.getOrgan());
        return synapseHit;
    }

    public String toJson(SearchRequest searchRequest) {
        StringWriter writer = new StringWriter();
        JsonGenerator generator = Json.createGenerator(writer);
        JsonpMapper mapper = new JacksonJsonpMapper();
        searchRequest.serialize(generator, mapper);
        generator.close();
        return writer.toString();
    }

}
