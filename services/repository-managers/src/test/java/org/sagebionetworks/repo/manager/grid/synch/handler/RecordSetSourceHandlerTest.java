package org.sagebionetworks.repo.manager.grid.synch.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.file.CsvFileHandleProvider;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.export.GridRecordSetExporter;
import org.sagebionetworks.repo.manager.grid.internal.replica.validation.GridRowValidator;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItem;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItemReader;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItemReference;
import org.sagebionetworks.repo.manager.grid.synch.row.CellCopyItem;
import org.sagebionetworks.repo.manager.grid.synch.row.RowCopyItem;
import org.sagebionetworks.repo.manager.table.RecordSetSchemaResolver;
import org.sagebionetworks.repo.manager.table.RecordSetSchemaResolver.ReconciledSchema;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.SyncType;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.schema.JsonSchemaObjectBinding;
import org.sagebionetworks.repo.model.schema.JsonSchemaVersionInfo;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.util.FileProvider;

import au.com.bytecode.opencsv.CSVReader;

@ExtendWith(MockitoExtension.class)
public class RecordSetSourceHandlerTest {

	@Mock
	private UserInfo mockUser;
	@Mock
	private EntityManager mockEntityManager;
	@Mock
	private FileHandleManager mockFileHandleManager;
	@Mock
	private CsvFileHandleProvider mockCsvFileHandleProvider;
	@Mock
	private RecordSetSchemaResolver mockSchemaResolver;
	@Mock
	private FileProvider mockFileProvider;
	@Mock
	private GridRecordSetExporter mockRecordSetExporter;
	@Mock
	private GridRowValidator mockGridRowValidator;
	@Mock
	private AsyncJobProgressCallback mockCallback;
	@Mock
	private CSVReader mockLatestReader;
	@Mock
	private CSVReader mockBaselineReader;
	@Mock
	private NodeDAO mockNodeDao;

	private GridSession session;
	private RecordSet recordSet;
	private RecordSet baselineRecordSet;
	private S3FileHandle latestFileHandle;
	private S3FileHandle baselineFileHandle;
	private CsvTableDescriptor csvDescriptor;
	private ReconciledSchema reconciledSchema;
	private File tempFile;

	// Expected keys (single-column upsertKey "id" of INTEGER type)
	private final String keyOne = UpsertKeyEncoder.encode(List.of(new ConValue(ConType.LONG, 1L)));
	private final String keyTwo = UpsertKeyEncoder.encode(List.of(new ConValue(ConType.LONG, 2L)));

	@BeforeEach
	public void before() throws IOException {
		csvDescriptor = new CsvTableDescriptor().setIsFirstLineHeader(true);
		session = new GridSession().setSessionId("321").setSourceEntityId("syn1").setSourceEntityVersionNumber(2L);

		recordSet = new RecordSet().setId("syn1").setDataFileHandleId("fhLatest").setUpsertKey(List.of("id"))
				.setCsvDescriptor(csvDescriptor);
		baselineRecordSet = new RecordSet().setId("syn1").setDataFileHandleId("fhBaseline").setCsvDescriptor(csvDescriptor);

		latestFileHandle = new S3FileHandle().setId("fhLatest");
		baselineFileHandle = new S3FileHandle().setId("fhBaseline");

		reconciledSchema = new ReconciledSchema(List.of(
				new ColumnModel().setName("id").setColumnType(ColumnType.INTEGER),
				new ColumnModel().setName("name").setColumnType(ColumnType.STRING).setMaximumSize(50L)),
				List.of(), Collections.emptyList());

		tempFile = Files.createTempFile("RecordSetSourceHandlerTest", ".bin").toFile();
	}

	@AfterEach
	public void after() {
		if (tempFile != null) {
			tempFile.delete();
		}
	}

