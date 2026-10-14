package org.sagebionetworks.repo.manager.grid.synch.handler;

import static org.sagebionetworks.repo.manager.grid.create.RecordSetCreateGridHandler.DEFAULT_RECORD_SET_CSV_DESCRIPTOR;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.file.CsvFileHandleProvider;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.export.GridRecordSetExporter;
import org.sagebionetworks.repo.manager.grid.internal.replica.validation.GridRowValidator;
import org.sagebionetworks.repo.manager.grid.row.translator.ColumnTypeToConType;
import org.sagebionetworks.repo.manager.grid.row.translator.Translator;
import org.sagebionetworks.repo.manager.grid.synch.io.DiskPointer;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItem;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItemReader;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItemWriter;
import org.sagebionetworks.repo.manager.grid.synch.row.CellCopyItem;
import org.sagebionetworks.repo.manager.grid.synch.row.RowCopyItem;
import org.sagebionetworks.repo.manager.table.RecordSetSchemaResolver;
import org.sagebionetworks.repo.manager.table.RecordSetSchemaResolver.ReconciledSchema;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.SyncType;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.util.FileProvider;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import au.com.bytecode.opencsv.CSVReader;

/**
 * {@link SourceHandler} for a grid session sourced from a RecordSet. The source
 * rows are the RecordSet's latest revision CSV, keyed by the RecordSet's current
 * {@code upsertKey}.
 * <p>
 * Only the grid is mutated in real-time during a merge. The RecordSet itself is
 * written only by the separate push (export) step, so all mutating operations
 * here are no-ops.
 * <p>
 * Deletion detection uses the synced-revision baseline: the key-set of the
 * revision recorded on the session ({@code sourceEntityVersionNumber}). A row
 * present in the latest revision but absent from the grid is treated as a user
 * deletion iff its key existed in that baseline (see
 * {@link #wasInSyncedBaseline(String)}). A null baseline (e.g. legacy sessions)
 * yields an empty set, so no deletions are inferred on the first sync.
 */
@Component
@Scope("prototype")
public class RecordSetSourceHandler implements SourceHandler {

	private final UserInfo user;
	private final GridSession session;
	private final EntityManager entityManager;
	private final FileHandleManager fileHandleManager;
	private final CsvFileHandleProvider csvFileHandleProvider;
	private final RecordSetSchemaResolver schemaResolver;
	private final FileProvider fileProvider;
	private final GridRecordSetExporter recordSetExporter;
	private final GridRowValidator gridRowValidator;
	private final NodeDAO nodeDao;

	private List<ColumnModel> schema;
	private List<String> upsertKey;
	private Set<String> requiredColumnNames;
	private Set<String> jsonSchemaColumnNames;
	private Map<String, Translator> translators;
	private CsvTableDescriptor csvDescriptor;
	private List<DiskPointer> diskPointers;
	private File tempFile;
	// Full-row content hash keyed by upsertKey, for complete-key rows only, used to
	// detect rows changed between the synced baseline and the latest revision.
	private Map<String, byte[]> latestHashByKey;
	private Map<String, byte[]> baselineHashByKey;
	private Long sourceVersion;
	private String schemaId;
	private RecordSet recordSet;
	private Set<String> keyColumns;
	private final Set<String> seenRowKeys = new HashSet<>();

	public RecordSetSourceHandler(UserInfo user, GridSession session, EntityManager entityManager,
			FileHandleManager fileHandleManager, CsvFileHandleProvider csvFileHandleProvider,
			RecordSetSchemaResolver schemaResolver, FileProvider fileProvider,
			GridRecordSetExporter recordSetExporter, GridRowValidator gridRowValidator, NodeDAO nodeDao) throws IOException {
		this.user = user;
		this.session = session;
		this.entityManager = entityManager;
		this.fileHandleManager = fileHandleManager;
		this.csvFileHandleProvider = csvFileHandleProvider;
		this.schemaResolver = schemaResolver;
		this.fileProvider = fileProvider;
		this.recordSetExporter = recordSetExporter;
		this.gridRowValidator = gridRowValidator;
		this.nodeDao = nodeDao;
		try {
			initialize();
		} catch (RuntimeException | IOException e) {
			// initialize() creates the temp file before it can fail (e.g. while loading
			// the baseline revision). Since construction never completes, this handler
			// is never returned to a try-with-resources, so close() would otherwise
			// never run and the temp file would leak on every retry of a failing sync.
			close();
			throw e;
		}
	}

