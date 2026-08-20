package org.sagebionetworks.grid.workers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.grid.internal.replica.export.GridReplicaCsvExporter;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.grid.DownloadFromGridRequest;
import org.sagebionetworks.repo.model.grid.DownloadFromGridResult;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.amazonaws.services.sqs.model.Message;

@ExtendWith(MockitoExtension.class)
public class GridCSVDownloadWorkerTest {
    @Mock
    private GridReplicaCsvExporter mockGridReplicaCsvExporter;
    @Mock
    private AsyncJobProgressCallback mockJobProgressCallback;

    @InjectMocks
    private GridCSVDownloadWorker worker;

    Long userId;
    UserInfo userInfo;
    DownloadFromGridRequest request;
    AsynchronousJobStatus status;
    String jobId;
    Message message;

    DownloadFromGridResult results;

    @BeforeEach
    public void before() throws Exception {
        userId = 987L;
        userInfo = new UserInfo(false, userId, AuthorizationConstants.DEFAULT_REALM_ID);

        request = new DownloadFromGridRequest();
        String sessionId = "some-session-id";
        request.setSessionId(sessionId);

        jobId = "1";
        status = new AsynchronousJobStatus();
        status.setJobId(jobId);
        status.setRequestBody(request);
        status.setStartedByUserId(userId);

        message = new Message();
        message.setBody(jobId);

        results = new DownloadFromGridResult();
        results.setSessionId(sessionId);
        String fileHandleId = "8888";
        results.setResultsFileHandleId(fileHandleId);

    }

    @Test
    public void testSuccess() throws Exception {
        when(mockGridReplicaCsvExporter.exportGridAsCsv(any(), any(), any(), any())).thenReturn(results);

        // call under test
        DownloadFromGridResult response = worker.run(jobId, userInfo, request, mockJobProgressCallback);

        assertEquals(results, response);
    }

    @Test
    public void testRecoverableMessageException() throws Exception {
        RecoverableMessageException ex = new RecoverableMessageException("recoverable");

        when(mockGridReplicaCsvExporter.exportGridAsCsv(any(), any(), any(), any())).thenThrow(ex);
        assertThrows(RecoverableMessageException.class, () -> {
            // call under test
            worker.run(jobId, userInfo, request, mockJobProgressCallback);
        }, "recoverable");
    }

    @Test
    public void testUnknownException() throws Exception {
        RuntimeException error = new RuntimeException("Bad stuff happened");
        // table not available
        when(mockGridReplicaCsvExporter.exportGridAsCsv(any(), any(), any(), any())).thenThrow(error);
        assertThrows(RuntimeException.class, () -> {
            // call under test
            worker.run(jobId, userInfo, request, mockJobProgressCallback);
        }, "Bad stuff happened");
    }

}