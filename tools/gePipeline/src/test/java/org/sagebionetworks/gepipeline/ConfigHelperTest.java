package org.sagebionetworks.gepipeline;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ConfigHelperTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	public void testGetProperty() throws Exception {
		// get an arbitrary property.  This forces a bunch of machinery to run,
		// testing that it works right
		ConfigHelper.getGEPipelineSmallCapacityGB();
	}

}
