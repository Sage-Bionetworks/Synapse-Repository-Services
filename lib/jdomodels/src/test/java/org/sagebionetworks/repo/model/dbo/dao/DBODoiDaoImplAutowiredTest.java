package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.UuidETagGenerator;
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
		// Create a DOI
		final String objectId = KeyFactory.keyToString(112233L);
		final DoiObjectType objectType = DoiObjectType.ENTITY;
		final Long versionNumber = 1L;
		final DoiStatus doiStatus = DoiStatus.IN_PROCESS;
		Doi doi = doiDao.createDoi(userId, objectId, objectType, versionNumber, doiStatus);
		assertNotNull(doi);
		assertNotNull(doi.getId());
		final String id1 = doi.getId();
		assertNotNull(doi.getEtag());
		assertFalse(UuidETagGenerator.ZERO_E_TAG.equals(doi.getEtag()));
		final String etag1 = doi.getEtag();
		assertEquals(objectId, doi.getObjectId());
		assertEquals(versionNumber, doi.getObjectVersion());
		assertEquals(objectType, doi.getDoiObjectType());
		assertEquals(doiStatus, doi.getDoiStatus());
		assertEquals(userId, doi.getCreatedBy());
		assertNotNull(doi.getCreatedOn());
		assertNotNull(doi.getUpdatedOn());
		// Create another DOI of null object version
		// This would be treated as a separate DOI
		doi = doiDao.createDoi(userId, objectId, objectType, null, doiStatus);
		assertNotNull(doi);
		assertNotNull(doi.getId());
		final String id2 = doi.getId();
		assertNotNull(doi.getEtag());
		assertFalse(UuidETagGenerator.ZERO_E_TAG.equals(doi.getEtag()));
		final String etag2 = doi.getEtag();
		assertEquals(objectId, doi.getObjectId());
		assertNull(doi.getObjectVersion());
		assertEquals(objectType, doi.getDoiObjectType());
		assertEquals(doiStatus, doi.getDoiStatus());
		assertEquals(userId, doi.getCreatedBy());
		assertNotNull(doi.getCreatedOn());
		assertNotNull(doi.getUpdatedOn());
		// Get the two DOIs back
		doi = doiDao.getDoi(objectId, objectType, versionNumber);
		assertNotNull(doi);
		assertNotNull(doi.getId());
		assertEquals(id1, doi.getId());
		assertNotNull(doi.getEtag());
		assertEquals(etag1, doi.getEtag());
		assertEquals(objectId, doi.getObjectId());
		assertEquals(versionNumber, doi.getObjectVersion());
		assertEquals(objectType, doi.getDoiObjectType());
		assertEquals(doiStatus, doi.getDoiStatus());
		assertEquals(userId, doi.getCreatedBy());
		assertNotNull(doi.getCreatedOn());
		assertNotNull(doi.getUpdatedOn());
		doi = doiDao.getDoi(objectId, objectType, null);
		assertNotNull(doi);
		assertNotNull(doi.getId());
		assertEquals(id2, doi.getId());
		assertNotNull(doi.getEtag());
		assertEquals(etag2, doi.getEtag());
		assertEquals(objectId, doi.getObjectId());
		System.out.println(doi.getObjectVersion());
		assertNull(doi.getObjectVersion());
		assertEquals(objectType, doi.getDoiObjectType());
		assertEquals(doiStatus, doi.getDoiStatus());
		assertEquals(userId, doi.getCreatedBy());
		assertNotNull(doi.getCreatedOn());
		assertNotNull(doi.getUpdatedOn());
		// Update status
		doiDao.updateDoiStatus(objectId, objectType, versionNumber, DoiStatus.READY, etag1);
		doi = doiDao.getDoi(objectId, objectType, versionNumber);
		assertNotNull(doi);
		assertNotNull(doi.getId());
		assertEquals(id1, doi.getId());
		assertNotNull(doi.getEtag());
		assertEquals(etag1, doi.getEtag());
		assertEquals(objectId, doi.getObjectId());
		assertEquals(versionNumber, doi.getObjectVersion());
		assertEquals(objectType, doi.getDoiObjectType());
		assertEquals(DoiStatus.READY, doi.getDoiStatus());
		assertEquals(userId, doi.getCreatedBy());
		assertNotNull(doi.getCreatedOn());
		assertNotNull(doi.getUpdatedOn());
		doiDao.updateDoiStatus(objectId, objectType, null, DoiStatus.ERROR, etag2);
		doi = doiDao.getDoi(objectId, objectType, null);
		assertNotNull(doi);
		assertNotNull(doi.getId());
		assertEquals(id2, doi.getId());
		assertNotNull(doi.getEtag());
		assertEquals(etag2, doi.getEtag());
		assertEquals(objectId, doi.getObjectId());
		assertNull(doi.getObjectVersion());
		assertEquals(objectType, doi.getDoiObjectType());
		assertEquals(DoiStatus.ERROR, doi.getDoiStatus());
		assertEquals(userId, doi.getCreatedBy());
		assertNotNull(doi.getCreatedOn());
		assertNotNull(doi.getUpdatedOn());
	}
}
