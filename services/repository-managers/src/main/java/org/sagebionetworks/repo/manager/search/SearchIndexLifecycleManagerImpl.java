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
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.manager.table.TableQueryManager;
import org.sagebionetworks.repo.manager.table.query.QueryTranslations;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.RowHandler;
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
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryOptions;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.QueryTranslator;
import org.sagebionetworks.table.cluster.description.IndexDescription;
import org.sagebionetworks.table.cluster.search.SearchIndexStatusDao;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.query.model.SqlContext;
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
	private static final String LOCK_KEY_PREFIX = "search-index-build:";
	private static final int MAX_ERROR_MESSAGE_LENGTH = 3000;
	private static final int BATCH_SIZE = 1000;
	private static final long MAX_ROWS = 500_000L;
	private static final ObjectMapper SEARCH_DOC_MAPPER = new ObjectMapper();

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
	private final TableQueryManager tableQueryManager;
	private final UserManager userManager;
	private final EntityManager entityManager;
	private final SynonymSetDao synonymSetDao;
	private final ColumnAnalyzerOverrideDao columnAnalyzerOverrideDao;
	private final TextAnalyzerDao textAnalyzerDao;
	private final TableManagerSupport tableManagerSupport;
	private final ColumnModelManager columnModelManager;
	private final WriteReadSemaphore writeReadSemaphore;

	public SearchIndexLifecycleManagerImpl(ConnectionFactory connectionFactory,
			OpenSearchManager openSearchManager,
			SearchConfigurationResolver searchConfigurationResolver,
			TableQueryManager tableQueryManager, UserManager userManager,
			EntityManager entityManager,
			SynonymSetDao synonymSetDao, ColumnAnalyzerOverrideDao columnAnalyzerOverrideDao,
			TextAnalyzerDao textAnalyzerDao,
			TableManagerSupport tableManagerSupport,
			ColumnModelManager columnModelManager,
			WriteReadSemaphore writeReadSemaphore) {
		this.connectionFactory = connectionFactory;
		this.openSearchManager = openSearchManager;
		this.searchConfigurationResolver = searchConfigurationResolver;
		this.tableQueryManager = tableQueryManager;
		this.userManager = userManager;
		this.entityManager = entityManager;
		this.synonymSetDao = synonymSetDao;
		this.columnAnalyzerOverrideDao = columnAnalyzerOverrideDao;
		this.textAnalyzerDao = textAnalyzerDao;
		this.tableManagerSupport = tableManagerSupport;
		this.columnModelManager = columnModelManager;
		this.writeReadSemaphore = writeReadSemaphore;
	}

	@Override
	@WriteTransaction
	public List<String> registerSchema(IdAndVersion searchIndexId, String definingSql) {
		ValidateArgument.required(searchIndexId, "searchIndexId");
		ValidateArgument.requiredNotBlank(definingSql, "definingSql");
		IdAndVersion sourceId = TableModelUtils.getSourceTableIds(definingSql).get(0);
		IndexDescription indexDescription = tableManagerSupport.getIndexDescription(sourceId);
		// SqlContext.query: TableIndexDescription rejects `build`; only Views/MVs accept it.
		QueryTranslator sqlQuery = QueryTranslator.builder()
				.sql(definingSql)
				.schemaProvider(tableManagerSupport)
				.sqlContext(SqlContext.query)
				.indexDescription(indexDescription)
				.build();
		List<String> schemaIds = sqlQuery.getSchemaOfSelect().stream()
				.map(c -> columnModelManager.createColumnModel(c).getId())
				.collect(Collectors.toList());
		columnModelManager.bindColumnsToVersionOfObject(schemaIds, searchIndexId);
		return schemaIds;
	}

	@Override
	public void handleCreate(ProgressCallback progressCallback, String entityId, Long userId) throws Exception {
		ValidateArgument.required(entityId, "entityId");
		ValidateArgument.required(userId, "userId");
		try (WriteLock lock = writeReadSemaphore.getWriteLock(
				new WriteLockRequest(progressCallback, "SearchIndexLifecycleManager.buildIndex", LOCK_KEY_PREFIX + entityId))) {
			buildIndex(progressCallback, entityId, userId, true);
		} catch (LockUnavilableException e) {
			throw new RecoverableMessageException(
					"Search index " + entityId + " is already being built by another worker", e);
		}
	}

	@Override
	public void handleUpdate(ProgressCallback progressCallback, String entityId, Long userId) throws Exception {
		ValidateArgument.required(entityId, "entityId");
		ValidateArgument.required(userId, "userId");
		try (WriteLock lock = writeReadSemaphore.getWriteLock(
				new WriteLockRequest(progressCallback, "SearchIndexLifecycleManager.buildIndex", LOCK_KEY_PREFIX + entityId))) {
			// In the MVP updates are unconditionally replacing the index on all changes regardless of what the change is.
			buildIndex(progressCallback, entityId, userId, true);
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
	 *
	 * @param progressCallback     Refreshes the per-entity write lock during the build.
	 * @param entityId             SearchIndex entity ID.
	 * @param userId               User who triggered the change. Authorization for the
	 *                             row stream is performed as the realm's anonymous user;
	 *                             this id is recorded for audit only.
	 * @param deleteExistingFirst  When true, the AOSS index is dropped before the build
	 *                             (the rebuild path); when false, the build assumes no
	 *                             existing index exists.
	 */
	private void buildIndex(ProgressCallback progressCallback, String entityId, Long userId,
			boolean deleteExistingFirst)
			throws Exception {
		SearchIndexStatusDao statusDao = connectionFactory.getSearchIndexStatusDao();
		try {
			UserInfo user = userManager.getUserInfo(userId);
			SearchIndex searchIndex = entityManager.getEntity(user, entityId, SearchIndex.class);

			String definingSQL = searchIndex.getDefiningSQL();

			statusDao.createOrUpdate(new SearchIndexStatus()
					.setSearchIndexId(entityId)
					.setState(SearchIndexState.CREATING));

			Optional<SearchConfiguration> configOpt = searchConfigurationResolver.resolve(
					user, searchIndex.getSearchConfigurationId(), searchIndex.getParentId());
			SearchConfiguration config = configOpt.orElse(null);

			List<ColumnAnalyzerOverride> overrides = loadColumnAnalyzerOverrides(config);

			// Replace every inline analyzer literal (on config.defaultAnalyzer and on each
			// override entry's analyzer slot) with a $ref to a synthetic qname, returning a
			// synthetic TextAnalyzer per inlined slot. After this pass the rest of the build
			// path is qname-only — no more inline branches.
			Map<String, TextAnalyzer> inlineAnalyzers = materializeInlineAnalyzerSlots(config, overrides);

			// Bound schema order must match the streamed row values' order positionally —
			// runQueryAsStream below relies on this alignment to map values to columns.
			List<ColumnModel> selectedColumns = tableManagerSupport.getTableSchema(IdAndVersion.parse(entityId));
			if (selectedColumns == null || selectedColumns.isEmpty()) {
				throw new IllegalStateException("SearchIndex " + entityId
						+ " has no bound schema — update the entity to re-register.");
			}
			List<SelectColumn> selectColumns = TableModelUtils.getSelectColumns(selectedColumns);

			// Build the index as the anonymous user. This enforces Sage governance:
			// only tables marked DataType.OPEN_DATA with PUBLIC read access can be
			// indexed. Any other table will fail authorization during queryPreflight,
			// and the search index will be recorded as FAILED.
			UserInfo anonymousUser = userManager.getUserInfo(user.getRealmAnonymousUserId());
			Query query = new Query();
			query.setSql(definingSQL);

			QueryOptions countOnly = new QueryOptions()
					.withRunQuery(false)
					.withRunCount(true)
					.withReturnSelectColumns(false)
					.withReturnFacets(false);
			QueryResultBundle countResult = tableQueryManager.querySinglePage(
					progressCallback, anonymousUser, query, countOnly);
			Long rowCount = countResult.getQueryCount();
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

			String indexName = getIndexName(entityId);
			if (deleteExistingFirst) {
				openSearchManager.deleteIndex(indexName);
			}
			openSearchManager.createIndex(indexName, selectedColumns,
					defaultAnalyzer,
					overrides, resolvedAnalyzers,
					config != null ? config.getColumnSemanticEnrichment() : null);

			// AOSS acknowledges createIndex and returns an already-queryable index before its
			// shards are actually ready to accept writes. Block until a real sentinel write
			// succeeds so the bulk stream below does not race against index_not_found_exception.
			openSearchManager.waitForIndexWritable(indexName);

			tableQueryManager.runQueryAsStream(progressCallback, anonymousUser, query,
					(QueryTranslations translations) -> new SearchIndexRowHandler(
							indexName, selectColumns, openSearchManager),
					ACCESS_TYPE.READ);

			statusDao.createOrUpdate(new SearchIndexStatus()
					.setSearchIndexId(entityId)
					.setState(SearchIndexState.ACTIVE));
		} catch (RecoverableMessageException | TableUnavailableException | TableFailedException | LockUnavilableException e) {
			// Propagate transient/infrastructure exceptions to the worker so it can
			// convert them into RecoverableMessageException (table unavailable / lock)
			// or permanent failure (table failed). Do not record FAILED here — the
			// failure is not in the search index's configuration.
			throw e;
		} catch (Throwable e) {
			// SearchIndexRowHandler.close() tunnels RecoverableMessageException through
			// IOException because RowHandler.close() can't declare anything else. Unwrap
			// here so a convergence-probe timeout reaches the do-not-mark-FAILED branch
			// above on retry instead of permanently failing the index.
			if (e instanceof IOException && e.getCause() instanceof RecoverableMessageException) {
				throw (RecoverableMessageException) e.getCause();
			}
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
			// Best-effort cleanup of partial AOSS index
			try {
				openSearchManager.deleteIndex(getIndexName(entityId));
			} catch (Exception deleteEx) {
				LOG.error("Failed to clean up partial index for entity: " + entityId, deleteEx);
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
			try {
				openSearchManager.deleteIndex(getIndexName(entityId));
				statusDao.delete(searchIndexId);
			} catch (Throwable e) {
				LOG.error("Failed to delete search index for entity: " + entityId, e);
			}
		} catch (LockUnavilableException e) {
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
			for (ColumnAnalyzerOverride cao : overrides) {
				if (cao.getOverrides() != null) {
					for (ColumnAnalyzerOverrideEntry entry : cao.getOverrides()) {
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
			for (ColumnAnalyzerOverride cao : overrides) {
				if (cao.getOverrides() == null) {
					continue;
				}
				for (ColumnAnalyzerOverrideEntry entry : cao.getOverrides()) {
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

	private String getIndexName(String entityId) {
		return INDEX_PREFIX + entityId;
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
			for (int i = 0; i < columns.size() && i < values.size(); i++) {
				String value = values.get(i);
				if (value != null) {
					SelectColumn column = columns.get(i);
					doc.put(column.getId(), convertForDocument(value, column.getColumnType()));
				}
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
			// AOSS bulk writes acknowledge before the documents are visible to _search or
			// _count. Block until the index reports the row count we streamed, so the
			// SearchIndex isn't flipped to ACTIVE while a query would still under-return.
			try {
				client.waitForDocumentCount(indexName, totalRows);
			} catch (RecoverableMessageException e) {
				// RowHandler.close() can only declare IOException; tunnel the recoverable
				// signal through it. The lifecycle build path unwraps before its FAILED
				// branch so the message goes back on SQS instead of marking the index FAILED.
				throw new IOException(e);
			}
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
			for (ColumnAnalyzerOverride cao : overrides) {
				if (cao.getOverrides() == null) {
					continue;
				}
				for (ColumnAnalyzerOverrideEntry entry : cao.getOverrides()) {
					assertAnalyzerExists(SearchOpaqueJsonUtil.readRef(entry.getAnalyzer()), analyzers,
							"override '" + cao.getName() + "' analyzer for column '" + entry.getColumnName() + "'");
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
