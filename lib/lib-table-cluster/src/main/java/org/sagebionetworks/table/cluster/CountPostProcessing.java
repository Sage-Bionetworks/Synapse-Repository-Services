package org.sagebionetworks.table.cluster;

import java.math.BigDecimal;

import org.sagebionetworks.util.ValidateArgument;

/**
 * How a protected count that is at or above the suppression threshold is obscured before it is released
 * in a row. Applied in the SQL after the k-threshold, so the threshold is always tested against the true
 * count.
 */
public sealed interface CountPostProcessing {

	/**
	 * The index database function that floors a count to the lower bound of its bin.
	 */
	String ROUND_FUNCTION_NAME = "aggregateCountRound";

	/**
	 * The index database function that releases a count as a value drawn around the midpoint of its bin.
	 */
	String BINNED_NOISE_FUNCTION_NAME = "aggregateCountBinnedNoise";

	/**
	 * @param countSql the SQL of the count to obscure.
	 * @return the SQL of the released value.
	 */
	String toSql(String countSql);

	/**
	 * The count is released unchanged.
	 */
	record None() implements CountPostProcessing {

		@Override
		public String toSql(String countSql) {
			return countSql;
		}
	}

	/**
	 * The count is released as the inclusive lower bound of the fixed-width bin that contains it.
	 *
	 * @param roundTo the bin width.
	 */
	record Rounding(long roundTo) implements CountPostProcessing {

		public Rounding {
			ValidateArgument.requirement(roundTo > 0, "roundTo must be a positive value");
		}

		@Override
		public String toSql(String countSql) {
			return ROUND_FUNCTION_NAME + "(" + countSql + ", " + roundTo + ")";
		}
	}

	/**
	 * The count is released as the midpoint of the fixed-width bin that contains it plus Laplace noise, kept
	 * within the bin and never below the threshold. The released value always lies in its count's own bin, so it
	 * reveals the bin and nothing finer, however often it is drawn.
	 *
	 * @param binSize   the bin width.
	 * @param scale     the Laplace scale, 1/epsilon.
	 * @param threshold the suppression threshold k every released non-zero count has already passed.
	 */
	record BinnedNoise(long binSize, double scale, long threshold) implements CountPostProcessing {

		public BinnedNoise {
			ValidateArgument.requirement(binSize > 0, "binSize must be a positive value");
			ValidateArgument.requirement(scale > 0 && Double.isFinite(scale), "scale must be a positive finite value");
		}

		@Override
		public String toSql(String countSql) {
			// A plain decimal keeps the literal free of exponent notation.
			return BINNED_NOISE_FUNCTION_NAME + "(" + countSql + ", " + binSize + ", "
					+ BigDecimal.valueOf(scale).stripTrailingZeros().toPlainString() + ", " + threshold + ")";
		}
	}
}
