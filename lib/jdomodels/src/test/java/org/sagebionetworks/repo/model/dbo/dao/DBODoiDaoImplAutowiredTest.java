package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DoiAdminDao;
import org.sagebionetworks.repo.model.DoiDao;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.model.doi.DoiObjectType;
import org.sagebionetworks.repo.model.doi.DoiStatus;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBODoiDaoImplAutowiredTest {

	@Autowired private DoiDao doiDao;
	@Autowired private DoiAdminDao doiAdminDao;
	@Autowired private UserGroupDAO userGroupDAO;
	private String userId;

	@Before
	public void before() throws Exception {
		assertNotNull(doiDao);
		assertNotNull(doiAdminDao);
		assertNotNull(userGroupDAO);
		userId = userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false).getId();
		assertNotNull(userId);
	}

	@After
	public void after() throws Exception {
		doiAdminDao.clear();
	}

	@Test
	public void test() throws Exception {
		final String objectId = KeyFactory.keyToString(112233L);
		final DoiObjectType objectType = DoiObjectType.ENTITY;
		final Long versionNumber = 1L;
		final DoiStatus doiStatus = DoiStatus.IN_PROCESS;
		Doi doi = doiDao.createDoi(userId, objectId, objectType, versionNumber, doiStatus);
		System.out.println(doi.getId());
		assertEquals(objectId, doi.getObjectId());
		assertEquals(versionNumber, doi.getObjectVersion());
		assertEquals(objectType, doi.getDoiObjectType());
		assertEquals(doiStatus, doi.getDoiStatus());
		assertEquals(userId, doi.getCreatedBy());
		System.out.println(doi.getCreatedOn());
		System.out.println(doi.getUpdatedOn());
	}
}
