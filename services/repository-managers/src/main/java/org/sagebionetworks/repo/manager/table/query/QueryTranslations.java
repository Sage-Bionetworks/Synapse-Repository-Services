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

	public QueryTranslations(QueryContext expansion, QueryOptions options) {
		ValidateArgument.required(expansion, "expansion");
		ValidateArgument.required(options, "options");
		
		mainQuery = new MainQuery(expansion);
		facetQueries = options.returnFacets() ? new FacetQueries(expansion) : null;
		countQuery = options.runCount() ? new CountQuery(expansion) : null;
		sumFileSizesQuery = options.runSumFileSizes() ? new SumFileSizesQuery(expansion) : null;
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

}
