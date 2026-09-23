package org.sagebionetworks.repo.manager.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch.core.search.SourceFilter;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.table.BenefactorAccessFilter;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.manager.table.TableQueryManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.search.SearchAutocompleteBody;
import org.sagebionetworks.repo.model.search.dsl.MatchFieldOptions;
import org.sagebionetworks.repo.model.search.dsl.PrefixFieldOptions;
import org.sagebionetworks.repo.model.search.dsl.Query;
import org.sagebionetworks.repo.model.search.SearchFieldValue;
import org.sagebionetworks.repo.model.search.SearchHit;
import org.sagebionetworks.repo.model.search.SearchQuery;
import org.sagebionetworks.repo.model.search.SearchQueryPart;
import org.sagebionetworks.repo.model.search.SearchQueryResults;
import org.sagebionetworks.repo.model.search.table.SearchAutocompleteRequest;
import org.sagebionetworks.repo.model.search.table.SearchIndex;
import org.sagebionetworks.repo.model.search.table.SearchIndexQuery;
import org.sagebionetworks.repo.model.search.table.SearchIndexState;
import org.sagebionetworks.repo.model.search.table.SearchIndexStatus;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.description.BenefactorDescription;
import org.sagebionetworks.table.cluster.description.IndexDescription;
import org.sagebionetworks.table.cluster.description.TableIndexDescription;
import org.sagebionetworks.table.cluster.search.SearchIndexStatusDao;

/**
 * Mockito unit tests for {@link SearchIndexQueryManagerImpl}. Covers the translation
 * helpers, state-machine gates ({@code checkIndexStatus}), response-part resolution,
 * and the full search/autocomplete flow with mocked dependencies. End-to-end behavior
 * against a live cluster is exercised separately by the {@code ITSearchQuery}
 * integration test.
 */
@ExtendWith(MockitoExtension.class)
public class SearchIndexQueryManagerImplTest {

	@Mock
	private EntityManager entityManager;
	@Mock
	private ConnectionFactory connectionFactory;
	@Mock
	private OpenSearchManager openSearchManager;
	@Mock
	private TableManagerSupport tableManagerSupport;
	@Mock
	private TableQueryManager tableQueryManager;
	@Mock
	private SearchIndexStatusDao searchIndexStatusDao;

	@InjectMocks
	private SearchIndexQueryManagerImpl manager;

	private UserInfo user;

	private static final String SEARCH_INDEX_ID = "1";
	private static final IdAndVersion SOURCE_ID = IdAndVersion.parse("syn456");
	private static final String NAME_COLUMN_ID = "111";
	private static final String DESC_COLUMN_ID = "222";
	private static final String NAME_COLUMN = "name";
	private static final String DESC_COLUMN = "description";

	@BeforeEach
	public void setUp() {
		user = new UserInfo(false, 999L, AuthorizationConstants.DEFAULT_REALM_ID);
	}

	private SearchIndex setupSearchIndex() {
		SearchIndex si = new SearchIndex();
		si.setId("1");
		si.setDefiningSQL("SELECT * FROM syn456");
		si.setParentId("syn789");
		return si;
	}

	private void setupAuthMocks() {
		// Preflight resolves the source IndexDescription and then checks read access. Stub both
		// so tests that only need auth to pass reach the index-status/query path under test.
		when(tableManagerSupport.getIndexDescription(SOURCE_ID))
				.thenReturn(new TableIndexDescription(SOURCE_ID));
		when(tableManagerSupport.validateTableReadAccess(any(), any())).thenReturn(AuthorizationStatus.authorized());
	}

	private SearchQuery buildBody() {
		// Minimal valid body: a match clause on the NAME_COLUMN, expressed as the opaque
		// OpenSearch DSL the manager forwards to OpenSearchManager. The manager doesn't
		// inspect the body any more than this — its own validation lives in
		// OpenSearchManagerImpl.executeSearch — so any non-null query slot works.
		return new SearchQuery().setQuery(matchQuery());
	}

	/** A minimal typed {@code match} query on {@link #NAME_COLUMN}. The manager forwards the body
	 *  to OpenSearchManager without inspecting it (and OpenSearchManager is mocked), so any
	 *  non-null query works. */
	private static Query matchQuery() {
		return new Query().setMatch(Map.of(NAME_COLUMN, new MatchFieldOptions().setQuery("test")));
	}

	/** A minimal typed {@code prefix} query on {@link #NAME_COLUMN}'s {@code .keyword} sub-field. */
	private static Query prefixQuery() {
		return new Query().setPrefix(
				Map.of(NAME_COLUMN + ".keyword", new PrefixFieldOptions().setValue("te")));
	}

	/** A typed {@code _source} filter ({@code {includes, excludes}}) for the SearchQuery body. */
	private static org.sagebionetworks.repo.model.search.dsl.SourceFilter sourceFilter(
			List<String> includes, List<String> excludes) {
		return new org.sagebionetworks.repo.model.search.dsl.SourceFilter()
				.setIncludes(includes).setExcludes(excludes);
	}

	/** Wrap a body in a SearchIndexQuery bound to {@link #SEARCH_INDEX_ID}. */
	private SearchIndexQuery buildRequest(SearchQuery body) {
		return new SearchIndexQuery().setSearchIndexId(SEARCH_INDEX_ID).setSearchQuery(body);
	}

	/** Wrap a body plus an explicit set of response parts. */
	private SearchIndexQuery buildRequest(SearchQuery body, SearchQueryPart... parts) {
		Set<SearchQueryPart> partSet = parts.length == 0
				? EnumSet.noneOf(SearchQueryPart.class)
				: EnumSet.copyOf(Arrays.asList(parts));
		return new SearchIndexQuery()
				.setSearchIndexId(SEARCH_INDEX_ID)
				.setSearchQuery(body)
				.setResponseParts(partSet);
	}

	/**
	 * Build a minimal autocomplete-shaped body: a {@code prefix} match on
	 * {@link #NAME_COLUMN}'s {@code .keyword} sub-field, wrapped in a {@code query}
	 * envelope. The autocomplete validator (now inside OpenSearchManager) requires the
	 * top-level clause inside {@code query} to be {@code prefix}, {@code match_phrase_prefix},
	 * or {@code match_bool_prefix} — but since OpenSearchManager is mocked here, that
	 * validation does not run.
	 */
	private static SearchAutocompleteBody buildAutocompleteBody() {
		return new SearchAutocompleteBody().setQuery(prefixQuery());
	}

	/** Build a minimal SearchAutocompleteRequest bound to {@link #SEARCH_INDEX_ID}. */
	private SearchAutocompleteRequest buildAutocompleteRequest() {
		return new SearchAutocompleteRequest()
				.setSearchIndexId(SEARCH_INDEX_ID)
				.setSearchQuery(buildAutocompleteBody());
	}

	/**
	 * Wires up the mocks needed for the full execute-query flow to succeed:
	 * status check, schema resolution. Returns the schema so tests can reference column
	 * names/IDs.
	 */
	private List<ColumnModel> setupHappyPathMocks() {
		List<ColumnModel> schema = Arrays.asList(
				TableModelTestUtils.createColumn(Long.parseLong(NAME_COLUMN_ID), NAME_COLUMN, ColumnType.STRING),
				TableModelTestUtils.createColumn(Long.parseLong(DESC_COLUMN_ID), DESC_COLUMN, ColumnType.STRING));
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(searchIndexStatusDao);
		when(searchIndexStatusDao.getStatus(1L)).thenReturn(Optional.of(
				new SearchIndexStatus().setSearchIndexId(SEARCH_INDEX_ID).setState(SearchIndexState.ACTIVE)));
		when(tableManagerSupport.getTableSchema(IdAndVersion.parse(SEARCH_INDEX_ID)))
				.thenReturn(schema);
		return schema;
	}

