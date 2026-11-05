package org.sagebionetworks.repo.manager.table;

import org.sagebionetworks.repo.model.AggregateDataConfiguration;
import org.sagebionetworks.repo.model.FacetPostProcessingConfig;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

/**
 * Validates an {@link AggregateDataConfiguration}, both when the Access and Compliance Team binds
 * one to a source and when a data manager supplies one as a query preview. Every count an
 * aggregate-only query returns must be obscured, so a configuration that cannot obscure them is
 * rejected up front rather than failing each later query.
 */
@Service
public class AggregateDataConfigurationValidator {

	private final FacetPostProcessorProvider facetPostProcessorProvider;

	public AggregateDataConfigurationValidator(FacetPostProcessorProvider facetPostProcessorProvider) {
		this.facetPostProcessorProvider = facetPostProcessorProvider;
	}

	/**
	 * @param configuration The configuration to validate.
	 * @throws IllegalArgumentException If the configuration is missing, has no positive suppression
	 *                                  threshold, or lacks a complete and valid facet post-processing
	 *                                  configuration.
	 */
	public void validate(AggregateDataConfiguration configuration) {
		ValidateArgument.required(configuration, "aggregateDataConfiguration");
		ValidateArgument.required(configuration.getSuppressionThreshold(), "aggregateDataConfiguration.suppressionThreshold");
		ValidateArgument.requirement(configuration.getSuppressionThreshold() > 0,
				"aggregateDataConfiguration.suppressionThreshold must be greater than zero.");
		// Quasi-identifier columns are optional: a config without them restricts the source to
		// aggregate-only reads (no row-level data), while a config with them additionally allows
		// row-returning aggregate queries with cell-level k-anonymity applied.
		FacetPostProcessingConfig facetConfig = configuration.getFacetPostProcessingConfig();
		ValidateArgument.required(facetConfig, "aggregateDataConfiguration.facetPostProcessingConfig");
		ValidateArgument.required(facetConfig.getAlgorithm(), "aggregateDataConfiguration.facetPostProcessingConfig.algorithm");
		ValidateArgument.required(facetConfig.getParameters(), "aggregateDataConfiguration.facetPostProcessingConfig.parameters");
		facetPostProcessorProvider.getProcessor(facetConfig.getAlgorithm()).validateParameters(facetConfig.getParameters());
	}

}
