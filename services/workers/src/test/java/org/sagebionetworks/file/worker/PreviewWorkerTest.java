package org.sagebionetworks.file.worker;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.repo.manager.file.preview.PreviewManager;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;

import com.amazonaws.services.sqs.model.Message;

public class PreviewWorkerTest {
	
	PreviewManager mockPreveiwManager;
	List<Message> inputList;
	ChangeMessage change;
	Message message;
	
	@Before
	public void before(){
		mockPreveiwManager = Mockito.mock(PreviewManager.class);
		inputList = new LinkedList<Message>();
		change = new ChangeMessage();
		change.setObjectType(ObjectType.FILE);
		change.setObjectId("123");
		change.setChangeType(ChangeType.CREATE);
		message = MessageUtils.createMessage(change, "outerId0000", "handler");
		inputList.add(message);
	}

	@Test
	public void testNotFound() throws Exception{
		// When a file is not found the message must be returned so it can be removed from the queue
		when(mockPreveiwManager.getFileMetadata(change.getObjectId())).thenThrow(new NotFoundException());
		// Create the worker.
		PreviewWorker worker = new PreviewWorker(mockPreveiwManager, inputList);
		// Fire!
		List<Message> processedList = worker.call();
		assertNotNull(processedList);
		// This message should have been processed and returned.
		assertTrue("When a file is not found, the message should be returned as processed so it can be deleted from the queue",isMessageOnList(processedList, message));
	}
	
	@Test
	public void testPreviewMessage() throws Exception{
		// We do not create previews for previews.
		PreviewFileHandle pfm = new PreviewFileHandle();
		when(mockPreveiwManager.getFileMetadata(change.getObjectId())).thenReturn(pfm);
		// create the worker.
		PreviewWorker worker = new PreviewWorker(mockPreveiwManager, inputList);
		// fire!
		List<Message> processedList = worker.call();
		assertNotNull(processedList);
		// This message should have been processed and returned.
		assertTrue("We do not create previews of previews, so the message should be returned as processed so it can be deleted from the queue",isMessageOnList(processedList, message));
	}
	
	@Test
	public void testNonFileMessage() throws Exception{
		// Non-file messages should be ignored and marked as processed.
		inputList = new LinkedList<Message>();
		change = new ChangeMessage();
		change.setObjectType(ObjectType.ENTITY);
		change.setObjectId("123");
		message = MessageUtils.createMessage(change, "outerId0000", "handler");
		inputList.add(message);
		// create the worker.
		PreviewWorker worker = new PreviewWorker(mockPreveiwManager, inputList);
		// fire!
		List<Message> processedList = worker.call();
		assertNotNull(processedList);
		// This message should have been processed and returned.
		assertTrue("We can ingnore non-file objects, so the message should be returned as processed so it can be deleted from the queue",isMessageOnList(processedList, message));
	}
	
	@Test
	public void testExternalFileMessage() throws Exception{
		// We do not create previews for previews.
		ExternalFileHandle meta = new ExternalFileHandle();
		when(mockPreveiwManager.getFileMetadata(change.getObjectId())).thenReturn(meta);
		// create the worker.
		PreviewWorker worker = new PreviewWorker(mockPreveiwManager, inputList);
		// fire!
		List<Message> processedList = worker.call();
		assertNotNull(processedList);
		// This message should have been processed and returned.
		assertTrue("We cannot currently process External files, so the message should be returned as processed so it can be deleted from the queue",isMessageOnList(processedList, message));
	}
	
	@Test
	public void testS3FileMetadataMessage() throws Exception{
		// We do not create previews for previews.
		S3FileHandle meta = new S3FileHandle();
		when(mockPreveiwManager.getFileMetadata(change.getObjectId())).thenReturn(meta);
		// create the worker.
		PreviewWorker worker = new PreviewWorker(mockPreveiwManager, inputList);
		// fire!
		List<Message> processedList = worker.call();
		assertNotNull(processedList);
		// This message should have been processed and returned.
		assertTrue("The file should have been processed and the message should be returned as processed so it can be deleted from the queue",isMessageOnList(processedList, message));
	}
	
	@Test
	public void testUpdateMessage() throws Exception{
		change.setChangeType(ChangeType.DELETE);
		message = MessageUtils.createMessage(change, "outerId0000", "handler");
		inputList.add(message);
		S3FileHandle meta = new S3FileHandle();
		when(mockPreveiwManager.getFileMetadata(change.getObjectId())).thenReturn(meta);
		
		// create the worker.
		PreviewWorker worker = new PreviewWorker(mockPreveiwManager, inputList);
		// fire!
		List<Message> processedList = worker.call();
		assertNotNull(processedList);
		// This message should have been processed and returned.
		assertTrue("The message should be returned as processed so it can be deleted from the queue",isMessageOnList(processedList, message));
		// We should generate 
		verify(mockPreveiwManager).generatePreview(any(S3FileHandle.class));
	}
	
	@Test
	public void testTemporarilyUnavailable() throws Exception{
		// When the preview manager throws a TemporarilyUnavailableException
		// that means it could not process this message right now.  Therefore,
		// the message should not be returned, so it will stay on the queue.
		S3FileHandle meta = new S3FileHandle();
		when(mockPreveiwManager.getFileMetadata(change.getObjectId())).thenReturn(meta);
		when(mockPreveiwManager.generatePreview(meta)).thenThrow(new TemporarilyUnavailableException());
		// create the worker.
		PreviewWorker worker = new PreviewWorker(mockPreveiwManager, inputList);
		// fire!
		List<Message> processedList = worker.call();
		assertNotNull(processedList);
		// The list should be empty
		assertEquals(0, processedList.size());
	}
	
