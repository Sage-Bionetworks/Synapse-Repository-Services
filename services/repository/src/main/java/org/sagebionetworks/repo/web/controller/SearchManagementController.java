package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.model.oauth.OAuthScope.modify;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.view;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.asynch.AsyncJobId;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.search.SearchQueryResults;
import org.sagebionetworks.repo.model.search.table.BindSearchConfigToEntityRequest;
import org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverride;
import org.sagebionetworks.repo.model.search.table.ListColumnAnalyzerOverridesRequest;
import org.sagebionetworks.repo.model.search.table.ListColumnAnalyzerOverridesResponse;
import org.sagebionetworks.repo.model.search.table.ListSearchConfigurationsRequest;
import org.sagebionetworks.repo.model.search.table.ListSearchConfigurationsResponse;
import org.sagebionetworks.repo.model.search.table.ListSynonymSetsRequest;
import org.sagebionetworks.repo.model.search.table.ListSynonymSetsResponse;
import org.sagebionetworks.repo.model.search.table.ListTextAnalyzersRequest;
import org.sagebionetworks.repo.model.search.table.ListTextAnalyzersResponse;
import org.sagebionetworks.repo.model.search.table.SearchAutocompleteRequest;
import org.sagebionetworks.repo.model.search.table.SearchConfigBinding;
import org.sagebionetworks.repo.model.search.table.SearchConfiguration;
import org.sagebionetworks.repo.model.search.table.SearchIndexQuery;
import org.sagebionetworks.repo.model.search.table.SynonymSet;
import org.sagebionetworks.repo.model.search.table.TextAnalyzer;
import org.sagebionetworks.repo.service.search.ColumnAnalyzerOverrideService;
import org.sagebionetworks.repo.service.search.SearchConfigurationService;
import org.sagebionetworks.repo.service.search.SearchIndexQueryService;
import org.sagebionetworks.repo.service.search.SynonymSetService;
import org.sagebionetworks.repo.service.search.TextAnalyzerService;
import org.sagebionetworks.repo.web.RequiredScope;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * <p>
 * Services for managing search configuration objects. These reusable building blocks
 * define how a <a href="${org.sagebionetworks.repo.model.search.table.SearchIndex}">SearchIndex</a>
 * analyzes, tokenizes, and matches text. They map directly to
 * <a href="https://docs.opensearch.org/latest/analyzers/custom-analyzer/">OpenSearch custom analyzer</a>
 * concepts and are assembled into a
 * <a href="${org.sagebionetworks.repo.model.search.table.SearchConfiguration}">SearchConfiguration</a>
 * that can be attached to a project.
 * </p>
 *
 * <h6>Text Analyzers</h6>
 * <p>
 * A <a href="${org.sagebionetworks.repo.model.search.table.TextAnalyzer}">TextAnalyzer</a> defines
 * a text analysis pipeline as the OpenSearch <code>settings.analysis</code> block: any combination
 * of <a href="https://docs.opensearch.org/latest/analyzers/character-filters/index/">char filters</a>,
 * <a href="https://docs.opensearch.org/latest/analyzers/tokenizers/index/">tokenizers</a>, and
 * <a href="https://docs.opensearch.org/latest/analyzers/token-filters/index/">token filters</a>,
 * plus one or more named custom analyzers that compose them. The whole <code>settings</code> field
 * is an opaque JSON-object string &mdash; you can paste a snippet from the OpenSearch docs straight
 * in. Synapse only checks that the JSON parses and that any <code>$ref</code> entries resolve;
 * AOSS validates the analyzer shape itself at index-build time.
 * </p>
 * <ul>
 * <li><a href="${POST.search.text.analyzer}">POST /search/text/analyzer</a></li>
 * <li><a href="${GET.search.text.analyzer.id}">GET /search/text/analyzer/{id}</a></li>
 * <li><a href="${PUT.search.text.analyzer.id}">PUT /search/text/analyzer/{id}</a></li>
 * <li><a href="${POST.search.text.analyzer.list}">POST /search/text/analyzer/list</a></li>
 * </ul>
 *
 * <h6>Column Analyzer Overrides</h6>
 * <p>
 * A <a href="${org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverride}">ColumnAnalyzerOverride</a>
 * assigns specific <a href="${org.sagebionetworks.repo.model.search.table.TextAnalyzer}">TextAnalyzers</a>
 * to individual columns, overriding the SearchConfiguration's default analyzer. Each override entry
 * specifies an index analyzer (used when building the index) and a search analyzer (used at query time).
 * This corresponds to the OpenSearch per-field
 * <a href="https://docs.opensearch.org/latest/analyzers/index-analyzers/">analyzer</a> +
 * <a href="https://docs.opensearch.org/latest/analyzers/search-analyzers/">search_analyzer</a>
 * mapping.
 * </p>
 * <ul>
 * <li><a href="${POST.search.column.analyzer.override}">POST /search/column/analyzer/override</a></li>
 * <li><a href="${GET.search.column.analyzer.override.columnAnalyzerOverrideId}">GET /search/column/analyzer/override/{columnAnalyzerOverrideId}</a></li>
 * <li><a href="${PUT.search.column.analyzer.override.columnAnalyzerOverrideId}">PUT /search/column/analyzer/override/{columnAnalyzerOverrideId}</a></li>
 * <li><a href="${POST.search.column.analyzer.override.list}">POST /search/column/analyzer/override/list</a></li>
 * </ul>
 *
 * <h6>Synonym Sets</h6>
 * <p>
 * A <a href="${org.sagebionetworks.repo.model.search.table.SynonymSet}">SynonymSet</a> is a shareable
 * OpenSearch <a href="https://docs.opensearch.org/latest/analyzers/token-filters/synonym-graph/">synonym_graph</a>
 * (or legacy <a href="https://docs.opensearch.org/latest/analyzers/token-filters/synonym/">synonym</a>)
 * token filter. Its <code>definition</code> is the full filter JSON, exactly as documented by
 * OpenSearch &mdash; supply OpenSearch's native synonym syntax (<code>a, b, c</code> for equivalent
 * (bidirectional) and <code>a, b =&gt; c, d</code> for explicit (directional) expansion).
 * A TextAnalyzer brings a SynonymSet into its filter registry by writing
 * <code>{"$ref": "{organizationName}-{name}"}</code> in place of an inline filter definition;
 * Synapse resolves the reference at index-build time before sending the analyzer to AOSS.
 * </p>
 * <ul>
 * <li><a href="${POST.search.synonym.set}">POST /search/synonym/set</a></li>
 * <li><a href="${GET.search.synonym.set.synonymSetId}">GET /search/synonym/set/{synonymSetId}</a></li>
 * <li><a href="${PUT.search.synonym.set.synonymSetId}">PUT /search/synonym/set/{synonymSetId}</a></li>
 * <li><a href="${POST.search.synonym.set.list}">POST /search/synonym/set/list</a></li>
 * </ul>
 *
 * <h6>Search Configurations</h6>
 * <p>
 * A <a href="${org.sagebionetworks.repo.model.search.table.SearchConfiguration}">SearchConfiguration</a>
 * bundles a default index-time and search-time
 * <a href="${org.sagebionetworks.repo.model.search.table.TextAnalyzer}">TextAnalyzer</a> together with
 * zero or more <a href="${org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverride}">ColumnAnalyzerOverrides</a>
 * into a reusable configuration. These settings populate the <code>analysis</code> section of the
 * OpenSearch index definition when a search index is created. Synonyms are wired in at the
 * TextAnalyzer level via <code>$ref</code>, not on the SearchConfiguration.
 * </p>
 * <p>
 * Attach a SearchConfiguration to an entity (SearchIndex, Folder, or Project) by creating a binding.
 * The effective configuration for any entity is resolved by walking up the entity hierarchy.
 * </p>
 * <ul>
 * <li><a href="${POST.search.configuration}">POST /search/configuration</a></li>
 * <li><a href="${GET.search.configuration.searchConfigurationId}">GET /search/configuration/{searchConfigurationId}</a></li>
 * <li><a href="${PUT.search.configuration.searchConfigurationId}">PUT /search/configuration/{searchConfigurationId}</a></li>
 * <li><a href="${POST.search.configuration.list}">POST /search/configuration/list</a></li>
 * </ul>
 *
 * <h6>Search Configuration Bindings</h6>
 * <p>
 * Bind a <a href="${org.sagebionetworks.repo.model.search.table.SearchConfiguration}">SearchConfiguration</a>
 * to an entity. The effective configuration for any entity is resolved by walking up the hierarchy
 * (entity &rarr; folder &rarr; project). Requires EDIT permission on the entity.
 * </p>
 * <ul>
 * <li><a href="${PUT.entity.entityId.searchconfig.binding}">PUT /entity/{entityId}/searchconfig/binding</a></li>
 * <li><a href="${GET.entity.entityId.searchconfig.binding}">GET /entity/{entityId}/searchconfig/binding</a></li>
 * <li><a href="${DELETE.entity.entityId.searchconfig.binding}">DELETE /entity/{entityId}/searchconfig/binding</a></li>
 * </ul>
 *
 * <h6>Public Resources and Cross-Organization Referencing</h6>
 * <p>
 * All search management resources (TextAnalyzers, SynonymSets, ColumnAnalyzerOverrides,
 * SearchConfigurations) are <b>publicly readable</b>. Any authenticated user can list and retrieve
 * resources from any Organization. This enables cross-organization reuse: a
 * <a href="${org.sagebionetworks.repo.model.search.table.SearchConfiguration}">SearchConfiguration</a>
 * can reference resources from any Organization using <b>qualified names</b> in the format
 * <code>{organizationName}-{resourceName}</code> (e.g., <code>org.sagebionetworks-SCIENTIFIC</code>).
 * For example, any user can build a SearchConfiguration that uses the platform-provided
 * <code>org.sagebionetworks</code> analyzers alongside custom resources from their own Organization.
 * </p>
 *
 * <h6>Name Immutability</h6>
 * <p>
 * Because resources are referenced by qualified name, the <code>name</code> and
 * <code>organizationName</code> fields are <b>immutable after creation</b>. Attempting to change
 * either on update will return a 400 error. To use a different name, create a new resource and
 * update any SearchConfigurations that reference the old one.
 * </p>
 *
 * <h6>Authorization</h6>
 * <p>
 * All management operations (create, update, delete) are restricted to <b>Sage Bionetworks employees</b>
 * who have the appropriate
 * <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}">ACCESS_TYPE</a> permission on the
 * <a href="${org.sagebionetworks.repo.model.schema.Organization}">Organization</a> that owns the resource.
 * Read and list operations are publicly accessible.
 * </p>
 *
 * <h6>Organization Scoping</h6>
 * <p>
 * All search management objects belong to an
 * <a href="${org.sagebionetworks.repo.model.schema.Organization}">Organization</a> identified by
 * <code>organizationName</code>. The organization and name cannot be changed after creation.
 * Resource names must start with a letter and contain only letters, digits, and underscores.
 * </p>
 *
 * <h6>Built-in OpenSearch Names Pass Through</h6>
 * <p>
 * Filter and tokenizer names inside a TextAnalyzer's chain arrays pass through to OpenSearch
 * verbatim when they don't match a name registered in the same TextAnalyzer's
 * <code>filter</code> / <code>tokenizer</code> / <code>char_filter</code> registry maps.
 * <code>lowercase</code>, <code>standard</code>, <code>english_stemmer</code>,
 * <code>icu_tokenizer</code>, <code>phonetic</code>, etc. all work without any Synapse-side
 * registration &mdash; the
 * <a href="https://docs.aws.amazon.com/opensearch-service/latest/developerguide/serverless-genref.html">analysis-icu, analysis-phonetic, analysis-kuromoji, analysis-nori, analysis-smartcn</a>
 * plugins ship with AOSS. File-based parameters (<code>stopwords_path</code>,
 * <code>synonyms_path</code>, <code>mappings_path</code>, <code>protected_words_path</code>,
 * and any other <code>*_path</code> key) are <b>not supported in AOSS Serverless</b> and will be
 * rejected by AOSS at index-build time &mdash; use the inline equivalents
 * (<code>stopwords</code>, <code>synonyms</code>, <code>mappings</code>,
 * <code>protected_words</code>).
 * </p>
 *
 * <h6>Search Queries</h6>
 * <p>
 * Query a <a href="${org.sagebionetworks.repo.model.search.table.SearchIndex}">SearchIndex</a>
 * using the async job pattern. Submit a
 * <a href="${org.sagebionetworks.repo.model.search.table.SearchIndexQuery}">SearchIndexQuery</a>
 * wrapping a pass-through <a href="https://docs.opensearch.org/latest/query-dsl/">OpenSearch query DSL</a>
 * body to start a job, then poll for
 * <a href="${org.sagebionetworks.repo.model.search.SearchQueryResults}">SearchQueryResults</a>.
 * A synchronous <a href="https://docs.opensearch.org/latest/search-plugins/searching-data/autocomplete/">autocomplete</a>
 * endpoint is also available for typeahead patterns.
 * </p>
 * <ul>
 * <li><a href="${POST.search.query.async.start}">POST /search/query/async/start</a> &mdash; Start async query</li>
 * <li><a href="${GET.search.query.async.get.asyncToken}">GET /search/query/async/get/{asyncToken}</a> &mdash; Poll for results</li>
 * <li><a href="${POST.search.autocomplete}">POST /search/autocomplete</a> &mdash; Synchronous autocomplete (max 8 results)</li>
 * </ul>
 *
 * <h6>Search Index Field Limits</h6>
 * <p>
 * When a search index is built, each column from the defining SQL query is mapped to an
 * OpenSearch field type. Text and keyword fields have an <code>ignore_above</code> limit on
 * their keyword sub-field &mdash; values longer than this limit are <b>not indexed</b> for
 * exact-match or sorting, but remain stored in the source document. Numeric, boolean, and
 * JSON fields have no such limit.
 * </p>
 * <b>Search Index Field Mapping and Limits</b>
 * <table border="1">
 * <tr>
 * <th>Synapse Column Type</th>
 * <th>OpenSearch Field Type</th>
 * <th>Keyword ignore_above</th>
 * <th>Default Analyzer</th>
 * </tr>
 * <tr>
 * <td>STRING, STRING_LIST</td>
 * <td>text + keyword sub-field</td>
 * <td>1,000 characters</td>
 * <td>SCIENTIFIC</td>
 * </tr>
 * <tr>
 * <td>MEDIUMTEXT</td>
 * <td>text + keyword sub-field</td>
 * <td>100,000 characters</td>
 * <td>SCIENTIFIC</td>
 * </tr>
 * <tr>
 * <td>LARGETEXT</td>
 * <td>text + keyword sub-field</td>
 * <td>8,192 characters</td>
 * <td>SCIENTIFIC</td>
 * </tr>
 * <tr>
 * <td>LINK</td>
 * <td>text + keyword sub-field</td>
 * <td>1,000 characters</td>
 * <td>KEYWORD</td>
 * </tr>
 * <tr>
 * <td>ENTITYID, USERID, ENTITYID_LIST, USERID_LIST</td>
 * <td>keyword</td>
 * <td>256 characters</td>
 * <td>KEYWORD</td>
 * </tr>
 * <tr>
 * <td>INTEGER, DATE, INTEGER_LIST, DATE_LIST, FILEHANDLEID, SUBMISSIONID, EVALUATIONID</td>
 * <td>long</td>
 * <td>N/A</td>
 * <td>KEYWORD</td>
 * </tr>
 * <tr>
 * <td>DOUBLE</td>
 * <td>double</td>
 * <td>N/A</td>
 * <td>KEYWORD</td>
 * </tr>
 * <tr>
 * <td>BOOLEAN, BOOLEAN_LIST</td>
 * <td>boolean</td>
 * <td>N/A</td>
 * <td>KEYWORD</td>
 * </tr>
 * <tr>
 * <td>JSON</td>
 * <td>object (dynamic mapping)</td>
 * <td>N/A</td>
 * <td>STANDARD</td>
 * </tr>
 * </table>
 * <p>
 * <b>Query Limits:</b> Results per page default to 25 with a maximum of 100.
 * Autocomplete results are capped at 8. The maximum number of rows that can be indexed
 * in a single search index is 500,000.
 * </p>
 */
