package org.sagebionetworks.repo.manager.migration;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.daemon.BackupAliasType;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.dao.TestUtils;
import org.sagebionetworks.repo.model.dbo.migration.ForeignKeyInfo;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableDAO;
import org.sagebionetworks.repo.model.dbo.persistence.DBONode;
import org.sagebionetworks.repo.model.dbo.persistence.DBORevision;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserGroup;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountsRequest;
import org.sagebionetworks.repo.model.migration.BackupManifest;
import org.sagebionetworks.repo.model.migration.BackupTypeRangeRequest;
import org.sagebionetworks.repo.model.migration.BackupTypeResponse;
import org.sagebionetworks.repo.model.migration.BatchChecksumRequest;
import org.sagebionetworks.repo.model.migration.BatchChecksumResponse;
import org.sagebionetworks.repo.model.migration.CalculateOptimalRangeRequest;
import org.sagebionetworks.repo.model.migration.CalculateOptimalRangeResponse;
import org.sagebionetworks.repo.model.migration.IdRange;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.MigrationTypeCounts;
import org.sagebionetworks.repo.model.migration.RangeChecksum;
import org.sagebionetworks.repo.model.migration.RestoreTypeRequest;
import org.sagebionetworks.repo.model.migration.RestoreTypeResponse;
import org.sagebionetworks.repo.model.migration.TypeData;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.FileProvider;
import org.springframework.jdbc.core.JdbcTemplate;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.StringInputStream;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * The Unit test for MigrationManagerImpl;
 *
 */
@ExtendWith(MockitoExtension.class)
public class MigrationManagerImplTest {
	
	@Mock
	MigratableTableDAO mockDao;
	@Mock
	StackStatusDao mockStatusDao;
	@Mock
	BackupFileStream mockBackupFileStream;
	@Mock
	SynapseS3Client mockS3Client;
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
	@Mock
	JdbcTemplate mockJdbcTemplate;
	@Captor
	ArgumentCaptor<Iterable<MigratableDatabaseObject<?, ?>>> iterableCator;
	@Mock
	MigrationTypeListener<DatabaseObject<?>> mockMigrationListener;
	@Captor
	ArgumentCaptor<GetObjectRequest> getObjectRequestCaptor;
	@InjectMocks
	MigrationManagerImpl manager;
	
	DBONode nodeOne;
	DBORevision revOne;
	DBONode nodeTwo;
	DBORevision revTwo;
	
	List<ForeignKeyInfo> nonRestrictedForeignKeys;
	Map<String, Set<String>> tableNameToPrimaryGroup;
	RangeChecksum sum;
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
	String backupKey;
	BackupManifest manifest;
	TypeData nodeTypeData;
	TypeData revisionTypeData;
	
	@BeforeEach
	public void before() throws IOException{
		ForeignKeyInfo info = new ForeignKeyInfo();
		info.setTableName("foo");
		info.setReferencedTableName("bar");
		nonRestrictedForeignKeys = Lists.newArrayList(info);

		tableNameToPrimaryGroup = new HashMap<>();
		// bar is within foo's primary group.
		tableNameToPrimaryGroup.put("FOO", Sets.newHashSet("BAR"));
		
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
		
		allObjects = new LinkedList<>();
		allObjects.addAll(nodeStream);
		allObjects.addAll(revisionStream);
		
		LinkedList<Long> ids =  new LinkedList<>();
		for(AuthorizationConstants.BOOTSTRAP_PRINCIPAL bootPrincpal: AuthorizationConstants.BOOTSTRAP_PRINCIPAL.values()) {
			ids.add(bootPrincpal.getPrincipalId());
		}
		this.bootstrapPrincipalIds = Collections.unmodifiableList(ids);
				
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
		
		sum = new RangeChecksum();
		sum.setBinNumber(1L);
		
		List<MigrationTypeListener<DatabaseObject<?>>> listeners = Lists.newArrayList(mockMigrationListener);
		manager.setMigrationListeners(listeners);
		
		when(mockDao.getObjectForType(MigrationType.PRINCIPAL)).thenReturn(new DBOUserGroup());
		manager.initialize();
		
		backupKey = "backupkey";
		
		nodeTypeData = new TypeData().setMigrationType(MigrationType.NODE.name()).setBackupIdColumnName("ID");
		revisionTypeData = new TypeData().setMigrationType(MigrationType.NODE_REVISION.name()).setBackupIdColumnName("OWNER_NODE_ID");
		
		manifest = new BackupManifest().setBatchSize(batchSize).setPrimaryType(nodeTypeData).setAliasType(backupAlias)
				.setMaximumId(100L).setMinimumId(10L).setSecondaryTypes(Arrays.asList(revisionTypeData));
	}
	
