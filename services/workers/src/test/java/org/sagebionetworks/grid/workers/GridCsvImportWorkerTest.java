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
import org.sagebionetworks.repo.manager.grid.internal.replica.merge.GridCsvImporter;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.grid.GridCsvImportRequest;
import org.sagebionetworks.repo.model.grid.GridCsvImportResponse;
import org.sagebionetworks.util.progress.ProgressingCallable;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockType;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.sagebionetworks.workers.util.semaphore.WriteLockRequest;
import org.sagebionetworks.workers.util.semaphore.WriteReadSemaphore;

@ExtendWith(MockitoExtension.class)
public class GridCsvImportWorkerTest {

	@Mock
    private GridCsvImporter mockGridCsvImporter;
    @Mock
    private AsyncJobProgressCallback mockJobProgressCallback;
    @Mock
    private WriteReadSemaphore mockSemaphore;

    @InjectMocks
    private GridCsvImportWorker worker;

    private UserInfo userInfo;
    private String jobId;

    private GridCsvImportRequest request;
    private GridCsvImportResponse response;
    private WriteLockRequest writeLockRequest;

    @BeforeEach
    public void before() throws Exception {
        userInfo = new UserInfo(false, 123L, AuthorizationConstants.DEFAULT_REALM_ID);

        jobId = "1";

        request = new GridCsvImportRequest().setSessionId("sessionId");
        
        response = new GridCsvImportResponse();
        
        when(mockJobProgressCallback.getLockTimeoutSeconds()).thenReturn(60L);
        writeLockRequest = new WriteLockRequest(mockJobProgressCallback, "gridCsvImport-1-123", "gridCsvImport-sessionId");

        
    }

    @Test
    public void testRun() throws Exception {        
        when(mockSemaphore.tryRunWithWriteLock(eq(writeLockRequest), any())).thenAnswer(i -> {
			ProgressingCallable<GridCsvImportResponse> callable = i.getArgument(1);			
			return callable.call(mockJobProgressCallback);
		});
        when(mockGridCsvImporter.importCsv(userInfo, request, mockJobProgressCallback)).thenReturn(response);
        
        // call under test
        GridCsvImportResponse result = worker.run(jobId, userInfo, request, mockJobProgressCallback);

        assertEquals(response, result);
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
			ProgressingCallable<GridCsvImportResponse> callable = i.getArgument(1);			
			return callable.call(mockJobProgressCallback);
		});
        
        RecoverableMessageException ex = new RecoverableMessageException("recoverable");

        when(mockGridCsvImporter.importCsv(any(), any(), any())).thenThrow(ex);
       
        assertEquals(ex, assertThrows(RecoverableMessageException.class, () -> {
            // call under test
            worker.run(jobId, userInfo, request, mockJobProgressCallback);
        }));
    }

    @Test
    public void testRunWithUnknownException() throws Exception {
    	when(mockJobProgressCallback.getLockTimeoutSeconds()).thenReturn(60L);
        when(mockSemaphore.tryRunWithWriteLock(eq(writeLockRequest), any())).thenAnswer(i -> {
			ProgressingCallable<GridCsvImportResponse> callable = i.getArgument(1);			
			return callable.call(mockJobProgressCallback);
		});
        
        RuntimeException ex = new RuntimeException("Bad stuff happened");
        
        when(mockGridCsvImporter.importCsv(any(), any(), any())).thenThrow(ex);
        
        assertEquals(ex, assertThrows(RuntimeException.class, () -> {
            // call under test
            worker.run(jobId, userInfo, request, mockJobProgressCallback);
        }));
    }

}
