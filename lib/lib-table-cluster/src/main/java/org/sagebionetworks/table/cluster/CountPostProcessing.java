package org.sagebionetworks.table.cluster;

import java.math.BigDecimal;
import java.util.Map;

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
	 * The index database function that releases a count as a single value from its bin, with noise drawn
	 * deterministically from the source's salt, the cell and the bin.
	 */
	String ROUND_NOISE_FUNCTION_NAME = "aggregateCountRoundNoise";

	/**
	 * The name of the bind parameter that carries the source's salt. It is never written into the SQL text.
	 */
	String SALT_PARAMETER_NAME = "aggregateDataSalt";

	/**
	 * A protected count as it appears in a row.
	 *
	 * @param countSql  the SQL of the count to obscure.
	 * @param labelSql  the SQL of a value that names the cell (the count's column and its row's group),
	 *                  independently of which records are in it.
	 * @param threshold the suppression threshold k the count has already passed.
	 */
	record CountCell(String countSql, String labelSql, long threshold) {

		public CountCell {
			ValidateArgument.required(countSql, "countSql");
			ValidateArgument.required(labelSql, "labelSql");
		}
	}

	/**
	 * @param cell the count to obscure.
	 * @return the SQL of the released value.
	 */
	String toSql(CountCell cell);

	/**
	 * @return the bind parameters the released SQL references, keyed by name.
	 */
	default Map<String, Object> getBindParameters() {
		return Map.of();
	}

	/**
	 * The count is released unchanged.
	 */
	record None() implements CountPostProcessing {

		@Override
		public String toSql(CountCell cell) {
			return cell.countSql();
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
		public String toSql(CountCell cell) {
			return ROUND_FUNCTION_NAME + "(" + cell.countSql() + ", " + roundTo + ")";
		}
	}

	/**
	 * The count is released as a single value from the fixed-width bin that contains it: the bin's midpoint
	 * plus Laplace noise, kept within the bin and never below the threshold. The noise is drawn from the salt,
	 * the cell's label and the bin, so the value depends on the count only through its bin and a cell always
	 * releases the same value.
	 *
	 * @param binSize the bin width.
	 * @param scale   the Laplace scale, 1/epsilon.
	 * @param salt    the secret salt of the aggregate data source; bound as a parameter, never inlined.
	 */
	record RoundedNoise(long binSize, double scale, String salt) implements CountPostProcessing {

		public RoundedNoise {
			ValidateArgument.requirement(binSize > 0, "binSize must be a positive value");
			ValidateArgument.requirement(scale > 0 && Double.isFinite(scale), "scale must be a positive finite value");
			ValidateArgument.required(salt, "salt");
		}

		@Override
		public String toSql(CountCell cell) {
			// A plain decimal keeps the literal free of exponent notation.
			return ROUND_NOISE_FUNCTION_NAME + "(" + cell.countSql() + ", " + binSize + ", "
					+ BigDecimal.valueOf(scale).stripTrailingZeros().toPlainString() + ", " + cell.threshold() + ", :"
					+ SALT_PARAMETER_NAME + ", " + cell.labelSql() + ")";
		}

		@Override
		public Map<String, Object> getBindParameters() {
			return Map.of(SALT_PARAMETER_NAME, salt);
		}

		@Override
		public String toString() {
			// The salt is a secret, so it is left out.
			return "RoundedNoise[binSize=" + binSize + ", scale=" + scale + "]";
		}
	}
}
