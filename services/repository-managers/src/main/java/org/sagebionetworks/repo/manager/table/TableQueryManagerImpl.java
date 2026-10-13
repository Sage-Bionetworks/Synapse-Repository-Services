package org.sagebionetworks.repo.manager.table;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager;
import org.sagebionetworks.repo.manager.table.query.ActionsRequiredQuery;
import org.sagebionetworks.repo.manager.table.query.AggregateQidColumnResolver;
import org.sagebionetworks.repo.manager.table.query.AggregateQidQueryValidator;
import org.sagebionetworks.repo.manager.table.query.BasicQuery;
import org.sagebionetworks.repo.manager.table.query.CacheableQueryExecutor;
import org.sagebionetworks.repo.manager.table.query.CohortCapture;
import org.sagebionetworks.repo.manager.table.query.CohortQueryValidator;
import org.sagebionetworks.repo.manager.table.query.CountQuery;
import org.sagebionetworks.repo.manager.table.query.FacetQueries;
import org.sagebionetworks.repo.manager.table.query.QueryContext;
import org.sagebionetworks.repo.manager.table.query.QueryExecutor;
import org.sagebionetworks.repo.manager.table.query.QueryTranslations;
import org.sagebionetworks.repo.manager.table.query.SnapshotSchemaProvider;
import org.sagebionetworks.repo.manager.table.query.StreamingQueryExecutor;
import org.sagebionetworks.repo.manager.table.query.SumFileSizesQuery;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AggregateDataConfiguration;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.FacetPostProcessingConfig;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.dbo.file.download.v2.ActionsRequiredDao;
import org.sagebionetworks.repo.model.dbo.file.download.v2.EntityActionRequiredCallback;
import org.sagebionetworks.repo.model.dbo.file.download.v2.FilesBatchProvider;
import org.sagebionetworks.repo.model.download.ActionRequiredCount;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.semaphore.LockContext;
import org.sagebionetworks.repo.model.semaphore.LockContext.ContextType;
import org.sagebionetworks.repo.model.table.CohortDefinition;
import org.sagebionetworks.repo.model.table.ColumnCohortFilter;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.DownloadFromTableRequest;
import org.sagebionetworks.repo.model.table.DownloadFromTableResult;
import org.sagebionetworks.repo.model.table.FacetColumnResult;
import org.sagebionetworks.repo.model.table.FacetColumnResultRange;
import org.sagebionetworks.repo.model.table.IndexAuthorizationSnapshot;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryFilter;
import org.sagebionetworks.repo.model.table.QueryNextPageToken;
import org.sagebionetworks.repo.model.table.QueryOptions;
import org.sagebionetworks.repo.model.table.QueryResult;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.RowSuppressionReasonCode;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.SumFileSizes;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.web.BelowThresholdException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.RowSuppressionException;
import org.sagebionetworks.table.cluster.CachedQueryRequest;
import org.sagebionetworks.table.cluster.CombinedQuery;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.QueryTranslator;
import org.sagebionetworks.table.cluster.ResolvedCohort;
import org.sagebionetworks.table.cluster.SchemaProvider;
import org.sagebionetworks.table.cluster.TableAndColumnMapper;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.columntranslation.ColumnTranslationReference;
import org.sagebionetworks.table.cluster.description.BenefactorDescription;
import org.sagebionetworks.table.cluster.description.IndexDescription;
import org.sagebionetworks.table.cluster.description.QueryIndexDescription;
import org.sagebionetworks.table.cluster.description.SnapshotIndexDescription;
import org.sagebionetworks.table.cluster.description.VirtualTableIndexDescription;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.CohortReference;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.InPredicate;
import org.sagebionetworks.table.query.model.Pagination;
import org.sagebionetworks.table.query.model.QueryExpression;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.TextMatchesPredicate;
import org.sagebionetworks.table.query.model.WhereClause;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.util.csv.CSVWriterStream;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.util.progress.ProgressingCallable;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;

public class TableQueryManagerImpl implements TableQueryManager {

	public static final int CACHED_QUERY_EXPIRES_IN_SEC = 60*5;
	public static final long MAX_ROWS_PER_CALL = 100;
	public static final long ACTIONS_REQUIRED_BATCH_SIZE = 10_000;
	public static final long MAX_ACTIONS_REQUIRED = 50;

	/**
	 * Why a query runs. Package-private so that no request can select any mode but {@link #STANDARD}.
	 */
	enum QueryMode {
		/** A query whose results are returned to the caller. */
		STANDARD,
		/** A request-scoped cohort query whose values are captured server-side and never returned. */
		COHORT_CAPTURE
	}

	private TableManagerSupport tableManagerSupport;
	private ConnectionFactory tableConnectionFactory;
	private EntityAuthorizationManager entityAuthorizationManager;
	private ExecutorService threadPool;
	private QueryCacheManager queryCacheManager;
	private FacetPostProcessorProvider facetPostProcessorProvider;
	private IndexAuthorizationSnapshotManager indexAuthorizationSnapshotManager;
	private AggregateQidColumnResolver aggregateQidColumnResolver;
	private StackConfiguration stackConfiguration;

	@Autowired
	public TableQueryManagerImpl(TableManagerSupport tableManagerSupport, ConnectionFactory tableConnectionFactory, EntityAuthorizationManager entityAuthorizationManager, ExecutorService cachedThreadPool, QueryCacheManager queryCacheManager, FacetPostProcessorProvider facetPostProcessorProvider, IndexAuthorizationSnapshotManager indexAuthorizationSnapshotManager, AggregateQidColumnResolver aggregateQidColumnResolver, StackConfiguration stackConfiguration) {
		this.tableManagerSupport = tableManagerSupport;
		this.tableConnectionFactory = tableConnectionFactory;
		this.entityAuthorizationManager = entityAuthorizationManager;
		this.threadPool = cachedThreadPool;
		this.queryCacheManager = queryCacheManager;
		this.facetPostProcessorProvider = facetPostProcessorProvider;
		this.indexAuthorizationSnapshotManager = indexAuthorizationSnapshotManager;
		this.aggregateQidColumnResolver = aggregateQidColumnResolver;
		this.stackConfiguration = stackConfiguration;
	}
	
	/**
	 * Injected via spring
	 */
	long maxBytesPerRequest;

