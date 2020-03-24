package org.sagebionetworks.repo.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;
import org.sagebionetworks.repo.model.util.ModelConstants;

/**
 * Test for the ACL DTO object
 * @author jmhill
 *
 */
public class AccessControlListTest {

	@Test
	public void testGrantEntityAdminACL(){
		String nodeId = "123";
		UserInfo info = new UserInfo(false, 123L);
		AccessControlList acl = AccessControlListUtil.createACLToGrantEntityAdminAccess(nodeId, info, new Date());
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
		assertEquals(ModelConstants.ENTITY_ADMIN_ACCESS_PERMISSIONS.size(), ra.getAccessType().size());
		// check each type
		for(ACCESS_TYPE type: ModelConstants.ENTITY_ADMIN_ACCESS_PERMISSIONS){
			assertTrue(ra.getAccessType().contains(type));
		}
	}
	
	@Test
	public void testGrantEvaluationAdminACL(){
		String nodeId = "123";
		UserInfo info = new UserInfo(false, 123L);
		AccessControlList acl = AccessControlListUtil.createACLToGrantEvaluationAdminAccess(nodeId, info, new Date());
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
		assertEquals(ModelConstants.EVALUATION_ADMIN_ACCESS_PERMISSIONS.size(), ra.getAccessType().size());
		// check each type
		for(ACCESS_TYPE type: ModelConstants.EVALUATION_ADMIN_ACCESS_PERMISSIONS){
			assertTrue(ra.getAccessType().contains(type));
		}
	}

}
