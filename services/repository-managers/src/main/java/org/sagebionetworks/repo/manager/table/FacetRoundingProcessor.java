package org.sagebionetworks.repo.manager.table;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.model.FacetPostProcessingAlgorithm;
import org.sagebionetworks.repo.model.FacetPostProcessingParameters;
import org.sagebionetworks.repo.model.FacetRoundingParameters;
import org.sagebionetworks.repo.model.table.FacetColumnResult;
import org.sagebionetworks.repo.model.table.FacetColumnResultBinnedValueCount;
import org.sagebionetworks.repo.model.table.FacetColumnResultBinnedValues;
import org.sagebionetworks.repo.model.table.FacetColumnResultValueCount;
import org.sagebionetworks.repo.model.table.FacetColumnResultValues;
import org.sagebionetworks.table.cluster.CountSuppressionSpec;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

/**
 * Obscures exact facet counts by floor-based binning: each count is reported as the inclusive lower
 * bound of the fixed-width bin that contains it. With a bin width of five, exact counts 210 through
 * 214 all report a bin minimum of 210, so the exact value is hidden within a known range.
 */
@Service
public class FacetRoundingProcessor implements FacetPostProcessor {

	@Override
	public FacetPostProcessingAlgorithm getAlgorithm() {
		return FacetPostProcessingAlgorithm.ROUNDING;
	}

	@Override
	public List<FacetColumnResult> process(List<FacetColumnResult> rawFacets, FacetPostProcessingParameters parameters) {
		ValidateArgument.required(rawFacets, "rawFacets");
		long binSize = binSize(parameters);

		List<FacetColumnResult> results = new ArrayList<>(rawFacets.size());
		for (FacetColumnResult facet : rawFacets) {
			if (facet instanceof FacetColumnResultValues) {
				results.add(bin((FacetColumnResultValues) facet, binSize));
			} else {
				// Facet types that do not carry counts (e.g. range facets) are passed through unchanged.
				results.add(facet);
			}
		}
		return results;
	}

	@Override
	public void validateParameters(FacetPostProcessingParameters parameters) {
		binSize(parameters);
	}

	@Override
	public long processCount(long count, FacetPostProcessingParameters parameters) {
		return floorToBin(count, binSize(parameters));
	}

	private static long binSize(FacetPostProcessingParameters parameters) {
		ValidateArgument.required(parameters, "parameters");
		ValidateArgument.requirement(parameters instanceof FacetRoundingParameters,
				"The ROUNDING algorithm requires FacetRoundingParameters");
		Long roundTo = ((FacetRoundingParameters) parameters).getRoundTo();
		ValidateArgument.required(roundTo, "roundTo");
		ValidateArgument.requirement(roundTo > 0, "roundTo must be a positive value");
		return roundTo;
	}

	/**
	 * @return The inclusive lower bound of the fixed-width bin that contains the count.
	 */
	private static long floorToBin(long count, long binSize) {
		return (count / binSize) * binSize;
	}

	private FacetColumnResultBinnedValues bin(FacetColumnResultValues facet, long binSize) {
		List<FacetColumnResultBinnedValueCount> binnedValues = new ArrayList<>(facet.getFacetValues().size());
		for (FacetColumnResultValueCount valueCount : facet.getFacetValues()) {
			// A masked count is already below the suppression threshold, so it stays masked rather than
			// being floored into the lowest bin.
			long binMin = CountSuppressionSpec.MASKED_COUNT == valueCount.getCount() ? CountSuppressionSpec.MASKED_COUNT
					: floorToBin(valueCount.getCount(), binSize);
			binnedValues.add(new FacetColumnResultBinnedValueCount()
					.setValue(valueCount.getValue())
					.setBinMin(binMin)
					.setIsSelected(valueCount.getIsSelected()));
		}
		return new FacetColumnResultBinnedValues()
				.setColumnName(facet.getColumnName())
				.setJsonPath(facet.getJsonPath())
				.setFacetType(facet.getFacetType())
				.setBinSize(binSize)
				.setBinnedValues(binnedValues);
	}

}
