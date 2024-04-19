package org.sagebionetworks.client.upload;

import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.file.CloudProviderFileHandleInterface;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;

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
		when(mockFile.exists()).thenReturn(true);
//		Path path = Path.of("foo.bin");
//		when(mockFile.toPath()).thenReturn(path);
		boolean forceRestart = false;
		// call under test
		CloudProviderFileHandleInterface result = MultithreadMultipartUpload.doUpload(mockPartCallableFactory,
				mockThreadPool, mockSynapseClient, mockFile, new MultipartUploadRequest(), forceRestart);
	}
}