	/**
	 * Builds a SearchQueryResults shaped like what OpenSearchManager returns to this
	 * manager: hit field names are already user-facing column names (the OpenSearchManager
	 * does the column-id → column-name rewrite internally before returning).
	 */
	private SearchQueryResults buildRawResults() {
		SearchHit hit = new SearchHit();
		hit.setRowId(42L);
		hit.setFields(new ArrayList<>(Arrays.asList(
				new SearchFieldValue().setName(NAME_COLUMN).setValue("Alice"),
				new SearchFieldValue().setName(DESC_COLUMN).setValue("bio"))));
		return new SearchQueryResults().setTotalHits(1L).setHits(new ArrayList<>(Collections.singletonList(hit)));
	}

	/**
	 * Verifies {@code openSearchManager.search(...)} was called exactly once with the expected
	 * arguments. The opaque body is captured so the caller can assert on its contents; the
	 * {@code columns} list is captured and its names asserted against
	 * {@code expectedColumnNames}.
	 *
	 * <p>Using concrete matchers here instead of {@code any()} ensures the test actually
	 * verifies the values the manager passed — not merely that the method was invoked.
	 */
	private SearchQuery verifyOpenSearchSearch(Set<SearchQueryPart> expectedParts,
			List<String> expectedColumnNames) {
		ArgumentCaptor<SearchQuery> bodyCaptor = ArgumentCaptor.forClass(SearchQuery.class);
		@SuppressWarnings({"unchecked", "rawtypes"})
		ArgumentCaptor<List<ColumnModel>> columnsCaptor = (ArgumentCaptor) ArgumentCaptor.forClass(List.class);
		verify(openSearchManager).search(
				eq("search-index-1"),
				bodyCaptor.capture(),
				columnsCaptor.capture(),
				eq(expectedParts),
				eq(Collections.emptyList()));
		assertEquals(expectedColumnNames, columnsCaptor.getValue().stream()
				.map(ColumnModel::getName).collect(Collectors.toList()));
		return bodyCaptor.getValue();
	}

	/**
	 * Autocomplete analog of {@link #verifyOpenSearchSearch}.
	 */
	private SearchAutocompleteBody verifyOpenSearchAutocomplete(Set<SearchQueryPart> expectedParts,
			List<String> expectedColumnNames) {
		ArgumentCaptor<SearchAutocompleteBody> bodyCaptor = ArgumentCaptor.forClass(SearchAutocompleteBody.class);
		@SuppressWarnings({"unchecked", "rawtypes"})
		ArgumentCaptor<List<ColumnModel>> columnsCaptor = (ArgumentCaptor) ArgumentCaptor.forClass(List.class);
		verify(openSearchManager).autocomplete(
				eq("search-index-1"),
				bodyCaptor.capture(),
				columnsCaptor.capture(),
				eq(expectedParts),
				eq(Collections.emptyList()));
		assertEquals(expectedColumnNames, columnsCaptor.getValue().stream()
				.map(ColumnModel::getName).collect(Collectors.toList()));
		return bodyCaptor.getValue();
	}

	/**
	 * Stubs {@code openSearchManager.search(...)} to return {@code returnValue} when called
	 * with matching arguments. Uses concrete matchers throughout — no positional {@code any()} —
	 * so a manager that wires the wrong options or columns misses the stub, receives
	 * {@code null}, and fails the test explicitly. The manager forwards the opaque body
	 * unchanged, so we only assert that it is a Map carrying a {@code query} key.
	 */
	private void stubOpenSearchSearchReturns(Set<SearchQueryPart> expectedOptions,
			List<String> expectedColumnNames, SearchQueryResults returnValue) {
		when(openSearchManager.search(
				eq("search-index-1"),
				argThat(b -> b != null && b.getQuery() != null),
				argThat(cols -> cols != null && expectedColumnNames.equals(
						cols.stream().map(ColumnModel::getName).collect(Collectors.toList()))),
				eq(expectedOptions),
				eq(Collections.emptyList())
		)).thenReturn(returnValue);
	}

	/** Autocomplete analog of {@link #stubOpenSearchSearchReturns}. */
	private void stubOpenSearchAutocompleteReturns(Set<SearchQueryPart> expectedOptions,
			List<String> expectedColumnNames, SearchQueryResults returnValue) {
		when(openSearchManager.autocomplete(
				eq("search-index-1"),
				argThat(b -> b != null && b.getQuery() != null),
				argThat(cols -> cols != null && expectedColumnNames.equals(
						cols.stream().map(ColumnModel::getName).collect(Collectors.toList()))),
				eq(expectedOptions),
				eq(Collections.emptyList())
		)).thenReturn(returnValue);
	}

	@Test
	public void testSearchWithNoReadOnSearchIndex() {
		when(entityManager.getEntity(user, "1", SearchIndex.class))
				.thenThrow(new UnauthorizedException("no access"));

		assertThrows(UnauthorizedException.class, () -> manager.search(user, buildRequest(buildBody())));
		verifyNoMoreInteractions(connectionFactory, openSearchManager);
	}

	@Test
	public void testSearchWithNoReadOnSourceTable() {
		SearchIndex si = setupSearchIndex();
		when(entityManager.getEntity(user, "1", SearchIndex.class)).thenReturn(si);
		TableIndexDescription indexDescription = new TableIndexDescription(SOURCE_ID);
		when(tableManagerSupport.getIndexDescription(SOURCE_ID)).thenReturn(indexDescription);
		when(tableManagerSupport.validateTableReadAccess(user, indexDescription))
				.thenReturn(AuthorizationStatus.accessDenied("no access to source"));

		// call under test
		assertThrows(UnauthorizedException.class, () -> manager.search(user, buildRequest(buildBody())));
		verifyNoMoreInteractions(connectionFactory, openSearchManager);
	}

	@Test
	public void testSearchWithCreatingStatus() {
		SearchIndex si = setupSearchIndex();
		when(entityManager.getEntity(user, "1", SearchIndex.class)).thenReturn(si);
		setupAuthMocks();
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(searchIndexStatusDao);
		when(searchIndexStatusDao.getStatus(1L)).thenReturn(Optional.of(
				new SearchIndexStatus().setSearchIndexId(SEARCH_INDEX_ID).setState(SearchIndexState.CREATING)));

		IllegalStateException ex = assertThrows(IllegalStateException.class,
				() -> manager.search(user, buildRequest(buildBody())));
		assertTrue(ex.getMessage().contains("still building"));
		verifyNoMoreInteractions(openSearchManager);
	}

