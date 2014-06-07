package org.sagebionetworks.change.workers;

import java.sql.Timestamp;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.message.RepositoryMessagePublisher;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.springframework.test.util.ReflectionTestUtils;

public class ChangeSentMessageSynchWorkerUnitTest {
	
	DBOChangeDAO mockChangeDao;
	RepositoryMessagePublisher mockRepositoryMessagePublisher;
	StackStatusDao mockStatusDao;
	ChangeSentMessageSynchWorker worker;
	int batchSize;
	
	@Before
	public void before(){
		mockChangeDao = Mockito.mock(DBOChangeDAO.class);
		mockRepositoryMessagePublisher = Mockito.mock(RepositoryMessagePublisher.class);
		mockStatusDao = Mockito.mock(StackStatusDao.class);
		worker = new ChangeSentMessageSynchWorker();
		ReflectionTestUtils.setField(worker, "changeDao", mockChangeDao);
		ReflectionTestUtils.setField(worker, "repositoryMessagePublisher", mockRepositoryMessagePublisher);
		ReflectionTestUtils.setField(worker, "stackStatusDao", mockStatusDao);
		when(mockStatusDao.getCurrentStatus()).thenReturn(StatusEnum.READ_WRITE);
		batchSize = 10;
		worker.setBatchSize(10);
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
		verify(mockChangeDao, never()).listUnsentMessages(anyLong(), anyLong(), any(Timestamp.class));
		verify(mockChangeDao, never()).setLastSynchedChangeNunber(anyLong(), anyLong());
	}
	
	@Test
	public void testStackDown(){
		when(mockStatusDao.getCurrentStatus()).thenReturn(StatusEnum.DOWN);
		worker.run();
		verify(mockChangeDao, never()).getMinimumChangeNumber();
		verify(mockChangeDao, never()).listUnsentMessages(anyLong(), anyLong(), any(Timestamp.class));
		verify(mockChangeDao, never()).setLastSynchedChangeNunber(anyLong(), anyLong());
	}
	
	@Test
	public void testStackReadOnly(){
		when(mockStatusDao.getCurrentStatus()).thenReturn(StatusEnum.READ_ONLY);
		worker.run();
		verify(mockChangeDao, never()).getMinimumChangeNumber();
		verify(mockChangeDao, never()).listUnsentMessages(anyLong(), anyLong(),  any(Timestamp.class));
		verify(mockChangeDao, never()).setLastSynchedChangeNunber(anyLong(), anyLong());
	}

}
