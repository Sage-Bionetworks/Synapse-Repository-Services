package org.sagebionetworks.repo.manager.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.search.ColumnAnalyzerOverrideDao;
import org.sagebionetworks.repo.model.dbo.search.SynonymSetDao;
import org.sagebionetworks.repo.model.dbo.search.TextAnalyzerDao;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.search.FacetRequest;
import org.sagebionetworks.repo.model.search.KeyRange;
import org.sagebionetworks.repo.model.search.KeyValues;
import org.sagebionetworks.repo.model.search.SearchFieldValue;
import org.sagebionetworks.repo.model.search.SearchHit;
import org.sagebionetworks.repo.model.search.SortField;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.FacetColumnResult;
import org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverride;
import org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverrideEntry;
import org.sagebionetworks.repo.model.search.table.SearchConfiguration;
import org.sagebionetworks.repo.model.search.table.SearchIndex;
import org.sagebionetworks.repo.model.search.table.SearchIndexQuery;
import org.sagebionetworks.repo.model.search.SearchQueryPart;
import org.sagebionetworks.repo.model.search.table.SearchIndexState;
import org.sagebionetworks.repo.model.search.table.SearchIndexStatus;
import org.sagebionetworks.repo.model.search.SearchQuery;
import org.sagebionetworks.repo.model.search.SearchQueryType;
import org.sagebionetworks.repo.model.search.SearchQueryResults;
import org.sagebionetworks.repo.model.search.table.TextAnalyzer;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.QueryTranslator;
import org.sagebionetworks.table.query.model.SqlContext;
import org.sagebionetworks.table.cluster.description.IndexDescription;
import org.sagebionetworks.table.cluster.search.SearchIndexStatusDao;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

@Service
public class SearchIndexQueryManagerImpl implements SearchIndexQueryManager {

	private static final String INDEX_PREFIX = "search-index-";

	private final EntityManager entityManager;
	private final ConnectionFactory connectionFactory;
	private final OpenSearchManager openSearchManager;
	private final SearchConfigurationResolver searchConfigurationResolver;
	private final UserManager userManager;
	private final TableManagerSupport tableManagerSupport;
	private final ColumnAnalyzerOverrideDao columnAnalyzerOverrideDao;
	private final SynonymSetDao synonymSetDao;
	private final TextAnalyzerDao textAnalyzerDao;

	public SearchIndexQueryManagerImpl(EntityManager entityManager,
			ConnectionFactory connectionFactory,
			OpenSearchManager openSearchManager,
			SearchConfigurationResolver searchConfigurationResolver,
			UserManager userManager,
			TableManagerSupport tableManagerSupport,
			ColumnAnalyzerOverrideDao columnAnalyzerOverrideDao,
			SynonymSetDao synonymSetDao,
			TextAnalyzerDao textAnalyzerDao) {
		this.entityManager = entityManager;
		this.connectionFactory = connectionFactory;
		this.openSearchManager = openSearchManager;
		this.searchConfigurationResolver = searchConfigurationResolver;
		this.userManager = userManager;
		this.tableManagerSupport = tableManagerSupport;
		this.columnAnalyzerOverrideDao = columnAnalyzerOverrideDao;
		this.synonymSetDao = synonymSetDao;
		this.textAnalyzerDao = textAnalyzerDao;
	}

	@Override
	public SearchQueryResults search(UserInfo user, SearchIndexQuery request) {
		return executeQuery(user, request, false);
	}

