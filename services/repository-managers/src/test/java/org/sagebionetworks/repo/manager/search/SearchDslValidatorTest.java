package org.sagebionetworks.repo.manager.search;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.FieldSort;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.AggregationRange;
import org.opensearch.client.opensearch._types.aggregations.DateRangeExpression;
import org.opensearch.client.opensearch._types.aggregations.ExtendedBounds;
import org.opensearch.client.opensearch._types.aggregations.FieldDateMath;
import org.opensearch.client.opensearch._types.aggregations.TermsExclude;
import org.opensearch.client.opensearch._types.aggregations.TermsInclude;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.BoostingQuery;
import org.opensearch.client.opensearch._types.query_dsl.ConstantScoreQuery;
import org.opensearch.client.opensearch._types.query_dsl.DisMaxQuery;
import org.opensearch.client.opensearch._types.query_dsl.FuzzyQuery;
import org.opensearch.client.opensearch._types.query_dsl.MatchPhrasePrefixQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.TermsQueryField;
import org.opensearch.client.opensearch._types.query_dsl.TermsLookup;
import org.opensearch.client.opensearch._types.query_dsl.WildcardQuery;
import org.opensearch.client.opensearch.core.search.FieldCollapse;
import org.opensearch.client.opensearch.core.search.Highlight;
import org.opensearch.client.opensearch.core.search.HighlightField;
import org.opensearch.client.opensearch.core.search.HighlighterType;
import org.opensearch.client.opensearch.core.search.Rescore;
import org.opensearch.client.opensearch.core.search.RescoreQuery;

import com.fasterxml.jackson.databind.ObjectMapper;

