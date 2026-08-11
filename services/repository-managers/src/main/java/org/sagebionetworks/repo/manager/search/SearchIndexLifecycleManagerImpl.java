package org.sagebionetworks.repo.manager.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.indices.IndexSettingsAnalysis;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.dbo.dao.table.DefiningSqlDependencyDao;
import org.sagebionetworks.repo.model.dbo.search.ColumnAnalyzerOverrideDao;
import org.sagebionetworks.repo.model.dbo.search.SynonymSetDao;
import org.sagebionetworks.repo.model.dbo.search.TextAnalyzerDao;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverride;
import org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverrideEntry;
import org.sagebionetworks.repo.model.search.table.SearchConfiguration;
import org.sagebionetworks.repo.model.search.table.SearchIndex;
import org.sagebionetworks.repo.model.search.table.SearchIndexState;
import org.sagebionetworks.repo.model.search.table.SearchIndexStatus;
import org.sagebionetworks.repo.model.search.table.SynonymSet;
import org.sagebionetworks.repo.model.search.table.TextAnalyzer;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.table.cluster.CachedQueryRequest;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.QueryTranslator;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.TranslatedQuery;
import org.sagebionetworks.table.cluster.description.BenefactorDescription;
import org.sagebionetworks.table.cluster.description.IndexDescription;
import org.sagebionetworks.table.cluster.search.SearchIndexStatusDao;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.CurrentUserFunction;
import org.sagebionetworks.table.query.model.DerivedColumn;
import org.sagebionetworks.table.query.model.SelectList;
import org.sagebionetworks.table.query.model.SqlContext;
import org.sagebionetworks.table.query.util.SqlElementUtils;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.sagebionetworks.workers.util.semaphore.WriteLock;
import org.sagebionetworks.workers.util.semaphore.WriteLockRequest;
import org.sagebionetworks.workers.util.semaphore.WriteReadSemaphore;
import org.springframework.stereotype.Service;

@Service
public class SearchIndexLifecycleManagerImpl implements SearchIndexLifecycleManager {

	private static final Logger LOG = LogManager.getLogger(SearchIndexLifecycleManagerImpl.class);
	private static final String INDEX_PREFIX = "search-index-";
	private static final String OBJECT_TYPE = ObjectType.SEARCH_INDEX.name();
	// Blue-green physical slots. Queries target the alias getAliasName(entityId); each build streams
	// into whichever of these two deterministic physical indices the alias is NOT currently serving,
	// then atomically repoints the alias and deletes the just-demoted slot. Two fixed names bound the
	// footprint to two indices per entity; the pre-build idle-slot delete is a defensive backstop for
	// the rare case where the post-swap delete failed, not the primary cleanup mechanism.
	static final String SLOT_A = "a";
	static final String SLOT_B = "b";
	private static final String LOCK_KEY_PREFIX = "search-index-build:";
	private static final int MAX_ERROR_MESSAGE_LENGTH = 3000;
	private static final int BATCH_SIZE = 1000;
	private static final long MAX_ROWS = 500_000L;
	private static final ObjectMapper SEARCH_DOC_MAPPER = new ObjectMapper();

	// Build-time dynamic shard sizing for the managed OpenSearch domain. The source table's
	// on-disk size is a conservative upper bound for the derived index (the defining SQL may
	// select a subset of columns), so this never under-shards.
	// Target shard size sits mid the AWS-recommended 10-30 GiB band, safely under the 50 GiB ceiling.
	static final long TARGET_SHARD_BYTES = 25L * 1024 * 1024 * 1024;
	// 6 shards x 50 GiB effective = ~300 GiB capacity, well above the
	// ~146 GiB max MySQL source table. Guards a runaway size reading.
	static final int MAX_SHARDS = 6;

	/**
	 * Compute the number of primary shards for an index from the source table's data size.
	 * Null or zero size maps to a single shard; otherwise the byte count is bucketed into
	 * {@link #TARGET_SHARD_BYTES} shards, clamped to {@code [1, MAX_SHARDS]}.
	 */
	static int computeShardCount(Long dataSizeBytes) {
		if (dataSizeBytes == null || dataSizeBytes <= 0) {
			return 1;
		}
		long shards = ((dataSizeBytes + TARGET_SHARD_BYTES) - 1) / TARGET_SHARD_BYTES;
		return (int) Math.max(1, Math.min(shards, MAX_SHARDS));
	}

    /**
	 * Convert a raw String row value (as delivered by the table query stream) into the Java
	 * type expected by the column's OpenSearch mapping. List columns are parsed as JSON
	 * arrays. Bare-string columns (text / keyword / link) pass through as {@code String};
	 * everything else is parsed via Jackson's untyped {@code readValue}, which yields the
	 * natural Java equivalent of the JSON token — {@code Integer}/{@code Long},
	 * {@code Double}, {@code Boolean}, {@code List}, or {@code Map} — each of which the
	 * OpenSearch client serializes as the right JSON type. Returning the raw String for
	 * non-string columns causes AOSS to reject the doc.
	 */
	static Object convertForDocument(String value, ColumnType type) {
		if (value == null) {
			return null;
		}
		// List types must be checked before the bare-string short-circuit: STRING_LIST maps
		// to TEXT and ENTITYID_LIST/USERID_LIST map to KEYWORD, so isTextType/isKeywordType
		// would otherwise pass the raw JSON-array string straight through.
		if (!ColumnTypeToOpenSearchMapping.isListType(type)
				&& (ColumnTypeToOpenSearchMapping.isTextType(type)
				|| ColumnTypeToOpenSearchMapping.isKeywordType(type)
				|| ColumnTypeToOpenSearchMapping.isLinkType(type))) {
			return value;
		}
		try {
			return SEARCH_DOC_MAPPER.readValue(value, Object.class);
		} catch (IOException e) {
			throw new IllegalArgumentException(
					"Failed to convert column value for type " + type + ": " + value, e);
		}
	}

