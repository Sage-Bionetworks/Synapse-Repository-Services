package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.AsynchJobType;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseBadRequestException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DataType;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverride;
import org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverrideEntry;
import org.sagebionetworks.repo.model.search.table.ListTextAnalyzersRequest;
import org.sagebionetworks.repo.model.search.table.ListTextAnalyzersResponse;
import org.sagebionetworks.repo.model.search.table.SearchConfiguration;
import org.sagebionetworks.repo.model.search.table.SearchIndex;
import org.sagebionetworks.repo.model.search.SearchAutocompleteBody;
import org.sagebionetworks.repo.model.search.SearchQuery;
import org.sagebionetworks.repo.model.search.SearchQueryPart;
import org.sagebionetworks.repo.model.search.SearchQueryResults;
import org.sagebionetworks.repo.model.search.dsl.Aggregation;
import org.sagebionetworks.repo.model.search.dsl.FieldCollapse;
import org.sagebionetworks.repo.model.search.dsl.Highlight;
import org.sagebionetworks.repo.model.search.dsl.HighlightField;
import org.sagebionetworks.repo.model.search.dsl.MatchAllQuery;
import org.sagebionetworks.repo.model.search.dsl.MatchBoolPrefixFieldOptions;
import org.sagebionetworks.repo.model.search.dsl.MatchFieldOptions;
import org.sagebionetworks.repo.model.search.dsl.MatchPhraseFieldOptions;
import org.sagebionetworks.repo.model.search.dsl.Query;
import org.sagebionetworks.repo.model.search.dsl.Rescore;
import org.sagebionetworks.repo.model.search.dsl.RescoreQuery;
import org.sagebionetworks.repo.model.search.dsl.TermFieldOptions;
import org.sagebionetworks.repo.model.search.dsl.TermsAggregation;
import org.sagebionetworks.repo.model.search.table.SearchAutocompleteRequest;
import org.sagebionetworks.repo.model.search.table.SearchIndexQuery;
import org.sagebionetworks.util.TimeUtils;

/**
 * Integration tests for the SearchIndex query path.
 *
 * The two test methods are split deliberately so a single AOSS analyzer issue can't take
 * out coverage of both paths:
 *
 *   - {@link #testAsyncQueryWithDefaultAnalyzer()} exercises the async start-job/poll-job path,
 *     the {@code responseParts} opt-in mechanic, and a single round-trip per typed
 *     {@code SearchQuery} field ({@code post_filter}, {@code highlight}, {@code collapse},
 *     {@code rescore}) against one shared fixture. Per-field semantic correctness lives in
 *     {@code OpenSearchManagerImplAutoWiredTest}; the IT only proves HTTP wiring.
 *   - {@link #testAutocompleteWithEdgeNgram()} exercises the sync autocomplete endpoint against
 *     an index built with a {@code ColumnAnalyzerOverride} mapped to the bootstrapped
 *     {@code AUTOCOMPLETE} / {@code AUTOCOMPLETE_SEARCH} edge-ngram analyzers.
 */
@ExtendWith(ITTestExtension.class)
public class ITSearchQueryTest {

	private static final long MAX_QUERY_TIMEOUT_MS = 1000 * 60 * 5;
	private static final long MAX_APPEND_TIMEOUT = 30 * 1000;

	private final SynapseAdminClient adminSynapse;
	private final SynapseClient synapse;
	private final List<Entity> entitiesToDelete = new ArrayList<>();

	public ITSearchQueryTest(SynapseAdminClient adminSynapse, SynapseClient synapse) {
		this.adminSynapse = adminSynapse;
		this.synapse = synapse;
	}

	@BeforeEach
	public void before() throws SynapseException {
		adminSynapse.clearAllLocks();
	}

	@AfterEach
	public void after() {
		// Delete in reverse order (search index before table before project)
		for (int i = entitiesToDelete.size() - 1; i >= 0; i--) {
			try {
				adminSynapse.deleteEntity(entitiesToDelete.get(i));
			} catch (SynapseException e) {
				// ignore
			}
		}
	}

	/** Build a SearchQuery wrapping a {@code match_all} clause — the
	 * catalog-style minimum payload now that {@code query} is required. */
	private static SearchQuery matchAllBody() {
		return new SearchQuery().setQuery(new Query().setMatch_all(new MatchAllQuery()));
	}