	public void setMaxBytesPerRequest(long maxBytesPerRequest) {
		this.maxBytesPerRequest = maxBytesPerRequest;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.manager.table.TableQueryManager#querySinglePage
	 * (org.sagebionetworks.common.util.progress.ProgressCallback,
	 * org.sagebionetworks.repo.model.UserInfo, java.lang.String, java.util.List,
	 * java.lang.Long, java.lang.Long, boolean, boolean, boolean)
	 */
	@Override
	public QueryResultBundle querySinglePage(ProgressCallback progressCallback, UserInfo user, Query query, QueryOptions options)
			throws TableUnavailableException, TableFailedException, LockUnavilableException {
		try {
			// Set the default values
			TableQueryManagerImpl.setDefaultsValues(query);
			
			//get combined sql before pre-flight and authorization
			String combinedSql = null;
			
			if (options.returnCombinedSql()) {
				combinedSql = createCombinedSql(user, query);
			}
			
			QueryExecutor queryExecutor = new CacheableQueryExecutor(queryCacheManager, CACHED_QUERY_EXPIRES_IN_SEC);

			// Acquire the read lock, confirm the table is available, then authorize, translate, and
			// run against the served index the lock pins (see queryAfterAuthorization).
			QueryResultBundle bundle = queryAfterAuthorization(progressCallback, user, query, this.maxBytesPerRequest,
					options, QueryMode.STANDARD, (sqlQuery, status) -> {
						QueryResultBundle result = executeQuery(user, sqlQuery, options, queryExecutor);
						setConsistentQueryEtag(result, options, status);
						addNextPageTokenIfNeeded(result, sqlQuery, query, options);
						return result;
					});

			// add combined sql to the bundle
			bundle.setCombinedSql(combinedSql);

			return bundle;
		} catch (EmptyResultException e) {
			// return an empty result.
			return createEmptyBundle(e.getTableId(), options);
		} catch (IOException e) {
			// The cache-backed executor performs no stream IO, so this is unreachable here.
			throw new IllegalStateException(e);
		}

	}

	/**
	 * Combined sql query is basic input query combined with all additional filter.
	 * createCombinedSql includes the following:
	 * <ol>
	 * <li>Parse the query SQL string, and identify the tableId.</li>
	 * <li>Build SqlQuery with additional filter, offset, limit and sortList.</li>
	 * <li>Add selected facet to query</li>
	 * <li>Parse again the combined sql to replace selectList with original select list</li>
	 * </ol>
	 *
	 * @param user
	 * @param query
	 * @return
	 *
	 */
	String createCombinedSql(UserInfo user, Query query) {
		return CombinedQuery.builder()
				.setQuery(query.getSql())
				.setSchemaProvider(tableManagerSupport)
				.setOverrideOffset(query.getOffset())
				.setOverrideLimit(query.getLimit())
				.setSelectedFacets(query.getSelectedFacets())
				.setSortList(query.getSort())
				.setAdditionalFilters(query.getAdditionalFilters()).build().getCombinedSql();
	}

	/**
	 * Query pre-flight includes the following:
	 * <ol>
	 * <li>Parse the query SQL string, and identify the tableId.</li>
	 * <li>Authenticate that the user has read access on the table.</li>
	 * <li>Gather table's schema information</li>
	 * <li>Add row level filtering as needed.</li>
	 * <li>Create processed {@link QueryTranslator} that is ready for execution.</li>
	 * </ol>
	 * a
	 * 
	 * @param user
	 * @param query
	 * @param cohorts the query's resolved cohorts keyed by name, empty when it references none.
	 * @param mode    {@link QueryMode#COHORT_CAPTURE} only when the query is a cohort definition.
	 * @return
	 * @throws EmptyResultException
	 * @throws TableFailedException
	 * @throws TableUnavailableException
	 * @throws NotFoundException
	 */
	QueryTranslations queryPreflight(UserInfo user, Query query, Map<String, ResolvedCohort> cohorts, Long maxBytesPerPage,
			QueryOptions options, QueryMode mode, ACCESS_TYPE...types)
			throws EmptyResultException, NotFoundException, TableUnavailableException, TableFailedException {
		ValidateArgument.required(user, "UserInfo");
		ValidateArgument.required(query, "Query");
		ValidateArgument.required(query.getSql(), "Query");
		// 1. Parse the SQL string

		QuerySpecification model = parserQuery(query.getSql());
		// We now have the table's ID.
		String tableId = model.getSingleTableName().orElseThrow(TableConstants.JOIN_NOT_SUPPORTED_IN_THIS_CONTEXT);
		IdAndVersion idAndVersion = IdAndVersion.parse(tableId);
		// For a materialized object the as-built authorization snapshot pins exactly what the served
		// index contains, so authorization and translation run against it rather than current truth
		// (closing the PLFM-9977 drift class). A VirtualTable has no index of its own, so it is
		// resolved as a query over its dependent's as-built snapshot (see getQueryIndexDescription).
		//
		// The caller holds the table's read lock and has already confirmed the table is AVAILABLE, so
		// this snapshot matches the served index and cannot be swapped while the query runs (see
		// queryAfterAuthorization).
		QueryIndexDescription indexDescription = getQueryIndexDescription(idAndVersion);
		SchemaProvider schemaProvider = new SnapshotSchemaProvider(tableManagerSupport,
				indexAuthorizationSnapshotManager::getAuthorizationSnapshot);
		// 2. Validate the user has read access on this table. Because table queries run in
		// this pipeline, we can enforce aggregate-only access: when row-level access is
		// denied only because a source is bound to AGGREGATE_DATA, load that source's
		// configuration and query in aggregate-only mode instead of throwing.
		AuthorizationStatus readStatus = tableManagerSupport.validateTableReadAccess(user, indexDescription);
		// When row-level access is denied only because the source is bound to AGGREGATE_DATA,
		// its bound configuration downgrades the denial to an aggregate-only read.
		Optional<AggregateDataConfiguration> aggregateConfiguration = readStatus.isAuthorized()
				? Optional.empty()
				: readStatus.getAggregateDataSourceId().flatMap(tableManagerSupport::getAggregateDataConfiguration);
		AggregateDataConfiguration aggregateDataConfiguration;
		if (aggregateConfiguration.isPresent()) {
			// Aggregate-only access: the ACT-bound configuration governs the query. A
			// request-supplied preview configuration is ignored for these users.
			aggregateDataConfiguration = aggregateConfiguration.get();
		} else {
			// Either fully authorized or denied with no aggregate fallback: this preserves
			// the standard denial and is a no-op when the read is authorized.
			readStatus.checkAuthorizationOrElseThrow();
			// A full-access data manager may preview exactly what an aggregate-only user
			// would see by supplying a configuration on the request. Applying it upstream
			// makes the entire pipeline treat the query identically to a real aggregate-only
			// read; it can only further restrict the manager's own view, so it is safe.
			aggregateDataConfiguration = options.getAggregateDataPreview().orElse(null);
		}
		// A preview leaves the caller's own full access intact, so only a real aggregate-only read may
		// consume an aggregate-only cohort.
		validateAggregateOnlyCohortUse(model, query.getAdditionalFilters(), cohorts, aggregateConfiguration.isPresent(),
				indexDescription, schemaProvider);

		// 3. Get the table's schema count from the as-built schema (the snapshot's pinned column id
		// set, or the live bound schema for a VirtualTable), so an empty-schema check reflects what
		// the index actually contains.
		long count = schemaProvider.getTableSchema(idAndVersion).size();
		if (count < 1L) {
			throw new EmptyResultException("Table schema is empty for: " + tableId, tableId);
		}
		String preprocessedSql = indexDescription.preprocessQuery(query.getSql());
		QueryExpression preprocessedModel = parserQueryQuerExpression(preprocessedSql);
		for(QuerySpecification qs: preprocessedModel.createIterable(QuerySpecification.class)) {
			// 4. Add row level filter as needed.
			// Table views must have a row level filter applied to the query. Preprocessing is the
			// identity for a materialized object (the single query specification is the queried
			// object), but a VirtualTable inlines its defining SQL, so a query specification can
			// target a dependency; each specification's filter therefore resolves that object's
			// own snapshot-backed description.
			IdAndVersion qsIdAndVersion = IdAndVersion.parse(
					qs.getSingleTableName().orElseThrow(TableConstants.JOIN_NOT_SUPPORTED_IN_THIS_CONTEXT));
			QueryIndexDescription filterDescription = qsIdAndVersion.equals(idAndVersion) ? indexDescription
					: getQueryIndexDescription(qsIdAndVersion);
			addRowLevelFilter(user, qs, filterDescription, types);
		}

		// 5. When this request asks for rows against an aggregate-only source, decide whether any
		// row-level results may be returned. A request that does not ask for rows always degrades to
		// the aggregate-only response (gated count + obscured facets) and imposes no restriction.
		List<Integer> protectedCountColumnIndexes = Collections.emptyList();
		if (options.runQuery() && aggregateDataConfiguration != null) {
			List<String> quasiIdentifierColumnNames = aggregateDataConfiguration.getQuasiIdentifierColumnNames();
			if (quasiIdentifierColumnNames == null || quasiIdentifierColumnNames.isEmpty()) {
				// The source defines no quasi-identifier columns, so it can never return row-level
				// results. Reject the row request explicitly rather than silently degrading to the
				// aggregate-only response, mirroring the QID-misuse rejection below.
				throw new RowSuppressionException(RowSuppressionReasonCode.NO_QUASI_IDENTIFIERS);
			}
			// The source defines quasi-identifier (QID) columns: enforce the count-only QID
			// restriction and capture which output columns are protected participant counts. A
			// violation withholds the rows via a RowSuppressionException. The restriction is applied
			// to the queried object's own columns that the as-built lineage derives from a QID, so a
			// column that reaches a QID through renaming or a chain of objects is still recognized.
			// A cohort's selected QID values are captured server-side and never returned, so a cohort
			// is exempt; its distinct-value threshold gate takes the place of this restriction.
			if (QueryMode.STANDARD.equals(mode)) {
				Set<String> qidColumnIds = aggregateQidColumnResolver.resolve(indexDescription);
				protectedCountColumnIndexes = AggregateQidQueryValidator.validate(model, qidColumnIds, schemaProvider);
			}
		}

		QueryContext expansion = QueryContext.builder()
			.setStartingSql(preprocessedModel.toSql())
			.setUserId(user.getId())
			.setSchemaProvider(schemaProvider)
			.setIndexDescription(indexDescription)
			.setMaxBytesPerPage(maxBytesPerPage)
			.setMaxRowsPerCall(MAX_ROWS_PER_CALL)
			.setAdditionalFilters(query.getAdditionalFilters())
			.setSelectedFacets(query.getSelectedFacets())
			.setSelectFileColumn(query.getSelectFileColumn())
			.setLimit(query.getLimit())
			.setOffset(query.getOffset())
			.setSort(query.getSort())
			.setIncludeEntityEtag(query.getIncludeEntityEtag())
			.setAggregateDataConfiguration(aggregateDataConfiguration)
			.setProtectedCountColumnIndexes(protectedCountColumnIndexes)
			.setCohorts(cohorts)
		.build();

		return new QueryTranslations(expansion, options);
	}

	/**
	 * Resolve the query-time {@link IndexDescription} for a queried object. Delegates to
	 * {@link IndexAuthorizationSnapshotManager#getSnapshotIndexDescription(IdAndVersion)} to keep the
	 * query manager abstracted from snapshot resolution details.
	 *
	 * @param idAndVersion the object being queried (or a dependent inlined by a VirtualTable)
	 * @return a snapshot-backed description
	 */
	IndexDescription getQueryIndexDescription(IdAndVersion idAndVersion) {
		return indexAuthorizationSnapshotManager.getSnapshotIndexDescription(idAndVersion);
	}

	/**
	 * Resolve each request-scoped cohort of the query by running its definition as the caller.
	 *
	 * @return the resolved cohorts keyed by name; empty when the query defines none.
	 * @throws IllegalArgumentException if the references and definitions do not match, or a cohort query
	 *                                  breaks a cohort rule.
	 * @throws BelowThresholdException  if the caller has aggregate-only access to a cohort source and the
	 *                                  cohort is non-empty but smaller than the source's threshold.
	 */
	Map<String, ResolvedCohort> resolveCohorts(ProgressCallback progressCallback, UserInfo user, Query query)
			throws TableUnavailableException, TableFailedException, LockUnavilableException, IOException {
		CohortQueryValidator.validateDefinitions(parserQueryQuerExpression(query.getSql()), query,
				stackConfiguration.getTableQueryMaxCohorts());
		if (query.getCohorts() == null || query.getCohorts().isEmpty()) {
			return Collections.emptyMap();
		}
		Map<String, ResolvedCohort> cohorts = new LinkedHashMap<>();
		for (CohortDefinition cohort : query.getCohorts()) {
			cohorts.put(cohort.getName(), resolveCohort(progressCallback, user, cohort));
		}
		return cohorts;
	}

	/**
	 * Stream one cohort query through the same pipeline as any query the caller runs, so READ access,
	 * row-level filters and the aggregate-only downgrade all apply, capturing its values server-side.
	 */
	ResolvedCohort resolveCohort(ProgressCallback progressCallback, UserInfo user, CohortDefinition cohort)
			throws TableUnavailableException, TableFailedException, LockUnavilableException, IOException {
		int maxValues = stackConfiguration.getTableQueryMaxCohortValues();
		// One more than the maximum detects an oversized cohort without capturing all of it.
		Query cohortQuery = new Query().setSql(CohortQueryValidator.createCohortSql(cohort))
				.setAdditionalFilters(cohort.getQuery().getAdditionalFilters())
				.setSelectedFacets(cohort.getQuery().getSelectedFacets()).setLimit(maxValues + 1L);
		CohortCapture capture = new CohortCapture(cohort.getName());
		runQueryAsStream(progressCallback, user, cohortQuery, QueryMode.COHORT_CAPTURE, capture);
		return capture.toResolvedCohort(maxValues);
	}

	/**
	 * An aggregate-only cohort may only filter a quasi-identifier column of a query that is itself
	 * aggregate-only for the caller. The quasi-identifier rules then keep the main query from projecting,
	 * grouping or ordering the expanded column, which would otherwise reveal row by row which values
	 * satisfy the cohort's restricted condition.
	 *
	 * @param aggregateOnly true when the caller has aggregate-only access to the main query's source.
	 * @throws UnauthorizedException if an aggregate-only cohort is applied to any other column or query.
	 */
	void validateAggregateOnlyCohortUse(QuerySpecification model, List<QueryFilter> additionalFilters,
			Map<String, ResolvedCohort> cohorts, boolean aggregateOnly, QueryIndexDescription indexDescription,
			SchemaProvider schemaProvider) {
		Set<String> restricted = cohorts.values().stream().filter(ResolvedCohort::aggregateOnly)
				.map(ResolvedCohort::name).collect(Collectors.toSet());
		if (restricted.isEmpty()) {
			return;
		}
		Set<String> qidColumnIds = aggregateOnly ? aggregateQidColumnResolver.resolve(indexDescription)
				: Collections.emptySet();
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, schemaProvider);
		for (InPredicate predicate : model.createIterable(InPredicate.class)) {
			Optional<String> cohortName = predicate.getInPredicateValue().getCohortReference()
					.map(CohortReference::getName);
			if (cohortName.isPresent() && restricted.contains(cohortName.get())) {
				Optional<ColumnTranslationReference> column = predicate.getLeftHandSide()
						.getChild() instanceof ColumnReference reference ? mapper.lookupColumnReference(reference)
								: Optional.empty();
				validateAggregateOnlyCohortColumn(cohortName.get(), column, qidColumnIds);
			}
		}
		for (ColumnCohortFilter filter : CohortQueryValidator.collectCohortFilters(additionalFilters)) {
			if (restricted.contains(filter.getCohortName())) {
				validateAggregateOnlyCohortColumn(filter.getCohortName(),
						mapper.lookupColumnReferenceByName(filter.getColumnName()), qidColumnIds);
			}
		}
	}