	private final ConnectionFactory connectionFactory;
	private final OpenSearchManager openSearchManager;
	private final SearchConfigurationResolver searchConfigurationResolver;
	private final EntityManager entityManager;
	private final SynonymSetDao synonymSetDao;
	private final ColumnAnalyzerOverrideDao columnAnalyzerOverrideDao;
	private final TextAnalyzerDao textAnalyzerDao;
	private final TableManagerSupport tableManagerSupport;
	private final ColumnModelManager columnModelManager;
	private final WriteReadSemaphore writeReadSemaphore;
	private final StackConfiguration stackConfiguration;
	private final DefiningSqlDependencyDao definingSqlDependencyDao;

	public SearchIndexLifecycleManagerImpl(ConnectionFactory connectionFactory,
			OpenSearchManager openSearchManager,
			SearchConfigurationResolver searchConfigurationResolver,
			EntityManager entityManager,
			SynonymSetDao synonymSetDao, ColumnAnalyzerOverrideDao columnAnalyzerOverrideDao,
			TextAnalyzerDao textAnalyzerDao,
			TableManagerSupport tableManagerSupport,
			ColumnModelManager columnModelManager,
			WriteReadSemaphore writeReadSemaphore,
			StackConfiguration stackConfiguration,
			DefiningSqlDependencyDao definingSqlDependencyDao) {
		this.connectionFactory = connectionFactory;
		this.openSearchManager = openSearchManager;
		this.searchConfigurationResolver = searchConfigurationResolver;
		this.entityManager = entityManager;
		this.synonymSetDao = synonymSetDao;
		this.columnAnalyzerOverrideDao = columnAnalyzerOverrideDao;
		this.textAnalyzerDao = textAnalyzerDao;
		this.tableManagerSupport = tableManagerSupport;
		this.columnModelManager = columnModelManager;
		this.writeReadSemaphore = writeReadSemaphore;
		this.stackConfiguration = stackConfiguration;
		this.definingSqlDependencyDao = definingSqlDependencyDao;
	}

	@Override
	@WriteTransaction
	public List<String> registerSchema(IdAndVersion searchIndexId, String definingSql) {
		ValidateArgument.required(searchIndexId, "searchIndexId");
		ValidateArgument.requiredNotBlank(definingSql, "definingSql");
		IdAndVersion sourceId = TableModelUtils.getSourceTableIds(definingSql).get(0);
		IndexDescription indexDescription = tableManagerSupport.getIndexDescription(sourceId);
		// A virtual table is a query rewrite, not a materialized index: it has no status row and
		// never fires a TABLE_STATUS_EVENT. A SearchIndex registered against one would build once
		// but never receive a source-availability event to rebuild on, drifting silently stale.
		// The dependency graph is one level deep, so the underlying table's events would fan out
		// to that table's own dependents, never to this SearchIndex. Forbid it at registration.
		if (TableType.virtualtable.equals(indexDescription.getTableType())) {
			throw new IllegalArgumentException(
					"The defining SQL of a search index cannot reference a virtual table.");
		}
		// SqlContext.query: TableIndexDescription rejects `build`; only Views/MVs accept it.
		QueryTranslator sqlQuery = QueryTranslator.builder()
				.sql(definingSql)
				.schemaProvider(tableManagerSupport)
				.sqlContext(SqlContext.query)
				.indexDescription(indexDescription)
				.build();
		// An aggregating defining SQL collapses source rows that may span different
		// benefactors into a single output row, for which there is no correct per-row
		// benefactor. When the source has benefactors, reject it up front (mirrors the
		// materialized-view guard) rather than building an index whose row-level access
		// filter would silently exclude every aggregated document.
		if (sqlQuery.isAggregatedResult() && !indexDescription.getBenefactors().isEmpty()) {
			throw new IllegalArgumentException(
					"The defining SQL of a search index over an access-controlled source cannot include a group by clause.");
		}
		List<String> schemaIds = sqlQuery.getSchemaOfSelect().stream()
				.map(c -> columnModelManager.createColumnModel(c).getId())
				.collect(Collectors.toList());
		columnModelManager.bindColumnsToVersionOfObject(schemaIds, searchIndexId);
		// Record the source -> SearchIndex edge so a source table/view that becomes AVAILABLE can
		// reverse-look-up which SearchIndex(es) depend on it and enqueue their rebuild.
		definingSqlDependencyDao.setSourceTable(searchIndexId, OBJECT_TYPE, sourceId);
		return schemaIds;
	}

	@Override
	public void handleCreate(ProgressCallback progressCallback, String entityId) throws Exception {
		ValidateArgument.required(entityId, "entityId");
		try (WriteLock lock = writeReadSemaphore.getWriteLock(
				new WriteLockRequest(progressCallback, "SearchIndexLifecycleManager.buildIndex", LOCK_KEY_PREFIX + entityId))) {
			buildIndex(progressCallback, entityId);
		} catch (LockUnavilableException e) {
			throw new RecoverableMessageException(
					"Search index " + entityId + " is already being built by another worker", e);
		}
	}

	@Override
	public void handleUpdate(ProgressCallback progressCallback, String entityId) throws Exception {
		ValidateArgument.required(entityId, "entityId");
		try (WriteLock lock = writeReadSemaphore.getWriteLock(
				new WriteLockRequest(progressCallback, "SearchIndexLifecycleManager.buildIndex", LOCK_KEY_PREFIX + entityId))) {
			// In the MVP updates are unconditionally replacing the index on all changes regardless of what the change is.
			buildIndex(progressCallback, entityId);
		} catch (LockUnavilableException e) {
			throw new RecoverableMessageException(
					"Search index " + entityId + " is already being built by another worker", e);
		}
	}

