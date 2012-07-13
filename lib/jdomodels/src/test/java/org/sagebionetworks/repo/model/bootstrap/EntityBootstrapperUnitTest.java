package org.sagebionetworks.repo.model.bootstrap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.DEFAULT_GROUPS;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class EntityBootstrapperUnitTest {
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
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
	public void testCreateAcl() throws DatastoreException {
		// Build up a list of entires
		List<AccessBootstrapData> list = new ArrayList<AccessBootstrapData>();
		
		AccessBootstrapData data = new AccessBootstrapData();

		UserGroup authenticatedUsers = userGroupDAO.findGroup(DEFAULT_GROUPS.AUTHENTICATED_USERS.name(), false);
		assertNotNull(authenticatedUsers);

		data.setGroupId(Long.parseLong(authenticatedUsers.getId()));
		List<ACCESS_TYPE> types = new ArrayList<ACCESS_TYPE>();
		types.add(ACCESS_TYPE.CHANGE_PERMISSIONS);
		types.add(ACCESS_TYPE.CREATE);
		data.setAccessTypeList(types);
		list.add(data);
		
		data = new AccessBootstrapData();
		UserGroup publicUsers = userGroupDAO.findGroup(DEFAULT_GROUPS.PUBLIC.name(), false);
		assertNotNull(publicUsers);

		data.setGroupId(Long.parseLong(publicUsers.getId()));
		types = new ArrayList<ACCESS_TYPE>();
		types.add(ACCESS_TYPE.DELETE);
		data.setAccessTypeList(types);
		list.add(data);
		
		String nodeId = "101";
		
		// Now create the ACL
		assertNotNull(userGroupDAO);
		String createdById = userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false).getId();
		AccessControlList acl = EntityBootstrapperImpl.createAcl(nodeId, createdById, list); 
		assertNotNull(acl);
		assertEquals(nodeId, acl.getId());
		assertNotNull(acl.getResourceAccess());
		assertEquals(2, acl.getResourceAccess().size());
		Iterator<ResourceAccess> it = acl.getResourceAccess().iterator();
		ResourceAccess authRa = null;
		ResourceAccess publicRa = null;
		while(it.hasNext()){
			ResourceAccess ra = it.next();
			if(authenticatedUsers.getId().equals(ra.getPrincipalId().toString())){
				authRa = ra;
			}else if(publicUsers.getId().equals(ra.getPrincipalId().toString())){
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
