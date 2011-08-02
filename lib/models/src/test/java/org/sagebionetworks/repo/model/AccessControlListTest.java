package org.sagebionetworks.repo.model;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_TYPE;

/**
 * Test for the ACL DTO object
 * @author jmhill
 *
 */
public class AccessControlListTest {

	@Test
	public void testGrantAll(){
		String nodeId = "123";
		UserInfo info = new UserInfo(false);
		UserGroup userGroup = new UserGroup();
		userGroup.setId("123");
		userGroup.setName("one");
		User user = new User();
		user.setId("33");
		user.setUserId("someUser@somedomain.net");
		info.setUser(user);
		info.setIndividualGroup(userGroup);
		AccessControlList acl = AccessControlList.createACLToGrantAll(nodeId, info);
		assertNotNull(acl);
		assertEquals(acl.getId(), nodeId);
		assertEquals(user.getUserId(), acl.getCreatedBy());
		assertEquals(user.getUserId(), acl.getModifiedBy());
		assertNotNull(acl.getCreationDate());
		assertNotNull(acl.getModifiedOn());
		assertNotNull(acl.getResourceAccess());
		assertEquals(1, acl.getResourceAccess().size());
		ResourceAccess ra = acl.getResourceAccess().iterator().next();
		assertNotNull(ra);
		assertEquals(userGroup.getName(), ra.getGroupName());
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
