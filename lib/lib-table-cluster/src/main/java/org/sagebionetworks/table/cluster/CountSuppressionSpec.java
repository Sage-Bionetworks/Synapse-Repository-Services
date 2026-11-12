package org.sagebionetworks.table.cluster;

import java.util.List;

import org.sagebionetworks.repo.model.AggregateCountSuppressionStrategy;

/**
 * Describes how a row-returning aggregate query should hide participant-count cells whose value is
 * non-zero but below the suppression threshold. Supplied to {@link QueryTranslator} so the
 * suppression is applied at the SQL level (see
 * {@link SQLTranslatorUtils#applyCountSuppression(org.sagebionetworks.table.query.model.QuerySpecification, AggregateCountSuppressionStrategy, long, List)}).
 *
 * @param strategy                    how below-threshold counts are treated
 * @param threshold                   the k value; counts in the open range (0, threshold) are suppressed
 * @param protectedCountColumnIndexes zero-based indexes into the select list of the protected count columns
 * @param definingCountColumnIndexes  zero-based indexes into the select list of a VirtualTable's inlined
 *                                    defining SQL of the protected count columns. Empty when the query
 *                                    inlines no defining SQL.
 */
public record CountSuppressionSpec(AggregateCountSuppressionStrategy strategy, long threshold,
		List<Integer> protectedCountColumnIndexes, List<Integer> definingCountColumnIndexes) {

	/**
	 * A spec that protects only counts of the outer query.
	 */
	public CountSuppressionSpec(AggregateCountSuppressionStrategy strategy, long threshold,
			List<Integer> protectedCountColumnIndexes) {
		this(strategy, threshold, protectedCountColumnIndexes, List.of());
	}
}
