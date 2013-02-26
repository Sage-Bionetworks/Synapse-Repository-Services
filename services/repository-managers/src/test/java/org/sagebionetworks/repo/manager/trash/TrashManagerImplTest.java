package org.sagebionetworks.repo.manager.trash;

import static org.mockito.Mockito.mock;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.dynamo.dao.nodetree.NodeTreeDao;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.NodeInheritanceManager;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.TagMessenger;
import org.sagebionetworks.repo.model.dbo.dao.DBOTrashCanDao;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.test.util.ReflectionTestUtils;

public class TrashManagerImplTest {

	private TrashManager trashManager;

	@Before
	public void before() throws Exception {

		AuthorizationManager authorizationManager = mock(AuthorizationManager.class);
		NodeManager nodeManager = mock(NodeManager.class);
		NodeInheritanceManager nodeInheritanceManager = mock(NodeInheritanceManager.class);
		NodeDAO nodeDao = mock(NodeDAO.class);
		NodeTreeDao nodeTreeDao = mock(NodeTreeDao.class);
		DBOTrashCanDao trashCanDao = mock(DBOTrashCanDao.class);
		TagMessenger tagMessenger = mock(TagMessenger.class);

		trashManager = new TrashManagerImpl();
		TrashManager unwrap = trashManager;
		if(AopUtils.isAopProxy(unwrap) && unwrap instanceof Advised) {
			Object target = ((Advised)unwrap).getTargetSource().getTarget();
			unwrap = (TrashManager)target;
		}
		ReflectionTestUtils.setField(unwrap, "authorizationManager", authorizationManager);
		ReflectionTestUtils.setField(unwrap, "nodeManager", nodeManager);
		ReflectionTestUtils.setField(unwrap, "nodeInheritanceManager", nodeInheritanceManager);
		ReflectionTestUtils.setField(unwrap, "nodeDao", nodeDao);
		ReflectionTestUtils.setField(unwrap, "nodeTreeDao", nodeTreeDao);
		ReflectionTestUtils.setField(unwrap, "trashCanDao", trashCanDao);
		ReflectionTestUtils.setField(unwrap, "tagMessenger", tagMessenger);
	}

	@Test
	public void test() throws Exception {
		Assert.assertTrue(true);
	}
}
