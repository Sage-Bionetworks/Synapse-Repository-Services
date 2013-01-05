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
import org.sagebionetworks.repo.model.file.ExternalFileMetadata;
import org.sagebionetworks.repo.model.file.PreviewFileMetadata;
import org.sagebionetworks.repo.model.file.S3FileMetadata;
import org.sagebionetworks.repo.model.message.ChangeMessage;
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
		PreviewFileMetadata pfm = new PreviewFileMetadata();
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
		ExternalFileMetadata meta = new ExternalFileMetadata();
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
		S3FileMetadata meta = new S3FileMetadata();
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
	public void testTemporarilyUnavailable() throws Exception{
		// When the preview manager throws a TemporarilyUnavailableException
		// that means it could not process this message right now.  Therefore,
		// the message should not be returned, so it will stay on the queue.
		S3FileMetadata meta = new S3FileMetadata();
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
		S3FileMetadata meta = new S3FileMetadata();
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
		S3FileMetadata meta = new S3FileMetadata();
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
		Message messageToFail = MessageUtils.createMessage(toFail, "outerId111", "handler");
		inputList.add(messageToFail);
		// This massage will be processed successfully.
		ChangeMessage toPass = new ChangeMessage();
		toPass.setObjectType(ObjectType.FILE);
		toPass.setObjectId("345");
		Message messageToPass = MessageUtils.createMessage(toPass, "outerId0000", "handler");
		inputList.add(messageToPass);
		// The first should exist.
		S3FileMetadata failMeta = new S3FileMetadata();
		failMeta.setId("123");
		S3FileMetadata passMeta = new S3FileMetadata();
		passMeta.setId("345");
		when(mockPreveiwManager.getFileMetadata(toFail.getObjectId())).thenReturn(failMeta);
		when(mockPreveiwManager.getFileMetadata(toPass.getObjectId())).thenReturn(passMeta);
		// Set one fail and one pass
		when(mockPreveiwManager.generatePreview(failMeta)).thenThrow(new Exception());
		when(mockPreveiwManager.generatePreview(passMeta)).thenReturn(new PreviewFileMetadata());
		// create the worker.
		PreviewWorker worker = new PreviewWorker(mockPreveiwManager, inputList);
		// fire!
		List<Message> processedList = worker.call();
		assertNotNull(processedList);
		assertFalse("The first messages failed with an unknown error so the message should not be returned so it can stay on the queue", isMessageOnList(processedList, messageToFail));
		assertTrue("The second message should have been processed sucuesfully and should have been returend so it can be removed from the queue.", isMessageOnList(processedList, messageToPass));
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