	/**
	 * Build (or rebuild) the AOSS index for {@code entityId}. Caller holds the
	 * per-entity write lock. Records lifecycle state on {@code SearchIndexStatus}:
	 * CREATING on entry, ACTIVE on success, FAILED with a truncated error message
	 * on permanent failure. Transient failures (table unavailable, lock contention,
	 * concurrent-delete on AOSS) propagate as {@link RecoverableMessageException}
	 * so the worker can re-queue without flipping the index to FAILED.
	 * <p>
	 * Every source row is indexed without authorization; read access is enforced only at
	 * query time via the per-row {@code _benefactor_<i>} filtering.
	 *
	 * @param progressCallback     Refreshes the per-entity write lock during the build.
	 * @param entityId             SearchIndex entity ID.
	 */
	private void buildIndex(ProgressCallback progressCallback, String entityId)
			throws Exception {
		LOG.info("Building search index for entity: {}", entityId);
		SearchIndexStatusDao statusDao = connectionFactory.getSearchIndexStatusDao();
		String aliasName = getAliasName(entityId);
		// The physical index this build streams into; assigned once the target slot is known so the
		// failure path can clean up the partial green index without touching the live one.
		String physicalSlot = null;
		try {
			SearchIndex searchIndex = entityManager.getEntityWithoutAuthorization(entityId, SearchIndex.class);

			String definingSQL = searchIndex.getDefiningSQL();

			// Blue-green: the alias resolves to the currently-live physical index, or is empty on the
			// first build. A first build has nothing to serve yet, so it records CREATING and the query
			// path throws until it finishes; a rebuild keeps the live index queryable and leaves the
			// state ACTIVE untouched until the alias swap at the end.
			Optional<String> oldTarget = openSearchManager.getAliasTarget(aliasName);
			if (oldTarget.isEmpty()) {
				statusDao.createOrUpdate(new SearchIndexStatus()
						.setSearchIndexId(entityId)
						.setState(SearchIndexState.CREATING));
			}

			Optional<SearchConfiguration> configOpt = searchConfigurationResolver.resolve(
					searchIndex.getSearchConfigurationId(), searchIndex.getParentId());
			SearchConfiguration config = configOpt.orElse(null);

			List<ColumnAnalyzerOverride> overrides = loadColumnAnalyzerOverrides(config);

			// Replace every inline analyzer literal (on config.defaultAnalyzer and on each
			// override entry's analyzer slot) with a $ref to a synthetic qname, returning a
			// synthetic TextAnalyzer per inlined slot. After this pass the rest of the build
			// path is qname-only — no more inline branches.
			Map<String, TextAnalyzer> inlineAnalyzers = materializeInlineAnalyzerSlots(config, overrides);

			// Bound schema order must match the streamed row values' order positionally — the
			// SearchIndexRowHandler relies on this alignment to map the leading row values to
			// document columns (and treat any trailing values as benefactor columns).
			List<ColumnModel> selectedColumns = tableManagerSupport.getTableSchema(IdAndVersion.parse(entityId));
			if (selectedColumns == null || selectedColumns.isEmpty()) {
				throw new IllegalStateException("SearchIndex " + entityId
						+ " has no bound schema — update the entity to re-register.");
			}
			List<SelectColumn> selectColumns = TableModelUtils.getSelectColumns(selectedColumns);

			IdAndVersion sourceId = TableModelUtils.getSourceTableIds(definingSQL).get(0);
			IndexDescription sourceIndexDescription = tableManagerSupport.getIndexDescription(sourceId);
			TableIndexDAO indexDao = connectionFactory.getConnection(sourceId);

			// Require the source to be AVAILABLE first. A PROCESSING source cannot be waited on by
			// retrying the message — a source table can take far longer to become AVAILABLE than
			// the SQS retention window, so retry-until-expiry sends the message to the dead-letter
			// queue. Instead record WAITING_FOR_SOURCE and consume the message; the rebuild fires
			// later, driven by the source's own TABLE_STATUS_EVENT(AVAILABLE), which fans out a
			// per-dependent rebuild message. A failed source is a permanent build failure.
			TableStatus sourceStatus = tableManagerSupport.getTableStatusOrCreateIfNotExists(sourceId);
			switch (sourceStatus.getState()) {
			case AVAILABLE:
				break;
			case PROCESSING:
				statusDao.createOrUpdate(new SearchIndexStatus()
						.setSearchIndexId(entityId)
						.setState(SearchIndexState.WAITING_FOR_SOURCE));
				LOG.info("Search index for entity {} is WAITING_FOR_SOURCE ({})", entityId, sourceId);
				return;
			default:
				throw new TableFailedException(sourceStatus);
			}

			// Conservative whole-table ceiling: the defining SQL queries a single source table
			// so it cannot expand the row count beyond the source's row count.
			// The handler's mid-stream MAX_ROWS guard remains
			// the backstop. A null count means the source index table is absent.
			Long rowCount = indexDao.getRowCountForTable(sourceId);
			if (rowCount != null && rowCount > MAX_ROWS) {
				throw new IllegalStateException(
						"Search index would exceed maximum of " + MAX_ROWS
								+ " rows. Row count: " + rowCount);
			}

			Map<String, TextAnalyzer> analyzers = collectAndLoadAnalyzers(
					config, overrides, selectedColumns);
			// Synthetic inline analyzers join the loaded set; downstream code treats them
			// identically to DAO-loaded TextAnalyzers.
			analyzers.putAll(inlineAnalyzers);
			String defaultAnalyzer = config != null ? SearchOpaqueJsonUtil.readRef(config.getDefaultAnalyzer()) : null;

			// Pre-flight: every default / override analyzer qname must resolve to a loaded
			// TextAnalyzer. A miss throws IllegalArgumentException — caught below and surfaced
			// via SearchIndexStatus.errorMessage.
			validateReferencedResources(defaultAnalyzer, overrides, analyzers);

			// Parse each analyzer's settings JSON and resolve all $ref entries to SynonymSet
			// definitions. The resolved value is the typed IndexSettingsAnalysis the
			// OpenSearchManager merges into the index's settings.analysis block. SynonymSet
			// qname existence is validated lazily here — a missing target raises
			// IllegalArgumentException via SearchOpaqueJsonUtil.
			Map<String, IndexSettingsAnalysis> resolvedAnalyzers = resolveAnalyzers(analyzers);

			int benefactorCount = sourceIndexDescription.getBenefactors().size();

			// Size the index from the source table's on-disk bytes. The source is a conservative
			// upper bound for the derived index, so this never under-shards. Replicas are
			// stack-coupled: the single-node dev domain cannot allocate a replica (it would sit
			// UNASSIGNED), so dev uses 0; prod uses 1 for HA and read scaling.
			Long dataSizeBytes = indexDao.getDataSizeBytesForTable(sourceId);
			int numberOfShards = computeShardCount(dataSizeBytes);
			int numberOfReplicas = stackConfiguration.isProductionStack() ? 1 : 0;

			// Build into the physical slot the alias is NOT currently serving, so the live index keeps
			// answering queries throughout. createIndex is delete-then-create on the fixed slot name, so
			// any orphan left in the idle slot by a previously-failed build is overwritten here.
			physicalSlot = getIdlePhysicalSlot(entityId, oldTarget);
			openSearchManager.deleteIndex(physicalSlot);
			openSearchManager.createIndex(physicalSlot, selectedColumns,
					defaultAnalyzer,
					overrides, resolvedAnalyzers,
					benefactorCount, numberOfShards, numberOfReplicas);

			// AOSS acknowledges createIndex and returns an already-queryable index before its
			// shards are actually ready to accept writes. Block until a real sentinel write
			// succeeds so the bulk stream below does not race against index_not_found_exception.
			openSearchManager.waitForIndexWritable(physicalSlot);

			// SqlContext.query (not build) emits the select against the source's materialized
			// index table; build context is rejected by table and view sources (only a
			// materialized view accepts it). No userId is supplied: a SearchIndex indexes every
			// source row without authorization and is served to many users through per-row
			// benefactor filtering, so there is no single current user to bind.
			QueryTranslator base = QueryTranslator.builder()
					.sql(definingSQL)
					.schemaProvider(tableManagerSupport)
					.sqlContext(SqlContext.query)
					.indexDescription(sourceIndexDescription)
					.build();
			// Splice the source's per-dependency benefactor columns into the select so the handler can
			// read them as trailing row values.
			TranslatedQuery query = buildWithBenefactorColumns(base, sourceIndexDescription);
			// queryAsStream does not close the handler; the try-with-resources flushes the final
			// partial batch.
			try (SearchIndexRowHandler handler = new SearchIndexRowHandler(
					physicalSlot, selectColumns, openSearchManager)) {
				indexDao.queryAsStream(query, handler);
			}

			// Atomically repoint the alias to the freshly-built slot. Only now does the new data
			// become visible to queries; the old index served every query up to this instant.
			openSearchManager.swapAlias(aliasName, physicalSlot, oldTarget);

			// The old index is no longer reachable via the alias; delete it now rather than waiting
			// for the next rebuild's idle-slot cleanup, so an entity that is never rebuilt again does
			// not leave an orphaned index behind. Best-effort — a failure here does not affect the
			// just-completed build; the next rebuild's pre-build idle-slot delete retries it.
			oldTarget.ifPresent(old -> {
				try {
					openSearchManager.deleteIndex(old);
				} catch (Exception e) {
					LOG.error("Failed to delete demoted search index " + old + " for entity: " + entityId, e);
				}
			});

			statusDao.createOrUpdate(new SearchIndexStatus()
					.setSearchIndexId(entityId)
					.setState(SearchIndexState.ACTIVE));
			LOG.info("Search index for entity {} is ACTIVE", entityId);
		} catch (RecoverableMessageException | TableFailedException | LockUnavilableException e) {
			// Propagate transient/infrastructure exceptions to the worker so it can
			// convert them into RecoverableMessageException (lock) or permanent failure
			// (table failed). Do not record FAILED here — the failure is not in the search
			// index's configuration.
			throw e;
		} catch (Throwable e) {
			// Another worker is currently deleting this same AOSS index. Translate
			// to a recoverable SQS retry: by the time the retry runs, the winning
			// delete has finished and our deleteIndex on retry no-ops via
			// index_not_found. Don't record FAILED — this is transient.
			if (e instanceof OpenSearchException
					&& OpenSearchManagerImpl.isConcurrentDeleteError((OpenSearchException) e)) {
				throw new RecoverableMessageException(
						"Concurrent delete in progress while building search index for entity "
								+ entityId, e);
			}
			// Defensive: a LockUnavilableException wrapped inside another exception is
			// still a transient writer-contention signal, not a build defect. Surface
			// the original so the worker re-queues the message instead of marking the
			// SearchIndex permanently FAILED.
			if (e.getCause() instanceof LockUnavilableException) {
				LockUnavilableException lockEx = (LockUnavilableException) e.getCause();
				LOG.warn("Lock unavailable for entity {}, retrying: {}", entityId, lockEx.getMessage());
				throw lockEx;
			}
			LOG.error("Failed to build search index for entity: " + entityId, e);
			String errorMessage = e.getMessage();
			if (errorMessage != null && errorMessage.length() > MAX_ERROR_MESSAGE_LENGTH) {
				errorMessage = errorMessage.substring(0, MAX_ERROR_MESSAGE_LENGTH);
			}
			// Mark the SearchIndex permanently FAILED. The truncated message stays the
			// single source of truth — users retrieve it via getSearchIndexStatus on the entity.
			statusDao.createOrUpdate(new SearchIndexStatus()
					.setSearchIndexId(entityId)
					.setState(SearchIndexState.FAILED)
					.setErrorMessage(errorMessage));
			// Best-effort cleanup of the partial green index. Only the slot this build was streaming
			// into is dropped — the live index (still referenced by the alias) is left untouched.
			if (physicalSlot != null) {
				try {
					openSearchManager.deleteIndex(physicalSlot);
				} catch (Exception deleteEx) {
					LOG.error("Failed to clean up partial index for entity: " + entityId, deleteEx);
				}
			}
		}
	}

