package org.sagebionetworks.repo.manager.trash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
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
	@Autowired private UserProvider userProvider;
	private UserInfo testUserInfo;
	private List<String> toClearList;

	@Before
	public void before() throws Exception {
		assertNotNull(trashManager);
		assertNotNull(nodeManager);
		assertNotNull(trashCanDao);
		assertNotNull(userProvider);
		testUserInfo = userProvider.getTestUserInfo();
		assertNotNull(testUserInfo);
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

		Node newNode = new Node();
		newNode.setName("TrashManagerImplAutowiredTest");
		newNode.setNodeType(EntityType.project.name());
		final String nodeId = nodeManager.createNewNode(newNode, testUserInfo);
		assertNotNull(nodeId);
		toClearList.add(nodeId);
		Node nodeBack = nodeManager.get(testUserInfo, nodeId);
		assertNotNull(nodeBack);
		final String parentId = nodeBack.getParentId();

		trashManager.moveToTrash(testUserInfo, nodeId);

		try {
			nodeBack = nodeManager.get(testUserInfo, nodeId);
			assertNotNull(nodeBack);
		} catch (UnauthorizedException e) {
			// TODO: We should throw NotFoundException for items in trash can.
			assertTrue(true);
		} catch (Throwable e) {
			fail();
		}

		results = trashManager.viewTrash(testUserInfo, 0, 1000);
		assertEquals(1L, results.getTotalNumberOfResults());
		assertEquals(1, results.getResults().size());
		TrashedEntity trash = results.getResults().get(0);
		assertNotNull(trash);
		assertEquals(nodeId, trash.getEntityId());
		assertEquals(parentId, trash.getOriginalParentId());
		System.out.println(trash.getDeletedByPrincipalId());
		System.out.println(trash.getDeletedOn());

		trashManager.restoreFromTrash(testUserInfo, nodeId, parentId);

		results = trashManager.viewTrash(testUserInfo, 0, 1000);
		assertEquals(0L, results.getTotalNumberOfResults());
		assertEquals(0, results.getResults().size());

		nodeBack = nodeManager.get(testUserInfo, nodeId);
		assertNotNull(nodeBack);
		assertEquals(nodeId, nodeBack.getId());
		assertEquals(parentId, nodeBack.getParentId());
	}
}