	/**
	 * Wire the common stubs and construct the handler (which runs initialize()).
	 * Latest CSV has rows id=1,2; baseline (version 2) has only id=1.
	 */
	private RecordSetSourceHandler buildHandler() throws IOException {
		when(mockEntityManager.getEntity(mockUser, "syn1", RecordSet.class)).thenReturn(recordSet);
		when(mockFileHandleManager.getRawFileHandleUnchecked("fhLatest")).thenReturn(latestFileHandle);
		when(mockSchemaResolver.getReconciledSchema(eq("syn1"), eq(latestFileHandle), any()))
				.thenReturn(reconciledSchema);
		when(mockCsvFileHandleProvider.getCsvReader(latestFileHandle, csvDescriptor)).thenReturn(mockLatestReader);
		when(mockLatestReader.readNext()).thenReturn(new String[] { "id", "name" }, new String[] { "1", "Alice" },
				new String[] { "2", "Bob" }, null);

		when(mockEntityManager.getEntityForVersion(mockUser, "syn1", 2L, RecordSet.class))
				.thenReturn(baselineRecordSet);
		when(mockFileHandleManager.getRawFileHandleUnchecked("fhBaseline")).thenReturn(baselineFileHandle);
		when(mockCsvFileHandleProvider.getCsvReader(baselineFileHandle, csvDescriptor)).thenReturn(mockBaselineReader);
		when(mockBaselineReader.readNext()).thenReturn(new String[] { "id", "name" }, new String[] { "1", "Alice" },
				null);

		when(mockFileProvider.createTempFile(any(), any())).thenReturn(tempFile);
		when(mockFileProvider.createFileOutputStream(tempFile)).thenReturn(new FileOutputStream(tempFile));

		return new RecordSetSourceHandler(mockUser, session, mockEntityManager, mockFileHandleManager,
				mockCsvFileHandleProvider, mockSchemaResolver, mockFileProvider,
				mockRecordSetExporter, mockGridRowValidator, mockNodeDao);
	}

	@Test
	public void testGetCurrentSourceSchema() throws IOException {
		RecordSetSourceHandler handler = buildHandler();
		// call under test
		assertEquals(List.of("id", "name"), handler.getCurrentSourceSchema());
	}

	@Test
	public void testGetSourceVersion() throws IOException {
		recordSet.setVersionNumber(9L);
		RecordSetSourceHandler handler = buildHandler();
		// call under test — the version the grid synchronized to (latest revision read)
		assertEquals(Optional.of(9L), handler.getSourceVersion());
	}

	@Test
	public void testGetSourceSchemaId() throws IOException {
		when(mockEntityManager.findBoundSchema("syn1")).thenReturn(Optional.of(new JsonSchemaObjectBinding()
				.setJsonSchemaVersionInfo(new JsonSchemaVersionInfo().set$id("my.org-Schema-1.0.0"))));
		RecordSetSourceHandler handler = buildHandler();
		// call under test
		assertEquals(Optional.of("my.org-Schema-1.0.0"), handler.getSourceSchema$Id());
	}

	@Test
	public void testGetSourceSchemaIdWithNoBoundSchema() throws IOException {
		// findBoundSchema returns empty (Mockito default) → no schema id
		RecordSetSourceHandler handler = buildHandler();
		// call under test
		assertEquals(Optional.empty(), handler.getSourceSchema$Id());
	}

	@Test
	public void testGetRowKeyMatchesSourceRowKey() throws IOException {
		RecordSetSourceHandler handler = buildHandler();
		// A grid row whose upsertKey cell "id" = 1 must produce the same key as the
		// CSV source row id=1 (parity).
		RowCopyItem gridRow = org.mockito.Mockito.mock(RowCopyItem.class);
		when(gridRow.getCells()).thenReturn(
				List.of(new CellCopyItem().setName("id").setValue(new ConValue(ConType.LONG, 1L)),
						new CellCopyItem().setName("name").setValue(new ConValue(ConType.STRING, "Alice"))));

		// call under test
		assertEquals(keyOne, handler.getRowKey(gridRow));
	}

	@Test
	public void testWasInSyncedBaseline() throws IOException {
		RecordSetSourceHandler handler = buildHandler();
		// Baseline (version 2) contained only id=1.
		// call under test
		assertTrue(handler.wasInSyncedBaseline(keyOne));
		assertFalse(handler.wasInSyncedBaseline(keyTwo));
	}

	@Test
	public void testChangedSinceBaselineWhenUnchanged() throws IOException {
		// Latest id=1 ("Alice") matches baseline id=1 ("Alice") → not changed.
		RecordSetSourceHandler handler = buildHandler();
		// call under test
		assertFalse(handler.changedSinceBaseline(keyOne));
	}

