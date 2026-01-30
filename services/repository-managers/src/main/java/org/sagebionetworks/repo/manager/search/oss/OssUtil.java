package org.sagebionetworks.repo.manager.search.oss;


import org.apache.commons.lang3.StringUtils;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.SuggestMode;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.FiltersAggregation;
import org.opensearch.client.opensearch._types.aggregations.FiltersBucket;
import org.opensearch.client.opensearch._types.aggregations.LongTermsAggregate;
import org.opensearch.client.opensearch._types.aggregations.LongTermsBucket;
import org.opensearch.client.opensearch._types.aggregations.StringTermsAggregate;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.MatchAllQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.RangeQuery;
import org.opensearch.client.opensearch._types.query_dsl.TermQuery;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.Suggest;
import org.opensearch.client.opensearch.core.search.Suggester;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.search.DocumentFields;
import org.sagebionetworks.repo.model.search.Facet;
import org.sagebionetworks.repo.model.search.FacetConstraint;
import org.sagebionetworks.repo.model.search.FacetTypeNames;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.KeyRange;
import org.sagebionetworks.repo.model.search.query.KeyValue;
import org.sagebionetworks.repo.model.search.query.SearchFacetOption;
import org.sagebionetworks.repo.model.search.query.SearchFacetSort;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.repo.model.search.query.Suggestion;
import org.sagebionetworks.repo.model.search.query.SuggestionList;
import org.sagebionetworks.repo.model.search.query.SuggestionQuery;
import org.sagebionetworks.repo.model.search.query.SuggestionResults;
import org.sagebionetworks.repo.manager.search.SearchConstants;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.sagebionetworks.repo.manager.search.SearchConstants.FIELD_ACL;


public class OssUtil {
    public final static String NAME_FIELD = "name";
    public final static String DESCRIPTION_FIELD = "description";
    public final static String NAME_TRIGRAM_FIELD = "name.trigram";
    public final static String DESCRIPTION_TRIGRAM_FIELD = "description.trigram";
    public final static String TERM_NAME_SUGGESTION = "term_name_suggestion";
    public final static String TERM_DESCRIPTION_SUGGESTION = "term_description_suggestion";
    public final static String PHRASE_NAME_SUGGESTION = "phrase_name_suggestion";
    public final static String PHRASE_DESCRIPTION_SUGGESTION = "phrase_description_suggestion";
    public final static String AGG_KEY = "query_counts";
    public final static int DEFAULT_SUGGEST_COUNT = 5;
    public final static int DEFAULT_MAX_Edit = 2;

    public static SearchRequest generateSearchRequestForSuggestion(SuggestionQuery suggestionQuery) {
        ValidateArgument.required(suggestionQuery, "suggestionQuery");
        List<String> terms = suggestionQuery.getSearchTerm();
        List<String> quotedList = new ArrayList<>();
        List<String> unquotedList = new ArrayList<>();
        int size = suggestionQuery.getSize() != null ? Math.toIntExact(suggestionQuery.getSize()) : DEFAULT_SUGGEST_COUNT;
        int maxEdits = suggestionQuery.getMaxEdit() != null ? Math.toIntExact(suggestionQuery.getMaxEdit()) : DEFAULT_MAX_Edit;

        if (terms != null && terms.size() == 1 && ("".equals(terms.get(0)))) {
            terms = null;
        }

        if (CollectionUtils.isEmpty(terms)) {
            throw new IllegalArgumentException(
                    "At least one search term should be provided for suggestion.");
        }

        SearchRequest.Builder searchBuilder = new SearchRequest.Builder()
                .index(SearchConstants.OPEN_SEARCH_INDEX_NAME).size(0);
        Suggester.Builder suggesterBuilder = new Suggester.Builder();

        terms.forEach(term -> {
            term = term.trim();
            int length = term.length();
            if (length > 2 && term.startsWith("\"") && term.endsWith("\"")) {
                // Safe removal of quotes
                quotedList.add(term.substring(1, length - 1));
            } else {
                unquotedList.add(term);
            }
        });

        addTermSuggester(suggesterBuilder, size, maxEdits, String.join(" ", unquotedList));
        addPhraseSuggester(suggesterBuilder, size, quotedList);

        Suggester suggester = suggesterBuilder.build();
        return searchBuilder.suggest(suggester).build();
    }

