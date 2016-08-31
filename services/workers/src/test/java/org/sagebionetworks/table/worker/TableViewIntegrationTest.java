package org.sagebionetworks.table.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.EntityPermissionsManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.manager.table.TableQueryManager;
import org.sagebionetworks.repo.manager.table.TableViewManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.SortItem;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.model.table.ViewType;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class TableViewIntegrationTest {
	
	public static final int MAX_WAIT_MS = 1000 * 60;
	
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
	private TableViewManager tableViewMangaer;
	@Autowired
	private TableQueryManager tableQueryManger;
	@Autowired
	EntityPermissionsManager entityPermissionsManager;
	
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
		boolean shouldPreviewBeGenerated = false;
		sharedHandle = fileHandleDao.createFile(sharedHandle, shouldPreviewBeGenerated);
		
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
		
		List<ColumnModel> defaultSchema = tableManagerSupport.getDefaultTableViewColumns(ViewType.file);
		defaultColumnIds = new LinkedList<String>();
		for(ColumnModel cm: defaultSchema){
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
		
		// Lookup the starting table status.
		TableStatus status = tableManagerSupport.getTableStatusOrCreateIfNotExists(fileViewId);
		
		// update the entity and run the query again
		file.setName("newName");
		entityManager.updateEntity(adminUserInfo, file, false, null);
		file = entityManager.getEntity(adminUserInfo, ""+fileId, FileEntity.class);
		// wait for the view status to change after the update
		waitForViewStatusChange(status, fileViewId);
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
				return tableQueryManger.querySinglePage(mockProgressCallbackVoid, user, sql, sortList, offset, limit, runQuery, runCount, isConsistent);
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
	 * Wait for a table's status to change.
	 * 
	 * @param startingStatus
	 * @param tablId
	 * @return
	 * @throws InterruptedException
	 */
	private TableStatus waitForViewStatusChange(TableStatus startingStatus, String tablId) throws InterruptedException{
		long start = System.currentTimeMillis();
		while(true){
			TableStatus nextStatus = tableManagerSupport.getTableStatusOrCreateIfNotExists(fileViewId);
			if(startingStatus.getResetToken().equals(nextStatus.getResetToken())){
				assertTrue("Timed out waiting for table view status change.", (System.currentTimeMillis()-start) <  MAX_WAIT_MS);
				System.out.println("Waiting for table status to change resetToken: "+nextStatus.getResetToken());
				Thread.sleep(1000);
			}else{
				return nextStatus;
			}
		}
		
	}

}
