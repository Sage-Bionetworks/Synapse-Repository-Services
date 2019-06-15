package org.sagebionetworks.profiler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

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

	ProfilerFrameStackManager spyProfilerFrameStackManager;

	final String methodName = "myMethodNameIsSoCool";

	long elapsedTime;

	@Before
	public void setUp(){
		elapsedTime = 1234567890;
		spyProfilerFrameStackManager = spy(new ProfilerFrameStackManager());
		profileHandlers = Arrays.asList(mockProfileHandler1, mockProfileHandler2);
		when(mockProfileHandler1.shouldCaptureProfile()).thenReturn(false);
		when(mockProfileHandler2.shouldCaptureProfile()).thenReturn(true);
		when(mockThreadLocalStack.get()).thenReturn(mockFrameStack);
		when(mockFrameStack.peek()).thenReturn(mockFrame);
		when(mockFrameStack.pop()).thenReturn(mockFrame);
		when(mockFrame.getName()).thenReturn(methodName);

		ReflectionTestUtils.setField(ProfilerFrameStackManager.class,"threadFrameStack", mockThreadLocalStack);

	}

	@Test
	public void testShouldCaptureData_nullHandlers(){
		spyProfilerFrameStackManager.setHandlers(null);

		assertFalse(spyProfilerFrameStackManager.shouldCaptureData());
	}

	@Test
	public void testShouldCaptureData_emptyHandlers(){
		spyProfilerFrameStackManager.setHandlers(Collections.emptyList());

		assertFalse(spyProfilerFrameStackManager.shouldCaptureData());
	}

	@Test
	public void testShouldCaptureData_multipleHandlers(){
		spyProfilerFrameStackManager.setHandlers(profileHandlers);

		assertTrue(spyProfilerFrameStackManager.shouldCaptureData());

		verify(mockProfileHandler1).shouldCaptureProfile();
		verify(mockProfileHandler2).shouldCaptureProfile();
	}

	@Test
	public void testDoFireProfile_nullHandlers(){
		spyProfilerFrameStackManager.setHandlers(null);

		spyProfilerFrameStackManager.doFireProfile(mockFrame);

		verify(mockProfileHandler1, never()).shouldCaptureProfile();
		verify(mockProfileHandler2, never()).shouldCaptureProfile();
		verify(mockProfileHandler1, never()).fireProfile(any());
		verify(mockProfileHandler2, never()).fireProfile(any());
	}

	@Test
	public void testDoFireProfile_emptyHandlers(){
		spyProfilerFrameStackManager.setHandlers(Collections.emptyList());

		spyProfilerFrameStackManager.doFireProfile(mockFrame);

		verify(mockProfileHandler1, never()).shouldCaptureProfile();
		verify(mockProfileHandler2, never()).shouldCaptureProfile();
		verify(mockProfileHandler1, never()).fireProfile(any());
		verify(mockProfileHandler2, never()).fireProfile(any());
	}

	@Test
	public void testDoFireProfile_multipleHandlers(){
		spyProfilerFrameStackManager.setHandlers(profileHandlers);

		spyProfilerFrameStackManager.doFireProfile(mockFrame);

		verify(mockProfileHandler1).shouldCaptureProfile();
		verify(mockProfileHandler2).shouldCaptureProfile();
		verify(mockProfileHandler1, never()).fireProfile(any());
		verify(mockProfileHandler2).fireProfile(mockFrame);
	}

	@Test
	public void testGetCurrentFrame__emptyStack(){
		String methodName = "asdf";
		when(mockFrameStack.isEmpty()).thenReturn(true);
		Frame currentFrame = spyProfilerFrameStackManager.getCurrentFrame(methodName);

		assertEquals(methodName, currentFrame.getName());
		assertNotEquals(mockFrame, currentFrame);

		verify(mockThreadLocalStack).get();
		verify(mockFrameStack).isEmpty();
	}


	@Test
	public void testGetCurrentFrame__nonEmptyStack(){
		String methodName = "asdf";

		when(mockFrame.addChildFrameIfAbsent(methodName)).thenReturn(new Frame(methodName));

		Frame currentFrame = spyProfilerFrameStackManager.getCurrentFrame(methodName);

		assertEquals(methodName, currentFrame.getName());

		verify(mockThreadLocalStack).get();
		verify(mockFrameStack).isEmpty();
		verify(mockFrameStack).peek();
		verify(mockFrame).addChildFrameIfAbsent(methodName);
	}

	@Test
	public void testStartProfiling(){
		doReturn(mockFrame).when(spyProfilerFrameStackManager).getCurrentFrame(methodName);


		spyProfilerFrameStackManager.startProfiling(methodName);

		verify(spyProfilerFrameStackManager).startProfiling(methodName);
		verify(mockThreadLocalStack).get();
		verify(spyProfilerFrameStackManager).getCurrentFrame(methodName);
		verify(mockFrameStack).push(mockFrame);

		verifyNoMoreInteractions(mockFrame);
		verifyNoMoreInteractions(mockThreadLocalStack);
		verifyNoMoreInteractions(mockFrameStack);
		verifyNoMoreInteractions(mockProfileHandler1);
		verifyNoMoreInteractions(mockProfileHandler2);
	}


	@Test
	public void testEndProfiling_methodNameNotMatchTopOfStack(){
		String unmatchingMethodName = "This is not the method name you are looking for";
		try {
			spyProfilerFrameStackManager.endProfiling(unmatchingMethodName, elapsedTime);
			fail("An IllegalStateException should have been thrown");
		}catch (IllegalArgumentException e){
			// expected Exception
		}

		verify(mockThreadLocalStack).get();
		verify(mockFrameStack).pop();
		verify(mockFrame, times(2)).getName();

		verifyNoMoreInteractions(mockFrame);
		verifyNoMoreInteractions(mockThreadLocalStack);
		verifyNoMoreInteractions(mockFrameStack);
		verifyNoMoreInteractions(mockProfileHandler1);
		verifyNoMoreInteractions(mockProfileHandler2);
	}

	@Test
	public void testEndProfiling_methodNameMatchesTopOfStack_FrameStackNotEmpty(){
		doNothing().when(spyProfilerFrameStackManager).doFireProfile(mockFrame);
		when(mockFrameStack.isEmpty()).thenReturn(false);

		spyProfilerFrameStackManager.endProfiling(methodName, elapsedTime);

		verify(mockThreadLocalStack).get();
		verify(mockFrameStack).pop();
		verify(mockFrameStack).isEmpty();
		verify(mockFrame).addElapsedTime(elapsedTime);
		verify(mockFrame).getName();
		verify(spyProfilerFrameStackManager,never()).doFireProfile(any());

		verifyNoMoreInteractions(mockFrame);
		verifyNoMoreInteractions(mockThreadLocalStack);
		verifyNoMoreInteractions(mockFrameStack);
		verifyNoMoreInteractions(mockProfileHandler1);
		verifyNoMoreInteractions(mockProfileHandler2);
	}

	@Test
	public void testEndProfiling_methodNameMatchesTopOfStack_FrameStackEmpty(){
		doNothing().when(spyProfilerFrameStackManager).doFireProfile(mockFrame);
		when(mockFrameStack.isEmpty()).thenReturn(true);

		spyProfilerFrameStackManager.endProfiling(methodName, elapsedTime);

		verify(mockThreadLocalStack).get();
		verify(mockFrameStack).pop();
		verify(mockFrameStack).isEmpty();
		verify(mockFrame).addElapsedTime(elapsedTime);
		verify(mockFrame).getName();
		verify(spyProfilerFrameStackManager).doFireProfile(mockFrame);

		verifyNoMoreInteractions(mockFrame);
		verifyNoMoreInteractions(mockThreadLocalStack);
		verifyNoMoreInteractions(mockFrameStack);
		verifyNoMoreInteractions(mockProfileHandler1);
		verifyNoMoreInteractions(mockProfileHandler2);
	}


}