    public static void addTermSuggester(Suggester.Builder suggesterBuilder, int size, int maxEdits, String text) {
        suggesterBuilder.suggesters(TERM_NAME_SUGGESTION, s -> s.text(text)
                .term(
                        ts -> ts
                                .field(NAME_FIELD)
                                .size(size)
                                .maxEdits(maxEdits)
                                .suggestMode(SuggestMode.Missing)
                ));
        suggesterBuilder.suggesters(TERM_DESCRIPTION_SUGGESTION, s -> s.text(text)
                .term(
                        ts -> ts
                                .field(DESCRIPTION_FIELD)
                                .size(size)
                                .maxEdits(maxEdits)
                                .suggestMode(SuggestMode.Missing
                                )));
    }

    public static void addPhraseSuggester(Suggester.Builder suggesterBuilder, int size, List<String> phrases) {
        if (phrases.isEmpty()) {
            return;
        }
        phrases.forEach(txt -> {
            String text = txt.replaceAll("\\s", "_");
            String nameSuggestor = PHRASE_NAME_SUGGESTION + "_" + text;
            String descriptionSuggestor = PHRASE_DESCRIPTION_SUGGESTION + "_" + text;
            suggesterBuilder.suggesters(nameSuggestor, s -> s.text(txt)
                    .phrase(
                            ps -> ps
                                    .field(NAME_TRIGRAM_FIELD)
                                    .size(size)
                    ));
            suggesterBuilder.suggesters(descriptionSuggestor, s -> s.text(txt)
                    .phrase(
                            ps -> ps
                                    .field(DESCRIPTION_TRIGRAM_FIELD)
                                    .size(size)
                    ));
        });
    }

    public static SuggestionResults convertToSynapseSuggestionResult(Map<String, List<Suggest<DocumentFields>>> suggestions) {
        ValidateArgument.required(suggestions, "suggestions");
        SuggestionResults results = new SuggestionResults();
        Map<String, Set<Suggestion>> map1 = new HashMap<>();

        suggestions.entrySet().stream()
                .map(Map.Entry::getValue)
                .filter(suggestList -> !suggestList.isEmpty())
                .flatMap(List::stream)
                .filter(suggest -> suggest != null && suggest.isTerm() && (suggest.term() != null))
                .forEach(suggest -> {
                    Set<Suggestion> optionSet = map1.computeIfAbsent(suggest.term().text(), k -> new HashSet<>());
                    suggest.term().options().stream()
                            .filter(option -> option != null && StringUtils.isNotEmpty(option.text()))
                            .map(option -> new Suggestion()
                                    .setTerm(option.text())
                                    .setScore(option.score()))
                            .forEach(optionSet::add);
                });

        suggestions.entrySet().stream()
                .map(Map.Entry::getValue)
                .filter(suggestList -> !suggestList.isEmpty())
                .flatMap(List::stream)
                .filter(suggest -> suggest != null && suggest.isPhrase() && (suggest.phrase() != null))
                .forEach(suggest -> {
                    Set<Suggestion> optionSet = map1.computeIfAbsent(suggest.phrase().text(), k -> new HashSet<>());
                    suggest.phrase().options().stream()
                            .filter(option -> option != null && StringUtils.isNotEmpty(option.text()))
                            .map(option -> new Suggestion()
                                    .setTerm("\"" + option.text() + "\"")
                                    .setScore(option.score()))
                            .forEach(optionSet::add);
                });

        List<SuggestionList> suggestionList = map1.entrySet()
                .stream()
                .map(entry -> { // 2. Transform each entry into a Suggestion object
                    return new SuggestionList()
                            .setKey(entry.getKey())
                            .setValues(entry.getValue());
                })
                .collect(Collectors.toList());

        return results.setSuggestions(suggestionList);

    }

