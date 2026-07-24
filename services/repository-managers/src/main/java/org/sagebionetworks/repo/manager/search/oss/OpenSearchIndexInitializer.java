package org.sagebionetworks.repo.manager.search.oss;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.CreateIndexResponse;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;
import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.repo.manager.search.SearchConstants;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class OpenSearchIndexInitializer {
    private static final String RESOURCE_EXISTS = "resource_already_exists_exception";
    private static final String TRIGRAM = "trigram";
    private Logger log;
    private OpenSearchClient client;

    public OpenSearchIndexInitializer(LoggerProvider logProvider, @Qualifier("synSearchOssClient") OpenSearchClient synSearchOssClient) {
        this.log = logProvider.getLogger(OpenSearchIndexInitializer.class.getName());
        this.client = synSearchOssClient;

    }

    public void init() throws IOException {
        try {
            OpenSearchIndicesClient indicesClient = client.indices();
            if (!indicesClient.exists(request -> request.index(SearchConstants.OPEN_SEARCH_INDEX_NAME)).value()) {
                CreateIndexRequest request = new CreateIndexRequest.Builder()
                        .index(SearchConstants.OPEN_SEARCH_INDEX_NAME)
                        .settings(s -> s
                                .analysis(a -> a
                                        .filter("shingle", f -> f.definition( d -> d
                                                .shingle(sh -> sh.minShingleSize(2).maxShingleSize(3))))
                                        .analyzer(TRIGRAM, an ->an
                                                .custom(c ->c
                                                        .tokenizer("standard")
                                                        .filter(List.of("lowercase", "shingle"))))
                                )
                        )
                        .mappings(m -> m
                                .source(s -> s.excludes(List.of(SearchConstants.FIELD_ACL,
                                        SearchConstants.FIELD_UPDATE_ACL, SearchConstants.FIELD_PARENT_ID)))
                                .properties(SearchConstants.FIELD_NAME, p -> p.text(text ->
                                        text.fields(TRIGRAM, f -> f.text( tt -> tt.analyzer(TRIGRAM)))))
                                .properties(SearchConstants.FIELD_DESCRIPTION,
                                        p -> p.text(text ->
                                                text.fields(TRIGRAM, f -> f.text( tt -> tt.analyzer(TRIGRAM)))))
                                .properties(SearchConstants.FIELD_CREATED_ON,
                                        p -> p.integer( i -> i))
                                .properties(SearchConstants.FIELD_MODIFIED_ON,
                                        p -> p.integer(i -> i))
                                .properties(SearchConstants.FIELD_NODE_TYPE,
                                        p -> p.keyword(k -> k))
                                .properties(SearchConstants.FIELD_ETAG,
                                        p -> p.keyword(k -> k))
                                .properties(SearchConstants.FIELD_PARENT_ID,
                                        p -> p.keyword(k -> k))
                                .properties(SearchConstants.FIELD_CREATED_BY,
                                        p -> p.keyword(k -> k))
                                .properties(SearchConstants.FIELD_MODIFIED_BY,
                                        p -> p.keyword(k -> k))
                                .properties(SearchConstants.FIELD_ACL,
                                        p -> p.keyword(k -> k))
                                .properties(SearchConstants.FIELD_UPDATE_ACL,
                                        p -> p.keyword(k -> k))
                                .properties(SearchConstants.FIELD_DIAGNOSIS,
                                        p -> p.keyword(k -> k))
                                .properties(SearchConstants.FIELD_TISSUE,
                                        p -> p.keyword(k -> k))
                                .properties(SearchConstants.FIELD_CONSORTIUM,
                                        p -> p.keyword(k -> k))
                                .properties(SearchConstants.FIELD_ORGAN,
                                        p -> p.keyword(k -> k))
                        ).build();

                CreateIndexResponse response = indicesClient.create(request);

                if (Boolean.TRUE.equals(response.acknowledged())) {
                    log.info("Index {} creation completed.", SearchConstants.OPEN_SEARCH_INDEX_NAME);
                } else {
                    log.error("Index {} creation was not acknowledged.", SearchConstants.OPEN_SEARCH_INDEX_NAME);
                }
            }
        } catch (OpenSearchException e) {
            if (RESOURCE_EXISTS.equals(e.error().type())) {
                log.error("Index {} already exists.", SearchConstants.OPEN_SEARCH_INDEX_NAME);
            } else {
                log.error("Index {} creation failed {}.", SearchConstants.OPEN_SEARCH_INDEX_NAME, e);
            }
        } catch (IOException e) {
            log.error("Index {} creation failed {}.", SearchConstants.OPEN_SEARCH_INDEX_NAME, e);
        }
    }
}