	@Override
	public SearchQueryResults autocomplete(UserInfo user, SearchIndexQuery request) {
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getSearchQuery(), "request.searchQuery");
		return executeQuery(user, request, true);
	}

	SearchQueryResults executeQuery(UserInfo user, SearchIndexQuery request, boolean isAutocomplete) {
		ValidateArgument.required(user, "user");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getSearchIndexId(), "request.searchIndexId");
		ValidateArgument.required(request.getSearchQuery(), "request.searchQuery");

		String searchIndexId = request.getSearchIndexId();
		SearchQuery query = request.getSearchQuery();
		Set<SearchQueryPart> parts = resolveRequestedParts(request.getResponseParts());

		SearchIndex searchIndex = entityManager.getEntity(user, searchIndexId, SearchIndex.class);

		String definingSQL = searchIndex.getDefiningSQL();
		List<IdAndVersion> sourceTableIds = TableModelUtils.getSourceTableIds(definingSQL);
		IdAndVersion sourceEntityId = sourceTableIds.get(0);
		// TODO: Copy-pasted from TableQueryManagerImpl.queryPreflight — READ on the source entity
		// (plus DOWNLOAD if it's a table) applied recursively across the IndexDescription. Remove
		// this duplication when row-level filtering lands and unifies the auth gate between the
		// table-query and search-query paths.
		IndexDescription indexDescription = tableManagerSupport.getIndexDescription(sourceEntityId);
		tableManagerSupport.validateTableReadAccess(user, indexDescription);

		checkIndexStatus(searchIndexId);
		Optional<SearchConfiguration> configOpt = searchConfigurationResolver.resolve(
				user, searchIndex.getSearchConfigurationId(), searchIndex.getParentId());
		SearchConfiguration config = configOpt.orElse(null);

		QueryMetadata metadata = buildQueryMetadata(IdAndVersion.parse(searchIndexId));
		List<ColumnModel> columns = metadata.getColumns();

		// Build name↔ID translation maps
		Map<String, String> nameToId = columns.stream()
				.collect(Collectors.toMap(ColumnModel::getName, ColumnModel::getId, (a, b) -> a));
		Map<String, String> idToName = columns.stream()
				.collect(Collectors.toMap(ColumnModel::getId, ColumnModel::getName, (a, b) -> a));

		// Auto-populate queryFields for autocomplete with all text and link columns
		if (isAutocomplete && (query.getQueryFields() == null || query.getQueryFields().isEmpty())) {
			query.setQueryFields(getSearchableColumnNames(columns));
		}

		// When facets aren't in the response parts, skip building/translating them entirely.
		if (!parts.contains(SearchQueryPart.FACETS)) {
			query.setFacetRequests(null);
		}

		// Snapshot the user-facing returnFields BEFORE translateQueryNamesToIds rewrites them
		// to column IDs in place. The response's selectColumns filter operates on user-facing
		// names, so the snapshot is what we need below.
		List<String> originalReturnFields = query.getReturnFields() == null
				? null
				: new ArrayList<>(query.getReturnFields());

		// Translate user-facing column names to IDs before sending to OpenSearch
		translateQueryNamesToIds(query, nameToId);

		// Load overrides and analyzers needed for field routing
		List<ColumnAnalyzerOverride> overrides = loadColumnAnalyzerOverrides(config);
		Map<String, TextAnalyzer> analyzers = collectAndLoadAnalyzers(config, overrides, columns);
		String defaultAnalyzer = config != null ? config.getDefaultAnalyzer() : null;

		SearchQueryResults rawResults;
		if (isAutocomplete) {
			rawResults = openSearchManager.autocomplete(getIndexName(searchIndexId), query, columns, defaultAnalyzer, overrides, analyzers, parts);
		} else {
			rawResults = openSearchManager.search(getIndexName(searchIndexId), query, columns, defaultAnalyzer, overrides, analyzers, parts);
		}

		// Translate column IDs back to names in the results before assembling the response.
		translateResultIdsToNames(rawResults, idToName);

		// Defense-in-depth: gate every opt-in field by the resolved parts even though
		// OpenSearchManager already obeys them. Keeps the response contract crisp regardless
		// of upstream behavior, and makes the per-part wiring obvious to readers.
		SearchQueryResults results = new SearchQueryResults().setOffset(rawResults.getOffset());
		if (parts.contains(SearchQueryPart.HITS)) {
			results.setHits(rawResults.getHits());
		}
		if (parts.contains(SearchQueryPart.TOTAL_HITS)) {
			results.setTotalHits(rawResults.getTotalHits());
		}
		if (parts.contains(SearchQueryPart.FACETS)) {
			results.setFacets(rawResults.getFacets());
		}
		// SELECT_COLUMNS is a manager-layer addition (not produced by OpenSearch), so set it here.
		if (parts.contains(SearchQueryPart.SELECT_COLUMNS)) {
			results.setSelectColumns(filterSelectColumnsForReturnFields(
					metadata.getSelectColumns(), originalReturnFields));
		}
		return results;
	}

	/**
	 * Resolves the caller's requested response parts. A null or empty set means
	 * "default minimal payload" (just {@link SearchQueryPart#HITS}); otherwise the
	 * input is returned as an {@link EnumSet} for O(1) {@code contains} lookups.
	 *
	 * <p>The default-minimal behavior matches what most callers want — they ask
	 * for hits, and only opt in to extras like total count or facets when needed.
	 * It also keeps the OpenSearch request lean for the common case (no aggregations,
	 * no total-hit tracking).
	 */
	static Set<SearchQueryPart> resolveRequestedParts(Set<SearchQueryPart> requested) {
		if (requested == null || requested.isEmpty()) {
			return EnumSet.of(SearchQueryPart.HITS);
		}
		return EnumSet.copyOf(requested);
	}

	/**
	 * Filter a SELECT-clause {@link SelectColumn} list down to entries whose names
	 * match {@code returnFields}. When {@code returnFields} is null or empty, the
	 * original list is returned unchanged. Unknown names are silently dropped; the
	 * SELECT-clause order is preserved.
	 */
	List<SelectColumn> filterSelectColumnsForReturnFields(List<SelectColumn> selectColumns, List<String> returnFields) {
		if (selectColumns == null) {
			return null;
		}
		if (returnFields == null || returnFields.isEmpty()) {
			return selectColumns;
		}
		Set<String> allowed = new HashSet<>(returnFields);
		return selectColumns.stream()
				.filter(sc -> allowed.contains(sc.getName()))
				.collect(Collectors.toList());
	}

	/**
	 * Returns the names of all text and link columns that are searchable for autocomplete.
	 */
	List<String> getSearchableColumnNames(List<ColumnModel> columns) {
		List<String> names = new ArrayList<>();
		for (ColumnModel column : columns) {
			if (ColumnTypeToOpenSearchMapping.isTextType(column.getColumnType())
					|| ColumnTypeToOpenSearchMapping.isLinkType(column.getColumnType())) {
				names.add(column.getName());
			}
		}
		return names;
	}

	/**
	 * Loads the bound {@link ColumnModel} list for the SearchIndex and the parallel
	 * {@link SelectColumn} list used by response serialization.
	 */
	QueryMetadata buildQueryMetadata(IdAndVersion searchIndexIdAndVersion) {
		List<ColumnModel> columns = tableManagerSupport.getTableSchema(searchIndexIdAndVersion);
		if (columns == null || columns.isEmpty()) {
			throw new IllegalStateException("SearchIndex " + searchIndexIdAndVersion
					+ " has no bound schema — update the entity to re-register.");
		}
		return new QueryMetadata(columns, TableModelUtils.getSelectColumns(columns));
	}

	Map<String, TextAnalyzer> collectAndLoadAnalyzers(SearchConfiguration config,
			List<ColumnAnalyzerOverride> overrides, List<ColumnModel> columns) {
		Set<String> qualifiedNames = new HashSet<>();

		// SCIENTIFIC is always needed for keyword .searchable sub-fields
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

	List<ColumnAnalyzerOverride> loadColumnAnalyzerOverrides(SearchConfiguration config) {
		if (config == null || config.getColumnAnalyzerOverrides() == null
				|| config.getColumnAnalyzerOverrides().isEmpty()) {
			return Collections.emptyList();
		}
		return new ArrayList<>(columnAnalyzerOverrideDao.getByQualifiedNames(
				config.getColumnAnalyzerOverrides()).values());
	}

	void checkIndexStatus(String searchIndexId) {
		SearchIndexStatusDao statusDao = connectionFactory.getSearchIndexStatusDao();
		Optional<SearchIndexStatus> statusOpt = statusDao.getStatus(KeyFactory.stringToKey(searchIndexId));
		if (statusOpt.isEmpty() || statusOpt.get().getState() == SearchIndexState.CREATING) {
			throw new IllegalStateException("Search index is still building. Please try again later.");
		}
		if (statusOpt.get().getState() == SearchIndexState.FAILED) {
			String storedError = statusOpt.get().getErrorMessage();
			String detail = storedError == null || storedError.isBlank()
					? "Delete or update the SearchIndex to trigger a rebuild."
					: storedError;
			throw new IllegalArgumentException(
					"Search index build failed: " + detail
							+ " Delete or update the SearchIndex to trigger a rebuild.");
		}
	}

	String getIndexName(String entityId) {
		return INDEX_PREFIX + entityId;
	}

	/**
	 * Translates all user-facing column name references in a SearchQuery to column IDs.
	 * Handles boost syntax in queryFields (e.g., "name^3" becomes "id^3").
	 */
	void translateQueryNamesToIds(SearchQuery query, Map<String, String> nameToId) {
		if (query.getQueryFields() != null) {
			query.setQueryFields(query.getQueryFields().stream()
					.map(field -> translateFieldWithBoost(field, nameToId))
					.collect(Collectors.toList()));
		}

		translateKeyed(query.getTermsFilters(), KeyValues::getKey, KeyValues::setKey, nameToId);
		translateKeyed(query.getRangeFilters(), KeyRange::getKey, KeyRange::setKey, nameToId);
		translateKeyed(query.getFacetRequests(), FacetRequest::getColumnName, FacetRequest::setColumnName, nameToId);

		query.setExistsFilters(translateNames(query.getExistsFilters(), nameToId));
		query.setNotExistsFilters(translateNames(query.getNotExistsFilters(), nameToId));
		query.setReturnFields(translateNames(query.getReturnFields(), nameToId));

		if (query.getSort() != null) {
			for (SortField sf : query.getSort()) {
				if (!"_score".equals(sf.getColumnName())) {
					String id = nameToId.get(sf.getColumnName());
					if (id != null) {
						sf.setColumnName(id);
					}
				}
			}
		}
	}

	String translateFieldWithBoost(String field, Map<String, String> nameToId) {
		int boostIdx = field.indexOf('^');
		if (boostIdx >= 0) {
			String name = field.substring(0, boostIdx);
			String boost = field.substring(boostIdx);
			String id = nameToId.get(name);
			return (id != null ? id : name) + boost;
		}
		String id = nameToId.get(field);
		return id != null ? id : field;
	}

	private <T> void translateKeyed(List<T> items, Function<T, String> getKey,
			BiConsumer<T, String> setKey, Map<String, String> nameToId) {
		if (items == null) {
			return;
		}
		for (T item : items) {
			String id = nameToId.get(getKey.apply(item));
			if (id != null) {
				setKey.accept(item, id);
			}
		}
	}

	List<String> translateNames(List<String> names, Map<String, String> nameToId) {
		if (names == null) {
			return null;
		}
		return names.stream()
				.map(name -> nameToId.getOrDefault(name, name))
				.collect(Collectors.toList());
	}

	/**
	 * Translates column IDs back to user-facing names in the search results.
	 * Handles field keys, highlight keys (stripping .searchable suffix), and facet column names.
	 */
	void translateResultIdsToNames(SearchQueryResults results, Map<String, String> idToName) {
		if (results.getHits() != null) {
			for (SearchHit hit : results.getHits()) {
				translateHitIdsToNames(hit, idToName);
			}
		}
		translateKeyed(results.getFacets(), FacetColumnResult::getColumnName, FacetColumnResult::setColumnName, idToName);
	}

	void translateHitIdsToNames(SearchHit hit, Map<String, String> idToName) {
		translateKeyed(hit.getFields(), SearchFieldValue::getName, SearchFieldValue::setName, idToName);
		if (hit.getHighlights() != null) {
			for (SearchFieldValue hv : hit.getHighlights()) {
				String key = hv.getName();
				if (key.endsWith(".searchable")) {
					key = key.substring(0, key.length() - ".searchable".length());
				}
				String name = idToName.get(key);
				hv.setName(name != null ? name : key);
			}
		}
	}

	/**
	 * Holder for the two parallel column views produced from a single
	 * {@link QueryTranslator} build: the full {@link ColumnModel} list used for
	 * analyzer routing and name/ID translation, and the parallel
	 * {@link SelectColumn} list surfaced in the response.
	 */
	static final class QueryMetadata {
		private final List<ColumnModel> columns;
		private final List<SelectColumn> selectColumns;

		QueryMetadata(List<ColumnModel> columns, List<SelectColumn> selectColumns) {
			this.columns = columns;
			this.selectColumns = selectColumns;
		}

		List<ColumnModel> getColumns() {
			return columns;
		}

		List<SelectColumn> getSelectColumns() {
			return selectColumns;
		}
	}
}
