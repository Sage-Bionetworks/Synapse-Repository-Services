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
import org.sagebionetworks.doi.DoiAsyncClient;
import org.sagebionetworks.doi.DxAsyncClient;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.model.DoiAdminDao;
import org.sagebionetworks.repo.model.DoiDao;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.model.doi.DoiObjectType;
import org.sagebionetworks.repo.model.doi.DoiStatus;
import org.sagebionetworks.repo.web.util.UserProvider;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class EntityDoiManagerImplAutowiredTest {

	// Max wait time for the DOI status to turn green
	private static long MAX_WAIT = 10000; // 10 seconds
	private static long PAUSE = 100;      // Pause between checks is 100 ms
	private static long EZID_CLIENT_DELAY = 1000;
	private static long DX_CLIENT_DELAY = 3000;

	@Autowired private EntityDoiManager entityDoiManager;
	@Autowired private NodeManager nodeManager;
	@Autowired private UserProvider userProvider;
	@Autowired private DoiDao doiDao;
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

		DoiAsyncClient mockEzidClient = new MockDoiAsyncClient(EZID_CLIENT_DELAY);
		DxAsyncClient mockDxClient = new MockDxAsyncClient(DX_CLIENT_DELAY);
		EntityDoiManager manager = entityDoiManager;
		if (AopUtils.isAopProxy(manager) && manager instanceof Advised) {
			Object target = ((Advised)manager).getTargetSource().getTarget();
			manager = (EntityDoiManagerImpl)target;
		}
		ReflectionTestUtils.setField(manager, "ezidAsyncClient", mockEzidClient);
		ReflectionTestUtils.setField(manager, "dxAsyncClient", mockDxClient);
	}

	@After
	public void after() throws Exception {
		for (String nodeId : toClearList) {
			nodeManager.delete(userProvider.getTestAdminUserInfo(), nodeId);
		}
		doiAdminDao.clear();
	}

	@SuppressWarnings("deprecation")
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
		assertEquals(DoiStatus.CREATED, doiStatus);
		while (time < MAX_WAIT && DoiStatus.CREATED.equals(doiStatus)) {
			Thread.sleep(PAUSE);
			time = time + PAUSE;
			doiGet = entityDoiManager.getDoi(userName, nodeId, null);
			doiStatus = doiGet.getDoiStatus();
		}
		assertEquals(DoiStatus.READY, doiStatus);
	}

	@SuppressWarnings("deprecation")
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
		assertEquals(DoiStatus.CREATED, doiStatus);
		while (time < MAX_WAIT && DoiStatus.CREATED.equals(doiStatus)) {
			Thread.sleep(PAUSE);
			time = time + PAUSE;
			doiGet = entityDoiManager.getDoi(userName, nodeId, 1L);
			doiStatus = doiGet.getDoiStatus();
		}
		assertEquals(DoiStatus.READY, doiStatus);
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testRetryableOnError() throws Exception {

		Node node = new Node();
		final String nodeName = "EntityDoiManagerImplAutowiredTest.testRetryableOnError()";
		node.setName(nodeName);
		node.setNodeType(EntityType.project.name());
		final String nodeId = nodeManager.createNewNode(node, testUserInfo);
		toClearList.add(nodeId);
		assertNotNull(nodeId);

		// Set up an error status first
		// Test that we should be able to recreate the DOI from here
		final String userId = testUserInfo.getIndividualGroup().getId();
		doiDao.createDoi(userId, nodeId, DoiObjectType.ENTITY, null, DoiStatus.ERROR);

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
		assertNull(doiCreate.getObjectVersion());
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
		assertEquals(DoiStatus.CREATED, doiStatus);
		while (time < MAX_WAIT && DoiStatus.CREATED.equals(doiStatus)) {
			Thread.sleep(PAUSE);
			time = time + PAUSE;
			doiGet = entityDoiManager.getDoi(userName, nodeId, null);
			doiStatus = doiGet.getDoiStatus();
		}
		assertEquals(DoiStatus.READY, doiStatus);
	}
}
