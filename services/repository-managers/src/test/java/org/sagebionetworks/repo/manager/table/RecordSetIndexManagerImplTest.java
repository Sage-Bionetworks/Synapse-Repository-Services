package org.sagebionetworks.repo.manager.table;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.CsvFileHandleProvider;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.semaphore.LockContext;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.table.cluster.description.IndexDescription;
import org.sagebionetworks.table.cluster.description.RecordSetIndexDescription;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.util.progress.ProgressingCallable;

@ExtendWith(MockitoExtension.class)
public class RecordSetIndexManagerImplTest {

	@Mock
	private TableManagerSupport mockTableManagerSupport;
	@Mock
	private TableIndexConnectionFactory mockConnectionFactory;
	@Mock
	private ColumnModelManager mockColumnModelManager;
	@Mock
	private EntityManager mockEntityManager;
	@Mock
	private UserManager mockUserManager;
	@Mock
	private FileHandleManager mockFileHandleManager;
	@Mock
	private CsvFileHandleProvider mockCsvFileHandleProvider;
	@Mock
	private NodeDAO mockNodeDao;
	@Mock
	private LoggerProvider mockLoggerProvider;
	@Mock
	private Logger mockLogger;
	@Mock
	private TableIndexManager mockIndexManager;
	@Mock
	private ProgressCallback mockProgressCallback;
	@Mock
	private UserInfo mockAdminUser;

	@Spy
	@InjectMocks
	private RecordSetIndexManagerImpl manager;

	/** The unversioned entity key used for the exclusive lock + entity-level status. */
	private IdAndVersion entityKey;
	/** The versioned key the factory resolves the entity reference to. */
	private IdAndVersion versionedKey;
	/** A versioned IdAndVersion fed in to prove the manager still operates on entityKey. */
	private IdAndVersion incomingIdAndVersion;
	private RecordSet recordSet;
	private S3FileHandle fileHandle;
	private CsvTableDescriptor csvDescriptor;
	/** The provider-bound schema the worker reads. */
	private List<ColumnModel> persistedSchema;
	private String token;
	private long currentRevision;

	@BeforeEach
	public void before() {
		when(mockLoggerProvider.getLogger(RecordSetIndexManagerImpl.class.getName())).thenReturn(mockLogger);
		manager = Mockito.spy(new RecordSetIndexManagerImpl(mockTableManagerSupport, mockConnectionFactory,
				mockColumnModelManager, mockEntityManager, mockUserManager, mockFileHandleManager,
				mockCsvFileHandleProvider, mockNodeDao, mockLoggerProvider));

		incomingIdAndVersion = IdAndVersion.parse("syn999.2");
		entityKey = IdAndVersion.newBuilder().setId(999L).build();
		versionedKey = IdAndVersion.newBuilder().setId(999L).setVersion(2L).build();
		currentRevision = 2L;
		csvDescriptor = new CsvTableDescriptor().setIsFirstLineHeader(true);
		recordSet = new RecordSet().setDataFileHandleId("9991").setCsvDescriptor(csvDescriptor);
		recordSet.setId("syn999");
		recordSet.setVersionNumber(currentRevision);
		recordSet.setEtag("some-etag");
		fileHandle = new S3FileHandle();
		fileHandle.setId("9991");
		persistedSchema = Arrays.asList(
				new ColumnModel().setId("100").setName("a").setColumnType(ColumnType.STRING).setMaximumSize(50L),
				new ColumnModel().setId("101").setName("b").setColumnType(ColumnType.INTEGER));
		token = "the-token";
	}

	@Test
	public void testCreateOrUpdateRecordSetIndexHappyPath() throws Exception {
		stubLockToRunInline();
		when(mockTableManagerSupport.isIndexWorkRequired(entityKey)).thenReturn(true);
		when(mockTableManagerSupport.startTableProcessing(entityKey)).thenReturn(token);
		String versionedToken = "versioned-token";
		when(mockTableManagerSupport.startTableProcessing(versionedKey)).thenReturn(versionedToken);
		when(mockNodeDao.getCurrentRevisionNumber("999")).thenReturn(currentRevision);
		IndexDescription entityDescription = new RecordSetIndexDescription(entityKey, currentRevision);
		IndexDescription versionedDescription = new RecordSetIndexDescription(versionedKey, currentRevision);
		when(mockUserManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId())).thenReturn(mockAdminUser);
		when(mockEntityManager.getEntityForVersion(mockAdminUser, "999", currentRevision, RecordSet.class))
				.thenReturn(recordSet);
		when(mockFileHandleManager.getRawFileHandleUnchecked("9991")).thenReturn(fileHandle);
		// The schema is bound by the provider; the worker only reads it.
		when(mockTableManagerSupport.getTableSchema(versionedKey)).thenReturn(persistedSchema);
		when(mockConnectionFactory.connectToTableIndex(entityKey)).thenReturn(mockIndexManager);
		List<IndexDescription> bothDescriptions = Arrays.asList(entityDescription, versionedDescription);
		// Single CSV pass writes rows into both index tables on this TableIndexManager mock.
		doReturn(42L).when(manager).loadRows(eq(mockIndexManager), eq(bothDescriptions), eq(persistedSchema),
				eq(fileHandle), eq(csvDescriptor), eq(currentRevision));

