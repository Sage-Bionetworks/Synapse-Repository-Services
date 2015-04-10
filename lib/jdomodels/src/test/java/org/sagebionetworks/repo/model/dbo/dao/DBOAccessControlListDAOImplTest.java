package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
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
public class DBOAccessControlListDAOImplTest {

	@Autowired
	private AccessControlListDAO aclDAO;
	
	@Autowired
	private NodeDAO nodeDAO;
	
	@Autowired
	private UserGroupDAO userGroupDAO;

	private Collection<Node> nodeList = new ArrayList<Node>();
	private Collection<UserGroup> groupList = new ArrayList<UserGroup>();
	private Collection<AccessControlList> aclList = new ArrayList<AccessControlList>();

	private Node node;
	private UserGroup group;
	private UserGroup group2;
	
	private Long createdById;
	private Long modifiedById;
	
	@Before
	public void setUp() throws Exception {
		createdById = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();

		// strictly speaking it's nonsensical for a group to be a 'modifier'.  we're just using it for testing purposes
		modifiedById = BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId();

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
		group.setIsIndividual(false);
		group.setId(userGroupDAO.create(group).toString());
		assertNotNull(group.getId());
		groupList.add(group);

		// Create a second user
		group2 = new UserGroup();
		group2.setIsIndividual(false);
		group2.setId(userGroupDAO.create(group2).toString());
		assertNotNull(group2.getId());
		groupList.add(group2);

		// Create an ACL for this node
		AccessControlList acl = new AccessControlList();
		acl.setId(nodeId);
		acl.setCreationDate(new Date(System.currentTimeMillis()));
		acl.setResourceAccess(new HashSet<ResourceAccess>());
		String aclId = aclDAO.create(acl, ObjectType.ENTITY);
		assertEquals(nodeId, aclId);

		acl = aclDAO.get(node.getId(), ObjectType.ENTITY);
		assertNotNull(acl);
		assertNotNull(acl.getEtag());
		final String etagBeforeUpdate = acl.getEtag();
		assertEquals(nodeId, acl.getId());
		Set<ResourceAccess> ras = new HashSet<ResourceAccess>();
		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(Long.parseLong(group.getId()));
		//ra.setDisplayName(group.getName());
		ra.setAccessType(new HashSet<ACCESS_TYPE>(
				Arrays.asList(new ACCESS_TYPE[]{
						ACCESS_TYPE.READ
				})));
		ras.add(ra);
		acl.setResourceAccess(ras);

		aclDAO.update(acl, ObjectType.ENTITY);
		acl = aclDAO.get(node.getId(), ObjectType.ENTITY);
		assertNotNull(acl);
		assertFalse(etagBeforeUpdate.equals(acl.getEtag()));
		ras = acl.getResourceAccess();
		assertEquals(1, ras.size());
		ResourceAccess raClone = ras.iterator().next();
		assertEquals(ra.getPrincipalId(), raClone.getPrincipalId());
		// TODO assertEquals(ra.getDisplayName(), raClone.getDisplayName());
		assertEquals(ra.getAccessType(), raClone.getAccessType());
		aclList.add(acl);
	}

	@After
	public void tearDown() throws Exception {
		for (AccessControlList acl : aclList){
			aclDAO.delete(acl.getId(), ObjectType.ENTITY);
		}
		for (Node n : nodeList) {
			nodeDAO.delete(n.getId());
		}
		nodeList.clear();
		aclList.clear();
		for (UserGroup g : groupList) {
			userGroupDAO.delete(g.getId());
		}
		groupList.clear();
	}

	/**
	 * Test method for {@link org.sagebionetworks.repo.model.dbo.dao.DBOAccessControlListDaoImpl#getForResource(java.lang.String)}.
	 */
	@Test
	public void testGetForResource() throws Exception {
		Node node = nodeList.iterator().next();
		String rid = node.getId();
		AccessControlList acl = aclDAO.get(rid, ObjectType.ENTITY);
		assertEquals(acl, aclList.iterator().next());
	}

	@Test (expected=NotFoundException.class)
	public void testGetForResourceBadID() throws Exception {
		String rid = "-598787";
		AccessControlList acl = aclDAO.get(rid, ObjectType.ENTITY);
		assertNull(acl);
	}
	
	@Test
	public void testNOOP() {
	
	}

	
	/**
	 * Test method for {@link org.sagebionetworks.repo.model.dbo.dao.DBOAccessControlListDaoImpl#canAccess(java.util.Collection, java.lang.String, org.sagebionetworks.repo.model.ACCESS_TYPE)}.
	 */
	@Test
	public void testCanAccess() throws Exception {
		Set<Long> gs = new HashSet<Long>();
		gs.add(Long.parseLong(group.getId()));
		
		// as expressed in 'setUp', 'group' has 'READ' access to 'node'
		assertTrue(aclDAO.canAccess(gs, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ));
		
		// but it doesn't have 'UPDATE' access
		assertFalse(aclDAO.canAccess(gs, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.UPDATE));
		
		// and no other group has been given access
		UserGroup sham = new UserGroup();
		sham.setId("-34876387468764"); // dummy
		gs.clear();
		gs.add(Long.parseLong(sham.getId()));
		assertFalse(aclDAO.canAccess(gs, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ));
	}

