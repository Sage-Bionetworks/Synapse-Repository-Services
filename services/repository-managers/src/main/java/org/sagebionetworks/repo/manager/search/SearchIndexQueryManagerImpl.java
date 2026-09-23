package org.sagebionetworks.repo.manager.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.search.SourceConfig;
import org.opensearch.client.opensearch.core.search.SourceFilter;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager;
import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager.TableIdAndType;
import org.sagebionetworks.repo.manager.table.BenefactorAccessFilter;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableQueryManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.search.SearchQuery;
import org.sagebionetworks.repo.model.search.SearchQueryPart;
import org.sagebionetworks.repo.model.search.SearchQueryResults;
import org.sagebionetworks.repo.model.search.table.SearchAutocompleteRequest;
import org.sagebionetworks.repo.model.search.table.SearchIndex;
import org.sagebionetworks.repo.model.search.table.SearchIndexQuery;
import org.sagebionetworks.repo.model.search.table.SearchIndexState;
import org.sagebionetworks.repo.model.search.table.SearchIndexStatus;
import org.sagebionetworks.repo.model.table.ColumnLineageEntry;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.IndexAuthorizationSnapshot;
import org.sagebionetworks.repo.model.table.IndexDescriptionSnapshot;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.SourceDependency;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.QueryTranslator;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.description.BenefactorDescription;
import org.sagebionetworks.table.cluster.search.SearchIndexStatusDao;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

@Service
public class SearchIndexQueryManagerImpl implements SearchIndexQueryManager {

	private static final String INDEX_PREFIX = "search-index-";
	private static final String STILL_BUILDING_MESSAGE = "Search index is still building. Please try again later.";
	private static final String INDEX_NOT_FOUND_EXCEPTION = "index_not_found_exception";

	private final EntityManager entityManager;
	private final EntityAuthorizationManager entityAuthorizationManager;
	private final ColumnModelManager columnModelManager;
	private final ConnectionFactory connectionFactory;
	private final OpenSearchManager openSearchManager;
	private final TableQueryManager tableQueryManager;

	public SearchIndexQueryManagerImpl(EntityManager entityManager,
			EntityAuthorizationManager entityAuthorizationManager,
			ColumnModelManager columnModelManager,
			ConnectionFactory connectionFactory,
			OpenSearchManager openSearchManager,
			TableQueryManager tableQueryManager) {
		this.entityManager = entityManager;
		this.entityAuthorizationManager = entityAuthorizationManager;
		this.columnModelManager = columnModelManager;
		this.connectionFactory = connectionFactory;
		this.openSearchManager = openSearchManager;
		this.tableQueryManager = tableQueryManager;
	}

	@Override
	public SearchQueryResults search(UserInfo user, SearchIndexQuery request) {
		ValidateArgument.required(user, "user");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getSearchIndexId(), "request.searchIndexId");
		ValidateArgument.required(request.getSearchQuery(), "request.searchQuery");

		String searchIndexId = request.getSearchIndexId();
		SearchQuery body = request.getSearchQuery();
		Set<SearchQueryPart> parts = resolveRequestedParts(request.getResponseParts());

		entityManager.getEntity(user, searchIndexId, SearchIndex.class);

		SourceFilter sourceFilter = parts.contains(SearchQueryPart.SELECT_COLUMNS)
				? extractSourceFilter(body)
				: null;

