package org.sagebionetworks.repo.manager.table;

import java.util.List;

import org.sagebionetworks.repo.model.FacetPostProcessingAlgorithm;
import org.sagebionetworks.repo.model.FacetPostProcessingParameters;
import org.sagebionetworks.repo.model.table.FacetColumnResult;

/**
 * Transforms the exact facet counts produced by a query into obscured counts before they are
 * returned for an aggregate-only query (or a data-manager preview). Each implementation realizes
 * one {@link FacetPostProcessingAlgorithm}. Implementations are resolved by algorithm through the
 * enum-keyed registry assembled in the manager configuration.
 */
public interface FacetPostProcessor {

	/**
	 * @return The algorithm realized by this processor. Used as the registry key.
	 */
	FacetPostProcessingAlgorithm getAlgorithm();

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

}