	void initialize() throws IOException {
		RecordSet recordSet = entityManager.getEntity(user, session.getSourceEntityId(), RecordSet.class);
		this.recordSet = recordSet;
		this.upsertKey = recordSet.getUpsertKey();
		this.keyColumns = new HashSet<>(recordSet.getUpsertKey());
		this.sourceVersion = recordSet.getVersionNumber();
		this.schemaId = entityManager.findBoundSchema(recordSet.getId())
				.map(binding -> binding.getJsonSchemaVersionInfo().get$id()).orElse(null);
		this.csvDescriptor = Optional.ofNullable(recordSet.getCsvDescriptor()).orElse(DEFAULT_RECORD_SET_CSV_DESCRIPTOR);

		FileHandle latestFileHandle = fileHandleManager.getRawFileHandleUnchecked(recordSet.getDataFileHandleId());

		ReconciledSchema reconciled = schemaResolver.getReconciledSchema(recordSet.getId(), latestFileHandle,
				csvDescriptor);
		this.schema = reconciled.getSchema();
		this.requiredColumnNames = reconciled.getRequiredColumnIndices().stream().map(i -> schema.get(i).getName())
				.collect(Collectors.toSet());
		this.jsonSchemaColumnNames = new HashSet<>(reconciled.getJsonSchemaColumnNames());
		this.translators = schema.stream().collect(Collectors.toMap(ColumnModel::getName,
				cm -> ColumnTypeToConType.lookUpType(cm.getColumnType()).getTranslator()));

		/*
		 * Stream the latest revision CSV to a disk-based index, keyed by upsertKey.
		 *
		 * Duplicate complete-key rows are all preserved. The first occurrence is
		 * matched to the copy during Phase 1; any additional occurrences survive as
		 * remaining rows and are added to the copy during Phase 2.
		 *
		 * Keyless source rows (incomplete upsertKey) get a synthetic UUID identity and
		 * are always-added as new rows when this revision is newer than the grid's synced baseline.
		 * When the grid is already synchronized to this revision (baseline == latest), omitting these
		 * rows prevents a redundant sync from importing the same keyless rows again.
		 */
		boolean importKeylessRows = isNewerThanBaseline(session.getSourceEntityVersionNumber(), sourceVersion);
		this.tempFile = fileProvider.createTempFile("RecordSetSource-" + recordSet.getId(), ".bin");
		this.diskPointers = new ArrayList<>();
		this.latestHashByKey = new HashMap<>();
		try (CSVReader reader = csvFileHandleProvider.getCsvReader(latestFileHandle, csvDescriptor);
				RowSourceItemWriter writer = new RowSourceItemWriter(fileProvider.createFileOutputStream(tempFile))) {
			Map<String, Integer> headerIndex = readHeader(reader);
			String[] row;
			while ((row = reader.readNext()) != null) {
				RowSourceItem synchRow = createSynchRow(row, headerIndex);
				if (!hasCompleteUpsertKey(synchRow.getData())) {
					if (!importKeylessRows) {
						continue;
					}
				} else {
					// Record the hash of the first complete-key occurrence for change detection
					// (putIfAbsent ensures duplicates do not overwrite the first occurrence's hash,
					// which is the row matched during Phase 1 of the sync).
					latestHashByKey.putIfAbsent(synchRow.getKey(), synchRow.getHash());
				}
				diskPointers.add(writer.nextRow(synchRow));
			}
		}

		this.baselineHashByKey = loadBaselineHashes(recordSet.getId(), session.getSourceEntityVersionNumber());
	}

	/**
	 * @return true when the latest source revision is newer than the grid's synced
	 *         baseline (or there is no baseline yet, i.e. the first sync). When
	 *         false, the grid already reflects this revision and keyless rows must
	 *         not be re-imported.
	 */
	static boolean isNewerThanBaseline(Long baselineVersion, Long latestVersion) {
		if (baselineVersion == null || latestVersion == null) {
			return true;
		}
		return latestVersion > baselineVersion;
	}

	/**
	 * Read the CSV header row and return a map of column name to its zero-based
	 * index in the CSV. If the descriptor indicates there is no header, returns an
	 * empty map (columns are then matched by schema order only).
	 */
	Map<String, Integer> readHeader(CSVReader reader) throws IOException {
		Map<String, Integer> headerIndex = new HashMap<>();
		if (Boolean.FALSE.equals(csvDescriptor.getIsFirstLineHeader())) {
			return headerIndex;
		}
		String[] header = reader.readNext();
		if (header != null) {
			for (int i = 0; i < header.length; i++) {
				headerIndex.put(header[i], i);
			}
		}
		return headerIndex;
	}

