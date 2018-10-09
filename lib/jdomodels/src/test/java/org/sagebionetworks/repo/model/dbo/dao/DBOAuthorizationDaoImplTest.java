package org.sagebionetworks.repo.model.dbo.dao;

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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author bhoff
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOAuthorizationDaoImplTest {
	
	@Autowired
	private AccessControlListDAO aclDAO;
	
	@Autowired
	private AuthorizationDAO authorizationDAO;

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
		node.setNodeType(EntityType.project);
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
	 * Test method for {@link org.sagebionetworks.repo.model.dbo.dao.DBOAccessControlListDaoImpl#canAccess(java.util.Set, java.lang.String, org.sagebionetworks.repo.model.ObjectType, org.sagebionetworks.repo.model.ACCESS_TYPE)}.
	 */
	@Test
	public void testCanAccess() throws Exception {
		Set<Long> gs = new HashSet<Long>();
		gs.add(Long.parseLong(group.getId()));
		
		// as expressed in 'setUp', 'group' has 'READ' access to 'node'
		assertTrue(authorizationDAO.canAccess(gs, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ));
		
		// but it doesn't have 'UPDATE' access
		assertFalse(authorizationDAO.canAccess(gs, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.UPDATE));
		
		// and no other group has been given access
		UserGroup sham = new UserGroup();
		sham.setId("-34876387468764"); // dummy
		gs.clear();
		gs.add(Long.parseLong(sham.getId()));
		assertFalse(authorizationDAO.canAccess(gs, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ));
	}
	
	@Test
	public void testCanAccessMultiple() throws Exception {
		Set<Long> gs = new HashSet<Long>();
		gs.add(Long.parseLong(group.getId()));
		
		Long nodeId = KeyFactory.stringToKey(node.getId());
		Set<Long> benefactors = Sets.newHashSet(nodeId, new Long(-1));
		
		Set<Long> results = authorizationDAO.getAccessibleBenefactors(gs, benefactors, ObjectType.ENTITY, ACCESS_TYPE.READ);
		assertNotNull(results);
		assertEquals(1, results.size());
		assertTrue(results.contains(nodeId));
		assertFalse(results.contains(new Long(-1)));
	}
	
	@Test
	public void testCanAccessMultipleNoMatch() throws Exception {
		Set<Long> gs = new HashSet<Long>();
		gs.add(Long.parseLong(group.getId()));
		
		// there should be no matches in this set.
		Set<Long> benefactors = Sets.newHashSet(new Long(-2), new Long(-1));
		
		Set<Long> results = authorizationDAO.getAccessibleBenefactors(gs, benefactors, ObjectType.ENTITY, ACCESS_TYPE.READ);
		assertNotNull(results);
		assertTrue(results.isEmpty());
	}
	
	@Test
	public void testCanAccessMultipleBenefactorsEmpty() throws Exception {
		Set<Long> gs = new HashSet<Long>();
		gs.add(Long.parseLong(group.getId()));
		
		// there should be no matches in this set.
		Set<Long> benefactors = Sets.newHashSet();
		
		Set<Long> results = authorizationDAO.getAccessibleBenefactors(gs, benefactors, ObjectType.ENTITY, ACCESS_TYPE.READ);
		assertNotNull(results);
		assertTrue(results.isEmpty());
	}
	
	@Test
	public void testCanAccessMultipleGroupsEmptyEmpty() throws Exception {
		Set<Long> gs = new HashSet<Long>();
		// there should be no matches in this set.
		Set<Long> benefactors = Sets.newHashSet(new Long(-2), new Long(-1));
		
		Set<Long> results = authorizationDAO.getAccessibleBenefactors(gs, benefactors, ObjectType.ENTITY, ACCESS_TYPE.READ);
		assertNotNull(results);
		assertTrue(results.isEmpty());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCanAccessMultipleGroupsNull() throws Exception {
		Set<Long> gs = null;
		Set<Long> benefactors = Sets.newHashSet(new Long(-2), new Long(-1));
		// call under test
		authorizationDAO.getAccessibleBenefactors(gs, benefactors, ObjectType.ENTITY, ACCESS_TYPE.READ);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCanAccessMultipleBenefactorsNull() throws Exception {
		Set<Long> gs = new HashSet<Long>();
		gs.add(Long.parseLong(group.getId()));
		Set<Long> benefactors = null;
		// call under test
	}

	/**
	 * Helper to create an ACL
	 * @param acl
	 * @param type
	 * @return
	 */
	public String createAcl(AccessControlList acl, ObjectType type) {
		String id = aclDAO.create(acl, type);
		acl = aclDAO.get(id, type);
		aclList.add(acl);
		return id;
	}
	
	@Test
	public void testGetNonVisibleChildrenOfEntity(){
		// add three children to the project
		Node visibleToBoth = nodeDAO.createNewNode(NodeTestUtils.createNewFolder("visibleToBoth", createdById, modifiedById, node.getId()));
		Node visibleToOne = nodeDAO.createNewNode(NodeTestUtils.createNewFolder("visibleToOne", createdById, modifiedById, node.getId()));
		Node visibleToTwo = nodeDAO.createNewNode(NodeTestUtils.createNewFolder("visibleToTwo", createdById, modifiedById, node.getId()));
		
		UserInfo userOne = new UserInfo(false, group.getId());
		UserInfo userTwo = new UserInfo(false, group2.getId());
		
		AccessControlList acl1 = AccessControlListUtil.createACLToGrantEntityAdminAccess(visibleToOne.getId(), userOne, new Date());
		createAcl(acl1, ObjectType.ENTITY);
		AccessControlList acl2 = AccessControlListUtil.createACLToGrantEntityAdminAccess(visibleToTwo.getId(), userTwo, new Date());
		createAcl(acl2, ObjectType.ENTITY);
		
		String parentId = node.getId();
		// one cannot see two
		Set<Long> results = authorizationDAO.getNonVisibleChildrenOfEntity(Sets.newHashSet(userOne.getId()), parentId);
		assertNotNull(results);
		assertEquals(1, results.size());
		assertTrue(results.contains(KeyFactory.stringToKey(visibleToTwo.getId())));
		// two cannot see one
		results = authorizationDAO.getNonVisibleChildrenOfEntity(Sets.newHashSet(userTwo.getId()), parentId);
		assertNotNull(results);
		assertEquals(1, results.size());
		assertTrue(results.contains(KeyFactory.stringToKey(visibleToOne.getId())));
	}
	
}
