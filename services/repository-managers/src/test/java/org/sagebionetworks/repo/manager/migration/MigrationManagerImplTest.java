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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
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
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.daemon.BackupAliasType;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.DBOSubjectAccessRequirementBackup;
import org.sagebionetworks.repo.model.dbo.migration.ForeignKeyInfo;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableDAO;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.persistence.DBONode;
import org.sagebionetworks.repo.model.dbo.persistence.DBORevision;
import org.sagebionetworks.repo.model.dbo.persistence.DBOSubjectAccessRequirement;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.migration.BackupTypeListRequest;
import org.sagebionetworks.repo.model.migration.BackupTypeRangeRequest;
import org.sagebionetworks.repo.model.migration.BackupTypeResponse;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeChecksum;
import org.sagebionetworks.repo.model.migration.RestoreTypeRequest;
import org.sagebionetworks.repo.model.migration.RestoreTypeResponse;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
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
	AmazonS3Client mockS3Client;
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
	BackupTypeListRequest listRequest;
	BackupTypeRangeRequest rangeRequest;
	Long batchSize;
	List<Long> backupIds;
	List<MigratableDatabaseObject<?, ?>> allObjects;
	List<Long> bootstrapPrincipalIds;
	List<MigratableDatabaseObject<?, ?>> nodeStream;
	List<MigratableDatabaseObject<?, ?>> revisionStream;
	RestoreTypeRequest restoreTypeRequest;
	
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
		listRequest = new BackupTypeListRequest();
		listRequest.setAliasType(backupAlias);
		listRequest.setMigrationType(MigrationType.NODE);
		batchSize = 2L;
		listRequest.setBatchSize(batchSize);
		backupIds = Lists.newArrayList(123L,456L);
		listRequest.setRowIdsToBackup(backupIds);
		
		nodeStream = Lists.newArrayList(nodeOne, nodeTwo);
		revisionStream = Lists.newArrayList(revOne, revTwo);
		when(mockDao.streamDatabaseObjects(MigrationType.NODE, backupIds, batchSize)).thenReturn(nodeStream);
		when(mockDao.streamDatabaseObjects(MigrationType.NODE_REVISION, backupIds, batchSize)).thenReturn(revisionStream);
		
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
	}
	
	@Test
	public void testGetBackupDataBatched(){
		// Set the batch size to be two for this test
		manager.setBackupBatchMax(2);
		// This is the full list of data we expect
		List<Long> fullList = Arrays.asList(1l,2l,3l,4l,5l);
		List<StubDatabaseObject> expected = createExpected(fullList);
		List<Long> batchOne = Arrays.asList(1l,2l);
		List<Long> batchTwo = Arrays.asList(3l,4l);
		List<Long> batchThree = Arrays.asList(5l);
		// We expect the dao to be called three times, once for each batch.
		when(mockDao.getBackupBatch(StubDatabaseObject.class, batchOne)).thenReturn(expected.subList(0, 2));
		when(mockDao.getBackupBatch(StubDatabaseObject.class, batchTwo)).thenReturn(expected.subList(2, 4));
		when(mockDao.getBackupBatch(StubDatabaseObject.class, batchThree)).thenReturn(expected.subList(4, 5));
		// Make the real call
		List<StubDatabaseObject> resutls = manager.getBackupDataBatched(StubDatabaseObject.class, fullList);
		assertNotNull(resutls);
		// The result should match the expected
		assertEquals(expected, resutls);
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSubjectAccessRequirementRoundTrip() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Writer writer = new OutputStreamWriter(out, "UTF-8");
		DBOSubjectAccessRequirement sar1 = new DBOSubjectAccessRequirement();
		sar1.setAccessRequirementId(101L);
		sar1.setSubjectId(987L);
		sar1.setSubjectType("ENTITY");
		{
			List<DBOSubjectAccessRequirement> databaseList = Arrays.asList(new DBOSubjectAccessRequirement[]{sar1});
			// Translate to the backup objects
			MigratableTableTranslation<DBOSubjectAccessRequirement, DBOSubjectAccessRequirementBackup> translator = 
				sar1.getTranslator();
			List<DBOSubjectAccessRequirementBackup> backupList = new LinkedList<DBOSubjectAccessRequirementBackup>();
			for(DBOSubjectAccessRequirement dbo: databaseList){
				backupList.add(translator.createBackupFromDatabaseObject(dbo));
			}
			// Now write the backup list to the stream
			// we use the table name as the Alias
			String alias = sar1.getTableMapping().getTableName();
			// Now write the backup to the stream
			BackupMarshalingUtils.writeBackupToWriter(backupList, alias, writer);
			writer.close();
		}
		
		// now read back in
		{
			ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
			DBOSubjectAccessRequirement sar2 = new DBOSubjectAccessRequirement();
			String alias = sar2.getTableMapping().getTableName();
			List<DBOSubjectAccessRequirementBackup> backupList = 
				(List<DBOSubjectAccessRequirementBackup>) BackupMarshalingUtils.readBackupFromStream(sar2.getBackupClass(), alias, in);
			assertTrue(backupList!=null);
			assertTrue(!backupList.isEmpty());
			// Now translate from the backup objects to the database objects.
			MigratableTableTranslation<DBOSubjectAccessRequirement, DBOSubjectAccessRequirementBackup> translator = sar2.getTranslator();
			List<DBOSubjectAccessRequirement> databaseList = new LinkedList<DBOSubjectAccessRequirement>();
			for(DBOSubjectAccessRequirementBackup backup: backupList){
				databaseList.add(translator.createDatabaseObjectFromBackup(backup));
			}
			// check content
			assertEquals(1, databaseList.size());
			assertEquals(sar1, databaseList.iterator().next());
		}
		
	}

	/**
	 * This test is used during migrating the DBORevision from stack 99 to stack 100.
	 * The old DBORevision contains reference and references.
	 * The new DBORevision only contains reference and does not contain references.
	 * The purpose of this test is to make sure that MigrationManagerImpl.createOrUpdateBatch()
	 * successfully migrates the old object to the new one.
	 * 
	 * For more information, please see PLFM-3499.
	 * @throws IOException 
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void createOrUpdateBatchOfDBORevisionTestPLFM3499() throws IOException {
		MigratableDatabaseObject mdo = new DBORevision();
		String alias = mdo.getTableMapping().getTableName();

		Reference ref = new Reference();
		ref.setTargetId("123");
		ref.setTargetVersionNumber(1L);

		String xml = 
				"<linked-list>" +
					"<JDOREVISION>" +
						"<owner>4490</owner>" +
						"<revisionNumber>1</revisionNumber>" +
						"<label>0.0.0</label>" +
						"<comment>0.0.0</comment>" +
						"<modifiedBy>273954</modifiedBy>" +
						"<modifiedOn>1312679694610</modifiedOn>" +
						"<annotations>H4sIAAAAAAAAALPJS8xN1S0uSExOteNSULDJTSwA0UBWal5JUSWEDeQVlxRl5qXbObq4eIZ4+vs5" +
								"+tjoQ4VgKhLz8vJLEksy8/OKYWJwfY4IOX0kyZT80qScVBySOfm49SWW4NKVlJOfhE3KRh/DfTb6" +
								"SF7E7t2AIE9fx6DIYeVXG31wHNvoI8U8AD9tTV8GAgAA</annotations>" +
						"<reference>H4sIAAAAAAAAALPJL0rXK05MT03KzM9LLSnPL8ou1itKLcjXy81PSc3RC0pNSy1KzUtOteNSULAp" +
								"SSxKTy3xTLEzNDK20YfzEFJhqUXFQIP8SnOTUovsDGFqUIW5bPSJthUA9SLq6KAAAAA=</reference>" +
					"</JDOREVISION>" +
				"</linked-list>";
		ByteArrayInputStream in = new ByteArrayInputStream(xml.getBytes("UTF-8"));

		List<DBORevision> backupList = (List<DBORevision>) BackupMarshalingUtils.readBackupFromStream(mdo.getBackupClass(), alias, in);
		assertNotNull(backupList);
		assertEquals(backupList.size(), 1);

		MigratableTableTranslation<DBORevision, DBORevision> translator = mdo.getTranslator();
		DBORevision backup = backupList.get(0);
		assertNotNull(backup.getReference());

		DBORevision databaseObject = translator.createDatabaseObjectFromBackup(backup);
		assertNotNull(databaseObject);
		assertEquals(ref, JDOSecondaryPropertyUtils.decompressedReference(databaseObject.getReference()));
	}
	
	/**
	 * Build a list of objects from a list of IDs
	 * @param fullList
	 * @return
	 */
	private List<StubDatabaseObject> createExpected(List<Long> fullList) {
		List<StubDatabaseObject> result = new LinkedList<StubDatabaseObject>();
		for(Long id: fullList){
			result.add(new StubDatabaseObject(id));
		}
		return result;
	}

	/**
	 * Simple Stub DatabaseObject for testing.
	 * @author jmhill
	 *
	 */
	public static class StubDatabaseObject implements DatabaseObject<StubDatabaseObject>{
		
		Long id;
		
		public StubDatabaseObject(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@Override
		public TableMapping<StubDatabaseObject> getTableMapping() {
			return null;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((id == null) ? 0 : id.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			StubDatabaseObject other = (StubDatabaseObject) obj;
			if (id == null) {
				if (other.id != null)
					return false;
			} else if (!id.equals(other.id))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "StubDatabaseObject [id=" + id + "]";
		}
		
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
		verify(mockDao).listNonRestrictedForeignKeys();
		verify(mockDao).mapSecondaryTablesToPrimaryGroups();
	}
	
	@Test
	public void testInitialize() {
		manager.initialize();
		// should trigger foreign key validation
		verify(mockDao).listNonRestrictedForeignKeys();
		verify(mockDao).mapSecondaryTablesToPrimaryGroups();
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
	public void testBackupListRequest() throws IOException {
 		// call under test
		manager.backupRequest(mockUser, listRequest);
		verify(mockBackupFileStream).writeBackupFile(eq(mockOutputStream), iterableCator.capture(), eq(backupAlias), eq(batchSize));
		List<MigratableDatabaseObject<?, ?>> results = new LinkedList<>();
		for(MigratableDatabaseObject<?, ?> object: iterableCator.getValue()) {
			results.add(object);
		}
		assertEquals(allObjects,results);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testBackupListRequestNonAdmin() throws IOException {
		when(mockUser.isAdmin()).thenReturn(false);
 		// call under test
		manager.backupRequest(mockUser, listRequest);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testBackupListRequestNullUser() throws IOException {
		mockUser = null;
 		// call under test
		manager.backupRequest(mockUser, listRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testBackupListRequestNullRequset() throws IOException {
		listRequest  = null;
 		// call under test
		manager.backupRequest(mockUser, listRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testBackupListRequestNullAliasType() throws IOException {
		listRequest.setAliasType(null);
 		// call under test
		manager.backupRequest(mockUser, listRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testBackupListRequestNullMigrationType() throws IOException {
		listRequest.setMigrationType(null);
 		// call under test
		manager.backupRequest(mockUser, listRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testBackupListRequestNullBatchSize() throws IOException {
		listRequest.setBatchSize(null);
 		// call under test
		manager.backupRequest(mockUser, listRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testBackupListRequestNullRowIds() throws IOException {
		listRequest.setRowIdsToBackup(null);
 		// call under test
		manager.backupRequest(mockUser, listRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testBackupListRequestEmptyRowIds() throws IOException {
		listRequest.setRowIdsToBackup(new LinkedList<>());
 		// call under test
		manager.backupRequest(mockUser, listRequest);
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
	public void testDeleteByIdAndFireEvent() {
		MigrationType type = MigrationType.NODE;
		List<Long> toDelete = Lists.newArrayList(123L,345L);
		int count = 1;
		when(mockDao.deleteById(any(MigrationType.class), anyListOf(Long.class))).thenReturn(count);
		int result = this.manager.deleteByIdAndFireEvent(type, toDelete);
		assertEquals(count, result);
		verify(mockDao).deleteById(type, toDelete);
		verify(mockMigrationListener).beforeDeleteBatch(type, toDelete);
	}
	
	@Test
	public void testDeleteByIdAndFireEventFiltered() {
		MigrationType type = MigrationType.PRINCIPAL;
		// include a bootstrap princpal
		List<Long> toDelete = Lists.newArrayList(
				123L,
				AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId()
		);
		// call under test
		this.manager.deleteByIdAndFireEvent(type, toDelete);
		List<Long> expectedId = Lists.newArrayList(123L);
		verify(mockDao).deleteById(type, expectedId);
		verify(mockMigrationListener).beforeDeleteBatch(type, expectedId);
	}
	
	@Test
	public void testPrincipalTypes() {
		assertEquals(3, MigrationManagerImpl.PRINCIPAL_TYPES.size());
		assertTrue(MigrationManagerImpl.PRINCIPAL_TYPES.contains(MigrationType.PRINCIPAL));
		assertTrue(MigrationManagerImpl.PRINCIPAL_TYPES.contains(MigrationType.CREDENTIAL));
		assertTrue(MigrationManagerImpl.PRINCIPAL_TYPES.contains(MigrationType.GROUP_MEMBERS));
	}
	
	@Test
	public void testDeleteByIdAuthorized() {
		when(mockUser.isAdmin()).thenReturn(true);
		MigrationType type = MigrationType.NODE;
		List<Long> toDelete = Lists.newArrayList(123L,345L);
		when(mockDao.deleteById(any(MigrationType.class), anyListOf(Long.class))).thenReturn(1,2,3);
		// call under test
		int count = manager.deleteById(mockUser, type, toDelete);
		assertEquals(3, count);
		// should delete secondary types
		verify(mockDao).deleteById(MigrationType.NODE_REVISION, toDelete);
		verify(mockDao).deleteById(MigrationType.NODE, toDelete);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testDeleteByIdUnAuthorized() {
		when(mockUser.isAdmin()).thenReturn(false);
		MigrationType type = MigrationType.NODE;
		List<Long> toDelete = Lists.newArrayList(123L,345L);
		when(mockDao.deleteById(any(MigrationType.class), anyListOf(Long.class))).thenReturn(1,2,3);
		// call under test
		manager.deleteById(mockUser, type, toDelete);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testDeleteByIdNullUser() {
		mockUser = null;
		MigrationType type = MigrationType.NODE;
		List<Long> toDelete = Lists.newArrayList(123L,345L);
		// call under test
		manager.deleteById(mockUser, type, toDelete);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testDeleteByIdNullType() {
		MigrationType type = null;
		List<Long> toDelete = Lists.newArrayList(123L,345L);
		// call under test
		manager.deleteById(mockUser, type, toDelete);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testDeleteByIdNullIds() {
		MigrationType type = MigrationType.NODE;
		List<Long> toDelete = null;
		// call under test
		manager.deleteById(mockUser, type, toDelete);
	}
	
	@Test
	public void testDeleteByIdEmptyList() {
		MigrationType type = MigrationType.NODE;
		List<Long> toDelete = new LinkedList<>();
		// call under test
		int count = manager.deleteById(mockUser, type, toDelete);
		assertEquals(0, count);
		verify(mockDao, never()).deleteById(any(MigrationType.class), anyListOf(Long.class));
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
		// secondary data should be deleted.
		verify(mockMigrationListener).beforeDeleteBatch(MigrationType.NODE_REVISION, idList);
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
		// no secondary deletes.
		verify(mockMigrationListener, never()).beforeDeleteBatch(any(MigrationType.class), anyListOf(Long.class));
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
		verify(mockMigrationListener, never()).beforeDeleteBatch(any(MigrationType.class), anyList());
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
	public void testRestoreRequest() throws IOException {
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
}
