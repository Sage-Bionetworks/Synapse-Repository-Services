package org.sagebionetworks.change.workers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.message.RepositoryMessagePublisher;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.util.TestClock;
import org.springframework.test.util.ReflectionTestUtils;

public class ChangeSentMessageSynchWorkerUnitTest {
	
	DBOChangeDAO mockChangeDao;
	RepositoryMessagePublisher mockRepositoryMessagePublisher;
	StackStatusDao mockStatusDao;
	ChangeSentMessageSynchWorker worker;
	ProgressCallback<Void> mockCallback;
	StackConfiguration mockConfiguration;
	WorkerLogger mockLogger;
	Random mockRandom;
	int pageSize = 10;
	TestClock testClock;
	ChangeMessage one;
	ChangeMessage two;
	
	@Before
	public void before(){
		mockChangeDao = Mockito.mock(DBOChangeDAO.class);
		mockRepositoryMessagePublisher = Mockito.mock(RepositoryMessagePublisher.class);
		mockStatusDao = Mockito.mock(StackStatusDao.class);
		mockCallback = Mockito.mock(ProgressCallback.class);
		mockConfiguration = Mockito.mock(StackConfiguration.class);
		mockLogger = Mockito.mock(WorkerLogger.class);
		mockRandom = Mockito.mock(Random.class);
		testClock = new TestClock();
		worker = new ChangeSentMessageSynchWorker();
		ReflectionTestUtils.setField(worker, "changeDao", mockChangeDao);
		ReflectionTestUtils.setField(worker, "repositoryMessagePublisher", mockRepositoryMessagePublisher);
		ReflectionTestUtils.setField(worker, "stackStatusDao", mockStatusDao);
		ReflectionTestUtils.setField(worker, "clock", testClock);
		ReflectionTestUtils.setField(worker, "configuration", mockConfiguration);
		ReflectionTestUtils.setField(worker, "workerLogger", mockLogger);
		ReflectionTestUtils.setField(worker, "random", mockRandom);
		when(mockStatusDao.isStackReadWrite()).thenReturn(true);
		when(mockConfiguration.getChangeSynchWorkerMinPageSize()).thenReturn(pageSize);
		when(mockConfiguration.getChangeSynchWorkerSleepTimeMS()).thenReturn(1000L);
		when(mockRandom.nextInt(anyInt())).thenReturn(1);
		
		one = new ChangeMessage();
		one.setObjectType(ObjectType.ENTITY);
		one.setObjectId("one");
		two = new ChangeMessage();
		two.setObjectType(ObjectType.FILE);
		two.setObjectId("two");
		when(mockChangeDao.listUnsentMessages(anyLong(), anyLong(), any(Timestamp.class))).thenReturn(Arrays.asList(one, two));
	}

	
	@Test
	public void testStackNotReadWrite() throws Exception{
		when(mockStatusDao.isStackReadWrite()).thenReturn(false);
		worker.run(mockCallback);
		verify(mockChangeDao, never()).getMinimumChangeNumber();
		verify(mockChangeDao, never()).listUnsentMessages(anyLong(), anyLong(),  any(Timestamp.class));
	}
	
	@Test
	public void testHappy() throws Exception{
		long max = pageSize*2+3;
		long min = 1;
		when(mockChangeDao.getCurrentChangeNumber()).thenReturn(max);
		when(mockChangeDao.getMinimumChangeNumber()).thenReturn(min);
		when(mockChangeDao.checkUnsentMessageByCheckSumForRange(1L, 11L)).thenReturn(true);
		when(mockChangeDao.checkUnsentMessageByCheckSumForRange(12L, 22L)).thenReturn(true);
		when(mockChangeDao.checkUnsentMessageByCheckSumForRange(23L, 33L)).thenReturn(false);
		// run
		long start = testClock.currentTimeMillis();
		worker.run(mockCallback);
		// Progress should be made for each page.
		verify(mockCallback, times(5)).progressMade(null);
		verify(mockRepositoryMessagePublisher).publishBatchToTopic(ObjectType.ENTITY, Arrays.asList(one));
		verify(mockRepositoryMessagePublisher).publishBatchToTopic(ObjectType.FILE, Arrays.asList(two));
		verify(mockLogger, times(10)).logCustomMetric(any(ProfileData.class));
		assertEquals(start + 3000, testClock.currentTimeMillis());
	}

}
