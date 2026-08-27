package org.sagebionetworks.repo.manager.table.query;

import java.util.Optional;

import org.sagebionetworks.repo.model.table.QueryOptions;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Encapsulates the translations of the main query plus each optional query.  Immutable.
 *
 */
public class QueryTranslations {

	private final MainQuery mainQuery;
	private final FacetQueries facetQueries;
	private final CountQuery countQuery;
	private final SumFileSizesQuery sumFileSizesQuery;
	private final ActionsRequiredQuery actionsRequiredQuery;
	private final boolean aggregateOnly;
	private final Long suppressionThreshold;

	public QueryTranslations(QueryContext expansion, QueryOptions options) {
		ValidateArgument.required(expansion, "expansion");
		ValidateArgument.required(options, "options");

		aggregateOnly = expansion.isAggregateOnly();
		suppressionThreshold = expansion.getSuppressionThreshold();

		mainQuery = new MainQuery(expansion);
		facetQueries = options.returnFacets() ? new FacetQueries(expansion) : null;
		// An aggregate-only query must always run the count to enforce the suppression
		// gate; a forced row count measures the cohort size rather than the number of
		// aggregate result rows.
		countQuery = (options.runCount() || aggregateOnly) ? new CountQuery(expansion, aggregateOnly) : null;
		sumFileSizesQuery = options.runSumFileSizes() ? new SumFileSizesQuery(expansion) : null;
		actionsRequiredQuery = options.returnActionsRequired() ? new ActionsRequiredQuery(expansion) : null;
	}

	public MainQuery getMainQuery() {
		return mainQuery;
	}

	public Optional<CountQuery> getCountQuery() {
		return Optional.ofNullable(countQuery);
	}

	public Optional<FacetQueries> getFacetQueries() {
		return Optional.ofNullable(facetQueries);
	}

	public Optional<SumFileSizesQuery> getSumFileSizesQuery() {
		return Optional.ofNullable(sumFileSizesQuery);
	}

	public Optional<ActionsRequiredQuery> getActionsRequiredQuery() {
		return Optional.ofNullable(actionsRequiredQuery);
	}

	/**
	 * @return True if this query is restricted to aggregate-only reads. When true, no
	 *         row-level data may be returned and the count must be gated against
	 *         {@link #getSuppressionThreshold()}.
	 */
	public boolean isAggregateOnly() {
		return aggregateOnly;
	}

	/**
	 * @return The suppression threshold to enforce when {@link #isAggregateOnly()} is
	 *         true; may be null otherwise.
	 */
	public Long getSuppressionThreshold() {
		return suppressionThreshold;
	}

}
