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
 */
public record CountSuppressionSpec(AggregateCountSuppressionStrategy strategy, long threshold,
		List<Integer> protectedCountColumnIndexes) {
}