	@Test
	public void testChangedSinceBaselineWhenChanged() throws IOException {
		// Latest id=1 ("Alice2") differs from baseline id=1 ("Alice") → changed, so a
		// row the user deleted from the grid will be re-imported.
		when(mockEntityManager.getEntity(mockUser, "syn1", RecordSet.class)).thenReturn(recordSet);
		when(mockFileHandleManager.getRawFileHandleUnchecked("fhLatest")).thenReturn(latestFileHandle);
		when(mockSchemaResolver.getReconciledSchema(eq("syn1"), eq(latestFileHandle), any()))
				.thenReturn(reconciledSchema);
		when(mockCsvFileHandleProvider.getCsvReader(latestFileHandle, csvDescriptor)).thenReturn(mockLatestReader);
		when(mockLatestReader.readNext()).thenReturn(new String[] { "id", "name" }, new String[] { "1", "Alice2" },
				null);
		when(mockEntityManager.getEntityForVersion(mockUser, "syn1", 2L, RecordSet.class))
				.thenReturn(baselineRecordSet);
		when(mockFileHandleManager.getRawFileHandleUnchecked("fhBaseline")).thenReturn(baselineFileHandle);
		when(mockCsvFileHandleProvider.getCsvReader(baselineFileHandle, csvDescriptor)).thenReturn(mockBaselineReader);
		when(mockBaselineReader.readNext()).thenReturn(new String[] { "id", "name" }, new String[] { "1", "Alice" },
				null);
		when(mockFileProvider.createTempFile(any(), any())).thenReturn(tempFile);
		when(mockFileProvider.createFileOutputStream(tempFile)).thenReturn(new FileOutputStream(tempFile));

		RecordSetSourceHandler handler = new RecordSetSourceHandler(mockUser, session, mockEntityManager,
				mockFileHandleManager, mockCsvFileHandleProvider, mockSchemaResolver, mockFileProvider,
				mockRecordSetExporter, mockGridRowValidator, mockNodeDao);
		// call under test
		assertTrue(handler.changedSinceBaseline(keyOne));
	}

	@Test
	public void testChangedSinceBaselineWhenNotInBothRevisions() throws IOException {
		// id=2 is only in the latest revision (not the baseline) → it is a new row,
		// not a change; only rows present in BOTH revisions can be "changed".
		RecordSetSourceHandler handler = buildHandler();
		// call under test
		assertFalse(handler.changedSinceBaseline(keyTwo));
	}

	@Test
	public void testWasInSyncedBaselineWithNullVersion() throws IOException {
		// A legacy session with no synced version has an empty baseline → no deletions inferred.
		session.setSourceEntityVersionNumber(null);
		when(mockEntityManager.getEntity(mockUser, "syn1", RecordSet.class)).thenReturn(recordSet);
		when(mockFileHandleManager.getRawFileHandleUnchecked("fhLatest")).thenReturn(latestFileHandle);
		when(mockSchemaResolver.getReconciledSchema(eq("syn1"), eq(latestFileHandle), any()))
				.thenReturn(reconciledSchema);
		when(mockCsvFileHandleProvider.getCsvReader(latestFileHandle, csvDescriptor)).thenReturn(mockLatestReader);
		when(mockLatestReader.readNext()).thenReturn(new String[] { "id", "name" }, new String[] { "1", "Alice" },
				null);
		when(mockFileProvider.createTempFile(any(), any())).thenReturn(tempFile);
		when(mockFileProvider.createFileOutputStream(tempFile)).thenReturn(new FileOutputStream(tempFile));

		RecordSetSourceHandler handler = new RecordSetSourceHandler(mockUser, session, mockEntityManager,
				mockFileHandleManager, mockCsvFileHandleProvider, mockSchemaResolver, mockFileProvider,
				mockRecordSetExporter, mockGridRowValidator, mockNodeDao);

		// call under test
		assertFalse(handler.wasInSyncedBaseline(keyOne));
	}

