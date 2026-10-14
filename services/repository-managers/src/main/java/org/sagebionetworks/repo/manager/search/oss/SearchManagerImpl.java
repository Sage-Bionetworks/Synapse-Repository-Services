package org.sagebionetworks.repo.manager.search.oss;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;

import org.apache.logging.log4j.Logger;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.search.Suggest;
import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.repo.manager.search.SearchDocumentDriver;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.IdAndAlias;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.search.DocumentFields;
import org.sagebionetworks.repo.model.search.DocumentTypeNames;
import org.sagebionetworks.repo.model.search.Hit;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.repo.model.search.query.SuggestionQuery;
import org.sagebionetworks.repo.model.search.query.SuggestionResults;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;
import org.sagebionetworks.repo.manager.search.SearchConstants;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;


@Service
public class SearchManagerImpl implements SearchManager {
    private Logger log;
    private ChangeMessageToOpenSearchDocumentTranslator translator;
    private OpenSearchIndexInitializer openSearchIndexInitializer;

    private OpenSearchClient openSearchClient;
    private SearchDocumentDriver searchDocumentDriver;

    public SearchManagerImpl(LoggerProvider logProvider, ChangeMessageToOpenSearchDocumentTranslator translator,
                             OpenSearchIndexInitializer openSearchIndexInitializer, @Qualifier("synSearchOssClient") OpenSearchClient synSearchOssClient,
                             SearchDocumentDriver searchDocumentDriver) {
        this.log = logProvider.getLogger(SearchManagerImpl.class.getName());
        this.translator = translator;
        this.openSearchIndexInitializer = openSearchIndexInitializer;
        this.openSearchClient = synSearchOssClient;
        this.searchDocumentDriver = searchDocumentDriver;
    }

    @PostConstruct()
    public void init() throws IOException {
        openSearchIndexInitializer.init();
    }

    @Override
    public void documentChangeMessages(List<ChangeMessage> messages) {
        try {
            List<BulkOperation> operations = messages.stream()
                    .map(translator::generateSearchDocumentIfNecessary)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(document -> {
                        if (DocumentTypeNames.add == document.getType()) {
                            return BulkOperation.of(op -> op
                                    .index(idx -> idx
                                            .index(SearchConstants.OPEN_SEARCH_INDEX_NAME)
                                            .id(document.getId())
                                            .document(document.getFields())));
                        } else {
                            return BulkOperation.of(op -> op
                                    .delete(idx -> idx
                                            .index(SearchConstants.OPEN_SEARCH_INDEX_NAME)
                                            .id(document.getId())));
                        }
                    }).collect(Collectors.toList());

            if (operations.isEmpty()) {
                return;
            }

            BulkResponse response = openSearchClient.bulk(req -> req.operations(operations));

            // if any message fails to process we will throw exception to reprocess it.
            long errorCount = response.items().stream()
            	.filter(item -> item.error() != null)
                .peek(item -> {
                	log.error("Could not process document {} (Operation: {}): {} (Error Type: {}).", item.id(), item.operationType(), item.error().reason(), item.error().type());
                })
                .count();

            if (errorCount > 0L) {
                log.error("Could not process a batch of {} documents, received {} error(s). Will retry.", messages.size(), errorCount);
                throw new RecoverableMessageException();
            }
            
        } catch (OpenSearchException | IOException e) {
            log.error(e);
            throw new RecoverableMessageException(e);
        }
    }

    @Override
    public boolean doesDocumentExist(String id, String etag) throws OpenSearchException, IOException {
        try {
            SearchRequest request = SearchRequest.of(r -> r
                    .index(SearchConstants.OPEN_SEARCH_INDEX_NAME)
                    .query( q ->q.bool(
                            b ->b.must(List.of(
                                    Query.of( q1 ->q1.term(t ->t.field(SearchConstants.FIELD_ID).value(FieldValue.of(id)))),
                                    Query.of(q2 -> q2.term( t ->t.field(SearchConstants.FIELD_ETAG).value(FieldValue.of(etag))))

                            )))));


            SearchResponse<DocumentFields> response = openSearchClient.search(request, DocumentFields.class);

            return !response.hits().hits().isEmpty();
        } catch (OpenSearchException | IOException e) {
            log.error(e);
            throw e;
        }
    }

