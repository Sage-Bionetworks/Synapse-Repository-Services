package org.sagebionetworks.profiler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

/**
 * Test for Frame.
 *
 */
public class FrameTest {
	
	Frame frame;
	
	@Before
	public void before(){
		// Create a frame that is a few levels deep
		frame = new Frame("frame");
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
	public void testPrint
}
