package org.sagebionetworks.repo.manager.grid.internal.replica.export;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.LocalFileUploadRequest;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.GridReplicaSupport;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowData;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowMetadata;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowObject;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.SynapseRow;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.GridReplicaViewManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.grid.DownloadFromGridRequest;
import org.sagebionetworks.repo.model.grid.DownloadFromGridResult;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.csv.CSVWriterProvider;

import au.com.bytecode.opencsv.CSVWriter;

@ExtendWith(MockitoExtension.class)
public class GridReplicaCsvExporterImplTest {
    @Mock
    private GridManager mockGridManager;
    @Mock
    private GridReplicaSupport gridReplicaSupport;
    @Mock
    private GridReplicaViewManager mockGridReplicaViewManager;
    @Mock
    private CSVWriterProvider mockCsvWriterProvider;
    @Mock
    private FileHandleManager mockFileHandleManager;

    @InjectMocks
    private GridReplicaCsvExporterImpl exporter;

    @Mock
    private GridSession mockGridSession;
    @Mock
    private GridConnectionInfo mockGridConnectionInfo;
    @Mock
    private GridHeader mockGridHeader;
    @Mock
    private CSVWriter mockCsvWriter;
    @Mock
    private AsyncJobProgressCallback mockJobProgressCallback;
    @Mock
    private RowViewCallbackHandler mockRowViewCallbackHandler;
    @Mock
    Iterator<RowView> mockRowViewIterator;
    @Captor
    private ArgumentCaptor<LocalFileUploadRequest> fileUploadCaptor;

    private DownloadFromGridRequest request;
    private List<RowView> rowViews;
    private String jobId = "1234";
    private String sessionId = "fakeGridSessionId";
    private String fileHandleId = "8888";
    private Long userId = 987L;
    private UserInfo userInfo;

