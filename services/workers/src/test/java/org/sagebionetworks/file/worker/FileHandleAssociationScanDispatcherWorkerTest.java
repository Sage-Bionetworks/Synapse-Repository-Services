package org.sagebionetworks.file.worker;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.file.FileHandleAssociationScannerJobManager;

@ExtendWith(MockitoExtension.class)
public class FileHandleAssociationScanDispatcherWorkerTest {
	
	@Mock
	private FileHandleAssociationScannerJobManager mockManager;
	
	@InjectMocks
	private FileHandleAssociationScanDispatcherWorker worker;
	
	@Mock
	private ProgressCallback mockCallback;

	@Test
	public void testRunWithIdle() throws Exception {
		
		when(mockManager.isScanJobIdle(anyInt())).thenReturn(true);
		
		// Call under test
		worker.run(mockCallback);
		
		verify(mockManager).isScanJobIdle(FileHandleAssociationScanDispatcherWorker.START_INTERVAL_DAYS);
		verify(mockManager).startScanJob();
	}
	
	@Test
	public void testRunWithBusy() throws Exception {
		
		when(mockManager.isScanJobIdle(anyInt())).thenReturn(false);
		
		// Call under test
		worker.run(mockCallback);
		
		verify(mockManager).isScanJobIdle(FileHandleAssociationScanDispatcherWorker.START_INTERVAL_DAYS);
		verifyNoMoreInteractions(mockManager);
	}

}