    /**
     * Filters suggestion results to return only the results which appear in a document the user can see
     * (represented in 'aggregateResponse').
     * * This function uses a pre-calculated aggregation map (aggregateResponse)
     * to verify if each suggestion term exists in at least one document
     * that the current user has access to. Suggestions tied only to inaccessible documents are removed.
     *
     * @param suggestionResults The initial list of suggestions from the search engine.
     * @param aggregateResponse A Map containing the aggregation results (AGG_KEY) which provides
     * document counts for each suggestion term scoped to the user's access rights.
     * @return The filtered SuggestionResults, containing only terms the user is authorized to see.
     */
    public static SuggestionResults eliminateSuggestionWithAccessDenied(SuggestionResults suggestionResults, Map<String, Aggregate> aggregateResponse) {
        ValidateArgument.required(suggestionResults, "suggestionResults");
        ValidateArgument.required(aggregateResponse, "aggregateResponse");

        if (!aggregateResponse.containsKey(AGG_KEY)) {
            // Since we cannot verify access without the aggregation data,
            // we assume access cannot be granted and return an empty set of suggestions.
            return new SuggestionResults().setSuggestions(Collections.emptyList());
        }

        boolean isAggregationValid = aggregateResponse.containsKey(AGG_KEY) &&
                aggregateResponse.get(AGG_KEY) != null &&
                aggregateResponse.get(AGG_KEY).filters() != null &&
                aggregateResponse.get(AGG_KEY).filters().buckets() != null;

        if (!isAggregationValid) {
            // If the required aggregation for access verification is missing or malformed return an empty result.
            return new SuggestionResults().setSuggestions(Collections.emptyList());
        }

        Map<String, FiltersBucket> filteredBucketMap = aggregateResponse.get(AGG_KEY).filters().buckets().keyed();
        for (SuggestionList suggestionList : suggestionResults.getSuggestions()) {

            // Stream over the existing options (suggestion.getValues()),
            // filter them, and collect the valid ones.
            if (suggestionList.getValues() == null || suggestionList.getValues().isEmpty()) {
                continue;
            }
            List<Suggestion> filteredOptions = suggestionList.getValues().stream()
                    .filter(option -> {
                        String term = option.getTerm();

                        // Check if the term exists in the map AND its docCount is > 0.
                        return term != null && filteredBucketMap.containsKey(term) &&
                                filteredBucketMap.get(term).docCount() > 0;
                    })
                    .map(option -> {
                        String term = option.getTerm();
                        option.setFrequency(filteredBucketMap.get(term).docCount());
                        //quotes were added so simple query treat quoted as phrase and otherwise term. Remove quotes from final result.
                        option.setTerm(term.replace("\"", ""));
                        return option;
                    })
                    .collect(Collectors.toList());
            suggestionList.setValues(new HashSet<>(filteredOptions));
        }
        return suggestionResults;
    }

    public static SearchRequest generateAggregationRequestToLimitAccess(UserInfo userInfo, SuggestionResults suggestionResults) {
        SearchRequest.Builder searchBuilder = new SearchRequest.Builder()
                .index(SearchConstants.OPEN_SEARCH_INDEX_NAME);
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
        Set<Long> userGroups = getAuthorizedUserGroups(userInfo);
        Map<String, Query> aggregations = new HashMap<>();
        if (!CollectionUtils.isEmpty(userGroups)) {
            boolBuilder.filter(
                    Query.of(tq -> tq.terms(t -> t
                            .field(FIELD_ACL)
                            .terms(queryTerm -> queryTerm.value(
                                    userGroups.stream()
                                            .map(FieldValue::of)
                                            .collect(Collectors.toList())
                            )))));
        }
        for (SuggestionList suggestionList : suggestionResults.getSuggestions()) {
            for (Suggestion suggestion : suggestionList.getValues()) {
                aggregations.put(suggestion.getTerm(), Query.of(q -> q.simpleQueryString(simpleQuery -> simpleQuery.query(suggestion.getTerm()))));
            }
        }

        // Aggregation filter should be added only for non empty values otherwise throws OpenSearch Exception
        if (!aggregations.isEmpty()) {
            FiltersAggregation fs = FiltersAggregation.of(f -> f.filters(bc -> bc.keyed(aggregations)));
            searchBuilder.aggregations(AGG_KEY, Aggregation.of(a -> a.filters(fs)));
        }

        // we are setting size to zero because we only need aggregation results.
        searchBuilder.size(0);

        return searchBuilder
                .query(boolBuilder.build()._toQuery()).build();
    }

