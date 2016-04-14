package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.sagebionetworks.repo.model.dbo.dao.NodeDAOImpl.TRASH_FOLDER_ID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.ActivityDAO;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.EntityTypeUtils;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.NodeHierarchy;
import org.sagebionetworks.repo.model.NodeInheritanceDAO;
import org.sagebionetworks.repo.model.NodeParentRelation;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ProjectHeader;
import org.sagebionetworks.repo.model.ProjectListSortColumn;
import org.sagebionetworks.repo.model.ProjectListType;
import org.sagebionetworks.repo.model.ProjectStat;
import org.sagebionetworks.repo.model.ProjectStatsDAO;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.VersionInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.entity.query.SortDirection;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.Callback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class NodeDAOImplTest {

	public static final long TEST_FILE_SIZE = 1234567l;

	@Autowired
	private NodeDAO nodeDao;

	@Autowired
	private NodeInheritanceDAO nodeInheritanceDAO;
	
	@Autowired
	private AccessControlListDAO accessControlListDAO;
	
	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private ActivityDAO activityDAO;
	
	@Autowired
	private FileHandleDao fileHandleDao;

	@Autowired
	private UserGroupDAO userGroupDAO;

	@Autowired
	private GroupMembersDAO groupMembersDAO;

	@Autowired
	private TeamDAO teamDAO;

	@Autowired
	private ProjectStatsDAO projectStatsDAO;
	
	@Autowired
	private PlatformTransactionManager txManager;
	
	private TransactionTemplate transactionTemplate;

	// the datasets that must be deleted at the end of each test.
	List<String> toDelete = new ArrayList<String>();
	List<String> activitiesToDelete = new ArrayList<String>();
	List<String> fileHandlesToDelete = new ArrayList<String>();
	private List<String> userGroupsToDelete = Lists.newArrayList();
	
	private Long creatorUserGroupId;	
	private Long altUserGroupId;
	private Activity testActivity = null;
	private Activity testActivity2 = null;
	
	private S3FileHandle fileHandle = null;
	private S3FileHandle fileHandle2 = null;

	private String user1;
	private String user2;
	private String user3;
	private String group;

	private String owned;

	private String participate;

	private String groupParticipate;

	private String subFolderProject;

	private String subFolderProject2;

	private String publicProject;

	private String trashed;

	private String publicProject2;

	private String nooneOwns;

	@Before
	public void before() throws Exception {
		creatorUserGroupId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		altUserGroupId = BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId();
		
		assertNotNull(nodeDao);
		assertNotNull(nodeInheritanceDAO);
		toDelete = new ArrayList<String>();
		
		testActivity = createTestActivity(new Random().nextLong());
		testActivity2 = createTestActivity(new Random().nextLong());
		
		// Create file handles that can be used in tests
		fileHandle = createTestFileHandle("One", creatorUserGroupId.toString());
		fileHandle2 = createTestFileHandle("Two", creatorUserGroupId.toString());

		UserGroup user = new UserGroup();
		user.setIsIndividual(true);
		user1 = userGroupDAO.create(user).toString();
		userGroupsToDelete.add(user1);

		user2 = userGroupDAO.create(user).toString();
		userGroupsToDelete.add(user2);

		user3 = userGroupDAO.create(user).toString();
		userGroupsToDelete.add(user3);

		user.setIsIndividual(false);
		group = userGroupDAO.create(user).toString();
		userGroupsToDelete.add(group);
		Team team = new Team();
		team.setName("team-" + new Random().nextInt());
		team.setId(group);
		teamDAO.create(team);

		groupMembersDAO.addMembers(group, Lists.newArrayList(user1));
		groupMembersDAO.addMembers(group, Lists.newArrayList(user3));
		
		transactionTemplate = new TransactionTemplate(txManager);
	}
	
	@After
	public void after() throws Exception {
		if (group != null) {
			teamDAO.delete(group);
		}
		if(toDelete != null && nodeDao != null){
			for(String id:  toDelete){
				// Delete each
				try{
					nodeDao.delete(id);
				}catch (NotFoundException e) {
					// happens if the object no longer exists.
				}
			}
		}
		if(activitiesToDelete != null & activityDAO != null) {
			for(String id : activitiesToDelete) {
				activityDAO.delete(id);
			}
		}
		if(fileHandleDao != null && fileHandle != null){
			for(String id : fileHandlesToDelete) {
				fileHandleDao.delete(id);
			}
		}
		for (String todelete : Lists.reverse(userGroupsToDelete)) {
			userGroupDAO.delete(todelete);
		}
	}
	
	private Node privateCreateNew(String name) {
		return NodeTestUtils.createNew(name, creatorUserGroupId);
	}
	
	private Node privateCreateNewDistinctModifier(String name) {
		return NodeTestUtils.createNew(name, creatorUserGroupId, altUserGroupId);
	}
	
	/**
	 * Helper method to create a node with multiple versions.
	 * @param numberOfVersions
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public String createNodeWithMultipleVersions(int numberOfVersions) throws Exception {
		Node node = privateCreateNew("createNodeWithMultipleVersions");
		// Start this node with version and comment information
		node.setVersionComment("This is the very first version of this node.");
		node.setVersionLabel("0.0.0");
		String id = nodeDao.createNew(node);
		toDelete.add(id);
		assertNotNull(id);
		
		// this is the number of versions to create
		for(int i=1; i<numberOfVersions; i++){
			Node current = nodeDao.getNode(id);
			current.setVersionComment("Comment "+i);
			current.setVersionLabel("0.0."+i);
			current.setFileHandleId(fileHandle.getId());
			nodeDao.createNewVersion(current);
		}
		return id;
	}
	
	@Test 
	public void testCreateNode() throws Exception{
		Node toCreate = privateCreateNewDistinctModifier("firstNodeEver");
		toCreate.setVersionComment("This is the first version of the first node ever!");
		toCreate.setVersionLabel("0.0.1");		
		toCreate.setActivityId(testActivity.getId());
		long initialCount = nodeDao.getCount();
		String id = nodeDao.createNew(toCreate);
		assertEquals(1+initialCount, nodeDao.getCount()); // piggy-back checking count on top of other tests :^)
		toDelete.add(id);
		assertNotNull(id);
		// This node should exist
		assertTrue(nodeDao.doesNodeExist(KeyFactory.stringToKey(id)));
		// Make sure we can fetch it
		Node loaded = nodeDao.getNode(id);
		assertNotNull(id);
		assertEquals(id, loaded.getId());
		assertNotNull(loaded.getETag());
		assertTrue(nodeDao.doesNodeRevisionExist(id, loaded.getVersionNumber()));
		assertFalse(nodeDao.doesNodeRevisionExist(id, loaded.getVersionNumber()+1));
		// All new nodes should start off as the first version.
		assertEquals(new Long(1),loaded.getVersionNumber());
		assertEquals(toCreate.getVersionComment(), loaded.getVersionComment());
		assertEquals(toCreate.getVersionLabel(), loaded.getVersionLabel());
		assertEquals(testActivity.getId(), loaded.getActivityId());
		
		// Since this node has no parent, it should be its own benefactor.
		String benefactorId = nodeInheritanceDAO.getBenefactor(id);
		assertEquals(id, benefactorId);
	}
	
	@Test
	public void testDoesNodeExistInvalid(){
		// call under test.
		boolean exists = nodeDao.doesNodeExist(-123L);
		assertFalse(exists);
	}
	
	@Test
	public void testDoesNodeExist(){
		Node toCreate = privateCreateNewDistinctModifier("exists");
		String id = nodeDao.createNew(toCreate);
		toDelete.add(id);
		Long nodeId = KeyFactory.stringToKey(id);
		// call under test.
		boolean exists = nodeDao.doesNodeExist(nodeId);
		assertTrue(exists);
	}
	
	@Test
	public void testDoesNodeExistAndInTrash(){
		Node toCreate = privateCreateNewDistinctModifier("exists");
		// put the node in the trash
		String id = nodeDao.createNew(toCreate);
		toDelete.add(id);
		// put the node in the trash
		nodeInheritanceDAO.addBeneficiary(id, ""+TRASH_FOLDER_ID);
		Long nodeId = KeyFactory.stringToKey(id);
		// call under test.
		boolean exists = nodeDao.doesNodeExist(nodeId);
		// the node should still exist even though it is in the trash.
		assertTrue(exists);
	}
	
	@Test
	public void testIsNodeAvailableInvalid(){
		// call under test.
		boolean avaiable = nodeDao.isNodeAvailable(-123L);
		assertFalse(avaiable);
	}
	
	@Test
	public void testIsNodeAvailable(){
		Node toCreate = privateCreateNewDistinctModifier("available");
		String id = nodeDao.createNew(toCreate);
		toDelete.add(id);
		Long nodeId = KeyFactory.stringToKey(id);
		// call under test.
		boolean avaiable = nodeDao.isNodeAvailable(nodeId);
		assertTrue(avaiable);
	}
	
	@Test
	public void testIsNodeAvailableAndInTrash(){
		Node toCreate = privateCreateNewDistinctModifier("available");
		String id = nodeDao.createNew(toCreate);
		toDelete.add(id);
		// put the node in the trash
		nodeInheritanceDAO.addBeneficiary(id, ""+TRASH_FOLDER_ID);
		Long nodeId = KeyFactory.stringToKey(id);
		// call under test.
		boolean avaiable = nodeDao.isNodeAvailable(nodeId);
		// the node should still exist even though it is in the trash.
		assertFalse(avaiable);
		// the node should exist
		assertTrue(nodeDao.doesNodeExist(nodeId));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateWithExistingId() throws Exception{
		Node toCreate = privateCreateNew("secondNodeEver");
		toCreate.setVersionComment("This is the first version of the first node ever!");
		toCreate.setVersionLabel("0.0.1");
		String id = nodeDao.createNew(toCreate);
		toDelete.add(id);
		assertNotNull(id);
		// Now create another node using this id.
		Node duplicate = privateCreateNew("should never exist");
		duplicate.setId(id);
		// This should throw an exception.
		String id2 = nodeDao.createNew(duplicate);
		toDelete.add(id2);
		assertNotNull(id2);;
	}
	
	@Test
	public void testCreateWithDuplicateName() throws Exception{
		String commonName = "name";
		Node parent = privateCreateNew("parent");
		String parentId = nodeDao.createNew(parent);
		toDelete.add(parentId);
		assertNotNull(parentId);
		Node one = privateCreateNew(commonName);
		one.setParentId(parentId);
		String id = nodeDao.createNew(one);
		toDelete.add(id);
		assertNotNull(id);
		// Now create another node using this id.
		Node oneDuplicate = privateCreateNew(commonName);
		oneDuplicate.setParentId(parentId);
		// This should throw an exception.
		try{
			String id2 = nodeDao.createNew(oneDuplicate);
			fail("Setting a duplicate name should have failed");
		}catch(IllegalArgumentException e){
			assertTrue(e.getMessage().indexOf("An entity with the name: name already exists") > -1);
		}
	}
	
	@Test
	public void testCreateWithDuplicateAlias() throws Exception{
		String commonAlias = "alias";
		Node parent = privateCreateNew("parent");
		String parentId = nodeDao.createNew(parent);
		toDelete.add(parentId);
		assertNotNull(parentId);
		Node one = privateCreateNew("name");
		one.setAlias(commonAlias);
		one.setParentId(parentId);
		String id = nodeDao.createNew(one);
		toDelete.add(id);
		assertNotNull(id);
		// Now create another node using this id.
		Node oneDuplicate = privateCreateNew("unique");
		oneDuplicate.setAlias(commonAlias);
		oneDuplicate.setParentId(parentId);
		// This should throw an exception.
		try{
			String id2 = nodeDao.createNew(oneDuplicate);
			fail("Setting a duplicate alias should have failed");
		}catch(IllegalArgumentException e){
			assertTrue(e.getMessage().indexOf("The friendly url name (alias): "+commonAlias+" is already taken.  Please select another.") > -1);

		}
	}
	
	@Test
	public void testCreateWithId() throws Exception{
		// Create a new node with an ID that is beyond the current max of the 
		// ID generator.
		long idLong = idGenerator.generateNewId() + 10;
		String idString = KeyFactory.keyToString(new Long(idLong));
		Node toCreate = privateCreateNew("secondNodeEver");
		toCreate.setId(idString);
		String fetchedId = nodeDao.createNew(toCreate);
		toDelete.add(fetchedId);
		// The id should be the same as what we provided
		assertEquals(idString, fetchedId);
		// Also make sure the ID generator was increment to reserve this ID.
		long nextId = idGenerator.generateNewId();
		assertEquals(idLong+1, nextId);
	}
	
	@Test
	public void testCreateWithIdGreaterThanIdGenerator() throws Exception{
		// Create a node with a specific id
		String id = KeyFactory.keyToString(new Long(idGenerator.generateNewId()+10));
		Node toCreate = privateCreateNew("secondNodeEver");
		toCreate.setId(id);
		String fetchedId = nodeDao.createNew(toCreate);
		toDelete.add(fetchedId);
		// The id should be the same as what we provided
		assertEquals(id, fetchedId);
	}
	
	@Test 
	public void testAddChild() throws Exception {
		Node parent = privateCreateNew("parent");
		String parentId = nodeDao.createNew(parent);
		assertNotNull(parentId);
		toDelete.add(parentId);

		// now add a certain group to the ACL of the parent
		AccessControlList acl = new AccessControlList();
		Set<ResourceAccess> ras = new HashSet<ResourceAccess>();
		ResourceAccess ra = new ResourceAccess();
		ra.setAccessType(new HashSet<ACCESS_TYPE>(Arrays.asList(new ACCESS_TYPE[]{ACCESS_TYPE.READ})));
		ra.setPrincipalId(altUserGroupId);
		ras.add(ra);
		acl.setResourceAccess(ras);
		acl.setId(parentId);
		acl.setCreationDate(new Date());
		accessControlListDAO.create(acl, ObjectType.ENTITY);
		
		
		//Now add an child
		Node child = privateCreateNew("child");
		child.setParentId(parentId);
		String childId = nodeDao.createNew(child);
		assertNotNull(childId);

		Set<Node> children = nodeDao.getChildren(parentId);
		assertNotNull(children);
		assertEquals(1, children.size());
		Node childLoaded = children.iterator().next();
		assertEquals(childId, childLoaded.getId());
		assertEquals(parentId, childLoaded.getParentId());
		// Make sure we can fetch it
		childLoaded = nodeDao.getNode(childId);
		assertNotNull(childLoaded);
		assertEquals(parentId, childLoaded.getParentId());
		// This child should be inheriting from its parent by default
		String childBenefactorId = nodeInheritanceDAO.getBenefactor(childId);
		assertEquals(parentId, childBenefactorId);
		
		// now add a grandchild
		Node grandkid = privateCreateNew("grandchild");
		grandkid.setParentId(childId);
		String grandkidId = nodeDao.createNew(grandkid);
		assertNotNull(grandkidId);

		// This grandchild should be inheriting from its grandparent by default
		String grandChildBenefactorId = nodeInheritanceDAO.getBenefactor(grandkidId);
		assertEquals(parentId, grandChildBenefactorId);
		
		// Now delete the parent and confirm the child,grandkid are gone too
		nodeDao.delete(parentId);
		// the child should no longer exist
		try{
			childLoaded = nodeDao.getNode(childId);
			fail("The child should not exist after the parent was deleted");
		}catch (NotFoundException e){
			// expected.
		}
		try{
			nodeDao.getNode(grandkidId);
			fail("The grandchild should not exist after the grandparent was deleted");
		}catch (NotFoundException e){
			// expected.
		}
	}

	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateUniqueChildNames() throws Exception {
		Node parent = privateCreateNew("parent");
		String parentId = nodeDao.createNew(parent);
		assertNotNull(parentId);
		toDelete.add(parentId);
		
		//Now add an child
		Node child = privateCreateNew("child");
		child.setParentId(parentId);
		String childId = nodeDao.createNew(child);
		assertNotNull(childId);
		toDelete.add(childId);
		
		// We should not be able to create a child with the same name
		child = privateCreateNew("child");
		child.setParentId(parentId);
		String child2Id = nodeDao.createNew(child);
		assertNotNull(child2Id);
		toDelete.add(child2Id);
	}

	@Ignore  // This is not working because the exception is thrown when the transaction is committed, not when we change it.
	@Test (expected=IllegalArgumentException.class)
	public void testUpdateUniqueChildNames() throws Exception {
		Node parent = privateCreateNew("parent");
		String parentId = nodeDao.createNew(parent);
		assertNotNull(parentId);
		toDelete.add(parentId);
		
		//Now add an child
		Node child = privateCreateNew("child");
		child.setParentId(parentId);
		String childId = nodeDao.createNew(child);
		assertNotNull(childId);
		toDelete.add(childId);
		
		// We should not be able to create a child with the same name
		child = privateCreateNew("child2");
		child.setParentId(parentId);
		String child2Id = nodeDao.createNew(child);
		assertNotNull(child2Id);
		toDelete.add(child2Id);
		
		// Now try to change child2's name to 'child' which should fail
		child = nodeDao.getNode(child2Id);
		child.setName("child");
		nodeDao.updateNode(child);
	}
	
	@Test
	public void testGetPath() throws Exception {
		Node parent = privateCreateNew("parent");
		String parentId = nodeDao.createNew(parent);
		assertNotNull(parentId);
		toDelete.add(parentId);
		
		//Now add an child
		Node child = privateCreateNew("child");
		child.setParentId(parentId);
		String childId = nodeDao.createNew(child);
		assertNotNull(childId);
		toDelete.add(parentId);
		
		//Now add an child
		Node grandChild = privateCreateNew("grandChild");
		grandChild.setParentId(childId);
		String grandChildId = nodeDao.createNew(grandChild);
		assertNotNull(grandChildId);
		toDelete.add(grandChildId);
		
		// Now make sure we can get all three nodes using their path
		String path = "/parent";
		String id = nodeDao.getNodeIdForPath(path);
		assertEquals(parentId, id);
		// Get the child
		path = "/parent/child";
		id = nodeDao.getNodeIdForPath(path);
		assertEquals(childId, id);
		// Get the grand child
		path = "/parent/child/grandChild";
		id = nodeDao.getNodeIdForPath(path);
		assertEquals(grandChildId, id);
	}
	
	
	@Test
	public void testGetPathDoesNotExist() throws Exception {
		// Make sure we get null for a path that does not exist.
		String path = "/fake/should/not/eixst";
		String id = nodeDao.getNodeIdForPath(path);
		assertEquals(null, id);
	}
	
 	// Calling getETagForUpdate() outside of a transaction in not allowed, and will throw an exception.
	@Test(expected=IllegalTransactionStateException.class)
	public void testGetETagForUpdate1() throws Exception {
		Node toCreate = privateCreateNew("testGetETagForUpdate");
		String id = nodeDao.createNew(toCreate);
		toDelete.add(id);
		assertNotNull(id);
		String eTag = nodeDao.peekCurrentEtag(id);
		eTag = nodeDao.lockNodeAndIncrementEtag(id, eTag);
		fail("Should have thrown an IllegalTransactionStateException");
	}

 	// Calling getETagForUpdate() outside of a transaction in not allowed, and will throw an exception.
	@Test(expected=IllegalTransactionStateException.class)
	public void testGetETagForUpdate2() throws Exception {
		Node toCreate = privateCreateNew("testGetETagForUpdate");
		String id = nodeDao.createNew(toCreate);
		toDelete.add(id);
		assertNotNull(id);
		String eTag = nodeDao.peekCurrentEtag(id);
		eTag = nodeDao.lockNodeAndIncrementEtag(id, eTag, ChangeType.DELETE);
		fail("Should have thrown an IllegalTransactionStateException");
	}
	
	@Test(expected=IllegalTransactionStateException.class)
	public void testLockNodesNoTransaction(){
		// must call must be made from within a transaction.
		nodeDao.lockNodes(new LinkedList<String>());
	}
	
	@Test
	public void testLockNodes(){
		// one
		Node toCreate = privateCreateNew("testOne");
		String id = nodeDao.createNew(toCreate);
		toDelete.add(id);
		assertNotNull(id);
		final Node one = nodeDao.getNode(id);

		// two
		toCreate = privateCreateNew("testTwo");
		id = nodeDao.createNew(toCreate);
		toDelete.add(id);
		assertNotNull(id);
		final Node two = nodeDao.getNode(id);

		// attempt to lock both within a transaction.
		List<String> lockedEtags = transactionTemplate.execute(new TransactionCallback<List<String>>() {
			@Override
			public List<String> doInTransaction(TransactionStatus status) {
				// Try to lock both nodes out of order
				List<String> etags = nodeDao.lockNodes(Lists.newArrayList(two.getId(), one.getId()));
				return etags;
			}
		});
		assertNotNull(lockedEtags);
		assertEquals(2, lockedEtags.size());
		// The first etag should be first
		assertEquals(one.getETag(), lockedEtags.get(0));
		assertEquals(two.getETag(), lockedEtags.get(1));
	}
	
	@Test
	public void testLockNodesEmpty(){
		// attempt to lock both within a transaction.
		List<String> lockedEtags = transactionTemplate.execute(new TransactionCallback<List<String>>() {
			@Override
			public List<String> doInTransaction(TransactionStatus status) {
				// Empty list should result in an empty return.
				List<String> etags = nodeDao.lockNodes(new LinkedList<String>());
				return etags;
			}
		});
		assertNotNull(lockedEtags);
		assertEquals(0, lockedEtags.size());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testLockNodesEmptyNull(){
		// attempt to lock both within a transaction.
		List<String> lockedEtags = transactionTemplate.execute(new TransactionCallback<List<String>>() {
			@Override
			public List<String> doInTransaction(TransactionStatus status) {
				// Empty list should result in an empty return.
				List<String> etags = nodeDao.lockNodes(null);
				return etags;
			}
		});
		assertNotNull(lockedEtags);
		assertEquals(0, lockedEtags.size());
	}
	

	@Test
	public void testUpdateNode() throws Exception{
		Node node = privateCreateNew("testUpdateNode");
		node.setActivityId(testActivity.getId());
		String id = nodeDao.createNew(node);
		toDelete.add(id);
		assertNotNull(id);
		// Now fetch the node
		Node copy = nodeDao.getNode(id);
		assertNotNull(copy);
		// Now change the copy and push it back
		copy.setName("myNewName");
		copy.setDescription("myNewDescription");
		copy.setActivityId(testActivity2.getId());
		nodeDao.updateNode(copy);
		Node updatedCopy = nodeDao.getNode(id);
		assertNotNull(updatedCopy);
		// The updated copy should match the copy now
		assertEquals(copy, updatedCopy);
	}
	
	@Test
	public void testUpdateNodeDuplicateName() throws Exception{
		String commonName = "name";
		Node parent = privateCreateNew("parent");
		String parentId = nodeDao.createNew(parent);
		toDelete.add(parentId);
		assertNotNull(parentId);
		Node one = privateCreateNew(commonName);
		one.setParentId(parentId);
		String id = nodeDao.createNew(one);
		toDelete.add(id);
		assertNotNull(id);
		// Now create another node using this id.
		Node oneDuplicate = privateCreateNew("unique");
		oneDuplicate.setParentId(parentId);
		String id2 = nodeDao.createNew(oneDuplicate);
		oneDuplicate = nodeDao.getNode(id2);
		// This should throw an exception.
		try{
			// Set this name to be a duplicate name.
			oneDuplicate.setName(commonName);
			// Now update this node
			nodeDao.updateNode(oneDuplicate);
			fail("Setting a duplicate name should have failed");
		}catch(IllegalArgumentException e){
			assertTrue(e.getMessage().indexOf("An entity with the name: name already exists") > -1);
		}
	}
	
	@Test
	public void testUpdateNodeDuplicateAlias() throws Exception{
		String commonAlias = "alias";
		Node parent = privateCreateNew("parent");
		String parentId = nodeDao.createNew(parent);
		toDelete.add(parentId);
		assertNotNull(parentId);
		Node one = privateCreateNew("child");
		one.setAlias(commonAlias);
		one.setParentId(parentId);
		String id = nodeDao.createNew(one);
		toDelete.add(id);
		assertNotNull(id);
		// Now create another node using this id.
		Node oneDuplicate = privateCreateNew("unique");
		oneDuplicate.setParentId(parentId);
		String id2 = nodeDao.createNew(oneDuplicate);
		oneDuplicate = nodeDao.getNode(id2);
		// This should throw an exception.
		try{
			// Set this name to be a duplicate name.
			oneDuplicate.setAlias(commonAlias);
			// Now update this node
			nodeDao.updateNode(oneDuplicate);
			fail("Setting a duplicate alias should have failed");
		}catch(IllegalArgumentException e){
			assertTrue(e.getMessage().indexOf("The friendly url name (alias): "+commonAlias+" is already taken.  Please select another.") > -1);
		}
	}
	
	@Test(expected=Exception.class)
	public void testNullName() throws Exception{
		Node node = privateCreateNew("setNameNull");
		node.setName(null);
		String id = nodeDao.createNew(node);
		toDelete.add(id);
	}
	
	@Test
	public void testCreateAllAnnotationsTypes() throws Exception{
		Node node = privateCreateNew("testCreateAnnotations");
		String id = nodeDao.createNew(node);
		toDelete.add(id);
		assertNotNull(id);
		// Now get the annotations for this node.
		NamedAnnotations named = nodeDao.getAnnotations(id);
		Annotations annos = named.getAdditionalAnnotations();
		assertNotNull(annos);
		assertNotNull(annos.getEtag());
		assertEquals(node.getCreatedByPrincipalId(), named.getCreatedBy());
		assertEquals(id, named.getId());
		// Now add some annotations to this node.
		annos.addAnnotation("stringOne", "one");
		annos.addAnnotation("doubleKey", new Double(23.5));
		annos.addAnnotation("longKey", new Long(1234));
		annos.addAnnotation("blobKey", "StringToBlob".getBytes("UTF-8"));
		byte[] bigBlob = new byte[6000];
		Arrays.fill(bigBlob, (byte)0xa3);
		annos.addAnnotation("bigBlob", bigBlob);
		annos.addAnnotation("dateKey", new Date(System.currentTimeMillis()));
		// update the eTag
		String newETagString = UUID.randomUUID().toString();
		annos.setEtag(newETagString);
		// Update them
		nodeDao.updateAnnotations(id, named);
		// Now get a copy and ensure it equals what we sent
		NamedAnnotations namedCopy = nodeDao.getAnnotations(id);
		Annotations copy = namedCopy.getAdditionalAnnotations();
		assertNotNull(copy);
		assertEquals("one", copy.getSingleValue("stringOne"));
		assertEquals(new Double(23.5), copy.getSingleValue("doubleKey"));
		assertEquals(new Long(1234), copy.getSingleValue("longKey"));
		byte[] blob = (byte[]) copy.getSingleValue("blobKey");
		assertNotNull(blob);
		String blobString = new String(blob, "UTF-8");
		assertEquals("StringToBlob", blobString);
		byte[] bigCopy = (byte[]) copy.getSingleValue("bigBlob");
		assertNotNull(bigCopy);
		assertTrue(Arrays.equals(bigBlob, bigCopy));
	}
	
	@Test
	public void testCreateAnnotations() throws Exception{
		Node node = privateCreateNew("testCreateAnnotations");
		String id = nodeDao.createNew(node);
		toDelete.add(id);
		assertNotNull(id);
		// Now get the annotations for this node.
		NamedAnnotations named = nodeDao.getAnnotations(id);
		Annotations annos = named.getAdditionalAnnotations();
		assertNotNull(annos);
		assertNotNull(annos.getEtag());
		assertNotNull(annos.getBlobAnnotations());
		assertNotNull(annos.getStringAnnotations());
		assertNotNull(annos.getDoubleAnnotations());
		assertNotNull(annos.getLongAnnotations());
		assertNotNull(annos.getDateAnnotations());
		// Now add some annotations to this node.
		annos.addAnnotation("stringOne", "one");
		annos.addAnnotation("doubleKey", new Double(23.5));
		annos.addAnnotation("longKey", new Long(1234));
		// Update them
		nodeDao.updateAnnotations(id, named);
		// Now get a copy and ensure it equals what we sent
		NamedAnnotations namedCopy = nodeDao.getAnnotations(id);
		Annotations copy = namedCopy.getAdditionalAnnotations();
		assertNotNull(copy);
		assertEquals(annos, copy);
		// clear an and update
		assertNotNull(copy.getStringAnnotations().remove("stringOne"));
		nodeDao.updateAnnotations(id, namedCopy);
		NamedAnnotations namedCopy2 = nodeDao.getAnnotations(id);
		Annotations copy2 = namedCopy2.getAdditionalAnnotations();
		assertNotNull(copy2);
		assertEquals(copy, copy2);
		// Make sure the node has a new eTag
		Node nodeCopy = nodeDao.getNode(id);
		assertNotNull(nodeCopy);
	}
	
	@Test
	public void testCreateNewVersion() throws Exception {
		Node node = privateCreateNew("testCreateNewVersion");
		// Start this node with version and comment information
		node.setVersionComment("This is the very first version of this node.");
		node.setVersionLabel("0.0.1");
		node.setVersionNumber(new Long(0));
		node.setActivityId(testActivity.getId());
		String id = nodeDao.createNew(node);
		toDelete.add(id);
		assertNotNull(id);
		// Load the node
		Node loaded = nodeDao.getNode(id);
		assertNotNull(loaded);
		assertEquals(node.getVersionComment(), loaded.getVersionComment());
		assertEquals(node.getVersionLabel(), loaded.getVersionLabel());
		assertEquals(NodeConstants.DEFAULT_VERSION_NUMBER, loaded.getVersionNumber());
		assertEquals(testActivity.getId(), loaded.getActivityId());
		// Now try to create a new version with a duplicate label
		try{
			Long newNumber = nodeDao.createNewVersion(loaded);
			fail("This should have failed due to a duplicate version label");
		}catch(IllegalArgumentException e){
			// Expected
		}
		// Since creation of a new version failed we should be back to one version
		loaded = nodeDao.getNode(id);
		assertNotNull(loaded);
		assertEquals(node.getVersionComment(), loaded.getVersionComment());
		assertEquals(node.getVersionLabel(), loaded.getVersionLabel());
		
		// Now try to create a new revision with new data
		Node newRev = nodeDao.getNode(id);
		newRev.setVersionLabel("0.0.2");
		newRev.setModifiedByPrincipalId(creatorUserGroupId);
		newRev.setModifiedOn(new Date(System.currentTimeMillis()));
		newRev.setActivityId(null);
		Long newNumber = nodeDao.createNewVersion(newRev);
		assertNotNull(newNumber);
		assertEquals(new Long(2), newNumber);
		// Now load the node and check the fields
		loaded = nodeDao.getNode(id);
		assertNotNull(loaded);
		assertEquals(newRev.getVersionComment(), loaded.getVersionComment());
		assertEquals(newRev.getVersionLabel(), loaded.getVersionLabel());
		assertEquals(newRev.getModifiedByPrincipalId(), loaded.getModifiedByPrincipalId());
		assertEquals(null, loaded.getActivityId());
	}
	
	@Test
	public void testCreateNewVersionNullLabel() throws Exception {
		Node node = privateCreateNew("testCreateNewVersion");
		// Start this node with version and comment information
		node.setVersionComment("This is the very first version of this node.");
		node.setVersionLabel("0.0.1");
		node.setVersionNumber(new Long(0));
		String id = nodeDao.createNew(node);
		toDelete.add(id);
		assertNotNull(id);
		// Load the node
		Node loaded = nodeDao.getNode(id);
		assertNotNull(loaded);
		assertEquals(node.getVersionComment(), loaded.getVersionComment());
		assertEquals(node.getVersionLabel(), loaded.getVersionLabel());
		assertEquals(NodeConstants.DEFAULT_VERSION_NUMBER, loaded.getVersionNumber());
		// Now try to create a new version with a null label
		loaded.setVersionLabel(null);
		Long newNumber = nodeDao.createNewVersion(loaded);
		// Since creation of a new version failed we should be back to one version
		loaded = nodeDao.getNode(id);
		assertNotNull(loaded);
		assertEquals(node.getVersionComment(), loaded.getVersionComment());
		assertEquals(newNumber.toString(), loaded.getVersionLabel());
	}
	
	@Test public void testCreateVersionDefaults() throws Exception {
		Node node = privateCreateNew("testCreateNewVersion");
		String id = nodeDao.createNew(node);
		toDelete.add(id);
		assertNotNull(id);
		Node loaded = nodeDao.getNode(id);
		assertNotNull(loaded);
		assertEquals(null, loaded.getVersionComment());
		assertEquals(NodeConstants.DEFAULT_VERSION_LABEL, loaded.getVersionLabel());
		assertEquals(NodeConstants.DEFAULT_VERSION_NUMBER, loaded.getVersionNumber());
	}

	@Test
	public void testNewVersionAnnotations() throws Exception {
		Node node = privateCreateNew("testCreateAnnotations");
		// Start this node with version and comment information
		node.setVersionComment("This is the very first version of this node.");
		node.setVersionLabel("0.0.1");
		String id = nodeDao.createNew(node);
		toDelete.add(id);
		assertNotNull(id);
		NamedAnnotations named = nodeDao.getAnnotations(id);
		Annotations annos = named.getAdditionalAnnotations();
		assertNotNull(annos);
		annos.addAnnotation("string", "value");
		annos.addAnnotation("date", new Date(1));
		annos.addAnnotation("double", 2.3);
		annos.addAnnotation("long", 56l);
		annos.addAnnotation("blob", "Some blob value".getBytes("UTF-8"));
		// Update the annotations
		nodeDao.updateAnnotations(id, named);
		// Now create a new version
		Node copy = nodeDao.getNode(id);
		copy.setVersionComment(null);
		copy.setVersionLabel("1.0.1");
		Long revNumber = nodeDao.createNewVersion(copy);
		assertEquals(new Long(2), revNumber);
		// At this point the new and old version should have the
		// same annotations.
		NamedAnnotations namedCopyV1 = nodeDao.getAnnotationsForVersion(id, 1L);
		assertNotNull(namedCopyV1.getEtag());
		assertEquals(NodeConstants.ZERO_E_TAG, namedCopyV1.getEtag());
		Annotations v1Annos = namedCopyV1.getAdditionalAnnotations();
		assertNotNull(v1Annos);
		assertEquals(NodeConstants.ZERO_E_TAG, v1Annos.getEtag());
		NamedAnnotations namedCopyV2 = nodeDao.getAnnotationsForVersion(id, 2L);
		Annotations v2Annos = namedCopyV2.getAdditionalAnnotations();
		assertNotNull(v2Annos);
		assertEquals(NodeConstants.ZERO_E_TAG, v2Annos.getEtag());
		assertEquals(v1Annos, v2Annos);
		NamedAnnotations namedCopy = nodeDao.getAnnotations(id);
		Annotations currentAnnos = namedCopy.getAdditionalAnnotations();
		assertNotNull(currentAnnos);
		assertNotNull(currentAnnos.getEtag());
		// They should be equal except for the e-tag
		assertFalse(currentAnnos.getEtag().equals(v2Annos.getEtag()));
		v2Annos.setEtag(currentAnnos.getEtag());
		assertEquals(currentAnnos, v2Annos);
		
		// Now update the current annotations
		currentAnnos.getDoubleAnnotations().clear();
		currentAnnos.addAnnotation("double", 8989898.2);
		nodeDao.updateAnnotations(id, namedCopy);
		
		// Now the old and new should no longer match.
		namedCopyV1 = nodeDao.getAnnotationsForVersion(id, 1L);
		assertNotNull(namedCopyV1.getEtag());
		assertEquals(NodeConstants.ZERO_E_TAG, namedCopyV1.getEtag());
		v1Annos = namedCopyV1.getAdditionalAnnotations();
		assertNotNull(v1Annos);
		assertEquals(2.3, v1Annos.getSingleValue("double"));
		namedCopyV2 = nodeDao.getAnnotationsForVersion(id, 2L);
		v2Annos = namedCopyV2.getAdditionalAnnotations();
		assertNotNull(v2Annos);
		assertEquals(NodeConstants.ZERO_E_TAG, v2Annos.getEtag());
		assertEquals(8989898.2, v2Annos.getSingleValue("double"));
		// The two version should now be out of synch with each other.
		assertFalse(v1Annos.equals(v2Annos));
		// The current annos should still match the v2
		namedCopy = nodeDao.getAnnotations(id);
		currentAnnos = namedCopy.getAdditionalAnnotations();
		assertNotNull(currentAnnos);
		assertNotNull(currentAnnos.getEtag());
		// They should be equal except for the e-tag
		assertFalse(currentAnnos.getEtag().equals(v2Annos.getEtag()));
		v2Annos.setEtag(currentAnnos.getEtag());
		assertEquals(currentAnnos, v2Annos);
		assertEquals(8989898.2, currentAnnos.getSingleValue("double"));
		
		// Node delete the current revision and confirm that the annotations are rolled back
		node = nodeDao.getNode(id);
		nodeDao.deleteVersion(id, node.getVersionNumber());
		NamedAnnotations rolledBackAnnos = nodeDao.getAnnotations(id);
		assertEquals(2.3, rolledBackAnnos.getAdditionalAnnotations().getSingleValue("double"));
	}
	
	@Test
	public void testGetVersionNumbers() throws Exception {
		// Create a number of versions
		int numberVersions = 10;
		String id = createNodeWithMultipleVersions(numberVersions);
		// Now list the versions
		List<Long> versionNumbers = nodeDao.getVersionNumbers(id);
		assertNotNull(versionNumbers);
		assertEquals(numberVersions,versionNumbers.size());
		// The highest version should be first
		assertEquals(new Long(numberVersions), versionNumbers.get(0));
		// The very fist version should be last
		assertEquals(new Long(1), versionNumbers.get(versionNumbers.size()-1));
		
		// Make sure we can fetch each version
		for(Long versionNumber: versionNumbers){
			Node nodeVersion = nodeDao.getNodeForVersion(id, versionNumber);
			assertNotNull(nodeVersion.getETag());
			assertEquals(NodeConstants.ZERO_E_TAG, nodeVersion.getETag());
			assertNotNull(nodeVersion);
			assertEquals(versionNumber, nodeVersion.getVersionNumber());
		}
	}
	
	@Test
	public void testGetVersionInfo() throws Exception {
		// Create a number of versions
		int numberVersions = 10;
		String id = createNodeWithMultipleVersions(numberVersions);
		// Now list the versions
		QueryResults<VersionInfo> versionsOfEntity = nodeDao.getVersionsOfEntity(id, 0, 10);
		assertNotNull(versionsOfEntity);
		assertEquals(numberVersions,versionsOfEntity.getResults().size());
		VersionInfo firstResult = versionsOfEntity.getResults().get(0);
		assertEquals(new Long(numberVersions), firstResult.getVersionNumber());
		//verify content size
		assertEquals(Long.toString(TEST_FILE_SIZE), firstResult.getContentSize());
		//verify md5 (is set to filename in our test filehandle)
		assertEquals(fileHandle.getFileName(), firstResult.getContentMd5());

		assertEquals(new Long(1), versionsOfEntity.getResults().get(versionsOfEntity.getResults().size()-1).getVersionNumber());
		for (VersionInfo vi : versionsOfEntity.getResults()) {
			Node node = nodeDao.getNodeForVersion(id, vi.getVersionNumber());
			assertNotNull(node.getETag());
			assertEquals(NodeConstants.ZERO_E_TAG, node.getETag());
			Date modDate = node.getModifiedOn();
			assertEquals(modDate, vi.getModifiedOn());
		}
	}

	@Test
	public void testVersionCount() throws Exception {
		// Create a number of versions
		int numberVersions = 10;
		String id = createNodeWithMultipleVersions(numberVersions);
		// Now list the versions
		long numVersions = nodeDao.getVersionCount(id);
		assertEquals(numberVersions, numVersions);
	}

	@Test
	public void testDeleteCurrentVersion() throws Exception {
		// Create a number of versions
		int numberVersions = 2;
		String id = createNodeWithMultipleVersions(numberVersions);
		Node node = nodeDao.getNode(id);
		long currentVersion = node.getVersionNumber();
		List<Long> startingVersions = nodeDao.getVersionNumbers(id);
		assertNotNull(startingVersions);
		assertEquals(numberVersions, startingVersions.size());
		// Delete the current version.
		nodeDao.deleteVersion(id, new Long(currentVersion));
		List<Long> endingVersions = nodeDao.getVersionNumbers(id);
		assertNotNull(endingVersions);
		assertEquals(numberVersions-1, endingVersions.size());
		assertFalse(endingVersions.contains(currentVersion));
		// Now make sure the current version of the node still exists
		node = nodeDao.getNode(id);
		assertNotNull(node);
		assertEquals("Deleting the current version of a node failed to change the current version to be current - 1",new Long(currentVersion-1), node.getVersionNumber());
	}
	
	@Test
	public void testDeleteFirstVersion() throws Exception {
		// Create a number of versions
		int numberVersions = 2;
		String id = createNodeWithMultipleVersions(numberVersions);
		Node node = nodeDao.getNode(id);
		long currentVersion = node.getVersionNumber();
		List<Long> startingVersions = nodeDao.getVersionNumbers(id);
		assertNotNull(startingVersions);
		assertEquals(numberVersions, startingVersions.size());
		// Delete the first version
		nodeDao.deleteVersion(id, new Long(1));
		List<Long> endingVersions = nodeDao.getVersionNumbers(id);
		assertNotNull(endingVersions);
		assertEquals(numberVersions-1, endingVersions.size());
		assertFalse(endingVersions.contains(new Long(1)));
		// The current version should not have changed.
		node = nodeDao.getNode(id);
		assertNotNull(node);
		assertEquals("Deleting the first version should not have changed the current version of the node",new Long(currentVersion), node.getVersionNumber());
	}
	
	@Test 
	public void testDeleteAllVersions() throws  Exception {
		// Create a number of versions
		int numberVersions = 3;
		String id = createNodeWithMultipleVersions(numberVersions);
		Node node = nodeDao.getNode(id);
		List<Long> startingVersions = nodeDao.getVersionNumbers(id);
		assertNotNull(startingVersions);
		assertEquals(numberVersions, startingVersions.size());
		// Now delete all versions. This should fail.
		try{
			for(Long versionNumber: startingVersions){
				nodeDao.deleteVersion(id, versionNumber);
			}
			fail("Should not have been able to delte all versions of a node");
		}catch(IllegalArgumentException e){
			// expected.
		}
		// There should be one version left and it should be the first version.
		node = nodeDao.getNode(id);
		assertNotNull(node);
		assertEquals("Deleting all versions except the first should have left the node in place with a current version of 1.",new Long(1), node.getVersionNumber());
	}
	
	@Test
	public void testPeekCurrentEtag() throws  Exception {
		Node node = privateCreateNew("testPeekCurrentEtag");
		// Start this node with version and comment information
		node.setVersionComment("This is the very first version of this node.");
		node.setVersionLabel("0.0.0");
		String id = nodeDao.createNew(node);
		toDelete.add(id);
		assertNotNull(id);
		node = nodeDao.getNode(id);
		assertNotNull(node);
		assertNotNull(node.getETag());
		String peekEtag = nodeDao.peekCurrentEtag(id);
		assertEquals(node.getETag(), peekEtag);
	}	
	
	@Test
	public void testGetEntityHeader() throws Exception {
		Node parent = privateCreateNew("parent");
		parent.setNodeType(EntityType.project);
		String parentId = nodeDao.createNew(parent);
		toDelete.add(parentId);
		assertNotNull(parentId);
		// Get the header of this node
		EntityHeader parentHeader = nodeDao.getEntityHeader(parentId, null);
		assertNotNull(parentHeader);
		assertEquals(EntityTypeUtils.getEntityTypeClassName(EntityType.project), parentHeader.getType());
		assertEquals("parent", parentHeader.getName());
		assertEquals(parentId, parentHeader.getId());
		
		Node child = privateCreateNew("child");
		child.setNodeType(EntityType.folder);
		child.setParentId(parentId);
		String childId = nodeDao.createNew(child);
		toDelete.add(childId);
		assertNotNull(childId);
		// Get the header of this node
		EntityHeader childHeader = nodeDao.getEntityHeader(childId, 1L);
		assertNotNull(childHeader);
		assertEquals(EntityTypeUtils.getEntityTypeClassName(EntityType.folder), childHeader.getType());
		assertEquals("child", childHeader.getName());
		assertEquals(childId, childHeader.getId());

		// Get the header of this node non versioned
		childHeader = nodeDao.getEntityHeader(childId, null);
		assertNotNull(childHeader);		
		assertEquals(childId, childHeader.getId());
		assertEquals(new Long(1), childHeader.getVersionNumber());

	}
	
	@Test
	public void testGetEntityHeaderByReference() throws Exception {
		Node parent = privateCreateNew("parent");
		parent.setNodeType(EntityType.project);
		String parentId = nodeDao.createNew(parent);
		Long parentBenefactor = KeyFactory.stringToKey(parentId);
		toDelete.add(parentId);
		assertNotNull(parentId);
		
		Node child = privateCreateNew("child");
		child.setNodeType(EntityType.folder);
		child.setParentId(parentId);
		String childId = nodeDao.createNew(child);
		toDelete.add(childId);
		assertNotNull(childId);
		child = nodeDao.getNode(childId);
		// create a second version
		child.setVersionLabel(""+child.getVersionNumber()+1);
		nodeDao.createNewVersion(child);
		
		List<Reference> request = new LinkedList<Reference>();
		Reference r = new Reference();
		r.setTargetId(parentId);
		r.setTargetVersionNumber(null);
		request.add(r);
		
		r = new Reference();
		r.setTargetId(child.getId());
		r.setTargetVersionNumber(null);
		request.add(r);
		
		r = new Reference();
		r.setTargetId(child.getId());
		r.setTargetVersionNumber(2L);
		request.add(r);
		
		r = new Reference();
		r.setTargetId(child.getId());
		r.setTargetVersionNumber(1L);
		request.add(r);
		
		List<EntityHeader> results = nodeDao.getEntityHeader(request);
		assertNotNull(results);
		assertEquals(4, results.size());
		
		EntityHeader header = results.get(0);
		assertEquals(parentId, header.getId());
		assertEquals("1", header.getVersionLabel());
		assertEquals(new Long(1), header.getVersionNumber());
		assertEquals(parentBenefactor, header.getBenefactorId());
		
		header = results.get(2);
		assertEquals(childId, header.getId());
		assertEquals("2", header.getVersionLabel());
		assertEquals(new Long(2), header.getVersionNumber());
		assertEquals(parentBenefactor, header.getBenefactorId());
		
		header = results.get(3);
		assertEquals(childId, header.getId());
		assertEquals("1", header.getVersionLabel());
		assertEquals(new Long(1), header.getVersionNumber());
		assertEquals(parentBenefactor, header.getBenefactorId());
	}
	
	/*
	 * Test for PLFM-3706 
	 * @throws Exception
	 */
	@Test 
	public void testGetEntityHeaderByReferenceEmpty() throws Exception {
		List<Reference> request = new LinkedList<Reference>();
		//call under test
		List<EntityHeader> results = nodeDao.getEntityHeader(request);
		assertNotNull(results);
		assertTrue(results.isEmpty());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetEntityHeaderByReferenceNull() throws Exception {
		List<Reference> request = null;
		// call under test.
		nodeDao.getEntityHeader(request);
	}
	
	@Test (expected=NotFoundException.class)
	public void testGetEntityHeaderDoesNotExist() throws NotFoundException, DatastoreException{
		// There should be no node with this id.
		long id = idGenerator.generateNewId();
		nodeDao.getEntityHeader(KeyFactory.keyToString(id), null);
	}
	
	@Test
	public void testGetEntityPath() throws Exception {
		Node node = privateCreateNew("parent");
		node.setNodeType(EntityType.project);
		String parentId = nodeDao.createNew(node);
		toDelete.add(parentId);
		assertNotNull(parentId);
		// Add a child		
		node = privateCreateNew("child");
		node.setNodeType(EntityType.folder);
		node.setParentId(parentId);
		String childId = nodeDao.createNew(node);
		toDelete.add(childId);
		assertNotNull(childId);
		// Add a GrandChild		
		node = privateCreateNew("grandChild");
		node.setNodeType(EntityType.folder);
		node.setParentId(childId);
		String grandId = nodeDao.createNew(node);
		toDelete.add(grandId);
		assertNotNull(grandId);
		
		// Get the individual headers
		EntityHeader[] array = new EntityHeader[3];
		array[0] = nodeDao.getEntityHeader(parentId, null);
		array[1] = nodeDao.getEntityHeader(childId, null);
		array[2] = nodeDao.getEntityHeader(grandId, null);
		
		// Now get the path for each node
		List<EntityHeader> path = nodeDao.getEntityPath(grandId);
		assertNotNull(path);
		assertEquals(3, path.size());
		assertEquals(array[0].getId(), path.get(0).getId());
		assertEquals(array[1].getId(), path.get(1).getId());
		assertEquals(array[2].getId(), path.get(2).getId());
		
		// child
		path = nodeDao.getEntityPath(childId);
		assertNotNull(path);
		assertEquals(2, path.size());
		assertEquals(array[0].getId(), path.get(0).getId());
		assertEquals(array[1].getId(), path.get(1).getId());
		
		// parent
		// child
		path = nodeDao.getEntityPath(parentId);
		assertNotNull(path);
		assertEquals(1, path.size());
		assertEquals(array[0].getId(), path.get(0).getId());
	}
	
	@Test(expected=NotFoundException.class)
	public void testGetEntityPathInvalidNode() throws Exception {
		nodeDao.getEntityPath("syn9999999");
	}
	
	// we introduced batch entity path search so we test the case that 
	// the path is bigger than one batch
	@Test
	public void testGetDeepEntityPath() throws Exception {
		testGetEntityPath(NodeDAOImpl.BATCH_PATH_DEPTH+3);
	}
	
	@Test
	public void testGetDeepEntityPathEdgeCase() throws Exception {
		testGetEntityPath(NodeDAOImpl.BATCH_PATH_DEPTH*2);
	}
	
	@Test 
	public void testGetShallowEntityPath() throws Exception {
		testGetEntityPath(1);
	}
	
	
	private void testGetEntityPath(int depth) throws NotFoundException {
		String[] ids = new String[depth];
		for (int i=0; i<depth; i++) {
			Node node = privateCreateNew("node_"+i);
			node.setNodeType(EntityType.project);
			if (i>0) node.setParentId(ids[i-1]);
			ids[i] = nodeDao.createNew(node);
			assertNotNull(ids[i]);
			toDelete.add(ids[i]);
		}
		EntityHeader[] array = new EntityHeader[depth];
		for (int i=0; i<depth; i++) {
			array[i] = nodeDao.getEntityHeader(ids[i], null);
			// 'getEntityPath' doesn't retreive version info, so we clear these fields for the sake of comparison
			array[i].setVersionLabel(null);
			array[i].setVersionNumber(null);
			array[i].setBenefactorId(null);
		}
		List<EntityHeader> path = nodeDao.getEntityPath(ids[depth-1]);
		assertNotNull(path);
		assertEquals(depth, path.size());
		for (int i=0; i<depth; i++) {
			assertEquals(array[i], path.get(i));
		}
	}
	
	@Test
	public void testGetChildrenList() throws NotFoundException, DatastoreException, InvalidModelException {
		Node node = privateCreateNew("parent");
		node.setNodeType(EntityType.project);
		String parentId = nodeDao.createNew(node);
		toDelete.add(parentId);
		assertNotNull(parentId);
		
		// Create a few children
		List<String> childIds = new ArrayList<String>();
		for(int i=0; i<4; i++){
			node = privateCreateNew("child"+i);
			node.setNodeType(EntityType.folder);
			node.setParentId(parentId);
			String id = nodeDao.createNew(node);
			toDelete.add(id);
			childIds.add(id);
		}
		// Now get the list of children
		List<String> fromDao =  nodeDao.getChildrenIdsAsList(parentId);
		// Check that the ids returned have the syn prefix
		assertEquals(childIds, fromDao);
	}
	
	@Test (expected=NotFoundException.class)
	public void testGetRefrenceDoesNotExist() throws DatastoreException, InvalidModelException, NotFoundException{
		// This should throw a not found exception.
		nodeDao.getNodeReference("syn123");
	}
	
	@Test
	public void testGetRefrenceNull() throws DatastoreException, InvalidModelException, NotFoundException{
		// Create a new node
		Node node = privateCreateNew("parent");
		node.setNodeType(EntityType.project);
		String id = nodeDao.createNew(node);
		toDelete.add(id);
		// This should be empty but not null
		Reference ref = nodeDao.getNodeReference(id);
		assertNull(ref);
	}
	
	@Test
	public void testGetRefrence() throws DatastoreException, InvalidModelException, NotFoundException{
		// Create a new node
		Node node = privateCreateNew("parent");
		node.setNodeType(EntityType.project);
		String parentId = nodeDao.createNew(node);
		toDelete.add(parentId);
		// Create a child with a reference to the parent
		node = privateCreateNew("child");
		node.setParentId(parentId);
		node.setNodeType(EntityType.folder);
		// Add a reference
		
		Reference ref = new Reference();
		ref.setTargetId(parentId);
		node.setReference(ref);
		String id = nodeDao.createNew(node);
		// This should be empty but not null
		Reference expectedRef = nodeDao.getNodeReference(id);
		assertNotNull(expectedRef);
		assertEquals(node.getReference(), expectedRef);
		
		// Now create a new revision and make sure we get the latest only
		node = nodeDao.getNode(id);
		ref = new Reference();
		ref.setTargetId(id);
		ref.setTargetVersionNumber(node.getVersionNumber());
		node.setReference(ref);
		node.setVersionLabel("v2");
		nodeDao.createNewVersion(node);
		// Now get the current references
		expectedRef = nodeDao.getNodeReference(id);
		assertNotNull(expectedRef);
		assertEquals(node.getReference(), expectedRef);
	}

	@Test
	public void testAddReferenceNoVersionSpecified() throws Exception {
		String deleteMeNode = null;

		// Create a node we will refer to, use the current version held in the repo svc
		Node node = privateCreateNew("referee");
		String id = nodeDao.createNew(node);
		toDelete.add(id);

		Reference ref = new Reference();
		ref.setTargetId(id);
		
		deleteMeNode = id;
		
		// Create the node that holds the references
		Node referer = privateCreateNew("referer");
		referer.setReference(ref);
		String refererId = nodeDao.createNew(referer);
		assertNotNull(refererId);
		toDelete.add(refererId);

		// Make sure it got stored okay
		Node storedNode = nodeDao.getNode(refererId);
		assertNotNull(storedNode);
		Reference storedRef = storedNode.getReference();
		assertNotNull(storedRef);
		assertEquals(null, storedRef.getTargetVersionNumber());
		
		// Now delete one of those nodes, such that one of our references has become 
		// invalid after we've created it.  This is okay and does not cause an error 
		// because we are not enforcing referential integrity.
		nodeDao.delete(deleteMeNode);
	}
	
	@Test 
	public void testUpdateReference() throws Exception {
		// Create a node we will refer to
		Node node = privateCreateNew("referee");
		node.setVersionNumber(999L);
		String id = nodeDao.createNew(node);
		toDelete.add(id);
		Reference ref = new Reference();
		ref.setTargetId(id);
		ref.setTargetVersionNumber(node.getVersionNumber());
		
		// Create the node that holds the references
		Node referer = privateCreateNew("referer");
		referer.setReference(ref);
		String refererId = nodeDao.createNew(referer);
		assertNotNull(refererId);
		toDelete.add(refererId);

		// Make sure it got stored okay
		Node storedNode = nodeDao.getNode(refererId);
		assertNotNull(storedNode);
		assertEquals(ref, storedNode.getReference());

		// Add a new one
		Node node2 = privateCreateNew("referee2");
		node2.setVersionNumber(999L);
		String id2 = nodeDao.createNew(node2);
		toDelete.add(id2);
		Reference ref2 = new Reference();
		ref2.setTargetId(id);
		ref2.setTargetVersionNumber(node2.getVersionNumber());
		storedNode.setReference(ref2);
		
		// Make sure it got updated okay
		nodeDao.updateNode(storedNode);
		storedNode = nodeDao.getNode(refererId);
		assertNotNull(storedNode);
		assertEquals(ref2, storedNode.getReference());
				
		// Now nuke all the references
		storedNode.setReference(null);
		nodeDao.updateNode(storedNode);
		storedNode = nodeDao.getNode(refererId);
		assertNotNull(storedNode);
		assertNull(storedNode.getReference());
	}
	
	/**
	 * Tests that getParentId method returns the Id of a node's parent.
	 * @throws Exception
	 */
	@Test
	public void testGetParentId() throws Exception {
		//make parent project
		Node node = privateCreateNew("parent");
		node.setNodeType(EntityType.project);
		String parentId = nodeDao.createNew(node);
		toDelete.add(parentId);
		assertNotNull(parentId);
		
		//add a child to the parent	
		node = privateCreateNew("child1");
		node.setNodeType(EntityType.folder);
		node.setParentId(parentId);
		String child1Id = nodeDao.createNew(node);
		toDelete.add(child1Id);
		
		// Now get child's parentId
		String answerParentId =  nodeDao.getParentId(child1Id);
		assertEquals(parentId, answerParentId);
	}
	
	/**
	 * Tests that changeNodeParent correctly sets a node's parent to reference
	 * the parentNode sent as a parameter.
	 * @throws Exception
	 */
	@Test
	public void testChangeNodeParent() throws Exception {
		//make a parent project
		Node node = privateCreateNew("parentProject");
		node.setNodeType(EntityType.project);
		String parentProjectId = nodeDao.createNew(node);
		toDelete.add(parentProjectId);
		assertNotNull(parentProjectId);
		
		//add a child to the parent
		node = privateCreateNew("child");
		node.setNodeType(EntityType.folder);
		node.setParentId(parentProjectId);
		String childId = nodeDao.createNew(node);
		toDelete.add(childId);
		assertNotNull(childId);
		
		//make a second project
		node = privateCreateNew("newParent");
		node.setNodeType(EntityType.project);
		String newParentId = nodeDao.createNew(node);
		toDelete.add(newParentId);
		assertNotNull(newParentId);
		
		//check state of child node before the change
		Node oldNode = nodeDao.getNode(childId);
		assertNotNull(oldNode);
		assertEquals(parentProjectId, oldNode.getParentId());
		
		//change child's parent to newProject
		boolean changeReturn = nodeDao.changeNodeParent(childId, newParentId, false);
		assertTrue(changeReturn);
		
		Node changedNode = nodeDao.getNode(childId);
		assertNotNull(changedNode);
		assertEquals(newParentId, changedNode.getParentId());		
	}
	
	/**
	 * Tests that changeNodeParent correctly sets a node's parent to reference the parentNode sent as a parameter.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testChangeNodeParentAndProject() throws Exception {
		// make a parent project
		Node node = privateCreateNew("parentProject");
		node.setNodeType(EntityType.project);
		String parentProjectId = nodeDao.createNew(node);
		toDelete.add(parentProjectId);
		assertNotNull(parentProjectId);

		// add a child to the parent
		node = privateCreateNew("child");
		node.setNodeType(EntityType.folder);
		node.setParentId(parentProjectId);
		String childId = nodeDao.createNew(node);
		toDelete.add(childId);
		assertNotNull(childId);

		String[] childChilds = new String[4];
		// add children to child
		for (int i = 0; i < childChilds.length; i++) {
			node = privateCreateNew("child" + i);
			node.setNodeType(EntityType.folder);
			node.setParentId(childId);
			childChilds[i] = nodeDao.createNew(node);
			toDelete.add(childChilds[i]);
			assertNotNull(childChilds[i]);
		}

		// make sure all project ids are set
		assertEquals(parentProjectId, nodeDao.getNode(parentProjectId).getProjectId());
		assertEquals(parentProjectId, nodeDao.getNode(childId).getProjectId());
		for (int i = 0; i < childChilds.length; i++) {
			assertEquals(parentProjectId, nodeDao.getNode(childChilds[i]).getProjectId());
		}

		// make a second project
		node = privateCreateNew("newParent");
		node.setNodeType(EntityType.project);
		String newParentId = nodeDao.createNew(node);
		toDelete.add(newParentId);
		assertNotNull(newParentId);

		// check state of child node before the change
		Node oldNode = nodeDao.getNode(childId);
		assertNotNull(oldNode);
		assertEquals(parentProjectId, oldNode.getParentId());

		// change child's parent to newProject
		boolean changeReturn = nodeDao.changeNodeParent(childId, newParentId, false);
		assertTrue(changeReturn);

		// make sure all project ids are set to new project
		assertEquals(newParentId, nodeDao.getNode(childId).getProjectId());
		for (int i = 0; i < childChilds.length; i++) {
			assertEquals(newParentId, nodeDao.getNode(childChilds[i]).getProjectId());
		}

		// change to trash (no project) and back
		nodeDao.changeNodeParent(childId, StackConfiguration.getTrashFolderEntityIdStatic(), true);

		// make sure all project ids are set to new project
		assertNull(nodeDao.getNode(childId).getProjectId());
		for (int i = 0; i < childChilds.length; i++) {
			assertNull(nodeDao.getNode(childChilds[i]).getProjectId());
		}

		nodeDao.changeNodeParent(childId, newParentId, false);

		// make sure all project ids are set to new project
		assertEquals(newParentId, nodeDao.getNode(childId).getProjectId());
		for (int i = 0; i < childChilds.length; i++) {
			assertEquals(newParentId, nodeDao.getNode(childChilds[i]).getProjectId());
		}
	}

	/**
	 * Tests that changeNodeParent correctly throws a IllegalArgumentException when the JDONode's parentId is null
	 * 
	 * @throws Exception
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testChangeNodeParentWhenParentIsNull() throws Exception {
		//make a project
		Node node = privateCreateNew("root");
		node.setNodeType(EntityType.project);
		String rootId = nodeDao.createNew(node);
		toDelete.add(rootId);
		assertNotNull(rootId);
		
		//make a second project
		node = privateCreateNew("newParent");
		node.setNodeType(EntityType.project);
		String newParentId = nodeDao.createNew(node);
		toDelete.add(newParentId);
		assertNotNull(newParentId);
		
		Node parent = nodeDao.getNode(rootId);
		assertNull(parent.getParentId());
		nodeDao.changeNodeParent(rootId, newParentId, false);
	}
	
	/**
	 * This was a test added for PLFM-3686, as the method was previously untested.
	 */
	@Test
	public void testUpdateNodeProjectId(){
		//make a parent project
		Node node = privateCreateNew("parentProject");
		node.setNodeType(EntityType.project);
		String parentProjectId = nodeDao.createNew(node);
		toDelete.add(parentProjectId);
		assertNotNull(parentProjectId);
		
		//add a child to the parent
		node = privateCreateNew("parentFolder");
		node.setNodeType(EntityType.folder);
		node.setParentId(parentProjectId);
		String parentFolderId = nodeDao.createNew(node);
		Node parentFolder = nodeDao.getNode(parentFolderId);
		toDelete.add(parentFolderId);
		assertNotNull(parentFolderId);
		
		node = privateCreateNew("child");
		node.setNodeType(EntityType.folder);
		node.setParentId(parentFolderId);
		String childId2 = nodeDao.createNew(node);
		Node child = nodeDao.getNode(parentFolderId);
		toDelete.add(childId2);
		assertNotNull(childId2);
		
		//make a second project
		node = privateCreateNew("newProject");
		node.setNodeType(EntityType.project);
		String newProjectId = nodeDao.createNew(node);
		toDelete.add(newProjectId);
		assertNotNull(newProjectId);
		
		// Move the parentFolder to the new project
		// call under test
		int count = nodeDao.updateProjectForAllChildren(parentFolder.getId(), newProjectId);
		assertEquals(2, count);
		// parent
		Node updated = nodeDao.getNode(parentFolderId);
		assertEquals(newProjectId, updated.getProjectId());
		assertFalse("Changing the projectId should change the etag", parentFolder.getETag().equals(updated.getETag()));
		//child
		updated = nodeDao.getNode(parentFolderId);
		assertEquals(newProjectId, updated.getProjectId());
		assertFalse("Changing the projectId should change the etag", child.getETag().equals(updated.getETag()));
	
	}
	
	/**
	 * Tests that changeNodeParent does nothing if the new parent parameter
	 * is the parent the current node already references
	 * @throws Exception
	 */
	@Test
	public void testChangeNodeParentWhenParamParentIsCurrentParent() throws Exception {
		//make a parent project
		Node node = privateCreateNew("parentProject");
		node.setNodeType(EntityType.project);
		String parentProjectId = nodeDao.createNew(node);
		toDelete.add(parentProjectId);
		assertNotNull(parentProjectId);
		
		//add a child to the parent
		node = privateCreateNew("child");
		node.setNodeType(EntityType.folder);
		node.setParentId(parentProjectId);
		String childId = nodeDao.createNew(node);
		toDelete.add(childId);
		assertNotNull(childId);
		
		//check current state of node
		Node oldNode = nodeDao.getNode(childId);
		assertNotNull(oldNode);
		assertEquals(childId, oldNode.getId());
		assertEquals(parentProjectId, oldNode.getParentId());
		
		//make the parentChange update
		boolean updateReturn = nodeDao.changeNodeParent(childId, parentProjectId, false);
		assertFalse(updateReturn);
		
		//check new state of node
		Node newNode = nodeDao.getNode(childId);
		assertNotNull(newNode);
		assertEquals(childId, newNode.getId());
		assertEquals(parentProjectId, newNode.getParentId());
	}

	@Test
	public void testReferenceDeleteCurrentVersion() throws NotFoundException, DatastoreException, InvalidModelException {
		Node referee = privateCreateNew("referee");
		referee.setVersionNumber(999L);
		String refereeid = nodeDao.createNew(referee);
		toDelete.add(refereeid);
		Reference ref = new Reference();
		ref.setTargetId(refereeid);
		ref.setTargetVersionNumber(referee.getVersionNumber());
		
		// Create the node that holds the reference
		Node parent = privateCreateNew("parent");
		parent.setNodeType(EntityType.project);
		parent.setReference(ref);
		parent.setVersionLabel("reference 1.0");
		String parentid = nodeDao.createNew(parent);
		toDelete.add(parentid);
		assertNotNull(parentid);
		
		// Get the newly created node
		Node node = nodeDao.getNode(parentid);
		assertEquals(node.getReference(), ref);
		
		// now create a new version and change the reference
		node = nodeDao.getNode(parentid);
		Reference ref2 = new Reference();
		ref2.setTargetId(refereeid);
		ref2.setTargetVersionNumber(referee.getVersionNumber());
		node.setReference(ref2);
		node.setVersionLabel("reference 2.0");
		nodeDao.createNewVersion(node);

		// Get the updated node
		node = nodeDao.getNode(parentid);
		// Since we added new reference, we should see them in the node and in the revision
		assertEquals(node.getReference(), ref2);
		
		// Delete the current version.
		nodeDao.deleteVersion(parentid, node.getVersionNumber());

		// Get the (rolled back) node and check that the reference have been reverted
		node = nodeDao.getNode(parentid);
		assertEquals(node.getReference(), ref);
	}
	
	@Test
	public void testForPLFM_791() throws Exception {
		// In the past annotations, were persisted in the annotations tables.  So when we had large strings we had to store
		// the values as BLOB annotations.  This is no longer the case.  Now all annotations are not persisted as a single 
		// zipped blob on the revision table.  Therefore, we only use the annotations tables for query.  This means there 
		// is no need to have a blob annotations table anymore.  There is no need to store very large strings in the
		// string annotations table since it does not make sense to query for large strings.
		// This test ensures that we can have giant string annotations without any problems.
		//make a parent project
		Node node = privateCreateNew("testForPLFM_791");
		node.setNodeType(EntityType.project);
		String projectId = nodeDao.createNew(node);
		toDelete.add(projectId);
		assertNotNull(projectId);
		// Now get the annotations of the entity
		NamedAnnotations annos = nodeDao.getAnnotations(projectId);
		assertNotNull(annos);
		assertNotNull(annos.getAdditionalAnnotations());
		// Create a very large string
		byte[] largeArray = new byte[10000];
		byte value = 101;
		Arrays.fill(largeArray, value);
		String largeString = new String(largeArray, "UTF-8");
		String key = "veryLargeString";
		annos.getAdditionalAnnotations().addAnnotation(key, largeString);
		// This update will fail before PLFM-791 is fixed.
		nodeDao.updateAnnotations(projectId, annos);
		// Get the values back
		annos = nodeDao.getAnnotations(projectId);
		assertNotNull(annos);
		assertNotNull(annos.getAdditionalAnnotations());
		// Make sure we can still get the string
		assertEquals(largeString, annos.getAdditionalAnnotations().getSingleValue(key));
	}
	
	@Test
	public void testGetCurrentRevNumber() throws NotFoundException, DatastoreException, InvalidModelException {
		Node backup = privateCreateNew("withReveNumber");
		backup.setNodeType(EntityType.project);
		String id = nodeDao.createNew(backup);
		toDelete.add(id);
		assertNotNull(id);
		Long currentRev = nodeDao.getCurrentRevisionNumber(id);
		assertEquals(NodeConstants.DEFAULT_VERSION_NUMBER, currentRev);
	}
	
	@Test
	public void testTypeFromNode() throws NotFoundException, DatastoreException, InvalidModelException {
		Node backup = privateCreateNew("getNodeTypeById");
		backup.setNodeType(EntityType.project);
		String id = nodeDao.createNew(backup);
		toDelete.add(id);
		assertNotNull(id);
		EntityType type = nodeDao.getNodeTypeById(id);
		assertEquals(EntityType.project, type);
	}
	
	@Test (expected=NotFoundException.class)
	public void testGetCurrentRevNumberDoesNotExist() throws NotFoundException, DatastoreException{
		// This should throw a NotFoundException exception
		Long currentRev = nodeDao.getCurrentRevisionNumber(KeyFactory.keyToString(new Long(-12)));
	}
	
	@Test 
	public void testGetActivityId() throws Exception{
		// v1: activity 1
		Node toCreate = privateCreateNewDistinctModifier("getCurrentActivityId");		
		toCreate.setActivityId(testActivity.getId());
		String id = nodeDao.createNew(toCreate);
		toDelete.add(id);
		assertNotNull(id);
		Node loadedV1 = nodeDao.getNode(id);	

		// v2: test null version
		Node newRev = nodeDao.getNode(id);
		newRev.setVersionLabel("2");
		newRev.setActivityId(null);
		Long v2 = nodeDao.createNewVersion(newRev);				
		Node loadedV2 = nodeDao.getNodeForVersion(id, v2);
		
		// v3: activity 2
		newRev = nodeDao.getNode(id);
		newRev.setVersionLabel("3");
		newRev.setActivityId(testActivity2.getId());
		Long v3 = nodeDao.createNewVersion(newRev);				
		Node loadedV3 = nodeDao.getNodeForVersion(id, v3);
		
		// test returned values in nodes 
		assertEquals(testActivity.getId(), loadedV1.getActivityId());
		assertEquals(null, loadedV2.getActivityId());
		assertEquals(testActivity2.getId(), loadedV3.getActivityId());

		// check getActivityId persistence
		assertEquals(testActivity.getId(), nodeDao.getActivityId(id, loadedV1.getVersionNumber()));
		assertEquals(null, nodeDao.getActivityId(id, loadedV2.getVersionNumber()));
		assertEquals(testActivity2.getId(), nodeDao.getActivityId(id, loadedV3.getVersionNumber()));
		
		// test current version (should be 3)
		assertEquals(testActivity2.getId(), nodeDao.getActivityId(id));
	}

	
	@Test
	public void testGetCreatedBy() throws NotFoundException, DatastoreException, InvalidModelException {
		Node node = privateCreateNew("foobar");
		node.setNodeType(EntityType.project);
		String id = nodeDao.createNew(node);
		toDelete.add(id);
		assertNotNull(id);
		Long createdBy = nodeDao.getCreatedBy(id);
		assertEquals(creatorUserGroupId, createdBy);
	}
	
	@Test (expected=NotFoundException.class)
	public void testGetCreatedByDoesNotExist() throws NotFoundException, DatastoreException{
		// This should throw a NotFoundException exception
		Long createdBy = nodeDao.getCreatedBy(KeyFactory.keyToString(new Long(-12)));
	}
	
	/**
	 * Tests isNodeRoot()
	 * @throws Exception
	 */
	@Test
	public void testIsNodeRoot() throws Exception {
		//make root node
		assertTrue(nodeDao.isNodeRoot(KeyFactory.SYN_ROOT_ID));
		
		//add a child to the root	
		Node node = privateCreateNew("child1");
		node.setNodeType(EntityType.folder);
		node.setParentId(KeyFactory.SYN_ROOT_ID);
		String child1Id = nodeDao.createNew(node);
		toDelete.add(child1Id);
		assertFalse(nodeDao.isNodeRoot(child1Id));
		
		// Now get child's parentId
		node = privateCreateNew("grandchild");
		node.setNodeType(EntityType.folder);
		node.setParentId(child1Id);
		String grandkidId = nodeDao.createNew(node);
		toDelete.add(grandkidId);
		assertFalse(nodeDao.isNodeRoot(grandkidId));
	}

	/**
	 * Tests isNodesParentRoot()
	 * @throws Exception
	 */
	@Test
	public void testIsNodesParentRoot() throws Exception {
		//add a child to the root	
		Node node = privateCreateNew("child1");
		node.setNodeType(EntityType.folder);
		node.setParentId(KeyFactory.SYN_ROOT_ID);
		String child1Id = nodeDao.createNew(node);
		toDelete.add(child1Id);
		assertTrue(nodeDao.isNodesParentRoot(child1Id));
		
		// Now get child's parentId
		node = privateCreateNew("grandchild");
		node.setNodeType(EntityType.folder);
		node.setParentId(child1Id);
		String grandkidId = nodeDao.createNew(node);
		toDelete.add(grandkidId);
		assertFalse(nodeDao.isNodesParentRoot(grandkidId));
	}
	
	@Test
	public void testHasChildren() throws DatastoreException, InvalidModelException, NotFoundException{
		//make root node
		assertFalse(nodeDao.isNodesParentRoot(KeyFactory.SYN_ROOT_ID));
		
		//add a child to the root	
		Node node = privateCreateNew("child1");
		node.setNodeType(EntityType.folder);
		node.setParentId(KeyFactory.SYN_ROOT_ID);
		String child1Id = nodeDao.createNew(node);
		toDelete.add(child1Id);
		assertTrue(nodeDao.isNodesParentRoot(child1Id));
		
		// The root should have children but the child should not
		assertTrue(nodeDao.doesNodeHaveChildren(KeyFactory.SYN_ROOT_ID));
		assertFalse(nodeDao.doesNodeHaveChildren(child1Id));
	}

	@Test
	public void testGetParentRelations() throws DatastoreException, InvalidModelException, NotFoundException {

		Node n1 = NodeTestUtils.createNew("testGetParentRelations.name1", creatorUserGroupId);
		String id1 = this.nodeDao.createNew(n1);
		this.toDelete.add(id1);
		Node n2 = NodeTestUtils.createNew("testGetParentRelations.name2", creatorUserGroupId, id1);
		String id2 = this.nodeDao.createNew(n2);
		this.toDelete.add(id2);
		Node n3 = NodeTestUtils.createNew("testGetParentRelations.name3", creatorUserGroupId, id1);
		String id3 = this.nodeDao.createNew(n3);
		this.toDelete.add(id3);
		Node n4 = NodeTestUtils.createNew("testGetParentRelations.name4", creatorUserGroupId, id2);
		String id4 = this.nodeDao.createNew(n4);
		this.toDelete.add(id4);

		Map<String, NodeParentRelation> map = new HashMap<String, NodeParentRelation>();
		QueryResults<NodeParentRelation> results = this.nodeDao.getParentRelations(0, 1);
		assertNotNull(results);
		assertTrue(results.getTotalNumberOfResults() > 4);
		List<NodeParentRelation> rList = results.getResults();
		assertNotNull(rList);
		assertEquals(1, rList.size());
		for (NodeParentRelation npr : rList) {
			map.put(npr.getId(), npr);
		}

		results = this.nodeDao.getParentRelations(1, 2);
		assertNotNull(results);
		assertTrue(results.getTotalNumberOfResults() > 4);
		rList = results.getResults();
		assertNotNull(rList);
		assertEquals(2, rList.size());
		for (NodeParentRelation npr : rList) {
			map.put(npr.getId(), npr);
		}

		results = this.nodeDao.getParentRelations(3, 1000);
		assertNotNull(results);
		assertTrue(results.getTotalNumberOfResults() > 4);
		rList = results.getResults();
		assertNotNull(rList);
		for (NodeParentRelation npr : rList) {
			map.put(npr.getId(), npr);
		}

		NodeParentRelation r = map.get(id1);
		assertEquals(id1, r.getId());
		assertNull(r.getParentId()); // The root
		assertNotNull(r.getETag());
		assertNotNull(r.getTimestamp());

		r = map.get(id2);
		assertEquals(id2, r.getId());
		assertNotNull(r.getParentId());
		assertEquals(id1, r.getParentId());
		assertNotNull(r.getETag());
		assertNotNull(r.getTimestamp());

		r = map.get(id3);
		assertEquals(id3, r.getId());
		assertNotNull(r.getParentId());
		assertEquals(id1, r.getParentId());
		assertNotNull(r.getETag());
		assertNotNull(r.getTimestamp());

		r = map.get(id4);
		assertEquals(id4, r.getId());
		assertNotNull(r.getParentId());
		assertEquals(id2, r.getParentId());
		assertNotNull(r.getETag());
		assertNotNull(r.getTimestamp());
	}
	
	@Test
	public void testNodeWithFileHandle() throws Exception{
		Node n1 = NodeTestUtils.createNew("testNodeWithFileHandle.name1", creatorUserGroupId);
		// Set the file handle on the new node
		n1.setFileHandleId(fileHandle.getId());
		String id = nodeDao.createNew(n1);
		assertNotNull(id);
		toDelete.add(id);
		// Validate that we cannot delete the handle while it is assigned to a node
		try{
			fileHandleDao.delete(fileHandle.getId());
			fail("Should not be able to delete a file handle that has been assigned");
		}catch(DataIntegrityViolationException e){
			// This is expected.
		}catch(UnexpectedRollbackException e){
			// This can also happen
		}
		Node clone = nodeDao.getNode(id);
		Long v1 = clone.getVersionNumber();
		assertEquals(fileHandle.getId(), clone.getFileHandleId());
		// Create a new version
		clone.setVersionLabel("v2");
		nodeDao.createNewVersion(clone);
		clone = nodeDao.getNode(id);
		// Create a new version
		Long v2 = clone.getVersionNumber();
		assertFalse(v1.equals(v2));
		// v2 should now have the second file handle
		assertEquals("Creating a new version should copy the FileHandlId from the previous version", fileHandle.getId(), clone.getFileHandleId());
		// Now create a third version with a new file handle
		clone.setFileHandleId(fileHandle2.getId());
		clone.setVersionLabel("v3");
		nodeDao.createNewVersion(clone);
		clone = nodeDao.getNode(id);
		// Create a new version
		Long v3 = clone.getVersionNumber();
		assertFalse(v3.equals(v2));
		Node v1Node = nodeDao.getNodeForVersion(id, v1);
		Node v2Node = nodeDao.getNodeForVersion(id, v2);
		Node v3Node = nodeDao.getNodeForVersion(id, v3);
		assertEquals("V1 should have the first file handle", fileHandle.getId(), v1Node.getFileHandleId());
		assertEquals("V2 should also have the first file handle", fileHandle.getId(), v2Node.getFileHandleId());
		assertEquals("V3 should also have the second file handle", fileHandle2.getId(), v3Node.getFileHandleId());
		// Get the file handle
		String fileHandleId = nodeDao.getFileHandleIdForVersion(id, null);
		assertEquals(fileHandle2.getId(), fileHandleId);
		// Try with the version parameter.
		fileHandleId = nodeDao.getFileHandleIdForVersion(id, v1);
		assertEquals(fileHandle.getId(), fileHandleId);
		// Make sure we can set it to null
		clone.setFileHandleId(null);
		nodeDao.updateNode(clone);
		clone = nodeDao.getNode(id);
		assertEquals(null, clone.getFileHandleId());
		fileHandleId = nodeDao.getFileHandleIdForVersion(id, null);
		assertEquals(null, fileHandleId);
		// Make sure we can set it to null
		// Now delete the node
		nodeDao.delete(id);
		// We should be able to delete the file handles now as they are no longer in use.
		fileHandleDao.delete(fileHandle.getId());
		fileHandleDao.delete(fileHandle2.getId());
	}
	
	@Test
	public void testGetCurrentVersions() throws Exception {
		Node n1 = NodeTestUtils.createNew("testGetCurrentVersions.name1", creatorUserGroupId);
		String id1 = this.nodeDao.createNew(n1);
		toDelete.add(id1);
		n1 = nodeDao.getNode(id1);
		n1.setVersionLabel("2nd");
		// create 2nd version
		nodeDao.createNewVersion(n1);
		n1 = nodeDao.getNode(id1);
		Node n2 = NodeTestUtils.createNew("testGetCurrentVersions.name2", creatorUserGroupId);
		String id2 = this.nodeDao.createNew(n2);
		toDelete.add(id2);
		n2 = nodeDao.getNode(id2);
		
		List<Reference> refs = nodeDao.getCurrentRevisionNumbers(Arrays.asList(new String[] { n1.getId(), n2.getId() }));
		
		Reference refN1 = new Reference();
		refN1.setTargetId(n1.getId());		
		refN1.setTargetVersionNumber(n1.getVersionNumber());
		Reference refN2 = new Reference();
		refN2.setTargetId(n2.getId());				
		refN2.setTargetVersionNumber(n2.getVersionNumber());

		assertTrue(refs.contains(refN1));
		assertTrue(refs.contains(refN2));
	}
	
	@Test
	public void testCreateTableNode() throws DatastoreException, InvalidModelException, NotFoundException{
		List<String> columnIds = new LinkedList<String>();
		columnIds.add("123");
		columnIds.add("456");
		Node n1 = NodeTestUtils.createNew(UUID.randomUUID().toString(), creatorUserGroupId);
		n1.setColumnModelIds(columnIds);
		String id1 = this.nodeDao.createNew(n1);
		toDelete.add(id1);
		n1 = nodeDao.getNode(id1);
		assertNotNull("ColumnModel ID were not saved!",n1.getColumnModelIds());
		List<String> expected = new LinkedList<String>();
		expected.add("123");
		expected.add("456");
		assertEquals(expected, n1.getColumnModelIds());
	}

	@Test
	public void testGetEntityHeaderByMd5() throws Exception {

		// Nothing yet
		List<EntityHeader> results = nodeDao.getEntityHeaderByMd5("md5");
		assertNotNull(results);
		assertEquals(0, results.size());

		// Add a node with a file handle
		Node node1 = NodeTestUtils.createNew("testGetEntityHeaderByMd5 node 1", creatorUserGroupId);
		node1.setFileHandleId(fileHandle.getId());
		final String node1Label1 = "1";
		node1.setVersionLabel(node1Label1);
		final String id1 = nodeDao.createNew(node1);
		assertNotNull(id1);
		toDelete.add(id1);
		node1.setId(id1);

		results = nodeDao.getEntityHeaderByMd5(fileHandle.getContentMd5());
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals(id1, results.get(0).getId());
		assertNotNull(results.get(0).getBenefactorId());
		assertEquals(Long.valueOf(1L), results.get(0).getVersionNumber());
		assertEquals(node1Label1, results.get(0).getVersionLabel());

		// Create a new version of the node of the same file
		final String node1Label2 = "Node 1 version label 2";
		node1.setVersionLabel(node1Label2);
		nodeDao.createNewVersion(node1);

		results = nodeDao.getEntityHeaderByMd5(fileHandle.getContentMd5());
		assertNotNull(results);
		assertEquals(2, results.size());
		assertEquals(id1, results.get(0).getId());
		assertEquals(id1, results.get(1).getId());
		assertFalse(results.get(0).getVersionNumber().equals(results.get(1).getVersionNumber()));

		// Add a new node with no file handle
		Node node2 = NodeTestUtils.createNew("testGetEntityHeaderByMd5 node 2", creatorUserGroupId);
		final String node2Label1 = "Node 2 version label 1";
		node1.setVersionLabel(node2Label1);
		final String id2 = nodeDao.createNew(node2);
		assertNotNull(id2);
		toDelete.add(id2);
		node2.setId(id2);

		results = nodeDao.getEntityHeaderByMd5(fileHandle.getContentMd5());
		assertNotNull(results);
		assertEquals(2, results.size());
		assertEquals(id1, results.get(0).getId());
		assertEquals(id1, results.get(1).getId());

		// Create a new version of node 2 with file handle
		final String node2Label2 = "Node 2 version label 2";
		node2.setVersionLabel(node2Label2);
		node2.setFileHandleId(fileHandle2.getId());
		nodeDao.createNewVersion(node2);

		results = nodeDao.getEntityHeaderByMd5(fileHandle.getContentMd5());
		assertNotNull(results);
		assertEquals(2, results.size());
		assertEquals(id1, results.get(0).getId());
		assertEquals(id1, results.get(1).getId());
		results = nodeDao.getEntityHeaderByMd5(fileHandle2.getContentMd5());
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals(id2, results.get(0).getId());
		assertEquals(Long.valueOf(2L), results.get(0).getVersionNumber());
	}

	@Test
	public void testGetProjectHeadersReturnInfo() throws Exception {
		UserInfo user1Info = createUserInfo(user1, false);

		Date before = new Date();
		Thread.sleep(2);

		String owned = createProject("testGetProjectHeaders.name1", user1);
		toDelete.add(owned);
		Node ownedProject = nodeDao.getNode(owned);
		// now add ACL for the user
		addReadAcl(owned, user1);

		PaginatedResults<ProjectHeader> projectHeaders = nodeDao.getProjectHeaders(user1Info, user1Info, null, ProjectListType.MY_PROJECTS,
				ProjectListSortColumn.LAST_ACTIVITY, SortDirection.DESC, 100, 0);
		assertEquals(1, projectHeaders.getTotalNumberOfResults());
		ProjectHeader header = projectHeaders.getResults().get(0);
		assertEquals(ownedProject.getName(), header.getName());
		assertEquals(owned, header.getId());

		Thread.sleep(2);
		Date between = new Date();
		Thread.sleep(2);

		assertTrue(header.getLastActivity().after(before));
		assertTrue(header.getLastActivity().before(between));

		// touching project stats
		ProjectStat projectStat = new ProjectStat(KeyFactory.stringToKey(owned), KeyFactory.stringToKey(user1), new Date());
		projectStatsDAO.update(projectStat);

		Thread.sleep(2);

		ownedProject.setModifiedByPrincipalId(Long.parseLong(user2));
		ownedProject.setModifiedOn(new Date());
		nodeDao.updateNode(ownedProject);
		projectStat = new ProjectStat(KeyFactory.stringToKey(owned), KeyFactory.stringToKey(user2), new Date());
		projectStatsDAO.update(projectStat);

		PaginatedResults<ProjectHeader> projectHeadersAfter = nodeDao.getProjectHeaders(user1Info, user1Info, null,
				ProjectListType.MY_PROJECTS, ProjectListSortColumn.LAST_ACTIVITY, SortDirection.DESC, 100, 0);
		assertEquals(1, projectHeadersAfter.getTotalNumberOfResults());
		ProjectHeader headerAfter = projectHeadersAfter.getResults().get(0);
		assertEquals(ownedProject.getName(), headerAfter.getName());
		assertEquals(owned, headerAfter.getId());
		assertNotNull(headerAfter.getLastActivity());

		assertEquals(user2, headerAfter.getModifiedBy().toString());
		assertTrue(headerAfter.getModifiedOn().after(headerAfter.getLastActivity()));

		Thread.sleep(2);
		Date after = new Date();

		assertTrue(headerAfter.getLastActivity().after(between));
		assertTrue(headerAfter.getLastActivity().before(after));
	}

	private static final Function<ProjectHeader, String> transformToId = new Function<ProjectHeader, String>() {
		@Override
		public String apply(ProjectHeader input) {
			return input.getId();
		}
	};

	@Test
	public void testGetProjectHeaders() throws Exception {
		UserInfo user1Info = createUserInfo(user1, false);
		UserInfo user2Info = createUserInfo(user2, false);
		UserInfo user3Info = createUserInfo(user3, false);

		createProjects();

		PaginatedResults<ProjectHeader> projectHeaders = nodeDao.getProjectHeaders(user1Info, user1Info, null, ProjectListType.MY_PROJECTS,
				ProjectListSortColumn.LAST_ACTIVITY, SortDirection.DESC, 100, 0);
		List<String> projectIds = Lists.transform(projectHeaders.getResults(), transformToId);
		assertEquals(Lists.newArrayList(publicProject, subFolderProject2, groupParticipate, participate, owned), projectIds);

		// sort opposite direction
		projectHeaders = nodeDao.getProjectHeaders(user1Info, user1Info, null, ProjectListType.MY_PROJECTS,
				ProjectListSortColumn.LAST_ACTIVITY, SortDirection.ASC, 100, 0);
		projectIds = Lists.transform(projectHeaders.getResults(), transformToId);
		assertEquals(Lists.newArrayList(owned, participate, groupParticipate, subFolderProject2, publicProject), projectIds);

		// sort by name
		projectHeaders = nodeDao.getProjectHeaders(user1Info, user1Info, null, ProjectListType.MY_PROJECTS,
				ProjectListSortColumn.PROJECT_NAME, SortDirection.DESC, 100, 0);
		projectIds = Lists.transform(projectHeaders.getResults(), transformToId);
		assertEquals(Lists.newArrayList(publicProject, subFolderProject2, groupParticipate, participate, owned), projectIds);

		projectHeaders = nodeDao.getProjectHeaders(user1Info, user1Info, null, ProjectListType.MY_PROJECTS,
				ProjectListSortColumn.LAST_ACTIVITY, SortDirection.DESC, 100, 0);
		projectIds = Lists.transform(projectHeaders.getResults(), transformToId);

		List<String> projectIds2 = Lists.newArrayList();
		for (int i = 0; i < 5; i++) {
			projectHeaders = nodeDao.getProjectHeaders(user1Info, user1Info, null, ProjectListType.MY_PROJECTS,
					ProjectListSortColumn.LAST_ACTIVITY, SortDirection.DESC, 1, i);
			assertEquals(1L, projectHeaders.getResults().size());
			assertEquals(5L, projectHeaders.getTotalNumberOfResults());
			projectIds2.add(projectHeaders.getResults().get(0).getId());
		}
		assertEquals(projectIds, projectIds2);

		// change sorting by touching project stats
		ProjectStat projectStat = new ProjectStat(KeyFactory.stringToKey(participate), KeyFactory.stringToKey(user1), new Date());
		projectStatsDAO.update(projectStat);
		Thread.sleep(2);
		projectStat = new ProjectStat(KeyFactory.stringToKey(owned), KeyFactory.stringToKey(user1), new Date());
		projectStatsDAO.update(projectStat);
		// project stat for other user should not matter
		Thread.sleep(2);
		projectStat = new ProjectStat(KeyFactory.stringToKey(groupParticipate), KeyFactory.stringToKey(user2), new Date(2000));
		projectStatsDAO.update(projectStat);

		projectHeaders = nodeDao.getProjectHeaders(user1Info, user1Info, null, ProjectListType.MY_PROJECTS,
				ProjectListSortColumn.LAST_ACTIVITY, SortDirection.DESC, 100, 0);
		assertEquals(Lists.newArrayList(owned, participate, publicProject, subFolderProject2, groupParticipate),
				Lists.transform(projectHeaders.getResults(), transformToId));

		// created by user1
		projectHeaders = nodeDao.getProjectHeaders(user1Info, user1Info, null, ProjectListType.MY_CREATED_PROJECTS,
				ProjectListSortColumn.LAST_ACTIVITY, SortDirection.DESC, 100, 0);
		assertEquals(Lists.newArrayList(owned, publicProject), Lists.transform(projectHeaders.getResults(), transformToId));

		// direct participation by user1
		projectHeaders = nodeDao.getProjectHeaders(user1Info, user1Info, null, ProjectListType.MY_PARTICIPATED_PROJECTS,
				ProjectListSortColumn.LAST_ACTIVITY, SortDirection.DESC, 100, 0);
		assertEquals(Lists.newArrayList(participate, subFolderProject2, groupParticipate),
				Lists.transform(projectHeaders.getResults(), transformToId));

		// participate via team by user1
		projectHeaders = nodeDao.getProjectHeaders(user1Info, user1Info, null, ProjectListType.MY_TEAM_PROJECTS,
				ProjectListSortColumn.LAST_ACTIVITY, SortDirection.DESC, 100, 0);
		assertEquals(Lists.newArrayList(groupParticipate), Lists.transform(projectHeaders.getResults(), transformToId));

		// user3 only has access to group project
		projectHeaders = nodeDao.getProjectHeaders(user3Info, user1Info, null, ProjectListType.OTHER_USER_PROJECTS,
				ProjectListSortColumn.LAST_ACTIVITY, SortDirection.DESC, 100, 0);
		assertEquals(Lists.newArrayList(publicProject, groupParticipate), Lists.transform(projectHeaders.getResults(), transformToId));

		// group only has access to group projects, and only user1 can access sub folder project
		projectHeaders = nodeDao.getProjectHeaders(user1Info, user1Info, teamDAO.get(group), ProjectListType.TEAM_PROJECTS,
				ProjectListSortColumn.LAST_ACTIVITY, SortDirection.DESC, 100, 0);
		assertEquals(Lists.newArrayList(subFolderProject2, groupParticipate), Lists.transform(projectHeaders.getResults(), transformToId));

		// group only has access to group project and user3 cannot access sub folder project
		projectHeaders = nodeDao.getProjectHeaders(user3Info, user3Info, teamDAO.get(group), ProjectListType.TEAM_PROJECTS,
				ProjectListSortColumn.LAST_ACTIVITY, SortDirection.DESC, 100, 0);
		assertEquals(Lists.newArrayList(groupParticipate), Lists.transform(projectHeaders.getResults(), transformToId));
	}

	@Test
	public void testGetProjectHeadersAsAdmin() throws Exception {
		UserInfo user1Info = createUserInfo(user1, true);
		UserInfo user2Info = createUserInfo(user2, false);
		UserInfo user3Info = createUserInfo(user3, false);

		createProjects();

		PaginatedResults<ProjectHeader> projectHeaders = nodeDao.getProjectHeaders(user1Info, user1Info, null, ProjectListType.MY_PROJECTS,
				ProjectListSortColumn.LAST_ACTIVITY, SortDirection.DESC, 100, 0);
		List<String> projectIds = Lists.transform(projectHeaders.getResults(), transformToId);
		assertEquals(Lists.newArrayList(publicProject, subFolderProject2, subFolderProject, groupParticipate, participate, owned), projectIds);

		// created by user1
		projectHeaders = nodeDao.getProjectHeaders(user1Info, user1Info, null, ProjectListType.MY_CREATED_PROJECTS,
				ProjectListSortColumn.LAST_ACTIVITY, SortDirection.DESC, 100, 0);
		assertEquals(Lists.newArrayList(publicProject, owned), Lists.transform(projectHeaders.getResults(), transformToId));

		// direct participation by user1
		projectHeaders = nodeDao.getProjectHeaders(user1Info, user1Info, null, ProjectListType.MY_PARTICIPATED_PROJECTS,
				ProjectListSortColumn.LAST_ACTIVITY, SortDirection.DESC, 100, 0);
		assertEquals(Lists.newArrayList(subFolderProject2, subFolderProject, groupParticipate, participate),
				Lists.transform(projectHeaders.getResults(), transformToId));

		// participate via team by user1
		projectHeaders = nodeDao.getProjectHeaders(user1Info, user1Info, null, ProjectListType.MY_TEAM_PROJECTS,
				ProjectListSortColumn.LAST_ACTIVITY, SortDirection.DESC, 100, 0);
		assertEquals(Lists.newArrayList(groupParticipate), Lists.transform(projectHeaders.getResults(), transformToId));

		// group only has access to group projects, and only user1 can access sub folder project
		projectHeaders = nodeDao.getProjectHeaders(user1Info, user1Info, teamDAO.get(group), ProjectListType.TEAM_PROJECTS,
				ProjectListSortColumn.LAST_ACTIVITY, SortDirection.DESC, 100, 0);
		assertEquals(Lists.newArrayList(subFolderProject2, subFolderProject, groupParticipate),
				Lists.transform(projectHeaders.getResults(), transformToId));
	}

	@Test
	public void testGetMyProjectHeaders() throws Exception {
		UserInfo user1Info = createUserInfo(user1, true);

		String first = createProject("aaa1", user1);
		toDelete.add(first);
		addReadAcl(first, user1);

		String second = createProject("AAA2", user1);
		toDelete.add(second);
		addReadAcl(second, user1);

		String third = createProject("bbb", user1);
		toDelete.add(third);
		addReadAcl(third, user1);

		PaginatedResults<ProjectHeader> projectHeaders = nodeDao.getProjectHeaders(user1Info, user1Info, null,
				ProjectListType.MY_CREATED_PROJECTS, ProjectListSortColumn.PROJECT_NAME, SortDirection.ASC, 100, 0);
		assertEquals(Lists.newArrayList(first, second, third), Lists.transform(projectHeaders.getResults(), transformToId));
	}

	private void createProjects() throws Exception, NotFoundException {
		owned = createProject("testGetProjectHeaders.1.owned", user1);
		toDelete.add(owned);
		Node ownedProject = nodeDao.getNode(owned);
		ownedProject.setVersionLabel("2nd");
		// create 2nd version
		nodeDao.createNewVersion(ownedProject);
		ownedProject = nodeDao.getNode(owned);
		// now add ACL for the user
		addReadAcl(owned, user1);

		participate = createProject("testGetProjectHeaders.2.participate", user2);
		addReadAcl(participate, user1);

		groupParticipate = createProject("testGetProjectHeaders.3.groupParticipate", user2);
		addReadAcl(groupParticipate, group);

		nooneOwns = createProject("testGetProjectHeaders.4.nooneOwns", user2);

		subFolderProject = createProject("testGetProjectHeaders.5.subFolderProject", user2);
		Node folder = NodeTestUtils.createNew("testGetProjectHeaders.folder1", Long.parseLong(user1));
		folder.setParentId(subFolderProject);
		folder.setNodeType(EntityType.folder);
		String ownerFolder = this.nodeDao.createNew(folder);
		toDelete.add(ownerFolder);
		addReadAcl(ownerFolder, group);

		subFolderProject2 = createProject("testGetProjectHeaders.6.subFolderProject2", user2);
		addReadAcl(subFolderProject2, user1);
		folder = NodeTestUtils.createNew("testGetProjectHeaders.folder1a", Long.parseLong(user1));
		folder.setParentId(subFolderProject2);
		folder.setNodeType(EntityType.folder);
		ownerFolder = this.nodeDao.createNew(folder);
		toDelete.add(ownerFolder);
		addReadAcl(ownerFolder, group);

		trashed = createProject("testGetProjectHeaders.7.trashed", user2, StackConfiguration.getTrashFolderEntityIdStatic());

		publicProject = createProject("testGetProjectHeaders.8.publicProject", user1);
		addReadAcl(publicProject, user1, BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId().toString());

		publicProject2 = createProject("testGetProjectHeaders.9.publicProject2", user2);
		addReadAcl(publicProject2, user2, BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId().toString(),
				BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId().toString(), BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS
						.getPrincipalId().toString());
	}

	@Test (expected=IllegalArgumentException.class)
	public void testCreateNodeLongNameTooLong() throws DatastoreException, InvalidModelException, NotFoundException {
		char[] chars = new char[260];
		Arrays.fill(chars, 'x');
		String name = new String(chars);
		Node n = NodeTestUtils.createNew(name, creatorUserGroupId);
		String id = nodeDao.createNew(n);
		assertNull(id);
	}
	
	@Test
	public void testCreateNodeLongName() throws DatastoreException, InvalidModelException, NotFoundException {
		char[] chars = new char[255];
		Arrays.fill(chars, 'x');
		String name = new String(chars);
		Node n = NodeTestUtils.createNew(name, creatorUserGroupId);
		String id = nodeDao.createNew(n);
		assertNotNull(id);
		toDelete.add(id);
	}
	
	@Test
	public void testGetAllContainerIds(){
		// Generate some hierarchy
		List<Node> hierarchy = createHierarchy();
		assertEquals(7, hierarchy.size());
		Long projectId = KeyFactory.stringToKey(hierarchy.get(0).getId());
		Long folder0Id = KeyFactory.stringToKey(hierarchy.get(1).getId());
		Long folder1Id = KeyFactory.stringToKey(hierarchy.get(2).getId());
		Long folder2Id = KeyFactory.stringToKey(hierarchy.get(4).getId());

		// Lookup all of the containers in this hierarchy
		List<Long> containers = nodeDao.getAllContainerIds(projectId);
		List<Long> expected = Lists.newArrayList(
				projectId, folder0Id, folder1Id, folder2Id
		);
		assertEquals(expected, containers);
		
		// Folder1 contains folder2
		containers = nodeDao.getAllContainerIds(folder1Id);
		expected = Lists.newArrayList(
				folder1Id, folder2Id
		);
		assertEquals(expected, containers);
		
		// Folder2 contains nothing
		containers = nodeDao.getAllContainerIds(folder2Id);
		expected = Lists.newArrayList(
				folder2Id
		);
		assertEquals(expected, containers);
	}
	
	@Test
	public void testStreamOverHierarchy(){
		// Generate some hierarchy
		List<Node> hierarchy = createHierarchy();
		assertEquals(7, hierarchy.size());
		Long projectId = KeyFactory.stringToKey(hierarchy.get(0).getId());
		Long folder0Id = KeyFactory.stringToKey(hierarchy.get(1).getId());
		Long folder1Id = KeyFactory.stringToKey(hierarchy.get(2).getId());
		Long folder2Id = KeyFactory.stringToKey(hierarchy.get(4).getId());
		// The these are the contains
		List<Long> containers = Lists.newArrayList(
				projectId, folder0Id, folder1Id, folder2Id
		);
		// Stream over the results and everything to the list
		final List<NodeHierarchy> resutls = new LinkedList<NodeHierarchy>();
		// call under test
		nodeDao.streamOverHierarchy(containers, new Callback<NodeHierarchy>() {
			@Override
			public void invoke(NodeHierarchy value) {
				resutls.add(value);
			}
		});
		assertEquals(6, resutls.size());
		// validate all of the streamed data.
		for(int i=0; i<6; i++){
			Node node = hierarchy.get(i+1);
			NodeHierarchy nodehierarchy = resutls.get(i);
			// compare the results
			assertEquals(node.getId(), KeyFactory.keyToString(nodehierarchy.getNodeId()));
			assertEquals(node.getParentId(), KeyFactory.keyToString(nodehierarchy.getParentId()));
			assertEquals(projectId, nodehierarchy.getProjectId());
			// lookup the benefactor for this node.
			String benefactor = nodeInheritanceDAO.getBenefactor(node.getId());
			assertEquals(benefactor, KeyFactory.keyToString(nodehierarchy.getBenefectorId()));
		}
	}
	
	@Test
	public void testBatchUpdateHierarchy(){
		// Generate some hierarchy
		List<Node> hierarchy = createHierarchy();
		assertEquals(7, hierarchy.size());
		Long folder0Id = KeyFactory.stringToKey(hierarchy.get(1).getId());
		Long folder1Id = KeyFactory.stringToKey(hierarchy.get(2).getId());
		Long folder2Id = KeyFactory.stringToKey(hierarchy.get(4).getId());
		Long file1Id = KeyFactory.stringToKey(hierarchy.get(5).getId());
		Long file2Id = KeyFactory.stringToKey(hierarchy.get(6).getId());
		// The these are the contains
		List<Long> containers = Lists.newArrayList(
				folder1Id, folder2Id
		);
		// create a batch containing folder2 and file1 to use for an update.
		final ArrayList<NodeHierarchy> batch = new ArrayList<NodeHierarchy>();
		nodeDao.streamOverHierarchy(containers, new Callback<NodeHierarchy>() {
			@Override
			public void invoke(NodeHierarchy value) {
				batch.add(value);
			}
		});
		assertEquals(3, batch.size());
		NodeHierarchy folder2Hierarchy = batch.get(0);
		NodeHierarchy file1Hierarchy = batch.get(1);
		NodeHierarchy file2Hierarchy = batch.get(2);
		// 
		assertEquals(folder2Id, folder2Hierarchy.getNodeId());
		assertEquals(file1Id, file1Hierarchy.getNodeId());
		assertEquals(file2Id, file2Hierarchy.getNodeId());
		
		// Move both to folder0
		folder2Hierarchy.setParentId(folder0Id);
		file1Hierarchy.setParentId(folder0Id);
		// set each to be their own benefactor
		folder2Hierarchy.setBenefectorId(folder2Hierarchy.getNodeId());
		file1Hierarchy.setBenefectorId(file1Hierarchy.getNodeId());
		// clear the project id for both
		folder2Hierarchy.setProjectId(folder1Id);
		file1Hierarchy.setProjectId(folder2Id);
		// clear all values for the third
		file2Hierarchy.setBenefectorId(null);
		file2Hierarchy.setParentId(null);
		file2Hierarchy.setProjectId(null);
		
		// update both as a batch
		//call under test
		nodeDao.batchUpdateHierarchy(batch);
		
		// Validate all of the changes
		Node folder2 = nodeDao.getNode(""+folder2Id);
		assertEquals(folder0Id, KeyFactory.stringToKey(folder2.getParentId()));
		assertEquals(folder1Id, KeyFactory.stringToKey(folder2.getProjectId()));
		String benefactor = nodeInheritanceDAO.getBenefactor(""+folder2Id);
		// should be its own benefactor
		assertEquals(folder2Id, KeyFactory.stringToKey(benefactor));
		
		// The etag should have changed.
		Node oldNode = hierarchy.get(4);
		assertEquals(oldNode.getId(), folder2.getId());
		assertFalse(folder2.getETag().equals(oldNode.getETag()));
		
		// file1
		Node file1 = nodeDao.getNode(""+file1Id);
		assertEquals(folder0Id, KeyFactory.stringToKey(file1.getParentId()));
		assertEquals(folder2Id, KeyFactory.stringToKey(file1.getProjectId()));
		benefactor = nodeInheritanceDAO.getBenefactor(""+file1Id);
		// should be its own benefactor
		assertEquals(file1Id, KeyFactory.stringToKey(benefactor));
		
		// file2 with nulls
		Node file2 = nodeDao.getNode(""+file2Id);
		assertEquals(null, file2.getParentId());
		assertEquals(null, file2.getProjectId());
		benefactor = nodeInheritanceDAO.getBenefactor(""+file2Id);
		// should be its own benefactor
		assertEquals("syn0", benefactor);
	}
	
	@Test
	public void testGetNodeIdByAlias(){
		Node node = privateCreateNew("testGetNodeIdByAlias");
		String alias = UUID.randomUUID().toString();
		node.setAlias(alias);
		node.setVersionComment("v1");
		node.setVersionLabel("1");
		String id = nodeDao.createNew(node);
		toDelete.add(id);
		assertNotNull(id);
		// call under test
		String lookupId = nodeDao.getNodeIdByAlias(alias);
		assertEquals(id, lookupId);
	}
	
	@Test (expected=NotFoundException.class)
	public void testGetNodeIdByAliasNotFound(){
		String alias = "doesNotExist";
		// call under test
		nodeDao.getNodeIdByAlias(alias);
	}
	
	@Test
	public void testGetProjectId(){
		Node project = NodeTestUtils.createNew("Project", creatorUserGroupId);
		project.setNodeType(EntityType.project);
		String id = nodeDao.createNew(project);
		toDelete.add(id);
		project = nodeDao.getNode(id);
		assertNotNull(project);
		assertEquals(id, project.getProjectId());
		// call under test
		String projectId = nodeDao.getProjectId(id);
		assertEquals(project.getProjectId(), projectId);
	}
	
	@Test (expected=NotFoundException.class)
	public void testGetProjectIdNotFound(){
		String doesNotExist = "syn9999999";
		// call under test
		nodeDao.getProjectId(doesNotExist);
	}
		

	/**
	 * Generate the following Hierarchy:
	 * <ul>
	 * <li>project</li>
	 * <li>folder0->project</li>
	 * <li>folder1->project</li>
	 * <li>file0->project</li>
	 * <li>folder2->folder1->project</li>
	 * <li>file1->folder1->project</li>
	 * <li>file2->folder2->folder1->project</li>
	 * </ul>
	 * 
	 * @return
	 */
	private List<Node> createHierarchy(){
		List<Node> resutls = new LinkedList<Node>();
		// Create a project
		Node project = NodeTestUtils.createNew("hierarchy", creatorUserGroupId);
		project.setNodeType(EntityType.project);
		String projectId = nodeDao.createNew(project);
		resutls.add(nodeDao.getNode(projectId));
		toDelete.add(projectId);
		
		String levelOneFolderId = null;
		// Create two folders
		for(int i=0; i<2; i++){
			Node folder = NodeTestUtils.createNew("folder"+i, creatorUserGroupId);
			folder.setNodeType(EntityType.folder);
			folder.setParentId(projectId);
			levelOneFolderId = nodeDao.createNew(folder);
			resutls.add(nodeDao.getNode(levelOneFolderId));
			toDelete.add(levelOneFolderId);
		}
		
		// file0
		Node file = NodeTestUtils.createNew("file0", creatorUserGroupId);
		file.setNodeType(EntityType.file);
		file.setParentId(projectId);
		file.setFileHandleId(fileHandle.getId());
		String fileId = nodeDao.createNew(file);
		resutls.add(nodeDao.getNode(fileId));
		toDelete.add(fileId);
		
		// folder2
		Node folder = NodeTestUtils.createNew("folder2", creatorUserGroupId);
		folder.setNodeType(EntityType.folder);
		folder.setParentId(levelOneFolderId);
		String folder2Id = nodeDao.createNew(folder);
		resutls.add(nodeDao.getNode(folder2Id));
		toDelete.add(folder2Id);
		
		// file1
		file = NodeTestUtils.createNew("file1", creatorUserGroupId);
		file.setNodeType(EntityType.file);
		file.setParentId(levelOneFolderId);
		file.setFileHandleId(fileHandle.getId());
		fileId = nodeDao.createNew(file);
		resutls.add(nodeDao.getNode(fileId));
		toDelete.add(fileId);
		
		// file2
		file = NodeTestUtils.createNew("file2", creatorUserGroupId);
		file.setNodeType(EntityType.file);
		file.setParentId(folder2Id);
		file.setFileHandleId(fileHandle.getId());
		fileId = nodeDao.createNew(file);
		resutls.add(nodeDao.getNode(fileId));
		// Set file2 to be its own benefactor
		nodeInheritanceDAO.addBeneficiary(fileId, fileId);
		toDelete.add(fileId);
		
		return resutls;
	}
	
	private UserInfo createUserInfo(String user, boolean isAdmin) throws NotFoundException {
		UserInfo userInfo = new UserInfo(isAdmin, Long.parseLong(user));
		Set<Long> groups = new HashSet<Long>();
		groups.add(BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId());
		groups.add(Long.parseLong(user));
		groups.add(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId());
		// Add all groups the user belongs to
		List<UserGroup> groupFromDAO = groupMembersDAO.getUsersGroups(user);
		for (UserGroup ug : groupFromDAO) {
			groups.add(Long.parseLong(ug.getId()));
		}
		userInfo.setGroups(groups);
		return userInfo;
	}

	private String createProject(String projectName, String user) throws Exception {
		return createProject(projectName, user, StackConfiguration.getRootFolderEntityIdStatic());
	}

	private String createProject(String projectName, String user, String parentId) throws Exception {
		Thread.sleep(2); // ensure ordering by creation date
		Node project = NodeTestUtils.createNew(projectName + "-" + new Random().nextInt(), Long.parseLong(user));
		project.setId(KeyFactory.keyToString(idGenerator.generateNewId()));
		project.setParentId(parentId);
		String projectId = this.nodeDao.createNew(project);
		toDelete.add(projectId);
		nodeInheritanceDAO.addBeneficiary(projectId, projectId);
		return projectId;
	}

	private void addReadAcl(String entity, String... usersToAdd) throws Exception {
		Set<ResourceAccess> ras = Sets.newHashSet();
		for (String userToAdd : usersToAdd) {
			ResourceAccess ra = new ResourceAccess();
			ra.setAccessType(Sets.newHashSet(ACCESS_TYPE.READ));
			ra.setPrincipalId(Long.parseLong(userToAdd));
			ras.add(ra);
		}
		AccessControlList acl = new AccessControlList();
		acl.setResourceAccess(ras);
		acl.setId(entity);
		acl.setCreationDate(new Date());
		accessControlListDAO.create(acl, ObjectType.ENTITY);
		nodeInheritanceDAO.addBeneficiary(entity, entity);
	}


	/*
	 * Private Methods
	 */
	private Activity createTestActivity(Long id) {
		Activity act = new Activity();
		act.setId(id.toString());
		act.setCreatedBy(creatorUserGroupId.toString());
		act.setCreatedOn(new Date(System.currentTimeMillis()));
		act.setModifiedBy(creatorUserGroupId.toString());
		act.setModifiedOn(new Date(System.currentTimeMillis()));
		activityDAO.create(act);
		activitiesToDelete.add(act.getId());
		return act;
	}
	
	/**
	 * Create a test FileHandle
	 * @param fileName
	 * @param createdById
	 * @return
	 */
	private S3FileHandle createTestFileHandle(String fileName, String createdById){
		S3FileHandle fileHandle = new S3FileHandle();
		fileHandle.setBucketName("bucket");
		fileHandle.setKey("key");
		fileHandle.setCreatedBy(createdById);
		fileHandle.setFileName(fileName);
		fileHandle.setContentMd5(fileName);
		fileHandle.setContentSize(TEST_FILE_SIZE);
		fileHandle = fileHandleDao.createFile(fileHandle);
		fileHandlesToDelete.add(fileHandle.getId());
		return fileHandle;
	}

}
