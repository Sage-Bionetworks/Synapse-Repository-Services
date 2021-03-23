package org.sagebionetworks.table.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.EntityAclManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.file.download.BulkDownloadManager;
import org.sagebionetworks.repo.manager.message.RepositoryMessagePublisher;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.manager.table.TableViewManager;
import org.sagebionetworks.repo.manager.table.TableViewManagerImpl;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AsynchJobFailedException;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2TestUtils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2Utils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.table.TableStatusDAO;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.file.AddFileToDownloadListRequest;
import org.sagebionetworks.repo.model.file.AddFileToDownloadListResponse;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.AppendableRowSetRequest;
import org.sagebionetworks.repo.model.table.ColumnChange;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.EntityUpdateResult;
import org.sagebionetworks.repo.model.table.EntityUpdateResults;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.ObjectField;
import org.sagebionetworks.repo.model.table.PartialRow;
import org.sagebionetworks.repo.model.table.PartialRowSet;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryOptions;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SnapshotRequest;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.model.table.TableSchemaChangeRequest;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUpdateRequest;
import org.sagebionetworks.repo.model.table.TableUpdateResponse;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionResponse;
import org.sagebionetworks.repo.model.table.ViewEntityType;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.model.table.ViewScope;
import org.sagebionetworks.repo.model.table.ViewType;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class TableViewIntegrationTest {
	
	public static final int MAX_WAIT_MS = 1000 * 60 * 3;
	
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
	private TableViewManager tableViewManager;
	@Autowired
	private EntityAclManager entityAclManager;
	@Autowired
	private ConnectionFactory tableConnectionFactory;
	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	private TableStatusDAO tableStatusDao;
	@Autowired
	private RepositoryMessagePublisher repositoryMessagePublisher;
	@Autowired
	private DBOChangeDAO changeDAO;
	@Autowired
	private AsynchronousJobWorkerHelper asyncHelper;
	@Autowired
	private BulkDownloadManager bulkDownloadManager;
	
	
	ProgressCallback mockProgressCallbackVoid;
	
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
	ColumnModel benefactorColumn;
	ColumnModel anno1Column;
	ColumnModel booleanColumn;
	ColumnModel stringColumn;
	ColumnModel entityIdColumn;
	ColumnModel stringListColumn;
	
	ViewObjectType viewObjectType;
	ViewEntityType viewEntityType;
	
	@BeforeEach
	public void before(){
		viewEntityType = ViewEntityType.entityview;
		mockProgressCallbackVoid= Mockito.mock(ProgressCallback.class);
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		bulkDownloadManager.truncateAllDownloadDataForAllUsers(adminUserInfo);
		NewUser user = new NewUser();
		user.setUserName(UUID.randomUUID().toString());
		user.setEmail(user.getUserName() + "@bond.com");
		long userId = userManager.createUser(user);
		userInfo = userManager.getUserInfo(userId);
		
		// create a shared fileHandle
		sharedHandle = createNewFileHandle();
		
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
		defaultSchema = tableManagerSupport.getDefaultTableViewColumns(viewEntityType, ViewTypeMask.File.getMask());
		// add an annotation column
		anno1Column = new ColumnModel();
		anno1Column.setColumnType(ColumnType.INTEGER);
		anno1Column.setName("foo");
		anno1Column = columnModelManager.createColumnModel(adminUserInfo, anno1Column);
		defaultSchema.add(anno1Column);
		
		defaultColumnIds = new LinkedList<String>();
		for(ColumnModel cm: defaultSchema){
			if(ObjectField.etag.name().equals(cm.getName())){
				etagColumn = cm;
			}else if (ObjectField.benefactorId.name().equals(cm.getName())) {
				benefactorColumn = cm;
			}
			defaultColumnIds.add(cm.getId());
		}
		
		booleanColumn = new ColumnModel();
		booleanColumn.setName("aBoolean");
		booleanColumn.setColumnType(ColumnType.BOOLEAN);
		booleanColumn = columnModelManager.createColumnModel(adminUserInfo, booleanColumn);
		
		stringColumn = new ColumnModel();
		stringColumn.setName("aString");
		stringColumn.setColumnType(ColumnType.STRING);
		stringColumn.setMaximumSize(50L);
		stringColumn = columnModelManager.createColumnModel(adminUserInfo, stringColumn);
		
		entityIdColumn = new ColumnModel();
		entityIdColumn.setName("anEntityId");
		entityIdColumn.setColumnType(ColumnType.ENTITYID);
		entityIdColumn = columnModelManager.createColumnModel(adminUserInfo, entityIdColumn);

		stringListColumn = new ColumnModel();
		stringListColumn.setName("stringList");
		stringListColumn.setColumnType(ColumnType.STRING_LIST);
		stringListColumn.setMaximumListLength(3L);
		stringListColumn = columnModelManager.createColumnModel(adminUserInfo, stringListColumn);
		
		viewObjectType = ViewObjectType.ENTITY;
	}
	
	/**
	 * Helper to create a new FileHandle
	 * @return
	 */
	public S3FileHandle createNewFileHandle() {
		S3FileHandle fh = new S3FileHandle();
		fh.setBucketName("fakeBucket");
		fh.setKey("fakeKey");
		fh.setContentMd5("md5");
		fh.setContentSize(123L);
		fh.setContentType("text/plain");
		fh.setCreatedBy(""+adminUserInfo.getId());
		fh.setCreatedOn(new Date(System.currentTimeMillis()));
		fh.setEtag(UUID.randomUUID().toString());
		fh.setFileName("foo.txt");
		fh.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		fh.setEtag(UUID.randomUUID().toString());
		fh.setPreviewId(fh.getId());
		fh = (S3FileHandle) fileHandleDao.createFile(fh);
		return fh;
	}

	/**
	 * Create a File View using the project as a scope.
	 * 
	 */
	private void createFileView(){
		ViewType type = ViewType.file;
		List<String> scope = Lists.newArrayList(project.getId());
		fileViewId = createView(type, scope);
	}
	

	/**
	 * Create a view of the given type and scope.
	 * 
	 * @param type
	 * @param scope
	 */
	private String createView(ViewType type, List<String> scope) {
		// Create a new file view
		EntityView view = new EntityView();
		view.setName("aFileView");
		view.setParentId(project.getId());
		view.setColumnIds(defaultColumnIds);
		view.setScopeIds(scope);
		view.setType(type);
		String viewId = entityManager.createEntity(adminUserInfo, view, null);
		view = entityManager.getEntity(adminUserInfo, viewId, EntityView.class);
		ViewScope viewScope = new ViewScope();
		viewScope.setViewEntityType(viewEntityType);
		viewScope.setScope(view.getScopeIds());
		viewScope.setViewType(view.getType());
		tableViewManager.setViewSchemaAndScope(adminUserInfo, view.getColumnIds(), viewScope, viewId);
		entitiesToDelete.add(view.getId());
		return viewId;
	}
	
	
	@AfterEach
	public void after(){
		// Delete children before parents
		Collections.reverse(entitiesToDelete);
		if(entitiesToDelete != null){
			for(String id: entitiesToDelete){
				try {
					entityManager.deleteEntity(adminUserInfo, id);

				} catch (Exception e) {}
			}
		}
		
		fileHandleDao.truncateTable();
		
		if(userInfo != null){
			userManager.deletePrincipal(adminUserInfo, userInfo.getId());
		}
	}
	
	@Test
	public void testFileView() throws Exception{
		createFileView();
		// wait for replication
		waitForEntityReplication(fileViewId, fileViewId);
		// query the view as a user that does not permission
		String sql = "select * from "+fileViewId;

		UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> {
			waitForConsistentQuery(userInfo, sql, (response) -> {
				fail("Should have failed");
			});
		});
		
		assertEquals("You lack READ access to the requested entity.", ex.getMessage());

		// grant the user read access to the view
		AccessControlList acl = AccessControlListUtil.createACL(fileViewId, userInfo, Sets.newHashSet(ACCESS_TYPE.READ), new Date(System.currentTimeMillis()));
		entityAclManager.overrideInheritance(acl, adminUserInfo);
		// wait for replication
		waitForEntityReplication(fileViewId, fileViewId);
		
		// run the query again
		waitForConsistentQuery(userInfo, sql, (results) -> {
			List<Row> rows  = extractRows(results);
			assertTrue(rows.isEmpty(), "The user has no access to the files in the project so the view should appear empty");
			// since the user has no access to files the results should be empty
			assertEquals(new Long(0), results.getQueryCount());
		});
		
		
		grantUserReadAccessOnProject();
		// wait for replication
		waitForEntityReplication(fileViewId, fileViewId);
		
		// run the query again
		waitForConsistentQuery(userInfo, sql, (results) -> {			
			assertNotNull(results);
			assertEquals(new Long(fileCount), results.getQueryCount());
			assertNotNull(results.getQueryResult());
			assertNotNull(results.getQueryResult().getQueryResults());
			assertNotNull(results.getQueryResult().getQueryResults().getRows());
			assertEquals(fileCount, results.getQueryResult().getQueryResults().getRows().size());
		});
	}
	
	@Test
	public void testFileViewWithEtag() throws Exception{
		createFileView();
		// wait for replication
		waitForEntityReplication(fileViewId, fileViewId);
		Query query = new Query();
		query.setSql("select * from "+fileViewId);
		query.setIncludeEntityEtag(true);

		// run the query again
		waitForConsistentQuery(adminUserInfo, query, (results) -> {			
			assertNotNull(results);
			assertEquals(new Long(fileCount), results.getQueryCount());
			assertNotNull(results.getQueryResult());
			assertNotNull(results.getQueryResult().getQueryResults());
			assertNotNull(results.getQueryResult().getQueryResults().getRows());
			List<Row> rows = results.getQueryResult().getQueryResults().getRows();
			assertEquals(fileCount, rows.size());
			validateRowsMatchFiles(rows);
		});
	}
	
	@Test
	public void testSumFileSizes() throws Exception{
		createFileView();
		// wait for replication
		waitForEntityReplication(fileViewId, fileViewId);
		Query query = new Query();
		query.setSql("select * from "+fileViewId);
		QueryOptions options = new QueryOptions().withRunSumFileSizes(true).withRunQuery(true);
		
		waitForConsistentQuery(adminUserInfo, query, options, (results) -> {			
			assertNotNull(results);
			assertNotNull(results.getSumFileSizes());
			assertFalse(results.getSumFileSizes().getGreaterThan());
			assertNotNull(results.getSumFileSizes().getSumFileSizesBytes());
			assertTrue(results.getSumFileSizes().getSumFileSizesBytes() > 0L);
		});
	}
	
	/**
	 * Validate the EntityRows match the FileEntity
	 * @param rows
	 */
	public void validateRowsMatchFiles(List<Row> rows){
		// Match each row to each file
		for(Row row: rows){
			// Lookup the entity
			FileEntity entity = entityManager.getEntity(adminUserInfo, ""+row.getRowId(), FileEntity.class);
			assertEquals(entity.getEtag(), row.getEtag());
		}
	}
	
	
	@Test
	public void testContentUpdate() throws Exception {
		createFileView();
		Long fileId = KeyFactory.stringToKey(fileIds.get(0));
		// lookup the file
		final FileEntity file = entityManager.getEntity(adminUserInfo, ""+fileId, FileEntity.class);
		waitForEntityReplication(fileViewId, file.getId());
		// query the etag of the first file
		String sql = "select etag from "+fileViewId+" where id = "+fileId;
		
		waitForConsistentQuery(adminUserInfo, sql, (results) -> {			
			assertNotNull(results);
			assertEquals(new Long(1), results.getQueryCount());
			assertNotNull(results.getQueryResult());
			assertNotNull(results.getQueryResult().getQueryResults());
			assertNotNull(results.getQueryResult().getQueryResults().getRows());
			Row row = results.getQueryResult().getQueryResults().getRows().get(0);
			assertEquals(fileId, row.getRowId());
			String etag = row.getValues().get(0);
			assertEquals(file.getEtag(), etag);
		});
		
		// update the entity and run the query again
		file.setName("newName");
		
		entityManager.updateEntity(adminUserInfo, file, false, null);
		// wait for the change to be replicated.
		waitForEntityReplication(fileViewId, file.getId());
		// run the query again
		waitForConsistentQuery(adminUserInfo, sql, (results) -> {			
			assertNotNull(results);
			assertEquals(new Long(1), results.getQueryCount());
			assertNotNull(results.getQueryResult());
			assertNotNull(results.getQueryResult().getQueryResults());
			assertNotNull(results.getQueryResult().getQueryResults().getRows());
			Row row = results.getQueryResult().getQueryResults().getRows().get(0);
			assertEquals(fileId, row.getRowId());
		});
	}
	
	@Test
	public void testForPLFM_4031() throws Exception{
		createFileView();
		Long fileId = KeyFactory.stringToKey(fileIds.get(0));
		// lookup the file
		FileEntity file = entityManager.getEntity(adminUserInfo, ""+fileId, FileEntity.class);
		waitForEntityReplication(fileViewId, file.getId());
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
		startAndWaitForJob(adminUserInfo, transaction, (response) -> {
			assertNotNull(response);
		});
		// run the query again
		String sql = "select etag from "+fileViewId+" where id = "+fileId;
		
		waitForConsistentQuery(adminUserInfo, sql, (results) -> {			
			assertNotNull(results);
			assertEquals(new Long(1), results.getQueryCount());
			assertNotNull(results.getQueryResult());
			assertNotNull(results.getQueryResult().getQueryResults());
			assertNotNull(results.getQueryResult().getQueryResults().getRows());
			Row row = results.getQueryResult().getQueryResults().getRows().get(0);
			assertEquals(fileId, row.getRowId());
			String etag = row.getValues().get(0);
			assertEquals(file.getEtag(), etag);
		});
	}
	
	@Test
	public void testUpdateAnnotationsWithPartialRowSet() throws Exception {
		createFileView();
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
		rowSet.setTableId(fileViewId);
		
		AppendableRowSetRequest appendRequest = new AppendableRowSetRequest();
		appendRequest.setEntityId(fileViewId);
		appendRequest.setToAppend(rowSet);
		
		TableUpdateTransactionRequest transactionRequest = new TableUpdateTransactionRequest();
		List<TableUpdateRequest> changes = new LinkedList<TableUpdateRequest>();
		changes.add(appendRequest);
		transactionRequest.setChanges(changes);
		transactionRequest.setEntityId(fileViewId);
		
		//  start the job to update the table
		startAndWaitForJob(adminUserInfo, transactionRequest, (TableUpdateTransactionResponse response) -> {
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
			assertNull(eur.getFailureCode());
			assertNull(eur.getFailureMessage());
		});		
		
		// is the annotation changed?
		Annotations annos = entityManager.getAnnotations(adminUserInfo, file.getId());
		assertEquals("123456789", AnnotationsV2Utils.getSingleValue(annos, anno1Column.getName()));
	}
	
	@Test
	public void testUpdateAnnotationsWithPartialRowSetWithEtag() throws Exception {
		// do not include the etag column in the view.
		defaultColumnIds.remove(etagColumn.getId());
		createFileView();
		Long fileId = KeyFactory.stringToKey(fileIds.get(0));
		// lookup the file
		FileEntity file = entityManager.getEntity(adminUserInfo, ""+fileId, FileEntity.class);
		waitForEntityReplication(fileViewId, file.getId());
		
		Map<String, String> rowValues = new HashMap<String, String>();
		rowValues.put(anno1Column.getId(), "123456789");
		PartialRow row = new PartialRow();
		row.setRowId(fileId);
		row.setValues(rowValues);
		row.setEtag(file.getEtag());
		PartialRowSet rowSet = new PartialRowSet();
		rowSet.setRows(Lists.newArrayList(row));
		rowSet.setTableId(fileViewId);
		
		AppendableRowSetRequest appendRequest = new AppendableRowSetRequest();
		appendRequest.setEntityId(fileViewId);
		appendRequest.setToAppend(rowSet);
		
		TableUpdateTransactionRequest transactionRequest = new TableUpdateTransactionRequest();
		List<TableUpdateRequest> changes = new LinkedList<TableUpdateRequest>();
		changes.add(appendRequest);
		transactionRequest.setChanges(changes);
		transactionRequest.setEntityId(fileViewId);
		//  start the job to update the table
		startAndWaitForJob(adminUserInfo, transactionRequest, (TableUpdateTransactionResponse response) -> {			
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
			assertNull(eur.getFailureCode());
			assertNull(eur.getFailureMessage());
		});
		
		// is the annotation changed?
		Annotations annos = entityManager.getAnnotations(adminUserInfo, file.getId());
		assertEquals("123456789", AnnotationsV2Utils.getSingleValue(annos, anno1Column.getName()));
	}
	
	
	/**
	 * This is a test for PLFM-4088.  Need to support 'syn123' in both 
	 * a query where clause query return values.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testQueryEntityColumnType() throws Exception {
		createFileView();
		String fileId = fileIds.get(0);
		assertTrue(fileId.startsWith("syn"));
		// lookup the file
		FileEntity file = entityManager.getEntity(adminUserInfo, ""+fileId, FileEntity.class);
		waitForEntityReplication(fileViewId, file.getId());
		
		String sql = "select id, parentId, projectId, benefactorId from "+fileViewId+" where id = '"+fileId+"'";
		waitForConsistentQuery(adminUserInfo, sql, (results) -> {			
			List<Row> rows  = extractRows(results);
			assertEquals(1, rows.size());
			Row row = rows.get(0);
			assertNotNull(row);
			assertNotNull(row.getValues());
			assertEquals(4, row.getValues().size());
			assertEquals(file.getId(), row.getValues().get(0));
			assertEquals(file.getParentId(), row.getValues().get(1));
			assertTrue(row.getValues().get(2).startsWith("syn"));
			assertTrue(row.getValues().get(3).startsWith("syn"));
		});
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
		createFileView();
		String fileId = fileIds.get(0);
		// Add a string column to the view
		ColumnModel stringColumn = new ColumnModel();
		stringColumn.setName("aString");
		stringColumn.setColumnType(ColumnType.STRING);
		stringColumn.setMaximumSize(1L);
		stringColumn = columnModelManager.createColumnModel(adminUserInfo,
				stringColumn);
		defaultColumnIds.add(stringColumn.getId());
		ViewScope scope = new ViewScope();
		scope.setViewEntityType(viewEntityType);
		scope.setScope(Lists.newArrayList(project.getId()));
		scope.setViewType(ViewType.file);
		tableViewManager.setViewSchemaAndScope(adminUserInfo, defaultColumnIds,
				scope, fileViewId);

		// Add an annotation with the same name and a value larger than the size
		// of the column.
		Annotations annos = entityManager.getAnnotations(adminUserInfo, fileId);
		AnnotationsV2TestUtils.putAnnotations(annos, stringColumn.getName(), "too big", AnnotationsValueType.STRING);
		entityManager.updateAnnotations(adminUserInfo, fileId, annos);
		waitForEntityReplication(fileViewId, fileId);

		
		String sql = "select * from " + fileViewId;
		
		AsynchJobFailedException e = assertThrows(AsynchJobFailedException.class, () -> {
			waitForConsistentQuery(adminUserInfo, sql, (response) -> {
				fail("Should eventually fail");
			});
		});
		
		assertEquals("The size of the column 'aString' is too small.  The column size needs to be at least 7 characters.",
				e.getStatus().getErrorMessage());
		
	}

	/**
	 * See PLFM-4371.
	 *
	 */
	@Test
	public void testPLFM_4371() throws Exception {
		createFileView();
		String fileId = fileIds.get(0);
		// Add a string column to the view
		ColumnModel stringColumn = new ColumnModel();
		stringColumn.setName("concreteType");
		stringColumn.setColumnType(ColumnType.STRING);
		stringColumn.setMaximumSize(500L);
		stringColumn = columnModelManager.createColumnModel(adminUserInfo,
				stringColumn);
		defaultColumnIds.add(stringColumn.getId());
		ViewScope scope = new ViewScope();
		scope.setViewEntityType(viewEntityType);
		scope.setScope(Lists.newArrayList(project.getId()));
		scope.setViewTypeMask(ViewTypeMask.File.getMask());
		tableViewManager.setViewSchemaAndScope(adminUserInfo, defaultColumnIds,
				scope, fileViewId);

		// Add an annotation with a duplicate name as a primary annotation.
		Annotations annos = entityManager.getAnnotations(adminUserInfo, fileId);
		AnnotationsV2TestUtils.putAnnotations(annos, stringColumn.getName(), "this is a duplicate value", AnnotationsValueType.STRING);
		entityManager.updateAnnotations(adminUserInfo, fileId, annos);
		// For PLFM-4371 the replication was failing due to the duplicate name
		waitForEntityReplication(fileViewId, fileId);
	}

	/**
	 * Test for PLFM-4366. To reproduce:
	 * <ol>
	 * <li>Create a view with at least one column and one row.</li>
	 * <li>Wait for the view to become available for query.</li>
	 * <li>Change the view's state to processing</li>
	 * </ol>
	 * Result: The view would remain in PROCESSING. The problem was the view
	 * worker would detect that the view was up-to-date and do nothing. If there
	 * is no work to do then the worker needs to ensure the view is AVAILABLE.
	 */
	@Test
	public void testPLFM_4366() throws Exception{
		createFileView();
		// wait for the view to be available for query
		waitForEntityReplication(fileViewId, fileViewId);
		// query the view as a user that does not permission
		String sql = "select * from "+fileViewId;
		waitForConsistentQuery(adminUserInfo, sql, (results) -> {			
			assertNotNull(results);
		});

		IdAndVersion idAndVersion = IdAndVersion.parse(fileViewId);
		
		// Set the view to processing without making any real changes
		tableManagerSupport.setTableToProcessingAndTriggerUpdate(idAndVersion);
		
		// The view should become available again.
		waitForConsistentQuery(adminUserInfo, sql, (results) -> {			
			assertNotNull(results);
		});
	}

	@Test
	public void testProjectView() throws Exception{
		// Create some projects
		List<Project> projects = new LinkedList<Project>();
		projects.add(project);
		// Create a scope using even projects
		List<String> scope = new LinkedList<String>();
		String lastProjectId = null;
		// create some projects
		for(int i=0; i<6; i++){
			Project nextProject = new Project();
			nextProject.setName(UUID.randomUUID().toString());
			lastProjectId = entityManager.createEntity(adminUserInfo, nextProject, null);
			nextProject = entityManager.getEntity(adminUserInfo, lastProjectId, Project.class);
			entitiesToDelete.add(lastProjectId);
			projects.add(nextProject);
			if(i%2 == 0){
				scope.add(lastProjectId);
			}
		}
		// Create a project view
		String viewId = createView(ViewType.project, scope);
		waitForEntityReplication(viewId, lastProjectId);
		// query the view as a user that does not permission
		String sql = "select * from "+viewId;
		
		waitForConsistentQuery(adminUserInfo, sql, (results) -> {			
			List<Row> rows  = extractRows(results);
			assertEquals(scope.size(), rows.size(), "Should have one row for each scope.");
		});
	}

	/**
	 * PLFM-4413 and PLFM-4410 are both bugs where view contents are incorrect after
	 * ACLs are added/removed to the scopes of a view.  This test covers both issues.
	 * @throws InterruptedException
	 */
	@Test
	public void testPLFM_4413() throws Exception {
		// Add a folder to the existing project
		Folder folder = new Folder();
		folder.setParentId(project.getId());
		folder.setName("StartsWithACL");
		String folderId = entityManager.createEntity(adminUserInfo, folder, null);
		// Add an ACL on the folder
		AccessControlList acl = AccessControlListUtil.createACL(folderId, adminUserInfo, Sets.newHashSet(ACCESS_TYPE.READ), new Date(System.currentTimeMillis()));
		entityAclManager.overrideInheritance(acl, adminUserInfo);
		// Add a file to the folder
		FileEntity file = new FileEntity();
		file.setName("ChangingBenefactor");
		file.setDataFileHandleId(sharedHandle.getId());
		file.setParentId(folderId);
		String fileId = entityManager.createEntity(adminUserInfo, file, null);
		Long fileIdLong = KeyFactory.stringToKey(fileId);
		// create the view for this scope
		createFileView();
		// wait for the view to be available for query
		waitForEntityReplication(fileViewId, fileViewId);
		// query for the file that inherits from the folder.
		String sql = "select * from "+fileViewId+" where benefactorId='"+folderId+"' and id = '"+fileId+"'";
		
		waitForConsistentQuery(adminUserInfo, sql, (results) -> {			
			List<Row> rows  = extractRows(results);
			assertEquals(1, rows.size());
			Row row = rows.get(0);
			assertEquals(fileIdLong, row.getRowId());
		});

		/*
		 * Removing the ACL on the folder should set the file's benefactor to be
		 * the project. This should be reflected in the view.
		 */
		entityAclManager.restoreInheritance(folderId, adminUserInfo);

		// Query for the the file with the project as its benefactor.
		sql = "select * from "+fileViewId+" where benefactorId='"+project.getId()+"' and id = '"+fileId+"'";
		
		waitForConsistentQuery(adminUserInfo, sql, (results) -> {
			List<Row> rows  = extractRows(results);
			assertEquals(1, rows.size());
			Row row = rows.get(0);
			assertEquals(fileIdLong, row.getRowId());
		});
	}

	/**
	 * The fix for PLFM-4399 involved adding a worker to reconcile
	 * entity replication with the truth.  This test ensure that when
	 * replication data is missing for a FileView, a query of the
	 * view triggers the reconciliation.
	 * @throws Exception
	 *
	 */
	@Test
	public void testFileViewReconciliation() throws Exception{
		createFileView();
		String firstFileId = fileIds.get(0);
		Long firtFileIdLong = KeyFactory.stringToKey(firstFileId);
		// wait for the view to be available for query
		waitForEntityReplication(fileViewId, firstFileId);
		// query the view as a user that does not permission
		String sql = "select * from "+fileViewId+" where id ='"+firstFileId+"'";
		int rowCount = 1;
		waitForRowCount(adminUserInfo, sql, rowCount);

		// manually delete the replicated data the file to simulate a data loss.
		IdAndVersion idAndVersion = IdAndVersion.parse(fileViewId);
		TableIndexDAO indexDao = tableConnectionFactory.getConnection(idAndVersion);
		indexDao.deleteObjectData(viewObjectType, Lists.newArrayList(firtFileIdLong));
		indexDao.truncateReplicationSyncExpiration();

		// This query should trigger the reconciliation to repair the lost data.
		// If the query returns a single row, then the deleted data was restored.
		waitForRowCount(adminUserInfo, sql, rowCount);
	}


	/**
	 * The fix for PLFM-4399 involved adding a worker to reconcile
	 * entity replication with the truth.  This test ensure that when
	 * replication data is missing for a ProjectView, a query of the
	 * view triggers the reconciliation.
	 * @throws Exception
	 *
	 */
	@Test
	public void testProjectViewReconciliation() throws Exception{
		String projectId = project.getId();
		Long projectIdLong = KeyFactory.stringToKey(projectId);
		List<String> scope = Lists.newArrayList(projectId);
		String viewId = createView(ViewType.project, scope);
		// wait for the view.
		waitForEntityReplication(viewId, projectId);
		// query the view as a user that does not permission
		String sql = "select * from "+viewId+" where id ='"+projectId+"'";
		int rowCount = 1;
		waitForRowCount(adminUserInfo, sql, rowCount);

		// manually delete the replicated data of the project to simulate a data loss.
		IdAndVersion idAndVersion = IdAndVersion.parse(viewId);
		TableIndexDAO indexDao = tableConnectionFactory.getConnection(idAndVersion);
		indexDao.deleteObjectData(viewObjectType, Lists.newArrayList(projectIdLong));
		indexDao.truncateReplicationSyncExpiration();

		// This query should trigger the reconciliation to repair the lost data.
		// If the query returns a single row, then the deleted data was restored.
		waitForRowCount(adminUserInfo, sql, rowCount);
	}

	/**
	 * For PLFM-4446, users want to add boolean annotation with 'true' and 'false'
	 * and view the annotations with a column of type boolean.
	 * @throws Exception
	 */
	@Test
	public void testPLFM_4446() throws Exception{
		// Add 'boolean' annotations to each file
		// one
		String fileId = fileIds.get(0);
		Annotations annos = entityManager.getAnnotations(adminUserInfo, fileId);
		AnnotationsV2TestUtils.putAnnotations(annos, booleanColumn.getName(), "true", AnnotationsValueType.STRING);
		entityManager.updateAnnotations(adminUserInfo, fileId, annos);
		// two
		fileId = fileIds.get(1);
		annos = entityManager.getAnnotations(adminUserInfo, fileId);
		AnnotationsV2TestUtils.putAnnotations(annos, booleanColumn.getName(), "false", AnnotationsValueType.STRING);
		entityManager.updateAnnotations(adminUserInfo, fileId, annos);
		// three
		// two
		fileId = fileIds.get(2);
		annos = entityManager.getAnnotations(adminUserInfo, fileId);
		AnnotationsV2TestUtils.putAnnotations(annos, booleanColumn.getName(),"", AnnotationsValueType.STRING);
		entityManager.updateAnnotations(adminUserInfo, fileId, annos);

		// Create the view
		defaultColumnIds = Lists.newArrayList(booleanColumn.getId());
		createFileView();

		// This query should trigger the reconciliation to repair the lost data.
		// If the query returns a single row, then the deleted data was restored.
		String sql = "select "+booleanColumn.getName()+" from "+fileViewId;
		
		waitForConsistentQuery(adminUserInfo, sql, (results) -> {			
			List<Row> rows  = extractRows(results);
			assertEquals(3, rows.size());
			assertEquals("true", rows.get(0).getValues().get(0));
			assertEquals("false", rows.get(1).getValues().get(0));
			assertEquals(null, rows.get(2).getValues().get(0));
		});
	}

	/**
	 * Part of PLFM-4521 states that viewing a 'Float point' annotation with a string column
	 * shows a null even when the value is not null.  This is unexpected, as any annotation
	 * type should be visible as a string.
	 * @throws Exception
	 */
	@Test
	public void testPLFM_4521FloatToString() throws Exception{
		// one
		String fileId = fileIds.get(0);
		Annotations annos = entityManager.getAnnotations(adminUserInfo, fileId);
		// save a double with the string name
		AnnotationsV2TestUtils.putAnnotations(annos, stringColumn.getName(), "1.3", AnnotationsValueType.DOUBLE);
		entityManager.updateAnnotations(adminUserInfo, fileId, annos);
		// two
		fileId = fileIds.get(1);
		annos = entityManager.getAnnotations(adminUserInfo, fileId);
		AnnotationsV2TestUtils.putAnnotations(annos, stringColumn.getName(), "not a double", AnnotationsValueType.STRING);
		entityManager.updateAnnotations(adminUserInfo, fileId, annos);
		// three
		// two
		fileId = fileIds.get(2);
		annos = entityManager.getAnnotations(adminUserInfo, fileId);
		AnnotationsV2TestUtils.putAnnotations(annos, stringColumn.getName(), "", AnnotationsValueType.STRING);
		entityManager.updateAnnotations(adminUserInfo, fileId, annos);

		// Create the view
		defaultColumnIds = Lists.newArrayList(stringColumn.getId());
		createFileView();

		// This query should trigger the reconciliation to repair the lost data.
		// If the query returns a single row, then the deleted data was restored.
		String sql = "select "+stringColumn.getName()+" from "+fileViewId;

		waitForConsistentQuery(adminUserInfo, sql, (results) -> {			
			List<Row> rows  = extractRows(results);
			assertEquals(3, rows.size());
			assertEquals("1.3", rows.get(0).getValues().get(0));
			assertEquals("not a double", rows.get(1).getValues().get(0));
			assertEquals("", rows.get(2).getValues().get(0));
		});
	}

	/**
	 * Part of PLFM-4521 states that a non-entity ID string annotation breaks a view that includes
	 * a column of type entity ID.
	 * @throws Exception
	 */
	@Test
	public void testPLFM_4521StringToEntityId() throws Exception{
		// Add 'boolean' annotations to each file
		// one
		String fileId = fileIds.get(0);
		Annotations annos = entityManager.getAnnotations(adminUserInfo, fileId);
		// save a double with the string name
		AnnotationsV2TestUtils.putAnnotations(annos, entityIdColumn.getName(), "syn123", AnnotationsValueType.STRING);
		entityManager.updateAnnotations(adminUserInfo, fileId, annos);
		// two
		fileId = fileIds.get(1);
		annos = entityManager.getAnnotations(adminUserInfo, fileId);
		// a long can be used as an entity ID.
		AnnotationsV2TestUtils.putAnnotations(annos, entityIdColumn.getName(), "456", AnnotationsValueType.LONG);
		entityManager.updateAnnotations(adminUserInfo, fileId, annos);
		// three
		// two
		fileId = fileIds.get(2);
		annos = entityManager.getAnnotations(adminUserInfo, fileId);
		AnnotationsV2TestUtils.putAnnotations(annos, entityIdColumn.getName(), "", AnnotationsValueType.STRING);
		entityManager.updateAnnotations(adminUserInfo, fileId, annos);

		// Create the view
		defaultColumnIds = Lists.newArrayList(entityIdColumn.getId());
		createFileView();

		// This query should trigger the reconciliation to repair the lost data.
		// If the query returns a single row, then the deleted data was restored.
		String sql = "select "+entityIdColumn.getName()+" from "+fileViewId;
		
		waitForConsistentQuery(adminUserInfo, sql, (results) -> {			
			List<Row> rows  = extractRows(results);
			assertEquals(3, rows.size());
			assertEquals("syn123", rows.get(0).getValues().get(0));
			assertEquals("syn456", rows.get(1).getValues().get(0));
			assertEquals(null, rows.get(2).getValues().get(0));
		});
	}

	/**
	 * For PLFM-4517 updating file entity annotations using a view update
	 * would add duplicate annotations with the same name but different type.
	 * Duplicate annotation names are no longer supported.  Therefore, updates
	 * to annotations using a view should replace annotations, not add duplicates.
	 * @throws Exception
	 */
	@Test
	public void testPLFM_4517() throws Exception{
		// Add various types of annotations with the same name to the files in the view.
		// one
		String fileId = fileIds.get(0);
		Annotations annos = entityManager.getAnnotations(adminUserInfo, fileId);
		AnnotationsV2TestUtils.putAnnotations(annos, stringColumn.getName(), "1.23", AnnotationsValueType.DOUBLE);
		entityManager.updateAnnotations(adminUserInfo, fileId, annos);
		// two
		fileId = fileIds.get(1);
		annos = entityManager.getAnnotations(adminUserInfo, fileId);
		AnnotationsV2TestUtils.putAnnotations(annos, stringColumn.getName(), "456", AnnotationsValueType.LONG);
		entityManager.updateAnnotations(adminUserInfo, fileId, annos);
		// three
		fileId = fileIds.get(2);
		annos = entityManager.getAnnotations(adminUserInfo, fileId);
		AnnotationsV2TestUtils.putAnnotations(annos, stringColumn.getName(), "789", AnnotationsValueType.TIMESTAMP_MS);
		entityManager.updateAnnotations(adminUserInfo, fileId, annos);

		// Create the view
		defaultColumnIds = Lists.newArrayList(stringColumn.getId(), etagColumn.getId());
		createFileView();

		// Query for the values as strings.
		String sql = "select "+stringColumn.getName()+", "+etagColumn.getName()+" from "+fileViewId;
		int rowCount = 3;
		
		RowSet rowSet = waitForConsistentQuery(adminUserInfo, sql, (results) -> {
			List<Row> rows  = extractRows(results);
			assertEquals(3, rows.size());
			assertEquals("1.23", rows.get(0).getValues().get(0));
			assertEquals("456", rows.get(1).getValues().get(0));
			assertEquals("789", rows.get(2).getValues().get(0));			
		}).getQueryResult().getQueryResults();

		// use the results to update the annotations.
		List<EntityUpdateResult> updates = updateView(rowSet, fileViewId);

		assertEquals(3, updates.size());
		// all of the update should have succeeded.
		for(EntityUpdateResult eur: updates){
			assertEquals(null, eur.getFailureMessage());
			assertEquals(null, eur.getFailureCode());
		}

	}

	@Test
	public void testUpdateViewWithRowsetAndEtag() throws Exception{
		// one
		String fileId = fileIds.get(0);
		Annotations annos = entityManager.getAnnotations(adminUserInfo, fileId);
		AnnotationsV2TestUtils.putAnnotations(annos, stringColumn.getName(), "1", AnnotationsValueType.STRING);
		entityManager.updateAnnotations(adminUserInfo, fileId, annos);
		
		// the view does not have an etag column
		defaultColumnIds = Lists.newArrayList(stringColumn.getId());
		createFileView();
		
		// wait for the view.
		waitForEntityReplication(fileViewId, fileId);
		
		// Query for the values as strings.
		String sql = "select "+stringColumn.getName()+" from "+fileViewId+" where ROW_ID="+KeyFactory.stringToKey(fileId);
		Query query = new Query();
		query.setSql(sql);
		query.setIncludeEntityEtag(true);
		
		final RowSet currentRowSet = waitForConsistentQuery(adminUserInfo, query, (results) -> {			
			List<Row> rows  = extractRows(results);
			assertEquals(1, rows.size());
			Row row = rows.get(0);
			assertNotNull(row.getEtag());
			assertEquals("1", row.getValues().get(0));
		}).getQueryResult().getQueryResults();

		// Saves the current row etag
		final String currentEtag = currentRowSet.getRows().get(0).getEtag();
		// change the value of the first row
		currentRowSet.getRows().get(0).setValues(Lists.newArrayList("111"));
			
		// use the results to update the annotations.
		List<EntityUpdateResult> updates = updateView(currentRowSet, fileViewId);
		assertEquals(1, updates.size());
		// all of the update should have succeeded.
		for(EntityUpdateResult eur: updates){
			assertEquals(null, eur.getFailureMessage());
			assertEquals(null, eur.getFailureCode());
		}
		
		// wait for the view.
		waitForEntityReplication(fileViewId, fileId);
		
		// Wait for the change to appear in the vie
		waitForConsistentQuery(adminUserInfo, query, (results) -> {			
			List<Row> rows = extractRows(results);
			assertEquals(1, rows.size());
			Row row = rows.get(0);
			assertNotNull(row.getEtag());
			assertEquals("111", row.getValues().get(0));
			assertFalse(currentEtag.equals(row.getEtag()));
		});

	}
	
	/**
	 * PLFM-4270 is request to add support views with both files and tables.
	 * @throws Exception 
	 */
	@Test
	public void testViewWithFilesAndTables() throws Exception{
		// use the default columns for this type.
		defaultSchema = tableManagerSupport.getDefaultTableViewColumns(viewEntityType, ViewTypeMask.getMaskForDepricatedType(ViewType.file_and_table));
		// Add a table to the project
		TableEntity table = new TableEntity();
		table.setName("someTable");
		table.setParentId(project.getId());
		String childTableId = entityManager.createEntity(adminUserInfo, table, null);
		table = entityManager.getEntity(adminUserInfo, childTableId, TableEntity.class);
		// create the view that includes both files and tables.
		ViewType type = ViewType.file_and_table;
		List<String> scope = Lists.newArrayList(project.getId());
		fileViewId = createView(type, scope);
	
		// wait for the view.
		waitForEntityReplication(fileViewId, childTableId);
		
		// Query for the values as strings.
		Query query = new Query();
		query.setSql("select * from "+fileViewId);
		query.setIncludeEntityEtag(true);

		final Long tableId = KeyFactory.stringToKey(table.getId());
		final String tableEtag = table.getEtag();
		
		waitForConsistentQuery(adminUserInfo, query, (results) -> {			
			List<Row> rows = extractRows(results);
			assertEquals(4, rows.size());
			// The last row should be the table
			Row last = rows.get(3);
			assertEquals(tableId, last.getRowId());
			assertEquals(tableEtag, last.getEtag());
		});
		
	}
	
	/**
	 * Test for PLFM-4733
	 */
	@Test
	public void testPLFM_4733() throws Exception {
		defaultSchema.clear();
		defaultColumnIds.clear();
		// create a view with just under the max number of columns.
		int maxColumnCount = TableViewManagerImpl.MAX_COLUMNS_PER_VIEW;
		for(int i=0; i<maxColumnCount; i++) {
			ColumnModel cm = new ColumnModel();
			cm.setColumnType(ColumnType.DOUBLE);
			cm.setName("c"+i);
			cm = columnModelManager.createColumnModel(adminUserInfo, cm);
			defaultSchema.add(cm);
			defaultColumnIds.add(cm.getId());
		}
		ViewType type = ViewType.file;
		List<String> scope = Lists.newArrayList(project.getId());
		fileViewId = createView(type, scope);
		
		// Query for the values as strings.
		int rowCount = 3;
		// query should work without failure.
		waitForRowCount(adminUserInfo, "select * from "+fileViewId, rowCount);
		
		// Try to add one more column should fail
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.INTEGER);
		cm.setName("c"+maxColumnCount+1);
		cm = columnModelManager.createColumnModel(adminUserInfo, cm);
		ColumnChange columnAddition = new ColumnChange();
		columnAddition.setNewColumnId(cm.getId());
		
		TableSchemaChangeRequest changeRequest = new TableSchemaChangeRequest();
		changeRequest.setEntityId(fileViewId);
		changeRequest.setChanges(Lists.newArrayList(columnAddition));
		
		TableUpdateTransactionRequest transactionRequest = new TableUpdateTransactionRequest();
		List<TableUpdateRequest> changes = new LinkedList<TableUpdateRequest>();
		changes.add(changeRequest);
		transactionRequest.setChanges(changes);
		transactionRequest.setEntityId(fileViewId);
		//  start the job to update the table
	 	IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			startAndWaitForJob(adminUserInfo, transactionRequest, (response) -> {
				fail("Should eventually fail");
			});
		});
		assertTrue(e.getMessage().contains(""+TableViewManagerImpl.MAX_COLUMNS_PER_VIEW));
		
	}
	
	@Test
	public void testViewSnapshot() throws Exception {
		createFileView();
		// add a column to the view.
		TableSchemaChangeRequest schemaChangeRequest = new TableSchemaChangeRequest();
		ColumnChange addColumn = new ColumnChange();
		addColumn.setNewColumnId(stringColumn.getId());
		schemaChangeRequest.setChanges(Lists.newArrayList(addColumn));
		// Add a string annotation to each file in the view
		List<PartialRow> rowsToAdd = new LinkedList<>();
		int counter = 0;
		for (String fileId : fileIds) {
			PartialRow row = new PartialRow();
			row.setRowId(KeyFactory.stringToKey(fileId));
			Map<String, String> values = new HashMap<>(1);
			values.put(stringColumn.getId(), "string value:" + counter++);
			FileEntity file = entityManager.getEntity(adminUserInfo, fileId, FileEntity.class);
			row.setEtag(file.getEtag());
			row.setValues(values);
			rowsToAdd.add(row);
		}
		PartialRowSet rowChange = new PartialRowSet();
		rowChange.setTableId(fileViewId);
		rowChange.setRows(rowsToAdd);
		AppendableRowSetRequest rowSetRequest = new AppendableRowSetRequest();
		rowSetRequest.setToAppend(rowChange);

		// create a snapshot after the change
		SnapshotRequest snapshotOptions = new SnapshotRequest();
		snapshotOptions.setSnapshotComment("the first view snapshot ever!");

		// Add all of the parts
		TableUpdateTransactionRequest transactionRequest = new TableUpdateTransactionRequest();
		transactionRequest.setEntityId(fileViewId);
		transactionRequest.setChanges(Lists.newArrayList(schemaChangeRequest, rowSetRequest));
		transactionRequest.setCreateSnapshot(true);
		transactionRequest.setSnapshotOptions(snapshotOptions);

		// Start the job that will change the schema and annotations to each file and
		// then snapshot the view.
		// call under test
		Long snaphsotVersionNumber = startAndWaitForJob(adminUserInfo, transactionRequest, (TableUpdateTransactionResponse response) -> {				
			assertNotNull(response);
			assertNotNull(response.getSnapshotVersionNumber());
		}).getSnapshotVersionNumber();

		// Query the snapshot
		Query query = new Query();
		query.setSql("select * from " + fileViewId + "." + snaphsotVersionNumber);
		query.setIncludeEntityEtag(true);
		
		waitForConsistentQuery(adminUserInfo, query, (queryResults) -> {			
			assertNotNull(queryResults);
			List<Row> rows = extractRows(queryResults);
			assertEquals(3, rows.size());
		});
	}
	
	/**
	 * See PLFM-6398.
	 *  
	 * @throws Exception
	 */
	@Test
	public void testViewSnapshotPLFM_6398() throws Exception {
		grantUserReadAccessOnProject();
		// Add a folder to the existing project
		Folder folderOne = new Folder();
		folderOne.setParentId(project.getId());
		folderOne.setName("folderOne");
		String folderOneId = entityManager.createEntity(adminUserInfo, folderOne, null);
		entitiesToDelete.add(folderOneId);
		Long folderOneIdLong = KeyFactory.stringToKey(folderOneId);
		// Add an ACL on the folder
		AccessControlList acl = AccessControlListUtil.createACL(folderOneId, userInfo, Sets.newHashSet(ACCESS_TYPE.READ), new Date(System.currentTimeMillis()));
		entityAclManager.overrideInheritance(acl, adminUserInfo);
		// Add a file to the folder
		FileEntity fileOne = new FileEntity();
		fileOne.setName("fileOne");
		fileOne.setDataFileHandleId(sharedHandle.getId());
		fileOne.setParentId(folderOneId);
		String fileOneId = entityManager.createEntity(adminUserInfo, fileOne, null);
		fileOne = entityManager.getEntity(adminUserInfo, fileOneId, FileEntity.class);
		Long fileOneIdLong = KeyFactory.stringToKey(fileOneId);
		
		// create the view for this scope
		createFileView();
		// grant the user read on the view.
		acl = AccessControlListUtil.createACL(fileViewId, userInfo, Sets.newHashSet(ACCESS_TYPE.READ), new Date(System.currentTimeMillis()));
		entityAclManager.overrideInheritance(acl, adminUserInfo);
		// wait for the view to be available for query
		waitForEntityReplication(fileViewId, fileOneId);
		
		// Create a snapshot for this view
		TableUpdateTransactionRequest transactionRequest = new TableUpdateTransactionRequest();
		transactionRequest.setEntityId(fileViewId);
		transactionRequest.setCreateSnapshot(true);
		
		startAndWaitForJob(adminUserInfo, transactionRequest, (TableUpdateTransactionResponse response) -> {			
			assertNotNull(response);
			assertEquals(new Long(1), response.getSnapshotVersionNumber());
		});
		
		// update the first file so the new etag does not match the etag from the snapshot
		String fileOneOldEtag = fileOne.getEtag();
		fileOne.setName("FileOne Renamed");
		boolean newVersion = false;
		String actvityId = null;
		entityManager.updateEntity(adminUserInfo, fileOne, newVersion, actvityId);
		fileOne = entityManager.getEntity(adminUserInfo, fileOneId, FileEntity.class);
		assertFalse(fileOne.getEtag().equals(fileOneOldEtag));
		
		// query for the file that inherits from the folder.
		String sql = "select name, etag, ROW_BENEFACTOR from "+fileViewId+".1 WHERE ROW_ID = "+fileOneIdLong;
		// at this point the user should be able see the three files in the root project and the file in the folder.
		waitForConsistentQuery(userInfo, sql, (results) -> {			
			List<Row> rows  = extractRows(results);
			assertEquals(1, rows.size());
			Row row = rows.get(0);
			assertEquals(fileOneIdLong, row.getRowId());
			// the name and etag should not change in the snapshot results and the benefactor should be the folder
			assertEquals(Lists.newArrayList("fileOne", fileOneOldEtag, folderOneIdLong.toString()), row.getValues());
		});
		
		/*
		 * Remove the ACL on the folder.  
		 */
		entityAclManager.restoreInheritance(folderOneId, adminUserInfo);
		
		waitForConsistentQuery(userInfo, sql, (results) -> {			
			List<Row> rows  = extractRows(results);
			assertEquals(1, rows.size());
			Row row = rows.get(0);
			assertEquals(fileOneIdLong, row.getRowId());
			// the name and etag should not change in the snapshot results and the benefactor should now be the project.
			assertEquals(Lists.newArrayList("fileOne", fileOneOldEtag, KeyFactory.stringToKey(project.getId()).toString()), row.getValues());
		});
	}

	/**
	 * Grant the user read access on the project.
	 * @throws ACLInheritanceException
	 */
	void grantUserReadAccessOnProject() throws ACLInheritanceException {
		// grant the user read permission the project
		AccessControlList projectAcl = entityAclManager.getACL(project.getId(), adminUserInfo);
		ResourceAccess access = new ResourceAccess();
		access.setAccessType(Sets.newHashSet(ACCESS_TYPE.READ));
		access.setPrincipalId(userInfo.getId());
		projectAcl.getResourceAccess().add(access);
		entityAclManager.updateACL(projectAcl, adminUserInfo);
	}
	
	/**
	 * For PLFM-5939 a version query (select * from syn123.1) was run against a view that did not have any snapshots.
	 * The query worker failed but the request was returned to the queue.  The cycle would then repeat continuously.
	 * @throws Exception 
	 */
	@Test
	public void testPLFM_5939() throws Exception {
		// one
		String fileId = fileIds.get(0);
		Annotations annos = entityManager.getAnnotations(adminUserInfo, fileId);
		AnnotationsV2TestUtils.putAnnotations(annos, stringColumn.getName(), "1", AnnotationsValueType.STRING);
		entityManager.updateAnnotations(adminUserInfo, fileId, annos);
		
		defaultColumnIds = Lists.newArrayList(stringColumn.getId());
		createFileView();
		
		// wait for the view.
		waitForEntityReplication(fileViewId, fileId);
		
		String message = assertThrows(AsynchJobFailedException.class, () -> {
			waitForRowCount(adminUserInfo, "select * from "+fileViewId+".1", 1);
		}).getMessage();
		
		assertTrue(message.contains("Snapshot not found"));
		
	}
	
	/**
	 * For PLFM-5957, a user queried for a snapshot prior to creating it.
	 * After the snapshot was created, the query still failed with "snapshot not found"
	 */
	@Test
	public void testPLFM_5957() throws Exception {
		// one
		String fileId = fileIds.get(0);
		Annotations annos = entityManager.getAnnotations(adminUserInfo, fileId);
		AnnotationsV2TestUtils.putAnnotations(annos, stringColumn.getName(), "1", AnnotationsValueType.STRING);
		entityManager.updateAnnotations(adminUserInfo, fileId, annos);
		
		defaultColumnIds = Lists.newArrayList(stringColumn.getId());
		createFileView();
		
		// wait for the view.
		waitForEntityReplication(fileViewId, fileId);
		
		// Query for the values as strings.
		int rowCount = fileIds.size();
		String sql = "select * from "+fileViewId+".1";
		
		assertThrows(AsynchJobFailedException.class, () -> {
			waitForRowCount(adminUserInfo, sql, rowCount);
		});
		
		// Create a snapshot for this view
		TableUpdateTransactionRequest transactionRequest = new TableUpdateTransactionRequest();
		transactionRequest.setEntityId(fileViewId);
		transactionRequest.setCreateSnapshot(true);
		
		startAndWaitForJob(adminUserInfo, transactionRequest, (TableUpdateTransactionResponse response) -> {			
			assertNotNull(response);
			assertEquals(new Long(1), response.getSnapshotVersionNumber());
		});
		
		// run the query again, this time there should be 3 rows.
		waitForRowCount(adminUserInfo, sql, rowCount);
	}

	/**
	 * Tests the UNNEST(colName) function which is applied to LIST_columnTypes
	 * @throws Exception
	 */
	@Test
	public void testUNNESTFunction() throws Exception{
		// Add 'boolean' annotations to each file
		// one
		String fileId = fileIds.get(0);
		Annotations annos = entityManager.getAnnotations(adminUserInfo, fileId);
		AnnotationsV2TestUtils.putAnnotations(annos, stringListColumn.getName(), Arrays.asList("val1", "val2"), AnnotationsValueType.STRING);
		entityManager.updateAnnotations(adminUserInfo, fileId, annos);
		// two
		fileId = fileIds.get(1);
		annos = entityManager.getAnnotations(adminUserInfo, fileId);
		AnnotationsV2TestUtils.putAnnotations(annos, stringListColumn.getName(), Arrays.asList("val3", "val4"), AnnotationsValueType.STRING);
		entityManager.updateAnnotations(adminUserInfo, fileId, annos);

		// Create the view
		defaultColumnIds = Lists.newArrayList(stringListColumn.getId());
		createFileView();

		// This query should trigger the reconciliation to repair the lost data.
		// If the query returns a single row, then the deleted data was restored.
		String sql = "select UNNEST("+stringListColumn.getName()+") from "+fileViewId;
		waitForConsistentQuery(adminUserInfo, sql, (results) -> {			
			List<Row> rows  = extractRows(results);
			assertEquals(5, rows.size());
			assertEquals("val1", rows.get(0).getValues().get(0));
			assertEquals("val2", rows.get(1).getValues().get(0));
			assertEquals("val3", rows.get(2).getValues().get(0));
			assertEquals("val4", rows.get(3).getValues().get(0));
			assertEquals(null, rows.get(4).getValues().get(0));
		});
	}
	
	/**
	 * With the fix for PLFM-5966. A query against a view that is out-of-date should
	 * no longer trigger the view's state to change to 'PROCESSING'. Instead, the
	 * view should remain 'AVAILABLE' while changes are applied to the view.
	 * @throws InterruptedException 
	 */
	@Test
	public void testViewRemainsAvailableWhileChanging() throws Exception {
		createFileView();
		// wait for replication
		waitForEntityReplication(fileViewId, fileViewId);
		IdAndVersion viewId = IdAndVersion.parse(fileViewId);
		// Wait for the 
		Query query = new Query();
		query.setSql("select * from "+fileViewId);
		
		QueryOptions options = new QueryOptions().withRunQuery(true).withReturnLastUpdatedOn(true);
		
		final Date lastUpdatedOn = waitForConsistentQuery(adminUserInfo, query, options, (results) -> {			
			assertNotNull(results);
			Date startingLastUpdatedOn = results.getLastUpdatedOn();
			assertNotNull(startingLastUpdatedOn);
		}).getLastUpdatedOn();
		
		// sleep to ensure lastUpdatedOnChanges
		Thread.sleep(101);

		// Update a file in the view
		String fileIdToUpdate = fileIds.get(0);
		FileEntity toUpdate = entityManager.getEntity(adminUserInfo, fileIdToUpdate, FileEntity.class);
		toUpdate.setName(toUpdate.getName()+"updated");
		boolean newVersion = false;
		String activityId = null;
		entityManager.updateEntity(adminUserInfo, toUpdate, newVersion, activityId);
		toUpdate = entityManager.getEntity(adminUserInfo, fileIdToUpdate, FileEntity.class);
		
		// wait for replication
		waitForEntityReplication(fileViewId, fileIdToUpdate);
		
		/*
		 * In the past this call would change the view's state to be processing when the
		 * view as out-of-date with the replication. Now when the view is out-of-date it
		 * must remain available for query while the worker applies deltas to the live
		 * view.
		 */
		TableStatus viewStatus = tableManagerSupport.getTableStatusOrCreateIfNotExists(viewId);
		assertEquals(TableState.AVAILABLE, viewStatus.getState());
		// wait for the query results
		waitForConsistentQuery(adminUserInfo, query, options, (results) -> {			
			assertNotNull(results.getLastUpdatedOn());
			// The view should have been updated since the last query
			assertTrue(results.getLastUpdatedOn().after(lastUpdatedOn));
		});
	}

	/**
	 * Existing views need to get built on a new stack even though
	 * the view state does not exist (see PLFM-6060). 
	 */
	@Test
	public void testPLFM_6060() throws Exception {
		createFileView();
		// wait for replication
		waitForEntityReplication(fileViewId, fileViewId);
		IdAndVersion viewId = IdAndVersion.parse(fileViewId);
		// wait for the view to become available to ensure the worker is done with the view.
		waitForViewToBeAvailable(viewId);
		
		// simulate the case where there is no state for the view
		tableStatusDao.clearAllTableState();
		this.tableConnectionFactory.getConnection(viewId).deleteTable(viewId);

		// simulate what happens after the migration of new stack, 
		// sending a change message to the view worker.
		broadcastChangeMessageToViewWorker(viewId);
		// The view should become available only from the message
		waitForViewToBeAvailable(viewId);
	}

	@Test
	public void testEntityView_multipleValueColumnRoundTrip() throws Exception {
		defaultColumnIds.add(stringListColumn.getId());
		createFileView();

		assertTrue(fileCount >= 2, "setup() needs to create at least 2 entities for this test to work");

		//set annotations for 2 files
		Annotations fileAnnotation1 = entityManager.getAnnotations(adminUserInfo, fileIds.get(0));
		AnnotationsV2TestUtils.putAnnotations(fileAnnotation1, stringListColumn.getName(), Arrays.asList("val1", "val2"), AnnotationsValueType.STRING);
		entityManager.updateAnnotations(adminUserInfo, fileIds.get(0), fileAnnotation1);

		Annotations fileAnnotation2 = entityManager.getAnnotations(adminUserInfo, fileIds.get(1));
		AnnotationsV2TestUtils.putAnnotations(fileAnnotation2, stringListColumn.getName(), Arrays.asList("val2", "val3"), AnnotationsValueType.STRING);
		entityManager.updateAnnotations(adminUserInfo, fileIds.get(1), fileAnnotation2);


		waitForEntityReplication(fileViewId, fileIds.get(0));


		waitForRowCount(adminUserInfo, "select id, etag, "+ stringListColumn.getName() +" from " + fileViewId, fileCount);
		
		//only 1 annotation has "val1" as a value
		waitForRowCount(adminUserInfo, "select * from "+ fileViewId + " where "+ stringListColumn.getName() +" HAS ('val1')", 1);
		//both annotations have "val2" as a value
		waitForRowCount(adminUserInfo, "select * from "+ fileViewId + " where "+ stringListColumn.getName() +" HAS ('val2')", 2);
		
		//HAS "val1" or "val3" should also cover both values
		waitForRowCount(adminUserInfo, "select * from "+ fileViewId + " where "+ stringListColumn.getName() +" HAS ('val1', 'val3')", 2);

		//modify annotation values by using updates to table view
		QueryResultBundle results = waitForRowCount(adminUserInfo, "select id, etag, "+ stringListColumn.getName() +" from " + fileViewId, fileCount);

		//change multiValueKey to new values
		RowSet rowsets = results.getQueryResult().getQueryResults();

		String firstChangeId = rowsets.getRows().get(0).getValues().get(0);
		rowsets.getRows().get(0).getValues().set(2, "[\"newVal1\", \"newVal2\"]");
		String secondChangeId = rowsets.getRows().get(1).getValues().get(0);
		rowsets.getRows().get(1).getValues().set(2, "[\"newVal4\", \"newVal5\", \"newVal6\"]");
		//push modified values to view
		updateView(rowsets, fileViewId);

		//check view is updated
		waitForRowCount(adminUserInfo, "select * from "+ fileViewId + " where "+ stringListColumn.getName() +" HAS ('newVal1', 'newVal6')", 2);
		
		//check Annotations on entities are updated
		assertEquals(Arrays.asList("newVal1", "newVal2"), entityManager.getAnnotations(adminUserInfo, firstChangeId).getAnnotations().get(stringListColumn.getName()).getValue());
		assertEquals(Arrays.asList("newVal4", "newVal5", "newVal6"), entityManager.getAnnotations(adminUserInfo, secondChangeId).getAnnotations().get(stringListColumn.getName()).getValue());
	}

	@Test
	public void testEntityView_multipleValueUserIdListAndEntityIdListRoundTrip() throws Exception {
		ColumnModel userIdList = new ColumnModel();
		userIdList.setColumnType(ColumnType.USERID_LIST);
		userIdList.setName("userIdList");
		userIdList = columnModelManager.createColumnModel(userIdList);

		ColumnModel entityIdList = new ColumnModel();
		entityIdList.setColumnType(ColumnType.ENTITYID_LIST);
		entityIdList.setName("entityIdList");
		entityIdList = columnModelManager.createColumnModel(entityIdList);

		defaultColumnIds.add(userIdList.getId());
		defaultColumnIds.add(entityIdList.getId());
		createFileView();

		assertTrue(fileCount >= 2, "setup() needs to create at least 2 entities for this test to work");

		//set annotations for 2 files
		Annotations fileAnnotation1 = entityManager.getAnnotations(adminUserInfo, fileIds.get(0));
		AnnotationsV2TestUtils.putAnnotations(fileAnnotation1, userIdList.getName(), Arrays.asList("111", "222"), AnnotationsValueType.LONG);
		AnnotationsV2TestUtils.putAnnotations(fileAnnotation1, entityIdList.getName(), Arrays.asList("999", "888"), AnnotationsValueType.LONG);
		entityManager.updateAnnotations(adminUserInfo, fileIds.get(0), fileAnnotation1);

		Annotations fileAnnotation2 = entityManager.getAnnotations(adminUserInfo, fileIds.get(1));
		AnnotationsV2TestUtils.putAnnotations(fileAnnotation2, userIdList.getName(), Arrays.asList("333", "444"), AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(fileAnnotation2, entityIdList.getName(), Arrays.asList("777", "666"), AnnotationsValueType.LONG);
		entityManager.updateAnnotations(adminUserInfo, fileIds.get(1), fileAnnotation2);


		waitForEntityReplication(fileViewId, fileIds.get(0));

		String query = "select id, etag, "+ userIdList.getName() + ", " + entityIdList.getName() +" from " + fileViewId;
		
		// expecting all rows
		QueryResultBundle results = waitForRowCount(adminUserInfo, query, fileCount);

		//change multiValueKey to new values
		RowSet rowsets = results.getQueryResult().getQueryResults();

		List<Row> rows = rowsets.getRows();
		assertEquals(Arrays.asList("[111, 222]", "[\"syn999\",\"syn888\"]"),rows.get(0).getValues().subList(2,4));
		assertEquals(Arrays.asList("[333, 444]", "[\"syn777\",\"syn666\"]"),rows.get(1).getValues().subList(2,4));


		String firstChangeId = "syn" + rowsets.getRows().get(0).getRowId();
		//change user Id
		rowsets.getRows().get(0).getValues().set(2, "[\"123\", \"456\"]");
		String secondChangeId = "syn" + rowsets.getRows().get(1).getRowId();
		//change Entity Id
		rowsets.getRows().get(1).getValues().set(3, "[\"syn314\", \"159\", \"syn265\"]");
		//push modified values to view
		updateView(rowsets, fileViewId);

		//check view is updated
		query = "select * from "+ fileViewId + " where "+ userIdList.getName() +" HAS ('123', '333')";
		
		waitForRowCount(adminUserInfo, query, 2);
		
		//check Annotations on entities are updated
		assertEquals(Arrays.asList("123", "456"), entityManager.getAnnotations(adminUserInfo, firstChangeId).getAnnotations().get(userIdList.getName()).getValue());
		assertEquals(Arrays.asList("314", "159", "265"), entityManager.getAnnotations(adminUserInfo, secondChangeId).getAnnotations().get(entityIdList.getName()).getValue());
	}


	@Test
	public void testEntityView_multipleValueColumn_UpdatedAnnotationExceedListMaxSize() throws Exception {
		defaultColumnIds.add(stringListColumn.getId());
		createFileView();

		assertTrue(fileCount >= 2, "setup() needs to create at least 2 entities for this test to work");

		//set annotations for 2 files
		Annotations fileAnnotation1 = entityManager.getAnnotations(adminUserInfo, fileIds.get(0));
		AnnotationsV2TestUtils.putAnnotations(fileAnnotation1, stringListColumn.getName(), Arrays.asList("val1", "val2"), AnnotationsValueType.STRING);
		entityManager.updateAnnotations(adminUserInfo, fileIds.get(0), fileAnnotation1);
		waitForEntityReplication(fileViewId, fileIds.get(0));

		waitForRowCount(adminUserInfo, "select id, etag, "+ stringListColumn.getName() +" from " + fileViewId, fileCount);
		
		//this annotation will exceed the limit
		Annotations fileAnnotation2 = entityManager.getAnnotations(adminUserInfo, fileIds.get(1));
		AnnotationsV2TestUtils.putAnnotations(fileAnnotation2, stringListColumn.getName(), Arrays.asList("val2", "val3", "val1", "val4"), AnnotationsValueType.STRING);
		entityManager.updateAnnotations(adminUserInfo, fileIds.get(1), fileAnnotation2);

		waitForEntityReplication(fileViewId, fileIds.get(1));
		
		String error = assertThrows(AsynchJobFailedException.class, () ->
			waitForConsistentQuery(adminUserInfo, "select id, etag, "+ stringListColumn.getName() +" from " + fileViewId, (results) -> {
				fail("This should fail eventually");
			})
		).getMessage();
		
		assertEquals("maximumListLength for ColumnModel \"stringList\" must be at least: 4", error);

	}

	@Test
	public void testEntityView_multipleValueColumnRoundTrip_changeMaxListLength() throws Exception {
		defaultColumnIds.add(stringListColumn.getId());
		createFileView();

		assertTrue(fileCount >= 2, "setup() needs to create at least 2 entities for this test to work");

		//set annotations for 2 files
		Annotations fileAnnotation1 = entityManager.getAnnotations(adminUserInfo, fileIds.get(0));
		AnnotationsV2TestUtils.putAnnotations(fileAnnotation1, stringListColumn.getName(), Arrays.asList("val1", "val2", "val3"), AnnotationsValueType.STRING);
		entityManager.updateAnnotations(adminUserInfo, fileIds.get(0), fileAnnotation1);

		Annotations fileAnnotation2 = entityManager.getAnnotations(adminUserInfo, fileIds.get(1));
		AnnotationsV2TestUtils.putAnnotations(fileAnnotation2, stringListColumn.getName(), Arrays.asList("val2", "val3"), AnnotationsValueType.STRING);
		entityManager.updateAnnotations(adminUserInfo, fileIds.get(1), fileAnnotation2);

		waitForEntityReplication(fileViewId, fileIds.get(0));

		waitForRowCount(adminUserInfo, "select id, etag, "+ stringListColumn.getName() +" from " + fileViewId, fileCount);

		//now reduce the maxListSize

		ColumnModel smallerStringListColumn = new ColumnModel();
		smallerStringListColumn.setName("stringList");
		smallerStringListColumn.setColumnType(ColumnType.STRING_LIST);
		smallerStringListColumn.setMaximumListLength(2L);
		smallerStringListColumn = columnModelManager.createColumnModel(adminUserInfo, smallerStringListColumn);

		// change the schema as a transaction
		ColumnChange remove = new ColumnChange();
		remove.setOldColumnId(stringListColumn.getId());
		remove.setNewColumnId(smallerStringListColumn.getId());
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
		startAndWaitForJob(adminUserInfo, transaction, (TableUpdateTransactionResponse response) -> {
			assertNotNull(response);
		});

		String error = assertThrows(AsynchJobFailedException.class, () ->
			waitForConsistentQuery(adminUserInfo, "select id, etag, "+ stringListColumn.getName() +" from " + fileViewId, (result) -> {
				fail("Should eventually fail");
			})
		).getMessage();
		
		assertEquals("maximumListLength for ColumnModel \"stringList\" must be at least: 3", error);
	}


	@Test
	public void testEntityView_multipleValueColumnRoundTrip_updateRowGreaterThanMaxListLength() throws Exception {
		defaultColumnIds.add(stringListColumn.getId());
		createFileView();

		assertTrue(fileCount >= 2, "setup() needs to create at least 2 entities for this test to work");

		//set annotations for 2 files
		Annotations fileAnnotation1 = entityManager.getAnnotations(adminUserInfo, fileIds.get(0));
		AnnotationsV2TestUtils.putAnnotations(fileAnnotation1, stringListColumn.getName(), Arrays.asList("val1", "val2", "val3"), AnnotationsValueType.STRING);
		entityManager.updateAnnotations(adminUserInfo, fileIds.get(0), fileAnnotation1);

		Annotations fileAnnotation2 = entityManager.getAnnotations(adminUserInfo, fileIds.get(1));
		AnnotationsV2TestUtils.putAnnotations(fileAnnotation2, stringListColumn.getName(), Arrays.asList("val2", "val3"), AnnotationsValueType.STRING);
		entityManager.updateAnnotations(adminUserInfo, fileIds.get(1), fileAnnotation2);


		waitForEntityReplication(fileViewId, fileIds.get(0));


		QueryResultBundle result = waitForRowCount(adminUserInfo, "select " + stringListColumn.getName() + " from " + fileViewId, fileCount);

		//update annotations via row changes to exceed maximumListLength
		RowSet rowset = result.getQueryResult().getQueryResults();
		rowset.getRows().get(0).setValues(Collections.singletonList("[\"val1\",\"val2\",\"val3\",\"val4\"]"));

		// wait for the change to complete
		String error = assertThrows(IllegalArgumentException.class, () ->
				updateView(rowset,fileViewId)
		).getMessage();
		
		assertEquals("Value at [0,16] was not a valid STRING_LIST. Exceeds the maximum number of list elements defined in the ColumnModel (3): \"[\"val1\",\"val2\",\"val3\",\"val4\"]\"", error);
	}
	
	/**
	 * Test for PLFM-5651 and the addition of both file size and file MD5s in views.
	 * @throws Exception 
	 */
	@Test
	public void testFileSizeAndMD5() throws Exception {
		createFileView();
		String fileZero = fileIds.get(0);
		waitForEntityReplication(fileViewId, fileZero);
		String sql = "select " + ObjectField.dataFileMD5Hex + "," + ObjectField.dataFileSizeBytes + " from "
				+ fileViewId + " where " + ObjectField.id + " = '" + fileZero+"'";
		
		waitForConsistentQuery(adminUserInfo, sql, (results) -> {			
			List<Row> rows = extractRows(results);
			assertNotNull(rows);
			assertEquals(1, rows.size());
			Row row = rows.get(0);
			assertNotNull(row);
			assertNotNull(row.getValues());
			assertEquals(2, row.getValues().size());
			assertEquals(sharedHandle.getContentMd5(), row.getValues().get(0));
			assertEquals(sharedHandle.getContentSize().toString(), row.getValues().get(1));
		});
	}
	
	/**
	 * This is a test added for PLFM-6468.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAddViewSnapshotToDownloadList() throws Exception {
		createFileView();
		String firstFileId = fileIds.get(0);
		asyncHelper.waitForEntityReplication(adminUserInfo, fileViewId, firstFileId, MAX_WAIT_MS);

		// create a snapshot of the view
		SnapshotRequest snapshotOptions = new SnapshotRequest();
		snapshotOptions.setSnapshotComment("first snapshot");
		TableUpdateTransactionRequest transactionRequest = new TableUpdateTransactionRequest();
		transactionRequest.setEntityId(fileViewId);
		transactionRequest.setChanges(null);
		transactionRequest.setCreateSnapshot(true);
		transactionRequest.setSnapshotOptions(snapshotOptions);
		Long snaphsotVersionNumber = startAndWaitForJob(adminUserInfo, transactionRequest, (TableUpdateTransactionResponse response) -> {				
			assertNotNull(response);
			assertNotNull(response.getSnapshotVersionNumber());
		}).getSnapshotVersionNumber();

		// Query the snapshot
		Query query = new Query();
		query.setSql("select * from " + fileViewId + "." + snaphsotVersionNumber+" where id='"+firstFileId+"'");
		query.setIncludeEntityEtag(true);
		
		QueryResultBundle bundle = waitForConsistentQuery(adminUserInfo, query, (queryResults) -> {			
			assertNotNull(queryResults);
			List<Row> rows = extractRows(queryResults);
			assertEquals(1, rows.size());
		});
		Row firstRow = extractRows(bundle).get(0);
		assertEquals(KeyFactory.stringToKey(firstFileId), firstRow.getRowId());
		assertEquals(1L, firstRow.getVersionNumber());

		FileEntity firstFile = entityManager.getEntity(adminUserInfo, firstFileId, FileEntity.class);
		String versionOneFileHandleId = firstFile.getDataFileHandleId();
		assertEquals(sharedHandle.getId(), versionOneFileHandleId);
		// Create a new version of the file with a new file handle
		S3FileHandle newFileHandle = createNewFileHandle();
		firstFile.setDataFileHandleId(newFileHandle.getId());
		firstFile.setVersionLabel("added a new data file");
		boolean createNewVersion = true;
		String activityId = null;
		// create a new version of the file
		entityManager.updateEntity(adminUserInfo, firstFile, createNewVersion, activityId);
		firstFile = entityManager.getEntity(adminUserInfo, firstFileId, FileEntity.class);
		assertEquals(2L, firstFile.getVersionNumber());
		assertEquals(newFileHandle.getId(), firstFile.getDataFileHandleId());
		
		// finally, use a query on the view snapshot to add the files the user's download list
		AddFileToDownloadListRequest request = new AddFileToDownloadListRequest();
		request.setQuery(query);
		
		// call under test
		asyncHelper.assertJobResponse(adminUserInfo, request, (AddFileToDownloadListResponse response) -> {		
			assertNotNull(response);
			assertNotNull(response.getDownloadList());
			List<FileHandleAssociation> list = response.getDownloadList().getFilesToDownload();
			assertNotNull(list);
			assertEquals(1, list.size());
			FileHandleAssociation association = list.get(0);
			assertEquals(firstFileId, association.getAssociateObjectId());
			assertEquals(FileHandleAssociateType.FileEntity, association.getAssociateObjectType());
			assertEquals(versionOneFileHandleId, association.getFileHandleId());
		}, MAX_WAIT_MS);
		
	}
	
	@Test
	public void testTableViewWithBooleanAnnotations() throws Exception{
		// one
		String fileId = fileIds.get(0);
		Annotations annos = entityManager.getAnnotations(adminUserInfo, fileId);
		AnnotationsV2TestUtils.putAnnotations(annos, booleanColumn.getName(), "true", AnnotationsValueType.BOOLEAN);
		entityManager.updateAnnotations(adminUserInfo, fileId, annos);
		// two
		fileId = fileIds.get(1);
		annos = entityManager.getAnnotations(adminUserInfo, fileId);
		AnnotationsV2TestUtils.putAnnotations(annos, booleanColumn.getName(), "false", AnnotationsValueType.BOOLEAN);
		entityManager.updateAnnotations(adminUserInfo, fileId, annos);
		// three
		fileId = fileIds.get(2);
		annos = entityManager.getAnnotations(adminUserInfo, fileId);
		AnnotationsV2TestUtils.putAnnotations(annos, booleanColumn.getName(), "True", AnnotationsValueType.BOOLEAN);
		entityManager.updateAnnotations(adminUserInfo, fileId, annos);

		// Create the view
		defaultColumnIds = Lists.newArrayList(booleanColumn.getId(), etagColumn.getId());
		createFileView();

		// Query for the values as strings.
		String sql = "select "+booleanColumn.getName()+", "+etagColumn.getName()+" from "+fileViewId;
		
		RowSet rowSet = waitForConsistentQuery(adminUserInfo, sql, (results) -> {
			List<Row> rows  = extractRows(results);
			assertEquals(3, rows.size());
			assertEquals("true", rows.get(0).getValues().get(0));
			assertEquals("false", rows.get(1).getValues().get(0));
			assertEquals("true", rows.get(2).getValues().get(0));			
		}).getQueryResult().getQueryResults();

		// use the results to update the annotations.
		List<EntityUpdateResult> updates = updateView(rowSet, fileViewId);

		assertEquals(3, updates.size());
		// all of the update should have succeeded.
		for(EntityUpdateResult eur: updates){
			assertEquals(null, eur.getFailureMessage());
			assertEquals(null, eur.getFailureCode());
		}
	}
	
	/**
	 * Broadcast a change message to the view worker.
	 * 
	 * @param viewId
	 */
	void broadcastChangeMessageToViewWorker(IdAndVersion viewId) {
		// Send a message to the worker to build the view
		ChangeMessage message = new ChangeMessage();
		message.setChangeType(ChangeType.CREATE);
		message.setObjectType(ObjectType.ENTITY_VIEW);
		message.setObjectId(viewId.toString());
		message = changeDAO.replaceChange(message);
		this.repositoryMessagePublisher.publishToTopic(message);
	}

	/**
	 * Wait for the view to become available.
	 * 
	 * @param viewId
	 * @throws InterruptedException
	 */
	void waitForViewToBeAvailable(IdAndVersion viewId) throws InterruptedException {
		long startTime = System.currentTimeMillis();
		while(true) {
			Optional<TableState> optional = tableManagerSupport.getTableStatusState(viewId);
			if(optional.isPresent() && TableState.AVAILABLE.equals(optional.get())) {
				break;
			}
			assertTrue((System.currentTimeMillis()-startTime) < MAX_WAIT_MS, "Timed out waiting for a view to become available.");
			System.out.println("Waiting for view to become available.");
			Thread.sleep(2000);
		}
	}
	

	/**
	 * Helper to update a view using a result set.
	 * @param rowSet
	 * @param viewId
	 * @return
	 * @throws InterruptedException
	 * @throws AsynchJobFailedException 
	 */
	public List<EntityUpdateResult> updateView(RowSet rowSet, String viewId) throws InterruptedException, AsynchJobFailedException{
		AppendableRowSetRequest appendRequest = new AppendableRowSetRequest();
		appendRequest.setEntityId(viewId);
		appendRequest.setToAppend(rowSet);
		List<TableUpdateRequest> updates = new LinkedList<TableUpdateRequest>();
		updates.add(appendRequest);
		TableUpdateTransactionRequest tutr = new TableUpdateTransactionRequest();
		tutr.setEntityId(viewId);
		tutr.setChanges(updates);
		
		EntityUpdateResults results = (EntityUpdateResults) startAndWaitForJob(adminUserInfo, tutr, (TableUpdateTransactionResponse response) -> {
			assertNotNull(response);
			assertNotNull(response.getResults());
			assertEquals(1, response.getResults().size());
			EntityUpdateResults updateResults = (EntityUpdateResults)response.getResults().get(0);
			assertNotNull(updateResults.getUpdateResults());
		}).getResults().get(0);
		
		return results.getUpdateResults();
	}
	
	/**
	 * Helper to get the rows from a query.
	 * 
	 * @param results
	 * @return
	 */
	private List<Row> extractRows(QueryResultBundle results) {
		assertNotNull(results);
		assertNotNull(results.getQueryResult());
		assertNotNull(results.getQueryResult().getQueryResults());
		assertNotNull(results.getQueryResult().getQueryResults().getRows());
		return results.getQueryResult().getQueryResults().getRows();
	}
	
	/**
	 * Start an asynchronous job and wait for the results.
	 * @param user
	 * @param body
	 * @return
	 * @throws InterruptedException 
	 * @throws AsynchJobFailedException 
	 */
	public <T extends AsynchronousResponseBody> T  startAndWaitForJob(UserInfo user, AsynchronousRequestBody body, Consumer<T> responseConsumer) throws InterruptedException, AsynchJobFailedException{
		return asyncHelper.assertJobResponse(user, body, responseConsumer, MAX_WAIT_MS).getResponse();
	}
	
	/**
	 * Wait for a query to return the expected number of rows.
	 * @param user
	 * @param sql
	 * @param rowCount
	 * @return
	 * @throws Exception
	 */
	private QueryResultBundle waitForRowCount(UserInfo user, String sql, int rowCount) throws Exception {
		Query query = new Query();
		query.setSql(sql);
		
		QueryOptions options = new QueryOptions()
				.withRunQuery(true)
				.withRunCount(true)
				.withReturnFacets(false);
		
		return waitForConsistentQuery(user, query, options, (results) -> {
			List<Row> rows = extractRows(results);
			assertEquals(rowCount, rows.size());
		});
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
	private QueryResultBundle waitForConsistentQuery(UserInfo user, String sql,  Consumer<QueryResultBundle> resultMatcher) throws Exception {
		Query query = new Query();
		query.setSql(sql);
		return waitForConsistentQuery(user, query, resultMatcher);
	}
	
	private QueryResultBundle waitForConsistentQuery(UserInfo user, Query query, Consumer<QueryResultBundle> resultMatcher) throws Exception {
		QueryOptions options = new QueryOptions().withRunQuery(true).withRunCount(true).withReturnFacets(false);
		return waitForConsistentQuery(user, query, options, resultMatcher);
	}
	
	private QueryResultBundle waitForConsistentQuery(UserInfo user, Query query, QueryOptions options, Consumer<QueryResultBundle> resultMatcher) throws Exception {
		return asyncHelper.assertQueryResult(user, query, options, resultMatcher, MAX_WAIT_MS);
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
	private ObjectDataDTO waitForEntityReplication(String tableId, String entityId) throws InterruptedException{
		return asyncHelper.waitForEntityReplication(adminUserInfo, tableId, entityId, MAX_WAIT_MS);
	}

}
