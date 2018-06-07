package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.sagebionetworks.repo.model.dbo.dao.NodeDAOImpl.TRASH_FOLDER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_PARENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NODE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
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
import org.sagebionetworks.repo.model.IdAndEtag;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.LimitExceededException;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.NodeIdAndType;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ProjectHeader;
import org.sagebionetworks.repo.model.ProjectListSortColumn;
import org.sagebionetworks.repo.model.ProjectListType;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.VersionInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableDAO;
import org.sagebionetworks.repo.model.entity.Direction;
import org.sagebionetworks.repo.model.entity.SortBy;
import org.sagebionetworks.repo.model.entity.query.SortDirection;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.table.AnnotationDTO;
import org.sagebionetworks.repo.model.table.AnnotationType;
import org.sagebionetworks.repo.model.table.EntityDTO;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class NodeDAOImplTest {

	public static final long TEST_FILE_SIZE = 1234567l;

	@Autowired
	private NodeDAO nodeDao;
	
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
	private PlatformTransactionManager txManager;
	
	@Autowired
	private MigratableTableDAO migratableTableDao;;
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
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

	private final String rootID = KeyFactory.keyToString(KeyFactory.ROOT_ID);
	
	UserInfo adminUser;

	@Before
	public void before() throws Exception {
		creatorUserGroupId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		altUserGroupId = BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId();
		adminUser = new UserInfo(true, creatorUserGroupId);
		
		assertNotNull(nodeDao);
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
	}
	
	@Test (expected=NotFoundException.class)
	public void testGetNodeNotFound(){
		nodeDao.getNode("syn123");
	}
	
	@Test (expected=NotFoundException.class)
	public void testGetNodeVersionNotFound(){
		nodeDao.getNodeForVersion("syn123", 1L);
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
		toCreate.setParentId(""+TRASH_FOLDER_ID);
		// put the node in the trash
		String id = nodeDao.createNew(toCreate);
		toDelete.add(id);
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
		// add an acl for this node.
		AccessControlList acl = AccessControlListUtil.createACLToGrantEntityAdminAccess(id, adminUser, new Date());
		accessControlListDAO.create(acl, ObjectType.ENTITY);
		Long nodeId = KeyFactory.stringToKey(id);
		// call under test.
		boolean avaiable = nodeDao.isNodeAvailable(nodeId);
		assertTrue(avaiable);
	}
	
	@Test
	public void testIsNodeAvailableAndInTrash(){
		Node toCreate = privateCreateNewDistinctModifier("available");
		toCreate.setParentId(""+TRASH_FOLDER_ID);
		String id = nodeDao.createNew(toCreate);
		toDelete.add(id);
		// put the node in the trash
//		nodeInheritanceDAO.addBeneficiary(id, ""+TRASH_FOLDER_ID);
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
		long idLong = idGenerator.generateNewId(IdType.ENTITY_ID) + 10;
		String idString = KeyFactory.keyToString(new Long(idLong));
		Node toCreate = privateCreateNew("secondNodeEver");
		toCreate.setId(idString);
		String fetchedId = nodeDao.createNew(toCreate);
		toDelete.add(fetchedId);
		// The id should be the same as what we provided
		assertEquals(idString, fetchedId);
		// Also make sure the ID generator was increment to reserve this ID.
		long nextId = idGenerator.generateNewId(IdType.ENTITY_ID);
		assertEquals(idLong+1, nextId);
	}
	
	@Test
	public void testCreateWithIdGreaterThanIdGenerator() throws Exception{
		// Create a node with a specific id
		String id = KeyFactory.keyToString(new Long(idGenerator.generateNewId(IdType.ENTITY_ID)+10));
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
		// Make sure we can fetch it
		Node childLoaded = nodeDao.getNode(childId);
		assertNotNull(childLoaded);
		assertEquals(parentId, childLoaded.getParentId());
		// This child should be inheriting from its parent by default
		String childBenefactorId = nodeDao.getBenefactor(childId);
		assertEquals(parentId, childBenefactorId);
		
		// now add a grandchild
		Node grandkid = privateCreateNew("grandchild");
		grandkid.setParentId(childId);
		String grandkidId = nodeDao.createNew(grandkid);
		assertNotNull(grandkidId);

		// This grandchild should be inheriting from its grandparent by default
		String grandChildBenefactorId = nodeDao.getBenefactor(grandkidId);
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
		
		// Get the latest version
		Node currentNode = nodeDao.getNode(id);

		// Make sure we can fetch each version
		for(Long versionNumber: versionNumbers){
			Node nodeVersion = nodeDao.getNodeForVersion(id, versionNumber);
			assertNotNull(nodeVersion);
			// The current version should have an etag, all other version should have the zero etag.
			if(currentNode.getVersionNumber().equals(nodeVersion.getVersionNumber())){
				assertEquals(currentNode.getETag(), nodeVersion.getETag());
			}else{
				assertEquals(NodeConstants.ZERO_E_TAG, nodeVersion.getETag());
			}
			assertEquals(versionNumber, nodeVersion.getVersionNumber());
		}
	}
	
	@Test
	public void testGetVersionInfo() throws Exception {
		// Create a number of versions
		int numberVersions = 10;
		String id = createNodeWithMultipleVersions(numberVersions);
		// Now list the versions
		List<VersionInfo> versionsOfEntity = nodeDao.getVersionsOfEntity(id, 0, 10);
		assertNotNull(versionsOfEntity);
		assertEquals(numberVersions,versionsOfEntity.size());
		VersionInfo firstResult = versionsOfEntity.get(0);
		assertEquals(new Long(numberVersions), firstResult.getVersionNumber());
		//verify content size
		assertEquals(Long.toString(TEST_FILE_SIZE), firstResult.getContentSize());
		//verify md5 (is set to filename in our test filehandle)
		assertEquals(fileHandle.getFileName(), firstResult.getContentMd5());
		
		// Get the latest version
		Node currentNode = nodeDao.getNode(id);

		assertEquals(new Long(1), versionsOfEntity.get(versionsOfEntity.size()-1).getVersionNumber());
		for (VersionInfo vi : versionsOfEntity) {
			Node nodeVersion = nodeDao.getNodeForVersion(id, vi.getVersionNumber());
			// The current version should have an etag, all other version should have the zero etag.
			if(currentNode.getVersionNumber().equals(nodeVersion.getVersionNumber())){
				assertEquals(currentNode.getETag(), nodeVersion.getETag());
			}else{
				assertEquals(NodeConstants.ZERO_E_TAG, nodeVersion.getETag());
			}
			Date modDate = nodeVersion.getModifiedOn();
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
	
	//mySQL seems to have a limit of 15 for the number of delete cascades to prevent infinite loops. https://dev.mysql.com/doc/mysql-reslimits-excerpt/5.5/en/ansi-diff-foreign-keys.html
	@Test (expected = DataIntegrityViolationException.class)
	public void testDeleteCascadeMax(){
		List<String> nodeIds = createNestedNodes(15);
		//delete the parent node 
		nodeDao.delete(nodeIds.get(0));
	}
	
	//anything less than 15 works
	@Test
	public void testDeleteCascadeNotMax(){
		List<String> nodeIds = createNestedNodes(14);
		//delete the parent node 
		nodeDao.delete(nodeIds.get(0));
		//check that all added nodes were deleted 
		for(String nodeID : nodeIds){
			assertFalse(nodeDao.doesNodeExist(KeyFactory.stringToKey(nodeID)));
		}
	}
	
	/**
	 * <pre>
	 * numLevels amount of nodes each referencing the previous
	 *    root
	 *     |
	 *   Node1
	 *     |
	 *   Node2
	 *     |
	 *     .
	 *     .
	 *     .
	 *     |
	 *    Node{numLevels}
	 * </pre>
	 * @param numLevels number of chained nodes to creates
	 * @return List of all created nodes' ids ordered by level ascendingly
	 * @throws DataIntegrityViolationException
	 */
	private List<String> createNestedNodes(int numLevels) throws DataIntegrityViolationException{
		
		/*

		  
		*/
		
		List<String> nodeIDs = new ArrayList<String>();
		
		for(int i = 0; i < numLevels; i++){
			String nodeName = "NodeDAOImplTest.createNestedNodes() Node:" + i;
			Node node = new Node();
			
			//set fields for the new node
			Date now = new Date();
			node.setName(nodeName);
			node.setParentId( nodeIDs.isEmpty() ? rootID : nodeIDs.get(nodeIDs.size() - 1) );//previous added node is the parent
			node.setNodeType(EntityType.project);
			node.setModifiedByPrincipalId(creatorUserGroupId);
			node.setModifiedOn(now);
			node.setCreatedOn(now);
			node.setCreatedByPrincipalId(creatorUserGroupId);
			
			//create the node in the database and update the parentid to that of the new node
			String nodeID = nodeDao.createNew(node);
			assertNotNull(nodeID);
			
			nodeIDs.add(nodeID);
			toDelete.add(0, nodeID);//have to delete the nodes in reverse order or else will not be able to clean up if test fails
		}
		assertTrue(nodeIDs.size() == numLevels);
		
		return nodeIDs;
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testDeleteListNullList(){
		nodeDao.delete((List<Long>) null);
	}
	
	@Test
	public void testDeleteListEmptyList(){
		assertEquals(0, nodeDao.delete(new ArrayList<Long>()));
	}
	
	
	@Test
	public void testDeleteListLeavesOnly(){
		List<Long> nodeIDs = new ArrayList<Long>();
		int numNodes = 2; 
		
		//create numNodes amount of Nodes. all children of the root
		for(int i = 0; i < numNodes; i++){
			String nodeName = "NodeDAOImplTest.testDeleteList() Node:" + i;
			Node node = new Node();
			
			//set fields for the new node
			Date now = new Date();
			node.setName(nodeName);
			node.setParentId( rootID );//previous added node is the parent
			node.setNodeType(EntityType.project);
			node.setModifiedByPrincipalId(creatorUserGroupId);
			node.setModifiedOn(now);
			node.setCreatedOn(now);
			node.setCreatedByPrincipalId(creatorUserGroupId);
			
			//create the node in the database and update the parentid to that of the new node
			String nodeID = nodeDao.createNew(node);
			assertNotNull(nodeID);
			
			nodeIDs.add(KeyFactory.stringToKey(nodeID));
			toDelete.add(nodeID);//add to cleanup list in case test fails
		}
		assertEquals(numNodes, nodeIDs.size());
		
		//check that the nodes were added
		for(Long nodeID : nodeIDs){
			assertTrue(nodeDao.doesNodeExist(nodeID));
		}
		
		//delete the nodes
		nodeDao.delete(nodeIDs);
		
		//check that the nodes no longer exist
		for(Long nodeID : nodeIDs){
			assertFalse(nodeDao.doesNodeExist(nodeID));
		}
	}
	@Test
	public void testDeleteListOfNodeWithChildren(){
		List<String> stringTypeNodeIds = createNestedNodes(2);//1 child
		List<Long> listParentOnly = new ArrayList<Long>();
		
		//only add the root parent
		listParentOnly.add(KeyFactory.stringToKey(stringTypeNodeIds.get(0)));
		
		nodeDao.delete(listParentOnly);
	}
	
	
	@Test (expected = DataIntegrityViolationException.class)
	public void testDeleteListCascadeMax(){
		List<String> stringTypeNodeIds = createNestedNodes(15);
		List<Long> listParentOnly = new ArrayList<Long>();
		
		//only add the root parent
		listParentOnly.add(KeyFactory.stringToKey(stringTypeNodeIds.get(0)));
		
		nodeDao.delete(listParentOnly);
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
		// add an acl for the parent
		AccessControlList acl = AccessControlListUtil.createACLToGrantEntityAdminAccess(parentId, adminUser, new Date());
		accessControlListDAO.create(acl, ObjectType.ENTITY);
		
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
		long id = idGenerator.generateNewId(IdType.ENTITY_ID);
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
	public void testGetProjectStatAdditionalConditionMY_CREATED_PROJECTS(){
		Map<String, Object> parameters = new HashMap<>();
		Long userId = 123L;
		ProjectListType type = ProjectListType.MY_CREATED_PROJECTS;
		String result = NodeDAOImpl.getProjectStatAdditionalCondition(parameters, userId, type);
		assertEquals(" AND n.CREATED_BY = :bCreatedBy", result);
		assertEquals(userId, parameters.get("bCreatedBy"));
	}
	
	@Test
	public void testGetProjectStatAdditionalConditionMY_PARTICIPATED_PROJECTS(){
		Map<String, Object> parameters = new HashMap<>();
		Long userId = 123L;
		ProjectListType type = ProjectListType.MY_PARTICIPATED_PROJECTS;
		String result = NodeDAOImpl.getProjectStatAdditionalCondition(parameters, userId, type);
		assertEquals(" AND n.CREATED_BY <> :bCreatedBy", result);
		assertEquals(userId, parameters.get("bCreatedBy"));
	}
	
	@Test
	public void testGetProjectStatAdditionalConditionMY_PROJECTS(){
		Map<String, Object> parameters = new HashMap<>();
		Long userId = 123L;
		ProjectListType type = ProjectListType.MY_PROJECTS;
		String result = NodeDAOImpl.getProjectStatAdditionalCondition(parameters, userId, type);
		assertEquals("", result);
		assertTrue(parameters.isEmpty());
	}
	
	@Test
	public void testGetProjectStatAdditionalConditionMY_TEAM_PROJECTS(){
		Map<String, Object> parameters = new HashMap<>();
		Long userId = 123L;
		ProjectListType type = ProjectListType.MY_TEAM_PROJECTS;
		String result = NodeDAOImpl.getProjectStatAdditionalCondition(parameters, userId, type);
		assertEquals("", result);
		assertTrue(parameters.isEmpty());
	}
	
	@Test
	public void testGetProjectStatAdditionalConditionOTHER_USER_PROJECTS(){
		Map<String, Object> parameters = new HashMap<>();
		Long userId = 123L;
		ProjectListType type = ProjectListType.OTHER_USER_PROJECTS;
		String result = NodeDAOImpl.getProjectStatAdditionalCondition(parameters, userId, type);
		assertEquals("", result);
		assertTrue(parameters.isEmpty());
	}
	
	@Test
	public void testGetProjectStatAdditionalConditionTEAM_PROJECTS(){
		Map<String, Object> parameters = new HashMap<>();
		Long userId = 123L;
		ProjectListType type = ProjectListType.TEAM_PROJECTS;
		String result = NodeDAOImpl.getProjectStatAdditionalCondition(parameters, userId, type);
		assertEquals("", result);
		assertTrue(parameters.isEmpty());
	}
	
	/**
	 * Must work for each type.
	 */
	@Test
	public void testGetProjectStatAdditionalConditionEachType(){
		Map<String, Object> parameters = new HashMap<>();
		Long userId = 123L;
		for(ProjectListType type : ProjectListType.values()){
			String result = NodeDAOImpl.getProjectStatAdditionalCondition(parameters, userId, type);
			assertNotNull(result);
		}
	}
	
	@Test
	public void testGetProjectStatsOderByAndPagingLAST_ACTIVITY(){
		Map<String, Object> parameters = new HashMap<>();
		ProjectListSortColumn sortColumn = ProjectListSortColumn.LAST_ACTIVITY;
		SortDirection sortDirection = SortDirection.ASC;
		Long limit = 10L;
		Long offset = 1L;
		String result = NodeDAOImpl.getProjectStatsOderByAndPaging(parameters, sortColumn, sortDirection, limit, offset);
		assertEquals(" ORDER BY coalesce(ps.LAST_ACCESSED, n.CREATED_ON) ASC limit :limitVal offset :offsetVal", result);
		assertEquals(limit, parameters.get("limitVal"));
		assertEquals(offset, parameters.get("offsetVal"));
	}
	
	@Test
	public void testGetProjectStatsOderByAndPagingPROJECT_NAME(){
		Map<String, Object> parameters = new HashMap<>();
		ProjectListSortColumn sortColumn = ProjectListSortColumn.PROJECT_NAME;
		SortDirection sortDirection = SortDirection.DESC;
		Long limit = 10L;
		Long offset = 1L;
		String result = NodeDAOImpl.getProjectStatsOderByAndPaging(parameters, sortColumn, sortDirection, limit, offset);
		assertEquals(" ORDER BY n.NAME COLLATE 'latin1_general_ci' DESC limit :limitVal offset :offsetVal", result);
		assertEquals(limit, parameters.get("limitVal"));
		assertEquals(offset, parameters.get("offsetVal"));
	}
	
	@Test
	public void testGetProjectStatsOderByAndPagingEachType(){
		Map<String, Object> parameters = new HashMap<>();
		SortDirection sortDirection = SortDirection.DESC;
		Long limit = 10L;
		Long offset = 1L;
		for(ProjectListSortColumn sortColumn: ProjectListSortColumn.values()){
			String result = NodeDAOImpl.getProjectStatsOderByAndPaging(parameters, sortColumn, sortDirection, limit, offset);
			assertNotNull(result);
		}
	}
	
	
	@Test
	public void testGetProjectHeaders() throws Exception{
		Long user1Id = Long.parseLong(user1);
		Node projectOne = createProject("testGetProjectHeaders.one", user1);
		Node projectTwo = createProject("testGetProjectHeaders.two", user1);
		Node projectThree = createProject("testGetProjectHeaders.three", user1);
		Set<Long> projectIds = Sets.newHashSet(KeyFactory.stringToKey(projectTwo.getId()), KeyFactory.stringToKey(projectThree.getId()));
		ProjectListType type = ProjectListType.MY_CREATED_PROJECTS;
		ProjectListSortColumn sortColumn = ProjectListSortColumn.LAST_ACTIVITY;
		SortDirection sortDirection = SortDirection.ASC;
		Long limit = 10L;
		Long offset = 0L;
		// call under test
		List<ProjectHeader> results = nodeDao.getProjectHeaders(user1Id, projectIds, type, sortColumn, sortDirection, limit, offset);
		assertNotNull(results);
		assertEquals(2, results.size());
		ProjectHeader first = results.get(0);
		assertEquals(projectTwo.getId(), first.getId());
		assertEquals(projectTwo.getName(), first.getName());
		assertEquals(projectTwo.getCreatedByPrincipalId(), first.getModifiedBy());
		assertEquals(projectTwo.getModifiedOn(), first.getModifiedOn());
		
		ProjectHeader second = results.get(0);
		assertEquals(second.getId(), second.getId());
	}
	
	@Test
	public void testGetProjectHeadersEmpty() throws Exception{
		Long user1Id = Long.parseLong(user1);
		// empty project ids should return an empty set.
		Set<Long> projectIds = new HashSet<>();
		ProjectListType type = ProjectListType.MY_CREATED_PROJECTS;
		ProjectListSortColumn sortColumn = ProjectListSortColumn.LAST_ACTIVITY;
		SortDirection sortDirection = SortDirection.ASC;
		Long limit = 10L;
		Long offset = 0L;
		// call under test
		List<ProjectHeader> results = nodeDao.getProjectHeaders(user1Id, projectIds, type, sortColumn, sortDirection, limit, offset);
		assertNotNull(results);
		assertTrue(results.isEmpty());
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
	public void testGetAllContainerIds() throws Exception {
		// Generate some hierarchy
		List<Node> hierarchy = createHierarchy();
		assertEquals(7, hierarchy.size());
		int maxIds = hierarchy.size()+1;
		Long projectId = KeyFactory.stringToKey(hierarchy.get(0).getId());
		Long folder0Id = KeyFactory.stringToKey(hierarchy.get(1).getId());
		Long folder1Id = KeyFactory.stringToKey(hierarchy.get(2).getId());
		Long folder2Id = KeyFactory.stringToKey(hierarchy.get(4).getId());

		// Lookup all of the containers in this hierarchy
		Set<Long> containers = nodeDao.getAllContainerIds(Arrays.asList(projectId), maxIds);
		Set<Long> expected = new LinkedHashSet<Long>(Lists.newArrayList(
				projectId, folder0Id, folder1Id, folder2Id
		));
		assertEquals(expected, containers);
		
		// Folder1 contains folder2
		containers = nodeDao.getAllContainerIds(Arrays.asList(folder1Id), maxIds);
		expected = new LinkedHashSet<Long>(Lists.newArrayList(
				folder1Id, folder2Id
		));
		assertEquals(expected, containers);
		
		// Folder2 contains nothing
		containers = nodeDao.getAllContainerIds(Arrays.asList(folder2Id), maxIds);
		expected = new LinkedHashSet<Long>(Lists.newArrayList(
				folder2Id
		));
		assertEquals(expected, containers);
	}
	

	/**
	 * Exceed the limit with one page of children.
	 * 
	 * @throws LimitExceededException
	 */
	@Test (expected=LimitExceededException.class)
	public void testGetAllContainerIdsLimitExceededFlat() throws LimitExceededException{
		// Generate some hierarchy
		// Create a project
		Node project = NodeTestUtils.createNew("hierarchy", creatorUserGroupId);
		project.setNodeType(EntityType.project);
		String projectId = nodeDao.createNew(project);
		Long projectIdLong = KeyFactory.stringToKey(projectId);
		toDelete.add(projectId);
		
		// Add three folders to the project
		for(int i=0; i<3; i++){
			Node folder = NodeTestUtils.createNew("folder"+i, creatorUserGroupId);
			folder.setNodeType(EntityType.folder);
			folder.setParentId(projectId);
			String folderId = nodeDao.createNew(folder);
			toDelete.add(folderId);
		}
		// loading more than two from a single page should fail.
		int maxIds = 2;
		// call under test
		nodeDao.getAllContainerIds(Arrays.asList(projectIdLong), maxIds);
	}
	
	/**
	 * Exceed the limit with multiple calls.
	 * 
	 * @throws LimitExceededException
	 */
	@Test (expected=LimitExceededException.class)
	public void testGetAllContainerIdsLimitExceededExpanded() throws LimitExceededException{
		// Generate some hierarchy
		// Create a project
		Node project = NodeTestUtils.createNew("hierarchy", creatorUserGroupId);
		project.setNodeType(EntityType.project);
		String projectId = nodeDao.createNew(project);
		Long projectIdLong = KeyFactory.stringToKey(projectId);
		toDelete.add(projectId);
		// for this test create hierarchy
		String parentId = projectId;
		// Add three folders to the project
		for(int i=0; i<3; i++){
			Node folder = NodeTestUtils.createNew("folder"+i, creatorUserGroupId);
			folder.setNodeType(EntityType.folder);
			folder.setParentId(parentId);
			String folderId = nodeDao.createNew(folder);
			toDelete.add(folderId);
			parentId = folderId;
		}
		// loading more than two from a single page should fail.
		int maxIds = 2;
		// call under test
		nodeDao.getAllContainerIds(Arrays.asList(projectIdLong), maxIds);
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
		project = nodeDao.createNewNode(project);
		toDelete.add(project.getId());
		// add some hierarchy
		Node parent = NodeTestUtils.createNew("parent", creatorUserGroupId);
		parent.setNodeType(EntityType.folder);
		parent.setParentId(project.getId());
		parent = nodeDao.createNewNode(parent);
		toDelete.add(parent.getId());
		Node child = NodeTestUtils.createNew("child", creatorUserGroupId);
		child.setParentId(parent.getId());
		child.setNodeType(EntityType.folder);
		child = nodeDao.createNewNode(child);
		toDelete.add(child.getId());
		
		// call under test
		assertEquals(project.getId(), nodeDao.getProjectId(project.getId()));
		assertEquals(project.getId(), nodeDao.getProjectId(parent.getId()));
		assertEquals(project.getId(), nodeDao.getProjectId(child.getId()));
	}
	
	/**
	 * Test for PLFM-4369.
	 * A timeout for this test means the function entered
	 * into an infinite loop and should be killed.
	 * 
	 * @throws Exception
	 */
	@Test (timeout=20000, expected=NotFoundException.class)
	public void testGetProjectIdChildWithNoParent() throws Exception{
		Node child = setUpChildWithNoParent();
		// Before the fix, this call call would hang with 100% CPU.
		nodeDao.getProjectId(child.getId());
	}
	
	/**
	 * Test for PLFM-4369.
	 * A timeout for this test means the function entered
	 * into an infinite loop and should be killed.
	 * 
	 * @throws Exception
	 */
	@Test (timeout=20000, expected=NotFoundException.class)
	public void testGetBenefactorIdChildWithNoParent() throws Exception{
		Node child = setUpChildWithNoParent();
		// Before the fix, this call call would hang with 100% CPU.
		nodeDao.getBenefactor(child.getId());
	}
	
	/**
	 * Test for PLFM-4369.
	 * A timeout for this test means the function entered
	 * into an infinite loop and should be killed.
	 * @throws Exception
	 */
	@Test (timeout=20000, expected=IllegalStateException.class)
	public void testGetProjectInfiniteLoop() throws Exception{
		String id = setUpChildAsItsOwnParent();
		// Before the fix, this call call would hang with 100% CPU.
		nodeDao.getProjectId(id);
	}
	
	/**
	 * Test for PLFM-4369.
	 * A timeout for this test means the function entered
	 * into an infinite loop and should be killed.
	 * @throws Exception
	 */
	@Test (timeout=20000, expected=IllegalStateException.class)
	public void testGetBenefactorIdInfiniteLoop() throws Exception{
		String id = setUpChildAsItsOwnParent();
		// Before the fix, this call call would hang with 100% CPU.
		nodeDao.getBenefactor(id);
	}
	
	/**
	 * Setup for PLFM-4369.  Setup a case where a child
	 * is its own parent, to test for an infinite loop in a function.
	 * 
	 * @return
	 * @throws Exception
	 */
	String setUpChildAsItsOwnParent() throws Exception {
		Node project = NodeTestUtils.createNew("Project", creatorUserGroupId);
		project.setNodeType(EntityType.folder);
		project = nodeDao.createNewNode(project);
		final Long projectId = KeyFactory.stringToKey(project.getId());
		toDelete.add(project.getId());
		// to delete the parent without deleting the child:
		migratableTableDao.runWithKeyChecksIgnored(new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				jdbcTemplate.update("UPDATE "+TABLE_NODE+" SET "+COL_NODE_PARENT_ID+" = ? WHERE "+COL_NODE_ID+" = ?", projectId, projectId);
				return null;
			}
		});
		return KeyFactory.keyToString(projectId);
	}
	
	

	/**
	 * Setup for PLFM-4369.  This is a case where a child exists
	 * but the child's parent does not exist.
	 * @return
	 * @throws Exception
	 */
	Node setUpChildWithNoParent() throws Exception {
		Node project = NodeTestUtils.createNew("Project", creatorUserGroupId);
		project.setNodeType(EntityType.project);
		project = nodeDao.createNewNode(project);
		final String projectid = project.getId();
		toDelete.add(project.getId());
		// add some hierarchy
		Node child = NodeTestUtils.createNew("parent", creatorUserGroupId);
		child.setNodeType(EntityType.folder);
		child.setParentId(project.getId());
		child = nodeDao.createNewNode(child);
		toDelete.add(child.getId());

		// to delete the parent without deleting the child:
		migratableTableDao.runWithKeyChecksIgnored(new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				nodeDao.delete(projectid);
				return null;
			}
		});
		// the parent should not exist.
		assertFalse(nodeDao.doesNodeExist(KeyFactory.stringToKey(child.getParentId())));
		// the child should exist.
		assertTrue(nodeDao.doesNodeExist(KeyFactory.stringToKey(child.getId())));
		return child;
	}
	
	@Test (expected=NotFoundException.class)
	public void testGetProjectNodeDoesNotEixst(){
		String doesNotExist = "syn9999999";
		// call under test
		nodeDao.getProjectId(doesNotExist);
	}
	
	@Test (expected=NotFoundException.class)
	public void testGetProjectNodeExistsWithNoProject(){
		// create a node that is not in a project.
		Node node = NodeTestUtils.createNew("someNode", creatorUserGroupId);
		node.setNodeType(EntityType.folder);
		node = nodeDao.createNewNode(node);
		toDelete.add(node.getId());
		// call under test
		nodeDao.getProjectId(node.getId());
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetFileHandleIdsAssociatedWithFileEntityNullFileHandleIds(){
		nodeDao.getFileHandleIdsAssociatedWithFileEntity(null, 1L);
	}

	@Test
	public void testGetFileHandleIdsAssociatedWithFileEntityEmptyFileHandleIds(){
		Set<Long> fileHandleIds = nodeDao.getFileHandleIdsAssociatedWithFileEntity(new ArrayList<Long>(0), 1L);
		assertNotNull(fileHandleIds);
		assertTrue(fileHandleIds.isEmpty());
	}

	@Test
	public void testGetFileHandleIdsAssociatedWithFileEntityEmptyFileHandleIdsReturned(){
		List<Long> fileHandleIds = Arrays.asList(1L, 2L);
		Set<Long> foundFileHandleIds = nodeDao.getFileHandleIdsAssociatedWithFileEntity(fileHandleIds, 1L);
		assertNotNull(foundFileHandleIds);
		assertTrue(foundFileHandleIds.isEmpty());
	}

	@Test
	public void testGetFileHandleIdsAssociatedWithFileEntity(){
		Node node = NodeTestUtils.createNew("testGetFileHandleIdsForFileEntity", creatorUserGroupId);
		node.setFileHandleId(fileHandle.getId());
		String id = nodeDao.createNew(node);
		toDelete.add(id);
		long fileHanldeId = Long.parseLong(fileHandle.getId());
		List<Long> fileHandleIds = Arrays.asList(fileHanldeId, 2L);
		Set<Long> foundFileHandleIds = nodeDao.getFileHandleIdsAssociatedWithFileEntity(fileHandleIds, KeyFactory.stringToKey(id));
		assertNotNull(foundFileHandleIds);
		assertEquals(1L, foundFileHandleIds.size());
		assertTrue(foundFileHandleIds.contains(fileHanldeId));

		nodeDao.delete(id);
		fileHandleDao.delete(fileHandle.getId());
	}
	
	@Test
	public void testGetEntityDTOs(){
		Node project = NodeTestUtils.createNew("project", creatorUserGroupId);
		project.setNodeType(EntityType.project);
		project = nodeDao.createNewNode(project);
		toDelete.add(project.getId());
		// Add an ACL at the project
		AccessControlList acl = AccessControlListUtil.createACLToGrantEntityAdminAccess(project.getId(), adminUser, new Date());
		accessControlListDAO.create(acl, ObjectType.ENTITY);
		
		Node file = NodeTestUtils.createNew("folder", creatorUserGroupId);
		file.setNodeType(EntityType.file);
		file.setParentId(project.getId());
		file.setFileHandleId(fileHandle.getId());
		file = nodeDao.createNewNode(file);
		long fileIdLong = KeyFactory.stringToKey(file.getId());
		toDelete.add(file.getId());
		NamedAnnotations annos = new NamedAnnotations();
		annos.setId(file.getId());
		annos.setCreatedBy(file.getCreatedByPrincipalId());
		annos.setCreationDate(file.getCreatedOn());
		annos.setEtag(file.getETag());
		annos.getAdditionalAnnotations().addAnnotation("aString", "someString");
		annos.getAdditionalAnnotations().addAnnotation("aLong", 123L);
		annos.getAdditionalAnnotations().addAnnotation("aDouble", 1.22);
		//Ensure that primary annotations are not included in the entity replication (PLFM-4601)
		annos.getPrimaryAnnotations().addAnnotation("primaryString", "primaryTest");
		nodeDao.updateAnnotations(file.getId(), annos);
		
		int maxAnnotationChars = 10;
		
		// call under test
		List<EntityDTO> results = nodeDao.getEntityDTOs(Lists.newArrayList(project.getId(),file.getId()), maxAnnotationChars);
		assertNotNull(results);
		assertEquals(2, results.size());
		EntityDTO fileDto = results.get(1);
		assertEquals(KeyFactory.stringToKey(file.getId()), fileDto.getId());
		assertEquals(file.getVersionNumber(), fileDto.getCurrentVersion());
		assertEquals(file.getCreatedByPrincipalId(), fileDto.getCreatedBy());
		assertEquals(file.getCreatedOn(), fileDto.getCreatedOn());
		assertEquals(file.getETag(), fileDto.getEtag());
		assertEquals(file.getName(), fileDto.getName());
		assertEquals(file.getNodeType(), fileDto.getType());
		assertEquals(KeyFactory.stringToKey(project.getId()), fileDto.getParentId());
		assertEquals(KeyFactory.stringToKey(project.getId()), fileDto.getBenefactorId());
		assertEquals(KeyFactory.stringToKey(project.getId()), fileDto.getProjectId());
		assertEquals(file.getModifiedByPrincipalId(), fileDto.getModifiedBy());
		assertEquals(file.getModifiedOn(), fileDto.getModifiedOn());
		assertEquals(new Long(Long.parseLong(file.getFileHandleId())), fileDto.getFileHandleId());
		assertNotNull(fileDto.getAnnotations());
		assertEquals(3, fileDto.getAnnotations().size());
		List<AnnotationDTO> expected = Lists.newArrayList(
				new AnnotationDTO(fileIdLong, "aString", AnnotationType.STRING, "someString"),
				new AnnotationDTO(fileIdLong, "aLong", AnnotationType.LONG, "123"),
				new AnnotationDTO(fileIdLong, "aDouble", AnnotationType.DOUBLE, "1.22")
		);
		assertEquals(expected, fileDto.getAnnotations());
		// null checks on the project
		EntityDTO projectDto = results.get(0);
		assertEquals(KeyFactory.stringToKey(project.getId()), projectDto.getId());
		assertEquals(null, projectDto.getParentId());
		assertEquals(projectDto.getId(), projectDto.getBenefactorId());
		assertEquals(projectDto.getId(), projectDto.getProjectId());
		assertEquals(null, projectDto.getFileHandleId());
		assertEquals(null, projectDto.getAnnotations());
	}
	
	@Test
	public void testGetEntityDTOsNullValues(){
		Node project = NodeTestUtils.createNew("project", creatorUserGroupId);
		project.setNodeType(EntityType.project);
		project = nodeDao.createNewNode(project);
		toDelete.add(project.getId());
		
		Node file = NodeTestUtils.createNew("folder", creatorUserGroupId);
		file.setNodeType(EntityType.file);
		file.setParentId(project.getId());
		file.setFileHandleId(fileHandle.getId());
		file = nodeDao.createNewNode(file);
		long fileIdLong = KeyFactory.stringToKey(file.getId());
		toDelete.add(file.getId());
		NamedAnnotations annos = new NamedAnnotations();
		annos.setId(file.getId());
		annos.setCreatedBy(file.getCreatedByPrincipalId());
		annos.setCreationDate(file.getCreatedOn());
		annos.setEtag(file.getETag());
		// added for PLFM_4184
		annos.getAdditionalAnnotations().getStringAnnotations().put("emptyList", new LinkedList<String>());
		// added for PLFM-4224
		annos.getAdditionalAnnotations().getLongAnnotations().put("nullList", null);
		annos.getAdditionalAnnotations().getDoubleAnnotations().put("listWithNullValue", Lists.newArrayList((Double)null));
		nodeDao.updateAnnotations(file.getId(), annos);
		
		int maxAnnotationChars = 10;
		
		// call under test
		List<EntityDTO> results = nodeDao.getEntityDTOs(Lists.newArrayList(project.getId(),file.getId()), maxAnnotationChars);
		assertNotNull(results);
		assertEquals(2, results.size());
		EntityDTO fileDto = results.get(1);
		assertEquals(KeyFactory.stringToKey(file.getId()), fileDto.getId());
		assertNotNull(fileDto.getAnnotations());
		assertEquals(0, fileDto.getAnnotations().size());
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
		AccessControlList acl = AccessControlListUtil.createACLToGrantEntityAdminAccess(fileId, adminUser, new Date());
		accessControlListDAO.create(acl, ObjectType.ENTITY);
		toDelete.add(fileId);
		
		return resutls;
	}

	private Node createProject(String projectName, String user) throws Exception {
		return createProject(projectName, user, StackConfigurationSingleton.singleton().getRootFolderEntityId());
	}

	private Node createProject(String projectName, String user, String parentId) throws Exception {
		Thread.sleep(2); // ensure ordering by creation date
		Node project = NodeTestUtils.createNew(projectName + "-" + new Random().nextInt(), Long.parseLong(user));
		project.setId(KeyFactory.keyToString(idGenerator.generateNewId(IdType.ENTITY_ID)));
		project.setParentId(parentId);
		project = this.nodeDao.createNewNode(project);
		toDelete.add(project.getId());
		return project;
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
		fileHandle.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		fileHandle.setEtag(UUID.randomUUID().toString());
		fileHandle = (S3FileHandle) fileHandleDao.createFile(fileHandle);
		fileHandlesToDelete.add(fileHandle.getId());
		return fileHandle;
	}
	
	@Test
	public void testGetFragmentExcludeNodeIdsNull(){
		Set<Long> toExclude = null;
		String result = NodeDAOImpl.getFragmentExcludeNodeIds(toExclude);
		assertEquals("", result);
	}
	
	@Test
	public void testGetFragmentExcludeNodeIdsEmpty(){
		Set<Long> toExclude = new HashSet<Long>();
		String result = NodeDAOImpl.getFragmentExcludeNodeIds(toExclude);
		assertEquals("", result);
	}
	
	@Test
	public void testGetFragmentExcludeNodeIdsNotEmpty(){
		Set<Long> toExclude = Sets.newHashSet(111L);
		String result = NodeDAOImpl.getFragmentExcludeNodeIds(toExclude);
		assertEquals(NodeDAOImpl.SQL_ID_NOT_IN_SET, result);
	}
	
	@Test
	public void testGetFragmentSortColumnName(){
		SortBy sortBy = SortBy.NAME;
		String result = NodeDAOImpl.getFragmentSortColumn(sortBy);
		assertEquals(NodeDAOImpl.N_NAME, result);
	}
	
	@Test
	public void testGetFragmentSortColumnCreatedOn(){
		SortBy sortBy = SortBy.CREATED_ON;
		String result = NodeDAOImpl.getFragmentSortColumn(sortBy);
		assertEquals(NodeDAOImpl.N_CREATED_ON, result);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetFragmentSortColumnNull(){
		SortBy sortBy = null;
		NodeDAOImpl.getFragmentSortColumn(sortBy);
	}
	
	/**
	 * This test will fail if a new SortBy is added without
	 * extending the method.
	 */
	@Test
	public void testGetFragmentSortColumnAllTypes(){
		// All known types should work
		for(SortBy sort: SortBy.values()){
			String result = NodeDAOImpl.getFragmentSortColumn(sort);
			assertNotNull(result);
		}
	}
	
	@Test
	public void testGetChildrenNoResults(){
		String parentId = "syn123";
		List<EntityType> includeTypes = Lists.newArrayList(EntityType.file);
		Set<Long> childIdsToExclude = null;
		SortBy sortBy = SortBy.NAME;
		Direction sortDirection = Direction.ASC;
		long limit = 10L;
		long offset = 0L;
		List<EntityHeader> results = nodeDao.getChildren(parentId, includeTypes, childIdsToExclude, sortBy, sortDirection, limit, offset);
		assertNotNull(results);
		assertTrue(results.isEmpty());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetChildrenNullParentId(){
		String parentId = null;
		List<EntityType> includeTypes = Lists.newArrayList(EntityType.file);
		Set<Long> childIdsToExclude = null;
		SortBy sortBy = SortBy.NAME;
		Direction sortDirection = Direction.ASC;
		long limit = 10L;
		long offset = 0L;
		nodeDao.getChildren(parentId, includeTypes, childIdsToExclude, sortBy, sortDirection, limit, offset);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetChildrenNullTypes(){
		String parentId = "syn123";
		List<EntityType> includeTypes = null;
		Set<Long> childIdsToExclude = null;
		SortBy sortBy = SortBy.NAME;
		Direction sortDirection = Direction.ASC;
		long limit = 10L;
		long offset = 0L;
		nodeDao.getChildren(parentId, includeTypes, childIdsToExclude, sortBy, sortDirection, limit, offset);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetChildrenEmptyTypes(){
		String parentId = "syn123";
		List<EntityType> includeTypes = new LinkedList<EntityType>();
		Set<Long> childIdsToExclude = null;
		SortBy sortBy = SortBy.NAME;
		Direction sortDirection = Direction.ASC;
		long limit = 10L;
		long offset = 0L;
		nodeDao.getChildren(parentId, includeTypes, childIdsToExclude, sortBy, sortDirection, limit, offset);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetChildrenNullSortByt(){
		String parentId = "syn123";
		List<EntityType> includeTypes = Lists.newArrayList(EntityType.file);
		Set<Long> childIdsToExclude = null;
		SortBy sortBy = null;
		Direction sortDirection = Direction.ASC;
		long limit = 10L;
		long offset = 0L;
		nodeDao.getChildren(parentId, includeTypes, childIdsToExclude, sortBy, sortDirection, limit, offset);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetChildrenNullDirection(){
		String parentId = "syn123";
		List<EntityType> includeTypes = Lists.newArrayList(EntityType.file);
		Set<Long> childIdsToExclude = null;
		SortBy sortBy = SortBy.CREATED_ON;
		Direction sortDirection = null;
		long limit = 10L;
		long offset = 0L;
		nodeDao.getChildren(parentId, includeTypes, childIdsToExclude, sortBy, sortDirection, limit, offset);
	}
	
	@Test
	public void testGetChildren(){
		List<Node> nodes = createHierarchy();
		
		Node project = nodes.get(0);
		// Add an ACL to the project for the benefactor
		AccessControlList acl = AccessControlListUtil.createACLToGrantEntityAdminAccess(project.getId(), adminUser, new Date());
		accessControlListDAO.create(acl, ObjectType.ENTITY);
		
		Node folder1 = nodes.get(1);
		Node folder2 = nodes.get(2);
		
		String parentId = project.getId();
		List<EntityType> includeTypes = Lists.newArrayList(EntityType.folder);
		// exclude folder 1.
		Set<Long> childIdsToExclude = Sets.newHashSet(KeyFactory.stringToKey(folder1.getId()), 111L);
		SortBy sortBy = SortBy.NAME;
		Direction sortDirection = Direction.ASC;
		long limit = 10L;
		long offset = 0L;
		List<EntityHeader> results = nodeDao.getChildren(parentId, includeTypes, childIdsToExclude, sortBy, sortDirection, limit, offset);
		assertNotNull(results);
		assertEquals(1, results.size());
		EntityHeader header = results.get(0);
		assertEquals(folder2.getName(), header.getName());
		assertEquals(folder2.getId(), header.getId());
		assertEquals(EntityTypeUtils.getEntityTypeClassName(EntityType.folder), header.getType());
		assertEquals(folder2.getVersionLabel(), header.getVersionLabel());
		assertEquals(folder2.getVersionNumber(), header.getVersionNumber());
		String benefactorId = nodeDao.getBenefactor(header.getId());
		Long benefactorLong = KeyFactory.stringToKey(benefactorId);
		assertEquals(benefactorLong, header.getBenefactorId());
	}
	
	@Test
	public void testGetChildCount(){
		List<Node> nodes = createHierarchy();
		Node project = nodes.get(0);
		long childCount = nodeDao.getChildCount(project.getId());
		assertEquals(3L, childCount);
	}
	
	@Test
	public void testGetChildCountDoesNotExist(){
		long childCount = nodeDao.getChildCount("syn111");
		assertEquals(0L, childCount);
	}

	@Test
	public void testGetChildrenTwo(){
		List<Node> nodes = createHierarchy();
		
		Node project = nodes.get(0);
		Node folder1 = nodes.get(1);
		Node folder2 = nodes.get(2);
		Node file1 = nodes.get(3);
		
		String parentId = project.getId();
		long limit = 10L;
		long offset = 0L;
		List<NodeIdAndType> results = nodeDao.getChildren(parentId, limit, offset);
		assertNotNull(results);
		assertEquals(3, results.size());
		for(NodeIdAndType result: results){
			if(folder1.getId().equals(result.getNodeId())){
				assertEquals(EntityType.folder, result.getType());
			}else if (folder2.getId().equals(result.getNodeId())){
				assertEquals(EntityType.folder, result.getType());
			}else if (file1.getId().equals(result.getNodeId())){
				assertEquals(EntityType.file, result.getType());
			}else{
				fail("unexpected child");
			}
		}
	}
	
	@Test
	public void testGetChildrenUnknownParentId(){
		String parentId = "syn1";
		long limit = 10L;
		long offset = 0L;
		List<NodeIdAndType> results = nodeDao.getChildren(parentId, limit, offset);
		assertNotNull(results);
		assertEquals(0, results.size());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testLoopupChildWithNullParentId(){
		nodeDao.lookupChild(null, "entityName");
	}

	@Test (expected = IllegalArgumentException.class)
	public void testLoopupChildWithInvalidParentId(){
		nodeDao.lookupChild("parentId", "entityName");
	}

	@Test (expected = IllegalArgumentException.class)
	public void testLoopupChildWithNullEntityName(){
		nodeDao.lookupChild("syn1", null);
	}

	@Test (expected = NotFoundException.class)
	public void testLoopupChildNotFound(){
		nodeDao.lookupChild("syn1", "entityName");
	}

	@Test
	public void testLoopupChildFound(){
		String entityName = "; drop table JDONODE;";
		// Create a project
		Node project = NodeTestUtils.createNew("project", creatorUserGroupId);
		project.setNodeType(EntityType.project);
		String projectId = nodeDao.createNew(project);
		toDelete.add(projectId);
		// create child
		Node child = NodeTestUtils.createNew(entityName, creatorUserGroupId);
		child.setNodeType(EntityType.folder);
		child.setParentId(projectId);
		String childId = nodeDao.createNew(child);
		toDelete.add(childId);
		assertEquals(childId, nodeDao.lookupChild(projectId, entityName));
	}
	
	@Test (expected=NotFoundException.class)
	public void testGetBenefactorEntityDoesNotExist(){
		// should not exist
		nodeDao.getBenefactor("syn9999");
	}
	
	@Test
	public void testGetBenefactorSelf(){
		// create a parent
		Node grandparent = NodeTestUtils.createNew("grandparent", creatorUserGroupId);
		grandparent = nodeDao.createNewNode(grandparent);
		toDelete.add(grandparent.getId());
		
		// There is no ACL for this node
		try{
			nodeDao.getBenefactor(grandparent.getId());
			fail("Does not have a benefactor");
		}catch(NotFoundException expected){
			// expected
		}
		AccessControlList acl = AccessControlListUtil.createACLToGrantEntityAdminAccess(grandparent.getId(), adminUser, new Date());
		// create an ACL with the same ID but wrong type.
		accessControlListDAO.create(acl, ObjectType.EVALUATION);
		try{
			nodeDao.getBenefactor(grandparent.getId());
			fail("Does not have a benefactor");
		}catch(NotFoundException expected){
			// expected
		}
		// Create an ACL with the correct type
		accessControlListDAO.create(acl, ObjectType.ENTITY);
		
		String benefactor = nodeDao.getBenefactor(grandparent.getId());
		assertEquals("Entity should be its own benefactor",grandparent.getId(), benefactor);
	}
	
	@Test
	public void testGetBenefactorNotSelf(){
		// Setup some hierarchy.
		// grandparent
		Node grandparent = NodeTestUtils.createNew("grandparent", creatorUserGroupId);
		grandparent = nodeDao.createNewNode(grandparent);
		toDelete.add(grandparent.getId());
		// parent
		Node parent = NodeTestUtils.createNew("parent", creatorUserGroupId);
		parent.setParentId(grandparent.getId());
		parent = nodeDao.createNewNode(parent);
		toDelete.add(parent.getId());
		// child
		Node child = NodeTestUtils.createNew("child", creatorUserGroupId);
		child.setParentId(parent.getId());
		child = nodeDao.createNewNode(child);
		toDelete.add(child.getId());
		// benefactor does not exist yet
		try{
			nodeDao.getBenefactor(child.getId());
			fail("Does not have a benefactor");
		}catch(NotFoundException expected){
			// expected
		}
		// add an ACL on the grandparent.
		AccessControlList acl = AccessControlListUtil.createACLToGrantEntityAdminAccess(grandparent.getId(), adminUser, new Date());
		accessControlListDAO.create(acl, ObjectType.ENTITY);
		// The benefactor of each should the grandparent.
		assertEquals(grandparent.getId(), nodeDao.getBenefactor(child.getId()));
		assertEquals(grandparent.getId(), nodeDao.getBenefactor(parent.getId()));
		assertEquals(grandparent.getId(), nodeDao.getBenefactor(grandparent.getId()));
	}
	
	
	@Test
	public void testGetSumOfChildCRCsForEachParentEmpty(){
		// Setup some hierarchy.
		List<Long> parentIds = new LinkedList<Long>();
		Map<Long, Long> results = nodeDao.getSumOfChildCRCsForEachParent(parentIds);
		assertNotNull(results);
		assertEquals(0, results.size());
	}
	
	@Test
	public void testGetSumOfChildCRCsForEachParent(){
		// Setup some hierarchy.
		// grandparent
		Node grandparent = NodeTestUtils.createNew("grandparent", creatorUserGroupId);
		grandparent = nodeDao.createNewNode(grandparent);
		Long grandId = KeyFactory.stringToKey(grandparent.getId());
		toDelete.add(grandparent.getId());
		// parent
		Node parent = NodeTestUtils.createNew("parent", creatorUserGroupId);
		parent.setParentId(grandparent.getId());
		parent = nodeDao.createNewNode(parent);
		Long parentId = KeyFactory.stringToKey(parent.getId());
		toDelete.add(parent.getId());
		// child
		Node child = NodeTestUtils.createNew("child", creatorUserGroupId);
		child.setParentId(parent.getId());
		child = nodeDao.createNewNode(child);
		Long childId = KeyFactory.stringToKey(child.getId());
		toDelete.add(child.getId());
		
		Long doesNotExist = -1L;
		List<Long> parentIds = Lists.newArrayList(grandId, parentId, childId, doesNotExist);
		// call under test
		Map<Long, Long> results = nodeDao.getSumOfChildCRCsForEachParent(parentIds);
		assertNotNull(results);
		assertEquals(2, results.size());
		Long crc = results.get(grandId);
		assertNotNull(results.get(grandId));
		assertNotNull(results.get(parentId));
		assertEquals(null, results.get(child));
		assertEquals(null, results.get(doesNotExist));
	}
	
	@Test
	public void testGetChildrenIdAndEtag(){
		// Setup some hierarchy.
		// grandparent
		Node grandparent = NodeTestUtils.createNew("grandparent", creatorUserGroupId);
		grandparent = nodeDao.createNewNode(grandparent);
		Long grandId = KeyFactory.stringToKey(grandparent.getId());
		toDelete.add(grandparent.getId());
		// parent
		Node parent = NodeTestUtils.createNew("parent", creatorUserGroupId);
		parent.setParentId(grandparent.getId());
		parent = nodeDao.createNewNode(parent);
		Long parentId = KeyFactory.stringToKey(parent.getId());
		toDelete.add(parent.getId());
		// child
		Node child = NodeTestUtils.createNew("child", creatorUserGroupId);
		child.setParentId(parent.getId());
		child = nodeDao.createNewNode(child);
		Long childId = KeyFactory.stringToKey(child.getId());
		toDelete.add(child.getId());
		// call under test
		List<IdAndEtag> results = nodeDao.getChildren(grandId);
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals(new IdAndEtag(parentId, parent.getETag()), results.get(0));
		// call under test
		results = nodeDao.getChildren(parentId);
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals(new IdAndEtag(childId, child.getETag()), results.get(0));
		// call under test
		results = nodeDao.getChildren(childId);
		assertNotNull(results);
		assertEquals(0, results.size());
	}
	
	@Test
	public void testGetChildrenIdAndEtagDoesNotExist(){
		Long doesNotExist = -1L;
		// call under test
		List<IdAndEtag> results = nodeDao.getChildren(doesNotExist);
		assertNotNull(results);
		assertEquals(0, results.size());
	}
	
	@Test
	public void testGetAvailableNodesEmpty(){
		List<Long> empty = new LinkedList<Long>();
		Set<Long> availableIds = nodeDao.getAvailableNodes(empty);
		assertNotNull(availableIds);
		assertTrue(availableIds.isEmpty());
	}
	
	@Test
	public void testGetAvailableNodes(){
		// one
		Node one = NodeTestUtils.createNew("one", creatorUserGroupId);
		one = nodeDao.createNewNode(one);
		Long oneId = KeyFactory.stringToKey(one.getId());
		toDelete.add(one.getId());
		// two is in the trash.
		Node two = NodeTestUtils.createNew("two", creatorUserGroupId);
		two.setParentId(""+NodeDAOImpl.TRASH_FOLDER_ID);
		two = nodeDao.createNewNode(two);
		Long twoId = KeyFactory.stringToKey(two.getId());
		toDelete.add(two.getId());
		
		Long doesNotExist = -1L;
		
		List<Long> ids = Lists.newArrayList(oneId, twoId, doesNotExist);
		Set<Long> availableIds = nodeDao.getAvailableNodes(ids);
		assertNotNull(availableIds);
		assertEquals(1, availableIds.size());
		assertTrue(availableIds.contains(oneId));
		assertFalse(availableIds.contains(twoId));
		assertFalse(availableIds.contains(doesNotExist));
	}
}
