package org.sagebionetworks.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.worker.utils.StackStatusGate;
import org.springframework.test.util.ReflectionTestUtils;

public class StackStatusGateTest {
	
	StackStatusDao mockStackStatusDao;
	WorkerLogger mockWorkerLogger;
	StackStatusGate gate;

	@Before
	public void before(){
		mockStackStatusDao = Mockito.mock(StackStatusDao.class);
		mockWorkerLogger = Mockito.mock(WorkerLogger.class);
		gate = new StackStatusGate();
		ReflectionTestUtils.setField(gate, "stackStatusDao", mockStackStatusDao);
		ReflectionTestUtils.setField(gate, "workerLogger", mockWorkerLogger);
	}

	@Test
	public void testCanRunReadOnly(){
		when(mockStackStatusDao.isStackReadWrite()).thenReturn(false);
		assertFalse(gate.canRun());
	}
	
	@Test
	public void testCanRunReadWrite(){
		when(mockStackStatusDao.isStackReadWrite()).thenReturn(true);
		assertTrue(gate.canRun());
	}
	
	@Test
	public void testRunFailed(){
		Exception error = new RuntimeException("Something");
		// call under test
		gate.runFailed(error);
		verify(mockWorkerLogger).logWorkerFailure(RuntimeException.class.getName(), error, false);
	}
}
