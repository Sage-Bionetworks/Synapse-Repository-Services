package org.sagebionetworks.repo.manager.table;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.FacetNoiseParameters;
import org.sagebionetworks.repo.model.FacetPostProcessingAlgorithm;
import org.sagebionetworks.repo.model.FacetPostProcessingParameters;
import org.sagebionetworks.repo.model.table.FacetColumnResult;
import org.sagebionetworks.repo.model.table.FacetColumnResultValueCount;
import org.sagebionetworks.repo.model.table.FacetColumnResultValues;
import org.sagebionetworks.table.cluster.CountPostProcessing;
import org.sagebionetworks.table.cluster.CountSuppressionSpec;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

/**
 * Releases each count as a single representative value from the fixed-width bin that contains it: the bin's
 * midpoint plus Laplace noise with scale 1/epsilon, kept within the bin and never below the suppression
 * threshold. The noise is drawn deterministically from a label naming the cell and the bin.
 * <p>
 * The released value always lies in the count's own bin, so it reveals exactly the bin, the same as rounding
 * with the same bin size, and no secret is needed to keep it that way. The noise only makes the value read as
 * a plain count rather than a range. A cell always releases the same value, so repeating a request gains
 * nothing. The index database function {@value CountPostProcessing#ROUND_NOISE_FUNCTION_NAME} computes the
 * same value for the counts a query releases in its rows.
 */
@Service
public class FacetNoiseProcessor implements FacetPostProcessor {

	/**
	 * The label of the total count of a query.
	 */
	static final String TOTAL_COUNT_LABEL = "count";

	/**
	 * Separates the parts of a label and of the hashed key: the unit separator, which a decimal bound never
	 * contains.
	 */
	static final char SEPARATOR = '\u001F';

	private static final int UNIFORM_HEX_DIGITS = 13;
	private static final double UNIFORM_RANGE = (double) (1L << (UNIFORM_HEX_DIGITS * 4));

	@Override
	public FacetPostProcessingAlgorithm getAlgorithm() {
		return FacetPostProcessingAlgorithm.NOISE;
	}

	@Override
	public List<FacetColumnResult> process(List<FacetColumnResult> rawFacets, FacetPostProcessingContext context) {
		ValidateArgument.required(rawFacets, "rawFacets");
		ValidateArgument.required(context, "context");
		FacetNoiseParameters parameters = validate(context.parameters());

		List<FacetColumnResult> results = new ArrayList<>(rawFacets.size());
		for (FacetColumnResult facet : rawFacets) {
			if (facet instanceof FacetColumnResultValues) {
				results.add(release((FacetColumnResultValues) facet, parameters, context));
			} else {
				// Facet types that do not carry counts (e.g. range facets) are passed through unchanged.
				results.add(facet);
			}
		}
		return results;
	}

	@Override
	public void validateParameters(FacetPostProcessingParameters parameters) {
		validate(parameters);
	}

	@Override
	public long processCount(long count, FacetPostProcessingContext context) {
		ValidateArgument.required(context, "context");
		FacetNoiseParameters parameters = validate(context.parameters());
		return release(count, parameters, context, TOTAL_COUNT_LABEL);
	}

	@Override
	public CountPostProcessing getCountPostProcessing(FacetPostProcessingContext context) {
		ValidateArgument.required(context, "context");
		FacetNoiseParameters parameters = validate(context.parameters());
		return new CountPostProcessing.RoundedNoise(parameters.getBinSize(), scale(parameters));
	}

	private static FacetNoiseParameters validate(FacetPostProcessingParameters parameters) {
		ValidateArgument.required(parameters, "parameters");
		ValidateArgument.requirement(parameters instanceof FacetNoiseParameters,
				"The NOISE algorithm requires FacetNoiseParameters");
		FacetNoiseParameters noiseParameters = (FacetNoiseParameters) parameters;
		ValidateArgument.required(noiseParameters.getBinSize(), "binSize");
		ValidateArgument.requirement(noiseParameters.getBinSize() > 0, "binSize must be a positive value");
		ValidateArgument.required(noiseParameters.getEpsilon(), "epsilon");
		ValidateArgument.requirement(noiseParameters.getEpsilon() > 0, "epsilon must be a positive value");
		return noiseParameters;
	}

