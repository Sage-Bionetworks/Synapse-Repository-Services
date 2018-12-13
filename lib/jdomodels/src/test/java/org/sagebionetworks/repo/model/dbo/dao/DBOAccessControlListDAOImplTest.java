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
	 * Test method for {@link org.sagebionetworks.repo.model.dbo.dao.DBOAccessControlListDaoImpl#get(java.lang.String, org.sagebionetworks.repo.model.ObjectType)}.
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
	 * Test method for {@link org.sagebionetworks.repo.model.dbo.dao.DBOAccessControlListDaoImpl#canAccess(java.util.Set, java.lang.String, org.sagebionetworks.repo.model.ObjectType, org.sagebionetworks.repo.model.ACCESS_TYPE)}.
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
	
	@Test
	public void testCanAccessMultiple() throws Exception {
		Set<Long> gs = new HashSet<Long>();
		gs.add(Long.parseLong(group.getId()));
		
		Long nodeId = KeyFactory.stringToKey(node.getId());
		Set<Long> benefactors = Sets.newHashSet(nodeId, new Long(-1));
		
		Set<Long> results = aclDAO.getAccessibleBenefactors(gs, benefactors, ObjectType.ENTITY, ACCESS_TYPE.READ);
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
		
		Set<Long> results = aclDAO.getAccessibleBenefactors(gs, benefactors, ObjectType.ENTITY, ACCESS_TYPE.READ);
		assertNotNull(results);
		assertTrue(results.isEmpty());
	}
	
	@Test
	public void testCanAccessMultipleBenefactorsEmpty() throws Exception {
		Set<Long> gs = new HashSet<Long>();
		gs.add(Long.parseLong(group.getId()));
		
		// there should be no matches in this set.
		Set<Long> benefactors = Sets.newHashSet();
		
		Set<Long> results = aclDAO.getAccessibleBenefactors(gs, benefactors, ObjectType.ENTITY, ACCESS_TYPE.READ);
		assertNotNull(results);
		assertTrue(results.isEmpty());
	}
	
	@Test
	public void testCanAccessMultipleGroupsEmptyEmpty() throws Exception {
		Set<Long> gs = new HashSet<Long>();
		// there should be no matches in this set.
		Set<Long> benefactors = Sets.newHashSet(new Long(-2), new Long(-1));
		
		Set<Long> results = aclDAO.getAccessibleBenefactors(gs, benefactors, ObjectType.ENTITY, ACCESS_TYPE.READ);
		assertNotNull(results);
		assertTrue(results.isEmpty());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCanAccessMultipleGroupsNull() throws Exception {
		Set<Long> gs = null;
		Set<Long> benefactors = Sets.newHashSet(new Long(-2), new Long(-1));
		// call under test
		aclDAO.getAccessibleBenefactors(gs, benefactors, ObjectType.ENTITY, ACCESS_TYPE.READ);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCanAccessMultipleBenefactorsNull() throws Exception {
		Set<Long> gs = new HashSet<Long>();
		gs.add(Long.parseLong(group.getId()));
		Set<Long> benefactors = null;
		// call under test
		aclDAO.getAccessibleBenefactors(gs, benefactors, ObjectType.ENTITY, ACCESS_TYPE.READ);
	}

	/**
	 * Test method for {@link org.sagebionetworks.repo.model.dbo.dao.DBOAccessControlListDaoImpl#get(java.lang.String, ObjectType)}.
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
	public void testGetAclIdNotExists() throws Exception {
		Long aclId = -598787L;
		aclDAO.get(aclId);
	}
	
	////////////////////
	//getAclIds() tests
	////////////////////
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetAclIdsNullList(){
		aclDAO.getAclIds(null, ObjectType.ENTITY);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetAclIdsNullObjectType(){
		aclDAO.getAclIds(new ArrayList<Long>(), null);
	}
	
	@Test 
	public void testGetAclIdsEmptyNodeIdsList(){
		List<Long> aclIds = aclDAO.getAclIds(new ArrayList<Long>(), ObjectType.ENTITY);
		assertNotNull(aclIds);
		assertTrue(aclIds.isEmpty());
	}
	
	
	@Test
	public void testGetAclIds(){
		List<Long> nodeIds = new ArrayList<Long>(); 
		for(Node node : nodeList){
			nodeIds.add(KeyFactory.stringToKey(node.getId()));
		}
		List<Long> aclIds = aclDAO.getAclIds(nodeIds, ObjectType.ENTITY);
		assertEquals(nodeIds.size(), aclIds.size());
		for(Long aclId : aclIds){
			AccessControlList acl2 = aclDAO.get(aclId);
			assertTrue(aclList.contains(acl2));
		}
	}
	
	@Test
	public void testGetAclIdsNotExist(){
		List<Long> badList = new ArrayList<Long>();
		badList.add(123L);
		badList.add(456L);
		List<Long> aclIDs = aclDAO.getAclIds(badList, ObjectType.ENTITY);
		assertEquals(0, aclIDs.size());
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
	 * Test method for {@link org.sagebionetworks.repo.model.dbo.dao.DBOAccessControlListDaoImpl#update(org.sagebionetworks.repo.model.AccessControlList, org.sagebionetworks.repo.model.ObjectType)}.
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
	
	@Test
	public void testDeleteList(){
		//setup
		List<Long> nodeIds = new ArrayList<Long>();
		for(Node node: nodeList){
			nodeIds.add(KeyFactory.stringToKey(node.getId()));
		}
		
		//delete the ACLs
		aclDAO.delete(nodeIds, ObjectType.ENTITY);
		
		//check that the ACLs were indeed deleted
		for(Long nodeId: nodeIds){
			try{
				aclDAO.get(KeyFactory.keyToString(nodeId), ObjectType.ENTITY);
				//should not get past here and throw exception ACL was deleted 
				fail();
			}catch (NotFoundException e){
				//expected
			}
		}
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetAllUserGroupsWithNullObjectId(){
		aclDAO.getPrincipalIds(null, ObjectType.ENTITY, ACCESS_TYPE.READ);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetAllUserGroupsWithNullObjectType(){
		aclDAO.getPrincipalIds(node.getId(), null, ACCESS_TYPE.READ);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetAllUserGroupsWithNullAccessType(){
		aclDAO.getPrincipalIds(node.getId(), ObjectType.ENTITY, null);
	}

	@Test
	public void testGetAllUserGroups(){
		AccessControlList acl = aclDAO.get(node.getId(), ObjectType.ENTITY);
		assertNotNull(acl);
		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(Long.parseLong(group.getId()));
		ra.setAccessType(new HashSet<ACCESS_TYPE>(
				Arrays.asList(new ACCESS_TYPE[]{
						ACCESS_TYPE.READ
				})));
		assertTrue(acl.getResourceAccess().contains(ra));
		Set<String> actual = aclDAO.getPrincipalIds(node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ);
		assertTrue(actual.contains(group.getId().toString()));
	}

	@Test
	public void testGetAllUserGroupsForNoAcl(){
		assertEquals(new HashSet<String>(), aclDAO.getPrincipalIds("-1", ObjectType.ENTITY, ACCESS_TYPE.READ));
	}
	
	@Test
	public void testGetAccessibleProjectIds(){
		Set<Long> principalIs = Sets.newHashSet(Long.parseLong(group.getId()), Long.parseLong(group.getId()));
		Set<Long> projectIds = aclDAO.getAccessibleProjectIds(principalIs,  ACCESS_TYPE.READ);
		assertNotNull(projectIds);
		assertEquals(1, projectIds.size());
		assertTrue(projectIds.contains(KeyFactory.stringToKey(node.getId())));
	}
	
	@Test
	public void testGetAccessibleProjectIdsNotAccessible(){
		// principals that does not exist
		Set<Long> principalIs = Sets.newHashSet(-123L);
		Set<Long> projectIds = aclDAO.getAccessibleProjectIds(principalIs,  ACCESS_TYPE.READ);
		assertNotNull(projectIds);
		assertTrue(projectIds.isEmpty());
	}
	
	@Test
	public void testGetAccessibleProjectIdsNonProject(){
		// create a resource on which to apply permissions
		Node folder = new Node();
		folder.setName("foo");
		folder.setCreatedOn(new Date());
		folder.setCreatedByPrincipalId(createdById);
		folder.setModifiedOn(new Date());
		folder.setModifiedByPrincipalId(modifiedById);
		folder.setNodeType(EntityType.folder);
		folder = nodeDAO.createNewNode(folder);
		nodeList.add(folder);
		
		UserGroup ug = new UserGroup();
		ug.setIsIndividual(false);
		ug.setId(userGroupDAO.create(ug).toString());
		assertNotNull(ug.getId());
		groupList.add(ug);
		
		// Create an ACL for this node
		AccessControlList acl = new AccessControlList();
		acl.setId(folder.getId());
		acl.setCreationDate(new Date(System.currentTimeMillis()));
		acl.setResourceAccess(new HashSet<ResourceAccess>());
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
		createAcl(acl, ObjectType.ENTITY);
		assertNotNull(acl);
		
		Set<Long> principalIs = Sets.newHashSet(Long.parseLong(ug.getId()));
		Set<Long> projectIds = aclDAO.getAccessibleProjectIds(principalIs,  ACCESS_TYPE.READ);
		assertNotNull(projectIds);
		assertTrue(projectIds.isEmpty());
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
		Set<Long> results = aclDAO.getNonVisibleChilrenOfEntity(Sets.newHashSet(userOne.getId()), parentId);
		assertNotNull(results);
		assertEquals(1, results.size());
		assertTrue(results.contains(KeyFactory.stringToKey(visibleToTwo.getId())));
		// two cannot see one
		results = aclDAO.getNonVisibleChilrenOfEntity(Sets.newHashSet(userTwo.getId()), parentId);
		assertNotNull(results);
		assertEquals(1, results.size());
		assertTrue(results.contains(KeyFactory.stringToKey(visibleToOne.getId())));
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
	public void testGetChildrenEntitiesWithAcls(){
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
		Long parentIdLong = KeyFactory.stringToKey(parentId);
		// one cannot see two
		List<Long> results = aclDAO.getChildrenEntitiesWithAcls(Lists.newArrayList(parentIdLong));
		assertNotNull(results);
		assertEquals(2, results.size());
		assertTrue(results.contains(KeyFactory.stringToKey(visibleToOne.getId())));
		assertTrue(results.contains(KeyFactory.stringToKey(visibleToTwo.getId())));
		assertFalse(results.contains(KeyFactory.stringToKey(visibleToBoth.getId())));
	}
	
	@Test
	public void testGetChildrenEntitiesWithAclsEmpty(){
		// empty list should return an empty list.s
		List<Long> results = aclDAO.getChildrenEntitiesWithAcls(new LinkedList<Long>());
		assertNotNull(results);
		assertTrue(results.isEmpty());
	}
}
