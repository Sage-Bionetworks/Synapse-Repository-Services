package org.sagebionetworks.change.workers;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

import org.mockito.Mockito;
import org.sagebionetworks.ImmutablePropertyAccessor;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.repo.manager.message.RepositoryMessagePublisher;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.dbo.dao.semaphore.ProgressCallback;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.util.DefaultClockProvider;
import org.springframework.test.util.ReflectionTestUtils;

public class ChangeSentMessageSynchWorkerUnitTest {
	
	DBOChangeDAO mockChangeDao;
	RepositoryMessagePublisher mockRepositoryMessagePublisher;
	StackStatusDao mockStatusDao;
	ChangeSentMessageSynchWorker worker;
	ProgressCallback mockCallback;
	StackConfiguration mockConfiguration;
	WorkerLogger mockLogger;
	Random mockRandom;
	int pageSize = 10;
	
	@Before
	public void before(){
		mockChangeDao = Mockito.mock(DBOChangeDAO.class);
		mockRepositoryMessagePublisher = Mockito.mock(RepositoryMessagePublisher.class);
		mockStatusDao = Mockito.mock(StackStatusDao.class);
		mockCallback = Mockito.mock(ProgressCallback.class);
		mockConfiguration = Mockito.mock(StackConfiguration.class);
		mockLogger = Mockito.mock(WorkerLogger.class);
		mockRandom = Mockito.mock(Random.class);
		worker = new ChangeSentMessageSynchWorker();
		ReflectionTestUtils.setField(worker, "changeDao", mockChangeDao);
		ReflectionTestUtils.setField(worker, "repositoryMessagePublisher", mockRepositoryMessagePublisher);
		ReflectionTestUtils.setField(worker, "stackStatusDao", mockStatusDao);
		ReflectionTestUtils.setField(worker, "clockProvider", new DefaultClockProvider());
		ReflectionTestUtils.setField(worker, "configuration", mockConfiguration);
		ReflectionTestUtils.setField(worker, "workerLogger", mockLogger);
		ReflectionTestUtils.setField(worker, "random", mockRandom);
		when(mockStatusDao.getCurrentStatus()).thenReturn(StatusEnum.READ_WRITE);
		when(mockConfiguration.getChangeSynchWorkerMinPageSize()).thenReturn(new ImmutablePropertyAccessor(pageSize));
		when(mockConfiguration.getChangeSynchWorkerSleepTimeMS()).thenReturn(new ImmutablePropertyAccessor(0L));
		when(mockRandom.nextInt(anyInt())).thenReturn(1);
	}
	
	
	@Test
	public void testStackDown(){
		when(mockStatusDao.getCurrentStatus()).thenReturn(StatusEnum.DOWN);
		worker.run(mockCallback);
		verify(mockChangeDao, never()).getMinimumChangeNumber();
		verify(mockChangeDao, never()).listUnsentMessages(anyLong(), anyLong(), any(Timestamp.class));
	}
	
	@Test
	public void testStackReadOnly(){
		when(mockStatusDao.getCurrentStatus()).thenReturn(StatusEnum.READ_ONLY);
		worker.run(mockCallback);
		verify(mockChangeDao, never()).getMinimumChangeNumber();
		verify(mockChangeDao, never()).listUnsentMessages(anyLong(), anyLong(),  any(Timestamp.class));
	}
	
	@Test
	public void testHappy(){
		long max = pageSize*2+3;
		long min = 1;
		when(mockChangeDao.getCurrentChangeNumber()).thenReturn(max);
		when(mockChangeDao.getMinimumChangeNumber()).thenReturn(min);
		when(mockChangeDao.checkUnsentMessageByCheckSumForRange(1L, 11L)).thenReturn(true);
		when(mockChangeDao.checkUnsentMessageByCheckSumForRange(12L, 22L)).thenReturn(true);
		when(mockChangeDao.checkUnsentMessageByCheckSumForRange(23L, 33L)).thenReturn(false);
		when(mockChangeDao.listUnsentMessages(anyLong(), anyLong(), any(Timestamp.class))).thenReturn(Arrays.asList(new ChangeMessage(), new ChangeMessage()));
		// run
		worker.run(mockCallback);
		// Progress should be made for each page.
		verify(mockCallback, times(5)).progressMade();
		verify(mockRepositoryMessagePublisher, times(2)).publishToTopic(any(ChangeMessage.class));
		verify(mockLogger, times(9)).logCustomMetric(any(ProfileData.class));
	}

}