	private static void validateAggregateOnlyCohortColumn(String cohortName, Optional<ColumnTranslationReference> column,
			Set<String> qidColumnIds) {
		boolean isQuasiIdentifier = column.flatMap(ColumnTranslationReference::getColumnId)
				.map(qidColumnIds::contains).orElse(false);
		if (!isQuasiIdentifier) {
			throw new UnauthorizedException("You have aggregate-only access to the source of cohort '" + cohortName
					+ "', so it may only filter a quasi-identifier column of a table to which you also have aggregate-only access");
		}
	}

	/**
	 * Receives the query once it has been translated under the table's read lock, together with the
	 * status captured at the availability check, and runs it. The consumer executes entirely inside
	 * the locked region, so the translated query and any handler it opens are pinned to the served
	 * index.
	 */
	@FunctionalInterface
	interface TranslatedQueryConsumer {
		QueryResultBundle apply(QueryTranslations query, TableStatus status) throws Exception;
	}

	/**
	 * The main entry point for all table queries. Any business logic that must be applied to all
	 * table queries should be applied here or lower.
	 * <p>
	 * Only SQL parsing (to extract the single table id) happens before the lock. The snapshot fetch,
	 * authorization, and translation are all deferred into the locked callback so they run against
	 * the exact index the read lock pins: acquire the read lock, confirm the table is AVAILABLE,
	 * authorize + translate against the served index, then run.
	 *
	 * @param progressCallback
	 * @param user
	 * @param query          the raw (untranslated) query.
	 * @param maxBytesPerPage
	 * @param options
	 * @param mode           {@link QueryMode#COHORT_CAPTURE} only when the query is a cohort definition.
	 * @param consumer       runs the translated query under the lock.
	 * @param types          additional access types to enforce during preflight.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws TableUnavailableException
	 * @throws TableFailedException
	 * @throws EmptyResultException
	 * @throws IOException
	 */
	QueryResultBundle queryAfterAuthorization(final ProgressCallback progressCallback, final UserInfo user, final Query query,
			final Long maxBytesPerPage, final QueryOptions options, final QueryMode mode, final TranslatedQueryConsumer consumer,
			final ACCESS_TYPE... types)
			throws DatastoreException, NotFoundException, TableUnavailableException, TableFailedException,
			LockUnavilableException, EmptyResultException, IOException {
		IdAndVersion idAndVersion = IdAndVersion.parse(
				parserQuery(query.getSql()).getSingleTableName().orElseThrow(TableConstants.JOIN_NOT_SUPPORTED_IN_THIS_CONTEXT));
		// Each cohort is resolved under its own source's read lock before this table's lock is taken,
		// so no two read locks are ever held at once.
		Map<String, ResolvedCohort> cohorts = resolveCohorts(progressCallback, user, query);
		return tryRunWithTableReadLock(progressCallback, idAndVersion, (ProgressCallback callback) -> {
			// The query can only run against an AVAILABLE index. Confirming availability while holding
			// the read lock blocks the builder's exclusive lock, so the snapshot the preflight fetches
			// next matches the served index and cannot be swapped while the query runs.
			final TableStatus status = validateTableIsAvailable(idAndVersion.toString());
			QueryTranslations translated = queryPreflight(user, query, cohorts, maxBytesPerPage, options, mode, types);
			return consumer.apply(translated, status);
		});
	}

