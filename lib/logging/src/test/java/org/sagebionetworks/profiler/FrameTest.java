package org.sagebionetworks.profiler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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

}
