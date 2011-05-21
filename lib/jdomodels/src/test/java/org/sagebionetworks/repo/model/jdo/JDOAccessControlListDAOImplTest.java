/**
 * 
 */
package org.sagebionetworks.repo.model.jdo;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ResourceAccess2;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.query.ObjectType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jdo.JdoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author bhoff
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class JDOAccessControlListDAOImplTest {
	
	@Autowired
	private AccessControlListDAO accessControlListDAO;
	
	@Autowired
	private NodeDAO nodeDAO;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private JdoTemplate jdoTemplate;

	private Collection<Node> nodeList = new ArrayList<Node>();
	private Collection<UserGroup> groupList = new ArrayList<UserGroup>();
	private Collection<AccessControlList> aclList = new ArrayList<AccessControlList>();

	private Node node = null;
	private AccessControlList acl = null;
	private UserGroup group = null;
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		// create a resource on which to apply permissions
		node = new Node();
		node.setName("foo");
		node.setCreatedOn(new Date());
		node.setCreatedBy("me");
		node.setModifiedOn(new Date());
		node.setModifiedBy("metoo");
		node.setNodeType(ObjectType.project.name());
		String nodeId = nodeDAO.createNew(node);
		assertNotNull(nodeId);
		node.setId(nodeId);
		nodeList.add(node);
		
		// create a group to give the permissions to
		group = new UserGroup();
		group.setName("bar");
		group.setId(userGroupDAO.create(group));
		assertNotNull(group.getId());
		groupList.add(group);
		
		acl = new AccessControlList();
		acl.setCreationDate(new Date());
		acl.setCreatedBy("me");
		acl.setModifiedOn(new Date());
		acl.setModifiedBy("you");
		acl.setResourceId(node.getId());
		Set<ResourceAccess2> ras = new HashSet<ResourceAccess2>();
		ResourceAccess2 ra = new ResourceAccess2();
		ra.setUserGroupId(group.getId());
		ra.setAccessType(new HashSet<AuthorizationConstants.ACCESS_TYPE>(
				Arrays.asList(new AuthorizationConstants.ACCESS_TYPE[]{
						AuthorizationConstants.ACCESS_TYPE.READ
				})));
		ras.add(ra);
		acl.setResourceAccess(ras);
		String id = accessControlListDAO.create(acl);
		acl.setId(id);
		aclList.add(acl);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		for (AccessControlList acl : aclList) accessControlListDAO.delete(acl.getId());
		for (Node n : nodeList) nodeDAO.delete(n.getId());
		for (UserGroup g : groupList) userGroupDAO.delete(g.getId());
		this.node=null;
		this.acl=null;
		this.group=null;
	}

	/**
	 * Test method for {@link org.sagebionetworks.repo.model.jdo.JDOAccessControlListDAOImpl#getForResource(java.lang.String)}.
	 */
	@Test
	public void testGetForResource() throws Exception {
		Node node = nodeList.iterator().next();
		String rid = node.getId();
		AccessControlList acl = accessControlListDAO.getForResource(rid);
		assertEquals(acl, aclList.iterator().next());
	}

	@Test
	public void testGetForResourceBadID() throws Exception {
		String rid = "-598787";
		AccessControlList acl = accessControlListDAO.getForResource(rid);
		assertNull(acl);
	}

	
	/**
	 * Test method for {@link org.sagebionetworks.repo.model.jdo.JDOAccessControlListDAOImpl#canAccess(java.util.Collection, java.lang.String, org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_TYPE)}.
	 */
	@Test
	public void testCanAccess() throws Exception {
		Collection<UserGroup> gs = new ArrayList<UserGroup>();
		gs.add(group);
		
		// as expressed in 'setUp', 'group' has 'READ' access to 'node'
		assertTrue(accessControlListDAO.canAccess(gs, node.getId(), AuthorizationConstants.ACCESS_TYPE.READ));
		
		// but it doesn't have 'UPDATE' access
		assertFalse(accessControlListDAO.canAccess(gs, node.getId(), AuthorizationConstants.ACCESS_TYPE.UPDATE));
		
		// and no other group has been given access
		UserGroup sham = new UserGroup();
		sham.setName("sham");
		sham.setId("-34876387468764"); // dummy
		gs.clear();
		gs.add(sham);
		assertFalse(accessControlListDAO.canAccess(gs, node.getId(), AuthorizationConstants.ACCESS_TYPE.READ));
	}

	/**
	 * Test method for {@link org.sagebionetworks.repo.model.jdo.JDOAccessControlListDAOImpl#authorizationSQL()}.
	 */
	@Test
	public void testAuthorizationSQL() throws Exception {
		Collection<Long> groupIds = new HashSet<Long>();
		groupIds.add(KeyFactory.stringToKey(group.getId()));
		System.out.println("testAuthorizationSQL: all ACLs: ");
		for (AccessControlList acl : accessControlListDAO.getAll()) {
			System.out.println("\t"+acl);
		}
		Collection<Object> nodeIds = 
			accessControlListDAO.execAuthorizationSQL(
					groupIds, 
					AuthorizationConstants.ACCESS_TYPE.READ);
		assertEquals(1, nodeIds.size());
		assertEquals(nodeIds.toString(), KeyFactory.stringToKey(node.getId()), nodeIds.iterator().next());
	}

	/**
	 * Test method for {@link org.sagebionetworks.repo.model.jdo.JDOBaseDAOImpl#get(java.lang.String)}.
	 */
	@Test
	public void testGet() throws Exception {
		
		AccessControlList acl = aclList.iterator().next();
		String id = acl.getId();
		
		AccessControlList acl2 = accessControlListDAO.get(id);
		
		assertEquals(acl, acl2);
		
		aclList.remove(acl);
		accessControlListDAO.delete(id);
		
		try {
			accessControlListDAO.get(id);
			fail("NotFoundException expected");	
		} catch (NotFoundException e) {  // any other kind of exception will cause a failure
			// as expected
		}


	}


	/**
	 * Test method for {@link org.sagebionetworks.repo.model.jdo.JDOBaseDAOImpl#getAll()}.
	 */
	@Test
	public void testGetAll() throws Exception {
		assertEquals(aclList, accessControlListDAO.getAll());
	}

	/**
	 * Test method for {@link org.sagebionetworks.repo.model.jdo.JDOBaseDAOImpl#update(org.sagebionetworks.repo.model.Base)}.
	 */
	@Test
	@Ignore
	public void testUpdate() throws Exception {
		Node node = nodeList.iterator().next();
		String rid = node.getId();
		AccessControlList acl = accessControlListDAO.getForResource(rid);
		Set<ResourceAccess2> ras = acl.getResourceAccess();
		ResourceAccess2 ra = ras.iterator().next();
		ra.setAccessType(new HashSet<AuthorizationConstants.ACCESS_TYPE>(
				Arrays.asList(new AuthorizationConstants.ACCESS_TYPE[]{
						AuthorizationConstants.ACCESS_TYPE.UPDATE,
						AuthorizationConstants.ACCESS_TYPE.CREATE
				})));
		accessControlListDAO.update(acl);
		
		Collection<UserGroup> gs = new ArrayList<UserGroup>();
		gs.add(group);
		assertFalse(accessControlListDAO.canAccess(gs, node.getId(), AuthorizationConstants.ACCESS_TYPE.READ));
		
		assertTrue(accessControlListDAO.canAccess(gs, node.getId(), AuthorizationConstants.ACCESS_TYPE.UPDATE));
		assertTrue(accessControlListDAO.canAccess(gs, node.getId(), AuthorizationConstants.ACCESS_TYPE.CREATE));
		
	}

}
