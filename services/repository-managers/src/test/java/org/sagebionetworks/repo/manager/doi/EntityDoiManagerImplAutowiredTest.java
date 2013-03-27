package org.sagebionetworks.repo.manager.doi;

import static org.mockito.Mockito.mock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.doi.DoiClient;
import org.sagebionetworks.doi.EzidAsyncClient;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.model.DoiAdminDao;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.StorageLocationDAOImpl;
import org.sagebionetworks.repo.model.doi.Doi;
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

		// Inject a mock DOI client
		// If the DOI already exists, EZID will return an error
		// We want to avoid the error, thus the mocking
		DoiClient mockDoiClient = mock(EzidAsyncClient.class);
		EntityDoiManager toInject = entityDoiManager;
		if(AopUtils.isAopProxy(toInject) && toInject instanceof Advised) {
			Object target = ((Advised)toInject).getTargetSource().getTarget();
			toInject = (EntityDoiManager)target;
		}
		ReflectionTestUtils.setField(toInject, "ezidAsyncClient", mockDoiClient);

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
		Doi doiCreate = entityDoiManager.createDoi(
				testUserInfo.getIndividualGroup().getName(), nodeId, null);
		assertNotNull(doiCreate);
		assertEquals(DoiStatus.IN_PROCESS, doiCreate.getDoiStatus());
		Thread.sleep(1000L);
		Doi doiGet = entityDoiManager.getDoi(
				testUserInfo.getIndividualGroup().getName(), nodeId, null);
		assertNotNull(doiGet);
	}
}