	@Override
	public void handleDelete(ProgressCallback progressCallback, String entityId) throws Exception {
		ValidateArgument.required(entityId, "entityId");
		Long searchIndexId = KeyFactory.stringToKey(entityId);
		// Cheap precheck before acquiring the write lock. Migration replay delivers ENTITY
		// changes for entities that have since been deleted; the worker funnels those through
		// here via the NotFoundException path. If there is no SearchIndexStatus row, there is
		// no AOSS index to delete and nothing to clean up — skip the lock acquire entirely.
		// Acquiring the per-entity write lock for every deleted-entity replay is the dominant
		// cost in the SEARCH_INDEX_LIFECYCLE worker's per-message profile.
		SearchIndexStatusDao statusDao = connectionFactory.getSearchIndexStatusDao();
		if (statusDao.getState(searchIndexId).isEmpty()) {
			return;
		}
		try (WriteLock lock = writeReadSemaphore.getWriteLock(
				new WriteLockRequest(progressCallback, "SearchIndexLifecycleManager.handleDelete", LOCK_KEY_PREFIX + entityId))) {
			// Re-check under the lock in case a concurrent delete already cleaned up.
			Optional<SearchIndexState> stateOpt = statusDao.getState(searchIndexId);
			if (stateOpt.isEmpty()) {
				return;
			}
			// Delete both physical slots independently so a failure on one does not skip the
			// other. Deleting the concrete index that backs the query alias also removes its
			// alias membership, so no separate alias removal is required; a slot that does not
			// exist is an idempotent no-op.
			boolean slotADeleted = tryDeleteSlot(getPhysicalSlotName(entityId, SLOT_A), entityId);
			boolean slotBDeleted = tryDeleteSlot(getPhysicalSlotName(entityId, SLOT_B), entityId);
			// Only drop the DB rows once both slots are confirmed gone, so a slot left behind by
			// a failed delete can still be found and retried on the next lifecycle message.
			if (slotADeleted && slotBDeleted) {
				statusDao.delete(searchIndexId);
				// The node ON DELETE CASCADE normally clears the source edge; drop it explicitly
				// too so a lifecycle delete that runs before the node delete leaves no stale edge.
				definingSqlDependencyDao.deleteObject(IdAndVersion.parse(entityId));
			}
		} catch (LockUnavilableException e) {
			throw new RecoverableMessageException(
					"Search index " + entityId + " is locked by another worker", e);
		}
	}

