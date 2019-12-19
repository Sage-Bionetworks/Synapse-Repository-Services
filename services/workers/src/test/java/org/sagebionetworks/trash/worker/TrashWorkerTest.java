package org.sagebionetworks.trash.worker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.trash.worker.TrashWorker.CUTOFF_TRASH_AGE_IN_DAYS;
import static org.sagebionetworks.trash.worker.TrashWorker.TRASH_DELETE_LIMIT;

import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.trash.TrashManager;
import org.sagebionetworks.repo.model.DatastoreException;

import com.google.common.collect.ImmutableList;

@ExtendWith(MockitoExtension.class)
public class TrashWorkerTest {
	
	@Mock
	private ProgressCallback mockProgressCallback;
	
	@Mock
	private TrashManager mockManager;
	
	@InjectMocks
	private TrashWorker worker;
	
	@Test
	public void testTrashListThrowError(){
		when(mockManager.getTrashLeavesBefore(CUTOFF_TRASH_AGE_IN_DAYS, TRASH_DELETE_LIMIT)).thenThrow(DatastoreException.class);
		worker.run(mockProgressCallback);
		verify(mockManager).getTrashLeavesBefore(CUTOFF_TRASH_AGE_IN_DAYS, TRASH_DELETE_LIMIT);
		verifyNoMoreInteractions(mockManager);
	}
	
	
	@Test
	public void testEmptyList() throws Exception {
		List<Long> trashList = new LinkedList<Long>();
		when(mockManager.getTrashLeavesBefore(CUTOFF_TRASH_AGE_IN_DAYS, TRASH_DELETE_LIMIT)).thenReturn(trashList);
		worker.run(mockProgressCallback);
		verify(mockManager).getTrashLeavesBefore(CUTOFF_TRASH_AGE_IN_DAYS, TRASH_DELETE_LIMIT);
		verifyNoMoreInteractions(mockManager);
	}
	
	@Test
	public void testSucessful() throws Exception {
		List<Long> trashList = ImmutableList.of(1L, 2L, 3L, 4L, 5L);
		when(mockManager.getTrashLeavesBefore(CUTOFF_TRASH_AGE_IN_DAYS, TRASH_DELETE_LIMIT)).thenReturn(trashList);
		worker.run(mockProgressCallback);
		verify(mockManager).getTrashLeavesBefore(CUTOFF_TRASH_AGE_IN_DAYS, TRASH_DELETE_LIMIT);
		verify(mockManager).purgeTrashAdmin(eq(trashList), any());
	}
	
	
}
