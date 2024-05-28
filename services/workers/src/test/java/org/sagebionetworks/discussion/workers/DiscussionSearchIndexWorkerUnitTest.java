package org.sagebionetworks.discussion.workers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.repo.manager.discussion.DiscussionSearchIndexManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

@ExtendWith(MockitoExtension.class)
public class DiscussionSearchIndexWorkerUnitTest {

	@Mock
	private WorkerLogger mockLogger;
	
	@Mock
	private DiscussionSearchIndexManager mockManager;
	
	@InjectMocks
	private DiscussionSearchIndexWorker worker;
	
	@Mock
	private ProgressCallback mockCallback;
	
	@Mock
	private ChangeMessage mockMessage;
	
	private String threadId;
	private String replyId;
	
	@BeforeEach
	public void before() {
		threadId = "123";
		replyId = "456";
	}
	
	@Test
	public void testRunWithThread() throws RecoverableMessageException, Exception {
		
		when(mockMessage.getObjectType()).thenReturn(ObjectType.THREAD);
		when(mockMessage.getObjectId()).thenReturn(threadId);
		
		// Call under test
		worker.run(mockCallback, mockMessage);
		
		verify(mockManager).processThreadChange(Long.valueOf(threadId));
		verifyNoMoreInteractions(mockLogger);
		
	}
	
	@Test
	public void testRunWithThreadAndRecoverableException() throws RecoverableMessageException, Exception {
		
		RecoverableMessageException ex = new RecoverableMessageException();
		
		when(mockMessage.getObjectType()).thenReturn(ObjectType.THREAD);
		when(mockMessage.getObjectId()).thenReturn(threadId);
		doThrow(ex).when(mockManager).processThreadChange(any());
		
		RecoverableMessageException result = assertThrows(RecoverableMessageException.class, () -> {			
			// Call under test
			worker.run(mockCallback, mockMessage);
		});
		
		assertEquals(ex, result);
		
		verify(mockManager).processThreadChange(Long.valueOf(threadId));
		verify(mockLogger).logWorkerFailure(DiscussionSearchIndexWorker.class, mockMessage, ex, true);		
	}
	
	@Test
	public void testRunWithThreadAndOtherException() throws RecoverableMessageException, Exception {
		
		RuntimeException ex = new RuntimeException();
		
		when(mockMessage.getObjectType()).thenReturn(ObjectType.THREAD);
		when(mockMessage.getObjectId()).thenReturn(threadId);
		doThrow(ex).when(mockManager).processThreadChange(any());
		
		// Call under test
		worker.run(mockCallback, mockMessage);
		
		verify(mockManager).processThreadChange(Long.valueOf(threadId));
		verify(mockLogger).logWorkerFailure(DiscussionSearchIndexWorker.class, mockMessage, ex, false);		
	}
	
	@Test
	public void testRunWithReply() throws RecoverableMessageException, Exception {
		
		when(mockMessage.getObjectType()).thenReturn(ObjectType.REPLY);
		when(mockMessage.getObjectId()).thenReturn(replyId);
		
		// Call under test
		worker.run(mockCallback, mockMessage);
		
		verify(mockManager).processReplyChange(Long.valueOf(replyId));
		verifyNoMoreInteractions(mockLogger);
		
	}
	
	@Test
	public void testRunWithOtherObject() throws RecoverableMessageException, Exception {
		
		when(mockMessage.getObjectType()).thenReturn(ObjectType.ACCESS_APPROVAL);
		when(mockMessage.getObjectId()).thenReturn(replyId);
		
		// Call under test
		worker.run(mockCallback, mockMessage);
		
		verifyNoMoreInteractions(mockManager);
		verifyNoMoreInteractions(mockLogger);
		
	}
	
	@Test
	public void testRunWithReplyAndRecoverableException() throws RecoverableMessageException, Exception {
		
		RecoverableMessageException ex = new RecoverableMessageException();
		
		when(mockMessage.getObjectType()).thenReturn(ObjectType.REPLY);
		when(mockMessage.getObjectId()).thenReturn(replyId);
		doThrow(ex).when(mockManager).processReplyChange(Long.valueOf(replyId));
		
		RecoverableMessageException result = assertThrows(RecoverableMessageException.class, () -> {			
			// Call under test
			worker.run(mockCallback, mockMessage);
		});
		
		assertEquals(ex, result);
		
		verify(mockManager).processReplyChange(Long.valueOf(replyId));
		verify(mockLogger).logWorkerFailure(DiscussionSearchIndexWorker.class, mockMessage, ex, true);		
	}
	
	@Test
	public void testRunWithReplyAndOtherException() throws RecoverableMessageException, Exception {
		
		RuntimeException ex = new RuntimeException();
		
		when(mockMessage.getObjectType()).thenReturn(ObjectType.REPLY);
		when(mockMessage.getObjectId()).thenReturn(replyId);
		doThrow(ex).when(mockManager).processReplyChange(Long.valueOf(replyId));
		
		// Call under test
		worker.run(mockCallback, mockMessage);
		
		verify(mockManager).processReplyChange(Long.valueOf(replyId));
		verify(mockLogger).logWorkerFailure(DiscussionSearchIndexWorker.class, mockMessage, ex, false);		
	}
	
	

}