    @BeforeEach
    public void before() {
        userInfo = new UserInfo(false);
        userInfo.setId(userId);

        request = new DownloadFromGridRequest();
        request.setSessionId(sessionId);

        rowViews = new ArrayList<>();
        rowViews.add(new RowView().setRowObject(new RowObject()
                .setMetadata(new RowMetadata().setSynapseRow(new SynapseRow().setRowId(1L).setVersionNumber(2L).setEtag("etag1")))
                .setData(new RowData().setNodes(Arrays.asList(
                        new ConstantNode().setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(100L)).setValue(new ConValue(ConType.STRING, "a")),
                        new ConstantNode().setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(101L)).setValue(new ConValue(ConType.STRING, "b"))
                )))));
        rowViews.add(new RowView().setRowObject(new RowObject()
                .setMetadata(new RowMetadata().setSynapseRow(new SynapseRow().setRowId(3L).setVersionNumber(4L).setEtag("etag2")))
                .setData(new RowData().setNodes(Arrays.asList(
                         new ConstantNode().setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(102L)).setValue(new ConValue(ConType.STRING, "c")),
                         new ConstantNode().setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(103L)).setValue(new ConValue(ConType.STRING, "d"))
                )))));
    }

    @Test
    public void testExportGridAsCsv() throws IOException {
        when(mockGridManager.getGridSession(userInfo, sessionId)).thenReturn(mockGridSession);
        when(gridReplicaSupport.getGridHeaderOrThrow(mockGridSession)).thenReturn(mockGridHeader);
        when(mockGridHeader.getOrderedColumns()).thenReturn(List.of(
                new Column().setName("col1"),
                new Column().setName("col2")
        ));
        when(mockJobProgressCallback.getJobId()).thenReturn(jobId);
        when(mockGridReplicaViewManager.getQueryIterator(eq(mockGridHeader), anyList())).thenReturn(mockRowViewIterator);
        when(mockRowViewIterator.hasNext()).thenReturn(true, true, false);
        when(mockRowViewIterator.next()).thenReturn(rowViews.get(0), rowViews.get(1));
        when(mockCsvWriterProvider.createWriter(any(), any())).thenReturn(mockCsvWriter);
        when(mockFileHandleManager.uploadLocalFile(any())).thenReturn(new S3FileHandle().setId(fileHandleId));

        // Call under test
        DownloadFromGridResult result = exporter.exportGridAsCsv(userInfo, request, mockJobProgressCallback, mockRowViewCallbackHandler);
        
        assertEquals(request.getSessionId(), result.getSessionId());
        assertEquals(fileHandleId, result.getResultsFileHandleId());
        ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
        verify(mockCsvWriter, times(3)).writeNext(captor.capture());
        List<String[]> writtenRows = captor.getAllValues();
        assertArrayEquals(
                new String[]{
                        "ROW_ID", "ROW_VERSION", "etag", "col1", "col2",
                        "1", "2", "etag1", "a", "b",
                        "3", "4", "etag2", "c", "d"
                },
                writtenRows.toArray()
        );
        verify(mockGridManager).getGridSession(userInfo, sessionId);
        rowViews.forEach(verify(mockRowViewCallbackHandler)::next);
        
        verifyFileUpload();
    }
    
    @Test
    public void testExportGridAsCsvWithNoViewHandler() throws IOException {
        when(mockGridManager.getGridSession(userInfo, sessionId)).thenReturn(mockGridSession);
        when(gridReplicaSupport.getGridHeaderOrThrow(mockGridSession)).thenReturn(mockGridHeader);
        when(mockGridHeader.getOrderedColumns()).thenReturn(List.of(
                new Column().setName("col1"),
                new Column().setName("col2")
        ));
        when(mockJobProgressCallback.getJobId()).thenReturn(jobId);
        when(mockGridReplicaViewManager.getQueryIterator(eq(mockGridHeader), anyList())).thenReturn(mockRowViewIterator);
        when(mockRowViewIterator.hasNext()).thenReturn(true, true, false);
        when(mockRowViewIterator.next()).thenReturn(rowViews.get(0), rowViews.get(1));
        when(mockCsvWriterProvider.createWriter(any(), any())).thenReturn(mockCsvWriter);
        when(mockFileHandleManager.uploadLocalFile(any())).thenReturn(new S3FileHandle().setId(fileHandleId));

        mockRowViewCallbackHandler = null;
        
        // Call under test
        DownloadFromGridResult result = exporter.exportGridAsCsv(userInfo, request, mockJobProgressCallback, mockRowViewCallbackHandler);
        
        assertEquals(request.getSessionId(), result.getSessionId());
        assertEquals(fileHandleId, result.getResultsFileHandleId());
        ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
        verify(mockCsvWriter, times(3)).writeNext(captor.capture());
        List<String[]> writtenRows = captor.getAllValues();
        assertArrayEquals(
                new String[]{
                        "ROW_ID", "ROW_VERSION", "etag", "col1", "col2",
                        "1", "2", "etag1", "a", "b",
                        "3", "4", "etag2", "c", "d"
                },
                writtenRows.toArray()
        );
        verify(mockGridManager).getGridSession(userInfo, sessionId);
        
        verifyFileUpload();
    }

    @Test
    public void testExportGridAsCsvWithNoHeader() throws IOException {
        request.setWriteHeader(false);

        when(mockGridManager.getGridSession(userInfo, sessionId)).thenReturn(mockGridSession);
        when(gridReplicaSupport.getGridHeaderOrThrow(mockGridSession)).thenReturn(mockGridHeader);
        when(mockJobProgressCallback.getJobId()).thenReturn(jobId);
        when(mockGridReplicaViewManager.getQueryIterator(eq(mockGridHeader), anyList())).thenReturn(mockRowViewIterator);
        when(mockRowViewIterator.hasNext()).thenReturn(true, true, false);
        when(mockRowViewIterator.next()).thenReturn(rowViews.get(0), rowViews.get(1));
        when(mockCsvWriterProvider.createWriter(any(), any())).thenReturn(mockCsvWriter);
        when(mockFileHandleManager.uploadLocalFile(any())).thenReturn(new S3FileHandle().setId(fileHandleId));

        // Call under test
        exporter.exportGridAsCsv(userInfo, request, mockJobProgressCallback, mockRowViewCallbackHandler);

        ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
        verify(mockCsvWriter, times(2)).writeNext(captor.capture());
        List<String[]> writtenRows = captor.getAllValues();
        assertArrayEquals(
                new String[]{
                        "1", "2", "etag1", "a", "b",
                        "3", "4", "etag2", "c", "d"
                },
                writtenRows.toArray()
        );
        
        rowViews.forEach(verify(mockRowViewCallbackHandler)::next);
        
        verifyFileUpload();
    }

    @Test
    public void testExportGridAsCsvWithoutRowIdAndVersion() throws IOException {
        request.setIncludeRowIdAndRowVersion(false);

        when(mockGridManager.getGridSession(userInfo, sessionId)).thenReturn(mockGridSession);
        when(gridReplicaSupport.getGridHeaderOrThrow(mockGridSession)).thenReturn(mockGridHeader);
        when(mockGridHeader.getOrderedColumns()).thenReturn(List.of(
                new Column().setName("col1"),
                new Column().setName("col2")
        ));
        when(mockJobProgressCallback.getJobId()).thenReturn(jobId);
        when(mockGridReplicaViewManager.getQueryIterator(eq(mockGridHeader), anyList())).thenReturn(mockRowViewIterator);
        when(mockRowViewIterator.hasNext()).thenReturn(true, true, false);
        when(mockRowViewIterator.next()).thenReturn(rowViews.get(0), rowViews.get(1));
        when(mockCsvWriterProvider.createWriter(any(), any())).thenReturn(mockCsvWriter);
        when(mockFileHandleManager.uploadLocalFile(any())).thenReturn(new S3FileHandle().setId(fileHandleId));

        // Call under test
        exporter.exportGridAsCsv(userInfo, request, mockJobProgressCallback, mockRowViewCallbackHandler);

        ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
        verify(mockCsvWriter, times(3)).writeNext(captor.capture());
        List<String[]> writtenRows = captor.getAllValues();
        assertArrayEquals(
                new String[]{
                        "etag", "col1", "col2",
                        "etag1", "a", "b",
                        "etag2", "c", "d"
                },
                writtenRows.toArray()
        );
        rowViews.forEach(verify(mockRowViewCallbackHandler)::next);
        
        verifyFileUpload();
    }

    @Test
    public void testExportGridAsCsvWithoutEtag() throws IOException {
        request.setIncludeEtag(false);

        when(mockGridManager.getGridSession(userInfo, sessionId)).thenReturn(mockGridSession);
        when(gridReplicaSupport.getGridHeaderOrThrow(mockGridSession)).thenReturn(mockGridHeader);
        when(mockGridHeader.getOrderedColumns()).thenReturn(List.of(
                new Column().setName("col1"),
                new Column().setName("col2")
        ));
        when(mockJobProgressCallback.getJobId()).thenReturn(jobId);
        when(mockGridReplicaViewManager.getQueryIterator(eq(mockGridHeader), anyList())).thenReturn(mockRowViewIterator);
        when(mockRowViewIterator.hasNext()).thenReturn(true, true, false);
        when(mockRowViewIterator.next()).thenReturn(rowViews.get(0), rowViews.get(1));
        when(mockCsvWriterProvider.createWriter(any(), any())).thenReturn(mockCsvWriter);
        when(mockFileHandleManager.uploadLocalFile(any())).thenReturn(new S3FileHandle().setId(fileHandleId));

        // Call under test
        exporter.exportGridAsCsv(userInfo, request, mockJobProgressCallback, mockRowViewCallbackHandler);

        ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
        verify(mockCsvWriter, times(3)).writeNext(captor.capture());
        List<String[]> writtenRows = captor.getAllValues();
        assertArrayEquals(
                new String[]{
                        "ROW_ID", "ROW_VERSION", "col1", "col2",
                        "1", "2", "a", "b",
                        "3", "4", "c", "d"
                },
                writtenRows.toArray()
        );
        
        rowViews.forEach(verify(mockRowViewCallbackHandler)::next);
        
        verifyFileUpload();
    }

    @Test
    public void testExportGridAsCsvWithNullOrEmptyValues() throws IOException {
        rowViews.get(0).getRowObject().getMetadata().getSynapseRow().setRowId(null).setVersionNumber(null).setEtag(null);
        rowViews.get(0).getRowObject().getData().setNodes(Arrays.asList(
                 new ConstantNode().setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(100L)).setValue(new ConValue(ConType.STRING, "a")),
                 new ConstantNode().setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(101L)).setValue(new ConValue(ConType.STRING, ""))
        ));

        when(mockGridManager.getGridSession(userInfo, sessionId)).thenReturn(mockGridSession);
        when(gridReplicaSupport.getGridHeaderOrThrow(mockGridSession)).thenReturn(mockGridHeader);
        when(mockGridHeader.getOrderedColumns()).thenReturn(List.of(
                new Column().setName("col1"),
                new Column().setName("col2")
        ));
        when(mockJobProgressCallback.getJobId()).thenReturn(jobId);
        when(mockGridReplicaViewManager.getQueryIterator(eq(mockGridHeader), anyList())).thenReturn(mockRowViewIterator);
        when(mockRowViewIterator.hasNext()).thenReturn(true, true, false);
        when(mockRowViewIterator.next()).thenReturn(rowViews.get(0), rowViews.get(1));
        when(mockCsvWriterProvider.createWriter(any(), any())).thenReturn(mockCsvWriter);
        when(mockFileHandleManager.uploadLocalFile(any())).thenReturn(new S3FileHandle().setId(fileHandleId));

        // Call under test
        exporter.exportGridAsCsv(userInfo, request, mockJobProgressCallback, mockRowViewCallbackHandler);

        ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
        verify(mockCsvWriter, times(3)).writeNext(captor.capture());
        List<String[]> writtenRows = captor.getAllValues();
        assertArrayEquals(
                new String[]{
                        "ROW_ID", "ROW_VERSION", "etag", "col1", "col2",
                        null, null, null, "a", "",
                        "3", "4", "etag2", "c", "d"
                },
                writtenRows.toArray()
        );
        
        rowViews.forEach(verify(mockRowViewCallbackHandler)::next);
        
        verifyFileUpload();
    }

    private void verifyFileUpload() {
        verify(mockFileHandleManager).uploadLocalFile(fileUploadCaptor.capture());
        LocalFileUploadRequest fileUploadRequest = fileUploadCaptor.getValue();
        assertNotNull(fileUploadRequest);
        assertEquals(userInfo.getId().toString(), fileUploadRequest.getUserId());
        assertEquals("text/csv", fileUploadRequest.getContentType());
        assertNull(fileUploadRequest.getFileName());
    }

}