    public static SearchRequest generateSearchRequest(UserInfo userInfo, SearchQuery searchQuery) {
        ValidateArgument.required(searchQuery, "searchQuery");

        List<String> terms = searchQuery.getQueryTerm();
        List<KeyValue> booleanQueries = searchQuery.getBooleanQuery();
        List<KeyRange> rangeQueries = searchQuery.getRangeQuery();
        List<SearchFacetOption> searchFacetOptions = searchQuery.getFacetOptions();
        Set<Long> userGroups = getAuthorizedUserGroups(userInfo);
        Map<String, Aggregation> aggregations = new HashMap<>();

        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
        SearchRequest.Builder searchBuilder = new SearchRequest.Builder()
                .index(SearchConstants.OPEN_SEARCH_INDEX_NAME);

        // clean up empty q
        if (terms != null && terms.size() == 1 && "".equals(terms.get(0))) {
            terms = null;
        }

        // check for minimum search requirements
        if (CollectionUtils.isEmpty(terms) && CollectionUtils.isEmpty(booleanQueries)) {
            throw new IllegalArgumentException(
                    "Either one queryTerm or one booleanQuery must be defined.");
        }

        if (!CollectionUtils.isEmpty(terms)) {
            String queryTerms = String.join(" ", terms);
            boolBuilder.must(m -> m.simpleQueryString(sqs -> sqs.query(queryTerms)));
        } else {
            // If no terms, use match_all
            boolBuilder.must(
                    MatchAllQuery.of(m -> m)._toQuery()
            );
        }

        if (!CollectionUtils.isEmpty(booleanQueries)) {
            booleanQueries.forEach(query -> {
                Query termQuery = TermQuery.of(t -> t
                        .field(query.getKey())
                        .value(FieldValue.of(query.getValue()))
                )._toQuery();
                if (query.getNot() != null && query.getNot()) {
                    // Add to must_not if this is a NOT condition
                    boolBuilder.mustNot(termQuery);
                } else {
                    boolBuilder.filter(termQuery);
                }
            });
        }

        if (!CollectionUtils.isEmpty(rangeQueries)) {

            rangeQueries.forEach(query -> {
                ValidateArgument.required(query.getKey(), "keyRange key");
                if (query.getMax() == null && query.getMin() == null) {
                    throw new IllegalArgumentException("At least one of min or max for key=" + query.getKey() + " must be not null.");
                }

                RangeQuery.Builder builder = new RangeQuery.Builder().field(query.getKey());
                if (query.getMin() != null) {
                    builder.gte(JsonData.of(query.getMin()));
                }
                if (query.getMax() != null) {
                    builder.lte(JsonData.of(query.getMax()));
                }

                boolBuilder.filter(Query.of(q -> q.range(builder.build())));

            });
        }

        if (!CollectionUtils.isEmpty(userGroups)) {
            boolBuilder.filter(
                    Query.of(tq -> tq.terms(t -> t
                            .field(FIELD_ACL)
                            .terms(queryTerm -> queryTerm.value(
                                    userGroups.stream()
                                            .map(FieldValue::of)
                                            .collect(Collectors.toList())
                            )))));
        }

        if (!CollectionUtils.isEmpty(searchFacetOptions)) {
            // in cloud search we check if its facet-able or not
            searchFacetOptions.forEach(facet -> {
                SortOrder order = SortOrder.Desc;
                if (SearchFacetSort.ALPHA == facet.getSortType()) {
                    order = SortOrder.Asc;
                }
                SortOrder finalOrder = order;
                // facet filed value should be exactly same as it is in DocumentField class
                String filedValue = SynapseToOpenSearchAggregationField.openSearchFieldFor(facet.getName());
                aggregations.put(filedValue, Aggregation.of(a -> a
                        .terms(t -> t
                                .field(filedValue)
                                .size(Math.toIntExact(facet.getMaxResultCount()))
                                .order((List.of(Map.of("_count", finalOrder)))))));
            });

            searchBuilder.aggregations(aggregations);
        }

        if (searchQuery.getSize() != null) {
            searchBuilder.size(Math.toIntExact(searchQuery.getSize()));
        }

        if (searchQuery.getStart() != null) {
            searchBuilder.from(Math.toIntExact(searchQuery.getStart()));
        }

        if (!CollectionUtils.isEmpty(searchQuery.getReturnFields())) {
            searchBuilder.source(s -> s
                    .filter(f -> f
                            .includes(searchQuery.getReturnFields())));
        }

        return searchBuilder
                .query(boolBuilder.build()._toQuery()).build();

    }

