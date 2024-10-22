package org.sagebionetworks.limits.workers;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.limits.ProjectStorageLimitManager;
import org.sagebionetworks.repo.model.limits.ProjectStorageEvent;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.amazonaws.services.sqs.model.Message;

@ExtendWith(MockitoExtension.class)
public class ProjectStorageDataRefreshWorkerTest {
	
	@Mock
	private ProjectStorageLimitManager mockManager;
	
	@InjectMocks
	private ProjectStorageDataRefreshWorker worker;

	@Mock
	private ProgressCallback mockCallback;

	@Mock
	private Message mockMessage;	
	
	@Test
	public void testRun() throws RecoverableMessageException, Exception {
		// Call under test
		worker.run(mockCallback, mockMessage, new ProjectStorageEvent().setProjectId(123L));
		
		verify(mockManager).refreshProjectStorageData(123L);
	}
}