	/**
	 * Set the consistent-query etag on the bundle's row result. The etag is only meaningful for a
	 * consistent (row-returning) query; an aggregate-only query suppresses rows, so the result may be
	 * absent even when a query was requested.
	 */
	void setConsistentQueryEtag(QueryResultBundle bundle, QueryOptions options, TableStatus status) {
		if (options.runQuery() && bundle.getQueryResult() != null) {
			bundle.getQueryResult().getQueryResults().setEtag(status.getLastTableChangeEtag());
		}
	}

	/**
	 * Populate the max-rows-per-page on the bundle and, when the returned page is full, the next-page
	 * token that will fetch the following page.
	 */
	void addNextPageTokenIfNeeded(QueryResultBundle bundle, QueryTranslations sqlQuery, Query query, QueryOptions options) {
		if (options.returnMaxRowsPerPage()) {
			bundle.setMaxRowsPerPage(sqlQuery.getMainQuery().getTranslator().getMaxRowsPerPage());
		}
		int maxRowsPerPage = sqlQuery.getMainQuery().getTranslator().getMaxRowsPerPage().intValue();
		if (isRowCountEqualToMaxRowsPerPage(bundle, maxRowsPerPage)) {
			long nextOffset = (query.getOffset() == null ? 0 : query.getOffset()) + maxRowsPerPage;
			// The token carries the unexpanded SQL and the cohort definitions, so the next page re-resolves
			// the cohorts and their values never reach the caller.
			QueryNextPageToken nextPageToken = TableQueryUtils.createNextPageToken(query.getSql(), query.getSort(),
					nextOffset, query.getLimit(), query.getSelectedFacets(), query.getAdditionalFilters(), query.getCohorts());
			bundle.getQueryResult().setNextPageToken(nextPageToken);
		}
	}

