package org.sagebionetworks.file.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.file.FileHandleUnlinkedManager;
import org.sagebionetworks.repo.model.exception.RecoverableException;
import org.sagebionetworks.repo.model.file.FileHandleUnlinkedRequest;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.amazonaws.services.sqs.model.Message;

@ExtendWith(MockitoExtension.class)
public class FileHandleUnlinkedWorkerTest {
	
	@Mock
	private ProgressCallback mockCallback;
	
	@Mock
	private Message mockMessage;
	
	@Mock
	private FileHandleUnlinkedManager mockManager;
	
	@InjectMocks
	private FileHandleUnlinkedWorker worker;
	
	@Mock
	private FileHandleUnlinkedRequest mockRequest;

	@Test
	public void testRun() throws RecoverableMessageException, Exception {
		
		when(mockManager.fromSqsMessage(any())).thenReturn(mockRequest);
				
		// Call under test
		worker.run(mockCallback, mockMessage);
		
		verify(mockManager).fromSqsMessage(mockMessage);
		verify(mockManager).processFileHandleUnlinkRequest(mockRequest);
	}
	
	@Test
	public void testRunWithRecoverableException() throws RecoverableMessageException, Exception {
		
		when(mockManager.fromSqsMessage(any())).thenReturn(mockRequest);
		
		RecoverableException ex = new RecoverableException("Recover");
		
		doThrow(ex).when(mockManager).processFileHandleUnlinkRequest(mockRequest);
				
		String message = assertThrows(RecoverableMessageException.class, () -> {			
			// Call under test
			worker.run(mockCallback, mockMessage);
		}).getMessage();
		
		assertEquals("Recover", message);
		
		verify(mockManager).fromSqsMessage(mockMessage);
		verify(mockManager).processFileHandleUnlinkRequest(mockRequest);
	}

}
