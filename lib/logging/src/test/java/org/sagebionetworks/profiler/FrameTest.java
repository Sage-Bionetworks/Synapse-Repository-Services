package org.sagebionetworks.profiler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
	
	Frame root = null;
	
	@Before
	public void before(){
		// Create a frame that is a few levels deep
		root = new Frame(0, "root");
		root.setEnd(12);
		// Add some children
		for(int i=0; i<4; i++){
			Frame child = new Frame(i, "child"+1);
			child.setEnd(i+1);
			root.addChild(child);
			// Add one more level
			for(int j=0; j<2; j++){
				Frame grand = new Frame(i, "grand"+j);
				grand.setEnd(i);
				child.addChild(grand);
			}
		}
	}

	/**
	 * @throws JSONException 
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonGenerationException 
	 * 
	 */
	@Test
	public void testJSONRoundTrip() throws JSONException {
		// Write it to json
		String json = Frame.writeFrameJSON(root);
		assertNotNull(json);
		System.out.println(json);
		// Now convert back to Frames
		Frame cloneRoot = Frame.readFrameFromJSON(json);
		assertNotNull(cloneRoot);
		assertEquals(root, cloneRoot);
	}
}