	/**
	 * Build a source row from a CSV data row. Each schema column's value is read
	 * from the CSV by matching column name (via the header index); schema columns
	 * absent from the CSV are translated as `undefined`.
	 *
	 * <p>
	 * A row with a complete upsertKey is keyed deterministically via
	 * {@link UpsertKeyEncoder}, so it matches the grid copy row for the same logical
	 * row. A row with an <em>incomplete</em> upsertKey (any key column null/blank)
	 * cannot be matched, so it is given a fresh random UUID key.
	 */
	RowSourceItem createSynchRow(String[] csvRow, Map<String, Integer> headerIndex) {
		TreeMap<String, ConValue> data = new TreeMap<>();
		for (ColumnModel column : schema) {
			String name = column.getName();
			String raw = rawValue(csvRow, headerIndex, name);
			if (keyColumns.contains(name) && isBlank(raw)) {
				// Incomplete key cell: keep the row, don't attempt to translate (and
				// possibly fail) — mark it undefined so it reads as a keyless row.
				data.put(name, new ConValue(ConType.UNDEFINED, null));
			} else {
				// Column types come from the latest revision's schema, but this method also
				// re-reads the synced baseline revision — an independent, immutable CSV that
				// may hold a value that schema cannot represent (e.g. alphanumeric where the
				// latest revision inferred an integer). Translate leniently so such a cell is
				// carried through as text instead of failing the whole sync.
				data.put(name, translators.get(name).translateLeniently(raw, requiredColumnNames.contains(name)));
			}
		}
		// The UUID ensures keyless rows are never matched to a copy row AND are always
		// imported as new rows.
		String key = hasCompleteUpsertKey(data) ? UpsertKeyEncoder.encodeFromData(data, upsertKey)
				: UUID.randomUUID().toString();
		return new RowSourceItem(data, key);
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	/**
	 * Load the full-row content hash of each complete-key row in the synced baseline
	 * revision, keyed by the row's upsertKey (computed with the current key columns
	 * and translators, so keys and hashes are comparable to the latest revision).
	 * A null version (legacy sessions) yields an empty map. Keyless baseline rows
	 * are ignored — they have synthetic UUID keys and play no part in deletion
	 * detection. When the baseline contains duplicate keys, only the first
	 * occurrence's hash is stored, consistent with how the latest revision is
	 * tracked.
	 */
	Map<String, byte[]> loadBaselineHashes(String recordSetId, Long baselineVersion) throws IOException {
		Map<String, byte[]> hashes = new HashMap<>();
		if (baselineVersion == null) {
			return hashes;
		}
		RecordSet baseline = entityManager.getEntityForVersion(user, recordSetId, baselineVersion, RecordSet.class);
		FileHandle baselineFileHandle = fileHandleManager.getRawFileHandleUnchecked(baseline.getDataFileHandleId());
		CsvTableDescriptor baselineDescriptor = Optional.ofNullable(baseline.getCsvDescriptor()).orElse(DEFAULT_RECORD_SET_CSV_DESCRIPTOR);
		try (CSVReader reader = csvFileHandleProvider.getCsvReader(baselineFileHandle, baselineDescriptor)) {
			Map<String, Integer> headerIndex = readHeader(reader);
			String[] row;
			while ((row = reader.readNext()) != null) {
				RowSourceItem baselineRow = createSynchRow(row, headerIndex);
				if (hasCompleteUpsertKey(baselineRow.getData())) {
					hashes.putIfAbsent(baselineRow.getKey(), baselineRow.getHash());
				}
			}
		}
		return hashes;
	}

	private static String rawValue(String[] csvRow, Map<String, Integer> headerIndex, String columnName) {
		Integer index = headerIndex.get(columnName);
		if (index == null || index >= csvRow.length) {
			return null;
		}
		return csvRow[index];
	}

	@Override
	public RowSourceItemReader getSourceRowReader() throws IOException {
		return new RowSourceItemReader(diskPointers, fileProvider.createRandomAccessFile(tempFile, "r"));
	}

	@Override
	public SourceWriter createSourceWriter(SyncType syncType) {
		return new RecordSetSourceWriter(user, recordSetExporter, gridRowValidator, recordSet, csvDescriptor,
				schemaId, syncType);
	}

	@Override
	public String getRowKey(RowCopyItem rowView) {
		Map<String, ConValue> cellsByName = rowView.getCells().stream()
				.collect(Collectors.toMap(CellCopyItem::getName, CellCopyItem::getValue, (a, b) -> b));
		return UpsertKeyEncoder.encodeFromData(cellsByName, upsertKey);
	}

	@Override
	public List<String> getCurrentSourceSchema() {
		return schema.stream().map(ColumnModel::getName).collect(Collectors.toList());
	}

	@Override
	public boolean wasInSyncedBaseline(String key) {
		return baselineHashByKey.containsKey(key);
	}

	/**
	 * A complete-key row changed since the baseline when it is present in both the
	 * baseline and latest revisions and their full-row hashes differ. Note: because
	 * this uses a full-row hash, a schema change between the two revisions (added or
	 * removed columns) also counts as a change, so deleted rows would be re-imported
	 * on a schema change.
	 */
	@Override
	public boolean changedSinceBaseline(String key) {
		byte[] baselineHash = baselineHashByKey.get(key);
		byte[] latestHash = latestHashByKey.get(key);
		return baselineHash != null && latestHash != null && !Arrays.equals(baselineHash, latestHash);
	}

	/**
	 * A grid row whose {@code upsertKey} is incomplete cannot be matched to a source
	 * row. It is excluded from the keyed merge, but the row is still included in the grid
	 * (as well as the CSV if this is a PULL_PUSH).
	 */
	@Override
	public boolean isUnmatchableCopyRow(RowCopyItem row, String key) {
		Map<String, ConValue> cellsByName = row.getCells().stream()
				.collect(Collectors.toMap(CellCopyItem::getName, CellCopyItem::getValue, (a, b) -> b));
		if (!hasCompleteUpsertKey(cellsByName)) {
			return true;
		}
		return !seenRowKeys.add(key);
	}

	@Override
	public Optional<Long> getSourceVersion() {
		return Optional.ofNullable(sourceVersion);
	}

	@Override
	public Optional<String> getSourceSchema$Id() {
		return Optional.ofNullable(schemaId);
	}

	/**
	 * @return true iff every {@code upsertKey} column has a non-null/defined value
	 *         in the given row data. A row without a complete key cannot be matched
	 *         in the merge process, so it is always copied to the copy from the source
	 *         and vice versa.
	 */
	boolean hasCompleteUpsertKey(Map<String, ConValue> cellsByName) {
		for (String keyColumn : upsertKey) {
			if (isNullValue(cellsByName.get(keyColumn))) {
				return false;
			}
		}
		return true;
	}

	private static boolean isNullValue(ConValue value) {
		return value == null || value.getValue() == null
				|| ConType.NULL.equals(value.getType())
				|| ConType.UNDEFINED.equals(value.getType());
	}

	@Override
	public Set<Long> getBenefactorIds() {
		return Collections.singleton(KeyFactory.stringToKey(nodeDao.getBenefactor(recordSet.getId())));
	}

	/**
	 * RecordSet sources support both {@link SyncType#PULL} and
	 * {@link SyncType#PULL_PUSH}. A {@code null} requested type defaults to
	 * {@link SyncType#PULL_PUSH}.
	 */
	@Override
	public void validateSyncType(SyncType syncType) {
		ValidateArgument.required(syncType, "syncType");
		switch (syncType) {
            case PULL -> {
				// allowed
			}
            case PULL_PUSH -> {
				// allowed
            }
        }
	}

	/**
	 * A grid column absent from the current source schema is preserved untouched
	 * during Phase 1 — never dropped from the grid and never pushed as a source
	 * schema change. This is how RecordSet keeps user-added (and prior-sync)
	 * columns across a synchronization.
	 */
	@Override
	public boolean isColumnExcludedFromMatching(String columnName) {
		return !getCurrentSourceSchema().contains(columnName);
	}

	/**
	 * A source column absent from the grid is treated as a user deletion when the
	 * grid is fully synced to the latest source version ({@code baseline == latest})
	 * and the column is not a JSON Schema property. This prevents the engine from
	 * silently re-importing a column the user intentionally removed. JSON Schema
	 * properties are always imported (new schema properties are always added), and
	 * when the source is newer than the baseline, deletion-honoring is disabled so
	 * the user can first pull the latest data, transform it, and re-delete columns
	 * before a subsequent PULL_PUSH.
	 */
	@Override
	public boolean isColumnDeletedByUser(String columnName) {
		boolean honorDeletions = !isNewerThanBaseline(session.getSourceEntityVersionNumber(), sourceVersion);
		return honorDeletions && !jsonSchemaColumnNames.contains(columnName);
	}

	@Override
	public void close() {
		if (tempFile != null) {
			tempFile.delete();
		}
	}
}