	/**
	 * Run the passed runner while holding the table's read lock.
	 * 
	 * @param callback
	 * @param tableId
	 * @param runner
	 * @return
	 * @throws TableUnavailableException
	 * @throws TableFailedException
	 * @throws EmptyResultException
	 * @throws IOException the streaming handler run under the lock failed.
	 */
	<R, T> R tryRunWithTableReadLock(ProgressCallback callback, IdAndVersion idAndversion, ProgressingCallable<R> runner)
			throws TableUnavailableException, TableFailedException, EmptyResultException, IOException {

		try {
			return tableManagerSupport.tryRunWithTableNonExclusiveLock(callback, new LockContext(ContextType.Query, idAndversion) , runner,
					idAndversion);
		} catch (RuntimeException | TableUnavailableException | EmptyResultException | TableFailedException | IOException e) {
			// runtime exceptions and the streaming handler's IOException are unchanged.
			throw e;
		} catch (Exception e) {
			// all other checked exceptions are converted to runtime
			throw new RuntimeException(e);
		}
	}

	/**
	 * Run a query as a stream after all authorization checks have been performed
	 * and any any required row-level filtering has been applied.
	 * 
	 * @param user
	 * @param query
	 * @param offset
	 * @param limit
	 * @param runQuery
	 * @param runCount
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws TableUnavailableException
	 * @throws TableFailedException
	 * @throws TableLockUnavailableException
	 */
	QueryResultBundle executeQuery(UserInfo user, QueryTranslations query, final QueryOptions options, QueryExecutor queryExecutor)
			throws TableUnavailableException, TableFailedException, LockUnavilableException {
		// build up the response.
		QueryResultBundle bundle = new QueryResultBundle();
		if(options.returnColumnModels()) {
			bundle.setColumnModels(query.getMainQuery().getTranslator().getTableSchema());
		}
		if(options.returnSelectColumns()) {
			bundle.setSelectColumns(query.getMainQuery().getTranslator().getSelectColumns());
		}

		IdAndVersion idAndVersion = IdAndVersion
				.parse(query.getMainQuery().getTranslator().getSingleTableIdOptional().orElseThrow(TableConstants.JOIN_NOT_SUPPORTED_IN_THIS_CONTEXT));
		TableIndexDAO indexDao = tableConnectionFactory.getConnection(idAndVersion);
		
		if (query.getMainQuery().getTranslator().isIncludeSearch() && !indexDao.isSearchEnabled(idAndVersion)) {
			throw new IllegalArgumentException("Invalid use of " + TextMatchesPredicate.KEYWORD + ". Full text search is not enabled on table " + idAndVersion + ".");
		}

		// Run the count first. An aggregate-only query always runs the count to enforce the
		// suppression gate against the number of matched rows. That gate must run before the main
		// query so that a below-threshold cohort withholds its rows without first computing them
		// (and, on the streaming path, emitting them to the row handler) only to discard the work.
		if (options.runCount() || query.isAggregateOnly()) {
			// count requested.
			Long count = runCountQuery(query.getCountQuery().orElseThrow(()-> new IllegalStateException("Expected a count query")), indexDao);
			if (query.isAggregateOnly()) {
				Long threshold = query.getSuppressionThreshold();
				if (threshold == null) {
					throw new IllegalStateException("An aggregate-only query requires a suppression threshold");
				}
				// Reject a non-empty result below the threshold; an empty result (0) or
				// one at/above the threshold is allowed.
				if (count > 0 && count < threshold) {
					throw new BelowThresholdException(threshold);
				}
			}
			bundle.setQueryCount(count);
		}

		// run the actual query if needed.
		if (options.runQuery()) {
			// Pre-flight rejects a row request against an aggregate-only source that defines no
			// quasi-identifier columns (RowSuppressionException), so reaching here with runQuery
			// always means rows may be returned: either full read access, or an aggregate-only source
			// that defines quasi-identifier columns and passed the count-only QID validation. In the
			// latter case cell-level k-anonymity has already been pushed into the executed SQL, so the
			// rows returned here are already suppressed regardless of whether they were materialized or
			// streamed. Reaching this point without either condition would silently drop the row
			// request, so fail loudly instead.
			if (query.isAggregateOnly() && !query.isRowReturningAggregate()) {
				throw new IllegalStateException(
						"A row request against an aggregate-only source without quasi-identifier columns must be rejected during pre-flight");
			}
			RowSet rowSet = runMainQuery(queryExecutor, indexDao, query.getMainQuery().getTranslator());
			QueryResult queryResult = new QueryResult();
			queryResult.setQueryResults(rowSet);
			bundle.setQueryResult(queryResult);
		}

		if (options.returnFacets()) {
			applyFacets(bundle, query, indexDao);
		}
		
		if(options.runSumFileSizes()) {
			SumFileSizes sumFileSizes = runSumFileSize(query.getSumFileSizesQuery()
					.orElseThrow(() -> new IllegalStateException("Expected sum of files sizes query")), indexDao);
			bundle.setSumFileSizes(sumFileSizes);
		}
		
		if(options.returnLastUpdatedOn()) {
			Date lastUpdatedOn = tableManagerSupport.getLastChangedOn(idAndVersion).orElse(new Date());
			bundle.setLastUpdatedOn(lastUpdatedOn);
		}

		if (options.returnActionsRequired()) {
			bundle.setActionsRequired(runActionsRequiredQuery(idAndVersion, user, query.getActionsRequiredQuery()
				.orElseThrow(()-> new IllegalStateException("Expected actions required query")), indexDao));
		}
		
		return bundle;
	}