	@Test
	public void testGetSourceRowReaderRoundTrip() throws IOException {
		RecordSetSourceHandler handler = buildHandler();
		when(mockFileProvider.createRandomAccessFile(tempFile, "r"))
				.thenReturn(new RandomAccessFile(tempFile, "r"));

		// call under test
		try (RowSourceItemReader reader = handler.getSourceRowReader()) {
			Optional<RowSourceItemReference> row1 = reader.consumeRow(keyOne);
			assertTrue(row1.isPresent());
			RowSourceItem fetched = row1.get().fetchRow();
			Map<String, ConValue> data = fetched.getData();
			assertEquals(new ConValue(ConType.LONG, 1L), data.get("id"));
			assertEquals(new ConValue(ConType.STRING, "Alice"), data.get("name"));

			assertTrue(reader.consumeRow(keyTwo).isPresent());
		}
	}

	private RowCopyItem gridRowWithId(ConValue idValue) {
		RowCopyItem row = mock(RowCopyItem.class);
		when(row.getCells())
				.thenReturn(List.of(new CellCopyItem().setName("id").setValue(idValue)));
		return row;
	}

	@Test
	public void testCreateSynchRowWithCompleteKey() throws IOException {
		RecordSetSourceHandler handler = buildHandler();
		Map<String, Integer> headerIndex = Map.of("id", 0, "name", 1);
		// call under test — a complete upsertKey row gets the deterministic encoded key
		RowSourceItem row = handler.createSynchRow(new String[] { "1", "Alice" }, headerIndex);
		assertEquals(keyOne, row.getKey());
	}

	@Test
	public void testCreateSynchRowWithIncompleteKeyGetsUniqueKey() throws IOException {
		RecordSetSourceHandler handler = buildHandler();
		Map<String, Integer> headerIndex = Map.of("id", 0, "name", 1);
		// call under test — two keyless rows (no "id") must get distinct keys so they
		// do not collapse in the disk index, and neither collides with a real key.
		RowSourceItem rowA = handler.createSynchRow(new String[] { "", "Alice" }, headerIndex);
		RowSourceItem rowB = handler.createSynchRow(new String[] { "", "Bob" }, headerIndex);
		assertNotEquals(rowA.getKey(), rowB.getKey());
		assertNotEquals(keyOne, rowA.getKey());
		assertNotEquals(
				UpsertKeyEncoder.encode(List.of(new ConValue(ConType.UNDEFINED, null))), rowA.getKey());
	}

	/**
	 * Build a handler whose latest CSV has one complete-key row (id=1) and one
	 * keyless row (blank id). The latest revision number is {@code sourceVersion};
	 * the synced baseline is version 2 (containing only id=1).
	 */
	private RecordSetSourceHandler buildHandlerWithKeylessLatest(Long sourceVersion) throws IOException {
		recordSet.setVersionNumber(sourceVersion);
		when(mockEntityManager.getEntity(mockUser, "syn1", RecordSet.class)).thenReturn(recordSet);
		when(mockFileHandleManager.getRawFileHandleUnchecked("fhLatest")).thenReturn(latestFileHandle);
		when(mockSchemaResolver.getReconciledSchema(eq("syn1"), eq(latestFileHandle), any()))
				.thenReturn(reconciledSchema);
		when(mockCsvFileHandleProvider.getCsvReader(latestFileHandle, csvDescriptor)).thenReturn(mockLatestReader);
		when(mockLatestReader.readNext()).thenReturn(new String[] { "id", "name" }, new String[] { "1", "Alice" },
				new String[] { "", "Keyless" }, null);

		when(mockEntityManager.getEntityForVersion(mockUser, "syn1", 2L, RecordSet.class))
				.thenReturn(baselineRecordSet);
		when(mockFileHandleManager.getRawFileHandleUnchecked("fhBaseline")).thenReturn(baselineFileHandle);
		when(mockCsvFileHandleProvider.getCsvReader(baselineFileHandle, csvDescriptor)).thenReturn(mockBaselineReader);
		when(mockBaselineReader.readNext()).thenReturn(new String[] { "id", "name" }, new String[] { "1", "Alice" },
				null);

		when(mockFileProvider.createTempFile(any(), any())).thenReturn(tempFile);
		when(mockFileProvider.createFileOutputStream(tempFile)).thenReturn(new FileOutputStream(tempFile));

		return new RecordSetSourceHandler(mockUser, session, mockEntityManager, mockFileHandleManager,
				mockCsvFileHandleProvider, mockSchemaResolver, mockFileProvider,
				mockRecordSetExporter, mockGridRowValidator, mockNodeDao);
	}

