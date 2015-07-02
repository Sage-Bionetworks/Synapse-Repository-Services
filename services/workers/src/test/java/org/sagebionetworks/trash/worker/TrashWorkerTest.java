package org.sagebionetworks.trash.worker;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.sql.Timestamp;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.trash.TrashManager;
import org.sagebionetworks.repo.manager.trash.TrashManager.PurgeCallback;
import org.sagebionetworks.workers.util.progress.ProgressCallback;
import org.springframework.test.util.ReflectionTestUtils;

public class TrashWorkerTest {
	
	ProgressCallback<Void> mockProgressCallback;
	TrashManager mockManager;
	TrashWorker worker;
	
	@Before
	public void before(){
		mockManager = mock(TrashManager.class);
		mockProgressCallback = Mockito.mock(ProgressCallback.class);
		worker = new TrashWorker();
		ReflectionTestUtils.setField(worker, "trashManager", mockManager);
	}
	@Test
	public void test() throws Exception {
		worker.run(mockProgressCallback);
		verify(mockManager, times(1)).getTrashBefore(any(Timestamp.class));
		verify(mockManager, times(1)).purgeTrash(anyList(), (PurgeCallback) isNull());
	}
}
