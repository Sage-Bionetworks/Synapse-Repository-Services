package org.sagebionetworks.repo.manager.search;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;

import org.json.JSONArray;
import org.json.JSONObject;
import org.opensearch.client.json.JsonpDeserializer;
import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.json.JsonpSerializable;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch._types.FieldSort;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.query_dsl.HybridQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.search.FieldCollapse;
import org.opensearch.client.opensearch.core.search.Highlight;
import org.opensearch.client.opensearch.core.search.Rescore;
import org.opensearch.client.opensearch.core.search.SourceConfig;
import org.opensearch.client.opensearch.indices.IndexSettingsAnalysis;
import org.sagebionetworks.repo.model.search.SearchQueryPart;
import org.sagebionetworks.repo.model.search.dsl.SearchPipeline;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Boundary helpers for the opaque-{@code "type": "object"} JSON values carried on the
 * search-feature DTOs &mdash; {@code TextAnalyzer.settings},
 * {@code SynonymSet.definition}, {@code SearchConfiguration.defaultAnalyzer}, and the
 * elements of {@code SearchConfiguration.columnAnalyzerOverrides} /
 * {@code ColumnAnalyzerOverrideEntry.analyzer}.
 *
 * <p>Four concerns:</p>
 * <ol>
 *   <li><b>Shape conversion.</b> {@link #parse(Object)}
 *       bridges the four shapes a curator-supplied value can take (raw JSON {@link String},
 *       {@link JSONObject} / {@link JSONArray}, {@link JSONObjectAdapter},
 *       Jackson-friendly {@link Map} / {@link java.util.Collection} / scalar) to the
 *       canonical forms the pipeline needs.</li>
 *   <li><b>Reference detection.</b> {@link #readRef(Object)} /
 *       {@link #readRef(JsonNode)} surface the qualified-name string from a
 *       {@code {"$ref": "{org}-{name}"}} reference object, regardless of whether the
 *       caller passes a {@link Map}, {@link JSONObject}, or {@link JsonNode}.</li>
 *   <li><b>Inline materialization.</b> {@link #toInline(Object, Class)} converts an
 *       inline literal value (a {@link Map} / {@link JSONObject} / etc.) into the typed
 *       POJO of the inlined resource (e.g. {@code ColumnAnalyzerOverride}) so the rest of
 *       the pipeline can work with typed accessors.
 *       {@link #toInlineAnalyzerSettings(Object, Function)} is the analyzer-slot variant
 *       &mdash; the inline value is a bare OpenSearch {@code settings.analysis} block
 *       (not wrapped in a {@code TextAnalyzer} envelope), so it goes straight to the
 *       typed {@link IndexSettingsAnalysis}.</li>
 *   <li><b>Analyzer-typed splice + deserialize.</b>
 *       {@link #spliceRefsInFilterMap(JsonNode, String, Function)} replaces every
 *       {@code $ref} entry in an analyzer's filter map with its resolved JSON;
 *       {@link #resolveAnalyzerSettings(JsonNode, Function)} bundles that splice with the
 *       OpenSearch typed deserializer to hand callers a typed
 *       {@link IndexSettingsAnalysis}.</li>
 * </ol>
 *
 * <p>Synapse only verifies that the JSON parses and that any refs resolve. AOSS / the
 * typed analyzer deserializer validate the analyzer / token-filter shape itself.</p>
 */
public final class SearchOpaqueJsonUtil {

	/**
	 * The single-key reference shape: {@code {"$ref": "{organizationName}-{name}"}}.
	 * Same shape on every binding slot: SearchConfiguration.defaultAnalyzer,
	 * SearchConfiguration.columnAnalyzerOverrides[*], ColumnAnalyzerOverrideEntry.analyzer,
	 * and the entries inside a TextAnalyzer's settings.filter registry.
	 */
	public static final String REF_KEY = "$ref";

	/**
	 * Top-level key holding an analyzer's filter registry inside a TextAnalyzer's
	 * {@code settings} blob. Per the schema, {@code $ref} is only permitted as the value
	 * of an entry inside this map.
	 */
	private static final String FILTER_KEY = "filter";

	private static final ObjectMapper MAPPER = new ObjectMapper();

	// JsonpMapper for the OpenSearch typed-deserializer in resolveAnalyzerSettings; owns
	// its own instance so callers don't have to plumb the OpenSearchClient transport in
	// just to get the typed analyzer model. The no-arg JacksonJsonpMapper picks up the
	// same Jackson defaults the client uses.
	private static final JsonpMapper JSONP_MAPPER = new JacksonJsonpMapper();

	private SearchOpaqueJsonUtil() {
		// utility
	}

	// ---------- shape conversion ----------

	/**
	 * Parse a JSON value into a Jackson tree. Accepts every shape an opaque-Object POJO
	 * field can hold: raw JSON {@link String}, {@link JSONObject} / {@link JSONArray},
	 * {@link JSONObjectAdapter}, Jackson-friendly {@link Map} / {@link java.util.Collection}
	 * / scalar.
	 *
	 * @throws IllegalArgumentException when {@code json} is {@code null} or fails to parse.
	 */
	public static JsonNode parse(Object json) {
		if (json == null) {
			throw new IllegalArgumentException("JSON object is required.");
		}
		try {
			return MAPPER.readTree(asJsonString(json));
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Invalid JSON: " + e.getOriginalMessage(), e);
		}
	}

	/**
	 * Render any of the supported opaque-JSON value shapes to a JSON string. See the
	 * class javadoc for the accepted shapes. Package-private so each branch is
	 * independently testable.
	 */
	static String asJsonString(Object json) {
		if (json instanceof String) {
			return (String) json;
		}
		if (json instanceof JSONObject || json instanceof JSONArray) {
			return json.toString();
		}
		if (json instanceof JSONObjectAdapter) {
			return ((JSONObjectAdapter) json).toJSONString();
		}
		if (json instanceof JSONEntity) {
			// A generated POJO (e.g. SearchQuery) whose opaque "type":"object" fields hold
			// JSONObjectAdapter values after a JSON round-trip. Jackson cannot serialize those
			// adapter instances, so render the whole entity through the schema adapter.
			try {
				return EntityFactory.createJSONStringForEntity((JSONEntity) json);
			} catch (JSONObjectAdapterException e) {
				throw new IllegalArgumentException("Invalid JSON: " + e.getMessage(), e);
			}
		}
		try {
			return MAPPER.writeValueAsString(json);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Invalid JSON: " + e.getOriginalMessage(), e);
		}
	}

	// ---------- OpenSearch typed-object (JsonP) bridging ----------

	/**
	 * Deserialize a JSON tree into a typed OpenSearch client object via its
	 * {@link JsonpDeserializer} (e.g. {@code Query._DESERIALIZER},
	 * {@code Aggregation._DESERIALIZER}). Reuses the shared {@link #JSONP_MAPPER} so callers
	 * don't repeat the parser/mapper plumbing.
	 */
	public static <T> T fromJsonpTree(JsonNode node, JsonpDeserializer<T> deserializer) {
		try (JsonParser parser = JSONP_MAPPER.jsonProvider().createParser(new StringReader(node.toString()))) {
			return deserializer.deserialize(parser, JSONP_MAPPER);
		}
	}

	/**
	 * Serialize a typed OpenSearch client object ({@code Aggregate}, {@code FieldValue}, ...) to a
	 * Jackson tree — the inverse of {@link #fromJsonpTree}, for assembling typed results back into
	 * an opaque JSON response.
	 */
	public static JsonNode toJsonpTree(JsonpSerializable value) {
		StringWriter writer = new StringWriter();
		try (JsonGenerator generator = JSONP_MAPPER.jsonProvider().createGenerator(writer)) {
			value.serialize(generator, JSONP_MAPPER);
		}
		try {
			return MAPPER.readTree(writer.toString());
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Failed to re-parse serialized OpenSearch value", e);
		}
	}

	// ---------- Jackson tree-construction helpers ----------

	/**
	 * Empty {@link ObjectNode} backed by the shared {@link #MAPPER}. Callers building up an
	 * opaque-JSON response (e.g. the aggregations result envelope) should reach
	 * for this rather than instantiating their own {@code ObjectMapper}.
	 */
	public static ObjectNode objectNode() {
		return MAPPER.createObjectNode();
	}

	/**
	 * Empty {@link com.fasterxml.jackson.databind.node.ArrayNode} backed by the shared
	 * {@link #MAPPER}. Same rationale as {@link #objectNode()}.
	 */
	public static com.fasterxml.jackson.databind.node.ArrayNode arrayNode() {
		return MAPPER.createArrayNode();
	}

	// ---------- caller-DSL → typed OpenSearch model ----------

	/**
	 * Parse, validate, field-rewrite, typed-deserialize, and apply a caller-supplied
	 * OpenSearch {@code _search} request body to {@code req}.
	 *
	 * <p>Each present sub-key is scanned for forbidden keys, field-rewritten (column name &rarr;
	 * column id, with auto-routing for text-typed columns where the operation needs doc values),
	 * typed-deserialized, structurally validated, and pushed onto the request builder.</p>
	 *
	 * <p>Defaults applied: omitted {@code from} &rarr; 0; omitted {@code size} &rarr;
	 * {@code defaultSize}; values past {@code maxSize} clamped. When {@code search_after} is
	 * present the cursor determines the page start and {@code from} is forced to 0.</p>
	 *
	 * <p>Behavior gated by {@code options}: when {@link SearchQueryPart#HITS} is absent the
	 * request goes out with {@code size=0} and sort / collapse / rescore / highlight /
	 * source / search_after are skipped. {@link SearchQueryPart#TOTAL_HITS} drives the
	 * {@code track_total_hits} count variant.</p>
	 *
	 * <p>Each query in {@code accessFilters} is AND-ed (as a {@code bool.filter} clause) with
	 * the caller's query, so a document must satisfy both the query and every filter. A
	 * benefactor-less source passes an empty list, applying no access filter.</p>
	 *
	 * <p>A {@code hybrid} body becomes the request's root query instead; see
	 * {@link #buildHybridQuery} and {@link #resolveSearchPipeline}.</p>
	 *
	 * @param opaque       the caller's body, in any of the shapes {@link #parse(Object)} accepts
	 * @param ctx          the column-name &rarr; column-id routing context for the target index
	 * @param req          the target {@link SearchRequest.Builder} (mutated in place)
	 * @param options      the response-parts the caller asked for
	 * @param defaultSize  default {@code size} when the body omits it
	 * @param maxSize      upper bound on {@code size}; larger values clamp
	 * @param accessFilters server-side access-control filters to AND with the query; must not be null
	 * @param semanticModelId the embedding model to place on every {@code neural} clause, or
	 *                     {@code null} when the index has no semantic field and every {@code neural}
	 *                     clause is dropped
	 * @param savedPipeline the saved search pipeline a {@code hybrid} body runs with when it carries
	 *                     no inline one: the target of its {@code $ref}, else the index's default;
	 *                     {@code null} for the system default
	 */
	static AppliedBody applyBodyToRequest(Object opaque, SearchFieldRewriter.RoutingContext ctx,
			SearchRequest.Builder req, Set<SearchQueryPart> options,
			int defaultSize, int maxSize, List<Query> accessFilters, String semanticModelId,
			SearchPipeline savedPipeline) {
		return applyBodyToRequest(opaque, ctx, req, options, defaultSize, maxSize, false, accessFilters,
				semanticModelId, savedPipeline);
	}

	/**
	 * What {@link #applyBodyToRequest} hands back to the transport.
	 *
	 * @param from           the effective {@code from} written to {@code req}, echoed to the caller
	 *                       as {@code SearchQueryResults.offset}
	 * @param searchPipeline the {@code search_pipeline} block to splice into the request body, or
	 *                       {@code null} for a non-hybrid body. The typed client only names a
	 *                       registered pipeline, as a query parameter, so an inline one travels
	 *                       separately.
	 * @param hybridMinScore the caller's {@code hybrid.min_score}, or {@code null}. The typed
	 *                       client's {@code HybridQuery} has no {@code min_score} property, so it
	 *                       travels separately to be spliced into {@code query.hybrid.min_score}.
	 */
	record AppliedBody(int from, JsonNode searchPipeline, JsonNode hybridMinScore) {
	}

	/**
	 * Autocomplete variant of {@link #applyBodyToRequest}: narrows the top-level allowlist
	 * to {@code query} and {@code _source}, and enforces the autocomplete top-level query
	 * allowlist on the inner clause.
	 */
	static int applyAutocompleteBodyToRequest(Object opaque,
			SearchFieldRewriter.RoutingContext ctx, SearchRequest.Builder req,
			Set<SearchQueryPart> options, int defaultSize, List<Query> accessFilters) {
		return applyBodyToRequest(opaque, ctx, req, options, defaultSize, defaultSize, true, accessFilters,
				null, null).from();
	}

	private static AppliedBody applyBodyToRequest(Object opaque, SearchFieldRewriter.RoutingContext ctx,
			SearchRequest.Builder req, Set<SearchQueryPart> options,
			int defaultSize, int maxSize, boolean autocomplete, List<Query> accessFilters,
			String semanticModelId, SearchPipeline savedPipeline) {
		// The body is the generated SearchQuery / SearchAutocompleteBody POJO, so any key outside the
		// schema was already rejected with HTTP 400 at the request boundary, and each surface with an
		// opaque slot is forbidden-key scanned individually as it is parsed below.
		JsonNode body = parse(opaque);
		List<Query> filters = accessFilters == null ? Collections.emptyList() : accessFilters;

		JsonNode hybrid = null;
		if (!autocomplete) {
			SearchDslValidator.validateTopLevelQueryChoice(body);
			hybrid = nodeOrNull(body, "hybrid");
		}
		boolean hybridRelevanceRanked = hybrid != null && SearchDslValidator.isRelevanceRanked(body);
		JsonNode searchPipeline = null;
		JsonNode hybridMinScore = null;
		if (hybrid != null) {
			List<Integer> sentPositions = sentClausePositions(hybrid, semanticModelId);
			searchPipeline = resolveSearchPipeline(nodeOrNull(body, "search_pipeline"),
					savedPipeline == null ? null : parse(savedPipeline), sentPositions);
			hybridMinScore = nodeOrNull(hybrid, "min_score");
			((ObjectNode) hybrid).remove("min_score");
			req.query(buildHybridQuery(hybrid, sentPositions, ctx, filters, semanticModelId));
		} else {
			Query query = parseRequiredQuery(body, ctx, autocomplete);
			// Wrap the caller's allowlist-validated query in a server-controlled bool: the caller's
			// query goes in must, and every server-side access-control filter goes in filter (AND
			// semantics) so a document must satisfy the query and every benefactor filter.
			req.query(wrapWithAccessFilters(query, filters));
		}

		if (!autocomplete) {
			JsonNode postFilter = body.get("post_filter");
			if (postFilter != null && !postFilter.isNull()) {
				req.postFilter(parseQuery(postFilter, ctx, false));
			}
			Map<String, Aggregation> aggregations = parseAggregations(body, ctx);
			if (!aggregations.isEmpty()) {
				req.aggregations(aggregations);
			}
		}

		boolean returnHits = options.contains(SearchQueryPart.HITS);
		boolean returnTotalHits = options.contains(SearchQueryPart.TOTAL_HITS);
		List<FieldValue> searchAfter = parseSearchAfter(body);
		boolean usingCursor = !searchAfter.isEmpty();
		int from = usingCursor ? 0 : SearchDslValidator.resolveFrom(body);
		int size = SearchDslValidator.resolveSize(body, defaultSize, maxSize);
		if (hybrid != null) {
			SearchDslValidator.validateHybridPageDepth(from, size);
		}
		if (hybridRelevanceRanked) {
			SearchDslValidator.validateHybridSearchAfter(usingCursor);
		}

		req.from(from);
		req.size(returnHits ? size : 0);
		req.trackTotalHits(t -> returnTotalHits
				? t.count(Integer.MAX_VALUE)
				: t.enabled(false));

		// Source filters, sort, collapse, rescore, highlight, and search_after are
		// meaningless without hits.
		if (returnHits) {
			JsonNode source = body.get("_source");
			if (source != null && !source.isNull()) {
				req.source(parseSource(source, ctx));
			}
			req.sort(parseSort(body, ctx));
			if (usingCursor) {
				req.searchAfter(searchAfter);
			}
			if (!autocomplete) {
				JsonNode highlight = body.get("highlight");
				if (highlight != null && !highlight.isNull()) {
					req.highlight(parseHighlight(highlight, ctx));
				}
				JsonNode collapse = body.get("collapse");
				if (collapse != null && !collapse.isNull()) {
					req.collapse(parseCollapse(collapse, ctx));
				}
				JsonNode rescore = body.get("rescore");
				if (rescore != null && !rescore.isNull()) {
					req.rescore(parseRescore(rescore, ctx));
				}
			}
		}
		return new AppliedBody(from, searchPipeline, hybridMinScore);
	}

	private static Query wrapWithAccessFilters(Query query, List<Query> filters) {
		return Query.of(q -> q.bool(b -> {
			b.must(query);
			if (!filters.isEmpty()) {
				b.filter(filters);
			}
			return b;
		}));
	}

	/** The value at {@code key}, or {@code null} when the key is absent or explicitly JSON null. */
	static JsonNode nodeOrNull(JsonNode body, String key) {
		JsonNode node = body.get(key);
		return (node == null || node.isNull()) ? null : node;
	}

	// ---------- hybrid query ----------

	/**
	 * The pipeline a hybrid query runs with when neither the request nor the index supplies one.
	 * Carries no {@code weights}, so OpenSearch weights every clause it receives equally.
	 */
	private static final JsonNode SYSTEM_DEFAULT_PIPELINE = parse("""
			{"phase_results_processors": [{"normalization-processor": {
				"normalization": {"technique": "min_max"},
				"combination": {"technique": "arithmetic_mean"}}}]}""");

	/**
	 * The positions in {@code hybrid.queries} of the clauses sent to OpenSearch, in order. Every
	 * clause is sent, except that a {@code neural} clause is dropped when {@code semanticModelId} is
	 * {@code null}: the index has no semantic field, so the query is answered by its other clauses.
	 *
	 * @throws IllegalArgumentException when the clause count is out of range, a {@code neural}
	 *         clause names an unknown vector field, or every clause would be dropped
	 */
	static List<Integer> sentClausePositions(JsonNode hybrid, String semanticModelId) {
		JsonNode queries = hybrid.get("queries");
		SearchDslValidator.validateHybridClauseCount(queries);
		List<Integer> sent = new ArrayList<>(queries.size());
		for (int i = 0; i < queries.size(); i++) {
			if (!SearchDslValidator.isNeuralClause(queries.get(i)) || semanticModelId != null) {
				sent.add(i);
			}
		}
		if (sent.isEmpty()) {
			throw new IllegalArgumentException("every clause of body.hybrid.queries is a neural clause, but"
					+ " this search index has no semantic field. Flag at least one column 'semantic' in its"
					+ " search configuration, or add a keyword clause.");
		}
		return sent;
	}

	/**
	 * The {@code search_pipeline} block a hybrid request runs with, narrowed to the clauses sent.
	 *
	 * <p>The pipeline is the request's inline pipeline when given, otherwise the saved pipeline,
	 * falling back to the system default. Its weights and bounds are positional against the clauses
	 * of {@code body.hybrid.queries}: each array keeps the entries at the sent clauses' positions, and
	 * the kept weights are rescaled to sum to 1.0, as OpenSearch requires one entry per clause and
	 * weights that sum to 1.0. A dropped {@code neural} clause so takes its own weight and bounds with
	 * it. Absent arrays stay absent.</p>
	 *
	 * @param requestPipeline the body's {@code search_pipeline}, or {@code null}
	 * @param savedPipeline   the saved pipeline, or {@code null} for the system default
	 * @param sentPositions   the ascending positions of the clauses sent, from {@link #sentClausePositions}
	 * @throws IllegalArgumentException when a weights or bounds array has no entry at a sent
	 *         position, or the kept weights sum to 0
	 */
	static JsonNode resolveSearchPipeline(JsonNode requestPipeline, JsonNode savedPipeline,
			List<Integer> sentPositions) {
		JsonNode pipeline;
		if (requestPipeline != null && readRef(requestPipeline) == null) {
			pipeline = requestPipeline;
		} else {
			pipeline = savedPipeline == null ? SYSTEM_DEFAULT_PIPELINE : savedPipeline;
		}
		// Copied so the arrays can be rewritten without touching the caller's or the shared default pipeline.
		JsonNode resolved = pipeline.deepCopy();
		for (JsonNode processor : resolved.path("phase_results_processors")) {
			JsonNode normalizationProcessor = processor.path("normalization-processor");
			JsonNode normalizationParameters = normalizationProcessor.path("normalization").path("parameters");
			keepSentEntries(normalizationParameters, "lower_bounds", sentPositions, false);
			keepSentEntries(normalizationParameters, "upper_bounds", sentPositions, false);
			keepSentEntries(normalizationProcessor.path("combination").path("parameters"), "weights",
					sentPositions, true);
		}
		return resolved;
	}

	/**
	 * Replace {@code parameters[key]}, when present, with its entries at {@code sentPositions},
	 * rescaled to sum to 1.0 when {@code rescale}.
	 */
	private static void keepSentEntries(JsonNode parameters, String key, List<Integer> sentPositions,
			boolean rescale) {
		JsonNode entries = parameters.get(key);
		if (entries == null || !entries.isArray()) {
			return;
		}
		int lastPosition = sentPositions.get(sentPositions.size() - 1);
		if (entries.size() <= lastPosition) {
			throw new IllegalArgumentException("this query sends the hybrid clause at position " + lastPosition
					+ ", but the search pipeline's " + key + " hold only " + entries.size() + " entries");
		}
		com.fasterxml.jackson.databind.node.ArrayNode kept = arrayNode();
		for (int position : sentPositions) {
			kept.add(entries.get(position));
		}
		((ObjectNode) parameters).set(key, rescale ? rescaleToUnitSum(kept) : kept);
	}

	private static com.fasterxml.jackson.databind.node.ArrayNode rescaleToUnitSum(
			com.fasterxml.jackson.databind.node.ArrayNode weights) {
		double total = 0.0;
		for (JsonNode weight : weights) {
			total += weight.asDouble();
		}
		if (total <= 0.0) {
			throw new IllegalArgumentException("the search pipeline's weights for the clauses of this query"
					+ " sum to 0, leaving no way to weight them");
		}
		com.fasterxml.jackson.databind.node.ArrayNode rescaled = arrayNode();
		for (JsonNode weight : weights) {
			rescaled.add(weight.asDouble() / total);
		}
		return rescaled;
	}

	/**
	 * Build the {@code hybrid} clause that becomes the request's root query from the clauses at
	 * {@code sentPositions}. The clause is never wrapped in another query: OpenSearch rejects a
	 * hybrid clause nested in most compounds, and for the few it accepts it silently ignores the
	 * search pipeline.
	 *
	 * <p>Access filters are applied twice &mdash; AND-ed into {@code hybrid.filter}, which
	 * pre-filters every clause including the vector search, and again inside each clause. Either
	 * placement alone is sufficient, so no single slot is the only thing enforcing row-level access.
	 * A caller's own {@code hybrid.filter} is AND-ed with them rather than replaced.</p>
	 *
	 * <p>{@code pagination_depth} is always {@link SearchDslValidator#HYBRID_PAGINATION_DEPTH}.</p>
	 */
	private static Query buildHybridQuery(JsonNode hybridNode, List<Integer> sentPositions,
			SearchFieldRewriter.RoutingContext ctx, List<Query> accessFilters, String semanticModelId) {
		JsonNode queries = hybridNode.get("queries");
		com.fasterxml.jackson.databind.node.ArrayNode sent = arrayNode();
		for (int position : sentPositions) {
			sent.add(queries.get(position));
		}
		((ObjectNode) hybridNode).set("queries", sent);
		SearchDslValidator.validateHybridLeafShapes(hybridNode);
		SearchFieldRewriter.rewriteRequestFields(hybridNode, ctx, SearchFieldRewriter.Surface.QUERY);
		HybridQuery hybrid = fromJsonpTree(hybridNode, HybridQuery._DESERIALIZER);
		SearchDslValidator.validateHybrid(hybrid);

		List<Query> hybridFilters = new ArrayList<>();
		if (hybrid.filter() != null) {
			hybridFilters.add(hybrid.filter());
		}
		hybridFilters.addAll(accessFilters);
		Query combinedFilter = andFilters(hybridFilters);
		List<Query> clauses = new ArrayList<>(hybrid.queries().size());
		for (Query clause : hybrid.queries()) {
			clauses.add(applyClauseFilters(clause, accessFilters, semanticModelId));
		}
		return Query.of(q -> q.hybrid(h -> {
			h.queries(clauses);
			if (combinedFilter != null) {
				h.filter(combinedFilter);
			}
			h.paginationDepth(SearchDslValidator.HYBRID_PAGINATION_DEPTH);
			return h;
		}));
	}

	/**
	 * Push the access filters into one hybrid clause, and place the platform's embedding model on
	 * a {@code neural} clause.
	 *
	 * <p>A {@code neural} clause carries them in {@code neural.filter}, where OpenSearch pre-filters
	 * the vector search so the candidates are drawn from the readable subset; the caller's own filter
	 * is AND-ed with them in a single conjunction, since {@code NeuralQuery.Builder.filter} is a plain
	 * setter. Any other clause is wrapped in a {@code bool} whose {@code must} holds the clause, which
	 * leaves its score untouched.</p>
	 *
	 * <p>{@code model_id} is a platform value rather than caller input, so a stale id cannot score
	 * against an incompatible embedding space.</p>
	 */
	private static Query applyClauseFilters(Query clause, List<Query> accessFilters, String semanticModelId) {
		if (clause.isNeural()) {
			List<Query> neuralFilters = new ArrayList<>();
			if (clause.neural().filter() != null) {
				neuralFilters.add(clause.neural().filter());
			}
			neuralFilters.addAll(accessFilters);
			return Query.of(q -> q.neural(clause.neural().toBuilder()
					.modelId(semanticModelId)
					.filter(andFilters(neuralFilters))
					.build()));
		}
		if (accessFilters.isEmpty()) {
			return clause;
		}
		return Query.of(q -> q.bool(b -> b.must(clause).filter(accessFilters)));
	}

	/**
	 * Combine filter clauses into one {@code bool} carrying them all in {@code filter}, or
	 * {@code null} when there are none.
	 */
	private static Query andFilters(List<Query> filters) {
		if (filters.isEmpty()) {
			return null;
		}
		return Query.of(q -> q.bool(b -> b.filter(filters)));
	}

	static Query parseRequiredQuery(JsonNode body,
			SearchFieldRewriter.RoutingContext ctx, boolean autocomplete) {
		JsonNode node = body.get("query");
		if (node == null || node.isNull()) {
			throw new IllegalArgumentException(
					"body.query is required (use {\"match_all\":{}} to match all documents)");
		}
		return parseQuery(node, ctx, autocomplete);
	}

	private static Query parseQuery(JsonNode node, SearchFieldRewriter.RoutingContext ctx,
			boolean autocomplete) {
		// The typed SearchQuery POJO already constrains the clause structure (any key outside the
		// schema is rejected with HTTP 400 at the request boundary). Validate the opaque leaf shapes,
		// then rewrite field references, deserialize to the typed OpenSearch query, and run the
		// resource caps.
		SearchDslValidator.validateQueryLeafShapes(node);
		SearchFieldRewriter.rewriteRequestFields(node, ctx, SearchFieldRewriter.Surface.QUERY);
		Query query = fromJsonpTree(node, Query._DESERIALIZER);
		SearchDslValidator.validateQuery(query, autocomplete);
		return query;
	}

	static Map<String, Aggregation> parseAggregations(JsonNode body,
			SearchFieldRewriter.RoutingContext ctx) {
		JsonNode node = body.get("aggregations");
		if (node == null || node.isNull()) {
			return Collections.emptyMap();
		}
		SearchDslValidator.validateAggregationLeafShapes(node);
		SearchFieldRewriter.rewriteRequestFields(node, ctx, SearchFieldRewriter.Surface.AGGREGATIONS);
		Map<String, Aggregation> result = new LinkedHashMap<>();
		Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
		while (fields.hasNext()) {
			Map.Entry<String, JsonNode> entry = fields.next();
			result.put(entry.getKey(), fromJsonpTree(entry.getValue(), Aggregation._DESERIALIZER));
		}
		SearchDslValidator.validateAggregations(result);
		return result;
	}

	private static Highlight parseHighlight(JsonNode node, SearchFieldRewriter.RoutingContext ctx) {
		// Any embedded highlight_query (top-level or per-field) is a full Query subtree whose
		// opaque leaf slots must pass the same shape gate as the main query.
		SearchDslValidator.validateHighlightQueryLeafShapes(node);
		SearchFieldRewriter.rewriteRequestFields(node, ctx, SearchFieldRewriter.Surface.HIGHLIGHT);
		Highlight highlight = fromJsonpTree(node, Highlight._DESERIALIZER);
		SearchDslValidator.validateHighlight(highlight);
		return highlight;
	}

	private static FieldCollapse parseCollapse(JsonNode node, SearchFieldRewriter.RoutingContext ctx) {
		// FieldCollapse is fully typed (field + max_concurrent_group_searches); no opaque slot
		// remains, so no forbidden-key scan is needed.
		SearchFieldRewriter.rewriteRequestFields(node, ctx, SearchFieldRewriter.Surface.COLLAPSE);
		FieldCollapse collapse = fromJsonpTree(node, FieldCollapse._DESERIALIZER);
		SearchDslValidator.validateFieldCollapse(collapse);
		return collapse;
	}

	static Rescore parseRescore(JsonNode node, SearchFieldRewriter.RoutingContext ctx) {
		JsonNode rescoreQueryNode = node.path("query").path("rescore_query");
		if (!rescoreQueryNode.isMissingNode()) {
			SearchDslValidator.validateQueryLeafShapes(rescoreQueryNode);
			SearchFieldRewriter.rewriteRequestFields(rescoreQueryNode, ctx,
					SearchFieldRewriter.Surface.QUERY);
		}
		Rescore rescore = fromJsonpTree(node, Rescore._DESERIALIZER);
		SearchDslValidator.validateRescore(rescore);
		return rescore;
	}

	/**
	 * Parse the {@code sort} array in native OpenSearch sort shape (a bare column-name string,
	 * {@code {column: "asc"|"desc"}}, or {@code {column: {order, mode, missing}}}), field-rewrite
	 * the column references, and deserialize each element into a typed {@link SortOptions}. The
	 * resulting kinds are then checked against {@link SearchDslValidator#ALLOWED_SORT_KINDS}, so
	 * only {@code field} and {@code _score} sorts survive &mdash; the {@code script},
	 * {@code _geo_distance}, and {@code _doc} sort kinds are rejected by not being on the allowlist.
	 *
	 * <p>The sort ends in {@code _row_id asc} (see {@link #withRowIdTiebreak}) unless the body
	 * carries a {@code rescore}, which OpenSearch will not combine with a non-relevance sort, or is a
	 * relevance-ranked {@code hybrid} query, which OpenSearch will not sort by {@code _score} and
	 * another key at once.</p>
	 */
	static List<SortOptions> parseSort(JsonNode body, SearchFieldRewriter.RoutingContext ctx) {
		// "Cannot use [sort] option in conjunction with [rescore]" for a rescoring query, and "_score
		// sort criteria cannot be applied with any other criteria" for a hybrid one: such a caller
		// cannot be given the tiebreak and forfeits deterministic search_after paging.
		boolean relevanceRankedHybrid = nodeOrNull(body, "hybrid") != null && SearchDslValidator.isRelevanceRanked(body);
		boolean tiebreak = nodeOrNull(body, "rescore") == null && !relevanceRankedHybrid;
		JsonNode node = body.get("sort");
		if (node == null || node.isNull() || (node.isArray() && node.isEmpty())) {
			// Default: relevance descending. Mirrors the OpenSearch default sort when
			// callers omit `sort` entirely.
			List<SortOptions> relevance = Collections.singletonList(SortOptions.of(so ->
					so.field(FieldSort.of(fs -> fs.field("_score").order(SortOrder.Desc)))));
			return tiebreak ? withRowIdTiebreak(relevance) : relevance;
		}
		// A bare top-level string ("title") is the column-name shorthand — JsonNode mutation can't
		// replace it in place, so wrap it in the array shorthand before rewriting.
		JsonNode walkable;
		if (node.isTextual()) {
			com.fasterxml.jackson.databind.node.ArrayNode wrapped = arrayNode();
			wrapped.add(node.asText());
			walkable = wrapped;
		} else {
			walkable = node;
		}
		SearchFieldRewriter.rewriteSortFields(walkable, ctx);
		List<SortOptions> sort = new ArrayList<>();
		if (walkable.isArray()) {
			for (JsonNode element : walkable) {
				sort.add(fromJsonpTree(element, SortOptions._DESERIALIZER));
			}
		} else {
			sort.add(fromJsonpTree(walkable, SortOptions._DESERIALIZER));
		}
		SearchDslValidator.validateSort(sort);
		return tiebreak ? withRowIdTiebreak(sort) : sort;
	}

	/**
	 * Append {@code _row_id asc} so the sort is a total order, unless the caller already sorts on
	 * {@code _row_id}. Without a unique final key, documents tying on every sort key have no
	 * defined order across shards and a {@code search_after} cursor skips the rest of the tie.
	 */
	private static List<SortOptions> withRowIdTiebreak(List<SortOptions> sort) {
		boolean alreadyPresent = sort.stream().anyMatch(so -> so.isField()
				&& OpenSearchManagerImpl.SYSTEM_FIELD_ROW_ID.equals(so.field().field()));
		if (alreadyPresent) {
			return sort;
		}
		List<SortOptions> withTiebreak = new ArrayList<>(sort);
		withTiebreak.add(SortOptions.of(so -> so.field(FieldSort.of(fs -> fs
				.field(OpenSearchManagerImpl.SYSTEM_FIELD_ROW_ID).order(SortOrder.Asc)))));
		return withTiebreak;
	}

	/**
	 * Parse the {@code _source} filter. The typed {@code SourceFilter} schema
	 * ({@code {includes, excludes}}) is already the native OpenSearch {@code SourceFilter} shape,
	 * so it is field-rewritten and deserialized directly; the boolean and bare-array shorthands
	 * are rejected by the schema before this runs.
	 */
	private static SourceConfig parseSource(JsonNode node, SearchFieldRewriter.RoutingContext ctx) {
		SearchFieldRewriter.rewriteSourceFields(node, ctx);
		return fromJsonpTree(node, SourceConfig._DESERIALIZER);
	}

	static List<FieldValue> parseSearchAfter(JsonNode body) {
		// search_after is typed List<Object> on the SearchQuery POJO, so by the time the body
		// reaches here it is an array (or absent); each element deserializes to a FieldValue below.
		JsonNode node = body.get("search_after");
		if (node == null || node.isNull()) {
			return Collections.emptyList();
		}
		List<FieldValue> values = new ArrayList<>(node.size());
		for (JsonNode element : node) {
			values.add(fromJsonpTree(element, FieldValue._DESERIALIZER));
		}
		return values;
	}

	// ---------- OpenSearch search-response serializers ----------

	/**
	 * Serialize the typed aggregation response (the {@code aggregations} block from
	 * {@code SearchResponse}) into an opaque JSON tree with column ids rewritten back to
	 * column names. Each top-level entry is a caller-chosen aggregation name (left
	 * unchanged); embedded {@code "field"} references are rewritten via {@code idToName}.
	 *
	 * <p>The return value is the deserialized Java tree (a {@link Map} for an object) the
	 * schema-to-pojo wire layer accepts &mdash; same shape as the JSON returned to clients,
	 * not a stringified copy.</p>
	 */
	public static Object serializeAggregations(
			Map<String, ? extends JsonpSerializable> aggregations,
			java.util.function.Function<String, String> idToName) {
		ObjectNode root = objectNode();
		for (Map.Entry<String, ? extends JsonpSerializable> entry : aggregations.entrySet()) {
			root.set(entry.getKey(), toJsonpTree(entry.getValue()));
		}
		SearchFieldRewriter.rewriteAggregationResults(root, idToName);
		return new JSONObject(root.toString());
	}

	/**
	 * Convert a list of typed sort values from the last hit of a results page into the
	 * opaque cursor list emitted as {@code SearchQueryResults.nextSearchAfter}. Each
	 * {@link JsonpSerializable} sort value is serialized via {@link #toJsonpTree} and then
	 * round-tripped to a generic Java tree (so the cursor list contains plain
	 * {@link Number} / {@link String} / etc. that the schema-to-pojo wire serializer accepts).
	 */
	public static java.util.List<Object> toSearchAfterCursor(
			java.util.List<? extends JsonpSerializable> sortValues) {
		java.util.List<Object> cursor = new java.util.ArrayList<>(sortValues.size());
		for (JsonpSerializable value : sortValues) {
			JsonNode tree = toJsonpTree(value);
			cursor.add(MAPPER.convertValue(tree, Object.class));
		}
		return cursor;
	}

	// ---------- reference detection ----------

	/**
	 * If {@code value} is the single-field reference shape {@code {"$ref": "..."}},
	 * return the qualified-name string; otherwise return {@code null}.
	 *
	 * <p>Accepts the post-DAO {@link JSONObject} shape, the {@link Map} shape (test
	 * fixtures / programmatic callers), and the wire-deserialized
	 * {@link JSONObjectAdapter} shape (controllers receive opaque-Object fields as a
	 * {@link JSONObjectAdapter} after JSON binding). Anything else &mdash; including a
	 * bare {@link String} scalar &mdash; returns {@code null}.</p>
	 */
	public static String readRef(Object value) {
		if (value instanceof JSONObject) {
			return readRefFromJsonObject((JSONObject) value);
		}
		if (value instanceof Map) {
			return readRefFromMap((Map<?, ?>) value);
		}
		if (value instanceof JSONObjectAdapter) {
			return readRefFromJsonObject(new JSONObject(((JSONObjectAdapter) value).toJSONString()));
		}
		return null;
	}

	static String readRefFromMap(Map<?, ?> map) {
		if (map.size() != 1) {
			return null;
		}
		Object ref = map.get(REF_KEY);
		return ref instanceof String ? (String) ref : null;
	}

	static String readRefFromJsonObject(JSONObject obj) {
		if (obj.length() != 1) {
			return null;
		}
		Object ref = obj.opt(REF_KEY);
		return ref instanceof String ? (String) ref : null;
	}

	/**
	 * If {@code node} is an object whose only field is {@link #REF_KEY} with a textual
	 * value, return that value; otherwise {@code null}. The Jackson-tree counterpart of
	 * {@link #readRef(Object)}.
	 */
	public static String readRef(JsonNode node) {
		if (node == null || !node.isObject() || node.size() != 1) {
			return null;
		}
		JsonNode ref = node.get(REF_KEY);
		return (ref != null && ref.isTextual()) ? ref.asText() : null;
	}

	/**
	 * Collect every qualified-name appearing as a {@link #REF_KEY} value anywhere in
	 * {@code root}. Returns a deduplicated, ordered set in walk order.
	 */
	public static Set<String> collectRefs(JsonNode root) {
		if (root == null) {
			return Collections.emptySet();
		}
		Set<String> refs = new LinkedHashSet<>();
		for (JsonNode v : root.findValues(REF_KEY)) {
			if (v.isTextual()) {
				refs.add(v.asText());
			}
		}
		return refs;
	}

	// ---------- inline materialization ----------

	/**
	 * Convert an inline-literal value (a value that's <i>not</i> a {@code $ref}) into
	 * the typed POJO of {@code clazz}. Callers should branch on
	 * {@link #readRef(Object)} first; {@code toInline} is for the inline branch.
	 *
	 * <p>Accepts {@link Map} / {@link java.util.Collection} / scalar trees as well as
	 * {@link JSONObject} / {@link JSONArray}; the latter are normalized to their
	 * JSON-string form before deserialization.</p>
	 *
	 * @throws IllegalArgumentException when the value cannot be deserialized as
	 *         {@code clazz}.
	 */
	public static <T> T toInline(Object value, Class<T> clazz) {
		if (value == null) {
			return null;
		}
		try {
			if (value instanceof JSONObject || value instanceof JSONArray) {
				return MAPPER.readValue(value.toString(), clazz);
			}
			if (value instanceof JSONObjectAdapter) {
				return MAPPER.readValue(((JSONObjectAdapter) value).toJSONString(), clazz);
			}
			return MAPPER.convertValue(value, clazz);
		} catch (IllegalArgumentException | JsonProcessingException e) {
			throw new IllegalArgumentException(
					"Invalid inline " + clazz.getSimpleName() + ": " + e.getMessage(), e);
		}
	}

	/**
	 * Parse an inline analyzer-slot literal &mdash; the bare OpenSearch
	 * {@code settings.analysis} block carried directly on
	 * {@code SearchConfiguration.defaultAnalyzer} or
	 * {@code ColumnAnalyzerOverrideEntry.analyzer} &mdash; into a typed
	 * {@link IndexSettingsAnalysis}, splicing any {@code $ref} entries inside the
	 * analyzer's filter map via {@code resolver}.
	 *
	 * <p>At create / update time callers pass {@code resolver = q -> null}: any {@code $ref}
	 * surfaces as {@link IllegalArgumentException}, since refs inside an inline-literal slot
	 * are not a supported feature. At index-build time callers pass the SynonymSet resolver
	 * so a TextAnalyzer that uses synonyms via {@code $ref} can still be inlined.</p>
	 *
	 * @return the typed analyzer settings; {@code null} when {@code value} is {@code null}.
	 * @throws IllegalArgumentException when the inline literal is malformed JSON, fails the
	 *         OpenSearch typed deserializer, or contains an unresolved {@code $ref}.
	 */
	public static IndexSettingsAnalysis toInlineAnalyzerSettings(Object value,
			Function<String, JsonNode> resolver) {
		if (value == null) {
			return null;
		}
		return resolveAnalyzerSettings(parse(value), resolver);
	}

	// ---------- analyzer-typed splice + deserialize ----------

	/**
	 * Splice every {@code {"$ref": "<qname>"}} entry inside the {@code root.}{@value
	 * #FILTER_KEY} map with the JSON returned by {@code resolver.apply(qname)}, then
	 * deserialize the resulting tree into the OpenSearch typed
	 * {@link IndexSettingsAnalysis}. Mutates {@code root} in place during the splice.
	 *
	 * <p>Per the schema contract, {@code $ref} is only permitted as a direct value of an
	 * entry in the top-level {@code filter} map of an analyzer's settings &mdash; the
	 * splice is therefore a single non-recursive pass over that map.</p>
	 *
	 * <p>Downstream callers use typed accessors ({@link IndexSettingsAnalysis#analyzer()},
	 * {@link IndexSettingsAnalysis#filter()}, etc.) instead of re-walking the JSON
	 * tree.</p>
	 *
	 * @param root     The settings tree; mutated in place.
	 * @param resolver Returns the JSON node to splice in for a given qname, or
	 *                 {@code null} if the target does not exist.
	 * @return         The typed analyzer settings; {@code null} when {@code root} is
	 *                 {@code null}.
	 * @throws IllegalArgumentException when a {@code $ref} target does not resolve, or
	 *         the spliced tree fails the typed deserializer (malformed component).
	 */
	public static IndexSettingsAnalysis resolveAnalyzerSettings(JsonNode root,
			Function<String, JsonNode> resolver) {
		if (root == null) {
			return null;
		}
		JsonNode filterMap = root.get(FILTER_KEY);
		if (filterMap != null && filterMap.isObject()) {
			ObjectNode filterObj = (ObjectNode) filterMap;
			List<String> keys = new ArrayList<>();
			Iterator<String> names = filterObj.fieldNames();
			while (names.hasNext()) {
				keys.add(names.next());
			}
			for (String key : keys) {
				String ref = readRef(filterObj.get(key));
				if (ref == null) {
					continue;
				}
				JsonNode target = resolver.apply(ref);
				if (target == null) {
					throw new IllegalArgumentException(
							"Unresolved $ref: '" + ref + "' at /" + FILTER_KEY + "/" + key);
				}
				filterObj.set(key, target);
			}
		}
		try (JsonParser parser = JSONP_MAPPER.jsonProvider()
				.createParser(new StringReader(root.toString()))) {
			return IndexSettingsAnalysis._DESERIALIZER.deserialize(parser, JSONP_MAPPER);
		} catch (RuntimeException e) {
			throw new IllegalArgumentException(
					"Invalid analyzer settings: " + e.getMessage(), e);
		}
	}
}