	private int countSourceRows(RecordSetSourceHandler handler) throws IOException {
		when(mockFileProvider.createRandomAccessFile(tempFile, "r")).thenReturn(new RandomAccessFile(tempFile, "r"));
		try (RowSourceItemReader reader = handler.getSourceRowReader()) {
			int count = 0;
			java.util.Iterator<RowSourceItemReference> it = reader.remainingRows();
			while (it.hasNext()) {
				it.next();
				count++;
			}
			return count;
		}
	}

	@Test
	public void testKeylessSourceRowImportedWhenRevisionNewer() throws IOException {
		// Latest revision (3) is newer than the synced baseline (2): the keyless row
		// is considered a new row and must be present in the source.
		RecordSetSourceHandler handler = buildHandlerWithKeylessLatest(3L);
		// call under test
		assertEquals(2, countSourceRows(handler));
	}

	@Test
	public void testKeylessSourceRowOmittedWhenAlreadySynced() throws IOException {
		// Latest revision (2) equals the synced baseline (2): the grid already
		// reflects this revision, so the keyless row must NOT be re-imported (this
		// prevents PULL -> PULL_PUSH and redundant PULLs from duplicating it).
		RecordSetSourceHandler handler = buildHandlerWithKeylessLatest(2L);
		// call under test — only the complete-key row (id=1) remains in the source
		assertEquals(1, countSourceRows(handler));
	}

	@Test
	public void testIsUnmatchableCopyRowWithCompleteKey() throws IOException {
		RecordSetSourceHandler handler = buildHandler();
		// call under test — a grid row with a complete, first-seen upsertKey is matchable
		assertFalse(handler.isUnmatchableCopyRow(gridRowWithId(new ConValue(ConType.LONG, 1L)), keyOne));
	}

	@Test
	public void testIsUnmatchableCopyRowWithIncompleteKey() throws IOException {
		RecordSetSourceHandler handler = buildHandler();
		// call under test — a grid row with an incomplete upsertKey is not matchable
		String incompleteKey = UpsertKeyEncoder.encode(List.of(new ConValue(ConType.UNDEFINED, null)));
		assertTrue(handler.isUnmatchableCopyRow(gridRowWithId(new ConValue(ConType.UNDEFINED, null)), incompleteKey));
	}

	@Test
	public void testIsUnmatchableCopyRowFirstCompleteKeyIsMatchableSecondIsFrozen() throws IOException {
		// Two grid rows share the same complete key: the first occurrence is
		// matchable; the second (duplicate) is not matchable
		RecordSetSourceHandler handler = buildHandler();

		// call under test
		assertFalse(handler.isUnmatchableCopyRow(gridRowWithId(new ConValue(ConType.LONG, 1L)), keyOne));
		assertTrue(handler.isUnmatchableCopyRow(gridRowWithId(new ConValue(ConType.LONG, 1L)), keyOne));
	}

	@Test
	public void testIsUnmatchableCopyRowTwoIncompleteKeyRowsBothFrozenViaCompletenessNotDedup() throws IOException {
		// Two rows both encode the same degenerate incomplete-key string. Both must
		// be unmatchable, the dedupe logic should not apply.
		RecordSetSourceHandler handler = buildHandler();
		String incompleteKey = UpsertKeyEncoder.encode(List.of(new ConValue(ConType.UNDEFINED, null)));

		// call under test
		assertTrue(handler.isUnmatchableCopyRow(gridRowWithId(new ConValue(ConType.UNDEFINED, null)), incompleteKey));
		assertTrue(handler.isUnmatchableCopyRow(gridRowWithId(new ConValue(ConType.UNDEFINED, null)), incompleteKey));
	}