public class SearchDslValidatorTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	// -----------------------------------------------------------------------------
	// Query: kind allowlist
	// -----------------------------------------------------------------------------

	@Test
	public void testValidateQueryWithMatchAll() {
		// call under test
		SearchDslValidator.validateQuery(Query.of(b -> b.matchAll(m -> m)), false);
	}

	@Test
	public void testValidateQueryWithBoolNested() {
		Query q = Query.of(b -> b.bool(bb -> bb
				.must(Query.of(m -> m.match(mq -> mq.field("foo").query(FieldValue.of("x")))))
				.filter(Query.of(m -> m.term(t -> t.field("bar").value(FieldValue.of("y")))))));
		SearchDslValidator.validateQuery(q, false);
	}

	// -----------------------------------------------------------------------------
	// Query: depth and clause-count caps
	// -----------------------------------------------------------------------------

	@Test
	public void testValidateQueryWithExcessiveDepth() {
		// Nest must clauses deeper than QUERY_MAX_DEPTH.
		Query inner = Query.of(b -> b.matchAll(m -> m));
		for (int i = 0; i < SearchDslValidator.QUERY_MAX_DEPTH + 2; i++) {
			Query nested = inner;
			inner = Query.of(b -> b.bool(bb -> bb.must(nested)));
		}
		Query deep = inner;
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateQuery(deep, false));
		assertTrue(ex.getMessage().contains("nested too deeply"));
	}

	@Test
	public void testValidateQueryWithExcessiveClauseCount() {
		List<Query> many = new ArrayList<>();
		for (int i = 0; i < SearchDslValidator.QUERY_MAX_CLAUSES + 2; i++) {
			many.add(Query.of(b -> b.matchAll(m -> m)));
		}
		Query q = Query.of(b -> b.bool(bb -> bb.must(many)));
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateQuery(q, false));
		assertTrue(ex.getMessage().contains("too many clauses"));
	}

	// -----------------------------------------------------------------------------
	// Query: compound-clause structural requirements
	// -----------------------------------------------------------------------------

	// dis_max / constant_score / boosting structural-required-slot checks are protected by
	// the typed builders themselves (each throws MissingRequiredPropertyException at
	// construction), so the validator's defensive null-check on those slots is unreachable
	// from typed callers. The validator still keeps the check for safety (a future schema
	// change to the OpenSearch client could relax the requirement); we don't try to fake
	// the construction path here.

	// -----------------------------------------------------------------------------
	// Query: per-kind custom rules
	// -----------------------------------------------------------------------------

	@Test
	public void testValidateQueryWithTermsLookupRejected() {
		Query q = Query.of(b -> b.terms(t -> t
				.field("foo")
				.terms(TermsQueryField.of(qf -> qf.lookup(TermsLookup.of(l -> l
						.index("other-index").id("1").path("p")))))));
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateQuery(q, false));
		assertTrue(ex.getMessage().contains("terms lookup form"));
	}

	@Test
	public void testValidateQueryWithTermsValuesAtCap() {
		List<FieldValue> values = new ArrayList<>();
		for (int i = 0; i <= SearchDslValidator.MAX_VALUES_PER_CLAUSE; i++) {
			values.add(FieldValue.of("v" + i));
		}
		Query q = Query.of(b -> b.terms(t -> t
				.field("foo")
				.terms(TermsQueryField.of(qf -> qf.value(values)))));
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateQuery(q, false));
		assertTrue(ex.getMessage().contains("terms array"));
	}

	@Test
	public void testValidateQueryWithMultiMatchFieldsAtCap() {
		List<String> fields = new ArrayList<>();
		for (int i = 0; i <= SearchDslValidator.MAX_VALUES_PER_CLAUSE; i++) {
			fields.add("f" + i);
		}
		Query q = Query.of(b -> b.multiMatch(mm -> mm.query("x").fields(fields)));
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateQuery(q, false));
		assertTrue(ex.getMessage().contains("multi_match.fields"));
	}

	@Test
	public void testValidateQueryWithMultiMatchExcessiveMaxExpansions() {
		Query q = Query.of(b -> b.multiMatch(mm -> mm.query("x").fields("f")
				.maxExpansions(SearchDslValidator.MAX_PREFIX_EXPANSIONS + 1)));
		assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateQuery(q, false));
	}

	@Test
	public void testValidateQueryWithFuzzyExcessiveMaxExpansions() {
		FuzzyQuery fq = FuzzyQuery.of(f -> f.field("foo").value(FieldValue.of("bar"))
				.maxExpansions(SearchDslValidator.MAX_PREFIX_EXPANSIONS + 1));
		Query q = Query.of(b -> b.fuzzy(fq));
		assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateQuery(q, false));
	}

	@Test
	public void testValidateQueryWithMatchPhrasePrefixExcessiveMaxExpansions() {
		MatchPhrasePrefixQuery mpp = MatchPhrasePrefixQuery.of(m -> m.field("foo").query("x")
				.maxExpansions(SearchDslValidator.MAX_PREFIX_EXPANSIONS + 1));
		Query q = Query.of(b -> b.matchPhrasePrefix(mpp));
		assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateQuery(q, false));
	}

	@Test
	public void testValidateQueryWithMatchBoolPrefixExcessiveMaxExpansions() {
		Query q = Query.of(b -> b.matchBoolPrefix(m -> m.field("foo").query("x")
				.maxExpansions(SearchDslValidator.MAX_PREFIX_EXPANSIONS + 1)));
		assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateQuery(q, false));
	}

	@Test
	public void testValidateQueryWithPrefixLeadingWildcard() {
		Query q = Query.of(b -> b.prefix(p -> p.field("foo").value("*bad")));
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateQuery(q, false));
		assertTrue(ex.getMessage().contains("leading wildcard"));
	}

	@Test
	public void testValidateQueryWithPrefixLeadingQuestionMark() {
		Query q = Query.of(b -> b.prefix(p -> p.field("foo").value("?bad")));
		assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateQuery(q, false));
	}

	@Test
	public void testValidateQueryWithWildcardLeadingWildcard() {
		Query q = Query.of(b -> b.wildcard(w -> w.field("foo").value("*ouch")));
		assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateQuery(q, false));
	}

	@Test
	public void testValidateQueryWithSimpleQueryStringFieldsAtCap() {
		List<String> fields = new ArrayList<>();
		for (int i = 0; i <= SearchDslValidator.MAX_VALUES_PER_CLAUSE; i++) {
			fields.add("f" + i);
		}
		Query q = Query.of(b -> b.simpleQueryString(s -> s.query("x").fields(fields)));
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateQuery(q, false));
		assertTrue(ex.getMessage().contains("simple_query_string.fields"));
	}

	@Test
	public void testValidateQueryWithSimpleQueryStringLeadingWildcardAndAnalyzeWildcard() {
		Query q = Query.of(b -> b.simpleQueryString(s -> s.query("*foo").analyzeWildcard(true)));
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateQuery(q, false));
		assertTrue(ex.getMessage().contains("leading wildcard"));
	}

	@Test
	public void testValidateQueryWithSimpleQueryStringLeadingWildcardWithoutAnalyze() {
		// Without analyze_wildcard the leading wildcard is just literal text, not expanded.
		Query q = Query.of(b -> b.simpleQueryString(s -> s.query("*foo")));
		// call under test — must not throw
		SearchDslValidator.validateQuery(q, false);
	}

	// -----------------------------------------------------------------------------
	// Autocomplete top-level narrowing
	// -----------------------------------------------------------------------------

	@Test
	public void testValidateAutocompleteWithMatchAllRejected() {
		Query q = Query.of(b -> b.matchAll(m -> m));
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateQuery(q, true));
		assertTrue(ex.getMessage().contains("autocomplete"));
	}

	@Test
	public void testValidateAutocompleteWithPrefixAccepted() {
		Query q = Query.of(b -> b.prefix(p -> p.field("foo").value("ba")));
		// call under test — must not throw
		SearchDslValidator.validateQuery(q, true);
	}

	@Test
	public void testValidateAutocompleteWithMatchBoolPrefixAccepted() {
		Query q = Query.of(b -> b.matchBoolPrefix(m -> m.field("foo").query("ba")));
		SearchDslValidator.validateQuery(q, true);
	}

	@Test
	public void testValidateAutocompleteCoversEveryAllowedTopLevelKind() {
		// Coverage guard: every kind in ALLOWED_AUTOCOMPLETE_TOP_LEVEL must be a member
		// of ALLOWED_QUERY_KINDS, otherwise the autocomplete path admits something the
		// general validator wouldn't.
		for (Query.Kind k : SearchDslValidator.ALLOWED_AUTOCOMPLETE_TOP_LEVEL) {
			assertTrue(SearchDslValidator.ALLOWED_QUERY_KINDS.contains(k),
					"autocomplete-allowed kind not in general allowlist: " + k);
		}
	}

	// -----------------------------------------------------------------------------
	// Aggregations
	// -----------------------------------------------------------------------------

	@Test
	public void testValidateAggregationsWithEveryAllowedKindRoundTrips() {
		// One-round-trip exhaustive coverage: validate a map containing one example of
		// every allowlisted aggregation kind. EnumSet.allOf-style guard at the bottom.
		Map<String, Aggregation> aggs = new LinkedHashMap<>();
		aggs.put("a_terms", Aggregation.of(b -> b.terms(t -> t.field("f"))));
		aggs.put("a_histogram", Aggregation.of(b -> b.histogram(h -> h.field("f").interval(1.0)
				.extendedBounds(ExtendedBounds.of(eb -> eb.min(0.0).max(10.0))))));
		aggs.put("a_date_histogram", Aggregation.of(b -> b.dateHistogram(h -> h.field("f")
				.calendarInterval(org.opensearch.client.opensearch._types.aggregations.CalendarInterval.Day)
				.hardBounds(ExtendedBounds.of(eb -> eb.min(org.opensearch.client.opensearch._types.aggregations.FieldDateMath.of(m -> m.expr("now-1d"))).max(org.opensearch.client.opensearch._types.aggregations.FieldDateMath.of(m -> m.expr("now"))))))));
		aggs.put("a_range", Aggregation.of(b -> b.range(r -> r.field("f")
				.ranges(AggregationRange.of(ar -> ar.from(JsonData.of(0.0)).to(JsonData.of(10.0)))))));
		aggs.put("a_date_range", Aggregation.of(b -> b.dateRange(r -> r.field("f"))));
		aggs.put("a_missing", Aggregation.of(b -> b.missing(m -> m.field("f"))));
		aggs.put("a_min", Aggregation.of(b -> b.min(m -> m.field("f"))));
		aggs.put("a_max", Aggregation.of(b -> b.max(m -> m.field("f"))));
		aggs.put("a_avg", Aggregation.of(b -> b.avg(m -> m.field("f"))));
		aggs.put("a_sum", Aggregation.of(b -> b.sum(m -> m.field("f"))));
		aggs.put("a_stats", Aggregation.of(b -> b.stats(m -> m.field("f"))));
		aggs.put("a_extended_stats", Aggregation.of(b -> b.extendedStats(m -> m.field("f"))));
		aggs.put("a_value_count", Aggregation.of(b -> b.valueCount(m -> m.field("f"))));
		aggs.put("a_cardinality", Aggregation.of(b -> b.cardinality(m -> m.field("f"))));
		aggs.put("a_filter", Aggregation.of(b -> b
				.filter(f -> f.term(t -> t.field("f").value(FieldValue.of("x"))))
				.aggregations("by_value", c -> c.terms(t -> t.field("f")))));
		aggs.put("a_filters", Aggregation.of(b -> b
				.filters(fs -> fs.filters(bk -> bk.keyed(Map.of(
						"hits", Query.of(q -> q.term(t -> t.field("f").value(FieldValue.of("x")))),
						"misses", Query.of(q -> q.term(t -> t.field("f").value(FieldValue.of("y"))))))))
				.aggregations("by_value", c -> c.terms(t -> t.field("f")))));
		// call under test
		SearchDslValidator.validateAggregations(aggs);

		// Coverage guard:
		EnumSet<Aggregation.Kind> covered = EnumSet.noneOf(Aggregation.Kind.class);
		for (Aggregation a : aggs.values()) {
			covered.add(a._kind());
		}
		assertEquals(SearchDslValidator.ALLOWED_AGGREGATION_KINDS, covered,
				"every allowlisted aggregation kind must appear in this round-trip");
	}

	@Test
	public void testValidateAggregationsWithTermsSizeAtCap() {
		Map<String, Aggregation> aggs = new LinkedHashMap<>();
		aggs.put("a", Aggregation.of(b -> b.terms(t -> t.field("f")
				.size(SearchDslValidator.MAX_AGG_SIZE + 1))));
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateAggregations(aggs));
		assertTrue(ex.getMessage().contains("'terms' aggregation 'size'"));
	}

	@Test
	public void testValidateAggregationsWithTermsShardSizeAtCap() {
		Map<String, Aggregation> aggs = new LinkedHashMap<>();
		aggs.put("a", Aggregation.of(b -> b.terms(t -> t.field("f")
				.shardSize(SearchDslValidator.MAX_AGG_SIZE + 1))));
		assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateAggregations(aggs));
	}

	@Test
	public void testValidateAggregationsWithRangeRangesAtCap() {
		List<AggregationRange> ranges = new ArrayList<>();
		for (int i = 0; i <= SearchDslValidator.MAX_VALUES_PER_CLAUSE; i++) {
			final double idx = i;
			ranges.add(AggregationRange.of(ar -> ar.from(JsonData.of(idx)).to(JsonData.of(idx + 1.0))));
		}
		Map<String, Aggregation> aggs = new LinkedHashMap<>();
		aggs.put("a", Aggregation.of(b -> b.range(r -> r.field("f").ranges(ranges))));
		assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateAggregations(aggs));
	}

	@Test
	public void testValidateAggregationsWithHistogramRequiresBounds() {
		Map<String, Aggregation> aggs = new LinkedHashMap<>();
		aggs.put("a", Aggregation.of(b -> b.histogram(h -> h.field("f").interval(1.0))));
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateAggregations(aggs));
		assertTrue(ex.getMessage().contains("extended_bounds"));
	}

	@Test
	public void testValidateAggregationsWithHistogramNonPositiveInterval() {
		Map<String, Aggregation> aggs = new LinkedHashMap<>();
		aggs.put("a", Aggregation.of(b -> b.histogram(h -> h.field("f").interval(0.0)
				.extendedBounds(ExtendedBounds.of(eb -> eb.min(0.0).max(10.0))))));
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateAggregations(aggs));
		assertTrue(ex.getMessage().contains("interval must be positive"));
	}

	@Test
	public void testValidateAggregationsWithDateHistogramRequiresBounds() {
		Map<String, Aggregation> aggs = new LinkedHashMap<>();
		aggs.put("a", Aggregation.of(b -> b.dateHistogram(h -> h.field("f"))));
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateAggregations(aggs));
		assertTrue(ex.getMessage().contains("extended_bounds"));
	}

	@Test
	public void testValidateAggregationsWithCardinalityExcessivePrecision() {
		Map<String, Aggregation> aggs = new LinkedHashMap<>();
		aggs.put("a", Aggregation.of(b -> b.cardinality(c -> c.field("f")
				.precisionThreshold(SearchDslValidator.MAX_PRECISION_THRESHOLD + 1))));
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateAggregations(aggs));
		assertTrue(ex.getMessage().contains("precision_threshold"));
	}

	@Test
	public void testValidateAggregationsWithExcessiveCount() {
		Map<String, Aggregation> aggs = new LinkedHashMap<>();
		for (int i = 0; i <= SearchDslValidator.AGG_MAX_COUNT; i++) {
			aggs.put("a" + i, Aggregation.of(b -> b.terms(t -> t.field("f"))));
		}
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateAggregations(aggs));
		assertTrue(ex.getMessage().contains("too many aggregations"));
	}

	@Test
	public void testValidateAggregationsWithExcessiveDepth() {
		// Build sub-aggregations nested past AGG_MAX_DEPTH.
		Aggregation deepest = Aggregation.of(b -> b.terms(t -> t.field("f")));
		for (int i = 0; i < SearchDslValidator.AGG_MAX_DEPTH + 2; i++) {
			final Aggregation inner = deepest;
			deepest = Aggregation.of(b -> b.terms(t -> t.field("f"))
					.aggregations("sub", inner));
		}
		Map<String, Aggregation> aggs = new LinkedHashMap<>();
		aggs.put("root", deepest);
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateAggregations(aggs));
		assertTrue(ex.getMessage().contains("nested too deeply"));
	}

	@Test
	public void testValidateAggregationsWithFilterValid() {
		// A well-formed single-bucket filter aggregation (a valid query body + a child agg) passes,
		// and the filter body is routed through walkQuery — the same path a top-level query takes.
		Query filterBody = Query.of(q -> q.term(t -> t.field("f").value(FieldValue.of("x"))));
		Map<String, Aggregation> aggs = new LinkedHashMap<>();
		aggs.put("a", Aggregation.of(b -> b
				.filter(filterBody)
				.aggregations("by_value", c -> c.terms(t -> t.field("f")))));

		try (MockedStatic<SearchDslValidator> mocked =
				mockStatic(SearchDslValidator.class, CALLS_REAL_METHODS)) {
			// call under test
			SearchDslValidator.validateAggregations(aggs);
			// The filter body reaches the shared query-validation path exactly once.
			mocked.verify(() -> SearchDslValidator.walkQuery(eq(filterBody), anyInt(), any()), times(1));
		}
	}

	@Test
	public void testValidateAggregationsWithFilterCrossIndexTermsLookupRejected() {
		// A cross-index terms lookup smuggled inside a filter body is rejected exactly as it is at
		// the top level.
		Map<String, Aggregation> aggs = new LinkedHashMap<>();
		aggs.put("a", Aggregation.of(b -> b
				.filter(f -> f.terms(t -> t.field("foo").terms(TermsQueryField.of(qf -> qf
						.lookup(TermsLookup.of(l -> l.index("other-index").id("1").path("p")))))))
				.aggregations("by_value", c -> c.terms(t -> t.field("f")))));
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateAggregations(aggs));
		assertTrue(ex.getMessage().contains("terms lookup form"));
	}

	@Test
	public void testValidateAggregationsWithFilterExcessiveClauseCount() {
		// A filter body's clauses count against the shared QUERY_MAX_CLAUSES budget, so it cannot
		// smuggle an unbounded number of clauses past the cap.
		List<Query> many = new ArrayList<>();
		for (int i = 0; i < SearchDslValidator.QUERY_MAX_CLAUSES + 2; i++) {
			many.add(Query.of(b -> b.matchAll(m -> m)));
		}
		Map<String, Aggregation> aggs = new LinkedHashMap<>();
		aggs.put("a", Aggregation.of(b -> b
				.filter(f -> f.bool(bb -> bb.must(many)))
				.aggregations("by_value", c -> c.terms(t -> t.field("f")))));
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateAggregations(aggs));
		assertTrue(ex.getMessage().contains("too many clauses"));
	}

	@Test
	public void testValidateAggregationsWithFiltersKeyedValid() {
		// A well-formed keyed filters aggregation passes, and every named bucket query is routed
		// through walkQuery.
		Query hits = Query.of(q -> q.term(t -> t.field("f").value(FieldValue.of("x"))));
		Query misses = Query.of(q -> q.term(t -> t.field("f").value(FieldValue.of("y"))));
		Map<String, Aggregation> aggs = new LinkedHashMap<>();
		aggs.put("a", Aggregation.of(b -> b
				.filters(fs -> fs.filters(bk -> bk.keyed(Map.of("hits", hits, "misses", misses))))
				.aggregations("by_value", c -> c.terms(t -> t.field("f")))));

		try (MockedStatic<SearchDslValidator> mocked =
				mockStatic(SearchDslValidator.class, CALLS_REAL_METHODS)) {
			// call under test
			SearchDslValidator.validateAggregations(aggs);
			mocked.verify(() -> SearchDslValidator.walkQuery(eq(hits), anyInt(), any()), times(1));
			mocked.verify(() -> SearchDslValidator.walkQuery(eq(misses), anyInt(), any()), times(1));
		}
	}

	@Test
	public void testValidateAggregationsWithFiltersArrayValid() {
		// The array (non-keyed) form is also accepted, and each query is routed through walkQuery.
		Query first = Query.of(q -> q.term(t -> t.field("f").value(FieldValue.of("x"))));
		Query second = Query.of(q -> q.term(t -> t.field("f").value(FieldValue.of("y"))));
		Map<String, Aggregation> aggs = new LinkedHashMap<>();
		aggs.put("a", Aggregation.of(b -> b
				.filters(fs -> fs.filters(bk -> bk.array(List.of(first, second))))
				.aggregations("by_value", c -> c.terms(t -> t.field("f")))));

		try (MockedStatic<SearchDslValidator> mocked =
				mockStatic(SearchDslValidator.class, CALLS_REAL_METHODS)) {
			// call under test
			SearchDslValidator.validateAggregations(aggs);
			mocked.verify(() -> SearchDslValidator.walkQuery(eq(first), anyInt(), any()), times(1));
			mocked.verify(() -> SearchDslValidator.walkQuery(eq(second), anyInt(), any()), times(1));
		}
	}

	@Test
	public void testValidateAggregationsWithFiltersKeyedCrossIndexTermsLookupRejected() {
		// Each named query in a keyed filters bucket is validated identically to the top-level query.
		Map<String, Aggregation> aggs = new LinkedHashMap<>();
		aggs.put("a", Aggregation.of(b -> b
				.filters(fs -> fs.filters(bk -> bk.keyed(Map.of(
						"ok", Query.of(q -> q.term(t -> t.field("f").value(FieldValue.of("x")))),
						"bad", Query.of(q -> q.terms(t -> t.field("foo").terms(TermsQueryField.of(qf -> qf
								.lookup(TermsLookup.of(l -> l.index("other-index").id("1").path("p")))))))))))
				.aggregations("by_value", c -> c.terms(t -> t.field("f")))));
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateAggregations(aggs));
		assertTrue(ex.getMessage().contains("terms lookup form"));
	}

	@Test
	public void testValidateAggregationsWithFiltersArrayCrossIndexTermsLookupRejected() {
		// The array (non-keyed) form is validated the same way as the keyed form.
		Map<String, Aggregation> aggs = new LinkedHashMap<>();
		aggs.put("a", Aggregation.of(b -> b
				.filters(fs -> fs.filters(bk -> bk.array(List.of(
						Query.of(q -> q.term(t -> t.field("f").value(FieldValue.of("x")))),
						Query.of(q -> q.terms(t -> t.field("foo").terms(TermsQueryField.of(qf -> qf
								.lookup(TermsLookup.of(l -> l.index("other-index").id("1").path("p")))))))))))
				.aggregations("by_value", c -> c.terms(t -> t.field("f")))));
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateAggregations(aggs));
		assertTrue(ex.getMessage().contains("terms lookup form"));
	}

	// -----------------------------------------------------------------------------
	// Null arguments
	// -----------------------------------------------------------------------------

	@Test
	public void testValidateQueryWithNull() {
		assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateQuery(null, false));
	}

	@Test
	public void testValidateAggregationsWithNull() {
		assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateAggregations(null));
	}

	// -----------------------------------------------------------------------------
	// Highlight
	// -----------------------------------------------------------------------------

	@Test
	public void testValidateHighlightWithEmptyFieldsRoundTrips() {
		Highlight h = Highlight.of(b -> b.fields(new LinkedHashMap<>()));
		// call under test
		SearchDslValidator.validateHighlight(h);
	}

	@Test
	public void testValidateHighlightWithSingleFieldRoundTrips() {
		Map<String, HighlightField> fields = new LinkedHashMap<>();
		fields.put("title", HighlightField.of(b -> b));
		Highlight h = Highlight.of(b -> b.fields(fields));
		// call under test
		SearchDslValidator.validateHighlight(h);
	}

	@Test
	public void testValidateHighlightWithSemanticBuiltinTypeRejected() {
		// `semantic` is not in the built-in enum (only Unified / Plain / FastVector), so
		// callers can only land it as a custom type string.
		HighlighterType semanticType = HighlighterType.of(t -> t.custom("semantic"));
		Highlight h = Highlight.of(b -> b.type(semanticType).fields(new LinkedHashMap<>()));
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateHighlight(h));
		assertTrue(ex.getMessage().contains("semantic"));
	}

	@Test
	public void testValidateHighlightWithSemanticPerFieldTypeRejected() {
		HighlighterType semanticType = HighlighterType.of(t -> t.custom("semantic"));
		Map<String, HighlightField> fields = new LinkedHashMap<>();
		fields.put("title", HighlightField.of(b -> b.type(semanticType)));
		Highlight h = Highlight.of(b -> b.fields(fields));
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateHighlight(h));
		assertTrue(ex.getMessage().contains("semantic"));
	}

	@Test
	public void testValidateHighlightWithExcessiveFieldsRejected() {
		Map<String, HighlightField> fields = new LinkedHashMap<>();
		for (int i = 0; i <= SearchDslValidator.MAX_HIGHLIGHT_FIELDS; i++) {
			fields.put("f" + i, HighlightField.of(b -> b));
		}
		Highlight h = Highlight.of(b -> b.fields(fields));
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateHighlight(h));
		assertTrue(ex.getMessage().contains("highlight.fields"));
	}

	@Test
	public void testValidateHighlightWithExcessiveNumberOfFragmentsRejected() {
		Map<String, HighlightField> fields = new LinkedHashMap<>();
		fields.put("title", HighlightField.of(b -> b
				.numberOfFragments(SearchDslValidator.MAX_HIGHLIGHT_FRAGMENTS + 1)));
		Highlight h = Highlight.of(b -> b.fields(fields));
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateHighlight(h));
		assertTrue(ex.getMessage().contains("number_of_fragments"));
	}

	@Test
	public void testValidateHighlightWithExcessiveFragmentSizeRejected() {
		Map<String, HighlightField> fields = new LinkedHashMap<>();
		fields.put("title", HighlightField.of(b -> b
				.fragmentSize(SearchDslValidator.MAX_HIGHLIGHT_FRAGMENT_SIZE + 1)));
		Highlight h = Highlight.of(b -> b.fields(fields));
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateHighlight(h));
		assertTrue(ex.getMessage().contains("fragment_size"));
	}

	@Test
	public void testValidateHighlightWithTopLevelFragmentCapsApplied() {
		// Top-level Highlight numberOfFragments / fragmentSize are validated too — the
		// AOSS defaults cascade to fields without an override.
		Highlight h = Highlight.of(b -> b
				.fragmentSize(SearchDslValidator.MAX_HIGHLIGHT_FRAGMENT_SIZE + 1)
				.fields(new LinkedHashMap<>()));
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateHighlight(h));
		assertTrue(ex.getMessage().contains("fragment_size"));
	}

	@Test
	public void testValidateHighlightWithNull() {
		assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateHighlight(null));
	}

	// -----------------------------------------------------------------------------
	// FieldCollapse
	// -----------------------------------------------------------------------------

	@Test
	public void testValidateFieldCollapseWithFieldOnlyAccepted() {
		FieldCollapse c = FieldCollapse.of(b -> b.field("100"));
		// call under test
		SearchDslValidator.validateFieldCollapse(c);
	}

	@Test
	public void testValidateFieldCollapseWithExcessiveConcurrentGroupSearchesRejected() {
		FieldCollapse c = FieldCollapse.of(b -> b.field("100")
				.maxConcurrentGroupSearches(99));
		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateFieldCollapse(c));
		assertTrue(ex.getMessage().contains("max_concurrent_group_searches"));
	}

	// -----------------------------------------------------------------------------
	// Rescore
	// -----------------------------------------------------------------------------

	@Test
	public void testValidateRescoreWithAllowedInnerQueryAccepted() {
		Rescore r = Rescore.of(b -> b.windowSize(50)
				.query(RescoreQuery.of(rq -> rq.rescoreQuery(
						Query.of(q -> q.matchAll(m -> m))))));
		// call under test
		SearchDslValidator.validateRescore(r);
	}

	@Test
	public void testValidateRescoreWithExcessiveWindowSizeRejected() {
		Rescore r = Rescore.of(b -> b.windowSize(5000)
				.query(RescoreQuery.of(rq -> rq.rescoreQuery(
						Query.of(q -> q.matchAll(m -> m))))));
		// call under test
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateRescore(r));
		assertTrue(ex.getMessage().contains("window_size"));
	}

	@Test
	public void testValidateRescoreWithNull() {
		assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateRescore(null));
	}

	@Test
	public void testValidateFieldCollapseWithNull() {
		assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateFieldCollapse(null));
	}

	// -----------------------------------------------------------------------------
	// Query allowlist coverage guard
	// -----------------------------------------------------------------------------

	/**
	 * Single round trip exercising every kind in
	 * {@link SearchDslValidator#ALLOWED_QUERY_KINDS}. EnumSet.allOf coverage guard at the
	 * bottom — adding a new allowlisted kind here without a fixture fails the test.
	 *
	 * <p>Compounds wrap leaves so the walker recurses into every kind in one validation.</p>
	 */
	@Test
	public void testValidateQueryWithEveryAllowedKindRoundTrips() {
		List<Query> leaves = new ArrayList<>();
		leaves.add(Query.of(b -> b.match(m -> m.field("f").query(FieldValue.of("x")))));
		leaves.add(Query.of(b -> b.multiMatch(mm -> mm.query("x").fields("f"))));
		leaves.add(Query.of(b -> b.matchPhrase(mp -> mp.field("f").query("x"))));
		leaves.add(Query.of(b -> b.matchPhrasePrefix(mpp -> mpp.field("f").query("x"))));
		leaves.add(Query.of(b -> b.matchBoolPrefix(mbp -> mbp.field("f").query("x"))));
		leaves.add(Query.of(b -> b.term(t -> t.field("f").value(FieldValue.of("x")))));
		leaves.add(Query.of(b -> b.terms(t -> t.field("f").terms(TermsQueryField.of(qf ->
				qf.value(List.of(FieldValue.of("x"))))))));
		leaves.add(Query.of(b -> b.range(r -> r.field("f").gte(JsonData.of(0)))));
		leaves.add(Query.of(b -> b.exists(e -> e.field("f"))));
		leaves.add(Query.of(b -> b.prefix(p -> p.field("f").value("x"))));
		leaves.add(Query.of(b -> b.wildcard(w -> w.field("f").value("x"))));
		leaves.add(Query.of(b -> b.fuzzy(f -> f.field("f").value(FieldValue.of("x")))));
		leaves.add(Query.of(b -> b.simpleQueryString(s -> s.query("x"))));
		leaves.add(Query.of(b -> b.matchAll(m -> m)));

		// Compounds wrap a different leaf each so all four compound branches run.
		List<Query> all = new ArrayList<>(leaves);
		all.add(Query.of(b -> b.bool(bb -> bb.must(leaves.get(0)).filter(leaves.get(5)))));
		all.add(Query.of(b -> b.disMax(dm -> dm.queries(leaves.get(1)))));
		all.add(Query.of(b -> b.constantScore(cs -> cs.filter(leaves.get(8)))));
		all.add(Query.of(b -> b.boosting(bo -> bo.positive(leaves.get(0)).negative(leaves.get(12))
				.negativeBoost(0.5f))));

		EnumSet<Query.Kind> covered = EnumSet.noneOf(Query.Kind.class);
		for (Query q : all) {
			// call under test — every kind must validate without throwing
			SearchDslValidator.validateQuery(q, false);
			covered.add(q._kind());
		}

		assertEquals(SearchDslValidator.ALLOWED_QUERY_KINDS, covered,
				"every allowlisted query kind must appear in this round-trip");
	}

	// -----------------------------------------------------------------------------
	// Aggregation recursion (positive case complementing the depth-cap rejection)
	// -----------------------------------------------------------------------------

	@Test
	public void testValidateAggregationsWithSubAggregationsRecursesPositive() {
		Aggregation child = Aggregation.of(b -> b.terms(t -> t.field("f")));
		Aggregation parent = Aggregation.of(b -> b.terms(t -> t.field("f"))
				.aggregations("child", child));
		Map<String, Aggregation> aggs = new LinkedHashMap<>();
		aggs.put("parent", parent);
		// call under test — recursion descends without throwing.
		SearchDslValidator.validateAggregations(aggs);
	}

	// -----------------------------------------------------------------------------
	// Terms: null TermsQueryField early return
	// -----------------------------------------------------------------------------

	// validateTerms's null TermsQueryField early return is unreachable from typed callers —
	// the typed TermsQuery builder requires a TermsQueryField at construction time. The
	// validator keeps the defensive null check.

	@Test
	public void testValidateTermsAggregationWithIncludeRegexLeftAlone() {
		// Regex-string include passes through (only inline-list form is capped).
		Map<String, Aggregation> aggs = new LinkedHashMap<>();
		aggs.put("a", Aggregation.of(b -> b.terms(t -> t.field("f")
				.include(TermsInclude.of(ti -> ti.regexp("^a.*"))))));
		// call under test — must not throw
		SearchDslValidator.validateAggregations(aggs);
	}

	@Test
	public void testValidateTermsAggregationWithExcludeListAtCap() {
		List<String> tooMany = new ArrayList<>();
		for (int i = 0; i <= SearchDslValidator.MAX_VALUES_PER_CLAUSE; i++) {
			tooMany.add("v" + i);
		}
		Map<String, Aggregation> aggs = new LinkedHashMap<>();
		aggs.put("a", Aggregation.of(b -> b.terms(t -> t.field("f")
				.exclude(TermsExclude.of(te -> te.terms(tooMany))))));
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateAggregations(aggs));
		assertTrue(ex.getMessage().contains("'exclude'"));
	}

	@Test
	public void testValidateDateRangeAggregationWithRangesAtCap() {
		List<DateRangeExpression> ranges = new ArrayList<>();
		for (int i = 0; i <= SearchDslValidator.MAX_VALUES_PER_CLAUSE; i++) {
			ranges.add(DateRangeExpression.of(
					dr -> dr.from(FieldDateMath.of(m -> m.expr("now-1d")))));
		}
		Map<String, Aggregation> aggs = new LinkedHashMap<>();
		aggs.put("a", Aggregation.of(b -> b.dateRange(r -> r.field("f").ranges(ranges))));
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateAggregations(aggs));
		assertTrue(ex.getMessage().contains("'date_range'"));
	}

	// -----------------------------------------------------------------------------
	// Highlight: nested highlight_query is recursively walked against the allowlist
	// -----------------------------------------------------------------------------

	@Test
	public void testValidateHighlightWithTopLevelAllowedHighlightQueryAccepted() {
		// An allowlisted top-level highlight_query must pass the recursive walkQuery.
		Highlight h = Highlight.of(b -> b
				.highlightQuery(Query.of(q -> q.matchAll(m -> m)))
				.fields(new LinkedHashMap<>()));
		// call under test
		assertDoesNotThrow(() -> SearchDslValidator.validateHighlight(h));
	}

	@Test
	public void testValidateHighlightWithPerFieldAllowedHighlightQueryAccepted() {
		// An allowlisted per-field highlight_query must pass the recursive walkQuery.
		Map<String, HighlightField> fields = new LinkedHashMap<>();
		fields.put("title", HighlightField.of(b -> b
				.highlightQuery(Query.of(q -> q.matchAll(m -> m)))));
		Highlight h = Highlight.of(b -> b.fields(fields));
		// call under test
		assertDoesNotThrow(() -> SearchDslValidator.validateHighlight(h));
	}

	@Test
	public void testValidateHighlightWithCustomNonSemanticTypeAccepted() {
		// A custom highlighter type that isn't `semantic` is allowed — exercises the
		// isCustom() branch of rejectSemanticType that returns without throwing.
		HighlighterType fvh = HighlighterType.of(t -> t.custom("fvh"));
		Highlight h = Highlight.of(b -> b.type(fvh).fields(new LinkedHashMap<>()));
		// call under test
		assertDoesNotThrow(() -> SearchDslValidator.validateHighlight(h));
	}

	// -----------------------------------------------------------------------------
	// prefix: empty value is accepted (exercises the pattern.isEmpty() early-out)
	// -----------------------------------------------------------------------------

	@Test
	public void testValidateQueryWithPrefixEmptyValueAccepted() {
		Query q = Query.of(b -> b.prefix(p -> p.field("foo").value("")));
		// call under test — empty value short-circuits the leading-wildcard check
		assertDoesNotThrow(() -> SearchDslValidator.validateQuery(q, false));
	}

	// -----------------------------------------------------------------------------
	// terms aggregation: inline include-list form
	// -----------------------------------------------------------------------------

	@Test
	public void testValidateTermsAggregationWithIncludeListUnderCapAccepted() {
		Map<String, Aggregation> aggs = new LinkedHashMap<>();
		aggs.put("a", Aggregation.of(b -> b.terms(t -> t.field("f")
				.include(TermsInclude.of(ti -> ti.terms(List.of("a", "b")))))));
		// call under test — under cap, must not throw
		assertDoesNotThrow(() -> SearchDslValidator.validateAggregations(aggs));
	}

	@Test
	public void testValidateTermsAggregationWithIncludeListAtCap() {
		List<String> tooMany = new ArrayList<>();
		for (int i = 0; i <= SearchDslValidator.MAX_VALUES_PER_CLAUSE; i++) {
			tooMany.add("v" + i);
		}
		Map<String, Aggregation> aggs = new LinkedHashMap<>();
		aggs.put("a", Aggregation.of(b -> b.terms(t -> t.field("f")
				.include(TermsInclude.of(ti -> ti.terms(tooMany))))));
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateAggregations(aggs));
		assertTrue(ex.getMessage().contains("'include'"));
	}

	// ---------- walkQuery ----------

	@Test
	public void testWalkQueryWithDepthOverLimitRejected() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> SearchDslValidator.walkQuery(Query.of(b -> b.matchAll(m -> m)),
						SearchDslValidator.QUERY_MAX_DEPTH + 1, new int[] { 0 }));
		assertTrue(ex.getMessage().contains("nested too deeply"));
	}

	@Test
	public void testWalkQueryWithClauseCountOverLimitRejected() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> SearchDslValidator.walkQuery(Query.of(b -> b.matchAll(m -> m)), 1,
						new int[] { SearchDslValidator.QUERY_MAX_CLAUSES }));
		assertTrue(ex.getMessage().contains("too many clauses"));
	}

	// ---------- walkDisMax ----------

	@Test
	public void testWalkDisMaxWithEmptyQueriesRejected() {
		// The typed DisMaxQuery builder requires `queries` at construction; an empty list here
		// exercises the helper's guard directly.
		DisMaxQuery empty = DisMaxQuery.of(d -> d.queries(new ArrayList<>()));
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> SearchDslValidator.walkDisMax(empty, 1, new int[] { 0 }));
		assertTrue(ex.getMessage().contains("requires a 'queries' field"));
	}

	@Test
	public void testWalkDisMaxWithQueriesRecurses() {
		DisMaxQuery dm = DisMaxQuery.of(d -> d.queries(Query.of(b -> b.matchAll(m -> m))));
		// call under test — must not throw
		SearchDslValidator.walkDisMax(dm, 1, new int[] { 0 });
	}

	// ---------- walkQueryList ----------

	@Test
	public void testWalkQueryListWithNullReturns() {
		// BoolQuery accessors never return null; passing null here exercises the guard directly.
		// call under test — must not throw
		SearchDslValidator.walkQueryList(null, 1, new int[] { 0 });
	}

	@Test
	public void testWalkQueryListWithElementsRecurses() {
		// call under test — must not throw
		SearchDslValidator.walkQueryList(List.of(Query.of(b -> b.matchAll(m -> m))), 1,
				new int[] { 0 });
	}

	// ---------- walkBool / walkConstantScore / walkBoosting ----------

	@Test
	public void testWalkBoolWithEverySlotRecurses() {
		BoolQuery bool = BoolQuery.of(b -> b
				.must(Query.of(q -> q.matchAll(m -> m)))
				.should(Query.of(q -> q.matchAll(m -> m)))
				.mustNot(Query.of(q -> q.matchAll(m -> m)))
				.filter(Query.of(q -> q.matchAll(m -> m))));
		// call under test — must not throw
		SearchDslValidator.walkBool(bool, 1, new int[] { 0 });
	}

	@Test
	public void testWalkConstantScoreWithFilterRecurses() {
		ConstantScoreQuery cs = ConstantScoreQuery.of(c -> c
				.filter(Query.of(q -> q.matchAll(m -> m))));
		// call under test — must not throw
		SearchDslValidator.walkConstantScore(cs, 1, new int[] { 0 });
	}

	@Test
	public void testWalkBoostingWithBothSlotsRecurses() {
		BoostingQuery bo = BoostingQuery.of(b -> b
				.positive(Query.of(q -> q.matchAll(m -> m)))
				.negative(Query.of(q -> q.matchAll(m -> m)))
				.negativeBoost(0.5f));
		// call under test — must not throw
		SearchDslValidator.walkBoosting(bo, 1, new int[] { 0 });
	}

	// ---------- rejectLeadingWildcardWildcard ----------

	@Test
	public void testRejectLeadingWildcardWildcardWithValueLeadingRejected() {
		WildcardQuery w = WildcardQuery.of(b -> b.field("foo").value("*bad"));
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> SearchDslValidator.rejectLeadingWildcardWildcard(w));
		assertTrue(ex.getMessage().contains("leading wildcard"));
	}

	@Test
	public void testRejectLeadingWildcardWildcardWithWildcardPropertyFallbackRejected() {
		// `value()` is null, so the helper falls back to the legacy `wildcard` property.
		WildcardQuery w = WildcardQuery.of(b -> b.field("foo").wildcard("*bad"));
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> SearchDslValidator.rejectLeadingWildcardWildcard(w));
		assertTrue(ex.getMessage().contains("leading wildcard"));
	}

	@Test
	public void testRejectLeadingWildcardWildcardWithNonLeadingAccepted() {
		WildcardQuery w = WildcardQuery.of(b -> b.field("foo").value("ok*"));
		// call under test — must not throw
		SearchDslValidator.rejectLeadingWildcardWildcard(w);
	}

	// ---------- rejectLeadingWildcard ----------

	@Test
	public void testRejectLeadingWildcardWithNullAccepted() {
		// call under test — must not throw
		SearchDslValidator.rejectLeadingWildcard(null, "prefix", "foo");
	}

	@Test
	public void testRejectLeadingWildcardWithEmptyAccepted() {
		// call under test — must not throw
		SearchDslValidator.rejectLeadingWildcard("", "prefix", "foo");
	}

	@Test
	public void testRejectLeadingWildcardWithStarRejected() {
		assertThrows(IllegalArgumentException.class,
				// call under test
				() -> SearchDslValidator.rejectLeadingWildcard("*x", "prefix", "foo"));
	}

	@Test
	public void testRejectLeadingWildcardWithQuestionMarkRejected() {
		assertThrows(IllegalArgumentException.class,
				// call under test
				() -> SearchDslValidator.rejectLeadingWildcard("?x", "prefix", "foo"));
	}

	@Test
	public void testRejectLeadingWildcardWithPlainPrefixAccepted() {
		// call under test — must not throw
		SearchDslValidator.rejectLeadingWildcard("abc", "prefix", "foo");
	}

	// ---------- checkMaxExpansions ----------

	@Test
	public void testCheckMaxExpansionsWithNullAccepted() {
		// call under test — must not throw
		SearchDslValidator.checkMaxExpansions(null, "fuzzy");
	}

	@Test
	public void testCheckMaxExpansionsWithAtCapAccepted() {
		// call under test — must not throw
		SearchDslValidator.checkMaxExpansions(SearchDslValidator.MAX_PREFIX_EXPANSIONS, "fuzzy");
	}

	@Test
	public void testCheckMaxExpansionsWithOverCapRejected() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> SearchDslValidator.checkMaxExpansions(
						SearchDslValidator.MAX_PREFIX_EXPANSIONS + 1, "fuzzy"));
		assertTrue(ex.getMessage().contains("max_expansions"));
	}

	// ---------- rejectSemanticType ----------

	@Test
	public void testRejectSemanticTypeWithNullAccepted() {
		// call under test — must not throw
		SearchDslValidator.rejectSemanticType(null, "highlight.type");
	}

	@Test
	public void testRejectSemanticTypeWithBuiltinNonSemanticAccepted() {
		// Built-in highlighter types (Unified/Plain/FastVector) are never `semantic`.
		HighlighterType builtin = HighlighterType.of(t -> t.builtin(
				org.opensearch.client.opensearch.core.search.BuiltinHighlighterType.Unified));
		// call under test — must not throw
		SearchDslValidator.rejectSemanticType(builtin, "highlight.type");
	}

	@Test
	public void testRejectSemanticTypeWithCustomNonSemanticAccepted() {
		// call under test — must not throw
		SearchDslValidator.rejectSemanticType(HighlighterType.of(t -> t.custom("fvh")),
				"highlight.type");
	}

	@Test
	public void testRejectSemanticTypeWithCustomSemanticRejected() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> SearchDslValidator.rejectSemanticType(HighlighterType.of(t -> t.custom("semantic")),
						"highlight.type"));
		assertTrue(ex.getMessage().contains("semantic"));
	}

	// ---------- checkHighlightCaps ----------

	@Test
	public void testCheckHighlightCapsWithBothWithinAccepted() {
		// call under test — must not throw
		SearchDslValidator.checkHighlightCaps(5, 100, "highlight");
	}

	@Test
	public void testCheckHighlightCapsWithExcessiveFragmentsRejected() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> SearchDslValidator.checkHighlightCaps(
						SearchDslValidator.MAX_HIGHLIGHT_FRAGMENTS + 1, null, "highlight"));
		assertTrue(ex.getMessage().contains("number_of_fragments"));
	}

	@Test
	public void testCheckHighlightCapsWithExcessiveFragmentSizeRejected() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> SearchDslValidator.checkHighlightCaps(null,
						SearchDslValidator.MAX_HIGHLIGHT_FRAGMENT_SIZE + 1, "highlight"));
		assertTrue(ex.getMessage().contains("fragment_size"));
	}

	// ---------- walkAggregation / walkAggregationMap ----------

	@Test
	public void testWalkAggregationMapWithDepthOverLimitRejected() {
		Map<String, Aggregation> map = new LinkedHashMap<>();
		map.put("a", Aggregation.of(b -> b.terms(t -> t.field("f"))));
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> SearchDslValidator.walkAggregationMap(map, SearchDslValidator.AGG_MAX_DEPTH + 1,
						new int[] { 0 }));
		assertTrue(ex.getMessage().contains("nested too deeply"));
	}

	@Test
	public void testWalkAggregationMapWithCountOverLimitRejected() {
		Map<String, Aggregation> map = new LinkedHashMap<>();
		map.put("a", Aggregation.of(b -> b.terms(t -> t.field("f"))));
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> SearchDslValidator.walkAggregationMap(map, 1,
						new int[] { SearchDslValidator.AGG_MAX_COUNT }));
		assertTrue(ex.getMessage().contains("too many aggregations"));
	}

	// ---------- checkAggSize ----------

	@Test
	public void testCheckAggSizeWithNullAccepted() {
		// call under test — must not throw
		SearchDslValidator.checkAggSize(null, "terms", "size");
	}

	@Test
	public void testCheckAggSizeWithAtCapAccepted() {
		// call under test — must not throw
		SearchDslValidator.checkAggSize(SearchDslValidator.MAX_AGG_SIZE, "terms", "size");
	}

	@Test
	public void testCheckAggSizeWithOverCapRejected() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> SearchDslValidator.checkAggSize(SearchDslValidator.MAX_AGG_SIZE + 1, "terms",
						"size"));
		assertTrue(ex.getMessage().contains("'terms' aggregation 'size'"));
	}

	// validateFieldCollapse field().isEmpty() and validateTerms's null-TermsQueryField early
	// return are unreachable: the typed FieldCollapse / TermsQuery builders require those slots at
	// construction (MissingRequiredPropertyException). The defensive checks stay in case a future
	// OpenSearch-client schema change relaxes the requirement.

	// -----------------------------------------------------------------------------
	// from / size resolution (resolveFrom / resolveSize)
	// -----------------------------------------------------------------------------

	@Test
	public void testResolveFromWithOmittedDefaultsToZero() throws Exception {
		// call under test
		assertEquals(0, SearchDslValidator.resolveFrom(MAPPER.readTree("{\"query\":{}}")));
	}

	@Test
	public void testResolveFromWithValidValue() throws Exception {
		// call under test
		assertEquals(5, SearchDslValidator.resolveFrom(MAPPER.readTree("{\"from\":5}")));
	}

	@Test
	public void testResolveFromWithNegativeRejected() throws Exception {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> SearchDslValidator.resolveFrom(MAPPER.readTree("{\"from\":-1}")));
		assertTrue(ex.getMessage().contains("body.from must be between 0 and"));
	}

	@Test
	public void testResolveFromWithOverflowRejected() throws Exception {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> SearchDslValidator.resolveFrom(MAPPER.readTree(
						"{\"from\":" + (Integer.MAX_VALUE + 1L) + "}")));
		assertTrue(ex.getMessage().contains("body.from must be between 0 and"));
	}

	@Test
	public void testResolveSizeWithOmittedDefaultsToDefaultSize() throws Exception {
		// call under test
		assertEquals(25, SearchDslValidator.resolveSize(MAPPER.readTree("{\"query\":{}}"), 25, 100));
	}

	@Test
	public void testResolveSizeWithValidValue() throws Exception {
		// call under test
		assertEquals(50, SearchDslValidator.resolveSize(MAPPER.readTree("{\"size\":50}"), 25, 100));
	}

	@Test
	public void testResolveSizeWithValueAboveMaxClamps() throws Exception {
		// call under test
		assertEquals(100, SearchDslValidator.resolveSize(MAPPER.readTree("{\"size\":10000}"), 25, 100));
	}

	@Test
	public void testResolveSizeWithNegativeRejected() throws Exception {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> SearchDslValidator.resolveSize(MAPPER.readTree("{\"size\":-1}"), 25, 100));
		assertTrue(ex.getMessage().contains("body.size must be non-negative"));
	}

	// -----------------------------------------------------------------------------
	// Opaque query leaf-value shape gate (validateQueryLeafShapes)
	// -----------------------------------------------------------------------------

	private static void validateQueryLeafShapes(String json) throws Exception {
		SearchDslValidator.validateQueryLeafShapes(MAPPER.readTree(json));
	}

	private static void validateAggregationLeafShapes(String json) throws Exception {
		SearchDslValidator.validateAggregationLeafShapes(MAPPER.readTree(json));
	}

	@Test
	public void testValidateQueryLeafShapesWithNullAccepted() {
		// call under test — a null/absent subtree is a no-op (deserializer handles structure).
		assertDoesNotThrow(() -> SearchDslValidator.validateQueryLeafShapes(null));
	}

	@Test
	public void testValidateQueryLeafShapesWithMatchScalarQueryAccepted() throws Exception {
		// call under test — scalar query value (string / number / boolean) is valid.
		validateQueryLeafShapes("{\"match\":{\"title\":{\"query\":\"amyloid\"}}}");
		validateQueryLeafShapes("{\"match\":{\"count\":{\"query\":5}}}");
		validateQueryLeafShapes("{\"match\":{\"flag\":{\"query\":true}}}");
	}

	@Test
	public void testValidateQueryLeafShapesWithMatchShorthandScalarAccepted() throws Exception {
		// call under test — the shorthand {"match":{"col":"x"}} form is left for the deserializer.
		assertDoesNotThrow(() -> validateQueryLeafShapes("{\"match\":{\"title\":\"amyloid\"}}"));
	}

	@Test
	public void testValidateQueryLeafShapesWithMatchObjectQueryRejected() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> validateQueryLeafShapes("{\"match\":{\"title\":{\"query\":{\"nested\":\"obj\"}}}}"));
		assertTrue(ex.getMessage().contains("match['title'].'query'"));
		assertTrue(ex.getMessage().contains("an object"));
	}

	@Test
	public void testValidateQueryLeafShapesWithMatchArrayQueryRejected() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> validateQueryLeafShapes("{\"match\":{\"title\":{\"query\":[1,2,3]}}}"));
		assertTrue(ex.getMessage().contains("match['title'].'query'"));
		assertTrue(ex.getMessage().contains("an array"));
	}

	@Test
	public void testValidateQueryLeafShapesWithMatchMinimumShouldMatchScalarAccepted() throws Exception {
		// minimum_should_match may be an integer or a percentage / formula string.
		validateQueryLeafShapes("{\"match\":{\"title\":{\"query\":\"x\",\"minimum_should_match\":2}}}");
		validateQueryLeafShapes("{\"match\":{\"title\":{\"query\":\"x\",\"minimum_should_match\":\"75%\"}}}");
	}

	@Test
	public void testValidateQueryLeafShapesWithMatchMinimumShouldMatchObjectRejected() {
		assertThrows(IllegalArgumentException.class,
				// call under test
				() -> validateQueryLeafShapes(
						"{\"match\":{\"title\":{\"query\":\"x\",\"minimum_should_match\":{\"bad\":1}}}}"));
	}

	@Test
	public void testValidateQueryLeafShapesWithTermObjectValueRejected() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> validateQueryLeafShapes("{\"term\":{\"status\":{\"value\":{\"bad\":1}}}}"));
		assertTrue(ex.getMessage().contains("term['status'].'value'"));
	}

	@Test
	public void testValidateQueryLeafShapesWithRangeScalarBoundsAccepted() throws Exception {
		// call under test — numeric and date-string bounds both collapse to "scalar".
		validateQueryLeafShapes("{\"range\":{\"age\":{\"gte\":18,\"lt\":65}}}");
		validateQueryLeafShapes("{\"range\":{\"created\":{\"gte\":\"2020-01-01\",\"lte\":\"2026-01-01\"}}}");
	}

	@Test
	public void testValidateQueryLeafShapesWithRangeObjectBoundRejected() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> validateQueryLeafShapes("{\"range\":{\"age\":{\"gte\":{\"nested\":\"obj\"}}}}"));
		assertTrue(ex.getMessage().contains("range['age'].'gte'"));
		assertTrue(ex.getMessage().contains("an object"));
	}

	@Test
	public void testValidateQueryLeafShapesWithRangeArrayBoundRejected() {
		assertThrows(IllegalArgumentException.class,
				// call under test
				() -> validateQueryLeafShapes("{\"range\":{\"age\":{\"gte\":[1,2,3]}}}"));
	}

	@Test
	public void testValidateQueryLeafShapesWithTermsScalarArrayAccepted() throws Exception {
		// call under test — terms is field-keyed to an array of scalars; boost sibling is scalar.
		validateQueryLeafShapes("{\"terms\":{\"tags\":[\"a\",\"b\",\"c\"],\"boost\":1.0}}");
		validateQueryLeafShapes("{\"terms\":{\"ids\":[1,2,3]}}");
	}

	@Test
	public void testValidateQueryLeafShapesWithTermsArrayOfObjectsRejected() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> validateQueryLeafShapes("{\"terms\":{\"tags\":[\"a\",{\"nested\":1}]}}"));
		assertTrue(ex.getMessage().contains("terms['tags'][1]"));
	}

	@Test
	public void testValidateQueryLeafShapesWithMultiMatchAccepted() throws Exception {
		// call under test
		validateQueryLeafShapes(
				"{\"multi_match\":{\"query\":\"amyloid\",\"fields\":[\"title^2\",\"abstract\"]}}");
	}

	@Test
	public void testValidateQueryLeafShapesWithMultiMatchObjectQueryRejected() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> validateQueryLeafShapes(
						"{\"multi_match\":{\"query\":{\"bad\":1},\"fields\":[\"title\"]}}"));
		assertTrue(ex.getMessage().contains("multi_match.query"));
	}

	@Test
	public void testValidateQueryLeafShapesWithMultiMatchFieldsContainingObjectRejected() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> validateQueryLeafShapes(
						"{\"multi_match\":{\"query\":\"x\",\"fields\":[\"title\",{\"bad\":1}]}}"));
		assertTrue(ex.getMessage().contains("multi_match.fields[1]"));
	}

	@Test
	public void testValidateQueryLeafShapesWithSimpleQueryStringFieldsObjectRejected() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> validateQueryLeafShapes(
						"{\"simple_query_string\":{\"query\":\"x\",\"fields\":[{\"bad\":1}]}}"));
		assertTrue(ex.getMessage().contains("simple_query_string.fields[0]"));
	}

	@Test
	public void testValidateQueryLeafShapesWithBoolMinimumShouldMatchObjectRejected() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> validateQueryLeafShapes(
						"{\"bool\":{\"should\":[],\"minimum_should_match\":{\"bad\":1}}}"));
		assertTrue(ex.getMessage().contains("bool.minimum_should_match"));
	}

	@Test
	public void testValidateQueryLeafShapesWithObjectBoundNestedInBoolFilterRejected() {
		// Recursion guard: a forbidden shape buried in bool.filter[*].range.gte must be reached.
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> validateQueryLeafShapes(
						"{\"bool\":{\"filter\":[{\"match_all\":{}},"
								+ "{\"range\":{\"age\":{\"gte\":{\"deep\":\"obj\"}}}}]}}"));
		assertTrue(ex.getMessage().contains("range['age'].'gte'"));
	}

	@Test
	public void testValidateQueryLeafShapesWithObjectValueNestedInConstantScoreRejected() {
		assertThrows(IllegalArgumentException.class,
				// call under test
				() -> validateQueryLeafShapes(
						"{\"constant_score\":{\"filter\":{\"term\":{\"s\":{\"value\":{\"bad\":1}}}}}}"));
	}

	@Test
	public void testValidateQueryLeafShapesWithObjectValueNestedInBoostingRejected() {
		assertThrows(IllegalArgumentException.class,
				// call under test
				() -> validateQueryLeafShapes(
						"{\"boosting\":{\"positive\":{\"match_all\":{}},"
								+ "\"negative\":{\"prefix\":{\"t\":{\"value\":{\"bad\":1}}}}}}"));
	}

	@Test
	public void testValidateQueryLeafShapesWithObjectValueNestedInDisMaxRejected() {
		assertThrows(IllegalArgumentException.class,
				// call under test
				() -> validateQueryLeafShapes(
						"{\"dis_max\":{\"queries\":[{\"wildcard\":{\"t\":{\"value\":{\"bad\":1}}}}]}}"));
	}

	// -----------------------------------------------------------------------------
	// Opaque aggregation leaf-value shape gate (validateAggregationLeafShapes)
	// -----------------------------------------------------------------------------

	@Test
	public void testValidateAggregationLeafShapesWithNullAccepted() {
		// call under test — absent map is a no-op.
		assertDoesNotThrow(() -> SearchDslValidator.validateAggregationLeafShapes(null));
	}

	@Test
	public void testValidateAggregationLeafShapesWithScalarBoundsAccepted() throws Exception {
		// call under test
		validateAggregationLeafShapes(
				"{\"by_age\":{\"histogram\":{\"field\":\"age\",\"interval\":10,"
						+ "\"extended_bounds\":{\"min\":0,\"max\":100}}}}");
	}

	@Test
	public void testValidateAggregationLeafShapesWithObjectExtendedBoundRejected() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> validateAggregationLeafShapes(
						"{\"by_age\":{\"histogram\":{\"field\":\"age\","
								+ "\"extended_bounds\":{\"min\":0,\"max\":{\"bad\":1}}}}}"));
		assertTrue(ex.getMessage().contains("histogram.extended_bounds.max"));
	}

	@Test
	public void testValidateAggregationLeafShapesWithObjectRangeFromRejected() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> validateAggregationLeafShapes(
						"{\"buckets\":{\"range\":{\"field\":\"age\","
								+ "\"ranges\":[{\"from\":0,\"to\":10},{\"from\":{\"bad\":1},\"to\":20}]}}}"));
		assertTrue(ex.getMessage().contains("range.ranges[1].from"));
	}

	@Test
	public void testValidateAggregationLeafShapesWithTermsIncludeRegexAccepted() throws Exception {
		// include / exclude may be a regex string OR an array of exact values.
		validateAggregationLeafShapes("{\"a\":{\"terms\":{\"field\":\"f\",\"include\":\"^a.*\"}}}");
		validateAggregationLeafShapes("{\"a\":{\"terms\":{\"field\":\"f\",\"include\":[\"a\",\"b\"]}}}");
	}

	@Test
	public void testValidateAggregationLeafShapesWithTermsIncludeObjectRejected() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> validateAggregationLeafShapes(
						"{\"a\":{\"terms\":{\"field\":\"f\",\"include\":{\"bad\":1}}}}"));
		assertTrue(ex.getMessage().contains("terms aggregation 'include'"));
	}

	@Test
	public void testValidateAggregationLeafShapesWithTermsOrderObjectAccepted() throws Exception {
		// `order` is a genuinely free-form sort object and must pass through untouched.
		assertDoesNotThrow(() -> validateAggregationLeafShapes(
				"{\"a\":{\"terms\":{\"field\":\"f\",\"order\":{\"_count\":\"desc\"}}}}"));
	}

	@Test
	public void testValidateAggregationLeafShapesWithTermsMissingScalarAccepted() throws Exception {
		// call under test
		validateAggregationLeafShapes("{\"a\":{\"terms\":{\"field\":\"f\",\"missing\":\"N/A\"}}}");
	}

	@Test
	public void testValidateAggregationLeafShapesWithMetricMissingObjectRejected() {
		// `missing` is opaque on the metric aggregations too (MissingValueOption), not just terms.
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> validateAggregationLeafShapes(
						"{\"a\":{\"avg\":{\"field\":\"f\",\"missing\":{\"bad\":1}}}}"));
		assertTrue(ex.getMessage().contains("avg aggregation 'missing'"));
	}

	@Test
	public void testValidateAggregationLeafShapesWithCardinalityMissingScalarAccepted() throws Exception {
		// call under test
		validateAggregationLeafShapes("{\"a\":{\"cardinality\":{\"field\":\"f\",\"missing\":0}}}");
	}

	@Test
	public void testValidateAggregationLeafShapesWithObjectBoundInSubAggregationRejected() {
		// Recursion guard: a forbidden shape inside a sub-aggregation must be reached.
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> validateAggregationLeafShapes(
						"{\"outer\":{\"terms\":{\"field\":\"f\"},"
								+ "\"aggregations\":{\"inner\":{\"histogram\":{\"field\":\"age\","
								+ "\"extended_bounds\":{\"min\":{\"bad\":1},\"max\":10}}}}}}"));
		assertTrue(ex.getMessage().contains("histogram.extended_bounds.min"));
	}

	@Test
	public void testValidateAggregationLeafShapesWithFilterObjectQueryValueRejected() throws Exception {
		// A filter body is a query subtree, so its opaque leaf slots pass the same shape gate as the
		// top-level query — an object where a scalar is required is rejected.
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> validateAggregationLeafShapes(
						"{\"a\":{\"filter\":{\"term\":{\"status\":{\"value\":{\"bad\":1}}}},"
								+ "\"aggregations\":{\"inner\":{\"terms\":{\"field\":\"f\"}}}}}"));
		assertTrue(ex.getMessage().contains("term['status'].'value'"));
	}

	@Test
	public void testValidateAggregationLeafShapesWithFiltersKeyedObjectQueryValueRejected() throws Exception {
		// Each named query in a keyed filters bucket passes through the query leaf-shape gate.
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> validateAggregationLeafShapes(
						"{\"a\":{\"filters\":{\"filters\":{"
								+ "\"ok\":{\"term\":{\"status\":{\"value\":\"y\"}}},"
								+ "\"bad\":{\"term\":{\"status\":{\"value\":{\"nested\":1}}}}}}}}"));
		assertTrue(ex.getMessage().contains("term['status'].'value'"));
	}

	@Test
	public void testValidateAggregationLeafShapesWithFiltersArrayObjectQueryValueRejected() throws Exception {
		// The array (non-keyed) filters form is gated the same way.
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> validateAggregationLeafShapes(
						"{\"a\":{\"filters\":{\"filters\":["
								+ "{\"term\":{\"status\":{\"value\":\"y\"}}},"
								+ "{\"term\":{\"status\":{\"value\":{\"nested\":1}}}}]}}}"));
		assertTrue(ex.getMessage().contains("term['status'].'value'"));
	}

	// -----------------------------------------------------------------------------
	// Highlight highlight_query leaf-shape gate (validateHighlightQueryLeafShapes)
	// -----------------------------------------------------------------------------

	private static void validateHighlightQueryLeafShapes(String json) throws Exception {
		SearchDslValidator.validateHighlightQueryLeafShapes(MAPPER.readTree(json));
	}

	@Test
	public void testValidateHighlightQueryLeafShapesWithNullAccepted() {
		// call under test — a null/non-object highlight block is a no-op.
		assertDoesNotThrow(() -> SearchDslValidator.validateHighlightQueryLeafShapes(null));
	}

	@Test
	public void testValidateHighlightQueryLeafShapesWithScalarQueriesAccepted() throws Exception {
		// call under test — scalar leaf values in both the top-level and per-field highlight_query pass.
		validateHighlightQueryLeafShapes(
				"{\"highlight_query\":{\"match\":{\"title\":{\"query\":\"x\"}}},"
						+ "\"fields\":{\"title\":{\"highlight_query\":{\"term\":{\"status\":{\"value\":\"y\"}}}}}}");
	}

	@Test
	public void testValidateHighlightQueryLeafShapesWithTopLevelObjectQueryRejected() {
		// The top-level highlight_query subtree is run through the same query leaf-shape gate.
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> validateHighlightQueryLeafShapes(
						"{\"highlight_query\":{\"match\":{\"title\":{\"query\":{\"bad\":1}}}}}"));
		assertTrue(ex.getMessage().contains("match['title'].'query'"));
	}

	@Test
	public void testValidateHighlightQueryLeafShapesWithPerFieldObjectQueryRejected() {
		// A per-field highlight_query under `fields` is gated too.
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> validateHighlightQueryLeafShapes(
						"{\"fields\":{\"title\":{\"highlight_query\":{\"term\":{\"status\":{\"value\":{\"bad\":1}}}}}}}"));
		assertTrue(ex.getMessage().contains("term['status'].'value'"));
	}

	// -----------------------------------------------------------------------------
	// Sort kind allowlist (validateSort)
	// -----------------------------------------------------------------------------

	@Test
	public void testValidateSortWithFieldAndScoreKindsAccepted() {
		List<SortOptions> sort = List.of(
				SortOptions.of(so -> so.field(FieldSort.of(fs -> fs.field("year").order(SortOrder.Desc)))),
				SortOptions.of(so -> so.score(sc -> sc.order(SortOrder.Desc))));
		// call under test — both allowlisted kinds pass.
		assertDoesNotThrow(() -> SearchDslValidator.validateSort(sort));
	}

	@Test
	public void testValidateSortWithDocKindRejected() {
		// _doc (internal Lucene order) is not on the allowlist. It deserializes to the Doc kind.
		List<SortOptions> sort = List.of(SortOptions.of(so -> so.doc(d -> d.order(SortOrder.Asc))));
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> SearchDslValidator.validateSort(sort));
		assertTrue(ex.getMessage().contains("sort kind is not allowed"));
	}

	@Test
	public void testValidateSortWithNullRejected() {
		assertThrows(IllegalArgumentException.class,
				// call under test
				() -> SearchDslValidator.validateSort(null));
	}

	@Test
	public void testAllowedSortKindsExcludesScriptAndGeoDistance() {
		// Membership guard: the script and geo-distance sort kinds (the ones that run Painless /
		// require geo fields) must never be on the allowlist. Constructing those typed variants
		// requires deep fixtures (Script / GeoLocation), so this asserts the allowlist directly —
		// parseSort rejects them via validateSort because they aren't members.
		assertTrue(!SearchDslValidator.ALLOWED_SORT_KINDS.contains(SortOptions.Kind.Script));
		assertTrue(!SearchDslValidator.ALLOWED_SORT_KINDS.contains(SortOptions.Kind.GeoDistance));
		assertTrue(!SearchDslValidator.ALLOWED_SORT_KINDS.contains(SortOptions.Kind.Doc));
		assertTrue(SearchDslValidator.ALLOWED_SORT_KINDS.contains(SortOptions.Kind.Field));
		assertTrue(SearchDslValidator.ALLOWED_SORT_KINDS.contains(SortOptions.Kind.Score));
	}

	// -----------------------------------------------------------------------------
	// Leaf-shape helpers — direct per-branch coverage
	// -----------------------------------------------------------------------------

	// ---------- validateQueryLeafShapesInArray ----------

	@Test
	public void testValidateQueryLeafShapesInArrayWithNullReturns() {
		// call under test — null array is a no-op.
		assertDoesNotThrow(() -> SearchDslValidator.validateQueryLeafShapesInArray(null));
	}

	@Test
	public void testValidateQueryLeafShapesInArrayWithNonArrayReturns() throws Exception {
		// call under test — a non-array node is left for the deserializer.
		assertDoesNotThrow(() -> SearchDslValidator.validateQueryLeafShapesInArray(
				MAPPER.readTree("{\"not\":\"an array\"}")));
	}

	@Test
	public void testValidateQueryLeafShapesInArrayWithElementsRecurses() throws Exception {
		// call under test — recurses into each element; a bad shape in any element is rejected.
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> SearchDslValidator.validateQueryLeafShapesInArray(MAPPER.readTree(
						"[{\"match_all\":{}},{\"term\":{\"s\":{\"value\":{\"bad\":1}}}}]")));
		assertTrue(ex.getMessage().contains("term['s'].'value'"));
	}

	// ---------- validateFieldKeyedScalarOptions ----------

	@Test
	public void testValidateFieldKeyedScalarOptionsWithAbsentClauseKindReturns() throws Exception {
		// call under test — the named clause kind is absent (map == null), a no-op.
		assertDoesNotThrow(() -> SearchDslValidator.validateFieldKeyedScalarOptions(
				MAPPER.readTree("{\"other\":{}}"), "match", "query"));
	}

	@Test
	public void testValidateFieldKeyedScalarOptionsWithNonObjectClauseKindReturns() throws Exception {
		// call under test — the clause kind value is not an object, left for the deserializer.
		assertDoesNotThrow(() -> SearchDslValidator.validateFieldKeyedScalarOptions(
				MAPPER.readTree("{\"match\":\"scalar\"}"), "match", "query"));
	}

	@Test
	public void testValidateFieldKeyedScalarOptionsWithShorthandScalarColumnSkipped() throws Exception {
		// call under test — the {"match":{"col":"x"}} shorthand (column value is a scalar, not an
		// options object) is acceptable and skipped.
		assertDoesNotThrow(() -> SearchDslValidator.validateFieldKeyedScalarOptions(
				MAPPER.readTree("{\"match\":{\"title\":\"amyloid\"}}"), "match", "query"));
	}

	@Test
	public void testValidateFieldKeyedScalarOptionsWithObjectOptionRejected() throws Exception {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> SearchDslValidator.validateFieldKeyedScalarOptions(
						MAPPER.readTree("{\"match\":{\"title\":{\"query\":{\"bad\":1}}}}"),
						"match", "query"));
		assertTrue(ex.getMessage().contains("match['title'].'query'"));
	}

	// ---------- validateSingleAggregationLeafShapes ----------

	@Test
	public void testValidateSingleAggregationLeafShapesWithNullReturns() {
		// call under test — null aggregation is a no-op.
		assertDoesNotThrow(() -> SearchDslValidator.validateSingleAggregationLeafShapes(null));
	}

	@Test
	public void testValidateSingleAggregationLeafShapesWithNonObjectReturns() throws Exception {
		// call under test — a non-object aggregation node is left for the deserializer.
		assertDoesNotThrow(() -> SearchDslValidator.validateSingleAggregationLeafShapes(
				MAPPER.readTree("\"scalar\"")));
	}

	@Test
	public void testValidateSingleAggregationLeafShapesWithBadMissingRejected() throws Exception {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> SearchDslValidator.validateSingleAggregationLeafShapes(
						MAPPER.readTree("{\"sum\":{\"field\":\"f\",\"missing\":{\"bad\":1}}}")));
		assertTrue(ex.getMessage().contains("sum aggregation 'missing'"));
	}

	// ---------- checkBoundsShape ----------

	@Test
	public void testCheckBoundsShapeWithNonObjectReturns() throws Exception {
		// call under test — a missing/non-object aggregation body short-circuits.
		assertDoesNotThrow(() -> SearchDslValidator.checkBoundsShape(
				MAPPER.readTree("{}").path("histogram"), "histogram"));
	}

	@Test
	public void testCheckBoundsShapeWithHardBoundsObjectMaxRejected() throws Exception {
		// Exercises the hard_bounds branch (the extended_bounds branch is covered via the agg facade).
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> SearchDslValidator.checkBoundsShape(
						MAPPER.readTree("{\"hard_bounds\":{\"min\":0,\"max\":{\"bad\":1}}}"), "histogram"));
		assertTrue(ex.getMessage().contains("histogram.hard_bounds.max"));
	}

	// ---------- checkMinMax ----------

	@Test
	public void testCheckMinMaxWithNullReturns() {
		// call under test — absent bounds is a no-op.
		assertDoesNotThrow(() -> SearchDslValidator.checkMinMax(null, "histogram.extended_bounds"));
	}

	@Test
	public void testCheckMinMaxWithNonObjectReturns() throws Exception {
		// call under test — a scalar where the bounds object is expected is left alone.
		assertDoesNotThrow(() -> SearchDslValidator.checkMinMax(
				MAPPER.readTree("5"), "histogram.extended_bounds"));
	}

	@Test
	public void testCheckMinMaxWithScalarMinAndMaxAccepted() throws Exception {
		// call under test — both scalar bounds pass.
		assertDoesNotThrow(() -> SearchDslValidator.checkMinMax(
				MAPPER.readTree("{\"min\":0,\"max\":100}"), "histogram.extended_bounds"));
	}

	@Test
	public void testCheckMinMaxWithObjectMinRejected() throws Exception {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> SearchDslValidator.checkMinMax(
						MAPPER.readTree("{\"min\":{\"bad\":1},\"max\":100}"), "histogram.extended_bounds"));
		assertTrue(ex.getMessage().contains("histogram.extended_bounds.min"));
	}

	// ---------- checkRangesShape ----------

	@Test
	public void testCheckRangesShapeWithNonObjectReturns() throws Exception {
		// call under test — a missing/non-object aggregation body short-circuits.
		assertDoesNotThrow(() -> SearchDslValidator.checkRangesShape(
				MAPPER.readTree("{}").path("range"), "range"));
	}

	@Test
	public void testCheckRangesShapeWithAbsentRangesReturns() throws Exception {
		// call under test — no `ranges` key is a no-op.
		assertDoesNotThrow(() -> SearchDslValidator.checkRangesShape(
				MAPPER.readTree("{\"field\":\"f\"}"), "range"));
	}

	@Test
	public void testCheckRangesShapeWithNonArrayRangesReturns() throws Exception {
		// call under test — a non-array `ranges` value is left for the deserializer.
		assertDoesNotThrow(() -> SearchDslValidator.checkRangesShape(
				MAPPER.readTree("{\"ranges\":\"nope\"}"), "range"));
	}

	@Test
	public void testCheckRangesShapeWithNonObjectRangeElementSkipped() throws Exception {
		// call under test — a non-object element in the ranges array is skipped (left for the
		// deserializer), not treated as a from/to-bearing object.
		assertDoesNotThrow(() -> SearchDslValidator.checkRangesShape(
				MAPPER.readTree("{\"ranges\":[\"scalar\",{\"from\":0,\"to\":10}]}"), "range"));
	}

	@Test
	public void testCheckRangesShapeWithObjectToBoundRejected() throws Exception {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> SearchDslValidator.checkRangesShape(
						MAPPER.readTree("{\"ranges\":[{\"from\":0,\"to\":{\"bad\":1}}]}"), "range"));
		assertTrue(ex.getMessage().contains("range.ranges[0].to"));
	}

	// ---------- isScalar ----------

	@Test
	public void testIsScalarAcrossNodeShapes() throws Exception {
		// call under test — every shape branch.
		assertFalse(SearchDslValidator.isScalar(null));
		assertFalse(SearchDslValidator.isScalar(MAPPER.readTree("null")));
		assertTrue(SearchDslValidator.isScalar(MAPPER.readTree("\"text\"")));
		assertTrue(SearchDslValidator.isScalar(MAPPER.readTree("5")));
		assertTrue(SearchDslValidator.isScalar(MAPPER.readTree("true")));
		assertFalse(SearchDslValidator.isScalar(MAPPER.readTree("{}")));
		assertFalse(SearchDslValidator.isScalar(MAPPER.readTree("[]")));
	}

	// ---------- describeShape ----------

	@Test
	public void testDescribeShapeAcrossNodeShapes() throws Exception {
		// call under test — every shape branch.
		assertEquals("null", SearchDslValidator.describeShape(null));
		assertEquals("null", SearchDslValidator.describeShape(MAPPER.readTree("null")));
		assertEquals("an object", SearchDslValidator.describeShape(MAPPER.readTree("{}")));
		assertEquals("an array", SearchDslValidator.describeShape(MAPPER.readTree("[]")));
		assertEquals("a scalar", SearchDslValidator.describeShape(MAPPER.readTree("\"x\"")));
	}

	// ---------- requireScalar ----------

	@Test
	public void testRequireScalarWithJavaNullReturns() {
		// call under test — an absent (Java null) value is a no-op.
		assertDoesNotThrow(() -> SearchDslValidator.requireScalar(null, "label"));
	}

	@Test
	public void testRequireScalarWithJsonNullReturns() throws Exception {
		// call under test — an explicit JSON null is a no-op.
		assertDoesNotThrow(() -> SearchDslValidator.requireScalar(MAPPER.readTree("null"), "label"));
	}

	@Test
	public void testRequireScalarWithScalarAccepted() throws Exception {
		// call under test
		assertDoesNotThrow(() -> SearchDslValidator.requireScalar(MAPPER.readTree("\"x\""), "label"));
	}

	@Test
	public void testRequireScalarWithObjectRejected() throws Exception {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> SearchDslValidator.requireScalar(MAPPER.readTree("{\"bad\":1}"), "label"));
		assertTrue(ex.getMessage().contains("label must be a number, string, or boolean, not an object"));
	}

	// ---------- requireScalarArray ----------

	@Test
	public void testRequireScalarArrayWithJavaNullReturns() {
		// call under test — an absent (Java null) value is a no-op.
		assertDoesNotThrow(() -> SearchDslValidator.requireScalarArray(null, "label"));
	}

	@Test
	public void testRequireScalarArrayWithJsonNullReturns() throws Exception {
		// call under test — an explicit JSON null is a no-op.
		assertDoesNotThrow(() -> SearchDslValidator.requireScalarArray(MAPPER.readTree("null"), "label"));
	}

	@Test
	public void testRequireScalarArrayWithScalarArrayAccepted() throws Exception {
		// call under test
		assertDoesNotThrow(() -> SearchDslValidator.requireScalarArray(
				MAPPER.readTree("[\"a\",1,true]"), "label"));
	}

	@Test
	public void testRequireScalarArrayWithNonArrayRejected() throws Exception {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> SearchDslValidator.requireScalarArray(MAPPER.readTree("\"x\""), "label"));
		assertTrue(ex.getMessage().contains("label must be an array"));
	}

	@Test
	public void testRequireScalarArrayWithNonScalarElementRejected() throws Exception {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> SearchDslValidator.requireScalarArray(MAPPER.readTree("[\"a\",{\"bad\":1}]"), "label"));
		assertTrue(ex.getMessage().contains("label[1] must be a number, string, or boolean, not an object"));
	}

	// ---------- requireScalarOrScalarArray ----------

	@Test
	public void testRequireScalarOrScalarArrayWithJavaNullReturns() {
		// call under test — an absent (Java null) value is a no-op.
		assertDoesNotThrow(() -> SearchDslValidator.requireScalarOrScalarArray(null, "label"));
	}

	@Test
	public void testRequireScalarOrScalarArrayWithJsonNullReturns() throws Exception {
		// call under test — an explicit JSON null is a no-op.
		assertDoesNotThrow(() -> SearchDslValidator.requireScalarOrScalarArray(
				MAPPER.readTree("null"), "label"));
	}

	@Test
	public void testRequireScalarOrScalarArrayWithScalarAccepted() throws Exception {
		// call under test
		assertDoesNotThrow(() -> SearchDslValidator.requireScalarOrScalarArray(
				MAPPER.readTree("\"x\""), "label"));
	}

	@Test
	public void testRequireScalarOrScalarArrayWithScalarArrayAccepted() throws Exception {
		// call under test — delegates to requireScalarArray.
		assertDoesNotThrow(() -> SearchDslValidator.requireScalarOrScalarArray(
				MAPPER.readTree("[\"a\",\"b\"]"), "label"));
	}

	@Test
	public void testRequireScalarOrScalarArrayWithArrayOfObjectsRejected() throws Exception {
		assertThrows(IllegalArgumentException.class,
				// call under test
				() -> SearchDslValidator.requireScalarOrScalarArray(
						MAPPER.readTree("[{\"bad\":1}]"), "label"));
	}

	@Test
	public void testRequireScalarOrScalarArrayWithObjectRejected() throws Exception {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				// call under test
				() -> SearchDslValidator.requireScalarOrScalarArray(
						MAPPER.readTree("{\"bad\":1}"), "label"));
		assertTrue(ex.getMessage().contains("label must be a number, string, or boolean, or an array"));
	}
}
