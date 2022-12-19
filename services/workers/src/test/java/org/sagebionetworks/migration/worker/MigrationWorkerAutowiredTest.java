package org.sagebionetworks.migration.worker;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.SemaphoreManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityRef;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.dbo.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.helper.DaoObjectHelper;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRangeChecksumRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationResponse;
import org.sagebionetworks.repo.model.migration.DatasetBackfillingRequest;
import org.sagebionetworks.repo.model.migration.DatasetBackfillingResponse;
import org.sagebionetworks.repo.model.migration.MigrationRangeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Dataset;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.worker.TestHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class MigrationWorkerAutowiredTest {

	public static final int MAX_WAIT_MS = 1000 * 60;

	@Autowired
	AsynchJobStatusManager asynchJobStatusManager;
	@Autowired
	SemaphoreManager semphoreManager;
	@Autowired
	UserManager userManager;
	@Autowired
	StackStatusDao stackStatusDao;
	@Autowired
	private AsynchronousJobWorkerHelper asyncHelper;
	@Autowired
	private TestHelper testHelper;
	@Autowired
	private ColumnModelManager columnModelManager;
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private DaoObjectHelper<S3FileHandle> fileHandleDaoHelper;
	@Autowired
	private TableRowTruthDAO tableRowTruthDao;
	@Autowired
	private TableIndexDAO indexDao;
	@Autowired
	private FileHandleDao fileHandleDao;

	private UserInfo adminUserInfo;
	private UserInfo user;
	private Project project;
	private ColumnModel stringColumnModel;

	@BeforeEach
	public void before() throws NotFoundException {
		semphoreManager.releaseAllLocksAsAdmin(new UserInfo(true));
		// Start with an empty queue.
		asynchJobStatusManager.emptyAllQueues();
		// Get the admin user
		adminUserInfo = userManager
				.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER
						.getPrincipalId());

		StackStatus status = new StackStatus();
		status.setStatus(StatusEnum.READ_ONLY);
		stackStatusDao.updateStatus(status);
	}

	@AfterEach
	public void after() throws Exception {
		StackStatus status = new StackStatus();
		status.setStatus(StatusEnum.READ_WRITE);
		stackStatusDao.updateStatus(status);
	}

	@Test
	public void testRoundtrip() throws Exception {
		AsyncMigrationRangeChecksumRequest req = new AsyncMigrationRangeChecksumRequest();
		req.setMinId(0L);
		req.setMaxId(Long.MAX_VALUE);
		req.setSalt("salt");
		req.setMigrationType(MigrationType.NODE);
		AsyncMigrationRequest request = new AsyncMigrationRequest();
		request.setAdminRequest(req);
		AsynchronousJobStatus status = asynchJobStatusManager.startJob(adminUserInfo, request);
		status = waitForStatus(adminUserInfo, status);
		assertNotNull(status);
		assertNotNull(status.getResponseBody());
		AsynchronousResponseBody resp = status.getResponseBody();
		assertTrue(resp instanceof AsyncMigrationResponse);
		AsyncMigrationResponse aResp = (AsyncMigrationResponse)resp;
		assertNotNull(aResp.getAdminResponse());
		assertTrue(aResp.getAdminResponse() instanceof MigrationRangeChecksum);
		MigrationRangeChecksum checksum = (MigrationRangeChecksum)aResp.getAdminResponse();
		assertTrue(0 == checksum.getMinid());
		assertTrue(Long.MAX_VALUE == checksum.getMaxid());
		assertNotNull(checksum.getChecksum());
	}

	private AsynchronousJobStatus waitForStatus(UserInfo user,
			AsynchronousJobStatus status) throws InterruptedException,
			DatastoreException, NotFoundException {
		long start = System.currentTimeMillis();
		while (!AsynchJobState.COMPLETE.equals(status.getJobState())) {
			Assertions.assertFalse(AsynchJobState.FAILED.equals(status.getJobState()), "Job Failed: " + status.getErrorDetails());
			assertTrue((System.currentTimeMillis() - start) < MAX_WAIT_MS, "Timed out waiting for table status");
			Thread.sleep(1000);
			// Get the status again
			status = this.asynchJobStatusManager.getJobStatus(user,
					status.getJobId());
		}
		return status;
	}

	@Test
	public void testDatasetBackfillingJob() throws Exception {
		List<Dataset> datasetToBeDelete = new ArrayList<>();
		try {
			beforeDatasetBackfillingJob();
			Dataset datasetWithoutItem = asyncHelper.createDataset(user, new Dataset()
					.setParentId(project.getId())
					.setName(UUID.randomUUID().toString())
					.setDescription(UUID.randomUUID().toString())
					.setColumnIds(Collections.singletonList(stringColumnModel.getId()))
					.setItems(Collections.emptyList())
			);
			assertNull(datasetWithoutItem.getChecksum());
			assertEquals(0, datasetWithoutItem.getSize());
			assertEquals(0, datasetWithoutItem.getCount());
			datasetToBeDelete.add(datasetWithoutItem);

			Dataset datasetWithoutFileSummary = createDatasetWithoutFileSummary(user, project, stringColumnModel);
			assertNull(datasetWithoutFileSummary.getChecksum());
			assertNull(datasetWithoutFileSummary.getSize());
			assertNull(datasetWithoutFileSummary.getCount());
			datasetToBeDelete.add(datasetWithoutFileSummary);

			Dataset datasetWithFileSummary = createDatasetWithFileSummary(user, project, stringColumnModel);
			assertNotNull(datasetWithFileSummary.getChecksum());
			assertEquals(2, datasetWithFileSummary.getCount());
			datasetToBeDelete.add(datasetWithFileSummary);

			DatasetBackfillingRequest req = new DatasetBackfillingRequest();
			AsyncMigrationRequest request = new AsyncMigrationRequest();
			request.setAdminRequest(req);
			AsynchronousJobStatus status = asynchJobStatusManager.startJob(adminUserInfo, request);
			status = waitForStatus(adminUserInfo, status);
			assertNotNull(status);
			AsynchronousResponseBody resp = status.getResponseBody();
			AsyncMigrationResponse aResp = (AsyncMigrationResponse) resp;
			assertTrue(aResp.getAdminResponse() instanceof DatasetBackfillingResponse);
			DatasetBackfillingResponse response = (DatasetBackfillingResponse) aResp.getAdminResponse();
			assertEquals(1d, response.getCount());

			Dataset updateDataset = entityManager.getEntity(user, datasetWithoutFileSummary.getId(), Dataset.class);
			assertNotNull(updateDataset.getChecksum());
			assertNotNull(updateDataset.getSize());
			assertNotNull(updateDataset.getCount());

		} finally {
			afterDatasetBackfillingJob(datasetToBeDelete);
		}
	}

	public void beforeDatasetBackfillingJob() {
		tableRowTruthDao.truncateAllRowData();
		testHelper.before();
		user = testHelper.createUser();
		project = testHelper.createProject(user);
		stringColumnModel = new ColumnModel();
		stringColumnModel.setName("aString");
		stringColumnModel.setColumnType(ColumnType.STRING);
		stringColumnModel.setMaximumSize(50L);
		stringColumnModel = columnModelManager.createColumnModel(user, stringColumnModel);
	}

	public void afterDatasetBackfillingJob(List<Dataset> datasetToBeDeleted) {
		tableRowTruthDao.truncateAllRowData();
		testHelper.cleanup();
		datasetToBeDeleted.forEach(dataset -> indexDao.deleteTable(IdAndVersion.parse(dataset.getId())));

		fileHandleDao.truncateTable();
	}


	private Dataset createDatasetWithFileSummary(UserInfo userInfo, Project project, ColumnModel columnModel) throws Exception {
		FileEntity fileOne = createFileEntityAndWaitForReplication(userInfo, project);
		FileEntity fileTwo = createFileEntityAndWaitForReplication(userInfo, project);

		Dataset dataset = asyncHelper.createDataset(userInfo, new Dataset()
				.setParentId(project.getId())
				.setName(UUID.randomUUID().toString())
				.setDescription(UUID.randomUUID().toString())
				.setColumnIds(Arrays.asList(columnModel.getId()))
				.setItems(List.of(
						new EntityRef().setEntityId(fileOne.getId()).setVersionNumber(fileOne.getVersionNumber()),
						new EntityRef().setEntityId(fileTwo.getId()).setVersionNumber(fileTwo.getVersionNumber())
				))
		);
		return dataset;
	}

	private Dataset createDatasetWithoutFileSummary(UserInfo userInfo, Project project, ColumnModel columnModel) throws Exception {
		FileEntity fileOne = createFileEntityAndWaitForReplication(userInfo, project);
		FileEntity fileTwo = createFileEntityAndWaitForReplication(userInfo, project);

		Dataset dataset = asyncHelper.createDatasetWithoutFileSummary(userInfo, new Dataset()
				.setParentId(project.getId())
				.setName(UUID.randomUUID().toString())
				.setDescription(UUID.randomUUID().toString())
				.setColumnIds(Arrays.asList(columnModel.getId()))
				.setItems(List.of(
						new EntityRef().setEntityId(fileOne.getId()).setVersionNumber(fileOne.getVersionNumber()),
						new EntityRef().setEntityId(fileTwo.getId()).setVersionNumber(fileTwo.getVersionNumber())
				))
		);
		return dataset;
	}

	private FileEntity createFileEntityAndWaitForReplication(UserInfo userInfo, Project project) throws Exception {
		String fileEntityId = entityManager.createEntity(userInfo, new FileEntity()
						.setName(UUID.randomUUID().toString())
						.setParentId(project.getId())
						.setDataFileHandleId(fileHandleDaoHelper.create((f) -> {
							f.setCreatedBy(userInfo.getId().toString());
							f.setFileName(UUID.randomUUID().toString());
							f.setContentSize(128_000L);
						}).getId()),
				null);

		FileEntity fileEntity = entityManager.getEntity(userInfo, fileEntityId, FileEntity.class);

		asyncHelper.waitForObjectReplication(ReplicationType.ENTITY, KeyFactory.stringToKey(fileEntity.getId()), fileEntity.getEtag(), MAX_WAIT_MS);

		return fileEntity;
	}
}
