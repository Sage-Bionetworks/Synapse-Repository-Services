package org.sagebionetworks.repo.manager.search;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Build-time guard for {@link SearchDslValidator}'s schema-guided opaque-leaf gate: fails the
 * build the moment a new {@code "type":"object"} leaf appears anywhere under the {@code dsl.Query}
 * or {@code dsl.Aggregation} schema without an explicit accounting here.
 *
 * <p>Every opaque leaf is scalar-enforced by default (see {@link SearchDslValidator}), so a newly
 * added leaf needs no code change to be safely rejected if non-scalar. This test exists purely so
 * that the addition is a <i>conscious</i> act: extend the matching frozen set below (and, only if
 * the new leaf is legitimately non-scalar or checked elsewhere, add an entry to
 * {@code SearchDslValidator.OPAQUE_LEAF_EXCEPTIONS}) rather than an unreviewed side effect of an
 * unrelated schema change.</p>
 */
public class SearchDslOpaqueLeafCoverageTest {

	private static final Set<String> KNOWN_QUERY_LEAVES = Set.of(
			"BoolQuery#minimum_should_match",
			"FuzzyFieldOptions#fuzziness",
			"FuzzyFieldOptions#value",
			"MatchBoolPrefixFieldOptions#fuzziness",
			"MatchBoolPrefixFieldOptions#minimum_should_match",
			"MatchBoolPrefixFieldOptions#query",
			"MatchFieldOptions#fuzziness",
			"MatchFieldOptions#minimum_should_match",
			"MatchFieldOptions#query",
			"MatchPhraseFieldOptions#query",
			"MatchPhrasePrefixFieldOptions#query",
			"MultiMatchQuery#fields[]",
			"MultiMatchQuery#fuzziness",
			"MultiMatchQuery#minimum_should_match",
			"MultiMatchQuery#query",
			"PrefixFieldOptions#value",
			"Query#terms",
			"QueryStringQuery#fields[]",
			"QueryStringQuery#minimum_should_match",
			"RangeFieldOptions#gt",
			"RangeFieldOptions#gte",
			"RangeFieldOptions#lt",
			"RangeFieldOptions#lte",
			"SimpleQueryStringQuery#fields[]",
			"SimpleQueryStringQuery#minimum_should_match",
			"TermFieldOptions#value",
			"WildcardFieldOptions#value",
			"WildcardFieldOptions#wildcard");

	/** Every {@link #KNOWN_QUERY_LEAVES} entry plus the aggregation-specific leaves. */
	private static final Set<String> KNOWN_AGGREGATION_LEAVES = union(KNOWN_QUERY_LEAVES, Set.of(
			"AggregationRange#from",
			"AggregationRange#to",
			"AvgAggregation#missing",
			"CardinalityAggregation#missing",
			"DateHistogramAggregation#missing",
			"DateHistogramAggregation#order",
			"DateRangeAggregation#missing",
			"ExtendedBounds#max",
			"ExtendedBounds#min",
			"ExtendedStatsAggregation#missing",
			"HistogramAggregation#missing",
			"HistogramAggregation#order",
			"MaxAggregation#missing",
			"MinAggregation#missing",
			"MissingAggregation#missing",
			"RangeAggregation#missing",
			"StatsAggregation#missing",
			"SumAggregation#missing",
			"TermsAggregation#exclude",
			"TermsAggregation#include",
			"TermsAggregation#missing",
			"TermsAggregation#order",
			"ValueCountAggregation#missing"));

	private static Set<String> union(Set<String> a, Set<String> b) {
		Set<String> all = new java.util.HashSet<>(a);
		all.addAll(b);
		return all;
	}

	@Test
	public void testQuerySchemaOpaqueLeavesMatchKnownSet() {
		// call under test
		assertEquals(KNOWN_QUERY_LEAVES, SearchDslValidator.collectOpaqueLeafKeys(SearchDslValidator.QUERY_SCHEMA));
	}

	@Test
	public void testAggregationSchemaOpaqueLeavesMatchKnownSet() {
		// call under test
		assertEquals(KNOWN_AGGREGATION_LEAVES,
				SearchDslValidator.collectOpaqueLeafKeys(SearchDslValidator.AGGREGATION_SCHEMA));
	}
}
