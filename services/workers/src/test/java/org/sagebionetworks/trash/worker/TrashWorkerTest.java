package org.sagebionetworks.trash.worker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.trash.worker.TrashWorker.CUTOFF_TRASH_AGE_IN_DAYS;
import static org.sagebionetworks.trash.worker.TrashWorker.TRASH_BATCH_SIZE;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.trash.TrashManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.StackStatusDao;

import com.google.common.collect.ImmutableList;

@ExtendWith(MockitoExtension.class)
public class TrashWorkerTest {
	
	@Mock
	private ProgressCallback mockProgressCallback;
	
	@Mock
	private TrashManager mockManager;
	
	@Mock
	private WorkerLogger mockWorkerLogger;
	
	@Mock
	private StackStatusDao mockStackStatusDao;
	
	@InjectMocks
	private TrashWorker worker;
	
	@Test
	public void testTrashListThrowError(){
		when(mockManager.getTrashLeavesBefore(CUTOFF_TRASH_AGE_IN_DAYS, TRASH_BATCH_SIZE)).thenThrow(DatastoreException.class);
		
		// Call under test
		worker.run(mockProgressCallback);
		verify(mockManager).getTrashLeavesBefore(CUTOFF_TRASH_AGE_IN_DAYS, TRASH_BATCH_SIZE);
		verifyNoMoreInteractions(mockManager);
	}
	
	
	@Test
	public void testEmptyList() throws Exception {
		List<Long> trashList = new LinkedList<Long>();
		when(mockManager.getTrashLeavesBefore(CUTOFF_TRASH_AGE_IN_DAYS, TRASH_BATCH_SIZE)).thenReturn(trashList);
		
		// Call under test
		worker.run(mockProgressCallback);
		verify(mockManager).getTrashLeavesBefore(CUTOFF_TRASH_AGE_IN_DAYS, TRASH_BATCH_SIZE);
		verifyNoMoreInteractions(mockManager);
	}
	
	@Test
	public void testSucessful() throws Exception {
		List<Long> trashList = ImmutableList.of(1L, 2L, 3L, 4L, 5L);
		when(mockManager.getTrashLeavesBefore(CUTOFF_TRASH_AGE_IN_DAYS, TRASH_BATCH_SIZE)).thenReturn(trashList);
		when(mockStackStatusDao.isStackReadWrite()).thenReturn(true);
		
		// Call under test
		worker.run(mockProgressCallback);
		verify(mockManager).getTrashLeavesBefore(CUTOFF_TRASH_AGE_IN_DAYS, TRASH_BATCH_SIZE);
		
		trashList.forEach(id -> {
			verify(mockManager).purgeTrash(any(), eq(Collections.singletonList(id)));
		});
	}
	
	@Test
	public void testContinueWithException() throws Exception {
		List<Long> trashList = ImmutableList.of(1L, 2L, 3L, 4L, 5L);
		when(mockManager.getTrashLeavesBefore(CUTOFF_TRASH_AGE_IN_DAYS, TRASH_BATCH_SIZE)).thenReturn(trashList);
		when(mockStackStatusDao.isStackReadWrite()).thenReturn(true);
		
		IllegalStateException ex = new IllegalStateException();
		
		List<Long> exIds = trashList.subList(1, 2);
		
		doThrow(ex).when(mockManager).purgeTrash(any(), eq(exIds));
		
		// Call under test
		worker.run(mockProgressCallback);
		
		verify(mockManager).getTrashLeavesBefore(CUTOFF_TRASH_AGE_IN_DAYS, TRASH_BATCH_SIZE);
		
		trashList.forEach(id -> {
			verify(mockManager).purgeTrash(any(), eq(Collections.singletonList(id)));
		});
	}
	
	@Test
	public void testWithUnexpectedException() throws Exception {
		
		IllegalStateException ex = new IllegalStateException();
		
		doThrow(ex).when(mockManager).getTrashLeavesBefore(CUTOFF_TRASH_AGE_IN_DAYS, TRASH_BATCH_SIZE);
		
		// Call under test
		worker.run(mockProgressCallback);
		
		verify(mockManager).getTrashLeavesBefore(CUTOFF_TRASH_AGE_IN_DAYS, TRASH_BATCH_SIZE);
		
		boolean willRetry = false;
		
		verify(mockWorkerLogger).logWorkerFailure(TrashWorker.class.getName(), ex, willRetry);
	}
	
	
}
