package org.sagebionetworks.table.worker;

import static org.junit.Assert.*;

import java.sql.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.EntityPermissionsManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.manager.table.TableQueryManager;
import org.sagebionetworks.repo.manager.table.TableViewManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.AppendableRowSetRequest;
import org.sagebionetworks.repo.model.table.ColumnChange;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.EntityDTO;
import org.sagebionetworks.repo.model.table.EntityField;
import org.sagebionetworks.repo.model.table.EntityUpdateResult;
import org.sagebionetworks.repo.model.table.EntityUpdateResults;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.model.table.PartialRow;
import org.sagebionetworks.repo.model.table.PartialRowSet;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SortItem;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableSchemaChangeRequest;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.model.table.TableUpdateRequest;
import org.sagebionetworks.repo.model.table.TableUpdateResponse;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionResponse;
import org.sagebionetworks.repo.model.table.ViewType;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class TableViewIntegrationTest {
	
	public static final int MAX_WAIT_MS = 1000 * 60 * 2;
	
	@Autowired
	private StackConfiguration config;
	@Autowired
	private UserManager userManager;
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private FileHandleDao fileHandleDao;
	@Autowired
	private TableManagerSupport tableManagerSupport;
	@Autowired
	private ColumnModelManager columnModelManager;
	@Autowired
	private TableViewManager tableViewMangaer;
	@Autowired
	private TableQueryManager tableQueryManger;
	@Autowired
	EntityPermissionsManager entityPermissionsManager;
	@Autowired
	ConnectionFactory tableConnectionFactory;
	@Autowired
	AsynchJobStatusManager asynchJobStatusManager;
	@Autowired
	private IdGenerator idGenerator;
	
	ProgressCallback<Void> mockProgressCallbackVoid;
	
	List<String> entitiesToDelete;
	UserInfo adminUserInfo;
	UserInfo userInfo;
	
	S3FileHandle sharedHandle;
	Project project;
	int fileCount;
	
	List<String> defaultColumnIds;
	
	List<String> fileIds;
	
	String fileViewId;
	EntityView entityView;
	List<ColumnModel> defaultSchema;
	
	ColumnModel etagColumn;
	ColumnModel anno1Column;
	
	@Before
	public void before(){
		// Only run this test if the table feature is enabled.
		Assume.assumeTrue(config.getTableEnabled());
		mockProgressCallbackVoid= Mockito.mock(ProgressCallback.class);
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		NewUser user = new NewUser();
		user.setUserName(UUID.randomUUID().toString());
		user.setEmail(user.getUserName() + "@bond.com");
		long userId = userManager.createUser(user);
		userInfo = userManager.getUserInfo(userId);
		
		// create a shared fileHandle
		S3FileHandle sharedHandle = new S3FileHandle();
		sharedHandle.setBucketName("fakeBucket");
		sharedHandle.setKey("fakeKey");
		sharedHandle.setContentMd5("md5");
		sharedHandle.setContentSize(123L);
		sharedHandle.setContentType("text/plain");
		sharedHandle.setCreatedBy(""+adminUserInfo.getId());
		sharedHandle.setCreatedOn(new Date(System.currentTimeMillis()));
		sharedHandle.setEtag(UUID.randomUUID().toString());
		sharedHandle.setFileName("foo.txt");
		sharedHandle.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		sharedHandle.setEtag(UUID.randomUUID().toString());
		sharedHandle.setPreviewId(sharedHandle.getId());
		sharedHandle = (S3FileHandle) fileHandleDao.createFile(sharedHandle);
		
		entitiesToDelete = new LinkedList<String>();
		
		project = new Project();
		project.setName(UUID.randomUUID().toString());
		String projectId = entityManager.createEntity(adminUserInfo, project, null);
		project = entityManager.getEntity(adminUserInfo, projectId, Project.class);
		entitiesToDelete.add(projectId);
		// add files to the project
		fileCount = 3;
		fileIds = new LinkedList<String>();
		for(int i=0; i<fileCount; i++){
			FileEntity file = new FileEntity();
			file.setName("foo"+i);
			file.setParentId(projectId);
			file.setDataFileHandleId(sharedHandle.getId());
			String fileId = entityManager.createEntity(adminUserInfo, file, null);
			fileIds.add(fileId);
		}
		
		defaultSchema = tableManagerSupport.getDefaultTableViewColumns(ViewType.file);
		// add an annotation column
		anno1Column = new ColumnModel();
		anno1Column.setColumnType(ColumnType.INTEGER);
		anno1Column.setName("foo");
		anno1Column = columnModelManager.createColumnModel(adminUserInfo, anno1Column);
		defaultSchema.add(anno1Column);
		
		defaultColumnIds = new LinkedList<String>();
		for(ColumnModel cm: defaultSchema){
			if(EntityField.etag.name().equals(cm.getName())){
				etagColumn = cm;
			}
			defaultColumnIds.add(cm.getId());
		}
		
		// Create a new file view
		EntityView fileView = new EntityView();
		fileView.setName("aFileView");
		fileView.setParentId(project.getId());
		fileView.setColumnIds(defaultColumnIds);
		fileView.setScopeIds(Lists.newArrayList(project.getId()));
		fileView.setType(ViewType.file);
		fileViewId = entityManager.createEntity(adminUserInfo, fileView, null);
		fileView = entityManager.getEntity(adminUserInfo, fileViewId, EntityView.class);
		tableViewMangaer.setViewSchemaAndScope(adminUserInfo, fileView.getColumnIds(), fileView.getScopeIds(), fileView.getType(), fileViewId);
	}
	
	@After
	public void after(){	
		if(entitiesToDelete != null){
			for(String id: entitiesToDelete){
				try {
					entityManager.deleteEntity(adminUserInfo, id);
				} catch (Exception e) {} 
			}
		}
		
		if(sharedHandle != null){
			fileHandleDao.delete(sharedHandle.getId());
		}
		
		if(userInfo != null){
			userManager.deletePrincipal(adminUserInfo, userInfo.getId());
		}
	}
	
	@Test
	public void testFileView() throws Exception{
		Long fileId = KeyFactory.stringToKey(fileIds.get(0));
		// wait for replication
		waitForEntityReplication(fileViewId, fileViewId);
		// query the view as a user that does not permission
		String sql = "select * from "+fileViewId;
		try {
			QueryResultBundle results = waitForConsistentQuery(userInfo, sql);
			fail("Should have failed.");
		} catch (UnauthorizedException e) {
			// expected
		}
		// grant the user read access to the view
		AccessControlList acl = AccessControlListUtil.createACL(fileViewId, userInfo, Sets.newHashSet(ACCESS_TYPE.READ), new Date(System.currentTimeMillis()));
		entityPermissionsManager.overrideInheritance(acl, adminUserInfo);
		// wait for replication
		waitForEntityReplication(fileViewId, fileViewId);
		// run the query again
		QueryResultBundle results = waitForConsistentQuery(userInfo, sql);
		assertNotNull(results);
		assertNotNull(results.getQueryResult());
		assertNotNull(results.getQueryResult().getQueryResults());
		assertNotNull(results.getQueryResult().getQueryResults().getRows());
		List<Row> rows = results.getQueryResult().getQueryResults().getRows();
		assertTrue("The user has no access to the files in the project so the view should appear empty",rows.isEmpty());
		// since the user has no access to files the results should be empty
		assertEquals(new Long(0), results.getQueryCount());
		
		// grant the user read permission the project
		AccessControlList projectAcl = entityPermissionsManager.getACL(project.getId(), adminUserInfo);
		ResourceAccess access = new ResourceAccess();
		access.setAccessType(Sets.newHashSet(ACCESS_TYPE.READ));
		access.setPrincipalId(userInfo.getId());
		projectAcl.getResourceAccess().add(access);
		entityPermissionsManager.updateACL(projectAcl, adminUserInfo);
		// wait for replication
		waitForEntityReplication(fileViewId, fileViewId);
		// run the query again
		results = waitForConsistentQuery(userInfo, sql);
		assertNotNull(results);
		assertEquals(new Long(fileCount), results.getQueryCount());
		assertNotNull(results.getQueryResult());
		assertNotNull(results.getQueryResult().getQueryResults());
		assertNotNull(results.getQueryResult().getQueryResults().getRows());
		rows = results.getQueryResult().getQueryResults().getRows();
		assertEquals(fileCount, rows.size());
	}
	
	@Test
	public void testContentUpdate() throws Exception {
		Long fileId = KeyFactory.stringToKey(fileIds.get(0));
		// lookup the file
		FileEntity file = entityManager.getEntity(adminUserInfo, ""+fileId, FileEntity.class);
		waitForEntityReplication(fileViewId, file.getId());
		// query the etag of the first file
		String sql = "select etag from "+fileViewId+" where id = "+fileId;
		QueryResultBundle results = waitForConsistentQuery(adminUserInfo, sql);
		assertNotNull(results);
		assertEquals(new Long(1), results.getQueryCount());
		assertNotNull(results.getQueryResult());
		assertNotNull(results.getQueryResult().getQueryResults());
		assertNotNull(results.getQueryResult().getQueryResults().getRows());
		Row row = results.getQueryResult().getQueryResults().getRows().get(0);
		assertEquals(fileId, row.getRowId());
		String etag = row.getValues().get(0);
		assertEquals(file.getEtag(), etag);
		
		// update the entity and run the query again
		file.setName("newName");
		entityManager.updateEntity(adminUserInfo, file, false, null);
		file = entityManager.getEntity(adminUserInfo, ""+fileId, FileEntity.class);
		// wait for the change to be replicated.
		waitForEntityReplication(fileViewId, file.getId());
		// run the query again
		results = waitForConsistentQuery(adminUserInfo, sql);
		assertNotNull(results);
		assertEquals(new Long(1), results.getQueryCount());
		assertNotNull(results.getQueryResult());
		assertNotNull(results.getQueryResult().getQueryResults());
		assertNotNull(results.getQueryResult().getQueryResults().getRows());
		row = results.getQueryResult().getQueryResults().getRows().get(0);
		assertEquals(fileId, row.getRowId());
		etag = row.getValues().get(0);
		assertEquals(file.getEtag(), etag);
	}
	
	@Test
	public void testForPLFM_4031() throws Exception{
		Long fileId = KeyFactory.stringToKey(fileIds.get(0));
		// lookup the file
		FileEntity file = entityManager.getEntity(adminUserInfo, ""+fileId, FileEntity.class);
		waitForEntityReplication(fileViewId, file.getId());
		ColumnModel benefactorColumn = EntityField.findMatch(defaultSchema, EntityField.benefactorId);
		// change the schema as a transaction
		ColumnChange remove = new ColumnChange();
		remove.setOldColumnId(benefactorColumn.getId());
		remove.setNewColumnId(null);
		List<ColumnChange> changes = Lists.newArrayList(remove);
		TableSchemaChangeRequest request = new TableSchemaChangeRequest();
		request.setEntityId(fileViewId);
		request.setChanges(changes);
		
		List<TableUpdateRequest> updates = new LinkedList<TableUpdateRequest>();
		updates.add(request);
		TableUpdateTransactionRequest transaction = new TableUpdateTransactionRequest();
		transaction.setEntityId(fileViewId);
		transaction.setChanges(updates);
	
		// wait for the change to complete
		startAndWaitForJob(adminUserInfo, transaction, TableUpdateTransactionResponse.class);
		// run the query again
		String sql = "select etag from "+fileViewId+" where id = "+fileId;
		QueryResultBundle results = waitForConsistentQuery(adminUserInfo, sql);
		assertNotNull(results);
		assertEquals(new Long(1), results.getQueryCount());
		assertNotNull(results.getQueryResult());
		assertNotNull(results.getQueryResult().getQueryResults());
		assertNotNull(results.getQueryResult().getQueryResults().getRows());
		Row row = results.getQueryResult().getQueryResults().getRows().get(0);
		assertEquals(fileId, row.getRowId());
		String etag = row.getValues().get(0);
		assertEquals(file.getEtag(), etag);
	}
	
	@Test
	public void testUpdateAnnotationsWithPartialRowSet() throws Exception {
		Long fileId = KeyFactory.stringToKey(fileIds.get(0));
		// lookup the file
		FileEntity file = entityManager.getEntity(adminUserInfo, ""+fileId, FileEntity.class);
		waitForEntityReplication(fileViewId, file.getId());
		
		Map<String, String> rowValues = new HashMap<String, String>();
		rowValues.put(anno1Column.getId(), "123456789");
		rowValues.put(etagColumn.getId(), file.getEtag());
		PartialRow row = new PartialRow();
		row.setRowId(fileId);
		row.setValues(rowValues);
		PartialRowSet rowSet = new PartialRowSet();
		rowSet.setRows(Lists.newArrayList(row));
		rowSet.setTableId(file.getId());
		
		AppendableRowSetRequest appendRequest = new AppendableRowSetRequest();
		appendRequest.setEntityId(fileViewId);
		appendRequest.setToAppend(rowSet);
		
		TableUpdateTransactionRequest transactionRequest = new TableUpdateTransactionRequest();
		List<TableUpdateRequest> changes = new LinkedList<TableUpdateRequest>();
		changes.add(appendRequest);
		transactionRequest.setChanges(changes);
		transactionRequest.setEntityId(fileViewId);
		//  start the job to update the table
		TableUpdateTransactionResponse response = startAndWaitForJob(adminUserInfo, transactionRequest, TableUpdateTransactionResponse.class);
		assertNotNull(response);
		assertNotNull(response.getResults());
		assertEquals(1, response.getResults().size());
		TableUpdateResponse rep = response.getResults().get(0);
		assertTrue(rep instanceof EntityUpdateResults);
		EntityUpdateResults updateResults = (EntityUpdateResults)rep;
		assertNotNull(updateResults.getUpdateResults());
		assertEquals(1, updateResults.getUpdateResults().size());
		EntityUpdateResult eur = updateResults.getUpdateResults().get(0);
		assertNotNull(eur);
		assertEquals(file.getId(), eur.getEntityId());
		System.out.println(eur.getFailureMessage());
		assertNull(eur.getFailureCode());
		assertNull(eur.getFailureMessage());
		
		// is the annotation changed?
		Annotations annos = entityManager.getAnnotations(adminUserInfo, file.getId());
		assertEquals(123456789L, annos.getSingleValue(anno1Column.getName()));
	}
	
	/**
	 * This is a test for PLFM-4088.  Need to support 'syn123' in both 
	 * a query where clause query return values.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testQueryEntityColumnType() throws Exception {
		String fileId = fileIds.get(0);
		assertTrue(fileId.startsWith("syn"));
		// lookup the file
		FileEntity file = entityManager.getEntity(adminUserInfo, ""+fileId, FileEntity.class);
		waitForEntityReplication(fileViewId, file.getId());
		
		String sql = "select id, parentId, projectId, benefactorId from "+fileViewId+" where id = "+fileId;
		QueryResultBundle results = waitForConsistentQuery(adminUserInfo, sql);
		assertNotNull(results);
		assertNotNull(results.getQueryResult());
		assertNotNull(results.getQueryResult().getQueryResults());
		assertNotNull(results.getQueryResult().getQueryResults().getRows());
		List<Row> rows = results.getQueryResult().getQueryResults().getRows();
		assertEquals(1, rows.size());
		Row row = rows.get(0);
		assertNotNull(row);
		assertNotNull(row.getValues());
		assertEquals(4, row.getValues().size());
		assertEquals(file.getId(), row.getValues().get(0));
		assertEquals(file.getParentId(), row.getValues().get(1));
		assertTrue(row.getValues().get(2).startsWith("syn"));
		assertTrue(row.getValues().get(3).startsWith("syn"));
	}
	
	/**
	 * Test for PLFM-4235. For PLFM-4235, an annotation with a value that is
	 * larger than the size of the Corresponding string column on the view. The
	 * view should be set to failed with a human readable error message.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPLFM_4235() throws Exception {
		String fileId = fileIds.get(0);
		// Add a string column to the view
		ColumnModel stringColumn = new ColumnModel();
		stringColumn.setName("aString");
		stringColumn.setColumnType(ColumnType.STRING);
		stringColumn.setMaximumSize(1L);
		stringColumn = columnModelManager.createColumnModel(adminUserInfo,
				stringColumn);
		defaultColumnIds.add(stringColumn.getId());
		tableViewMangaer.setViewSchemaAndScope(adminUserInfo, defaultColumnIds,
				Lists.newArrayList(project.getId()), ViewType.file, fileViewId);

		// Add an annotation with the same name and a value larger than the size
		// of the column.
		Annotations annos = entityManager.getAnnotations(adminUserInfo, fileId);
		annos.addAnnotation(stringColumn.getName(), "too big");
		entityManager.updateAnnotations(adminUserInfo, fileId, annos);
		waitForEntityReplication(fileViewId, fileId);

		try {
			String sql = "select * from " + fileViewId;
			waitForConsistentQuery(adminUserInfo, sql);
			fail("should have failed");
		} catch (TableFailedException expected) {
			assertEquals(
					"The size of the column 'aString' is too small.  The column size needs to be at least 7 characters.",
					expected.getStatus().getErrorMessage());
		}
	}
	
	/**
	 * Start an asynchronous job and wait for the results.
	 * @param user
	 * @param body
	 * @return
	 * @throws InterruptedException 
	 */
	@SuppressWarnings("unchecked")
	public <T extends AsynchronousResponseBody> T  startAndWaitForJob(UserInfo user, AsynchronousRequestBody body, Class<? extends T> clazz) throws InterruptedException{
		long startTime = System.currentTimeMillis();
		AsynchronousJobStatus status = asynchJobStatusManager.startJob(user, body);
		while(true){
			status = asynchJobStatusManager.getJobStatus(user, status.getJobId());
			switch(status.getJobState()){
			case FAILED:
				assertTrue("Job failed: "+status.getErrorDetails(), false);
			case PROCESSING:
				assertTrue("Timed out waiting for job to complete",(System.currentTimeMillis()-startTime) < MAX_WAIT_MS);
				System.out.println("Waiting for job: "+status.getProgressMessage());
				Thread.sleep(1000);
				break;
			case COMPLETE:
				return (T)status.getResponseBody();
			}
		}
	}
	
	/**
	 * Attempt to run a query. If the table is unavailable, it will continue to try until successful or the timeout is exceeded.
	 * 
	 * @param user
	 * @param sql
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws InterruptedException
	 */
	private QueryResultBundle waitForConsistentQuery(UserInfo user, String sql) throws Exception {
		long start = System.currentTimeMillis();
		while(true){
			try {
				List<SortItem> sortList = null;
				Long offset = null;
				Long limit = null;
				boolean runQuery = true;
				boolean runCount = true;
				boolean isConsistent = true;
				return tableQueryManger.querySinglePage(mockProgressCallbackVoid, user, sql, sortList, null, offset, limit, runQuery, runCount, false, isConsistent);
			} catch (LockUnavilableException e) {
				System.out.println("Waiting for table lock: "+e.getLocalizedMessage());
			} catch (TableUnavailableException e) {
				System.out.println("Waiting for table view worker to build table. Status: "+e.getStatus());
			}
			assertTrue("Timed out waiting for table view worker to make the table available.", (System.currentTimeMillis()-start) <  MAX_WAIT_MS);
			Thread.sleep(1000);
		}
	}
	
	/**
	 * Wait for EntityReplication to show the given etag for the given entityId.
	 * 
	 * @param tableId
	 * @param entityId
	 * @param etag
	 * @return
	 * @throws InterruptedException
	 */
	private EntityDTO waitForEntityReplication(String tableId, String entityId) throws InterruptedException{
		Entity entity = entityManager.getEntity(adminUserInfo, entityId);
		long start = System.currentTimeMillis();
		TableIndexDAO indexDao = tableConnectionFactory.getConnection(tableId);
		while(true){
			EntityDTO dto = indexDao.getEntityData(KeyFactory.stringToKey(entityId));
			if(dto == null || !dto.getEtag().equals(entity.getEtag())){
				assertTrue("Timed out waiting for table view status change.", (System.currentTimeMillis()-start) <  MAX_WAIT_MS);
				System.out.println("Waiting for entity replication. id: "+entityId+" etag: "+entity.getEtag());
				Thread.sleep(1000);
			}else{
				return dto;
			}
		}
	}

}
