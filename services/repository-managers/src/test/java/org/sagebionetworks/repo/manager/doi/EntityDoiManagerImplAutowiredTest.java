package org.sagebionetworks.repo.manager.doi;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.model.DoiAdminDao;
import org.sagebionetworks.repo.web.util.UserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class EntityDoiManagerImplAutowiredTest {

	@Autowired private EntityDoiManager entityDoiManager;
	@Autowired private NodeManager nodeManager;
	@Autowired private UserProvider userProvider;
	@Autowired private DoiAdminDao doiAdminDao;
	private List<String> toClearList;

	@Before
	public void before() throws Exception {
		assertNotNull(entityDoiManager);
		assertNotNull(nodeManager);
		assertNotNull(userProvider);
		assertNotNull(doiAdminDao);
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
	public void test() throws Exception {
		
	}
}
