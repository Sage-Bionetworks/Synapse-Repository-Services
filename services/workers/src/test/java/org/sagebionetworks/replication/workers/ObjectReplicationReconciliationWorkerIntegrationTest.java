package org.sagebionetworks.replication.workers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.evaluation.EvaluationManager;
import org.sagebionetworks.repo.manager.evaluation.SubmissionManager;
import org.sagebionetworks.repo.manager.replication.ReplicationManager;
import org.sagebionetworks.repo.manager.table.TableViewManager;
import org.sagebionetworks.repo.manager.table.metadata.DefaultColumnModelMapper;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.entitybundle.v2.EntityBundle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.helper.FileHandleObjectHelper;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.Dataset;
import org.sagebionetworks.repo.model.table.DatasetItem;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.ObjectField;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.model.table.SubmissionView;
import org.sagebionetworks.repo.model.table.View;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.model.table.ViewScope;
import org.sagebionetworks.repo.model.table.ViewType;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.Lists;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class ObjectReplicationReconciliationWorkerIntegrationTest {

	private static final int MAX_WAIT_MS = 2 * 60 * 1000;

	@Autowired
	private EntityManager entityManager;
	@Autowired
	private ConnectionFactory tableConnectionFactory;
	@Autowired
	UserManager userManager;
	@Autowired
	private TableViewManager viewManager;
	@Autowired
	private DefaultColumnModelMapper modelMapper;
	@Autowired
	private ReplicationManager replicationManager;
	@Mock
	private ProgressCallback mockProgressCallback;
	@Autowired
	private EvaluationManager evaluationManager;
	@Autowired
	private SubmissionManager submissionManager;
	@Autowired
	private FileHandleObjectHelper fileHandleDaoHelper;

	private TableIndexDAO indexDao;

	private UserInfo adminUserInfo;
	private Project project;

	private String projectId;
	private Long projectIdLong;

	@BeforeEach
	public void before() throws Exception {
		evaluationManager.truncateAll();
		entityManager.truncateAll();
		fileHandleDaoHelper.truncateAll();
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());

		project = new Project();
		project.setName(UUID.randomUUID().toString());
		projectId = entityManager.createEntity(adminUserInfo, project, null);
		project = entityManager.getEntity(adminUserInfo, projectId, Project.class);
		projectIdLong = KeyFactory.stringToKey(projectId);
		indexDao = tableConnectionFactory.getAllConnections().get(0);
		// ensure a sycn can occur.
		indexDao.truncateReplicationSyncExpiration();
	}

	@AfterEach
	public void after() {
		evaluationManager.truncateAll();
		entityManager.truncateAll();
		fileHandleDaoHelper.truncateAll();
	}

	@Test
	public void testReconciliationWithEntityView() throws Exception {
		ReplicationType type = ReplicationType.ENTITY;
		// Add a folder to the project
		Folder folder = addHierarchyToProject();
		// wait for the folder to replicated
		ObjectDataDTO dto = waitForEntityDto(type, folder.getId());
		assertNotNull(dto);
		assertEquals(projectIdLong, dto.getBenefactorId());

		// Simulate out-of-synch by deleting the project's replication data
		indexDao.deleteObjectData(type, Lists.newArrayList(KeyFactory.stringToKey(folder.getId())));

		// ensure a sycn can occur.
		indexDao.truncateReplicationSyncExpiration();
		// by creating a view we trigger a reconcile.
		List<String> scope = Lists.newArrayList(project.getId());
		long viewTypeMask = 0x08;
		EntityView view = createEntityView(scope, viewTypeMask);

		// wait for reconciliation to restore the deleted data.
		waitForEntityDto(type, folder.getId());

		ViewObjectType viewObjectType = ViewObjectType.ENTITY;
		IdAndVersion viewId = IdAndVersion.parse(view.getId());
		// the replication must be synchronized at this point.
		assertTrue(replicationManager.isReplicationSynchronizedForView(viewObjectType, viewId));
	}

	@Test
	public void testReconciliationWithSubmissionView() throws Exception {
		ReplicationType type = ReplicationType.SUBMISSION;

		Evaluation evaluation = evaluationManager.createEvaluation(adminUserInfo,
				new Evaluation().setContentSource(projectId).setName("eval"));

		EntityBundle bundle = new EntityBundle();
		bundle.setEntity(project);
		bundle.setFileHandles(Collections.emptyList());

		Submission submission = submissionManager.createSubmission(adminUserInfo,
				new Submission().setEvaluationId(evaluation.getId()).setEntityId(projectId).setVersionNumber(1L),
				project.getEtag(), null, bundle);

		ObjectDataDTO dto = waitForEntityDto(type, submission.getId());
		assertNotNull(dto);

		// Simulate out-of-synch by deleting the submission data
		indexDao.deleteObjectData(type, Lists.newArrayList(KeyFactory.stringToKey(submission.getId())));

		// ensure a sycn can occur.
		indexDao.truncateReplicationSyncExpiration();
		// by creating a view we trigger a reconcile.
		List<String> scope = Lists.newArrayList(evaluation.getId());
		SubmissionView view = createSubmissionView(scope);

		// wait for reconciliation to restore the deleted data.
		waitForEntityDto(type, submission.getId());

		ViewObjectType viewObjectType = ViewObjectType.SUBMISSION;
		IdAndVersion viewId = IdAndVersion.parse(view.getId());
		// the replication must be synchronized at this point.
		assertTrue(replicationManager.isReplicationSynchronizedForView(viewObjectType, viewId));
	}

	@Test
	public void testReconciliationWithDataset() throws Exception {
		ReplicationType type = ReplicationType.ENTITY;
		FileHandle fileHandle = fileHandleDaoHelper.create((f) -> {
			f.setCreatedBy(adminUserInfo.getId().toString());
			f.setFileName("someFile");
			f.setContentSize(123L);
		});
		// Add a folder to the project
		String fileId = entityManager.createEntity(adminUserInfo,
				new FileEntity().setParentId(projectId).setName("aFile").setDataFileHandleId(fileHandle.getId()), null);

		// wait for the folder to replicated
		ObjectDataDTO dto = waitForEntityDto(type, fileId);
		assertNotNull(dto);
		assertEquals(projectIdLong, dto.getBenefactorId());

		// Simulate out-of-synch by deleting the project's replication data
		indexDao.deleteObjectData(type, Lists.newArrayList(KeyFactory.stringToKey(fileId)));

		// ensure a sycn can occur.
		indexDao.truncateReplicationSyncExpiration();

		List<DatasetItem> items = Arrays.asList(new DatasetItem().setEntityId(fileId).setVersionNumber(1L));
		Dataset dataset = createDataset(items);

		// wait for reconciliation to restore the deleted data.
		waitForEntityDto(type, fileId);

		ViewObjectType viewObjectType = ViewObjectType.DATASET;
		IdAndVersion viewId = IdAndVersion.parse(dataset.getId());
		// the replication must be synchronized at this point.
		assertTrue(replicationManager.isReplicationSynchronizedForView(viewObjectType, viewId));
	}

	/**
	 * With PLFM_5352, there are cases where the benefactor can be out-of-date in
	 * the entity replication table. The reconciliation process should detect and
	 * repair these cases.
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void testPLFM_5352() throws InterruptedException {
		ReplicationType type = ReplicationType.ENTITY;
		// Add a folder to the project
		Folder folder = addHierarchyToProject();
		// wait for the folder to replicated
		ObjectDataDTO dto = waitForEntityDto(type, folder.getId());
		assertNotNull(dto);
		assertEquals(projectIdLong, dto.getBenefactorId());

		// simulate a stale benefactor on the folder
		indexDao.deleteObjectData(type, Lists.newArrayList(KeyFactory.stringToKey(folder.getId())));
		dto.setBenefactorId(KeyFactory.stringToKey(folder.getId()));
		indexDao.addObjectData(type, Lists.newArrayList(dto));

		// ensure a sycn can occur.
		indexDao.truncateReplicationSyncExpiration();
		// by creating a view we trigger a reconcile.
		List<String> scope = Lists.newArrayList(project.getId());
		long viewTypeMask = 0x08;
		EntityView view = createEntityView(scope, viewTypeMask);
		IdAndVersion.parse(view.getId());

		// Wait for the benefactor to be fixed
		Long expectedBenefactor = projectIdLong;
		waitForEntityDto(type, folder.getId(), expectedBenefactor);

		ViewObjectType viewObjectType = ViewObjectType.ENTITY;
		IdAndVersion viewId = IdAndVersion.parse(view.getId());
		// the replication must be synchronized at this point.
		assertTrue(replicationManager.isReplicationSynchronizedForView(viewObjectType, viewId));
	}

	EntityView createEntityView(List<String> scopeIds, long viewTypeMask) {
		EntityView view = new EntityView();
		view.setName(UUID.randomUUID().toString());
		view.setScopeIds(scopeIds);
		view.setViewTypeMask(viewTypeMask);
		view.setParentId(projectId);
		return createView(ViewObjectType.ENTITY, view, scopeIds, viewTypeMask);
	}
	
	Dataset createDataset(List<DatasetItem> items) {
		Dataset dataset = new Dataset();
		dataset.setName(UUID.randomUUID().toString());
		dataset.setParentId(projectId);
		dataset.setItems(items);
		List<String> scopeIds = items.stream().map(i-> i.getEntityId()).collect(Collectors.toList());
		return createView(ViewObjectType.DATASET, dataset, scopeIds, 0L);
	}

	SubmissionView createSubmissionView(List<String> scopeIds) {
		SubmissionView view = new SubmissionView();
		view.setName(UUID.randomUUID().toString());
		view.setScopeIds(scopeIds);
		view.setParentId(projectId);
		return createView(ViewObjectType.SUBMISSION, view, scopeIds, 0L);
	}

	<T extends View> T createView(ViewObjectType type, T view, List<String> scopeIds, long viewTypeMask) {
		List<ColumnModel> cm = modelMapper.getColumnModels(type, ObjectField.name);
		view.setColumnIds(Lists.newArrayList(cm.get(0).getId()));
		String activityId = null;
		String viewId = entityManager.createEntity(adminUserInfo, view, activityId);
		view = (T) entityManager.getEntity(adminUserInfo, viewId, view.getClass());
		ViewScope scope = new ViewScope();
		scope.setViewEntityType(type.getViewEntityType());
		scope.setScope(scopeIds);
		scope.setViewType(ViewType.file);
		scope.setViewTypeMask(viewTypeMask);
		viewManager.setViewSchemaAndScope(adminUserInfo, view.getColumnIds(), scope, view.getId());
		return view;
	}

	/**
	 * Add a folder and a file to the project. hierarchy
	 * 
	 * @return
	 */
	public Folder addHierarchyToProject() {
		// Add a folder to the project
		Folder folder = new Folder();
		folder.setName(UUID.randomUUID().toString());
		folder.setParentId(projectId);
		String folderIdString = entityManager.createEntity(adminUserInfo, folder, null);
		folder = entityManager.getEntity(adminUserInfo, folderIdString, Folder.class);

		// add a child folder
		Folder child = new Folder();
		child.setName(UUID.randomUUID().toString());
		child.setParentId(folder.getId());
		String childId = entityManager.createEntity(adminUserInfo, child, null);
		return entityManager.getEntity(adminUserInfo, childId, Folder.class);
	}

	/**
	 * Helper to wait for an entity's replication data to appear.
	 * 
	 * @param entityId
	 * @return
	 * @throws InterruptedException
	 */
	public ObjectDataDTO waitForEntityDto(ReplicationType type, String objectId) throws InterruptedException {
		Long expectedBenefactor = null;
		return waitForEntityDto(type, objectId, expectedBenefactor);
	}

	/**
	 * Helper to wait for an entity's replication data to appear.
	 * 
	 * @param entityId
	 * @param When     not null, will wait for the given entity benefactor to match
	 *                 the provided value.
	 * @return
	 * @throws InterruptedException
	 */
	public ObjectDataDTO waitForEntityDto(ReplicationType type, String objectId, Long expectedBenefactor)
			throws InterruptedException {
		long startTimeMS = System.currentTimeMillis();
		while (true) {
			ObjectDataDTO entityDto = indexDao.getObjectDataForCurrentVersion(type, KeyFactory.stringToKey(objectId));
			if (entityDto != null) {
				if (expectedBenefactor == null || expectedBenefactor.equals(entityDto.getBenefactorId())) {
					return entityDto;
				}
			}
			System.out.println("Waiting for object data to be replicated for id: " + objectId);
			Thread.sleep(2000);
			assertTrue(System.currentTimeMillis() - startTimeMS < MAX_WAIT_MS,
					"Timed-out waiting for entity data to be replicated.");
		}
	}
}
