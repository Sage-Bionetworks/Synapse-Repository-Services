package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalStorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOStorageLocationDAOImplTest {

	@Autowired
	StorageLocationDAO storageLocationDAO;

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

	private void doTestCRUD(StorageLocationSetting locationSetting) throws Exception {
		locationSetting.setDescription("description");
		locationSetting.setCreatedBy(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		locationSetting.setCreatedOn(new Date());
		Long id = storageLocationDAO.create(locationSetting);

		StorageLocationSetting clone = storageLocationDAO.get(id);
		assertEquals(locationSetting.getClass(), clone.getClass());
		assertEquals(locationSetting.getDescription(), clone.getDescription());

		List<StorageLocationSetting> byOwner = storageLocationDAO.getByOwner(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		assertTrue(byOwner.contains(clone));
	}
}
