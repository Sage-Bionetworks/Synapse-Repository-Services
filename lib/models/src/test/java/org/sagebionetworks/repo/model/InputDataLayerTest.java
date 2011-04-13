package org.sagebionetworks.repo.model;


import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author deflaux
 *
 */
public class InputDataLayerTest {

	/**
	 * @throws Exception
	 */
	@Test(expected=InvalidModelException.class)
	public void testInvalidTypeName() throws Exception {
		try {
			InputDataLayer layer = new InputDataLayer();
			layer.setType("this should fail");
		}
		catch(InvalidModelException e) {
			assertEquals("'type' must be one of: E G C", e.getMessage());
			throw e;
		}
	}

}