	/**
	 * Resolve the facet statistics for the query and set them on the bundle. For a
	 * full-access read the raw facet results are returned unchanged. For an
	 * aggregate-only read the facet counts must be obscured before they reach the
	 * user, so this fails closed: if the configuration required to obscure them is
	 * missing it throws rather than leak exact counts.
	 *
	 * @param bundle   the response to populate.
	 * @param query    the translated query.
	 * @param indexDao the connection to the table's index.
	 */
	void applyFacets(QueryResultBundle bundle, QueryTranslations query, TableIndexDAO indexDao) {
		List<FacetColumnResult> facetResults = runFacetQueries(
				query.getFacetQueries().orElseThrow(() -> new IllegalStateException("Expected a facet query")), indexDao);

		if (query.isAggregateOnly()) {
			// Fail closed: an aggregate-only query must obscure its facet counts. Missing
			// post-processing configuration is a data leak, so throw rather than return
			// exact counts.
			FacetPostProcessingConfig config = query.getAggregateDataConfiguration()
					.map(AggregateDataConfiguration::getFacetPostProcessingConfig)
					.orElseThrow(() -> new IllegalStateException(
							"An aggregate-only query requires a facet post-processing configuration"));
			// Range facets expose the exact min/max of the restricted rows, which is not a
			// count that post-processing can obscure; drop them entirely.
			facetResults = facetResults.stream().filter(facet -> !(facet instanceof FacetColumnResultRange))
					.collect(Collectors.toList());
			facetResults = facetPostProcessorProvider.getProcessor(config.getAlgorithm())
					.process(facetResults, config.getParameters());
			bundle.setFacetPostProcessingApplied(true);
		} else {
			bundle.setFacetPostProcessingApplied(false);
		}

		bundle.setFacets(facetResults);
	}

	/**
	 * Runs facet queries (enumeration count or range min/max) for all columns in
	 * queryFacetColumns.
	 *
	 * @param originalQuery     the non-transformed query that was submitted by the
	 *                          user.
	 * @param queryFacetColumns
	 * @param indexDao
	 * @return
	 */
	public List<FacetColumnResult> runFacetQueries(FacetQueries facetQuereis, TableIndexDAO indexDao) {
		ValidateArgument.required(facetQuereis, "facetQuereis");
		ValidateArgument.required(indexDao, "indexDao");
		List<FacetTransformer> transformers = facetQuereis.getFacetInformationQueries();
		List<Future<FacetColumnResult>> futures = new ArrayList<>(transformers.size());
		for (FacetTransformer facetQueryTransformer : transformers) {
			futures.add(threadPool.submit(() -> {
				CachedQueryRequest cacheRequest = CachedQueryRequest.clone(facetQueryTransformer.getFacetSqlQuery()).setExpiresInSec(CACHED_QUERY_EXPIRES_IN_SEC);
				RowSet rowSet = queryCacheManager.getQueryResults(indexDao, cacheRequest);
				return facetQueryTransformer.translateToResult(rowSet);
			}));
		}
		List<FacetColumnResult> results = new ArrayList<>(futures.size());
		for (Future<FacetColumnResult> future : futures) {
			try {
				results.add(future.get());
			} catch (Exception e) {
				new IllegalStateException(e);
			}
		}
		return results;
	}

	/**
	 * For the given bundle, is the number of rows equal to the maximum rows per
	 * page? This is used to determine if a next page token should be included with
	 * a query result.
	 * 
	 * @param bundle
	 * @return
	 */
	public static boolean isRowCountEqualToMaxRowsPerPage(QueryResultBundle bundle, int maxRowsPerPage) {
		if (bundle != null) {
			if (bundle.getQueryResult() != null) {
				if (bundle.getQueryResult().getQueryResults() != null) {
					if(bundle.getQueryResult().getQueryResults().getRows() != null){
						int resultSize = bundle.getQueryResult().getQueryResults().getRows().size();
						return maxRowsPerPage == resultSize;
					}
				}
			}
		}
		return false;
	}

	@Override
	public QueryResult queryNextPage(ProgressCallback progressCallback, UserInfo user, QueryNextPageToken nextPageToken)
			throws TableUnavailableException, TableFailedException, LockUnavilableException {
		Query query = TableQueryUtils.createQueryFromNextPageToken(nextPageToken);
		QueryOptions options = new QueryOptions().withRunQuery(true).withRunCount(false).withReturnFacets(false).withRunSumFileSizes(false);;
		QueryResultBundle queryResult = querySinglePage(progressCallback, user, query, options);
		return queryResult.getQueryResult();
	}

	@Override
	public QueryResultBundle queryBundle(ProgressCallback progressCallback, UserInfo user,
			QueryBundleRequest queryBundle)
			throws TableUnavailableException, TableFailedException, LockUnavilableException {
		ValidateArgument.required(queryBundle.getQuery(), "query");
		ValidateArgument.required(queryBundle.getQuery().getSql(), "query.sql");
		QueryOptions options = new QueryOptions().withMask(queryBundle.getPartMask())
				.withAggregateDataPreview(queryBundle.getAggregateDataPreview());
		// execute the query
		return querySinglePage(progressCallback, user, queryBundle.getQuery(),  options);
	}

	/**
	 * Set the default value for a Query
	 * 
	 * @param listRequest
	 * @return
	 */
	public static void setDefaultsValues(Query query) {
		ValidateArgument.required(query, "query");
		if (query.getIncludeEntityEtag() == null) {
			// default to false
			query.setIncludeEntityEtag(false);
		}
	}

	/**
	 * Set the default value for a download request.
	 * 
	 * @param request
	 * @return
	 */
	public static void setDefaultValues(DownloadFromTableRequest request) {
		ValidateArgument.required(request, "request");
		// get query defaults
		TableQueryManagerImpl.setDefaultsValues((Query) request);
		if (request.getIncludeRowIdAndRowVersion() == null) {
			// default to true
			request.setIncludeRowIdAndRowVersion(true);
		}
		if (request.getWriteHeader() == null) {
			// default to true
			request.setWriteHeader(true);
		}
	}

	/**
	 * 
	 * @param sql
	 * @param writer
	 * @return The resulting RowSet will not contain any
	 * @throws TableUnavailableException
	 * @throws NotFoundException
	 * @throws TableFailedException
	 * @throws IOException 
	 * @throws TableLockUnavailableException
	 */
	@Override
	public DownloadFromTableResult runQueryDownloadAsCSV(ProgressCallback progressCallback, UserInfo user,
			DownloadFromTableRequest request, final CSVWriterStream writer) throws TableUnavailableException,
			NotFoundException, TableFailedException, LockUnavilableException, IOException {
		setDefaultValues(request);
		QueryResultBundle result = runQueryAsStream(progressCallback, user, request, query -> {
			if (!query.getMainQuery().getTranslator().getIncludesRowIdAndVersion()) {
				request.setIncludeRowIdAndRowVersion(false);
				request.setIncludeEntityEtag(false);
			}
			// This handler will capture the row data.
			CSVWriterRowHandler handler = new CSVWriterRowHandler(writer,
					query.getMainQuery().getTranslator().getSelectColumns(), request.getIncludeRowIdAndRowVersion(),
					query.getMainQuery().getTranslator().getIncludeEntityEtag());

			if (request.getWriteHeader()) {
				handler.writeHeader();
			}
			return handler;
		});
		// convert the response
		DownloadFromTableResult response = new DownloadFromTableResult();
		response.setHeaders(result.getSelectColumns());
		response.setTableId(result.getQueryResult().getQueryResults().getTableId());
		// pass along the etag.
		response.setEtag(result.getQueryResult().getQueryResults().getEtag());
		return response;
	}
	
