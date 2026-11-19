package org.sagebionetworks.repo.manager.table;

import java.util.List;

import org.sagebionetworks.repo.model.AggregateDataConfiguration;
import org.sagebionetworks.repo.model.FacetPostProcessingConfig;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.dao.table.TableType;
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
	private final TableManagerSupport tableManagerSupport;

	public AggregateDataConfigurationValidator(FacetPostProcessorProvider facetPostProcessorProvider,
			TableManagerSupport tableManagerSupport) {
		this.facetPostProcessorProvider = facetPostProcessorProvider;
		this.tableManagerSupport = tableManagerSupport;
	}

	/**
	 * @param configuration The configuration to validate.
	 * @throws IllegalArgumentException If the configuration is missing, has no positive suppression
	 *                                  threshold, lacks a complete and valid facet post-processing
	 *                                  configuration, or approves an ID that is versioned or does not
	 *                                  identify a VirtualTable.
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
		validateApprovedVirtualTableIds(configuration.getApprovedVirtualTableIds());
	}

	/**
	 * The list is optional: without it the source allows no aggregate-only read at all.
	 */
	void validateApprovedVirtualTableIds(List<String> approvedVirtualTableIds) {
		if (approvedVirtualTableIds == null) {
			return;
		}
		for (String approvedId : approvedVirtualTableIds) {
			ValidateArgument.required(approvedId, "aggregateDataConfiguration.approvedVirtualTableIds[]");
			IdAndVersion idAndVersion = IdAndVersion.parse(approvedId);
			// Approval is granted to the VirtualTable itself, which is never versioned.
			ValidateArgument.requirement(idAndVersion.getVersion().isEmpty(),
					"aggregateDataConfiguration.approvedVirtualTableIds must not include a version: " + approvedId);
			ValidateArgument.requirement(TableType.virtualtable.equals(tableManagerSupport.getTableType(idAndVersion)),
					"aggregateDataConfiguration.approvedVirtualTableIds must only include VirtualTables: " + approvedId);
		}
	}

}
