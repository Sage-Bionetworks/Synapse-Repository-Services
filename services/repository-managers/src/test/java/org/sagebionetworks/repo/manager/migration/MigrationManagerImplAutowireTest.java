package org.sagebionetworks.repo.manager.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.ProjectSettingsManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.manager.table.TableIndexManagerImpl;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ProjectSettingsDAO;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.bootstrap.EntityBootstrapper;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.dao.TestUtils;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.migration.*;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

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
	private ProjectSettingsDAO projectSettingsDao;

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
	StackConfiguration stackConfig;
	ProgressCallback<Void> mockProgressCallback;
	ProgressCallback<Void> mockProgressCallbackVoid;
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

		for (int i = 0; i < projectIds.length; i++) {
			Project project = new Project();
			project.setName(UUID.randomUUID().toString());
			projectIds[i] = entityManager.createEntity(adminUser, project, null);
		}

		// Do this only if table enabled
		if (StackConfiguration.singleton().getTableEnabled()) {
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
		stackConfig = new StackConfiguration();
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
		RowMetadataResult rmr =	migrationManager.getRowMetadaForType(adminUser, MigrationType.FILE_HANDLE, 100, 0);
		long m = Long.MAX_VALUE;
		for (RowMetadata rm: rmr.getList()) {
			Long id = rm.getId();
			if (id < m) {
				m = id;
			}
		}
		assertTrue(m < Long.MAX_VALUE);
		assertEquals(m, min);
		assertTrue(min <= max);
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
		asyncMigrationRangeChecksumRequest.setType(MigrationType.FILE_HANDLE.name());
		
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
			amtcReq.setType(MigrationType.FILE_HANDLE.name());
			
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
 	
	// Deprecated?
	@Test
	public void testListRowMetadata(){
		RowMetadataResult result = migrationManager.getRowMetadaForType(adminUser, MigrationType.FILE_HANDLE, Long.MAX_VALUE, startCount);
		assertNotNull(result);
		assertEquals(new Long(startCount+2), result.getTotalCount());
		assertNotNull(result.getList());
		assertEquals(2, result.getList().size());
		// List the delta
		List<Long> ids = new LinkedList<Long>();
		for(RowMetadata rm: result.getList()){
			ids.add(rm.getId());
		}
		RowMetadataResult delta = migrationManager.getRowMetadataDeltaForType(adminUser, MigrationType.FILE_HANDLE, ids);
		assertNotNull(delta);
		assertNotNull(delta.getList());
		assertEquals(result.getList(), delta.getList());
	}
	
	@Test
	public void testListRowMetadataByRange() {
		long minId = migrationManager.getMinId(adminUser, MigrationType.FILE_HANDLE);
		long maxId = migrationManager.getMaxId(adminUser, MigrationType.FILE_HANDLE);
		RowMetadataResult result = migrationManager.getRowMetadataByRangeForType(adminUser, MigrationType.FILE_HANDLE, startId, maxId, maxId - startId + 1, 0);
		assertNotNull(result);
		assertEquals(new Long(startCount+2), result.getTotalCount());
		assertNotNull(result.getList());
		System.out.println("minid: " + minId + ", maxId: " + maxId + ", resList:" + result.getList());
		assertEquals(2, result.getList().size());
	}
	
	@Test
	public void testProcessAsyncRowMetadataByRange() {
		long minId = migrationManager.getMinId(adminUser, MigrationType.FILE_HANDLE);
		long maxId = migrationManager.getMaxId(adminUser, MigrationType.FILE_HANDLE);
		RowMetadataResult expectedRowMetadaResult = migrationManager.getRowMetadataByRangeForType(adminUser, MigrationType.FILE_HANDLE, startId, maxId, maxId - startId + 1, 0);
		AsyncMigrationRowMetadataRequest amrmReq = new AsyncMigrationRowMetadataRequest();
		amrmReq.setLimit(maxId - startId + 1);
		amrmReq.setMaxId(maxId);
		amrmReq.setMinId(startId);
		amrmReq.setOffset(0L);
		amrmReq.setType(MigrationType.FILE_HANDLE.name());
		
		RowMetadataResult amrmRes = migrationManager.processAsyncMigrationRowMetadataRequest(adminUser, amrmReq);
		
		assertNotNull(amrmRes);
		assertEquals(new Long(startCount+2), amrmRes.getTotalCount());
		assertNotNull(amrmRes.getList());
		System.out.println("minid: " + minId + ", maxId: " + maxId + ", resList:" + amrmRes.getList());
		assertEquals(2, amrmRes.getList().size());
	}
	
	@Test
	public void testRoundTrip() throws Exception{
		RowMetadataResult result = migrationManager.getRowMetadaForType(adminUser, MigrationType.FILE_HANDLE, Long.MAX_VALUE, startCount);
		assertNotNull(result);
		// List the delta
		List<Long> ids1 = new LinkedList<Long>();
		ids1.add(Long.parseLong(preview.getId()));
		List<Long> ids2 = new LinkedList<Long>();
		ids2.add(Long.parseLong(withPreview.getId()));
		// Write the backup data
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Writer writer = new OutputStreamWriter(out, "UTF-8");
		migrationManager.writeBackupBatch(adminUser, MigrationType.FILE_HANDLE, ids1, writer);
		writer.flush();
		String xml1 = new String(out.toByteArray(), "UTF-8");
		System.out.println(xml1);
		out = new ByteArrayOutputStream();
		writer = new OutputStreamWriter(out, "UTF-8");
		migrationManager.writeBackupBatch(adminUser, MigrationType.FILE_HANDLE, ids2, writer);
		writer.flush();
		String xml2 = new String(out.toByteArray(), "UTF-8");
		System.out.println(xml2);
		// Now delete the rows
		migrationManager.deleteObjectsById(adminUser, MigrationType.FILE_HANDLE, ids1);
		migrationManager.deleteObjectsById(adminUser, MigrationType.FILE_HANDLE, ids2);
		// The count should be the same as start
		assertEquals(startCount, migrationManager.getCount(adminUser, MigrationType.FILE_HANDLE));
		// Now restore them from the xml
		ByteArrayInputStream in = new ByteArrayInputStream(xml1.getBytes("UTF-8"));
		List<Long> ids = migrationManager.createOrUpdateBatch(adminUser, MigrationType.FILE_HANDLE, in);
		assertEquals(ids1, ids);
		in = new ByteArrayInputStream(xml2.getBytes("UTF-8"));
		migrationManager.createOrUpdateBatch(adminUser, MigrationType.FILE_HANDLE, in);
		// The count should be backup
		assertEquals(startCount+2, migrationManager.getCount(adminUser, MigrationType.FILE_HANDLE));
		// Calling again should not fail
		in = new ByteArrayInputStream(xml1.getBytes("UTF-8"));
		migrationManager.createOrUpdateBatch(adminUser, MigrationType.FILE_HANDLE, in);
		// Now get the data
		RowMetadataResult afterResult = migrationManager.getRowMetadaForType(adminUser, MigrationType.FILE_HANDLE, Long.MAX_VALUE, startCount);
		assertNotNull(result);
		assertEquals(result.getList(), afterResult.getList());

		// Do this only if table enabled
		if (StackConfiguration.singleton().getTableEnabled()) {
			// pretend to be worker and generate caches and index
			List<ColumnModel> currentSchema = tableManagerSupport.getColumnModelsForTable(tableId);
			TableIndexDAO indexDao = tableConnectionFactory.getConnection(tableId);
			TableIndexManagerImpl manager = new TableIndexManagerImpl(indexDao,tableManagerSupport);
			manager.setIndexSchema(tableId, mockProgressCallbackVoid, currentSchema);
			List<ColumnModel> models = columnManager.getColumnModelsForTable(adminUser, tableId);
			RowReferenceSet rowRefs = new RowReferenceSet();
			rowRefs.setRows(Collections.singletonList(TableModelTestUtils.createRowReference(0L, 0L)));
			rowRefs.setTableId(tableId);
			rowRefs.setHeaders(TableModelUtils.getSelectColumns(models));
			assertEquals(0, indexDao.getRowCountForTable(tableId).intValue());

			migrationManager.deleteObjectsById(adminUser, MigrationType.TABLE_SEQUENCE, Lists.newArrayList(KeyFactory.stringToKey(tableId)));
		}
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
		assertEquals(null, result);
	}
	
	@Test
	public void testDeleteAll() throws Exception{
		// Delete all data
		migrationManager.deleteAllData(adminUser);
		
		// The counts for all tables should be zero 
		// Except for 3 special cases, which are the minimal required rows to successfully
		//   call userManager.getUserInfo(AuthorizationConstants.MIGRATION_USER_NAME);
		for (MigrationType type : MigrationType.values()) {
			if (type == MigrationType.PRINCIPAL) {
				assertEquals("All non-essential " + type + " should have been deleted", 
						4L, migrationManager.getCount(adminUser, type));
			} else if (type == MigrationType.CREDENTIAL
					|| type == MigrationType.GROUP_MEMBERS) {
				assertEquals("All non-essential " + type + " should have been deleted", 
						1L, migrationManager.getCount(adminUser, type));
			} else if (migrationManager.isMigrationTypeUsed(adminUser, type)) {
				assertEquals("All data of type " + type + " should have been deleted", 
						0L, migrationManager.getCount(adminUser, type));
			}
		}
	}

}