	@Override
	public void rebuildIfStale(ProgressCallback progressCallback, String entityId) throws Exception {
		ValidateArgument.required(entityId, "entityId");
		try (WriteLock lock = writeReadSemaphore.getWriteLock(
				new WriteLockRequest(progressCallback, "SearchIndexLifecycleManager.rebuildIfStale",
						LOCK_KEY_PREFIX + entityId))) {
			// Authoritative gate under the lock: the lock linearizes this against any in-flight build.
			//  - WAITING_FOR_SOURCE: first-availability build.
			//  - ACTIVE: live-sync — always rebuild.
			//  - CREATING / FAILED / absent: no-op (a CREATING index that has since finished is observed
			//    as ACTIVE and rebuilt; FAILED is terminal until a manual rebuild).
			Optional<SearchIndexState> stateOpt = connectionFactory.getSearchIndexStatusDao()
					.getState(KeyFactory.stringToKey(entityId));
			if (stateOpt.isEmpty()) {
				return;
			}
			SearchIndexState state = stateOpt.get();
			if (SearchIndexState.WAITING_FOR_SOURCE.equals(state) || SearchIndexState.ACTIVE.equals(state)) {
				buildIndex(progressCallback, entityId);
			}
		} catch (LockUnavilableException e) {
			// Another worker holds the per-entity lock (an in-flight build). Requeue for retry once it
			// frees, same as the materialized view rebuild path.
			throw new RecoverableMessageException(
					"Search index " + entityId + " is locked by another worker", e);
		}
	}