	@Test
	public void testSearchWithFailedStatusIncludesStoredErrorMessage() {
		SearchIndex si = setupSearchIndex();
		when(entityManager.getEntity(user, "1", SearchIndex.class)).thenReturn(si);
		setupAuthMocks();
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(searchIndexStatusDao);
		when(searchIndexStatusDao.getStatus(1L)).thenReturn(Optional.of(new SearchIndexStatus()
				.setSearchIndexId(SEARCH_INDEX_ID)
				.setState(SearchIndexState.FAILED)
				.setErrorMessage("Column 'bogus_col' does not exist.")));

		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> manager.search(user, buildRequest(buildBody())));

		assertTrue(ex.getMessage().contains("build failed"));
		assertTrue(ex.getMessage().contains("Column 'bogus_col' does not exist."),
				"Expected the stored error message to be forwarded to the user: " + ex.getMessage());
		assertTrue(ex.getMessage().contains("Delete or update the SearchIndex"));
		verifyNoMoreInteractions(openSearchManager);
	}

	@Test
	public void testSearchWithFailedStatusAndMissingErrorMessage() {
		SearchIndex si = setupSearchIndex();
		when(entityManager.getEntity(user, "1", SearchIndex.class)).thenReturn(si);
		setupAuthMocks();
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(searchIndexStatusDao);
		when(searchIndexStatusDao.getStatus(1L)).thenReturn(Optional.of(new SearchIndexStatus()
				.setSearchIndexId(SEARCH_INDEX_ID)
				.setState(SearchIndexState.FAILED)));

		// call under test — no stored error, fall back to the generic remediation hint
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> manager.search(user, buildRequest(buildBody())));

		assertTrue(ex.getMessage().contains("Delete or update the SearchIndex"));
		verifyNoMoreInteractions(openSearchManager);
	}

	@Test
	public void testSearchWithMissingStatus() {
		SearchIndex si = setupSearchIndex();
		when(entityManager.getEntity(user, "1", SearchIndex.class)).thenReturn(si);
		setupAuthMocks();
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(searchIndexStatusDao);
		when(searchIndexStatusDao.getStatus(1L)).thenReturn(Optional.empty());

		IllegalStateException ex = assertThrows(IllegalStateException.class,
				() -> manager.search(user, buildRequest(buildBody())));
		assertTrue(ex.getMessage().contains("still building"));
		verifyNoMoreInteractions(openSearchManager);
	}

	@Test
	public void testSearchWithActiveStatus() {
		SearchIndex si = setupSearchIndex();
		when(entityManager.getEntity(user, "1", SearchIndex.class)).thenReturn(si);
		setupAuthMocks();
		setupHappyPathMocks();
		stubOpenSearchSearchReturns(
				EnumSet.of(SearchQueryPart.HITS, SearchQueryPart.TOTAL_HITS),
				Arrays.asList(NAME_COLUMN, DESC_COLUMN), buildRawResults());

		SearchQuery body = buildBody();

		// call under test — request HITS + TOTAL_HITS so the assertions on totalHits work
		SearchQueryResults results = manager.search(user, buildRequest(body,
				SearchQueryPart.HITS, SearchQueryPart.TOTAL_HITS));

		assertNotNull(results);
		assertEquals(1L, results.getTotalHits());
		SearchHit hit = results.getHits().get(0);
		// OpenSearchManager already returns hit field names as user-facing column names; the
		// query-manager just forwards them.
		assertEquals(NAME_COLUMN, hit.getFields().get(0).getName());
		assertEquals(DESC_COLUMN, hit.getFields().get(1).getName());

		verifyOpenSearchSearch(
				EnumSet.of(SearchQueryPart.HITS, SearchQueryPart.TOTAL_HITS),
				Arrays.asList(NAME_COLUMN, DESC_COLUMN));
	}

	@Test
	public void testAutocompleteWithActiveStatusDispatchesToOpenSearchManager() {
		// The manager hardcodes the response parts to HITS and forwards the caller's
		// opaque body unchanged.
		SearchIndex si = setupSearchIndex();
		when(entityManager.getEntity(user, "1", SearchIndex.class)).thenReturn(si);
		setupAuthMocks();
		setupHappyPathMocks();
		stubOpenSearchAutocompleteReturns(
				EnumSet.of(SearchQueryPart.HITS),
				Arrays.asList(NAME_COLUMN, DESC_COLUMN), buildRawResults());

		// call under test
		SearchQueryResults results = manager.autocomplete(user, buildAutocompleteRequest());

		assertNotNull(results);
		assertEquals(NAME_COLUMN, results.getHits().get(0).getFields().get(0).getName());

		// HITS is the only resolved part — the slim request carries no responseParts knob.
		// The forwarded body is the caller's body, untouched. The autocomplete envelope is
		// already restricted to query + _source by its schema, so the absence of any other
		// slot is structural rather than something the manager needs to enforce.
		SearchAutocompleteBody forwarded = verifyOpenSearchAutocomplete(
				EnumSet.of(SearchQueryPart.HITS),
				Arrays.asList(NAME_COLUMN, DESC_COLUMN));
		assertNotNull(forwarded.getQuery());
		assertNull(forwarded.get_source());
	}

	@Test
	public void testAutocompleteWithReturnFieldsForwardsToOpenSearchManager() {
		// Caller supplies _source.includes inside the body; the manager forwards it unchanged.
		SearchIndex si = setupSearchIndex();
		when(entityManager.getEntity(user, "1", SearchIndex.class)).thenReturn(si);
		setupAuthMocks();
		setupHappyPathMocks();
		stubOpenSearchAutocompleteReturns(
				EnumSet.of(SearchQueryPart.HITS),
				Arrays.asList(NAME_COLUMN, DESC_COLUMN), buildRawResults());

		org.sagebionetworks.repo.model.search.dsl.SourceFilter source =
				sourceFilter(new ArrayList<>(Arrays.asList(NAME_COLUMN)), null);
		SearchAutocompleteBody body = new SearchAutocompleteBody()
				.setQuery(prefixQuery()).set_source(source);

		// call under test
		manager.autocomplete(user, new SearchAutocompleteRequest()
				.setSearchIndexId(SEARCH_INDEX_ID)
				.setSearchQuery(body));

		SearchAutocompleteBody forwarded = verifyOpenSearchAutocomplete(
				EnumSet.of(SearchQueryPart.HITS),
				Arrays.asList(NAME_COLUMN, DESC_COLUMN));
		assertEquals(source, forwarded.get_source());
	}

	// --- Focused unit tests for package-protected helpers ---

	@Test
	public void testGetIndexNameAppliesPrefix() {
		// call under test — AOSS index name is derived from the SearchIndex entity ID.
		assertEquals("search-index-syn123", manager.getIndexName("syn123"));
	}

	// --- checkIndexStatus (state machine) ---

	@Test
	public void testCheckIndexStatusWithMissingThrowsRecoverable() {
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(searchIndexStatusDao);
		when(searchIndexStatusDao.getStatus(123L)).thenReturn(Optional.empty());

		// call under test — missing status row means a build hasn't completed yet.
		IllegalStateException e = assertThrows(IllegalStateException.class,
				() -> manager.checkIndexStatus("syn123"));
		assertTrue(e.getMessage().contains("still building"),
				"Caller (worker) translates 'still building' → RecoverableMessageException: " + e.getMessage());
	}

	@Test
	public void testCheckIndexStatusWithCreatingThrowsRecoverable() {
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(searchIndexStatusDao);
		when(searchIndexStatusDao.getStatus(123L)).thenReturn(Optional.of(
				new SearchIndexStatus().setState(SearchIndexState.CREATING)));

		// call under test
		IllegalStateException e = assertThrows(IllegalStateException.class,
				() -> manager.checkIndexStatus("syn123"));
		assertTrue(e.getMessage().contains("still building"));
	}

	@Test
	public void testCheckIndexStatusWithActiveReturnsNormally() {
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(searchIndexStatusDao);
		when(searchIndexStatusDao.getStatus(123L)).thenReturn(Optional.of(
				new SearchIndexStatus().setState(SearchIndexState.ACTIVE)));

		// call under test
		manager.checkIndexStatus("syn123");
		// No exception means happy path. Nothing else to assert.
	}

	@Test
	public void testCheckIndexStatusWithFailedSurfacesStoredErrorMessage() {
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(searchIndexStatusDao);
		when(searchIndexStatusDao.getStatus(123L)).thenReturn(Optional.of(
				new SearchIndexStatus().setState(SearchIndexState.FAILED)
						.setErrorMessage("TextAnalyzer 'biomed-ghost' (defaultAnalyzer) does not resolve.")));

		// call under test
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> manager.checkIndexStatus("syn123"));

		assertTrue(e.getMessage().contains("biomed-ghost"),
				"Stored error message must be surfaced verbatim: " + e.getMessage());
		assertTrue(e.getMessage().contains("Delete or update the SearchIndex"),
				"Remediation hint must be appended: " + e.getMessage());
	}

	@Test
	public void testCheckIndexStatusWithFailedAndBlankErrorUsesGenericMessage() {
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(searchIndexStatusDao);
		when(searchIndexStatusDao.getStatus(123L)).thenReturn(Optional.of(
				new SearchIndexStatus().setState(SearchIndexState.FAILED).setErrorMessage("")));

		// call under test
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> manager.checkIndexStatus("syn123"));

		assertTrue(e.getMessage().contains("Delete or update the SearchIndex to trigger a rebuild."),
				"Generic remediation hint must replace blank stored error: " + e.getMessage());
	}

	@ParameterizedTest
	@EnumSource(value = SearchIndexState.class, names = {"CREATING"})
	public void testCheckIndexStatusTransitionalStatesThrowIllegalState(SearchIndexState state) {
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(searchIndexStatusDao);
		when(searchIndexStatusDao.getStatus(123L)).thenReturn(Optional.of(
				new SearchIndexStatus().setState(state)));

		// call under test — every transitional state must surface IllegalStateException so the
		// worker can translate to RecoverableMessageException.
		assertThrows(IllegalStateException.class, () -> manager.checkIndexStatus("syn123"));
	}

	// --- buildQueryMetadata ---

	@Test
	public void testBuildQueryMetadataThrowsWhenNoSchemaBound() {
		IdAndVersion id = IdAndVersion.parse("syn123");
		when(tableManagerSupport.getTableSchema(id)).thenReturn(Collections.emptyList());

		// call under test
		IllegalStateException e = assertThrows(IllegalStateException.class,
				() -> manager.buildQueryMetadata(id));

		assertTrue(e.getMessage().contains("no bound schema"),
				"Exception must hint at the recovery path: " + e.getMessage());
	}

	@Test
	public void testBuildQueryMetadataReturnsParallelLists() {
		IdAndVersion id = IdAndVersion.parse("syn123");
		List<ColumnModel> columns = Arrays.asList(
				new ColumnModel().setId("col-1").setName("title").setColumnType(ColumnType.STRING),
				new ColumnModel().setId("col-2").setName("year").setColumnType(ColumnType.INTEGER));
		when(tableManagerSupport.getTableSchema(id)).thenReturn(columns);

		// call under test
		SearchIndexQueryManagerImpl.QueryMetadata metadata = manager.buildQueryMetadata(id);

		assertEquals(columns, metadata.getColumns());
		assertEquals(2, metadata.getSelectColumns().size());
	}

	// --- responseParts: resolveRequestedParts ---

	@Test
	public void testResolveRequestedPartsWithNullReturnsHitsOnly() {
		// call under test — default minimal payload is HITS only.
		Set<SearchQueryPart> result = SearchIndexQueryManagerImpl.resolveRequestedParts(null);

		assertEquals(EnumSet.of(SearchQueryPart.HITS), result);
	}

	@Test
	public void testResolveRequestedPartsWithEmptyReturnsHitsOnly() {
		// call under test
		Set<SearchQueryPart> result = SearchIndexQueryManagerImpl.resolveRequestedParts(Collections.emptySet());

		assertEquals(EnumSet.of(SearchQueryPart.HITS), result);
	}

	@Test
	public void testResolveRequestedPartsCopiesInputAsEnumSet() {
		Set<SearchQueryPart> input = new java.util.HashSet<>(
				Arrays.asList(SearchQueryPart.HITS, SearchQueryPart.TOTAL_HITS, SearchQueryPart.SELECT_COLUMNS));
		// call under test
		Set<SearchQueryPart> result = SearchIndexQueryManagerImpl.resolveRequestedParts(input);

		assertEquals(input, result);
		assertTrue(result instanceof EnumSet, "Result must be an EnumSet for O(1) contains");
	}

	// --- responseParts: filterSelectColumnsForSourceFilter ---

	@Test
	public void testFilterSelectColumnsForSourceFilterWithNullColumnsReturnsNull() {
		// call under test
		assertNull(manager.filterSelectColumnsForSourceFilter(
				null, SourceFilter.of(b -> b.includes("a"))));
	}

	@Test
	public void testFilterSelectColumnsForSourceFilterWithNullFilterKeepsAll() {
		List<SelectColumn> input = Arrays.asList(
				new SelectColumn().setName("title"), new SelectColumn().setName("abstract"));
		// call under test — null filter → keep everything.
		assertEquals(input, manager.filterSelectColumnsForSourceFilter(input, null));
	}

	@Test
	public void testFilterSelectColumnsForSourceFilterWithEmptyFilterKeepsAll() {
		List<SelectColumn> input = Arrays.asList(new SelectColumn().setName("title"));
		// call under test — filter with neither includes nor excludes → keep everything.
		assertEquals(input, manager.filterSelectColumnsForSourceFilter(
				input, SourceFilter.of(b -> b)));
	}

	@Test
	public void testFilterSelectColumnsForSourceFilterIncludesKeepsOrderAndFilters() {
		List<SelectColumn> input = Arrays.asList(
				new SelectColumn().setName("title"),
				new SelectColumn().setName("abstract"),
				new SelectColumn().setName("authors"));
		// call under test — names not in includes drop; SELECT order preserved.
		List<SelectColumn> result = manager.filterSelectColumnsForSourceFilter(
				input, SourceFilter.of(b -> b.includes("authors", "title")));

		assertEquals(Arrays.asList("title", "authors"),
				result.stream().map(SelectColumn::getName).collect(Collectors.toList()));
	}

	@Test
	public void testFilterSelectColumnsForSourceFilterExcludesDropsNamedColumns() {
		List<SelectColumn> input = Arrays.asList(
				new SelectColumn().setName("title"),
				new SelectColumn().setName("abstract"),
				new SelectColumn().setName("authors"));
		// call under test — excludes drop named columns; remaining order preserved.
		List<SelectColumn> result = manager.filterSelectColumnsForSourceFilter(
				input, SourceFilter.of(b -> b.excludes("abstract")));

		assertEquals(Arrays.asList("title", "authors"),
				result.stream().map(SelectColumn::getName).collect(Collectors.toList()));
	}

	@Test
	public void testFilterSelectColumnsForSourceFilterIncludesAndExcludesIntersect() {
		List<SelectColumn> input = Arrays.asList(
				new SelectColumn().setName("title"),
				new SelectColumn().setName("abstract"),
				new SelectColumn().setName("authors"));
		// call under test — survive only when included AND not excluded.
		List<SelectColumn> result = manager.filterSelectColumnsForSourceFilter(
				input, SourceFilter.of(b -> b.includes("title", "abstract").excludes("abstract")));

		assertEquals(Arrays.asList("title"),
				result.stream().map(SelectColumn::getName).collect(Collectors.toList()));
	}

	// --- State-table: responseParts → which response fields populated ---

	/**
	 * Mocks the openSearchManager to return a fully-populated SearchQueryResults regardless
	 * of which parts are requested. The manager's own gating then determines which fields
	 * survive — that's what we want to assert here. (Real OpenSearchManagerImpl populates
	 * only the requested fields; the AutoWired test covers that.)
	 */
	private SearchQueryResults rawHits() {
		SearchHit hit = new SearchHit();
		hit.setRowId(1L);
		hit.setFields(new ArrayList<>(Arrays.asList(
				new SearchFieldValue().setName(NAME_COLUMN).setValue("Alice"))));
		// OpenSearchManager returns aggregationResults as an opaque JSON string with field
		// references already rewritten to column names; the manager just forwards it.
		return new SearchQueryResults()
				.setHits(new ArrayList<>(Arrays.asList(hit)))
				.setTotalHits(7L)
				.setAggregationResults("{\"" + NAME_COLUMN + "\":{\"buckets\":[]}}")
				.setOffset(0L);
	}

	@Test
	public void testSearchWithDefaultPartsReturnsHitsOnly() {
		SearchIndex si = setupSearchIndex();
		when(entityManager.getEntity(user, "1", SearchIndex.class)).thenReturn(si);
		setupAuthMocks();
		setupHappyPathMocks();
		// Raw results without aggregations — mirrors what OpenSearchManager returns when
		// the body did not supply aggregations.
		SearchQueryResults raw = rawHits().setAggregationResults(null);
		stubOpenSearchSearchReturns(
				EnumSet.of(SearchQueryPart.HITS),
				Arrays.asList(NAME_COLUMN, DESC_COLUMN), raw);

		// call under test — buildRequest with no parts ⇒ default minimal payload
		SearchQueryResults results = manager.search(user, buildRequest(buildBody()));

		assertNotNull(results.getHits());
		assertNull(results.getTotalHits(),     "totalHits should be null when TOTAL_HITS not requested");
		assertNull(results.getSelectColumns(), "selectColumns should be null when SELECT_COLUMNS not requested");
		assertNull(results.getAggregationResults(),
				"aggregationResults should be null when body.aggregations was not supplied");
		assertEquals(0L, results.getOffset(),  "offset is always populated");
	}

	@Test
	public void testSearchWithAllPartsRequested() {
		SearchIndex si = setupSearchIndex();
		when(entityManager.getEntity(user, "1", SearchIndex.class)).thenReturn(si);
		setupAuthMocks();
		setupHappyPathMocks();
		stubOpenSearchSearchReturns(
				EnumSet.of(SearchQueryPart.HITS, SearchQueryPart.TOTAL_HITS,
						SearchQueryPart.SELECT_COLUMNS),
				Arrays.asList(NAME_COLUMN, DESC_COLUMN), rawHits());

		// call under test — request every SearchQueryPart. Aggregations come back because
		// the raw OpenSearchManager response carried them (presence-driven), not because
		// of an enum bit.
		SearchQueryResults results = manager.search(user, buildRequest(buildBody(),
				SearchQueryPart.HITS, SearchQueryPart.TOTAL_HITS,
				SearchQueryPart.SELECT_COLUMNS));

		assertNotNull(results.getHits());
		assertEquals(7L, results.getTotalHits());
		assertNotNull(results.getSelectColumns());
		assertNotNull(results.getAggregationResults(),
				"aggregationResults should be forwarded when the raw response carried them");
		assertEquals(0L, results.getOffset());
	}

	@Test
	public void testSearchAggregationResultsForwardedWhenBodySuppliedAggregations() {
		// Aggregations are presence-driven by the body, not by a SearchQueryPart bit.
		SearchIndex si = setupSearchIndex();
		when(entityManager.getEntity(user, "1", SearchIndex.class)).thenReturn(si);
		setupAuthMocks();
		setupHappyPathMocks();
		stubOpenSearchSearchReturns(
				EnumSet.of(SearchQueryPart.HITS),
				Arrays.asList(NAME_COLUMN, DESC_COLUMN), rawHits());

		// call under test — only HITS requested, but raw results carry aggregationResults.
		SearchQueryResults results = manager.search(user, buildRequest(buildBody()));

		assertNotNull(results.getAggregationResults(),
				"aggregationResults must be forwarded whenever the raw response carries them");
	}

	@Test
	public void testSearchAggregationResultsNullWhenBodyHadNoAggregations() {
		SearchIndex si = setupSearchIndex();
		when(entityManager.getEntity(user, "1", SearchIndex.class)).thenReturn(si);
		setupAuthMocks();
		setupHappyPathMocks();
		SearchQueryResults raw = rawHits().setAggregationResults(null);
		stubOpenSearchSearchReturns(
				EnumSet.of(SearchQueryPart.HITS),
				Arrays.asList(NAME_COLUMN, DESC_COLUMN), raw);

		// call under test
		SearchQueryResults results = manager.search(user, buildRequest(buildBody()));

		assertNull(results.getAggregationResults(),
				"aggregationResults must be null when the raw response carries none");
	}

	@Test
	public void testSearchWithSelectColumnsOnly() {
		SearchIndex si = setupSearchIndex();
		when(entityManager.getEntity(user, "1", SearchIndex.class)).thenReturn(si);
		setupAuthMocks();
		setupHappyPathMocks();
		// Raw response without aggregations — body had no aggregations, so OpenSearchManager returns
		// aggregationResults = null.
		SearchQueryResults raw = rawHits().setAggregationResults(null);
		stubOpenSearchSearchReturns(
				EnumSet.of(SearchQueryPart.SELECT_COLUMNS),
				Arrays.asList(NAME_COLUMN, DESC_COLUMN), raw);

		// call under test
		SearchQueryResults results = manager.search(user, buildRequest(buildBody(), SearchQueryPart.SELECT_COLUMNS));

		assertNull(results.getHits(),     "hits should be null when HITS not requested");
		assertNull(results.getTotalHits());
		assertNotNull(results.getSelectColumns(),
				"selectColumns should be populated when SELECT_COLUMNS is requested");
		assertNull(results.getAggregationResults());
		assertEquals(0L, results.getOffset());
	}

	@Test
	public void testSearchWithSelectColumnsAndSourceIncludes() {
		// _source.includes narrows the SELECT_COLUMNS response to the named subset.
		SearchIndex si = setupSearchIndex();
		when(entityManager.getEntity(user, "1", SearchIndex.class)).thenReturn(si);
		setupAuthMocks();
		setupHappyPathMocks();
		stubOpenSearchSearchReturns(
				EnumSet.of(SearchQueryPart.SELECT_COLUMNS),
				Arrays.asList(NAME_COLUMN, DESC_COLUMN), rawHits());

		org.sagebionetworks.repo.model.search.dsl.SourceFilter source =
				sourceFilter(new ArrayList<>(Arrays.asList(NAME_COLUMN)), null);
		SearchQuery body = new SearchQuery().setQuery(matchQuery()).set_source(source);

		// call under test
		SearchQueryResults results = manager.search(user,
				buildRequest(body, SearchQueryPart.SELECT_COLUMNS));

		assertEquals(1, results.getSelectColumns().size());
		assertEquals(NAME_COLUMN, results.getSelectColumns().get(0).getName());
	}

	@Test
	public void testSearchWithSelectColumnsAndSourceExcludes() {
		// _source.excludes drops the named columns from the SELECT_COLUMNS response. The
		// previous implementation ignored excludes entirely and surfaced the column even
		// though AOSS already omitted its value from the hit.
		SearchIndex si = setupSearchIndex();
		when(entityManager.getEntity(user, "1", SearchIndex.class)).thenReturn(si);
		setupAuthMocks();
		setupHappyPathMocks();
		stubOpenSearchSearchReturns(
				EnumSet.of(SearchQueryPart.SELECT_COLUMNS),
				Arrays.asList(NAME_COLUMN, DESC_COLUMN), rawHits());

		org.sagebionetworks.repo.model.search.dsl.SourceFilter source =
				sourceFilter(null, new ArrayList<>(Arrays.asList(DESC_COLUMN)));
		SearchQuery body = new SearchQuery().setQuery(matchQuery()).set_source(source);

		// call under test
		SearchQueryResults results = manager.search(user,
				buildRequest(body, SearchQueryPart.SELECT_COLUMNS));

		assertEquals(1, results.getSelectColumns().size());
		assertEquals(NAME_COLUMN, results.getSelectColumns().get(0).getName());
	}

	@Test
	public void testSearchWithSelectColumnsAndSourceIncludesAndExcludes() {
		// Both clauses combine: a column survives only if it matches includes AND is not
		// in excludes.
		SearchIndex si = setupSearchIndex();
		when(entityManager.getEntity(user, "1", SearchIndex.class)).thenReturn(si);
		setupAuthMocks();
		setupHappyPathMocks();
		stubOpenSearchSearchReturns(
				EnumSet.of(SearchQueryPart.SELECT_COLUMNS),
				Arrays.asList(NAME_COLUMN, DESC_COLUMN), rawHits());

		org.sagebionetworks.repo.model.search.dsl.SourceFilter source = sourceFilter(
				new ArrayList<>(Arrays.asList(NAME_COLUMN, DESC_COLUMN)),
				new ArrayList<>(Arrays.asList(DESC_COLUMN)));
		SearchQuery body = new SearchQuery().setQuery(matchQuery()).set_source(source);

		// call under test
		SearchQueryResults results = manager.search(user,
				buildRequest(body, SearchQueryPart.SELECT_COLUMNS));

		assertEquals(1, results.getSelectColumns().size());
		assertEquals(NAME_COLUMN, results.getSelectColumns().get(0).getName());
	}

	@Test
	public void testSearchWithSelectColumnsAndNoSourceFilter() {
		// No _source key means no narrowing; full SELECT-clause survives.
		SearchIndex si = setupSearchIndex();
		when(entityManager.getEntity(user, "1", SearchIndex.class)).thenReturn(si);
		setupAuthMocks();
		setupHappyPathMocks();
		stubOpenSearchSearchReturns(
				EnumSet.of(SearchQueryPart.SELECT_COLUMNS),
				Arrays.asList(NAME_COLUMN, DESC_COLUMN), rawHits());

		// call under test — body has no _source key (buildBody() omits it).
		SearchQueryResults results = manager.search(user,
				buildRequest(buildBody(), SearchQueryPart.SELECT_COLUMNS));

		assertEquals(2, results.getSelectColumns().size());
		assertEquals(NAME_COLUMN, results.getSelectColumns().get(0).getName());
		assertEquals(DESC_COLUMN, results.getSelectColumns().get(1).getName());
	}

	@Test
	public void testSearchPassesResolvedPartsToOpenSearchManager() {
		SearchIndex si = setupSearchIndex();
		when(entityManager.getEntity(user, "1", SearchIndex.class)).thenReturn(si);
		setupAuthMocks();
		setupHappyPathMocks();
		stubOpenSearchSearchReturns(
				EnumSet.of(SearchQueryPart.HITS, SearchQueryPart.TOTAL_HITS),
				Arrays.asList(NAME_COLUMN, DESC_COLUMN), rawHits());

		// call under test — request HITS + TOTAL_HITS
		manager.search(user, buildRequest(buildBody(), SearchQueryPart.HITS, SearchQueryPart.TOTAL_HITS));

		// Verify the manager forwarded the resolved EnumSet to OpenSearchManager unchanged.
		// verifyOpenSearchSearch uses eq(expectedParts) on the parts slot, so this assertion
		// is enforced through matcher equality rather than a separate captor.
		verifyOpenSearchSearch(
				EnumSet.of(SearchQueryPart.HITS, SearchQueryPart.TOTAL_HITS),
				Arrays.asList(NAME_COLUMN, DESC_COLUMN));
	}

	// --- Autocomplete: only HITS is ever populated (caller cannot opt in to extras) ---

	@Test
	public void testAutocompleteAlwaysReturnsHitsOnly() {
		SearchIndex si = setupSearchIndex();
		when(entityManager.getEntity(user, "1", SearchIndex.class)).thenReturn(si);
		setupAuthMocks();
		setupHappyPathMocks();
		// rawHits carries totalHits / aggregationResults too — assert the manager strips them.
		stubOpenSearchAutocompleteReturns(
				EnumSet.of(SearchQueryPart.HITS),
				Arrays.asList(NAME_COLUMN, DESC_COLUMN), rawHits());

		// call under test
		SearchQueryResults results = manager.autocomplete(user, buildAutocompleteRequest());

		assertNotNull(results.getHits());
		assertNull(results.getTotalHits(),
				"autocomplete must not surface totalHits regardless of OpenSearchManager output");
		assertNull(results.getSelectColumns());
		assertNull(results.getAggregationResults(),
				"autocomplete must not surface aggregationResults regardless of OpenSearchManager output");
		assertEquals(0L, results.getOffset());
	}

	// --- Validation tests ---

	@Test
	public void testAutocompleteWithNullRequestThrows() {
		// call under test
		assertThrows(IllegalArgumentException.class, () -> manager.autocomplete(user, null));
		verifyNoMoreInteractions(entityManager, connectionFactory, openSearchManager, tableManagerSupport, tableQueryManager);
	}

	@Test
	public void testAutocompleteWithNullSearchIndexIdThrows() {
		SearchAutocompleteRequest request = new SearchAutocompleteRequest().setSearchQuery(buildAutocompleteBody());

		// call under test
		assertThrows(IllegalArgumentException.class, () -> manager.autocomplete(user, request));
		verifyNoMoreInteractions(entityManager, connectionFactory, openSearchManager, tableManagerSupport, tableQueryManager);
	}

	@Test
	public void testAutocompleteWithNullSearchQueryThrows() {
		SearchAutocompleteRequest request = new SearchAutocompleteRequest().setSearchIndexId(SEARCH_INDEX_ID);

		// call under test
		assertThrows(IllegalArgumentException.class, () -> manager.autocomplete(user, request));
		verifyNoMoreInteractions(entityManager, connectionFactory, openSearchManager, tableManagerSupport, tableQueryManager);
	}

	@Test
	public void testSearchWithNullRequestThrows() {
		// call under test
		assertThrows(IllegalArgumentException.class, () -> manager.search(user, null));
	}

	@Test
	public void testSearchWithNullSearchIndexIdThrows() {
		SearchIndexQuery request = new SearchIndexQuery().setSearchQuery(buildBody());

		// call under test
		assertThrows(IllegalArgumentException.class, () -> manager.search(user, request));
	}

	// A bound literal column with a synthetic id round-trips through the query path
	// without tripping `Collectors.toMap`'s no-null-values rule when nameToId is built.
	// The alias intentionally differs from the literal value so the rename is
	// observable in the bound schema reaching OpenSearch.
	@Test
	public void testSearchWithLiteralColumnInDefiningSqlAssignsSyntheticId() {
		SearchIndex si = setupSearchIndex();
		si.setDefiningSQL("SELECT name, 'tag' as tag_alias FROM syn456");
		when(entityManager.getEntity(user, "1", SearchIndex.class)).thenReturn(si);
		setupAuthMocks();
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(searchIndexStatusDao);
		when(searchIndexStatusDao.getStatus(1L)).thenReturn(Optional.of(
				new SearchIndexStatus().setSearchIndexId(SEARCH_INDEX_ID).setState(SearchIndexState.ACTIVE)));
		ColumnModel nameCol = TableModelTestUtils.createColumn(
				Long.parseLong(NAME_COLUMN_ID), NAME_COLUMN, ColumnType.STRING);
		ColumnModel tagAliasCol = new ColumnModel().setId("999").setName("tag_alias")
				.setColumnType(ColumnType.STRING).setMaximumSize(50L);
		when(tableManagerSupport.getTableSchema(IdAndVersion.parse(SEARCH_INDEX_ID)))
				.thenReturn(Arrays.asList(nameCol, tagAliasCol));
		@SuppressWarnings({"unchecked", "rawtypes"})
		ArgumentCaptor<List<ColumnModel>> columnsCaptor = (ArgumentCaptor) ArgumentCaptor.forClass(List.class);
		when(openSearchManager.search(eq("search-index-1"),
				argThat(b -> b != null && b.getQuery() != null), columnsCaptor.capture(),
				eq(EnumSet.of(SearchQueryPart.HITS)), eq(Collections.emptyList())))
				.thenReturn(new SearchQueryResults().setHits(Collections.emptyList()));

		// call under test
		manager.search(user, buildRequest(buildBody()));

		// The bound list with both real-id and synthetic-id columns reached OpenSearch — the
		// caller's body is opaque, so the column list is what carries the schema information
		// the OpenSearchManager needs for name→id rewriting.
		assertEquals(Arrays.asList(nameCol, tagAliasCol), columnsCaptor.getValue());
		// The schema is read once via `getTableSchema(searchIndexId)` — no per-request
		// QueryTranslator construction. Verify the new code path is taken.
		verify(tableManagerSupport).getTableSchema(IdAndVersion.parse(SEARCH_INDEX_ID));
	}

	@Test
	public void testSearchWhenSearchIndexHasNoBoundSchemaThrows() {
		SearchIndex si = setupSearchIndex();
		when(entityManager.getEntity(user, "1", SearchIndex.class)).thenReturn(si);
		setupAuthMocks();
		when(connectionFactory.getSearchIndexStatusDao()).thenReturn(searchIndexStatusDao);
		when(searchIndexStatusDao.getStatus(1L)).thenReturn(Optional.of(
				new SearchIndexStatus().setSearchIndexId(SEARCH_INDEX_ID).setState(SearchIndexState.ACTIVE)));
		// SearchIndex created on a stack before `registerSchema` existed — no bound columns.
		when(tableManagerSupport.getTableSchema(IdAndVersion.parse(SEARCH_INDEX_ID)))
				.thenReturn(Collections.emptyList());

		// call under test — clear failure rather than NPE in nameToId construction.
		IllegalStateException ex = assertThrows(IllegalStateException.class,
				() -> manager.search(user, buildRequest(buildBody())));
		assertTrue(ex.getMessage().contains("no bound schema"),
				"expected 'no bound schema' in message, got: " + ex.getMessage());
		verifyNoMoreInteractions(openSearchManager);
	}

	// ===================== branch coverage: extractSourceFilter =====================

	/**
	 * One test exercising every shape the typed {@code body._source} ({@code SourceFilter}) can
	 * take: absent, includes-only, excludes-only, and both. The helper deserializes the typed
	 * {@code {includes, excludes}} (the native OpenSearch {@code SourceFilter} shape) straight
	 * through — this covers every branch on the manager side.
	 */
	@Test
	public void testExtractSourceFilterWithEverySourceShape() {
		// null body → null (defensive guard)
		assertNull(SearchIndexQueryManagerImpl.extractSourceFilter(null),
				"null body → null (defensive guard)");

		// absent _source → null
		assertNull(SearchIndexQueryManagerImpl.extractSourceFilter(new SearchQuery()),
				"absent _source → null");

		// includes only.
		SourceFilter includesOnly = SearchIndexQueryManagerImpl.extractSourceFilter(
				new SearchQuery().set_source(sourceFilter(Arrays.asList("title", "name"), null)));
		assertNotNull(includesOnly);
		assertEquals(Arrays.asList("title", "name"), includesOnly.includes());

		// excludes only.
		SourceFilter excludesFilter = SearchIndexQueryManagerImpl.extractSourceFilter(
				new SearchQuery().set_source(sourceFilter(null, Arrays.asList("private"))));
		assertNotNull(excludesFilter);
		assertEquals(Arrays.asList("private"), excludesFilter.excludes());

		// both includes and excludes.
		SourceFilter both = SearchIndexQueryManagerImpl.extractSourceFilter(
				new SearchQuery().set_source(
						sourceFilter(Arrays.asList("title", "name"), Arrays.asList("private"))));
		assertNotNull(both);
		assertEquals(Arrays.asList("title", "name"), both.includes());
		assertEquals(Arrays.asList("private"), both.excludes());
	}

	// ===================== branch coverage: SearchQueryPart powerset =====================

	/**
	 * Drive the full search() path with every subset of {@link SearchQueryPart}, asserting
	 * that the manager's per-part gates write the expected fields onto the result. EnumSet
	 * coverage guard at the bottom — adding a new SearchQueryPart value without a fixture
	 * fails the test until the assertion table is updated.
	 *
	 * <p>The mocked OpenSearchManager always returns a fully-populated raw response so we
	 * can isolate the manager's gating from the OpenSearchManager's gating (which is
	 * separately covered in OpenSearchManagerImplTest).</p>
	 */
	@Test
	public void testSearchWithEverySearchQueryPartCombination() {
		EnumSet<SearchQueryPart> guard = EnumSet.noneOf(SearchQueryPart.class);
		SearchQueryPart[] all = SearchQueryPart.values();
		for (int mask = 0; mask < (1 << all.length); mask++) {
			EnumSet<SearchQueryPart> parts = EnumSet.noneOf(SearchQueryPart.class);
			for (int b = 0; b < all.length; b++) {
				if ((mask & (1 << b)) != 0) {
					parts.add(all[b]);
					guard.add(all[b]);
				}
			}

			// Reset mocks per iteration so each scenario starts fresh.
			reset(entityManager, connectionFactory, openSearchManager,
					tableManagerSupport, tableQueryManager, searchIndexStatusDao);

			SearchIndex si = setupSearchIndex();
			when(entityManager.getEntity(user, "1", SearchIndex.class)).thenReturn(si);
			setupAuthMocks();
			List<ColumnModel> schema = setupHappyPathMocks();

			java.util.Set<SearchQueryPart> resolved = parts.isEmpty()
					? EnumSet.of(SearchQueryPart.HITS) : EnumSet.copyOf(parts);
			stubOpenSearchSearchReturns(resolved,
					schema.stream().map(ColumnModel::getName).collect(Collectors.toList()),
					rawHits());

			// call under test
			SearchQueryResults results = manager.search(user, buildRequest(buildBody(),
					parts.toArray(new SearchQueryPart[0])));

			// HITS gate
			boolean expectHits = resolved.contains(SearchQueryPart.HITS);
			assertEquals(expectHits, results.getHits() != null, "HITS gate, mask=" + mask);

			// TOTAL_HITS gate
			assertEquals(resolved.contains(SearchQueryPart.TOTAL_HITS),
					results.getTotalHits() != null, "TOTAL_HITS gate, mask=" + mask);

			// SELECT_COLUMNS gate
			assertEquals(resolved.contains(SearchQueryPart.SELECT_COLUMNS),
					results.getSelectColumns() != null, "SELECT_COLUMNS gate, mask=" + mask);

			// aggregations are not gated by SearchQueryPart — they pass through
			// whenever the raw response carried them. rawHits() carries aggregationResults.
			assertNotNull(results.getAggregationResults(),
					"aggregations always pass through, mask=" + mask);
		}
		assertEquals(EnumSet.allOf(SearchQueryPart.class), guard,
				"every SearchQueryPart must be exercised across the powerset");
	}

	// ===================== buildBenefactorAccessFilters =====================

	@Test
	public void testBuildBenefactorAccessFiltersWithNoBenefactors() {
		// A table source has no benefactors → no filter → reproduces public-data behavior.
		IndexDescription source = new TableIndexDescription(SOURCE_ID);

		// call under test
		List<org.opensearch.client.opensearch._types.query_dsl.Query> filters =
				manager.buildBenefactorAccessFilters(user, source);

		assertEquals(Collections.emptyList(), filters);
		verifyNoMoreInteractions(connectionFactory);
	}

	@Test
	public void testBuildBenefactorAccessFiltersWithMultipleDependencies() {
		// A materialized view with two benefactor-bearing dependencies produces one terms
		// filter per dependency, in getBenefactors() order, each on field _benefactor_i and
		// always including the -1 sentinel.
		IndexDescription source = org.mockito.Mockito.mock(IndexDescription.class);
		when(source.getBenefactors()).thenReturn(Arrays.asList(
				new BenefactorDescription("ROW_BENEFACTOR_A0", org.sagebionetworks.repo.model.ObjectType.ENTITY),
				new BenefactorDescription("ROW_BENEFACTOR_A1", org.sagebionetworks.repo.model.ObjectType.ENTITY)));
		when(source.getIdAndVersion()).thenReturn(SOURCE_ID);

		TableIndexDAO indexDao = org.mockito.Mockito.mock(TableIndexDAO.class);
		when(connectionFactory.getConnection(SOURCE_ID)).thenReturn(indexDao);
		// User can read benefactor 10 (dep 0) and 20 (dep 1) but not 11.
		// computeAccessibleBenefactors already bakes in the -1 sentinel.
		when(tableQueryManager.computeAccessibleBenefactors(
				eq(user), eq(source), eq(indexDao), eq(ACCESS_TYPE.READ)))
				.thenReturn(Arrays.asList(
						new BenefactorAccessFilter("ROW_BENEFACTOR_A0",
								new java.util.HashSet<>(Arrays.asList(10L, -1L))),
						new BenefactorAccessFilter("ROW_BENEFACTOR_A1",
								new java.util.HashSet<>(Arrays.asList(20L, -1L)))));

		// call under test
		List<org.opensearch.client.opensearch._types.query_dsl.Query> filters =
				manager.buildBenefactorAccessFilters(user, source);

		assertEquals(2, filters.size());
		// Dependency 0 → _benefactor_0 terms {10, -1}
		assertEquals("_benefactor_0", filters.get(0).terms().field());
		assertEquals(new java.util.HashSet<>(Arrays.asList(10L, -1L)),
				filters.get(0).terms().terms().value().stream()
						.map(v -> v.longValue()).collect(Collectors.toSet()));
		// Dependency 1 → _benefactor_1 terms {20, -1}
		assertEquals("_benefactor_1", filters.get(1).terms().field());
		assertEquals(new java.util.HashSet<>(Arrays.asList(20L, -1L)),
				filters.get(1).terms().terms().value().stream()
						.map(v -> v.longValue()).collect(Collectors.toSet()));
	}

	@Test
	public void testBuildBenefactorAccessFiltersWhenSourceTableNotBuilt() {
		// When the source index table does not exist yet, computeAccessibleBenefactors handles
		// the BadSqlGrammarException internally and returns fail-closed results: only the -1
		// sentinel is accessible. The filter matches ONLY _benefactor_0 == -1, so
		// benefactor-protected data is never exposed while the table is still building.
		IndexDescription source = org.mockito.Mockito.mock(IndexDescription.class);
		when(source.getBenefactors()).thenReturn(Arrays.asList(
				new BenefactorDescription("ROW_BENEFACTOR_A0", org.sagebionetworks.repo.model.ObjectType.ENTITY)));
		when(source.getIdAndVersion()).thenReturn(SOURCE_ID);

		TableIndexDAO indexDao = org.mockito.Mockito.mock(TableIndexDAO.class);
		when(connectionFactory.getConnection(SOURCE_ID)).thenReturn(indexDao);
		// computeAccessibleBenefactors already bakes in the -1 sentinel and handles the
		// table-not-yet-built case internally by falling back to {-1L}.
		when(tableQueryManager.computeAccessibleBenefactors(
				eq(user), eq(source), eq(indexDao), eq(ACCESS_TYPE.READ)))
				.thenReturn(Collections.singletonList(
						new BenefactorAccessFilter("ROW_BENEFACTOR_A0",
								new java.util.HashSet<>(Arrays.asList(-1L)))));

		// call under test
		List<org.opensearch.client.opensearch._types.query_dsl.Query> filters =
				manager.buildBenefactorAccessFilters(user, source);

		assertEquals(1, filters.size());
		// Fail-closed: the only term is the -1 sentinel, never an empty/match-all filter.
		assertEquals("_benefactor_0", filters.get(0).terms().field());
		assertEquals(new java.util.HashSet<>(Arrays.asList(-1L)),
				filters.get(0).terms().terms().value().stream()
						.map(v -> v.longValue()).collect(Collectors.toSet()));
	}
}
