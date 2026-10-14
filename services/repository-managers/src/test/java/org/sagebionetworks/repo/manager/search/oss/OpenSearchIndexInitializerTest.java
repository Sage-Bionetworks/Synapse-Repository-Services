package org.sagebionetworks.repo.manager.search.oss;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.ErrorCause;
import org.opensearch.client.opensearch._types.ErrorResponse;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.CreateIndexResponse;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;
import org.opensearch.client.transport.endpoints.BooleanResponse;
import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.repo.manager.search.SearchConstants;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class OpenSearchIndexInitializerTest {
    ArgumentCaptor<ExistsRequest> captorIndexExists = ArgumentCaptor.forClass(ExistsRequest.class);
    ArgumentCaptor<CreateIndexRequest> captorIndexCreation = ArgumentCaptor.forClass(CreateIndexRequest.class);
    @Mock
    private OpenSearchClient mockOpenSearchClient;
    @Mock
    private OpenSearchIndicesClient mockIndicesClient;
    @Mock
    Logger mockLog;
    @Mock
    LoggerProvider mockLoggerProvider;

    private OpenSearchIndexInitializer initializer;

    @BeforeEach
    public void before() {
        when(mockLoggerProvider.getLogger(anyString())).thenReturn(mockLog);
        initializer = new OpenSearchIndexInitializer(mockLoggerProvider, mockOpenSearchClient);
    }

    /**
     * Helper method for Mockito 5 compatibility: Sets up a stub that executes the lambda
     * parameter for exists() so the ExistsRequest can be captured.
     */
    private void stubExistsToExecuteLambda(boolean existsResult) throws IOException {
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.Function<ExistsRequest.Builder, org.opensearch.client.util.ObjectBuilder<ExistsRequest>> fn =
                invocation.getArgument(0);
            ExistsRequest.Builder builder = new ExistsRequest.Builder();
            fn.apply(builder);
            return new BooleanResponse(existsResult);
        }).when(mockIndicesClient).exists(ArgumentMatchers.<java.util.function.Function>any());
    }

    @Test
    public void testIndexCreation() throws IOException {
        when(mockOpenSearchClient.indices()).thenReturn(mockIndicesClient);
        stubExistsToExecuteLambda(false);
        when(mockIndicesClient.create((CreateIndexRequest) ArgumentMatchers.any())).thenReturn(
                new CreateIndexResponse.Builder().index(SearchConstants.OPEN_SEARCH_INDEX_NAME)
                        .acknowledged(true).shardsAcknowledged(true).build());

        //call under test
        initializer.init();

        verify(mockIndicesClient).exists(ArgumentMatchers.<java.util.function.Function>any());
        verify(mockLog).info("Index {} creation completed.", SearchConstants.OPEN_SEARCH_INDEX_NAME);
        verify(mockIndicesClient).create(captorIndexCreation.capture());
        CreateIndexRequest capturedCreation = captorIndexCreation.getValue();
        assertEquals(SearchConstants.OPEN_SEARCH_INDEX_NAME, capturedCreation.index());
    }

    @Test
    public void testNullAcknowledgeIndexCreation() throws IOException {
        when(mockOpenSearchClient.indices()).thenReturn(mockIndicesClient);
        stubExistsToExecuteLambda(false);
        when(mockIndicesClient.create((CreateIndexRequest) ArgumentMatchers.any())).thenReturn(
                new CreateIndexResponse.Builder().index(SearchConstants.OPEN_SEARCH_INDEX_NAME)
                        .acknowledged(false).shardsAcknowledged(true).build());

        //call under test
        initializer.init();

        verify(mockIndicesClient).exists(ArgumentMatchers.<java.util.function.Function>any());
        verify(mockIndicesClient).create(captorIndexCreation.capture());
        CreateIndexRequest capturedCreation = captorIndexCreation.getValue();
        assertEquals(SearchConstants.OPEN_SEARCH_INDEX_NAME, capturedCreation.index());
        verify(mockLog).error("Index {} creation was not acknowledged.", SearchConstants.OPEN_SEARCH_INDEX_NAME);
    }

    @Test
    public void testCreateIndexThrowOpenSearchException() throws IOException {
        when(mockOpenSearchClient.indices()).thenReturn(mockIndicesClient);
        stubExistsToExecuteLambda(false);
        OpenSearchException exception = new OpenSearchException(
                ErrorResponse.of(er -> er.error(ErrorCause.of(er1 -> er1.reason("reason").type("type")))));
        when(mockIndicesClient.create((CreateIndexRequest) ArgumentMatchers.any())).thenThrow(exception);

        //call under test
        initializer.init();

        verify(mockIndicesClient).exists(ArgumentMatchers.<java.util.function.Function>any());
        verify(mockIndicesClient).create(captorIndexCreation.capture());
        CreateIndexRequest capturedCreation = captorIndexCreation.getValue();
        assertEquals(SearchConstants.OPEN_SEARCH_INDEX_NAME, capturedCreation.index());
        verify(mockLog).error("Index {} creation failed {}.", SearchConstants.OPEN_SEARCH_INDEX_NAME, exception);
    }
    @Test
    public void testCreateIndexThrowResourceAlreadyExistsException() throws IOException {
        when(mockOpenSearchClient.indices()).thenReturn(mockIndicesClient);
        stubExistsToExecuteLambda(false);
        when(mockIndicesClient.create((CreateIndexRequest) ArgumentMatchers.any())).thenThrow(new OpenSearchException(
                ErrorResponse.of(er -> er.error(ErrorCause.of(er1 -> er1.reason("because index already exists")
                        .type("resource_already_exists_exception"))))));

        //call under test
        initializer.init();

        verify(mockIndicesClient).exists(ArgumentMatchers.<java.util.function.Function>any());
        verify(mockIndicesClient).create(captorIndexCreation.capture());
        CreateIndexRequest capturedCreation = captorIndexCreation.getValue();
        assertEquals(SearchConstants.OPEN_SEARCH_INDEX_NAME, capturedCreation.index());
        verify(mockLog).error("Index {} already exists.",
                SearchConstants.OPEN_SEARCH_INDEX_NAME);
    }

    @Test
    public void testCreateIndexThrowIOException() throws IOException {
        IOException exception = new IOException("IOException");
        when(mockOpenSearchClient.indices()).thenReturn(mockIndicesClient);
        stubExistsToExecuteLambda(false);
        when(mockIndicesClient.create((CreateIndexRequest) ArgumentMatchers.any())).thenThrow(exception);

        //call under test
        initializer.init();

        verify(mockIndicesClient).exists(ArgumentMatchers.<java.util.function.Function>any());
        verify(mockIndicesClient).create(captorIndexCreation.capture());
        CreateIndexRequest capturedCreation = captorIndexCreation.getValue();
        assertEquals(SearchConstants.OPEN_SEARCH_INDEX_NAME, capturedCreation.index());
        verify(mockLog).error("Index {} creation failed {}.",
                SearchConstants.OPEN_SEARCH_INDEX_NAME, exception);
    }

    @Test
    public void testIndexAlreadyExists() throws IOException {
        when(mockOpenSearchClient.indices()).thenReturn(mockIndicesClient);
        stubExistsToExecuteLambda(true);

        //call under test
        initializer.init();

        verify(mockIndicesClient).exists(ArgumentMatchers.<java.util.function.Function>any());
        verify(mockIndicesClient, Mockito.never()).create((CreateIndexRequest) ArgumentMatchers.any());
    }
}




