package org.sagebionetworks.profiler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.json.JSONException;
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

	/**
	 * @throws JSONException 
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonGenerationException 
	 * 
	 */
	@Test
	public void testJSONRoundTrip() throws JSONException { //TODO: fix
		// Write it to json
		String json = Frame.writeFrameJSON(frame);
		assertNotNull(json);
		System.out.println(json);
		// Now convert back to Frames
		Frame cloneRoot = Frame.readFrameFromJSON(json);
		assertNotNull(cloneRoot);
		assertEquals(frame, cloneRoot);
	}


	@Test
	public void testAddFrameIfAbsent_frameIsAbsent(){
		Frame child = frame.addFrameIfAbsent("childFrame");
		assertSame(child, frame.getChild("frame"));
	}

	@Test
	public void testAddFrameIfAbsent_frameAlreadyExists(){
		Frame existingChild = new Frame("childFrame");
		frame.addChild(existingChild);

		Frame child = frame.addFrameIfAbsent("childFrame");
		assertSame(existingChild, child);
	}

	@Test
	public void testAddElapsedTime(){

	}
}
