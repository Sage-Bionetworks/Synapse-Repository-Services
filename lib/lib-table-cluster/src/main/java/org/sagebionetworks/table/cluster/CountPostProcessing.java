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
	 * The index database function that adds zero-mean Laplace noise to a count.
	 */
	String NOISE_FUNCTION_NAME = "aggregateCountNoise";

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
	 * The count is released with zero-mean Laplace noise added, rounded to an integer and clamped at zero.
	 * <p>
	 * Each released count draws its own noise, as the facets and the total count do. Counts of the same
	 * participants, whether repeated in one query (e.g. {@code COUNT(1)}, {@code COUNT(2)}) or across queries,
	 * can therefore be averaged toward the exact count. This is an accepted risk until the noise is keyed to
	 * the participants (PLFM-10146).
	 *
	 * @param scale the Laplace scale, 1/epsilon.
	 */
	record LaplaceNoise(double scale) implements CountPostProcessing {

		public LaplaceNoise {
			ValidateArgument.requirement(scale > 0 && Double.isFinite(scale), "scale must be a positive finite value");
		}

		@Override
		public String toSql(String countSql) {
			// A plain decimal keeps the literal free of exponent notation.
			return NOISE_FUNCTION_NAME + "(" + countSql + ", "
					+ BigDecimal.valueOf(scale).stripTrailingZeros().toPlainString() + ")";
		}
	}
}