	private static double scale(FacetNoiseParameters parameters) {
		return 1.0 / parameters.getEpsilon();
	}

	/**
	 * A facet value's cell is named by its facet and its value, neither of which depends on which records it
	 * counts.
	 */
	static String facetValueLabel(FacetColumnResultValues facet, FacetColumnResultValueCount valueCount) {
		return String.join(String.valueOf(SEPARATOR), "facet", facet.getColumnName(),
				Objects.toString(facet.getJsonPath(), ""), Objects.toString(valueCount.getValue(), ""));
	}

	private static FacetColumnResultValues release(FacetColumnResultValues facet, FacetNoiseParameters parameters,
			FacetPostProcessingContext context) {
		List<FacetColumnResultValueCount> releasedValues = new ArrayList<>(facet.getFacetValues().size());
		for (FacetColumnResultValueCount valueCount : facet.getFacetValues()) {
			// A masked count is already below the suppression threshold, so it stays masked.
			long count = CountSuppressionSpec.MASKED_COUNT == valueCount.getCount() ? CountSuppressionSpec.MASKED_COUNT
					: release(valueCount.getCount(), parameters, context, facetValueLabel(facet, valueCount));
			releasedValues.add(new FacetColumnResultValueCount()
					.setValue(valueCount.getValue())
					.setCount(count)
					.setIsSelected(valueCount.getIsSelected()));
		}
		return new FacetColumnResultValues()
				.setColumnName(facet.getColumnName())
				.setJsonPath(facet.getJsonPath())
				.setFacetType(facet.getFacetType())
				.setFacetValues(releasedValues);
	}

	private static long release(long count, FacetNoiseParameters parameters, FacetPostProcessingContext context,
			String label) {
		return release(count, parameters.getBinSize(), scale(parameters), context.suppressionThreshold(), label);
	}

	/**
	 * @param count     the exact count; a count of zero is released as zero.
	 * @param binSize   the width of each bin.
	 * @param scale     the Laplace scale, 1/epsilon.
	 * @param threshold the suppression threshold k.
	 * @param label     names the cell, independently of which records it counts.
	 * @return the released value.
	 */
	static long release(long count, long binSize, double scale, long threshold, String label) {
		// The threshold gate already reveals whether a count is zero, so zero stays zero.
		if (count <= 0) {
			return count;
		}
		long lowerBound = (count / binSize) * binSize;
		long upperBound = lowerBound + binSize - 1;
		// The threshold gate already reveals that a released non-zero count is at least k, so flooring at k hides
		// nothing more and never shows a value below the threshold.
		long minimum = Math.max(lowerBound, threshold);
		double centered = lowerBound + (binSize - 1) / 2.0 + laplace(uniform(label, lowerBound), scale);
		long value = (long) Math.floor(centered + 0.5);
		// Clamping to the bin is what keeps the release to the bin: it holds whatever the noise is.
		return Math.min(upperBound, Math.max(minimum, value));
	}

	/**
	 * @return a value in (0, 1) from the first 52 bits of SHA-256(label, lowerBound).
	 */
	static double uniform(String label, long lowerBound) {
		String key = label + SEPARATOR + lowerBound;
		String hex = HexFormat.of().formatHex(sha256(key.getBytes(StandardCharsets.UTF_8)));
		long bits = Long.parseLong(hex.substring(0, UNIFORM_HEX_DIGITS), 16);
		// Offsetting by half a step keeps the value strictly inside (0, 1), so the logarithm is finite.
		return (bits + 0.5) / UNIFORM_RANGE;
	}

	/**
	 * Map a uniform value in (0, 1) to a zero-mean Laplace sample through the inverse CDF.
	 */
	static double laplace(double uniform, double scale) {
		double centered = uniform - 0.5;
		return -scale * Math.signum(centered) * Math.log(1 - 2 * Math.abs(centered));
	}

	private static byte[] sha256(byte[] input) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(input);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

}
