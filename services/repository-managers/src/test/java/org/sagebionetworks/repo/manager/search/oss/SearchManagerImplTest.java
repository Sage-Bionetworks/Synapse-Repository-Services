package org.sagebionetworks.repo.manager.search.oss;


import com.google.common.collect.Lists;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.ErrorCause;
import org.opensearch.client.opensearch._types.ErrorResponse;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.Result;
import org.opensearch.client.opensearch._types.ShardStatistics;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;
import org.opensearch.client.opensearch.core.bulk.DeleteOperation;
import org.opensearch.client.opensearch.core.bulk.OperationType;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.TotalHits;
import org.opensearch.client.opensearch.core.search.TotalHitsRelation;
import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.repo.manager.search.SearchDocumentDriver;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.IdAndAlias;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.DocumentFields;
import org.sagebionetworks.repo.model.search.DocumentTypeNames;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.search.SearchConstants;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SearchManagerImplTest {

    ArgumentCaptor<BulkRequest> bulkRequestArgumentCaptor = ArgumentCaptor.forClass(BulkRequest.class);

    ArgumentCaptor<SearchRequest> searchRequestArgumentCaptor = ArgumentCaptor.forClass(SearchRequest.class);
    @Mock
    ChangeMessageToOpenSearchDocumentTranslator mockTranslator;
    @Mock
    Logger mockLog;
    @Mock
    LoggerProvider mockLoggerProvider;
    @Mock
    OpenSearchIndexInitializer mockOpenSearchIndexInitializer;
    @Mock
    SearchDocumentDriver mockSearchDocumentDriver;
    Document document;
    @Mock
    private OpenSearchClient mockSearchClient;
    private SearchManagerImpl mockSearchManager;

    @BeforeEach
    public void before() {
        when(mockLoggerProvider.getLogger(anyString())).thenReturn(mockLog);
        document = new Document();
        document.setType(DocumentTypeNames.add);
        document.setId("syn1");
        document.setFields(new DocumentFields().setName("test").setEtag("etag"));
        mockSearchManager = new SearchManagerImpl(mockLoggerProvider, mockTranslator, mockOpenSearchIndexInitializer, mockSearchClient, mockSearchDocumentDriver);
    }


    @Test
    public void testADDDocumentChangeMessages() throws IOException {
        when(mockTranslator.generateSearchDocumentIfNecessary(any(ChangeMessage.class)))
                .thenReturn(Optional.of(document)).thenReturn(Optional.empty());
        BulkResponseItem item = new BulkResponseItem.Builder()
                .index(SearchConstants.OPEN_SEARCH_INDEX_NAME)
                .id(document.getId())
                .status(201)
                .result(String.valueOf(Result.Created))
                .operationType(OperationType.Create)
                .build();
        when(mockSearchClient.bulk(any(BulkRequest.class))).thenReturn(new BulkResponse.Builder()
                .items(List.of(item)).errors(false).took(1L).build());

        //call under test
        mockSearchManager.documentChangeMessages(List.of(new ChangeMessage(), new ChangeMessage()));
        verify(mockSearchClient).bulk(bulkRequestArgumentCaptor.capture());
        BulkRequest request = bulkRequestArgumentCaptor.getValue();
        assertEquals(1, request.operations().size());
        assertEquals(SearchConstants.OPEN_SEARCH_INDEX_NAME, request.operations().get(0).index().index());
        assertEquals(request.operations().get(0).index().document(), document.getFields());
    }

    @Test
    public void testDeleteDocumentChangeMessages() throws IOException {
        document.setType(DocumentTypeNames.delete);

        when(mockTranslator.generateSearchDocumentIfNecessary(any(ChangeMessage.class)))
                .thenReturn(Optional.of(document));
        BulkResponseItem item = new BulkResponseItem.Builder()
                .index(SearchConstants.OPEN_SEARCH_INDEX_NAME)
                .id(document.getId())
                .status(404)
                .operationType(OperationType.Delete)
                .build();

        when(mockSearchClient.bulk(any(BulkRequest.class))).thenReturn(new BulkResponse.Builder()
                .items(List.of(item)).errors(false).took(1L).build());

        //call under test
        mockSearchManager.documentChangeMessages(List.of(new ChangeMessage()));
        verify(mockSearchClient).bulk(bulkRequestArgumentCaptor.capture());
        BulkRequest request = bulkRequestArgumentCaptor.getValue();
        assertEquals(1, request.operations().size());
        assertEquals(DeleteOperation.class, request.operations().get(0).delete().getClass());
        assertEquals(SearchConstants.OPEN_SEARCH_INDEX_NAME, request.operations().get(0).delete().index());
        verifyZeroInteractions(mockLog);
    }

    @Test
    public void testDocumentChangeMessagesWithNoDocument() {
        when(mockTranslator.generateSearchDocumentIfNecessary(any(ChangeMessage.class))).thenReturn(Optional.empty());

        //call under test
        mockSearchManager.documentChangeMessages(List.of(new ChangeMessage()));
        verifyZeroInteractions(mockSearchClient);
    }

    @Test
    public void testDocumentChangeMessagesErrorInResponse() throws IOException {
        when(mockTranslator.generateSearchDocumentIfNecessary(any(ChangeMessage.class)))
                .thenReturn(Optional.of(document));
        ErrorCause errorCause = ErrorCause.of(er -> er.reason("reason").type("type"));
        BulkResponseItem itemAdd = new BulkResponseItem.Builder()
                .index(SearchConstants.OPEN_SEARCH_INDEX_NAME)
                .id(document.getId())
                .status(201)
                .operationType(OperationType.Index)
                .build();

        BulkResponseItem itemAdd2 = new BulkResponseItem.Builder()
                .index(SearchConstants.OPEN_SEARCH_INDEX_NAME)
                .id(document.getId() + 1)
                .status(403)
                .operationType(OperationType.Index)
                .error(errorCause)
                .build();
        BulkResponseItem itemDel = new BulkResponseItem.Builder()
                .index(SearchConstants.OPEN_SEARCH_INDEX_NAME)
                .id(document.getId())
                .status(403)
                .error(errorCause)
                .operationType(OperationType.Delete)
                .build();

        when(mockSearchClient.bulk(any(BulkRequest.class))).thenReturn(new BulkResponse.Builder()
                .items(List.of(itemAdd, itemAdd2, itemDel)).errors(false).took(1L).build());

        assertThrows(RecoverableMessageException.class, () -> {
            //call under test
            mockSearchManager.documentChangeMessages(List.of(new ChangeMessage(), new ChangeMessage(), new ChangeMessage()));

        });

        verify(mockLog).error("Document {} has error {}.",
                document.getId(), errorCause);
        verify(mockLog).error("Document {} has error {}.",
                document.getId() + 1, errorCause);
    }

    @Test
    public void testDocumentChangeMessagesErrorWithOpenSearchException() throws IOException {
        document.setType(DocumentTypeNames.delete);
        OpenSearchException exception = new OpenSearchException(
                ErrorResponse.of(er -> er.error(ErrorCause.of(er1 -> er1.reason("reason").type("type")))));

        when(mockTranslator.generateSearchDocumentIfNecessary(any(ChangeMessage.class)))
                .thenReturn(Optional.of(document));

        when(mockSearchClient.bulk(any(BulkRequest.class))).thenThrow(exception);

        assertThrows(RecoverableMessageException.class, () -> {
            //call under test
            mockSearchManager.documentChangeMessages(List.of(new ChangeMessage(), new ChangeMessage()));
        });
        verify(mockLog).error(exception);
    }

    @Test
    public void testDocumentChangeMessagesErrorWithIOException() throws IOException {
        document.setType(DocumentTypeNames.delete);
        IOException exception = new IOException("IOException");
        when(mockTranslator.generateSearchDocumentIfNecessary(any(ChangeMessage.class)))
                .thenReturn(Optional.of(document));

        when(mockSearchClient.bulk(any(BulkRequest.class))).thenThrow(exception);

        assertThrows(RecoverableMessageException.class, () -> {
            //call under test
            mockSearchManager.documentChangeMessages(List.of(new ChangeMessage(), new ChangeMessage()));
        });
        verify(mockLog).error(exception);
    }

    @Test
    public void testDoesDocumentExist() throws IOException {
        when(mockSearchClient.search(any(SearchRequest.class), eq(DocumentFields.class))).thenReturn(SearchResponse.searchResponseOf(sr -> sr
                .hits(h -> h.hits(List.of(Hit.of(hit -> hit.id(document.getId()).source(document.getFields())
                        .index(SearchConstants.OPEN_SEARCH_INDEX_NAME))))).took(1).timedOut(false)
                .shards(ShardStatistics.of(sh -> sh.successful(1).failed(0).total(1)))));

        //call under test
        mockSearchManager.doesDocumentExist(document.getId(), document.getFields().getEtag());
        verify(mockSearchClient).search(searchRequestArgumentCaptor.capture(), eq(DocumentFields.class));

        SearchRequest capturedRequest = searchRequestArgumentCaptor.getValue();
        assertEquals(SearchConstants.OPEN_SEARCH_INDEX_NAME, capturedRequest.index().get(0));
    }

    @Test
    public void testDoesDocumentExistWithNoDocument() throws IOException {
        when(mockSearchClient.search(any(SearchRequest.class), eq(DocumentFields.class))).thenReturn(SearchResponse.searchResponseOf(sr -> sr
                .hits(h -> h.hits(List.of(Hit.of(hit -> hit.index(SearchConstants.OPEN_SEARCH_INDEX_NAME)
                        .id(null).source(null))))).took(1).timedOut(false)
                .shards(ShardStatistics.of(sh -> sh.successful(1).failed(0).total(1)))));

        //call under test
        mockSearchManager.doesDocumentExist(document.getId(), document.getFields().getEtag());
        verify(mockSearchClient).search(searchRequestArgumentCaptor.capture(), eq(DocumentFields.class));

        SearchRequest capturedRequest = searchRequestArgumentCaptor.getValue();
        assertEquals(SearchConstants.OPEN_SEARCH_INDEX_NAME, capturedRequest.index().get(0));
    }

    @Test
    public void testDoesDocumentExistWithOpenSearchException() throws IOException {
        OpenSearchException exception = new OpenSearchException(
                ErrorResponse.of(er -> er.error(ErrorCause.of(ec -> ec.reason("reason").type("type")))));
        when(mockSearchClient.search(any(SearchRequest.class), eq(DocumentFields.class))).thenThrow(exception);

        assertThrows(OpenSearchException.class, () -> {
            //call under test
            mockSearchManager.doesDocumentExist(document.getId(), document.getFields().getEtag());
        });
        verify(mockLog).error(exception);
    }

    @Test
    public void testDoesDocumentExistWithIOException() throws IOException {
        IOException exception = new IOException("IOException");
        when(mockSearchClient.search(any(SearchRequest.class), eq(DocumentFields.class))).thenThrow(exception);

        assertThrows(IOException.class, () -> {
            //call under test
            mockSearchManager.doesDocumentExist(document.getId(), document.getFields().getEtag());
        });
        verify(mockLog).error(exception);
    }

    @Test
    public void testSearch() throws IOException {
        SearchQuery query = new SearchQuery().setQueryTerm(List.of("hello world"));

        when(mockSearchClient.search(any(SearchRequest.class), any())).thenReturn(SearchResponse.searchResponseOf(sr -> sr
                .hits(h -> h.hits(List.of(Hit.of(hit -> hit.id(document.getId()).source(document.getFields())
                        .index(SearchConstants.OPEN_SEARCH_INDEX_NAME)))).total(TotalHits.of(th -> th.value(1L)
                        .relation(TotalHitsRelation.Eq)))).took(1).timedOut(false)
                .shards(ShardStatistics.of(sh -> sh.successful(1).failed(0).total(1)))));

        SearchResults results = mockSearchManager.search(new UserInfo(true), query);
        assertEquals(1l, results.getFound());
        assertEquals(document.getId(), results.getHits().get(0).getId());
        verify(mockSearchClient).search(searchRequestArgumentCaptor.capture(), eq(DocumentFields.class));

        SearchRequest capturedRequest = searchRequestArgumentCaptor.getValue();
        assertEquals(SearchConstants.OPEN_SEARCH_INDEX_NAME, capturedRequest.index().get(0));
        verify(mockSearchDocumentDriver, times(1)).getAliases(Collections.singletonList(document.getId()));
    }

    @Test
    public void testSearchWithPath() throws IOException {
        SearchQuery query = new SearchQuery().setQueryTerm(List.of("hello world"))
                .setReturnFields(Lists.newArrayList((SearchConstants.FIELD_PATH)));
        when(mockSearchDocumentDriver.getEntityPath(document.getId())).thenReturn(new EntityPath());
        when(mockSearchDocumentDriver.getAliases(any())).thenReturn(List.of(new IdAndAlias("id", "alias")));

        when(mockSearchClient.search(any(SearchRequest.class), any())).thenReturn(SearchResponse.searchResponseOf(sr -> sr
                .hits(h -> h.hits(List.of(Hit.of(hit -> hit.id(document.getId()).source(document.getFields())
                        .index(SearchConstants.OPEN_SEARCH_INDEX_NAME)))).total(TotalHits.of(th -> th.value(1L)
                        .relation(TotalHitsRelation.Eq)))).took(1).timedOut(false)
                .shards(ShardStatistics.of(sh -> sh.successful(1).failed(0).total(1)))));

        SearchResults results = mockSearchManager.search(new UserInfo(true), query);
        assertEquals(1l, results.getFound());
        assertEquals(document.getId(), results.getHits().get(0).getId());
        verify(mockSearchClient).search(searchRequestArgumentCaptor.capture(), eq(DocumentFields.class));
        SearchRequest capturedRequest = searchRequestArgumentCaptor.getValue();
        assertEquals(SearchConstants.OPEN_SEARCH_INDEX_NAME, capturedRequest.index().get(0));

        verify(mockSearchDocumentDriver, times(1)).getEntityPath(document.getId());
        verify(mockSearchDocumentDriver, times(1)).getAliases(Collections.singletonList(document.getId()));
    }

    @Test
    public void testSearchWithPathException() throws IOException {
        SearchQuery query = new SearchQuery().setQueryTerm(List.of("hello world"))
                .setReturnFields(Lists.newArrayList((SearchConstants.FIELD_PATH)));
        when(mockSearchDocumentDriver.getEntityPath(document.getId())).thenThrow(NotFoundException.class);

        when(mockSearchClient.search(any(SearchRequest.class), any())).thenReturn(SearchResponse.searchResponseOf(sr -> sr
                .hits(h -> h.hits(List.of(Hit.of(hit -> hit.id(document.getId()).source(document.getFields())
                        .index(SearchConstants.OPEN_SEARCH_INDEX_NAME)))).total(TotalHits.of(th -> th.value(1L)
                        .relation(TotalHitsRelation.Eq)))).took(1).timedOut(false)
                .shards(ShardStatistics.of(sh -> sh.successful(1).failed(0).total(1)))));

        //call under test
        SearchResults results = mockSearchManager.search(new UserInfo(true), query);
        assertEquals(0, results.getHits().size());

    }

    public SearchResponse<DocumentFields> createSearchResponse() {
        DocumentFields document = new DocumentFields()
                .setAcl(List.of("1", "2"))
                .setName("AnyName").setConsortium("cons").setCreated_by("me").setCreated_on(123l)
                .setModified_by("you").setModified_on(345l).setDescription("description").setDiagnosis("2")
                .setEtag("1").setNode_type("folder").setOrgan("ear").setTissue("tissue")
                .setParent_id("p_id").setUpdate_acl(List.of("234", "678"));

        return SearchResponse.searchResponseOf(res -> res.documents(document)
                .shards(shard -> shard.total(1).failed(0l).successful(1l))
                .timedOut(false).took(10).hits(hh -> hh.hits(ht -> ht.source(document).id("id")
                        .index(SearchConstants.OPEN_SEARCH_INDEX_NAME)).total(t -> t.value(1l).relation(TotalHitsRelation.valueOf("Eq")))));
    }
}
