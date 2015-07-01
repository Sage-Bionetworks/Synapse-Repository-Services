package org.sagebionetworks.repo.manager.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.ProjectSettingsManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableRowManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ProjectSettingsDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.bootstrap.EntityBootstrapper;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.table.CurrentVersionCacheDao;
import org.sagebionetworks.repo.model.dbo.dao.TestUtils;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.file.ExternalUploadDestination;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.UploadDestination;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.sagebionetworks.repo.model.migration.RowMetadataResult;
import org.sagebionetworks.repo.model.project.ExternalUploadDestinationSetting;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationSetting;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.util.ProgressCallback;
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
	TableRowManager tableRowManager;

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

	private List<String> toDelete;
	private UserInfo adminUser;
	private String creatorUserGroupId;
	private S3FileHandle withPreview;
	private PreviewFileHandle preview;
	private long startCount;
	private String tableId;
	private String[] projectIds = new String[3];
	StackConfiguration stackConfig;
	ProgressCallback<Long> mockProgressCallback;

	@Before
	public void before() throws Exception {
		mockProgressCallback = Mockito.mock(ProgressCallback.class);
		toDelete = new LinkedList<String>();
		adminUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		creatorUserGroupId = adminUser.getId().toString();
		assertNotNull(creatorUserGroupId);
		startCount = fileHandleDao.getCount();
		
		// The one will have a preview
		withPreview = TestUtils.createS3FileHandle(creatorUserGroupId);
		withPreview.setFileName("withPreview.txt");
		withPreview = fileHandleDao.createFile(withPreview);
		assertNotNull(withPreview);
		toDelete.add(withPreview.getId());
		// The Preview
		preview = TestUtils.createPreviewFileHandle(creatorUserGroupId);
		preview.setFileName("preview.txt");
		preview = fileHandleDao.createFile(preview);
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
			List<Long> headers = TableModelUtils.getIds(schema);
			// Create the table.
			TableEntity table = new TableEntity();
			table.setName(UUID.randomUUID().toString());
			table.setColumnIds(Lists.transform(headers, TableModelUtils.LONG_TO_STRING));
			tableId = entityManager.createEntity(adminUser, table, null);
			columnManager.bindColumnToObject(adminUser, Lists.transform(headers, TableModelUtils.LONG_TO_STRING), tableId, true);

			// Now add some data
			RowSet rowSet = new RowSet();
			rowSet.setRows(TableModelTestUtils.createRows(schema, 2));
			rowSet.setHeaders(TableModelUtils.createColumnModelColumnMapper(schema, false).getSelectColumns());
			rowSet.setTableId(tableId);
			tableRowManager.appendRows(adminUser, tableId, TableModelUtils.createColumnModelColumnMapper(schema, false), rowSet, mockProgressCallback);
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
	}
	
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
			List<ColumnModel> currentSchema = tableRowManager.getColumnModelsForTable(tableId);
			TableIndexDAO indexDao = tableConnectionFactory.getConnection(tableId);
			indexDao.createOrUpdateTable(currentSchema, tableId);
			tableRowManager.updateLatestVersionCache(tableId, new ProgressCallback<Long>() {
				@Override
				public void progressMade(Long version) {
				}
			});
			List<ColumnModel> models = columnManager.getColumnModelsForTable(adminUser, tableId);
			RowReferenceSet rowRefs = new RowReferenceSet();
			rowRefs.setRows(Collections.singletonList(TableModelTestUtils.createRowReference(0L, 0L)));
			rowRefs.setTableId(tableId);
			rowRefs.setHeaders(TableModelUtils.createColumnModelColumnMapper(models, false).getSelectColumns());
			tableRowManager.getCellValues(adminUser, tableId, rowRefs, TableModelUtils.createColumnModelColumnMapper(models, false));

			assertEquals(0, indexDao.getRowCountForTable(tableId).intValue());
			CurrentVersionCacheDao currentRowCacheDao = connectionFactory.getCurrentVersionCacheConnection(KeyFactory.stringToKey(tableId));
			assertEquals(2, currentRowCacheDao.getCurrentVersions(KeyFactory.stringToKey(tableId), 0L, 10L).size());

			migrationManager.deleteObjectsById(adminUser, MigrationType.TABLE_SEQUENCE, Lists.newArrayList(KeyFactory.stringToKey(tableId)));

			assertNull(indexDao.getRowCountForTable(tableId));
			currentRowCacheDao = connectionFactory.getCurrentVersionCacheConnection(KeyFactory.stringToKey(tableId));
			assertEquals(0, currentRowCacheDao.getCurrentVersions(KeyFactory.stringToKey(tableId), 0L, 10L).size());
		}
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