	/**
	 * Collect every TextAnalyzer qualified name referenced by the configuration's
	 * defaults, the per-column overrides, and each column type's system default, then
	 * bulk-load them via {@code textAnalyzerDao}. Returns a mutable map keyed by
	 * qualified name.
	 *
	 * @param config    Effective {@link SearchConfiguration}; may be {@code null}.
	 * @param overrides Resolved column-analyzer overrides; may be empty.
	 * @param columns   Columns of the source table; the system default analyzer for
	 *                  each column type is always loaded.
	 * @return mutable map qualified-name → TextAnalyzer.
	 */
	Map<String, TextAnalyzer> collectAndLoadAnalyzers(SearchConfiguration config,
			List<ColumnAnalyzerOverride> overrides, List<ColumnModel> columns) {
		Set<String> qualifiedNames = new HashSet<>();

		qualifiedNames.add(ColumnTypeToOpenSearchMapping.getDefaultAnalyzerQualifiedName(ColumnType.STRING));

		if (overrides != null) {
			for (ColumnAnalyzerOverride override : overrides) {
				if (override.getOverrides() != null) {
					for (ColumnAnalyzerOverrideEntry entry : override.getOverrides()) {
						String qname = SearchOpaqueJsonUtil.readRef(entry.getAnalyzer());
						if (qname != null) {
							qualifiedNames.add(qname);
						}
					}
				}
			}
		}

		if (config != null) {
			String defaultQname = SearchOpaqueJsonUtil.readRef(config.getDefaultAnalyzer());
			if (defaultQname != null) {
				qualifiedNames.add(defaultQname);
			}
		}

		for (ColumnModel column : columns) {
			qualifiedNames.add(ColumnTypeToOpenSearchMapping.getDefaultAnalyzerQualifiedName(column.getColumnType()));
		}

		return new HashMap<>(textAnalyzerDao.getByQualifiedNames(new ArrayList<>(qualifiedNames)));
	}

	/**
	 * Parse and resolve each TextAnalyzer's settings JSON. The output map is keyed by the
	 * same qualified names as the input and holds the post-{@code $ref}-resolution typed
	 * {@link IndexSettingsAnalysis} ready for {@link OpenSearchManager#createIndex}.
	 * {@code $ref} values are resolved by looking up the corresponding SynonymSet
	 * definition through {@link SynonymSetDao}.
	 *
	 * @throws IllegalArgumentException when a {@code $ref} target qname does not resolve to
	 *         an existing SynonymSet (deleted between TextAnalyzer save and index build).
	 */
	Map<String, IndexSettingsAnalysis> resolveAnalyzers(Map<String, TextAnalyzer> analyzers) {
		Map<String, IndexSettingsAnalysis> resolved = new HashMap<>();
		for (Map.Entry<String, TextAnalyzer> entry : analyzers.entrySet()) {
			JsonNode root = SearchOpaqueJsonUtil.parse(entry.getValue().getSettings());
			IndexSettingsAnalysis settings = SearchOpaqueJsonUtil.resolveAnalyzerSettings(root, qname -> {
				Map<String, SynonymSet> map = synonymSetDao.getByQualifiedNames(
						Collections.singletonList(qname));
				SynonymSet ss = map.get(qname);
				if (ss == null) {
					return null;
				}
				return SearchOpaqueJsonUtil.parse(ss.getDefinition());
			});
			resolved.put(entry.getKey(), settings);
		}
		return resolved;
	}

	// Package-private for branch-coverage tests.
	List<ColumnAnalyzerOverride> loadColumnAnalyzerOverrides(SearchConfiguration config) {
		if (config == null || config.getColumnAnalyzerOverrides() == null
				|| config.getColumnAnalyzerOverrides().isEmpty()) {
			return Collections.emptyList();
		}
		List<String> qnames = new ArrayList<>();
		List<ColumnAnalyzerOverride> inlineOverrides = new ArrayList<>();
		for (Object element : config.getColumnAnalyzerOverrides()) {
			String qname = SearchOpaqueJsonUtil.readRef(element);
			if (qname != null) {
				qnames.add(qname);
			} else {
				ColumnAnalyzerOverride inline = SearchOpaqueJsonUtil.toInline(element,
						ColumnAnalyzerOverride.class);
				if (inline != null) {
					inlineOverrides.add(inline);
				}
			}
		}
		List<ColumnAnalyzerOverride> result = new ArrayList<>();
		if (!qnames.isEmpty()) {
			result.addAll(columnAnalyzerOverrideDao.getByQualifiedNames(qnames).values());
		}
		result.addAll(inlineOverrides);
		return result;
	}

	/**
	 * Walk every analyzer slot on the configuration / overrides and replace each inline
	 * literal with a {@code $ref} to a synthetic qualified name, returning a synthetic
	 * {@link TextAnalyzer} for each so the inline analyzer joins the qname-keyed pipeline
	 * (existence check, settings resolution, AOSS registration) without further branches.
	 *
	 * <p>Synthetic qnames use the {@code synapse-inline_*} prefix. The synthetic qname must
	 * satisfy {@link SearchResourceConstants#QUALIFIED_NAME_PATTERN} (so downstream code
	 * that re-validates qnames passes through) and must not start with an underscore (AOSS
	 * rejects analyzer keys that do). The synthetic qnames live only in memory during the
	 * build &mdash; nothing is persisted under these names &mdash; so a real TextAnalyzer
	 * row in the {@code synapse} organization with a clashing local name would shadow the
	 * synthetic only within a single build, which is harmless because each build assigns
	 * synthetic qnames fresh from inline literals. The
	 * SynonymSet {@code $ref} resolver runs on each inline literal during this pass so the
	 * settings JSON the synthetic TextAnalyzer carries is the same already-validated bytes
	 * the curator submitted &mdash; {@link #resolveAnalyzers} re-parses and splices refs
	 * uniformly across both real and synthetic entries.</p>
	 *
	 * <p>Mutates {@code config} and each entry of {@code overrides} in place: the inline
	 * literal slot becomes a {@code {"$ref": "__inline-..."}} value, so all downstream
	 * {@link SearchOpaqueJsonUtil#readRef} calls (including
	 * {@link OpenSearchManagerImpl#buildMappings}) return the synthetic qname.</p>
	 */
	Map<String, TextAnalyzer> materializeInlineAnalyzerSlots(SearchConfiguration config,
			List<ColumnAnalyzerOverride> overrides) {
		Map<String, TextAnalyzer> synthetic = new HashMap<>();
		if (config != null) {
			Object defaultSlot = config.getDefaultAnalyzer();
			if (defaultSlot != null && SearchOpaqueJsonUtil.readRef(defaultSlot) == null) {
				String qname = "synapse-inline_default";
				config.setDefaultAnalyzer(refMap(qname));
				synthetic.put(qname, syntheticTextAnalyzer(qname, defaultSlot));
			}
		}
		int counter = 0;
		if (overrides != null) {
			for (ColumnAnalyzerOverride override : overrides) {
				if (override.getOverrides() == null) {
					continue;
				}
				for (ColumnAnalyzerOverrideEntry entry : override.getOverrides()) {
					Object analyzerSlot = entry.getAnalyzer();
					if (analyzerSlot == null
							|| SearchOpaqueJsonUtil.readRef(analyzerSlot) != null) {
						continue;
					}
					String qname = "synapse-inline_override_" + (counter++);
					entry.setAnalyzer(refMap(qname));
					synthetic.put(qname, syntheticTextAnalyzer(qname, analyzerSlot));
				}
			}
		}
		return synthetic;
	}

