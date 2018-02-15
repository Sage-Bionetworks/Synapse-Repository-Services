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
		Frame existingChild = new Frame("childFrame");
		frame.addChild(existingChild);

		Frame child = frame.addChildFrameIfAbsent("childFrame");
		assertSame(existingChild, child);
	}

	@Test
	public void testAddChild_newChild(){
		Frame child = new Frame("myFrame");
		frame.addChild(child);
		assertSame(child, frame.getChild(child.getName()));
	}

	@Test
	public void testAddChild_alreadyExist(){
		frame.addChild(new Frame("sameName"));
		try{
			frame.addChild(new Frame("sameName"));
			fail();
		}catch (IllegalArgumentException e){
			//expected
		}
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
		assertEquals(String.format("%n[%1$d] ELAPSE: 00:00:00.042 METHOD: frame%n" +
					"[%1$d] ELAPSE: ---- 00:00:00.012 METHOD: childFrame1 (Count: 2 Average: 00:00:00.006, Min: 00:00:00.002, Max: 00:00:00.010)%n" +
					"[%1$d] ELAPSE: ---- 00:00:00.000 METHOD: childFrame2%n" +
					"[%1$d] ELAPSE: -------- 00:00:00.000 METHOD: grandChild", threadId)
				, frame.toString());

	}

}