	@Test
	public void testgetMigrationChecksumForTypeReadWriteMode() throws Exception {
		when(mockStatusDao.getCurrentStatus()).thenReturn(StatusEnum.READ_WRITE);
		UserInfo user = new UserInfo(true, "0");
		assertThrows(RuntimeException.class, ()->{
			// call under test
			manager.getChecksumForType(user, MigrationType.FILE_HANDLE);
		});
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
		String message = assertThrows(IllegalStateException.class, ()->{
			// Call under test
			manager.validateForeignKeys();
		}).getMessage();
		// expected
		assertTrue(message.contains(info.getTableName().toUpperCase()));
		assertTrue(message.contains(info.getReferencedTableName().toUpperCase()));
		assertTrue(message.contains(info.getDeleteRule()));
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
		when(mockFileProvider.createTempFile(any(), any())).thenReturn(mockFile);
		when(mockFileProvider.createFileOutputStream(any())).thenReturn(mockOutputStream);
		String backupKey = "backupkey";
		List<MigratableDatabaseObject<?, ?>> stream = new LinkedList<>();
		MigrationType type = MigrationType.NODE;
		BackupAliasType aliasType = BackupAliasType.TABLE_NAME;
		long batchSize = 2;
		// call under test
		BackupTypeResponse response = manager.backupStreamToS3(type, stream, aliasType, batchSize, backupKey);
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
		when(mockFileProvider.createTempFile(anyString(), anyString())).thenReturn(mockFile);
		when(mockFileProvider.createFileOutputStream(any(File.class))).thenReturn(mockOutputStream);
		
		// setup an failure
		IOException toBeThrown = new IOException("some kind of IO error");
		doThrow(toBeThrown).when(mockBackupFileStream).writeBackupFile(any(), any(), any(), anyLong());
		// call under test
		List<MigratableDatabaseObject<?, ?>> stream = new LinkedList<>();
		MigrationType type = MigrationType.NODE;
		BackupAliasType aliasType = BackupAliasType.TABLE_NAME;
		long batchSize = 2;
		IOException e = assertThrows(IOException.class, ()->{
			// call under test
			manager.backupStreamToS3(type, stream, aliasType, batchSize, backupKey);
		});
		assertEquals(toBeThrown.getMessage(), e.getMessage());
		// the stream must be closed
		verify(mockOutputStream).close();
		// the temp file must be deleted
		verify(mockFile).delete();
	}
	
	@Test
	public void testBackupRangeRequest() throws IOException {
		when(mockFileProvider.createTempFile(any(), any())).thenReturn(mockFile);
		when(mockFileProvider.createFileOutputStream(any())).thenReturn(mockOutputStream);
		when(mockUser.isAdmin()).thenReturn(true);
		when(mockDao.getObjectForType(any())).thenReturn(new DBONode());
		when(mockDao.streamDatabaseObjects(MigrationType.NODE, rangeRequest.getMinimumId(), rangeRequest.getMaximumId(),
				batchSize)).thenReturn(nodeStream);
		when(mockDao.streamDatabaseObjects(MigrationType.NODE_REVISION, rangeRequest.getMinimumId(),
				rangeRequest.getMaximumId(), batchSize)).thenReturn(revisionStream);
		
 		// call under test
		manager.backupRequest(mockUser, rangeRequest);
		verify(mockBackupFileStream).writeBackupFile(eq(mockOutputStream), iterableCator.capture(), eq(backupAlias), eq(batchSize));
		List<MigratableDatabaseObject<?, ?>> results = new LinkedList<>();
		for(MigratableDatabaseObject<?, ?> object: iterableCator.getValue()) {
			results.add(object);
		}
		assertEquals(allObjects,results);
	}
	
	@Test
	public void testBackupRangeRequestNonAdmin() throws IOException {
		when(mockUser.isAdmin()).thenReturn(false);
		assertThrows(UnauthorizedException.class, ()->{
	 		// call under test
			manager.backupRequest(mockUser, rangeRequest);
		});
	}

	@Test
	public void testBackupRangeRequestNullUser() throws IOException {
		mockUser = null;
		assertThrows(IllegalArgumentException.class, ()->{
	 		// call under test
			manager.backupRequest(mockUser, rangeRequest);
		});
	}
	
	@Test
	public void testBackupRageRequestNullRequset() throws IOException {
		rangeRequest  = null;
		assertThrows(IllegalArgumentException.class, ()->{
	 		// call under test
			manager.backupRequest(mockUser, rangeRequest);
		});
	}
	
	@Test
	public void testBackupRangetRequestNullAliasType() throws IOException {
		rangeRequest.setAliasType(null);
		assertThrows(IllegalArgumentException.class, ()->{
	 		// call under test
			manager.backupRequest(mockUser, rangeRequest);
		});
	}
	
	@Test
	public void testBackupRangeRequestNullMigrationType() throws IOException {
		rangeRequest.setMigrationType(null);
		assertThrows(IllegalArgumentException.class, ()->{
	 		// call under test
			manager.backupRequest(mockUser, rangeRequest);
		});
	}
	
	@Test
	public void testBackupRangeRequestNullBatchSize() throws IOException {
		rangeRequest.setBatchSize(null);
		assertThrows(IllegalArgumentException.class, ()->{
	 		// call under test
			manager.backupRequest(mockUser, rangeRequest);
		});
	}
	
	@Test
	public void testBackupRangeRequestNullMinId() throws IOException {
		rangeRequest.setMinimumId(null);
		assertThrows(IllegalArgumentException.class, ()->{
	 		// call under test
			manager.backupRequest(mockUser, rangeRequest);
		});
	}
	
