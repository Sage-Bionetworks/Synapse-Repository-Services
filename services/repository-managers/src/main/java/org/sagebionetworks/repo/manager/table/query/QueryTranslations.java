package org.sagebionetworks.repo.manager.table.query;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.AggregateDataConfiguration;
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
	private final AggregateDataConfiguration aggregateDataConfiguration;
	private final List<Integer> protectedCountColumnIndexes;

	public QueryTranslations(QueryContext expansion, QueryOptions options) {
		ValidateArgument.required(expansion, "expansion");
		ValidateArgument.required(options, "options");

		aggregateDataConfiguration = expansion.getAggregateDataConfiguration().orElse(null);
		protectedCountColumnIndexes = expansion.getProtectedCountColumnIndexes();

		mainQuery = new MainQuery(expansion);
		facetQueries = options.returnFacets() ? new FacetQueries(expansion) : null;
		// An aggregate-only query must always run the count to enforce the suppression
		// gate; a forced row count measures the cohort size rather than the number of
		// aggregate result rows.
		countQuery = (options.runCount() || isAggregateOnly()) ? new CountQuery(expansion, isAggregateOnly()) : null;
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
	 * @return The configuration bound to the source that restricts this query to
	 *         aggregate-only reads, or empty when the user has full read access.
	 *         When present it carries the suppression threshold and any facet
	 *         post-processing algorithm to apply.
	 */
	public Optional<AggregateDataConfiguration> getAggregateDataConfiguration() {
		return Optional.ofNullable(aggregateDataConfiguration);
	}

	/**
	 * @return True if this query is restricted to aggregate-only reads. When true, no
	 *         row-level data may be returned and the count must be gated against the
	 *         configuration's suppression threshold.
	 */
	public boolean isAggregateOnly() {
		return aggregateDataConfiguration != null;
	}

	/**
	 * @return The suppression threshold to enforce when {@link #isAggregateOnly()} is
	 *         true; null otherwise.
	 */
	public Long getSuppressionThreshold() {
		return aggregateDataConfiguration == null ? null : aggregateDataConfiguration.getSuppressionThreshold();
	}

	/**
	 * @return True when this is an aggregate-only query against a source that defines
	 *         quasi-identifier columns. Unlike a plain aggregate-only query, which returns no
	 *         rows, this query may return rows once each protected participant-count cell has
	 *         cell-level k-anonymity applied.
	 */
	public boolean isRowReturningAggregate() {
		if (aggregateDataConfiguration == null) {
			return false;
		}
		List<String> qids = aggregateDataConfiguration.getQuasiIdentifierColumnNames();
		return qids != null && !qids.isEmpty();
	}

	/**
	 * @return The zero-based indexes into each result row's values of the columns that are a
	 *         participant count of a quasi-identifier and must have cell-level k-anonymity
	 *         applied. Empty unless {@link #isRowReturningAggregate()} is true and such a
	 *         column is present.
	 */
	public List<Integer> getProtectedCountColumnIndexes() {
		return protectedCountColumnIndexes == null ? Collections.emptyList() : protectedCountColumnIndexes;
	}

}
