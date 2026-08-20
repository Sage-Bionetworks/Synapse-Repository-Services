package org.sagebionetworks.grid.workers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.grid.internal.replica.export.GridRecordSetExporter;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.grid.GridRecordSetExportRequest;
import org.sagebionetworks.repo.model.grid.GridRecordSetExportResponse;
import org.sagebionetworks.util.progress.ProgressingCallable;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockType;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.sagebionetworks.workers.util.semaphore.WriteLockRequest;
import org.sagebionetworks.workers.util.semaphore.WriteReadSemaphore;

@ExtendWith(MockitoExtension.class)
public class GridRecordSetExportWorkerTest {
    @Mock
    private GridRecordSetExporter mockGridRecordSetExporter;
    @Mock
    private AsyncJobProgressCallback mockJobProgressCallback;
    @Mock
    private WriteReadSemaphore mockSemaphore;

    @InjectMocks
    private GridRecordSetExportWorker worker;

    private UserInfo userInfo;
    private String jobId;

    private GridRecordSetExportRequest request;
    private GridRecordSetExportResponse results;
    private WriteLockRequest writeLockRequest;

    @BeforeEach
    public void before() throws Exception {
        userInfo = new UserInfo(false, 123L, AuthorizationConstants.DEFAULT_REALM_ID);

        jobId = "1";

        request = new GridRecordSetExportRequest()
        	.setSessionId("sessionId");
        
        results = new GridRecordSetExportResponse();
        
        when(mockJobProgressCallback.getLockTimeoutSeconds()).thenReturn(60L);
        writeLockRequest = new WriteLockRequest(mockJobProgressCallback, "gridRecordSetExport-1-123", "gridRecordSetExport-sessionId");

        
    }

    @Test
    public void testRun() throws Exception {        
        when(mockSemaphore.tryRunWithWriteLock(eq(writeLockRequest), any())).thenAnswer(i -> {
			ProgressingCallable<GridRecordSetExportResponse> callable = i.getArgument(1);			
			return callable.call(mockJobProgressCallback);
		});
        when(mockGridRecordSetExporter.exportGrid(userInfo, request, mockJobProgressCallback)).thenReturn(results);
        
        // call under test
        GridRecordSetExportResponse response = worker.run(jobId, userInfo, request, mockJobProgressCallback);

        assertEquals(results, response);
    }
    
    @Test
    public void testRunWithLockUnavailableException() throws Exception {
        when(mockJobProgressCallback.getLockTimeoutSeconds()).thenReturn(60L);
        
        LockUnavilableException ex = new LockUnavilableException(LockType.Write, "key", "context");
        
        when(mockSemaphore.tryRunWithWriteLock(eq(writeLockRequest), any())).thenThrow(ex);
        
        assertEquals(ex, assertThrows(RecoverableMessageException.class, () -> {        	
        	// call under test
        	worker.run(jobId, userInfo, request, mockJobProgressCallback);
        }).getCause());
    }

    @Test
    public void testRunWithRecoverableMessageException() throws Exception {
        when(mockJobProgressCallback.getLockTimeoutSeconds()).thenReturn(60L);
        when(mockSemaphore.tryRunWithWriteLock(eq(writeLockRequest), any())).thenAnswer(i -> {
			ProgressingCallable<GridRecordSetExportResponse> callable = i.getArgument(1);			
			return callable.call(mockJobProgressCallback);
		});
        
        RecoverableMessageException ex = new RecoverableMessageException("recoverable");

        when(mockGridRecordSetExporter.exportGrid(any(), any(), any())).thenThrow(ex);
       
        assertEquals(ex, assertThrows(RecoverableMessageException.class, () -> {
            // call under test
            worker.run(jobId, userInfo, request, mockJobProgressCallback);
        }));
    }

    @Test
    public void testRunWithUnknownException() throws Exception {
    	when(mockJobProgressCallback.getLockTimeoutSeconds()).thenReturn(60L);
        when(mockSemaphore.tryRunWithWriteLock(eq(writeLockRequest), any())).thenAnswer(i -> {
			ProgressingCallable<GridRecordSetExportResponse> callable = i.getArgument(1);			
			return callable.call(mockJobProgressCallback);
		});
        
        RuntimeException ex = new RuntimeException("Bad stuff happened");
        
        when(mockGridRecordSetExporter.exportGrid(any(), any(), any())).thenThrow(ex);
        
        assertEquals(ex, assertThrows(RuntimeException.class, () -> {
            // call under test
            worker.run(jobId, userInfo, request, mockJobProgressCallback);
        }));
    }

}