package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.sql.Timestamp;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.DoiAdminDao;
import org.sagebionetworks.repo.model.DoiMigratableDao;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.doi.DoiStatus;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBODoiMigratableDaoImplAutowiredTest {

	@Autowired DoiMigratableDao doiMigratableDao;
	@Autowired DoiAdminDao doiAdminDao;

	@Before
	public void before() {
		assertNotNull(doiMigratableDao);
		assertNotNull(doiAdminDao);
	}

	@After
	public void after() {
		doiAdminDao.clear();
	}

	@Test
	public void testGetUpdateDelete() {

		assertEquals(0, doiMigratableDao.getCount());

		final Long createdBy = 1L;
		final Timestamp createdOn = new Timestamp((new Date()).getTime());
		final ObjectType objectType = ObjectType.ENTITY;
		final DoiStatus doiStatus = DoiStatus.CREATED;
		final String etag = "etag";
		final Long id = 2L;
		final Long objectId = 3L;
		final Long objectVersion = 4L;
		final Timestamp updatedOn = new Timestamp((new Date()).getTime());
		Doi dto = new Doi();
		dto.setCreatedBy(createdBy.toString());
		dto.setCreatedOn(createdOn);
		dto.setObjectType(objectType);
		dto.setDoiStatus(doiStatus);
		dto.setEtag(etag);
		dto.setId(id.toString());
		dto.setObjectId(objectId.toString());
		dto.setObjectVersion(objectVersion);
		dto.setUpdatedOn(updatedOn);

		doiMigratableDao.createOrUpdate(dto);
		assertEquals(1, doiMigratableDao.getCount());

		Doi dtoRetrieved = doiMigratableDao.get(id.toString());
		assertNotNull(dtoRetrieved);
		assertEquals(createdBy.toString(), dtoRetrieved.getCreatedBy());
		assertEquals(createdOn.getTime()/1000, dtoRetrieved.getCreatedOn().getTime()/1000);
		assertEquals(objectType, dtoRetrieved.getObjectType());
		assertEquals(doiStatus, dtoRetrieved.getDoiStatus());
		assertEquals(etag, dtoRetrieved.getEtag());
		assertEquals(id.toString(), dtoRetrieved.getId());
		assertEquals(objectId, KeyFactory.stringToKey(dtoRetrieved.getObjectId()));
		assertEquals(objectVersion, dtoRetrieved.getObjectVersion());
		assertEquals(updatedOn.getTime()/1000, dtoRetrieved.getUpdatedOn().getTime()/1000);

		final String etag2 = "etag2";
		dto.setEtag(etag2);
		doiMigratableDao.createOrUpdate(dto);
		assertEquals(1, doiMigratableDao.getCount());
		dtoRetrieved = doiMigratableDao.get(id.toString());
		assertEquals(etag2, dtoRetrieved.getEtag());

		final Long id2 = 99L;
		dto.setId(id2.toString());
		dto.setObjectId("98");
		doiMigratableDao.createOrUpdate(dto);
		assertEquals(2, doiMigratableDao.getCount());

		doiMigratableDao.delete(id.toString());
		assertEquals(1, doiMigratableDao.getCount());
		assertNull(doiMigratableDao.get(id.toString()));
		dtoRetrieved = doiMigratableDao.get(id2.toString());
		assertEquals(id2.toString(), dtoRetrieved.getId());

		doiMigratableDao.delete(id2.toString());
		assertEquals(0, doiMigratableDao.getCount());
	}
}