    public static Set<Long> getAuthorizedUserGroups(UserInfo userInfo) {
        ValidateArgument.required(userInfo, "userInfo");

        if (userInfo.isAdmin()) {
            return Collections.emptySet();
        }

        Set<Long> userGroups = userInfo.getGroups();
        ValidateArgument.requiredNotEmpty(userGroups, "userGroup for " + userInfo.getId());
        return userGroups;
    }


    public static SearchResults convertToSynapseSearchResult(SearchResponse<DocumentFields> response, Integer from) {
        SearchResults synapseSearchResults = new SearchResults();
        synapseSearchResults.setFacets(getFacets(response));
        synapseSearchResults.setStart(from == null ? 0L : Long.valueOf(from));
        synapseSearchResults.setFound(response.hits() == null ? 0L : response.hits().total().value());
        synapseSearchResults.setHits(convertToSynapseHit(response.hits().hits()));
        return synapseSearchResults;
    }

    public static List<Facet> getFacets(SearchResponse<DocumentFields> response) {

        List<Facet> facetList = new ArrayList<>();

        Map<String, Aggregate> aggregationMap = response.aggregations();

        for (Map.Entry<String, Aggregate> entry : aggregationMap.entrySet()) {
            String facetName = entry.getKey();

            Facet facet = new Facet();
            facet.setName(facetName);
            FacetTypeNames facetType = OpenSearchFieldType.fieldType(SynapseToOpenSearchAggregationField.synapseFieldFor(facetName));
            facet.setType(facetType);
            List<FacetConstraint> constraints = new ArrayList<>();

            Aggregate aggregate = entry.getValue();

            if (aggregate.isSterms()) {
                StringTermsAggregate termsAgg = aggregate.sterms();

                for (StringTermsBucket bucket : termsAgg.buckets().array()) {
                    FacetConstraint constraint = new FacetConstraint();
                    constraint.setValue(bucket.key());
                    constraint.setCount(bucket.docCount());
                    constraints.add(constraint);
                }
            } else if (aggregate.isLterms()) {
                LongTermsAggregate termsAgg = aggregate.lterms();

                for (LongTermsBucket bucket : termsAgg.buckets().array()) {
                    constraints.add(new FacetConstraint().setValue(bucket.key()).setCount(bucket.docCount()));
                }
            }

            facet.setConstraints(constraints);
            facetList.add(facet);
        }

        return facetList;
    }

    public static List<org.sagebionetworks.repo.model.search.Hit> convertToSynapseHit(List<Hit<DocumentFields>> hits) {
        List<org.sagebionetworks.repo.model.search.Hit> hitList = new ArrayList<>(hits.size());
        for (Hit<DocumentFields> hit : hits) {
            org.sagebionetworks.repo.model.search.Hit synapseHit = new org.sagebionetworks.repo.model.search.Hit();
            synapseHit.setId(hit.id());
            synapseHit.setCreated_by(hit.source().getCreated_by());
            synapseHit.setCreated_on(hit.source().getCreated_on());
            synapseHit.setDescription(hit.source().getDescription());
            synapseHit.setDiagnosis(hit.source().getDiagnosis());
            synapseHit.setEtag(hit.source().getEtag());
            synapseHit.setModified_by(hit.source().getModified_by());
            synapseHit.setModified_on(hit.source().getModified_on());
            synapseHit.setName(hit.source().getName());
            synapseHit.setNode_type(hit.source().getNode_type());
            synapseHit.setTissue(hit.source().getTissue());
            synapseHit.setConsortium(hit.source().getConsortium());
            synapseHit.setOrgan(hit.source().getOrgan());
            hitList.add(synapseHit);
        }

        return hitList;
    }

}
