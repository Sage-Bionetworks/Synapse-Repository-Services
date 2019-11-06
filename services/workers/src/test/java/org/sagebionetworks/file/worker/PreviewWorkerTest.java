package org.sagebionetworks.file.worker;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.EOFException;
import java.time.Instant;
import java.time.Period;
import java.util.Date;
import java.util.List;

import javax.imageio.IIOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.aws.CannotDetermineBucketLocationException;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.file.preview.PreviewGenerationNotSupportedException;
import org.sagebionetworks.repo.manager.file.preview.PreviewManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.ProxyFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.amazonaws.services.s3.internal.AmazonS3ExceptionBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.sqs.model.Message;

@ExtendWith(MockitoExtension.class)
public class PreviewWorkerTest {

	@Mock
	ProgressCallback mockProgressCallback;
	@Mock
	PreviewManager mockPreveiwManager;
	@Mock
	WorkerLogger mockWorkerLogger;

	@InjectMocks
	PreviewWorker worker;

	ChangeMessage change;

	
	@BeforeEach
	public void before(){
		change = new ChangeMessage();
		change.setObjectType(ObjectType.FILE);
		change.setObjectId("123");
		change.setChangeType(ChangeType.CREATE);
		change.setTimestamp(new Date());
	}

	@Test
	public void testNotFound() throws Exception{
		// When a file is not found the message must be returned so it can be removed from the queue
		when(mockPreveiwManager.getFileMetadata(change.getObjectId())).thenThrow(new NotFoundException());
		// Fire!
		worker.run(mockProgressCallback, change);
		verify(mockWorkerLogger, never()).logWorkerFailure(eq(PreviewWorker.class), eq(change), any(NotFoundException.class), eq(false));
	}
	
	@Test
	public void testPreviewMessage() throws Exception{
		// We do not create previews for previews.
		S3FileHandle pfm = new S3FileHandle();
		pfm.setIsPreview(true);
		when(mockPreveiwManager.getFileMetadata(change.getObjectId())).thenReturn(pfm);
		// Fire!
		worker.run(mockProgressCallback, change);
		verify(mockWorkerLogger, never()).logWorkerFailure(eq(PreviewWorker.class), eq(change), any(NotFoundException.class), eq(false));
	}
	
	@Test
	public void testNonFileMessage() throws Exception{
		// Non-file messages should be ignored and marked as processed.
		change = new ChangeMessage();
		change.setObjectType(ObjectType.ENTITY);
		change.setObjectId("123");
		// Fire!
		worker.run(mockProgressCallback, change);
		verify(mockWorkerLogger, never()).logWorkerFailure(eq(PreviewWorker.class), eq(change), any(NotFoundException.class), eq(false));
	}
	
	@Test
	public void testExternalFileMessage() throws Exception{
		// We do not create previews for previews.
		ExternalFileHandle meta = new ExternalFileHandle();
		when(mockPreveiwManager.getFileMetadata(change.getObjectId())).thenReturn(meta);
		// Fire!
		worker.run(mockProgressCallback, change);
		verify(mockWorkerLogger, never()).logWorkerFailure(eq(PreviewWorker.class), eq(change), any(NotFoundException.class), eq(false));
	}
	
	@Test
	public void testProxyFileMessage() throws Exception{
		// We do not create previews for previews.
		ProxyFileHandle meta = new ProxyFileHandle();
		when(mockPreveiwManager.getFileMetadata(change.getObjectId())).thenReturn(meta);
		// Fire!
		worker.run(mockProgressCallback, change);
		verify(mockWorkerLogger, never()).logWorkerFailure(eq(PreviewWorker.class), eq(change), any(NotFoundException.class), eq(false));
	}
	
	@Test
	public void testS3FileMetadataMessage() throws Exception{
		// We do not create previews for previews.
		S3FileHandle meta = new S3FileHandle();
		when(mockPreveiwManager.getFileMetadata(change.getObjectId())).thenReturn(meta);
		// Fire!
		worker.run(mockProgressCallback, change);
		verify(mockWorkerLogger, never()).logWorkerFailure(eq(PreviewWorker.class), eq(change), any(NotFoundException.class), eq(false));
	}
	
	@Test
	public void testUpdateMessage() throws Exception{
		change.setChangeType(ChangeType.UPDATE);
		S3FileHandle meta = new S3FileHandle();
		meta.setIsPreview(false);
		when(mockPreveiwManager.getFileMetadata(change.getObjectId())).thenReturn(meta);
		// Fire!
		worker.run(mockProgressCallback, change);
		// We should generate 
		verify(mockPreveiwManager).generatePreview(any(S3FileHandle.class));
		verify(mockWorkerLogger, never()).logWorkerFailure(eq(PreviewWorker.class), eq(change), any(NotFoundException.class), eq(false));
	}

