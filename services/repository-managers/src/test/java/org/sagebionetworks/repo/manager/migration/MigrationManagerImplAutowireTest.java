package org.sagebionetworks.repo.manager.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.ProjectSettingsManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.bootstrap.EntityBootstrapper;
import org.sagebionetworks.repo.model.daemon.BackupAliasType;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.dao.TestUtils;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRangeChecksumRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeChecksumRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountsRequest;
import org.sagebionetworks.repo.model.migration.BackupTypeRangeRequest;
import org.sagebionetworks.repo.model.migration.BackupTypeResponse;
import org.sagebionetworks.repo.model.migration.MigrationRangeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.MigrationTypeCounts;
import org.sagebionetworks.repo.model.migration.RestoreTypeRequest;
import org.sagebionetworks.repo.model.migration.RestoreTypeResponse;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class MigrationManagerImplAutowireTest {
	
	@Autowired
	private FileHandleDao fileHandleDao;
	
	@Autowired
	private UserManager userManager;	
	
	@Autowired
	private MigrationManager migrationManager;
	
	@Autowired
	private EntityBootstrapper entityBootstrapper;
	
	@Autowired
	ConnectionFactory tableConnectionFactory;

	@Autowired
	TableEntityManager tableEntityManager;
	
	@Autowired
	TableManagerSupport tableManagerSupport;

	@Autowired
	EntityManager entityManager;

	@Autowired
	ColumnModelManager columnManager;

	@Autowired
	ConnectionFactory connectionFactory;

	@Autowired
	ProjectSettingsManager projectSettingsManager;

	@Autowired
	NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Autowired
	FileHandleManager fileHandleManager;
	
	@Autowired
	StackStatusDao stackStatusDao;

	@Autowired
	IdGenerator idGenerator;

	private List<String> toDelete;
	private UserInfo adminUser;
	private String creatorUserGroupId;
	private S3FileHandle withPreview;
	private PreviewFileHandle preview;
	private long startCount;
	private String tableId;
	private String[] projectIds = new String[3];
	List<Project> projects;
	List<Long> projectIdsLong;
	ProgressCallback mockProgressCallback;
	ProgressCallback mockProgressCallbackVoid;
	private long startId;

	@Before
	public void before() throws Exception {
		mockProgressCallback = Mockito.mock(ProgressCallback.class);
		mockProgressCallbackVoid = Mockito.mock(ProgressCallback.class);
		toDelete = new LinkedList<String>();
		adminUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		creatorUserGroupId = adminUser.getId().toString();
		assertNotNull(creatorUserGroupId);
		startCount = fileHandleDao.getCount();
		
		// The one will have a preview
		withPreview = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		withPreview.setFileName("withPreview.txt");
		startId = Integer.parseInt(withPreview.getId());
		// The Preview
		preview = TestUtils.createPreviewFileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		preview.setFileName("preview.txt");

		List<FileHandle> fileHandleToCreate = new LinkedList<FileHandle>();
		fileHandleToCreate.add(preview);
		fileHandleToCreate.add(withPreview);
		fileHandleDao.createBatch(fileHandleToCreate);

		withPreview = (S3FileHandle) fileHandleDao.get(withPreview.getId());
		assertNotNull(withPreview);
		toDelete.add(withPreview.getId());
		preview = (PreviewFileHandle) fileHandleDao.get(preview.getId());
		assertNotNull(preview);
		toDelete.add(preview.getId());

		// Assign it as a preview
		fileHandleDao.setPreviewId(withPreview.getId(), preview.getId());
		// The etag should have changed
		withPreview = (S3FileHandle) fileHandleDao.get(withPreview.getId());

		projectIdsLong = new LinkedList<>();
		projects = new LinkedList<>();
		for (int i = 0; i < projectIds.length; i++) {
			Project project = new Project();
			project.setName(UUID.randomUUID().toString());
			String id = entityManager.createEntity(adminUser, project, null);
			projectIds[i] = id;
			projects.add(entityManager.getEntity(adminUser, id, Project.class));
			projectIdsLong.add(KeyFactory.stringToKey(id));
		}

		// create columns
		LinkedList<ColumnModel> schema = new LinkedList<ColumnModel>();
		for (ColumnModel cm : TableModelTestUtils.createOneOfEachType()) {
			cm = columnManager.createColumnModel(adminUser, cm);
			schema.add(cm);
		}
		List<String> headers = TableModelUtils.getIds(schema);
		// Create the table.
		TableEntity table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setColumnIds(headers);
		tableId = entityManager.createEntity(adminUser, table, null);
		tableEntityManager.setTableSchema(adminUser, headers, tableId);

		// Now add some data
		RowSet rowSet = new RowSet();
		rowSet.setRows(TableModelTestUtils.createRows(schema, 2));
		rowSet.setHeaders(TableModelUtils.getSelectColumns(schema));
		rowSet.setTableId(tableId);
		tableEntityManager.appendRows(adminUser, tableId, rowSet, mockProgressCallback);
	}
	
	@After
	public void after() throws Exception{
		// Since this test can delete all data make sure bootstrap data gets put back.
		entityBootstrapper.bootstrapAll();
		// If we have deleted all data make sure the bootstrap process puts it back
		if(fileHandleDao != null && toDelete != null){
			for(String id: toDelete){
				fileHandleDao.delete(id);
			}
		}
		try {
			entityManager.deleteEntity(adminUser, tableId);
		} catch (Exception e) {
		}
		for (String projectId : projectIds) {
			try {
				entityManager.deleteEntity(adminUser, projectId);
			} catch (Exception e) {
			}
		}
	}
	
	@Test
	public void testGetCount(){
		long count = migrationManager.getCount(adminUser, MigrationType.FILE_HANDLE);
		assertEquals(startCount+2, count);
	}
	
	@Test
	public void testGetMaxId() {
		long mx = migrationManager.getMaxId(adminUser, MigrationType.FILE_HANDLE);
		assertEquals(Long.parseLong(preview.getId()), mx);
		assertEquals(startId+1, mx);
	}
	
	@Test
	public void testGetMinId() {
		long min = migrationManager.getMinId(adminUser, MigrationType.FILE_HANDLE);
		long max = migrationManager.getMaxId(adminUser, MigrationType.FILE_HANDLE);
		assertTrue(min < max);
	}
	
	@Test
	public void testGetMigrationTypeCount() {
		MigrationTypeCount expectedCount = new MigrationTypeCount();
		expectedCount.setType(MigrationType.FILE_HANDLE);
		expectedCount.setMinid(migrationManager.getMinId(adminUser, MigrationType.FILE_HANDLE));
		expectedCount.setMaxid(migrationManager.getMaxId(adminUser, MigrationType.FILE_HANDLE));
		expectedCount.setCount(migrationManager.getCount(adminUser, MigrationType.FILE_HANDLE));
		MigrationTypeCount mtc = migrationManager.getMigrationTypeCount(adminUser, MigrationType.FILE_HANDLE);
		assertNotNull(mtc);
		assertEquals(expectedCount, mtc);
	}
	
	@Test
	public void testProcessAsyncMigrationTypeCount() {
		MigrationTypeCount expectedCount = new MigrationTypeCount();
		expectedCount.setType(MigrationType.FILE_HANDLE);
		expectedCount.setMinid(migrationManager.getMinId(adminUser, MigrationType.FILE_HANDLE));
		expectedCount.setMaxid(migrationManager.getMaxId(adminUser, MigrationType.FILE_HANDLE));
		expectedCount.setCount(migrationManager.getCount(adminUser, MigrationType.FILE_HANDLE));
		
		AsyncMigrationTypeCountRequest asyncMigrationTypeCountRequest = new AsyncMigrationTypeCountRequest();
		asyncMigrationTypeCountRequest.setType(MigrationType.FILE_HANDLE.name());
		
		MigrationTypeCount amtcRes = migrationManager.processAsyncMigrationTypeCountRequest(adminUser, asyncMigrationTypeCountRequest);
		
		assertNotNull(amtcRes);
		assertEquals(expectedCount, amtcRes);
	}

	@Test
	public void testProcessAsyncMigrationTypeCounts() {
		MigrationTypeCounts expectedCounts = new MigrationTypeCounts();
		MigrationTypeCount expectedCount = new MigrationTypeCount();
		expectedCount.setType(MigrationType.FILE_HANDLE);
		expectedCount.setMinid(migrationManager.getMinId(adminUser, MigrationType.FILE_HANDLE));
		expectedCount.setMaxid(migrationManager.getMaxId(adminUser, MigrationType.FILE_HANDLE));
		expectedCount.setCount(migrationManager.getCount(adminUser, MigrationType.FILE_HANDLE));
		List<MigrationTypeCount> l = new LinkedList<MigrationTypeCount>();
		l.add(expectedCount);
		expectedCounts.setList(l);

		AsyncMigrationTypeCountsRequest asyncMigrationTypeCountsRequest = new AsyncMigrationTypeCountsRequest();
		List<MigrationType> types = new LinkedList<MigrationType>();
		types.add(MigrationType.FILE_HANDLE);
		asyncMigrationTypeCountsRequest.setTypes(types);

		MigrationTypeCounts amtcRes = migrationManager.processAsyncMigrationTypeCountsRequest(adminUser, asyncMigrationTypeCountsRequest);

		assertNotNull(amtcRes);
		assertEquals(expectedCounts, amtcRes);
	}
	@Test
	public void testGetChecksumForIdRange() {
		long max = migrationManager.getMaxId(adminUser, MigrationType.FILE_HANDLE);
		String salt = "salt";
		MigrationRangeChecksum mrc = migrationManager.getChecksumForIdRange(adminUser, MigrationType.FILE_HANDLE, salt, 0L, max);
		assertNotNull(mrc);
		assertNotNull(mrc.getChecksum());
		assertTrue(mrc.getChecksum().contains("%"));
		assertEquals(MigrationType.FILE_HANDLE, mrc.getType());
		assertEquals(0L, mrc.getMinid().longValue());
		assertEquals(max, mrc.getMaxid().longValue());
		
	}
	
	@Test
	public void testProcessAsyncChecksumForRange() {
		long max = migrationManager.getMaxId(adminUser, MigrationType.FILE_HANDLE);
		String salt = "salt";
		MigrationRangeChecksum expectedRangeChecksum = migrationManager.getChecksumForIdRange(adminUser, MigrationType.FILE_HANDLE, salt, 0L, max);
		MigrationRangeChecksum expectedAsyncMigrationRangeChecksumResult = new MigrationRangeChecksum();
		AsyncMigrationRangeChecksumRequest asyncMigrationRangeChecksumRequest = new AsyncMigrationRangeChecksumRequest();
		asyncMigrationRangeChecksumRequest.setMaxId(max);
		asyncMigrationRangeChecksumRequest.setMinId(0L);
		asyncMigrationRangeChecksumRequest.setSalt("salt");
		asyncMigrationRangeChecksumRequest.setMigrationType(MigrationType.FILE_HANDLE);
		
		MigrationRangeChecksum acrcRes = migrationManager.processAsyncMigrationRangeChecksumRequest(adminUser, asyncMigrationRangeChecksumRequest);

		assertNotNull(acrcRes);
		assertTrue(acrcRes.getChecksum().contains("%"));
		assertEquals(MigrationType.FILE_HANDLE, acrcRes.getType());
		assertEquals(0L, acrcRes.getMinid().longValue());
		assertEquals(max, acrcRes.getMaxid().longValue());
		
	}
	
	@Test
	public void testGetChecksumForType() {
		try {
			setStackStatus(StatusEnum.READ_ONLY);
			
			MigrationTypeChecksum mtc = migrationManager.getChecksumForType(adminUser, MigrationType.FILE_HANDLE);
			assertNotNull(mtc);
			assertNotNull(mtc.getChecksum());
			assertEquals(MigrationType.FILE_HANDLE, mtc.getType());
		} finally {
			setStackStatus(StatusEnum.READ_WRITE);
		}
	}
	
	@Test
	public void testProcessAsyncChecksumForType() {
		try {
			setStackStatus(StatusEnum.READ_ONLY);
			
			MigrationTypeChecksum expectedMtc = migrationManager.getChecksumForType(adminUser, MigrationType.FILE_HANDLE);
			MigrationTypeChecksum expectedAsyncMtcRes = new MigrationTypeChecksum();
			AsyncMigrationTypeChecksumRequest amtcReq = new AsyncMigrationTypeChecksumRequest();
			amtcReq.setMigrationType(MigrationType.FILE_HANDLE);
			
			MigrationTypeChecksum amtcRes = migrationManager.processAsyncMigrationTypeChecksumRequest(adminUser, amtcReq);
			
			assertNotNull(amtcRes);
			assertEquals(MigrationType.FILE_HANDLE, amtcRes.getType());
			assertEquals(expectedMtc.getChecksum(), amtcRes.getChecksum());
		} finally {
			setStackStatus(StatusEnum.READ_WRITE);
		}
	}
	
	private void setStackStatus(StatusEnum s) {
		StackStatus sStatus = new StackStatus();
		sStatus.setStatus(s);
		sStatus.setCurrentMessage("Stack in " + s.name() + " mode");
		stackStatusDao.updateStatus(sStatus);
	}
	
	@Test
	public void testGetMigrationTypes() {
		List<MigrationType> expected = new LinkedList<MigrationType>(Arrays.asList(MigrationType.values()));
		List<MigrationType> actual = migrationManager.getMigrationTypes(adminUser);
		assertEquals(expected, actual);
	}

	@Test
	public void testGetMigrationTypeNames() {
		List<MigrationType> expectedTypes = new LinkedList<MigrationType>(Arrays.asList(MigrationType.values()));
		List<String> expectedTypeNames = new LinkedList<String>();
		for (MigrationType t: expectedTypes) {
			expectedTypeNames.add(t.name());
		}
		List<String> actual = migrationManager.getMigrationTypeNames(adminUser);
		assertEquals(expectedTypeNames, actual);
	}

	@Test
	public void testGetPrimaryTypeNames() {
		List<MigrationType> expectedTypes = migrationManager.getPrimaryMigrationTypes(adminUser);
		List<String> expectedTypeNames = new LinkedList<String>();
		for (MigrationType t: expectedTypes) {
			expectedTypeNames.add(t.name());
		}
		List<String> actual = migrationManager.getPrimaryMigrationTypeNames(adminUser);
		assertEquals(expectedTypeNames, actual);
	}

	@Test
	public void testGetSecondaryTypes(){
		// Node should have revision as a secondary.
		List<MigrationType> result = migrationManager.getSecondaryTypes(MigrationType.NODE);
		List<MigrationType> expected = new LinkedList<MigrationType>();
		expected.add(MigrationType.NODE_REVISION);
		assertEquals(expected, result);
		
		// file handles do not have secondary so null
		result = migrationManager.getSecondaryTypes(MigrationType.FILE_HANDLE);
		assertNotNull(result);
		assertTrue(result.isEmpty());
	}
	

	@Test
	public void testBackupAndRestoreByRange() throws IOException {
		MigrationType type =  MigrationType.NODE;
		BackupAliasType backupType = BackupAliasType.TABLE_NAME;
		long batchSize = 2;
		long minId = projectIdsLong.get(0);
		long maxId = projectIdsLong.get(projectIdsLong.size()-1);
		
		BackupTypeRangeRequest request = new BackupTypeRangeRequest();
		request.setMigrationType(type);
		request.setAliasType(backupType);
		request.setBatchSize(batchSize);
		request.setMinimumId(minId);
		// +1 since maxId is exclusive.
		request.setMaximumId(maxId+1);
		// call under test
		BackupTypeResponse backupResponse = migrationManager.backupRequest(adminUser, request);
		assertNotNull(backupResponse);
		// restore the data from the backup
		RestoreTypeRequest restoreRequest = new RestoreTypeRequest();
		restoreRequest.setMigrationType(type);
		restoreRequest.setAliasType(backupType);
		restoreRequest.setBatchSize(batchSize);
		restoreRequest.setBackupFileKey(backupResponse.getBackupFileKey());
		// call under test
		RestoreTypeResponse restoreReponse = migrationManager.restoreRequest(adminUser, restoreRequest);
		assertNotNull(restoreReponse);
		// each node and revision should be restored.
		assertEquals(new Long(projects.size()*2), restoreReponse.getRestoredRowCount());
		// validate all of the data was restored.
		validateProjectsRestored();
	}
	
	/**
	 * Validate all of the projects match what is in memory
	 */
	private void validateProjectsRestored() {
		for(Project project: projects) {
			Project projectClone = entityManager.getEntity(adminUser, project.getId(), Project.class);
			assertEquals(project, projectClone);
		}
	}
	
	/**
	 * PLFM_4829 - Backup and restore a range with no data.
	 * @throws IOException 
	 */
	@Test
	public void testPLFM_4829() throws IOException {
		MigrationType type =  MigrationType.NODE;
		BackupAliasType backupType = BackupAliasType.TABLE_NAME;
		long batchSize = 100;
		long minId = Long.MAX_VALUE-100;
		long maxId = Long.MAX_VALUE;
		
		BackupTypeRangeRequest request = new BackupTypeRangeRequest();
		request.setMigrationType(type);
		request.setAliasType(backupType);
		request.setBatchSize(batchSize);
		request.setMinimumId(minId);
		// +1 since maxId is exclusive.
		request.setMaximumId(maxId);
		// call under test
		BackupTypeResponse backupResponse = migrationManager.backupRequest(adminUser, request);
		assertNotNull(backupResponse);

		// restore the data from the backup
		RestoreTypeRequest restoreRequest = new RestoreTypeRequest();
		restoreRequest.setMigrationType(type);
		restoreRequest.setAliasType(backupType);
		restoreRequest.setBatchSize(batchSize);
		restoreRequest.setBackupFileKey(backupResponse.getBackupFileKey());
		// call under test
		RestoreTypeResponse restoreReponse = migrationManager.restoreRequest(adminUser, restoreRequest);
		assertNotNull(restoreReponse);
		// each node and revision should be restored.
		assertEquals(new Long(0), restoreReponse.getRestoredRowCount());
		// validate all of the data was restored.
		validateProjectsRestored();
	}
}