	@Test
	public void testUnknownError() throws Exception{
		// If we do not know what type of error occurred, then we assume
		// that we will be able to recover from it and therefore, the message
		// should not be returned as processed.
		S3FileHandle meta = new S3FileHandle();
		when(mockPreveiwManager.getFileMetadata(change.getObjectId())).thenReturn(meta);
		when(mockPreveiwManager.generatePreview(meta)).thenThrow(new Exception());
		// create the worker.
		PreviewWorker worker = new PreviewWorker(mockPreveiwManager, inputList);
		// fire!
		List<Message> processedList = worker.call();
		assertNotNull(processedList);
		// The list should be empty
		assertEquals(0, processedList.size());
	}
	
	@Test
	public void testIllegalArgumentException() throws Exception{
		// We cannot recover from this type of exception so the message should be returned.
		S3FileHandle meta = new S3FileHandle();
		when(mockPreveiwManager.getFileMetadata(change.getObjectId())).thenReturn(meta);
		when(mockPreveiwManager.generatePreview(meta)).thenThrow(new IllegalArgumentException());
		// create the worker.
		PreviewWorker worker = new PreviewWorker(mockPreveiwManager, inputList);
		// fire!
		List<Message> processedList = worker.call();
		assertNotNull(processedList);
		// This message should have been processed and returned.
		assertTrue("We cannot recover from this type of exception so the message should be returned as processed so it can be deleted from the queue",isMessageOnList(processedList, message));
	}
	
	@Test
	public void testMixedSuccess() throws Exception{
		// A failure to process one message should not prevent us from processing another message.
		inputList = new LinkedList<Message>();
		// This message will fail to be processed.
		ChangeMessage toFail = new ChangeMessage();
		toFail.setObjectType(ObjectType.FILE);
		toFail.setObjectId("123");
		toFail.setChangeType(ChangeType.CREATE);
		Message messageToFail = MessageUtils.createMessage(toFail, "outerId111", "handler");
		inputList.add(messageToFail);
		// This massage will be processed successfully.
		ChangeMessage toPass = new ChangeMessage();
		toPass.setObjectType(ObjectType.FILE);
		toPass.setObjectId("345");
		toPass.setChangeType(ChangeType.CREATE);
		Message messageToPass = MessageUtils.createMessage(toPass, "outerId0000", "handler");
		inputList.add(messageToPass);
		// The first should exist.
		S3FileHandle failMeta = new S3FileHandle();
		failMeta.setId("123");
		S3FileHandle passMeta = new S3FileHandle();
		passMeta.setId("345");
		when(mockPreveiwManager.getFileMetadata(toFail.getObjectId())).thenReturn(failMeta);
		when(mockPreveiwManager.getFileMetadata(toPass.getObjectId())).thenReturn(passMeta);
		// Set one fail and one pass
		when(mockPreveiwManager.generatePreview(failMeta)).thenThrow(new Exception());
		when(mockPreveiwManager.generatePreview(passMeta)).thenReturn(new PreviewFileHandle());
		// create the worker.
		PreviewWorker worker = new PreviewWorker(mockPreveiwManager, inputList);
		// fire!
		List<Message> processedList = worker.call();
		assertNotNull(processedList);
		assertFalse("The first messages failed with an unknown error so the message should not be returned so it can stay on the queue", isMessageOnList(processedList, messageToFail));
		assertTrue("The second message should have been processed successfully and should have been returned so it can be removed from the queue.", isMessageOnList(processedList, messageToPass));
	}
	
	@Test
	public void testIgnoreDeleteMessage() throws Exception{
		// Update messages should be ignored.
		inputList = new LinkedList<Message>();
		change.setChangeType(ChangeType.DELETE);
		message = MessageUtils.createMessage(change, "outerId0000", "handler");
		inputList.add(message);
		S3FileHandle meta = new S3FileHandle();
		when(mockPreveiwManager.getFileMetadata(change.getObjectId())).thenReturn(meta);
		
		// create the worker.
		PreviewWorker worker = new PreviewWorker(mockPreveiwManager, inputList);
		// fire!
		List<Message> processedList = worker.call();
		assertNotNull(processedList);
		// This message should have been processed and returned.
		assertTrue("The message should be returned as processed so it can be deleted from the queue",isMessageOnList(processedList, message));
		// We should not generate a 
		verify(mockPreveiwManager, never()).generatePreview(any(S3FileHandle.class));
	}
	
	@Test
	public void testIgnoreDeleteessage() throws Exception{
		// delete messages should be ignored.
		inputList = new LinkedList<Message>();
		change.setChangeType(ChangeType.DELETE);
		message = MessageUtils.createMessage(change, "outerId0000", "handler");
		inputList.add(message);
		S3FileHandle meta = new S3FileHandle();
		// create the worker.
		PreviewWorker worker = new PreviewWorker(mockPreveiwManager, inputList);
		// fire!
		List<Message> processedList = worker.call();
		assertNotNull(processedList);
		// This message should have been processed and returned.
		assertTrue("The message should be returned as processed so it can be deleted from the queue",isMessageOnList(processedList, message));
		// We should not generate a 
		verify(mockPreveiwManager, never()).generatePreview(any(S3FileHandle.class));
	}
	
	/**
	 * Helper to validate that the passed message was processed and on the resulting list.
	 * @param processedList
	 * @param message
	 */
	public boolean isMessageOnList(List<Message> processedList, Message message){
		for(Message m: processedList){
			if(m.equals(message)){
				return true;
			}
		}
		return false;
	}
}
