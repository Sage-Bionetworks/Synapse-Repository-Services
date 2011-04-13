package org.sagebionetworks.web.unitshared;

import org.sagebionetworks.web.shared.Identifer;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Test for the immutable identifier.
 * 
 * @author jmhill
 *
 */
public class IdentiferTest {
	
	@Test
	public void testConstructorEquals(){
		// Make sure null constructor fails
		try{
			new Identifer(null);
			fail("Null should have thrown an exception");
		}catch (IllegalArgumentException e){
			// expected
		}
		try{
			new Identifer("");
			fail("Null should have thrown an exception");
		}catch (IllegalArgumentException e){
			// expected
		}

		// Make sure we can make one from a string
		Identifer idOrignal  = new Identifer("0x120");
		
		// It should not equal null
		assertTrue("Should not be the same", !idOrignal.equals(null));
		assertTrue("Should not be the same", !idOrignal.equals(new Object()));
		assertTrue("Should be the same", idOrignal.equals(idOrignal));
		
		Identifer other = new Identifer("0x1200");
		assertTrue("Should not be the same", !idOrignal.equals(other));
		// flip it
		assertTrue("Should not be the same", !other.equals(idOrignal));
		
		// Now create one that is that is the same as the original with white space
		Identifer sameAsOriginal  = new Identifer(" 0x120 ");
		assertTrue("Should be the same", idOrignal.equals(sameAsOriginal));
		// flip it
		assertTrue("Should be the same", sameAsOriginal.equals(idOrignal));
		// They should also have the same hash
		assertTrue("Should be the same", idOrignal.hashCode() == sameAsOriginal.hashCode());
	}
	
	@Test
	public void testRoundTrip(){
		// We should be able to make a copy of an ID from its string
		Identifer idOrignal  = new Identifer("0x2340");
		String stringValue = idOrignal.toString();
		assertNotNull(stringValue);
		Identifer copyFromString  = new Identifer(stringValue);
		// They should be the same
		assertTrue("They should be the same", idOrignal.equals(copyFromString));
		// flip it
		assertTrue("They should be the same", copyFromString.equals(idOrignal));
	}

}
