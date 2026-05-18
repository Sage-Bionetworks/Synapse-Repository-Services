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
 * define how a search index analyzes, tokenizes, and matches text. They map directly to
 * <a href="https://docs.opensearch.org/latest/analyzers/custom-analyzer/">OpenSearch custom analyzer</a>
 * concepts and are assembled into a
 * <a href="${org.sagebionetworks.repo.model.search.table.SearchConfiguration}">SearchConfiguration</a>
 * that can be attached to a project.
 * </p>
 *
 * <h6>Text Analyzers</h6>
 * <p>
 * A <a href="${org.sagebionetworks.repo.model.search.table.TextAnalyzer}">TextAnalyzer</a> defines
 * a text analysis pipeline consisting of a
 * <a href="https://docs.opensearch.org/latest/analyzers/tokenizers/index/">tokenizer</a> and an
 * ordered list of <a href="https://docs.opensearch.org/latest/analyzers/token-filters/index/">token filters</a>.
 * The tokenizer breaks text into individual tokens, and the filters process those tokens
 * (e.g., lowercasing, stemming, stop word removal). Custom token filters such as stop word lists
 * can also be defined inline.
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
 * This corresponds to the OpenSearch
 * <a href="https://docs.opensearch.org/latest/mappings/mapping-parameters/analyzer/">per-field analyzer mapping</a>
 * capability.
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
 * A <a href="${org.sagebionetworks.repo.model.search.table.SynonymSet}">SynonymSet</a> defines
 * synonym rules that are applied during index construction via the OpenSearch
 * <a href="https://docs.opensearch.org/latest/analyzers/token-filters/synonym/">synonym token filter</a>.
 * Two rule types are supported:
 * </p>
 * <ul>
 * <li><b>Equivalent</b> &mdash; all terms are interchangeable (e.g., &quot;cancer&quot;,
 *     &quot;tumor&quot;, &quot;neoplasm&quot;). Searching for any one term matches documents
 *     containing any of the others.</li>
 * <li><b>Explicit</b> &mdash; a directional expansion where the first term expands to the
 *     remaining terms (e.g., &quot;AD&quot; expands to &quot;Alzheimer's disease&quot;).</li>
 * </ul>
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
 * bundles a default <a href="${org.sagebionetworks.repo.model.search.table.TextAnalyzer}">TextAnalyzer</a>,
 * zero or more <a href="${org.sagebionetworks.repo.model.search.table.SynonymSet}">SynonymSets</a>,
 * and zero or more <a href="${org.sagebionetworks.repo.model.search.table.ColumnAnalyzerOverride}">ColumnAnalyzerOverrides</a>
 * into a reusable configuration. These settings are used to build the
 * <code>analysis</code> section of an OpenSearch index definition when a search index is created.
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
 * (entity → folder → project). Requires EDIT permission on the entity.
 * </p>
 * <ul>
 * <li><a href="${PUT.entity.entityId.searchconfig.binding}">PUT /entity/{entityId}/searchconfig/binding</a></li>
 * <li><a href="${GET.entity.entityId.searchconfig.binding}">GET /entity/{entityId}/searchconfig/binding</a></li>
 * <li><a href="${DELETE.entity.entityId.searchconfig.binding}">DELETE /entity/{entityId}/searchconfig/binding</a></li>
 * </ul>
 *
 * <h6>Public Resources and Cross-Organization Referencing</h6>
 * <p>
 * All search management resources (TextAnalyzers, SynonymSets, ColumnAnalyzerOverrides) are
 * <b>publicly readable</b>. Any authenticated user can list and retrieve resources from any
 * Organization. This enables cross-organization reuse: a
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
 * <h6>Search Queries</h6>
 * <p>
 * Query a <a href="${org.sagebionetworks.repo.model.search.table.SearchIndex}">SearchIndex</a>
 * using the async job pattern. Submit a
 * <a href="${org.sagebionetworks.repo.model.search.table.SearchIndexQuery}">SearchIndexQuery</a>
 * to start a job, then poll for
 * <a href="${org.sagebionetworks.repo.model.search.SearchQueryResults}">SearchQueryResults</a>.
 * A synchronous autocomplete endpoint is also available for typeahead patterns.
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
 * <td>text + keyword sub-field (or keyword + searchable, depending on analyzer)</td>
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
	 * The analyzer's settings are validated against the OpenSearch analysis API before creation.
	 * Invalid tokenizer or filter configurations will return a 400 error.
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
	 * Concurrency is managed via the etag field. If the etag in the request does not match
	 * the current etag, a 409 Conflict is returned.
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
	 * If organizationName is null, all text analyzers across all Organizations are returned.
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
	 *        and at least one override entry mapping a column to an analyzer.
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
	 * If organizationName is null, all column analyzer overrides across all Organizations are returned.
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
	 *
	 * @param userId The ID of the authenticated user.
	 * @param request The synonym set to create. Must include organizationName, name, and rules.
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
	 * Concurrency is managed via the etag field. If the etag in the request does not match
	 * the current etag, a 409 Conflict is returned.
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
	 * If organizationName is null, all synonym sets across all Organizations are returned.
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
	 * If organizationName is null, all search configurations across all Organizations are returned.
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
	 * to an entity. The caller must have EDIT permission on the entity. Any existing binding on that
	 * entity is replaced. The effective configuration for any entity is resolved by walking up the
	 * hierarchy (entity → folder → project).
	 *
	 * @param userId The ID of the authenticated user.
	 * @param entityId The ID of the entity to bind to.
	 * @param request The bind request containing the searchConfigurationId.
	 * @return The created binding.
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
	 * for an entity by walking up the hierarchy (entity → folder → project). Returns the first binding
	 * found on the entity or any ancestor.
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
	 * on a specific entity. Does not affect ancestor bindings. The caller must have EDIT permission on
	 * the entity.
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
	 * <a href="${org.sagebionetworks.repo.model.search.SearchQuery}">SearchQuery</a>
	 * — see that schema for the full set of available knobs. Highlights:
	 * </p>
	 * <ul>
	 *   <li><b>queryType</b> — the full-text query model. See
	 *       <a href="${org.sagebionetworks.repo.model.search.SearchQueryType}">SearchQueryType</a>
	 *       for the per-type explanation of scoring, accepted parameters, and when to use each.
	 *       Default is <code>SIMPLE_QUERY_STRING</code>. An empty or null <code>queryText</code>
	 *       automatically selects <code>MATCH_ALL</code>.</li>
	 *   <li><b>queryFields</b> — restrict the search to specific columns, with optional per-field
	 *       boost using <code>column^N</code> syntax (e.g. <code>"title^3"</code>). Empty means
	 *       all indexed fields.</li>
	 *   <li><b>Filters</b> — <code>termsFilters</code>, <code>rangeFilters</code>,
	 *       <code>existsFilters</code>, and <code>notExistsFilters</code> narrow the result set.
	 *       All filters run in non-scoring context (yes/no matching, cacheable) — they do not
	 *       contribute to relevance.</li>
	 *   <li><b>Facets</b> — <code>facetRequests</code> produce bucket aggregations per column.
	 *       Sort by <code>COUNT</code> or <code>KEY</code> in either direction; cap each facet
	 *       with <code>maxValueCount</code>.</li>
	 *   <li><b>Response tuning</b> — <code>returnFields</code>, <code>sort</code> (including the
	 *       special <code>_score</code> pseudo-column), <code>offset</code>,
	 *       <code>limit</code> (capped at 100), and <code>highlight</code>.</li>
	 * </ul>
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
	 * This endpoint is purpose-built for type-ahead input: it overrides any supplied
	 * <code>queryType</code> with <code>PREFIX</code> (see
	 * <a href="${org.sagebionetworks.repo.model.search.SearchQueryType}">SearchQueryType</a>
	 * for the full description) and caps <code>limit</code> at 8 to keep responses small
	 * and latency low. Caller-supplied <code>facetRequests</code> and
	 * <code>highlight</code> are ignored — autocomplete returns matching hits only.
	 * </p>
	 * <p>
	 * For anything that needs scored relevance, faceting, or result counts, use the async
	 * <a href="${POST.search.query.async.start}">POST /search/query/async/start</a> endpoint
	 * instead.
	 * </p>
	 *
	 * @param userId The ID of the authenticated user.
	 * @param request The search query request including the <code>searchIndexId</code>.
	 * @return The autocomplete results (up to 8 hits).
	 */
	@RequiredScope({ view })
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.SEARCH_AUTOCOMPLETE, method = RequestMethod.POST)
	public @ResponseBody SearchQueryResults autocomplete(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody SearchIndexQuery request) {
		return searchIndexQueryService.autocomplete(userId, request);
	}
}
