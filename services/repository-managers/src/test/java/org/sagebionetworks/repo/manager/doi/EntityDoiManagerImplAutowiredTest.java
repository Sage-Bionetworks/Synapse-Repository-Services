package org.sagebionetworks.repo.manager.doi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
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
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DoiAdminDao;
import org.sagebionetworks.repo.model.DoiDao;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.model.doi.DoiStatus;
import org.sagebionetworks.repo.web.NotFoundException;
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
	private UserInfo adminUserInfo;
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
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
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
		assertNull(doiCreate.getObjectVersion());
		assertNotNull(doiCreate.getCreatedOn());
		assertEquals(testUserInfo.getId().toString(), doiCreate.getCreatedBy());
		assertNotNull(doiCreate.getUpdatedOn());
		assertEquals(DoiStatus.IN_PROCESS, doiCreate.getDoiStatus());

		Doi doiGet = entityDoiManager.getDoiForVersion(testUserId, nodeId, null);
		assertNotNull(doiGet);
		assertNotNull(doiGet.getId());
		assertEquals(nodeId, doiGet.getObjectId());
		assertEquals(ObjectType.ENTITY, doiGet.getObjectType());
		assertNull(doiGet.getObjectVersion());
		assertNotNull(doiGet.getCreatedOn());
		assertEquals(testUserInfo.getId().toString(), doiGet.getCreatedBy());
		assertNotNull(doiGet.getUpdatedOn());
		assertNotNull(doiGet.getDoiStatus());
		assertEquals(doiGet, doiCreate);

		// Wait for status to turn green
		DoiStatus doiStatus = doiGet.getDoiStatus();
		long time = 0L;
		while (time < MAX_WAIT && DoiStatus.IN_PROCESS.equals(doiStatus)) {
			Thread.sleep(PAUSE);
			time = time + PAUSE;
			doiGet = entityDoiManager.getDoiForVersion(testUserId, nodeId, null);
			doiStatus = doiGet.getDoiStatus();
		}
		assertEquals(DoiStatus.CREATED, doiStatus);
		while (time < MAX_WAIT && DoiStatus.CREATED.equals(doiStatus)) {
			Thread.sleep(PAUSE);
			time = time + PAUSE;
			doiGet = entityDoiManager.getDoiForVersion(testUserId, nodeId, null);
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

		Doi doiGet = entityDoiManager.getDoiForVersion(testUserId, nodeId, 1L);
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
			doiGet = entityDoiManager.getDoiForVersion(testUserId, nodeId, 1L);
			doiStatus = doiGet.getDoiStatus();
		}
		assertEquals(DoiStatus.CREATED, doiStatus);
		while (time < MAX_WAIT && DoiStatus.CREATED.equals(doiStatus)) {
			Thread.sleep(PAUSE);
			time = time + PAUSE;
			doiGet = entityDoiManager.getDoiForVersion(testUserId, nodeId, 1L);
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
		Doi dto = new Doi();
		dto.setCreatedBy(userId);
		dto.setObjectId(nodeId);
		dto.setObjectType(ObjectType.ENTITY);
		dto.setObjectVersion(null);
		dto.setDoiStatus(DoiStatus.ERROR);
		doiDao.createDoi(dto);

		Doi doiCreate = entityDoiManager.createDoi(testUserId, nodeId, null);

		assertNotNull(doiCreate);
		assertNotNull(doiCreate.getId());
		assertEquals(nodeId, doiCreate.getObjectId());
		assertEquals(ObjectType.ENTITY, doiCreate.getObjectType());
		assertNull(doiCreate.getObjectVersion());
		assertNotNull(doiCreate.getCreatedOn());
		assertEquals(testUserInfo.getId().toString(), doiCreate.getCreatedBy());
		assertNotNull(doiCreate.getUpdatedOn());
		assertEquals(DoiStatus.IN_PROCESS, doiCreate.getDoiStatus());

		Doi doiGet = entityDoiManager.getDoiForVersion(testUserId, nodeId, null);
		assertNotNull(doiGet);
		assertNotNull(doiGet.getId());
		assertEquals(nodeId, doiGet.getObjectId());
		assertEquals(ObjectType.ENTITY, doiGet.getObjectType());
		assertNull(doiCreate.getObjectVersion());
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
			doiGet = entityDoiManager.getDoiForVersion(testUserId, nodeId, null);
			doiStatus = doiGet.getDoiStatus();
		}
		assertEquals(DoiStatus.CREATED, doiStatus);
		while (time < MAX_WAIT && DoiStatus.CREATED.equals(doiStatus)) {
			Thread.sleep(PAUSE);
			time = time + PAUSE;
			doiGet = entityDoiManager.getDoiForVersion(testUserId, nodeId, null);
			doiStatus = doiGet.getDoiStatus();
		}
		assertEquals(DoiStatus.READY, doiStatus);
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void testGetDoiForCurrentVersionProject() {
		Node node = new Node();
		final String nodeName = "EntityDoiManagerImplAutowiredTest.testGetDoiForCurrentVersionProject()";
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
		assertNull(doiCreate.getObjectVersion());
		assertNotNull(doiCreate.getCreatedOn());
		assertEquals(testUserInfo.getId().toString(), doiCreate.getCreatedBy());
		assertNotNull(doiCreate.getUpdatedOn());
		assertEquals(DoiStatus.IN_PROCESS, doiCreate.getDoiStatus());
		
		// getDoiForCurrentVersion looks for the current, numbered version, and only a null
		// versionNumber DOI has been created, so a NotFoundException is thrown
		Doi doiGetCurrent = entityDoiManager.getDoiForCurrentVersion(testUserId, nodeId);
		assertNotNull(doiGetCurrent);
		assertNotNull(doiGetCurrent.getId());
		assertEquals(nodeId, doiGetCurrent.getObjectId());
		assertEquals(ObjectType.ENTITY, doiGetCurrent.getObjectType());
		assertNull(doiCreate.getObjectVersion());
		assertNotNull(doiGetCurrent.getCreatedOn());
		assertEquals(testUserInfo.getId().toString(), doiGetCurrent.getCreatedBy());
		assertNotNull(doiGetCurrent.getUpdatedOn());
		assertNotNull(doiGetCurrent.getDoiStatus());
	}
	
	@SuppressWarnings("deprecation")
	@Test(expected = NotFoundException.class)
	public void testGetDoiForCurrentVersionFile() {
		Node node = new Node();
		final String nodeName = "EntityDoiManagerImplAutowiredTest.testGetDoiForCurrentVersionFile()";
		node.setName(nodeName);
		node.setNodeType(EntityType.file);
		final String nodeId = nodeManager.createNewNode(node, adminUserInfo);
		NamedAnnotations namedAnnos = new NamedAnnotations();
		namedAnnos.setId(node.getId());
		toClearList.add(nodeId);
		assertNotNull(nodeId);

		Doi doiCreate = entityDoiManager.createDoi(adminUserInfo.getId(), nodeId, null);
		assertNotNull(doiCreate);
		assertNotNull(doiCreate.getId());
		assertEquals(nodeId, doiCreate.getObjectId());
		assertEquals(ObjectType.ENTITY, doiCreate.getObjectType());
		assertNull(doiCreate.getObjectVersion());
		assertNotNull(doiCreate.getCreatedOn());
		assertEquals(adminUserInfo.getId().toString(), doiCreate.getCreatedBy());
		assertNotNull(doiCreate.getUpdatedOn());
		assertEquals(DoiStatus.IN_PROCESS, doiCreate.getDoiStatus());
		
		// getDoiForCurrentVersion looks for the current, numbered version, and only a null
		// versionNumber DOI has been created, so a NotFoundException is thrown
		Doi doiGetCurrent = entityDoiManager.getDoiForCurrentVersion(adminUserInfo.getId(), nodeId);
		assertNull(doiGetCurrent);
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void testDOIVersioning() {
		Node node = new Node();
		final String nodeName = "EntityDoiManagerImplAutowiredTest.testDOIVersioning()";
		node.setName(nodeName);
		node.setNodeType(EntityType.file);
		node.setVersionLabel("0.0.1");
		final String nodeId = nodeManager.createNewNode(node, adminUserInfo);
		NamedAnnotations namedAnnos = new NamedAnnotations();
		namedAnnos.setId(node.getId());
		toClearList.add(nodeId);
		assertNotNull(nodeId);
		
		// add a second version
		Node updatedNode = nodeManager.get(adminUserInfo, nodeId);
		String eTagBeforeUpdate = updatedNode.getETag();
		NamedAnnotations namedToUpdate = nodeManager.getAnnotations(adminUserInfo, nodeId);
		namedToUpdate = nodeManager.getAnnotations(adminUserInfo, nodeId);
		Annotations annosToUpdate = namedToUpdate.getAdditionalAnnotations();
		updatedNode.setVersionLabel("0.0.2");
		annosToUpdate.addAnnotation("longKey", new Long(12));
		annosToUpdate.getStringAnnotations().clear();
		String valueOnSecondVersion = "Value on the second version.";
		annosToUpdate.addAnnotation("stringKey", valueOnSecondVersion);
		Node afterUpdate = nodeManager.update(adminUserInfo, updatedNode, namedToUpdate, true);
		assertNotNull(afterUpdate);
		assertNotNull(afterUpdate.getETag());
		assertNotEquals(afterUpdate.getETag(), eTagBeforeUpdate);

		// Create Doi for version 1
		Doi doiCreate = entityDoiManager.createDoi(adminUserInfo.getId(), nodeId, 1L);
		assertNotNull(doiCreate);
		assertNotNull(doiCreate.getId());
		assertEquals(nodeId, doiCreate.getObjectId());
		assertEquals(ObjectType.ENTITY, doiCreate.getObjectType());
		assertEquals(doiCreate.getObjectVersion(), Long.valueOf(1L));
		assertNotNull(doiCreate.getCreatedOn());
		assertEquals(adminUserInfo.getId().toString(), doiCreate.getCreatedBy());
		assertNotNull(doiCreate.getUpdatedOn());
		assertEquals(DoiStatus.IN_PROCESS, doiCreate.getDoiStatus());
		
		// Create Doi for version 2
		doiCreate = entityDoiManager.createDoi(adminUserInfo.getId(), nodeId, 2L);
		assertNotNull(doiCreate);
		assertNotNull(doiCreate.getId());
		assertEquals(nodeId, doiCreate.getObjectId());
		assertEquals(ObjectType.ENTITY, doiCreate.getObjectType());
		assertEquals(doiCreate.getObjectVersion(), Long.valueOf(2L));
		assertNotNull(doiCreate.getCreatedOn());
		assertEquals(adminUserInfo.getId().toString(), doiCreate.getCreatedBy());
		assertNotNull(doiCreate.getUpdatedOn());
		assertEquals(DoiStatus.IN_PROCESS, doiCreate.getDoiStatus());
		
		// After creation of a non-null versionNumber DOI, the getDoiForCurrentVersion
		// should return the created, most recent Doi
		Doi doiGetCurrent = entityDoiManager.getDoiForCurrentVersion(adminUserInfo.getId(), nodeId);
		assertNotNull(doiGetCurrent);
		assertNotNull(doiGetCurrent.getId());
		assertEquals(nodeId, doiGetCurrent.getObjectId());
		assertEquals(ObjectType.ENTITY, doiGetCurrent.getObjectType());
		assertEquals(doiGetCurrent.getObjectVersion(), Long.valueOf(2L));
		assertNotNull(doiGetCurrent.getCreatedOn());
		assertEquals(adminUserInfo.getId().toString(), doiGetCurrent.getCreatedBy());
		assertNotNull(doiGetCurrent.getUpdatedOn());
		assertEquals(DoiStatus.IN_PROCESS, doiGetCurrent.getDoiStatus());
		assertEquals(doiCreate, doiGetCurrent);
		
		// getDoiForVersion should return the created Doi with the matching versionNumber
		Doi doiGetVersion = entityDoiManager.getDoiForVersion(adminUserInfo.getId(), nodeId, 2L);
		assertNotNull(doiGetVersion);
		assertNotNull(doiGetVersion.getId());
		assertEquals(nodeId, doiGetVersion.getObjectId());
		assertEquals(ObjectType.ENTITY, doiGetVersion.getObjectType());
		assertEquals(doiGetVersion.getObjectVersion(), Long.valueOf(2L));
		assertNotNull(doiGetVersion.getCreatedOn());
		assertEquals(adminUserInfo.getId().toString(), doiGetVersion.getCreatedBy());
		assertNotNull(doiGetVersion.getUpdatedOn());
		assertEquals(DoiStatus.IN_PROCESS, doiGetVersion.getDoiStatus());
		assertEquals(doiCreate, doiGetVersion);
		
		// getDoiForVersion should retrieve the requested version, whether it's
		// the most recent or not
		doiGetVersion = entityDoiManager.getDoiForVersion(adminUserInfo.getId(), nodeId, 1L);
		assertNotNull(doiGetVersion);
		assertNotNull(doiGetVersion.getId());
		assertEquals(nodeId, doiGetVersion.getObjectId());
		assertEquals(ObjectType.ENTITY, doiGetVersion.getObjectType());
		assertEquals(doiGetVersion.getObjectVersion(), Long.valueOf(1L));
		assertNotNull(doiGetVersion.getCreatedOn());
		assertEquals(adminUserInfo.getId().toString(), doiGetVersion.getCreatedBy());
		assertNotNull(doiGetVersion.getUpdatedOn());
		assertEquals(DoiStatus.IN_PROCESS, doiGetVersion.getDoiStatus());
		assertNotEquals(doiCreate, doiGetVersion);
	}
}
