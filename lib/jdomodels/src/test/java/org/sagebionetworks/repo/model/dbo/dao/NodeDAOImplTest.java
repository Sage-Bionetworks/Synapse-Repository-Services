package org.sagebionetworks.repo.model.dbo.dao;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.ActivityDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.EntityTypeUtils;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.IdAndAlias;
import org.sagebionetworks.repo.model.IdAndEtag;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.LimitExceededException;
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
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2TestUtils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2Utils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableDAO;
import org.sagebionetworks.repo.model.dbo.persistence.DBONode;
import org.sagebionetworks.repo.model.dbo.persistence.DBORevision;
import org.sagebionetworks.repo.model.dbo.schema.JsonSchemaTestHelper;
import org.sagebionetworks.repo.model.entity.Direction;
import org.sagebionetworks.repo.model.entity.NameIdType;
import org.sagebionetworks.repo.model.entity.SortBy;
import org.sagebionetworks.repo.model.entity.query.SortDirection;
import org.sagebionetworks.repo.model.file.ChildStatsRequest;
import org.sagebionetworks.repo.model.file.ChildStatsResponse;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.helper.DaoObjectHelper;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.schema.BoundObjectType;
import org.sagebionetworks.repo.model.schema.JsonSchemaObjectBinding;
import org.sagebionetworks.repo.model.schema.JsonSchemaVersionInfo;
import org.sagebionetworks.repo.model.table.AnnotationType;
import org.sagebionetworks.repo.model.table.ObjectAnnotationDTO;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.SnapshotRequest;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;
import org.sagebionetworks.repo.web.FileHandleLinkedException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.UnexpectedRollbackException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.sagebionetworks.repo.model.dbo.dao.NodeDAOImpl.TRASH_FOLDER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_PARENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NODE;

