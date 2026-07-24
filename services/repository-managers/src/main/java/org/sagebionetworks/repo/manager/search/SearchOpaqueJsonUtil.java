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
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.search.FieldCollapse;
import org.opensearch.client.opensearch.core.search.Highlight;
import org.opensearch.client.opensearch.core.search.Rescore;
import org.opensearch.client.opensearch.core.search.SourceConfig;
import org.opensearch.client.opensearch.indices.IndexSettingsAnalysis;
import org.sagebionetworks.repo.model.search.SearchQueryPart;
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
	 * @param opaque       the caller's body, in any of the shapes {@link #parse(Object)} accepts
	 * @param ctx          the column-name &rarr; column-id routing context for the target index
	 * @param req          the target {@link SearchRequest.Builder} (mutated in place)
	 * @param options      the response-parts the caller asked for
	 * @param defaultSize  default {@code size} when the body omits it
	 * @param maxSize      upper bound on {@code size}; larger values clamp
	 * @param accessFilters server-side access-control filters to AND with the query; must not be null
	 * @return             the effective {@code from} written to {@code req} (echoed back to
	 *                     the caller as {@code SearchQueryResults.offset})
	 */
	static int applyBodyToRequest(Object opaque, SearchFieldRewriter.RoutingContext ctx,
			SearchRequest.Builder req, Set<SearchQueryPart> options,
			int defaultSize, int maxSize, List<Query> accessFilters) {
		return applyBodyToRequest(opaque, ctx, req, options, defaultSize, maxSize, false, accessFilters);
	}

	/**
	 * Autocomplete variant of {@link #applyBodyToRequest}: narrows the top-level allowlist
	 * to {@code query} and {@code _source}, and enforces the autocomplete top-level query
	 * allowlist on the inner clause.
	 */
	static int applyAutocompleteBodyToRequest(Object opaque,
			SearchFieldRewriter.RoutingContext ctx, SearchRequest.Builder req,
			Set<SearchQueryPart> options, int defaultSize, List<Query> accessFilters) {
		return applyBodyToRequest(opaque, ctx, req, options, defaultSize, defaultSize, true, accessFilters);
	}

	private static int applyBodyToRequest(Object opaque, SearchFieldRewriter.RoutingContext ctx,
			SearchRequest.Builder req, Set<SearchQueryPart> options,
			int defaultSize, int maxSize, boolean autocomplete, List<Query> accessFilters) {
		// The body is the generated SearchQuery / SearchAutocompleteBody POJO, so any key outside the
		// schema was already rejected with HTTP 400 at the request boundary, and each surface with an
		// opaque slot is forbidden-key scanned individually as it is parsed below.
		JsonNode body = parse(opaque);

		Query query = parseRequiredQuery(body, ctx, autocomplete);
		// Wrap the caller's allowlist-validated query in a server-controlled bool: the caller's
		// query goes in must, and every server-side access-control filter goes in filter (AND
		// semantics) so a document must satisfy the query and every benefactor filter.
		req.query(q -> q.bool(b -> {
			b.must(query);
			if (accessFilters != null && !accessFilters.isEmpty()) {
				b.filter(accessFilters);
			}
			return b;
		}));

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
		return from;
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
	 */
	static List<SortOptions> parseSort(JsonNode body, SearchFieldRewriter.RoutingContext ctx) {
		JsonNode node = body.get("sort");
		if (node == null || node.isNull() || (node.isArray() && node.isEmpty())) {
			// Default: relevance descending. Mirrors the OpenSearch default sort when
			// callers omit `sort` entirely.
			return Collections.singletonList(SortOptions.of(so ->
					so.field(FieldSort.of(fs -> fs.field("_score").order(SortOrder.Desc)))));
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
		return sort;
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
