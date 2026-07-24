package org.sagebionetworks.repo.manager.search;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.Buckets;
import org.opensearch.client.opensearch._types.aggregations.CardinalityAggregation;
import org.opensearch.client.opensearch._types.aggregations.DateHistogramAggregation;
import org.opensearch.client.opensearch._types.aggregations.DateRangeAggregation;
import org.opensearch.client.opensearch._types.aggregations.FiltersAggregation;
import org.opensearch.client.opensearch._types.aggregations.HistogramAggregation;
import org.opensearch.client.opensearch._types.aggregations.RangeAggregation;
import org.opensearch.client.opensearch._types.aggregations.TermsAggregation;
import org.opensearch.client.opensearch._types.aggregations.TermsExclude;
import org.opensearch.client.opensearch._types.aggregations.TermsInclude;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.BoostingQuery;
import org.opensearch.client.opensearch._types.query_dsl.ConstantScoreQuery;
import org.opensearch.client.opensearch._types.query_dsl.DisMaxQuery;
import org.opensearch.client.opensearch._types.query_dsl.FuzzyQuery;
import org.opensearch.client.opensearch._types.query_dsl.MatchBoolPrefixQuery;
import org.opensearch.client.opensearch._types.query_dsl.MatchPhrasePrefixQuery;
import org.opensearch.client.opensearch._types.query_dsl.MultiMatchQuery;
import org.opensearch.client.opensearch._types.query_dsl.PrefixQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.SimpleQueryStringQuery;
import org.opensearch.client.opensearch._types.query_dsl.TermsQuery;
import org.opensearch.client.opensearch._types.query_dsl.TermsQueryField;
import org.opensearch.client.opensearch._types.query_dsl.WildcardQuery;
import org.opensearch.client.opensearch.core.search.FieldCollapse;
import org.opensearch.client.opensearch.core.search.Highlight;
import org.opensearch.client.opensearch.core.search.HighlightField;
import org.opensearch.client.opensearch.core.search.HighlighterType;
import org.opensearch.client.opensearch.core.search.Rescore;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * The typed resource-cap validator. It runs <i>after</i> the request &mdash; already narrowed to
 * the schema-declared keys by the generated {@code SearchQuery} POJO at the HTTP boundary and
 * checked by the opaque leaf-shape gate ({@link #validateQueryLeafShapes} /
 * {@link #validateAggregationLeafShapes}) &mdash; has been deserialized into the typed OpenSearch
 * objects ({@link Query}, {@link Aggregation}, {@link Highlight}, {@link FieldCollapse},
 * {@link Rescore}). Each {@code validate*} method walks its typed object and enforces the numeric
 * caps that need typed accessors and prevent a request from expanding into an unbounded shape inside
 * AOSS: query depth and clause count, aggregation depth and count, value-array length,
 * prefix-expansion, histogram bucket bound, cardinality precision, and the leading-wildcard rejection.
 *
 * <p>This is the defense-in-depth layer behind the typed POJO's structural allowlist. Each
 * {@code walk*} switch has a throwing {@code default}, so a query / aggregation kind that the
 * OpenSearch client adds but nobody wires into the allowlist is rejected here even if it somehow
 * survived deserialization.</p>
 *
 * <p>This class also owns the {@code from} / {@code size} resolution on {@code SearchQuery.body}
 * ({@link #resolveFrom} / {@link #resolveSize}) &mdash; the numeric-bound rules the {@code integer}
 * schema type does not express. These run on the {@link JsonNode} tree, so they live here with the
 * rest of the request validation. The top-level key allowlist itself is enforced by
 * the generated {@code SearchQuery} / {@code SearchAutocompleteBody} POJOs, which reject any key
 * outside the schema with HTTP 400 at the request boundary.</p>
 *
 * <p><b>Resource exhaustion / AOSS denial-of-wallet caps.</b> Depth and total-count caps bound
 * query shape; an inline {@code terms} value array is capped at {@link #MAX_VALUES_PER_CLAUSE};
 * bucket aggregation {@code size} / {@code shard_size} are capped at {@link #MAX_AGG_SIZE};
 * {@code prefix} / {@code wildcard} values that begin with {@code *} or {@code ?} are rejected
 * because a leading wildcard forces a full inverted-index scan.</p>
 *
 * <p>Throws {@link IllegalArgumentException} (HTTP 400) on any violation.</p>
 */
final class SearchDslValidator {

	// --------------------------------------------------------------
	// Limits — package-private so tests can reference symbolically.
	// --------------------------------------------------------------

	static final int QUERY_MAX_DEPTH = 20;
	static final int QUERY_MAX_CLAUSES = 256;
	/**
	 * Maximum number of values that may appear in an array carried inside a single clause:
	 * applies to {@code terms} value arrays, {@code multi_match.fields},
	 * and {@code terms} aggregation {@code include}/{@code exclude} arrays. Any of these
	 * would otherwise expand into many internal clauses or buckets and bypass
	 * {@link #QUERY_MAX_CLAUSES} / {@link #MAX_AGG_SIZE}.
	 */
	static final int MAX_VALUES_PER_CLAUSE = 1024;

	static final int AGG_MAX_DEPTH = 10;
	static final int AGG_MAX_COUNT = 100;
	/** Maximum value for {@code size} / {@code shard_size} on bucket aggregations. */
	static final int MAX_AGG_SIZE = 1000;
	/** Maximum {@code precision_threshold} on a {@code cardinality} aggregation. */
	static final int MAX_PRECISION_THRESHOLD = 10000;

	/** Maximum entries in a {@code highlight.fields} map. Each entry is one highlighted column. */
	static final int MAX_HIGHLIGHT_FIELDS = 50;
	/** Maximum {@code number_of_fragments} on a highlight field (top-level or per-field). AOSS default is 5. */
	static final int MAX_HIGHLIGHT_FRAGMENTS = 100;
	/** Maximum {@code fragment_size} on a highlight field, in characters. AOSS default is 100. */
	static final int MAX_HIGHLIGHT_FRAGMENT_SIZE = 1000;
	/**
	 * Built-in highlighter type name we reject upfront. The {@code semantic} highlighter
	 * requires a deployed ML model the Synapse stack does not provision; AOSS would error
	 * server-side anyway, so reject with a clear 400 before round-tripping.
	 */
	static final String FORBIDDEN_HIGHLIGHTER_TYPE_SEMANTIC = "semantic";

	/** Maximum value for {@code max_concurrent_group_searches} on a {@code collapse} block. */
	static final int MAX_COLLAPSE_CONCURRENT_GROUP_SEARCHES = 10;

	/** Maximum value for {@code window_size} on a {@code rescore} stage. */
	static final int MAX_RESCORE_WINDOW_SIZE = 1000;

	/**
	 * Maximum value for {@code max_expansions} on the prefix-expansion clauses
	 * ({@code match_phrase_prefix}, {@code match_bool_prefix}, {@code fuzzy}, and
	 * {@code multi_match}). Default is 50 in OpenSearch with no upper bound; an unbounded
	 * value rewrites into many term queries.
	 */
	static final int MAX_PREFIX_EXPANSIONS = 50;

	// --------------------------------------------------------------
	// Kind allowlists.
	// --------------------------------------------------------------

	/**
	 * Query kinds a caller may use. Compound kinds recurse into their nested query slots;
	 * everything not listed here is rejected. Intentionally excludes script-bearing
	 * ({@code Script}, {@code ScriptScore}, {@code FunctionScore}), cross-index
	 * ({@code MoreLikeThis}, {@code GeoShape}, {@code HasChild}, {@code HasParent},
	 * {@code Percolate}), and validation-bypassing ({@code Wrapper}) kinds.
	 */
	static final Set<Query.Kind> ALLOWED_QUERY_KINDS = EnumSet.of(
			// leaf
			Query.Kind.Match, Query.Kind.MultiMatch, Query.Kind.MatchPhrase,
			Query.Kind.MatchPhrasePrefix, Query.Kind.MatchBoolPrefix,
			Query.Kind.Term, Query.Kind.Terms, Query.Kind.Range, Query.Kind.Exists,
			Query.Kind.Prefix, Query.Kind.Wildcard, Query.Kind.Fuzzy,
			Query.Kind.SimpleQueryString, Query.Kind.MatchAll,
			// compound
			Query.Kind.Bool, Query.Kind.DisMax, Query.Kind.ConstantScore, Query.Kind.Boosting);

	/**
	 * Aggregation kinds a caller may use. Bucket aggregations may carry nested
	 * sub-aggregations via {@code aggregations()}. The {@code Filter}/{@code Filters} aggregations
	 * wrap a query body, which is validated through the same path as the top-level query
	 * ({@link #validateQuery}) in {@link #walkAggregation}.
	 * <p>
	 * Intentionally excludes {@code ScriptedMetric} and pipeline aggregations (e.g.
	 * {@code BucketScript}, {@code BucketSelector}). Also intentionally excludes {@code Global}
	 * (and any other aggregation that resets or escapes the query scope): a {@code Global}
	 * aggregation deliberately runs outside the current search context, so it would see documents
	 * the top-level query &mdash; and the future server-side row-level ACL filter layered into that
	 * query's {@code bool.must} &mdash; excludes. {@code Filter}/{@code Filters} are safe because
	 * they run <i>inside</i> the search context and so remain scoped by the top-level query.
	 */
	static final Set<Aggregation.Kind> ALLOWED_AGGREGATION_KINDS = EnumSet.of(
			// bucket
			Aggregation.Kind.Terms, Aggregation.Kind.Histogram, Aggregation.Kind.DateHistogram,
			Aggregation.Kind.Range, Aggregation.Kind.DateRange, Aggregation.Kind.Missing,
			Aggregation.Kind.Filter, Aggregation.Kind.Filters,
			// metric
			Aggregation.Kind.Min, Aggregation.Kind.Max, Aggregation.Kind.Avg,
			Aggregation.Kind.Sum, Aggregation.Kind.Stats, Aggregation.Kind.ExtendedStats,
			Aggregation.Kind.ValueCount, Aggregation.Kind.Cardinality);

	/**
	 * Top-level autocomplete kinds. Narrower than {@link #ALLOWED_QUERY_KINDS} so the
	 * type-ahead surface stays minimal and predictable.
	 */
	static final Set<Query.Kind> ALLOWED_AUTOCOMPLETE_TOP_LEVEL = EnumSet.of(
			Query.Kind.Prefix, Query.Kind.MatchPhrasePrefix, Query.Kind.MatchBoolPrefix);

	/**
	 * Sort kinds a caller may use. Only ordering by a column value ({@code Field}) or by relevance
	 * ({@code Score}) is allowed; everything else is rejected by not being on this allowlist.
	 * Intentionally excludes {@code Script} (runs Painless), {@code GeoDistance} (Synapse search
	 * indexes have no geo fields), and {@code Doc} (internal Lucene doc-id order, not meaningful to
	 * a caller).
	 */
	static final Set<SortOptions.Kind> ALLOWED_SORT_KINDS = EnumSet.of(
			SortOptions.Kind.Field, SortOptions.Kind.Score);

	private SearchDslValidator() {
	}

	// --------------------------------------------------------------
	// Pagination resolution. The body has already round-tripped through the typed SearchQuery
	// POJO, so `from` / `size` arrive as integral numbers (or absent); these only enforce the
	// numeric bounds the integer schema type does not (negative, int-range, size clamp).
	// --------------------------------------------------------------

	/**
	 * Resolve the effective {@code from} offset on a body. Defaults to {@code 0} when absent;
	 * rejects negatives and anything above {@code Integer.MAX_VALUE} (the {@code SearchQuery.from}
	 * schema type is {@code integer}, which the generated POJO carries as a {@code Long} and so
	 * permits values outside the {@code int} range this needs to narrow into).
	 */
	static int resolveFrom(JsonNode body) {
		JsonNode node = body.get("from");
		if (node == null || node.isNull()) {
			return 0;
		}
		long value = node.asLong();
		if (value < 0L || value > Integer.MAX_VALUE) {
			throw new IllegalArgumentException(
					"body.from must be between 0 and " + Integer.MAX_VALUE);
		}
		return (int) value;
	}

	/**
	 * Resolve the effective {@code size} on a body. Defaults to {@code defaultSize} when absent;
	 * rejects negative values; clamps anything above {@code maxSize} down to {@code maxSize}.
	 */
	static int resolveSize(JsonNode body, int defaultSize, int maxSize) {
		JsonNode node = body.get("size");
		if (node == null || node.isNull()) {
			return defaultSize;
		}
		long value = node.asLong();
		if (value < 0L) {
			throw new IllegalArgumentException("body.size must be non-negative");
		}
		return (int) Math.min(value, maxSize);
	}

	// --------------------------------------------------------------
	// Opaque leaf-value shape checks (raw JsonNode).
	//
	// A number of DSL leaf slots are schema-typed as an opaque "object" because their
	// value is polymorphic at the JSON level (a number, a string, a date string, a
	// boolean, or an array of those). The typed OpenSearch deserializer constrains some
	// of them but lets others through as arbitrary JSON (e.g. a `range` bound is carried
	// as JsonData, which accepts a nested object or array). These checks run on the clean
	// JsonNode before deserialization and reject anything that isn't the expected shape:
	// a single scalar where one value is expected, an array of scalars where a value list
	// is expected. They do NOT check the value against the target column's type — number,
	// string, and date all collapse to "scalar" here.
	// --------------------------------------------------------------

	/**
	 * Validate the opaque leaf-value shapes of a query-DSL subtree (one {@link Query}
	 * clause, as the caller wrote it). Recurses through the compound clauses and checks the
	 * opaque scalar / scalar-array slots on each leaf clause. Slots that are genuinely
	 * free-form (none in the query DSL) are left alone; missing or null slots are ignored.
	 */
	static void validateQueryLeafShapes(JsonNode clause) {
		if (clause == null || !clause.isObject()) {
			return;
		}
		// Field-keyed leaf clauses: map of column name to its per-field options object. The
		// opaque option keys inside each value must be scalars.
		validateFieldKeyedScalarOptions(clause, "match", "query", "minimum_should_match", "fuzziness");
		validateFieldKeyedScalarOptions(clause, "match_phrase", "query");
		validateFieldKeyedScalarOptions(clause, "match_phrase_prefix", "query");
		validateFieldKeyedScalarOptions(clause, "match_bool_prefix", "query", "minimum_should_match", "fuzziness");
		validateFieldKeyedScalarOptions(clause, "term", "value");
		validateFieldKeyedScalarOptions(clause, "range", "gte", "gt", "lte", "lt");
		validateFieldKeyedScalarOptions(clause, "prefix", "value");
		validateFieldKeyedScalarOptions(clause, "wildcard", "value", "wildcard");
		validateFieldKeyedScalarOptions(clause, "fuzzy", "value", "fuzziness");

		// terms: field-keyed object whose column entry is an array of scalars; boost / _name
		// siblings are scalars.
		JsonNode terms = clause.get("terms");
		if (terms != null && terms.isObject()) {
			Iterator<Map.Entry<String, JsonNode>> entries = terms.fields();
			while (entries.hasNext()) {
				Map.Entry<String, JsonNode> entry = entries.next();
				JsonNode value = entry.getValue();
				if (value.isArray()) {
					requireScalarArray(value, "terms['" + entry.getKey() + "']");
				}
			}
		}

		// multi_match / simple_query_string carry their references in an explicit "fields"
		// array and (multi_match) an opaque "query" / "minimum_should_match" / "fuzziness".
		JsonNode multiMatch = clause.get("multi_match");
		if (multiMatch != null && multiMatch.isObject()) {
			requireScalar(multiMatch.get("query"), "multi_match.query");
			requireScalarArray(multiMatch.get("fields"), "multi_match.fields");
			requireScalar(multiMatch.get("minimum_should_match"), "multi_match.minimum_should_match");
			requireScalar(multiMatch.get("fuzziness"), "multi_match.fuzziness");
		}
		JsonNode simpleQueryString = clause.get("simple_query_string");
		if (simpleQueryString != null && simpleQueryString.isObject()) {
			requireScalarArray(simpleQueryString.get("fields"), "simple_query_string.fields");
			requireScalar(simpleQueryString.get("minimum_should_match"),
					"simple_query_string.minimum_should_match");
		}

		// Compound clauses: validate the opaque slot then recurse into nested query clauses.
		JsonNode bool = clause.get("bool");
		if (bool != null && bool.isObject()) {
			requireScalar(bool.get("minimum_should_match"), "bool.minimum_should_match");
			validateQueryLeafShapesInArray(bool.get("must"));
			validateQueryLeafShapesInArray(bool.get("should"));
			validateQueryLeafShapesInArray(bool.get("must_not"));
			validateQueryLeafShapesInArray(bool.get("filter"));
		}
		JsonNode disMax = clause.get("dis_max");
		if (disMax != null && disMax.isObject()) {
			validateQueryLeafShapesInArray(disMax.get("queries"));
		}
		JsonNode constantScore = clause.get("constant_score");
		if (constantScore != null && constantScore.isObject()) {
			validateQueryLeafShapes(constantScore.get("filter"));
		}
		JsonNode boosting = clause.get("boosting");
		if (boosting != null && boosting.isObject()) {
			validateQueryLeafShapes(boosting.get("positive"));
			validateQueryLeafShapes(boosting.get("negative"));
		}
	}

	static void validateQueryLeafShapesInArray(JsonNode array) {
		if (array == null || !array.isArray()) {
			return;
		}
		for (JsonNode element : array) {
			validateQueryLeafShapes(element);
		}
	}

	/**
	 * Run the query leaf-shape gate over every {@code highlight_query} subtree in a highlight
	 * block &mdash; the top-level one and each per-field one under {@code fields}.
	 */
	static void validateHighlightQueryLeafShapes(JsonNode highlight) {
		if (highlight == null || !highlight.isObject()) {
			return;
		}
		validateQueryLeafShapes(highlight.get("highlight_query"));
		JsonNode fields = highlight.get("fields");
		if (fields != null && fields.isObject()) {
			Iterator<Map.Entry<String, JsonNode>> entries = fields.fields();
			while (entries.hasNext()) {
				JsonNode field = entries.next().getValue();
				if (field != null && field.isObject()) {
					validateQueryLeafShapes(field.get("highlight_query"));
				}
			}
		}
	}

	/**
	 * For a field-keyed leaf clause ({@code match}, {@code term}, {@code range}, ...) whose
	 * value is a map of column name to its per-field options object, require each of the
	 * listed opaque option keys to be a scalar when present.
	 */
	static void validateFieldKeyedScalarOptions(JsonNode clause, String clauseKind,
			String... scalarOptionKeys) {
		JsonNode map = clause.get(clauseKind);
		if (map == null || !map.isObject()) {
			return;
		}
		Iterator<Map.Entry<String, JsonNode>> columns = map.fields();
		while (columns.hasNext()) {
			Map.Entry<String, JsonNode> column = columns.next();
			JsonNode options = column.getValue();
			if (!options.isObject()) {
				// Shorthand scalar form ({"match":{"col":"x"}}) is acceptable; anything else is
				// left for the typed deserializer to reject.
				continue;
			}
			for (String key : scalarOptionKeys) {
				requireScalar(options.get(key),
						clauseKind + "['" + column.getKey() + "'].'" + key + "'");
			}
		}
	}

	/**
	 * Validate the opaque leaf-value shapes of an aggregations map (aggregation name to
	 * aggregation object, as the caller wrote it). Recurses into sub-aggregations.
	 */
	static void validateAggregationLeafShapes(JsonNode aggregationsMap) {
		if (aggregationsMap == null || !aggregationsMap.isObject()) {
			return;
		}
		Iterator<Map.Entry<String, JsonNode>> entries = aggregationsMap.fields();
		while (entries.hasNext()) {
			validateSingleAggregationLeafShapes(entries.next().getValue());
		}
	}

	/**
	 * Aggregation kinds carrying the opaque {@code missing} value-substitution option (the
	 * {@code MissingValueOption} schema interface): the metric aggregations plus {@code terms}.
	 * The {@code missing} <i>aggregation kind</i> is unrelated and not in this set.
	 */
	private static final Set<String> AGG_KINDS_WITH_MISSING_OPTION = Set.of(
			"terms", "min", "max", "sum", "avg", "stats", "extended_stats",
			"value_count", "cardinality");

	static void validateSingleAggregationLeafShapes(JsonNode aggregation) {
		if (aggregation == null || !aggregation.isObject()) {
			return;
		}
		// `missing` is an opaque scalar substitution value on the metric aggregations and terms.
		for (String aggKind : AGG_KINDS_WITH_MISSING_OPTION) {
			JsonNode body = aggregation.get(aggKind);
			if (body != null && body.isObject()) {
				requireScalar(body.get("missing"), aggKind + " aggregation 'missing'");
			}
		}
		JsonNode terms = aggregation.get("terms");
		if (terms != null && terms.isObject()) {
			// include / exclude are a regex string or an array of exact values. `order` is the
			// typed {metric: "asc"|"desc"} sort spec (a SortOrder enum value), not arbitrary JSON,
			// so the deserializer constrains it and it needs no shape check here.
			requireScalarOrScalarArray(terms.get("include"), "terms aggregation 'include'");
			requireScalarOrScalarArray(terms.get("exclude"), "terms aggregation 'exclude'");
		}
		checkBoundsShape(aggregation.path("histogram"), "histogram");
		checkBoundsShape(aggregation.path("date_histogram"), "date_histogram");
		checkRangesShape(aggregation.path("range"), "range");
		checkRangesShape(aggregation.path("date_range"), "date_range");

		// `filter` / `filters` bodies are full query subtrees; their opaque leaf slots must pass the
		// same shape gate as the top-level query (mirrors how a highlight_query body is gated).
		JsonNode filter = aggregation.get("filter");
		if (filter != null && filter.isObject()) {
			validateQueryLeafShapes(filter);
		}
		// The filters slot is either a keyed object (name to query) or an array of queries; iterating
		// a JsonNode yields the map values in the first case and the elements in the second.
		JsonNode filters = aggregation.path("filters").get("filters");
		if (filters != null) {
			for (JsonNode query : filters) {
				validateQueryLeafShapes(query);
			}
		}

		validateAggregationLeafShapes(aggregation.get("aggregations"));
	}

	static void checkBoundsShape(JsonNode aggregationBody, String aggType) {
		if (!aggregationBody.isObject()) {
			return;
		}
		checkMinMax(aggregationBody.get("extended_bounds"), aggType + ".extended_bounds");
		checkMinMax(aggregationBody.get("hard_bounds"), aggType + ".hard_bounds");
	}

	static void checkMinMax(JsonNode bounds, String label) {
		if (bounds == null || !bounds.isObject()) {
			return;
		}
		requireScalar(bounds.get("min"), label + ".min");
		requireScalar(bounds.get("max"), label + ".max");
	}

	static void checkRangesShape(JsonNode aggregationBody, String aggType) {
		if (!aggregationBody.isObject()) {
			return;
		}
		JsonNode ranges = aggregationBody.get("ranges");
		if (ranges == null || !ranges.isArray()) {
			return;
		}
		for (int i = 0; i < ranges.size(); i++) {
			JsonNode range = ranges.get(i);
			if (range.isObject()) {
				requireScalar(range.get("from"), aggType + ".ranges[" + i + "].from");
				requireScalar(range.get("to"), aggType + ".ranges[" + i + "].to");
			}
		}
	}

	static boolean isScalar(JsonNode node) {
		return node != null && (node.isTextual() || node.isNumber() || node.isBoolean());
	}

	static String describeShape(JsonNode node) {
		if (node == null || node.isNull()) {
			return "null";
		}
		if (node.isObject()) {
			return "an object";
		}
		if (node.isArray()) {
			return "an array";
		}
		return "a scalar";
	}

	static void requireScalar(JsonNode value, String label) {
		if (value == null || value.isNull()) {
			return;
		}
		if (!isScalar(value)) {
			throw new IllegalArgumentException(label
					+ " must be a number, string, or boolean, not " + describeShape(value));
		}
	}

	static void requireScalarArray(JsonNode value, String label) {
		if (value == null || value.isNull()) {
			return;
		}
		if (!value.isArray()) {
			throw new IllegalArgumentException(label
					+ " must be an array of numbers, strings, or booleans, not " + describeShape(value));
		}
		for (int i = 0; i < value.size(); i++) {
			if (!isScalar(value.get(i))) {
				throw new IllegalArgumentException(label + "[" + i
						+ "] must be a number, string, or boolean, not " + describeShape(value.get(i)));
			}
		}
	}

	static void requireScalarOrScalarArray(JsonNode value, String label) {
		if (value == null || value.isNull() || isScalar(value)) {
			return;
		}
		if (value.isArray()) {
			requireScalarArray(value, label);
			return;
		}
		throw new IllegalArgumentException(label
				+ " must be a number, string, or boolean, or an array of those, not " + describeShape(value));
	}

	// --------------------------------------------------------------
	// Public façades — typed validation.
	// --------------------------------------------------------------

	/**
	 * Validate a query-DSL subtree. When {@code autocomplete} is true, additionally requires
	 * the top-level kind to be one of {@link #ALLOWED_AUTOCOMPLETE_TOP_LEVEL}.
	 */
	static void validateQuery(Query query, boolean autocomplete) {
		if (query == null) {
			throw new IllegalArgumentException("query must not be null");
		}
		if (autocomplete && !ALLOWED_AUTOCOMPLETE_TOP_LEVEL.contains(query._kind())) {
			throw new IllegalArgumentException(
					"autocomplete query top-level clause must be one of "
							+ ALLOWED_AUTOCOMPLETE_TOP_LEVEL + "; found '" + query._kind() + "'");
		}
		int[] count = new int[] { 0 };
		walkQuery(query, 1, count);
	}

	/** Validate an aggregations map &mdash; aggregation name to {@link Aggregation}. */
	static void validateAggregations(Map<String, Aggregation> aggregations) {
		if (aggregations == null) {
			throw new IllegalArgumentException("aggregations must not be null");
		}
		int[] count = new int[] { 0 };
		walkAggregationMap(aggregations, 1, count);
	}

	/**
	 * Validate a {@link Highlight} block. Enforces the field-count cap, per-field
	 * fragment / fragment-size caps, and rejects the {@code semantic} highlighter type.
	 * Any nested {@code highlight_query} (top-level or per-field) is recursively validated
	 * against the same query allowlist as the main {@code SearchQuery.query}.
	 */
	static void validateHighlight(Highlight highlight) {
		if (highlight == null) {
			throw new IllegalArgumentException("highlight must not be null");
		}
		rejectSemanticType(highlight.type(), "highlight.type");
		checkHighlightCaps(highlight.numberOfFragments(), highlight.fragmentSize(), "highlight");
		if (highlight.highlightQuery() != null) {
			int[] count = new int[] { 0 };
			walkQuery(highlight.highlightQuery(), 1, count);
		}
		Map<String, HighlightField> fields = highlight.fields();
		if (fields.size() > MAX_HIGHLIGHT_FIELDS) {
			throw new IllegalArgumentException("highlight.fields has " + fields.size()
					+ " entries; max is " + MAX_HIGHLIGHT_FIELDS);
		}
		for (Map.Entry<String, HighlightField> entry : fields.entrySet()) {
			HighlightField hf = entry.getValue();
			String label = "highlight.fields[" + entry.getKey() + "]";
			rejectSemanticType(hf.type(), label + ".type");
			checkHighlightCaps(hf.numberOfFragments(), hf.fragmentSize(), label);
			if (hf.highlightQuery() != null) {
				int[] count = new int[] { 0 };
				walkQuery(hf.highlightQuery(), 1, count);
			}
		}
	}

	static void rejectSemanticType(HighlighterType type, String label) {
		if (type == null) {
			return;
		}
		String name = type.isBuiltin() ? type.builtin().jsonValue()
				: type.isCustom() ? type.custom() : null;
		if (FORBIDDEN_HIGHLIGHTER_TYPE_SEMANTIC.equalsIgnoreCase(name)) {
			throw new IllegalArgumentException(label
					+ " 'semantic' is not allowed (requires a deployed ML model)");
		}
	}

	static void checkHighlightCaps(Integer numberOfFragments, Integer fragmentSize, String label) {
		if (numberOfFragments != null && numberOfFragments > MAX_HIGHLIGHT_FRAGMENTS) {
			throw new IllegalArgumentException(label + ".number_of_fragments is "
					+ numberOfFragments + "; max is " + MAX_HIGHLIGHT_FRAGMENTS);
		}
		if (fragmentSize != null && fragmentSize > MAX_HIGHLIGHT_FRAGMENT_SIZE) {
			throw new IllegalArgumentException(label + ".fragment_size is "
					+ fragmentSize + "; max is " + MAX_HIGHLIGHT_FRAGMENT_SIZE);
		}
	}

	/**
	 * Validate a {@link FieldCollapse} block. Caps {@code max_concurrent_group_searches}. The
	 * unsupported {@code inner_hits} key is rejected upstream &mdash; it is absent from the typed
	 * {@code FieldCollapse} schema, so a caller-supplied {@code inner_hits} is rejected at the
	 * request boundary before reaching here.
	 */
	static void validateFieldCollapse(FieldCollapse collapse) {
		if (collapse == null) {
			throw new IllegalArgumentException("collapse must not be null");
		}
		if (collapse.field().isEmpty()) {
			throw new IllegalArgumentException("collapse.field is required");
		}
		Integer concurrent = collapse.maxConcurrentGroupSearches();
		if (concurrent != null && concurrent > MAX_COLLAPSE_CONCURRENT_GROUP_SEARCHES) {
			throw new IllegalArgumentException("collapse.max_concurrent_group_searches is "
					+ concurrent + "; max is " + MAX_COLLAPSE_CONCURRENT_GROUP_SEARCHES);
		}
	}

	/**
	 * Validate a {@link Rescore} stage. Caps {@code window_size} and recursively validates the
	 * inner {@code rescore_query} subtree against the same allowlist as the top-level
	 * {@code SearchQuery.query}.
	 */
	static void validateRescore(Rescore rescore) {
		if (rescore == null) {
			throw new IllegalArgumentException("rescore must not be null");
		}
		Integer windowSize = rescore.windowSize();
		if (windowSize != null && windowSize > MAX_RESCORE_WINDOW_SIZE) {
			throw new IllegalArgumentException("rescore.window_size is " + windowSize
					+ "; max is " + MAX_RESCORE_WINDOW_SIZE);
		}
		int[] count = new int[] { 0 };
		walkQuery(rescore.query().rescoreQuery(), 1, count);
	}

	/**
	 * Validate a deserialized {@code sort} list against {@link #ALLOWED_SORT_KINDS}. Callers send
	 * native OpenSearch sort shapes, which the deserializer resolves into typed {@link SortOptions}
	 * of a particular kind; any kind not on the allowlist (notably {@code Script} and
	 * {@code GeoDistance}) is rejected here. This is the sort-surface analogue of the query /
	 * aggregation kind allowlists.
	 */
	static void validateSort(List<SortOptions> sort) {
		if (sort == null) {
			throw new IllegalArgumentException("sort must not be null");
		}
		for (SortOptions option : sort) {
			if (!ALLOWED_SORT_KINDS.contains(option._kind())) {
				throw new IllegalArgumentException("sort kind is not allowed: '" + option._kind()
						+ "'. Allowed kinds: " + ALLOWED_SORT_KINDS);
			}
		}
	}

	// --------------------------------------------------------------
	// Query walk.
	// --------------------------------------------------------------

	static void walkQuery(Query query, int depth, int[] count) {
		if (depth > QUERY_MAX_DEPTH) {
			throw new IllegalArgumentException(
					"query is nested too deeply (max depth " + QUERY_MAX_DEPTH + ")");
		}
		if (++count[0] > QUERY_MAX_CLAUSES) {
			throw new IllegalArgumentException(
					"query has too many clauses (max " + QUERY_MAX_CLAUSES + ")");
		}
		// Switch over the clause kinds that carry an additional cap; the remaining allowlisted leaves
		// fall through to the no-op default. The reachable kinds are bounded by the generated SearchQuery
		// POJO, whose query slot is the typed Query schema: its properties are exactly ALLOWED_QUERY_KINDS,
		// and a body carrying any other clause is rejected with HTTP 400 at the request boundary before
		// reaching here (see JSONEntityHttpMessageConverterHelper.validateJSONEntityRecursive).
		switch (query._kind()) {
		case Bool:
			walkBool(query.bool(), depth, count);
			break;
		case DisMax:
			walkDisMax(query.disMax(), depth, count);
			break;
		case ConstantScore:
			walkConstantScore(query.constantScore(), depth, count);
			break;
		case Boosting:
			walkBoosting(query.boosting(), depth, count);
			break;
		case Terms:
			validateTerms(query.terms());
			break;
		case MultiMatch:
			validateMultiMatch(query.multiMatch());
			break;
		case Prefix:
			rejectLeadingWildcardPrefix(query.prefix());
			break;
		case Wildcard:
			rejectLeadingWildcardWildcard(query.wildcard());
			break;
		case SimpleQueryString:
			validateSimpleQueryString(query.simpleQueryString());
			break;
		case Fuzzy:
			validateFuzzyMaxExpansions(query.fuzzy());
			break;
		case MatchPhrasePrefix:
			validateMatchPhrasePrefix(query.matchPhrasePrefix());
			break;
		case MatchBoolPrefix:
			validateMatchBoolPrefix(query.matchBoolPrefix());
			break;
		default:
			// Allowlisted leaves with no additional caps to enforce (Match, MatchPhrase, Term, Range,
			// Exists, MatchAll). Any kind outside ALLOWED_QUERY_KINDS is not a property of the Query
			// schema and is rejected at the request boundary, so it cannot reach here.
			break;
		}
	}

	static void walkBool(BoolQuery bool, int depth, int[] count) {
		walkQueryList(bool.must(), depth + 1, count);
		walkQueryList(bool.should(), depth + 1, count);
		walkQueryList(bool.mustNot(), depth + 1, count);
		walkQueryList(bool.filter(), depth + 1, count);
	}

	static void walkDisMax(DisMaxQuery disMax, int depth, int[] count) {
		List<Query> queries = disMax.queries();
		if (queries.isEmpty()) {
			throw new IllegalArgumentException("'dis_max' clause requires a 'queries' field");
		}
		walkQueryList(queries, depth + 1, count);
	}

	static void walkConstantScore(ConstantScoreQuery constantScore, int depth, int[] count) {
		Query filter = constantScore.filter();
		walkQuery(filter, depth + 1, count);
	}

	static void walkBoosting(BoostingQuery boosting, int depth, int[] count) {
		Query positive = boosting.positive();
		Query negative = boosting.negative();
		walkQuery(positive, depth + 1, count);
		walkQuery(negative, depth + 1, count);
	}

	static void walkQueryList(List<Query> queries, int depth, int[] count) {
		if (queries == null) {
			return;
		}
		for (Query queryClause : queries) {
			walkQuery(queryClause, depth, count);
		}
	}

	// --------------------------------------------------------------
	// Per-kind custom checks.
	// --------------------------------------------------------------

	/**
	 * A {@code terms} clause: reject the cross-index lookup form, cap the inline value array
	 * at {@link #MAX_VALUES_PER_CLAUSE}.
	 */
	static void validateTerms(TermsQuery termsQuery) {
		TermsQueryField terms = termsQuery.terms();
		if (terms._kind() == TermsQueryField.Kind.Lookup) {
			throw new IllegalArgumentException(
					"terms lookup form is not allowed (cross-index reference): '"
							+ termsQuery.field() + "'");
		}
		List<?> values = terms.value();
		if (values != null && values.size() > MAX_VALUES_PER_CLAUSE) {
			throw new IllegalArgumentException("terms array for field '" + termsQuery.field()
					+ "' has " + values.size() + " values; max is " + MAX_VALUES_PER_CLAUSE);
		}
	}

	/**
	 * {@code multi_match}: each {@code fields} entry expands into one internal match clause,
	 * and {@code phrase_prefix} type inherits the {@code max_expansions} concern.
	 */
	static void validateMultiMatch(MultiMatchQuery mm) {
		List<String> fields = mm.fields();
		if (fields.size() > MAX_VALUES_PER_CLAUSE) {
			throw new IllegalArgumentException("multi_match.fields has " + fields.size()
					+ " entries; max is " + MAX_VALUES_PER_CLAUSE);
		}
		checkMaxExpansions(mm.maxExpansions(), "multi_match");
	}

	static void rejectLeadingWildcardPrefix(PrefixQuery prefix) {
		rejectLeadingWildcard(prefix.value(), "prefix", prefix.field());
	}

	static void rejectLeadingWildcardWildcard(WildcardQuery wildcard) {
		String value = wildcard.value() != null ? wildcard.value() : wildcard.wildcard();
		rejectLeadingWildcard(value, "wildcard", wildcard.field());
	}

	static void rejectLeadingWildcard(String pattern, String clause, String field) {
		if (pattern == null || pattern.isEmpty()) {
			return;
		}
		char first = pattern.charAt(0);
		if (first == '*' || first == '?') {
			throw new IllegalArgumentException("leading wildcard is not allowed in '" + clause
					+ "' on field '" + field + "' (forces a full index scan)");
		}
	}

	/**
	 * {@code simple_query_string}: cap {@code fields} length, reject a leading wildcard in
	 * {@code query} when {@code analyze_wildcard} is true (otherwise the leading wildcard
	 * wouldn't actually be evaluated as one). The mini-DSL inside {@code query} is otherwise
	 * passed through &mdash; AOSS request timeouts bound the worst case.
	 */
	static void validateSimpleQueryString(SimpleQueryStringQuery sq) {
		List<String> fields = sq.fields();
		if (fields.size() > MAX_VALUES_PER_CLAUSE) {
			throw new IllegalArgumentException("simple_query_string.fields has " + fields.size()
					+ " entries; max is " + MAX_VALUES_PER_CLAUSE);
		}
		if (Boolean.TRUE.equals(sq.analyzeWildcard())) {
			String pattern = sq.query();
			if (!pattern.isEmpty()) {
				char first = pattern.charAt(0);
				if (first == '*' || first == '?') {
					throw new IllegalArgumentException(
							"leading wildcard is not allowed in 'simple_query_string.query' "
									+ "with analyze_wildcard=true (forces a full index scan)");
				}
			}
		}
	}

	static void validateFuzzyMaxExpansions(FuzzyQuery fuzzy) {
		checkMaxExpansions(fuzzy.maxExpansions(), "fuzzy");
	}

	static void validateMatchPhrasePrefix(MatchPhrasePrefixQuery mpp) {
		checkMaxExpansions(mpp.maxExpansions(), "match_phrase_prefix");
	}

	static void validateMatchBoolPrefix(MatchBoolPrefixQuery mbp) {
		checkMaxExpansions(mbp.maxExpansions(), "match_bool_prefix");
	}

	static void checkMaxExpansions(Integer value, String clause) {
		if (value != null && value > MAX_PREFIX_EXPANSIONS) {
			throw new IllegalArgumentException("'" + clause + "' max_expansions is "
					+ value + "; max is " + MAX_PREFIX_EXPANSIONS);
		}
	}

	// --------------------------------------------------------------
	// Aggregation walk.
	// --------------------------------------------------------------

	static void walkAggregationMap(Map<String, Aggregation> map, int depth, int[] count) {
		if (depth > AGG_MAX_DEPTH) {
			throw new IllegalArgumentException(
					"aggregations are nested too deeply (max depth " + AGG_MAX_DEPTH + ")");
		}
		for (Map.Entry<String, Aggregation> entry : map.entrySet()) {
			if (++count[0] > AGG_MAX_COUNT) {
				throw new IllegalArgumentException(
						"too many aggregations (max " + AGG_MAX_COUNT + ")");
			}
			walkAggregation(entry.getValue(), depth, count);
		}
	}

	static void walkAggregation(Aggregation agg, int depth, int[] count) {
		// Switch over the aggregation kinds that carry an additional cap; the remaining allowlisted
		// kinds fall through to the no-op default. Per the same posture as walkQuery, the reachable
		// kinds are bounded by the Aggregation schema, whose properties are exactly
		// ALLOWED_AGGREGATION_KINDS; any other kind is rejected at the request boundary.
		switch (agg._kind()) {
		case Terms:
			validateTermsAgg(agg.terms());
			break;
		case Range:
			validateRangeAgg(agg.range());
			break;
		case DateRange:
			validateDateRangeAgg(agg.dateRange());
			break;
		case Histogram:
			validateHistogramAgg(agg.histogram());
			break;
		case DateHistogram:
			validateDateHistogramAgg(agg.dateHistogram());
			break;
		case Cardinality:
			validateCardinalityAgg(agg.cardinality());
			break;
		case Filter:
			// The filter body is a query clause; validate it through the same depth/clause caps and
			// per-kind checks as the top-level query, sharing the aggregation's clause counter so a
			// filter body cannot smuggle additional clauses past QUERY_MAX_CLAUSES.
			walkQuery(agg.filter(), depth + 1, count);
			break;
		case Filters:
			walkFilters(agg.filters(), depth + 1, count);
			break;
		default:
			// Allowlisted aggregations with no additional caps to enforce (Missing, Min, Max, Avg,
			// Sum, Stats, ExtendedStats, ValueCount). Any kind outside ALLOWED_AGGREGATION_KINDS is not
			// a property of the Aggregation schema and is rejected at the request boundary, so it
			// cannot reach here.
			break;
		}
		Map<String, Aggregation> subAggregations = agg.aggregations();
		if (!subAggregations.isEmpty()) {
			walkAggregationMap(subAggregations, depth + 1, count);
		}
	}

	/**
	 * Validate the query bodies carried by a {@code filters} aggregation. The {@code filters} slot is
	 * a tagged union of either a keyed map (name to query) or an array of queries; each query is run
	 * through {@link #walkQuery} with the shared clause counter, exactly like the single-bucket
	 * {@code filter} body.
	 */
	static void walkFilters(FiltersAggregation filters, int depth, int[] count) {
		Buckets<Query> buckets = filters.filters();
		if (buckets == null) {
			return;
		}
		if (buckets.isKeyed()) {
			for (Query query : buckets.keyed().values()) {
				walkQuery(query, depth, count);
			}
		} else if (buckets.isArray()) {
			for (Query query : buckets.array()) {
				walkQuery(query, depth, count);
			}
		}
	}

	/**
	 * A {@code terms} aggregation: cap {@code size} / {@code shard_size} and the
	 * include/exclude term-list lengths. Regex-string {@code include}/{@code exclude} are
	 * passed through (catastrophic regex is bounded by AOSS request timeouts).
	 */
	static void validateTermsAgg(TermsAggregation terms) {
		checkAggSize(terms.size(), "terms", "size");
		checkAggSize(terms.shardSize(), "terms", "shard_size");
		TermsInclude include = terms.include();
		if (include != null && include.isTerms()) {
			List<String> list = include.terms();
			if (list != null && list.size() > MAX_VALUES_PER_CLAUSE) {
				throw new IllegalArgumentException("'terms' aggregation 'include' has "
						+ list.size() + " entries; max is " + MAX_VALUES_PER_CLAUSE);
			}
		}
		TermsExclude exclude = terms.exclude();
		if (exclude != null && exclude.isTerms()) {
			List<String> list = exclude.terms();
			if (list != null && list.size() > MAX_VALUES_PER_CLAUSE) {
				throw new IllegalArgumentException("'terms' aggregation 'exclude' has "
						+ list.size() + " entries; max is " + MAX_VALUES_PER_CLAUSE);
			}
		}
	}

	static void validateRangeAgg(RangeAggregation range) {
		List<?> ranges = range.ranges();
		if (ranges.size() > MAX_VALUES_PER_CLAUSE) {
			throw new IllegalArgumentException("'range' aggregation 'ranges' has "
					+ ranges.size() + " entries; max is " + MAX_VALUES_PER_CLAUSE);
		}
	}

	static void validateDateRangeAgg(DateRangeAggregation dateRange) {
		List<?> ranges = dateRange.ranges();
		if (ranges.size() > MAX_VALUES_PER_CLAUSE) {
			throw new IllegalArgumentException("'date_range' aggregation 'ranges' has "
					+ ranges.size() + " entries; max is " + MAX_VALUES_PER_CLAUSE);
		}
	}

	/**
	 * A {@code histogram} aggregation produces (max-min)/interval buckets. Without bounds
	 * the count is unbounded; require either {@code extended_bounds} or {@code hard_bounds},
	 * and require a positive {@code interval}.
	 */
	static void validateHistogramAgg(HistogramAggregation histogram) {
		Double interval = histogram.interval();
		if (interval != null && interval <= 0) {
			throw new IllegalArgumentException("'histogram' interval must be positive");
		}
		if (histogram.extendedBounds() == null && histogram.hardBounds() == null) {
			throw new IllegalArgumentException(
					"'histogram' must specify 'extended_bounds' or 'hard_bounds' to bound bucket count");
		}
	}

	static void validateDateHistogramAgg(DateHistogramAggregation dateHistogram) {
		// date_histogram allows interval / calendar_interval / fixed_interval; the typed
		// model can't easily check positivity for Time-typed intervals, so we focus on the
		// bounds requirement which is what actually caps bucket count.
		if (dateHistogram.extendedBounds() == null && dateHistogram.hardBounds() == null) {
			throw new IllegalArgumentException(
					"'date_histogram' must specify 'extended_bounds' or 'hard_bounds' to bound bucket count");
		}
	}

	static void validateCardinalityAgg(CardinalityAggregation cardinality) {
		Integer precision = cardinality.precisionThreshold();
		if (precision != null && precision > MAX_PRECISION_THRESHOLD) {
			throw new IllegalArgumentException("'cardinality' precision_threshold is "
					+ precision + "; max is " + MAX_PRECISION_THRESHOLD);
		}
	}

	static void checkAggSize(Integer value, String aggType, String key) {
		if (value != null && value > MAX_AGG_SIZE) {
			throw new IllegalArgumentException("'" + aggType + "' aggregation '" + key
					+ "' is " + value + "; max is " + MAX_AGG_SIZE);
		}
	}

}
