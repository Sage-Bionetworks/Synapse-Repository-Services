package org.sagebionetworks.worker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.util.Clock;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class IdGeneratoryCleanupWorkerTest {

	@Mock
	IdGenerator mockIdGenerator;
	@Mock
	Clock mockClock;
	@Mock
	ProgressCallback mockProgress;
	
	IdGeneratorCleanupWorker worker;
	
	@Before
	public void before() {
		worker = new IdGeneratorCleanupWorker();
		ReflectionTestUtils.setField(worker, "idGenerator", mockIdGenerator);
		ReflectionTestUtils.setField(worker, "clock", mockClock);
		
		// setup two milliseconds between calls.
		when(mockClock.currentTimeMillis()).thenReturn(0L, 2L,4L,6L);
	}
	
	@Test
	public void testCleanupType() throws InterruptedException {
		IdType type = IdType.ACCESS_APPROVAL_ID;
		// call under test
		worker.cleanupType(type);
		verify(mockIdGenerator).cleanupType(type, IdGeneratorCleanupWorker.ROW_LIMIT);
		verify(mockClock, times(2)).currentTimeMillis();
		// should sleep for twice the runtime of the cleanup
		verify(mockClock).sleep(4L);
	}
	
	
	@Test
	public void testRun() throws Exception {
		// call under test
		worker.run(mockProgress);
		// should be called for each type
		verify(mockIdGenerator, times(IdType.values().length)).cleanupType(any(IdType.class), anyLong());
	}
	
}
