package org.sagebionetworks.repo.manager.table;

import static org.sagebionetworks.repo.manager.grid.create.RecordSetCreateGridHandler.DEFAULT_RECORD_SET_CSV_DESCRIPTOR;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.file.CsvFileHandleProvider;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.table.CSVToRowIterator;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.semaphore.LockContext;
import org.sagebionetworks.repo.model.semaphore.LockContext.ContextType;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.table.SparseRowDto;
import org.sagebionetworks.table.cluster.description.IndexDescription;
import org.sagebionetworks.table.cluster.description.RecordSetIndexDescription;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.model.SparseChangeSet;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import au.com.bytecode.opencsv.CSVReader;

@Service
public class RecordSetIndexManagerImpl implements RecordSetIndexManager {

	private static final int MAX_BYTES_PER_BATCH = 1024 * 1024 * 2;

	private final TableManagerSupport tableManagerSupport;
	private final TableIndexConnectionFactory connectionFactory;
	private final ColumnModelManager columnModelManager;
	private final EntityManager entityManager;
	private final UserManager userManager;
	private final FileHandleManager fileHandleManager;
	private final CsvFileHandleProvider csvFileHandleProvider;
	private final NodeDAO nodeDao;
	private final Logger log;

	@Autowired
	public RecordSetIndexManagerImpl(TableManagerSupport tableManagerSupport,
			TableIndexConnectionFactory connectionFactory, ColumnModelManager columnModelManager,
			EntityManager entityManager, UserManager userManager, FileHandleManager fileHandleManager,
			CsvFileHandleProvider csvFileHandleProvider, NodeDAO nodeDao, LoggerProvider loggerProvider) {
		this.tableManagerSupport = tableManagerSupport;
		this.connectionFactory = connectionFactory;
		this.columnModelManager = columnModelManager;
		this.entityManager = entityManager;
		this.userManager = userManager;
		this.fileHandleManager = fileHandleManager;
		this.csvFileHandleProvider = csvFileHandleProvider;
		this.nodeDao = nodeDao;
		this.log = loggerProvider.getLogger(RecordSetIndexManagerImpl.class.getName());
	}

	@Override
	public void createOrUpdateRecordSetIndex(IdAndVersion idAndVersion, ProgressCallback progressCallback)
			throws Exception {
		ValidateArgument.required(idAndVersion, "idAndVersion");
		ValidateArgument.required(progressCallback, "progressCallback");

		// The exclusive lock is always taken at the entity level so concurrent
		// version-targeted rebuilds for the same RecordSet are serialized.
		IdAndVersion entityKey = IdAndVersion.newBuilder().setId(idAndVersion.getId()).build();
		tableManagerSupport.tryRunWithTableExclusiveLock(progressCallback,
				new LockContext(ContextType.BuildTableIndex, entityKey), entityKey,
				(ProgressCallback inner) -> {
					createOrUpdateHoldingLock(idAndVersion, entityKey);
					return null;
				});
	}