	/**
	 * Test method for {@link org.sagebionetworks.repo.model.dbo.dao.DBOAccessControlListDaoImpl#get(java.lang.String, Long)}.
	 */
	@Test
	public void testGet() throws Exception {
		for (AccessControlList acl : aclList) {
			String id = acl.getId();
			AccessControlList acl2 = aclDAO.get(id, ObjectType.ENTITY);
			assertEquals(acl, acl2);
		}
	}

	/**
	 * Test get method with bad ownerId
	 * @throws Exception
	 */
	@Test (expected=NotFoundException.class)
	public void testGetWithBadId() throws Exception {
		AccessControlList acl = aclList.iterator().next();
		String id = acl.getId();
		aclList.remove(acl);
		aclDAO.delete(id, ObjectType.ENTITY);
		aclDAO.get(id, ObjectType.ENTITY);
	}

	/**
	 * This test tests 2 methods:
	 *     get using aclID, and
	 *     getAclId using ownerId and ownerType
	 * @throws Exception
	 */
	@Test
	public void testGetAclId() throws Exception {
		for (AccessControlList acl : aclList) {
			String id = acl.getId();
			// get the AclId using ownerId and ownerType
			Long aclId = aclDAO.getAclId(id, ObjectType.ENTITY);
			// get the acl using the AclId
			AccessControlList acl2 = aclDAO.get(aclId);
			assertEquals(acl, acl2);
		}
	}

	@Test  (expected=NotFoundException.class)
	public void testGetWithBadAclID() throws Exception {
		Long aclId = -598787L;
		aclDAO.get(aclId);
	}

	/**
	 * Test method getOwnerType using AclId
	 * @throws Exception
	 */
	@Test
	public void testGetOwnerType() throws Exception {
		for (AccessControlList acl : aclList) {
			String id = acl.getId();
			// get the AclId using ownerId and ownerType
			Long aclId = aclDAO.getAclId(id, ObjectType.ENTITY);
			assertEquals(ObjectType.ENTITY, aclDAO.getOwnerType(aclId));
		}
	}

	@Test  (expected=NotFoundException.class)
	public void testGetOwnerTypeWithBadAclID() throws Exception {
		Long aclId = -598787L;
		aclDAO.getOwnerType(aclId);
	}

	/**
	 * Test method for {@link org.sagebionetworks.repo.model.dbo.dao.DBOAccessControlListDaoImpl#update(org.sagebionetworks.repo.model.Base)}.
	 */
	@Test
	public void testUpdate() throws Exception {
		Node node = nodeList.iterator().next();
		String rid = node.getId();
		AccessControlList acl = aclDAO.get(rid, ObjectType.ENTITY);
		Set<ResourceAccess> ras = acl.getResourceAccess();
		ResourceAccess ra = ras.iterator().next();
		ra.setAccessType(new HashSet<ACCESS_TYPE>(
				Arrays.asList(new ACCESS_TYPE[]{
						ACCESS_TYPE.UPDATE,
						ACCESS_TYPE.CREATE
				})));
		String etagBeforeUpdate = acl.getEtag();
		aclDAO.update(acl, ObjectType.ENTITY);
		
		Set<Long> gs = new HashSet<Long>();
		gs.add(Long.parseLong(group.getId()));
		assertFalse(aclDAO.canAccess(gs, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ));
		assertTrue(aclDAO.canAccess(gs, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.UPDATE));
		assertTrue(aclDAO.canAccess(gs, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.CREATE));

		AccessControlList acl2 = aclDAO.get(rid, ObjectType.ENTITY);
		assertFalse(etagBeforeUpdate.equals(acl2.getEtag()));

		try {
			acl2.setEtag("someFakeEtag");
			aclDAO.update(acl2, ObjectType.ENTITY);
		} catch (ConflictingUpdateException e) {
			// Expected
			assertTrue(true);
		}
	}
	
	
	@Test
	public void testUpdateMultipleGroups() throws Exception {
		Node node = nodeList.iterator().next();
		String rid = node.getId();
		AccessControlList acl = aclDAO.get(rid, ObjectType.ENTITY);
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
		aclDAO.update(acl, ObjectType.ENTITY);
		
		Set<Long> gs = new HashSet<Long>();
		gs.add(Long.parseLong(group.getId()));
		Set<Long> gs2 = new HashSet<Long>();
		gs2.add(Long.parseLong(group2.getId()));
		assertFalse(aclDAO.canAccess(gs, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ));
		assertTrue(aclDAO.canAccess(gs2, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ));
		
		// Group one can do this but 2 cannot.
		assertTrue(aclDAO.canAccess(gs, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.UPDATE));
		assertTrue(aclDAO.canAccess(gs, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.CREATE));
		// Now try 2
		assertFalse(aclDAO.canAccess(gs2, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.UPDATE));
		assertFalse(aclDAO.canAccess(gs2, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.CREATE));
		
	}

}
