package org.sagebionetworks.repo.manager.search.oss;


import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.LongTermsAggregate;
import org.opensearch.client.opensearch._types.aggregations.LongTermsBucket;
import org.opensearch.client.opensearch._types.aggregations.StringTermsAggregate;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.MatchAllQuery;
import org.opensearch.client.opensearch._types.query_dsl.MultiMatchQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.RangeQuery;
import org.opensearch.client.opensearch._types.query_dsl.TermQuery;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
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
import org.sagebionetworks.search.SearchConstants;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.sagebionetworks.search.SearchConstants.FIELD_ACL;


public class OssUtil {

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
            boolBuilder.must(m ->m.simpleQueryString(sqs ->sqs.query(queryTerms)));
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
                if (query.getMin() != null){
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
