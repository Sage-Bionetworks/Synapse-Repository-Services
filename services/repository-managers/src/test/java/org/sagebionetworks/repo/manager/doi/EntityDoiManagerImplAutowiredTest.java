package org.sagebionetworks.repo.manager.doi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.model.DoiAdminDao;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.model.doi.DoiObjectType;
import org.sagebionetworks.repo.model.doi.DoiStatus;
import org.sagebionetworks.repo.web.util.UserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class EntityDoiManagerImplAutowiredTest {

	// Max wait time for the DOI status to turn green
	private static long MAX_WAIT = 12000; // 12 seconds
	private static long PAUSE = 2000;     // Pause between waits is 2 seconds

	@Autowired private EntityDoiManager entityDoiManager;
	@Autowired private NodeManager nodeManager;
	@Autowired private UserProvider userProvider;
	@Autowired private DoiAdminDao doiAdminDao;
	private UserInfo testUserInfo;
	private List<String> toClearList;

	@Before
	public void before() throws Exception {

		assertNotNull(entityDoiManager);
		assertNotNull(nodeManager);
		assertNotNull(userProvider);
		assertNotNull(doiAdminDao);

		testUserInfo = userProvider.getTestUserInfo();
		assertNotNull(testUserInfo);

		toClearList = new ArrayList<String>();
	}

	@After
	public void after() throws Exception {
		for (String nodeId : toClearList) {
			nodeManager.delete(userProvider.getTestAdminUserInfo(), nodeId);
		}
		doiAdminDao.clear();
	}

	@Test
	public void testRoundTrip() throws Exception {

		Node node = new Node();
		final String nodeName = "EntityDoiManagerImplAutowiredTest.testRoundTrip()";
		node.setName(nodeName);
		node.setNodeType(EntityType.project.name());
		final String nodeId = nodeManager.createNewNode(node, testUserInfo);
		toClearList.add(nodeId);
		assertNotNull(nodeId);

		final String userName = testUserInfo.getIndividualGroup().getName();
		Doi doiCreate = entityDoiManager.createDoi(userName, nodeId, null);
		assertNotNull(doiCreate);
		assertNotNull(doiCreate.getId());
		assertEquals(nodeId, doiCreate.getObjectId());
		assertEquals(DoiObjectType.ENTITY, doiCreate.getDoiObjectType());
		assertNull(doiCreate.getObjectVersion());
		assertNotNull(doiCreate.getCreatedOn());
		assertEquals(testUserInfo.getIndividualGroup().getId(), doiCreate.getCreatedBy());
		assertNotNull(doiCreate.getUpdatedOn());
		assertEquals(DoiStatus.IN_PROCESS, doiCreate.getDoiStatus());

		Doi doiGet = entityDoiManager.getDoi(userName, nodeId, null);
		assertNotNull(doiGet);
		assertNotNull(doiGet.getId());
		assertEquals(nodeId, doiGet.getObjectId());
		assertEquals(DoiObjectType.ENTITY, doiGet.getDoiObjectType());
		assertNull(doiGet.getObjectVersion());
		assertNotNull(doiGet.getCreatedOn());
		assertEquals(testUserInfo.getIndividualGroup().getId(), doiGet.getCreatedBy());
		assertNotNull(doiGet.getUpdatedOn());
		assertNotNull(doiGet.getDoiStatus());

		// Wait for status to turn green
		DoiStatus doiStatus = doiGet.getDoiStatus();
		long time = 0L;
		while (time < MAX_WAIT && DoiStatus.IN_PROCESS.equals(doiStatus)) {
			Thread.sleep(PAUSE);
			time = time + PAUSE;
			doiGet = entityDoiManager.getDoi(userName, nodeId, null);
			doiStatus = doiGet.getDoiStatus();
		}
		assertEquals(DoiStatus.READY, doiStatus);
	}

	@Test
	public void testRoundTripWithVersionNumber() throws Exception {

		Node node = new Node();
		final String nodeName = "EntityDoiManagerImplAutowiredTest.testRoundTrip()";
		node.setName(nodeName);
		node.setNodeType(EntityType.project.name());
		final String nodeId = nodeManager.createNewNode(node, testUserInfo);
		toClearList.add(nodeId);
		assertNotNull(nodeId);

		final String userName = testUserInfo.getIndividualGroup().getName();
		Doi doiCreate = entityDoiManager.createDoi(userName, nodeId, 1L);
		assertNotNull(doiCreate);
		assertNotNull(doiCreate.getId());
		assertEquals(nodeId, doiCreate.getObjectId());
		assertEquals(DoiObjectType.ENTITY, doiCreate.getDoiObjectType());
		assertEquals(Long.valueOf(1), doiCreate.getObjectVersion());
		assertNotNull(doiCreate.getCreatedOn());
		assertEquals(testUserInfo.getIndividualGroup().getId(), doiCreate.getCreatedBy());
		assertNotNull(doiCreate.getUpdatedOn());
		assertEquals(DoiStatus.IN_PROCESS, doiCreate.getDoiStatus());

		Doi doiGet = entityDoiManager.getDoi(userName, nodeId, 1L);
		assertNotNull(doiGet);
		assertNotNull(doiGet.getId());
		assertEquals(nodeId, doiGet.getObjectId());
		assertEquals(DoiObjectType.ENTITY, doiGet.getDoiObjectType());
		assertEquals(Long.valueOf(1), doiCreate.getObjectVersion());
		assertNotNull(doiGet.getCreatedOn());
		assertEquals(testUserInfo.getIndividualGroup().getId(), doiGet.getCreatedBy());
		assertNotNull(doiGet.getUpdatedOn());
		assertNotNull(doiGet.getDoiStatus());

		// Wait for status to turn green
		DoiStatus doiStatus = doiGet.getDoiStatus();
		long time = 0L;
		while (time < MAX_WAIT && DoiStatus.IN_PROCESS.equals(doiStatus)) {
			Thread.sleep(PAUSE);
			time = time + PAUSE;
			doiGet = entityDoiManager.getDoi(userName, nodeId, 1L);
			doiStatus = doiGet.getDoiStatus();
		}
		assertEquals(DoiStatus.READY, doiStatus);
	}
}
