package org.sagebionetworks.trash.worker;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.sql.Timestamp;

import org.junit.Test;
import org.sagebionetworks.repo.manager.trash.TrashManager;

public class TrashWorkerTest {
	@Test
	public void test() throws Exception {
		TrashManager mockManager = mock(TrashManager.class);
		TrashWorker worker = new TrashWorker(mockManager);
		worker.purgeTrash();
		verify(mockManager, times(1)).getTrashBefore(any(Timestamp.class));
		verify(mockManager, times(1)).purgeTrash(anyList());
	}
}