	/**
	 * Async query path against an index built with the platform default analyzer (no
	 * ColumnAnalyzerOverride). The IT covers HTTP wiring only:
	 * <ul>
	 *   <li>start-job/poll-job round trip and the {@code responseParts} opt-in mechanic
	 *       (HITS + TOTAL_HITS + SELECT_COLUMNS populated when requested, null at default);</li>
	 *   <li>one round-trip per typed {@code SearchQuery} field — {@code post_filter},
	 *       {@code highlight}, {@code collapse}, {@code rescore} — proving the typed POJO
	 *       survives the wire and AOSS responds.</li>
	 * </ul>
	 * Per-field semantic correctness (aggregation-vs-hits narrowing for post_filter,
	 * &lt;em&gt; wrapping for highlight, group-by for collapse, re-rank for rescore) is asserted
	 * in {@code OpenSearchManagerImplAutoWiredTest}, which is faster and isolates AOSS flake
	 * from controller wiring regressions.
	 */
	@Test
	public void testAsyncQueryWithDefaultAnalyzer() throws Exception {
		Project project = new Project();
		project.setName("ITAsyncQuery_Default_" + UUID.randomUUID());
		project = synapse.createEntity(project);
		entitiesToDelete.add(project);

		grantPublicRead(project.getId());

		// Two columns satisfy the typed-field round-trips below: a STRING column for
		// collapse / post_filter (term-routes to .keyword), and a LARGETEXT column for
		// the match query, highlight fragments, and rescore phrase boost.
		ColumnModel projectIdCol = new ColumnModel();
		projectIdCol.setName("projectId");
		projectIdCol.setColumnType(ColumnType.STRING);
		projectIdCol.setMaximumSize(50L);
		projectIdCol = synapse.createColumnModel(projectIdCol);

		ColumnModel titleCol = new ColumnModel();
		titleCol.setName("title");
		titleCol.setColumnType(ColumnType.LARGETEXT);
		titleCol = synapse.createColumnModel(titleCol);

		TableEntity table = new TableEntity();
		table.setName("AsyncQueryDefaultTable");
		table.setParentId(project.getId());
		table.setColumnIds(Arrays.asList(projectIdCol.getId(), titleCol.getId()));
		table = synapse.createEntity(table);
		entitiesToDelete.add(table);

		// The lifecycle worker queries table data as the anonymous user, which requires the
		// source table to be marked OPEN_DATA (Sage governance).
		adminSynapse.changeEntitysDataType(table.getId(), DataType.OPEN_DATA);

		List<ColumnModel> columns = synapse.getColumnModelsForTableEntity(table.getId());
		RowSet rowSet = new RowSet();
		rowSet.setTableId(table.getId());
		rowSet.setHeaders(TableModelUtils.getSelectColumns(columns));
		// Two projectId values (projA, projB) so collapse returns >1 group; the title text
		// includes "tumor" (highlight target) and "amyloid plaques" (rescore phrase target).
		rowSet.setRows(Arrays.asList(
			new Row().setValues(Arrays.asList("projA", "BRCA1 tumor amyloid plaques")),
			new Row().setValues(Arrays.asList("projA", "BRCA2 tumor amyloid plaques")),
			new Row().setValues(Arrays.asList("projB", "TP53 tumor suppressor")),
			new Row().setValues(Arrays.asList("projB", "EGFR tumor signaling"))
		));
		synapse.appendRowsToTable(rowSet, MAX_APPEND_TIMEOUT, table.getId());

		// SearchIndex with no SearchConfiguration — default analyzer (no edge_ngram).
		SearchIndex searchIndex = new SearchIndex();
		searchIndex.setName("AsyncQueryDefaultSearchIndex");
		searchIndex.setParentId(project.getId());
		searchIndex.setDefiningSQL("select * from " + table.getId());
		searchIndex = adminSynapse.createEntity(searchIndex);
		entitiesToDelete.add(searchIndex);

		final String searchIndexId = searchIndex.getId();

		// Async query with all opt-in parts.
		SearchIndexQuery fullQuery = new SearchIndexQuery();
		fullQuery.setSearchIndexId(searchIndexId);
		fullQuery.setSearchQuery(matchAllBody());
		fullQuery.setResponseParts(EnumSet.of(
				SearchQueryPart.HITS, SearchQueryPart.TOTAL_HITS, SearchQueryPart.SELECT_COLUMNS));

		// call under test — async start/poll path with responseParts populated
		AsyncJobHelper.assertAysncJobResult(synapse, AsynchJobType.SearchIndexQuery, fullQuery,
			(SearchQueryResults results) -> {
				assertNotNull(results);
				assertEquals(4L, (long) results.getTotalHits());
				assertNotNull(results.getSelectColumns(),
					"selectColumns should be populated when SELECT_COLUMNS is requested");
				assertEquals(2, results.getSelectColumns().size(),
					"definingSQL is 'select * from <table>' with two columns (projectId, title)");
			},
			MAX_QUERY_TIMEOUT_MS,
			AsyncJobHelper.INFINITE_RETRIES
		);

		// Async query with responseParts left null — defaults to HITS only, the rest must be null.
		SearchIndexQuery defaultPartsQuery = new SearchIndexQuery();
		defaultPartsQuery.setSearchIndexId(searchIndexId);
		defaultPartsQuery.setSearchQuery(matchAllBody());

		// call under test — async path, default response parts
		AsyncJobHelper.assertAysncJobResult(synapse, AsynchJobType.SearchIndexQuery, defaultPartsQuery,
			(SearchQueryResults results) -> {
				assertNotNull(results);
				assertNotNull(results.getHits(), "HITS is always populated by default");
				assertNull(results.getTotalHits(),
					"totalHits should be null when responseParts is left at default (HITS only)");
				assertNull(results.getSelectColumns(),
					"selectColumns should be null when responseParts is left at default (HITS only)");
			},
			MAX_QUERY_TIMEOUT_MS,
			AsyncJobHelper.INFINITE_RETRIES
		);

		// --- Typed SearchQuery field round-trips: one call per field, asserting only that
		// the typed POJO reaches AOSS and the corresponding response slot is populated.
		// Behavioral correctness is exercised in OpenSearchManagerImplAutoWiredTest.

		// post_filter + aggregations: aggregationResults must be populated.
		SearchIndexQuery postFilterQuery = new SearchIndexQuery();
		postFilterQuery.setSearchIndexId(searchIndexId);
		postFilterQuery.setSearchQuery(new SearchQuery()
				.setQuery(new Query().setMatch_all(new MatchAllQuery()))
				.setAggregations(Map.of("by_project",
						new Aggregation().setTerms(new TermsAggregation().setField("projectId"))))
				.setPost_filter(new Query().setTerm(
						Map.of("projectId", new TermFieldOptions().setValue("projA")))));
		postFilterQuery.setResponseParts(EnumSet.of(SearchQueryPart.TOTAL_HITS));

		// call under test — post_filter + aggregations round-trip
		AsyncJobHelper.assertAysncJobResult(synapse, AsynchJobType.SearchIndexQuery, postFilterQuery,
			(SearchQueryResults results) -> {
				assertNotNull(results);
				assertNotNull(results.getAggregationResults(),
					"aggregationResults must be populated when aggregations are supplied");
			},
			MAX_QUERY_TIMEOUT_MS,
			AsyncJobHelper.INFINITE_RETRIES
		);

		// highlight: SearchHit.highlights must be populated for each matching hit.
		SearchIndexQuery highlightQuery = new SearchIndexQuery();
		highlightQuery.setSearchIndexId(searchIndexId);
		highlightQuery.setSearchQuery(new SearchQuery()
				.setQuery(new Query().setMatch(Map.of("title", new MatchFieldOptions().setQuery("tumor"))))
				.setHighlight(new Highlight().setFields(Map.of("title", new HighlightField()))));
		highlightQuery.setResponseParts(EnumSet.of(SearchQueryPart.HITS));

		// call under test — highlight round-trip
		AsyncJobHelper.assertAysncJobResult(synapse, AsynchJobType.SearchIndexQuery, highlightQuery,
			(SearchQueryResults results) -> {
				assertNotNull(results);
				assertNotNull(results.getHits());
				assertTrue(results.getHits().size() >= 1, "expected at least one tumor hit");
				assertNotNull(results.getHits().get(0).getHighlights(),
					"highlights must be populated when highlight is requested");
			},
			MAX_QUERY_TIMEOUT_MS,
			AsyncJobHelper.INFINITE_RETRIES
		);

		// collapse: hits must respect the collapse grouping. collapse and rescore are
		// exercised in separate calls — OpenSearch rejects rescore combined with collapse.
		SearchIndexQuery collapseQuery = new SearchIndexQuery();
		collapseQuery.setSearchIndexId(searchIndexId);
		collapseQuery.setSearchQuery(new SearchQuery()
				.setQuery(new Query().setMatch(Map.of("title", new MatchFieldOptions().setQuery("tumor"))))
				.setCollapse(new FieldCollapse().setField("projectId")));
		collapseQuery.setResponseParts(EnumSet.of(SearchQueryPart.HITS));

		// call under test — collapse round-trip
		AsyncJobHelper.assertAysncJobResult(synapse, AsynchJobType.SearchIndexQuery, collapseQuery,
			(SearchQueryResults results) -> {
				assertNotNull(results);
				assertNotNull(results.getHits());
				assertEquals(2, results.getHits().size(),
					"collapse on projectId must return one hit per distinct value (projA, projB)");
			},
			MAX_QUERY_TIMEOUT_MS,
			AsyncJobHelper.INFINITE_RETRIES
		);

		// rescore: re-ranks the top window; exercised on its own (incompatible with collapse).
		SearchIndexQuery rescoreQuery = new SearchIndexQuery();
		rescoreQuery.setSearchIndexId(searchIndexId);
		rescoreQuery.setSearchQuery(new SearchQuery()
				.setQuery(new Query().setMatch(Map.of("title", new MatchFieldOptions().setQuery("tumor"))))
				.setRescore(new Rescore()
						.setWindow_size(50L)
						.setQuery(new RescoreQuery()
								.setRescore_query(new Query().setMatch_phrase(
										Map.of("title", new MatchPhraseFieldOptions().setQuery("amyloid plaques"))))
								.setQuery_weight(1.0)
								.setRescore_query_weight(5.0))));
		rescoreQuery.setResponseParts(EnumSet.of(SearchQueryPart.HITS));

		// call under test — rescore round-trip
		AsyncJobHelper.assertAysncJobResult(synapse, AsynchJobType.SearchIndexQuery, rescoreQuery,
			(SearchQueryResults results) -> {
				assertNotNull(results);
				assertNotNull(results.getHits());
				assertEquals(4, results.getHits().size(),
					"rescore never drops hits — all four 'tumor' rows remain");
			},
			MAX_QUERY_TIMEOUT_MS,
			AsyncJobHelper.INFINITE_RETRIES
		);
	}

