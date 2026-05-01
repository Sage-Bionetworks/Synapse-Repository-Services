package org.sagebionetworks.repo.manager.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.manager.table.TableQueryManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.search.ColumnAnalyzerOverrideDao;
import org.sagebionetworks.repo.model.dbo.search.SynonymSetDao;
import org.sagebionetworks.repo.model.dbo.search.TextAnalyzerDao;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.table.cluster.QueryTranslator;
import org.sagebionetworks.table.cluster.description.IndexDescription;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.query.model.SqlContext;
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
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.search.SearchIndexStatusDao;
import org.sagebionetworks.repo.manager.table.query.QueryTranslations;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.stereotype.Service;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryOptions;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.dao.table.RowHandler;

import java.io.IOException;

@Service
public class SearchIndexLifecycleManagerImpl implements SearchIndexLifecycleManager {

	private static final Logger LOG = LogManager.getLogger(SearchIndexLifecycleManagerImpl.class);
	private static final String INDEX_PREFIX = "search-index-";
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

	public SearchIndexLifecycleManagerImpl(ConnectionFactory connectionFactory,
			OpenSearchManager openSearchManager,
			SearchConfigurationResolver searchConfigurationResolver,
			TableQueryManager tableQueryManager, UserManager userManager,
			EntityManager entityManager,
			SynonymSetDao synonymSetDao, ColumnAnalyzerOverrideDao columnAnalyzerOverrideDao,
			TextAnalyzerDao textAnalyzerDao,
			TableManagerSupport tableManagerSupport,
			ColumnModelManager columnModelManager) {
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
	public void handleCreate(ProgressCallback progressCallback, String entityId, Long userId)
			throws RecoverableMessageException, TableUnavailableException, TableFailedException, LockUnavilableException {
		buildIndex(progressCallback, entityId, userId, true);
	}

	@Override
	public void handleUpdate(ProgressCallback progressCallback, String entityId, Long userId)
			throws RecoverableMessageException, TableUnavailableException, TableFailedException, LockUnavilableException {
		// In the MVP updates are unconditionally replacing the index on all changes regardless of what the change is.
		buildIndex(progressCallback, entityId, userId, true);
	}

	private void buildIndex(ProgressCallback progressCallback, String entityId, Long userId,
			boolean deleteExistingFirst)
			throws RecoverableMessageException, TableUnavailableException, TableFailedException, LockUnavilableException {
		ValidateArgument.required(entityId, "entityId");
		ValidateArgument.required(userId, "userId");
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
			List<SynonymSet> synonymSets = loadSynonymSets(config);

			// Bound schema is stored in SELECT-list order, lining up positionally with
			// the row values streamed by `runQueryAsStream` below.
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
			String indexName = getIndexName(entityId);
			String defaultAnalyzer = config != null ? config.getDefaultAnalyzer() : null;
			if (deleteExistingFirst) {
				openSearchManager.deleteIndex(indexName);
			}
			openSearchManager.createIndex(indexName, selectedColumns, defaultAnalyzer,
					synonymSets, overrides, analyzers);

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
			LOG.error("Failed to build search index for entity: " + entityId, e);
			String errorMessage = e.getMessage();
			if (errorMessage != null && errorMessage.length() > MAX_ERROR_MESSAGE_LENGTH) {
				errorMessage = errorMessage.substring(0, MAX_ERROR_MESSAGE_LENGTH);
			}
			// Set FAILED first — status table is the source of truth
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
	public void handleDelete(String entityId) throws RecoverableMessageException {
		ValidateArgument.required(entityId, "entityId");
		Long searchIndexId = KeyFactory.stringToKey(entityId);
		SearchIndexStatusDao statusDao = connectionFactory.getSearchIndexStatusDao();

		Optional<SearchIndexState> stateOpt = statusDao.getState(searchIndexId);
		if (stateOpt.isEmpty()) {
			// No status row — already cleaned up, nothing to do
			return;
		}
		if (stateOpt.get() == SearchIndexState.CREATING) {
			// Cannot interrupt an in-flight build — retry the delete later.
			throw new RecoverableMessageException(
					"Search index " + entityId + " is still building. Will retry delete later.");
		}
		try {
			openSearchManager.deleteIndex(getIndexName(entityId));
			statusDao.delete(searchIndexId);
		} catch (Throwable e) {
			LOG.error("Failed to delete search index for entity: " + entityId, e);
		}
	}

	Map<String, TextAnalyzer> collectAndLoadAnalyzers(SearchConfiguration config,
			List<ColumnAnalyzerOverride> overrides, List<ColumnModel> columns) {
		Set<String> qualifiedNames = new HashSet<>();

		qualifiedNames.add(ColumnTypeToOpenSearchMapping.getDefaultAnalyzerQualifiedName(ColumnType.STRING));

		if (overrides != null) {
			for (ColumnAnalyzerOverride cao : overrides) {
				if (cao.getOverrides() != null) {
					for (ColumnAnalyzerOverrideEntry entry : cao.getOverrides()) {
						if (entry.getIndexAnalyzer() != null) {
							qualifiedNames.add(entry.getIndexAnalyzer());
						}
						if (entry.getSearchAnalyzer() != null) {
							qualifiedNames.add(entry.getSearchAnalyzer());
						}
					}
				}
			}
		}

		if (config != null && config.getDefaultAnalyzer() != null) {
			qualifiedNames.add(config.getDefaultAnalyzer());
		}

		for (ColumnModel column : columns) {
			qualifiedNames.add(ColumnTypeToOpenSearchMapping.getDefaultAnalyzerQualifiedName(column.getColumnType()));
		}

		return new HashMap<>(textAnalyzerDao.getByQualifiedNames(new ArrayList<>(qualifiedNames)));
	}

	private List<SynonymSet> loadSynonymSets(SearchConfiguration config) {
		if (config == null || config.getSynonymSets() == null || config.getSynonymSets().isEmpty()) {
			return Collections.emptyList();
		}
		return new ArrayList<>(synonymSetDao.getByQualifiedNames(config.getSynonymSets()).values());
	}

	private List<ColumnAnalyzerOverride> loadColumnAnalyzerOverrides(SearchConfiguration config) {
		if (config == null || config.getColumnAnalyzerOverrides() == null
				|| config.getColumnAnalyzerOverrides().isEmpty()) {
			return Collections.emptyList();
		}
		return new ArrayList<>(columnAnalyzerOverrideDao.getByQualifiedNames(
				config.getColumnAnalyzerOverrides()).values());
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
		}
	}
}
