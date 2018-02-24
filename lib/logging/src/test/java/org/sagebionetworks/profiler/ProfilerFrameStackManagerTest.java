package org.sagebionetworks.profiler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class ProfilerFrameStackManagerTest {
	@Mock
	ThreadLocal<Stack<Frame>> mockThreadLocalStack;

	@Mock
	Stack<Frame> mockFrameStack;

	@Mock
	Frame mockFrame;

	@Mock
	ProfileHandler mockProfileHandler1;

	@Mock
	ProfileHandler mockProfileHandler2;

	List<ProfileHandler> profileHandlers;

	ProfilerFrameStackManager spyProfiler; //TODO: rename

	final String methodName = "myMethodNameIsSoCool";

	long elapsedTime;

	Object[] methodArgs;
	@Before
	public void setUp(){
		elapsedTime = 1234567890;
		methodArgs = new Object[] {};
		spyProfiler = spy(new ProfilerFrameStackManager());
		profileHandlers = Arrays.asList(mockProfileHandler1, mockProfileHandler2);
		when(mockProfileHandler1.shouldCaptureProfile(methodArgs)).thenReturn(false);
		when(mockProfileHandler2.shouldCaptureProfile(methodArgs)).thenReturn(true);
		when(mockThreadLocalStack.get()).thenReturn(mockFrameStack);
		when(mockFrameStack.peek()).thenReturn(mockFrame);
		when(mockFrameStack.pop()).thenReturn(mockFrame);
		when(mockFrame.getName()).thenReturn(methodName);

		ReflectionTestUtils.setField(ProfilerFrameStackManager.class,"threadFrameStack", mockThreadLocalStack);

	}

	@After
	public void noMoreInteractionsPls(){
		verifyNoMoreInteractions(spyProfiler);
		verifyNoMoreInteractions(mockFrame);
		verifyNoMoreInteractions(mockThreadLocalStack);
		verifyNoMoreInteractions(mockFrameStack);
		verifyNoMoreInteractions(mockProfileHandler1);
		verifyNoMoreInteractions(mockProfileHandler2);
	}

	@Test
	public void testShouldCaptureData_nullHandlers(){
		spyProfiler.setHandlers(null);

		assertFalse(spyProfiler.shouldCaptureData(methodArgs));

		verify(spyProfiler).setHandlers(null);
		verify(spyProfiler).shouldCaptureData(methodArgs);
	}

	@Test
	public void testShouldCaptureData_emptyHandlers(){
		spyProfiler.setHandlers(Collections.emptyList());

		assertFalse(spyProfiler.shouldCaptureData(methodArgs));

		verify(spyProfiler).shouldCaptureData(methodArgs);
		verify(spyProfiler).setHandlers(Collections.emptyList());

	}

	@Test
	public void testShouldCaptureData_multipleHandlers(){
		spyProfiler.setHandlers(profileHandlers);

		assertTrue(spyProfiler.shouldCaptureData(methodArgs));

		verify(spyProfiler).shouldCaptureData(methodArgs);
		verify(spyProfiler).setHandlers(profileHandlers);
		verify(mockProfileHandler1).shouldCaptureProfile(methodArgs);
		verify(mockProfileHandler2).shouldCaptureProfile(methodArgs);
	}

	@Test
	public void testDoFireProfile_nullHandlers(){
		spyProfiler.setHandlers(null);

		spyProfiler.doFireProfile(mockFrame, methodArgs);

		verify(spyProfiler).doFireProfile(mockFrame, methodArgs);
		verify(spyProfiler).setHandlers(null);
		verify(mockProfileHandler1, never()).shouldCaptureProfile(any());
		verify(mockProfileHandler2, never()).shouldCaptureProfile(any());
		verify(mockProfileHandler1, never()).fireProfile(any());
		verify(mockProfileHandler2, never()).fireProfile(any());
	}

	@Test
	public void testDoFireProfile_emptyHandlers(){
		spyProfiler.setHandlers(Collections.emptyList());

		spyProfiler.doFireProfile(mockFrame, methodArgs);

		verify(spyProfiler).setHandlers(Collections.emptyList());
		verify(spyProfiler).doFireProfile(mockFrame, methodArgs);

		verify(mockProfileHandler1, never()).shouldCaptureProfile(any());
		verify(mockProfileHandler2, never()).shouldCaptureProfile(any());
		verify(mockProfileHandler1, never()).fireProfile(any());
		verify(mockProfileHandler2, never()).fireProfile(any());
	}

	@Test
	public void testDoFireProfile_multipleHandlers(){
		spyProfiler.setHandlers(profileHandlers);

		spyProfiler.doFireProfile(mockFrame, methodArgs);


		verify(spyProfiler).doFireProfile(mockFrame, methodArgs);
		verify(spyProfiler).setHandlers(profileHandlers);
		verify(mockProfileHandler1).shouldCaptureProfile(methodArgs);
		verify(mockProfileHandler2).shouldCaptureProfile(methodArgs);
		verify(mockProfileHandler1, never()).fireProfile(any());
		verify(mockProfileHandler2).fireProfile(mockFrame);
	}

	@Test
	public void testGetCurrentFrame__emptyStack(){
		String methodName = "asdf";
		when(mockFrameStack.isEmpty()).thenReturn(true);
		Frame currentFrame = spyProfiler.getCurrentFrame(methodName);

		assertEquals(methodName, currentFrame.getName());
		assertNotEquals(mockFrame, currentFrame);

		verify(mockThreadLocalStack).get();
		verify(spyProfiler).getCurrentFrame(methodName);
		verify(mockFrameStack).isEmpty();
	}


	@Test
	public void testGetCurrentFrame__nonEmptyStack(){
		String methodName = "asdf";

		when(mockFrame.addChildFrameIfAbsent(methodName)).thenReturn(new Frame(methodName));

		Frame currentFrame = spyProfiler.getCurrentFrame(methodName);

		assertEquals(methodName, currentFrame.getName());

		verify(spyProfiler).getCurrentFrame(methodName);
		verify(mockThreadLocalStack).get();
		verify(mockFrameStack).isEmpty();
		verify(mockFrameStack).peek();
		verify(mockFrame).addChildFrameIfAbsent(methodName);
	}

	@Test
	public void testStartProfiling(){
		doReturn(mockFrame).when(spyProfiler).getCurrentFrame(methodName);


		spyProfiler.startProfiling(methodName, methodArgs);

		verify(spyProfiler).startProfiling(methodName, methodArgs);
		verify(mockThreadLocalStack).get();
		verify(spyProfiler).getCurrentFrame(methodName);
		verify(mockFrameStack).push(mockFrame);
	}


	@Test
	public void testEndProfiling_methodNameNotMatchTopOfStack(){
		String unmatchingMethodName = "This is not the method name you are looking for";
		try {
			spyProfiler.endProfiling(unmatchingMethodName, methodArgs, elapsedTime);
			fail("An IllegalStateException should have been thrown");
		}catch (IllegalStateException e){
			// expected Exception
		}

		verify(spyProfiler).endProfiling(unmatchingMethodName, methodArgs, elapsedTime);
		verify(mockThreadLocalStack).get();
		verify(mockFrameStack).pop();
		verify(mockFrame, times(2)).getName();


	}

	@Test
	public void testEndProfiling_methodNameMatchesTopOfStack_FrameStackNotEmpty(){
		doNothing().when(spyProfiler).doFireProfile(mockFrame, methodArgs);
		when(mockFrameStack.isEmpty()).thenReturn(false);

		spyProfiler.endProfiling(methodName, methodArgs, elapsedTime);

		verify(spyProfiler).endProfiling(methodName, methodArgs, elapsedTime);
		verify(mockThreadLocalStack).get();
		verify(mockFrameStack).pop();
		verify(mockFrameStack).isEmpty();
		verify(mockFrame).addElapsedTime(elapsedTime);
		verify(mockFrame).getName();
		verify(spyProfiler,never()).doFireProfile(any(), any());
	}

	@Test
	public void testEndProfiling_methodNameMatchesTopOfStack_FrameStackEmpty(){
		doNothing().when(spyProfiler).doFireProfile(mockFrame, methodArgs);
		when(mockFrameStack.isEmpty()).thenReturn(true);

		spyProfiler.endProfiling(methodName, methodArgs, elapsedTime);

		verify(spyProfiler).endProfiling(methodName, methodArgs, elapsedTime);
		verify(mockThreadLocalStack).get();
		verify(mockFrameStack).pop();
		verify(mockFrameStack).isEmpty();
		verify(mockFrame).addElapsedTime(elapsedTime);
		verify(mockFrame).getName();
		verify(spyProfiler).doFireProfile(mockFrame, methodArgs);
	}


}
