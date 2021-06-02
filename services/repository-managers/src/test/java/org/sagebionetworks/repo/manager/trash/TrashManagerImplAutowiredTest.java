package org.sagebionetworks.repo.manager.trash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.EntityAclManager;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.dataaccess.AccessRequirementManager;
import org.sagebionetworks.repo.manager.dataaccess.AccessRequirementManagerImpl;
import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.trash.TrashCanDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.Lists;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class TrashManagerImplAutowiredTest {

	@Autowired 
	private TrashManager trashManager;
	
	@Autowired 
	private NodeManager nodeManager;
	
	@Autowired 
	private EntityAclManager entityAclManager;
	
	@Autowired 
	private EntityAuthorizationManager entityAuthorizationManager;
	
	@Autowired 
	private TrashCanDao trashCanDao;
	
	@Autowired 
	private NodeDAO nodeDAO;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired 
	private AccessRequirementManager accessRequirementManager;
	
	private UserInfo testAdminUserInfo;
	private UserInfo testUserInfo;
	private String trashCanId;
	private List<String> toClearList;
	
	private AccessRequirement accessRequirementToDelete;

	@BeforeEach
	public void before() throws Exception {
		testAdminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		
		NewUser user = new NewUser();
		user.setEmail(UUID.randomUUID().toString() + "@test.com");
		user.setUserName(UUID.randomUUID().toString());
		testUserInfo = userManager.getUserInfo(userManager.createUser(user));
		testUserInfo.getGroups().add(BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
		testUserInfo.setAcceptsTermsOfUse(true);
		assertNotNull(testUserInfo);
		assertFalse(testUserInfo.isAdmin());

		trashCanId = KeyFactory.keyToString(TrashConstants.TRASH_FOLDER_ID);
		assertNotNull(trashCanId);
		Node trashFolder = nodeDAO.getNode(trashCanId);
		assertNotNull(trashFolder);
		assertNotNull(trashFolder.getParentId());
		assertTrue(nodeDAO.isNodeRoot(trashFolder.getParentId()));
		String benefactorId = nodeDAO.getBenefactor(trashCanId);
		assertNotNull(benefactorId);
		assertEquals(trashCanId, benefactorId);

		toClearList = new ArrayList<String>();
		cleanUp(); // Clean up leftovers from other test cases
	}

	@AfterEach
	public void after() throws Exception {
		cleanUp();
		
		userManager.deletePrincipal(testAdminUserInfo, testUserInfo.getId());
	}
	
	private List<TrashedEntity> inspectUsersTrashCan(UserInfo userInfo, int expectedSize) throws Exception {
		List<TrashedEntity> results = trashManager.listTrashedEntities(userInfo, userInfo, 0L, 1000L);
		assertEquals(expectedSize, results.size());
		return results;
	}
	
	private Node createNode(final String name, EntityType type, String parentId) throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException {
		Node node = new Node();
		node.setName(name);
		node.setNodeType(type);
		if (parentId!=null) node.setParentId(parentId);
		final String nodeId = nodeManager.createNewNode(node, testUserInfo);
		assertNotNull(nodeId);
		toClearList.add(nodeId);
		Node nodeRetrieved = nodeManager.getNode(testUserInfo, nodeId);
		assertNotNull(nodeRetrieved);
		if (parentId!=null) assertEquals(parentId, nodeRetrieved.getParentId());
		return nodeRetrieved;
	}

	@Test
	public void testSingleNodeRoundTrip() throws Exception {
		inspectUsersTrashCan(testUserInfo, 0);
		Node nodeParent = createNode("TrashManagerImplAutowiredTest.testSingleNodeRoundTrip() Parent",EntityType.project, null);
		final String nodeParentId = nodeParent.getId();
		String nodeChildName = "TrashManagerImplAutowiredTest.testSingleNodeRoundTrip() Child";
		Node nodeChild = createNode(nodeChildName, EntityType.folder, nodeParentId);
		final String nodeChildId = nodeChild.getId();

		trashManager.moveToTrash(testUserInfo, nodeChildId, false);

		Assertions.assertThrows(EntityInTrashCanException.class, () -> {
			nodeManager.getNode(testUserInfo, nodeChildId);
		});

		List<TrashedEntity> results = inspectUsersTrashCan(testUserInfo, 1);
		TrashedEntity trash = results.get(0);
		assertNotNull(trash);
		assertEquals(nodeChildId, trash.getEntityId());
		assertEquals(nodeChildName, trash.getEntityName());
		assertEquals(nodeParentId, trash.getOriginalParentId());
		assertEquals(testUserInfo.getId().toString(), trash.getDeletedByPrincipalId());
		assertNotNull(trash.getDeletedOn());

		trashManager.restoreFromTrash(testUserInfo, nodeChildId, nodeParentId);

		inspectUsersTrashCan(testUserInfo, 0);

		Node nodeChildRetrieved = nodeManager.getNode(testUserInfo, nodeChildId);
		assertNotNull(nodeChildRetrieved);
		assertEquals(nodeChildId, nodeChildRetrieved.getId());
		assertEquals(nodeChildName, nodeChildRetrieved.getName());
		assertEquals(nodeParentId, nodeChildRetrieved.getParentId());
		assertEquals(nodeParentId, nodeDAO.getBenefactor(nodeChildRetrieved.getId()));
	}
	
	@Test
	public void testRestrictedNodeRoundTrip() throws Exception {
		inspectUsersTrashCan(testUserInfo, 0);
		Node nodeParent = createNode("TrashManagerImplAutowiredTest.testSingleNodeRoundTrip() Parent",EntityType.project, null);
		final String nodeParentId = nodeParent.getId();
		String nodeChildName = "TrashManagerImplAutowiredTest.testSingleNodeRoundTrip() Child";
		Node nodeChild = createNode(nodeChildName, EntityType.folder, nodeParentId);
		final String nodeChildId = nodeChild.getId();

		// add an access requirement to the parent
		AccessRequirement ar = AccessRequirementManagerImpl.newLockAccessRequirement(testAdminUserInfo, nodeParentId, "jiraKey");
		accessRequirementToDelete = accessRequirementManager.createAccessRequirement(testAdminUserInfo, ar);
		
		// delete and try to restore to some other (unrestricted) parent
		trashManager.moveToTrash(testUserInfo, nodeChildId, false);
		Node adoptiveParent = createNode("TrashManagerImplAutowiredTest.testSingleNodeRoundTrip() Adoptive Parent",EntityType.project, null);
		final String adoptiveParentId = adoptiveParent.getId();
		
		Assertions.assertThrows(UnauthorizedException.class, () -> {
			trashManager.restoreFromTrash(testUserInfo, nodeChildId, adoptiveParentId);
		});
		
		// restore to original parent
		trashManager.restoreFromTrash(testUserInfo, nodeChildId, nodeParentId);
	}

	@Test
	public void testRestrictedNodeDeleteOriginalParent() throws Exception {
		inspectUsersTrashCan(testUserInfo, 0);
		Node nodeParent = createNode("TrashManagerImplAutowiredTest.testSingleNodeRoundTrip() Parent",EntityType.project, null);
		final String nodeParentId = nodeParent.getId();
		String nodeChildName = "TrashManagerImplAutowiredTest.testSingleNodeRoundTrip() Child";
		Node nodeChild = createNode(nodeChildName, EntityType.folder, nodeParentId);
		final String nodeChildId = nodeChild.getId();

		// delete and try to restore to some other (unrestricted) parent
		trashManager.moveToTrash(testUserInfo, nodeChildId, false);
		
		// now delete the original parent
		nodeManager.delete(testUserInfo, nodeParentId);
		toClearList.remove(nodeParentId);
		
		Node adoptiveParent = createNode("TrashManagerImplAutowiredTest.testSingleNodeRoundTrip() Adoptive Parent",EntityType.project, null);
		final String adoptiveParentId = adoptiveParent.getId();
		
		Assertions.assertThrows(NotFoundException.class, () -> {
			trashManager.restoreFromTrash(testUserInfo, nodeChildId, adoptiveParentId);
		});
		
		// ACT member or Synapse administrator CAN do the operation though
		trashManager.restoreFromTrash(testAdminUserInfo, nodeChildId, adoptiveParentId);
	}

	@Test
	public void testSingleNodeRoundTripRestoreToRoot() throws Exception {

		List<TrashedEntity> results = trashManager.listTrashedEntities(testUserInfo, testUserInfo, 0L, 1000L);
		assertEquals(0, results.size());

		Node node = new Node();
		node.setName("TrashManagerImplAutowiredTest.testSingleNodeRoundTripRestoreToRoot()");
		node.setNodeType(EntityType.project);
		final String nodeId = nodeManager.createNewNode(node, testUserInfo);
		assertNotNull(nodeId);
		toClearList.add(nodeId);

		node = new Node();
		node.setName("TrashManagerImplAutowiredTest.testSingleNodeRoundTripRestoreToRoot() child");
		node.setNodeType(EntityType.folder);
		node.setParentId(nodeId);
		final String childId = nodeManager.createNewNode(node, testUserInfo);
		assertNotNull(childId);
		toClearList.add(childId);

		Node nodeRetrieved = nodeManager.getNode(testUserInfo, nodeId);
		Node childNodeRetrieved = nodeManager.getNode(testUserInfo, childId);
		assertNotNull(nodeRetrieved);
		final String parentId = nodeRetrieved.getParentId();
		assertNotNull(parentId);
		assertEquals(nodeId, childNodeRetrieved.getParentId());

		trashManager.moveToTrash(testUserInfo, nodeId, false);

		Assertions.assertThrows(EntityInTrashCanException.class, () -> {
			nodeManager.getNode(testUserInfo, nodeId);
		});

		results = trashManager.listTrashedEntities(testUserInfo, testUserInfo, 0L, 1000L);
		assertEquals(1, results.size());
		TrashedEntity trash = results.get(0);
		assertNotNull(trash);
		assertEquals(nodeId, trash.getEntityId());
		assertEquals(parentId, trash.getOriginalParentId());
		assertEquals(testUserInfo.getId().toString(), trash.getDeletedByPrincipalId());
		assertNotNull(trash.getDeletedOn());
		trashManager.restoreFromTrash(testUserInfo, nodeId, parentId);

		results = trashManager.listTrashedEntities(testUserInfo, testUserInfo, 0L, 1000L);
		assertEquals(0, results.size());

		nodeRetrieved = nodeManager.getNode(testUserInfo, nodeId);
		childNodeRetrieved = nodeManager.getNode(testUserInfo, childId);
		assertNotNull(nodeRetrieved);
		assertEquals(nodeId, nodeRetrieved.getId());
		assertEquals(nodeId, nodeDAO.getBenefactor(nodeRetrieved.getId()));
		assertEquals(nodeId, childNodeRetrieved.getParentId());
	}

	@Test
	public void testMultipleNodeRoundTrip() throws Exception {

		List<TrashedEntity> results = trashManager.listTrashedEntities(testUserInfo, testUserInfo, 0L, 1000L);
		assertEquals(0, results.size());

		//
		// Create the following simple topoloy:
		//
		//                  root
		//                  /  \
		//            [nodeA] [nodeB]
		//              |        |
		//           node00    node01
		//           /     \
		//        node11 [node12]
		//          |       |
		//        node21  node22
		//
		// [] indicates benefactors. In this test, we will move node00 and node01 to trash can
		//
		final Node nodeA = new Node();
		final String nodeNameA = "TrashManagerImplAutowiredTest.testMultipleNodeRoundTrip() A";
		nodeA.setName(nodeNameA);
		nodeA.setNodeType(EntityType.project);

		final Node nodeB = new Node();
		final String nodeNameB = "TrashManagerImplAutowiredTest.testMultipleNodeRoundTrip() B";
		nodeB.setName(nodeNameB);
		nodeB.setNodeType(EntityType.project);

		final Node node00 = new Node();
		final String nodeName00 = "TrashManagerImplAutowiredTest.testMultipleNodeRoundTrip() 00 or 01";
		node00.setName(nodeName00);
		node00.setNodeType(EntityType.folder);

		final Node node01 = new Node();
		final String nodeName01 = "TrashManagerImplAutowiredTest.testMultipleNodeRoundTrip() 00 or 01";
		assertEquals(nodeName00, nodeName01); // PLFM-1760
		node01.setName(nodeName01);
		node01.setNodeType(EntityType.folder);

		final Node node11 = new Node();
		final String nodeName11 = "TrashManagerImplAutowiredTest.testMultipleNodeRoundTrip() 11";
		node11.setName(nodeName11);
		node11.setNodeType(EntityType.folder);

		final Node node12 = new Node();
		final String nodeName12 = "TrashManagerImplAutowiredTest.testMultipleNodeRoundTrip() 12";
		node12.setName(nodeName12);
		node12.setNodeType(EntityType.folder);

		final Node node21 = new Node();
		final String nodeName21 = "TrashManagerImplAutowiredTest.testMultipleNodeRoundTrip() 21";
		node21.setName(nodeName21);
		node21.setNodeType(EntityType.folder);

		final Node node22 = new Node();
		final String nodeName22 = "TrashManagerImplAutowiredTest.testMultipleNodeRoundTrip() 22";
		node22.setName(nodeName22);
		node22.setNodeType(EntityType.folder);

		// Create the nodes
		final String nodeIdA = nodeManager.createNewNode(nodeA, testUserInfo);
		assertNotNull(nodeIdA);
		toClearList.add(nodeIdA);

		final String nodeIdB = nodeManager.createNewNode(nodeB, testUserInfo);
		assertNotNull(nodeIdB);
		toClearList.add(nodeIdB);

		List<String> projectA = Lists.newArrayList();
		List<String> projectB = Lists.newArrayList();

		node00.setParentId(nodeIdA);
		final String nodeId00 = nodeManager.createNewNode(node00, testUserInfo);
		assertNotNull(nodeId00);
		toClearList.add(nodeId00);
		projectA.add(nodeId00);

		node01.setParentId(nodeIdB);
		final String nodeId01 = nodeManager.createNewNode(node01, testUserInfo);
		assertNotNull(nodeId01);
		toClearList.add(nodeId01);
		projectB.add(nodeId01);

		node11.setParentId(nodeId00);
		final String nodeId11 = nodeManager.createNewNode(node11, testUserInfo);
		assertNotNull(nodeId11);
		toClearList.add(nodeId11);
		projectA.add(nodeId11);

		node12.setParentId(nodeId00);
		final String nodeId12 = nodeManager.createNewNode(node12, testUserInfo);
		assertNotNull(nodeId12);
		toClearList.add(nodeId12);
		projectA.add(nodeId12);

		node21.setParentId(nodeId11);
		final String nodeId21 = nodeManager.createNewNode(node21, testUserInfo);
		assertNotNull(nodeId21);
		toClearList.add(nodeId21);
		projectA.add(nodeId21);

		node22.setParentId(nodeId12);
		final String nodeId22 = nodeManager.createNewNode(node22, testUserInfo);
		assertNotNull(nodeId22);
		toClearList.add(nodeId22);
		projectA.add(nodeId22);

		// Modify nodeId12 to be its own benefactor
		AccessControlList acl = AccessControlListUtil.createACLToGrantEntityAdminAccess(nodeId12, testUserInfo, new Date());
		entityAclManager.overrideInheritance(acl, testUserInfo);
		assertEquals(nodeId12, nodeDAO.getBenefactor(nodeId12));
		assertEquals(nodeId12, nodeDAO.getBenefactor(nodeId22));

		// Make sure we can read the nodes before moving the trash can
		Node nodeBack00 = nodeManager.getNode(testUserInfo, nodeId00);
		assertNotNull(nodeBack00);
		final String parentId00 = nodeBack00.getParentId();
		assertEquals(nodeIdA, parentId00);

		Node nodeBack01 = nodeManager.getNode(testUserInfo, nodeId01);
		assertNotNull(nodeBack01);
		final String parentId01 = nodeBack01.getParentId();
		assertEquals(nodeIdB, parentId01);

		Node nodeBack11 = nodeManager.getNode(testUserInfo, nodeId11);
		assertNotNull(nodeBack11);
		final String parentId11 = nodeBack11.getParentId();
		assertEquals(nodeId00, parentId11);

		Node nodeBack12 = nodeManager.getNode(testUserInfo, nodeId12);
		assertNotNull(nodeBack12);
		final String parentId12 = nodeBack12.getParentId();
		assertEquals(nodeId00, parentId12);

		Node nodeBack21 = nodeManager.getNode(testUserInfo, nodeId21);
		assertNotNull(nodeBack21);
		final String parentId21 = nodeBack21.getParentId();
		assertEquals(nodeId11, parentId21);

		Node nodeBack22 = nodeManager.getNode(testUserInfo, nodeId22);
		assertNotNull(nodeBack22);
		final String parentId22 = nodeBack22.getParentId();
		assertEquals(nodeId12, parentId22);

		trashManager.moveToTrash(testUserInfo, nodeId00, false);
		// node01 has the same name as node00 (PLFM-1760)
		trashManager.moveToTrash(testUserInfo, nodeId01, false);
		
		// Validate all ACLs were removed from the hierarchy.
		// both node 12 and 22 should no longer have an ACL, with the trash as the benefactor
		assertEquals(TrashConstants.TRASH_FOLDER_ID_STRING, nodeDAO.getBenefactor(nodeId12));
		assertEquals(TrashConstants.TRASH_FOLDER_ID_STRING, nodeDAO.getBenefactor(nodeId22));

		// After moved to trash, the nodes are not accessible any more
		Assertions.assertThrows(EntityInTrashCanException.class, () -> {
			nodeManager.getNode(testUserInfo, nodeId00);
		});

		Assertions.assertThrows(EntityInTrashCanException.class, () -> {
			nodeManager.getNode(testUserInfo, nodeId01);
		});

		Assertions.assertThrows(EntityInTrashCanException.class, () -> {
			nodeManager.getNode(testUserInfo, nodeId11);
		});

		Assertions.assertThrows(EntityInTrashCanException.class, () -> {
			nodeManager.getNode(testUserInfo, nodeId12);
		});

		Assertions.assertThrows(EntityInTrashCanException.class, () -> {
			nodeManager.getNode(testUserInfo, nodeId21);
		});

		Assertions.assertThrows(EntityInTrashCanException.class, () -> {
			nodeManager.getNode(testUserInfo, nodeId22);
		});

		// But we can see them in the trash can
		results = trashManager.listTrashedEntities(testUserInfo, testUserInfo, 0L, 1000L);
		assertEquals(2, results.size());

		// Restore node00 and its descendants
		trashManager.restoreFromTrash(testUserInfo, nodeId00, parentId00);

		results = trashManager.listTrashedEntities(testUserInfo, testUserInfo, 0L, 1000L);
		assertEquals(1, results.size());

		nodeBack00 = nodeManager.getNode(testUserInfo, nodeId00);
		assertNotNull(nodeBack00);
		assertEquals(nodeId00, nodeBack00.getId());
		assertEquals(nodeName00, nodeBack00.getName());
		assertEquals(parentId00, nodeBack00.getParentId());

		nodeBack11 = nodeManager.getNode(testUserInfo, nodeId11);
		assertNotNull(nodeBack11);
		assertEquals(nodeId11, nodeBack11.getId());
		assertEquals(nodeName11, nodeBack11.getName());
		assertEquals(parentId11, nodeBack11.getParentId());

		nodeBack12 = nodeManager.getNode(testUserInfo, nodeId12);
		assertNotNull(nodeBack12);
		assertEquals(nodeId12, nodeBack12.getId());
		assertEquals(nodeName12, nodeBack12.getName());
		assertEquals(parentId12, nodeBack12.getParentId());

		nodeBack21 = nodeManager.getNode(testUserInfo, nodeId21);
		assertNotNull(nodeBack21);
		assertEquals(nodeId21, nodeBack21.getId());
		assertEquals(nodeName21, nodeBack21.getName());
		assertEquals(parentId21, nodeBack21.getParentId());

		nodeBack22 = nodeManager.getNode(testUserInfo, nodeId22);
		assertNotNull(nodeBack22);
		assertEquals(nodeId22, nodeBack22.getId());
		assertEquals(nodeName22, nodeBack22.getName());
		assertEquals(parentId22, nodeBack22.getParentId());

		// Restore node01
		trashManager.restoreFromTrash(testUserInfo, nodeId01, null);

		results = trashManager.listTrashedEntities(testUserInfo, testUserInfo, 0L, 1000L);
		assertEquals(0, results.size());

		nodeBack01 = nodeManager.getNode(testUserInfo, nodeId01);
		assertNotNull(nodeBack01);
		assertEquals(nodeId01, nodeBack01.getId());
		assertEquals(nodeName01, nodeBack01.getName());
		assertEquals(parentId01, nodeBack01.getParentId());
	}

	@Test
	public void testPurgeNodeForUser() throws Exception {

		List<TrashedEntity> results = trashManager.listTrashedEntities(testUserInfo, testUserInfo, 0L, 1000L);
		assertEquals(0, results.size());

		//
		//           root
		//           /  \
		//          A1  A2
		//          |    |
		//          B1  B2
		//          |
		//          C1
		//
		final Node nodeA1 = new Node();
		final String nodeNameA1 = "TrashManagerImplAutowiredTest.testPurge() A1";
		nodeA1.setName(nodeNameA1);
		nodeA1.setNodeType(EntityType.project);
		final String nodeIdA1 = nodeManager.createNewNode(nodeA1, testUserInfo);
		assertNotNull(nodeIdA1);
		toClearList.add(nodeIdA1);

		final Node nodeA2 = new Node();
		final String nodeNameA2 = "TrashManagerImplAutowiredTest.testPurge() A2";
		nodeA2.setName(nodeNameA2);
		nodeA2.setNodeType(EntityType.project);
		final String nodeIdA2 = nodeManager.createNewNode(nodeA2, testUserInfo);
		assertNotNull(nodeIdA2);
		toClearList.add(nodeIdA2);

		final Node nodeB1 = new Node();
		final String nodeNameB1 = "TrashManagerImplAutowiredTest.testPurge() B1";
		nodeB1.setName(nodeNameB1);
		nodeB1.setNodeType(EntityType.folder);
		nodeB1.setParentId(nodeIdA1);
		final String nodeIdB1 = nodeManager.createNewNode(nodeB1, testUserInfo);
		assertNotNull(nodeIdB1);
		toClearList.add(nodeIdB1);
		
		final Node nodeB2 = new Node();
		final String nodeNameB2 = "TrashManagerImplAutowiredTest.testPurge() B2";
		nodeB2.setName(nodeNameB2);
		nodeB2.setNodeType(EntityType.folder);
		nodeB2.setParentId(nodeIdA2);
		final String nodeIdB2 = nodeManager.createNewNode(nodeB2, testUserInfo);
		assertNotNull(nodeIdB2);
		toClearList.add(nodeIdB2);

		final Node nodeC1 = new Node();
		final String nodeNameC1 = "TrashManagerImplAutowiredTest.testPurge() C1";
		nodeC1.setName(nodeNameC1);
		nodeC1.setNodeType(EntityType.folder);
		nodeC1.setParentId(nodeIdB1);
		final String nodeIdC1 = nodeManager.createNewNode(nodeC1, testUserInfo);
		assertNotNull(nodeIdC1);
		toClearList.add(nodeIdC1);
		
		// Move all of them to trash can
		trashManager.moveToTrash(testUserInfo, nodeIdA1, false);
		trashManager.moveToTrash(testUserInfo, nodeIdA2, false);

		// Purge A2
		trashManager.flagForPurge(testUserInfo, nodeIdA2);
		
		results = trashManager.listTrashedEntities(testUserInfo, testUserInfo, 0L, 1000L);
		assertEquals(1, results.size());
		for (TrashedEntity trash : results) {
			assertNotEquals(nodeIdB2, trash.getEntityId());
		}

		// Purge A1 (a root with 2 descendants)
		trashManager.flagForPurge(testUserInfo, nodeIdA1);

		results = trashManager.listTrashedEntities(testUserInfo, testUserInfo, 0L, 1000L);
		assertEquals(0, results.size());
	}

	@Test
	public void testRestoreToParentThatIsInTrashCan() throws Exception {
		// A --> Parent
		final Node nodeA = new Node();
		final String nodeNameA = "TrashManagerImplAutowiredTest.testRestoreToParentThatIsInTrashCan() A";
		nodeA.setName(nodeNameA);
		nodeA.setNodeType(EntityType.project);
		final String nodeIdA = nodeManager.createNewNode(nodeA, testUserInfo);
		assertNotNull(nodeIdA);
		toClearList.add(nodeIdA);
		
		// B --> Child
		final Node nodeB = new Node();
		final String nodeNameB = "TrashManagerImplAutowiredTest.testRestoreToParentThatIsInTrashCan() B";
		nodeB.setName(nodeNameB);
		nodeB.setNodeType(EntityType.folder);
		nodeB.setParentId(nodeIdA);
		final String nodeIdB = nodeManager.createNewNode(nodeB, testUserInfo);
		assertNotNull(nodeIdB);
		toClearList.add(nodeIdB);
		
		// Move both to trash.
		trashManager.moveToTrash(testUserInfo, nodeIdB, false);
		trashManager.moveToTrash(testUserInfo, nodeIdA, false);
		
		// Restore B from trash.
		Assertions.assertThrows(ParentInTrashCanException.class, () -> {
			trashManager.restoreFromTrash(testUserInfo, nodeIdB, nodeIdA);
		});
	}

	@Test
	public void testCanDownload() throws Exception {
		final Node node = new Node();
		final String nodeName = "TrashManagerImplAutowiredTest.testCanDownload()";
		node.setName(nodeName);
		node.setNodeType(EntityType.project);
		final String nodeId = nodeManager.createNewNode(node, testAdminUserInfo);
		assertNotNull(nodeId);
		toClearList.add(nodeId);
		trashManager.moveToTrash(testAdminUserInfo, nodeId, false);
		
		Assertions.assertThrows(EntityInTrashCanException.class, () -> {
			entityAuthorizationManager.hasAccess(testAdminUserInfo, nodeId, ACCESS_TYPE.DOWNLOAD).checkAuthorizationOrElseThrow();;
		});
	}
	
	@Test
	public void testMoveToTrashWithPriorityPurge() throws Exception {
		Node nodeParent = createNode("TrashManagerImplAutowiredTest.testSingleNodeRoundTrip() Parent",EntityType.project, null);
		final String nodeParentId = nodeParent.getId();
		String nodeChildName = "TrashManagerImplAutowiredTest.testSingleNodeRoundTrip() Child";
		Node nodeChild = createNode(nodeChildName, EntityType.folder, nodeParentId);
		final String nodeChildId = nodeChild.getId();

		boolean priorityPurge = true;
				
		// Call under test
		trashManager.moveToTrash(testUserInfo, nodeChildId, priorityPurge);
		
		// Should not be included in the user trash list
		inspectUsersTrashCan(testUserInfo, 0);
	}
	
	@Test
	public void testFlagForPriorityPurge() throws Exception {
		Node nodeParent = createNode("TrashManagerImplAutowiredTest.testSingleNodeRoundTrip() Parent",EntityType.project, null);
		final String nodeParentId = nodeParent.getId();
		String nodeChildName = "TrashManagerImplAutowiredTest.testSingleNodeRoundTrip() Child";
		Node nodeChild = createNode(nodeChildName, EntityType.folder, nodeParentId);
		final String nodeChildId = nodeChild.getId();

		boolean priorityPurge = false;
				
		trashManager.moveToTrash(testUserInfo, nodeChildId, priorityPurge);
		

		// Should now be in the user trash can
		List<TrashedEntity> trash = inspectUsersTrashCan(testUserInfo, 1);
		
		assertEquals(nodeChildId, trash.get(0).getEntityId());
		
		// Call under test
		trashManager.flagForPurge(testUserInfo, nodeChildId);
		
		// Should not be included in the user trash list
		inspectUsersTrashCan(testUserInfo, 0);
	}
	
	@Test
	public void testPurgeTrashDeepHierarchy_PLFM_5932() {
		final Node root = new Node();
		final String rootName = "TrashManagerImplAutowiredTest.testPurgeTrashAdminDeepHierarchy() Root Node";
		root.setName(rootName);
		root.setNodeType(EntityType.project);
		String rootId = nodeManager.createNewNode(root, testAdminUserInfo);
		toClearList.add(rootId);
		
		String parentId = rootId;
		int depth = 20;
		
		for (int i=0; i<depth; i++) {
			Node childNode = new Node();
			String nodeNameB = "TrashManagerImplAutowiredTest.testPurgeTrashAdminDeepHierarchy() Child " + i;
			childNode.setName(nodeNameB);
			childNode.setNodeType(EntityType.folder);
			childNode.setParentId(parentId);
			parentId = nodeManager.createNewNode(childNode, testAdminUserInfo);
			toClearList.add(parentId);
		}
		
		trashManager.moveToTrash(testAdminUserInfo, rootId, false);
		
		trashManager.purgeTrash(testAdminUserInfo, Collections.singletonList(KeyFactory.stringToKey(rootId)));
	}

	private void cleanUp() throws Exception {
		if (accessRequirementToDelete!=null) {
			accessRequirementManager.deleteAccessRequirement(testAdminUserInfo, accessRequirementToDelete.getId().toString());
		}
		for (String nodeId : toClearList) {
			try {
				nodeManager.delete(testAdminUserInfo, nodeId);
			} catch (NotFoundException e) {}
		}
		
		trashCanDao.truncate();
		
		List<String> children = nodeDAO.getChildrenIdsAsList(trashCanId);
		
		for (String child : children) {
			try {
				nodeManager.delete(testAdminUserInfo, child);
			} catch (NotFoundException e) {}
		}
	}
}