	@Test
	public void testBackupRangeRequestNullMaxId() throws IOException {
		rangeRequest.setMaximumId(null);
		assertThrows(IllegalArgumentException.class, ()->{
	 		// call under test
			manager.backupRequest(mockUser, rangeRequest);
		});
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
		List<DatabaseObject<?>> currentBatch = Lists.newArrayList(nodeOne);
		List<Long> idList = Lists.newArrayList(nodeOne.getId());
		when(mockDao.createOrUpdate(currentType, currentBatch)).thenReturn(idList);
		when(mockMigrationListener.supports(any())).thenReturn(true);
		// call under test
		manager.restoreBatch(currentType, currentBatch);
		verify(mockMigrationListener).beforeCreateOrUpdate(mockJdbcTemplate, currentBatch);
		verify(mockMigrationListener).afterCreateOrUpdate(mockJdbcTemplate, currentBatch);
	}
	
	@Test
	public void testRestoreBatchSecondary() throws IOException {
		// current is a secondary.
		MigrationType currentType = MigrationType.NODE_REVISION;
		// batch of revisions.
		List<DatabaseObject<?>> currentBatch = Lists.newArrayList(revOne);
		List<Long> idList = Lists.newArrayList(revOne.getOwner());
		when(mockDao.createOrUpdate(currentType, currentBatch)).thenReturn(idList);
		when(mockMigrationListener.supports(any())).thenReturn(true);
		// call under test
		manager.restoreBatch(currentType, currentBatch);
		verify(mockMigrationListener).beforeCreateOrUpdate(mockJdbcTemplate, currentBatch);
		verify(mockMigrationListener).afterCreateOrUpdate(mockJdbcTemplate, currentBatch);
	}
	
	@Test
	public void testRestoreBatchWithUnsupportedListener() {
		MigrationType currentType = MigrationType.NODE;
		List<DatabaseObject<?>> currentBatch = Lists.newArrayList(nodeOne);
		List<Long> idList = Lists.newArrayList(nodeOne.getId());
		when(mockDao.createOrUpdate(currentType, currentBatch)).thenReturn(idList);
		when(mockMigrationListener.supports(any())).thenReturn(false);
		// call under test
		manager.restoreBatch(currentType, currentBatch);
		verifyNoMoreInteractions(mockMigrationListener);
	}
	
	@Test
	public void testRestoreBatchEmpty() {
		// current is a secondary.
		MigrationType currentType = MigrationType.NODE_REVISION;
		// Empty batch
		List<DatabaseObject<?>> currentBatch = new LinkedList<>();
		// call under test
		manager.restoreBatch(currentType, currentBatch);
		verifyZeroInteractions(mockMigrationListener);
	}
	
