package org.sagebionetworks.repo.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenHelper;
import org.sagebionetworks.repo.manager.oauth.OpenIDConnectManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.SnapshotRequest;
import org.sagebionetworks.repo.model.table.SnapshotResponse;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.service.metadata.EventType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.Lists;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class EntityServiceImplAutowiredTest  {

	@Autowired
	private EntityService entityService;
	
	@Autowired
	private FileHandleDao fileHandleDao;
	
	@Autowired
	private ColumnModelManager columnModelManager;
	
	@Autowired TableEntityManager tableEntityManager;

	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private OIDCTokenHelper oidcTokenHelper;
	
	@Autowired
	private OpenIDConnectManager oidcManager;
	
	private String accessToken;
	
	private Project project;
	private List<String> toDelete;
	private HttpServletRequest mockRequest;
	private Long adminUserId;
	private UserInfo adminUserInfo;
	
	private S3FileHandle fileHandle1;
	private S3FileHandle fileHandle2;
	
	private ColumnModel column;
	
	@BeforeEach
	public void before() throws Exception {
		toDelete = new LinkedList<String>();
		// Map test objects to their urls
		// Make sure we have a valid user.
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		
		accessToken = oidcTokenHelper.createTotalAccessToken(adminUserId);
		adminUserInfo = oidcManager.getUserAuthorization(accessToken);
		UserInfo.validateUserInfo(adminUserInfo);

		mockRequest = Mockito.mock(HttpServletRequest.class);
		when(mockRequest.getServletPath()).thenReturn("/repo/v1");
		// Create a project
		project = new Project();
		project = entityService.createEntity(adminUserInfo, project, null);
		toDelete.add(project.getId());
		
		// Create some file handles
		fileHandle1 = new S3FileHandle();
		fileHandle1.setBucketName("bucket");
		fileHandle1.setKey("key");
		fileHandle1.setCreatedBy(adminUserInfo.getId().toString());
		fileHandle1.setCreatedOn(new Date());
		fileHandle1.setContentSize(123l);
		fileHandle1.setConcreteType("text/plain");
		fileHandle1.setEtag("etag");
		fileHandle1.setFileName("foo.bar");
		fileHandle1.setContentMd5("md5");
		fileHandle1.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		fileHandle1.setEtag(UUID.randomUUID().toString());
		
		fileHandle2 = new S3FileHandle();
		fileHandle2.setBucketName("bucket");
		fileHandle2.setKey("key2");
		fileHandle2.setCreatedBy(adminUserInfo.getId().toString());
		fileHandle2.setCreatedOn(new Date());
		fileHandle2.setContentSize(123l);
		fileHandle2.setConcreteType("text/plain");
		fileHandle2.setEtag("etag");
		fileHandle2.setFileName("two.txt");
		fileHandle2.setContentMd5("md5");
		fileHandle2.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		fileHandle2.setEtag(UUID.randomUUID().toString());

		List<FileHandle> fileHandleToCreate = new LinkedList<FileHandle>();
		fileHandleToCreate.add(fileHandle1);
		fileHandleToCreate.add(fileHandle2);
		fileHandleDao.createBatch(fileHandleToCreate);
		fileHandle1 = (S3FileHandle) fileHandleDao.get(fileHandle1.getId());
		fileHandle2 = (S3FileHandle) fileHandleDao.get(fileHandle2.getId());
		
		column = new ColumnModel();
		column.setColumnType(ColumnType.INTEGER);
		column.setName("anInteger");
		column = columnModelManager.createColumnModel(adminUserInfo, column);
	}
	
	@AfterEach
	public void after(){
		if(toDelete != null){
			for(String id: toDelete){
				try {
					entityService.deleteEntity(adminUserId, id);
				} catch (Exception e) {	}
			}
		}
		if(fileHandle1 != null){
			fileHandleDao.delete(fileHandle1.getId());
		}
		if(fileHandle2 != null){
			fileHandleDao.delete(fileHandle2.getId());
		}
	}
	
	/**
	 * PLFM-1754 "Disallow FileEntity with Null FileHandle"
	 * @throws Exception
	 */
	@Test
	public void testPLFM_1754CreateNullFileHandleId() throws Exception {
		FileEntity file = new FileEntity();
		file.setParentId(project.getId());
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			entityService.createEntity(adminUserInfo, file, null);
		});
	}
	
	/**
	 * PLFM-1754 "Disallow FileEntity with Null FileHandle"
	 * @throws Exception
	 */
	@Test
	public void testPLFM_1754HappyCase() throws Exception {
		FileEntity file = new FileEntity();
		file.setParentId(project.getId());
		file.setDataFileHandleId(fileHandle1.getId());
		file = entityService.createEntity(adminUserInfo, file, null);
		assertNotNull(file);
		// Make sure we can update it 
		file.setDataFileHandleId(fileHandle2.getId());
		file = entityService.updateEntity(adminUserInfo, file, false, null);
	}
	
	/**
	 * PLFM-1754 "Disallow FileEntity with Null FileHandle"
	 * @throws Exception
	 */
	@Test
	public void testPLFM_1754UpdateNull() throws Exception {
		FileEntity file = new FileEntity();
		file.setParentId(project.getId());
		file.setDataFileHandleId(fileHandle1.getId());
		final FileEntity createdFile = entityService.createEntity(adminUserInfo, file, null);
		assertNotNull(createdFile);
		// Now try to set it to null
		createdFile.setDataFileHandleId(null);
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			entityService.updateEntity(adminUserInfo, createdFile, false, null);
		});
	}
	
	/**
	 * PLFM-1744 "Any change to a FileEntity's 'dataFileHandleId' should trigger a new version."
	 * @throws NotFoundException 
	 * @throws UnauthorizedException 
	 * @throws InvalidModelException 
	 * @throws DatastoreException 
	 */
	@Test
	public void testPLFM_1744() throws Exception {
		FileEntity file = new FileEntity();
		file.setParentId(project.getId());
		file.setDataFileHandleId(fileHandle1.getId());
		file = entityService.createEntity(adminUserInfo, file, null);
		assertNotNull(file);
		assertEquals(new Long(1), file.getVersionNumber(), "Should start off as version one");
		// Make sure we can update it 
		file.setDataFileHandleId(fileHandle2.getId());
		file = entityService.updateEntity(adminUserInfo, file, false, null);
		// This should trigger a version change.
		assertEquals(new Long(2), file.getVersionNumber(), "Changing the dataFileHandleId of a FileEntity should have created a new version");
		// Now make sure if we change the name but the file
		file.setName("newName");
		file = entityService.updateEntity(adminUserInfo, file, false, null);
		assertEquals(new Long(2), file.getVersionNumber(), "A new version should not have been created when a name changed");
	}

	@Test
	public void testProjectAlias() {
		String alias1 = "alias" + RandomUtils.nextInt();
		String alias2 = "alias" + RandomUtils.nextInt();
		String alias3 = "alias" + RandomUtils.nextInt();
		// create alias1
		Project project1 = new Project();
		project1.setName("project" + UUID.randomUUID());
		project1.setAlias(alias1);
		project1 = entityService.createEntity(adminUserInfo, project1, null);
		toDelete.add(project1.getId());
		assertEquals(alias1, ((Project) entityService.getEntity(adminUserInfo, project1.getId())).getAlias());
		// create alias2
		Project project2 = new Project();
		project2.setName("project" + UUID.randomUUID());
		project2.setAlias(alias2);
		project2 = entityService.createEntity(adminUserInfo, project2, null);
		toDelete.add(project2.getId());
		assertEquals(alias2, ((Project) entityService.getEntity(adminUserInfo, project2.getId())).getAlias());
		// fail on create alias1
		Project projectFailCreate = new Project();
		projectFailCreate.setName("project" + UUID.randomUUID());
		projectFailCreate.setAlias(alias1);
		try {
			entityService.createEntity(adminUserInfo, projectFailCreate, null);
			fail("duplicate entry should have been rejected");
		} catch (NameConflictException e) {
			// expected
		}
		// update to null
		project2.setAlias(null);
		project2 = entityService.updateEntity(adminUserInfo, project2, false, null);
		assertNull(((Project) entityService.getEntity(adminUserInfo, project2.getId())).getAlias());
		// fail on update to alias1
		try {
			project2.setAlias(alias1);
			entityService.updateEntity(adminUserInfo, project2, false, null);
			fail("duplicate entry should have been rejected");
		} catch (NameConflictException e) {
			// expected
		}
		project2.setAlias(alias3);
		project2 = entityService.updateEntity(adminUserInfo, project2, false, null);
		assertEquals(alias3, ((Project) entityService.getEntity(adminUserInfo, project2.getId())).getAlias());
		// create alias2 again
		Project project2Again = new Project();
		project2Again.setName("project" + UUID.randomUUID());
		project2Again.setAlias(alias2);
		project2Again = entityService.createEntity(adminUserInfo, project2Again, null);
		toDelete.add(project2Again.getId());
		assertEquals(alias2, ((Project) entityService.getEntity(adminUserInfo, project2Again.getId())).getAlias());
	}
	
	@Test
	public void testTableEntityCreateGet(){
		List<String> columnIds = Lists.newArrayList(column.getId());
		TableEntity table = new TableEntity();
		table.setParentId(project.getId());
		table.setName("SampleTable");
		table.setColumnIds(columnIds);
		
		table = entityService.createEntity(adminUserInfo, table, null);
		assertEquals(columnIds, table.getColumnIds());
		// default label and comment should be added.
		assertEquals(TableConstants.IN_PROGRESS, table.getVersionLabel());
		assertEquals(TableConstants.IN_PROGRESS, table.getVersionComment());
		
		table = entityService.getEntity(adminUserId, table.getId(), TableEntity.class);
		assertEquals(columnIds, table.getColumnIds());
		assertEquals(TableConstants.IN_PROGRESS, table.getVersionLabel());
		assertEquals(TableConstants.IN_PROGRESS, table.getVersionComment());
	}
	
	@Test
	public void testTableEntityCreateWithLableAndComment(){
		List<String> columnIds = Lists.newArrayList(column.getId());
		TableEntity table = new TableEntity();
		table.setParentId(project.getId());
		table.setName("SampleTable");
		table.setColumnIds(columnIds);
		String label = "a label";
		String comment = "a comment";
		table.setVersionLabel(label);
		table.setVersionComment(comment);
		
		table = entityService.createEntity(adminUserInfo, table, null);
		assertEquals(columnIds, table.getColumnIds());
		// default label and comment should be added.
		assertEquals(label, table.getVersionLabel());
		assertEquals(comment, table.getVersionComment());
		
		table = entityService.getEntity(adminUserId, table.getId(), TableEntity.class);
		assertEquals(columnIds, table.getColumnIds());
		assertEquals(label, table.getVersionLabel());
		assertEquals(comment, table.getVersionComment());
	}
	
	@Test
	public void testCreateTableEntityVersion() {
		List<String> columnIds = Lists.newArrayList(column.getId());
		TableEntity table = new TableEntity();
		table.setParentId(project.getId());
		table.setName("SampleTable");
		table.setColumnIds(columnIds);
		
		table = entityService.createEntity(adminUserInfo, table, null);
		// the first version of a table should not have a transaction linked
		Optional<Long> optional =tableEntityManager.getTransactionForVersion(table.getId(), table.getVersionNumber());
		assertNotNull(optional);
		assertFalse(optional.isPresent());
	}
	
	@Test
	public void testTableUpdateNewVersion() {
		List<String> columnIds = Lists.newArrayList(column.getId());
		TableEntity table = new TableEntity();
		table.setParentId(project.getId());
		table.setName("SampleTable");
		table.setColumnIds(columnIds);
		
		table = entityService.createEntity(adminUserInfo, table, null);
		String activityId = null;
		boolean newVersion = true;
		table.setVersionLabel(null);
		// Call under test
		entityService.updateEntity(adminUserInfo, table, newVersion, activityId);
	}
	
	@Test
	public void testTableUpdateNoNewVersion() {
		List<String> columnIds = Lists.newArrayList(column.getId());
		TableEntity table = new TableEntity();
		table.setParentId(project.getId());
		table.setName("SampleTable");
		table.setColumnIds(columnIds);
		
		table = entityService.createEntity(adminUserInfo, table, null);
		long firstVersion = table.getVersionNumber();
		String activityId = null;
		boolean newVersion = false;
		// Create a new version of the entity
		table = entityService.updateEntity(adminUserInfo, table, newVersion, activityId);
		assertTrue(firstVersion == table.getVersionNumber());
		// update without a version change should not result in the binding of a transaction.
		Optional<Long> optional =tableEntityManager.getTransactionForVersion(table.getId(), table.getVersionNumber());
		assertNotNull(optional);
		assertFalse(optional.isPresent());
	}
	
	/**
	 * Test for PLFM-5685
	 */
	@Test
	public void testGetEntityVersionTableSchema() {
		
		ColumnModel one = new ColumnModel();
		one.setColumnType(ColumnType.INTEGER);
		one.setName("one");
		one = columnModelManager.createColumnModel(adminUserInfo, one);
		
		ColumnModel two = new ColumnModel();
		two.setColumnType(ColumnType.INTEGER);
		two.setName("two");
		two = columnModelManager.createColumnModel(adminUserInfo, two);
		
		List<String> firstSchema = Lists.newArrayList(one.getId());
		List<String> secondSchema = Lists.newArrayList(one.getId(), two.getId());
		TableEntity table = new TableEntity();
		table.setParentId(project.getId());
		table.setName("PLFM-5685");
		table.setColumnIds(firstSchema);
		
		table = entityService.createEntity(adminUserInfo, table, null);
		
		// call under test
		table = entityService.getEntityForVersion(adminUserInfo, table.getId(), 1L, TableEntity.class);
		assertEquals(firstSchema, table.getColumnIds());
		
		// create a snapshot to to create a new version.
		SnapshotRequest snapshotRequest = new SnapshotRequest();
		SnapshotResponse snapshotResponse = tableEntityManager.createTableSnapshot(adminUserInfo, table.getId(), snapshotRequest);
		snapshotResponse.getSnapshotVersionNumber();
		
		// call under test
		table = entityService.getEntityForVersion(adminUserInfo, table.getId(), 1L, TableEntity.class);
		assertEquals(firstSchema, table.getColumnIds());
		// call under test
		table = entityService.getEntityForVersion(adminUserInfo, table.getId(), 2L, TableEntity.class);
		assertEquals(firstSchema, table.getColumnIds());
		// call under test
		table = entityService.getEntity(adminUserInfo, table.getId(), TableEntity.class, EventType.GET);
		assertEquals(firstSchema, table.getColumnIds());
		
		// change the schema of version two.
		boolean newVersion = false;
		String activityId = null;
		table.setColumnIds(secondSchema);
		table = entityService.updateEntity(adminUserInfo, table, newVersion, activityId);
		
		// call under test
		table = entityService.getEntityForVersion(adminUserInfo, table.getId(), 1L, TableEntity.class);
		assertEquals(firstSchema, table.getColumnIds());
		// call under test
		table = entityService.getEntityForVersion(adminUserInfo, table.getId(), 2L, TableEntity.class);
		assertEquals(secondSchema, table.getColumnIds());
		// call under test
		table = entityService.getEntity(adminUserInfo, table.getId(), TableEntity.class, EventType.GET);
		assertEquals(secondSchema, table.getColumnIds());
	}
}
