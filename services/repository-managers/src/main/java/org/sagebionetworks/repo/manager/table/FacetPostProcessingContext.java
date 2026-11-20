package org.sagebionetworks.repo.manager.table;

import org.sagebionetworks.repo.model.FacetPostProcessingParameters;
import org.sagebionetworks.util.ValidateArgument;

/**
 * What a {@link FacetPostProcessor} needs to obscure the counts of one aggregate-only query.
 *
 * @param parameters           the algorithm-specific parameters of the bound or previewed configuration.
 * @param suppressionThreshold the k value every released non-zero count has already passed.
 * @param salt                 the secret salt of the aggregate data source. It must never be returned to a
 *                             caller.
 */
public record FacetPostProcessingContext(FacetPostProcessingParameters parameters, long suppressionThreshold,
		String salt) {

	public FacetPostProcessingContext {
		ValidateArgument.required(parameters, "parameters");
		ValidateArgument.required(salt, "salt");
	}

	@Override
	public String toString() {
		// The salt is a secret, so it is left out.
		return "FacetPostProcessingContext[parameters=" + parameters + ", suppressionThreshold=" + suppressionThreshold
				+ "]";
	}
}
