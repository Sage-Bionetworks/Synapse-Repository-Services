package org.sagebionetworks.repo.manager;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.project.ExternalUploadDestinationSetting;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationSetting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

/**
 * Tests message access requirement checking and the sending of messages Note: only the logic for sending messages is
 * tested, a separate test handles tests of sending emails
 * 
 * Sorting of messages is not tested. All tests order their results as most recent first.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class ProjectSettingsImplTest {

	@Autowired
	private ProjectSettingsManager projectSettingsManager;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private UserManager userManager;

	private UserInfo adminUserInfo;
	private String projectId;
	private String childId;
	private String childChildId;

	@Before
	public void setUp() throws Exception {
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		Project project = new Project();
		project.setName("project" + RandomStringUtils.randomAlphanumeric(10));
		projectId = entityManager.createEntity(adminUserInfo, project, null);

		Folder child = new Folder();
		child.setName("child");
		child.setParentId(projectId);
		childId = entityManager.createEntity(adminUserInfo, child, null);

		Folder childChild = new Folder();
		childChild.setName("child");
		childChild.setParentId(childId);
		childChildId = entityManager.createEntity(adminUserInfo, childChild, null);
	}

	@After
	public void tearDown() throws Exception {
		entityManager.deleteEntity(adminUserInfo, childChildId);
		entityManager.deleteEntity(adminUserInfo, childId);
		entityManager.deleteEntity(adminUserInfo, projectId);
	}

	@Test
	public void testCRUD() throws Exception {
		UploadDestinationListSetting toCreate = new UploadDestinationListSetting();
		toCreate.setProjectId(projectId);
		toCreate.setSettingsType(ProjectSettingsType.UPLOAD);
		toCreate.setDestinations(Lists.<UploadDestinationSetting> newArrayList(new ExternalUploadDestinationSetting()));
		ExternalUploadDestinationSetting s1 = ((ExternalUploadDestinationSetting) toCreate.getDestinations().get(0));
		s1.setUrl("sftp://url");
		s1.setUploadType(UploadType.SFTP);
		ProjectSetting settings = projectSettingsManager.createProjectSetting(adminUserInfo, toCreate);
		assertTrue(settings instanceof UploadDestinationListSetting);

		ProjectSetting copy = projectSettingsManager.getProjectSetting(adminUserInfo, settings.getId());
		assertEquals(settings, copy);

		ExternalUploadDestinationSetting s2 = ((ExternalUploadDestinationSetting) ((UploadDestinationListSetting) settings).getDestinations().get(0));
		s2.setUrl("sftp://url");
		s2.setUploadType(UploadType.SFTP);
		projectSettingsManager.updateProjectSetting(adminUserInfo, settings);
		copy = projectSettingsManager.getProjectSetting(adminUserInfo, settings.getId());
		assertNotSame(settings, copy);
		settings.setEtag(copy.getEtag());
		assertEquals(settings, copy);

		projectSettingsManager.deleteProjectSetting(adminUserInfo, settings.getId());
	}

	@Test
	public void testFind() throws Exception {
		UploadDestinationListSetting toCreate = new UploadDestinationListSetting();
		toCreate.setProjectId(projectId);
		toCreate.setSettingsType(ProjectSettingsType.UPLOAD);
		toCreate.setDestinations(Lists.<UploadDestinationSetting> newArrayList(new ExternalUploadDestinationSetting()));
		ExternalUploadDestinationSetting s = ((ExternalUploadDestinationSetting) toCreate.getDestinations().get(0));
		s.setUrl("https://url");
		s.setUploadType(UploadType.HTTPS);
		projectSettingsManager.createProjectSetting(adminUserInfo, toCreate);

		UploadDestinationListSetting setting = projectSettingsManager.getProjectSettingForParent(adminUserInfo, projectId,
				ProjectSettingsType.UPLOAD, UploadDestinationListSetting.class);
		assertEquals("https://url", ((ExternalUploadDestinationSetting) setting.getDestinations().get(0)).getUrl());

		setting = projectSettingsManager.getProjectSettingForParent(adminUserInfo, childId, ProjectSettingsType.UPLOAD,
				UploadDestinationListSetting.class);
		assertEquals("https://url", ((ExternalUploadDestinationSetting) setting.getDestinations().get(0)).getUrl());

		setting = projectSettingsManager.getProjectSettingForParent(adminUserInfo, childChildId, ProjectSettingsType.UPLOAD,
				UploadDestinationListSetting.class);
		assertEquals("https://url", ((ExternalUploadDestinationSetting) setting.getDestinations().get(0)).getUrl());
	}
}
