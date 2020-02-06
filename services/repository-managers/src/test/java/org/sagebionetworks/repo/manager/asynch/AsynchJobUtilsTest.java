package org.sagebionetworks.repo.manager.asynch;


import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.AsynchJobFailedException;
import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.file.BulkFileDownloadRequest;
import org.sagebionetworks.repo.model.table.DownloadFromTableRequest;

public class AsynchJobUtilsTest {

	@Test
	public void testExtractRequestBody(){
		AsynchronousJobStatus status = new AsynchronousJobStatus();
		status.setRequestBody(new DownloadFromTableRequest());
		// call under test.
		DownloadFromTableRequest body = AsynchJobUtils.extractRequestBody(status, DownloadFromTableRequest.class);
		assertNotNull(body);
		assertEquals(status.getRequestBody(), body);
	}
	
	@Test
	public void testExtractRequestBodyNullStatus(){
		AsynchronousJobStatus status = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test.
			AsynchJobUtils.extractRequestBody(status, DownloadFromTableRequest.class);
		});
	}
	
	@Test
	public void testExtractRequestBodyNullBody(){
		AsynchronousJobStatus status = new AsynchronousJobStatus();
		status.setRequestBody(null);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test.
			AsynchJobUtils.extractRequestBody(status, DownloadFromTableRequest.class);
		});
	}
	
	@Test
	public void testExtractRequestBodyWrongType(){
		AsynchronousJobStatus status = new AsynchronousJobStatus();
		status.setRequestBody(null);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test.
			AsynchJobUtils.extractRequestBody(status, BulkFileDownloadRequest.class);
		});
	}
	
	@Test
	public void testThrowExceptionIfFailed_Complete() throws Throwable {
		AsynchronousJobStatus jobStatus = new AsynchronousJobStatus();
		jobStatus.setJobState(AsynchJobState.COMPLETE);
		// call under test
		AsynchJobUtils.throwExceptionIfFailed(jobStatus);
	}
	
	@Test
	public void testThrowExceptionIfFailed_Processing() throws Throwable {
		AsynchronousJobStatus jobStatus = new AsynchronousJobStatus();
		jobStatus.setJobState(AsynchJobState.PROCESSING);
		// call under test
		AsynchJobUtils.throwExceptionIfFailed(jobStatus);
	}
	
	@Test
	public void testThrowExceptionIfFailed_Failed() throws Throwable {
		IllegalArgumentException exception = new IllegalArgumentException("some exception");
		AsynchronousJobStatus jobStatus = new AsynchronousJobStatus();
		jobStatus.setJobState(AsynchJobState.FAILED);
		jobStatus.setException(exception.getClass().getName());
		jobStatus.setErrorDetails(exception.getMessage());
		jobStatus.setErrorMessage(exception.getMessage());
		IllegalArgumentException result = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			AsynchJobUtils.throwExceptionIfFailed(jobStatus);
		});
		assertEquals(result.getMessage(), exception.getMessage());
	}
	
	@Test
	public void testThrowExceptionIfFailed_FailedNoException() throws Throwable {
		AsynchronousJobStatus jobStatus = new AsynchronousJobStatus();
		jobStatus.setJobState(AsynchJobState.FAILED);
		jobStatus.setException(null);
		AsynchJobFailedException result = assertThrows(AsynchJobFailedException.class, ()->{
			// call under test
			AsynchJobUtils.throwExceptionIfFailed(jobStatus);
		});
		assertEquals(result.getStatus(), jobStatus);
	}
	
	@Test
	public void testThrowExceptionIfFailed_FailedUnknownException() throws Throwable {
		AsynchronousJobStatus jobStatus = new AsynchronousJobStatus();
		jobStatus.setJobState(AsynchJobState.FAILED);
		jobStatus.setException("foo.bar.NotRealException");
		AsynchJobFailedException result = assertThrows(AsynchJobFailedException.class, ()->{
			// call under test
			AsynchJobUtils.throwExceptionIfFailed(jobStatus);
		});
		assertEquals(result.getStatus(), jobStatus);
	}
}
