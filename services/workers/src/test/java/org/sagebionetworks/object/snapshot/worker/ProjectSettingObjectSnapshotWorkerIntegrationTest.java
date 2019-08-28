package org.sagebionetworks.object.snapshot.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Date;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ProjectSettingsDAO;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.workers.util.aws.message.QueueCleaner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-context.xml"})
public class ProjectSettingObjectSnapshotWorkerIntegrationTest {
	private static final String QUEUE_NAME = "OBJECT";

	@Autowired
	private NodeDAO nodeDao;
	@Autowired
	ProjectSettingsDAO projectSettingsDao;
	@Autowired
	private ObjectRecordDAO objectRecordDAO;
	@Autowired
	private QueueCleaner queueCleaner;

	private String projectId;

	private String type;

	@Before
	public void before() {
		assertNotNull(nodeDao);
		assertNotNull(objectRecordDAO);
		assertNotNull(queueCleaner);

		Long userId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();

		Node project = new Node();
		project.setName("project");
		project.setNodeType(EntityType.project);
		project.setCreatedByPrincipalId(userId);
		project.setCreatedOn(new Date());
		project.setModifiedByPrincipalId(userId);
		project.setModifiedOn(new Date());
		projectId = nodeDao.createNew(project);

		type = UploadDestinationListSetting.class.getSimpleName().toLowerCase();
		queueCleaner.purgeQueue(StackConfigurationSingleton.singleton().getQueueName(QUEUE_NAME));
	}

	@After
	public void teardown() throws Exception {
		if (projectId != null) {
			nodeDao.delete(projectId);
		}
	}

	@Test
	public void test() throws Exception {
		Set<String> keys = ObjectSnapshotWorkerIntegrationTestUtils.listAllKeys(objectRecordDAO, type);

		// Create a new type
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setEtag("etag");
		setting.setId(null);
		setting.setProjectId(projectId);
		setting.setSettingsType(ProjectSettingsType.upload);

		// there should not be a settings to begin with
		assertFalse(projectSettingsDao.get(projectId, ProjectSettingsType.upload).isPresent());
		assertEquals(0, projectSettingsDao.getAllForProject(projectId).size());

		// Create it
		String id = projectSettingsDao.create(setting);
		setting.setId(id);
		assertNotNull(id);

		// Fetch it by id
		UploadDestinationListSetting clone = (UploadDestinationListSetting) projectSettingsDao.get(id);
		assertEquals(setting, clone);

		ObjectRecord expectedRecord = new ObjectRecord();
		expectedRecord.setJsonClassName(setting.getClass().getSimpleName().toLowerCase());
		expectedRecord.setJsonString(EntityFactory.createJSONStringForEntity(setting));

		assertTrue(ObjectSnapshotWorkerIntegrationTestUtils.waitForObjects(keys, Arrays.asList(expectedRecord), objectRecordDAO, type));

	}

}
