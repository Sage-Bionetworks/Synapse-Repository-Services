package org.sagebionetworks.change.workers;

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
	}
	
	@Test
	public void testStackDown(){
		when(mockStatusDao.getCurrentStatus()).thenReturn(StatusEnum.DOWN);
		worker.run();
		verify(mockChangeDao, never()).getMinimumChangeNumber();
		verify(mockChangeDao, never()).listUnsentMessages(anyLong(), anyLong());
	}
	
	@Test
	public void testStackReadOnly(){
		when(mockStatusDao.getCurrentStatus()).thenReturn(StatusEnum.READ_ONLY);
		worker.run();
		verify(mockChangeDao, never()).getMinimumChangeNumber();
		verify(mockChangeDao, never()).listUnsentMessages(anyLong(), anyLong());
	}

}
