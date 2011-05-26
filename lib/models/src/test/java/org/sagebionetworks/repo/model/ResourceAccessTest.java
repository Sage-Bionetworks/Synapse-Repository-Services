package org.sagebionetworks.repo.model;

import static org.junit.Assert.*;

import java.util.HashSet;

import org.junit.Test;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_TYPE;

/**
 * Make sure hash and equals are as expected.
 * 
 * @author jmhill
 *
 */
public class ResourceAccessTest {
	
	@Test
	public void testHashAndEquals(){
		// Since ResourceAccess objects are enforced with a Java Set
		// Then the hash() and equals() methods must work as expected
		// Two ResourceAccess objects are equal if they have the same group.
//		ResourceAccess one = new ResourceAccess();
//		one.setUserGroupId("10101");
//		one.setAccessType(new HashSet<ACCESS_TYPE>());
//		one.getAccessType().add(ACCESS_TYPE.CHANGE_PERMISSIONS);
//		
//		// Two is the same with just the id
//		ResourceAccess two = new ResourceAccess();
//		two.setUserGroupId("10101");
//		assertEquals(one, two);
	}

}