	/**
	 * Batch by type when the batch size is larger than the type batches.
	 */
	@Test
	public void testRestoreStreamBatchByType() {
		when(mockBackupFileStream.readBackupFile(any(), any())).thenReturn(allObjects);
		when(mockDao.isMigrationTypeRegistered(MigrationType.NODE.name())).thenReturn(true);
		when(mockDao.isMigrationTypeRegistered(MigrationType.NODE_REVISION.name())).thenReturn(true);
		
		batchSize = 1000L;
		manifest.setBatchSize(batchSize);
		
		// call under test
		RestoreTypeResponse response = manager.restoreStream(mockInputStream, manifest);
		assertNotNull(response);
		
		verify(mockDao).deleteByRange(nodeTypeData, manifest.getMinimumId(),
				manifest.getMaximumId());
		verify(mockDao).deleteByRange(revisionTypeData, manifest.getMinimumId(),
				manifest.getMaximumId());
		
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
	 * @throws IOException 
	 */
	@Test
	public void testRestoreStreamBatchBySize() throws IOException {
		when(mockBackupFileStream.readBackupFile(any(), any())).thenReturn(allObjects);
		when(mockDao.isMigrationTypeRegistered(MigrationType.NODE.name())).thenReturn(true);
		when(mockDao.isMigrationTypeRegistered(MigrationType.NODE_REVISION.name())).thenReturn(true);
		
		manifest.setBatchSize(1L);
		// call under test
		RestoreTypeResponse response = manager.restoreStream(mockInputStream, manifest);
		assertNotNull(response);
		// count should match 
		assertEquals(new Long(allObjects.size()), response.getRestoredRowCount());
		
		verify(mockDao).deleteByRange(revisionTypeData, manifest.getMinimumId(),
				manifest.getMaximumId());
		verify(mockDao).deleteByRange(nodeTypeData, manifest.getMinimumId(),
				manifest.getMaximumId());
		
		verify(mockDao, times(4)).createOrUpdate(any(MigrationType.class), anyList());
		// each row should be its own batch
		verify(mockDao).createOrUpdate(MigrationType.NODE, Lists.newArrayList(nodeOne));
		verify(mockDao).createOrUpdate(MigrationType.NODE, Lists.newArrayList(nodeTwo));
		verify(mockDao).createOrUpdate(MigrationType.NODE_REVISION, Lists.newArrayList(Lists.newArrayList(revOne)));
		verify(mockDao).createOrUpdate(MigrationType.NODE_REVISION, Lists.newArrayList(Lists.newArrayList(revTwo)));
	}
	
	@Test
	public void testRestoreStreamWithNullMaxid() {
		when(mockBackupFileStream.readBackupFile(any(), any())).thenReturn(allObjects);
		when(mockDao.isMigrationTypeRegistered(MigrationType.NODE.name())).thenReturn(true);
		when(mockDao.isMigrationTypeRegistered(MigrationType.NODE_REVISION.name())).thenReturn(true);
		
		manifest.setMaximumId(null);
		
		// call under test
		RestoreTypeResponse response = manager.restoreStream(mockInputStream, manifest);
		assertNotNull(response);
		// count should match 
		assertEquals(new Long(allObjects.size()), response.getRestoredRowCount());
		
		verify(mockDao, never()).deleteByRange(any(), anyLong(), anyLong());
		
		verify(mockDao, times(2)).createOrUpdate(any(MigrationType.class), anyList());
		// first batch is nodes
		verify(mockDao).createOrUpdate(MigrationType.NODE, Lists.newArrayList(nodeOne, nodeTwo));
		// second batch is revisions.
		verify(mockDao).createOrUpdate(MigrationType.NODE_REVISION, Lists.newArrayList(Lists.newArrayList(revOne, revTwo)));
	}
	
	@Test
	public void testRestoreStreamWithNullMinId() {
		when(mockBackupFileStream.readBackupFile(any(), any())).thenReturn(allObjects);
		when(mockDao.isMigrationTypeRegistered(MigrationType.NODE.name())).thenReturn(true);
		when(mockDao.isMigrationTypeRegistered(MigrationType.NODE_REVISION.name())).thenReturn(true);
		
		manifest.setMinimumId(null);
		
		// call under test
		RestoreTypeResponse response = manager.restoreStream(mockInputStream, manifest);
		assertNotNull(response);
		// count should match 
		assertEquals(new Long(allObjects.size()), response.getRestoredRowCount());
		
		verify(mockDao, never()).deleteByRange(any(), anyLong(), anyLong());
		
		verify(mockDao, times(2)).createOrUpdate(any(MigrationType.class), anyList());
		// first batch is nodes
		verify(mockDao).createOrUpdate(MigrationType.NODE, Lists.newArrayList(nodeOne, nodeTwo));
		// second batch is revisions.
		verify(mockDao).createOrUpdate(MigrationType.NODE_REVISION, Lists.newArrayList(Lists.newArrayList(revOne, revTwo)));
	}
	
	@Test
	public void testRestoreStreamPrimaryNotRegistered() {
		MigrationType primaryType = MigrationType.NODE;
		// primary not registered
		when(mockDao.isMigrationTypeRegistered(primaryType.name())).thenReturn(false);
		// call under test
		RestoreTypeResponse response = manager.restoreStream(mockInputStream, manifest);
		assertNotNull(response);
		// count should match 
		assertEquals(new Long(0), response.getRestoredRowCount());
		verify(mockDao, never()).deleteByRange(any(), anyLong(), anyLong());
		verify(mockDao, never()).createOrUpdate(any(MigrationType.class), anyList());
	}
	
	@Test
	public void testRestoreStreamSecondaryNotRegistered() throws IOException {
		when(mockBackupFileStream.readBackupFile(any(), any())).thenReturn(allObjects);
		when(mockDao.isMigrationTypeRegistered(MigrationType.NODE.name())).thenReturn(true);
		// secondary not registered.
		when(mockDao.isMigrationTypeRegistered(MigrationType.NODE_REVISION.name())).thenReturn(false);

		// call under test
		RestoreTypeResponse response = manager.restoreStream(mockInputStream, manifest);
		assertNotNull(response);
		// count should match 
		assertEquals(new Long(nodeStream.size()), response.getRestoredRowCount());
		
		verify(mockDao).deleteByRange(nodeTypeData, manifest.getMinimumId(), manifest.getMaximumId());
		verify(mockDao, never()).deleteByRange(eq(revisionTypeData), anyLong(), anyLong());

		verify(mockDao, times(1)).createOrUpdate(any(MigrationType.class), anyList());
		// primary should be added but not secondary.
		verify(mockDao).createOrUpdate(MigrationType.NODE, Lists.newArrayList(nodeOne, nodeTwo));
	}
	
	@Test
	public void testCreateManifestKey() {
		String manifestKey = MigrationManagerImpl.createManifestKey("some/base/key");
		assertEquals("some/base/key/manifest.json", manifestKey);
	}
	
	@Test
	public void testGetManifestWithNoS3Manifest() {
		when(mockDao.getObjectForType(any())).thenReturn(new DBONode());
		when(mockS3Client.doesObjectExist(any(), any())).thenReturn(false);
		when(mockDao.getTypeData(MigrationType.NODE)).thenReturn(nodeTypeData);
		when(mockDao.getTypeData(MigrationType.NODE_REVISION)).thenReturn(revisionTypeData);
		
		String manifestKey = MigrationManagerImpl.createManifestKey(restoreTypeRequest.getBackupFileKey());
		// Call under test
		BackupManifest manifest = manager.getManifest(restoreTypeRequest);

		BackupManifest expected = new BackupManifest().setPrimaryType(nodeTypeData)
						.setAliasType(restoreTypeRequest.getAliasType()).setBatchSize(restoreTypeRequest.getBatchSize())
						.setMaximumId(restoreTypeRequest.getMaximumRowId())
						.setMinimumId(restoreTypeRequest.getMinimumRowId())
				.setSecondaryTypes(Arrays.asList(revisionTypeData));
		assertEquals(expected, manifest);
		verify(mockDao).getObjectForType(MigrationType.NODE);
		verify(mockS3Client).doesObjectExist(MigrationManagerImpl.backupBucket, manifestKey);
		verify(mockS3Client, never()).getObject(any());
	}
	
	@Test
	public void testGetManifestWithS3Manifest() throws Exception {
		when(mockS3Client.doesObjectExist(any(), any())).thenReturn(true);

		BackupManifest s3Manifest = new BackupManifest().setPrimaryType(nodeTypeData).setMinimumId(1L).setMaximumId(14L)
				.setSecondaryTypes(Arrays.asList(revisionTypeData));

		S3Object mockS3Object = Mockito.mock(S3Object.class);
		S3ObjectInputStream objectStream = Mockito.spy(new S3ObjectInputStream(new ByteArrayInputStream(
				EntityFactory.createJSONStringForEntity(s3Manifest).getBytes(StandardCharsets.UTF_8)), null));
		
		when(mockS3Object.getObjectContent()).thenReturn(objectStream);
		when(mockS3Client.getObject(any())).thenReturn(mockS3Object);
		String manifestKey = MigrationManagerImpl.createManifestKey(restoreTypeRequest.getBackupFileKey());
		// Call under test
		BackupManifest manifest = manager.getManifest(restoreTypeRequest);
		assertEquals(s3Manifest, manifest);
		
		verify(mockDao, never()).getObjectForType(eq(MigrationType.NODE));
		verify(mockS3Client).doesObjectExist(MigrationManagerImpl.backupBucket, manifestKey);
		verify(mockS3Client).getObject(new GetObjectRequest(MigrationManagerImpl.backupBucket, manifestKey));
		verify(objectStream).close();
	}

	@Test
	public void testGetManifestWithS3ManifestWithOldFile() throws Exception {
		when(mockS3Client.doesObjectExist(any(), any())).thenReturn(true);

		String manifestJSON = TestUtils.loadFromClasspath("manifest-test.json");
		S3Object mockS3Object = Mockito.mock(S3Object.class);
		S3ObjectInputStream objectStream = Mockito.spy(new S3ObjectInputStream(new StringInputStream(
				manifestJSON), null));
		
		when(mockS3Object.getObjectContent()).thenReturn(objectStream);
		when(mockS3Client.getObject(any())).thenReturn(mockS3Object);
		String manifestKey = MigrationManagerImpl.createManifestKey(restoreTypeRequest.getBackupFileKey());
		// Call under test
		BackupManifest manifest = manager.getManifest(restoreTypeRequest);
		assertNotNull(manifest);
		
		verify(mockS3Client).doesObjectExist(MigrationManagerImpl.backupBucket, manifestKey);
		verify(mockS3Client).getObject(new GetObjectRequest(MigrationManagerImpl.backupBucket, manifestKey));
		verify(objectStream).close();
	}

	@Test
	public void testRestoreRequestNoRange() throws IOException {
		when(mockS3Client.doesObjectExist(any(), any())).thenReturn(false);
		when(mockFileProvider.createTempFile(any(), any())).thenReturn(mockFile);
		when(mockFileProvider.createFileInputStream(any())).thenReturn(mockFileInputStream);
		when(mockUser.isAdmin()).thenReturn(true);
		when(mockBackupFileStream.readBackupFile(any(), any())).thenReturn(allObjects);
		when(mockDao.getObjectForType(any())).thenReturn(new DBONode());
		when(mockDao.isMigrationTypeRegistered(MigrationType.NODE.name())).thenReturn(true);
		when(mockDao.isMigrationTypeRegistered(MigrationType.NODE_REVISION.name())).thenReturn(true);
		when(mockDao.getTypeData(MigrationType.NODE)).thenReturn(nodeTypeData);
		when(mockDao.getTypeData(MigrationType.NODE_REVISION)).thenReturn(revisionTypeData);
		
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
		// the bucket retention policy will delete the files.
		verify(mockS3Client, never()).deleteObject(any(), any());
		verify(mockFileInputStream).close();
		verify(mockFile).delete();
		
		// delete by range should not occur when the range is missing.
		verify(mockDao, never()).deleteByRange(any(TypeData.class), anyLong(), anyLong());
	}
	
	
	@Test
	public void testRestoreRequestWithRange() throws IOException {
		when(mockFileProvider.createTempFile(any(), any())).thenReturn(mockFile);
		when(mockFileProvider.createFileInputStream(any())).thenReturn(mockFileInputStream);
		when(mockUser.isAdmin()).thenReturn(true);
		when(mockBackupFileStream.readBackupFile(any(), any())).thenReturn(allObjects);
		when(mockDao.getObjectForType(any())).thenReturn(new DBONode());
		when(mockDao.isMigrationTypeRegistered(anyString())).thenReturn(true);
		when(mockDao.getTypeData(MigrationType.NODE)).thenReturn(nodeTypeData);
		when(mockDao.getTypeData(MigrationType.NODE_REVISION)).thenReturn(revisionTypeData);
		
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
		verify(mockFileInputStream).close();
		verify(mockFile).delete();
		
		// should delete the primary
		verify(mockDao).deleteByRange(nodeTypeData, min, max);
		// should delete the secondary
		verify(mockDao).deleteByRange(revisionTypeData, min, max);
	}
	
	
	@Test
	public void testRestoreRequestCleanup() throws IOException {
		when(mockFileProvider.createTempFile(any(), any())).thenReturn(mockFile);
		when(mockUser.isAdmin()).thenReturn(true);

		// setup failure
		AmazonServiceException exception = new AmazonServiceException("failed");
		when(mockS3Client.getObject(any(), any(File.class))).thenThrow(exception);
		String message = assertThrows(AmazonServiceException.class, ()->{
			// call under test
			manager.restoreRequest(mockUser, restoreTypeRequest);
		}).getMessage();
		// expected
		assertEquals(message, exception.getMessage());
		// temp should be deleted.
		verify(mockFile).delete();
	}
	
	
	@Test
	public void testRestoreRequestUnauthorized() throws IOException {
		// must be an admin
		when(mockUser.isAdmin()).thenReturn(false);
		assertThrows(UnauthorizedException.class, ()->{
			// call under test
			manager.restoreRequest(mockUser, restoreTypeRequest);
		});
	}
	
	@Test
	public void testRestoreRequestNullUser() throws IOException {
		mockUser = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.restoreRequest(mockUser, restoreTypeRequest);
		});
	}
	
	@Test
	public void testRestoreRequestNullRequest() throws IOException {
		restoreTypeRequest = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.restoreRequest(mockUser, restoreTypeRequest);
		});
	}
	
	@Test
	public void testRestoreRequestNullBackupAlias() throws IOException {
		restoreTypeRequest.setAliasType(null);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.restoreRequest(mockUser, restoreTypeRequest);
		});
	}
	
	@Test
	public void testRestoreRequestNullKey() throws IOException {
		restoreTypeRequest.setBackupFileKey(null);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.restoreRequest(mockUser, restoreTypeRequest);
		});
	}
	
	@Test
	public void testRestoreRequestNullMigrationType() throws IOException {
		restoreTypeRequest.setMigrationType(null);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.restoreRequest(mockUser, restoreTypeRequest);
		});
	}
	
	@Test
	public void testRestoreRequestNullBatchSize() throws IOException {
		restoreTypeRequest.setBatchSize(null);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.restoreRequest(mockUser, restoreTypeRequest);
		});
	}
	
	@Test
	public void testDeleteByRange() throws IOException {
		when(mockDao.isMigrationTypeRegistered(any())).thenReturn(true);
		
		long minimumId = 3L;
		long maximumId = 45L;
		manifest.setMinimumId(minimumId);
		manifest.setMaximumId(maximumId);
		
		// call under test
		manager.deleteByRange(manifest);
		// should delete the primary
		verify(mockDao).deleteByRange(nodeTypeData, minimumId, maximumId);
		// should delete the secondary
		verify(mockDao).deleteByRange(revisionTypeData, minimumId, maximumId);
		verify(mockDao).isMigrationTypeRegistered(MigrationType.NODE.name());
		verify(mockDao).isMigrationTypeRegistered(MigrationType.NODE_REVISION.name());
	}
	
	@Test
	public void testDeleteByRangeWitNullMax() throws IOException {
		Long minimumId = 3L;
		Long maximumId = null;
		manifest.setMinimumId(minimumId);
		manifest.setMaximumId(maximumId);
		
		// call under test
		manager.deleteByRange(manifest);
		verify(mockDao, never()).deleteByRange(any(), anyLong(), anyLong());
		verify(mockDao, never()).isMigrationTypeRegistered(any());
	}
	
	@Test
	public void testDeleteByRangeWitNullMin() throws IOException {
		Long minimumId = null;
		Long maximumId = 45L;
		manifest.setMinimumId(minimumId);
		manifest.setMaximumId(maximumId);
		
		// call under test
		manager.deleteByRange(manifest);
		verify(mockDao, never()).deleteByRange(any(), anyLong(), anyLong());
		verify(mockDao, never()).isMigrationTypeRegistered(any());
	}
	
	@Test
	public void testDeleteByRangeWitNullBoth() throws IOException {
		Long minimumId = null;
		Long maximumId = null;
		manifest.setMinimumId(minimumId);
		manifest.setMaximumId(maximumId);
		
		// call under test
		manager.deleteByRange(manifest);
		verify(mockDao, never()).deleteByRange(any(), anyLong(), anyLong());
		verify(mockDao, never()).isMigrationTypeRegistered(any());
	}
	
	@Test
	public void testDeleteByRangeWithNotRegisteredPrimary() {
		when(mockDao.isMigrationTypeRegistered(MigrationType.NODE.name())).thenReturn(false);
		when(mockDao.isMigrationTypeRegistered(MigrationType.NODE_REVISION.name())).thenReturn(true);
		
		long minimumId = 3L;
		long maximumId = 45L;
		manifest.setMinimumId(minimumId);
		manifest.setMaximumId(maximumId);
		// call under test
		manager.deleteByRange(manifest);
		verify(mockDao, never()).deleteByRange(eq(nodeTypeData), anyLong(), anyLong());
		// should delete the secondary
		verify(mockDao).deleteByRange(revisionTypeData, minimumId, maximumId);
	}
	
	@Test
	public void testDeleteByRangeWithNotRegisteredSecondary() {
		when(mockDao.isMigrationTypeRegistered(MigrationType.NODE.name())).thenReturn(true);
		when(mockDao.isMigrationTypeRegistered(MigrationType.NODE_REVISION.name())).thenReturn(false);
		long minimumId = 3L;
		long maximumId = 45L;
		manifest.setMinimumId(minimumId);
		manifest.setMaximumId(maximumId);
		// call under test
		manager.deleteByRange(manifest);
		
		verify(mockDao, never()).deleteByRange(eq(revisionTypeData), anyLong(), anyLong());
		verify(mockDao).deleteByRange(nodeTypeData, minimumId, maximumId);
	}
	
	@Test
	public void testCalculateOptimalRanges() throws IOException {
		when(mockUser.isAdmin()).thenReturn(true);
		when(mockDao.calculateRangesForType(any(), anyLong(),  anyLong(),  anyLong())).thenReturn(ranges);
		
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
	
	@Test
	public void testCalculateOptimalRangesUnauthorized() {
		when(mockUser.isAdmin()).thenReturn(false);
		assertThrows(UnauthorizedException.class, ()->{
			// call under test
			manager.calculateOptimalRanges(mockUser, optimalRangeRequest);
		});
	}
	
	@Test
	public void testCalculateOptimalRangesNullRequest() {
		optimalRangeRequest = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.calculateOptimalRanges(mockUser, optimalRangeRequest);
		});
	}
	
	@Test
	public void testCalculateOptimalRangesNullType() {
		optimalRangeRequest.setMigrationType(null);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.calculateOptimalRanges(mockUser, optimalRangeRequest);
		});
	}
	
	@Test
	public void testCalculateOptimalRangesNullMin() {
		optimalRangeRequest.setMinimumId(null);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.calculateOptimalRanges(mockUser, optimalRangeRequest);
		});
	}
	
	@Test
	public void testCalculateOptimalRangesNullMax() {
		optimalRangeRequest.setMaximumId(null);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.calculateOptimalRanges(mockUser, optimalRangeRequest);
		});
	}
	
	@Test
	public void testCalculateOptimalRangesNullOptimalSize() {
		optimalRangeRequest.setOptimalRowsPerRange(null);
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.calculateOptimalRanges(mockUser, optimalRangeRequest);
		});
	}
	
	@Test
	public void testCalculateBatchChecksums() throws IOException {
		when(mockUser.isAdmin()).thenReturn(true);
		when(mockDao.calculateBatchChecksums(any())).thenReturn(Lists.newArrayList(sum));
		
		BatchChecksumRequest request = new BatchChecksumRequest();
		request.setBatchSize(3L);
		request.setMinimumId(0L);
		request.setMaximumId(0L);
		request.setSalt("some salt");
		request.setMigrationType(MigrationType.FILE_HANDLE);
		// call under test
		BatchChecksumResponse response = manager.calculateBatchChecksums(mockUser, request);
		verify(mockDao).calculateBatchChecksums(request);
		assertNotNull(response);
		assertEquals(request.getMigrationType(), response.getMigrationType());
		assertNotNull(response.getCheksums());
		assertEquals(1, response.getCheksums().size());
	}
	
	@Test
	public void testCalculateBatchChecksumsNonAdmin() {
		when(mockUser.isAdmin()).thenReturn(false);
		BatchChecksumRequest request = new BatchChecksumRequest();
		request.setBatchSize(3L);
		request.setMinimumId(0L);
		request.setMaximumId(0L);
		request.setSalt("some salt");
		request.setMigrationType(MigrationType.FILE_HANDLE);

		assertThrows(UnauthorizedException.class, ()->{
			// call under test
			manager.calculateBatchChecksums(mockUser, request);
		});
	}
	
	@Test
	public void testCalculateBatchChecksumsNullUser() {
		BatchChecksumRequest request = new BatchChecksumRequest();
		request.setBatchSize(3L);
		request.setMinimumId(0L);
		request.setMaximumId(0L);
		request.setSalt("some salt");
		request.setMigrationType(MigrationType.FILE_HANDLE);
		mockUser = null;

		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.calculateBatchChecksums(mockUser, request);
		});
	}
	
	
	@Test
	public void testProcessAsyncMigrationTypeCountsRequestWitNullUser() {
		mockUser = null;
		AsyncMigrationTypeCountsRequest request = new AsyncMigrationTypeCountsRequest();
		request.setTypes(Lists.newArrayList(MigrationType.PRINCIPAL,MigrationType.NODE, MigrationType.CHANGE));
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.processAsyncMigrationTypeCountsRequest(mockUser, request);
		}).getMessage();
		assertEquals("user is required.",message);
	}
	
	@Test
	public void testProcessAsyncMigrationTypeCountsRequestWitNullRequest() {
		AsyncMigrationTypeCountsRequest request = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.processAsyncMigrationTypeCountsRequest(mockUser, request);
		}).getMessage();
		assertEquals("request is required.",message);
	}
	
	@Test
	public void testProcessAsyncMigrationTypeCountsRequestWitNullRequestTypes() {
		AsyncMigrationTypeCountsRequest request = new AsyncMigrationTypeCountsRequest();
		request.setTypes(null);
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.processAsyncMigrationTypeCountsRequest(mockUser, request);
		}).getMessage();
		assertEquals("request.types is required.",message);
	}
	
	@Test
	public void testProcessAsyncMigrationTypeCountsRequestWithNonAdmin() {
		when(mockUser.isAdmin()).thenReturn(false);
		
		AsyncMigrationTypeCountsRequest request = new AsyncMigrationTypeCountsRequest();
		request.setTypes(Lists.newArrayList(MigrationType.PRINCIPAL,MigrationType.NODE, MigrationType.CHANGE));
		
		assertThrows(UnauthorizedException.class, ()->{
			// call under test
			manager.processAsyncMigrationTypeCountsRequest(mockUser, request);
		});
		verify(mockDao, times(0)).getMigrationTypeCount(any());
	}
	
	@Test
	public void testProcessAsyncMigrationTypeCountsRequest() {
		when(mockUser.isAdmin()).thenReturn(true);

		MigrationTypeCount principalResult = new MigrationTypeCount().setType(MigrationType.PRINCIPAL).setMaxid(1L);
		MigrationTypeCount nodeResult = new MigrationTypeCount().setType(MigrationType.NODE).setMaxid(2L);
		MigrationTypeCount changeResult = new MigrationTypeCount().setType(MigrationType.CHANGE).setMaxid(3L);

		when(mockDao.getMigrationTypeCount(MigrationType.PRINCIPAL)).thenReturn(principalResult);
		when(mockDao.getMigrationTypeCount(MigrationType.NODE)).thenReturn(nodeResult);
		when(mockDao.getMigrationTypeCount(MigrationType.CHANGE)).thenReturn(changeResult);

		AsyncMigrationTypeCountsRequest request = new AsyncMigrationTypeCountsRequest();
		request.setTypes(Lists.newArrayList(MigrationType.PRINCIPAL, MigrationType.NODE, MigrationType.CHANGE));

		// The return order should match the request order.
		MigrationTypeCounts expected = new MigrationTypeCounts()
				.setList(Lists.newArrayList(principalResult, nodeResult, changeResult));
	
		InOrder inOrder = inOrder(mockDao);

		// call under test
		MigrationTypeCounts results = manager.processAsyncMigrationTypeCountsRequest(mockUser, request);
		assertEquals(expected, results);
		verify(mockDao, times(3)).getMigrationTypeCount(any());
		// changes must go first to ensure no change migrates before its associated data.
		inOrder.verify(mockDao).getMigrationTypeCount(MigrationType.CHANGE);
		inOrder.verify(mockDao).getMigrationTypeCount(MigrationType.PRINCIPAL);
		inOrder.verify(mockDao).getMigrationTypeCount(MigrationType.NODE);
	}
	
	@Test
	public void testProcessAsyncMigrationTypeCountsRequestWithoutChanges() {
		when(mockUser.isAdmin()).thenReturn(true);

		MigrationTypeCount principalResult = new MigrationTypeCount().setType(MigrationType.PRINCIPAL).setMaxid(1L);
		MigrationTypeCount nodeResult = new MigrationTypeCount().setType(MigrationType.NODE).setMaxid(2L);
		MigrationTypeCount acl = new MigrationTypeCount().setType(MigrationType.ACL).setMaxid(3L);

		when(mockDao.getMigrationTypeCount(MigrationType.PRINCIPAL)).thenReturn(principalResult);
		when(mockDao.getMigrationTypeCount(MigrationType.NODE)).thenReturn(nodeResult);
		when(mockDao.getMigrationTypeCount(MigrationType.ACL)).thenReturn(acl);

		AsyncMigrationTypeCountsRequest request = new AsyncMigrationTypeCountsRequest();
		request.setTypes(Lists.newArrayList(MigrationType.PRINCIPAL, MigrationType.NODE, MigrationType.ACL));

		// The return order should match the request order.
		MigrationTypeCounts expected = new MigrationTypeCounts()
				.setList(Lists.newArrayList(principalResult, nodeResult, acl));
	
		InOrder inOrder = inOrder(mockDao);

		// call under test
		MigrationTypeCounts results = manager.processAsyncMigrationTypeCountsRequest(mockUser, request);
		assertEquals(expected, results);
		verify(mockDao, times(3)).getMigrationTypeCount(any());
		inOrder.verify(mockDao).getMigrationTypeCount(MigrationType.PRINCIPAL);
		inOrder.verify(mockDao).getMigrationTypeCount(MigrationType.NODE);
		inOrder.verify(mockDao).getMigrationTypeCount(MigrationType.ACL);
	}
}
