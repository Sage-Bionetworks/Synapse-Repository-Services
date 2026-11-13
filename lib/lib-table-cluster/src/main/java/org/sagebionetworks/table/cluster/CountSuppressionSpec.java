package org.sagebionetworks.table.cluster;

import java.util.Set;

import org.sagebionetworks.repo.model.AggregateCountSuppressionStrategy;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Describes how an aggregate query should hide counts whose value is non-zero but below the suppression
 * threshold. Supplied to {@link QueryTranslator} so the suppression is applied at the SQL level.
 * <p>
 * Every select column of the outer query whose value is a {@code COUNT} is protected. A count computed by
 * a VirtualTable's inlined defining SQL is protected where it is computed when its column is named here.
 *
 * @param strategy               how below-threshold counts are treated
 * @param threshold              the k value; counts in the open range (0, threshold) are suppressed
 * @param definingCountColumnIds the ids of the inlined object's columns whose value is a count computed by
 *                               its defining SQL and must be suppressed there. Empty when the query
 *                               inlines no defining SQL or reads none of its counts.
 */
public record CountSuppressionSpec(AggregateCountSuppressionStrategy strategy, long threshold,
		Set<String> definingCountColumnIds) {

	/**
	 * The value that replaces a below-threshold count under
	 * {@link AggregateCountSuppressionStrategy#MASK_BELOW_THRESHOLD}.
	 */
	public static final long MASKED_COUNT = -1L;

	public CountSuppressionSpec {
		ValidateArgument.required(strategy, "strategy");
		ValidateArgument.required(definingCountColumnIds, "definingCountColumnIds");
		definingCountColumnIds = Set.copyOf(definingCountColumnIds);
	}
}
