package org.sagebionetworks.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.worker.job.tracking.JobTracker;
import org.sagebionetworks.worker.utils.WorkerProfiler;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class WorkerProfilerTest {

	@Mock
	private JobTracker mockJobTracker;
	
	@Mock
	private ProceedingJoinPoint mockProceedingJoinPoint;
	
	private WorkerProfiler profiler;
	
	private Object mockResponse;
	
	private String workerName;
	
	@BeforeEach
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
		
		Exception result = assertThrows(Exception.class, () -> {
			// call under test
			profiler.profileMessageDrivenRunner(mockProceedingJoinPoint);
		});
	
		assertEquals(result, someError);
		
		// the job should start and stop even with a failure
		verify(mockJobTracker).jobStarted(workerName);
		verify(mockJobTracker).jobEnded(workerName);
	}

}