	void createOrUpdateHoldingLock(IdAndVersion idAndVersion, IdAndVersion entityKey) {
		long currentRevision = nodeDao.getCurrentRevisionNumber(entityKey.getId().toString());
		// A versionless message OR a versioned message targeting the current
		// revision rebuilds both T{id} (the alias for unversioned queries)
		// and T{id}_{targetVersion} (the per-version snapshot). A versioned
		// message for any other version only rebuilds that version's
		// snapshot
		long targetVersion = idAndVersion.getVersion().orElse(currentRevision);
		boolean bindDefaultVersion = targetVersion == currentRevision;
		IdAndVersion versionedKey = IdAndVersion.newBuilder()
				.setId(entityKey.getId())
				.setVersion(targetVersion)
				.build();
		IdAndVersion statusKey = bindDefaultVersion ? entityKey : versionedKey;

		if (!tableManagerSupport.isIndexWorkRequired(statusKey)) {
			return;
		}
		final String token = tableManagerSupport.startTableProcessing(statusKey);

		try {
			IndexDescription versionedDescription = new RecordSetIndexDescription(versionedKey, targetVersion);
			IndexDescription entityDescription = null;
			if (bindDefaultVersion) {
				entityDescription = new RecordSetIndexDescription(entityKey, targetVersion);
			}
			RecordSet recordSet = entityManager.getEntityForVersion(getAdminUser(),
					entityKey.getId().toString(), targetVersion, RecordSet.class);
			FileHandle dataFileHandle = fileHandleManager.getRawFileHandleUnchecked(recordSet.getDataFileHandleId());
			// RecordSet.csvDescriptor is optional, so fall back to the same default CsvDescriptor as the grid create flow
			CsvTableDescriptor csvDescriptor = Optional.ofNullable(recordSet.getCsvDescriptor()).orElse(DEFAULT_RECORD_SET_CSV_DESCRIPTOR);
			// The inferred column schema was computed and persisted at update-time by the RecordSetMetadataProvider
			List<ColumnModel> persistedColumns = tableManagerSupport.getTableSchema(versionedKey);
			if (persistedColumns.isEmpty()) {
				throw new IllegalArgumentException("RecordSet schema has not been bound for " + versionedKey + ".");
			}

			TableIndexManager indexManager = connectionFactory.connectToTableIndex(entityKey);

			// Reset the destination index table(s), then populate them in a single CSV pass.
			indexManager.resetTableIndex(versionedDescription, persistedColumns, false);
			List<IndexDescription> destinations;
			if (bindDefaultVersion) {
				indexManager.resetTableIndex(entityDescription, persistedColumns, false);
				destinations = Arrays.asList(entityDescription, versionedDescription);
			} else {
				destinations = Collections.singletonList(versionedDescription);
			}
			long rowCount = loadRows(indexManager, destinations, persistedColumns, dataFileHandle, csvDescriptor,
					targetVersion);
			indexManager.buildTableIndexIndices(versionedDescription, persistedColumns);
			indexManager.setIndexVersion(versionedKey, targetVersion);
			if (bindDefaultVersion) {
				indexManager.buildTableIndexIndices(entityDescription, persistedColumns);
				indexManager.setIndexVersion(entityKey, targetVersion);
			}

			// Use the RecordSet revision's etag as the table change etag, since each versioned index build corresponds
			// to a single RecordSet revision.
			String tableChangeEtag = recordSet.getEtag();

			if (bindDefaultVersion) {
				// Per-version TableStatus first so "syn123.{v}" queries find AVAILABLE without re-triggering.
				String versionedToken = tableManagerSupport.startTableProcessing(versionedKey);
				tableManagerSupport.attemptToSetTableStatusToAvailable(versionedKey, versionedToken, tableChangeEtag);
				// Entity-level TableStatus last — what unversioned queries read.
				tableManagerSupport.attemptToSetTableStatusToAvailable(entityKey, token, tableChangeEtag);
			} else {
				// Older/out-of-order rebuild — only the per-version status was processed by this build.
				tableManagerSupport.attemptToSetTableStatusToAvailable(versionedKey, token, tableChangeEtag);
			}
			log.info("Built RecordSet index {} (rev {}) with {} rows", versionedKey, targetVersion, rowCount);
		} catch (Exception e) {
			// Persist the failure on whichever status row this build was processing.
			tableManagerSupport.attemptToSetTableStatusToFailed(statusKey, e);
			throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
		}
	}

	UserInfo getAdminUser() {
		return userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
	}

	long loadRows(TableIndexManager indexManager, List<IndexDescription> indexDescriptions, List<ColumnModel> schema,
			FileHandle dataFileHandle, CsvTableDescriptor csvDescriptor, long changeSetVersion) throws IOException {
		long rowCount = 0;
		// CSV rows have no inherent rowId, so just start at 1.
		long nextRowId = 1L;
		try (CSVReader csvReader = csvFileHandleProvider.getCsvReader(dataFileHandle, csvDescriptor)) {
			CSVToRowIterator iterator = new CSVToRowIterator(schema, csvReader, csvDescriptor.getIsFirstLineHeader(),
					null);
			List<SparseRowDto> batch = new LinkedList<>();
			int batchBytes = 0;
			while (iterator.hasNext()) {
				SparseRowDto row = iterator.next();
				row.setRowId(nextRowId++);
				row.setVersionNumber(changeSetVersion);
				batch.add(row);
				rowCount++;
				batchBytes += TableModelUtils.calculateActualRowSize(row);
				if (batchBytes >= MAX_BYTES_PER_BATCH) {
					applyBatch(indexManager, indexDescriptions, schema, batch, changeSetVersion);
					batch.clear();
					batchBytes = 0;
				}
			}
			if (!batch.isEmpty()) {
				applyBatch(indexManager, indexDescriptions, schema, batch, changeSetVersion);
			}
		}
		return rowCount;
	}

	private void applyBatch(TableIndexManager indexManager, List<IndexDescription> indexDescriptions,
			List<ColumnModel> schema, List<SparseRowDto> batch, long changeSetVersion) {
		for (IndexDescription indexDescription : indexDescriptions) {
			SparseChangeSet delta = new SparseChangeSet(indexDescription.getIdAndVersion().getId().toString(), schema,
					batch, null);
			indexManager.applyChangeSetToIndex(indexDescription.getIdAndVersion(), delta, changeSetVersion);
		}
	}

	@Override
	public void deleteRecordSetIndex(IdAndVersion idAndVersion) {
		ValidateArgument.required(idAndVersion, "idAndVersion");
		// Drop the entity-level stub (T{id} + T{id}_STATUS) and unbind all columns. Per-version
		// snapshots T{id}_{v} are left as orphans — they're unreachable now that the entity is
		// gone (matches how TableEntity treats versioned snapshot index tables after delete).
		IdAndVersion entityKey = IdAndVersion.newBuilder().setId(idAndVersion.getId()).build();
		try {
			TableIndexManager indexManager = connectionFactory.connectToTableIndex(entityKey);
			indexManager.deleteTableIndex(entityKey);
		} catch (TableIndexConnectionUnavailableException e) {
			log.warn("Index unavailable while deleting RecordSet index for " + idAndVersion, e);
		}
		columnModelManager.unbindAllColumnsAndOwnerFromObject(idAndVersion.getId().toString());
	}

}