	/**
	 * Build a handler whose latest CSV has a duplicate complete-key row (id=1
	 * appears twice with different names) plus one unique row (id=2).
	 * The baseline still has only id=1 (Alice).
	 */
	private RecordSetSourceHandler buildHandlerWithDuplicateSourceKey() throws IOException {
		when(mockEntityManager.getEntity(mockUser, "syn1", RecordSet.class)).thenReturn(recordSet);
		when(mockFileHandleManager.getRawFileHandleUnchecked("fhLatest")).thenReturn(latestFileHandle);
		when(mockSchemaResolver.getReconciledSchema(eq("syn1"), eq(latestFileHandle), any()))
				.thenReturn(reconciledSchema);
		when(mockCsvFileHandleProvider.getCsvReader(latestFileHandle, csvDescriptor)).thenReturn(mockLatestReader);
		// id=1 appears twice; id=2 appears once
		when(mockLatestReader.readNext()).thenReturn(
				new String[] { "id", "name" },
				new String[] { "1", "Alice" },
				new String[] { "1", "Bob" },
				new String[] { "2", "Charlie" },
				null);

		when(mockEntityManager.getEntityForVersion(mockUser, "syn1", 2L, RecordSet.class))
				.thenReturn(baselineRecordSet);
		when(mockFileHandleManager.getRawFileHandleUnchecked("fhBaseline")).thenReturn(baselineFileHandle);
		when(mockCsvFileHandleProvider.getCsvReader(baselineFileHandle, csvDescriptor)).thenReturn(mockBaselineReader);
		when(mockBaselineReader.readNext()).thenReturn(new String[] { "id", "name" }, new String[] { "1", "Alice" },
				null);

		when(mockFileProvider.createTempFile(any(), any())).thenReturn(tempFile);
		when(mockFileProvider.createFileOutputStream(tempFile)).thenReturn(new FileOutputStream(tempFile));

		return new RecordSetSourceHandler(mockUser, session, mockEntityManager, mockFileHandleManager,
				mockCsvFileHandleProvider, mockSchemaResolver, mockFileProvider,
				mockRecordSetExporter, mockGridRowValidator, mockNodeDao);
	}

	@Test
	public void testDuplicateSourceKeyFirstOccurrenceMatchedSecondOccurrenceSurvivesToRemainingRows()
			throws IOException {
		// When the source CSV has two rows with the same key (id=1 → Alice and Bob),
		// consuming keyOne must return the first occurrence (Alice), and the duplicate
		// (Bob) must survive in remainingRows() so Phase 2 can add it to the copy.
		RecordSetSourceHandler handler = buildHandlerWithDuplicateSourceKey();
		when(mockFileProvider.createRandomAccessFile(tempFile, "r"))
				.thenReturn(new RandomAccessFile(tempFile, "r"));

		try (RowSourceItemReader reader = handler.getSourceRowReader()) {
			// call under test — first occurrence returned
			Optional<RowSourceItemReference> consumed = reader.consumeRow(keyOne);
			assertTrue(consumed.isPresent());
			assertEquals(new ConValue(ConType.STRING, "Alice"), consumed.get().fetchRow().getData().get("name"));

			// call under test — duplicate (Bob) and Charlie survive as remaining rows
			Iterator<RowSourceItemReference> remaining = reader.remainingRows();
			assertTrue(remaining.hasNext());
			assertEquals(new ConValue(ConType.STRING, "Bob"), remaining.next().fetchRow().getData().get("name"));
			assertTrue(remaining.hasNext());
			assertEquals(new ConValue(ConType.STRING, "Charlie"), remaining.next().fetchRow().getData().get("name"));
			assertFalse(remaining.hasNext());
		}
	}

