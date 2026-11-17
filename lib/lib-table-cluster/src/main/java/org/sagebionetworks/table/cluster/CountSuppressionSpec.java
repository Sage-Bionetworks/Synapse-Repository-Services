package org.sagebionetworks.table.cluster;

import java.util.Set;

import org.sagebionetworks.repo.model.AggregateCountSuppressionStrategy;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Describes how an aggregate query should protect its counts. Supplied to {@link QueryTranslator} so the
 * protection is applied at the SQL level: counts whose value is non-zero but below the suppression threshold
 * are hidden, and the remaining counts are obscured by the post-processing.
 * <p>
 * Every select column of the outer query whose value is a {@code COUNT} is protected. A count computed by
 * a VirtualTable's inlined defining SQL is protected where it is computed when its column is named here.
 *
 * @param strategy                    how below-threshold counts are treated
 * @param threshold                   the k value; counts in the open range (0, threshold) are suppressed
 * @param definingCountColumnIds      the ids of the inlined object's columns whose value is a count computed
 *                                    by its defining SQL and must be suppressed there. Empty when the query
 *                                    inlines no defining SQL or reads none of its counts.
 * @param outerCountPostProcessing    how a count of the outer query that is not suppressed is obscured
 *                                    before it is released. A caller that obscures the outer counts itself
 *                                    releases them unchanged.
 * @param definingCountPostProcessing how a count of the inlined defining SQL that is not suppressed is
 *                                    obscured. The outer query reads only this value, so it is the value
 *                                    every caller releases.
 */
public record CountSuppressionSpec(AggregateCountSuppressionStrategy strategy, long threshold,
		Set<String> definingCountColumnIds, CountPostProcessing outerCountPostProcessing,
		CountPostProcessing definingCountPostProcessing) {

	/**
	 * The value that replaces a below-threshold count under
	 * {@link AggregateCountSuppressionStrategy#MASK_BELOW_THRESHOLD}.
	 */
	public static final long MASKED_COUNT = -1L;

	public CountSuppressionSpec {
		ValidateArgument.required(strategy, "strategy");
		ValidateArgument.required(definingCountColumnIds, "definingCountColumnIds");
		ValidateArgument.required(outerCountPostProcessing, "outerCountPostProcessing");
		ValidateArgument.required(definingCountPostProcessing, "definingCountPostProcessing");
		definingCountColumnIds = Set.copyOf(definingCountColumnIds);
	}
}
