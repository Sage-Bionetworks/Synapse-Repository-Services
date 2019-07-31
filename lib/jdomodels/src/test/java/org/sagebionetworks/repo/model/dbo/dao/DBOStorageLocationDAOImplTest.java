package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOStorageLocation;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalStorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.util.TemporaryCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOStorageLocationDAOImplTest {

	@Autowired
	StorageLocationDAO storageLocationDAO;

	List<Long> toDelete;

	@Before
	public void before() {
		toDelete = new LinkedList<>();
	}

	@After
	public void after() {
		toDelete.forEach(storageLocationDAO::delete);
	}

	@Test
	public void testCRUD1() throws Exception {
		ExternalStorageLocationSetting locationSetting = new ExternalStorageLocationSetting();
		locationSetting.setUploadType(UploadType.SFTP);
		locationSetting.setUrl("sftp://");
		doTestCRUD(locationSetting);
	}

	@Test
	public void testCRUD2() throws Exception {
		ExternalS3StorageLocationSetting locationSetting = new ExternalS3StorageLocationSetting();
		locationSetting.setUploadType(UploadType.S3);
		locationSetting.setBucket("bucket");
		doTestCRUD(locationSetting);
	}

	@Test
	public void testIdempotentCreation() throws Exception {
		ExternalS3StorageLocationSetting locationSetting = new ExternalS3StorageLocationSetting();
		locationSetting.setUploadType(UploadType.S3);
		locationSetting.setBucket("bucket");
		locationSetting.setDescription("Some description");
		locationSetting.setCreatedBy(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		locationSetting.setCreatedOn(new Date());

		Long id = storageLocationDAO.create(locationSetting);
		Long sameId = storageLocationDAO.create(locationSetting);

		toDelete.add(id);

		assertEquals(id, sameId);
	}

	@Test
	public void testFindAllWithDuplicates() throws Exception {
		List<Long> ids = createStorageLocationsWithSameHash(2);
		Long firstCreatedId = ids.get(0);
		Long lastCreatedId = ids.get(1);
		
		// Call under test
		List<Long> withDuplicates = storageLocationDAO.findAllWithDuplicates();

		assertFalse("Unexpected id " + firstCreatedId, withDuplicates.contains(firstCreatedId));
		assertTrue("Expected id " + lastCreatedId + ", not found: " + withDuplicates.toString(), withDuplicates.contains(lastCreatedId));
	}

	@Test
	public void testFindDuplicates() throws Exception {
		List<Long> ids = createStorageLocationsWithSameHash(2);
		
		Long firstCreatedId = ids.get(0);
		Long lastCreatedId = ids.get(1);

		// Call under test
		Set<Long> duplicates = storageLocationDAO.findDuplicates(lastCreatedId);

		assertEquals(1, duplicates.size());
		assertTrue("Expected id " + firstCreatedId + ", not found: " + duplicates.toString(), duplicates.contains(firstCreatedId));

	}

	@Autowired
	@TemporaryCode(author = "marco.marasca@sagebase.org")
	private DBOBasicDao basicDao;

	@Autowired
	@TemporaryCode(author = "marco.marasca@sagebase.org")
	private IdGenerator idGenerator;

	private List<Long> createStorageLocationsWithSameHash(int n) throws Exception {

		List<Long> createdIds = new ArrayList<>();

		String hash = UUID.randomUUID().toString();

		for (int i = 0; i < n; i++) {
			ExternalStorageLocationSetting locationSetting = TestUtils.createExternalStorageLocation(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId(), "Description");
			Long id = createStorageLocationWithHash(locationSetting, hash);
			createdIds.add(id);
		}

		return createdIds;
	}

	private Long createStorageLocationWithHash(StorageLocationSetting setting, String hash) throws Exception {
		DBOStorageLocation dbo = StorageLocationUtils.convertDTOtoDBO(setting);
		dbo.setDataHash(hash);
		if (dbo.getId() == null) {
			dbo.setId(idGenerator.generateNewId(IdType.STORAGE_LOCATION_ID));
		}
		if (dbo.getEtag() == null) {
			dbo.setEtag(UUID.randomUUID().toString());
		}
		Long id = basicDao.createNew(dbo).getId();
		toDelete.add(id);
		return id;
	}

	private void doTestCRUD(StorageLocationSetting locationSetting) throws Exception {
		locationSetting.setDescription("description");
		locationSetting.setCreatedBy(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		locationSetting.setCreatedOn(new Date());
		Long id = storageLocationDAO.create(locationSetting);

		toDelete.add(id);

		StorageLocationSetting clone = storageLocationDAO.get(id);
		assertEquals(locationSetting.getClass(), clone.getClass());
		assertEquals(locationSetting.getDescription(), clone.getDescription());

		List<StorageLocationSetting> byOwner = storageLocationDAO
				.getByOwner(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		assertTrue(byOwner.contains(clone));
	}
}
