package org.sagebionetworks.repo.manager.table;

import java.util.List;

import org.sagebionetworks.repo.model.FacetPostProcessingAlgorithm;
import org.sagebionetworks.repo.model.FacetPostProcessingParameters;
import org.sagebionetworks.repo.model.table.FacetColumnResult;

/**
 * Transforms the exact counts produced by a query (the facet counts and the total query count) into
 * obscured counts before they are returned for an aggregate-only query (or a data-manager preview).
 * Each implementation realizes
 * one {@link FacetPostProcessingAlgorithm}. Implementations are resolved by algorithm through the
 * enum-keyed registry assembled in the manager configuration.
 */
public interface FacetPostProcessor {

	/**
	 * @return The algorithm realized by this processor. Used as the registry key.
	 */
	FacetPostProcessingAlgorithm getAlgorithm();

	/**
	 * Validates that the given parameters are usable by this processor's algorithm.
	 *
	 * @param parameters The algorithm-specific parameters.
	 * @throws IllegalArgumentException If the parameters are missing, of the wrong type for this
	 *                                  algorithm, or carry invalid values.
	 */
	void validateParameters(FacetPostProcessingParameters parameters);

	/**
	 * Applies this processor's algorithm to the given facet results, obscuring the exact counts.
	 * Facet result types that do not carry counts (e.g. range facets) are returned unchanged.
	 *
	 * @param rawFacets  The exact facet results to obscure.
	 * @param parameters The algorithm-specific parameters (the concrete type must match this
	 *                   processor's algorithm).
	 * @return A new list of facet results with the counts obscured.
	 */
	List<FacetColumnResult> process(List<FacetColumnResult> rawFacets, FacetPostProcessingParameters parameters);

	/**
	 * Applies this processor's algorithm to a single exact count, such as the total number of rows
	 * matched by a query.
	 *
	 * @param count      The exact count to obscure.
	 * @param parameters The algorithm-specific parameters (the concrete type must match this
	 *                   processor's algorithm).
	 * @return The obscured count, never negative.
	 */
	long processCount(long count, FacetPostProcessingParameters parameters);

}
