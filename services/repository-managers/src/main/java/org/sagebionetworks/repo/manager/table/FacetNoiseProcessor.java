package org.sagebionetworks.repo.manager.table;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;

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
 * Releases each count as a single value drawn around the midpoint of the fixed-width bin that contains it:
 * the midpoint plus Laplace noise with scale 1/epsilon, kept within the bin and never below the suppression
 * threshold.
 * <p>
 * The released value always lies in its count's own bin, so it reveals the bin and nothing finer, the same as
 * rounding with the same bin size. Because the noise is centered on the bin rather than on the count,
 * averaging repeated releases converges on the bin's midpoint and gains nothing. The noise only makes the
 * value read as a plain count rather than a range. The index database function
 * {@value CountPostProcessing#BINNED_NOISE_FUNCTION_NAME} draws the same way for the counts a query releases
 * in its rows.
 */
@Service
public class FacetNoiseProcessor implements FacetPostProcessor {

	private final RandomGenerator random;

	public FacetNoiseProcessor() {
		this(new Random());
	}

	/**
	 * Test seam: supply a {@link RandomGenerator} so the noise draws can be controlled or seeded.
	 */
	FacetNoiseProcessor(RandomGenerator random) {
		this.random = random;
	}

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
				results.add(release((FacetColumnResultValues) facet, parameters, context.suppressionThreshold()));
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
		return release(count, parameters, context.suppressionThreshold());
	}

	@Override
	public CountPostProcessing getCountPostProcessing(FacetPostProcessingContext context) {
		ValidateArgument.required(context, "context");
		FacetNoiseParameters parameters = validate(context.parameters());
		return new CountPostProcessing.BinnedNoise(parameters.getBinSize(), scale(parameters),
				context.suppressionThreshold());
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

	private FacetColumnResultValues release(FacetColumnResultValues facet, FacetNoiseParameters parameters,
			long threshold) {
		List<FacetColumnResultValueCount> releasedValues = new ArrayList<>(facet.getFacetValues().size());
		for (FacetColumnResultValueCount valueCount : facet.getFacetValues()) {
			// A masked count is already below the suppression threshold, so it stays masked.
			long count = CountSuppressionSpec.MASKED_COUNT == valueCount.getCount() ? CountSuppressionSpec.MASKED_COUNT
					: release(valueCount.getCount(), parameters, threshold);
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

	private long release(long count, FacetNoiseParameters parameters, long threshold) {
		return release(count, parameters.getBinSize(), scale(parameters), threshold, random.nextDouble());
	}

	/**
	 * @param count     the exact count; a count of zero is released as zero.
	 * @param binSize   the width of each bin.
	 * @param scale     the Laplace scale, 1/epsilon.
	 * @param threshold the suppression threshold k.
	 * @param uniform   a uniform draw in [0, 1).
	 * @return the released value.
	 */
	static long release(long count, long binSize, double scale, long threshold, double uniform) {
		// The threshold gate already reveals whether a count is zero, so zero stays zero.
		if (count <= 0) {
			return count;
		}
		long lowerBound = (count / binSize) * binSize;
		long upperBound = lowerBound + binSize - 1;
		// The threshold gate already reveals that a released non-zero count is at least k, so flooring at k hides
		// nothing more and never shows a value below the threshold.
		long minimum = Math.max(lowerBound, threshold);
		double centered = lowerBound + (binSize - 1) / 2.0 + laplace(uniform, scale);
		long value = (long) Math.floor(centered + 0.5);
		// Clamping to the bin is what limits the release to the bin, whatever the noise is.
		return Math.min(upperBound, Math.max(minimum, value));
	}

	/**
	 * Map a uniform draw in [0, 1) to a zero-mean Laplace sample through the inverse CDF. A draw of exactly zero
	 * would map to an infinite sample, so it maps to zero noise instead.
	 */
	static double laplace(double uniform, double scale) {
		double centered = uniform - 0.5;
		if (Math.abs(centered) >= 0.5) {
			return 0;
		}
		return -scale * Math.signum(centered) * Math.log(1 - 2 * Math.abs(centered));
	}

}