    @Override
    public SearchResults search(UserInfo userInfo, SearchQuery searchQuery) {
        long start = System.currentTimeMillis();
        try {
            boolean includePath = false;
            if (searchQuery.getReturnFields() != null) {
                // We do not want to pass FIELD_PATH along to the search index as it is not there. So we remove that field
                // and use includePath to indicate that the FIELD_PATH was requested.
                //List<T>.remove() returns a boolean indicating whether the return fields previously contained FIELD_PATH
                includePath = searchQuery.getReturnFields().remove(SearchConstants.FIELD_PATH);
            }

            // Create the search request
            long t1 = System.currentTimeMillis() - start;
            SearchRequest searchRequest = OssUtil.generateSearchRequest(userInfo, searchQuery);
            long t2 = System.currentTimeMillis() - start;
            SearchResponse<DocumentFields> response = openSearchClient.search(searchRequest, DocumentFields.class);
            long t3 = System.currentTimeMillis() - start;
            SearchResults results = OssUtil.convertToSynapseSearchResult(response, searchRequest.from());
            long t4 = System.currentTimeMillis() - start;
            long t5 = System.currentTimeMillis();

            if (results != null && results.getHits() != null) {
                if (includePath) {
                    // FIELD_PATH is resolved here after search results are retrieved from OpenSearch
                    addPathDataToHits(results.getHits());
                }
                t5 = System.currentTimeMillis() - start;
                addAliasesToHits(results.getHits());
            }
            long tEnd = System.currentTimeMillis() - start;
            log.info("Search timing: staringOfSearch={} preparingRequest={} sendingToOpensearch={} resultReceivedFromOpensearch={} " +
                    " convertedToSynapseResult={} pathAddedToHits={} aliasesAddedToHits={}.", start, t1, t2, t3, t4, t5, tEnd);
            return results;
        } catch (IOException exception) {
            log.error(exception);
            throw new TemporarilyUnavailableException(exception.getMessage());
        }
    }


    @Override
    public SuggestionResults getSuggestions(UserInfo userInfo, SuggestionQuery suggestionQuery) {
        SearchRequest searchRequest = OssUtil.generateSearchRequestForSuggestion(suggestionQuery);
        try {
            //get suggestions from OpenSearch regardless of user access to documents
            Map<String, List<Suggest<DocumentFields>>> suggestions =
                    openSearchClient.search(searchRequest, DocumentFields.class).suggest();
            SuggestionResults results = OssUtil.convertToSynapseSuggestionResult(suggestions);
            SearchRequest aggregationRequest = OssUtil.generateAggregationRequestToLimitAccess(userInfo, results);
            //find out which suggestions appear in documents the user can see.
            Map<String, Aggregate> aggregations = openSearchClient.search(aggregationRequest, DocumentFields.class).aggregations();
            //filter the suggestions to return only those which appear in document(s) the user can see
            return OssUtil.eliminateSuggestionWithAccessDenied(results, aggregations);
        } catch (OpenSearchException | IOException exception) {
            log.error(exception);
            throw new IllegalStateException(exception.getMessage());
        }
    }


    /**
     * Add path data to the hit list.
     *
     * @param hits
     */
    public void addPathDataToHits(List<Hit> hits) {
        // For each hit we need to add the path
        List<Hit> toRemove = new LinkedList<>();
        for (Hit hit : hits) {
            try {
                EntityPath path = searchDocumentDriver.getEntityPath(hit.getId());
                hit.setPath(path);
            } catch (NotFoundException e) {
                // Add a warning and remove it from the hits
                log.warn("Found a search document that did not exist in the repository: " + hit);
                // We need to remove this from the hits
                toRemove.add(hit);
            }
        }
        hits.removeAll(toRemove);
    }

    /**
     * Add aliases to the hit list.
     *
     * @param hits
     */
    public void addAliasesToHits(List<Hit> hits) {
        // add aliases
        List<String> ids = new ArrayList<String>();
        for (Hit hit : hits) {
            ids.add(hit.getId());
        }
        List<IdAndAlias> aliases = searchDocumentDriver.getAliases(ids);
        Map<String, String> idToAliasMap = new HashMap<String, String>();
        for (IdAndAlias ia : aliases) {
            idToAliasMap.put(ia.getId(), ia.getAlias());
        }
        for (Hit hit : hits) {
            hit.setAlias(idToAliasMap.get(hit.getId()));
        }
    }
}