		return queryLiveIndex(user, searchIndexId, target -> shapeResults(
				openSearchManager.search(target.physicalIndex(), body, target.metadata().getColumns(), parts,
						target.accessFilters()),
				parts, target.metadata(), sourceFilter));
	}

	@Override
	public SearchQueryResults autocomplete(UserInfo user, SearchAutocompleteRequest request) {
		ValidateArgument.required(user, "user");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getSearchIndexId(), "request.searchIndexId");
		ValidateArgument.required(request.getSearchQuery(), "request.searchQuery");

		String searchIndexId = request.getSearchIndexId();
		entityManager.getEntity(user, searchIndexId, SearchIndex.class);

		Set<SearchQueryPart> parts = EnumSet.of(SearchQueryPart.HITS);
		return queryLiveIndex(user, searchIndexId, target -> {
			SearchQueryResults rawResults = openSearchManager.autocomplete(target.physicalIndex(),
					request.getSearchQuery(), target.metadata().getColumns(), parts, target.accessFilters());
			return new SearchQueryResults()
					.setOffset(rawResults.getOffset())
					.setHits(rawResults.getHits());
		});
	}

	/**
	 * Run {@code query} against the physical index currently behind the SearchIndex's alias,
	 * authorized and filtered by the snapshot that physical index was built with.
	 */
	private SearchQueryResults queryLiveIndex(UserInfo user, String searchIndexId,
			Function<LiveQueryTarget, SearchQueryResults> query) {
		try {
			return query.apply(resolveLiveQueryTarget(user, searchIndexId));
		} catch (IllegalStateException e) {
			if (!isIndexNotFound(e)) {
				throw e;
			}
			// The resolved physical index was deleted after an alias swap retired it; the alias now
			// points at a newer physical index with its own snapshot, so resolve and authorize again.
			return query.apply(resolveLiveQueryTarget(user, searchIndexId));
		}
	}

	private LiveQueryTarget resolveLiveQueryTarget(UserInfo user, String searchIndexId) {
		Optional<OpenSearchManager.LiveIndex> liveIndexOpt = openSearchManager.getLiveIndex(getIndexName(searchIndexId));
		if (liveIndexOpt.isEmpty()) {
			// Nothing is served yet, so there is no snapshot to authorize against; the build status is
			// the only useful answer (a failed build surfaces its stored error).
			checkIndexStatus(searchIndexId);
			throw new IllegalStateException(STILL_BUILDING_MESSAGE);
		}
		OpenSearchManager.LiveIndex liveIndex = liveIndexOpt.get();
		IndexAuthorizationSnapshot snapshot = liveIndex.snapshot();
		IndexDescriptionSnapshot source = snapshot.getIndexDescription();
		// Authorize before consulting the build status so a caller without access to the served
		// source never sees status detail.
		entityAuthorizationManager.canQueryTableOrView(user, collectTableNodes(source)).checkAuthorizationOrElseThrow();
		checkIndexStatus(searchIndexId);
		return new LiveQueryTarget(liveIndex.physicalIndex(), buildQueryMetadata(snapshot.getColumnLineage()),
				buildBenefactorAccessFilters(user, source));
	}

	private static boolean isIndexNotFound(IllegalStateException e) {
		return e.getCause() instanceof OpenSearchException cause && cause.error() != null
				&& INDEX_NOT_FOUND_EXCEPTION.equals(cause.error().type());
	}

	/**
	 * The as-built source followed by each of its flattened transitive dependencies, as the
	 * (id, type) nodes of a single table-query authorization decision.
	 */
	static List<TableIdAndType> collectTableNodes(IndexDescriptionSnapshot source) {
		List<TableIdAndType> nodes = new ArrayList<>(source.getDependencies().size() + 1);
		nodes.add(toTableNode(source.getObjectId(), source.getTableType()));
		for (SourceDependency dependency : source.getDependencies()) {
			nodes.add(toTableNode(dependency.getObjectId(), dependency.getTableType()));
		}
		return nodes;
	}

	private static TableIdAndType toTableNode(String objectId, String tableType) {
		return new TableIdAndType(KeyFactory.stringToKey(objectId).toString(), TableType.valueOf(tableType));
	}

	/**
	 * Build the per-dependency benefactor access filters for the as-built source. For each
	 * benefactor column (in snapshot order, which matches the {@code _benefactor_i} field naming
	 * written at build time), resolve the benefactors the user can READ, always including the
	 * {@code -1} sentinel (the default for rows with no benefactor), and produce a {@code terms}
	 * filter on field {@code _benefactor_i}. The filters are AND-ed at query time, so a document
	 * is returned only if the user can read every source dependency's benefactor. Returns an
	 * empty list for a benefactor-less source (e.g. a table), applying no row filter; access to
	 * such a source is enforced at the entity level.
	 */
	List<Query> buildBenefactorAccessFilters(UserInfo user, IndexDescriptionSnapshot source) {
		if (source.getBenefactors().isEmpty()) {
			return Collections.emptyList();
		}
		IdAndVersion sourceId = IdAndVersion.newBuilder()
				.setId(KeyFactory.stringToKey(source.getObjectId()))
				.setVersion(source.getVersionNumber())
				.build();
		List<BenefactorDescription> benefactors = source.getBenefactors().stream()
				.map(b -> new BenefactorDescription(b.getBenefactorColumnName(), ObjectType.valueOf(b.getBenefactorType())))
				.collect(Collectors.toList());
		TableIndexDAO indexDao = connectionFactory.getConnection(sourceId);
		// Shared with the table-query SQL row-level filter so both gates compute accessibility
		// identically (including the -1 sentinel).
		List<BenefactorAccessFilter> accessibleBenefactors =
				tableQueryManager.computeAccessibleBenefactors(user, sourceId, benefactors, indexDao, ACCESS_TYPE.READ);
		List<Query> filters = new ArrayList<>(accessibleBenefactors.size());
		for (int i = 0; i < accessibleBenefactors.size(); i++) {
			final String field = "_benefactor_" + i;
			final Set<Long> terms = accessibleBenefactors.get(i).accessibleIds();
			filters.add(Query.of(tq -> tq.terms(t -> t
					.field(field)
					.terms(qt -> qt.value(terms.stream()
							.map(FieldValue::of)
							.collect(Collectors.toList()))))));
		}
		return filters;
	}

	private SearchQueryResults shapeResults(SearchQueryResults rawResults, Set<SearchQueryPart> parts,
			QueryMetadata metadata, SourceFilter sourceFilter) {
		SearchQueryResults results = new SearchQueryResults().setOffset(rawResults.getOffset());
		if (parts.contains(SearchQueryPart.HITS)) {
			results.setHits(rawResults.getHits());
			results.setNextSearchAfter(rawResults.getNextSearchAfter());
		}
		if (parts.contains(SearchQueryPart.TOTAL_HITS)) {
			results.setTotalHits(rawResults.getTotalHits());
		}
		// Aggregations are scoped by the caller supplying body.aggregations, not a
		// SearchQueryPart bit. The raw field is null when not requested.
		results.setAggregationResults(rawResults.getAggregationResults());
		if (parts.contains(SearchQueryPart.SELECT_COLUMNS)) {
			results.setSelectColumns(filterSelectColumnsForSourceFilter(
					metadata.getSelectColumns(), sourceFilter));
		}
		return results;
	}

	/**
	 * Resolves the caller's requested response parts. A null or empty set means
	 * "default minimal payload" (just {@link SearchQueryPart#HITS}); otherwise the
	 * input is returned as an {@link EnumSet} for O(1) {@code contains} lookups.
	 */
	static Set<SearchQueryPart> resolveRequestedParts(Set<SearchQueryPart> requested) {
		if (requested == null || requested.isEmpty()) {
			return EnumSet.of(SearchQueryPart.HITS);
		}
		return EnumSet.copyOf(requested);
	}

	/**
	 * Parse the caller-supplied {@code body._source} into the OpenSearch typed
	 * {@link SourceFilter}. Returns null when no filter is supplied or the body itself is null.
	 * The typed {@code {includes, excludes}} schema is the native {@code SourceFilter} shape, so
	 * it deserializes straight through.
	 */
	static SourceFilter extractSourceFilter(SearchQuery body) {
		org.sagebionetworks.repo.model.search.dsl.SourceFilter source =
				body == null ? null : body.get_source();
		if (source == null) {
			return null;
		}
		SourceConfig sourceConfig = SearchOpaqueJsonUtil.fromJsonpTree(
				SearchOpaqueJsonUtil.parse(source), SourceConfig._DESERIALIZER);
		return sourceConfig.isFilter() ? sourceConfig.filter() : null;
	}

	/**
	 * Filter a SELECT-clause {@link SelectColumn} list to honor the caller's
	 * {@code _source} filter. A column survives if it matches {@code includes} (or
	 * {@code includes} is empty/absent) AND is not in {@code excludes}. When
	 * {@code filter} is null or has neither includes nor excludes, the original list
	 * is returned unchanged. Unknown names are silently dropped; the SELECT-clause
	 * order is preserved.
	 */
	List<SelectColumn> filterSelectColumnsForSourceFilter(List<SelectColumn> selectColumns, SourceFilter filter) {
		if (selectColumns == null) {
			return null;
		}
		if (filter == null) {
			return selectColumns;
		}
		List<String> includes = filter.includes();
		List<String> excludes = filter.excludes();
		boolean hasIncludes = includes != null && !includes.isEmpty();
		boolean hasExcludes = excludes != null && !excludes.isEmpty();
		if (!hasIncludes && !hasExcludes) {
			return selectColumns;
		}
		Set<String> includeSet = hasIncludes ? new HashSet<>(includes) : null;
		Set<String> excludeSet = hasExcludes ? new HashSet<>(excludes) : Collections.emptySet();
		return selectColumns.stream()
				.filter(sc -> includeSet == null || includeSet.contains(sc.getName()))
				.filter(sc -> !excludeSet.contains(sc.getName()))
				.collect(Collectors.toList());
	}

	/**
	 * Loads the {@link ColumnModel} list of the indexed output columns named by the as-built
	 * column lineage (in lineage order) and the parallel {@link SelectColumn} list used by
	 * response serialization.
	 */
	QueryMetadata buildQueryMetadata(List<ColumnLineageEntry> columnLineage) {
		List<String> columnIds = columnLineage.stream().map(ColumnLineageEntry::getOutputColumnId)
				.collect(Collectors.toList());
		List<ColumnModel> columns = columnModelManager.getAndValidateColumnModels(columnIds);
		return new QueryMetadata(columns, TableModelUtils.getSelectColumns(columns));
	}

	void checkIndexStatus(String searchIndexId) {
		SearchIndexStatusDao statusDao = connectionFactory.getSearchIndexStatusDao();
		Optional<SearchIndexStatus> statusOpt = statusDao.getStatus(KeyFactory.stringToKey(searchIndexId));
		if (statusOpt.isEmpty() || statusOpt.get().getState() == SearchIndexState.CREATING) {
			throw new IllegalStateException(STILL_BUILDING_MESSAGE);
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
	 * A physical index resolved from the SearchIndex's alias, with the columns and row-level
	 * access filters derived from the snapshot that physical index was built with.
	 */
	private record LiveQueryTarget(String physicalIndex, QueryMetadata metadata, List<Query> accessFilters) {
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
