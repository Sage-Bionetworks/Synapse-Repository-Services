package org.sagebionetworks.table.worker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.table.MaterializedViewManager;

import com.amazonaws.services.sqs.model.Message;

@ExtendWith(MockitoExtension.class)
public class MaterializedViewSourceUpdateWorkerTest {
	
	@Mock
	private MaterializedViewManager mockManager;
	
	@InjectMocks
	private MaterializedViewSourceUpdateWorker worker;
	
	@Mock
	private ProgressCallback mockCallBack;
	
	@Mock
	private Message mockMessage;

	@Test
	public void testRun() throws Exception {
		
	}

}
