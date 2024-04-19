package org.sagebionetworks.client.upload;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.file.CloudProviderFileHandleInterface;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadStatus;

@ExtendWith(MockitoExtension.class)
public class MultithreadMultipartUploadTest {

	@Mock
	private SynapseClient mockSynapseClient;

	@Mock
	private PartCallableFactory mockPartCallableFactory;

	@Mock
	private ExecutorService mockThreadPool;

	@Mock
	private File mockFile;

	@Test
	public void testDoUpload() throws FileNotFoundException, SynapseException, IOException {
//		File temp = File.createTempFile("testDoUpload", "txt");
//		FileUtils.writeStringToFile(temp, "Some data", StandardCharsets.UTF_8);
//		when(mockSynapseClient.startMultipartUpload(any(), anyBoolean())).thenReturn(new MultipartUploadStatus());
//		boolean forceRestart = false;
//		// call under test
//		CloudProviderFileHandleInterface result = MultithreadMultipartUpload.doUpload(mockPartCallableFactory,
//				mockThreadPool, mockSynapseClient, temp, new MultipartUploadRequest(), forceRestart);
//		
//		temp.delete();
	}
}
