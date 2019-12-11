package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalStorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOStorageLocationDAOImplTest {

	@Autowired
	StorageLocationDAO storageLocationDAO;

	List<Long> toDelete;

	@BeforeEach
	public void before() {
		toDelete = new LinkedList<>();
	}

	@AfterEach
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
	public void testDefaultUploadType() throws Exception {
		ExternalStorageLocationSetting locationSetting = new ExternalStorageLocationSetting();
		locationSetting.setUploadType(null);
		StorageLocationSetting result = doTestCRUD(locationSetting);
		assertEquals(UploadType.NONE, result.getUploadType());
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

	private StorageLocationSetting doTestCRUD(StorageLocationSetting locationSetting) throws Exception {
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

		return clone;
	}
}
