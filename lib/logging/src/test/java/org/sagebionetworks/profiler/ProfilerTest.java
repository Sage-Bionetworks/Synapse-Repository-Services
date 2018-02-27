package org.sagebionetworks.profiler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class ProfilerTest {

	@Mock
	ProceedingJoinPoint mockProceedingJoinPoint;

	@Mock
	ProfilerFrameStackManager mockProfilerFrameStackManager;

	Profiler spyProfiler;

	@Before
	public void setUp(){
		Profiler profiler = new Profiler();
		ReflectionTestUtils.setField(profiler, "frameStackManager", mockProfilerFrameStackManager);

		spyProfiler = spy(profiler);
	}

	@Test
	public void testDoBasicProfiling_shouldNotCaptureData() throws Throwable{
		doReturn(false).when(mockProfilerFrameStackManager).shouldCaptureData();

		spyProfiler.doBasicProfiling(mockProceedingJoinPoint);

		verify(mockProfilerFrameStackManager).shouldCaptureData();
		verify(mockProceedingJoinPoint).proceed();

		verifyNoMoreInteractions(mockProceedingJoinPoint);
		verifyNoMoreInteractions(mockProfilerFrameStackManager);
	}

	@Test
	public void testDoBasicProfiling_shouldCaptureData() throws Throwable{
		doReturn(true).when(mockProfilerFrameStackManager).shouldCaptureData();
		Object target = new Object();
		String signatureName = "fakename()";
		Signature mockSignature = mock(Signature.class);
		when(mockSignature.getName()).thenReturn(signatureName);
		String expectedMethodName = target.getClass().getName() + "." + signatureName;
		when(mockProceedingJoinPoint.getTarget()).thenReturn(target);
		when(mockProceedingJoinPoint.getSignature()).thenReturn(mockSignature);


		spyProfiler.doBasicProfiling(mockProceedingJoinPoint);

		verify(mockProfilerFrameStackManager).shouldCaptureData();
		verify(mockProceedingJoinPoint).getSignature();
		verify(mockProceedingJoinPoint).getTarget();
		verify(mockSignature).getName();
		verify(mockProfilerFrameStackManager).startProfiling(expectedMethodName);
		verify(mockProceedingJoinPoint).proceed();
		verify(mockProfilerFrameStackManager).endProfiling(eq(expectedMethodName), anyLong());


		verifyNoMoreInteractions(mockSignature);
		verifyNoMoreInteractions(mockProceedingJoinPoint);
		verifyNoMoreInteractions(mockProfilerFrameStackManager);
	}
}