	@Override
	public QueryResultBundle runQueryAsStream(ProgressCallback progressCallback, UserInfo user, Query request,
			RowHandlerProvider provider, ACCESS_TYPE...types) throws TableUnavailableException, NotFoundException, TableFailedException,
			LockUnavilableException, IOException {
		return runQueryAsStream(progressCallback, user, request, QueryMode.STANDARD, provider, types);
	}

	/**
	 * See {@link #runQueryAsStream(ProgressCallback, UserInfo, Query, RowHandlerProvider, ACCESS_TYPE...)}.
	 *
	 * @param mode {@link QueryMode#COHORT_CAPTURE} only when the query is a cohort definition.
	 */
	QueryResultBundle runQueryAsStream(ProgressCallback progressCallback, UserInfo user, Query request, QueryMode mode,
			RowHandlerProvider provider, ACCESS_TYPE...types) throws TableUnavailableException, NotFoundException, TableFailedException,
			LockUnavilableException, IOException {
		try {
			QueryOptions options = new QueryOptions().withRunQuery(true).withReturnSelectColumns(true)
					.withRunCount(false).withReturnFacets(false);
			// there is no limit to the size
			Long maxBytes = null;
			// The handler is opened and consumed entirely inside the locked callback, after the query
			// has been translated against the served index, so its stream is pinned to that index.
			return queryAfterAuthorization(progressCallback, user, request, maxBytes, options, mode, (query, status) -> {
				try (RowHandler handler = provider.getHandler(query)) {
					QueryResultBundle bundle = executeQuery(user, query, options, new StreamingQueryExecutor(handler));
					setConsistentQueryEtag(bundle, options, status);
					return bundle;
				}
			}, types);
		} catch (EmptyResultException e) { // this is thrown in queryPreflight()
			throw new IllegalArgumentException("Table " + e.getTableId() + " has an empty schema", e);
		}
	}

	RowSet runMainQuery(QueryExecutor queryExecutor, TableIndexDAO indexDao, QueryTranslator query) {
		ValidateArgument.required(queryExecutor, "The queryExecutor");
		ValidateArgument.required(indexDao, "The indexDao");
		ValidateArgument.required(query, "The query");
		return queryExecutor.executeQuery(indexDao, query);
	}

	/**
	 * Run a count query.
	 * 
	 * @param query
	 * @return
	 */
	long runCountQuery(CountQuery query, TableIndexDAO indexDao) {
		return query.getCountQuery().map(countSqlQuery-> {
			
			CachedQueryRequest cacheRequest = new CachedQueryRequest()
				.setOutputSQL(countSqlQuery.getSql())
				.setParameters(countSqlQuery.getParameters())
				.setSelectColumns(List.of(new SelectColumn().setColumnType(ColumnType.INTEGER)))
				.setIncludesRowIdAndVersion(false)
				.setIncludesRowIdAndVersion(false)
				.setSingleTableId(query.getSingleTableId())
				.setTableHash(query.getTableHash())
				.setExpiresInSec(CACHED_QUERY_EXPIRES_IN_SEC);
			
			RowSet result = queryCacheManager.getQueryResults(indexDao, cacheRequest);
			
			Long count = result.getRows().stream()
				.findFirst()
				.map( row -> row.getValues().stream().findFirst().map(Long::valueOf).orElse(0L))
				.orElse(0L);

			/*
			 * Post processing for count. When a limit and/or offset is specified in a
			 * query, count(*) just ignores those, since it assumes the limit & offset apply
			 * to the one row count(*) returns. In actuality, we want to apply that limit &
			 * offset to the count itself. We do that here manually.
			 */
			Pagination pagination = query.getOriginalPagination();
			if (pagination != null) {
				if (pagination.getOffsetLong() != null) {
					long offsetForCount = pagination.getOffsetLong();
					count = Math.max(0, count - offsetForCount);
				}
				if (pagination.getLimitLong() != null) {
					long limitForCount = pagination.getLimitLong();
					count = Math.min(limitForCount, count);
				}
			}
			return count;
		}).orElse(1L);
	}
	
	/**
	 * Run the queries to get the sum of the file sizes (bytes) for the given query.
	 * 
	 * @param query
	 * @param indexDao
	 * @return
	 */
	SumFileSizes runSumFileSize(SumFileSizesQuery query, TableIndexDAO indexDao) {
		return query.getRowIdAndVersionQuery().map(basicQuery->{
			SumFileSizes result = new SumFileSizes();
			result.setGreaterThan(false);
			result.setSumFileSizesBytes(0L);
			List<IdAndVersion> rowIdAndVersions = indexDao.getRowIdAndVersions(basicQuery.getSql(), basicQuery.getParameters());
			boolean isGreaterThan = rowIdAndVersions.size() > MAX_ROWS_PER_CALL;
			result.setGreaterThan(isGreaterThan);
			// Use the rowIds to calculate the sum of the file sizes.
			long sumFileSizesBytes = indexDao.getSumOfFileSizes(ViewObjectType.ENTITY.getMainType(), rowIdAndVersions);
			result.setSumFileSizesBytes(sumFileSizesBytes);
			return result;
		}).orElse(new SumFileSizes().setGreaterThan(false).setSumFileSizesBytes(0L));
	}
	
	List<ActionRequiredCount> runActionsRequiredQuery(IdAndVersion idAndVersion, UserInfo user, ActionsRequiredQuery actionsRequiredQuery, TableIndexDAO indexDao) {
		FilesBatchProvider filesProvider = (limit, offset) -> {
			BasicQuery filesQuery = actionsRequiredQuery.getFileEntityQuery(limit, offset);
			return indexDao.querySingleColumn(filesQuery.getSql(), filesQuery.getParameters(), Long.class);
		};
			
		EntityActionRequiredCallback actionsProvider = (fileIds) -> entityAuthorizationManager.getActionsRequiredForDownload(user, fileIds);
		
		ActionsRequiredDao actionsRequiredDao = tableManagerSupport.getActionsRequiredDao(idAndVersion);
		
		return indexDao.executeInWriteTransaction((txState) -> {
			
			try {
				actionsRequiredDao.createActionsRequiredTable(user.getId(), ACTIONS_REQUIRED_BATCH_SIZE, filesProvider, actionsProvider);
		
				return actionsRequiredDao.getActionsRequiredCount(user.getId(), MAX_ACTIONS_REQUIRED);
			} finally {
				actionsRequiredDao.dropActionsRequiredTable(user.getId());
			}
		});
	}