	/**
	 * Sync autocomplete with edge_ngram AUTOCOMPLETE analyzer configured via
	 * ColumnAnalyzerOverride and SearchConfiguration. This is the optimal setup for
	 * high-performance type-ahead: edge_ngram pre-computes prefix tokens at index time so
	 * matching is an exact token lookup.
	 */
	@Test
	public void testAutocompleteWithEdgeNgram() throws Exception {
		Project project = new Project();
		project.setName("ITSearchAutocomplete_EdgeNgram_" + UUID.randomUUID());
		project = synapse.createEntity(project);
		entitiesToDelete.add(project);

		grantPublicRead(project.getId());

		ListTextAnalyzersResponse analyzers = adminSynapse.listTextAnalyzers(new ListTextAnalyzersRequest());
		String orgName = analyzers.getResults().get(0).getOrganizationName();

		// AUTOCOMPLETE owns both analyzer.default (edge_ngram index) and analyzer.default_search
		// (non-ngram search), so a single qname covers index and search time.
		ColumnAnalyzerOverrideEntry entry = new ColumnAnalyzerOverrideEntry();
		entry.setColumnName("geneName");
		entry.setAnalyzer(new org.json.JSONObject().put("$ref", orgName + "-AUTOCOMPLETE"));

		ColumnAnalyzerOverride override = new ColumnAnalyzerOverride();
		override.setName("IT_AUTOCOMPLETE_OVERRIDE_" + UUID.randomUUID().toString().replace("-", ""));
		override.setOrganizationName(orgName);
		override.setOverrides(Arrays.asList(entry));
		override = adminSynapse.createColumnAnalyzerOverride(override);

		String overrideQualifiedName = orgName + "-" + override.getName();
		SearchConfiguration config = new SearchConfiguration();
		config.setName("IT_AUTOCOMPLETE_CONFIG_" + UUID.randomUUID().toString().replace("-", ""));
		config.setOrganizationName(orgName);
		config.setColumnAnalyzerOverrides(Arrays.asList(
				new org.json.JSONObject().put("$ref", overrideQualifiedName)));
		config = adminSynapse.createSearchConfiguration(config);

		ColumnModel nameCol = new ColumnModel();
		nameCol.setName("geneName");
		nameCol.setColumnType(ColumnType.STRING);
		nameCol.setMaximumSize(100L);
		nameCol = synapse.createColumnModel(nameCol);

		TableEntity table = new TableEntity();
		table.setName("AutocompleteEdgeNgramTable");
		table.setParentId(project.getId());
		table.setColumnIds(Arrays.asList(nameCol.getId()));
		table = synapse.createEntity(table);
		entitiesToDelete.add(table);

		adminSynapse.changeEntitysDataType(table.getId(), DataType.OPEN_DATA);

		List<ColumnModel> columns = synapse.getColumnModelsForTableEntity(table.getId());
		RowSet rowSet = new RowSet();
		rowSet.setTableId(table.getId());
		rowSet.setHeaders(TableModelUtils.getSelectColumns(columns));
		rowSet.setRows(Arrays.asList(
			new Row().setValues(Arrays.asList("BRCA1")),
			new Row().setValues(Arrays.asList("BRCA2")),
			new Row().setValues(Arrays.asList("TP53"))
		));
		synapse.appendRowsToTable(rowSet, MAX_APPEND_TIMEOUT, table.getId());

		SearchIndex searchIndex = new SearchIndex();
		searchIndex.setName("AutocompleteEdgeNgramSearchIndex");
		searchIndex.setParentId(project.getId());
		searchIndex.setDefiningSQL("select * from " + table.getId());
		searchIndex.setSearchConfigurationId(config.getId());
		searchIndex = adminSynapse.createEntity(searchIndex);
		entitiesToDelete.add(searchIndex);

		// Wait for the index to be ACTIVE by polling a count query. If the index ends up FAILED
		// the manager raises IllegalArgumentException with the stored errorMessage which the
		// async helper surfaces verbatim — that's what we want, since the autocomplete check
		// below would otherwise time out without context.
		SearchIndexQuery waitIndexQuery = new SearchIndexQuery();
		waitIndexQuery.setSearchIndexId(searchIndex.getId());
		waitIndexQuery.setSearchQuery(matchAllBody());
		waitIndexQuery.setResponseParts(EnumSet.of(SearchQueryPart.TOTAL_HITS));

		AsyncJobHelper.assertAysncJobResult(synapse, AsynchJobType.SearchIndexQuery, waitIndexQuery,
			(SearchQueryResults results) -> assertEquals(3L, (long) results.getTotalHits()),
			MAX_QUERY_TIMEOUT_MS,
			AsyncJobHelper.INFINITE_RETRIES
		);

		// Autocomplete request shape is the slim SearchAutocompleteRequest: searchIndexId,
		// a prefix-flavored top-level DSL clause, and (optionally) returnFields. The column
		// is bound to the AUTOCOMPLETE analyzer chain so `match_bool_prefix` against it does
		// the edge-ngram work at index time.
		SearchAutocompleteRequest autocompleteRequest = new SearchAutocompleteRequest()
				.setSearchIndexId(searchIndex.getId())
				.setSearchQuery(new SearchAutocompleteBody()
						.setQuery(new Query().setMatch_bool_prefix(
								Map.of("geneName", new MatchBoolPrefixFieldOptions().setQuery("BRC")))));

		// call under test — AOSS is eventually consistent per query type and per replica, so
		// the match_all wait above does not guarantee this match_bool_prefix autocomplete sees
		// every document yet. Poll the autocomplete itself until both BRCA hits surface.
		SearchQueryResults autocompleteResults = waitForQuery(
			() -> synapse.searchAutocomplete(autocompleteRequest),
			r -> r.getHits() != null && r.getHits().size() >= 2,
			"at least 2 autocomplete hits for 'BRC' (BRCA1, BRCA2)");
		assertNotNull(autocompleteResults);
		assertNotNull(autocompleteResults.getHits());
		assertTrue(autocompleteResults.getHits().size() >= 2,
			"Expected at least 2 autocomplete hits for 'BRC' (BRCA1, BRCA2)");
		// The slim request has no responseParts knob — autocomplete is always hits-only.
		assertNull(autocompleteResults.getTotalHits(),
			"autocomplete must always omit totalHits");
		assertNull(autocompleteResults.getSelectColumns(),
			"autocomplete must always omit selectColumns");
		assertNull(autocompleteResults.getAggregationResults(),
			"autocomplete must always omit aggregationResults");
	}

