package org.sagebionetworks.repo.manager.search;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.manager.search.SearchFieldRewriter.RoutingContext;
import org.sagebionetworks.repo.manager.search.SearchFieldRewriter.RoutingMode;
import org.sagebionetworks.repo.manager.search.SearchFieldRewriter.Surface;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class SearchFieldRewriterTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final Map<String, String> NAME_TO_ID = new LinkedHashMap<>();
	static {
		NAME_TO_ID.put("title", "100");
		NAME_TO_ID.put("name", "101");
		NAME_TO_ID.put("count", "102");
	}

	/** Name-only routing context: maps via NAME_TO_ID, reports every column as non-text so
	 *  KEYWORD_FOR_TEXT mode is a no-op. Behaviorally equivalent to the legacy 2-arg overloads. */
	private static final RoutingContext NAME_ONLY = new RoutingContext() {
		@Override public String mapName(String name) {
			return NAME_TO_ID.getOrDefault(name, name);
		}
		@Override public boolean isTextLike(String columnId) { return false; }
	};

	private static final Map<String, String> ID_TO_NAME = new LinkedHashMap<>();
	static {
		ID_TO_NAME.put("100", "title");
		ID_TO_NAME.put("101", "name");
		ID_TO_NAME.put("102", "count");
	}
	private static final Function<String, String> REVERSE = ID_TO_NAME::get;

	private static JsonNode parse(String json) throws JsonProcessingException {
		return MAPPER.readTree(json);
	}

	// -----------------------------------------------------------------------------
	// rewriteFieldRef — pure textual handling
	// -----------------------------------------------------------------------------

	@Test
	public void testRewriteFieldRefWithKnownName() {
		assertEquals("100", SearchFieldRewriter.rewriteFieldRef("title", NAME_ONLY, RoutingMode.BARE));
	}

	@Test
	public void testRewriteFieldRefWithKeywordSubField() {
		assertEquals("100.keyword", SearchFieldRewriter.rewriteFieldRef("title.keyword", NAME_ONLY, RoutingMode.BARE));
	}

	@Test
	public void testRewriteFieldRefWithBoost() {
		assertEquals("100^3", SearchFieldRewriter.rewriteFieldRef("title^3", NAME_ONLY, RoutingMode.BARE));
	}

	@Test
	public void testRewriteFieldRefWithKeywordAndBoost() {
		assertEquals("100.keyword^2",
				SearchFieldRewriter.rewriteFieldRef("title.keyword^2", NAME_ONLY, RoutingMode.BARE));
	}

	@Test
	public void testRewriteFieldRefWithUnknownNamePassesThrough() {
		// Unknown names go to AOSS as-is so the error message surfaces the typo.
		assertEquals("ghost.keyword",
				SearchFieldRewriter.rewriteFieldRef("ghost.keyword", NAME_ONLY, RoutingMode.BARE));
	}

	@Test
	public void testRewriteFieldRefWithUnrecognizedSubFieldPassesThrough() {
		// Only ".keyword" is recognized as a sub-field selector; anything else is part of
		// the column name (which must include the dot literally).
		assertEquals("title.searchable",
				SearchFieldRewriter.rewriteFieldRef("title.searchable", NAME_ONLY, RoutingMode.BARE));
	}

	@Test
	public void testRewriteFieldRefWithNullReturnsNull() {
		assertNull(SearchFieldRewriter.rewriteFieldRef(null, NAME_ONLY, RoutingMode.BARE));
	}

	// -----------------------------------------------------------------------------
	// rewriteRequestFields — JsonNode tree mutation
	// -----------------------------------------------------------------------------

	@Test
	public void testRewriteRequestFieldsWithShorthandMatch() throws IOException {
		// Shorthand form: the inner object's single key IS the field name.
		JsonNode dsl = parse("{\"match\":{\"title\":\"amyloid\"}}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, NAME_ONLY, Surface.QUERY);

		assertEquals("amyloid", dsl.get("match").get("100").asText());
		assertEquals(1, dsl.get("match").size());
	}

	@Test
	public void testRewriteRequestFieldsWithShorthandRangeKeepsValueObject() throws IOException {
		// Shorthand range with a nested operator object as the value.
		JsonNode dsl = parse("{\"range\":{\"count\":{\"gte\":1,\"lt\":10}}}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, NAME_ONLY, Surface.QUERY);

		JsonNode rangeBody = dsl.get("range").get("102");
		assertEquals(1, rangeBody.get("gte").asInt());
		assertEquals(10, rangeBody.get("lt").asInt());
	}

	@Test
	public void testRewriteRequestFieldsWithShorthandUnknownNamePassesThrough() throws IOException {
		JsonNode dsl = parse("{\"term\":{\"ghost\":\"x\"}}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, NAME_ONLY, Surface.QUERY);

		assertEquals("x", dsl.get("term").get("ghost").asText());
	}

	@Test
	public void testRewriteRequestFieldsWithShorthandKeywordSuffix() throws IOException {
		JsonNode dsl = parse("{\"term\":{\"title.keyword\":\"x\"}}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, NAME_ONLY, Surface.QUERY);

		assertEquals("x", dsl.get("term").get("100.keyword").asText());
	}

	@Test
	public void testRewriteRequestFieldsWithLongFormFieldStillWorks() throws IOException {
		// Long-form with explicit "field" property — leaf rule rewrites the value, shorthand
		// rule sees the literal key "field" and skips it.
		JsonNode dsl = parse("{\"match\":{\"field\":\"title\",\"query\":\"a\"}}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, NAME_ONLY, Surface.QUERY);

		assertEquals("100", dsl.get("match").get("field").asText());
		assertEquals("a", dsl.get("match").get("query").asText());
	}

	@Test
	public void testRewriteRequestFieldsWithLeafField() throws IOException {
		JsonNode dsl = parse("{\"match\":{\"field\":\"title\",\"query\":\"hi\"}}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, NAME_ONLY, Surface.QUERY);

		assertEquals("100", dsl.get("match").get("field").asText());
		assertEquals("hi", dsl.get("match").get("query").asText());
	}

	@Test
	public void testRewriteRequestFieldsWithUnknownLeafFieldPassesThrough() throws IOException {
		JsonNode dsl = parse("{\"match\":{\"field\":\"ghost\",\"query\":\"hi\"}}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, NAME_ONLY, Surface.QUERY);

		assertEquals("ghost", dsl.get("match").get("field").asText());
	}

	@Test
	public void testRewriteRequestFieldsWithKeywordAndBoostSuffixes() throws IOException {
		JsonNode dsl = parse("{\"term\":{\"field\":\"title.keyword\"}}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, NAME_ONLY, Surface.QUERY);

		assertEquals("100.keyword", dsl.get("term").get("field").asText());
	}

	@Test
	public void testRewriteRequestFieldsWithFieldsArray() throws IOException {
		JsonNode dsl = parse("{\"multi_match\":{\"query\":\"hello\","
				+ "\"fields\":[\"title^3\",\"name.keyword\",\"ghost\"],"
				+ "\"max_expansions\":20}}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, NAME_ONLY, Surface.QUERY);

		JsonNode mm = dsl.get("multi_match");
		assertEquals("100^3", mm.get("fields").get(0).asText());
		assertEquals("101.keyword", mm.get("fields").get(1).asText());
		// Unknown name passes through unchanged.
		assertEquals("ghost", mm.get("fields").get(2).asText());
		// Sibling properties are untouched.
		assertEquals("hello", mm.get("query").asText());
		assertEquals(20, mm.get("max_expansions").asInt());
	}

	@Test
	public void testRewriteRequestFieldsWithDeepNesting() throws IOException {
		JsonNode dsl = parse("{\"bool\":{"
				+ "\"must\":[{\"match\":{\"field\":\"title\",\"query\":\"a\"}}],"
				+ "\"filter\":[{\"term\":{\"field\":\"name.keyword\"}}],"
				+ "\"should\":[{\"dis_max\":{\"queries\":["
				+ "  {\"range\":{\"field\":\"count\",\"gt\":1}},"
				+ "  {\"exists\":{\"field\":\"title\"}}"
				+ "]}}]"
				+ "}}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, NAME_ONLY, Surface.QUERY);

		JsonNode bool = dsl.get("bool");
		assertEquals("100", bool.get("must").get(0).get("match").get("field").asText());
		assertEquals("101.keyword", bool.get("filter").get(0).get("term").get("field").asText());
		JsonNode disMaxQs = bool.get("should").get(0).get("dis_max").get("queries");
		assertEquals("102", disMaxQs.get(0).get("range").get("field").asText());
		assertEquals("100", disMaxQs.get(1).get("exists").get("field").asText());
	}

	@Test
	public void testRewriteRequestFieldsWithAggregationSubAggs() throws IOException {
		JsonNode dsl = parse("{\"by_title\":{"
				+ "\"terms\":{\"field\":\"title\"},"
				+ "\"aggregations\":{\"inner\":{\"avg\":{\"field\":\"count\"}}}"
				+ "}}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, NAME_ONLY, Surface.QUERY);

		JsonNode outer = dsl.get("by_title");
		assertEquals("100", outer.get("terms").get("field").asText());
		assertEquals("102",
				outer.get("aggregations").get("inner").get("avg").get("field").asText());
	}

	@Test
	public void testRewriteRequestFieldsWithNullIsNoOp() {
		// call under test
		SearchFieldRewriter.rewriteRequestFields(null, NAME_ONLY, Surface.QUERY);
	}

	@Test
	public void testRewriteRequestFieldsWithEmptyObjectIsNoOp() throws IOException {
		JsonNode dsl = parse("{}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, NAME_ONLY, Surface.QUERY);

		assertEquals(0, dsl.size());
	}

	// -----------------------------------------------------------------------------
	// Parity coverage: every column-bearing surface across allowlisted kinds.
	// -----------------------------------------------------------------------------

	@Test
	public void testRewriteRequestFieldsWithEveryShorthandLeafKind() throws IOException {
		// One fixture per allowlisted shorthand-keyed leaf kind. Each carries the column
		// name as the inner object's single key; my walker must rewrite every one.
		JsonNode dsl = parse("{"
				+ "\"a\":{\"match\":{\"title\":\"x\"}},"
				+ "\"b\":{\"match_phrase\":{\"title\":\"x\"}},"
				+ "\"c\":{\"match_phrase_prefix\":{\"title\":\"x\"}},"
				+ "\"d\":{\"match_bool_prefix\":{\"title\":\"x\"}},"
				+ "\"e\":{\"term\":{\"title\":\"x\"}},"
				+ "\"f\":{\"terms\":{\"title\":[\"x\",\"y\"]}},"
				+ "\"g\":{\"range\":{\"count\":{\"gte\":1}}},"
				+ "\"h\":{\"prefix\":{\"title\":\"x\"}},"
				+ "\"i\":{\"wildcard\":{\"title\":\"x*y\"}},"
				+ "\"j\":{\"fuzzy\":{\"title\":\"x\"}}"
				+ "}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, NAME_ONLY, Surface.QUERY);

		assertEquals("x", dsl.get("a").get("match").get("100").asText());
		assertEquals("x", dsl.get("b").get("match_phrase").get("100").asText());
		assertEquals("x", dsl.get("c").get("match_phrase_prefix").get("100").asText());
		assertEquals("x", dsl.get("d").get("match_bool_prefix").get("100").asText());
		assertEquals("x", dsl.get("e").get("term").get("100").asText());
		assertEquals(2, dsl.get("f").get("terms").get("100").size());
		assertEquals(1, dsl.get("g").get("range").get("102").get("gte").asInt());
		assertEquals("x", dsl.get("h").get("prefix").get("100").asText());
		assertEquals("x*y", dsl.get("i").get("wildcard").get("100").asText());
		assertEquals("x", dsl.get("j").get("fuzzy").get("100").asText());
	}

	@Test
	public void testRewriteRequestFieldsWithExistsLongFormOnly() throws IOException {
		// `exists` is long-form-only on the wire; only the `field` rule applies.
		JsonNode dsl = parse("{\"exists\":{\"field\":\"title\"}}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, NAME_ONLY, Surface.QUERY);

		assertEquals("100", dsl.get("exists").get("field").asText());
	}

	@Test
	public void testRewriteRequestFieldsWithTermsAggOrderKeysLeftAlone() throws IOException {
		// `order` keys are pseudo-keys (_count / _key) or sub-agg names — never columns.
		// The walker must rewrite the explicit `field` and leave `order` keys verbatim,
		// even when an order key happens to collide with a known column name.
		JsonNode dsl = parse("{\"by_x\":{\"terms\":{"
				+ "\"field\":\"title\","
				+ "\"order\":{\"_count\":\"desc\",\"title\":\"asc\"}"
				+ "}}}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, NAME_ONLY, Surface.QUERY);

		JsonNode terms = dsl.get("by_x").get("terms");
		assertEquals("100", terms.get("field").asText());
		assertEquals("desc", terms.get("order").get("_count").asText());
		assertEquals("asc", terms.get("order").get("title").asText());
	}

	@Test
	public void testRewriteRequestFieldsWithCallerNamedAggregationKeysLeftAlone() throws IOException {
		// Aggregation names are caller-chosen labels. Even if a caller names one "field"
		// or "fields", the walker must not treat the label as a column reference because
		// the value is an object/array of agg-definition shape, not a textual column ref.
		JsonNode dsl = parse("{\"field\":{\"terms\":{\"field\":\"title\"}},"
				+ "\"fields\":{\"avg\":{\"field\":\"count\"}}}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, NAME_ONLY, Surface.QUERY);

		// Outer "field" / "fields" labels untouched.
		assertTrue(dsl.has("field"));
		assertTrue(dsl.has("fields"));
		// Inner column refs rewritten.
		assertEquals("100", dsl.get("field").get("terms").get("field").asText());
		assertEquals("102", dsl.get("fields").get("avg").get("field").asText());
	}

	@Test
	public void testRewriteRequestFieldsWithTermObjectValueShorthand() throws IOException {
		// `term` shorthand with a value object (e.g. {value: ..., boost: 2}).
		JsonNode dsl = parse("{\"term\":{\"title\":{\"value\":\"x\",\"boost\":2.0}}}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, NAME_ONLY, Surface.QUERY);

		assertEquals("x", dsl.get("term").get("100").get("value").asText());
		assertEquals(2.0, dsl.get("term").get("100").get("boost").asDouble(), 0.0001);
	}

	@Test
	public void testRewriteRequestFieldsWithRangeAggregationLongFormNotMistakenForShorthand() throws IOException {
		// The literal "range" key also names an aggregation kind, but aggregations always
		// use long-form `field`. A `range` aggregation body like {field: "count", ranges: [...]}
		// has size > 1, so the shorthand handler skips it and rule 1 picks up the `field`.
		JsonNode dsl = parse("{\"price_buckets\":{\"range\":{"
				+ "\"field\":\"count\",\"ranges\":[{\"to\":10},{\"from\":10,\"to\":20}]"
				+ "}}}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, NAME_ONLY, Surface.QUERY);

		assertEquals("102", dsl.get("price_buckets").get("range").get("field").asText());
		assertEquals(2, dsl.get("price_buckets").get("range").get("ranges").size());
	}

	@Test
	public void testRewriteRequestFieldsWithSingleFieldOnlyAggBodyDoesNotRewriteFieldKey() throws IOException {
		// Pathological: an aggregation body that happens to have only `field` — the shorthand
		// handler must still skip because the literal key is "field".
		JsonNode dsl = parse("{\"by_x\":{\"avg\":{\"field\":\"count\"}}}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, NAME_ONLY, Surface.QUERY);

		// `field` key preserved, value rewritten via rule 1 — not via the shorthand rule.
		assertEquals("102", dsl.get("by_x").get("avg").get("field").asText());
	}

	@Test
	public void testRewriteRequestFieldsWithCompoundQueriesRecurseIntoLeaves() throws IOException {
		// bool / dis_max / constant_score / boosting all carry nested query slots that the
		// walker must descend into. Each slot uses a different key (must/should/queries/
		// filter/positive/negative); none of these are shorthand kinds, so the walker
		// recurses into them by the default branch.
		JsonNode dsl = parse("{\"bool\":{"
				+ "\"must\":[{\"match\":{\"title\":\"a\"}}],"
				+ "\"should\":[{\"dis_max\":{\"queries\":["
				+ "  {\"constant_score\":{\"filter\":{\"term\":{\"title\":\"x\"}}}},"
				+ "  {\"boosting\":{"
				+ "    \"positive\":{\"match\":{\"name\":\"y\"}},"
				+ "    \"negative\":{\"match\":{\"name\":\"z\"}},"
				+ "    \"negative_boost\":0.5}}"
				+ "]}}]"
				+ "}}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, NAME_ONLY, Surface.QUERY);

		JsonNode bool = dsl.get("bool");
		assertEquals("a", bool.get("must").get(0).get("match").get("100").asText());
		JsonNode disMaxQs = bool.get("should").get(0).get("dis_max").get("queries");
		assertEquals("x", disMaxQs.get(0).get("constant_score").get("filter").get("term").get("100").asText());
		JsonNode boosting = disMaxQs.get(1).get("boosting");
		assertEquals("y", boosting.get("positive").get("match").get("101").asText());
		assertEquals("z", boosting.get("negative").get("match").get("101").asText());
	}

	// -----------------------------------------------------------------------------
	// rewriteAggregationResults — response side, column id → column name
	// -----------------------------------------------------------------------------

	@Test
	public void testRewriteAggregationResultsRewritesEmbeddedField() throws IOException {
		JsonNode response = parse("{\"by_title\":{\"buckets\":["
				+ "{\"key\":\"a\",\"doc_count\":3,\"avg_count\":{\"field\":\"102\",\"value\":1.5}}"
				+ "]}}");

		// call under test
		SearchFieldRewriter.rewriteAggregationResults(response, REVERSE);

		assertEquals("count", response.get("by_title").get("buckets").get(0)
				.get("avg_count").get("field").asText());
	}

	@Test
	public void testRewriteAggregationResultsLeavesAggregationKeysUnchanged() throws IOException {
		// Top-level keys are caller-chosen aggregation names, not column references.
		JsonNode response = parse("{\"100\":{\"value\":42,\"field\":\"100\"}}");

		// call under test
		SearchFieldRewriter.rewriteAggregationResults(response, REVERSE);

		// The top-level "100" key is left alone (it's a label); the embedded "field" is rewritten.
		assertTrue(response.has("100"));
		assertEquals("title", response.get("100").get("field").asText());
	}

	@Test
	public void testRewriteAggregationResultsWithUnmappedIdLeavesItAlone() throws IOException {
		JsonNode response = parse("{\"agg\":{\"field\":\"999\"}}");

		// call under test
		SearchFieldRewriter.rewriteAggregationResults(response, REVERSE);

		// idToName returned null; field is left as-is.
		assertEquals("999", response.get("agg").get("field").asText());
	}

	// -----------------------------------------------------------------------------
	// Round-trip: rewriteRequestFields + rewriteAggregationResults are inverses
	// -----------------------------------------------------------------------------

	@Test
	public void testRoundTripRequestThenResponseRestoresName() throws IOException {
		JsonNode dsl = parse("{\"terms\":{\"field\":\"title\"}}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, NAME_ONLY, Surface.QUERY);
		assertEquals("100", dsl.get("terms").get("field").asText());

		SearchFieldRewriter.rewriteAggregationResults(dsl, REVERSE);
		assertEquals("title", dsl.get("terms").get("field").asText());
	}

	// -----------------------------------------------------------------------------
	// .keyword auto-routing — RoutingContext + Surface
	// -----------------------------------------------------------------------------

	/** title=100 (TEXT), name=101 (TEXT), count=102 (numeric), userId=103 (KEYWORD). */
	private static final Set<String> TEXT_IDS = new HashSet<>();
	static {
		TEXT_IDS.add("100");
		TEXT_IDS.add("101");
	}

	private static final RoutingContext ROUTING = new RoutingContext() {
		@Override public String mapName(String name) {
			return NAME_TO_ID.getOrDefault(name, name);
		}
		@Override public boolean isTextLike(String columnId) {
			return TEXT_IDS.contains(columnId);
		}
	};

	// rewriteFieldRef — explicit mode

	@Test
	public void testRewriteFieldRefWithKeywordModeOnTextColumnAppendsKeyword() {
		// call under test
		assertEquals("100.keyword",
				SearchFieldRewriter.rewriteFieldRef("title", ROUTING, RoutingMode.KEYWORD_FOR_TEXT));
	}

	@Test
	public void testRewriteFieldRefWithKeywordModeOnNumericColumnLeavesBare() {
		// count is not text-like — KEYWORD_FOR_TEXT must not append .keyword.
		// call under test
		assertEquals("102",
				SearchFieldRewriter.rewriteFieldRef("count", ROUTING, RoutingMode.KEYWORD_FOR_TEXT));
	}

	@Test
	public void testRewriteFieldRefWithKeywordModeIsIdempotentOnExplicitKeyword() {
		// Caller already wrote .keyword — auto-router must not double it.
		// call under test
		assertEquals("100.keyword",
				SearchFieldRewriter.rewriteFieldRef("title.keyword", ROUTING, RoutingMode.KEYWORD_FOR_TEXT));
	}

	@Test
	public void testRewriteFieldRefWithBareModeOnTextColumnLeavesBare() {
		// match-family routing must not append .keyword on text.
		// call under test
		assertEquals("100",
				SearchFieldRewriter.rewriteFieldRef("title", ROUTING, RoutingMode.BARE));
	}

	@Test
	public void testRewriteFieldRefWithKeywordModePreservesBoost() {
		// call under test
		assertEquals("100.keyword^3",
				SearchFieldRewriter.rewriteFieldRef("title^3", ROUTING, RoutingMode.KEYWORD_FOR_TEXT));
	}

	@Test
	public void testRewriteFieldRefWithUnknownNameAndKeywordModeNoAutoRoute() {
		// Without a resolved id we don't know the column type — must not auto-append.
		// call under test
		assertEquals("ghost",
				SearchFieldRewriter.rewriteFieldRef("ghost", ROUTING, RoutingMode.KEYWORD_FOR_TEXT));
	}

	// rewriteRequestFields — query surface

	@Test
	public void testRewriteRequestFieldsWithTermShorthandOnTextColumnRoutesKeyword() throws IOException {
		// term on a text column needs .keyword for exact match.
		JsonNode dsl = parse("{\"term\":{\"title\":\"My Project\"}}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, ROUTING, Surface.QUERY);

		assertEquals("My Project", dsl.get("term").get("100.keyword").asText());
	}

	@Test
	public void testRewriteRequestFieldsWithTermShorthandOnNumericColumnLeavesBare() throws IOException {
		JsonNode dsl = parse("{\"term\":{\"count\":7}}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, ROUTING, Surface.QUERY);

		assertEquals(7, dsl.get("term").get("102").asInt());
	}

	@Test
	public void testRewriteRequestFieldsWithMatchOnTextColumnLeavesBare() throws IOException {
		// match-family clauses always use the analyzed text field.
		JsonNode dsl = parse("{\"match\":{\"title\":\"amyloid\"}}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, ROUTING, Surface.QUERY);

		assertEquals("amyloid", dsl.get("match").get("100").asText());
	}

	@Test
	public void testRewriteRequestFieldsWithRangeOnTextColumnRoutesKeyword() throws IOException {
		JsonNode dsl = parse("{\"range\":{\"title\":{\"gte\":\"a\",\"lt\":\"m\"}}}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, ROUTING, Surface.QUERY);

		assertEquals("a", dsl.get("range").get("100.keyword").get("gte").asText());
	}

	@Test
	public void testRewriteRequestFieldsWithRangeOnNumericColumnLeavesBare() throws IOException {
		JsonNode dsl = parse("{\"range\":{\"count\":{\"gte\":1,\"lt\":10}}}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, ROUTING, Surface.QUERY);

		assertEquals(1, dsl.get("range").get("102").get("gte").asInt());
	}

	@Test
	public void testRewriteRequestFieldsWithPrefixWildcardFuzzyOnTextRouteKeyword() throws IOException {
		JsonNode dsl = parse("{"
				+ "\"a\":{\"prefix\":{\"title\":\"x\"}},"
				+ "\"b\":{\"wildcard\":{\"title\":\"x*y\"}},"
				+ "\"c\":{\"fuzzy\":{\"title\":\"x\"}},"
				+ "\"d\":{\"match_phrase_prefix\":{\"title\":\"x\"}}"
				+ "}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, ROUTING, Surface.QUERY);

		assertEquals("x", dsl.get("a").get("prefix").get("100.keyword").asText());
		assertEquals("x*y", dsl.get("b").get("wildcard").get("100.keyword").asText());
		assertEquals("x", dsl.get("c").get("fuzzy").get("100.keyword").asText());
		assertEquals("x", dsl.get("d").get("match_phrase_prefix").get("100.keyword").asText());
	}

	@Test
	public void testRewriteRequestFieldsWithMultiMatchOnTextLeavesBare() throws IOException {
		// multi_match is match-family — bare on every entry, even text columns.
		JsonNode dsl = parse("{\"multi_match\":{\"query\":\"hi\","
				+ "\"fields\":[\"title^3\",\"name\"]}}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, ROUTING, Surface.QUERY);

		assertEquals("100^3", dsl.get("multi_match").get("fields").get(0).asText());
		assertEquals("101", dsl.get("multi_match").get("fields").get(1).asText());
	}

	@Test
	public void testRewriteRequestFieldsWithExistsOnTextLeavesBare() throws IOException {
		// exists works on either form; we route bare for stability.
		JsonNode dsl = parse("{\"exists\":{\"field\":\"title\"}}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, ROUTING, Surface.QUERY);

		assertEquals("100", dsl.get("exists").get("field").asText());
	}

	@Test
	public void testRewriteRequestFieldsWithBoolMixesRoutingByChildKind() throws IOException {
		// In the same bool: a match (bare) and a term (keyword) on the same text column.
		JsonNode dsl = parse("{\"bool\":{"
				+ "\"must\":[{\"match\":{\"title\":\"amyloid\"}}],"
				+ "\"filter\":[{\"term\":{\"title\":\"primary\"}}]"
				+ "}}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, ROUTING, Surface.QUERY);

		assertEquals("amyloid",
				dsl.get("bool").get("must").get(0).get("match").get("100").asText());
		assertEquals("primary",
				dsl.get("bool").get("filter").get(0).get("term").get("100.keyword").asText());
	}

	// rewriteRequestFields — aggregations surface (every kind routes for text)

	@Test
	public void testRewriteRequestFieldsWithEveryAggKindRoutesTextThroughKeyword() throws IOException {
		JsonNode dsl = parse("{"
				+ "\"a\":{\"terms\":{\"field\":\"title\"}},"
				+ "\"b\":{\"min\":{\"field\":\"title\"}},"
				+ "\"c\":{\"max\":{\"field\":\"title\"}},"
				+ "\"d\":{\"avg\":{\"field\":\"title\"}},"
				+ "\"e\":{\"sum\":{\"field\":\"title\"}},"
				+ "\"f\":{\"stats\":{\"field\":\"title\"}},"
				+ "\"g\":{\"extended_stats\":{\"field\":\"title\"}},"
				+ "\"h\":{\"value_count\":{\"field\":\"title\"}},"
				+ "\"i\":{\"cardinality\":{\"field\":\"title\"}},"
				+ "\"j\":{\"missing\":{\"field\":\"title\"}}"
				+ "}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, ROUTING, Surface.AGGREGATIONS);

		assertEquals("100.keyword", dsl.get("a").get("terms").get("field").asText());
		assertEquals("100.keyword", dsl.get("b").get("min").get("field").asText());
		assertEquals("100.keyword", dsl.get("c").get("max").get("field").asText());
		assertEquals("100.keyword", dsl.get("d").get("avg").get("field").asText());
		assertEquals("100.keyword", dsl.get("e").get("sum").get("field").asText());
		assertEquals("100.keyword", dsl.get("f").get("stats").get("field").asText());
		assertEquals("100.keyword", dsl.get("g").get("extended_stats").get("field").asText());
		assertEquals("100.keyword", dsl.get("h").get("value_count").get("field").asText());
		assertEquals("100.keyword", dsl.get("i").get("cardinality").get("field").asText());
		assertEquals("100.keyword", dsl.get("j").get("missing").get("field").asText());
	}

	@Test
	public void testRewriteRequestFieldsWithAggOnNumericColumnLeavesBare() throws IOException {
		JsonNode dsl = parse("{\"avg_count\":{\"avg\":{\"field\":\"count\"}}}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, ROUTING, Surface.AGGREGATIONS);

		assertEquals("102", dsl.get("avg_count").get("avg").get("field").asText());
	}

	@Test
	public void testRewriteRequestFieldsWithSubAggregationsRouteIndependently() throws IOException {
		JsonNode dsl = parse("{\"by_title\":{"
				+ "\"terms\":{\"field\":\"title\"},"
				+ "\"aggregations\":{\"avg_count\":{\"avg\":{\"field\":\"count\"}}}"
				+ "}}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, ROUTING, Surface.AGGREGATIONS);

		// Outer terms on text — routed.
		assertEquals("100.keyword", dsl.get("by_title").get("terms").get("field").asText());
		// Inner avg on numeric — bare.
		assertEquals("102",
				dsl.get("by_title").get("aggregations").get("avg_count").get("avg").get("field").asText());
	}

	// Response side: .keyword stripped on the way out

	@Test
	public void testRewriteAggregationResultsStripsKeywordSuffix() throws IOException {
		// The server may have routed the request through .keyword; the response should echo
		// just the bare column name to the caller.
		JsonNode response = parse("{\"by_title\":{\"buckets\":["
				+ "{\"key\":\"a\",\"doc_count\":3,"
				+ "\"avg_count\":{\"field\":\"100.keyword\",\"value\":1.5}}"
				+ "]}}");

		// call under test
		SearchFieldRewriter.rewriteAggregationResults(response, REVERSE);

		assertEquals("title",
				response.get("by_title").get("buckets").get(0).get("avg_count").get("field").asText());
	}

	@Test
	public void testRewriteAggregationResultsBarePassesThroughUnchanged() throws IOException {
		// Numeric column response — no .keyword to strip, and the bare id resolves.
		JsonNode response = parse("{\"avg_count\":{\"field\":\"102\",\"value\":3.0}}");

		// call under test
		SearchFieldRewriter.rewriteAggregationResults(response, REVERSE);

		assertEquals("count", response.get("avg_count").get("field").asText());
	}

	// rewriteRequestFields — highlight surface (object-keyed `fields` map)

	@Test
	public void testRewriteRequestFieldsWithHighlightFieldsRewritesKeys() throws IOException {
		// highlight.fields is an object keyed by column name; each key must be rewritten
		// to its column id. The empty option blocks are kept as-is.
		JsonNode dsl = parse("{\"fields\":{\"title\":{},\"name\":{}}}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, ROUTING, Surface.HIGHLIGHT);

		// Both column names rewritten to their ids; option values preserved.
		assertTrue(dsl.get("fields").has("100"));
		assertTrue(dsl.get("fields").has("101"));
		assertFalse(dsl.get("fields").has("title"));
		assertFalse(dsl.get("fields").has("name"));
	}

	@Test
	public void testRewriteRequestFieldsWithHighlightFieldsLeavesBareForTextColumn() throws IOException {
		// Highlight uses the analyzed (bare) text field — no .keyword auto-routing.
		JsonNode dsl = parse("{\"fields\":{\"title\":{}}}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, ROUTING, Surface.HIGHLIGHT);

		assertTrue(dsl.get("fields").has("100"));
		// Must NOT be 100.keyword.
		assertFalse(dsl.get("fields").has("100.keyword"));
	}

	@Test
	public void testRewriteRequestFieldsWithHighlightQueryDescendsAsQuerySurface() throws IOException {
		// A nested highlight_query carries a Query subtree; the walker must switch surfaces
		// so the auto-router applies (term on text → .keyword).
		JsonNode dsl = parse("{\"fields\":{\"title\":{"
				+ "\"highlight_query\":{\"term\":{\"title\":\"abc\"}}"
				+ "}}}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, ROUTING, Surface.HIGHLIGHT);

		// title key rewritten to 100; the nested term on text must auto-route to .keyword.
		JsonNode innerTerm = dsl.get("fields").get("100").get("highlight_query").get("term");
		assertEquals("abc", innerTerm.get("100.keyword").asText());
	}

	@Test
	public void testRewriteRequestFieldsWithHighlightUnknownNamePassesThrough() throws IOException {
		// Unknown names go to AOSS as-is so the error message surfaces the typo.
		JsonNode dsl = parse("{\"fields\":{\"ghost\":{}}}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, ROUTING, Surface.HIGHLIGHT);

		assertTrue(dsl.get("fields").has("ghost"));
	}

	@Test
	public void testRoutedRoundTripRestoresBareName() throws IOException {
		// Caller writes the bare name; the request rewriter routes through .keyword; the
		// response rewriter strips .keyword and returns the bare name.
		JsonNode dsl = parse("{\"by_title\":{\"terms\":{\"field\":\"title\"}}}");

		// Request side
		SearchFieldRewriter.rewriteRequestFields(dsl, ROUTING, Surface.AGGREGATIONS);
		assertEquals("100.keyword", dsl.get("by_title").get("terms").get("field").asText());

		// Response side
		SearchFieldRewriter.rewriteAggregationResults(dsl, REVERSE);
		assertEquals("title", dsl.get("by_title").get("terms").get("field").asText());
	}

	@Test
	public void testRewriteRequestFieldsWithFilterAggDescendsAsQuerySurface() throws IOException {
		// A filter aggregation's body is a Query subtree; the walker must switch to the query surface
		// so a term on a text column auto-routes to .keyword (the agg surface would route differently
		// and the column-name key must still resolve to its id).
		JsonNode dsl = parse("{\"by_status\":{"
				+ "\"filter\":{\"term\":{\"title\":\"abc\"}},"
				+ "\"aggregations\":{\"avg_count\":{\"avg\":{\"field\":\"count\"}}}"
				+ "}}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, ROUTING, Surface.AGGREGATIONS);

		// The filter body's term on text routes to .keyword and the column name maps to its id.
		assertEquals("abc",
				dsl.get("by_status").get("filter").get("term").get("100.keyword").asText());
		// The sibling sub-aggregation still routes on the aggregations surface (numeric → bare).
		assertEquals("102",
				dsl.get("by_status").get("aggregations").get("avg_count").get("avg").get("field").asText());
	}

	@Test
	public void testRewriteRequestFieldsWithFiltersKeyedDescendsAsQuerySurface() throws IOException {
		// Each named query in a keyed filters bucket is a Query subtree routed on the query surface.
		JsonNode dsl = parse("{\"by_status\":{\"filters\":{\"filters\":{"
				+ "\"hits\":{\"term\":{\"title\":\"a\"}},"
				+ "\"misses\":{\"term\":{\"name\":\"b\"}}"
				+ "}}}}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, ROUTING, Surface.AGGREGATIONS);

		JsonNode buckets = dsl.get("by_status").get("filters").get("filters");
		assertEquals("a", buckets.get("hits").get("term").get("100.keyword").asText());
		assertEquals("b", buckets.get("misses").get("term").get("101.keyword").asText());
	}

	@Test
	public void testRewriteRequestFieldsWithFiltersArrayDescendsAsQuerySurface() throws IOException {
		// The array (non-keyed) filters form descends into each query the same way.
		JsonNode dsl = parse("{\"by_status\":{\"filters\":{\"filters\":["
				+ "{\"term\":{\"title\":\"a\"}},"
				+ "{\"term\":{\"name\":\"b\"}}"
				+ "]}}}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, ROUTING, Surface.AGGREGATIONS);

		JsonNode buckets = dsl.get("by_status").get("filters").get("filters");
		assertEquals("a", buckets.get(0).get("term").get("100.keyword").asText());
		assertEquals("b", buckets.get(1).get("term").get("101.keyword").asText());
	}

	// -----------------------------------------------------------------------------
	// Surface enum coverage guard
	// -----------------------------------------------------------------------------

	/**
	 * Single round trip exercising every {@link SearchFieldRewriter.Surface}: each surface
	 * gets a body containing one column-name reference that must be rewritten. EnumSet.allOf
	 * coverage guard at the bottom — adding a new {@code Surface} value without a fixture
	 * fails the test until the fixture is added.
	 */
	@Test
	public void testRewriteRequestFieldsWithEverySurface() throws IOException {
		EnumMap<Surface, String> bodies = new EnumMap<>(Surface.class);
		bodies.put(Surface.QUERY,        "{\"match\":{\"title\":\"x\"}}");
		bodies.put(Surface.AGGREGATIONS, "{\"a\":{\"terms\":{\"field\":\"title\"}}}");
		bodies.put(Surface.HIGHLIGHT,    "{\"fields\":{\"title\":{}}}");
		bodies.put(Surface.COLLAPSE,     "{\"field\":\"title\"}");

		Set<Surface> covered = EnumSet.noneOf(Surface.class);
		for (Map.Entry<Surface, String> entry : bodies.entrySet()) {
			JsonNode dsl = parse(entry.getValue());
			// call under test
			SearchFieldRewriter.rewriteRequestFields(dsl, NAME_ONLY, entry.getKey());
			// Every surface must have rewritten "title" → "100" somewhere in the tree;
			// findValuesAsText collects every "field" value plus the highlight `fields` key.
			String allText = dsl.toString();
			assertFalse(allText.contains("\"title\""),
					"surface=" + entry.getKey() + " left a column name in: " + allText);
			covered.add(entry.getKey());
		}
		assertEquals(EnumSet.allOf(Surface.class), covered,
				"every Surface must be exercised by this round trip");
	}

	// -----------------------------------------------------------------------------
	// rewriteFieldRef — boost / sub-field / unknown combinations not covered above
	// -----------------------------------------------------------------------------

	@Test
	public void testRewriteFieldRefWithBoostOnlyKnownName() {
		assertEquals("100^2.0",
				SearchFieldRewriter.rewriteFieldRef("title^2.0", NAME_ONLY, RoutingMode.BARE));
	}

	@Test
	public void testRewriteFieldRefWithExplicitKeywordSuffixPreservedNoAutoRoute() {
		// Caller-supplied .keyword must be preserved AND the auto-router must NOT append a
		// second .keyword — the dot-handling treats the explicit suffix as the sub-field.
		RoutingContext textRouting = new RoutingContext() {
			@Override public String mapName(String name) {
				return "title".equals(name) ? "100" : name;
			}
			@Override public boolean isTextLike(String columnId) {
				return "100".equals(columnId);
			}
		};
		assertEquals("100.keyword",
				SearchFieldRewriter.rewriteFieldRef("title.keyword", textRouting, RoutingMode.KEYWORD_FOR_TEXT));
	}

	@Test
	public void testRewriteFieldRefWithUnknownNameAndKeywordModeNoSubFieldAppended() {
		// Auto-router only fires when name resolves; unknown name must come back unchanged
		// even in KEYWORD_FOR_TEXT mode.
		assertEquals("ghost",
				SearchFieldRewriter.rewriteFieldRef("ghost", NAME_ONLY, RoutingMode.KEYWORD_FOR_TEXT));
	}

	@Test
	public void testRewriteFieldRefWithEmptyStringPassesThrough() {
		assertEquals("",
				SearchFieldRewriter.rewriteFieldRef("", NAME_ONLY, RoutingMode.BARE));
	}

	// -----------------------------------------------------------------------------
	// Sort: mixed-shape array fixture (each shape was tested individually)
	// -----------------------------------------------------------------------------

	@Test
	public void testRewriteSortFieldsWithMixedArrayShapes() throws IOException {
		// One sort array carrying every legal element shape so the sort walker exercises
		// each branch in a single fixture: bare string, _score passthrough, object-shorthand.
		JsonNode dsl = parse("[\"title\",\"_score\",{\"name\":\"asc\"}]");

		// call under test
		SearchFieldRewriter.rewriteSortFields(dsl, NAME_ONLY);

		assertEquals("100", dsl.get(0).asText());
		assertEquals("_score", dsl.get(1).asText(),
				"_score must pass through unchanged");
		assertTrue(dsl.get(2).has("101"),
				"object-shorthand key must be rewritten name → id");
		assertFalse(dsl.get(2).has("name"));
	}

	@Test
	public void testRewriteSortFieldsWithBareScoreObjectKeyPassesThrough() throws IOException {
		// {"_score": "asc"} — the object-key path should leave "_score" alone.
		JsonNode dsl = parse("[{\"_score\":\"asc\"}]");
		SearchFieldRewriter.rewriteSortFields(dsl, NAME_ONLY);
		assertTrue(dsl.get(0).has("_score"));
	}

	// -----------------------------------------------------------------------------
	// Shorthand vs long-form on the same kind
	// -----------------------------------------------------------------------------

	@Test
	public void testRewriteRequestFieldsWithLongFormFieldKeyOnShorthandKindNotRewrittenAsKey() throws IOException {
		// {"term": {"field": "title", "value": "x"}} — the inner object has a "field" key
		// (long-form), so the shorthand-key rewrite must NOT replace "field" with the
		// column id. Instead, the leaf "field" property gets rewritten.
		JsonNode dsl = parse("{\"term\":{\"field\":\"title\",\"value\":\"x\"}}");

		// call under test
		SearchFieldRewriter.rewriteRequestFields(dsl, NAME_ONLY, Surface.QUERY);

		// "field" key remains; its value is rewritten to the id.
		assertTrue(dsl.get("term").has("field"));
		assertEquals("100", dsl.get("term").get("field").asText());
		assertEquals("x", dsl.get("term").get("value").asText());
	}

	// -----------------------------------------------------------------------------
	// rewriteAggregationResults — deep nesting
	// -----------------------------------------------------------------------------

	@Test
	public void testRewriteAggregationResultsWithNestedSubAggsRewritesAtEveryDepth() throws IOException {
		// 3-deep aggregation response — id rewriting must descend through the entire tree.
		JsonNode dsl = parse(
				"{\"by_title\":{\"buckets\":["
						+ "{\"key\":\"v1\",\"by_name\":{\"buckets\":["
						+ "{\"key\":\"v2\",\"by_count\":{"
						+ "\"buckets\":[{\"key\":\"v3\"}],\"meta\":{\"field\":\"102.keyword\"}}}]"
						+ ",\"meta\":{\"field\":\"101\"}}}]"
						+ ",\"meta\":{\"field\":\"100.keyword\"}}}");

		// call under test
		SearchFieldRewriter.rewriteAggregationResults(dsl, REVERSE);

		assertEquals("title", dsl.at("/by_title/meta/field").asText());
		assertEquals("name", dsl.at("/by_title/buckets/0/by_name/meta/field").asText());
		assertEquals("count",
				dsl.at("/by_title/buckets/0/by_name/buckets/0/by_count/meta/field").asText());
	}

	@Test
	public void testRewriteAggregationResultsWithUnmappedIdAndKeywordSuffixPreservesRaw() throws IOException {
		// Unknown id with .keyword: rewriteIdRefStrippingKeyword returns the raw input
		// unchanged so the .keyword stays in place — important because the caller never
		// asked for the keyword stripped on a non-Synapse id.
		JsonNode dsl = parse("{\"a\":{\"meta\":{\"field\":\"999.keyword\"}}}");

		SearchFieldRewriter.rewriteAggregationResults(dsl, REVERSE);

		assertEquals("999.keyword", dsl.at("/a/meta/field").asText());
	}

	// -----------------------------------------------------------------------------
	// Null-node guards on the three in-place walkers
	// -----------------------------------------------------------------------------

	@Test
	public void testRewriteSortFieldsWithNullIsNoOp() {
		// call under test — null body must be a no-op, not an NPE.
		assertDoesNotThrow(() -> SearchFieldRewriter.rewriteSortFields(null, NAME_ONLY));
	}

	@Test
	public void testRewriteSourceFieldsWithNullIsNoOp() {
		// call under test
		assertDoesNotThrow(() -> SearchFieldRewriter.rewriteSourceFields(null, NAME_ONLY));
	}

	@Test
	public void testRewriteAggregationResultsWithNullIsNoOp() {
		// call under test
		assertDoesNotThrow(() -> SearchFieldRewriter.rewriteAggregationResults(null, REVERSE));
	}

	// -----------------------------------------------------------------------------
	// Sort: top-level object (not wrapped in an array)
	// -----------------------------------------------------------------------------

	@Test
	public void testRewriteSortFieldsWithTopLevelObjectRewritesKey() throws IOException {
		// {"name":"asc"} as the whole sort value (not inside an array) — the top-level
		// object branch must rewrite the key name → id.
		JsonNode dsl = parse("{\"name\":\"asc\"}");

		// call under test
		SearchFieldRewriter.rewriteSortFields(dsl, NAME_ONLY);

		assertTrue(dsl.has("101"));
		assertFalse(dsl.has("name"));
		assertEquals("asc", dsl.get("101").asText());
	}

	// -----------------------------------------------------------------------------
	// _source: object form with includes / excludes arrays
	// -----------------------------------------------------------------------------

	@Test
	public void testRewriteSourceFieldsWithIncludesExcludesObjectFormRewrites() throws IOException {
		// {"includes":["title","ghost"],"excludes":["name"]} — each array element is
		// rewritten name → id; the unknown "ghost" passes through unchanged.
		JsonNode dsl = parse("{\"includes\":[\"title\",\"ghost\"],\"excludes\":[\"name\"]}");

		// call under test
		SearchFieldRewriter.rewriteSourceFields(dsl, NAME_ONLY);

		assertEquals("100", dsl.get("includes").get(0).asText());
		assertEquals("ghost", dsl.get("includes").get(1).asText());
		assertEquals("101", dsl.get("excludes").get(0).asText());
	}

	// ---------- kindMapFor ----------

	@Test
	public void testKindMapForWithQueryRoutesTermThroughKeyword() {
		// call under test
		assertEquals(RoutingMode.KEYWORD_FOR_TEXT,
				SearchFieldRewriter.kindMapFor(Surface.QUERY).get("term"));
		assertEquals(RoutingMode.BARE, SearchFieldRewriter.kindMapFor(Surface.QUERY).get("match"));
	}

	@Test
	public void testKindMapForWithAggregationsRoutesTermsThroughKeyword() {
		// call under test
		assertEquals(RoutingMode.KEYWORD_FOR_TEXT,
				SearchFieldRewriter.kindMapFor(Surface.AGGREGATIONS).get("terms"));
	}

	@Test
	public void testKindMapForWithHighlightIsEmpty() {
		// call under test
		assertTrue(SearchFieldRewriter.kindMapFor(Surface.HIGHLIGHT).isEmpty());
	}

	@Test
	public void testKindMapForWithCollapseIsEmpty() {
		// call under test
		assertTrue(SearchFieldRewriter.kindMapFor(Surface.COLLAPSE).isEmpty());
	}

	// kindMapFor's default IllegalStateException is unreachable: Surface is exhausted by the
	// switch, and `switch(null)` NPEs before reaching the default. Left documented, not faked.

	// ---------- rewriteShorthandKey ----------

	@Test
	public void testRewriteShorthandKeyWithSingleKeyRewritten() throws IOException {
		ObjectNode obj = (ObjectNode) parse("{\"title\":\"x\"}");
		// call under test
		SearchFieldRewriter.rewriteShorthandKey(obj, NAME_ONLY, RoutingMode.BARE);
		assertEquals("x", obj.get("100").asText());
		assertFalse(obj.has("title"));
	}

	@Test
	public void testRewriteShorthandKeyWithMultipleKeysSkipped() throws IOException {
		// size != 1 → long-form, the helper leaves it for the leaf rule.
		ObjectNode obj = (ObjectNode) parse("{\"field\":\"title\",\"query\":\"x\"}");
		// call under test
		SearchFieldRewriter.rewriteShorthandKey(obj, NAME_ONLY, RoutingMode.BARE);
		assertEquals("title", obj.get("field").asText());
	}

	@Test
	public void testRewriteShorthandKeyWithFieldKeySkipped() throws IOException {
		// A single key that is the literal "field" marks long-form — skipped.
		ObjectNode obj = (ObjectNode) parse("{\"field\":\"title\"}");
		// call under test
		SearchFieldRewriter.rewriteShorthandKey(obj, NAME_ONLY, RoutingMode.BARE);
		assertEquals("title", obj.get("field").asText());
	}

	@Test
	public void testRewriteShorthandKeyWithUnchangedKeyLeftAlone() throws IOException {
		// Unknown name maps to itself → no swap.
		ObjectNode obj = (ObjectNode) parse("{\"ghost\":\"x\"}");
		// call under test
		SearchFieldRewriter.rewriteShorthandKey(obj, NAME_ONLY, RoutingMode.BARE);
		assertEquals("x", obj.get("ghost").asText());
	}

	// ---------- rewriteHighlightFieldsMap ----------

	@Test
	public void testRewriteHighlightFieldsMapWithKnownNameRewritten() throws IOException {
		ObjectNode obj = (ObjectNode) parse("{\"title\":{},\"name\":{}}");
		// call under test
		SearchFieldRewriter.rewriteHighlightFieldsMap(obj, NAME_ONLY);
		assertTrue(obj.has("100"));
		assertTrue(obj.has("101"));
		assertFalse(obj.has("title"));
	}

	@Test
	public void testRewriteHighlightFieldsMapWithUnknownNameUnchanged() throws IOException {
		ObjectNode obj = (ObjectNode) parse("{\"ghost\":{}}");
		// call under test
		SearchFieldRewriter.rewriteHighlightFieldsMap(obj, NAME_ONLY);
		assertTrue(obj.has("ghost"));
	}

	// ---------- rewriteSortObjectKeys ----------

	@Test
	public void testRewriteSortObjectKeysWithScoreSkipped() throws IOException {
		ObjectNode obj = (ObjectNode) parse("{\"_score\":\"desc\"}");
		// call under test
		SearchFieldRewriter.rewriteSortObjectKeys(obj, NAME_ONLY);
		assertTrue(obj.has("_score"));
	}

	@Test
	public void testRewriteSortObjectKeysWithKnownNameRewritten() throws IOException {
		ObjectNode obj = (ObjectNode) parse("{\"title\":{\"order\":\"asc\"}}");
		// call under test
		SearchFieldRewriter.rewriteSortObjectKeys(obj, NAME_ONLY);
		assertTrue(obj.has("100"));
		assertFalse(obj.has("title"));
	}

	@Test
	public void testRewriteSortObjectKeysWithUnknownNameUnchanged() throws IOException {
		ObjectNode obj = (ObjectNode) parse("{\"ghost\":\"asc\"}");
		// call under test
		SearchFieldRewriter.rewriteSortObjectKeys(obj, NAME_ONLY);
		assertEquals("asc", obj.get("ghost").asText());
	}

	// ---------- rewriteSourceArrayInPlace ----------

	@Test
	public void testRewriteSourceArrayInPlaceWithTextualRewritten() throws IOException {
		ArrayNode array = (ArrayNode) parse("[\"title\",\"name\"]");
		// call under test
		SearchFieldRewriter.rewriteSourceArrayInPlace(array, NAME_ONLY);
		assertEquals("100", array.get(0).asText());
		assertEquals("101", array.get(1).asText());
	}

	@Test
	public void testRewriteSourceArrayInPlaceWithNonTextualLeftAlone() throws IOException {
		// A non-textual element (number) is skipped, not rewritten.
		ArrayNode array = (ArrayNode) parse("[\"title\",5]");
		// call under test
		SearchFieldRewriter.rewriteSourceArrayInPlace(array, NAME_ONLY);
		assertEquals("100", array.get(0).asText());
		assertEquals(5, array.get(1).asInt());
	}

	// ---------- rewriteIdRefStrippingKeyword ----------

	@Test
	public void testRewriteIdRefStrippingKeywordWithNullReturnsNull() {
		// call under test
		assertNull(SearchFieldRewriter.rewriteIdRefStrippingKeyword(null, REVERSE));
	}

	@Test
	public void testRewriteIdRefStrippingKeywordWithKeywordAndBoostStripsKeyword() {
		// {id}.keyword^boost → {name}^boost
		// call under test
		assertEquals("title^2",
				SearchFieldRewriter.rewriteIdRefStrippingKeyword("100.keyword^2", REVERSE));
	}

	@Test
	public void testRewriteIdRefStrippingKeywordWithKeywordStripsToName() {
		// call under test
		assertEquals("count",
				SearchFieldRewriter.rewriteIdRefStrippingKeyword("102.keyword", REVERSE));
	}

	@Test
	public void testRewriteIdRefStrippingKeywordWithUnmappedIdPassesThrough() {
		// call under test
		assertEquals("999.keyword",
				SearchFieldRewriter.rewriteIdRefStrippingKeyword("999.keyword", REVERSE));
	}
}
