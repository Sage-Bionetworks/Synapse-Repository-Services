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
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

/**
 * Obscures exact facet counts by adding Laplace-distributed noise, the standard mechanism for
 * differential privacy. The noise scale is derived from the privacy budget epsilon (scale =
 * 1/epsilon); a smaller epsilon yields a wider distribution and stronger obfuscation. Noised counts
 * are clamped to zero so a count is never negative.
 */
@Service
public class FacetNoiseProcessor implements FacetPostProcessor {

	private final RandomGenerator random;

	public FacetNoiseProcessor() {
		this(new Random());
	}

	/**
	 * Test seam: supply a {@link RandomGenerator} so the noise draws can be controlled or seeded and
	 * the output made reproducible.
	 */
	FacetNoiseProcessor(RandomGenerator random) {
		this.random = random;
	}

	@Override
	public FacetPostProcessingAlgorithm getAlgorithm() {
		return FacetPostProcessingAlgorithm.NOISE;
	}

	@Override
	public List<FacetColumnResult> process(List<FacetColumnResult> rawFacets, FacetPostProcessingParameters parameters) {
		ValidateArgument.required(rawFacets, "rawFacets");
		ValidateArgument.required(parameters, "parameters");
		ValidateArgument.requirement(parameters instanceof FacetNoiseParameters,
				"The NOISE algorithm requires FacetNoiseParameters");

		Double epsilon = ((FacetNoiseParameters) parameters).getEpsilon();
		ValidateArgument.required(epsilon, "epsilon");
		ValidateArgument.requirement(epsilon > 0, "epsilon must be a positive value");

		double scale = 1.0 / epsilon;

		List<FacetColumnResult> results = new ArrayList<>(rawFacets.size());
		for (FacetColumnResult facet : rawFacets) {
			if (facet instanceof FacetColumnResultValues) {
				results.add(addNoise((FacetColumnResultValues) facet, scale));
			} else {
				// Facet types that do not carry counts (e.g. range facets) are passed through unchanged.
				results.add(facet);
			}
		}
		return results;
	}

	private FacetColumnResultValues addNoise(FacetColumnResultValues facet, double scale) {
		List<FacetColumnResultValueCount> noisedValues = new ArrayList<>(facet.getFacetValues().size());
		for (FacetColumnResultValueCount valueCount : facet.getFacetValues()) {
			long noised = Math.max(0, valueCount.getCount() + Math.round(sampleLaplace(scale)));
			noisedValues.add(new FacetColumnResultValueCount()
					.setValue(valueCount.getValue())
					.setCount(noised)
					.setIsSelected(valueCount.getIsSelected()));
		}
		return new FacetColumnResultValues()
				.setColumnName(facet.getColumnName())
				.setJsonPath(facet.getJsonPath())
				.setFacetType(facet.getFacetType())
				.setFacetValues(noisedValues);
	}

	/**
	 * Draws a sample from a zero-mean Laplace distribution using inverse-CDF sampling: a uniform
	 * draw on (-0.5, 0.5] is mapped through the inverse cumulative distribution function.
	 */
	private double sampleLaplace(double scale) {
		double u = random.nextDouble() - 0.5;
		return -scale * Math.signum(u) * Math.log(1 - 2 * Math.abs(u));
	}

}