	@Test
	public void testPreviewGenerationNotSupported() throws Exception {
		S3FileHandle meta = new S3FileHandle();
		meta.setIsPreview(false);
		when(mockPreveiwManager.getFileMetadata(change.getObjectId())).thenReturn(meta);
		PreviewGenerationNotSupportedException expectedException = new PreviewGenerationNotSupportedException("Test does not allow it!");
		when(mockPreveiwManager.generatePreview(meta)).thenThrow(expectedException);
		worker.run(mockProgressCallback, change);
		verifyZeroInteractions(mockWorkerLogger);
	}

	@Test
	public void testTemporarilyUnavailable() throws Exception{
		// When the preview manager throws a TemporarilyUnavailableException
		// that means it could not process this message right now.  Therefore,
		// the message should not be returned, so it will stay on the queue.
		S3FileHandle meta = new S3FileHandle();
		meta.setIsPreview(false);
		when(mockPreveiwManager.getFileMetadata(change.getObjectId())).thenReturn(meta);
		TemporarilyUnavailableException expectedException = new TemporarilyUnavailableException();
		when(mockPreveiwManager.generatePreview(meta)).thenThrow(expectedException);
		try {
			// Fire!
			worker.run(mockProgressCallback, change);
			fail("Should have thrown an exception");
		} catch (RecoverableMessageException e) {
			// expected
		}
		verify(mockWorkerLogger).logWorkerFailure(PreviewWorker.class, change, expectedException, true);
	}

	@Test
	public void testAmazonS3Exception_ErrorCode_NoSuchKey() throws Exception {
		S3FileHandle meta = new S3FileHandle();
		meta.setIsPreview(false);
		when(mockPreveiwManager.getFileMetadata(change.getObjectId())).thenReturn(meta);
		AmazonS3ExceptionBuilder exceptionBuilder = new AmazonS3ExceptionBuilder();
		exceptionBuilder.setErrorCode("NoSuchKey");
		when(mockPreveiwManager.generatePreview(meta)).thenThrow(exceptionBuilder.build());
		// Fire!
		worker.run(mockProgressCallback, change);
		verifyZeroInteractions(mockWorkerLogger);
	}

	@Test
	public void testAmazonS3Exception_ErrorCode_AccessDenied() throws Exception {
		S3FileHandle meta = new S3FileHandle();
		meta.setIsPreview(false);
		when(mockPreveiwManager.getFileMetadata(change.getObjectId())).thenReturn(meta);
		AmazonS3ExceptionBuilder exceptionBuilder = new AmazonS3ExceptionBuilder();
		exceptionBuilder.setErrorCode("AccessDenied");
		when(mockPreveiwManager.generatePreview(meta)).thenThrow(exceptionBuilder.build());
		// Fire!
		worker.run(mockProgressCallback, change);
		verifyZeroInteractions(mockWorkerLogger);
	}


	@Test
	public void testAmazonS3Exception_OtherErrorCodes() throws Exception {
		S3FileHandle meta = new S3FileHandle();
		meta.setIsPreview(false);
		when(mockPreveiwManager.getFileMetadata(change.getObjectId())).thenReturn(meta);
		AmazonS3ExceptionBuilder exceptionBuilder = new AmazonS3ExceptionBuilder();
		exceptionBuilder.setErrorCode("SomethingDifferent");

		AmazonS3Exception expectedException = exceptionBuilder.build();
		when(mockPreveiwManager.generatePreview(meta)).thenThrow(expectedException);
		// Fire!
		worker.run(mockProgressCallback, change);
		verify(mockWorkerLogger).logWorkerFailure(PreviewWorker.class, change, expectedException, false);
	}

	@Test
	public void testCannotDetermineBucketLocationException() throws Exception {
		S3FileHandle meta = new S3FileHandle();
		meta.setIsPreview(false);
		when(mockPreveiwManager.getFileMetadata(change.getObjectId())).thenReturn(meta);

		when(mockPreveiwManager.generatePreview(meta)).thenThrow(new CannotDetermineBucketLocationException());
		// Fire!
		worker.run(mockProgressCallback, change);
		verifyZeroInteractions(mockWorkerLogger);
	}
	
	@Test
	public void testUnknownError() throws Exception{
		// If we do not know what type of error occurred, then we assume
		// that we will be able to recover from it and therefore, the message
		// should not be returned as processed.
		S3FileHandle meta = new S3FileHandle();
		meta.setIsPreview(false);
		when(mockPreveiwManager.getFileMetadata(change.getObjectId())).thenReturn(meta);
		Exception expectedException = new Exception();
		when(mockPreveiwManager.generatePreview(meta)).thenThrow(expectedException);
		// Fire!
		worker.run(mockProgressCallback, change);
		verify(mockWorkerLogger).logWorkerFailure(PreviewWorker.class, change, expectedException, false);
	}
	
