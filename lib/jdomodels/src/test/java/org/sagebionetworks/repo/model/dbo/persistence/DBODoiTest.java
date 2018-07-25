package org.sagebionetworks.repo.model.dbo.persistence;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.doi.DoiStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBODoiTest {

	@Autowired
	private DBOBasicDao dboBasicDao;

	@Autowired
	private IdGenerator idGenerator;

	private List<Long> toDelete = null;

	private Long userId;

	@After
	public void after() throws DatastoreException {
		if(dboBasicDao != null && toDelete != null){
			for(Long id: toDelete){
				MapSqlParameterSource params = new MapSqlParameterSource();
				params.addValue("id", id);
				dboBasicDao.deleteObjectByPrimaryKey(DBODoi.class, params);
			}
		}
	}

	@Before
	public void before(){
		toDelete = new LinkedList<Long>();
		userId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
	}

	@Test
	public void createDoiWithoutVersionNumber() {
		DBODoi doi = new DBODoi();
		doi.setId(idGenerator.generateNewId(IdType.DOI_ID));
		doi.setCreatedOn(Timestamp.valueOf(LocalDateTime.now()));
		doi.setUpdatedOn(Timestamp.valueOf(LocalDateTime.now()));
		doi.setCreatedBy(userId);
		doi.setETag("some etag");
		doi.setDoiStatus(DoiStatus.IN_PROCESS);
		doi.setObjectId(1234L);
		doi.setObjectType(ObjectType.ENTITY);
		doi.setObjectVersion(-1L);

		// Method under test
		DBODoi clone = dboBasicDao.createNew(doi);
		assertEquals(userId, clone.getCreatedBy());
		assertEquals((Long) 1234L, clone.getObjectId());
		assertEquals(ObjectType.ENTITY.name(), clone.getObjectType());
		assertEquals("some etag", clone.getETag());
		assertEquals(DoiStatus.IN_PROCESS.name(), clone.getDoiStatus());
		assertNotNull(clone.getObjectId());
		assertNotNull(clone.getETag());
		assertEquals(Long.valueOf(-1), clone.getObjectVersion());
		toDelete.add(clone.getId());
	}

	@Test
	public void createDoiWithVersionNumber() {
		DBODoi doi = new DBODoi();
		doi.setId(idGenerator.generateNewId(IdType.DOI_ID));
		doi.setCreatedOn(Timestamp.valueOf(LocalDateTime.now()));
		doi.setUpdatedOn(Timestamp.valueOf(LocalDateTime.now()));
		doi.setCreatedBy(userId);
		doi.setETag("some etag");
		doi.setDoiStatus(DoiStatus.IN_PROCESS);
		doi.setObjectId(1234L);
		doi.setObjectType(ObjectType.ENTITY);
		doi.setObjectVersion(4L);

		// Method under test
		DBODoi clone = dboBasicDao.createNew(doi);
		assertEquals(userId, clone.getCreatedBy());
		assertEquals((Long) 1234L, clone.getObjectId());
		assertEquals(ObjectType.ENTITY.name(), clone.getObjectType());
		assertEquals("some etag", clone.getETag());
		assertEquals(DoiStatus.IN_PROCESS.name(), clone.getDoiStatus());
		assertNotNull(clone.getObjectId());
		assertNotNull(clone.getETag());
		assertEquals((Long) 4L, clone.getObjectVersion());
		toDelete.add(clone.getId());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testFailTwoObjectsWithSameVersion() {
		DBODoi doi = new DBODoi();
		doi.setId(idGenerator.generateNewId(IdType.DOI_ID));
		doi.setCreatedOn(Timestamp.valueOf(LocalDateTime.now()));
		doi.setUpdatedOn(Timestamp.valueOf(LocalDateTime.now()));
		doi.setCreatedBy(userId);
		doi.setETag("some etag");
		doi.setDoiStatus(DoiStatus.IN_PROCESS);
		doi.setObjectId(1234L);
		doi.setObjectType(ObjectType.ENTITY);
		doi.setObjectVersion(4L);

		DBODoi clone1 = null;
		try {
			clone1 = dboBasicDao.createNew(doi);
		} catch (IllegalArgumentException e) {
			fail(); // Adding the first item should succeed.
		}
		toDelete.add(clone1.getId());
		doi.setId(idGenerator.generateNewId(IdType.DOI_ID));
		// Method under test
		DBODoi clone2 = dboBasicDao.createNew(doi);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testFailTwoObjectsWithNoVersion() {
		DBODoi doi = new DBODoi();
		doi.setId(idGenerator.generateNewId(IdType.DOI_ID));
		doi.setCreatedOn(Timestamp.valueOf(LocalDateTime.now()));
		doi.setUpdatedOn(Timestamp.valueOf(LocalDateTime.now()));
		doi.setCreatedBy(userId);
		doi.setETag("some etag");
		doi.setDoiStatus(DoiStatus.IN_PROCESS);
		doi.setObjectId(1234L);
		doi.setObjectType(ObjectType.ENTITY);
		doi.setObjectVersion(-1L);

		DBODoi clone1 = null;
		try {
			clone1 = dboBasicDao.createNew(doi);
		} catch (IllegalArgumentException e) {
			fail(); // Adding the first item should succeed.
		}
		toDelete.add(clone1.getId());
		doi.setId(idGenerator.generateNewId(IdType.DOI_ID));
		// Method under test
		DBODoi clone2 = dboBasicDao.createNew(doi);
	}

	@Test
	public void testMultiVersionedDoi() {
		DBODoi doi = new DBODoi();
		doi.setId(idGenerator.generateNewId(IdType.DOI_ID));
		doi.setCreatedOn(Timestamp.valueOf(LocalDateTime.now()));
		doi.setUpdatedOn(Timestamp.valueOf(LocalDateTime.now()));
		doi.setCreatedBy(userId);
		doi.setETag("some etag");
		doi.setDoiStatus(DoiStatus.IN_PROCESS);
		doi.setObjectId(1234L);
		doi.setObjectType(ObjectType.ENTITY);
		doi.setObjectVersion(-1L);

		DBODoi clone1 = dboBasicDao.createNew(doi);

		toDelete.add(clone1.getId());
		doi.setId(idGenerator.generateNewId(IdType.DOI_ID));
		doi.setObjectVersion(1L);
		// Method under test
		DBODoi clone2 = dboBasicDao.createNew(doi);
		toDelete.add(clone2.getId());

		doi.setId(idGenerator.generateNewId(IdType.DOI_ID));
		doi.setObjectVersion(3L);

		// Method under test
		DBODoi clone3 = dboBasicDao.createNew(doi);
		toDelete.add(clone3.getId());
	}

	@Test
	public void translateToNonNullVersion() {
		DBODoi doi = new DBODoi();
		doi.setId(idGenerator.generateNewId(IdType.DOI_ID));
		doi.setCreatedOn(Timestamp.valueOf(LocalDateTime.now()));
		doi.setUpdatedOn(Timestamp.valueOf(LocalDateTime.now()));
		doi.setCreatedBy(userId);
		doi.setETag("some etag");
		doi.setDoiStatus(DoiStatus.IN_PROCESS);
		doi.setObjectId(1234L);
		doi.setObjectType(ObjectType.ENTITY);
		doi.setObjectVersion(null);

		// Method under test
		DBODoi newDoi = doi.getTranslator().createDatabaseObjectFromBackup(doi);
		assertEquals((Long) (-1L), newDoi.getObjectVersion());
		assertEquals(doi.getCreatedBy(), newDoi.getCreatedBy());
		assertEquals(doi.getObjectId(), newDoi.getObjectId());
		assertEquals(doi.getObjectType(), newDoi.getObjectType());
		assertEquals(doi.getETag(), newDoi.getETag());
		assertEquals(doi.getDoiStatus(), newDoi.getDoiStatus());
		assertEquals(doi.getCreatedOn(), newDoi.getCreatedOn());
		assertEquals(doi.getUpdatedOn(), newDoi.getUpdatedOn());
	}

	@Test
	public void translateToNullVersionBackup() {
		DBODoi doi = new DBODoi();
		doi.setId(idGenerator.generateNewId(IdType.DOI_ID));
		doi.setCreatedOn(Timestamp.valueOf(LocalDateTime.now()));
		doi.setUpdatedOn(Timestamp.valueOf(LocalDateTime.now()));
		doi.setCreatedBy(userId);
		doi.setETag("some etag");
		doi.setDoiStatus(DoiStatus.IN_PROCESS);
		doi.setObjectId(1234L);
		doi.setObjectType(ObjectType.ENTITY);
		doi.setObjectVersion((-1L));
		// Method under test
		DBODoi backup = doi.getTranslator().createBackupFromDatabaseObject(doi);
		assertEquals(null, backup.getObjectVersion());
		assertEquals(doi.getCreatedBy(), backup.getCreatedBy());
		assertEquals(doi.getObjectId(), backup.getObjectId());
		assertEquals(doi.getObjectType(), backup.getObjectType());
		assertEquals(doi.getETag(), backup.getETag());
		assertEquals(doi.getDoiStatus(), backup.getDoiStatus());
		assertEquals(doi.getCreatedOn(), backup.getCreatedOn());
		assertEquals(doi.getUpdatedOn(), backup.getUpdatedOn());
	}
}
