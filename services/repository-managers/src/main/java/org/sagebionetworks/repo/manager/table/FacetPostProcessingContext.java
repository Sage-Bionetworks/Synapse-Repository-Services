package org.sagebionetworks.repo.manager.table;

import org.sagebionetworks.repo.model.FacetPostProcessingParameters;
import org.sagebionetworks.util.ValidateArgument;

/**
 * What a {@link FacetPostProcessor} needs to obscure the counts of one aggregate-only query.
 *
 * @param parameters           the algorithm-specific parameters of the bound or previewed configuration.
 * @param suppressionThreshold the k value every released non-zero count has already passed.
 */
public record FacetPostProcessingContext(FacetPostProcessingParameters parameters, long suppressionThreshold) {

	public FacetPostProcessingContext {
		ValidateArgument.required(parameters, "parameters");
	}
}