	@Test
	public void testChangedSinceBaselineWithDuplicateSourceKeyUsesFirstOccurrence() throws IOException {
		// When the source CSV has two rows with the same key and the first occurrence
		// is identical to the baseline, changedSinceBaseline must return false.
		// A bug where latestHashByKey stores the last occurrence's hash would
		// incorrectly return true when the duplicate rows differ from each other.
		when(mockEntityManager.getEntity(mockUser, "syn1", RecordSet.class)).thenReturn(recordSet);
		when(mockFileHandleManager.getRawFileHandleUnchecked("fhLatest")).thenReturn(latestFileHandle);
		when(mockSchemaResolver.getReconciledSchema(eq("syn1"), eq(latestFileHandle), any()))
				.thenReturn(reconciledSchema);
		when(mockCsvFileHandleProvider.getCsvReader(latestFileHandle, csvDescriptor)).thenReturn(mockLatestReader);
		// id=1 appears twice: first is Alice (same as baseline), second is Bob (different)
		when(mockLatestReader.readNext()).thenReturn(
				new String[] { "id", "name" },
				new String[] { "1", "Alice" },
				new String[] { "1", "Bob" },
				null);

		when(mockEntityManager.getEntityForVersion(mockUser, "syn1", 2L, RecordSet.class))
				.thenReturn(baselineRecordSet);
		when(mockFileHandleManager.getRawFileHandleUnchecked("fhBaseline")).thenReturn(baselineFileHandle);
		when(mockCsvFileHandleProvider.getCsvReader(baselineFileHandle, csvDescriptor)).thenReturn(mockBaselineReader);
		when(mockBaselineReader.readNext()).thenReturn(new String[] { "id", "name" }, new String[] { "1", "Alice" },
				null);

		when(mockFileProvider.createTempFile(any(), any())).thenReturn(tempFile);
		when(mockFileProvider.createFileOutputStream(tempFile)).thenReturn(new FileOutputStream(tempFile));

		RecordSetSourceHandler handler = new RecordSetSourceHandler(mockUser, session, mockEntityManager,
				mockFileHandleManager, mockCsvFileHandleProvider, mockSchemaResolver, mockFileProvider,
				mockRecordSetExporter, mockGridRowValidator, mockNodeDao);

		// call under test — first occurrence (Alice) matches baseline (Alice): not changed
		assertFalse(handler.changedSinceBaseline(keyOne));
	}

	@Test
	public void testValidateSyncTypeRecordSetNullThrows() throws IOException {
		RecordSetSourceHandler handler = buildHandler();
		// call under test — the manager applies the PULL_PUSH default before calling
		// this, so a null here is a contract violation.
		assertThrows(IllegalArgumentException.class, () -> handler.validateSyncType(null));
	}

	@Test
	public void testValidateSyncTypeRecordSetPull() throws IOException {
		RecordSetSourceHandler handler = buildHandler();
		// call under test — RecordSet supports PULL (no exception)
		handler.validateSyncType(SyncType.PULL);
	}

	@Test
	public void testValidateSyncTypeRecordSetPullPush() throws IOException {
		RecordSetSourceHandler handler = buildHandler();
		// call under test — RecordSet supports PULL_PUSH (no exception)
		handler.validateSyncType(SyncType.PULL_PUSH);
	}

	@Test
	public void testIsColumnExcludedFromMatchingWithGridOnlyColumn() throws IOException {
		RecordSetSourceHandler handler = buildHandler();
		// Source schema is [id, name]; a grid column absent from the source is preserved
		// (excluded from matching so it is never dropped or pushed).
		// call under test
		assertTrue(handler.isColumnExcludedFromMatching("extra"));
	}

	@Test
	public void testIsColumnExcludedFromMatchingWithSourceColumn() throws IOException {
		RecordSetSourceHandler handler = buildHandler();
		// A column present in the source schema participates in keyed matching.
		// call under test
		assertFalse(handler.isColumnExcludedFromMatching("id"));
		assertFalse(handler.isColumnExcludedFromMatching("name"));
	}

	/**
	 * Builds a handler for deletion-honoring tests. Uses the given baseline and
	 * latest versions and a ReconciledSchema with explicit JSON Schema column names.
	 * The CSV contains only a header row (no data) to keep the test minimal.
	 */
	private RecordSetSourceHandler buildHandlerForDeletion(long baselineVersion, long latestVersion,
			ReconciledSchema schema, String[] csvHeader) throws IOException {
		session.setSourceEntityVersionNumber(baselineVersion);
		recordSet.setVersionNumber(latestVersion);

		when(mockEntityManager.getEntity(mockUser, "syn1", RecordSet.class)).thenReturn(recordSet);
		when(mockFileHandleManager.getRawFileHandleUnchecked("fhLatest")).thenReturn(latestFileHandle);
		when(mockSchemaResolver.getReconciledSchema(eq("syn1"), eq(latestFileHandle), any()))
				.thenReturn(schema);
		when(mockCsvFileHandleProvider.getCsvReader(latestFileHandle, csvDescriptor)).thenReturn(mockLatestReader);
		when(mockLatestReader.readNext()).thenReturn(csvHeader, (String[]) null);

		when(mockEntityManager.getEntityForVersion(mockUser, "syn1", baselineVersion, RecordSet.class))
				.thenReturn(baselineRecordSet);
		when(mockFileHandleManager.getRawFileHandleUnchecked("fhBaseline")).thenReturn(baselineFileHandle);
		when(mockCsvFileHandleProvider.getCsvReader(baselineFileHandle, csvDescriptor)).thenReturn(mockBaselineReader);
		when(mockBaselineReader.readNext()).thenReturn(csvHeader, (String[]) null);

		when(mockFileProvider.createTempFile(any(), any())).thenReturn(tempFile);
		when(mockFileProvider.createFileOutputStream(tempFile)).thenReturn(new FileOutputStream(tempFile));

		return new RecordSetSourceHandler(mockUser, session, mockEntityManager, mockFileHandleManager,
				mockCsvFileHandleProvider, mockSchemaResolver, mockFileProvider,
				mockRecordSetExporter, mockGridRowValidator, mockNodeDao);
	}

