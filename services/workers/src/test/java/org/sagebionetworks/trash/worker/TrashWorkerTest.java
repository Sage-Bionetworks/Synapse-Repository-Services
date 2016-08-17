package org.sagebionetworks.trash.worker;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;

import static org.sagebionetworks.trash.worker.TrashWorker.MAX_TRASH_ITEMS;
import static org.sagebionetworks.trash.worker.TrashWorker.TRASH_AGE_IN_DAYS;

import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.trash.TrashManager;
import org.sagebionetworks.repo.manager.trash.TrashManager.PurgeCallback;
import org.sagebionetworks.repo.model.UserInfo;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class TrashWorkerTest {
	
	@Mock
	ProgressCallback<Void> mockProgressCallback;
	
	@Mock
	TrashManager mockManager;
	
	TrashWorker worker;
	
	@Before
	public void before(){
		worker = new TrashWorker();
		ReflectionTestUtils.setField(worker, "trashManager", mockManager);
	}
	@Test
	public void test() throws Exception {
		List<Long> trashList = new LinkedList<Long>();
		when(mockManager.getTrashLeavesBefore(TRASH_AGE_IN_DAYS, MAX_TRASH_ITEMS)).thenReturn(trashList);
		worker.run(mockProgressCallback);
		verify(mockManager, times(1)).getTrashLeavesBefore(TRASH_AGE_IN_DAYS, MAX_TRASH_ITEMS);
		verify(mockManager, times(1)).purgeTrashAdmin(eq(trashList), any(UserInfo.class));
	}
}