	@Test
	public void testStartSearchQueryWithUnsupportedKey() {
		// An unsupported key anywhere in the typed search DSL is rejected with HTTP 400 at request
		// submission (the SearchIndexQuery body round-trips through the boundary guard), before any
		// async job is created — so this needs no built index.
		FakeSearchQuery body = new FakeSearchQuery();
		body.setQuery(new Query().setMatch_all(new MatchAllQuery()));
		body.setNotPartOfSpecification("nope");
		SearchIndexQuery request = new SearchIndexQuery()
				.setSearchIndexId("syn1")
				.setSearchQuery(body);

		// call under test
		String message = assertThrows(SynapseBadRequestException.class,
				() -> synapse.startSearchIndexQuery(request)).getMessage();
		assertEquals("JSON Element in Entity is Unsupported: notPartOfSpecification", message);
	}

	/**
	 * A search/autocomplete call against the live cluster that may throw {@link SynapseException}.
	 */
	@FunctionalInterface
	private interface QueryCall {
		SearchQueryResults call() throws SynapseException;
	}

	/**
	 * Poll a synchronous search/autocomplete call until {@code condition} holds, then return the
	 * matching result. AOSS is eventually consistent per query type and per replica, so a freshly
	 * built index that already answers a {@code match_all} probe may transiently under-return for a
	 * different query shape (autocomplete, match, post_filter). Tests asserting on a specific
	 * synchronous query must poll that exact query rather than trusting a prior wait. Mirrors the
	 * {@code waitForSearch}/{@code waitForSearchHits} helpers in {@code OpenSearchManagerImplAutoWiredTest}.
	 *
	 * @param queryCall   the call under test
	 * @param condition   the readiness predicate on the result (e.g. expected hit count reached)
	 * @param description what is being waited for, used in the timeout message
	 * @return the result that satisfied {@code condition}
	 */
	private SearchQueryResults waitForQuery(QueryCall queryCall,
			java.util.function.Predicate<SearchQueryResults> condition, String description) {
		SearchQueryResults[] holder = { null };
		boolean ready = TimeUtils.waitForExponential(MAX_QUERY_TIMEOUT_MS, 1000L, null, (v) -> {
			try {
				SearchQueryResults r = queryCall.call();
				holder[0] = r;
				return r != null && condition.test(r);
			} catch (SynapseException e) {
				// index_not_found / still-propagating — keep polling
				return false;
			}
		});
		assertTrue(ready, "Timed out waiting for " + description);
		return holder[0];
	}

	private void grantPublicRead(String entityId) throws SynapseException {
		AccessControlList acl = synapse.getACL(entityId);
		ResourceAccess publicAccess = new ResourceAccess();
		publicAccess.setPrincipalId(
				AuthorizationConstants.BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId());
		publicAccess.setAccessType(new HashSet<>(Arrays.asList(ACCESS_TYPE.READ)));
		acl.getResourceAccess().add(publicAccess);
		// Tables require DOWNLOAD to query content — grant it to AUTHENTICATED_USERS.
		ResourceAccess authUsersAccess = new ResourceAccess();
		authUsersAccess.setPrincipalId(
				AuthorizationConstants.BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId());
		authUsersAccess.setAccessType(new HashSet<>(Arrays.asList(ACCESS_TYPE.READ, ACCESS_TYPE.DOWNLOAD)));
		acl.getResourceAccess().add(authUsersAccess);
		synapse.updateACL(acl);
	}
}