	private static Map<String, String> refMap(String qname) {
		Map<String, String> ref = new HashMap<>(1);
		ref.put(SearchOpaqueJsonUtil.REF_KEY, qname);
		return ref;
	}

	private static TextAnalyzer syntheticTextAnalyzer(String qname, Object inlineSettings) {
		return new TextAnalyzer().setName(qname).setSettings(inlineSettings);
	}

	/**
	 * Splice the source index's physical benefactor columns into the base query so the streamed rows
	 * carry one benefactor value per source dependency (in {@link IndexDescription#getBenefactors()}
	 * order).
	 * <p>
	 * The columns must land <em>before</em> the trailing by-name metadata columns ({@code ROW_ID,
	 * ROW_VERSION}) and be mirrored into the select-column headers as INTEGER columns, because
	 * {@code SQLTranslatorUtils.readRow} reads {@code Row.values} positionally (the first
	 * {@code getSelectColumns().length} result columns) while reading {@code ROW_ID}/{@code ROW_VERSION}
	 * by name. The base translator's {@code getSelectColumns()} holds only the document columns
	 * (metadata columns are added to the SQL by name, not to the headers), so the splice index is
	 * simply that header count.
	 * <p>
	 * Only a materialized view stores its per-dependency benefactors as physical columns of its index
	 * table that must be spliced in here. A view's single benefactor is already read by name into
	 * {@code Row.benefactorId} (via the by-name metadata columns the base query emits), and a table has
	 * none, so for any non-materialized-view source the base query is returned unchanged.
	 *
	 * @param base   a query-context {@link QueryTranslator} built from the source's defining SQL.
	 * @param source the source's {@link IndexDescription}.
	 * @return a {@link TranslatedQuery} ready to stream through {@code TableIndexDAO.queryAsStream}.
	 */
	static TranslatedQuery buildWithBenefactorColumns(QueryTranslator base, IndexDescription source) {
		if (!TableType.materializedview.equals(source.getTableType())) {
			return CachedQueryRequest.clone(base);
		}

		List<String> benefactorColumnNames = new ArrayList<>();
		for (BenefactorDescription desc : source.getBenefactors()) {
			benefactorColumnNames.add(desc.getBenefactorColumnName());
		}
		if (benefactorColumnNames.isEmpty()) {
			return CachedQueryRequest.clone(base);
		}

		// The document columns are the only headers on the base translator; the benefactor columns
		// splice in just after them, ahead of the by-name ROW_ID/ROW_VERSION metadata in the SQL.
		int spliceIndex = base.getSelectColumns().size();
		SelectList selectList = base.getTranslatedModel().getFirstElementOfType(SelectList.class);
		for (int i = 0; i < benefactorColumnNames.size(); i++) {
			DerivedColumn column = SqlElementUtils.createNonQuotedDerivedColumn(benefactorColumnNames.get(i));
			selectList.getColumns().add(spliceIndex + i, column);
		}
		selectList.recursiveSetParent();
		String outputSQL = base.getTranslatedModel().toSql();

		List<SelectColumn> headers = new ArrayList<>(base.getSelectColumns());
		for (String name : benefactorColumnNames) {
			headers.add(new SelectColumn().setName(name).setColumnType(ColumnType.INTEGER));
		}

		return CachedQueryRequest.clone(base).setOutputSQL(outputSQL).setSelectColumns(headers);
	}

	/** The stable alias queries target: {@code search-index-<entityId>}. */
	private String getAliasName(String entityId) {
		return INDEX_PREFIX + entityId;
	}

	/** A physical slot index: {@code search-index-<entityId>-<slot>}. */
	private String getPhysicalSlotName(String entityId, String slot) {
		return INDEX_PREFIX + entityId + "-" + slot;
	}

	/**
	 * The physical slot to build into: the one the alias is NOT currently serving. On the first build
	 * ({@code currentTarget} empty) this is slot A; otherwise it is whichever slot the alias does not
	 * already point at, so the live index keeps serving queries while the new one is built.
	 */
	private String getIdlePhysicalSlot(String entityId, Optional<String> currentTarget) {
		String slotA = getPhysicalSlotName(entityId, SLOT_A);
		if (currentTarget.isPresent() && currentTarget.get().equals(slotA)) {
			return getPhysicalSlotName(entityId, SLOT_B);
		}
		return slotA;
	}

