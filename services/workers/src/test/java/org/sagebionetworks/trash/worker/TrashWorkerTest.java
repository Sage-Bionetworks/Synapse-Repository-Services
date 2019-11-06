package org.sagebionetworks.trash.worker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.trash.worker.TrashWorker.CUTOFF_TRASH_AGE_IN_DAYS;
import static org.sagebionetworks.trash.worker.TrashWorker.TRASH_DELETE_LIMIT;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.trash.TrashManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class TrashWorkerTest {
	
	@Mock
	ProgressCallback mockProgressCallback;
	
	@Mock
	TrashManager mockManager;
	
	TrashWorker worker;
	
	@Before
	public void before(){
		worker = new TrashWorker();
		ReflectionTestUtils.setField(worker, "trashManager", mockManager);
	}
	@Test
	public void testTrashListThrowError(){
		when(mockManager.getTrashLeavesBefore(CUTOFF_TRASH_AGE_IN_DAYS, TRASH_DELETE_LIMIT)).thenThrow(DatastoreException.class);
		worker.run(mockProgressCallback);
		verify(mockManager).getTrashLeavesBefore(CUTOFF_TRASH_AGE_IN_DAYS, TRASH_DELETE_LIMIT);
		verifyNoMoreInteractions(mockManager);
	}
	
	
	@Test
	public void testSucessful() throws Exception {
		List<Long> trashList = new LinkedList<Long>();
		when(mockManager.getTrashLeavesBefore(CUTOFF_TRASH_AGE_IN_DAYS, TRASH_DELETE_LIMIT)).thenReturn(trashList);
		worker.run(mockProgressCallback);
		verify(mockManager, times(1)).getTrashLeavesBefore(CUTOFF_TRASH_AGE_IN_DAYS, TRASH_DELETE_LIMIT);
		verify(mockManager, times(1)).purgeTrashAdmin(eq(trashList), any(UserInfo.class));
	}
	
	
}