@ControllerInfo(displayName = "Search Management Services", path = "repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class SearchManagementController {

	@Autowired
	private TextAnalyzerService textAnalyzerService;

	@Autowired
	private ColumnAnalyzerOverrideService columnAnalyzerOverrideService;

	@Autowired
	private SynonymSetService synonymSetService;

	@Autowired
	private SearchConfigurationService searchConfigurationService;

	@Autowired
	private SearchIndexQueryService searchIndexQueryService;

	// ==================== Text Analyzers ====================

	/**
	 * Create a new <a href="${org.sagebionetworks.repo.model.search.table.TextAnalyzer}">TextAnalyzer</a>
	 * within the specified
	 * <a href="${org.sagebionetworks.repo.model.schema.Organization}">Organization</a>.
	 * <p>
	 * The caller must be a Sage Bionetworks employee with
	 * <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}">ACCESS_TYPE.CREATE</a>
	 * permission on the Organization.
	 * </p>
	 * <p>
	 * The analyzer's <code>settings</code> JSON is parsed and any <code>$ref</code> entries
	 * inside its <code>filter</code> registry are checked for qualified-name format and
	 * existence. Component shape and parameter validation (tokenizer types, filter parameters,
	 * file-based <code>*_path</code> keys, etc.) is deferred to AOSS at index-build time.
	 * </p>
	 *
	 * @param userId The ID of the authenticated user.
	 * @param request The text analyzer to create. Must include organizationName, name, and settings.
	 * @return The created text analyzer with a generated ID, etag, and audit timestamps.
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.SEARCH_TEXT_ANALYZER, method = RequestMethod.POST)
	public @ResponseBody TextAnalyzer createTextAnalyzer(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody TextAnalyzer request) {
		return textAnalyzerService.create(userId, request);
	}

	/**
	 * Get a <a href="${org.sagebionetworks.repo.model.search.table.TextAnalyzer}">TextAnalyzer</a>
	 * by its ID.
	 * <p>
	 * This is a public read operation &mdash; no special authorization is required.
	 * </p>
	 *
	 * @param userId The ID of the authenticated user.
	 * @param id The numeric ID of the text analyzer to retrieve.
	 * @return The requested text analyzer.
	 * @throws NotFoundException If no text analyzer exists with the given ID.
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SEARCH_TEXT_ANALYZER_ID, method = RequestMethod.GET)
	public @ResponseBody TextAnalyzer getTextAnalyzer(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable Long id) {
		return textAnalyzerService.get(userId, id);
	}

	/**
	 * Update a <a href="${org.sagebionetworks.repo.model.search.table.TextAnalyzer}">TextAnalyzer</a>.
	 * <p>
	 * The caller must be a Sage Bionetworks employee with
	 * <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}">ACCESS_TYPE.UPDATE</a>
	 * permission on the Organization. The <code>organizationName</code> and <code>name</code>
	 * are immutable and cannot be changed after creation.
	 * </p>
	 * <p>
	 * The new <code>settings</code> JSON is re-parsed and any <code>$ref</code> entries are
	 * re-checked for qualified-name format and existence. Concurrency is managed via the etag
	 * field; an etag mismatch returns 409 Conflict.
	 * </p>
	 *
	 * @param userId The ID of the authenticated user.
	 * @param id The path ID of the text analyzer (must match the request body's ID).
	 * @param request The updated text analyzer.
	 * @return The updated text analyzer with a new etag.
	 * @throws NotFoundException If no text analyzer exists with the given ID.
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SEARCH_TEXT_ANALYZER_ID, method = RequestMethod.PUT)
	public @ResponseBody TextAnalyzer updateTextAnalyzer(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable Long id,
			@RequestBody TextAnalyzer request) {
		String idString = String.valueOf(id);
		if (!idString.equals(request.getId())) {
			throw new IllegalArgumentException(
				"The path ID: " + idString + " does not match the request body's ID: " + request.getId());
		}
		return textAnalyzerService.update(userId, request);
	}

	/**
	 * List <a href="${org.sagebionetworks.repo.model.search.table.TextAnalyzer}">TextAnalyzer</a>
	 * objects, optionally filtered by Organization.
	 * <p>
	 * This is a public read operation. Results are paginated using a next page token.
	 * If <code>organizationName</code> is null, all text analyzers across all Organizations are returned.
	 * </p>
	 *
	 * @param userId The ID of the authenticated user.
	 * @param request The list request. Set organizationName to filter by Organization,
	 *        or leave null to list all. Use nextPageToken for pagination.
	 * @return A paginated list of text analyzers.
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SEARCH_TEXT_ANALYZER_LIST, method = RequestMethod.POST)
	public @ResponseBody ListTextAnalyzersResponse listTextAnalyzers(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody ListTextAnalyzersRequest request) {
		return textAnalyzerService.list(userId, request);
	}

	// ==================== Column Analyzer Overrides ====================

	/**
	 * Create a new <a href="${org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverride}">ColumnAnalyzerOverride</a>
	 * within the specified
	 * <a href="${org.sagebionetworks.repo.model.schema.Organization}">Organization</a>.
	 * <p>
	 * The caller must be a Sage Bionetworks employee with
	 * <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}">ACCESS_TYPE.CREATE</a>
	 * permission on the Organization.
	 * </p>
	 *
	 * @param userId The ID of the authenticated user.
	 * @param request The column analyzer override to create. Must include organizationName, name,
	 *        and at least one override entry mapping a column to an analyzer pair.
	 * @return The created column analyzer override with a generated ID, etag, and audit timestamps.
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.SEARCH_COLUMN_ANALYZER_OVERRIDE, method = RequestMethod.POST)
	public @ResponseBody ColumnAnalyzerOverride createColumnAnalyzerOverride(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody ColumnAnalyzerOverride request) {
		return columnAnalyzerOverrideService.create(userId, request);
	}

	/**
	 * Get a <a href="${org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverride}">ColumnAnalyzerOverride</a>
	 * by its ID.
	 * <p>
	 * This is a public read operation &mdash; no special authorization is required.
	 * </p>
	 *
	 * @param userId The ID of the authenticated user.
	 * @param columnAnalyzerOverrideId The ID of the column analyzer override to retrieve.
	 * @return The requested column analyzer override.
	 * @throws NotFoundException If no column analyzer override exists with the given ID.
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SEARCH_COLUMN_ANALYZER_OVERRIDE_ID, method = RequestMethod.GET)
	public @ResponseBody ColumnAnalyzerOverride getColumnAnalyzerOverride(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String columnAnalyzerOverrideId) {
		return columnAnalyzerOverrideService.get(userId, columnAnalyzerOverrideId);
	}

	/**
	 * Update a <a href="${org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverride}">ColumnAnalyzerOverride</a>.
	 * <p>
	 * The caller must be a Sage Bionetworks employee with
	 * <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}">ACCESS_TYPE.UPDATE</a>
	 * permission on the Organization. The <code>organizationName</code> and <code>name</code>
	 * are immutable and cannot be changed after creation.
	 * </p>
	 * <p>
	 * Concurrency is managed via the etag field. If the etag in the request does not match
	 * the current etag, a 409 Conflict is returned.
	 * </p>
	 *
	 * @param userId The ID of the authenticated user.
	 * @param columnAnalyzerOverrideId The path ID (must match the request body's ID).
	 * @param request The updated column analyzer override.
	 * @return The updated column analyzer override with a new etag.
	 * @throws NotFoundException If no column analyzer override exists with the given ID.
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SEARCH_COLUMN_ANALYZER_OVERRIDE_ID, method = RequestMethod.PUT)
	public @ResponseBody ColumnAnalyzerOverride updateColumnAnalyzerOverride(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String columnAnalyzerOverrideId,
			@RequestBody ColumnAnalyzerOverride request) {
		if (!columnAnalyzerOverrideId.equals(request.getId())) {
			throw new IllegalArgumentException(
				"The path ID: " + columnAnalyzerOverrideId + " does not match the request body's ID: " + request.getId());
		}
		return columnAnalyzerOverrideService.update(userId, request);
	}

	/**
	 * List <a href="${org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverride}">ColumnAnalyzerOverride</a>
	 * objects, optionally filtered by Organization.
	 * <p>
	 * This is a public read operation. Results are paginated using a next page token.
	 * If <code>organizationName</code> is null, all column analyzer overrides across all Organizations are returned.
	 * </p>
	 *
	 * @param userId The ID of the authenticated user.
	 * @param request The list request. Set organizationName to filter by Organization,
	 *        or leave null to list all. Use nextPageToken for pagination.
	 * @return A paginated list of column analyzer overrides.
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SEARCH_COLUMN_ANALYZER_OVERRIDE_LIST, method = RequestMethod.POST)
	public @ResponseBody ListColumnAnalyzerOverridesResponse listColumnAnalyzerOverrides(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody ListColumnAnalyzerOverridesRequest request) {
		return columnAnalyzerOverrideService.list(userId, request);
	}

	// ==================== Synonym Sets ====================

	/**
	 * Create a new <a href="${org.sagebionetworks.repo.model.search.table.SynonymSet}">SynonymSet</a>
	 * within the specified
	 * <a href="${org.sagebionetworks.repo.model.schema.Organization}">Organization</a>.
	 * <p>
	 * The caller must be a Sage Bionetworks employee with
	 * <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}">ACCESS_TYPE.CREATE</a>
	 * permission on the Organization.
	 * </p>
	 * <p>
	 * The supplied <code>definition</code> is parsed to confirm it is valid JSON and otherwise
	 * passed through to AOSS as-is. AOSS Serverless rejects file-based parameters
	 * (<code>synonyms_path</code> etc.) at index-build time — supply the synonym list inline
	 * via the <code>synonyms</code> array; see
	 * <a href="${org.sagebionetworks.repo.model.search.table.SynonymSet}">SynonymSet</a>.
	 * </p>
	 *
	 * @param userId The ID of the authenticated user.
	 * @param request The synonym set to create. Must include organizationName, name, and definition.
	 * @return The created synonym set with a generated ID, etag, and audit timestamps.
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.SEARCH_SYNONYM_SET, method = RequestMethod.POST)
	public @ResponseBody SynonymSet createSynonymSet(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody SynonymSet request) {
		return synonymSetService.create(userId, request);
	}

	/**
	 * Get a <a href="${org.sagebionetworks.repo.model.search.table.SynonymSet}">SynonymSet</a>
	 * by its ID.
	 * <p>
	 * This is a public read operation &mdash; no special authorization is required.
	 * </p>
	 *
	 * @param userId The ID of the authenticated user.
	 * @param synonymSetId The ID of the synonym set to retrieve.
	 * @return The requested synonym set.
	 * @throws NotFoundException If no synonym set exists with the given ID.
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SEARCH_SYNONYM_SET_ID, method = RequestMethod.GET)
	public @ResponseBody SynonymSet getSynonymSet(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String synonymSetId) {
		return synonymSetService.get(userId, synonymSetId);
	}

	/**
	 * Update a <a href="${org.sagebionetworks.repo.model.search.table.SynonymSet}">SynonymSet</a>.
	 * <p>
	 * The caller must be a Sage Bionetworks employee with
	 * <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}">ACCESS_TYPE.UPDATE</a>
	 * permission on the Organization. The <code>organizationName</code> and <code>name</code>
	 * are immutable and cannot be changed after creation.
	 * </p>
	 * <p>
	 * The new <code>definition</code> is re-parsed to confirm it is valid JSON and otherwise
	 * passed through to AOSS as-is. Concurrency is managed via the etag field; an etag
	 * mismatch returns 409 Conflict.
	 * </p>
	 *
	 * @param userId The ID of the authenticated user.
	 * @param synonymSetId The path ID (must match the request body's ID).
	 * @param request The updated synonym set.
	 * @return The updated synonym set with a new etag.
	 * @throws NotFoundException If no synonym set exists with the given ID.
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SEARCH_SYNONYM_SET_ID, method = RequestMethod.PUT)
	public @ResponseBody SynonymSet updateSynonymSet(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String synonymSetId,
			@RequestBody SynonymSet request) {
		if (!synonymSetId.equals(request.getId())) {
			throw new IllegalArgumentException(
				"The path ID: " + synonymSetId + " does not match the request body's ID: " + request.getId());
		}
		return synonymSetService.update(userId, request);
	}

	/**
	 * List <a href="${org.sagebionetworks.repo.model.search.table.SynonymSet}">SynonymSet</a>
	 * objects, optionally filtered by Organization.
	 * <p>
	 * This is a public read operation. Results are paginated using a next page token.
	 * If <code>organizationName</code> is null, all synonym sets across all Organizations are returned.
	 * </p>
	 *
	 * @param userId The ID of the authenticated user.
	 * @param request The list request. Set organizationName to filter by Organization,
	 *        or leave null to list all. Use nextPageToken for pagination.
	 * @return A paginated list of synonym sets.
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SEARCH_SYNONYM_SET_LIST, method = RequestMethod.POST)
	public @ResponseBody ListSynonymSetsResponse listSynonymSets(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody ListSynonymSetsRequest request) {
		return synonymSetService.list(userId, request);
	}

	// ==================== Search Configurations ====================

	/**
	 * Create a new <a href="${org.sagebionetworks.repo.model.search.table.SearchConfiguration}">SearchConfiguration</a>
	 * within the specified
	 * <a href="${org.sagebionetworks.repo.model.schema.Organization}">Organization</a>.
	 * <p>
	 * The caller must be a Sage Bionetworks employee with
	 * <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}">ACCESS_TYPE.CREATE</a>
	 * permission on the Organization.
	 * </p>
	 *
	 * @param userId The ID of the authenticated user.
	 * @param request The search configuration to create. Must include organizationName and name.
	 *        Optionally include defaultAnalyzer and columnAnalyzerOverrides.
	 * @return The created search configuration with a generated ID, etag, and audit timestamps.
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.SEARCH_CONFIGURATION, method = RequestMethod.POST)
	public @ResponseBody SearchConfiguration createSearchConfiguration(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody SearchConfiguration request) {
		return searchConfigurationService.create(userId, request);
	}

	/**
	 * Get a <a href="${org.sagebionetworks.repo.model.search.table.SearchConfiguration}">SearchConfiguration</a>
	 * by its ID.
	 * <p>
	 * This is a public read operation &mdash; no special authorization is required.
	 * </p>
	 *
	 * @param userId The ID of the authenticated user.
	 * @param searchConfigurationId The ID of the search configuration to retrieve.
	 * @return The requested search configuration.
	 * @throws NotFoundException If no search configuration exists with the given ID.
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SEARCH_CONFIGURATION_ID, method = RequestMethod.GET)
	public @ResponseBody SearchConfiguration getSearchConfiguration(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String searchConfigurationId) {
		return searchConfigurationService.get(userId, searchConfigurationId);
	}

	/**
	 * Update a <a href="${org.sagebionetworks.repo.model.search.table.SearchConfiguration}">SearchConfiguration</a>.
	 * <p>
	 * The caller must be a Sage Bionetworks employee with
	 * <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}">ACCESS_TYPE.UPDATE</a>
	 * permission on the Organization. The <code>organizationName</code> and <code>name</code>
	 * are immutable and cannot be changed after creation.
	 * </p>
	 * <p>
	 * Concurrency is managed via the etag field. If the etag in the request does not match
	 * the current etag, a 409 Conflict is returned.
	 * </p>
	 *
	 * @param userId The ID of the authenticated user.
	 * @param searchConfigurationId The path ID (must match the request body's ID).
	 * @param request The updated search configuration.
	 * @return The updated search configuration with a new etag.
	 * @throws NotFoundException If no search configuration exists with the given ID.
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SEARCH_CONFIGURATION_ID, method = RequestMethod.PUT)
	public @ResponseBody SearchConfiguration updateSearchConfiguration(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String searchConfigurationId,
			@RequestBody SearchConfiguration request) {
		if (!searchConfigurationId.equals(request.getId())) {
			throw new IllegalArgumentException(
				"The path ID: " + searchConfigurationId + " does not match the request body's ID: " + request.getId());
		}
		return searchConfigurationService.update(userId, request);
	}

	/**
	 * List <a href="${org.sagebionetworks.repo.model.search.table.SearchConfiguration}">SearchConfiguration</a>
	 * objects, optionally filtered by Organization.
	 * <p>
	 * This is a public read operation. Results are paginated using a next page token.
	 * If <code>organizationName</code> is null, all search configurations across all Organizations are returned.
	 * </p>
	 *
	 * @param userId The ID of the authenticated user.
	 * @param request The list request. Set organizationName to filter by Organization,
	 *        or leave null to list all. Use nextPageToken for pagination.
	 * @return A paginated list of search configurations.
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SEARCH_CONFIGURATION_LIST, method = RequestMethod.POST)
	public @ResponseBody ListSearchConfigurationsResponse listSearchConfigurations(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody ListSearchConfigurationsRequest request) {
		return searchConfigurationService.list(userId, request);
	}

	// ==================== Search Configuration Bindings ====================

	/**
	 * Bind a <a href="${org.sagebionetworks.repo.model.search.table.SearchConfiguration}">SearchConfiguration</a>
	 * to an entity (typically a project). The caller must have EDIT permission on the entity.
	 * Replaces any existing binding on that entity. Descendant SearchIndex entities inherit the
	 * configuration unless they set their own <code>searchConfigurationId</code>; the effective
	 * configuration for any entity is resolved by walking up the hierarchy
	 * (entity → folder → project).
	 *
	 * @param userId The ID of the authenticated user.
	 * @param entityId The ID of the entity to bind to.
	 * @param request The bind request containing the searchConfigurationId.
	 * @return The created (or replaced) binding.
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_SEARCH_CONFIG_BINDING, method = RequestMethod.PUT)
	public @ResponseBody SearchConfigBinding bindSearchConfigToEntity(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String entityId,
			@RequestBody BindSearchConfigToEntityRequest request) {
		request.setEntityId(entityId);
		return searchConfigurationService.bindSearchConfigToEntity(userId, request);
	}

	/**
	 * Get the effective <a href="${org.sagebionetworks.repo.model.search.table.SearchConfigBinding}">SearchConfigBinding</a>
	 * for an entity. Walks up the entity hierarchy (entity → folder → project) and returns the
	 * first binding found on the entity or any ancestor.
	 * <p>
	 * This is a public read operation &mdash; no special authorization on the binding is required.
	 * </p>
	 *
	 * @param userId The ID of the authenticated user.
	 * @param entityId The ID of the entity to look up.
	 * @return The effective binding.
	 * @throws NotFoundException If no binding exists on the entity or any ancestor.
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_SEARCH_CONFIG_BINDING, method = RequestMethod.GET)
	public @ResponseBody SearchConfigBinding getSearchConfigBinding(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String entityId) {
		return searchConfigurationService.getSearchConfigBinding(userId, entityId);
	}

	/**
	 * Clear the <a href="${org.sagebionetworks.repo.model.search.table.SearchConfigBinding}">SearchConfigBinding</a>
	 * on a specific entity. Does not affect ancestor bindings — descendants that were inheriting
	 * via the cleared binding will fall back to the next binding up the hierarchy. The caller
	 * must have EDIT permission on the entity.
	 *
	 * @param userId The ID of the authenticated user.
	 * @param entityId The ID of the entity whose binding to clear.
	 * @throws NotFoundException If no binding exists directly on this entity.
	 */
	@RequiredScope({ view, modify })
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.ENTITY_SEARCH_CONFIG_BINDING, method = RequestMethod.DELETE)
	public void clearSearchConfigBinding(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String entityId) {
		searchConfigurationService.clearSearchConfigBinding(userId, entityId);
	}

	// ==================== Search Queries ====================

	/**
	 * Start an asynchronous search query job against a
	 * <a href="${org.sagebionetworks.repo.model.search.table.SearchIndex}">SearchIndex</a>.
	 * <p>
	 * The request wraps a
	 * <a href="${org.sagebionetworks.repo.model.search.SearchQuery}">SearchQuery</a> — the
	 * top-level OpenSearch <a href="https://docs.opensearch.org/latest/api-reference/search-apis/search/"><code>_search</code></a>
	 * body, allowlist-validated server-side and submitted to AOSS. Each slot's contents are
	 * pass-through <a href="https://docs.opensearch.org/latest/query-dsl/">OpenSearch DSL</a>.
	 * </p>
	 * <p>
	 * <b>Required:</b> <code>query</code> is required (use <code>{"match_all":{}}</code> to
	 * match all documents). <code>from</code> and <code>search_after</code> are mutually
	 * exclusive: when <code>search_after</code> is supplied the server pins <code>from=0</code>
	 * internally; supplying both <code>search_after</code> and <code>from &gt; 0</code> is
	 * rejected with HTTP 400.
	 * </p>
	 * <p>
	 * <b>Two pagination modes.</b> Use <code>from</code> + <code>size</code> for the simple
	 * "jump to page N" case. Use <code>search_after</code> for deep pagination past the
	 * OpenSearch <code>from</code> + <code>size</code> ~10,000-row ceiling: omit
	 * <code>search_after</code> on the first request and on every subsequent request pass
	 * back the previous response's <code>nextSearchAfter</code> unchanged. Cursors are
	 * stable as long as the underlying sort is unchanged. For shallow paging within the
	 * first ~10,000 rows, prefer <code>from</code> + <code>size</code>.
	 * </p>
	 *
	 * <h6>Field references and sub-field routing</h6>
	 * <p>
	 * Field references use column names everywhere (DSL clauses, aggregation
	 * <code>field</code>, <code>highlight.fields</code> keys,
	 * <code>sort</code>, <code>_source.includes</code> / <code>_source.excludes</code>). The
	 * server resolves names to internal column ids before sending to OpenSearch and rewrites
	 * them back to column names on response so callers see their original schema.
	 * </p>
	 * <p>
	 * <b>Pass the bare column name in every clause.</b> The server knows the index schema and
	 * routes text-typed columns (STRING, STRING_LIST, MEDIUMTEXT, LARGETEXT, LINK) through
	 * <code>{column}.keyword</code> automatically when the operation requires it:
	 * <code>term</code> / <code>terms</code> / <code>prefix</code> / <code>wildcard</code> /
	 * <code>fuzzy</code> / <code>range</code> / <code>match_phrase_prefix</code>, every
	 * aggregation kind, <code>sort</code>, and <code>collapse</code>. The relevance-scored
	 * match-family clauses (<code>match</code>, <code>multi_match</code>,
	 * <code>match_phrase</code>, <code>match_bool_prefix</code>,
	 * <code>simple_query_string</code>) use the analyzed text field directly. Numeric,
	 * boolean, keyword (ENTITYID / USERID), and date columns always use the bare name.
	 * </p>
	 * <p>
	 * Aggregation results come back with field references reported as the
	 * caller's bare column name — the server strips the <code>.keyword</code> suffix it
	 * auto-appended on the request side. Callers who prefer to be explicit may still supply
	 * <code>{columnName}.keyword</code> on a reference; the server preserves the suffix
	 * verbatim. The <code>{field}^{boost}</code> form on <code>multi_match.fields</code> is
	 * also preserved.
	 * </p>
	 *
	 * <h6>Allowlisted top-level keys</h6>
	 * <p>
	 * <b><code>query</code></b> — required. See the
	 * <a href="https://docs.opensearch.org/latest/query-dsl/">OpenSearch query DSL</a>.
	 * <a href="https://docs.opensearch.org/latest/query-dsl/compound/index/">Compound</a>
	 * (<a href="https://docs.opensearch.org/latest/query-dsl/compound/bool/"><code>bool</code></a>
	 * / <a href="https://docs.opensearch.org/latest/query-dsl/compound/disjunction-max/"><code>dis_max</code></a>
	 * / <a href="https://docs.opensearch.org/latest/query-dsl/compound/constant-score/"><code>constant_score</code></a>
	 * / <a href="https://docs.opensearch.org/latest/query-dsl/compound/boosting/"><code>boosting</code></a>)
	 * and leaf (<a href="https://docs.opensearch.org/latest/query-dsl/full-text/match/"><code>match</code></a>
	 * / <a href="https://docs.opensearch.org/latest/query-dsl/full-text/multi-match/"><code>multi_match</code></a>
	 * / <a href="https://docs.opensearch.org/latest/query-dsl/full-text/match-phrase/"><code>match_phrase</code></a>
	 * / <a href="https://docs.opensearch.org/latest/query-dsl/full-text/match-phrase-prefix/"><code>match_phrase_prefix</code></a>
	 * / <a href="https://docs.opensearch.org/latest/query-dsl/full-text/match-bool-prefix/"><code>match_bool_prefix</code></a>
	 * / <a href="https://docs.opensearch.org/latest/query-dsl/term/term/"><code>term</code></a>
	 * / <a href="https://docs.opensearch.org/latest/query-dsl/term/terms/"><code>terms</code></a>
	 * / <a href="https://docs.opensearch.org/latest/query-dsl/term/range/"><code>range</code></a>
	 * / <a href="https://docs.opensearch.org/latest/query-dsl/term/exists/"><code>exists</code></a>
	 * / <a href="https://docs.opensearch.org/latest/query-dsl/term/prefix/"><code>prefix</code></a>
	 * / <a href="https://docs.opensearch.org/latest/query-dsl/term/wildcard/"><code>wildcard</code></a>
	 * / <a href="https://docs.opensearch.org/latest/query-dsl/term/fuzzy/"><code>fuzzy</code></a>
	 * / <a href="https://docs.opensearch.org/latest/query-dsl/full-text/simple-query-string/"><code>simple_query_string</code></a>
	 * / <a href="https://docs.opensearch.org/latest/query-dsl/match-all/"><code>match_all</code></a>)
	 * clauses. The server wraps the supplied subtree as a
	 * <code>must</code> clause inside its own <code>bool</code>.
	 * </p>
	 * <p>
	 * <b><code>post_filter</code></b> — optional. Same DSL shape as <code>query</code>, applied
	 * <i>after</i> aggregations are computed: aggregations see the unfiltered population
	 * (matched by <code>query</code>) while the returned hits are narrowed by
	 * <code>post_filter</code>. For filters that should also constrain aggregations, place
	 * them inside <code>query.bool.filter</code> instead.
	 * </p>
	 * <p>
	 * <b><code>aggregations</code></b> — optional. Map of
	 * caller-chosen name to <a href="https://docs.opensearch.org/latest/aggregations/">aggregation</a>
	 * definition. Supports <a href="https://docs.opensearch.org/latest/aggregations/bucket/terms/"><code>terms</code></a>
	 * / <a href="https://docs.opensearch.org/latest/aggregations/bucket/histogram/"><code>histogram</code></a>
	 * / <a href="https://docs.opensearch.org/latest/aggregations/bucket/date-histogram/"><code>date_histogram</code></a>
	 * / <a href="https://docs.opensearch.org/latest/aggregations/bucket/range/"><code>range</code></a>
	 * / <a href="https://docs.opensearch.org/latest/aggregations/bucket/date-range/"><code>date_range</code></a>
	 * / <a href="https://docs.opensearch.org/latest/aggregations/metric/minimum/"><code>min</code></a>
	 * / <a href="https://docs.opensearch.org/latest/aggregations/metric/maximum/"><code>max</code></a>
	 * / <a href="https://docs.opensearch.org/latest/aggregations/metric/average/"><code>avg</code></a>
	 * / <a href="https://docs.opensearch.org/latest/aggregations/metric/sum/"><code>sum</code></a>
	 * / <a href="https://docs.opensearch.org/latest/aggregations/metric/stats/"><code>stats</code></a>
	 * / <a href="https://docs.opensearch.org/latest/aggregations/metric/extended-stats/"><code>extended_stats</code></a>
	 * / <a href="https://docs.opensearch.org/latest/aggregations/metric/value-count/"><code>value_count</code></a>
	 * / <a href="https://docs.opensearch.org/latest/aggregations/metric/cardinality/"><code>cardinality</code></a>
	 * / <a href="https://docs.opensearch.org/latest/aggregations/bucket/missing/"><code>missing</code></a>,
	 * with nested
	 * sub-aggregations. Aggregations need doc values; text-typed columns are auto-routed
	 * through <code>.keyword</code>. The raw aggregation result
	 * comes back on <code>SearchQueryResults.aggregationResults</code>, with field references
	 * rewritten back to bare column names.
	 * </p>
	 * <p>
	 * <b><code>highlight</code></b> — optional. Adds per-field
	 * <a href="https://docs.opensearch.org/latest/search-plugins/searching-data/highlight/">snippet fragments</a>
	 * with matched
	 * terms wrapped in <code>&lt;em&gt;</code> / <code>&lt;/em&gt;</code> tags (configurable
	 * via <code>pre_tags</code> / <code>post_tags</code>) to each
	 * <code>SearchQueryResults.hits[*].highlights</code> entry. <code>highlight.fields</code>
	 * keys are caller column names. Allowlisted highlighter types: <code>unified</code>
	 * (default), <code>plain</code>, <code>fvh</code>; <code>semantic</code> is rejected.
	 * </p>
	 * <p>
	 * <b><code>collapse</code></b> — optional.
	 * <a href="https://docs.opensearch.org/latest/search-plugins/searching-data/collapse-search/">Groups the result list</a>
	 * so only one hit is
	 * returned per distinct value of <code>field</code>. Collapse needs doc values;
	 * text-typed columns are auto-routed through <code>.keyword</code>.
	 * <code>inner_hits</code> is rejected.
	 * </p>
	 * <p>
	 * <b><code>rescore</code></b> — optional.
	 * <a href="https://docs.opensearch.org/latest/query-dsl/rescore/">Re-ranks</a>
	 * the top <code>window_size</code> hits
	 * returned by <code>query</code> using a secondary, typically more expensive, scoring
	 * query. The original ranking is preserved past the rescore window. The inner
	 * <code>rescore_query</code> is validated against the same allowlist as
	 * <code>query</code>.
	 * </p>
	 * <p>
	 * <b><code>sort</code></b> — optional. OpenSearch
	 * <a href="https://docs.opensearch.org/latest/search-plugins/searching-data/sort/">sort</a>
	 * shape (string column name,
	 * <code>{column: "asc|desc"}</code>, or <code>{column: {order: ...}}</code>). The
	 * pseudo-column <code>_score</code> sorts by relevance. When omitted, results are sorted
	 * by relevance descending (<code>_score</code> DESC). Text-typed columns are auto-routed
	 * through <code>.keyword</code>.
	 * </p>
	 * <p>
	 * <b><code>_source</code></b> — optional.
	 * <a href="https://docs.opensearch.org/latest/search-plugins/searching-data/retrieve-specific-fields/">Source filter</a>.
	 * Accepts the full OpenSearch
	 * <code>SourceConfig</code> shape: a boolean (<code>false</code> to omit
	 * <code>_source</code> entirely), an array of column-name patterns (shorthand for
	 * <code>{includes: [...]}</code>), or <code>{includes: [...], excludes: [...]}</code>.
	 * Names are column-name → column-id rewritten before being sent to AOSS.
	 * </p>
	 * <p>
	 * <b><code>from</code></b> — optional. Zero-based
	 * <a href="https://docs.opensearch.org/latest/search-plugins/searching-data/paginate/">pagination</a>
	 * offset; default
	 * <code>0</code>. Maximum reach: <code>from + size</code> &le; ~10,000. For deeper
	 * pagination, switch to <code>search_after</code>; when a cursor is supplied
	 * <code>from</code> is ignored.
	 * </p>
	 * <p>
	 * <b><code>size</code></b> — optional. Maximum number of hits to return per page.
	 * Default: 25. Maximum: 100 (larger values are silently capped). Set to 0 with HITS
	 * omitted from <code>SearchIndexQuery.responseParts</code> to retrieve only aggregation
	 * counts.
	 * </p>
	 * <p>
	 * <b><code>search_after</code></b> — optional. Opaque
	 * <a href="https://docs.opensearch.org/latest/search-plugins/searching-data/paginate/">cursor</a>
	 * emitted as
	 * <code>nextSearchAfter</code> on the previous response. Pass back unchanged. Stable as
	 * long as the underlying sort is unchanged. Mutually exclusive with
	 * <code>from &gt; 0</code>.
	 * </p>
	 * <p>
	 * Any other top-level key returns HTTP 400 naming the offender.
	 * </p>
	 *
	 * <h6>Per-request limits</h6>
	 * <p>Violations return HTTP 400 with a message naming the limit:</p>
	 * <b>Per-request Limits by Surface</b>
	 * <table border="1">
	 * <tr>
	 * <th>Surface</th>
	 * <th>Limit</th>
	 * <th>Value</th>
	 * </tr>
	 * <tr>
	 * <td><code>query</code></td>
	 * <td>max nesting depth</td>
	 * <td>20</td>
	 * </tr>
	 * <tr>
	 * <td><code>query</code></td>
	 * <td>max total clauses</td>
	 * <td>256</td>
	 * </tr>
	 * <tr>
	 * <td><code>query</code></td>
	 * <td>max inline <code>terms</code> array length</td>
	 * <td>1,024</td>
	 * </tr>
	 * <tr>
	 * <td><code>aggregations</code></td>
	 * <td>max nesting depth</td>
	 * <td>10</td>
	 * </tr>
	 * <tr>
	 * <td><code>aggregations</code></td>
	 * <td>max total aggregations</td>
	 * <td>100</td>
	 * </tr>
	 * <tr>
	 * <td><code>aggregations</code></td>
	 * <td>max bucket <code>size</code> / <code>shard_size</code></td>
	 * <td>1,000</td>
	 * </tr>
	 * <tr>
	 * <td><code>highlight</code></td>
	 * <td>max <code>fields</code> entries</td>
	 * <td>50</td>
	 * </tr>
	 * <tr>
	 * <td><code>highlight</code></td>
	 * <td>max <code>number_of_fragments</code> per field</td>
	 * <td>100</td>
	 * </tr>
	 * <tr>
	 * <td><code>highlight</code></td>
	 * <td>max <code>fragment_size</code> per field</td>
	 * <td>1,000</td>
	 * </tr>
	 * <tr>
	 * <td><code>collapse</code></td>
	 * <td>max <code>max_concurrent_group_searches</code></td>
	 * <td>10</td>
	 * </tr>
	 * <tr>
	 * <td><code>rescore</code></td>
	 * <td>max <code>window_size</code> (single stage only)</td>
	 * <td>1,000</td>
	 * </tr>
	 * </table>
	 * <p>
	 * The <code>query</code> limits above apply equally to <code>post_filter</code> and
	 * <code>rescore.rescore_query</code>. In <code>query</code> / <code>post_filter</code> /
	 * <code>rescore.rescore_query</code>, <code>prefix</code> values starting with
	 * <code>*</code> or <code>?</code> are rejected.
	 * </p>
	 * <p>
	 * Any nested <code>highlight_query</code> is validated against the same allowlist as
	 * <code>query</code>.
	 * </p>
	 *
	 * <h6>Disallowed clauses</h6>
	 * <p>The following are rejected anywhere in the body with HTTP 400:</p>
	 * <ul>
	 * <li>Inside <code>query</code> / <code>post_filter</code> /
	 * <code>rescore.rescore_query</code> / <code>highlight.highlight_query</code>:
	 * <code>script</code>, <code>script_score</code>, <code>function_score</code>,
	 * <code>more_like_this</code>, <code>geo_shape</code> / <code>shape</code> with an indexed
	 * shape, <code>has_child</code> / <code>has_parent</code>, <code>terms</code>-lookup
	 * form, <code>percolate</code>, <code>wrapper</code>.</li>
	 * <li>Inside <code>aggregations</code>: scripted aggregations, pipeline aggregations,
	 * embedded <code>script</code>.</li>
	 * <li>Inside <code>highlight</code>: embedded <code>script</code> or
	 * <code>indexed_shape</code>.</li>
	 * <li>Inside <code>collapse</code>: <code>inner_hits</code>.</li>
	 * </ul>
	 *
	 * <p>
	 * Results are returned as a
	 * <a href="${org.sagebionetworks.repo.model.search.SearchQueryResults}">SearchQueryResults</a>.
	 * Use <a href="${GET.search.query.async.get.asyncToken}">GET /search/query/async/get/{asyncToken}</a>
	 * to poll for results — while the job is running the GET returns HTTP 202 with a
	 * <a href="${org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus}">AsynchronousJobStatus</a>.
	 * </p>
	 * <p>
	 * The caller must have <code>READ</code> access to the source table or view that backs the
	 * SearchIndex. Row-level access is enforced automatically: rows the caller cannot read are
	 * filtered out of the results before they leave the server.
	 * </p>
	 *
	 * @param userId The ID of the authenticated user.
	 * @param request The search query request including the <code>searchIndexId</code> and the
	 *                embedded <code>SearchQuery</code>.
	 * @return An async job token. Poll
	 *         <a href="${GET.search.query.async.get.asyncToken}">GET /search/query/async/get/{asyncToken}</a>
	 *         for the final results.
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.SEARCH_QUERY_ASYNC_START, method = RequestMethod.POST)
	public @ResponseBody AsyncJobId startQuery(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody SearchIndexQuery request) {
		AsynchronousJobStatus job = searchIndexQueryService.startSearchQuery(userId, request);
		AsyncJobId asyncJobId = new AsyncJobId();
		asyncJobId.setToken(job.getJobId());
		return asyncJobId;
	}

	/**
	 * Get the results of a previously started asynchronous search query.
	 * <p>
	 * Note: When the result is not ready yet, this method will return a status
	 * code of 202 (ACCEPTED) and the response body will be a
	 * <a href="${org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus}">AsynchronousJobStatus</a> object.
	 * </p>
	 *
	 * @param userId The ID of the authenticated user.
	 * @param asyncToken The token returned by the start query endpoint.
	 * @return The search query results.
	 * @throws Throwable
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SEARCH_QUERY_ASYNC_GET, method = RequestMethod.GET)
	public @ResponseBody SearchQueryResults getQueryResults(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String asyncToken) throws Throwable {
		AsynchronousJobStatus jobStatus = searchIndexQueryService.getSearchQueryResults(userId, asyncToken);
		return (SearchQueryResults) jobStatus.getResponseBody();
	}

	/**
	 * Perform a synchronous autocomplete search query against a
	 * <a href="${org.sagebionetworks.repo.model.search.table.SearchIndex}">SearchIndex</a>.
	 * <p>
	 * Purpose-built for type-ahead input. The request shape is deliberately narrow: there is
	 * no <code>size</code> (the server caps every response at 8 hits), no <code>from</code>
	 * / <code>search_after</code> (a dropdown does not paginate), no <code>sort</code>
	 * (results are ordered by relevance), and no <code>aggregations</code> /
	 * <code>responseParts</code> (autocomplete returns matching hits
	 * only). For anything beyond a type-ahead lookup — scored relevance, faceting, totals,
	 * deep pagination — use the async
	 * <a href="${POST.search.query.async.start}">POST /search/query/async/start</a> endpoint
	 * with <a href="${org.sagebionetworks.repo.model.search.table.SearchIndexQuery}">SearchIndexQuery</a>
	 * instead.
	 * </p>
	 *
	 * <h6>Allowlisted top-level keys</h6>
	 * <p>
	 * Only <code>query</code> and <code>_source</code> are accepted on the
	 * <code>searchQuery</code>; any other top-level key returns HTTP 400 naming the offender.
	 * </p>
	 * <p>
	 * <b><code>query</code></b> — required. The top-level clause must be one of
	 * <a href="https://docs.opensearch.org/latest/query-dsl/term/prefix/"><code>prefix</code></a>,
	 * <a href="https://docs.opensearch.org/latest/query-dsl/full-text/match-phrase-prefix/"><code>match_phrase_prefix</code></a>, or
	 * <a href="https://docs.opensearch.org/latest/query-dsl/full-text/match-bool-prefix/"><code>match_bool_prefix</code></a>;
	 * any other clause type (including compound clauses such
	 * as <code>bool</code>) is rejected with HTTP 400. The same per-clause guarantees as the
	 * async search endpoint apply: scripts, cross-index references, and the
	 * <code>wrapper</code> form are rejected; depth, total-clause, and inline
	 * <code>terms</code> array length are capped.
	 * </p>
	 * <p>
	 * <b><code>_source</code></b> — optional. Source filter; same shape as on
	 * <a href="${org.sagebionetworks.repo.model.search.SearchQuery}">SearchQuery._source</a>.
	 * Narrowing this to the columns the dropdown actually displays reduces response size,
	 * especially for wide indexes.
	 * </p>
	 *
	 * <h6>Field references</h6>
	 * <p>
	 * On text-typed columns (STRING, STRING_LIST, MEDIUMTEXT, LARGETEXT, LINK), use
	 * <code>{columnName}.keyword</code> for <code>prefix</code> and
	 * <code>match_phrase_prefix</code> (these are exact-match operations against the raw /
	 * non-tokenized sub-field). <code>match_bool_prefix</code> uses the bare column name (it
	 * analyzes the input). Numeric / boolean / keyword columns always use the bare name.
	 * </p>
	 *
	 * <p>Example — prefix match on the <code>title.keyword</code> sub-field:</p>
	 * <pre><code>"searchQuery": { "query": { "prefix": { "title.keyword": "can" } } }</code></pre>
	 *
	 * @param userId The ID of the authenticated user.
	 * @param request The autocomplete request including the <code>searchIndexId</code> and
	 *                a prefix-flavored DSL clause.
	 * @return The autocomplete hits (up to 8).
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SEARCH_AUTOCOMPLETE, method = RequestMethod.POST)
	public @ResponseBody SearchQueryResults autocomplete(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody SearchAutocompleteRequest request) {
		return searchIndexQueryService.autocomplete(userId, request);
	}
}