	@Test
	public void testIsColumnDeletedByUserWhenFullySyncedAndNotSchemaProperty() throws IOException {
		// Source has [id, name, extra]. "extra" is NOT in the JSON schema and the grid is
		// fully synced (baseline == latest) → an absent-from-grid "extra" is a user deletion.
		ReconciledSchema schema = new ReconciledSchema(
				List.of(new ColumnModel().setName("id").setColumnType(ColumnType.INTEGER),
						new ColumnModel().setName("name").setColumnType(ColumnType.STRING).setMaximumSize(50L),
						new ColumnModel().setName("extra").setColumnType(ColumnType.STRING).setMaximumSize(50L)),
				List.of(),
				List.of("id", "name")); // JSON schema: id and name only, not extra

		RecordSetSourceHandler handler = buildHandlerForDeletion(5L, 5L, schema,
				new String[] { "id", "name", "extra" });

		// call under test — "extra" was deleted by the user and is not in JSON schema
		assertTrue(handler.isColumnDeletedByUser("extra"));
	}

	@Test
	public void testIsColumnDeletedByUserWithJsonSchemaProperty() throws IOException {
		// "key" IS a JSON schema property → always imported, never treated as a deletion.
		ReconciledSchema schema = new ReconciledSchema(
				List.of(new ColumnModel().setName("id").setColumnType(ColumnType.INTEGER),
						new ColumnModel().setName("name").setColumnType(ColumnType.STRING).setMaximumSize(50L),
						new ColumnModel().setName("key").setColumnType(ColumnType.STRING).setMaximumSize(50L)),
				List.of(),
				List.of("id", "name", "key")); // key IS in JSON schema

		RecordSetSourceHandler handler = buildHandlerForDeletion(5L, 5L, schema,
				new String[] { "id", "name", "key" });

		// call under test — key must be imported because it is a JSON schema property
		assertFalse(handler.isColumnDeletedByUser("key"));
	}

	@Test
	public void testIsColumnDeletedByUserWhenSourceIsNewerThanBaseline() throws IOException {
		// baseline (2) < latest (5): the source moved ahead, so deletion-honoring is
		// disabled and no absent column is treated as a user deletion.
		ReconciledSchema schema = new ReconciledSchema(
				List.of(new ColumnModel().setName("id").setColumnType(ColumnType.INTEGER),
						new ColumnModel().setName("name").setColumnType(ColumnType.STRING).setMaximumSize(50L),
						new ColumnModel().setName("extra").setColumnType(ColumnType.STRING).setMaximumSize(50L)),
				List.of(),
				List.of("id", "name")); // extra not in JSON schema

		RecordSetSourceHandler handler = buildHandlerForDeletion(2L, 5L, schema,
				new String[] { "id", "name", "extra" });

		// call under test — version gate disables deletion honoring
		assertFalse(handler.isColumnDeletedByUser("extra"));
	}

	@Test
	public void testGetBenefactorIds() throws IOException {
		when(mockNodeDao.getBenefactor(recordSet.getId())).thenReturn("syn9999");

		RecordSetSourceHandler handler = buildHandler();
		// call under test
		Set<Long> ids = handler.getBenefactorIds();

		assertEquals(1, ids.size());
		assertTrue(ids.contains(9999L));
	}

}

