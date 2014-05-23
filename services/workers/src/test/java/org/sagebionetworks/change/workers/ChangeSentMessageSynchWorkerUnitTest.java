package org.sagebionetworks.change.workers;

import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.message.RepositoryMessagePublisher;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.springframework.test.util.ReflectionTestUtils;

public class ChangeSentMessageSynchWorkerUnitTest {
	
	DBOChangeDAO mockChangeDao;
	RepositoryMessagePublisher mockRepositoryMessagePublisher;
	ChangeSentMessageSynchWorker worker;
	int batchSize;
	
	@Before
	public void before(){
		mockChangeDao = Mockito.mock(DBOChangeDAO.class);
		mockRepositoryMessagePublisher = Mockito.mock(RepositoryMessagePublisher.class);
		worker = new ChangeSentMessageSynchWorker();
		ReflectionTestUtils.setField(worker, "changeDao", mockChangeDao);
		ReflectionTestUtils.setField(worker, "repositoryMessagePublisher", mockRepositoryMessagePublisher);
		batchSize = 10;
		worker.setBatchSize(10);
	}
	
	@Test
	public void testHighMinChange(){
		long minChangeNumber = batchSize*2;
		long currentChangeNum = minChangeNumber+batchSize*2;
		long upperBounds = minChangeNumber+batchSize;
		when(mockChangeDao.getMinimumChangeNumber()).thenReturn(minChangeNumber);
		when(mockChangeDao.getCurrentChangeNumber()).thenReturn(currentChangeNum);
		when(mockChangeDao.getLastSynchedChangeNumber()).thenReturn(-1L);
		when(mockChangeDao.getMaxSentChangeNumber(-1L)).thenReturn(-1L);
		when(mockChangeDao.getMaxSentChangeNumber(upperBounds)).thenReturn(upperBounds);
		worker.run();
		verify(mockChangeDao, times(1)).listUnsentMessages(minChangeNumber, upperBounds);
		verify(mockChangeDao, times(1)).setLastSynchedChangeNunber(-1L,  upperBounds);
	}
	
	@Test
	public void testUnderBatchSize(){
		long minChangeNumber = batchSize*2;
		long currentChangeNum = minChangeNumber+batchSize/2;
		long upperBounds = currentChangeNum;
		when(mockChangeDao.getMinimumChangeNumber()).thenReturn(minChangeNumber);
		when(mockChangeDao.getCurrentChangeNumber()).thenReturn(currentChangeNum);
		when(mockChangeDao.getLastSynchedChangeNumber()).thenReturn(-1L);
		when(mockChangeDao.getMaxSentChangeNumber(-1L)).thenReturn(-1L);
		when(mockChangeDao.getMaxSentChangeNumber(upperBounds)).thenReturn(upperBounds);
		worker.run();
		verify(mockChangeDao, times(1)).listUnsentMessages(minChangeNumber, upperBounds);
		verify(mockChangeDao, times(1)).setLastSynchedChangeNunber(-1L,  upperBounds);
	}
	
	@Test
	public void testInMiddle(){
		long minChangeNumber = batchSize*2;
		long currentChangeNum = minChangeNumber+batchSize*2;
		long lastSychedChangeNumberStart = minChangeNumber+2;
		long upperBounds = lastSychedChangeNumberStart+batchSize;
		when(mockChangeDao.getMinimumChangeNumber()).thenReturn(minChangeNumber);
		when(mockChangeDao.getCurrentChangeNumber()).thenReturn(currentChangeNum);
		when(mockChangeDao.getLastSynchedChangeNumber()).thenReturn(lastSychedChangeNumberStart);
		when(mockChangeDao.getMaxSentChangeNumber(lastSychedChangeNumberStart)).thenReturn(lastSychedChangeNumberStart);
		when(mockChangeDao.getMaxSentChangeNumber(upperBounds)).thenReturn(upperBounds);
		worker.run();
		verify(mockChangeDao, times(1)).listUnsentMessages(lastSychedChangeNumberStart, upperBounds);
		verify(mockChangeDao, times(1)).setLastSynchedChangeNunber(lastSychedChangeNumberStart,  upperBounds);
	}
	
	@Test
	public void testInMiddleLastSentMissing(){
		long minChangeNumber = batchSize*2;
		long currentChangeNum = minChangeNumber+batchSize*2;
		long lastSychedChangeNumberStart = minChangeNumber+2;
		long upperBounds = lastSychedChangeNumberStart+batchSize-1;
		when(mockChangeDao.getMinimumChangeNumber()).thenReturn(minChangeNumber);
		when(mockChangeDao.getCurrentChangeNumber()).thenReturn(currentChangeNum);
		when(mockChangeDao.getLastSynchedChangeNumber()).thenReturn(lastSychedChangeNumberStart);
		when(mockChangeDao.getMaxSentChangeNumber(lastSychedChangeNumberStart)).thenReturn(lastSychedChangeNumberStart-1);
		when(mockChangeDao.getMaxSentChangeNumber(upperBounds)).thenReturn(upperBounds);
		worker.run();
		verify(mockChangeDao, times(1)).listUnsentMessages(lastSychedChangeNumberStart-1, upperBounds);
		verify(mockChangeDao, times(1)).setLastSynchedChangeNunber(lastSychedChangeNumberStart,  upperBounds);
	}
	
	@Test
	public void testDone(){
		long minChangeNumber = batchSize*2;
		long currentChangeNum = minChangeNumber+batchSize*2;
		long lastSychedChangeNumberStart = currentChangeNum;
		long upperBounds = currentChangeNum;
		when(mockChangeDao.getMinimumChangeNumber()).thenReturn(minChangeNumber);
		when(mockChangeDao.getCurrentChangeNumber()).thenReturn(currentChangeNum);
		when(mockChangeDao.getLastSynchedChangeNumber()).thenReturn(lastSychedChangeNumberStart);
		when(mockChangeDao.getMaxSentChangeNumber(lastSychedChangeNumberStart)).thenReturn(lastSychedChangeNumberStart);
		when(mockChangeDao.getMaxSentChangeNumber(upperBounds)).thenReturn(upperBounds);
		worker.run();
		verify(mockChangeDao, times(1)).listUnsentMessages(lastSychedChangeNumberStart, upperBounds);
		verify(mockChangeDao, never()).setLastSynchedChangeNunber(anyLong(), anyLong());
	}

}
