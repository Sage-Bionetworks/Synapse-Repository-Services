package org.sagebionetworks.repo.manager.doi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.doi.DoiAsyncClient;
import org.sagebionetworks.doi.DxAsyncClient;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DoiAdminDao;
import org.sagebionetworks.repo.model.DoiDao;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.model.doi.DoiStatus;
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

	@Autowired 
	private EntityDoiManager entityDoiManager;
	
	@Autowired 
	private NodeManager nodeManager;
	
	@Autowired 
	private DoiDao doiDao;
	
	@Autowired 
	private DoiAdminDao doiAdminDao;
	
	@Autowired
	private UserManager userManager;
	
	private Long testUserId;
	private UserInfo testUserInfo;
	private List<String> toClearList;

	@Before
	public void before() throws Exception {
		NewUser user = new NewUser();
		user.setEmail(UUID.randomUUID().toString() + "@test.com");
		user.setUserName(UUID.randomUUID().toString());
		testUserId = userManager.createUser(user);
		testUserInfo = userManager.getUserInfo(testUserId);

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
		UserInfo adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		for (String nodeId : toClearList) {
			nodeManager.delete(adminUserInfo, nodeId);
		}
		doiAdminDao.clear();
		
		userManager.deletePrincipal(adminUserInfo, testUserId);
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testRoundTrip() throws Exception {

		Node node = new Node();
		final String nodeName = "EntityDoiManagerImplAutowiredTest.testRoundTrip()";
		node.setName(nodeName);
		node.setNodeType(EntityType.project);
		final String nodeId = nodeManager.createNewNode(node, testUserInfo);
		toClearList.add(nodeId);
		assertNotNull(nodeId);

		Doi doiCreate = entityDoiManager.createDoi(testUserId, nodeId, null);
		assertNotNull(doiCreate);
		assertNotNull(doiCreate.getId());
		assertEquals(nodeId, doiCreate.getObjectId());
		assertEquals(ObjectType.ENTITY, doiCreate.getObjectType());
		assertEquals(doiCreate.getObjectVersion(), Long.valueOf(1));
		assertNotNull(doiCreate.getCreatedOn());
		assertEquals(testUserInfo.getId().toString(), doiCreate.getCreatedBy());
		assertNotNull(doiCreate.getUpdatedOn());
		assertEquals(DoiStatus.IN_PROCESS, doiCreate.getDoiStatus());

		Doi doiGet = entityDoiManager.getDoi(testUserId, nodeId, null);
		assertNotNull(doiGet);
		assertNotNull(doiGet.getId());
		assertEquals(nodeId, doiGet.getObjectId());
		assertEquals(ObjectType.ENTITY, doiGet.getObjectType());
		assertEquals(doiGet.getObjectVersion(), Long.valueOf(1));
		assertNotNull(doiGet.getCreatedOn());
		assertEquals(testUserInfo.getId().toString(), doiGet.getCreatedBy());
		assertNotNull(doiGet.getUpdatedOn());
		assertNotNull(doiGet.getDoiStatus());

		// Wait for status to turn green
		DoiStatus doiStatus = doiGet.getDoiStatus();
		long time = 0L;
		while (time < MAX_WAIT && DoiStatus.IN_PROCESS.equals(doiStatus)) {
			Thread.sleep(PAUSE);
			time = time + PAUSE;
			doiGet = entityDoiManager.getDoi(testUserId, nodeId, null);
			doiStatus = doiGet.getDoiStatus();
		}
		assertEquals(DoiStatus.CREATED, doiStatus);
		while (time < MAX_WAIT && DoiStatus.CREATED.equals(doiStatus)) {
			Thread.sleep(PAUSE);
			time = time + PAUSE;
			doiGet = entityDoiManager.getDoi(testUserId, nodeId, null);
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
		node.setNodeType(EntityType.project);
		final String nodeId = nodeManager.createNewNode(node, testUserInfo);
		toClearList.add(nodeId);
		assertNotNull(nodeId);

		Doi doiCreate = entityDoiManager.createDoi(testUserId, nodeId, 1L);
		assertNotNull(doiCreate);
		assertNotNull(doiCreate.getId());
		assertEquals(nodeId, doiCreate.getObjectId());
		assertEquals(ObjectType.ENTITY, doiCreate.getObjectType());
		assertEquals(Long.valueOf(1), doiCreate.getObjectVersion());
		assertNotNull(doiCreate.getCreatedOn());
		assertEquals(testUserInfo.getId().toString(), doiCreate.getCreatedBy());
		assertNotNull(doiCreate.getUpdatedOn());
		assertEquals(DoiStatus.IN_PROCESS, doiCreate.getDoiStatus());

		Doi doiGet = entityDoiManager.getDoi(testUserId, nodeId, 1L);
		assertNotNull(doiGet);
		assertNotNull(doiGet.getId());
		assertEquals(nodeId, doiGet.getObjectId());
		assertEquals(ObjectType.ENTITY, doiGet.getObjectType());
		assertEquals(Long.valueOf(1), doiCreate.getObjectVersion());
		assertNotNull(doiGet.getCreatedOn());
		assertEquals(testUserInfo.getId().toString(), doiGet.getCreatedBy());
		assertNotNull(doiGet.getUpdatedOn());
		assertNotNull(doiGet.getDoiStatus());

		// Wait for status to turn green
		DoiStatus doiStatus = doiGet.getDoiStatus();
		long time = 0L;
		while (time < MAX_WAIT && DoiStatus.IN_PROCESS.equals(doiStatus)) {
			Thread.sleep(PAUSE);
			time = time + PAUSE;
			doiGet = entityDoiManager.getDoi(testUserId, nodeId, 1L);
			doiStatus = doiGet.getDoiStatus();
		}
		assertEquals(DoiStatus.CREATED, doiStatus);
		while (time < MAX_WAIT && DoiStatus.CREATED.equals(doiStatus)) {
			Thread.sleep(PAUSE);
			time = time + PAUSE;
			doiGet = entityDoiManager.getDoi(testUserId, nodeId, 1L);
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
		node.setNodeType(EntityType.project);
		final String nodeId = nodeManager.createNewNode(node, testUserInfo);
		toClearList.add(nodeId);
		assertNotNull(nodeId);

		// Set up an error status first
		// Test that we should be able to recreate the DOI from here
		final String userId = testUserInfo.getId().toString();
		doiDao.createDoi(userId, nodeId, ObjectType.ENTITY, null, DoiStatus.ERROR);

		Doi doiCreate = entityDoiManager.createDoi(testUserId, nodeId, null);

		assertNotNull(doiCreate);
		assertNotNull(doiCreate.getId());
		assertEquals(nodeId, doiCreate.getObjectId());
		assertEquals(ObjectType.ENTITY, doiCreate.getObjectType());
		assertEquals(Long.valueOf(1), doiCreate.getObjectVersion());
		assertNotNull(doiCreate.getCreatedOn());
		assertEquals(testUserInfo.getId().toString(), doiCreate.getCreatedBy());
		assertNotNull(doiCreate.getUpdatedOn());
		assertEquals(DoiStatus.IN_PROCESS, doiCreate.getDoiStatus());

		Doi doiGet = entityDoiManager.getDoi(testUserId, nodeId, null);
		assertNotNull(doiGet);
		assertNotNull(doiGet.getId());
		assertEquals(nodeId, doiGet.getObjectId());
		assertEquals(ObjectType.ENTITY, doiGet.getObjectType());
		assertEquals(Long.valueOf(1), doiCreate.getObjectVersion());
		assertNotNull(doiGet.getCreatedOn());
		assertEquals(testUserInfo.getId().toString(), doiGet.getCreatedBy());
		assertNotNull(doiGet.getUpdatedOn());
		assertNotNull(doiGet.getDoiStatus());

		// Wait for status to turn green
		DoiStatus doiStatus = doiGet.getDoiStatus();
		long time = 0L;
		while (time < MAX_WAIT && DoiStatus.IN_PROCESS.equals(doiStatus)) {
			Thread.sleep(PAUSE);
			time = time + PAUSE;
			doiGet = entityDoiManager.getDoi(testUserId, nodeId, null);
			doiStatus = doiGet.getDoiStatus();
		}
		assertEquals(DoiStatus.CREATED, doiStatus);
		while (time < MAX_WAIT && DoiStatus.CREATED.equals(doiStatus)) {
			Thread.sleep(PAUSE);
			time = time + PAUSE;
			doiGet = entityDoiManager.getDoi(testUserId, nodeId, null);
			doiStatus = doiGet.getDoiStatus();
		}
		assertEquals(DoiStatus.READY, doiStatus);
	}
}