	/**
	 * Attempts to delete a single physical slot, isolated from any sibling slot delete.
	 * A concurrent-delete race is translated into a {@link RecoverableMessageException} so the
	 * caller retries; any other failure is logged and swallowed, returning {@code false} so the
	 * caller knows this slot was not confirmed deleted.
	 */
	private boolean tryDeleteSlot(String physicalSlot, String entityId) {
		try {
			openSearchManager.deleteIndex(physicalSlot);
			return true;
		} catch (OpenSearchException e) {
			if (OpenSearchManagerImpl.isConcurrentDeleteError(e)) {
				throw new RecoverableMessageException(
						"Concurrent delete in progress while deleting search index for entity " + entityId, e);
			}
			LOG.error("Failed to delete search index slot " + physicalSlot + " for entity: " + entityId, e);
			return false;
		} catch (Exception e) {
			LOG.error("Failed to delete search index slot " + physicalSlot + " for entity: " + entityId, e);
			return false;
		}
	}

	static class SearchIndexRowHandler implements RowHandler {

		private final String indexName;
		private final List<SelectColumn> columns;
		private final OpenSearchManager client;
		private final List<BulkOperation> batch = new ArrayList<>();
		private long totalRows = 0;

		SearchIndexRowHandler(String indexName, List<SelectColumn> columns, OpenSearchManager client) {
			this.indexName = indexName;
			this.columns = columns;
			this.client = client;
		}

		@Override
		public void nextRow(Row row) {
			if (totalRows >= MAX_ROWS) {
				throw new IllegalStateException(
						"Search index exceeds maximum of " + MAX_ROWS + " rows.");
			}
			Map<String, Object> doc = new HashMap<>();
			doc.put("_row_id", row.getRowId());
			doc.put("_row_version", row.getVersionNumber());
			List<String> values = row.getValues();
			// The bound schema (columns) defines the document columns; they are the leading
			// values of the row. Any trailing values are the benefactor columns the translator
			// appended to the select (one per source dependency, in getBenefactors() order).
			// This relies on the invariant that the translated select's document-column count
			// equals the bound-schema width, since both derive from the same defining SQL.
			for (int i = 0; i < columns.size() && i < values.size(); i++) {
				String value = values.get(i);
				if (value != null) {
					SelectColumn column = columns.get(i);
					doc.put(column.getId(), convertForDocument(value, column.getColumnType()));
				}
			}
			// Write one _benefactor_N field per source dependency, in the same order the
			// query-time ACL filter expects. A materialized view appends its benefactor columns
			// as trailing row values; a view exposes its single benefactor through the by-name
			// scalar Row.benefactorId (nothing trails values). A plain table has no benefactor
			// and leaves both empty.
			if (values.size() > columns.size()) {
				for (int i = columns.size(); i < values.size(); i++) {
					String value = values.get(i);
					if (value != null) {
						doc.put("_benefactor_" + (i - columns.size()), Long.valueOf(value));
					}
				}
			} else if (row.getBenefactorId() != null) {
				doc.put("_benefactor_0", row.getBenefactorId());
			}
			String docId = String.valueOf(row.getRowId());
			batch.add(BulkOperation.of(op -> op
					.index(idx -> idx
							.index(indexName)
							.id(docId)
							.document(doc))));
			totalRows++;
			if (batch.size() >= BATCH_SIZE) {
				flush();
			}
		}

		private void flush() {
			if (!batch.isEmpty()) {
				client.bulkIndex(indexName, new ArrayList<>(batch));
				batch.clear();
			}
		}

		@Override
		public void close() throws IOException {
			flush();
		}
	}

	/**
	 * Pre-flight check: every TextAnalyzer qname referenced by the configuration's defaults
	 * and the per-column overrides must resolve to a loaded TextAnalyzer. Throws on first
	 * miss so the outer catch records the failure into {@code SearchIndexStatus.errorMessage}
	 * (truncated to 3000 chars by {@link #MAX_ERROR_MESSAGE_LENGTH}) and the SearchIndex is
	 * marked FAILED. SynonymSet references inside each TextAnalyzer's settings are validated
	 * separately, lazily, by {@link #resolveAnalyzers}.
	 *
	 * @param defaultAnalyzer Qualified name of the SearchConfiguration's primary
	 *                        TextAnalyzer; may be {@code null}.
	 * @param overrides       Resolved column-analyzer overrides; may be empty.
	 * @param analyzers       Map of qualified name → TextAnalyzer loaded via
	 *                        {@link #collectAndLoadAnalyzers}.
	 * @throws IllegalArgumentException when any non-null qname does not resolve to a
	 *         loaded TextAnalyzer.
	 */
	// Package-private for branch-coverage tests.
	void validateReferencedResources(String defaultAnalyzer,
			List<ColumnAnalyzerOverride> overrides,
			Map<String, TextAnalyzer> analyzers) {

		// Default analyzer: must exist in the loaded analyzers map when set.
		assertAnalyzerExists(defaultAnalyzer, analyzers, "defaultAnalyzer");

		// Override analyzers: must exist when set.
		if (overrides != null) {
			for (ColumnAnalyzerOverride override : overrides) {
				if (override.getOverrides() == null) {
					continue;
				}
				for (ColumnAnalyzerOverrideEntry entry : override.getOverrides()) {
					assertAnalyzerExists(SearchOpaqueJsonUtil.readRef(entry.getAnalyzer()), analyzers,
							"override '" + override.getName() + "' analyzer for column '" + entry.getColumnName() + "'");
				}
			}
		}
	}

	private static void assertAnalyzerExists(String qname,
			Map<String, TextAnalyzer> analyzers, String context) {
		if (qname == null || analyzers.containsKey(qname)) {
			return;
		}
		throw new IllegalArgumentException("TextAnalyzer '" + qname + "' (" + context + ") does not resolve.");
	}

}
