package org.sagebionetworks.profiler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Test for Frame.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class FrameTest {
	
	Frame frame;
	StringBuilder builder;

	@Mock
	Frame mockFrame;

	@Mock
	Frame mockFrame2;

	LinkedHashMap<String, Frame> mockFrameChildren;
	LinkedHashMap<String, Frame> mockFrame2Children;


	@Before
	public void before(){
		// Create a frame that is a few levels deep
		frame = new Frame("frame");
		builder = new StringBuilder();

		mockFrameChildren = new LinkedHashMap<>();
		mockFrame2Children = new LinkedHashMap<>();
		ReflectionTestUtils.setField(mockFrame, "children", mockFrameChildren);
		ReflectionTestUtils.setField(mockFrame2, "children", mockFrame2Children);
	}


	@Test
	public void testAddFrameIfAbsent_frameIsAbsent(){
		Frame child = frame.addChildFrameIfAbsent("childFrame");
		assertSame(child, frame.getChild(child.getName()));
	}

	@Test
	public void testAddFrameIfAbsent_frameAlreadyExists(){
		Frame existingChild = frame.addChildFrameIfAbsent("childFrame");

		Frame child = frame.addChildFrameIfAbsent("childFrame");
		assertSame(existingChild, child);
	}

	@Test
	public void testAddElapsedTime(){
		long time = 42;
		frame.addElapsedTime(time);
		assertEquals(time, frame.getTotalTimeMilis());
	}

	@Test
	public void testToString(){
		long time = 42;
		frame.addElapsedTime(time);
		Frame child1 = frame.addChildFrameIfAbsent("childFrame1");
		child1.addElapsedTime(10);
		child1.addElapsedTime(2);
		Frame child2 = frame.addChildFrameIfAbsent("childFrame2");
		Frame grandchild = child2.addChildFrameIfAbsent("grandChild");

		long threadId = Thread.currentThread().getId();
		assertEquals(String.format("%n[%1$d] ELAPSE:00:00:00.042 METHOD: frame%n" +
					"[%1$d] ELAPSE:----00:00:00.012 METHOD: childFrame1 (Count: 2 Average: 00:00:00.006, Min: 00:00:00.002, Max: 00:00:00.010)%n" +
					"[%1$d] ELAPSE:----00:00:00.000 METHOD: childFrame2%n" +
					"[%1$d] ELAPSE:--------00:00:00.000 METHOD: grandChild", threadId)
				, frame.toString());

	}

	@Test
	public void testGetMinTimeMilis(){
		frame.addElapsedTime(1);
		frame.addElapsedTime(2);
		assertEquals(1, frame.getMinTimeMilis());
	}

	@Test
	public void testGetMaxTimeMillis(){
		frame.addElapsedTime(1);
		frame.addElapsedTime(2);
		assertEquals(2, frame.getMaxTimeMilis());
	}

	@Test
	public void testGetTotalTimeMillis(){
		frame.addElapsedTime(1);
		frame.addElapsedTime(2);
		assertEquals(3, frame.getTotalTimeMilis());
	}

	@Test
	public void testGetAverageTimeMillis_noValuesAdded(){
		Frame frameNoData = new Frame("some name");
		assertEquals(0L, frameNoData.getAverageTimeMilis());
	}

	@Test
	public void testGetAverageTimeMillis_averageRoundsDown(){
		frame.addElapsedTime(1);
		frame.addElapsedTime(2);
		assertEquals(2, frame.getAverageTimeMilis());
	}

	@Test
	public void testGetAverageTimeMillis_averageRoundsUp(){
		frame.addElapsedTime(3);
		frame.addElapsedTime(3);
		frame.addElapsedTime(4);
		assertEquals(3, frame.getAverageTimeMilis());
	}

	@Test
	public void testFormatDuration_milliseconds(){
		assertEquals("00:00:00.999",Frame.formatDuration(999));
	}
	@Test
	public void testFormatDuration_seconds(){
		assertEquals("00:00:59.999",Frame.formatDuration(59 * 1000 + 999));
	}
	@Test
	public void testFormatDuration_minutes(){
		assertEquals("00:59:59.999",Frame.formatDuration( (59 * 60 + 59) * 1000 + 999));
	}
	@Test
	public void testFormatDuration_hours(){
		assertEquals("42:59:59.999",Frame.formatDuration( ((42 * 60 + 59) * 60 + 59) * 1000 + 999));
	}

	@Test
	public void testAppendStatisticsToStringBuilder_withIndent(){
		frame.addElapsedTime(42);
		frame.appendStatisticsToStringBuilder(builder,"----");
		assertEquals(String.format("%n[1] ELAPSE:----00:00:00.042 METHOD: frame"), builder.toString());
	}

	@Test
	public void testAppendStatisticsToStringBuilder_withNoIndent(){
		frame.addElapsedTime(42);
		frame.appendStatisticsToStringBuilder(builder, "");
		assertEquals(String.format("%n[1] ELAPSE:00:00:00.042 METHOD: frame"), builder.toString());
	}

	@Test
	public void testAppendStatisticsToStringBuilder_multipleElapsedTime(){
		frame.addElapsedTime(42);
		frame.addElapsedTime(58);
		frame.appendStatisticsToStringBuilder(builder, "");
		assertEquals(String.format("%n[1] ELAPSE:00:00:00.100 METHOD: frame (Count: 2 Average: 00:00:00.050, Min: 00:00:00.042, Max: 00:00:00.058)"), builder.toString());
	}

	@Test
	public void testAddFramesToBuffer_noIndent(){
		Frame.addFramesToBuffer(Collections.singletonList(mockFrame), builder, 0);
		verify(mockFrame).appendStatisticsToStringBuilder(builder,"");
		verifyNoMoreInteractions(mockFrame);
	}

	@Test
	public void testAddFramesToBuffer_withIndent(){
		int level = 2;
		Frame.addFramesToBuffer(Collections.singletonList(mockFrame), builder, level);
		verify(mockFrame).appendStatisticsToStringBuilder(builder, StringUtils.repeat(Frame.INDENT_STRING, level));
		verifyNoMoreInteractions(mockFrame);
	}

	@Test
	public void testAddFramesToBuffer_MultipleFrames(){
		Frame.addFramesToBuffer(Arrays.asList(mockFrame, mockFrame2), builder,0);
		verify(mockFrame).appendStatisticsToStringBuilder(builder,"");
		verify(mockFrame2).appendStatisticsToStringBuilder(builder,"");
		verifyNoMoreInteractions(mockFrame);
		verifyNoMoreInteractions(mockFrame2);
	}
	@Test
	public void testAddFramesToBuffer_RecursiveCall(){
		mockFrameChildren.put("key doesn't matter", mockFrame2);
		Frame.addFramesToBuffer(Collections.singletonList(mockFrame), builder,0);
		verify(mockFrame).appendStatisticsToStringBuilder(builder,"");
		verify(mockFrame2).appendStatisticsToStringBuilder(builder,StringUtils.repeat(Frame.INDENT_STRING, 1));
		verifyNoMoreInteractions(mockFrame);
		verifyNoMoreInteractions(mockFrame2);
	}
}