		// call under test
		manager.createOrUpdateRecordSetIndex(incomingIdAndVersion, mockProgressCallback);

		// The worker no longer creates/binds columns — it reads the bound schema.
		verify(mockColumnModelManager, never()).createColumnModels(any(), any());
		verify(mockColumnModelManager, never()).bindColumnsToDefaultVersionOfObject(any(), any());
		verify(mockColumnModelManager, never()).bindColumnsToVersionOfObject(any(), any());
		// Both index tables are reset before the single-pass row load.
		verify(mockIndexManager).resetTableIndex(entityDescription, persistedSchema, false);
		verify(mockIndexManager).resetTableIndex(versionedDescription, persistedSchema, false);
		verify(manager).loadRows(mockIndexManager, bothDescriptions, persistedSchema, fileHandle, csvDescriptor,
				currentRevision);
		// Secondary indices are built and the version stamped on each table.
		verify(mockIndexManager).buildTableIndexIndices(entityDescription, persistedSchema);
		verify(mockIndexManager).setIndexVersion(entityKey, currentRevision);
		verify(mockIndexManager).buildTableIndexIndices(versionedDescription, persistedSchema);
		verify(mockIndexManager).setIndexVersion(versionedKey, currentRevision);
		// Both TABLE_STATUS rows flip to AVAILABLE.
		verify(mockTableManagerSupport).attemptToSetTableStatusToAvailable(eq(versionedKey), eq(versionedToken), eq("some-etag"));
		verify(mockTableManagerSupport).attemptToSetTableStatusToAvailable(eq(entityKey), eq(token), eq("some-etag"));
		verify(mockTableManagerSupport, never()).attemptToSetTableStatusToFailed(any(), any());
	}

	@Test
	public void testCreateOrUpdateRecordSetIndexWithUnboundSchemaFails() throws Exception {
		stubLockToRunInline();
		when(mockTableManagerSupport.isIndexWorkRequired(entityKey)).thenReturn(true);
		when(mockTableManagerSupport.startTableProcessing(entityKey)).thenReturn(token);
		when(mockNodeDao.getCurrentRevisionNumber("999")).thenReturn(currentRevision);
		when(mockUserManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId())).thenReturn(mockAdminUser);
		when(mockEntityManager.getEntityForVersion(mockAdminUser, "999", currentRevision, RecordSet.class))
				.thenReturn(recordSet);
		when(mockFileHandleManager.getRawFileHandleUnchecked("9991")).thenReturn(fileHandle);
		// No schema bound for this version.
		when(mockTableManagerSupport.getTableSchema(versionedKey)).thenReturn(Collections.emptyList());

		// call under test
		assertThrows(RuntimeException.class,
				() -> manager.createOrUpdateRecordSetIndex(incomingIdAndVersion, mockProgressCallback));

		verify(mockTableManagerSupport).attemptToSetTableStatusToFailed(eq(entityKey), any(Exception.class));
		verify(mockTableManagerSupport, never()).attemptToSetTableStatusToAvailable(any(), any(), any());
		verify(mockIndexManager, never()).resetTableIndex(any(IndexDescription.class), any(List.class),
				Mockito.anyBoolean());
	}

	@Test
	public void testCreateOrUpdateRecordSetIndexSkipsWhenNotRequired() throws Exception {
		stubLockToRunInline();
		// Incoming version matches the current revision, so the worker checks
		// the entity-level status row (the same one the versionless trigger
		// reset to PROCESSING).
		when(mockNodeDao.getCurrentRevisionNumber("999")).thenReturn(currentRevision);
		when(mockTableManagerSupport.isIndexWorkRequired(entityKey)).thenReturn(false);

		// call under test
		manager.createOrUpdateRecordSetIndex(incomingIdAndVersion, mockProgressCallback);

		verify(mockTableManagerSupport, never()).startTableProcessing(any());
		verify(mockTableManagerSupport, never()).attemptToSetTableStatusToAvailable(any(), any(), any());
		verify(mockTableManagerSupport, never()).attemptToSetTableStatusToFailed(any(), any());
		verifyNoMoreInteractions(mockConnectionFactory, mockEntityManager, mockFileHandleManager, mockColumnModelManager);
	}

	@Test
	public void testCreateOrUpdateRecordSetIndexForOlderVersionOnlyBuildsSnapshot() throws Exception {
		// An out-of-order or repair trigger arrives for v=1, but the latest
		// revision is v=3. The worker must build only T{id}_1, leaving the
		// entity-level alias T{id} (the target for "syn123" queries) pointing
		// at the latest revision.
		long olderVersion = 1L;
		long latestRevision = 3L;
		IdAndVersion olderIncoming = IdAndVersion.parse("syn999.1");
		IdAndVersion versionedOlderKey = IdAndVersion.newBuilder().setId(999L).setVersion(olderVersion).build();
		RecordSet olderRecordSet = new RecordSet().setDataFileHandleId("9991").setCsvDescriptor(csvDescriptor);
		olderRecordSet.setId("syn999");
		olderRecordSet.setVersionNumber(olderVersion);
		olderRecordSet.setEtag("older-etag");

		stubLockToRunInline();
		when(mockNodeDao.getCurrentRevisionNumber("999")).thenReturn(latestRevision);
		when(mockTableManagerSupport.isIndexWorkRequired(versionedOlderKey)).thenReturn(true);
		when(mockTableManagerSupport.startTableProcessing(versionedOlderKey)).thenReturn(token);
		IndexDescription versionedDescription = new RecordSetIndexDescription(versionedOlderKey, olderVersion);
		IndexDescription entityDescriptionAtOlder = new RecordSetIndexDescription(entityKey, olderVersion);
		when(mockUserManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId())).thenReturn(mockAdminUser);
		when(mockEntityManager.getEntityForVersion(mockAdminUser, "999", olderVersion, RecordSet.class))
				.thenReturn(olderRecordSet);
		when(mockFileHandleManager.getRawFileHandleUnchecked("9991")).thenReturn(fileHandle);
		// The older version reads its own snapshot's bound schema.
		when(mockTableManagerSupport.getTableSchema(versionedOlderKey)).thenReturn(persistedSchema);
		when(mockConnectionFactory.connectToTableIndex(entityKey)).thenReturn(mockIndexManager);
		List<IndexDescription> versionedOnly = Collections.singletonList(versionedDescription);
		doReturn(7L).when(manager).loadRows(eq(mockIndexManager), eq(versionedOnly), eq(persistedSchema),
				eq(fileHandle), eq(csvDescriptor), eq(olderVersion));

		// call under test
		manager.createOrUpdateRecordSetIndex(olderIncoming, mockProgressCallback);

		// Only the versioned index table is reset / built / version-stamped.
		verify(mockIndexManager).resetTableIndex(versionedDescription, persistedSchema, false);
		verify(mockIndexManager, never()).resetTableIndex(eq(entityDescriptionAtOlder), any(), Mockito.anyBoolean());
		verify(mockIndexManager).buildTableIndexIndices(versionedDescription, persistedSchema);
		verify(mockIndexManager, never()).buildTableIndexIndices(eq(entityDescriptionAtOlder), any());
		verify(mockIndexManager).setIndexVersion(versionedOlderKey, olderVersion);
		verify(mockIndexManager, never()).setIndexVersion(eq(entityKey), Mockito.anyLong());
		// Only the per-version status flips to AVAILABLE; the entity-level status row is untouched.
		verify(mockTableManagerSupport).attemptToSetTableStatusToAvailable(eq(versionedOlderKey), eq(token), eq("older-etag"));
		verify(mockTableManagerSupport, never()).attemptToSetTableStatusToAvailable(eq(entityKey), any(), any());
		verify(mockTableManagerSupport, never()).attemptToSetTableStatusToFailed(any(), any());
	}

	@Test
	public void testCreateOrUpdateRecordSetIndexForOlderVersionRecordsFailureAtVersionedStatus() throws Exception {
		// When an older-version rebuild fails, the failure must be persisted
		// on the per-version status row, not the entity-level one — the
		// entity-level alias may still be AVAILABLE for a different revision.
		long olderVersion = 1L;
		long latestRevision = 3L;
		IdAndVersion olderIncoming = IdAndVersion.parse("syn999.1");
		IdAndVersion versionedOlderKey = IdAndVersion.newBuilder().setId(999L).setVersion(olderVersion).build();

		stubLockToRunInline();
		when(mockNodeDao.getCurrentRevisionNumber("999")).thenReturn(latestRevision);
		when(mockTableManagerSupport.isIndexWorkRequired(versionedOlderKey)).thenReturn(true);
		when(mockTableManagerSupport.startTableProcessing(versionedOlderKey)).thenReturn(token);
		when(mockUserManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId())).thenReturn(mockAdminUser);
		when(mockEntityManager.getEntityForVersion(mockAdminUser, "999", olderVersion, RecordSet.class))
				.thenReturn(recordSet);
		when(mockFileHandleManager.getRawFileHandleUnchecked("9991")).thenReturn(fileHandle);
		when(mockTableManagerSupport.getTableSchema(versionedOlderKey)).thenReturn(Collections.emptyList());

		// call under test
		assertThrows(RuntimeException.class,
				() -> manager.createOrUpdateRecordSetIndex(olderIncoming, mockProgressCallback));

		verify(mockTableManagerSupport).attemptToSetTableStatusToFailed(eq(versionedOlderKey), any(Exception.class));
		verify(mockTableManagerSupport, never()).attemptToSetTableStatusToFailed(eq(entityKey), any());
		verify(mockTableManagerSupport, never()).attemptToSetTableStatusToAvailable(any(), any(), any());
	}

	@Test
	public void testCreateOrUpdateRecordSetIndexWithVersionlessIncomingRebuildsBoth() throws Exception {
		// A versionless message (legacy / explicit entity-level trigger) still
		// resolves to currentRevision and rebuilds both T{id} and T{id}_{current}.
		IdAndVersion versionless = IdAndVersion.newBuilder().setId(999L).build();
		stubLockToRunInline();
		when(mockTableManagerSupport.isIndexWorkRequired(entityKey)).thenReturn(true);
		when(mockTableManagerSupport.startTableProcessing(entityKey)).thenReturn(token);
		String versionedToken = "versioned-token";
		when(mockTableManagerSupport.startTableProcessing(versionedKey)).thenReturn(versionedToken);
		when(mockNodeDao.getCurrentRevisionNumber("999")).thenReturn(currentRevision);
		IndexDescription entityDescription = new RecordSetIndexDescription(entityKey, currentRevision);
		IndexDescription versionedDescription = new RecordSetIndexDescription(versionedKey, currentRevision);
		when(mockUserManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId())).thenReturn(mockAdminUser);
		when(mockEntityManager.getEntityForVersion(mockAdminUser, "999", currentRevision, RecordSet.class))
				.thenReturn(recordSet);
		when(mockFileHandleManager.getRawFileHandleUnchecked("9991")).thenReturn(fileHandle);
		when(mockTableManagerSupport.getTableSchema(versionedKey)).thenReturn(persistedSchema);
		when(mockConnectionFactory.connectToTableIndex(entityKey)).thenReturn(mockIndexManager);
		List<IndexDescription> bothDescriptions = Arrays.asList(entityDescription, versionedDescription);
		doReturn(42L).when(manager).loadRows(eq(mockIndexManager), eq(bothDescriptions), eq(persistedSchema),
				eq(fileHandle), eq(csvDescriptor), eq(currentRevision));

		// call under test
		manager.createOrUpdateRecordSetIndex(versionless, mockProgressCallback);

		verify(mockIndexManager).resetTableIndex(entityDescription, persistedSchema, false);
		verify(mockIndexManager).resetTableIndex(versionedDescription, persistedSchema, false);
		verify(mockTableManagerSupport).attemptToSetTableStatusToAvailable(eq(versionedKey), eq(versionedToken), eq("some-etag"));
		verify(mockTableManagerSupport).attemptToSetTableStatusToAvailable(eq(entityKey), eq(token), eq("some-etag"));
	}

	@Test
	public void testDeleteRecordSetIndex() throws Exception {
		when(mockConnectionFactory.connectToTableIndex(entityKey)).thenReturn(mockIndexManager);

		// call under test — delete drops the entity-level stub and unbinds the columns.
		// Per-version snapshot tables T{id}_{v} are intentionally left as unreachable orphans.
		manager.deleteRecordSetIndex(incomingIdAndVersion);

		verify(mockIndexManager).deleteTableIndex(entityKey);
		verify(mockColumnModelManager).unbindAllColumnsAndOwnerFromObject("999");
	}

	@Test
	public void testDeleteRecordSetIndexSurvivesIndexUnavailable() {
		when(mockConnectionFactory.connectToTableIndex(entityKey))
				.thenThrow(new TableIndexConnectionUnavailableException("nope"));

		// call under test — bindings are still cleaned up.
		manager.deleteRecordSetIndex(entityKey);

		verify(mockColumnModelManager).unbindAllColumnsAndOwnerFromObject("999");
	}

	@SuppressWarnings("unchecked")
	private void stubLockToRunInline() throws Exception {
		doAnswer(invocation -> {
			ProgressCallback inner = invocation.getArgument(0);
			ProgressingCallable<Object> callable = invocation.getArgument(3);
			return callable.call(inner);
		}).when(mockTableManagerSupport).tryRunWithTableExclusiveLock(any(ProgressCallback.class),
				any(LockContext.class), eq(entityKey), any(ProgressingCallable.class));
	}
}
