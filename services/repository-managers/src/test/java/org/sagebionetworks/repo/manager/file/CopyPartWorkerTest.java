package org.sagebionetworks.repo.manager.file;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.file.ChunkedFileToken;

public class CopyPartWorkerTest {
	
	MultipartManager mockManger;
	ChunkedFileToken token;
	int partNumber;
	Long storageId;
			
	@Before
	public void before(){
		mockManger = Mockito.mock(MultipartManager.class);
		storageId = 11L;
		token = new ChunkedFileToken();
		token.setKey("key");
		token.setUploadId("uploadId");
		token.setStorageLocationId(storageId);
		partNumber = 1;
	}
	
	@Test (expected=RuntimeException.class)
	public void testTimeout() throws Exception{
		// This should trigger a timeout.
		when(mockManger.doesPartExist(token, partNumber, storageId)).thenReturn(false);
		CopyPartWorker cpw = new CopyPartWorker(mockManger, token, 1, 100);
		cpw.call();
	}
	
	@Test
	public void testHappyCase() throws Exception{
		when(mockManger.doesPartExist(token, partNumber, storageId)).thenReturn(true);
		CopyPartWorker cpw = new CopyPartWorker(mockManger, token, 1, 100);
		cpw.call();
		verify(mockManger).copyPart(token, partNumber, storageId);
	}
}