	/**
	 * Parser a query and convert ParseExceptions to IllegalArgumentExceptions
	 * 
	 * @param sql
	 * @return
	 */
	private QuerySpecification parserQuery(String sql) {
		try {
			return TableQueryParser.parserQuery(sql);
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	private QueryExpression parserQueryQuerExpression(String sql) {
		try {
			return new TableQueryParser(sql).queryExpression();
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	public Long getMaxRowsPerPage(List<ColumnModel> models) {
		// Calculate the size
		int maxRowSizeBytes = TableModelUtils.calculateMaxRowSize(models);
		if (maxRowSizeBytes < 1)
			return null;
		return (long) (this.maxBytesPerRequest / maxRowSizeBytes);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.manager.table.TableStatusManager#
	 * validateTableIsAvailable(java.lang.String)
	 */
	@Override
	public TableStatus validateTableIsAvailable(String tableId)
			throws NotFoundException, TableUnavailableException, TableFailedException {
		IdAndVersion idAndVersion = IdAndVersion.parse(tableId);
		final TableStatus status = tableManagerSupport.getTableStatusOrCreateIfNotExists(idAndVersion);
		switch (status.getState()) {
		case AVAILABLE:
			return status;
		case PROCESSING:
			// When the table is not available, we communicate the current status of the
			// table in this exception.
			throw new TableUnavailableException(status);
		default:
		case PROCESSING_FAILED:
			// When the table is in a failed state, we communicate the current status of the
			// table in this exception.
			throw new TableFailedException(status);
		}
	}

	/**
	 * Create a new empty query result bundle.
	 * 
	 * @param tableId
	 * @return
	 */
	public static QueryResultBundle createEmptyBundle(String tableId, QueryOptions options) {
		QueryResult result = new QueryResult();
		QueryResultBundle bundle = new QueryResultBundle();
		if(options.runQuery()) {
			RowSet emptyRowSet = new RowSet();
			emptyRowSet.setRows(new LinkedList<Row>());
			emptyRowSet.setTableId(tableId);
			emptyRowSet.setHeaders(new LinkedList<SelectColumn>());
			result.setQueryResults(emptyRowSet);
			bundle.setQueryResult(result);
		}
		if(options.runCount()) {
			bundle.setQueryCount(0L);
		}
		if(options.returnSelectColumns()) {
			bundle.setSelectColumns(new LinkedList<SelectColumn>());
		}
		if(options.returnColumnModels()) {
			bundle.setColumnModels(new LinkedList<ColumnModel>());
		}
		if(options.returnMaxRowsPerPage()) {
			bundle.setMaxRowsPerPage(1L);
		}
		if(options.runSumFileSizes()) {
			SumFileSizes sum = new SumFileSizes();
			sum.setGreaterThan(false);
			sum.setSumFileSizesBytes(0L);
			bundle.setSumFileSizes(sum);
		}
		if (options.returnActionsRequired()) {
			bundle.setActionsRequired(Collections.emptyList());
		}
		return bundle;
	}

	/**
	 * Add a row level filter to the given query.
	 * 
	 * @param user
	 * @param query
	 * @return
	 * @throws TableFailedException
	 * @throws TableUnavailableException
	 * @throws NotFoundException
	 */
	void addRowLevelFilter(UserInfo user, QuerySpecification query, QueryIndexDescription indexDescription, ACCESS_TYPE...types)
			throws NotFoundException, TableUnavailableException, TableFailedException {
		if(indexDescription.getBenefactors().isEmpty()) {
			// with no benefactors nothing is needed.
			return;
		}
		TableIndexDAO indexDao = tableConnectionFactory.getConnection(indexDescription.getIdAndVersion());
		for (BenefactorAccessFilter filter : computeAccessibleBenefactors(user, indexDescription, indexDao, types)) {
			buildBenefactorFilter(query, filter.accessibleIds(), filter.benefactorColumnName());
		}
	}

	@Override
	public List<BenefactorAccessFilter> computeAccessibleBenefactors(UserInfo user,
			QueryIndexDescription indexDescription, TableIndexDAO indexDao, ACCESS_TYPE... types) {
		List<BenefactorDescription> benefactors = indexDescription.getBenefactors();
		List<BenefactorAccessFilter> filters = new ArrayList<>(benefactors.size());
		for (BenefactorDescription dependencyDesc : benefactors) {
			// lookup the distinct benefactor IDs applied to the table.
			Set<Long> tableBenefactors;
			try {
				tableBenefactors = indexDao.getDistinctLongValues(indexDescription.getIdAndVersion(), dependencyDesc.getBenefactorColumnName());
			} catch (BadSqlGrammarException e) { // table has not been created yet
				tableBenefactors = Collections.emptySet();
			}
			Set<Long> accessibleBenefactors = tableManagerSupport.getAccessibleBenefactors(user, dependencyDesc.getBenefactorType(), tableBenefactors, types);

			// -1 is the default value for a row with no benefactor; it must always be accessible.
			accessibleBenefactors.add(-1L);
			filters.add(new BenefactorAccessFilter(dependencyDesc.getBenefactorColumnName(), accessibleBenefactors));
		}
		return filters;
	}

    /**
     * Build a new query with a benefactor filter applied to the SQL from the passed
     * query.
     *
     * @param originalQuery
     * @param accessibleBenefactors
     * @return
     * @throws EmptyResultException
     */
    public static void buildBenefactorFilter(QuerySpecification originalQuery,
                                                           Set<Long> accessibleBenefactors,
                                                           String benefactorColumnName) {
        ValidateArgument.required(originalQuery, "originalQuery");
        ValidateArgument.required(accessibleBenefactors, "accessibleBenefactors");

        // copy the original model
        try {
            WhereClause where = originalQuery.getTableExpression().getWhereClause();
            StringBuilder filterBuilder = new StringBuilder();
            filterBuilder.append("WHERE ");
            if (where != null) {
                filterBuilder.append("(");
                filterBuilder.append(where.getSearchCondition().toSql());
                filterBuilder.append(") AND ");
            }

			filterBuilder.append(benefactorColumnName);
			filterBuilder.append(" IN (");

			filterBuilder.append(accessibleBenefactors.stream()
					.map(String::valueOf)
					.collect(Collectors.joining(",")));
			filterBuilder.append(")");

            // create the new where
            where = new TableQueryParser(filterBuilder.toString()).whereClause();
            originalQuery.getTableExpression().replaceWhere(where);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

	@Override
	public Long getMaxBytesPerRequest() {
		return maxBytesPerRequest;
	}

}
