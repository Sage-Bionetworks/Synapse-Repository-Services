/**
 * 
 */
package org.sagebionetworks.repo.model.jdo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.MigratableObjectData;
import org.sagebionetworks.repo.model.MigratableObjectDescriptor;
import org.sagebionetworks.repo.model.MigratableObjectType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

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
	
	private Collection<Node> nodeList = new ArrayList<Node>();
	private Collection<UserGroup> groupList = new ArrayList<UserGroup>();
	private Collection<AccessControlList> aclList = new ArrayList<AccessControlList>();

	private Node node = null;
	private AccessControlList acl = null;
	private UserGroup group = null;
	private UserGroup group2 = null;
	
	private Long createdById = null;
	private Long modifiedById = null;
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		createdById = Long.parseLong(userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false).getId());
		assertNotNull(createdById);
		// strictly speaking it's nonsensical for a group to be a 'modifier'.  we're just using it for testing purposes
		modifiedById = Long.parseLong(userGroupDAO.findGroup(AuthorizationConstants.DEFAULT_GROUPS.AUTHENTICATED_USERS.name(), false).getId());
		assertNotNull(modifiedById);

		// create a resource on which to apply permissions
		node = new Node();
		node.setName("foo");
		node.setCreatedOn(new Date());
		node.setCreatedByPrincipalId(createdById);
		node.setModifiedOn(new Date());
		node.setModifiedByPrincipalId(modifiedById);
		node.setNodeType(EntityType.project.name());
		String nodeId = nodeDAO.createNew(node);
		assertNotNull(nodeId);
		node = nodeDAO.getNode(nodeId);
		nodeList.add(node);
		
		// create a group to give the permissions to
		group = new UserGroup();
		group.setName("bar");
		group.setId(userGroupDAO.create(group));
		assertNotNull(group.getId());
		groupList.add(group);
		
		// Create a second user
		group2 = new UserGroup();
		group2.setName("bar2");
		group2.setId(userGroupDAO.create(group2));
		assertNotNull(group2.getId());
		groupList.add(group2);
		
		// Create an ACL for this node
		AccessControlList acl = new AccessControlList();
		acl.setId(nodeId);
		acl.setCreationDate(new Date(System.currentTimeMillis()));
		acl.setResourceAccess(new HashSet<ResourceAccess>());
		accessControlListDAO.create(acl);
		
		acl = accessControlListDAO.getForResource(node.getId());
		assertNotNull(acl);
		assertNotNull(acl.getEtag());
		assertEquals(node.getId(), acl.getId());
		Set<ResourceAccess> ras = new HashSet<ResourceAccess>();
		ResourceAccess ra = new ResourceAccess();
		ra.setGroupName(group.getName()); 
		ra.setPrincipalId(Long.parseLong(group.getId()));
		//ra.setDisplayName(group.getName());
		ra.setAccessType(new HashSet<ACCESS_TYPE>(
				Arrays.asList(new ACCESS_TYPE[]{
						ACCESS_TYPE.READ
				})));
		ras.add(ra);
		acl.setResourceAccess(ras);
		accessControlListDAO.update(acl);
		acl = accessControlListDAO.getForResource(node.getId());
		assertNotNull(acl);
		ras = acl.getResourceAccess();
		assertEquals(1, ras.size());
		ResourceAccess raClone = ras.iterator().next();
		assertEquals(ra.getPrincipalId(), raClone.getPrincipalId());
		// TODO assertEquals(ra.getDisplayName(), raClone.getDisplayName());
		assertEquals(ra.getAccessType(), raClone.getAccessType());
		aclList.add(acl);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		for (Node n : nodeList) nodeDAO.delete(n.getId());
		nodeList.clear();
		for (UserGroup g : groupList) userGroupDAO.delete(g.getId());
		groupList.clear();
		aclList.clear();
		this.node=null;
		this.acl=null;
		this.group=null;
		this.group2=null;
	}
	
	@Test
	public void testMigrationData() throws Exception {
		// first check what happens if dependencies are NOT requested
		QueryResults<MigratableObjectData> results = nodeDAO.getMigrationObjectData(0, 10000, false);
		List<MigratableObjectData> ods = results.getResults();
		assertEquals(ods.size(), results.getTotalNumberOfResults());
		assertTrue(ods.size()>0);
		boolean foundId = false;
		for (MigratableObjectData od : ods) {
			if (od.getId().getId().equals(node.getId())) {
				foundId=true;
			}
			assertEquals(MigratableObjectType.ENTITY, od.getId().getType());
			
		}
		assertTrue(foundId);
		
		// now query for objects WITH dependencies
		results = nodeDAO.getMigrationObjectData(0, 10000, true);
		ods = results.getResults();
		assertEquals(ods.size(), results.getTotalNumberOfResults());
		assertTrue(ods.size()>0);
		foundId = false;
		for (MigratableObjectData od : ods) {
			if (od.getId().getId().equals(node.getId())) {
				foundId=true;
				Collection<MigratableObjectDescriptor> deps = od.getDependencies();
				assertEquals(" dependencies: "+deps.toString(), 0, deps.size());
			}
			assertEquals(MigratableObjectType.ENTITY, od.getId().getType());
		}
		assertTrue(foundId);
		
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

	@Test (expected=NotFoundException.class)
	public void testGetForResourceBadID() throws Exception {
		String rid = "-598787";
		AccessControlList acl = accessControlListDAO.getForResource(rid);
		assertNull(acl);
	}
	
	@Test
	public void testNOOP() {
	
	}

	
	/**
	 * Test method for {@link org.sagebionetworks.repo.model.jdo.JDOAccessControlListDAOImpl#canAccess(java.util.Collection, java.lang.String, org.sagebionetworks.repo.model.ACCESS_TYPE)}.
	 */
	@Test
	public void testCanAccess() throws Exception {
		Collection<UserGroup> gs = new ArrayList<UserGroup>();
		gs.add(group);
		
		// as expressed in 'setUp', 'group' has 'READ' access to 'node'
		assertTrue(accessControlListDAO.canAccess(gs, node.getId(), ACCESS_TYPE.READ));
		
		// but it doesn't have 'UPDATE' access
		assertFalse(accessControlListDAO.canAccess(gs, node.getId(), ACCESS_TYPE.UPDATE));
		
		// and no other group has been given access
		UserGroup sham = new UserGroup();
		sham.setName("sham");
		sham.setId("-34876387468764"); // dummy
		gs.clear();
		gs.add(sham);
		assertFalse(accessControlListDAO.canAccess(gs, node.getId(), ACCESS_TYPE.READ));
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
	 * Test method for {@link org.sagebionetworks.repo.model.jdo.JDOBaseDAOImpl#update(org.sagebionetworks.repo.model.Base)}.
	 */
	@Test
	public void testUpdate() throws Exception {
		Node node = nodeList.iterator().next();
		String rid = node.getId();
		AccessControlList acl = accessControlListDAO.getForResource(rid);
		Set<ResourceAccess> ras = acl.getResourceAccess();
		ResourceAccess ra = ras.iterator().next();
		ra.setAccessType(new HashSet<ACCESS_TYPE>(
				Arrays.asList(new ACCESS_TYPE[]{
						ACCESS_TYPE.UPDATE,
						ACCESS_TYPE.CREATE
				})));
		String etagBeforeUpdate = acl.getEtag();
		accessControlListDAO.update(acl);
		
		Collection<UserGroup> gs = new ArrayList<UserGroup>();
		gs.add(group);
		assertFalse(accessControlListDAO.canAccess(gs, node.getId(), ACCESS_TYPE.READ));
		
		assertTrue(accessControlListDAO.canAccess(gs, node.getId(), ACCESS_TYPE.UPDATE));
		assertTrue(accessControlListDAO.canAccess(gs, node.getId(), ACCESS_TYPE.CREATE));
	
		AccessControlList acl2 = accessControlListDAO.getForResource(rid);
		// This test is moved to permission manager
		//assertFalse(etagBeforeUpdate.equals(acl2.getEtag()));
	}
	
	
	@Test
	public void testUpdateMultipleGroups() throws Exception {
		Node node = nodeList.iterator().next();
		String rid = node.getId();
		AccessControlList acl = accessControlListDAO.getForResource(rid);
		Set<ResourceAccess> ras = acl.getResourceAccess();
		ResourceAccess ra = ras.iterator().next();
		ra.setAccessType(new HashSet<ACCESS_TYPE>(
				Arrays.asList(new ACCESS_TYPE[]{
						ACCESS_TYPE.UPDATE,
						ACCESS_TYPE.CREATE
				})));
		// Now add a new resource access for group 2
		ResourceAccess ra2 = new ResourceAccess();
		ra2.setPrincipalId(Long.parseLong(group2.getId()));
		//ra2.setDisplayName(group2.getName());
		ra2.setAccessType(new HashSet<ACCESS_TYPE>(
				Arrays.asList(new ACCESS_TYPE[]{
						ACCESS_TYPE.READ,
				})));
		acl.getResourceAccess().add(ra2);
		accessControlListDAO.update(acl);
		
		Collection<UserGroup> gs = new ArrayList<UserGroup>();
		gs.add(group);
		Collection<UserGroup> gs2 = new ArrayList<UserGroup>();
		gs2.add(group2);
		assertFalse(accessControlListDAO.canAccess(gs, node.getId(), ACCESS_TYPE.READ));
		assertTrue(accessControlListDAO.canAccess(gs2, node.getId(), ACCESS_TYPE.READ));
		
		// Group one can do this but 2 cannot.
		assertTrue(accessControlListDAO.canAccess(gs, node.getId(), ACCESS_TYPE.UPDATE));
		assertTrue(accessControlListDAO.canAccess(gs, node.getId(), ACCESS_TYPE.CREATE));
		// Now try 2
		assertFalse(accessControlListDAO.canAccess(gs2, node.getId(), ACCESS_TYPE.UPDATE));
		assertFalse(accessControlListDAO.canAccess(gs2, node.getId(), ACCESS_TYPE.CREATE));
		
	}

}
