package org.sagebionetworks.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.worker.job.tracking.JobTracker;
import org.sagebionetworks.worker.utils.WorkerProfiler;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class WorkerProfilerTest {

	@Mock
	JobTracker mockJobTracker;
	
	@Mock
	ProceedingJoinPoint mockProceedingJoinPoint;
	
	WorkerProfiler profiler;
	
	Object mockResponse;
	
	String workerName;
	
	@Before
	public void before() throws Throwable{
		profiler = new WorkerProfiler();
		ReflectionTestUtils.setField(profiler, "jobTracker", mockJobTracker);
		when(mockProceedingJoinPoint.getTarget()).thenReturn(this);
		workerName = this.getClass().getSimpleName();
		mockResponse = new Object();
		when(mockProceedingJoinPoint.proceed()).thenReturn(mockResponse);
	}
	
	@Test
	public void testProfileMessageDrivenRunner() throws Throwable{
		// call under test
		Object result = profiler.profileMessageDrivenRunner(mockProceedingJoinPoint);
		assertEquals(mockResponse, result);
		verify(mockJobTracker).jobStarted(workerName);
		verify(mockJobTracker).jobEnded(workerName);
	}
	
	@Test
	public void testProfileMessageDrivenRunnerFailure() throws Throwable{
		Exception someError = new Exception("some error");
		when(mockProceedingJoinPoint.proceed()).thenThrow(someError);
		// call under test
		try{
			profiler.profileMessageDrivenRunner(mockProceedingJoinPoint);
			fail("shoudl have failed");
		}catch (Exception e){
			// expected
			assertEquals(someError, e);
		}
		// the job should start and stop even with a failure
		verify(mockJobTracker).jobStarted(workerName);
		verify(mockJobTracker).jobEnded(workerName);
	}

}
