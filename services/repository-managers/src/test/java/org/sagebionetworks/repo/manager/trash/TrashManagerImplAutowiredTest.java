package org.sagebionetworks.repo.manager.trash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.NodeInheritanceManager;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.PermissionsManager;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.DBOTrashCanDao;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;
import org.sagebionetworks.repo.web.util.UserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class TrashManagerImplAutowiredTest {

	@Autowired private TrashManager trashManager;
	@Autowired private NodeManager nodeManager;
	@Autowired private NodeInheritanceManager nodeInheritanceManager;
	@Autowired private PermissionsManager permissionsManager;
	@Autowired private DBOTrashCanDao trashCanDao;
	@Autowired private NodeDAO nodeDAO;
	@Autowired private UserProvider userProvider;
	private UserInfo testUserInfo;
	private String trashCanId;
	private List<String> toClearList;

	@Before
	public void before() throws Exception {

		assertNotNull(trashManager);
		assertNotNull(nodeManager);
		assertNotNull(nodeInheritanceManager);
		assertNotNull(permissionsManager);
		assertNotNull(trashCanDao);
		assertNotNull(nodeDAO);
		assertNotNull(userProvider);

		testUserInfo = userProvider.getTestUserInfo();
		assertNotNull(testUserInfo);
		assertFalse(testUserInfo.isAdmin());

		trashCanId = nodeDAO.getNodeIdForPath(TrashConstants.TRASH_FOLDER_PATH);
		assertNotNull(trashCanId);
		Node trashFolder = nodeDAO.getNode(trashCanId);
		assertNotNull(trashFolder);
		assertNotNull(trashFolder.getParentId());
		assertTrue(nodeDAO.isNodeRoot(trashFolder.getParentId()));
		String benefactorId = nodeInheritanceManager.getBenefactor(trashCanId);
		assertNotNull(benefactorId);
		assertEquals(trashCanId, benefactorId);

		toClearList = new ArrayList<String>();
		cleanUp(); // Clean up leftovers from other test cases
	}

	@After
	public void after() throws Exception {
		cleanUp();
	}

	@Test
	public void testSingleNodeRoundTrip() throws Exception {

		QueryResults<TrashedEntity> results = trashManager.viewTrash(testUserInfo, 0L, 1000L);
		assertEquals(0L, results.getTotalNumberOfResults());
		assertEquals(0, results.getResults().size());

		Node nodeParent = new Node();
		final String nodeParentName = "TrashManagerImplAutowiredTest.testSingleNodeRoundTrip() Parent";
		nodeParent.setName(nodeParentName);
		nodeParent.setNodeType(EntityType.project.name());
		final String nodeParentId = nodeManager.createNewNode(nodeParent, testUserInfo);
		assertNotNull(nodeParentId);
		toClearList.add(nodeParentId);
		Node nodeParentRetrieved = nodeManager.get(testUserInfo, nodeParentId);
		assertNotNull(nodeParentRetrieved);

		Node nodeChild = new Node();
		final String nodeChildName = "TrashManagerImplAutowiredTest.testSingleNodeRoundTrip() Child";
		nodeChild.setName(nodeChildName);
		nodeChild.setNodeType(EntityType.dataset.name());
		nodeChild.setParentId(nodeParentId);
		final String nodeChildId = nodeManager.createNewNode(nodeChild, testUserInfo);
		assertNotNull(nodeChildId);
		toClearList.add(nodeChildId);
		Node nodeChildRetrieved = nodeManager.get(testUserInfo, nodeChildId);
		assertNotNull(nodeChildRetrieved);
		assertNotNull(nodeChildRetrieved.getParentId());
		assertEquals(nodeParentId, nodeChildRetrieved.getParentId());

		trashManager.moveToTrash(testUserInfo, nodeChildId);

		try {
			nodeChildRetrieved = nodeManager.get(testUserInfo, nodeChildId);
			fail();
		} catch (UnauthorizedException e) {
			// TODO: PLFM-1725 We should throw NotFoundException for items in trash can.
			assertTrue(true);
		}

		results = trashManager.viewTrash(testUserInfo, 0L, 1000L);
		assertEquals(1L, results.getTotalNumberOfResults());
		assertEquals(1, results.getResults().size());
		TrashedEntity trash = results.getResults().get(0);
		assertNotNull(trash);
		assertEquals(nodeChildId, trash.getEntityId());
		assertEquals(nodeChildName, trash.getEntityName());
		assertEquals(nodeParentId, trash.getOriginalParentId());
		assertEquals(testUserInfo.getIndividualGroup().getId(), trash.getDeletedByPrincipalId());
		assertNotNull(trash.getDeletedOn());

		trashManager.restoreFromTrash(testUserInfo, nodeChildId, nodeParentId);

		results = trashManager.viewTrash(testUserInfo, 0L, 1000L);
		assertEquals(0L, results.getTotalNumberOfResults());
		assertEquals(0, results.getResults().size());

		nodeChildRetrieved = nodeManager.get(testUserInfo, nodeChildId);
		assertNotNull(nodeChildRetrieved);
		assertEquals(nodeChildId, nodeChildRetrieved.getId());
		assertEquals(nodeChildName, nodeChildRetrieved.getName());
		assertEquals(nodeParentId, nodeChildRetrieved.getParentId());
		assertEquals(nodeParentId, nodeInheritanceManager.getBenefactor(nodeChildRetrieved.getId()));
	}

	@Test
	public void testSingleNodeRoundTripRestoreToRoot() throws Exception {

		QueryResults<TrashedEntity> results = trashManager.viewTrash(testUserInfo, 0L, 1000L);
		assertEquals(0L, results.getTotalNumberOfResults());
		assertEquals(0, results.getResults().size());

		Node node = new Node();
		node.setName("TrashManagerImplAutowiredTest.testSingleNodeRoundTripRestoreToRoot()");
		node.setNodeType(EntityType.project.name());
		final String nodeId = nodeManager.createNewNode(node, testUserInfo);
		assertNotNull(nodeId);
		toClearList.add(nodeId);
		Node nodeRetrieved = nodeManager.get(testUserInfo, nodeId);
		assertNotNull(nodeRetrieved);
		final String parentId = nodeRetrieved.getParentId();
		assertNotNull(parentId);

		trashManager.moveToTrash(testUserInfo, nodeId);

		try {
			nodeRetrieved = nodeManager.get(testUserInfo, nodeId);
			fail();
		} catch (UnauthorizedException e) {
			// TODO: PLFM-1725 We should throw NotFoundException for items in trash can.
			assertTrue(true);
		}

		results = trashManager.viewTrash(testUserInfo, 0L, 1000L);
		assertEquals(1L, results.getTotalNumberOfResults());
		assertEquals(1, results.getResults().size());
		TrashedEntity trash = results.getResults().get(0);
		assertNotNull(trash);
		assertEquals(nodeId, trash.getEntityId());
		assertEquals(parentId, trash.getOriginalParentId());
		assertEquals(testUserInfo.getIndividualGroup().getId(), trash.getDeletedByPrincipalId());
		assertNotNull(trash.getDeletedOn());

		trashManager.restoreFromTrash(testUserInfo, nodeId, parentId);

		results = trashManager.viewTrash(testUserInfo, 0L, 1000L);
		assertEquals(0L, results.getTotalNumberOfResults());
		assertEquals(0, results.getResults().size());

		nodeRetrieved = nodeManager.get(testUserInfo, nodeId);
		assertNotNull(nodeRetrieved);
		assertEquals(nodeId, nodeRetrieved.getId());
		assertEquals(nodeId, nodeInheritanceManager.getBenefactor(nodeRetrieved.getId()));
	}

	@Test
	public void testMultipleNodeRoundTrip() throws Exception {

		QueryResults<TrashedEntity> results = trashManager.viewTrash(testUserInfo, 0L, 1000L);
		assertEquals(0L, results.getTotalNumberOfResults());
		assertEquals(0, results.getResults().size());

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
		final String nodeNameA = "TrashManagerImplAutowiredTesttestMultipleNodeRoundTrip() A";
		nodeA.setName(nodeNameA);
		nodeA.setNodeType(EntityType.project.name());

		final Node nodeB = new Node();
		final String nodeNameB = "TrashManagerImplAutowiredTesttestMultipleNodeRoundTrip() B";
		nodeB.setName(nodeNameB);
		nodeB.setNodeType(EntityType.project.name());

		final Node node00 = new Node();
		final String nodeName00 = "TrashManagerImplAutowiredTesttestMultipleNodeRoundTrip() 00 or 01";
		node00.setName(nodeName00);
		node00.setNodeType(EntityType.folder.name());

		final Node node01 = new Node();
		final String nodeName01 = "TrashManagerImplAutowiredTesttestMultipleNodeRoundTrip() 00 or 01";
		assertEquals(nodeName00, nodeName01); // PLFM-1760
		node01.setName(nodeName01);
		node01.setNodeType(EntityType.folder.name());

		final Node node11 = new Node();
		final String nodeName11 = "TrashManagerImplAutowiredTesttestMultipleNodeRoundTrip() 11";
		node11.setName(nodeName11);
		node11.setNodeType(EntityType.folder.name());

		final Node node12 = new Node();
		final String nodeName12 = "TrashManagerImplAutowiredTesttestMultipleNodeRoundTrip() 12";
		node12.setName(nodeName12);
		node12.setNodeType(EntityType.folder.name());

		final Node node21 = new Node();
		final String nodeName21 = "TrashManagerImplAutowiredTesttestMultipleNodeRoundTrip() 21";
		node21.setName(nodeName21);
		node21.setNodeType(EntityType.dataset.name());

		final Node node22 = new Node();
		final String nodeName22 = "TrashManagerImplAutowiredTesttestMultipleNodeRoundTrip() 22";
		node22.setName(nodeName22);
		node22.setNodeType(EntityType.dataset.name());

		// Create the nodes
		final String nodeIdA = nodeManager.createNewNode(nodeA, testUserInfo);
		assertNotNull(nodeIdA);
		toClearList.add(nodeIdA);

		final String nodeIdB = nodeManager.createNewNode(nodeB, testUserInfo);
		assertNotNull(nodeIdB);
		toClearList.add(nodeIdB);

		node00.setParentId(nodeIdA);
		final String nodeId00 = nodeManager.createNewNode(node00, testUserInfo);
		assertNotNull(nodeId00);
		toClearList.add(nodeId00);

		node01.setParentId(nodeIdB);
		final String nodeId01 = nodeManager.createNewNode(node01, testUserInfo);
		assertNotNull(nodeId01);
		toClearList.add(nodeId01);

		node11.setParentId(nodeId00);
		final String nodeId11 = nodeManager.createNewNode(node11, testUserInfo);
		assertNotNull(nodeId11);
		toClearList.add(nodeId11);

		node12.setParentId(nodeId00);
		final String nodeId12 = nodeManager.createNewNode(node12, testUserInfo);
		assertNotNull(nodeId12);
		toClearList.add(nodeId12);

		node21.setParentId(nodeId11);
		final String nodeId21 = nodeManager.createNewNode(node21, testUserInfo);
		assertNotNull(nodeId21);
		toClearList.add(nodeId21);

		node22.setParentId(nodeId12);
		final String nodeId22 = nodeManager.createNewNode(node22, testUserInfo);
		assertNotNull(nodeId22);
		toClearList.add(nodeId22);

		// Modify nodeId12 to be its own benefactor
		AccessControlList acl = AccessControlListUtil.createACLToGrantAll(nodeId12, testUserInfo);
		permissionsManager.overrideInheritance(acl, testUserInfo);
		assertEquals(nodeId12, nodeInheritanceManager.getBenefactor(nodeId12));
		assertEquals(nodeId12, nodeInheritanceManager.getBenefactor(nodeId22));

		// Make sure we can read the nodes before moving the trash can
		Node nodeBack00 = nodeManager.get(testUserInfo, nodeId00);
		assertNotNull(nodeBack00);
		final String parentId00 = nodeBack00.getParentId();
		assertEquals(nodeIdA, parentId00);

		Node nodeBack01 = nodeManager.get(testUserInfo, nodeId01);
		assertNotNull(nodeBack01);
		final String parentId01 = nodeBack01.getParentId();
		assertEquals(nodeIdB, parentId01);

		Node nodeBack11 = nodeManager.get(testUserInfo, nodeId11);
		assertNotNull(nodeBack11);
		final String parentId11 = nodeBack11.getParentId();
		assertEquals(nodeId00, parentId11);

		Node nodeBack12 = nodeManager.get(testUserInfo, nodeId12);
		assertNotNull(nodeBack12);
		final String parentId12 = nodeBack12.getParentId();
		assertEquals(nodeId00, parentId12);

		Node nodeBack21 = nodeManager.get(testUserInfo, nodeId21);
		assertNotNull(nodeBack21);
		final String parentId21 = nodeBack21.getParentId();
		assertEquals(nodeId11, parentId21);

		Node nodeBack22 = nodeManager.get(testUserInfo, nodeId22);
		assertNotNull(nodeBack22);
		final String parentId22 = nodeBack22.getParentId();
		assertEquals(nodeId12, parentId22);

		trashManager.moveToTrash(testUserInfo, nodeId00);
		// node01 has the same name as node00 (PLFM-1760)
		trashManager.moveToTrash(testUserInfo, nodeId01);

		// After moved to trash, the nodes are not accessible any more
		try {
			nodeBack00 = nodeManager.get(testUserInfo, nodeId00);
			fail();
		} catch (UnauthorizedException e) {
			// TODO: We should throw NotFoundException for items in trash can.
			assertTrue(true);
		}

		try {
			nodeBack01 = nodeManager.get(testUserInfo, nodeId01);
			fail();
		} catch (UnauthorizedException e) {
			// TODO: We should throw NotFoundException for items in trash can.
			assertTrue(true);
		}

		try {
			nodeBack11 = nodeManager.get(testUserInfo, nodeId11);
			fail();
		} catch (UnauthorizedException e) {
			// TODO: We should throw NotFoundException for items in trash can.
			assertTrue(true);
		}

		try {
			nodeBack12 = nodeManager.get(testUserInfo, nodeId12);
			fail();
		} catch (UnauthorizedException e) {
			// TODO: We should throw NotFoundException for items in trash can.
			assertTrue(true);
		}

		try {
			nodeBack21 = nodeManager.get(testUserInfo, nodeId21);
			fail();
		} catch (UnauthorizedException e) {
			// TODO: We should throw NotFoundException for items in trash can.
			assertTrue(true);
		}

		try {
			nodeBack22 = nodeManager.get(testUserInfo, nodeId22);
			fail();
		} catch (UnauthorizedException e) {
			// TODO: We should throw NotFoundException for items in trash can.
			assertTrue(true);
		}

		// But we can see them in the trash can
		results = trashManager.viewTrash(testUserInfo, 0L, 1000L);
		assertEquals(6L, results.getTotalNumberOfResults());
		assertEquals(6, results.getResults().size());

		// Restore node00 and its descendants
		trashManager.restoreFromTrash(testUserInfo, nodeId00, parentId00);

		results = trashManager.viewTrash(testUserInfo, 0L, 1000L);
		assertEquals(1L, results.getTotalNumberOfResults());
		assertEquals(1, results.getResults().size());

		nodeBack00 = nodeManager.get(testUserInfo, nodeId00);
		assertNotNull(nodeBack00);
		assertEquals(nodeId00, nodeBack00.getId());
		assertEquals(nodeName00, nodeBack00.getName());
		assertEquals(parentId00, nodeBack00.getParentId());

		nodeBack11 = nodeManager.get(testUserInfo, nodeId11);
		assertNotNull(nodeBack11);
		assertEquals(nodeId11, nodeBack11.getId());
		assertEquals(nodeName11, nodeBack11.getName());
		assertEquals(parentId11, nodeBack11.getParentId());

		nodeBack12 = nodeManager.get(testUserInfo, nodeId12);
		assertNotNull(nodeBack12);
		assertEquals(nodeId12, nodeBack12.getId());
		assertEquals(nodeName12, nodeBack12.getName());
		assertEquals(parentId12, nodeBack12.getParentId());

		nodeBack21 = nodeManager.get(testUserInfo, nodeId21);
		assertNotNull(nodeBack21);
		assertEquals(nodeId21, nodeBack21.getId());
		assertEquals(nodeName21, nodeBack21.getName());
		assertEquals(parentId21, nodeBack21.getParentId());

		nodeBack22 = nodeManager.get(testUserInfo, nodeId22);
		assertNotNull(nodeBack22);
		assertEquals(nodeId22, nodeBack22.getId());
		assertEquals(nodeName22, nodeBack22.getName());
		assertEquals(parentId22, nodeBack22.getParentId());

		// Restore node01
		trashManager.restoreFromTrash(testUserInfo, nodeId01, null);

		results = trashManager.viewTrash(testUserInfo, 0L, 1000L);
		assertEquals(0L, results.getTotalNumberOfResults());
		assertEquals(0, results.getResults().size());

		nodeBack01 = nodeManager.get(testUserInfo, nodeId01);
		assertNotNull(nodeBack01);
		assertEquals(nodeId01, nodeBack01.getId());
		assertEquals(nodeName01, nodeBack01.getName());
		assertEquals(parentId01, nodeBack01.getParentId());
	}

	private void cleanUp() throws Exception {
		for (String nodeId : toClearList) {
			nodeManager.delete(userProvider.getTestAdminUserInfo(), nodeId);
		}
		String userGroupId = testUserInfo.getIndividualGroup().getId();
		List<TrashedEntity> trashList = trashCanDao.getInRangeForUser(userGroupId, 0L, Long.MAX_VALUE);
		for (TrashedEntity trash : trashList) {
			trashCanDao.delete(userGroupId, trash.getEntityId());
		}
		List<String> children = nodeDAO.getChildrenIdsAsList(trashCanId);
		for (String child : children) {
			nodeManager.delete(userProvider.getTestAdminUserInfo(), child);
		}
	}
}
