package org.sagebionetworks.repo.manager.search.oss;

import com.google.common.collect.Sets;
import jakarta.json.Json;
import jakarta.json.stream.JsonGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.FiltersBucket;
import org.opensearch.client.opensearch._types.aggregations.LongTermsBucket;
import org.opensearch.client.opensearch._types.aggregations.LongTermsBucketKey;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.RangeQuery;
import org.opensearch.client.opensearch._types.query_dsl.TermQuery;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.PhraseSuggest;
import org.opensearch.client.opensearch.core.search.PhraseSuggestOption;
import org.opensearch.client.opensearch.core.search.Suggest;
import org.opensearch.client.opensearch.core.search.TermSuggest;
import org.opensearch.client.opensearch.core.search.TermSuggestOption;
import org.opensearch.client.opensearch.core.search.TotalHitsRelation;
import org.sagebionetworks.repo.model.AuthorizationConstants;
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
import org.sagebionetworks.repo.manager.search.SearchConstants;
import org.sagebionetworks.repo.model.search.query.Suggestion;
import org.sagebionetworks.repo.model.search.query.SuggestionList;
import org.sagebionetworks.repo.model.search.query.SuggestionQuery;
import org.sagebionetworks.repo.model.search.query.SuggestionResults;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.sagebionetworks.repo.manager.search.SearchConstants.FIELD_ACL;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sagebionetworks.repo.manager.search.oss.OssUtil.TERM_DESCRIPTION_SUGGESTION;
import static org.sagebionetworks.repo.manager.search.oss.OssUtil.TERM_NAME_SUGGESTION;

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

        userGroups = List.of(123L, 456L, 789L);
        userInfo = new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID, Sets.newLinkedHashSet(userGroups));

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
                        Query.of(q1 -> q1.simpleQueryString(mm -> mm.query(expectedTerms)))))
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
                Query.of(q1 -> q1.simpleQueryString(mm -> mm.query(expectedTerms))))));


        SearchRequest expectedRequest = expectedSearchRequestBaseNoQueryTermSet.build();

        //call under test
        SearchRequest request = OssUtil.generateSearchRequest(new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID), query.setQueryTerm(q));
        assertEquals(1, request.query().bool().must().size());
        assertEquals(0, request.query().bool().filter().size());

        String actualJson = toJson(request);
        String expectedJson = toJson(expectedRequest);

        assertEquals(expectedJson, actualJson);
    }

    @Test
    public void testEliminateSuggestionWithAccessDeniedRemovesOptionsWithNoAccess() {
        SuggestionList suggestionList = new SuggestionList()
                .setKey("term1")
                .setValues(new HashSet<>(List.of(
                        new Suggestion().setTerm("allowed"),
                        new Suggestion().setTerm("denied")
                )));
        SuggestionResults suggestionResults = new SuggestionResults().setSuggestions(List.of(suggestionList));

        FiltersBucket allowedBucket = FiltersBucket.of(b -> b.docCount(5L));
        Map<String, FiltersBucket> filteredBucketMap = Map.of("allowed", allowedBucket);
        Aggregate aggregate = Aggregate.of(a -> a.filters(f -> f.buckets(b -> b.keyed(filteredBucketMap))));
        Map<String, Aggregate> aggregateResponse = Map.of("query_counts", aggregate);

        //call under test
        SuggestionResults filtered = OssUtil.eliminateSuggestionWithAccessDenied(suggestionResults, aggregateResponse);
        assertEquals(1, filtered.getSuggestions().size());
        SuggestionList filteredSuggestion = filtered.getSuggestions().get(0);
        assertEquals(1, filteredSuggestion.getValues().size());
        Suggestion onlySuggestion = filteredSuggestion.getValues().iterator().next();
        assertEquals("allowed", onlySuggestion.getTerm());
        assertEquals(5L, onlySuggestion.getFrequency().longValue());
    }

    @Test
    public void testEliminateSuggestionWithAccessDeniedWithNoMatchingBuckets() {
        SuggestionList suggestionList = new SuggestionList()
                .setKey("term1")
                .setValues(new HashSet<>(List.of(
                        new Suggestion().setTerm("denied")
                )));
        SuggestionResults suggestionResults = new SuggestionResults().setSuggestions(List.of(suggestionList));

        Map<String, FiltersBucket> filteredBucketMap = Map.of();
        Aggregate aggregate = Aggregate.of(a -> a.filters(f -> f.buckets(b -> b.keyed(filteredBucketMap))));
        Map<String, Aggregate> aggregateResponse = Map.of("query_counts", aggregate);

        //call under test
        SuggestionResults filtered = OssUtil.eliminateSuggestionWithAccessDenied(suggestionResults, aggregateResponse);
        assertEquals(1, filtered.getSuggestions().size());
        assertTrue(filtered.getSuggestions().get(0).getValues().isEmpty());
    }

    @Test
    public void testEliminateSuggestionWithAccessDeniedUpdatesFrequencyForMultipleOptions() {
        SuggestionList suggestionList = new SuggestionList()
                .setKey("term1")
                .setValues(new HashSet<>(List.of(
                        new Suggestion().setTerm("a"),
                        new Suggestion().setTerm("b")
                )));
        FiltersBucket bucketA = FiltersBucket.of(b -> b.docCount(2L));
        FiltersBucket bucketB = FiltersBucket.of(b -> b.docCount(3L));
        Map<String, FiltersBucket> filteredBucketMap = Map.of("a", bucketA, "b", bucketB);
        Aggregate aggregate = Aggregate.of(a -> a.filters(f -> f.buckets(b -> b.keyed(filteredBucketMap))));
        Map<String, Aggregate> aggregateResponse = Map.of("query_counts", aggregate);

        //call under test
        SuggestionResults filtered = OssUtil.eliminateSuggestionWithAccessDenied(
                new SuggestionResults().setSuggestions(List.of(suggestionList)), aggregateResponse);
        assertEquals(1, filtered.getSuggestions().size());
        Set<Suggestion> suggestions = filtered.getSuggestions().get(0).getValues();
        assertEquals(2, suggestions.size());
        for (Suggestion s : suggestions) {
            if ("a".equals(s.getTerm())) {
                assertEquals(2L, s.getFrequency().longValue());
            } else if ("b".equals(s.getTerm())) {
                assertEquals(3L, s.getFrequency().longValue());
            }
        }
    }

    @Test
    public void testEliminateSuggestionWithAccessDeniedWithNullAggregateResponse() {
        SuggestionList suggestionList = new SuggestionList().setKey("term1").setValues(new HashSet<>(List.of(new Suggestion().setTerm("a"))));
        SuggestionResults suggestionResults = new SuggestionResults().setSuggestions(List.of(suggestionList));
        String message = assertThrows(IllegalArgumentException.class, () -> {
            //call under test
            OssUtil.eliminateSuggestionWithAccessDenied(suggestionResults, null);
        }).getMessage();
        assertEquals("aggregateResponse is required.", message);
    }

    @Test
    public void testEliminateSuggestionWithAccessDeniedWithNullValuesSet() {
        SuggestionList suggestionList = new SuggestionList().setKey("term1").setValues(null);
        SuggestionResults suggestionResults = new SuggestionResults().setSuggestions(List.of(suggestionList));
        Map<String, FiltersBucket> filteredBucketMap = Map.of("a", FiltersBucket.of(b -> b.docCount(1L)));
        Aggregate aggregate = Aggregate.of(a -> a.filters(f -> f.buckets(b -> b.keyed(filteredBucketMap))));
        Map<String, Aggregate> aggregateResponse = Map.of("query_counts", aggregate);
        //call under test
        SuggestionResults filtered = OssUtil.eliminateSuggestionWithAccessDenied(suggestionResults, aggregateResponse);
        assertNull(filtered.getSuggestions().get(0).getValues());
    }

    @Test
    public void testEliminateSuggestionWithAccessDeniedWithNullTerm() {
        SuggestionList suggestionList = new SuggestionList().setKey("term1").setValues(new HashSet<>(List.of(new Suggestion().setTerm(null))));
        SuggestionResults suggestionResults = new SuggestionResults().setSuggestions(List.of(suggestionList));
        Map<String, FiltersBucket> filteredBucketMap = Map.of("a", FiltersBucket.of(b -> b.docCount(1L)));
        Aggregate aggregate = Aggregate.of(a -> a.filters(f -> f.buckets(b -> b.keyed(filteredBucketMap))));
        Map<String, Aggregate> aggregateResponse = Map.of("query_counts", aggregate);
        //call under test
        SuggestionResults filtered = OssUtil.eliminateSuggestionWithAccessDenied(suggestionResults, aggregateResponse);
        assertTrue(filtered.getSuggestions().get(0).getValues().isEmpty());
    }

    @Test
    public void testEliminateSuggestionWithAccessDeniedWithTermNotInBucketMap() {
        SuggestionList suggestionList = new SuggestionList().setKey("term1").setValues(new HashSet<>(List.of(new Suggestion().setTerm("notfound"))));
        SuggestionResults suggestionResults = new SuggestionResults().setSuggestions(List.of(suggestionList));
        Map<String, FiltersBucket> filteredBucketMap = Map.of("a", FiltersBucket.of(b -> b.docCount(1L)));
        Aggregate aggregate = Aggregate.of(a -> a.filters(f -> f.buckets(b -> b.keyed(filteredBucketMap))));
        Map<String, Aggregate> aggregateResponse = Map.of("query_counts", aggregate);
        //call under test
        SuggestionResults filtered = OssUtil.eliminateSuggestionWithAccessDenied(suggestionResults, aggregateResponse);
        assertTrue(filtered.getSuggestions().get(0).getValues().isEmpty());
    }

    @Test
    public void testEliminateSuggestionWithAccessDeniedHandlesMultipleBucketsWithMixedDocCounts() {
        SuggestionList suggestionList = new SuggestionList().setKey("disease").setValues(new HashSet<>(List.of(
                new Suggestion().setTerm("cancer"),
                new Suggestion().setTerm("tumor"),
                new Suggestion().setTerm("leukemia"),
                new Suggestion().setTerm("unknown")
        )));
        Map<String, FiltersBucket> filteredBucketMap = new HashMap<>();
        filteredBucketMap.put("cancer", FiltersBucket.of(b -> b.docCount(5L)));
        filteredBucketMap.put("tumor", FiltersBucket.of(b -> b.docCount(3L)));
        filteredBucketMap.put("leukemia", FiltersBucket.of(b -> b.docCount(0L)));
        Aggregate aggregate = Aggregate.of(a -> a.filters(f -> f.buckets(b -> b.keyed(filteredBucketMap))));
        Map<String, Aggregate> aggregateResponse = Map.of("query_counts", aggregate);

        SuggestionResults suggestionResults = new SuggestionResults().setSuggestions(List.of(suggestionList));

        //call under test
        SuggestionResults filtered = OssUtil.eliminateSuggestionWithAccessDenied(suggestionResults, aggregateResponse);

        Set<String> terms = filtered.getSuggestions().get(0).getValues().stream().map(Suggestion::getTerm).collect(java.util.stream.Collectors.toSet());
        assertTrue(terms.contains("cancer"));
        assertTrue(terms.contains("tumor"));
        assertFalse(terms.contains("leukemia"));
        assertFalse(terms.contains("unknown"));
        for (Suggestion s : filtered.getSuggestions().get(0).getValues()) {
            if ("cancer".equals(s.getTerm())) {
                assertEquals(5L, s.getFrequency().longValue());
            }
            if ("tumor".equals(s.getTerm())) {
                assertEquals(3L, s.getFrequency().longValue());
            }
        }
    }

    @Test
    public void eliminateSuggestionWithAccessDeniedHandlesEmptyFilteredBucketMap() {
        SuggestionList suggestionList = new SuggestionList().setKey("disease").setValues(new HashSet<>(List.of(
                new Suggestion().setTerm("cancer"),
                new Suggestion().setTerm("tumor")
        )));
        Map<String, FiltersBucket> filteredBucketMap = Collections.emptyMap();
        Aggregate aggregate = Aggregate.of(a -> a.filters(f -> f.buckets(b -> b.keyed(filteredBucketMap))));
        Map<String, Aggregate> aggregateResponse = Map.of("query_counts", aggregate);

        SuggestionResults suggestionResults = new SuggestionResults().setSuggestions(List.of(suggestionList));
        //call under test
        SuggestionResults filtered = OssUtil.eliminateSuggestionWithAccessDenied(suggestionResults, aggregateResponse);

        assertTrue(filtered.getSuggestions().get(0).getValues().isEmpty());
    }


    @Test
    public void generateSearchRequestForSuggestionThrowsOnNullQuery() {
        String message = assertThrows(IllegalArgumentException.class, () -> {
            //call under test
            OssUtil.generateSearchRequestForSuggestion(null);
        }).getMessage();
        assertEquals("suggestionQuery is required.", message);
    }

    @Test
    public void generateSearchRequestForSuggestionThrowsOnNullSearchTermList() {
        SuggestionQuery query = new SuggestionQuery().setSearchTerm(null);
        String message = assertThrows(IllegalArgumentException.class, () -> {
            //call under test
            OssUtil.generateSearchRequestForSuggestion(query);
        }).getMessage();
        assertEquals("At least one search term should be provided for suggestion.", message);
    }

    @Test
    public void generateSearchRequestForSuggestionThrowsOnEmptySearchTermList() {
        SuggestionQuery query = new SuggestionQuery().setSearchTerm(Collections.emptyList());
        String message = assertThrows(IllegalArgumentException.class, () -> {
            //call under test
            OssUtil.generateSearchRequestForSuggestion(query);
        }).getMessage();
        assertEquals("At least one search term should be provided for suggestion.", message);
    }

    @Test
    public void generateSearchRequestForSuggestionThrowsOnEmptySearchTerm() {
        SuggestionQuery query = new SuggestionQuery().setSearchTerm(List.of(""));
        String message = assertThrows(IllegalArgumentException.class, () -> {
            //call under test
            OssUtil.generateSearchRequestForSuggestion(query);
        }).getMessage();
        assertEquals("At least one search term should be provided for suggestion.", message);
    }

    @Test
    public void generateSearchRequestForSuggestionBuildsRequestForSingleTerm() {
        SuggestionQuery query = new SuggestionQuery().setSearchTerm(List.of("cancer"));
        //call under test
        SearchRequest request = OssUtil.generateSearchRequestForSuggestion(query);
        assertNotNull(request);
        assertNotNull(request.suggest());
        assertTrue(request.suggest().suggesters().containsKey(TERM_NAME_SUGGESTION));
        assertTrue(request.suggest().suggesters().containsKey(TERM_DESCRIPTION_SUGGESTION));
        assertEquals("cancer", request.suggest().suggesters().get(TERM_NAME_SUGGESTION).text());
        assertEquals("cancer", request.suggest().suggesters().get(TERM_DESCRIPTION_SUGGESTION).text());
    }

    @Test
    public void generateSearchRequestForSuggestionBuildsRequestForMultipleTerms() {
        SuggestionQuery query = new SuggestionQuery().setSearchTerm(List.of("cancer", "tumor"));
        //call under test
        SearchRequest request = OssUtil.generateSearchRequestForSuggestion(query);
        assertNotNull(request);
        assertNotNull(request.suggest());
        assertTrue(request.suggest().suggesters().containsKey(TERM_NAME_SUGGESTION));
        assertTrue(request.suggest().suggesters().containsKey(TERM_DESCRIPTION_SUGGESTION));
        assertEquals("cancer tumor", request.suggest().suggesters().get(TERM_NAME_SUGGESTION).text());
        assertEquals("cancer tumor", request.suggest().suggesters().get(TERM_NAME_SUGGESTION).text());
    }

    @Test
    public void generateSearchRequestForSuggestionHandlesWhitespaceTerms() {
        SuggestionQuery query = new SuggestionQuery().setSearchTerm(List.of("  cancer  ", "  tumor "));
        //call under test
        SearchRequest request = OssUtil.generateSearchRequestForSuggestion(query);
        assertNotNull(request);
        assertEquals("cancer tumor", request.suggest().suggesters().get(TERM_NAME_SUGGESTION).text());
        assertEquals("cancer tumor", request.suggest().suggesters().get(TERM_DESCRIPTION_SUGGESTION).text());
    }

    @Test
    public void generateSearchRequestForSuggestionHandlesSpecialCharacters() {
        SuggestionQuery query = new SuggestionQuery().setSearchTerm(List.of("c@ncer", "tu#mor"));
        //call under test
        SearchRequest request = OssUtil.generateSearchRequestForSuggestion(query);
        assertNotNull(request);
        assertEquals("c@ncer tu#mor", request.suggest().suggesters().get(TERM_NAME_SUGGESTION).text());
        assertEquals("c@ncer tu#mor", request.suggest().suggesters().get(TERM_DESCRIPTION_SUGGESTION).text());
    }

    @Test
    public void generateSearchRequestForSuggestionBuildsRequestForSinglePhrase() {
        SuggestionQuery query = new SuggestionQuery().setSearchTerm(List.of("\"cancr patient\""));
        //call under test
        SearchRequest request = OssUtil.generateSearchRequestForSuggestion(query);
        assertNotNull(request);
        assertNotNull(request.suggest());
        assertTrue(request.suggest().suggesters().containsKey("phrase_name_suggestion_cancr_patient"));
        assertTrue(request.suggest().suggesters().containsKey("phrase_description_suggestion_cancr_patient"));
        assertEquals("cancr patient", request.suggest().suggesters().get("phrase_name_suggestion_cancr_patient").text());
        assertEquals("cancr patient", request.suggest().suggesters().get("phrase_description_suggestion_cancr_patient").text());
    }

    @Test
    public void generateSearchRequestForSuggestionBuildsRequestForMultiplePhrase() {
        SuggestionQuery query = new SuggestionQuery().setSearchTerm(List.of("\"cancr patient\"", "\"tum@r size3\""));
        //call under test
        SearchRequest request = OssUtil.generateSearchRequestForSuggestion(query);
        assertNotNull(request);
        assertNotNull(request.suggest());
        assertTrue(request.suggest().suggesters().containsKey("phrase_description_suggestion_tum@r_size3"));
        assertTrue(request.suggest().suggesters().containsKey("phrase_name_suggestion_tum@r_size3"));
        assertTrue(request.suggest().suggesters().containsKey("phrase_name_suggestion_cancr_patient"));
        assertTrue(request.suggest().suggesters().containsKey("phrase_description_suggestion_cancr_patient"));
        assertEquals("cancr patient", request.suggest().suggesters().get("phrase_name_suggestion_cancr_patient").text());
        assertEquals("tum@r size3", request.suggest().suggesters().get("phrase_description_suggestion_tum@r_size3").text());
    }

    @Test
    public void generateSearchRequestForSuggestionBuildsRequestForEmptyPhraseList() {
        SuggestionQuery query = new SuggestionQuery().setSearchTerm(List.of("\"\""));
        //call under test
        SearchRequest request = OssUtil.generateSearchRequestForSuggestion(query);
        assertNotNull(request.suggest());
        assertFalse(request.suggest().suggesters().containsKey("phrase_description_suggestion"));
        assertFalse(request.suggest().suggesters().containsKey("phrase_name_suggestion"));
    }

    @Test
    public void convertToSynapseSuggestionResultWithNullKeyInMap() {
        Suggest<DocumentFields> suggest = createTermSuggest("term1", List.of(createOption("opt", 1, 1.0f)));
        Map<String, List<Suggest<DocumentFields>>> suggestions = new HashMap<>();
        suggestions.put(null, List.of(suggest));
        //call under test
        SuggestionResults results = OssUtil.convertToSynapseSuggestionResult(suggestions);
        assertNotNull(results);
        assertEquals(1, results.getSuggestions().size());
        assertEquals("term1", results.getSuggestions().get(0).getKey());
    }

    @Test
    public void convertToSynapseSuggestionResultWithOptionsListContainingNull() {
        TermSuggestOption opt1 = createOption("opt1", 1, 1.0f);
        List<TermSuggestOption> options = new ArrayList<>();
        options.add(opt1);
        options.add(null);
        Suggest<DocumentFields> suggest = createTermSuggest("term1", options);
        Map<String, List<Suggest<DocumentFields>>> suggestions = Map.of("key1", List.of(suggest));
        //call under test
        SuggestionResults results = OssUtil.convertToSynapseSuggestionResult(suggestions);
        assertNotNull(results);
        assertEquals(1, results.getSuggestions().size());
        assertEquals(1, results.getSuggestions().get(0).getValues().size());
    }

    @Test
    public void testConvertToSynapseSuggestionResultWithNull() {
        String message = assertThrows(IllegalArgumentException.class, () -> {
            //call under test
            OssUtil.convertToSynapseSuggestionResult(null);
        }).getMessage();

        assertEquals("suggestions is required.", message);
    }

    @Test
    public void testConvertToSynapseSuggestionResultWithEmptySuggestionsMap() {
        //call under test
        SuggestionResults resultsEmpty = OssUtil.convertToSynapseSuggestionResult(Collections.emptyMap());
        assertNotNull(resultsEmpty);
        assertTrue(resultsEmpty.getSuggestions().isEmpty());
    }


    @Test
    public void convertToSynapseSuggestionResultWithNullSuggestInList() {
        List<Suggest<DocumentFields>> suggestList = new ArrayList<>();
        suggestList.add(null);
        Map<String, List<Suggest<DocumentFields>>> suggestions = Map.of("key1", suggestList);

        //  call under test
        SuggestionResults results = OssUtil.convertToSynapseSuggestionResult(suggestions);
        assertNotNull(results);
        assertTrue(results.getSuggestions().isEmpty());
    }

    @Test
    public void testConvertToSynapseSuggestionResultEmptyMap() {
        Map<String, List<Suggest<DocumentFields>>> suggestions = new HashMap<>();
        //call under test
        SuggestionResults results = OssUtil.convertToSynapseSuggestionResult(suggestions);
        assertNotNull(results);
        assertTrue(results.getSuggestions().isEmpty());
    }

    @Test
    public void convertToSynapseSuggestionResultHandlesSuggestWithOutOptions() {
        Suggest<DocumentFields> suggest = createTermSuggest("term1", Collections.emptyList());

        Map<String, List<Suggest<DocumentFields>>> suggestions = Map.of("key1", List.of(suggest));
        //call under test
        SuggestionResults results = OssUtil.convertToSynapseSuggestionResult(suggestions);
        assertNotNull(results);
        assertEquals(1, results.getSuggestions().size());
        assertTrue(results.getSuggestions().get(0).getValues().isEmpty());
    }

    @Test
    public void testConvertToSynapseSuggestionResultMapWithEmptyLists() {
        Map<String, List<Suggest<DocumentFields>>> suggestions = Map.of("key1", Collections.emptyList());
        //call under test
        SuggestionResults results = OssUtil.convertToSynapseSuggestionResult(suggestions);
        assertNotNull(results);
        assertTrue(results.getSuggestions().isEmpty());
    }

    @Test
    public void testConvertToSynapseSuggestionResultWithTermAndOptions() {
        TermSuggestOption opt1 = createOption("opt1", 2, 1.5f);
        PhraseSuggestOption opt2 = PhraseSuggestOption.of(o -> o.text("opt2").score(2.5f));
        Suggest<DocumentFields> termSuggest = createTermSuggest("term1", List.of(opt1));
        Suggest<DocumentFields> phraseSuggest = createPhraseSuggest("\"term2\"", List.of(opt2));
        Map<String, List<Suggest<DocumentFields>>> suggestions = Map.of("key1", List.of(termSuggest), "key2", List.of(phraseSuggest));

        //call under test
        SuggestionResults results = OssUtil.convertToSynapseSuggestionResult(suggestions);
        assertNotNull(results);
        assertEquals(2, results.getSuggestions().size());

        Map<String, SuggestionList> resultKeys = results.getSuggestions().stream()
                .collect(Collectors.toMap(SuggestionList::getKey, Function.identity()));

        SuggestionList synSuggestion = resultKeys.get("term1");
        Suggestion expectedSug1 =new Suggestion().setTerm("opt1").setScore(1.5).setFrequency(null);
        assertEquals(1, synSuggestion.getValues().size());
        assertTrue(synSuggestion.getValues().contains(expectedSug1));

        SuggestionList synSuggestion2 = resultKeys.get("\"term2\"");
        Suggestion expectedSug2 =new Suggestion().setTerm("\"opt2\"").setScore(2.5).setFrequency(null);
        assertEquals(1, synSuggestion2.getValues().size());
        assertTrue(synSuggestion2.getValues().contains(expectedSug2));
    }

    @Test
    public void testConvertToSynapseSuggestionResultMultipleSuggestsPerList() {
        Suggest<DocumentFields> suggest1 = createTermSuggest("term1", List.of(createOption("opt1", 1, 1.0f)));
        Suggest<DocumentFields> suggest2 = createTermSuggest("term2", List.of(createOption("opt2", 2, 2.0f)));
        Map<String, List<Suggest<DocumentFields>>> suggestions = Map.of("key1", List.of(suggest1, suggest2));

        //call under test
        SuggestionResults results = OssUtil.convertToSynapseSuggestionResult(suggestions);
        assertNotNull(results);
        Set<String> terms = new HashSet<>();
        for (SuggestionList s : results.getSuggestions()) {
            terms.add(s.getKey());
        }
        assertTrue(terms.contains("term1"));
        assertTrue(terms.contains("term2"));
    }

    @Test
    public void testConvertToSynapseSuggestionResultMultipleKeys() {
        Suggest<DocumentFields> suggest1 = createTermSuggest("term1", List.of(createOption("opt1", 1, 1.0f)));
        Suggest<DocumentFields> suggest2 = createTermSuggest("term2", List.of(createOption("opt2", 2, 2.0f)));
        Map<String, List<Suggest<DocumentFields>>> suggestions = Map.of(
                "key1", List.of(suggest1),
                "key2", List.of(suggest2)
        );

        //call under test
        SuggestionResults results = OssUtil.convertToSynapseSuggestionResult(suggestions);
        assertNotNull(results);
        Set<String> terms = new HashSet<>();
        for (SuggestionList s : results.getSuggestions()) {
            terms.add(s.getKey());
        }
        assertTrue(terms.contains("term1"));
        assertTrue(terms.contains("term2"));
    }

    @Test
    public void testConvertToSynapseSuggestionResultDuplicateOptionsForSameTerm() {
        TermSuggestOption opt1 = createOption("dup", 1, 1.0f);
        TermSuggestOption opt2 = createOption("dup", 1, 1.0f);
        Suggest<DocumentFields> suggest = createTermSuggest("term1", List.of(opt1, opt2));
        Map<String, List<Suggest<DocumentFields>>> suggestions = Map.of("key1", List.of(suggest));

        //call under test
        SuggestionResults results = OssUtil.convertToSynapseSuggestionResult(suggestions);
        assertNotNull(results);
        assertEquals(1, results.getSuggestions().size());
        SuggestionList synSuggestion = results.getSuggestions().get(0);
        assertEquals(1, synSuggestion.getValues().size());
        assertEquals("dup", synSuggestion.getValues().iterator().next().getTerm());
    }

    private TermSuggestOption createOption(String text, long freq, float score) {
        return TermSuggestOption.of(o -> o.text(text).freq(freq).score(score));
    }

    private Suggest<DocumentFields> createTermSuggest(String termText, List<TermSuggestOption> options) {
        return Suggest.of(s -> s.term(TermSuggest.of(ts -> ts.text(termText).length(termText.length()).offset(0).options(options))));
    }

    private Suggest<DocumentFields> createPhraseSuggest(String termText, List<PhraseSuggestOption> options) {
        return Suggest.of(s -> s.phrase(PhraseSuggest.of(ts -> ts.text(termText).length(termText.length()).offset(0).options(options))));
    }

    @Test
    void testAuthorizedUserAndSuggestionsPresent() {
        SuggestionList suggestionList = new SuggestionList().setKey("disease").setValues(new HashSet<>(List.of(
                new Suggestion().setTerm("cancer"),
                new Suggestion().setTerm("tumor")
        )));
        SuggestionResults suggestionResults = new SuggestionResults().setSuggestions(List.of(suggestionList));

        //call under test
        SearchRequest request = OssUtil.generateAggregationRequestToLimitAccess(userInfo, suggestionResults);

        assertEquals(SearchConstants.OPEN_SEARCH_INDEX_NAME, request.index().get(0));

        // 2. Verify ACL Filter (Authorization) is present
        BoolQuery boolQuery = request.query().bool();
        assertNotNull(boolQuery.filter());

        // 3. Verify Aggregation is present
        Aggregation queryCountsAgg = request.aggregations().get("query_counts");
        assertNotNull(queryCountsAgg);

        // 4. Verify Aggregation Content
        Map<String, Query> aggFilters = queryCountsAgg.filters().filters().keyed();
        assertEquals(2, aggFilters.size());
        assertTrue(aggFilters.containsKey("cancer"));
        assertTrue(aggFilters.containsKey("tumor"));

        // 5. Verify Aggregation Query Type (Simple Query String)
        assertEquals("SimpleQueryString", aggFilters.get("cancer")._kind().name());
    }

    @Test
    void testAdminUserAndSuggestionsPresent() {
        SuggestionList suggestionList = new SuggestionList().setKey("disease").setValues(new HashSet<>(List.of(
                new Suggestion().setTerm("cancer"),
                new Suggestion().setTerm("tumor")
        )));
        SuggestionResults suggestionResults = new SuggestionResults().setSuggestions(List.of(suggestionList));

        UserInfo userInfo = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
        //call under test
        SearchRequest request = OssUtil.generateAggregationRequestToLimitAccess(userInfo, suggestionResults);

        // 1. Verify ACL Filter is ABSENT
        BoolQuery boolQuery = request.query().bool();
        assertTrue(boolQuery.filter().isEmpty());

        // 2. Verify Aggregation is present
        Aggregation queryCountsAgg = request.aggregations().get("query_counts");
        assertNotNull(queryCountsAgg);
        assertEquals(2, queryCountsAgg.filters().filters().keyed().size());
    }

    @Test
    void testAggregationIsEmptyWithoutSuggestion() {
        SuggestionResults emptyResults = new SuggestionResults().setSuggestions(Collections.emptyList()); // No suggestions

        // call under test
        SearchRequest request = OssUtil.generateAggregationRequestToLimitAccess(userInfo, emptyResults);

        BoolQuery boolQuery = request.query().bool();
        assertNotNull(boolQuery.filter());

        // Verify Aggregation is present but empty
        assertNotNull(request.aggregations());
        assertTrue(request.aggregations().isEmpty());
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
                .shards(shard -> shard.total(1).failed(0).successful(1))
                .aggregations(Map.of(nodeType, Aggregate.of(a -> a
                                        .sterms(terms -> terms
                                                .buckets(buckets -> buckets.array(Arrays.asList(
                                                        StringTermsBucket.of(b -> b.key(nodeBucketKey).docCount(100L))
                                                ))).sumOtherDocCount(0L))),
                                createdON, Aggregate.of(a -> a
                                        .lterms(terms -> terms
                                                .buckets(buckets -> buckets.array(Arrays.asList(
                                                        LongTermsBucket.of(b -> b.key(LongTermsBucketKey.of(k -> k.signed(Long.parseLong(createdOnBucketKey)))).keyAsString(createdOnBucketKey).docCount(100L))
                                                ))).sumOtherDocCount(0L)))
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
