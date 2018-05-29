package org.sagebionetworks.repo.manager.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.daemon.BackupAliasType;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.migration.ForeignKeyInfo;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableDAO;
import org.sagebionetworks.repo.model.dbo.persistence.DBONode;
import org.sagebionetworks.repo.model.dbo.persistence.DBORevision;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserGroup;
import org.sagebionetworks.repo.model.migration.BackupTypeRangeRequest;
import org.sagebionetworks.repo.model.migration.BackupTypeResponse;
import org.sagebionetworks.repo.model.migration.CalculateOptimalRangeRequest;
import org.sagebionetworks.repo.model.migration.CalculateOptimalRangeResponse;
import org.sagebionetworks.repo.model.migration.IdRange;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeChecksum;
import org.sagebionetworks.repo.model.migration.RestoreTypeRequest;
import org.sagebionetworks.repo.model.migration.RestoreTypeResponse;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * The Unit test for MigrationManagerImpl;
 * @author jmhill
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class MigrationManagerImplTest {
	
	@Mock
	MigratableTableDAO mockDao;
	@Mock
	StackStatusDao mockStatusDao;
	@Mock
	BackupFileStream mockBackupFileStream;
	@Mock
	AmazonS3 mockS3Client;
	@Mock
	FileProvider mockFileProvider;
	@Mock
	File mockFile;
	@Mock
	FileOutputStream mockOutputStream;
	@Mock
	InputStream mockInputStream;
	@Mock
	FileInputStream mockFileInputStream;
	@Mock
	UserInfo mockUser;
	@Captor
	ArgumentCaptor<Iterable<MigratableDatabaseObject<?, ?>>> iterableCator;
	@Mock
	MigrationTypeListener mockMigrationListener;
	@Captor
	ArgumentCaptor<GetObjectRequest> getObjectRequestCaptor;
	
	MigrationManagerImpl manager;
	
	DBONode nodeOne;
	DBORevision revOne;
	DBONode nodeTwo;
	DBORevision revTwo;
	
	BackupAliasType backupAlias;
	BackupTypeRangeRequest rangeRequest;
	Long batchSize;
	List<Long> backupIds;
	List<MigratableDatabaseObject<?, ?>> allObjects;
	List<Long> bootstrapPrincipalIds;
	List<MigratableDatabaseObject<?, ?>> nodeStream;
	List<MigratableDatabaseObject<?, ?>> revisionStream;
	RestoreTypeRequest restoreTypeRequest;
	CalculateOptimalRangeRequest optimalRangeRequest;
	List<IdRange> ranges;
	
	@Before
	public void before() throws IOException{
		manager = new MigrationManagerImpl();
		ReflectionTestUtils.setField(manager, "backupBatchMax", 50);
		ReflectionTestUtils.setField(manager, "migratableTableDao", mockDao);
		ReflectionTestUtils.setField(manager, "stackStatusDao", mockStatusDao);
		ReflectionTestUtils.setField(manager, "backupFileStream", mockBackupFileStream);
		ReflectionTestUtils.setField(manager, "s3Client", mockS3Client);
		ReflectionTestUtils.setField(manager, "fileProvider", mockFileProvider);
		ReflectionTestUtils.setField(manager, "migrationListeners", Lists.newArrayList(mockMigrationListener));
		
		ForeignKeyInfo info = new ForeignKeyInfo();
		info.setTableName("foo");
		info.setReferencedTableName("bar");
		List<ForeignKeyInfo> nonRestrictedForeignKeys = Lists.newArrayList(info);
		when(mockDao.listNonRestrictedForeignKeys()).thenReturn(nonRestrictedForeignKeys);
		
		Map<String, Set<String>> tableNameToPrimaryGroup = new HashMap<>();
		// bar is within foo's primary group.
		tableNameToPrimaryGroup.put("FOO", Sets.newHashSet("BAR"));
		when(mockDao.mapSecondaryTablesToPrimaryGroups()).thenReturn(tableNameToPrimaryGroup);
		
		when(mockFileProvider.createTempFile(anyString(), anyString())).thenReturn(mockFile);
		when(mockFileProvider.createFileOutputStream(any(File.class))).thenReturn(mockOutputStream);
		when(mockFileProvider.createFileInputStream(any(File.class))).thenReturn(mockFileInputStream);
		// default to admin
		when(mockUser.isAdmin()).thenReturn(true);
		
		when(mockDao.getObjectForType(MigrationType.NODE)).thenReturn(new DBONode());
		when(mockDao.getObjectForType(MigrationType.PRINCIPAL)).thenReturn(new DBOUserGroup());
		
		nodeOne = new DBONode();
		nodeOne.setId(123L);;
		revOne = new DBORevision();
		revOne.setOwner(nodeOne.getId());
		revOne.setRevisionNumber(1L);
		
		nodeTwo = new DBONode();
		nodeTwo.setId(456L);
		revTwo = new DBORevision();
		revTwo.setOwner(nodeTwo.getId());
		revTwo.setRevisionNumber(0L);
		
		backupAlias = BackupAliasType.MIGRATION_TYPE_NAME;
		batchSize = 2L;
		backupIds = Lists.newArrayList(123L,456L);
		
		nodeStream = Lists.newArrayList(nodeOne, nodeTwo);
		revisionStream = Lists.newArrayList(revOne, revTwo);
		
		rangeRequest = new BackupTypeRangeRequest();
		rangeRequest.setAliasType(backupAlias);
		rangeRequest.setMigrationType(MigrationType.NODE);
		rangeRequest.setBatchSize(batchSize);
		rangeRequest.setMinimumId(nodeOne.getId());
		rangeRequest.setMaximumId(nodeTwo.getId());
		
		when(mockDao.streamDatabaseObjects(MigrationType.NODE, rangeRequest.getMinimumId(), rangeRequest.getMaximumId(),
				batchSize)).thenReturn(nodeStream);
		when(mockDao.streamDatabaseObjects(MigrationType.NODE_REVISION, rangeRequest.getMinimumId(),
				rangeRequest.getMaximumId(), batchSize)).thenReturn(revisionStream);
		
		allObjects = new LinkedList<>();
		allObjects.addAll(nodeStream);
		allObjects.addAll(revisionStream);
		
		LinkedList<Long> ids =  new LinkedList<>();
		for(AuthorizationConstants.BOOTSTRAP_PRINCIPAL bootPrincpal: AuthorizationConstants.BOOTSTRAP_PRINCIPAL.values()) {
			ids.add(bootPrincpal.getPrincipalId());
		}
		this.bootstrapPrincipalIds = Collections.unmodifiableList(ids);
		
		when(mockDao.isMigrationTypeRegistered(MigrationType.NODE)).thenReturn(true);
		when(mockDao.isMigrationTypeRegistered(MigrationType.NODE_REVISION)).thenReturn(true);
		
		// setup read of all objects.
		when(mockBackupFileStream.readBackupFile(any(InputStream.class), any(BackupAliasType.class))).thenReturn(allObjects);
		
		restoreTypeRequest = new RestoreTypeRequest();
		restoreTypeRequest.setAliasType(backupAlias);
		restoreTypeRequest.setBackupFileKey("backupFileKey");
		restoreTypeRequest.setBatchSize(batchSize);
		restoreTypeRequest.setMigrationType(MigrationType.NODE);
		
		optimalRangeRequest = new CalculateOptimalRangeRequest();
		optimalRangeRequest.setMigrationType(MigrationType.NODE);
		optimalRangeRequest.setMinimumId(1L);
		optimalRangeRequest.setMaximumId(99L);
		optimalRangeRequest.setOptimalRowsPerRange(11L);
		
		IdRange range = new IdRange();
		range.setMinimumId(1L);
		range.setMaximumId(100L);
		ranges = Lists.newArrayList(range);
		
		when(mockDao.calculateRangesForType(any(MigrationType.class), anyLong(),  anyLong(),  anyLong())).thenReturn(ranges);
		
		manager.initialize();
	}
	
	@Test(expected=RuntimeException.class)
	public void testgetMigrationChecksumForTypeReadWriteMode() throws Exception {
		when(mockStatusDao.getCurrentStatus()).thenReturn(StatusEnum.READ_WRITE);
		UserInfo user = new UserInfo(true, "0");
		MigrationTypeChecksum c = manager.getChecksumForType(user, MigrationType.FILE_HANDLE);
	}

	@Test
	public void testgetMigrationChecksumForTypeReadOnlyMode() throws Exception {
		when(mockStatusDao.getCurrentStatus()).thenReturn(StatusEnum.READ_ONLY);
		UserInfo user = new UserInfo(true, "0");
		MigrationTypeChecksum c = manager.getChecksumForType(user, MigrationType.FILE_HANDLE);
	}
	
	/**
	 * Case where secondary table references a table within its primary group.
	 */
	@Test
	public void testValidateForeignKeysRefrenceWithinPrimaryGroup() {		
		// Call under test
		manager.validateForeignKeys();
		verify(mockDao, times(2)).listNonRestrictedForeignKeys();
		verify(mockDao, times(2)).mapSecondaryTablesToPrimaryGroups();
	}
	
	@Test
	public void testInitialize() {
		manager.initialize();
		// should trigger foreign key validation
		verify(mockDao, times(2)).listNonRestrictedForeignKeys();
		verify(mockDao, times(2)).mapSecondaryTablesToPrimaryGroups();
	}
	
	/**
	 * Case where secondary table references a table outside its primary group.
	 */
	@Test
	public void testValidateForeignKeysRefrenceOutsidePrimaryGroup() {
		ForeignKeyInfo info = new ForeignKeyInfo();
		info.setTableName("foo");
		info.setReferencedTableName("bar");
		info.setDeleteRule("CASCADE");
		
		List<ForeignKeyInfo> nonRestrictedForeignKeys = Lists.newArrayList(info);
		when(mockDao.listNonRestrictedForeignKeys()).thenReturn(nonRestrictedForeignKeys);
		Map<String, Set<String>> tableNameToPrimaryGroup = new HashMap<>();
		// bar is not in foo's primary group.
		tableNameToPrimaryGroup.put("FOO", Sets.newHashSet("cats"));
		when(mockDao.mapSecondaryTablesToPrimaryGroups()).thenReturn(tableNameToPrimaryGroup);
		try {
			// Call under test
			manager.validateForeignKeys();
			fail();
		} catch (IllegalStateException e) {
			System.out.println(e.getMessage());
			// expected
			assertTrue(e.getMessage().contains(info.getTableName().toUpperCase()));
			assertTrue(e.getMessage().contains(info.getReferencedTableName().toUpperCase()));
			assertTrue(e.getMessage().contains(info.getDeleteRule()));
		}
	}
	
	/**
	 * Case where non-secondary table has a restricted foreign key.
	 */
	@Test
	public void testValidateForeignKeysRefrenceNonSecondaryTable() {
		ForeignKeyInfo info = new ForeignKeyInfo();
		info.setTableName("foo");
		info.setReferencedTableName("bar");
		info.setDeleteRule("CASCADE");
		
		List<ForeignKeyInfo> nonRestrictedForeignKeys = Lists.newArrayList(info);
		when(mockDao.listNonRestrictedForeignKeys()).thenReturn(nonRestrictedForeignKeys);
		Map<String, Set<String>> tableNameToPrimaryGroup = new HashMap<>();
		// foo is not a secondary table so it has no entry in this map.
		tableNameToPrimaryGroup.put("bar", Sets.newHashSet("foobar"));
		when(mockDao.mapSecondaryTablesToPrimaryGroups()).thenReturn(tableNameToPrimaryGroup);
		// Call under test
		manager.validateForeignKeys();
	}
	
	@Test
	public void testCreateNewBackupKey() {
		String stack = "dev";
		String instance = "test1";
		MigrationType type = MigrationType.NODE_REVISION;
		String key = MigrationManagerImpl.createNewBackupKey(stack, instance, type);
		assertNotNull(key);
		assertTrue(key.startsWith("dev-test1-NODE_REVISION"));
		assertTrue(key.contains(".zip"));
	}
	
	@Test
	public void testBackupStreamToS3() throws IOException {
		List<MigratableDatabaseObject<?, ?>> stream = new LinkedList<>();
		MigrationType type = MigrationType.NODE;
		BackupAliasType aliasType = BackupAliasType.TABLE_NAME;
		long batchSize = 2;
		// call under test
		BackupTypeResponse response = manager.backupStreamToS3(type, stream, aliasType, batchSize);
		assertNotNull(response);
		assertNotNull(response.getBackupFileKey());
		verify(mockBackupFileStream).writeBackupFile(mockOutputStream, stream, aliasType, batchSize);
		verify(mockS3Client).putObject(MigrationManagerImpl.backupBucket, response.getBackupFileKey(), mockFile);
		// the stream must be flushed and closed.
		verify(mockOutputStream).flush();
		verify(mockOutputStream, times(2)).close();
		// the temp file must be deleted
		verify(mockFile).delete();
	}
	
	@Test
	public void testBackupStreamToS3Exception() throws IOException {
		// setup an failure
		IOException toBeThrown = new IOException("some kind of IO error");
		doThrow(toBeThrown).when(mockBackupFileStream).writeBackupFile(any(OutputStream.class), any(Iterable.class), any(BackupAliasType.class), anyLong());
		// call under test
		List<MigratableDatabaseObject<?, ?>> stream = new LinkedList<>();
		MigrationType type = MigrationType.NODE;
		BackupAliasType aliasType = BackupAliasType.TABLE_NAME;
		long batchSize = 2;
		// call under test
		try {
			manager.backupStreamToS3(type, stream, aliasType, batchSize);
			fail();
		} catch (Exception e) {
			// expected
			assertEquals(toBeThrown.getMessage(), e.getMessage());
		}
		// the stream must be closed
		verify(mockOutputStream).close();
		// the temp file must be deleted
		verify(mockFile).delete();
	}
	
	@Test
	public void testBackupRangeRequest() throws IOException {
 		// call under test
		manager.backupRequest(mockUser, rangeRequest);
		verify(mockBackupFileStream).writeBackupFile(eq(mockOutputStream), iterableCator.capture(), eq(backupAlias), eq(batchSize));
		List<MigratableDatabaseObject<?, ?>> results = new LinkedList<>();
		for(MigratableDatabaseObject<?, ?> object: iterableCator.getValue()) {
			results.add(object);
		}
		assertEquals(allObjects,results);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testBackupRangeRequestNonAdmin() throws IOException {
		when(mockUser.isAdmin()).thenReturn(false);
 		// call under test
		manager.backupRequest(mockUser, rangeRequest);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testBackupRangeRequestNullUser() throws IOException {
		mockUser = null;
 		// call under test
		manager.backupRequest(mockUser, rangeRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testBackupRageRequestNullRequset() throws IOException {
		rangeRequest  = null;
 		// call under test
		manager.backupRequest(mockUser, rangeRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testBackupRangetRequestNullAliasType() throws IOException {
		rangeRequest.setAliasType(null);
 		// call under test
		manager.backupRequest(mockUser, rangeRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testBackupRangeRequestNullMigrationType() throws IOException {
		rangeRequest.setMigrationType(null);
 		// call under test
		manager.backupRequest(mockUser, rangeRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testBackupRangeRequestNullBatchSize() throws IOException {
		rangeRequest.setBatchSize(null);
 		// call under test
		manager.backupRequest(mockUser, rangeRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testBackupRangeRequestNullMinId() throws IOException {
		rangeRequest.setMinimumId(null);
 		// call under test
		manager.backupRequest(mockUser, rangeRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testBackupRangeRequestNullMaxId() throws IOException {
		rangeRequest.setMaximumId(null);
 		// call under test
		manager.backupRequest(mockUser, rangeRequest);
	}
	
	@Test
	public void testFilterBootstrapPrincipalsPrincipalType() {
		MigrationType type = MigrationType.PRINCIPAL;
		List<Long> toTest = new LinkedList<>(bootstrapPrincipalIds);
		// this should not be filtered.
		toTest.add(1234L);
		// call under test
		List<Long> filtered = MigrationManagerImpl.filterBootstrapPrincipals(type, toTest);
		assertNotNull(filtered);
		// all prinpals should be removed.
		assertEquals(Lists.newArrayList(1234L), filtered);
	}
	
	@Test
	public void testFilterBootstrapPrincipalsNonPrincipalType() {
		MigrationType type = MigrationType.NODE;
		List<Long> toTest = new LinkedList<>(bootstrapPrincipalIds);
		// non-principal ID.
		toTest.add(1234L);
		int startSize = toTest.size();
		// call under test
		List<Long> filtered = MigrationManagerImpl.filterBootstrapPrincipals(type, toTest);
		assertNotNull(filtered);
		assertEquals(startSize, filtered.size());
		assertEquals(toTest, filtered);
	}
	
	@Test
	public void testPrincipalTypes() {
		DBOUserGroup dbo = new DBOUserGroup();
		List<MigratableDatabaseObject<?, ?>> secondaries = dbo.getSecondaryTypes();
		// includes principal plus principal's secondaries
		assertEquals(secondaries.size()+1, MigrationManagerImpl.PRINCIPAL_TYPES.size());
		assertTrue(MigrationManagerImpl.PRINCIPAL_TYPES.contains(MigrationType.PRINCIPAL));
		for(MigratableDatabaseObject<?, ?> secondary: secondaries) {
			assertTrue(MigrationManagerImpl.PRINCIPAL_TYPES.contains(secondary.getMigratableTableType()));
		}
	}
	
	@Test
	public void testRestoreBatchPrimary() {
		MigrationType currentType = MigrationType.NODE;
		MigrationType primaryType = MigrationType.NODE;
		List<MigrationType> secondaryTypes = Lists.newArrayList(MigrationType.NODE_REVISION);
		List<DatabaseObject<?>> currentBatch = Lists.newArrayList(nodeOne);
		List<Long> idList = Lists.newArrayList(nodeOne.getId());
		when(mockDao.createOrUpdate(currentType, currentBatch)).thenReturn(idList);
		// call under test
		manager.restoreBatch(currentType, primaryType, secondaryTypes, currentBatch);
		verify(mockMigrationListener).afterCreateOrUpdate(currentType, currentBatch);
	}
	
	@Test
	public void testRestoreBatchSecondary() {
		// current is a secondary.
		MigrationType currentType = MigrationType.NODE_REVISION;
		// node is the primary
		MigrationType primaryType = MigrationType.NODE;
		// secondary passed in
		List<MigrationType> secondaryTypes = Lists.newArrayList(MigrationType.NODE_REVISION);
		// batch of revisions.
		List<DatabaseObject<?>> currentBatch = Lists.newArrayList(revOne);
		List<Long> idList = Lists.newArrayList(revOne.getOwner());
		when(mockDao.createOrUpdate(currentType, currentBatch)).thenReturn(idList);
		// call under test
		manager.restoreBatch(currentType, primaryType, secondaryTypes, currentBatch);
		verify(mockMigrationListener).afterCreateOrUpdate(MigrationType.NODE_REVISION, currentBatch);
	}
	
	@Test
	public void testRestoreBatchEmpty() {
		// current is a secondary.
		MigrationType currentType = MigrationType.NODE_REVISION;
		// node is the primary
		MigrationType primaryType = MigrationType.NODE;
		// secondary passed in
		List<MigrationType> secondaryTypes = Lists.newArrayList(MigrationType.NODE_REVISION);
		// Empty batch
		List<DatabaseObject<?>> currentBatch = new LinkedList<>();
		// call under test
		manager.restoreBatch(currentType, primaryType, secondaryTypes, currentBatch);
		verify(mockMigrationListener, never()).afterCreateOrUpdate(any(MigrationType.class), anyList());
	}
	
	/**
	 * Batch by type when the batch size is larger than the type batches.
	 */
	@Test
	public void testRestoreStreamBatchByType() {
		MigrationType primaryType = MigrationType.NODE;
		batchSize = 1000L;
		// call under test
		RestoreTypeResponse response = manager.restoreStream(mockInputStream, primaryType, backupAlias, batchSize);
		assertNotNull(response);
		// count should match 
		assertEquals(new Long(allObjects.size()), response.getRestoredRowCount());
		verify(mockDao, times(2)).createOrUpdate(any(MigrationType.class), anyList());
		// first batch is nodes
		verify(mockDao).createOrUpdate(MigrationType.NODE, Lists.newArrayList(nodeOne, nodeTwo));
		// second batch is revisions.
		verify(mockDao).createOrUpdate(MigrationType.NODE_REVISION, Lists.newArrayList(Lists.newArrayList(revOne, revTwo)));
	}
	
	/**
	 * Batch by size when the batch size is smaller than the type batches.
	 */
	@Test
	public void testRestoreStreamBatchBySize() {
		MigrationType primaryType = MigrationType.NODE;
		batchSize = 1L;
		// call under test
		RestoreTypeResponse response = manager.restoreStream(mockInputStream, primaryType, backupAlias, batchSize);
		assertNotNull(response);
		// count should match 
		assertEquals(new Long(allObjects.size()), response.getRestoredRowCount());
		verify(mockDao, times(4)).createOrUpdate(any(MigrationType.class), anyList());
		// each row should be its own batch
		verify(mockDao).createOrUpdate(MigrationType.NODE, Lists.newArrayList(nodeOne));
		verify(mockDao).createOrUpdate(MigrationType.NODE, Lists.newArrayList(nodeTwo));
		verify(mockDao).createOrUpdate(MigrationType.NODE_REVISION, Lists.newArrayList(Lists.newArrayList(revOne)));
		verify(mockDao).createOrUpdate(MigrationType.NODE_REVISION, Lists.newArrayList(Lists.newArrayList(revTwo)));
	}
	
	@Test
	public void testRestoreStreamPrimaryNotRegistered() {
		MigrationType primaryType = MigrationType.NODE;
		// primary not registered
		when(mockDao.isMigrationTypeRegistered(primaryType)).thenReturn(false);
		// call under test
		RestoreTypeResponse response = manager.restoreStream(mockInputStream, primaryType, backupAlias, batchSize);
		assertNotNull(response);
		// count should match 
		assertEquals(new Long(0), response.getRestoredRowCount());
		verify(mockDao, never()).createOrUpdate(any(MigrationType.class), anyList());
	}
	
	@Test
	public void testRestoreStreamSecondaryNotRegistered() {
		MigrationType primaryType = MigrationType.NODE;
		// secondary not registered.
		when(mockDao.isMigrationTypeRegistered(MigrationType.NODE_REVISION)).thenReturn(false);
		// call under test
		RestoreTypeResponse response = manager.restoreStream(mockInputStream, primaryType, backupAlias, batchSize);
		assertNotNull(response);
		// count should match 
		assertEquals(new Long(nodeStream.size()), response.getRestoredRowCount());
		verify(mockDao, times(1)).createOrUpdate(any(MigrationType.class), anyList());
		// primary should be added but not secondary.
		verify(mockDao).createOrUpdate(MigrationType.NODE, Lists.newArrayList(nodeOne, nodeTwo));
	}
	
	@Test
	public void testRestoreRequestNoRange() throws IOException {
		restoreTypeRequest.setMaximumRowId(null);
		restoreTypeRequest.setMinimumRowId(null);
		// call under test
		RestoreTypeResponse response = manager.restoreRequest(mockUser, restoreTypeRequest);
		assertNotNull(response);
		// the file should be fetched from S3
		verify(mockS3Client).getObject(getObjectRequestCaptor.capture(), eq(mockFile));
		GetObjectRequest gor = getObjectRequestCaptor.getValue();
		assertNotNull(gor);
		assertEquals(MigrationManagerImpl.backupBucket, gor.getBucketName());
		assertEquals(restoreTypeRequest.getBackupFileKey(), gor.getKey());
		// the file should be deleted
		verify(mockS3Client).deleteObject(MigrationManagerImpl.backupBucket, restoreTypeRequest.getBackupFileKey());
		verify(mockFileInputStream).close();
		verify(mockFile).delete();
		
		// delete by range should not occur when the range is missing.
		verify(mockDao, never()).deleteByRange(any(MigrationType.class), anyLong(), anyLong());
	}
	
	
	@Test
	public void testRestoreRequestWithRange() throws IOException {
		long max = 99L;
		long min = 3L;
		restoreTypeRequest.setMaximumRowId(max);
		restoreTypeRequest.setMinimumRowId(min);
		// call under test
		RestoreTypeResponse response = manager.restoreRequest(mockUser, restoreTypeRequest);
		assertNotNull(response);
		// the file should be fetched from S3
		verify(mockS3Client).getObject(getObjectRequestCaptor.capture(), eq(mockFile));
		GetObjectRequest gor = getObjectRequestCaptor.getValue();
		assertNotNull(gor);
		assertEquals(MigrationManagerImpl.backupBucket, gor.getBucketName());
		assertEquals(restoreTypeRequest.getBackupFileKey(), gor.getKey());
		// the file should be deleted
		verify(mockS3Client).deleteObject(MigrationManagerImpl.backupBucket, restoreTypeRequest.getBackupFileKey());
		verify(mockFileInputStream).close();
		verify(mockFile).delete();
		
		// should delete the primary
		verify(mockDao).deleteByRange(MigrationType.NODE, min, max);
		// should delete the secondary
		verify(mockDao).deleteByRange(MigrationType.NODE_REVISION, min, max);
	}
	
	
	@Test
	public void testRestoreRequestCleanup() throws IOException {
		// setup failure
		AmazonServiceException exception = new AmazonServiceException("failed");
		doThrow(exception).when(mockS3Client).deleteObject(anyString(), anyString());
		try {
			// call under test
			manager.restoreRequest(mockUser, restoreTypeRequest);
			fail();
		} catch (Exception e) {
			// expected
			assertEquals(exception.getMessage(), e.getMessage());
		}
		// stream should be closed
		verify(mockFileInputStream).close();
		// temp should be deleted.
		verify(mockFile).delete();
	}
	
	
	@Test (expected=UnauthorizedException.class)
	public void testRestoreRequestUnauthorized() throws IOException {
		// must be an admin
		when(mockUser.isAdmin()).thenReturn(false);
		// call under test
		manager.restoreRequest(mockUser, restoreTypeRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testRestoreRequestNullUser() throws IOException {
		mockUser = null;
		// call under test
		manager.restoreRequest(mockUser, restoreTypeRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testRestoreRequestNullRequest() throws IOException {
		restoreTypeRequest = null;
		// call under test
		manager.restoreRequest(mockUser, restoreTypeRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testRestoreRequestNullBackupAlias() throws IOException {
		restoreTypeRequest.setAliasType(null);
		// call under test
		manager.restoreRequest(mockUser, restoreTypeRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testRestoreRequestNullKey() throws IOException {
		restoreTypeRequest.setBackupFileKey(null);
		// call under test
		manager.restoreRequest(mockUser, restoreTypeRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testRestoreRequestNullMigrationType() throws IOException {
		restoreTypeRequest.setMigrationType(null);
		// call under test
		manager.restoreRequest(mockUser, restoreTypeRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testRestoreRequestNullBatchSize() throws IOException {
		restoreTypeRequest.setBatchSize(null);
		// call under test
		manager.restoreRequest(mockUser, restoreTypeRequest);
	}
	
	@Test
	public void testDeleteByRange() {
		MigrationType type = MigrationType.NODE;
		long minimumId = 3L;
		long maximumId = 45L;
		// call under test
		manager.deleteByRange(type, minimumId, maximumId);
		// should delete the primary
		verify(mockDao).deleteByRange(type, minimumId, maximumId);
		// should delete the secondary
		verify(mockDao).deleteByRange(MigrationType.NODE_REVISION, minimumId, maximumId);
	}
	
	@Test
	public void testDeleteByRangeNotRegistered() {
		MigrationType type = MigrationType.NODE;
		when(mockDao.isMigrationTypeRegistered(type)).thenReturn(false);
		long minimumId = 3L;
		long maximumId = 45L;
		// call under test
		manager.deleteByRange(type, minimumId, maximumId);
		// deletes should not occur
		verify(mockDao, never()).deleteByRange(any(MigrationType.class), anyLong(), anyLong());
	}
	
	@Test
	public void testCalculateOptimalRanges() {
		// call under test
		CalculateOptimalRangeResponse response = manager.calculateOptimalRanges(mockUser, optimalRangeRequest);
		assertNotNull(response);
		assertEquals(optimalRangeRequest.getMigrationType(), response.getMigrationType());
		assertEquals(ranges, response.getRanges());
		verify(mockDao).calculateRangesForType(
				optimalRangeRequest.getMigrationType(),
				optimalRangeRequest.getMinimumId(),
				optimalRangeRequest.getMaximumId(),
				optimalRangeRequest.getOptimalRowsPerRange());
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testCalculateOptimalRangesUnauthorized() {
		when(mockUser.isAdmin()).thenReturn(false);
		// call under test
		manager.calculateOptimalRanges(mockUser, optimalRangeRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCalculateOptimalRangesNullRequest() {
		optimalRangeRequest = null;
		// call under test
		manager.calculateOptimalRanges(mockUser, optimalRangeRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCalculateOptimalRangesNullType() {
		optimalRangeRequest.setMigrationType(null);
		// call under test
		manager.calculateOptimalRanges(mockUser, optimalRangeRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCalculateOptimalRangesNullMin() {
		optimalRangeRequest.setMinimumId(null);
		// call under test
		manager.calculateOptimalRanges(mockUser, optimalRangeRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCalculateOptimalRangesNullMax() {
		optimalRangeRequest.setMaximumId(null);
		// call under test
		manager.calculateOptimalRanges(mockUser, optimalRangeRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCalculateOptimalRangesNullOptimalSize() {
		optimalRangeRequest.setOptimalRowsPerRange(null);
		// call under test
		manager.calculateOptimalRanges(mockUser, optimalRangeRequest);
	}
}
