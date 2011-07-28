package org.sagebionetworks.repo.model.bootstrap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationConstants.DEFAULT_GROUPS;
import org.sagebionetworks.repo.model.ResourceAccess;

public class EntityBootstrapperUnitTest {
	
	@Test
	public void testSplitParentPathAndName(){
		String path = " /root ";
		String[] results = EntityBootstrapperImpl.splitParentPathAndName(path);
		assertNotNull(results);
		// The parent path for this should be null
		assertTrue(results[0] == null);
		assertEquals("root", results[1]);
	}
	
	
	@Test
	public void testSplitParentPathAndNameLonger(){
		String path = "/root/parent/child ";
		String[] results = EntityBootstrapperImpl.splitParentPathAndName(path);
		assertNotNull(results);
		// The parent path for this should be null
		assertEquals("/root/parent", results[0]);
		assertEquals("child", results[1]);
	}
	
	@Test
	public void testCreateAcl(){
		// Build up a list of entires
		List<AccessBootstrapData> list = new ArrayList<AccessBootstrapData>();
		
		AccessBootstrapData data = new AccessBootstrapData();
		data.setGroup(DEFAULT_GROUPS.AUTHENTICATED_USERS);
		List<ACCESS_TYPE> types = new ArrayList<ACCESS_TYPE>();
		types.add(ACCESS_TYPE.CHANGE_PERMISSIONS);
		types.add(ACCESS_TYPE.CREATE);
		data.setAccessTypeList(types);
		list.add(data);
		
		data = new AccessBootstrapData();
		data.setGroup(DEFAULT_GROUPS.PUBLIC);
		types = new ArrayList<ACCESS_TYPE>();
		types.add(ACCESS_TYPE.DELETE);
		data.setAccessTypeList(types);
		list.add(data);
		
		String nodeId = "101";
		Map<DEFAULT_GROUPS, String> map = new HashMap<DEFAULT_GROUPS, String>();
		map.put(DEFAULT_GROUPS.AUTHENTICATED_USERS, "99");
		map.put(DEFAULT_GROUPS.PUBLIC, "25");
		
		// Now create the ACL
		AccessControlList acl = EntityBootstrapperImpl.createAcl(nodeId, list, map);
		assertNotNull(acl);
		assertEquals(nodeId, acl.getResourceId());
		assertNotNull(acl.getResourceAccess());
		assertEquals(2, acl.getResourceAccess().size());
		Iterator<ResourceAccess> it = acl.getResourceAccess().iterator();
		ResourceAccess authRa = null;
		ResourceAccess publicRa = null;
		while(it.hasNext()){
			ResourceAccess ra = it.next();
			if("99".equals(ra.getUserGroupId())){
				authRa = ra;
			}else if("25".equals(ra.getUserGroupId())){
				publicRa = ra;
			}
		}
		assertNotNull(authRa);
		assertNotNull(publicRa);
		
		assertEquals(2, authRa.getAccessType().size());
		assertTrue(authRa.getAccessType().contains(ACCESS_TYPE.CHANGE_PERMISSIONS));
		assertTrue(authRa.getAccessType().contains(ACCESS_TYPE.CREATE));
		
		assertEquals(1, publicRa.getAccessType().size());
		assertTrue(publicRa.getAccessType().contains(ACCESS_TYPE.DELETE));
		
		
	}
}
