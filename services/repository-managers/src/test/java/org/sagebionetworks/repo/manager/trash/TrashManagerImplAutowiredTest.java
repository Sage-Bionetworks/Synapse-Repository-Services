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
	}
}
