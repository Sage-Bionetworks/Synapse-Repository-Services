package org.sagebionetworks.repo.manager.asynch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
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
	
	@Test (expected=IllegalArgumentException.class)
	public void testExtractRequestBodyNullStatus(){
		AsynchronousJobStatus status = null;
		// call under test.
		AsynchJobUtils.extractRequestBody(status, DownloadFromTableRequest.class);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testExtractRequestBodyNullBody(){
		AsynchronousJobStatus status = new AsynchronousJobStatus();
		status.setRequestBody(null);
		// call under test.
		AsynchJobUtils.extractRequestBody(status, DownloadFromTableRequest.class);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testExtractRequestBodyWrongType(){
		AsynchronousJobStatus status = new AsynchronousJobStatus();
		status.setRequestBody(null);
		// call under test.
		AsynchJobUtils.extractRequestBody(status, BulkFileDownloadRequest.class);
	}
}
