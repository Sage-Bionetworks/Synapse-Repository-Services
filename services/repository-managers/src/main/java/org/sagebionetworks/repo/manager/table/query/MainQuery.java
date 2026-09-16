package org.sagebionetworks.repo.manager.table.query;

import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.AggregateCountSuppressionStrategy;
import org.sagebionetworks.repo.model.AggregateDataConfiguration;
import org.sagebionetworks.table.cluster.CombinedQuery;
import org.sagebionetworks.table.cluster.CountSuppressionSpec;
import org.sagebionetworks.table.cluster.QueryTranslator;
import org.sagebionetworks.util.ValidateArgument;

public class MainQuery {

	private final QueryTranslator sqlQuery;

	public MainQuery(QueryContext expansion) {

		ValidateArgument.required(expansion, "expansion");

		CombinedQuery combined = CombinedQuery.builder().setQuery(expansion.getStartingSql())
				.setSchemaProvider(expansion.getSchemaProvider()).setOverrideOffset(expansion.getOffset())
				.setOverrideLimit(expansion.getLimit()).setSelectedFacets(expansion.getSelectedFacets())
				.setSortList(expansion.getSort()).setAdditionalFilters(expansion.getAdditionalFilters()).build();

		sqlQuery = QueryTranslator.builder(combined.getCombinedSql(), expansion.getUserId())
				.schemaProvider(expansion.getSchemaProvider()).indexDescription(expansion.getIndexDescription())
				.maxBytesPerPage(expansion.getMaxBytesPerPage()).includeEntityEtag(expansion.getIncludeEntityEtag())
				.countSuppressionSpec(createCountSuppressionSpec(expansion))
				.build();
	}

	/**
	 * Build the cell-level k-anonymity spec for the main (row-returning) query, or null when there is
	 * nothing to suppress. Suppression applies only when the source is aggregate-bound and the query
	 * projects at least one protected quasi-identifier count.
	 */
	static CountSuppressionSpec createCountSuppressionSpec(QueryContext expansion) {
		Optional<AggregateDataConfiguration> config = expansion.getAggregateDataConfiguration();
		List<Integer> protectedIndexes = expansion.getProtectedCountColumnIndexes();
		if (config.isEmpty() || protectedIndexes.isEmpty()) {
			return null;
		}
		AggregateDataConfiguration configuration = config.get();
		// When the ACT has not chosen a strategy, below-threshold rows are excluded.
		AggregateCountSuppressionStrategy strategy = configuration.getCountSuppressionStrategy() == null
				? AggregateCountSuppressionStrategy.EXCLUDE_ROW
				: configuration.getCountSuppressionStrategy();
		return new CountSuppressionSpec(strategy, configuration.getSuppressionThreshold(), protectedIndexes);
	}

	public QueryTranslator getTranslator() {
		return sqlQuery;
	}

}
