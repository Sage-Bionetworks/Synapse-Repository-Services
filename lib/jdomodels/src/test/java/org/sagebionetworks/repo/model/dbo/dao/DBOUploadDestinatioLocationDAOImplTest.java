package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ProjectSettingsDAO;
import org.sagebionetworks.repo.model.UploadDestinationLocationDAO;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.project.ExternalS3UploadDestinationLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalUploadDestinationLocationSetting;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationLocationSetting;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOUploadDestinatioLocationDAOImplTest {

	@Autowired
	UploadDestinationLocationDAO uploadDestinationLocationDAO;

	@Test
	public void testCRUD1() throws Exception {
		ExternalUploadDestinationLocationSetting locationSetting = new ExternalUploadDestinationLocationSetting();
		locationSetting.setUploadType(UploadType.SFTP);
		locationSetting.setUrl("sftp://");
		doTestCRUD(locationSetting);
	}

	@Test
	public void testCRUD2() throws Exception {
		ExternalS3UploadDestinationLocationSetting locationSetting = new ExternalS3UploadDestinationLocationSetting();
		locationSetting.setUploadType(UploadType.S3);
		locationSetting.setBucket("bucket");
		doTestCRUD(locationSetting);
	}

	private void doTestCRUD(UploadDestinationLocationSetting locationSetting) throws Exception {
		locationSetting.setDescription("description");
		Long id = uploadDestinationLocationDAO.create(locationSetting);

		UploadDestinationLocationSetting clone = uploadDestinationLocationDAO.get(id);
		assertEquals(locationSetting.getClass(), clone.getClass());
		assertEquals(locationSetting.getDescription(), clone.getDescription());

		clone.setDescription("new description");
		UploadDestinationLocationSetting updated = uploadDestinationLocationDAO.update(clone);
		assertEquals(clone.getDescription(), updated.getDescription());
		assertEquals(updated, uploadDestinationLocationDAO.get(id));
	}
}
