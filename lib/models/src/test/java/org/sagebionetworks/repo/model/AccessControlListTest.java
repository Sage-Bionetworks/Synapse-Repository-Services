package org.sagebionetworks.repo.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;

/**
 * Test for the ACL DTO object
 * @author jmhill
 *
 */
public class AccessControlListTest {

	@Test
	public void testGrantAll(){
		String nodeId = "123";
		UserInfo info = new UserInfo(false, 123L);
		AccessControlList acl = AccessControlListUtil.createACLToGrantEntityAdminAccess(nodeId, info);
		assertNotNull(acl);
		assertEquals(acl.getId(), nodeId);
		assertNotNull(acl.getCreationDate());
		assertNotNull(acl.getResourceAccess());
		assertEquals(1, acl.getResourceAccess().size());
		ResourceAccess ra = acl.getResourceAccess().iterator().next();
		assertNotNull(ra);
		assertEquals(info.getId(), ra.getPrincipalId());
		assertNotNull(ra.getAccessType());
		// There should be one for each type
		ACCESS_TYPE[] array = ACCESS_TYPE.values();
		assertEquals(array.length, ra.getAccessType().size());
		// check each type
		for(ACCESS_TYPE type: array){
			assertTrue(ra.getAccessType().contains(type));
		}
	}
}