@ExtendWith(SpringExtension.class)
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
	private MigratableTableDAO migratableTableDao;;
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DBOBasicDao basicDao;
	
	@Autowired
	private JsonSchemaTestHelper jsonSchemaTestHelper;
	
	@Autowired
	private DaoObjectHelper<Node> nodeDaoHelper;

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
	private S3FileHandle fileHandle3 = null;

	private String user1;
	private String user2;
	private String user3;
	private String group;

	private final String rootID = KeyFactory.keyToString(KeyFactory.ROOT_ID);
	
	UserInfo adminUser;

	@BeforeEach
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
		fileHandle3 = createTestFileHandle("Three", creatorUserGroupId.toString());

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
	}
	
	@AfterEach
	public void after() throws Exception {
		if (group != null) {
			teamDAO.delete(group);
		}
		if(toDelete != null && nodeDao != null){
			for(String id:  toDelete){
				// Delete each
				try{
					nodeDao.deleteTree(id, 100);
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
	
	/**
	 * Cases where all of the optionally fields are null.
	 */
	@Test
	public void testCreateNodeOptionalNull() {
		Node node = new Node();
		node.setName("name");
		node.setCreatedByPrincipalId(creatorUserGroupId);
		node.setModifiedByPrincipalId(creatorUserGroupId);
		node.setCreatedOn(new Date(System.currentTimeMillis()));
		node.setModifiedOn(node.getCreatedOn());
		node.setNodeType(EntityType.folder);
		node.setId(null);
		node.setActivityId(null);
		node.setAlias(null);
		node.setColumnModelIds(null);
		node.setScopeIds(null);
		node.setETag(null);
		node.setParentId(null);
		node.setReference(null);
		node.setFileHandleId(null);
		node.setVersionComment(null);
		node.setVersionLabel(null);
		node.setVersionNumber(null);
		// call under test
		node = nodeDao.createNewNode(node);
		toDelete.add(node.getId());
		assertNotNull(node.getId());
		assertEquals(null, node.getAlias());
		assertEquals(null, node.getActivityId());
		assertEquals(null, node.getColumnModelIds());
		assertEquals(null, node.getScopeIds());
		assertNotNull(node.getETag());
		assertEquals(null, node.getParentId());
		assertEquals(null, node.getReference());
		assertEquals(null, node.getFileHandleId());
		assertEquals(null, node.getVersionComment());
		assertEquals(NodeConstants.DEFAULT_VERSION_LABEL, node.getVersionLabel());
		assertEquals(NodeConstants.DEFAULT_VERSION_NUMBER, node.getVersionNumber());
	}
	
	@Test
	public void testCreateNodeOptionalNotNull() {
		Node parent = NodeTestUtils.createNew("parent", creatorUserGroupId);
		parent = nodeDao.createNewNode(parent);
		toDelete.add(parent.getId());
		
		Node node = new Node();
		node.setName("name");
		node.setCreatedByPrincipalId(creatorUserGroupId);
		node.setModifiedByPrincipalId(creatorUserGroupId);
		node.setCreatedOn(new Date(System.currentTimeMillis()));
		node.setModifiedOn(node.getCreatedOn());
		node.setNodeType(EntityType.project);
		node.setId("syn123");
		node.setActivityId(testActivity.getId());
		node.setAlias("someAlias");
		node.setColumnModelIds(Lists.newArrayList("111","222"));
		node.setScopeIds(Lists.newArrayList("333","444"));
		node.setETag("not the real etag");
		node.setParentId(parent.getId());
		Reference ref = new Reference();
		ref.setTargetId(parent.getId());
		ref.setTargetVersionNumber(0L);
		node.setReference(ref);
		node.setFileHandleId(this.fileHandle.getId());
		node.setVersionComment("a comment");
		node.setVersionLabel(" a label");
		node.setVersionNumber(12L);
		// call under test
		Node afterCreate = nodeDao.createNewNode(node);
		toDelete.add(afterCreate.getId());
		assertNotNull(afterCreate.getId());
		assertEquals(node.getAlias(), afterCreate.getAlias());
		assertEquals(node.getActivityId(), afterCreate.getActivityId());
		assertEquals(node.getColumnModelIds(), afterCreate.getColumnModelIds());
		assertEquals(node.getScopeIds(), afterCreate.getScopeIds());
		assertFalse(afterCreate.getETag().equals(node.getETag()));
		assertEquals(node.getParentId(), afterCreate.getParentId());
		assertEquals(node.getReference(), afterCreate.getReference());
		assertEquals(node.getFileHandleId(), afterCreate.getFileHandleId());
		assertEquals(node.getVersionComment(), afterCreate.getVersionComment());
		assertEquals(node.getVersionLabel(), afterCreate.getVersionLabel());
		assertEquals(NodeConstants.DEFAULT_VERSION_NUMBER, afterCreate.getVersionNumber());
	}
	
	@Test
	public void testCreateNodeNull() {
		Node node = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			nodeDao.createNewNode(node);
		});
	}
	
	@Test
	public void testCreateNodeNullCreatedBy() {
		Node node = new Node();
		node.setName("name");
		node.setCreatedByPrincipalId(null);
		node.setModifiedByPrincipalId(creatorUserGroupId);
		node.setCreatedOn(new Date(System.currentTimeMillis()));
		node.setModifiedOn(node.getCreatedOn());
		node.setNodeType(EntityType.folder);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			nodeDao.createNewNode(node);
		});
	}
	
	@Test
	public void testCreateNodeNullCreatedOn() {
		Node node = new Node();
		node.setName("name");
		node.setCreatedByPrincipalId(creatorUserGroupId);
		node.setModifiedByPrincipalId(null);
		node.setCreatedOn(new Date(System.currentTimeMillis()));
		node.setModifiedOn(node.getCreatedOn());
		node.setNodeType(EntityType.folder);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			nodeDao.createNewNode(node);
		});
	}
	
	@Test
	public void testCreateNodeNullModifiedBy() {
		Node node = new Node();
		node.setName("name");
		node.setCreatedByPrincipalId(creatorUserGroupId);
		node.setModifiedByPrincipalId(creatorUserGroupId);
		node.setCreatedOn(null);
		node.setModifiedOn(new Date(System.currentTimeMillis()));
		node.setNodeType(EntityType.folder);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			nodeDao.createNewNode(node);
		});
	}
	
	@Test
	public void testCreateNodeNullModifiedOn() {
		Node node = new Node();
		node.setName("name");
		node.setCreatedByPrincipalId(creatorUserGroupId);
		node.setModifiedByPrincipalId(creatorUserGroupId);
		node.setCreatedOn(new Date(System.currentTimeMillis()));
		node.setModifiedOn(null);
		node.setNodeType(EntityType.folder);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			nodeDao.createNewNode(node);
		});
	}
	
	@Test
	public void testCreateNodeNullType() {
		Node node = new Node();
		node.setName("name");
		node.setCreatedByPrincipalId(creatorUserGroupId);
		node.setModifiedByPrincipalId(creatorUserGroupId);
		node.setCreatedOn(new Date(System.currentTimeMillis()));
		node.setModifiedOn(node.getCreatedOn());
		node.setNodeType(null);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			nodeDao.createNewNode(node);
		});
	}
	
	@Test
	public void testBootstrapNode() {
		// start with a unique ID
		Long lastId = this.idGenerator.generateNewId(IdType.ENTITY_ID);
		Long boostrapId = new Long(lastId+2);
		Node node  = NodeTestUtils.createNew("foo", creatorUserGroupId);
		// call under test
		node = nodeDao.bootstrapNode(node, boostrapId);
		assertNotNull(node);
		toDelete.add(node.getId());
		assertEquals(boostrapId, KeyFactory.stringToKey(node.getId()));
		// the provided ID should be reserved so the next Id should be larger
		Long nextId = this.idGenerator.generateNewId(IdType.ENTITY_ID);
		assertEquals(new Long(boostrapId+1), nextId);
	}
	
	@Test
	public void testGetNodeNotFound(){
		assertThrows(NotFoundException.class, ()->{
			// call under test
			nodeDao.getNode("syn123");
		});
	}
	
	@Test
	public void testGetNodeVersionNotFound(){
		assertThrows(NotFoundException.class, ()->{
			// call under test
			nodeDao.getNodeForVersion("syn123", 1L);
		});
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
		assertThrows(NotFoundException.class, ()->{
			nodeDao.getNode(childId);
		});
		assertThrows(NotFoundException.class, ()->{
			nodeDao.getNode(grandkidId);
		});
	}

	
	@Test
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
		assertThrows(IllegalArgumentException.class, ()->{
			nodeDao.createNew(child);
		});
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
		// ensure modified on changes.
		Thread.sleep(1);
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
	public void testUpdateOptionalNull() {
		Node node = NodeTestUtils.createNew("foo", creatorUserGroupId);
		node = nodeDao.createNewNode(node);
		toDelete.add(node.getId());
		// set all optional fields to null
		node.setActivityId(null);
		node.setAlias(null);
		node.setParentId(null);
		node.setColumnModelIds(null);
		node.setScopeIds(null);
		node.setReference(null);
		// call under test
		nodeDao.updateNode(node);
		Node updated = nodeDao.getNode(node.getId());
		assertEquals(node, updated);
	}
	
	@Test
	public void testUpdateAllFields() {
		Node parent = NodeTestUtils.createNew("parent", creatorUserGroupId);
		parent = nodeDao.createNewNode(parent);
		toDelete.add(parent.getId());
		
		Node node = NodeTestUtils.createNew("foo", creatorUserGroupId);
		node = nodeDao.createNewNode(node);
		toDelete.add(node.getId());
		// set all fields that can be updated
		node.setActivityId(testActivity.getId());
		node.setAlias("anAlias");
		node.setVersionLabel("updateLabel");
		node.setVersionComment("some comment");
		node.setParentId(parent.getId());
		node.setColumnModelIds(Lists.newArrayList("1","2","3"));
		node.setScopeIds(Lists.newArrayList("4","5","6"));
		Reference ref = new Reference();
		ref.setTargetId("syn789");
		ref.setTargetVersionNumber(22L);
		node.setReference(ref);
		// call under test
		nodeDao.updateNode(node);
		Node updated = nodeDao.getNode(node.getId());
		assertEquals(node, updated);
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
	
	@Test
	public void testNullName() throws Exception{
		Node node = privateCreateNew("setNameNull");
		node.setName(null);
		node = nodeDao.createNewNode(node);
		toDelete.add(node.getId());
		// name should match the ID.
		assertEquals(node.getId(), node.getName());
	}
	
	@Test
	public void testCreateAllAnnotationsTypes() throws Exception{
		Node node = privateCreateNew("testCreateAnnotations");
		String id = nodeDao.createNew(node);
		toDelete.add(id);
		assertNotNull(id);
		// Now get the annotations for this node.
		Annotations annos = nodeDao.getUserAnnotations(id);
		assertNotNull(annos);
		assertNotNull(annos.getEtag());
		assertEquals(id, annos.getId());
		// Now add some annotations to this node.
		AnnotationsV2TestUtils.putAnnotations(annos,"stringOne", "one", AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(annos,"doubleKey", "23.5", AnnotationsValueType.DOUBLE);
		AnnotationsV2TestUtils.putAnnotations(annos,"longKey", "1234", AnnotationsValueType.LONG);
		AnnotationsV2TestUtils.putAnnotations(annos,"dateKey", Long.toString(System.currentTimeMillis()), AnnotationsValueType.TIMESTAMP_MS);
		AnnotationsV2TestUtils.putAnnotations(annos,"booleanKey", "true", AnnotationsValueType.BOOLEAN);
		// update the eTag
		String newETagString = UUID.randomUUID().toString();
		annos.setEtag(newETagString);
		// Update them
		nodeDao.updateUserAnnotations(id, annos);
		// Now get a copy and ensure it equals what we sent
		Annotations copy = nodeDao.getUserAnnotations(id);
		assertNotNull(copy);
		assertEquals("one", AnnotationsV2Utils.getSingleValue(copy, "stringOne"));
		assertEquals("23.5", AnnotationsV2Utils.getSingleValue(copy, "doubleKey"));
		assertEquals("1234",AnnotationsV2Utils.getSingleValue(copy, "longKey"));
		assertEquals("true",AnnotationsV2Utils.getSingleValue(copy, "booleanKey"));
	}
	
	@Test
	public void testCreateAnnotations() throws Exception{
		Node node = privateCreateNew("testCreateAnnotations");
		String id = nodeDao.createNew(node);
		toDelete.add(id);
		assertNotNull(id);
		// Now get the annotations for this node.
		Annotations annos= nodeDao.getUserAnnotations(id);
		assertNotNull(annos);
		assertNotNull(annos.getEtag());
		assertNotNull(annos.getAnnotations());
		assertTrue(annos.getAnnotations().isEmpty());
		// Now add some annotations to this node.
		AnnotationsV2TestUtils.putAnnotations(annos, "stringOne", "one", AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(annos, "doubleKey", "23.5", AnnotationsValueType.DOUBLE);
		AnnotationsV2TestUtils.putAnnotations(annos, "longKey", "1234", AnnotationsValueType.LONG);
		// Update them
		nodeDao.updateUserAnnotations(id, annos);
		// Now get a copy and ensure it equals what we sent
		Annotations copy = nodeDao.getUserAnnotations(id);
		assertNotNull(copy);
		assertEquals(annos, copy);
		// clear an and update
		assertNotNull(copy.getAnnotations().remove("stringOne"));
		nodeDao.updateUserAnnotations(id, copy);
		Annotations copy2 = nodeDao.getUserAnnotations(id);
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
		// With a null label the version label should be assigned as the new version number
		loaded = nodeDao.getNode(id);
		assertNotNull(loaded);
		assertEquals(node.getVersionComment(), loaded.getVersionComment());
		assertEquals(newNumber.toString(), loaded.getVersionLabel());
		assertEquals(NodeConstants.DEFAULT_VERSION_NUMBER + 1, loaded.getVersionNumber());
	}
	
	@Test
	public void testCreateVersionDefaults() throws Exception {
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
		Annotations annos = nodeDao.getUserAnnotations(id);
		assertNotNull(annos);
		AnnotationsV2TestUtils.putAnnotations(annos, "string", "value", AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(annos, "date", "1", AnnotationsValueType.TIMESTAMP_MS);
		AnnotationsV2TestUtils.putAnnotations(annos, "double", "2.3", AnnotationsValueType.DOUBLE);
		AnnotationsV2TestUtils.putAnnotations(annos, "long", "56l", AnnotationsValueType.LONG);
		// Update the annotations
		nodeDao.updateUserAnnotations(id, annos);
		// Now create a new version
		Node copy = nodeDao.getNode(id);
		copy.setVersionComment(null);
		copy.setVersionLabel("1.0.1");
		Long revNumber = nodeDao.createNewVersion(copy);
		assertEquals(new Long(2), revNumber);
		// At this point the new and old version should have the
		// same annotations.
		Annotations v1Annos = nodeDao.getUserAnnotationsForVersion(id, 1L);
		assertNotNull(v1Annos);
		assertEquals(NodeConstants.ZERO_E_TAG, v1Annos.getEtag());
		Annotations v2Annos = nodeDao.getUserAnnotationsForVersion(id, 2L);
		assertNotNull(v2Annos);
		assertEquals(NodeConstants.ZERO_E_TAG, v2Annos.getEtag());
		assertEquals(v1Annos, v2Annos);
		Annotations currentAnnos = nodeDao.getUserAnnotations(id);
		assertNotNull(currentAnnos);
		assertNotNull(currentAnnos.getEtag());
		// They should be equal except for the e-tag
		assertFalse(currentAnnos.getEtag().equals(v2Annos.getEtag()));
		v2Annos.setEtag(currentAnnos.getEtag());
		assertEquals(currentAnnos, v2Annos);
		
		// Now update the current annotations
		AnnotationsV2TestUtils.putAnnotations(currentAnnos, "double", "8989898.2", AnnotationsValueType.DOUBLE);
		nodeDao.updateUserAnnotations(id, currentAnnos);
		
		// Now the old and new should no longer match.
		v1Annos = nodeDao.getUserAnnotationsForVersion(id, 1L);
		assertNotNull(v1Annos);
		assertEquals("2.3", AnnotationsV2Utils.getSingleValue(v1Annos, "double"));
		assertEquals(NodeConstants.ZERO_E_TAG, v1Annos.getEtag());

		v2Annos = nodeDao.getUserAnnotationsForVersion(id, 2L);
		assertNotNull(v2Annos);
		assertEquals(NodeConstants.ZERO_E_TAG, v2Annos.getEtag());
		assertEquals("8989898.2", AnnotationsV2Utils.getSingleValue(v2Annos, "double"));
		// The two version should now be out of synch with each other.
		assertFalse(v1Annos.equals(v2Annos));
		// The current annos should still match the v2
		currentAnnos = nodeDao.getUserAnnotations(id);
		assertNotNull(currentAnnos);
		assertNotNull(currentAnnos.getEtag());
		// They should be equal except for the e-tag
		assertFalse(currentAnnos.getEtag().equals(v2Annos.getEtag()));
		v2Annos.setEtag(currentAnnos.getEtag());
		assertEquals(currentAnnos, v2Annos);
		assertEquals("8989898.2", AnnotationsV2Utils.getSingleValue(currentAnnos, "double"));
		
		// Node delete the current revision and confirm that the annotations are rolled back
		node = nodeDao.getNode(id);
		nodeDao.deleteVersion(id, node.getVersionNumber());
		Annotations rolledBackAnnos = nodeDao.getUserAnnotations(id);
		assertEquals("2.3", AnnotationsV2Utils.getSingleValue(rolledBackAnnos, "double"));
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
	public void testGetLatestVersionNumber() throws Exception {
		// Create a number of versions
		int numberVersions = 5;
		String id = createNodeWithMultipleVersions(numberVersions);
		
		Node currentNode = nodeDao.getNode(id);
		
		// Call under test
		Optional<Long> result = nodeDao.getLatestVersionNumber(id);
		
		assertTrue(result.isPresent());
		assertEquals(currentNode.getVersionNumber(), result.get());
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
		assertEquals(new Long(currentVersion-1), node.getVersionNumber()
				,"Deleting the current version of a node failed to change the current version to be current - 1");
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
		assertEquals(new Long(currentVersion), node.getVersionNumber(),
				"Deleting the first version should not have changed the current version of the node");
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
		
		// Now delete all versions but the last one.
		for (Long versionNumber : startingVersions.subList(0, startingVersions.size() - 1)) {	
			nodeDao.deleteVersion(id, versionNumber);
		}
		
		// The following fails as it is the latest version
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			nodeDao.deleteVersion(id, startingVersions.get(startingVersions.size() - 1));
		});
		
		// There should be one version left and it should be the first version.
		node = nodeDao.getNode(id);
		assertNotNull(node);
		assertEquals(new Long(1), node.getVersionNumber(),
				"Deleting all versions except the first should have left the node in place with a current version of 1.");
	}
	
	// Test for PLFM-3781: Older revisions should not be recycled
	@Test
	public void testCreateNewVersionAfterVersionDeletion() throws Exception {
		int numberVersions = 2;
		
		// Creates a node with 2 versions
		String id = createNodeWithMultipleVersions(numberVersions);
		
		// Now deletes the latest version
		Long latestVersion = nodeDao.getLatestVersionNumber(id).get();
		
		nodeDao.deleteVersion(id, latestVersion);
		
		// Creates a new version
		Node node = nodeDao.getNode(id);
		Node newVersion = node;
		
		// Reset the label to avoid duplicates
		newVersion.setVersionLabel(null);
		
		Long newVersionNumber = nodeDao.createNewVersion(newVersion);
		
		node = nodeDao.getNode(id);
		
		// The new version assigned to the node should not reuse old version numbers
		assertEquals(newVersionNumber, node.getVersionNumber());
		assertEquals(NodeConstants.DEFAULT_VERSION_NUMBER + numberVersions, newVersionNumber);
		
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
	
	@Test
	public void testDeleteCascadeGreaterThanMax(){
		List<String> nodeIds = createNestedNodes(20);
		
		UncategorizedSQLException ex = Assertions.assertThrows(UncategorizedSQLException.class, () -> {
			//delete the parent node 
			nodeDao.delete(nodeIds.get(0));
		});
		
		assertTrue(ex.getMessage().contains("Foreign key cascade delete/update exceeds max depth of 15."));
	}
	
	@Test
	public void testDeleteTreeWithSubtreeSizeLessThanLimit() {
		int depth = 20;
		
		List<String> nodeIds = createNestedNodes(depth);
		
		// Call under test
		boolean deleted = nodeDao.deleteTree(nodeIds.get(0), depth);
	
		assertTrue(deleted);
		
		//check that all added nodes were deleted 
		for(String nodeID : nodeIds) {
			assertFalse(nodeDao.doesNodeExist(KeyFactory.stringToKey(nodeID)));
		}
	}
	
	@Test
	public void testDeleteTreeWithSubtreeSizeGreaterThanLimit() {
		int depth = 20;
		
		List<String> nodeIds = createNestedNodes(depth);
		
		int limit = depth / 2;
		
		// Call under test
		boolean deleted = nodeDao.deleteTree(nodeIds.get(0), limit);
	
		assertFalse(deleted);
		
		// Calling it again should finalize the deletion
		deleted = nodeDao.deleteTree(nodeIds.get(0), limit);
		
		assertTrue(deleted);
		
		//check that all added nodes were deleted 
		for(String nodeID : nodeIds){
			assertFalse(nodeDao.doesNodeExist(KeyFactory.stringToKey(nodeID)));
		}
	}
	
	@Test
	public void testDeleteTreeNoContainer() {
		Node node = NodeTestUtils.createNew("parent", creatorUserGroupId);
		node.setNodeType(EntityType.file);
		node = nodeDao.createNewNode(node);
		toDelete.add(node.getId());
		
		boolean deleted = nodeDao.deleteTree(node.getId(), 1);
	
		assertTrue(deleted);
	}
	
	@Test
	public void testDeleteTreeWithLeaves() {
		int depth = 20;
		
		List<String> nodeIds = createNestedNodes(depth);
		List<String> fileIds = new ArrayList<>();
		
		// Add a couple of files in the last node
		fileIds.add(addFile(nodeIds.get(nodeIds.size() - 1)));
		fileIds.add(addFile(nodeIds.get(nodeIds.size() - 1)));
		
		// Add some in the middle
		fileIds.add(addFile(nodeIds.get(nodeIds.size() / 2)));
		
		int limit = depth + fileIds.size();
		
		// Call under test
		boolean deleted = nodeDao.deleteTree(nodeIds.get(0), limit);
		
		assertTrue(deleted);
		
		for (String fileId : fileIds) {
			assertFalse(nodeDao.doesNodeExist(KeyFactory.stringToKey(fileId)));
		}
		
	}
	
	private String addFile(String parentId) {
		Node file = NodeTestUtils.createNew("file_" + UUID.randomUUID().toString(), creatorUserGroupId);
		
		file.setNodeType(EntityType.file);
		file.setParentId(parentId);
		file.setFileHandleId(fileHandle.getId());
		file = nodeDao.createNewNode(file);
		
		toDelete.add(file.getId());
		
		return file.getId();
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
			String nodeName = "NodeDAOImplTest.createNestedNodes() Node:" + i + " " + UUID.randomUUID().toString();
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
		EntityHeader parentHeader = nodeDao.getEntityHeader(parentId);
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
		EntityHeader childHeader = nodeDao.getEntityHeader(childId);
		assertNotNull(childHeader);
		assertEquals(EntityTypeUtils.getEntityTypeClassName(EntityType.folder), childHeader.getType());
		assertEquals("child", childHeader.getName());
		assertEquals(childId, childHeader.getId());

		// Get the header of this node non versioned
		childHeader = nodeDao.getEntityHeader(childId);
		assertNotNull(childHeader);		
		assertEquals(childId, childHeader.getId());
		assertEquals(new Long(1), childHeader.getVersionNumber());

	}
	
	@Test
	public void testGetEntityHeaderByReference() throws Exception {
		Node parent = privateCreateNew("parent");
		parent.setNodeType(EntityType.project);
		String parentId = nodeDao.createNew(parent);
		parent = nodeDao.getNode(parentId);
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

		// Call under test
		List<EntityHeader> results = nodeDao.getEntityHeader(request);
		assertNotNull(results);
		assertEquals(4, results.size());
		
		EntityHeader header = results.get(0);
		assertEquals(parentId, header.getId());
		assertEquals("1", header.getVersionLabel());
		assertEquals(new Long(1), header.getVersionNumber());
		assertEquals(parentBenefactor, header.getBenefactorId());
		assertEquals(parent.getCreatedByPrincipalId().toString(), header.getCreatedBy());
		assertEquals(parent.getCreatedOn(), header.getCreatedOn());
		assertEquals(parent.getModifiedByPrincipalId().toString(), header.getModifiedBy());
		assertEquals(parent.getModifiedOn(), header.getModifiedOn());
		assertTrue(header.getIsLatestVersion());
		
		header = results.get(2);
		assertEquals(childId, header.getId());
		assertEquals("2", header.getVersionLabel());
		assertEquals(new Long(2), header.getVersionNumber());
		assertEquals(parentBenefactor, header.getBenefactorId());
		assertTrue(header.getIsLatestVersion());

		header = results.get(3);
		assertEquals(childId, header.getId());
		assertEquals("1", header.getVersionLabel());
		assertEquals(new Long(1), header.getVersionNumber());
		assertEquals(parentBenefactor, header.getBenefactorId());
		assertFalse(header.getIsLatestVersion());
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
	
	@Test
	public void testGetEntityHeaderByReferenceNull() throws Exception {
		List<Reference> request = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test.
			nodeDao.getEntityHeader(request);
		});
	}
	
	@Test
	public void testGetEntityHeaderDoesNotExist() throws NotFoundException, DatastoreException{
		// There should be no node with this id.
		long id = idGenerator.generateNewId(IdType.ENTITY_ID);
		assertThrows(NotFoundException.class, ()->{
			nodeDao.getEntityHeader(KeyFactory.keyToString(id));
		});
	}
	
	@Test
	public void testGetEntityPathId() throws Exception {
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
		
		// call under test
		List<Long> path = nodeDao.getEntityPathIds(grandId);
		assertNotNull(path);
		assertEquals(3, path.size());
		assertEquals(KeyFactory.stringToKey(parentId), path.get(0));
		assertEquals(KeyFactory.stringToKey(childId), path.get(1));
		assertEquals(KeyFactory.stringToKey(grandId), path.get(2));
	}
	
	@Test
	public void testGetEntityPathIdNotFound() throws Exception {
		assertThrows(NotFoundException.class, ()->{
			// call under test
			nodeDao.getEntityPathIds("syn99999999");
		});
	}
	
	/**
	 * Maybe we should prevent users from creating such a loop.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetEntityPathIdsInfiniteLoop() throws Exception {
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
		// setup an infinite loop
		parent.setParentId(grandChildId);
		nodeDao.updateNode(parent);
		
		String message = assertThrows(IllegalStateException.class, ()->{
			// call under test
			nodeDao.getEntityPathIds(grandChildId);
		}).getMessage();
		assertEquals("Path depth limit of: "+NodeConstants.MAX_PATH_DEPTH+" exceeded for: "+grandChildId, message);
	}
	
	@Test
	public void testGetEntityPathIdIncludeSelfTrue() throws Exception {
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
		
		// call under test
		boolean includeSelf = true;
		List<Long> path = nodeDao.getEntityPathIds(childId, includeSelf);
		assertNotNull(path);
		assertEquals(2, path.size());
		assertEquals(KeyFactory.stringToKey(parentId), path.get(0));
		assertEquals(KeyFactory.stringToKey(childId), path.get(1));
	}
	
	@Test
	public void testGetEntityPathIdIncludeSelfFalse() throws Exception {
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
		
		// call under test
		boolean includeSelf = false;
		List<Long> path = nodeDao.getEntityPathIds(childId, includeSelf);
		assertNotNull(path);
		assertEquals(1, path.size());
		assertEquals(KeyFactory.stringToKey(parentId), path.get(0));
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
		array[0] = nodeDao.getEntityHeader(parentId);
		array[1] = nodeDao.getEntityHeader(childId);
		array[2] = nodeDao.getEntityHeader(grandId);
		
		// Now get the path for each node
		List<NameIdType> path = nodeDao.getEntityPath(grandId);
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
	
	@Test
	public void testGetEntityPathInvalidNode() throws Exception {
		assertThrows(NotFoundException.class, ()->{
			nodeDao.getEntityPath("syn9999999");
		});
	}
	
	@Test
	public void testGetEntityPathInfiniteLoop() throws Exception {
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
		// setup an infinite loop
		parent.setParentId(grandChildId);
		nodeDao.updateNode(parent);
		
		String message = assertThrows(IllegalStateException.class, ()->{
			// call under test
			nodeDao.getEntityPath(grandChildId);
		}).getMessage();
		assertEquals("Path depth limit of: "+NodeConstants.MAX_PATH_DEPTH+" exceeded for: "+grandChildId, message);
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
			array[i] = nodeDao.getEntityHeader(ids[i]);
		}
		List<NameIdType> path = nodeDao.getEntityPath(ids[depth-1]);
		assertNotNull(path);
		assertEquals(depth, path.size());
		for (int i=0; i<depth; i++) {
			EntityHeader header = array[i];
			NameIdType nameIdType = path.get(i);
			assertEquals(header.getId(), nameIdType.getId());
			assertEquals(header.getName(), nameIdType.getName());
			assertEquals(header.getType(), nameIdType.getType());
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
		Annotations annos = nodeDao.getUserAnnotations(projectId);
		assertNotNull(annos);
		// Create a very large string
		byte[] largeArray = new byte[10000];
		byte value = 101;
		Arrays.fill(largeArray, value);
		String largeString = new String(largeArray, "UTF-8");
		String key = "veryLargeString";
		AnnotationsV2TestUtils.putAnnotations(annos, key, largeString, AnnotationsValueType.STRING);
		// This update will fail before PLFM-791 is fixed.
		nodeDao.updateUserAnnotations(projectId, annos);
		// Get the values back
		annos = nodeDao.getUserAnnotations(projectId);
		assertNotNull(annos);
		// Make sure we can still get the string
		assertEquals(largeString, AnnotationsV2Utils.getSingleValue(annos, key));
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
	
	@Test
	public void testGetCurrentRevNumberDoesNotExist() throws NotFoundException, DatastoreException{
		assertThrows(NotFoundException.class, ()->{
			nodeDao.getCurrentRevisionNumber(KeyFactory.keyToString(new Long(-12)));
		});
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
	
	@Test
	public void testGetCreatedByDoesNotExist() throws NotFoundException, DatastoreException{
		assertThrows(NotFoundException.class, ()->{
			nodeDao.getCreatedBy(KeyFactory.keyToString(new Long(-12)));
		});
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
		}catch(FileHandleLinkedException e){
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
		assertEquals(fileHandle.getId(), clone.getFileHandleId(), "Creating a new version should copy the FileHandlId from the previous version");
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
		assertEquals(fileHandle.getId(), v1Node.getFileHandleId(),"V1 should have the first file handle");
		assertEquals(fileHandle.getId(), v2Node.getFileHandleId(),"V2 should also have the first file handle");
		assertEquals(fileHandle2.getId(), v3Node.getFileHandleId(), "V3 should also have the second file handle");
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
		assertNotNull(n1.getColumnModelIds(),"ColumnModel ID were not saved!");
		List<String> expected = new LinkedList<String>();
		expected.add("123");
		expected.add("456");
		assertEquals(expected, n1.getColumnModelIds());
	}



	/**
	 * PLFM-5960
	 * @throws Exception
	 */
	@Test
	public void testGetEntityHeaderByMd5NoMatches() {
		// Call under test
		List<EntityHeader> results = nodeDao.getEntityHeaderByMd5(fileHandle3.getContentMd5());
		assertNotNull(results);
		assertEquals(0, results.size());
	}

	/**
	 * PLFM-5960
	 */
	@Test
	public void testGetEntityHeaderByMd5WithOver200Matching() {

		for(int i = 0; i < NodeDAO.NODE_VERSION_LIMIT_BY_FILE_MD5 + 1; i++) {
			// Add a node with a file handle
			Node curr = NodeTestUtils.createNew("testGetEntityHeaderByMd5 node " + i, creatorUserGroupId);
			curr.setFileHandleId(fileHandle3.getId());
			final String id = nodeDao.createNew(curr);
			assertNotNull(id);
			toDelete.add(id);
		}

		// Call under test
		List<EntityHeader> results = nodeDao.getEntityHeaderByMd5(fileHandle3.getContentMd5());
		assertNotNull(results);
		assertEquals(NodeDAO.NODE_VERSION_LIMIT_BY_FILE_MD5, results.size());
	}
	
	@Test
	public void testGetProjectStatAdditionalCondition_CREATED(){
		Map<String, Object> parameters = new HashMap<>();
		Long userId = 123L;
		ProjectListType type = ProjectListType.CREATED;
		String result = NodeDAOImpl.getProjectStatAdditionalCondition(parameters, userId, type);
		assertEquals(" AND n.CREATED_BY = :bCreatedBy", result);
		assertEquals(userId, parameters.get("bCreatedBy"));
	}
	
	@Test
	public void testGetProjectStatAdditionalCondition_PARTICIPATED(){
		Map<String, Object> parameters = new HashMap<>();
		Long userId = 123L;
		ProjectListType type = ProjectListType.PARTICIPATED;
		String result = NodeDAOImpl.getProjectStatAdditionalCondition(parameters, userId, type);
		assertEquals(" AND n.CREATED_BY <> :bCreatedBy", result);
		assertEquals(userId, parameters.get("bCreatedBy"));
	}
	
	@Test
	public void testGetProjectStatAdditionalCondition_ALL(){
		Map<String, Object> parameters = new HashMap<>();
		Long userId = 123L;
		ProjectListType type = ProjectListType.ALL;
		String result = NodeDAOImpl.getProjectStatAdditionalCondition(parameters, userId, type);
		assertEquals("", result);
		assertTrue(parameters.isEmpty());
	}
	
	@Test
	public void testGetProjectStatAdditionalConditionMY_TEAM_PROJECTS(){
		Map<String, Object> parameters = new HashMap<>();
		Long userId = 123L;
		ProjectListType type = ProjectListType.TEAM;
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
		assertEquals(" ORDER BY n.NAME DESC limit :limitVal offset :offsetVal", result);
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
		ProjectListType type = ProjectListType.CREATED;
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
		ProjectListType type = ProjectListType.CREATED;
		ProjectListSortColumn sortColumn = ProjectListSortColumn.LAST_ACTIVITY;
		SortDirection sortDirection = SortDirection.ASC;
		Long limit = 10L;
		Long offset = 0L;
		// call under test
		List<ProjectHeader> results = nodeDao.getProjectHeaders(user1Id, projectIds, type, sortColumn, sortDirection, limit, offset);
		assertNotNull(results);
		assertTrue(results.isEmpty());
	}


	@Test
	public void testCreateNodeLongNameTooLong() throws DatastoreException, InvalidModelException, NotFoundException {
		char[] chars = new char[260];
		Arrays.fill(chars, 'x');
		String name = new String(chars);
		Node n = NodeTestUtils.createNew(name, creatorUserGroupId);
		assertThrows(IllegalArgumentException.class, ()->{
			nodeDao.createNew(n);
		});
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
	@Test
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
		assertThrows(LimitExceededException.class, ()->{
			// call under test
			nodeDao.getAllContainerIds(Arrays.asList(projectIdLong), maxIds);
		});
	}
	
	@Test
	public void testGetAllContainerIdsOrderByDistanceDesc() {
		int treeDepth = 20;

		List<Long> nodeIds = createNestedNodes(treeDepth).stream().map(KeyFactory::stringToKey).collect(Collectors.toList());

		int limit = 1000;

		// Call under test
		List<Long> result = nodeDao.getSubTreeNodeIdsOrderByDistanceDesc(nodeIds.get(0), limit);

		List<Long> expected = nodeIds.subList(1, nodeIds.size());
		
		Collections.reverse(expected);

		assertEquals(expected, result);

	}
	
	/**
	 * Exceed the limit with multiple calls.
	 * 
	 * @throws LimitExceededException
	 */
	@Test
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
		assertThrows(LimitExceededException.class, ()->{
			// call under test
			nodeDao.getAllContainerIds(Arrays.asList(projectIdLong), maxIds);
		});
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
	
	@Test
	public void testGetNodeIdByAliasNotFound(){
		String alias = "doesNotExist";
		assertThrows(NotFoundException.class, ()->{
			// call under test
			nodeDao.getNodeIdByAlias(alias);
		});
	}
	
	@Test
	public void testGetAliasByNodeId() {
		Node node = privateCreateNew("testGetAliasByNodeId");
		String alias = UUID.randomUUID().toString();
		node.setAlias(alias);
		node.setVersionComment("v1");
		node.setVersionLabel("1");
		String id = nodeDao.createNew(node);
		toDelete.add(id);
		assertNotNull(id);
		// call under test
		List<IdAndAlias> actual = nodeDao.getAliasByNodeId(Collections.singletonList(id));
		
		List<IdAndAlias> expected = Collections.singletonList(new IdAndAlias(id, alias));
		assertEquals(expected, actual);
	}
	
	@Test
	public void testGetAliasByNodeIdEmptyList() {
		// call under test
		List<IdAndAlias> actual = nodeDao.getAliasByNodeId(Collections.EMPTY_LIST);
		
		assertTrue(actual.isEmpty());
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
	@Test
	public void testGetProjectIdChildWithNoParent() throws Exception{
		Node child = setUpChildWithNoParent();
		assertThrows(NotFoundException.class, ()->{
			// Before the fix, this call call would hang with 100% CPU.
			nodeDao.getProjectId(child.getId());
		});
	}
	
	/**
	 * Test for PLFM-4369.
	 * A timeout for this test means the function entered
	 * into an infinite loop and should be killed.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetBenefactorIdChildWithNoParent() throws Exception{
		Node child = setUpChildWithNoParent();
		assertThrows(NotFoundException.class, ()->{
			// Before the fix, this call call would hang with 100% CPU.
			nodeDao.getBenefactor(child.getId());
		});
	}
	
	/**
	 * Test for PLFM-4369.
	 * A timeout for this test means the function entered
	 * into an infinite loop and should be killed.
	 * @throws Exception
	 */
	@Test
	public void testGetProjectInfiniteLoop() throws Exception{
		String id = setUpChildAsItsOwnParent();
		assertThrows(IllegalStateException.class, ()->{
			// Before the fix, this call call would hang with 100% CPU.
			nodeDao.getProjectId(id);
		});
	}
	
	/**
	 * Test for PLFM-4369.
	 * A timeout for this test means the function entered
	 * into an infinite loop and should be killed.
	 * @throws Exception
	 */
	@Test
	public void testGetBenefactorIdInfiniteLoop() throws Exception{
		String id = setUpChildAsItsOwnParent();
		assertThrows(IllegalStateException.class, ()->{
			// Before the fix, this call call would hang with 100% CPU.
			nodeDao.getBenefactor(id);
		});
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
		migratableTableDao.runWithKeyChecksIgnored(() -> {
			basicDao.deleteObjectByPrimaryKey(DBONode.class, new MapSqlParameterSource("id", KeyFactory.stringToKey(projectid)));
			return null;
		});
		// the parent should not exist.
		assertFalse(nodeDao.doesNodeExist(KeyFactory.stringToKey(child.getParentId())));
		// the child should exist.
		assertTrue(nodeDao.doesNodeExist(KeyFactory.stringToKey(child.getId())));
		return child;
	}
	
	@Test
	public void testGetProjectNodeDoesNotEixst(){
		String doesNotExist = "syn9999999";
		assertThrows(NotFoundException.class, ()->{
			// call under test
			nodeDao.getProjectId(doesNotExist);
		});
	}
	
	@Test
	public void testGetProjectNodeExistsWithNoProject(){
		// create a node that is not in a project.
		Node node = NodeTestUtils.createNew("someNode", creatorUserGroupId);
		node.setNodeType(EntityType.folder);
		node = nodeDao.createNewNode(node);
		String nodeId = node.getId();
		toDelete.add(node.getId());
		assertThrows(NotFoundException.class, ()->{
			// call under test
			nodeDao.getProjectId(nodeId);
		});
	}

	@Test
	public void testGetFileHandleIdsAssociatedWithFileEntityNullFileHandleIds(){
		assertThrows(IllegalArgumentException.class, ()->{
			nodeDao.getFileHandleIdsAssociatedWithFileEntity(null, 1L);
		});
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
	public void testGetFileHandleAssociationsForCurrentVersion(){
		Node node = NodeTestUtils.createNew("getFileHandleAssociationsForCurrentVersion", creatorUserGroupId);
		node.setFileHandleId(fileHandle.getId());
		node.setNodeType(EntityType.file);
		String id = nodeDao.createNew(node);
		Long idLong = KeyFactory.stringToKey(id);
		toDelete.add(id);
		// call under test
		List<FileHandleAssociation> associations = nodeDao.getFileHandleAssociationsForCurrentVersion(Lists.newArrayList(id));
		assertNotNull(associations);
		assertEquals(1, associations.size());
		FileHandleAssociation association = associations.get(0);
		assertEquals(idLong.toString(), association.getAssociateObjectId());
		assertEquals(FileHandleAssociateType.FileEntity, association.getAssociateObjectType());
		assertEquals(fileHandle.getId(), association.getFileHandleId());
		
		// create a new version with file two
		node = nodeDao.getNode(id);
		node.setVersionNumber(2L);
		node.setFileHandleId(fileHandle2.getId());
		node.setVersionLabel("v-2");
		nodeDao.createNewVersion(node);
		
		// call under test
		associations = nodeDao.getFileHandleAssociationsForCurrentVersion(Lists.newArrayList(id));
		assertEquals(1, associations.size());
		association = associations.get(0);
		assertEquals(idLong.toString(), association.getAssociateObjectId());
		assertEquals(FileHandleAssociateType.FileEntity, association.getAssociateObjectType());
		assertEquals(fileHandle2.getId(), association.getFileHandleId());
	}
	
	@Test
	public void testGetFileHandleAssociationsForCurrentVersionNonFile(){
		Node node = NodeTestUtils.createNew("getFileHandleAssociationsForCurrentVersion", creatorUserGroupId);
		// create a project without a file.
		node.setNodeType(EntityType.project);
		node.setFileHandleId(null);
		String id = nodeDao.createNew(node);
		Long idLong = KeyFactory.stringToKey(id);
		toDelete.add(id);
		// call under test
		List<FileHandleAssociation> associations = nodeDao.getFileHandleAssociationsForCurrentVersion(Lists.newArrayList(id));
		assertNotNull(associations);
		assertTrue(associations.isEmpty());
	}
	
	@Test
	public void testGetFileHandleAssociationsForCurrentVersionEmptyList(){
		// call under test
		List<FileHandleAssociation> associations = nodeDao.getFileHandleAssociationsForCurrentVersion(new LinkedList());
		assertNotNull(associations);
		assertTrue(associations.isEmpty());
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
		Annotations userAnnos = new Annotations();
		userAnnos.setId(file.getId());
		userAnnos.setEtag(file.getETag());
		AnnotationsV2TestUtils.putAnnotations(userAnnos, "aString", "someString", AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(userAnnos, "aLong", "123", AnnotationsValueType.LONG);
		AnnotationsV2TestUtils.putAnnotations(userAnnos, "aDouble", "1.22", AnnotationsValueType.DOUBLE);
		AnnotationsV2TestUtils.putAnnotations(userAnnos, "aDouble2", "1.22", AnnotationsValueType.DOUBLE);
		nodeDao.updateUserAnnotations(file.getId(), userAnnos);
		//Ensure that entity property annotations are not included in the entity replication (PLFM-4601)

		org.sagebionetworks.repo.model.Annotations entityPropertyAnnotations = new org.sagebionetworks.repo.model.Annotations();
		entityPropertyAnnotations.setId(file.getId());
		entityPropertyAnnotations.setEtag(file.getETag());
		entityPropertyAnnotations.addAnnotation("primaryString", "primaryTest");
		nodeDao.updateEntityPropertyAnnotations(file.getId(), entityPropertyAnnotations);

		int maxAnnotationChars = 10;
		
		List<Long> ids = KeyFactory.stringToKey(ImmutableList.of(project.getId(), file.getId()));
		// call under test
		List<ObjectDataDTO> results = nodeDao.getEntityDTOs(ids, maxAnnotationChars);
		assertNotNull(results);
		assertEquals(2, results.size());
		ObjectDataDTO fileDto = results.get(1);
		assertEquals(KeyFactory.stringToKey(file.getId()), fileDto.getId());
		assertEquals(file.getVersionNumber(), fileDto.getCurrentVersion());
		assertEquals(file.getCreatedByPrincipalId(), fileDto.getCreatedBy());
		assertEquals(file.getCreatedOn(), fileDto.getCreatedOn());
		assertEquals(file.getETag(), fileDto.getEtag());
		assertEquals(file.getName(), fileDto.getName());
		assertEquals(file.getNodeType().name(), fileDto.getSubType());
		assertEquals(KeyFactory.stringToKey(project.getId()), fileDto.getParentId());
		assertEquals(KeyFactory.stringToKey(project.getId()), fileDto.getBenefactorId());
		assertEquals(KeyFactory.stringToKey(project.getId()), fileDto.getProjectId());
		assertEquals(file.getModifiedByPrincipalId(), fileDto.getModifiedBy());
		assertEquals(file.getModifiedOn(), fileDto.getModifiedOn());
		assertEquals(new Long(Long.parseLong(file.getFileHandleId())), fileDto.getFileHandleId());
		assertEquals(fileHandle.getContentSize(), fileDto.getFileSizeBytes());
		assertEquals(NodeUtils.isBucketSynapseStorage(fileHandle.getBucketName()), fileDto.getIsInSynapseStorage());
		assertEquals(fileHandle.getContentMd5(), fileDto.getFileMD5());

		assertNotNull(fileDto.getAnnotations());
		assertEquals(4, fileDto.getAnnotations().size());
		List<ObjectAnnotationDTO> expected = Lists.newArrayList(
				new ObjectAnnotationDTO(fileIdLong, "aString", AnnotationType.STRING, "someString"),
				new ObjectAnnotationDTO(fileIdLong, "aLong", AnnotationType.LONG, "123"),
				new ObjectAnnotationDTO(fileIdLong, "aDouble", AnnotationType.DOUBLE, "1.22"),
				new ObjectAnnotationDTO(fileIdLong, "aDouble2", AnnotationType.DOUBLE, "1.22")
		);
		// Annotation order is not preserved by the JSON database column used to store annotations
		for(ObjectAnnotationDTO expectedDto: expected) {
			assertTrue(fileDto.getAnnotations().contains(expectedDto));
		}
		// null checks on the project
		ObjectDataDTO projectDto = results.get(0);
		assertEquals(KeyFactory.stringToKey(project.getId()), projectDto.getId());
		assertEquals(null, projectDto.getParentId());
		assertEquals(projectDto.getId(), projectDto.getBenefactorId());
		assertEquals(projectDto.getId(), projectDto.getProjectId());
		assertEquals(null, projectDto.getFileHandleId());
		assertEquals(null, projectDto.getAnnotations());
		assertEquals(null, projectDto.getFileHandleId());
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
		Annotations annos = new Annotations();
		annos.setId(file.getId());
		annos.setEtag(file.getETag());
		// added for PLFM_4184
		AnnotationsV2TestUtils.putAnnotations(annos,"emptyList", Collections.emptyList(), AnnotationsValueType.STRING);
		// added for PLFM-4224
		AnnotationsV2TestUtils.putAnnotations(annos, "listWithNullValue", Collections.singletonList(null), AnnotationsValueType.DOUBLE);
		nodeDao.updateUserAnnotations(file.getId(), annos);
		
		int maxAnnotationChars = 10;
		
		List<Long> ids = KeyFactory.stringToKey(ImmutableList.of(project.getId(), file.getId()));
		// call under test
		List<ObjectDataDTO> results = nodeDao.getEntityDTOs(ids, maxAnnotationChars);
		assertNotNull(results);
		assertEquals(2, results.size());
		ObjectDataDTO fileDto = results.get(1);
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
		List<Node> results = new LinkedList<Node>();
		// Create a project
		Node project = NodeTestUtils.createNew("hierarchy", creatorUserGroupId);
		project.setNodeType(EntityType.project);
		String projectId = nodeDao.createNew(project);
		results.add(nodeDao.getNode(projectId));
		toDelete.add(projectId);
		
		String levelOneFolderId = null;
		// Create two folders
		for(int i=0; i<2; i++){
			Node folder = NodeTestUtils.createNew("folder"+i, creatorUserGroupId);
			folder.setNodeType(EntityType.folder);
			folder.setParentId(projectId);
			levelOneFolderId = nodeDao.createNew(folder);
			results.add(nodeDao.getNode(levelOneFolderId));
			toDelete.add(levelOneFolderId);
		}
		
		// file0
		Node file = NodeTestUtils.createNew("file0", creatorUserGroupId);
		file.setNodeType(EntityType.file);
		file.setParentId(projectId);
		file.setFileHandleId(fileHandle.getId());
		String fileId = nodeDao.createNew(file);
		results.add(nodeDao.getNode(fileId));
		toDelete.add(fileId);
		
		// folder2
		Node folder = NodeTestUtils.createNew("folder2", creatorUserGroupId);
		folder.setNodeType(EntityType.folder);
		folder.setParentId(levelOneFolderId);
		String folder2Id = nodeDao.createNew(folder);
		results.add(nodeDao.getNode(folder2Id));
		toDelete.add(folder2Id);
		
		// file1
		file = NodeTestUtils.createNew("file1", creatorUserGroupId);
		file.setNodeType(EntityType.file);
		file.setParentId(levelOneFolderId);
		file.setFileHandleId(fileHandle.getId());
		fileId = nodeDao.createNew(file);
		results.add(nodeDao.getNode(fileId));
		toDelete.add(fileId);
		
		// file2
		file = NodeTestUtils.createNew("file2", creatorUserGroupId);
		file.setNodeType(EntityType.file);
		file.setParentId(folder2Id);
		file.setFileHandleId(fileHandle.getId());
		fileId = nodeDao.createNew(file);
		results.add(nodeDao.getNode(fileId));
		// Set file2 to be its own benefactor
		AccessControlList acl = AccessControlListUtil.createACLToGrantEntityAdminAccess(fileId, adminUser, new Date());
		accessControlListDAO.create(acl, ObjectType.ENTITY);
		toDelete.add(fileId);
		
		return results;
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

	@Test
	public void testGetFragmentSortColumnModifiedOn() {
		SortBy sortBy = SortBy.MODIFIED_ON;
		String result = NodeDAOImpl.getFragmentSortColumn(sortBy);
		assertEquals(NodeDAOImpl.R_MODIFIED_ON, result);
	}

	@Test
	public void testGetFragmentSortColumnNull(){
		SortBy sortBy = null;
		assertThrows(IllegalArgumentException.class, ()->{
			NodeDAOImpl.getFragmentSortColumn(sortBy);
		});
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
	
	@Test
	public void testGetChildrenNullParentId(){
		String parentId = null;
		List<EntityType> includeTypes = Lists.newArrayList(EntityType.file);
		Set<Long> childIdsToExclude = null;
		SortBy sortBy = SortBy.NAME;
		Direction sortDirection = Direction.ASC;
		long limit = 10L;
		long offset = 0L;
		assertThrows(IllegalArgumentException.class, ()->{
			nodeDao.getChildren(parentId, includeTypes, childIdsToExclude, sortBy, sortDirection, limit, offset);
		});
	}
	
	@Test
	public void testGetChildrenNullTypes(){
		String parentId = "syn123";
		List<EntityType> includeTypes = null;
		Set<Long> childIdsToExclude = null;
		SortBy sortBy = SortBy.NAME;
		Direction sortDirection = Direction.ASC;
		long limit = 10L;
		long offset = 0L;
		assertThrows(IllegalArgumentException.class, ()->{
			nodeDao.getChildren(parentId, includeTypes, childIdsToExclude, sortBy, sortDirection, limit, offset);
		});
	}
	
	@Test
	public void testGetChildrenEmptyTypes(){
		String parentId = "syn123";
		List<EntityType> includeTypes = new LinkedList<EntityType>();
		Set<Long> childIdsToExclude = null;
		SortBy sortBy = SortBy.NAME;
		Direction sortDirection = Direction.ASC;
		long limit = 10L;
		long offset = 0L;
		assertThrows(IllegalArgumentException.class, ()->{
			nodeDao.getChildren(parentId, includeTypes, childIdsToExclude, sortBy, sortDirection, limit, offset);
		});
	}
	
	@Test
	public void testGetChildrenNullSortByt(){
		String parentId = "syn123";
		List<EntityType> includeTypes = Lists.newArrayList(EntityType.file);
		Set<Long> childIdsToExclude = null;
		SortBy sortBy = null;
		Direction sortDirection = Direction.ASC;
		long limit = 10L;
		long offset = 0L;
		assertThrows(IllegalArgumentException.class, ()->{
			nodeDao.getChildren(parentId, includeTypes, childIdsToExclude, sortBy, sortDirection, limit, offset);
		});
	}
	
	@Test
	public void testGetChildrenNullDirection(){
		String parentId = "syn123";
		List<EntityType> includeTypes = Lists.newArrayList(EntityType.file);
		Set<Long> childIdsToExclude = null;
		SortBy sortBy = SortBy.CREATED_ON;
		Direction sortDirection = null;
		long limit = 10L;
		long offset = 0L;
		assertThrows(IllegalArgumentException.class, ()->{
			nodeDao.getChildren(parentId, includeTypes, childIdsToExclude, sortBy, sortDirection, limit, offset);
		});
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
		assertEquals(folder2.getCreatedByPrincipalId().toString(), header.getCreatedBy());
		assertEquals(folder2.getCreatedOn(), header.getCreatedOn());
		assertEquals(folder2.getModifiedByPrincipalId().toString(), header.getModifiedBy());
		assertEquals(folder2.getModifiedOn(), header.getModifiedOn());
		String benefactorId = nodeDao.getBenefactor(header.getId());
		Long benefactorLong = KeyFactory.stringToKey(benefactorId);
		assertEquals(benefactorLong, header.getBenefactorId());
	}

	/**
	 * Generate the following Hierarchy:
	 * <ul>
	 * <li>project</li>
	 * <li>folder0->project, modifiedOn = 01/01/1000</li>
	 * <li>folder1->project, modifiedOn = 01/01/2000</li>
	 * <li>folder2->project, modifiedOn = 01/01/3000</li>
	 * </ul>
	 *
	 * @return list of nodes with the hierarchy outlined above
	 */
	private List<Node> createHierarchyToTestModifiedOnSort(){
		List<Node> results = new LinkedList<Node>();

		SimpleDateFormat date = new SimpleDateFormat("dd/MM/yyyy");

		// Create a project
		Node project = NodeTestUtils.createNew("hierarchy", creatorUserGroupId);
		project.setNodeType(EntityType.project);
		String projectId = nodeDao.createNew(project);
		results.add(nodeDao.getNode(projectId));
		toDelete.add(projectId);

		String folderId = null;
		Node folder1 = NodeTestUtils.createNew("folder1", creatorUserGroupId);
		folder1.setNodeType(EntityType.folder);
		folder1.setParentId(projectId);

		try {
			// set modifiedOn to year 1000
			folder1.setModifiedOn(date.parse("01/01/1000"));
		} catch (ParseException error) {
			fail(error);
		}
		folderId = nodeDao.createNew(folder1);
		results.add(nodeDao.getNode(folderId));
		toDelete.add(folderId);

		Node folder2 = NodeTestUtils.createNew("folder2", creatorUserGroupId);
		folder2.setNodeType(EntityType.folder);
		folder2.setParentId(projectId);
		try {
			// set modifiedOn to year 2000
			folder2.setModifiedOn(date.parse("01/01/2000"));
		} catch (ParseException error) {
			fail(error);
		}
		folderId = nodeDao.createNew(folder2);
		results.add(nodeDao.getNode(folderId));
		toDelete.add(folderId);

		Node folder3 = NodeTestUtils.createNew("folder3", creatorUserGroupId);
		folder3.setNodeType(EntityType.folder);
		folder3.setParentId(projectId);
		try {
			// set modifiedOn to year 3000
			folder3.setModifiedOn(date.parse("01/01/3000"));
		} catch (ParseException error) {
			fail(error);
		}
		folderId = nodeDao.createNew(folder3);
		results.add(nodeDao.getNode(folderId));
		toDelete.add(folderId);

		return results;
	}

	@Test
	public void testGetChildrenSortByModifiedOn(){
		List<Node> nodes = createHierarchyToTestModifiedOnSort();

		String parentId = nodes.get(0).getId();
		// children of the project created by createHierarchyToTestModifiedOnSort
		Node folder1 = nodes.get(1); // created first
		Node folder2 = nodes.get(2); // created second
		Node folder3 = nodes.get(3); // created third
		// set up params for request
		List<EntityType> includeTypes = Lists.newArrayList(EntityType.folder);
		SortBy sortBy = SortBy.MODIFIED_ON;
		long limit = 10L;
		long offset = 0L;
		// sort in descending order
		List<EntityHeader> descendingResults = nodeDao.getChildren(parentId, includeTypes, null, sortBy, Direction.DESC, limit, offset);
		assertNotNull(descendingResults);
		assertEquals(3, descendingResults.size());
		assertEquals(folder3.getCreatedOn(), descendingResults.get(0).getCreatedOn());
		assertEquals(folder2.getCreatedOn(), descendingResults.get(1).getCreatedOn());
		assertEquals(folder1.getCreatedOn(), descendingResults.get(2).getCreatedOn());
		// sort in ascending order
		List<EntityHeader> ascendingResults = nodeDao.getChildren(parentId, includeTypes, null, sortBy, Direction.ASC, limit, offset);
		assertNotNull(ascendingResults);
		assertEquals(3, ascendingResults.size());
		assertEquals(folder1.getCreatedOn(), ascendingResults.get(0).getCreatedOn());
		assertEquals(folder2.getCreatedOn(), ascendingResults.get(1).getCreatedOn());
		assertEquals(folder3.getCreatedOn(), ascendingResults.get(2).getCreatedOn());
	}
	
	@Test
	public void testGetChildrenStats() {
		List<Node> nodes = createHierarchy();

		Node project = nodes.get(0);
		Node folder2 = nodes.get(2);
		Node fileZero = nodes.get(3);

		String parentId = project.getId();
		List<EntityType> includeTypes = Lists.newArrayList(EntityType.file, EntityType.folder);
		// exclude folder 2.
		Set<Long> childIdsToExclude = Sets.newHashSet(KeyFactory.stringToKey(folder2.getId()), 111L);
		// call under test
		ChildStatsResponse results = nodeDao.getChildernStats(new ChildStatsRequest().withParentId(parentId)
				.withIncludeTypes(includeTypes).withChildIdsToExclude(childIdsToExclude)
				.withIncludeTotalChildCount(true).withIncludeSumFileSizes(true));
		assertNotNull(results);
		assertEquals(new Long(2), results.getTotalChildCount());
		assertEquals(new Long(TEST_FILE_SIZE), results.getSumFileSizesBytes());
	}
	
	@Test
	public void testGetChildrenStatsNoResults() {
		String parentId = "syn123";
		List<EntityType> includeTypes = Lists.newArrayList(EntityType.file, EntityType.folder);
		Set<Long> childIdsToExclude = Sets.newHashSet(111L);
		// call under test
		ChildStatsResponse results = nodeDao.getChildernStats(new ChildStatsRequest().withParentId(parentId)
				.withIncludeTypes(includeTypes).withChildIdsToExclude(childIdsToExclude)
				.withIncludeTotalChildCount(true).withIncludeSumFileSizes(true));
		assertNotNull(results);
		assertEquals(new Long(0), results.getTotalChildCount());
		assertEquals(new Long(0), results.getSumFileSizesBytes());
	}
	
	@Test
	public void testGetChildrenStatsNothingToExclude() {
		String parentId = "syn123";
		List<EntityType> includeTypes = Lists.newArrayList(EntityType.file, EntityType.folder);
		Set<Long> childIdsToExclude = new HashSet<>();
		// call under test
		ChildStatsResponse results = nodeDao.getChildernStats(new ChildStatsRequest().withParentId(parentId)
				.withIncludeTypes(includeTypes).withChildIdsToExclude(childIdsToExclude)
				.withIncludeTotalChildCount(true).withIncludeSumFileSizes(true));
		assertNotNull(results);
		assertEquals(new Long(0), results.getTotalChildCount());
		assertEquals(new Long(0), results.getSumFileSizesBytes());
	}
	
	@Test
	public void testGetChildrenStatsIncludedNull() {
		String parentId = "syn123";
		List<EntityType> includeTypes = Lists.newArrayList(EntityType.file, EntityType.folder);
		Set<Long> childIdsToExclude = Sets.newHashSet(111L);
		// call under test
		ChildStatsResponse results = nodeDao.getChildernStats(new ChildStatsRequest().withParentId(parentId)
				.withIncludeTypes(includeTypes).withChildIdsToExclude(childIdsToExclude)
				.withIncludeTotalChildCount(null).withIncludeSumFileSizes(null));
		assertNotNull(results);
		assertEquals(null, results.getTotalChildCount());
		assertEquals(null, results.getSumFileSizesBytes());
	}
	
	@Test
	public void testGetChildrenStatsIncludedFalse() {
		String parentId = "syn123";
		List<EntityType> includeTypes = Lists.newArrayList(EntityType.file, EntityType.folder);
		Set<Long> childIdsToExclude = Sets.newHashSet(111L);
		// call under test
		ChildStatsResponse results = nodeDao.getChildernStats(new ChildStatsRequest().withParentId(parentId)
				.withIncludeTypes(includeTypes).withChildIdsToExclude(childIdsToExclude)
				.withIncludeTotalChildCount(false).withIncludeSumFileSizes(false));
		assertNotNull(results);
		assertEquals(null, results.getTotalChildCount());
		assertEquals(null, results.getSumFileSizesBytes());
	}
	
	@Test
	public void testGetChildrenStatsIncludedCountFalseIncludeSumNull() {
		String parentId = "syn123";
		List<EntityType> includeTypes = Lists.newArrayList(EntityType.file, EntityType.folder);
		Set<Long> childIdsToExclude = Sets.newHashSet(111L);
		// call under test
		ChildStatsResponse results = nodeDao.getChildernStats(new ChildStatsRequest().withParentId(parentId)
				.withIncludeTypes(includeTypes).withChildIdsToExclude(childIdsToExclude)
				.withIncludeTotalChildCount(false).withIncludeSumFileSizes(null));
		assertNotNull(results);
		assertEquals(null, results.getTotalChildCount());
		assertEquals(null, results.getSumFileSizesBytes());
	}
	
	@Test
	public void testGetChildrenStatsIncludedCountNullIncludeSumFalse() {
		String parentId = "syn123";
		List<EntityType> includeTypes = Lists.newArrayList(EntityType.file, EntityType.folder);
		Set<Long> childIdsToExclude = Sets.newHashSet(111L);
		// call under test
		ChildStatsResponse results = nodeDao.getChildernStats(new ChildStatsRequest().withParentId(parentId)
				.withIncludeTypes(includeTypes).withChildIdsToExclude(childIdsToExclude)
				.withIncludeTotalChildCount(null).withIncludeSumFileSizes(false));
		assertNotNull(results);
		assertEquals(null, results.getTotalChildCount());
		assertEquals(null, results.getSumFileSizesBytes());
	}
	
	@Test
	public void testGetChildrenStatsIncludedCountTrueIncludeSumFalse() {
		String parentId = "syn123";
		List<EntityType> includeTypes = Lists.newArrayList(EntityType.file, EntityType.folder);
		Set<Long> childIdsToExclude = Sets.newHashSet(111L);
		// call under test
		ChildStatsResponse results = nodeDao.getChildernStats(new ChildStatsRequest().withParentId(parentId)
				.withIncludeTypes(includeTypes).withChildIdsToExclude(childIdsToExclude)
				.withIncludeTotalChildCount(true).withIncludeSumFileSizes(false));
		assertNotNull(results);
		assertEquals(new Long(0), results.getTotalChildCount());
		assertEquals(null, results.getSumFileSizesBytes());
	}
	
	@Test
	public void testGetChildrenStatsIncludedCountFalseIncludeSumTrue() {
		String parentId = "syn123";
		List<EntityType> includeTypes = Lists.newArrayList(EntityType.file, EntityType.folder);
		Set<Long> childIdsToExclude = Sets.newHashSet(111L);
		// call under test
		ChildStatsResponse results = nodeDao.getChildernStats(new ChildStatsRequest().withParentId(parentId)
				.withIncludeTypes(includeTypes).withChildIdsToExclude(childIdsToExclude)
				.withIncludeTotalChildCount(false).withIncludeSumFileSizes(true));
		assertNotNull(results);
		assertEquals(null, results.getTotalChildCount());
		assertEquals(new Long(0), results.getSumFileSizesBytes());
	}
	
	@Test
	public void testGetChildrenStatsNullRequest() {
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			nodeDao.getChildernStats(null);
		});
	}
	
	@Test
	public void testGetChildrenStatsNullParentId() {
		String parentId = null;
		List<EntityType> includeTypes = Lists.newArrayList(EntityType.file, EntityType.folder);
		Set<Long> childIdsToExclude = Sets.newHashSet(111L);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			nodeDao.getChildernStats(new ChildStatsRequest().withParentId(parentId)
					.withIncludeTypes(includeTypes).withChildIdsToExclude(childIdsToExclude)
					.withIncludeTotalChildCount(true).withIncludeSumFileSizes(false));
		});
	}
	
	@Test
	public void testGetChildrenStatsNullTypes() {
		String parentId = "syn123";
		List<EntityType> includeTypes = null;
		Set<Long> childIdsToExclude = Sets.newHashSet(111L);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			nodeDao.getChildernStats(new ChildStatsRequest().withParentId(parentId)
					.withIncludeTypes(includeTypes).withChildIdsToExclude(childIdsToExclude)
					.withIncludeTotalChildCount(true).withIncludeSumFileSizes(false));
		});
	}
	
	@Test
	public void testGetChildrenStatsEmptyTypes() {
		String parentId = "syn123";
		List<EntityType> includeTypes = new LinkedList<>();
		Set<Long> childIdsToExclude = Sets.newHashSet(111L);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			nodeDao.getChildernStats(new ChildStatsRequest().withParentId(parentId)
					.withIncludeTypes(includeTypes).withChildIdsToExclude(childIdsToExclude)
					.withIncludeTotalChildCount(true).withIncludeSumFileSizes(false));
		});
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

	@Test
	public void testLoopupChildWithNullParentId(){
		assertThrows(IllegalArgumentException.class, ()->{
			nodeDao.lookupChild(null, "entityName");
		});
	}

	@Test
	public void testLoopupChildWithInvalidParentId(){
		assertThrows(IllegalArgumentException.class, ()->{
			nodeDao.lookupChild("parentId", "entityName");
		});
	}

	@Test
	public void testLoopupChildWithNullEntityName(){
		assertThrows(IllegalArgumentException.class, ()->{
			nodeDao.lookupChild("syn1", null);
		});
	}

	@Test 
	public void testLoopupChildNotFound(){
		assertThrows(NotFoundException.class, ()->{
			nodeDao.lookupChild("syn1", "entityName");
		});
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
	
	@Test
	public void testGetBenefactorEntityDoesNotExist(){
		assertThrows(NotFoundException.class, ()->{
			// should not exist
			nodeDao.getBenefactor("syn9999");
		});
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
		assertEquals(grandparent.getId(), benefactor, "Entity should be its own benefactor");
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
		parent.setNodeType(EntityType.folder);
		parent = nodeDao.createNewNode(parent);
		Long parentId = KeyFactory.stringToKey(parent.getId());
		toDelete.add(parent.getId());
		
		// add an acl for the parent
		AccessControlList acl = AccessControlListUtil.createACLToGrantEntityAdminAccess(""+parentId, adminUser, new Date());
		accessControlListDAO.create(acl, ObjectType.ENTITY);
		
		// child
		Node child = NodeTestUtils.createNew("child", creatorUserGroupId);
		child.setParentId(parent.getId());
		child.setNodeType(EntityType.file);
		child = nodeDao.createNewNode(child);
		Long childId = KeyFactory.stringToKey(child.getId());
		toDelete.add(child.getId());
		// call under test
		List<IdAndEtag> results = nodeDao.getChildren(grandId);
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals(new IdAndEtag(parentId, parent.getETag(), parentId), results.get(0));
		// call under test
		results = nodeDao.getChildren(parentId);
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals(new IdAndEtag(childId, child.getETag(), parentId), results.get(0));
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
	
	@Test
	public void testTouch() throws InterruptedException {
		Long user1Id = Long.parseLong(user1);
		Long user2Id = Long.parseLong(user2);
		// created by user 1.
		Node start = NodeTestUtils.createNew("one",  user1Id);
		start = nodeDao.createNewNode(start);
		Long oneId = KeyFactory.stringToKey(start.getId());
		toDelete.add(start.getId());
		// sleep to change updatedOn
		Thread.sleep(10);
		
		// call under test (updated by user 2).
		String newEtag = nodeDao.touch(user2Id, start.getId());
		assertNotNull(newEtag);
		// etag must change
		assertFalse(start.getETag().equals(newEtag));
		Node afterTouch = nodeDao.getNode(start.getId());
		assertEquals(afterTouch.getETag(), newEtag);
		assertTrue(afterTouch.getModifiedOn().getTime() > start.getModifiedOn().getTime());
		assertEquals(user2Id, afterTouch.getModifiedByPrincipalId());
		assertEquals(user1Id, start.getCreatedByPrincipalId());
	}
	
	@Test
	public void testSnapshotVersion() throws InterruptedException {
		Long user1Id = Long.parseLong(user1);
		Long user2Id = Long.parseLong(user2);
		Node node = NodeTestUtils.createNew("one",  user1Id);
		node = nodeDao.createNewNode(node);
		toDelete.add(node.getId());
		// sleep so modified on is larger than the start.
		Thread.sleep(10);
		
		SnapshotRequest request1 = new SnapshotRequest();
		request1.setSnapshotComment("a comment string");
		request1.setSnapshotLabel("some label");
		request1.setSnapshotActivityId(testActivity.getId());
		
		// call under test
		Long snapshotVersion1 = nodeDao.snapshotVersion(user1Id, node.getId(), request1);
		assertEquals(node.getVersionNumber(), snapshotVersion1);
		Node current = nodeDao.getNodeForVersion(node.getId(), snapshotVersion1);
		assertEquals(user1Id, current.getModifiedByPrincipalId());
		assertTrue(current.getModifiedOn().getTime() > node.getModifiedOn().getTime());
		assertEquals(request1.getSnapshotComment(), current.getVersionComment());
		assertEquals(request1.getSnapshotLabel(), current.getVersionLabel());
		assertEquals(request1.getSnapshotActivityId(), current.getActivityId());
		
		// Create a new version then a new snapshot
		current.setVersionComment("in-progress");
		current.setVersionLabel("in-progress");
		current.setActivityId(null);
		Long newVersion = nodeDao.createNewVersion(current);
		
		// Create a second snapshot for the current version.s
		SnapshotRequest request2 = new SnapshotRequest();
		request2.setSnapshotComment("different comment");
		request2.setSnapshotLabel("different label");
		request2.setSnapshotActivityId(testActivity2.getId());
		
		// call under test
		Long snapshotVersion2 = nodeDao.snapshotVersion(user2Id, node.getId(), request2);
		assertEquals(newVersion, snapshotVersion2);
		Node snapshot2 = nodeDao.getNodeForVersion(node.getId(), snapshotVersion2);
		assertEquals(user2Id, snapshot2.getModifiedByPrincipalId());
		assertEquals(request2.getSnapshotComment(), snapshot2.getVersionComment());
		assertEquals(request2.getSnapshotLabel(), snapshot2.getVersionLabel());
		assertEquals(request2.getSnapshotActivityId(), snapshot2.getActivityId());
		
		// the first snapshot should not be changed.
		Node snapshot1 = nodeDao.getNodeForVersion(node.getId(), snapshotVersion1);
		assertEquals(user1Id, snapshot1.getModifiedByPrincipalId());
		assertEquals(request1.getSnapshotComment(), snapshot1.getVersionComment());
		assertEquals(request1.getSnapshotLabel(), snapshot1.getVersionLabel());
		assertEquals(request1.getSnapshotActivityId(), snapshot1.getActivityId());
	}
	
	@Test
	public void testSnapshotVersionNullValues() {
		Long user1Id = Long.parseLong(user1);
		Node node = NodeTestUtils.createNew("one",  user1Id);
		node = nodeDao.createNewNode(node);
		toDelete.add(node.getId());
		
		SnapshotRequest request = new SnapshotRequest();
		request.setSnapshotComment(null);
		request.setSnapshotLabel(null);
		request.setSnapshotActivityId(null);
		
		// call under test
		Long snapshotVersion = nodeDao.snapshotVersion(user1Id, node.getId(), request);
		assertEquals(node.getVersionNumber(), snapshotVersion);
		Node current = nodeDao.getNodeForVersion(node.getId(), snapshotVersion);
		assertEquals(null, current.getVersionComment());
		assertEquals(snapshotVersion.toString(), current.getVersionLabel());
		assertEquals(null, current.getActivityId());
	}
	
	@Test
	public void testSnapshotVersionNullId() {
		Long user1Id = Long.parseLong(user1);
		String nodeId = null;
		SnapshotRequest request = new SnapshotRequest();
		request.setSnapshotComment(null);
		request.setSnapshotLabel(null);
		request.setSnapshotActivityId(null);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			nodeDao.snapshotVersion(user1Id, nodeId, request);
		});
	}
	
	@Test
	public void testSnapshotVersionNullRequest() {
		Long user1Id = Long.parseLong(user1);
		Node node = NodeTestUtils.createNew("one",  user1Id);
		node = nodeDao.createNewNode(node);
		toDelete.add(node.getId());
		
		SnapshotRequest request = null;
		
		// call under test
		Long snapshotVersion = nodeDao.snapshotVersion(user1Id, node.getId(), request);
		assertEquals(node.getVersionNumber(), snapshotVersion);
		Node current = nodeDao.getNodeForVersion(node.getId(), snapshotVersion);
		assertEquals(null, current.getVersionComment());
		assertEquals(snapshotVersion.toString(), current.getVersionLabel());
		assertEquals(null, current.getActivityId());
	}
	
	@Test
	public void testSnapshotVersionNullUserId() {
		Long user1Id = Long.parseLong(user1);;
		Node node = NodeTestUtils.createNew("one",  user1Id);
		node = nodeDao.createNewNode(node);
		String nodeId = node.getId();
		toDelete.add(node.getId());
		
		SnapshotRequest request = new SnapshotRequest();
		request.setSnapshotComment(null);
		request.setSnapshotLabel(null);
		request.setSnapshotActivityId(null);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			Long nullUserId = null;
			Long snapshotVersion = nodeDao.snapshotVersion(nullUserId, nodeId, request);
		});
	}
	
	@Test
	public void testPLFM_5439() {
		// Create two nodes with the same parent that differ by case only
		Node parent = NodeTestUtils.createNew("parent", creatorUserGroupId);
		parent = nodeDao.createNewNode(parent);
		toDelete.add(parent.getId());
		// child one
		Node one = NodeTestUtils.createNew("Foo", creatorUserGroupId);
		one.setParentId(parent.getId());
		one = nodeDao.createNewNode(one);
		toDelete.add(one.getId());
		// child two
		Node two = NodeTestUtils.createNew("foo", creatorUserGroupId);
		two.setParentId(parent.getId());
		two = nodeDao.createNewNode(two);
		toDelete.add(two.getId());
	}

	@Test
	public void testUserAnnotations_EmptyAnnotationsRoundTrip(){
		Node node = nodeDao.createNewNode(privateCreateNew("testEmptyNamedAnnotations"));
		String id = node.getId();
		toDelete.add(id);
		assertNotNull(id);
		// Now get the annotations for this node.
		Annotations annos = nodeDao.getUserAnnotations(id);
		assertNotNull(annos);
		assertTrue(annos.getAnnotations().isEmpty());
		// Write the annotation to database
		nodeDao.updateUserAnnotations(id, annos);

		//check no BLOB no data has been stored in the actual JDOREVISIONS table
		MapSqlParameterSource parameterSource = new MapSqlParameterSource();
		parameterSource.addValue("owner", KeyFactory.stringToKey(id));
		parameterSource.addValue("revisionNumber", nodeDao.getCurrentRevisionNumber(id));
		DBORevision nodeRevision = basicDao.getObjectByPrimaryKey(DBORevision.class, parameterSource);

		// Now retrieve it and we should stil get back an empty NamedAnnotation
		Annotations copy = nodeDao.getUserAnnotations(id);
		assertNotNull(copy);
		assertTrue(copy.getAnnotations().isEmpty());
		assertEquals(annos, copy);
	}

	@Test
	public void testUserAnnotationsRoundTrip(){
		Node node = nodeDao.createNewNode(privateCreateNew("testUpdateUserAnnotations"));
		toDelete.add(node.getId());


		Annotations annotationsV2 = new Annotations();
		annotationsV2.setAnnotations(Collections.singletonMap(
				"myKey",
				AnnotationsV2TestUtils.createNewValue(AnnotationsValueType.STRING, "myValue1", "myValue2"
				)));
		nodeDao.updateUserAnnotations(node.getId(), annotationsV2);

		Annotations retrievedAnnotations = nodeDao.getUserAnnotations(node.getId());

		//verify id and etag information were added
		assertEquals(node.getId(), retrievedAnnotations.getId());
		assertNotNull(retrievedAnnotations.getEtag());

		//verify that the annotations map are equivalent but not same object
		assertNotSame(annotationsV2.getAnnotations(), retrievedAnnotations.getAnnotations());
		assertEquals(annotationsV2.getAnnotations(), retrievedAnnotations.getAnnotations());
	}

	@Test
	public void testGetUserAnnotationsForVersion(){
		Node node = nodeDao.createNewNode(privateCreateNew("testGetUserAnnotationsForVersion"));
		String nodeId = node.getId();
		toDelete.add(nodeId);

		//set up annotations for first version
		Annotations oldNodeVersionAnnotations = new Annotations();
		oldNodeVersionAnnotations.setAnnotations(Collections.singletonMap(
				"myKey",
				AnnotationsV2TestUtils.createNewValue(AnnotationsValueType.STRING, "version1Value", "version1Value2"
				)));
		nodeDao.updateUserAnnotations(nodeId, oldNodeVersionAnnotations);
		Long oldVersion = node.getVersionNumber();


		//create a new version of the node and give it annotations
		node.setVersionComment("Comment "+2);
		node.setVersionLabel("2");
		Long newVersion = nodeDao.createNewVersion(node);

		assertNotEquals(oldVersion, newVersion);

		Annotations newNodeVersionAnnotations = new Annotations();
		newNodeVersionAnnotations.setAnnotations(Collections.singletonMap(
				"myKey",
				AnnotationsV2TestUtils.createNewValue(AnnotationsValueType.STRING, "version2Value", "version2Value2"
				)));
		nodeDao.updateUserAnnotations(nodeId, newNodeVersionAnnotations);


		Annotations retrievedOldVersion = nodeDao.getUserAnnotationsForVersion(nodeId, oldVersion);
		Annotations retrievedNewVersion = nodeDao.getUserAnnotationsForVersion(nodeId, newVersion);

		//verify old and new versions do not have same content
		assertNotEquals(retrievedOldVersion.getAnnotations(),retrievedNewVersion.getAnnotations());

		//verify that the annotations map are equivalent but not same object
		assertNotSame(oldNodeVersionAnnotations.getAnnotations(), retrievedOldVersion.getAnnotations());
		assertEquals(oldNodeVersionAnnotations.getAnnotations(), retrievedOldVersion.getAnnotations());

		//verify that the annotations map are equivalent but not same object
		assertNotSame(newNodeVersionAnnotations.getAnnotations(), retrievedNewVersion.getAnnotations());
		assertEquals(newNodeVersionAnnotations.getAnnotations(), retrievedNewVersion.getAnnotations());
	}

	@Test
	public void testEntityPropertiesRoundTrip(){

		String nodeId = nodeDao.createNewNode(privateCreateNew("testEntityPropertiesRoundTrip")).getId();
		
		toDelete.add(nodeId);

		org.sagebionetworks.repo.model.Annotations entityPropertyAnnotations = new org.sagebionetworks.repo.model.Annotations();
		entityPropertyAnnotations.addAnnotation("primaryString", "primaryTest");
		nodeDao.updateEntityPropertyAnnotations(nodeId, entityPropertyAnnotations);
		org.sagebionetworks.repo.model.Annotations retrieved = nodeDao.getEntityPropertyAnnotations(nodeId);

		assertEquals(entityPropertyAnnotations.getStringAnnotations(), retrieved.getStringAnnotations());
	}

	@Test
	public void testEntityPropertiesForVersion(){
		org.sagebionetworks.repo.model.Annotations oldEntityPropertyAnnotations = new org.sagebionetworks.repo.model.Annotations();
		oldEntityPropertyAnnotations.addAnnotation("primaryString", "primaryTest");

		Node node = nodeDao.createNewNode(privateCreateNew("testEntityPropertiesRoundTrip"));
		String nodeId = node.getId();
		Long oldNodeVersion = node.getVersionNumber();

		nodeDao.updateEntityPropertyAnnotations(nodeId, oldEntityPropertyAnnotations);

		Node newNode = nodeDao.getNode(nodeId);
		newNode.setVersionComment("Comment "+2);
		newNode.setVersionLabel("2");
		Long newNodeVersion = nodeDao.createNewVersion(newNode);
		org.sagebionetworks.repo.model.Annotations newEntityPropertiesAnnotations = new org.sagebionetworks.repo.model.Annotations();
		newEntityPropertiesAnnotations.addAnnotation("primaryString", "NEW VALUE YEAH");
		nodeDao.updateEntityPropertyAnnotations(nodeId, newEntityPropertiesAnnotations);


		org.sagebionetworks.repo.model.Annotations retrievedOldVersion = nodeDao.getEntityPropertyAnnotationsForVersion(nodeId, oldNodeVersion);
		org.sagebionetworks.repo.model.Annotations retrievedNewVersion = nodeDao.getEntityPropertyAnnotationsForVersion(nodeId, newNodeVersion);

		assertNotEquals(retrievedOldVersion.getStringAnnotations(), retrievedNewVersion.getStringAnnotations());

		assertEquals(oldEntityPropertyAnnotations.getStringAnnotations(), retrievedOldVersion.getStringAnnotations());
		assertEquals(newEntityPropertiesAnnotations.getStringAnnotations(), retrievedNewVersion.getStringAnnotations());
	}
	
	@Test
	public void testGetName() {
		String name = "some name to test for";
		Node node = nodeDao.createNewNode(privateCreateNew(name));
		toDelete.add(node.getId());
		// call under test
		String resultName = nodeDao.getNodeName(node.getId());
		assertEquals(name, resultName);
	}
	
	@Test
	public void testGetNameNotFound() {
		assertThrows(NotFoundException.class, ()->{
			// call under test
			nodeDao.getNodeName("syn999999");
		});
	}
	
	@Test
	public void testFindFirstBoundJsonSchema() throws JSONObjectAdapterException {
		jsonSchemaTestHelper.truncateAll();
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

		String schema$id = "my.org-foo.bar-1.2.3";
		int index = 0;
		JsonSchemaVersionInfo schemaInfo = jsonSchemaTestHelper.createNewSchemaVersion(creatorUserGroupId, schema$id,
				index);
		// bind the schema to the grand parent
		JsonSchemaObjectBinding binding = jsonSchemaTestHelper.bindSchemaToObject(creatorUserGroupId, schema$id,
				grandId, BoundObjectType.entity);

		// call under test
		Long boundEntityId = nodeDao.getEntityIdOfFirstBoundSchema(childId);
		assertEquals(grandId, boundEntityId);
		// call under test
		boundEntityId = nodeDao.getEntityIdOfFirstBoundSchema(parentId);
		assertEquals(grandId, boundEntityId);
		// call under test
		boundEntityId = nodeDao.getEntityIdOfFirstBoundSchema(grandId);
		assertEquals(grandId, boundEntityId);

		// override the schema at the parent level
		binding = jsonSchemaTestHelper.bindSchemaToObject(creatorUserGroupId, schema$id, parentId,
				BoundObjectType.entity);

		// call under test
		boundEntityId = nodeDao.getEntityIdOfFirstBoundSchema(childId);
		assertEquals(parentId, boundEntityId);
		// call under test
		boundEntityId = nodeDao.getEntityIdOfFirstBoundSchema(parentId);
		assertEquals(parentId, boundEntityId);
		// call under test
		boundEntityId = nodeDao.getEntityIdOfFirstBoundSchema(grandId);
		assertEquals(grandId, boundEntityId);
	}
	
	@Test
	public void testFindFirstBoundJsonSchemaWithMaxDepth() throws JSONObjectAdapterException {
		jsonSchemaTestHelper.truncateAll();
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

		String schema$id = "my.org-foo.bar-1.2.3";
		int index = 0;
		JsonSchemaVersionInfo schemaInfo = jsonSchemaTestHelper.createNewSchemaVersion(creatorUserGroupId, schema$id,
				index);
		// bind the schema to the grand parent
		JsonSchemaObjectBinding binding = jsonSchemaTestHelper.bindSchemaToObject(creatorUserGroupId, schema$id,
				grandId, BoundObjectType.entity);
		
		// call under test
		Long boundEntityId = nodeDao.getEntityIdOfFirstBoundSchema(childId, 3/*maxDepth*/);
		assertEquals(grandId, boundEntityId);
		String message = assertThrows(NotFoundException.class, () -> {
			// call under test
			nodeDao.getEntityIdOfFirstBoundSchema(childId, 2/*maxDepth*/);
		}).getMessage();
		assertEquals("No JSON schema found for 'syn" + childId + "'", message);
	}

	@Test
	public void testFindFirstBoundJsonSchemaWithNoMatch() {
		jsonSchemaTestHelper.truncateAll();
		// setup a node with no binding.
		Node grandparent = NodeTestUtils.createNew("grandparent", creatorUserGroupId);
		grandparent = nodeDao.createNewNode(grandparent);
		Long grandId = KeyFactory.stringToKey(grandparent.getId());
		toDelete.add(grandparent.getId());
		String message = assertThrows(NotFoundException.class, () -> {
			// call under test
			nodeDao.getEntityIdOfFirstBoundSchema(grandId);
		}).getMessage();
		assertEquals("No JSON schema found for 'syn" + grandId + "'", message);
	}

	@Test
	public void testFindFirstBoundJsonSchemaWithNullNodeId() {
		jsonSchemaTestHelper.truncateAll();
		Long nodeId = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			nodeDao.getEntityIdOfFirstBoundSchema(nodeId);
		});
	}
	
	@Test
	public void testUpdateRevisionFileHandle() {
		Node node = NodeTestUtils.createNew("Node", creatorUserGroupId);
		node.setNodeType(EntityType.file);
		node.setFileHandleId(fileHandle.getId());
		node = nodeDao.createNewNode(node);
		
		toDelete.add(node.getId());
		
		String nodeId = node.getId();
		Long versionNumber = node.getVersionNumber();
		String newFileHandleId = fileHandle2.getId();
		
		// Call under test
		boolean result = nodeDao.updateRevisionFileHandle(nodeId, versionNumber, newFileHandleId);
		
		assertTrue(result);
	}
	
	@Test
	public void testUpdateRevisionFileHandleWithNonExistingNode() {
		
		String nodeId = "123";
		Long versionNumber = 2L;
		String newFileHandleId = fileHandle2.getId();
		
		// Call under test
		boolean result = nodeDao.updateRevisionFileHandle(nodeId, versionNumber, newFileHandleId);
		
		assertFalse(result);
	}
	
	@Test
	public void testUpdateRevisionFileHandleWithNonExistingRevision() {
		
		Node node = NodeTestUtils.createNew("Node", creatorUserGroupId);
		node.setNodeType(EntityType.file);
		node.setFileHandleId(fileHandle.getId());
		node = nodeDao.createNewNode(node);
		
		toDelete.add(node.getId());
		
		String nodeId = node.getId();
		Long versionNumber = node.getVersionNumber() + 1;
		String newFileHandleId = fileHandle2.getId();
		
		// Call under test
		boolean result = nodeDao.updateRevisionFileHandle(nodeId, versionNumber, newFileHandleId);
		
		assertFalse(result);
	}
	
	@Test
	public void testGetEntityPathDepthWithNullId() {
		String entityId = null;
		int maxDepth = 10;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			nodeDao.getEntityPathDepth(entityId, maxDepth);
		}).getMessage();
		assertEquals("entityId is required.", message);
	}
	
	@Test
	public void testGetEntityPathDepthWithDoesNotExist() {
		String entityId = "syn111";
		int maxDepth = 10;
		String message = assertThrows(NotFoundException.class, ()->{
			// call under test
			nodeDao.getEntityPathDepth(entityId, maxDepth);
		}).getMessage();
		assertEquals("Not found entityId: 'syn111'", message);
	}
	
	@Test
	public void testGetEntityPathDepth() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName("aProject");
			n.setCreatedByPrincipalId(creatorUserGroupId);
		});
		toDelete.add(project.getId());
		Node folder = nodeDaoHelper.create(n -> {
			n.setName("aFolder");
			n.setCreatedByPrincipalId(creatorUserGroupId);
			n.setParentId(project.getId());
			n.setNodeType(EntityType.folder);
		});
		Node file = nodeDaoHelper.create(n -> {
			n.setName("aFile");
			n.setCreatedByPrincipalId(creatorUserGroupId);
			n.setParentId(folder.getId());
			n.setNodeType(EntityType.file);
		});
		int maxDepth = 10;
		// call under test
		assertEquals(1, nodeDao.getEntityPathDepth(project.getId(), maxDepth));
		assertEquals(2, nodeDao.getEntityPathDepth(folder.getId(), maxDepth));
		assertEquals(3, nodeDao.getEntityPathDepth(file.getId(), maxDepth));
		maxDepth = 1;
		assertEquals(1, nodeDao.getEntityPathDepth(file.getId(), maxDepth));
	}
}
