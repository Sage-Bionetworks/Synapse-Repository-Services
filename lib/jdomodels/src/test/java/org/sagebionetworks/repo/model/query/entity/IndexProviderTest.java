package org.sagebionetworks.repo.model.query.entity;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class IndexProviderTest {

	@Test
	public void testNext(){
		IndexProvider provider = new IndexProvider();
		assertEquals(0,provider.nextIndex()); 
		assertEquals(1,provider.nextIndex()); 
		assertEquals(2,provider.nextIndex()); 
	}

}