	@Test
	public void testEOFError() throws Exception {
		S3FileHandle meta = new S3FileHandle();
		meta.setIsPreview(false);
		when(mockPreveiwManager.getFileMetadata(change.getObjectId())).thenReturn(meta);
		Exception expectedException = new RuntimeException();
		expectedException.initCause(new EOFException());
		when(mockPreveiwManager.generatePreview(meta)).thenThrow(expectedException);
		// Fire!
		worker.run(mockProgressCallback, change);
		verify(mockWorkerLogger).logWorkerFailure(PreviewWorker.class, change, expectedException, false);
	}
	
	@Test
	public void testIllegalArgumentException() throws Exception{
		// We cannot recover from this type of exception so the message should be returned.
		S3FileHandle meta = new S3FileHandle();
		meta.setIsPreview(false);
		when(mockPreveiwManager.getFileMetadata(change.getObjectId())).thenReturn(meta);
		IllegalArgumentException expectedException = new IllegalArgumentException();
		when(mockPreveiwManager.generatePreview(meta)).thenThrow(expectedException);
		// Fire!
		worker.run(mockProgressCallback, change);
		verify(mockWorkerLogger).logWorkerFailure(PreviewWorker.class, change, expectedException, false);
	}
	
	@Test
	public void testErrorReadingPNG() throws Exception{
		// We cannot recover from this type of exception so the message should be returned.
		S3FileHandle meta = new S3FileHandle();
		meta.setIsPreview(false);
		when(mockPreveiwManager.getFileMetadata(change.getObjectId())).thenReturn(meta);
		IIOException causeException = new javax.imageio.IIOException("Error reading PNG image data");
		RuntimeException expectedException = new RuntimeException(causeException);
		when(mockPreveiwManager.generatePreview(meta)).thenThrow(expectedException);
		// Fire!
		worker.run(mockProgressCallback, change);
		verifyZeroInteractions(mockWorkerLogger);
	}
	
	@Test
	public void testEmptyFile() throws Exception{
		// We cannot recover from this type of exception so the message should be returned.
		S3FileHandle meta = new S3FileHandle();
		meta.setIsPreview(false);
		meta.setContentSize(0L);
		when(mockPreveiwManager.getFileMetadata(change.getObjectId())).thenReturn(meta);
		when(mockPreveiwManager.generatePreview(meta)).thenReturn(null);
		// Fire!
		worker.run(mockProgressCallback, change);
		// We should generate 
		verify(mockPreveiwManager).generatePreview(any(S3FileHandle.class));
		verify(mockWorkerLogger, never()).logWorkerFailure(eq(PreviewWorker.class), eq(change), any(NotFoundException.class), anyBoolean());
	}
	
	@Test
	public void testIgnoreDeleteMessage() throws Exception{
		// Update messages should be ignored.
		change.setChangeType(ChangeType.DELETE);
		S3FileHandle meta = new S3FileHandle();
		// Fire!
		worker.run(mockProgressCallback, change);
		// We should not generate a 
		verify(mockPreveiwManager, never()).generatePreview(any(S3FileHandle.class));
		verify(mockWorkerLogger, never()).logWorkerFailure(eq(PreviewWorker.class), eq(change), any(NotFoundException.class), eq(false));
	}
	
	@Test
	public void testIgnoreDeleteessage() throws Exception{
		// delete messages should be ignored.
		change.setChangeType(ChangeType.DELETE);
		// Fire!
		worker.run(mockProgressCallback, change);
		// We should not generate a 
		verify(mockPreveiwManager, never()).generatePreview(any(S3FileHandle.class));
		verify(mockWorkerLogger, never()).logWorkerFailure(eq(PreviewWorker.class), eq(change), any(NotFoundException.class), eq(false));
	}

	@Test
	public void testMessageOlderThanADayFromNow() throws Exception {
		//timestamps more than a day old should be ignored
		change.setTimestamp(Date.from(Instant.now().minus(Period.ofDays(1))));
		// Fire!
		worker.run(mockProgressCallback, change);
		// We should not generate a
		verify(mockPreveiwManager, never()).generatePreview(any(S3FileHandle.class));
		verify(mockWorkerLogger, never()).logWorkerFailure(eq(PreviewWorker.class), eq(change), any(NotFoundException.class), eq(false));
	}

	@Test
	public void testMessageYoungerThanADayFromNow() throws Exception {
		//timestamps more than a day old should be ignored
		change.setTimestamp(Date.from(Instant.now().minus(Period.ofDays(1)).plusSeconds(1)));
		S3FileHandle meta = new S3FileHandle();
		meta.setIsPreview(false);
		when(mockPreveiwManager.getFileMetadata(change.getObjectId())).thenReturn(meta);
		// Fire!
		worker.run(mockProgressCallback, change);
		// We should not generate a
		verify(mockPreveiwManager).generatePreview(any(S3FileHandle.class));
		verify(mockWorkerLogger, never()).logWorkerFailure(eq(PreviewWorker.class), eq(change), any(NotFoundException.class), eq(false));
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
