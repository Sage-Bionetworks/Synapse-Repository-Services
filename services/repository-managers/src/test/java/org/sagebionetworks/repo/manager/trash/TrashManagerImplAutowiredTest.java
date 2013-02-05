package org.sagebionetworks.repo.manager.trash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.DBOTrashCanDao;
import org.sagebionetworks.repo.web.util.UserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class TrashManagerImplAutowiredTest {

	@Autowired private TrashManager trashManager;
	@Autowired private NodeManager nodeManager;
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
		assertNotNull(trashCanDao);
		assertNotNull(nodeDAO);
		assertNotNull(userProvider);

		testUserInfo = userProvider.getTestUserInfo();
		assertNotNull(testUserInfo);
		assertFalse(testUserInfo.isAdmin());

		trashCanId = nodeDAO.getNodeIdForPath(TrashConstants.TRASH_FOLDER_PATH);
		assertNotNull(trashCanId);
		System.out.println("TrashManagerImplAutowiredTest.before() -- The trash can folder is " + trashCanId);
		Node trashFolder = nodeDAO.getNode(trashCanId);
		assertNotNull(trashFolder);
		//assertNotNull(trashFolder.getBenefactorId());
		//System.out.println("TrashManagerImplAutowiredTest.before() -- Trash can folder's benefactor is " + trashFolder.getBenefactorId());
		assertNotNull(trashFolder.getParentId());
		assertTrue(nodeDAO.isNodeRoot(trashFolder.getParentId()));
		System.out.println("TrashManagerImplAutowiredTest.before() -- The root is " + trashFolder.getParentId());

		// Clear the trash can table for the test user
		String userGroupId = testUserInfo.getIndividualGroup().getId();
		int count = trashCanDao.getCount(userGroupId);
		List<TrashedEntity> trashList = trashCanDao.getInRangeForUser(userGroupId, 0, count);
		for (TrashedEntity trash : trashList) {
			trashCanDao.delete(userGroupId, trash.getEntityId());
		}

		toClearList = new ArrayList<String>();
	}

	@After
	public void after() throws Exception {
		if (nodeManager != null && toClearList != null && userProvider != null) {
			for (String nodeId : toClearList) {
				nodeManager.delete(userProvider.getTestAdminUserInfo(), nodeId);
			}
		}
	}

	@Test
	public void testSingleNodeRoundTrip() throws Exception {

		QueryResults<TrashedEntity> results = trashManager.viewTrash(testUserInfo, 0, 1000);
		assertEquals(0L, results.getTotalNumberOfResults());
		assertEquals(0, results.getResults().size());

		Node nodeParent = new Node();
		nodeParent.setName("TrashManagerImplAutowiredTest.testSingleNodeRoundTrip() Parent");
		nodeParent.setNodeType(EntityType.project.name());
		final String nodeParentId = nodeManager.createNewNode(nodeParent, testUserInfo);
		assertNotNull(nodeParentId);
		toClearList.add(nodeParentId);
		Node nodeParentRetrieved = nodeManager.get(testUserInfo, nodeParentId);
		assertNotNull(nodeParentRetrieved);

		Node nodeChild = new Node();
		nodeChild.setName("TrashManagerImplAutowiredTest.testSingleNodeRoundTrip() Child");
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

		results = trashManager.viewTrash(testUserInfo, 0, 1000);
		assertEquals(1L, results.getTotalNumberOfResults());
		assertEquals(1, results.getResults().size());
		TrashedEntity trash = results.getResults().get(0);
		assertNotNull(trash);
		assertEquals(nodeChildId, trash.getEntityId());
		assertEquals(nodeParentId, trash.getOriginalParentId());
		assertEquals(testUserInfo.getIndividualGroup().getId(), trash.getDeletedByPrincipalId());
		assertNotNull(trash.getDeletedOn());

		trashManager.restoreFromTrash(testUserInfo, nodeChildId, nodeParentId);

		results = trashManager.viewTrash(testUserInfo, 0, 1000);
		assertEquals(0L, results.getTotalNumberOfResults());
		assertEquals(0, results.getResults().size());

		nodeChildRetrieved = nodeManager.get(testUserInfo, nodeChildId);
		assertNotNull(nodeChildRetrieved);
		assertEquals(nodeChildId, nodeChildRetrieved.getId());
		assertEquals(nodeParentId, nodeChildRetrieved.getParentId());
	}

	@Test
	public void testSingleNodeRoundTripRestoreToRoot() throws Exception {

		QueryResults<TrashedEntity> results = trashManager.viewTrash(testUserInfo, 0, 1000);
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

		results = trashManager.viewTrash(testUserInfo, 0, 1000);
		assertEquals(1L, results.getTotalNumberOfResults());
		assertEquals(1, results.getResults().size());
		TrashedEntity trash = results.getResults().get(0);
		assertNotNull(trash);
		assertEquals(nodeId, trash.getEntityId());
		assertEquals(parentId, trash.getOriginalParentId());
		assertEquals(testUserInfo.getIndividualGroup().getId(), trash.getDeletedByPrincipalId());
		assertNotNull(trash.getDeletedOn());

		trashManager.restoreFromTrash(testUserInfo, nodeId, parentId);

		results = trashManager.viewTrash(testUserInfo, 0, 1000);
		assertEquals(0L, results.getTotalNumberOfResults());
		assertEquals(0, results.getResults().size());

		try {
			// This is a rather strange scenario
			// Where the node is restored to the root (or any node that the
			// user has the CREATE privilege, the node's benefactor is set
			// the root. The user does not have READ access on the node however
			nodeRetrieved = nodeManager.get(testUserInfo, nodeId);
			fail();
		} catch (UnauthorizedException e) {
			assertTrue(true);
		}
	}

	@Test
	public void testMultipleNodeRoundTrip() throws Exception {

		QueryResults<TrashedEntity> results = trashManager.viewTrash(testUserInfo, 0, 1000);
		assertEquals(0L, results.getTotalNumberOfResults());
		assertEquals(0, results.getResults().size());

		//
		// Create the following simple topoloy:
		//
		//            root
		//             |
		//            node
		//             |
		//           node00
		//           /    \
		//        node11 node12
		//          |
		//        node21
		//
		// In this test, we will move node00 to trash can
		//
		Node node = new Node();
		node.setName("TrashManagerImplAutowiredTesttestMultipleNodeRoundTrip()");
		node.setNodeType(EntityType.project.name());

		Node node00 = new Node();
		node00.setName("TrashManagerImplAutowiredTesttestMultipleNodeRoundTrip() 00");
		node00.setNodeType(EntityType.folder.name());

		Node node11 = new Node();
		node11.setName("TrashManagerImplAutowiredTesttestMultipleNodeRoundTrip() 11");
		node11.setNodeType(EntityType.folder.name());

		Node node12 = new Node();
		node12.setName("TrashManagerImplAutowiredTesttestMultipleNodeRoundTrip() 12");
		node12.setNodeType(EntityType.folder.name());

		Node node21 = new Node();
		node21.setName("TrashManagerImplAutowiredTesttestMultipleNodeRoundTrip() 21");
		node21.setNodeType(EntityType.dataset.name());

		final String nodeId = nodeManager.createNewNode(node, testUserInfo);
		assertNotNull(nodeId);
		toClearList.add(nodeId);

		node00.setParentId(nodeId);
		final String nodeId00 = nodeManager.createNewNode(node00, testUserInfo);
		assertNotNull(nodeId00);
		toClearList.add(nodeId00);

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

		Node nodeBack00 = nodeManager.get(testUserInfo, nodeId00);
		assertNotNull(nodeBack00);
		final String parentId00 = nodeBack00.getParentId();

		Node nodeBack11 = nodeManager.get(testUserInfo, nodeId11);
		assertNotNull(nodeBack11);
		final String parentId11 = nodeBack11.getParentId();

		Node nodeBack12 = nodeManager.get(testUserInfo, nodeId12);
		assertNotNull(nodeBack12);
		final String parentId12 = nodeBack12.getParentId();

		Node nodeBack21 = nodeManager.get(testUserInfo, nodeId21);
		assertNotNull(nodeBack21);
		final String parentId21 = nodeBack21.getParentId();

		trashManager.moveToTrash(testUserInfo, nodeId00);

		try {
			nodeBack00 = nodeManager.get(testUserInfo, nodeId00);
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

		results = trashManager.viewTrash(testUserInfo, 0, 1000);
		assertEquals(4L, results.getTotalNumberOfResults());
		assertEquals(4, results.getResults().size());

		trashManager.restoreFromTrash(testUserInfo, nodeId00, parentId00);

		results = trashManager.viewTrash(testUserInfo, 0, 1000);
		assertEquals(0L, results.getTotalNumberOfResults());
		assertEquals(0, results.getResults().size());

		nodeBack00 = nodeManager.get(testUserInfo, nodeId00);
		assertNotNull(nodeBack00);
		assertEquals(nodeId00, nodeBack00.getId());
		assertEquals(parentId00, nodeBack00.getParentId());

		nodeBack11 = nodeManager.get(testUserInfo, nodeId11);
		assertNotNull(nodeBack11);
		assertEquals(nodeId11, nodeBack11.getId());
		assertEquals(parentId11, nodeBack11.getParentId());

		nodeBack12 = nodeManager.get(testUserInfo, nodeId12);
		assertNotNull(nodeBack12);
		assertEquals(nodeId12, nodeBack12.getId());
		assertEquals(parentId12, nodeBack12.getParentId());

		nodeBack21 = nodeManager.get(testUserInfo, nodeId21);
		assertNotNull(nodeBack21);
		assertEquals(nodeId21, nodeBack21.getId());
		assertEquals(parentId21, nodeBack21.getParentId());
	}
}
