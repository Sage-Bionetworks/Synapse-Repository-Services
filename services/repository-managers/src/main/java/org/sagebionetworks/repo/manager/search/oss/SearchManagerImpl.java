package org.sagebionetworks.repo.manager.search.oss;

import org.apache.logging.log4j.Logger;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.search.DocumentFields;
import org.sagebionetworks.repo.model.search.DocumentTypeNames;
import org.sagebionetworks.search.SearchConstants;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
public class SearchManagerImpl implements SearchManager {
    private Logger log;
    private ChangeMessageToOpenSearchDocumentTranslator translator;
    private OpenSearchIndexInitializer openSearchIndexInitializer;

    private OpenSearchClient openSearchClient;

    public SearchManagerImpl(LoggerProvider logProvider, ChangeMessageToOpenSearchDocumentTranslator translator,
                             OpenSearchIndexInitializer openSearchIndexInitializer, OpenSearchClient synSearchOssClient) {
        this.log = logProvider.getLogger(SearchManagerImpl.class.getName());
        this.translator = translator;
        this.openSearchIndexInitializer = openSearchIndexInitializer;
        this.openSearchClient = synSearchOssClient;
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
            long hasError = response.items().stream().filter(item -> item.error() != null)
                    .peek(item -> log.error("Document {} has error {}.", item.id(), item.error())).count();

            if (hasError > 0L) {
                log.error("The OpenSearch response has {} error", hasError);
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
}
