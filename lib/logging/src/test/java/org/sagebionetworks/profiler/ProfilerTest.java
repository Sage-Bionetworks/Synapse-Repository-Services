package org.sagebionetworks.profiler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
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

	Profiler spyProfiler;

	@Mock
	ProfileHandler mockProfileHandler1;

	@Mock
	ProfileHandler mockProfileHandler2;

	List<ProfileHandler> profileHandlers;

	@Mock
	ThreadLocal<Stack<Frame>> mockThreadLocalStack;

	@Mock
	Stack<Frame> mockFrameStack;

	@Mock
	Frame mockFrame;

	@Before
	public void setUp(){
		spyProfiler = spy(new Profiler());
		profileHandlers = Arrays.asList(mockProfileHandler1, mockProfileHandler2);
		when(mockProfileHandler1.shouldCaptureProfile(any())).thenReturn(false);
		when(mockProfileHandler2.shouldCaptureProfile(any())).thenReturn(true);

		when(mockThreadLocalStack.get()).thenReturn(mockFrameStack);
		ReflectionTestUtils.setField(Profiler.class,"threadFrameStack", mockThreadLocalStack);
		when(mockFrameStack.peek()).thenReturn(mockFrame);
	}

	@Test
	public void testShouldCaptureData_nullHandlers(){
		spyProfiler.setHandlers(null);
		assertNull(spyProfiler.getHandlers());

		assertFalse(spyProfiler.shouldCaptureData(new Object[]{}));
	}

	@Test
	public void testShouldCaptureData_emptyHandlers(){
		spyProfiler.setHandlers(Collections.emptyList());

		assertFalse(spyProfiler.shouldCaptureData(new Object[]{}));
	}

	@Test
	public void testShouldCaptureData_multipleHandlers(){
		spyProfiler.setHandlers(profileHandlers);

		assertTrue(spyProfiler.shouldCaptureData(new Object[]{}));
	}

	@Test
	public void testDoFireProfile_nullHandlers(){
		spyProfiler.setHandlers(null);
		assertNull(spyProfiler.getHandlers());

		spyProfiler.doFireProfile(mockFrame);

		verify(mockProfileHandler1, never()).shouldCaptureProfile(any());
		verify(mockProfileHandler2, never()).shouldCaptureProfile(any());
		verify(mockProfileHandler1, never()).fireProfile(any());
		verify(mockProfileHandler2, never()).fireProfile(any());
	}

	@Test
	public void testDoFireProfile_emptyHandlers(){
		spyProfiler.setHandlers(Collections.emptyList());

		spyProfiler.doFireProfile(mockFrame);

		verify(mockProfileHandler1, never()).shouldCaptureProfile(any());
		verify(mockProfileHandler2, never()).shouldCaptureProfile(any());
		verify(mockProfileHandler1, never()).fireProfile(any());
		verify(mockProfileHandler2, never()).fireProfile(any());
	}

	@Test
	public void testDoFireProfile_multipleHandlers(){
		spyProfiler.setHandlers(profileHandlers);

		spyProfiler.doFireProfile(mockFrame);

		verify(mockProfileHandler1).shouldCaptureProfile(null);
		verify(mockProfileHandler2).shouldCaptureProfile(null);
		verify(mockProfileHandler1, never()).fireProfile(any());
		verify(mockProfileHandler2).fireProfile(mockFrame);
	}

	@Test
	public void testGetCurrentFrame__emptyStack(){
		String methodName = "asdf";
		when(mockFrameStack.isEmpty()).thenReturn(true);
		Frame currentFrame = spyProfiler.getCurrentFrame(methodName);

		assertEquals(methodName, currentFrame.getName());
	}


	@Test
	public void testGetCurrentFrame__nonEmptyStack(){
		String methodName = "asdf";

		when(mockFrame.addChildFrameIfAbsent(methodName)).thenReturn(new Frame(methodName));

		Frame currentFrame = spyProfiler.getCurrentFrame(methodName);

		assertEquals(methodName, currentFrame.getName());
		verify(mockFrame).addChildFrameIfAbsent(methodName);
	}

	@Test
	public void testDoBasicProfiling_shouldNotCaptureData() throws Throwable{
		doReturn(false).when(spyProfiler).shouldCaptureData(any());

		spyProfiler.doBasicProfiling(mockProceedingJoinPoint);

		verify(spyProfiler).doBasicProfiling(mockProceedingJoinPoint);
		verify(spyProfiler).shouldCaptureData(any());
		verify(mockProceedingJoinPoint).getArgs();
		verify(mockProceedingJoinPoint).proceed();
		verifyNoMoreInteractions(mockProceedingJoinPoint);
		verifyNoMoreInteractions(spyProfiler);
	}

	@Test
	public void testDoBasicProfiling_shouldCaptureData() throws Throwable{
		doReturn(true).when(spyProfiler).shouldCaptureData(any());
		Object target = new Object();
		String signatureName = "fakename()";
		Signature mockSignature = mock(Signature.class);
		when(mockSignature.getName()).thenReturn(signatureName);
		String expectedMethodName = target.getClass().getName() + "." + signatureName;
		when(mockProceedingJoinPoint.getTarget()).thenReturn(target);
		when(mockProceedingJoinPoint.getSignature()).thenReturn(mockSignature);
		doReturn(mockFrame).when(spyProfiler).getCurrentFrame(expectedMethodName);


		spyProfiler.doBasicProfiling(mockProceedingJoinPoint);

		verify(spyProfiler).doBasicProfiling(mockProceedingJoinPoint);
		verify(spyProfiler).shouldCaptureData(any());
		verify(mockProceedingJoinPoint).getArgs();
		verify(mockProceedingJoinPoint).getSignature();
		verify(mockProceedingJoinPoint).getTarget();
		verify(mockSignature).getName();
		verify(mockThreadLocalStack).get();
		verify(spyProfiler).getCurrentFrame(expectedMethodName);
		verify(mockFrameStack).push(mockFrame);
		verify(mockProceedingJoinPoint).proceed();
		verify(mockFrame).addElapsedTime(anyLong());
		verify(mockFrameStack).pop();
		verify(mockFrameStack).isEmpty();



		verifyNoMoreInteractions(mockProceedingJoinPoint);
		verifyNoMoreInteractions(spyProfiler);
		verifyNoMoreInteractions(mockSignature);
		verifyNoMoreInteractions(mockFrame);
		verifyNoMoreInteractions(mockThreadLocalStack);
		verifyNoMoreInteractions(mockFrameStack);
	}
}
